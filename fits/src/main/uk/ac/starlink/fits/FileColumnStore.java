package uk.ac.starlink.fits;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.util.DataBufferedInputStream;
import uk.ac.starlink.util.DataBufferedOutputStream;

/**
 * Column store implementation which stores data in (temporary) disk files.
 * This class is an abstract superclass; it provides a factory method for
 * obtaining concrete instances for various data types.
 *
 * @author   Mark Taylor
 * @since    26 Jun 2006
 */
abstract class FileColumnStore implements ColumnStore {

    private final ValueInfo info_;
    private final File file_;
    private final DataOutput out_;
    private final char formatChar_;
    private final int typeBytes_;
    private final boolean dumpCopy_;
    private long nrow_;
    private int[] itemShape_;
    private byte[] copyBuf_;

    private final static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.fits" );

    /**
     * Constructor with optional dumpCopy flag.
     *
     * @param   info   column metadata
     * @param   formatChar  FITS format character label
     * @param   typeBytes  number of bytes per primitive array element;
     *          this value is not necessarily the size of elements in the
     *          column since they may be arrays of items of this size
     * @param   dumpCopy  true only if the intermediate file is written in
     *          such a way that it can be dumped directly to the output 
     *          FITS file
     */
    protected FileColumnStore( ValueInfo info, char formatChar,
                               int typeBytes, boolean dumpCopy )
            throws IOException {
        info_ = info;
        formatChar_ = formatChar;
        typeBytes_ = typeBytes;
        dumpCopy_ = dumpCopy;
        setItemShape( new int[] { 1 } );
        file_ = File.createTempFile( "col-"
                                   + info.getName().replaceAll( "\\W+", "" ),
                                     ".bin" );
        file_.deleteOnExit();
        // Note: a DataOutputStream on a BufferedOutputStream is slow.
        // out_ = new DataOutputStream(
        //            new BufferedOutputStream(
        //                new FileOutputStream( file_ ) ) );
        out_ = new DataBufferedOutputStream( new FileOutputStream( file_ ) );
    }

    /**
     * Constructor which sets dumpCopy false.
     *
     * @param   info   column metadata
     * @param   formatChar  FITS format character label
     * @param   typeBytes  number of bytes per primitive array element;
     *          this value is not necessarily the size of elements in the
     *          column since they may be arrays of items of this size
     */
    protected FileColumnStore( ValueInfo info, char formatChar, int typeBytes )
            throws IOException {
        this( info, formatChar, typeBytes, false );
    }

    public void storeValue( Object value ) throws IOException {
        storeValue( value, out_ );
        nrow_++;
    }

    public void streamData( DataOutput out ) throws IOException {
        if ( dumpCopy_ ) {
            FileInputStream in = new FileInputStream( file_ );
            int bufsiz = 64 * 1024;
            byte[] buf = new byte[ bufsiz ];
            long start = System.currentTimeMillis();
            try {
                for ( long nbyte = getDataLength(); nbyte > 0; ) {
                    int count =
                        in.read( buf, 0, 
                                 (int) Math.min( nbyte, (long) bufsiz ) );
                    if ( count < 0 ) {
                        throw new EOFException();
                    }
                    out.write( buf, 0, count );
                    nbyte -= count;
                }
            }
            finally {
                in.close();
            }
            logger_.config( "Dump data rate: "
                          + ( 1e-3f * getDataLength() 
                                    / ( System.currentTimeMillis() - start) )
                          + " Mbyte/sec" );
        }
        else {
            // Note: a DataInputStream on a BufferedInputStream is slow.
            // DataInput in = new DataInputStream(
            //                    new BufferedInputStream(
            //                        new FileInputStream( file_ ) ) );
            DataInput in =
                new DataBufferedInputStream( new FileInputStream( file_ ) );
            try {
                for ( long irow = 0; irow < nrow_; irow++ ) {
                    copyValue( in, out );
                }
            }
            finally {
                if ( in instanceof InputStream ) {
                    ((InputStream) in).close();
                }
            }
        }
    }

    public void endStores() throws IOException {
        if ( out_ instanceof OutputStream ) {
            ((OutputStream) out_).close();
        }
    }

    public long getDataLength() {
        return multiply( itemShape_ ) * typeBytes_ * nrow_;
    }

    public List<CardImage> getHeaderInfo( BintableColumnHeader colhead,
                                          int jcol ) {
        String forcol = " for column " + jcol;
        CardFactory cfact = colhead.getCardFactory();
        List<CardImage> cards = new ArrayList<>();

        /* Column name. */
        String name = info_.getName();
        if ( name != null && name.trim().length() > 0 ) {
            cards.add( cfact.createStringCard( colhead.getKeyName( "TTYPE" ),
                                               name, "label" + forcol ) );
        }

        /* Column format. */
        long nItem = multiply( itemShape_ ) * nrow_;
        cards.add( cfact.createStringCard( colhead.getKeyName( "TFORM" ),
                                           nItem + "" + formatChar_,
                                           "format" + forcol ) );

        /* Column dimensions. */
        StringBuffer dimbuf = new StringBuffer( "(" );
        for ( int i = 0; i < itemShape_.length; i++ ) {
            dimbuf.append( itemShape_[ i ] )
                  .append( ',' );
        }
        dimbuf.append( nrow_ )
              .append( ')' );
        cards.add( cfact.createStringCard( colhead.getKeyName( "TDIM" ),
                                           dimbuf.toString(),
                                           "dimensions" + forcol ) );

        /* Column units. */
        String unit = info_.getUnitString();
        if ( unit != null && unit.trim().length() > 0 ) {
            cards.add( cfact.createStringCard( colhead.getKeyName( "TUNIT" ),
                                               unit, "units" + forcol ) );
        }

        /* Column description. */
        String comm = info_.getDescription();
        if ( comm != null && comm.trim().length() > 0 ) {
            cards.add( cfact.createStringCard( colhead.getKeyName( "TCOMM" ),
                                               comm, null ) );
        }

        /* UCD. */
        String ucd = info_.getUCD();
        if ( ucd != null && ucd.trim().length() > 0 ) {
            cards.add( cfact.createStringCard( colhead.getKeyName( "TUCD" ),
                                               ucd, null ) );
        }

        /* Utype. */
        String utype = info_.getUtype();
        if ( utype != null && utype.trim().length() > 0 ) {
            cards.add( cfact.createStringCard( colhead.getKeyName( "TUTYP" ),
                                               utype, null ) );
        }
        return cards;
    }

    public void dispose() throws IOException {
        if ( file_.exists() ) {
            file_.delete();
        }
    }

    protected void finalize() throws Throwable {
        try {
            dispose();
        }
        finally {
            super.finalize();
        }
    }

    protected abstract void storeValue( Object value, DataOutput out )
            throws IOException;

    /**
     * Transfers an element of the column from an input stream to an output
     * stream.
     *
     * @param   in  source stream
     * @param   out destination stream
     */
    protected void copyValue( DataInput in, DataOutput out )
            throws IOException {
        in.readFully( copyBuf_ );
        out.write( copyBuf_ );
    }

    /**
     * Sets the shape of elements of this column.  By default this is
     * a scalar, but call this to modify it to a vector or multidimensional
     * array.
     *
     * @param   shape   dimensions array
     */
    protected void setItemShape( int[] shape ) {
        itemShape_ = shape;
        copyBuf_ = new byte[ multiply( itemShape_ ) * typeBytes_ ];
    }

    /**
     * Returns the metadata for this column store.
     *
     * @return  column metadata object
     */
    ValueInfo getValueInfo() {
        return info_;
    }

    /**
     * Utility method which multiplies the elements of an array.
     *
     * @param   dims   integer array
     * @return  product of elements of <code>dims</code>
     */
    private static int multiply( int[] dims ) {
        int product = 1;
        for ( int i = 0; i < dims.length; i++ ) {
            product *= dims[ i ];
        }
        return product;
    }

    /**
     * Factory method which provides instances of this class for given
     * column descriptions.
     *
     * @param   info  column description
     * @param   config   output configuration details
     * @return  suitable column store for column described by <code>info</code>
     */
    public static ColumnStore
            createColumnStore( ValueInfo info,
                               final FitsTableSerializerConfig config )
            throws IOException {
        Class<?> clazz = info.getContentClass();
        final byte padByte = config.getPadCharacter();

        if ( clazz == Boolean.class ) {
            return new FileColumnStore( info, 'L', 1, true ) {
                protected void storeValue( Object value, DataOutput out )
                        throws IOException {
                    byte b;
                    if ( Boolean.TRUE.equals( value ) ) {
                        b = (byte) 'T';
                    }
                    else if ( Boolean.FALSE.equals( value ) ) {
                        b = (byte) 'F';
                    }
                    else {
                        b = (byte) 0;
                    }
                    out.writeByte( b );
                }
            };
        }

        else if ( clazz == Byte.class ) {
            if ( config.allowSignedByte() ) {
                return new IntegerColumnStore( info, IntegerStorage
                                                    .createByteStorage() ) {
                    public List<CardImage>
                            getHeaderInfo( BintableColumnHeader colhead,
                                           int jcol ) {
                        List<CardImage> cards =
                            new ArrayList<>( super.getHeaderInfo( colhead,
                                                                  jcol ) );
                        cards.add( colhead.getCardFactory()
                                  .createRealCard( colhead.getKeyName( "TZERO"),
                                                   -128.0, "unsigned offset" ));
                        return cards;
                    }
                };
            }
            else {
                return new IntegerColumnStore( info, IntegerStorage
                                                    .createShortStorage() );
            }
        }

        else if ( clazz == Short.class ) {
            return new IntegerColumnStore( info, IntegerStorage
                                                .createShortStorage() );
        }

        else if ( clazz == Integer.class ) {
            return new IntegerColumnStore( info, IntegerStorage
                                                .createIntStorage() );
        }

        else if ( clazz == Long.class ) {
            return new IntegerColumnStore( info, IntegerStorage
                                                .createLongStorage() );
        }

        else if ( clazz == Float.class ) {
            return new FileColumnStore( info, 'E', 4, true ) {
                protected void storeValue( Object value, DataOutput out )
                        throws IOException {
                    out.writeFloat( value instanceof Number
                                        ? ((Number) value).floatValue()
                                        : Float.NaN );
                }
            };
        }

        else if ( clazz == Double.class ) {
            return new FileColumnStore( info, 'D', 8, true ) {
                protected void storeValue( Object value, DataOutput out )
                        throws IOException {
                    out.writeDouble( value instanceof Number
                                         ? ((Number) value).doubleValue()
                                         : Double.NaN );
                }
            };
        }

        else if ( clazz == Character.class ) {
            return new FileColumnStore( info, 'A', 1, true ) {
                protected void storeValue( Object value, DataOutput out )
                        throws IOException {
                    out.writeByte( value instanceof Character
                                       ? ((Character) value).charValue()
                                       : (char) padByte );
                }
            };
        }

        else if ( clazz == String.class ) {
            return new FileColumnStore( info, 'A', 1 ) {
                int maxleng_ = config.allowZeroLengthString() ? 0 : 1;
                byte[] copyBuffer_;
                protected void storeValue( Object value, DataOutput out )
                        throws IOException {
                    String sval = (String) value;
                    int leng = sval == null ? 0 : sval.length();
                    maxleng_ = Math.max( maxleng_, leng );
                    out.writeInt( leng );
                    for ( int i = 0; i < leng; i++ ) {
                        out.writeByte( (byte) sval.charAt( i ) );
                    }
                }
                public void endStores() throws IOException {
                    super.endStores();
                    setItemShape( new int[] { maxleng_ } );
                    copyBuffer_ = new byte[ maxleng_ ];
                }
                protected void copyValue( DataInput in, DataOutput out )
                        throws IOException {
                    int leng = in.readInt();
                    if ( leng < 0 || leng > maxleng_ ) {
                        throw new IOException( "Corrupted temporary file" );
                    }
                    in.readFully( copyBuffer_, 0, leng );
                    Arrays.fill( copyBuffer_, leng, maxleng_, padByte );
                    out.write( copyBuffer_ );
                }
            };
        }

        else if ( clazz == byte[].class ) {
            int[] dims = info.getShape();
            ArrayStorage handler = ArrayStorage.BYTE;
            return ( dims != null &&
                     dims.length > 0 &&
                     dims[ dims.length - 1 ] > 0 )
                 ? new FixedArrayColumnStore( info, handler ) {
                       public List<CardImage>
                               getHeaderInfo( BintableColumnHeader colhead,
                                              int jcol ) {
                           List<CardImage> cards =
                               new ArrayList<>( super.getHeaderInfo( colhead,
                                                                     jcol ) );
                           cards.add( colhead.getCardFactory()
                                     .createRealCard( colhead
                                                     .getKeyName( "TZERO" ),
                                                      -128.0,
                                                      "unsigned offset" ) );
                           return cards;
                       }
                   }
                 : new VariableArrayColumnStore( info, handler ) {
                       public List<CardImage>
                               getHeaderInfo( BintableColumnHeader colhead,
                                              int jcol ) {
                           List<CardImage> cards =
                               new ArrayList<>( super.getHeaderInfo( colhead,
                                                                     jcol ) );
                           cards.add( colhead.getCardFactory()
                                     .createRealCard( colhead
                                                     .getKeyName( "TZERO" ),
                                                      -128.0,
                                                      "unsigned offset" ) );
                           return cards;
                       }
                   };
        }

        else if ( clazz == boolean[].class ) {
            return createArrayColumnStore( info, ArrayStorage.BOOLEAN );
        }

        else if ( clazz == short[].class ) {
            return createArrayColumnStore( info, ArrayStorage.SHORT );
        }

        else if ( clazz == int[].class ) {
            return createArrayColumnStore( info, ArrayStorage.INT );
        }

        else if ( clazz == long[].class ) {
            return createArrayColumnStore( info, ArrayStorage.LONG );
        }

        else if ( clazz == float[].class ) {
            return createArrayColumnStore( info, ArrayStorage.FLOAT );
        }
 
        else if ( clazz == double[].class ) {
            return createArrayColumnStore( info, ArrayStorage.DOUBLE );
        }

        else if ( clazz == String[].class ) {
            return new FileColumnStore( info, 'A', 1 ) {
                int maxChars_ = 1;
                int maxStrings_;
                byte[] blankString_; 

                protected void storeValue( Object value, DataOutput out )
                        throws IOException {
                    if ( value instanceof String[] ) {
                        String[] strings = (String[]) value;
                        int nstring = strings.length;
                        out.writeInt( nstring );
                        maxStrings_ = Math.max( maxStrings_, nstring );
                        for ( int is = 0; is < nstring; is++ ) {
                            String sval = strings[ is ];
                            int nchar = sval == null ? 0 : sval.length();
                            out.writeInt( nchar );
                            maxChars_ = Math.max( maxChars_, nchar );
                            for ( int ic = 0; ic < nchar; ic++ ) {
                                out.writeByte( (byte) sval.charAt( ic ) );
                            }
                        }
                    }
                }

                public void endStores() throws IOException {
                    super.endStores();
                    setItemShape( new int[] { maxChars_, maxStrings_ } );
                    blankString_ = new byte[ maxChars_ ];
                    Arrays.fill( blankString_, padByte );
                }

                protected void copyValue( DataInput in, DataOutput out )
                        throws IOException {
                    int nstring = in.readInt();
                    if ( nstring < 0 || nstring > maxStrings_ ) {
                        throw new IOException( "Corrupted temp file for " +
                                               getValueInfo() );
                    }
                    for ( int is = 0; is < nstring; is++ ) {
                        int nchar = in.readInt();
                        if ( nchar < 0 || nchar > maxChars_ ) {
                            throw new IOException( "Corrupted temp file for " +
                                                   getValueInfo() );
                        }
                        for ( int ic = 0; ic < nchar; ic++ ) {
                            out.writeByte( in.readByte() );
                        }
                        for ( int ic = nchar; ic < maxChars_; ic++ ) {
                            out.writeByte( padByte );
                        }
                    }
                    for ( int is = nstring; is < maxStrings_; is++ ) {
                        out.write( blankString_ );
                    }
                }
            };
        }

        else {
            return null;
        }
    }

    /**
     * Constructs a column store instance for an array type.
     *
     * @param   info  column description
     * @param   handler  ArrayStorage instance compatible with <code>info</code>
     * @return   column store suitable for <code>info</code> based on
     *           <code>handler</code>
     */
    private static FileColumnStore createArrayColumnStore(
            ValueInfo info, ArrayStorage handler ) throws IOException {
        int[] dims = info.getShape();
        return dims != null && dims.length > 0 && dims[ dims.length - 1 ] > 0
             ? (FileColumnStore) new FixedArrayColumnStore( info, handler )
             : (FileColumnStore) new VariableArrayColumnStore( info, handler );
    }

    /**
     * ColumnStore concrete subclass which can store arrays of fixed shape.
     * Type-specific data handling is delegated to a component
     * {@link ArrayStorage} object.
     */
    private static class FixedArrayColumnStore extends FileColumnStore {

        private final ArrayStorage handler_;
        private final Object buffer_;
        private final int size_;

        /**
         * Constructor.
         *
         * @param  info      column description
         * @param  handler   object which knows how to store arrays of
         *         specific data types
         */
        FixedArrayColumnStore( ValueInfo info, ArrayStorage handler )
                throws IOException {
            super( info, handler.getFormatChar(), handler.getTypeBytes(),
                   true );
            handler_ = handler;
            size_ = multiply( info.getShape() );
            buffer_ = Array.newInstance( handler.getComponentClass(), size_ );
            setItemShape( info.getShape() );
        } 

        protected void storeValue( Object value, DataOutput out ) 
                throws IOException {
            if ( value == null ||
                 value.getClass() != buffer_.getClass() ||
                 Array.getLength( value ) != size_ ) {
                value = buffer_;
            }
            handler_.writeArray( value, out );
        }
    }

    /**
     * ColumnStore concrete subclass which can store arrays of variable shape.
     * Type-specific data andling is delegated to a component
     * {@link ArrayStorage} object.
     * Each record in the temporary file is preceded by an integer which 
     * indicates how many blocks it contains.
     */
    private static class VariableArrayColumnStore extends FileColumnStore {

        private final ArrayStorage handler_;
        private final Object buffer_;
        private final byte[] byteBuffer_;
        private final byte[] blankBuffer_;
        private final int blockSize_;
        private final int[] blockShape_;
        private int maxBlocks_;

        /**
         * Constructor.
         *
         * @param   info  column description
         * @param  handler   object which knows how to store arrays of
         *         specific data types
         */
        VariableArrayColumnStore( ValueInfo info, ArrayStorage handler )
                throws IOException {
            super( info, handler.getFormatChar(), handler.getTypeBytes() );
            handler_ = handler;
            int[] dims = info.getShape();
            if ( dims != null && dims.length > 1 ) {
                blockShape_ = new int[ dims.length - 1 ];
                System.arraycopy( dims, 0, blockShape_, 0, dims.length - 1 );
                blockSize_ = multiply( blockShape_ );
            }
            else {
                blockShape_ = new int[ 0 ];
                blockSize_ = 1;
            }
            buffer_ = Array.newInstance( handler.getComponentClass(),
                                         blockSize_ );
            byteBuffer_ = new byte[ blockSize_ * handler.getTypeBytes() ];
            blankBuffer_ = byteBuffer_.clone();
        }

        protected void storeValue( Object value, DataOutput out )
                throws IOException {
            int nblock = value != null && value.getClass() == buffer_.getClass()
                       ? Array.getLength( value ) / blockSize_
                       : 0;
            out.writeInt( nblock );
            maxBlocks_ = Math.max( maxBlocks_, nblock );
            for ( int i = 0; i < nblock; i++ ) {
                System.arraycopy( value, i * blockSize_, buffer_, 0,
                                  blockSize_ );
                handler_.writeArray( buffer_, out );
            }
        }

        public void endStores() throws IOException {
            super.endStores();
            int[] dims = new int[ blockShape_.length + 1 ];
            System.arraycopy( blockShape_, 0, dims, 0, blockShape_.length );
            dims[ blockShape_.length ] = maxBlocks_;
            setItemShape( dims );
        }

        protected void copyValue( DataInput in, DataOutput out )
                throws IOException {
            int nblock = in.readInt();
            if ( nblock < 0 || nblock > maxBlocks_ ) {
                throw new IOException( "Corrupted column store file for " +
                                       getValueInfo() );
            }
            for ( int i = 0; i < nblock; i++ ) {
                in.readFully( byteBuffer_ );
                out.write( byteBuffer_ );
            }
            for ( int i = nblock; i < maxBlocks_; i++ ) {
                out.write( blankBuffer_ );
            }
        }
    }

    /**
     * FileColumnStore concrete subclass which uses an IntegerStorage object
     * to deal with integer data types.  When writing, each value is
     * flagged by an additional byte to indicate nullness.
     */
    private static class IntegerColumnStore extends FileColumnStore {

        private final IntegerStorage handler_;
        private final byte[] copyBuffer_;
        private boolean hasNulls_;
        private byte[] badBuffer_;
        private final byte GOOD = (byte) 1;
        private final byte BAD = (byte) 2;

        /**
         * Constructor.
         *
         * @param   info  column description
         * @param   handler  object which knows how to store particular
         *          integer types
         */
        IntegerColumnStore( ValueInfo info, IntegerStorage handler )
                throws IOException {
            super( info, handler.getFormatChar(), handler.getTypeBytes() );
            handler_ = handler;
            copyBuffer_ = new byte[ handler.getTypeBytes() ];
        }

        protected void storeValue( Object value, DataOutput out )
                throws IOException {
            if ( value instanceof Number ) {
                out.write( GOOD );
                long val = ((Number) value).longValue();
                handler_.writeValue( val, out );
            }
            else {
                hasNulls_ = true;
                out.write( BAD );
            }
        }

        public void endStores() throws IOException {
            super.endStores();
            if ( hasNulls_ ) {
                badBuffer_ = handler_.getBadBytes();
                if ( badBuffer_ == null ) {
                    badBuffer_ = new byte[ handler_.getTypeBytes() ];
                    badBuffer_[ 0 ] = (byte) 0x80;
                    logger_.warning( "Can't find unused null value for column "
                                   + getValueInfo() + " - using 0x80..." );
                }
            }
        }

        protected void copyValue( DataInput in, DataOutput out )
                throws IOException {
            byte[] buf;
            switch ( in.readByte() ) {
                case GOOD:
                    in.readFully( copyBuffer_ );
                    buf = copyBuffer_;
                    break;
                case BAD:
                    buf = badBuffer_;
                    break;
                default:
                    throw new IOException( "Corrupted data" );
            }
            out.write( buf );
        }

        public List<CardImage> getHeaderInfo( BintableColumnHeader colhead,
                                              int jcol ) {
            List<CardImage> cards =
                new ArrayList<>( super.getHeaderInfo( colhead, jcol ) );
            if ( hasNulls_ ) {
                Number bad = handler_.getBadNumber();
                if ( bad != null ) {
                    cards.add( colhead.getCardFactory()
                              .createIntegerCard( colhead.getKeyName( "TNULL" ),
                                                  bad.longValue(),
                                                  "blank value" ) );
                }
            }
            return cards;
        }
    }
}
