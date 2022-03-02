package uk.ac.starlink.ttools.plot2.data;

import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.table.ByteStore;
import uk.ac.starlink.table.storage.ByteStoreAccess;
import uk.ac.starlink.table.storage.FileByteStore;
import uk.ac.starlink.table.storage.NioByteStoreAccess;
import uk.ac.starlink.table.storage.Codec;
import uk.ac.starlink.util.DataBufferedOutputStream;

/**
 * Arranges for storage of column data (arrays of typed values)
 * in byte buffers.
 * Any {@link uk.ac.starlink.table.ByteStore} type can be used,
 * but there are special entry points for use with storage in named files,
 * which makes it possible to use this class for column storage that
 * persists beyond the length of a JVM.
 * 
 * @author   Mark Taylor
 * @since    6 Jan 2020
 */
public abstract class ColumnStorage {

    private static final Map<StorageType,ColumnStorage> map_ = createTypedMap();
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.plot2.data" );

    /**
     * Creates a CachedColumn for read/write of a data array given any
     * ByteStore type.
     *
     * @param  byteStoreSupplier  factory for byte storage instances
     * @return   column storage object for data compatible with this
     */
    public abstract CachedColumn
        createColumn( Supplier<ByteStore> byteStoreSupplier );

    /**
     * Returns an array of filenames that can be used for disk-based I/O
     * of data using this storage type.
     * A base filename is supplied: for a given instance of this class,
     * the returned array will always have the same content for the
     * same base file.  This value should be a suitable name for a regular
     * file.  The current implementation adds extensions such as ".dat"
     * before using this name.
     *
     * @param  baseFile  context filename
     * @return  array of one or more filenames that can be used for storage
     */
    public abstract File[] getFileNames( File baseFile );

    /**
     * Returns a CachedColumn that can be used to read/write data using
     * disk storage.  The <code>files</code> array should be a value
     * returned from {@link #getFileNames getFileNames} method.
     *
     * @param   files  array of filenames into which bytes will be written
     * @return  CachedColumn instance for compatible data write/read
     */
    public abstract CachedColumn createDiskColumn( File[] files )
            throws IOException;

    /**
     * Returns a CachedReader that can be used to read data previously
     * written by this storage object.
     * If data has been written to the result of the
     * {@link #createDiskColumn createDiskColumn} method invoked with
     * the same <code>files</code> argument, the reader returned by
     * this method will be able to retrieve it.
     *
     * @param   files  array of filenames from which bytes will be read
     * @return  CachedReader instance from which compatible data can be read
     */
    public abstract CachedReader createDiskReader( File[] files )
            throws IOException;

    /**
     * Returns an estimate of the number of rows contained in files
     * previously written by this storage object.
     * The <code>files</code> array should be a value
     * returned from {@link #getFileNames getFileNames} method.
     * An approximate value is permitted; if no information is available,
     * -1 may be returned.
     *
     * @param   files  array of filenames from which bytes will be read
     * @return   best estimate of row count, or -1 if not known
     */
    public abstract long getDiskRowCount( File[] files );

    /**
     * Returns a ColumnStorage object suitable for use with a given StorageType.
     * Return values are thread safe.
     *
     * @param  type  storage type
     * @return  column storage object
     */
    public static ColumnStorage getStorage( StorageType type ) {
        return map_.get( type );
    }

    /**
     * Constructs and returns the mapping from StorageTypes to ColumnStorage
     * objects.
     *
     * @return  map
     */
    private static Map<StorageType,ColumnStorage> createTypedMap() {
        Map<StorageType,ColumnStorage> map = new EnumMap<>( StorageType.class );
        map.put( StorageType.DOUBLE, new FixedStorage( () -> Codec.DOUBLE ) );
        map.put( StorageType.FLOAT, new FixedStorage( () -> Codec.FLOAT ) );
        map.put( StorageType.LONG, new FixedStorage( () -> Codec.LONG ) );
        map.put( StorageType.INT, new FixedStorage( () -> Codec.INT ) );
        map.put( StorageType.SHORT, new FixedStorage( () -> Codec.SHORT ) );
        map.put( StorageType.BYTE, new FixedStorage( () -> Codec.BYTE ) );
        map.put( StorageType.DOUBLE3, new FixedStorage( Double3Codec::new ) );
        map.put( StorageType.FLOAT3, new FixedStorage( Float3Codec::new ) );
        map.put( StorageType.INT3, new FixedStorage( Int3Codec::new ) );
        map.put( StorageType.BOOLEAN, new BooleanStorage() );
        map.put( StorageType.DOUBLE_ARRAY,
                 new IndexedStorage( Codec.DOUBLE_ARRAY ) );
        map.put( StorageType.FLOAT_ARRAY,
                 new IndexedStorage( Codec.FLOAT_ARRAY ) );
        map.put( StorageType.STRING,
                 new IndexedStorage( Codec.STRING ) );
        assert map.size() == StorageType.values().length;
        return map;
    }

    /**
     * Utility method to acquire a ByteStoreAccess given a template 
     * buffer set; this makes a deep copy of the buffer set, so the
     * result does not interfere with the supplied originals. 
     *
     * @param   bufs  template buffer set
     * @return   access object
     */
    private static ByteStoreAccess createCopyAccess( ByteBuffer[] bufs ) {
        return NioByteStoreAccess
              .createAccess( NioByteStoreAccess.copyBuffers( bufs ) );
    }

    /**
     * Utility method to aquire a ByteStoreAccess view of a given file.
     *
     * @param  file   existing readable file
     * @return    accessor for bytes in file
     */
    private static ByteStoreAccess createAccess( File file )
            throws IOException {
        return NioByteStoreAccess
              .createAccess( FileByteStore.toByteBuffers( file ) );
    }

    /**
     * Partial Codec implementation for de/serializing fixed-size items best
     * represented as java Objects.
     */
    private static abstract class FixedObjectCodec extends Codec {
        final int itemSize_;

        /**
         * Constructor.
         *
         * @param  itemSize   size of serialized item in bytes
         */
        FixedObjectCodec( int itemSize ) {
            itemSize_ = itemSize;
        }
        public int decodeInt( ByteStoreAccess in ) throws IOException { 
            in.skip( itemSize_ );
            return 0;
        }
        public long decodeLong( ByteStoreAccess in ) throws IOException {
            in.skip( itemSize_ );
            return 0;
        }
        public double decodeDouble( ByteStoreAccess in ) throws IOException {
            in.skip( itemSize_ );
            return Double.NaN;
        }
        public boolean decodeBoolean( ByteStoreAccess in ) throws IOException {
            in.skip( itemSize_ );
            return false;
        }
        public int getItemSize() {
            return itemSize_;
        }                                  
    }                                      

    /**
     * Codec implementation for 3-element double arrays.
     * Note instances of this class are not thread-safe.
     */
    private static class Double3Codec extends FixedObjectCodec {
        double[] vec3_ = new double[ 3 ];
        Double3Codec() {
            super( 24 );
        }
        public int encode( Object value, DataOutput out ) throws IOException {
            double[] array = (double[]) value;
            out.writeDouble( array[ 0 ] );
            out.writeDouble( array[ 1 ] );
            out.writeDouble( array[ 2 ] );
            return 24;
        }
        public Object decodeObject( ByteStoreAccess in ) throws IOException {
            vec3_[ 0 ] = in.readDouble();
            vec3_[ 1 ] = in.readDouble();
            vec3_[ 2 ] = in.readDouble();
            return vec3_;
        }
    }

    /**
     * Codec implementation for 3-element float arrays.
     * Note instances of this class are not thread-safe.
     */
    private static class Float3Codec extends FixedObjectCodec {
        float[] vec3_ = new float[ 3 ];
        Float3Codec() {
            super( 12 );
        }
        public int encode( Object value, DataOutput out ) throws IOException {
            float[] array = (float[]) value;
            out.writeFloat( array[ 0 ] );
            out.writeFloat( array[ 1 ] );
            out.writeFloat( array[ 2 ] );
            return 12;
        }
        public Object decodeObject( ByteStoreAccess in ) throws IOException {
            vec3_[ 0 ] = in.readFloat();
            vec3_[ 1 ] = in.readFloat();
            vec3_[ 2 ] = in.readFloat();
            return vec3_;
        }
    }

    /**
     * Codec implementation for 3-element double arrays.
     * Note instances of this class are not thread-safe.
     */
    private static class Int3Codec extends FixedObjectCodec {
        int[] vec3_ = new int[ 3 ];
        Int3Codec() {
            super( 12 );
        }
        public int encode( Object value, DataOutput out ) throws IOException {
            int[] array = (int[]) value;
            out.writeInt( array[ 0 ] );
            out.writeInt( array[ 1 ] );
            out.writeInt( array[ 2 ] );
            return 12;
        }
        public Object decodeObject( ByteStoreAccess in ) throws IOException {
            vec3_[ 0 ] = in.readInt();
            vec3_[ 1 ] = in.readInt();
            vec3_[ 2 ] = in.readInt();
            return vec3_;
        }
    }

    /**
     * Partial CachedReader implementation that takes care of IOExceptions.
     * The first IOException encountered during a read is reported through
     * the logging system, and any subsequent ones are ignored.
     */
    private static abstract class ErrorLogReader implements CachedReader {
        boolean errorLogged_;

        /**
         * Read an integer, possibly throwing an exception.
         *
         * @param  ix  read index
         * @return  value at index
         */
        abstract int intValue( long ix ) throws IOException;

        /**
         * Read a long, possibly throwing an exception.
         *
         * @param  ix  read index
         * @return  value at index
         */
        abstract long longValue( long ix ) throws IOException;

        /**
         * Read a double, possibly throwing an exception.
         *
         * @param  ix  read index
         * @return  value at index
         */
        abstract double doubleValue( long ix ) throws IOException;

        /**
         * Read a boolean, possibly throwing an exception.
         *
         * @param  ix  read index
         * @return  value at index
         */
        abstract boolean booleanValue( long ix ) throws IOException;

        /**
         * Read an Object, possibly throwing an exception.
         *
         * @param  ix  read index
         * @return  value at index
         */
        abstract Object objectValue( long ix ) throws IOException;

        public int getIntValue( long ix ) {
            try {
                return intValue( ix );
            }
            catch ( IOException e ) {
                logError( e );
                return 0;
            }
        }
        public long getLongValue( long ix ) {
            try {
                return longValue( ix );
            }
            catch ( IOException e ) {
                logError( e );
                return 0;
            }
        }
        public double getDoubleValue( long ix ) {
            try {
                return doubleValue( ix );
            }
            catch ( IOException e ) {
                logError( e );
                return Double.NaN;
            }
        }
        public boolean getBooleanValue( long ix ) {
            try {
                return booleanValue( ix );
            }
            catch ( IOException e ) {
                logError( e );
                return false;
            }
        }
        public Object getObjectValue( long ix ) {
            try {
                return objectValue( ix );
            }
            catch ( IOException e ) {
                logError( e );
                return null;
            }
        }

        /**
         * Does appropriate reporting of an error.
         * The first time it's called, this reports the error through
         * the logging system, and subsequent calls do nothing.
         */
        private void logError( IOException e ) {
            if ( ! errorLogged_ ) {
                logger_.log( Level.WARNING, "Read error", e );
                errorLogged_ = true;
            }
        }
    }

    /**
     * Partial CachedColumn implementation for columns streamed to a
     * single ByteStore.
     */
    private static abstract class StreamColumn implements CachedColumn {

        final ByteStore store_;
        final DataBufferedOutputStream out_;
        long nrow_;
        ByteBuffer[] bufs_;

        /**
         * Constructor.
         *
         * @param  store  byte store
         */
        StreamColumn( ByteStore store ) {
            store_ = store;
            out_ = new DataBufferedOutputStream( store.getOutputStream() );
        }

        /**
         * Subclasses must implement this method to do the actual writing
         * of a value to the output stream, <code>out_<code>.
         *
         * @param  value  value to write
         */
        abstract void doAdd( Object value ) throws IOException;

        /**
         * Subclasses must implement this method to create a CachedReader.
         *
         * @param  access  accessor for data that has been streamed
         * @return   column reader object, not expected to be safe for
         *           concurrent use from multiple threads
         */
        abstract CachedReader createReader( ByteStoreAccess access );

        public void add( Object value ) throws IOException {
            doAdd( value );
            nrow_++;
        }

        public long getRowCount() {
            return nrow_;
        }

        public void endAdd() throws IOException {
            out_.close();
            bufs_ = store_.toByteBuffers();
        }

        public CachedReader createReader() {
            ByteStoreAccess access = createCopyAccess( bufs_ );
            return createReader( access );
        }
    }

    /**
     * Partial ColumnStorage implementation for storing a column
     * in a single ByteStore.
     */
    private static abstract class SingleStorage extends ColumnStorage {

        /**
         * Creates a column based on a single ByteStore.
         *
         * @param  byteStore  byte storage
         * @return   typed storage object
         */
        abstract CachedColumn createSingleColumn( ByteStore byteStore );

        /** 
         * Creates a column reader based on a single ByteStoreAccess.
         *
         * @param  access byte storage accessor
         * @return  typed data accessor
         */
        abstract CachedReader createSingleReader( ByteStoreAccess access );

        public CachedColumn createColumn( Supplier<ByteStore> bsSupplier ) {
            return createSingleColumn( bsSupplier.get() );
        }

        public File[] getFileNames( File baseFile ) {
            return new File[] { new File( baseFile + ".dat" ) };
        }

        public CachedColumn createDiskColumn( File[] files )
                throws IOException {
            return createSingleColumn( new MoveFileByteStore( files[ 0 ] ) );
        }

        public CachedReader createDiskReader( File[] files )
                throws IOException {
            return createSingleReader( createAccess( files[ 0 ] ) );
        }

    }

    /**
     * ColumnStorage implementation for storing objects with a fixed-length
     * byte representation.
     */
    private static class FixedStorage extends SingleStorage {
        private final Supplier<Codec> codecSupplier_;
        private final int itemSize_;

        /**
         * Constructor.
         *
         * @param  codecSupplier  supplier of type-appropriate Codecs
         */
        FixedStorage( Supplier<Codec> codecSupplier ) {
            codecSupplier_ = codecSupplier;
            itemSize_ = codecSupplier.get().getItemSize();
        }

        public long getDiskRowCount( File[] files ) {
            long leng = files[ 0 ].length();
            return leng > 0 ? leng / itemSize_ : -1;
        }

        CachedColumn createSingleColumn( ByteStore byteStore ) {
            final Codec encoder = codecSupplier_.get();
            return new StreamColumn( byteStore ) {
                public void doAdd( Object value ) throws IOException {
                    encoder.encode( value, out_ );
                }
                CachedReader createReader( ByteStoreAccess access ) {
                    return createSingleReader( access );
                }
            };
        }

        CachedReader createSingleReader( final ByteStoreAccess access ) {
            final Codec decoder = codecSupplier_.get();
            final int nbyte = decoder.getItemSize();
            return new ErrorLogReader() {
                int intValue( long ix ) throws IOException {
                    access.seek( ix * nbyte );
                    return decoder.decodeInt( access );
                }
                long longValue( long ix ) throws IOException {
                    access.seek( ix * nbyte );
                    return decoder.decodeLong( access );
                }
                double doubleValue( long ix ) throws IOException {
                    access.seek( ix * nbyte );
                    return decoder.decodeDouble( access );
                }
                boolean booleanValue( long ix ) throws IOException {
                    access.seek( ix * nbyte );
                    return decoder.decodeBoolean( access );
                }
                Object objectValue( long ix ) throws IOException {
                    access.seek( ix * nbyte );
                    return decoder.decodeObject( access );
                }
            };
        }
    }

    /**
     * ColumnStorage implementation for storing boolean values.
     * Packs 8 boolean values into each byte.
     */
    private static class BooleanStorage extends SingleStorage {

        public long getDiskRowCount( File[] files ) {
            long leng = files[ 0 ].length();
            return leng > 0 ? leng * 8 + 7 : -1;
        }

        CachedColumn createSingleColumn( ByteStore byteStore ) {
            return new StreamColumn( byteStore ) {
                int iflags_;
                int ipos_;
                void doAdd( Object value ) throws IOException {
                    if ( Boolean.TRUE.equals( value ) ) {
                        iflags_ |= 1 << ipos_;
                    }
                    if ( ++ipos_ == 8 ) {
                        out_.write( iflags_ );
                        ipos_ = 0;
                        iflags_ = 0;
                    }
                }
                public void endAdd() throws IOException {
                    if ( ipos_ > 0 ) {
                        out_.write( iflags_ );
                    }
                    super.endAdd();
                }
                public CachedReader createReader( ByteStoreAccess access ) {
                    return createSingleReader( access );
                }
            };
        }

        CachedReader createSingleReader( final ByteStoreAccess access ) {
            return new ErrorLogReader() {
                long ioffLast_ = -1;
                int bytLast_;
                boolean booleanValue( long ix ) throws IOException {
                    long ioff = ix / 8;
                    final int byt;
                    if ( ioff != ioffLast_ ) {
                        ioffLast_ = ioff;
                        access.seek( ioff );
                        bytLast_ = access.readByte();
                    }
                    return ( bytLast_ & ( 1 << ( (int) ix % 8 ) ) ) != 0;
                }
                int intValue( long ix ) throws IOException {
                    return booleanValue( ix ) ? 1 : 0;
                }
                long longValue( long ix ) throws IOException {
                    return booleanValue( ix ) ? 1 : 0;
                }
                double doubleValue( long ix ) throws IOException {
                    return booleanValue( ix ) ? 1.0 : 0.0;
                }
                Object objectValue( long ix ) throws IOException {
                    return Boolean.valueOf( booleanValue( ix ) );
                }
            };
        }
    }

    /**
     * ColumnStorage implementation for storing variable-length objects.
     * The item count is prepended to each run of data bytes, and a
     * separate index buffer is maintained to support random access.
     * That is arguably a bit wasteful; it would be possible to infer the
     * item count from the index file.
     */
    private static class IndexedStorage extends ColumnStorage {
        final Codec arrayCodec_;

        /**
         * Constructor.
         *
         * @param  arrayCodec   thread-safe codec for storing array values
         */
        IndexedStorage( Codec arrayCodec ) {
            arrayCodec_ = arrayCodec;
        }

        public CachedColumn createColumn( Supplier<ByteStore> bsSupplier ) {
            return createIndexedColumn( bsSupplier.get(), bsSupplier.get() );
        }

        public File[] getFileNames( File baseFile ) {
            return new File[] {
                new File( baseFile + ".dat" ),
                new File( baseFile + ".idx" ),
            };
        }

        public CachedColumn createDiskColumn( File[] files )
                throws IOException {
            return createIndexedColumn( new MoveFileByteStore( files[ 0 ] ),
                                        new MoveFileByteStore( files[ 1 ] ) );
        }

        public CachedReader createDiskReader( File[] files )
                throws IOException {
            return createIndexedReader( createAccess( files[ 0 ] ),
                                        createAccess( files[ 1 ] ) );
        }

        public long getDiskRowCount( File[] files ) {
            long leng = files[ 1 ].length();
            return leng > 0 ? leng / 8 : -1;
        }

        /**
         * Creates a column based on two ByteStores.
         *
         * @param  dataStore  object for storage of data bytes
         * @param  indexStore  object for storage of index bytes
         */
        CachedColumn createIndexedColumn( final ByteStore dataStore,
                                          final ByteStore indexStore ) {
            final DataBufferedOutputStream dataOut =
                new DataBufferedOutputStream( dataStore.getOutputStream() );
            final DataBufferedOutputStream indexOut =
                new DataBufferedOutputStream( indexStore.getOutputStream() );
            return new CachedColumn() {
                long index_;
                long nrow_;
                ByteBuffer[] dataBufs_;
                ByteBuffer[] indexBufs_;
                public void add( Object value ) throws IOException {
                    indexOut.writeLong( index_ );
                    index_ += arrayCodec_.encode( value, dataOut );
                    nrow_++;
                }
                public long getRowCount() {
                    return nrow_;
                }
                public void endAdd() throws IOException {
                    indexOut.close();
                    dataOut.close();
                    dataBufs_ = dataStore.toByteBuffers();
                    indexBufs_ = indexStore.toByteBuffers();
                }
                public CachedReader createReader() {
                    return createIndexedReader( createCopyAccess( dataBufs_ ),
                                                createCopyAccess( indexBufs_ ));
                }
            };
        }

        /**
         * Creates a column reader based on two ByteStoreAccesses.
         *
         * @param  dataAccess  reads data bytes
         * @param  indexAccess  reads index bytes
         * @return  typed data accessor
         */
        CachedReader createIndexedReader( final ByteStoreAccess dataAccess,
                                          final ByteStoreAccess indexAccess ) {
            return new ErrorLogReader() {
                long nextIx_;
                Object objectValue( long ix ) throws IOException {

                    /* The assumption is that the data values are stored
                     * contiguously, so if we're reading the next item
                     * in the list, no explicit seek is required. */
                    if ( ix != nextIx_ ) {
                        indexAccess.seek( ix * 8 );
                        long ioff = indexAccess.readLong();
                        dataAccess.seek( ioff );
                    }
                    nextIx_ = ix + 1;
                    return arrayCodec_.decodeObject( dataAccess );
                }
                int intValue( long ix ) throws IOException {
                    objectValue( ix );
                    return 0;
                }
                long longValue( long ix ) throws IOException {
                    objectValue( ix );
                    return 0;
                }
                double doubleValue( long ix ) throws IOException {
                    objectValue( ix );
                    return Double.NaN;
                }
                boolean booleanValue( long ix ) throws IOException {
                    objectValue( ix );
                    return false;
                }
            };
        }
    }
}
