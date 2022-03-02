package uk.ac.starlink.fits;

import java.io.Closeable;
import java.io.DataInput;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.logging.Logger;
import uk.ac.starlink.table.ByteStore;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.util.Compression;
import uk.ac.starlink.util.DataBufferedInputStream;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;
import uk.ac.starlink.util.IOUtils;
import uk.ac.starlink.util.Loader;

/**
 * Represents a sequence of bytes, and can create BasicInput objects
 * to read it.
 *
 * @author   Mark Taylor
 * @since    2 Dec 2014
 */
public abstract class InputFactory implements Closeable {

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.fits" );

    /**
     * Indicates whether the BasicInput objects created by this factory
     * will support random access.
     *
     * @return   true iff BasicInput.isRandom() will return true for
     *           created objects
     */
    public abstract boolean isRandom();

    /**
     * Returns a BasicInput instance to read this object's byte stream.
     *
     * @param  isSeq  if true, the returned object is expected to be used
     *                for sequential access only; this value is a hint
     *                which may or may not be used by the implementation
     * @return  new reader; if isSeq is false and isRandom returns true,
     *          it will be capable of random access
     */
    public abstract BasicInput createInput( boolean isSeq ) throws IOException;

    /**
     * Constructs an instance of this class to read a given data source.
     *
     * @param   datsrc  data source
     * @param   offset   offset into file of stream start
     * @param   leng  number of bytes in stream
     * @return  new instance
     */
    public static InputFactory createFactory( DataSource datsrc,
                                              long offset, long leng )
            throws IOException {
        boolean isFile = datsrc instanceof FileDataSource;
        if ( isFile && datsrc.getCompression() == Compression.NONE ) {
            File uncompressedFile = ((FileDataSource) datsrc).getFile();
            return createFileFactory( uncompressedFile, offset, leng );
        }
        else {
            if ( isFile ) {
                logger_.warning( "Can't map compressed file " + datsrc.getName()
                               + " - uncompressing may improve performance" );
            }
            else {
                logger_.info( "Will read stream (not random-access): "
                            + datsrc );
            }
            return createSequentialFactory( datsrc, offset, leng );
        }
    }

    /**
     * Constructs an instance of this class to read a given data source
     * viewed as a stream, not a file.
     *
     * @param   datsrc  data source
     * @param   offset   offset into file of stream start
     * @param   leng  number of bytes in stream
     * @return  new instance
     */
    public static InputFactory createSequentialFactory( final DataSource datsrc,
                                                        final long offset,
                                                        final long leng ) {
        return new AbstractInputFactory( false ) {
            public BasicInput createInput( boolean isSeq )
                    throws IOException {
                InputStream baseIn = datsrc.getInputStream();
                IOUtils.skip( baseIn, offset );
                DataInput dataIn = new DataBufferedInputStream( baseIn );
                return createSequentialInput( dataIn );
            }
            public void close() {
                datsrc.close();
            }
        };
    }

    /**
     * Constructs an instance of this class to read a given uncompressed file.
     * It must be uncompressed for the random access to be able to locate
     * the right part to read.
     *
     * @param  uncompressedFile  unencoded file
     * @param   offset   offset into file of stream start
     * @param   leng  number of bytes in stream
     * @return  new instance
     */
    public static InputFactory createFileFactory( File uncompressedFile,
                                                  final long offset,
                                                  final long leng )
           throws IOException {
        final File file = uncompressedFile;
        final String logName = file.getName();
        if ( leng <= BlockMappedInput.DEFAULT_BLOCKSIZE * 2 ) {
            logger_.info( "Will map as single block: " + logName );
            final int ileng = (int) leng;
            RandomAccessFile raf = new RandomAccessFile( file, "r" );
            final FileChannel chan = raf.getChannel();
            return new AbstractInputFactory( true ) {
                public BasicInput createInput( boolean isSeq )
                        throws IOException {
                    return new SimpleMappedInput( chan, offset, ileng,
                                                  logName );
                }
                public void close() throws IOException {
                    chan.close();
                }
            };
        }
        else if ( Loader.is64Bit() ) {
            logger_.info( "Will map as multiple blocks: " + file );
            RandomAccessFile raf = new RandomAccessFile( file, "r" );
            final FileChannel chan = raf.getChannel();
            return new AbstractInputFactory( true ) {
                public BasicInput createInput( boolean isSeq )
                        throws IOException {
                    return BlockMappedInput
                          .createInput( chan, offset, leng, logName, ! isSeq );
                }
                public void close() throws IOException {
                    chan.close();
                }
            };
        }
        else {
            logger_.info( "Will read as BufferedRandomInput: " + file
                        + " (avoid too much mapping on 32-bit OS" );
            return new AbstractInputFactory( true ) {
                public BasicInput createInput( boolean isSeq )
                        throws IOException {
                    RandomAccessFile raf = new RandomAccessFile( file, "r" );
                    return new BufferedRandomInput( raf, offset );
                }
                public void close() {
                }
            };
        }
    }

    /**
     * Returns an InputFactory for which random access is guaranteed.
     * If necessary, the bytes from the data source will be cached
     * using the supplied storage policy.
     *
     * @param   datsrc  data source
     * @param   offset   offset into file of stream start
     * @param   leng  number of bytes in stream
     * @param   policy   storage policy for caching if required,
     *                   or null for default
     * @return  new instance
     */
    public static InputFactory createRandomFactory( DataSource datsrc,
                                                    long offset, long leng,
                                                    StoragePolicy policy )
            throws IOException {
        boolean isFile = datsrc instanceof FileDataSource;
        if ( isFile && datsrc.getCompression() == Compression.NONE ) {
            File uncompressedFile = ((FileDataSource) datsrc).getFile();
            return createFileFactory( uncompressedFile, offset, leng );
        }
        else {
            logger_.warning( "Caching non-random FITS data" );
            ByteStore byteStore =
                ( policy == null ? StoragePolicy.getDefaultPolicy() : policy )
               .makeByteStore();
            try (
                InputStream in = datsrc.getInputStream();
                OutputStream out = byteStore.getOutputStream();
            ) {
                IOUtils.skip( in, offset );
                byte[] buf = new byte[ 1024 * 64 ];
                while ( leng > 0 ) {
                    int nb = in.read( buf, 0,
                                      (int) Math.min( leng, buf.length ) );
                    if ( nb < 0 ) {
                        throw new EOFException( "FITS file too short" );
                    }
                    out.write( buf, 0, nb );
                    leng -= nb;
                }
            }
            return createByteStoreFactory( byteStore );
        }
    }

    /**
     * Returns an input factory based on a populated bytestore.
     *
     * @param   byteStore  cache containing byte content
     * @return   new input factory
     */
    public static InputFactory
            createByteStoreFactory( final ByteStore byteStore )
            throws IOException {
        final ByteBuffer[] bbufs = byteStore.toByteBuffers();
        final int nbuf = bbufs.length;
        if ( nbuf == 1 ) {
            final ByteBuffer bbuf = bbufs[ 0 ];
            return new AbstractInputFactory( true ) {
                public BasicInput createInput( boolean isSeq ) {
                    return new ByteBufferInput( bbuf.duplicate() );
                }
                public void close() throws IOException {
                    byteStore.close();
                }
            };
        }
        else {
            return new AbstractInputFactory( true ) {
                public BasicInput createInput( boolean isSeq ) {
                    ByteBuffer[] bbufs1 = new ByteBuffer[ nbuf ];
                    for ( int ib = 0; ib < nbuf; ib++ ) {
                        bbufs1[ ib ] = bbufs[ ib ].duplicate();
                    }
                    return new MultiByteBufferInput( bbufs1 );
                }
                public void close() throws IOException {
                    byteStore.close();
                }
            };
        }
    }

    /**
     * Returns a non-random-access BasicInput based on a supplied input stream.
     * The result is just an adapter wrapping the supplied DataInput.
     *
     * @param  in  input stream
     * @return  non-random BasicInput
     */
    public static BasicInput createSequentialInput( final DataInput in ) {
        return new BasicInput() {
            public byte readByte() throws IOException {
                return in.readByte();
            }
            public short readShort() throws IOException {
                return in.readShort();
            }
            public int readInt() throws IOException {
                return in.readInt();
            }
            public long readLong() throws IOException {
                return in.readLong();
            }
            public float readFloat() throws IOException {
                return in.readFloat();
            }
            public double readDouble() throws IOException {
                return in.readDouble();
            }
            public void readBytes( byte[] bbuf ) throws IOException {
                in.readFully( bbuf );
            }
            public void skip( long nbyte ) throws IOException {
                IOUtils.skipBytes( in, nbyte );
            }
            public boolean isRandom() {
                return false;
            }
            public void seek( long offset ) {
                throw new UnsupportedOperationException();
            }
            public long getOffset() {
                throw new UnsupportedOperationException();
            }
            public void close() {
            }
        };
    }

    /** 
     * Utility partial implementation.
     */
    private static abstract class AbstractInputFactory extends InputFactory {
        private final boolean isRandom_;

        /**
         * Constructor.
         *
         * @param  isRandom  true iff random access is supported
         */
        AbstractInputFactory( boolean isRandom ) {
            isRandom_ = isRandom;
        }

        public boolean isRandom() {
            return isRandom_;
        }
    }
}
