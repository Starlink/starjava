package uk.ac.starlink.table.storage;

import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.logging.Logger;
import uk.ac.starlink.table.ValueInfo;

/**
 * Serializes and deserializes objects to/from a data stream.
 * A given instance of this class is only able to de/serialize 
 * an object of one class.  Obtain an instance from the
 * static {@link #getCodec} method, or use one of the static members.
 * The static members are all safe for concurrent use from multiple threads.
 * 
 * <p>This is (supposed to be) considerably more lightweight than 
 * all the {@link java.io.Serializable} business - for one thing
 * there's no way to tell from the stream what kind of item has been
 * serialized, you have to make sure the right Codec instance is on
 * hand.  In general it deals with primitive wrapper objects and
 * arrays of same, but new Codec instances for different classes
 * can be added.
 *
 * @author   Mark Taylor (Starlink)
 * @since    3 Aug 2004
 */
public abstract class Codec {

    private volatile int warnings_;
    private static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.table.storage" );

    /** Codec for 8-bit byte, no null handling. */
    public static final Codec BYTE = new ByteCodec();

    /** Codec for 16-bit integer, no null handling. */
    public static final Codec SHORT = new ShortCodec();

    /** Codec for 32-bit integer, no null handling. */
    public static final Codec INT = new IntCodec();

    /** Codec for 64-bit integer, no null handling. */
    public static final Codec LONG = new LongCodec();

    /** Codec for 32-bit floating point, nulls treated like NaN. */
    public static final Codec FLOAT = new FloatCodec();

    /** Codec for 64-bit floating point, nulls treated like NaN. */
    public static final Codec DOUBLE = new DoubleCodec();

    /** Codec for 16-bit character, no null handling. */
    public static final Codec CHAR = new CharCodec();

    /** Codec for byte-serialized boolean values, null handled. */
    public static final Codec BOOLEAN = new BooleanCodec();

    /** Codec for variable-length array of 8-bit bytes. */
    public static final Codec BYTE_ARRAY =
        new VariableArrayCodec( new ByteCodec1() );

    /** Codec for variable-length array of 16-bit integers. */
    public static final Codec SHORT_ARRAY =
        new VariableArrayCodec( new ShortCodec1() );

    /** Codec for variable-length array of 32-bit integers. */
    public static final Codec INT_ARRAY =
        new VariableArrayCodec( new IntCodec1() );

    /** Codec for variable-length array of 64-bit integers. */
    public static final Codec LONG_ARRAY =
        new VariableArrayCodec( new LongCodec1() );

    /** Codec for variable-length array of 32-bit floats. */
    public static final Codec FLOAT_ARRAY =
        new VariableArrayCodec( new FloatCodec1() );

    /** Codec for variable-length array of 64-bit doubles. */
    public static final Codec DOUBLE_ARRAY =
        new VariableArrayCodec( new DoubleCodec1() );

    /** Codec for variable-length string. */
    public static final Codec STRING = new VariableStringCodec();

    /**
     * Serializes an object to a stream.
     *
     * @param  value  object to serialize
     * @param  out    destination stream, positioned at place to write
     * @return  number of bytes written
     */
    abstract public int encode( Object value, DataOutput out )
            throws IOException;

    /**
     * Deserializes an item from a stream.
     *
     * @param  in  source stream, positioned at start of item
     * @return  deserialized item
     */
    abstract public Object decodeObject( ByteStoreAccess in )
            throws IOException;

    /**
     * Deserialises an item from a stream, and presents it as an integer
     * if possible.  If the stream does not contain integer-like items,
     * the result may not be useful.
     *
     * @param  in  source stream, positioned at start of item
     * @return  best-effort integer equivalent of deserialised item
     */
    abstract public int decodeInt( ByteStoreAccess in )
            throws IOException;

    /**
     * Deserialises an item from a stream, and presents it as a long integer
     * if possible.  If the stream does not contain integer-like items,
     * the result may not be useful.
     *
     * @param  in  source stream, positioned at start of item
     * @return  best-effort long equivalent of deserialised item
     */
    abstract public long decodeLong( ByteStoreAccess in )
            throws IOException;

    /**
     * Deserialises an item from a stream, and presents it as a double
     * if possible.  If the stream does not contain numeric items,
     * the result may not be useful.
     *
     * @param  in  source stream, positioned at start of item
     * @return  best-effort double equivalent of deserialised item
     */
    abstract public double decodeDouble( ByteStoreAccess in )
            throws IOException;

    /**
     * Deserialises an item from a stream, and presents it as a boolean
     * if possible.  If the stream does not contain boolean-like items,
     * the result may not be useful.
     *
     * @param  in  source stream, positioned at start of item
     * @return  best-effort boolean equivalent of deserialised item
     */
    abstract public boolean decodeBoolean( ByteStoreAccess in )
            throws IOException;

    /**
     * Returns the number of bytes a call to <code>encode</code> will write.
     * If this value may vary, -1 is returned.
     *
     * @return  size in bytes of serialized items, or -1
     */
    abstract public int getItemSize();

    /**
     * Returns a codec suitable for serializing/deserializing the contents
     * of a given ValueInfo.  If no codec can be supplied to match
     * <code>info</code>, <code>null</code> is returned.
     *
     * @param  info  object describing the kind of item which is required to
     *         be de/serialized
     * @return  codec for the job
     */
    public static Codec getCodec( ValueInfo info ) {
        Class<?> clazz = info.getContentClass();
        if ( clazz == Byte.class ) {
            return new FlaggedCodec( BYTE );
        }
        else if ( clazz == Short.class ) {
            return new FlaggedCodec( SHORT );
        }
        else if ( clazz == Integer.class ) {
            return new FlaggedCodec( INT );
        }
        else if ( clazz == Long.class ) {
            return new FlaggedCodec( LONG );
        }
        else if ( clazz == Character.class ) {
            return new FlaggedCodec( CHAR );
        }
        else if ( clazz == Float.class ) {
            return FLOAT;
        }
        else if ( clazz == Double.class ) {
            return DOUBLE;
        }
        else if ( clazz == Boolean.class ) {
            return BOOLEAN;
        }
        else if ( clazz == String.class ) {
            int esize = info.getElementSize();
            return esize > 0 ? (Codec) new FixedStringCodec( esize )
                             : (Codec) new VariableStringCodec();
        }
        else if ( clazz == String[].class ) {
            return new StringArrayCodec();
        }
        else if ( clazz.isArray() ) {
            int[] shape = info.getShape();
            int nel = 1;
            boolean isFixed = true;
            for ( int i = 0; i < shape.length; i++ ) {
                nel *= shape[ i ];
                if ( shape[ i ] <= 0 ) {
                    isFixed = false;
                }
            }
            Codec1 codec1 = null;
            if ( clazz == byte[].class ) {
                codec1 = new ByteCodec1();
            }
            else if ( clazz == short[].class ) {
                codec1 = new ShortCodec1();
            }
            else if ( clazz == int[].class ) {
                codec1 = new IntCodec1();
            }
            else if ( clazz == long[].class ) {
                codec1 = new LongCodec1();
            }
            else if ( clazz == char[].class ) {
                codec1 = new CharCodec1();
            }
            else if ( clazz == float[].class ) {
                codec1 = new FloatCodec1();
            }
            else if ( clazz == double[].class ) {
                codec1 = new DoubleCodec1();
            }
            else if ( clazz == boolean[].class ) {
                codec1 = new BooleanCodec1();
            }
            if ( codec1 != null ) {
                return isFixed 
                     ? (Codec) new FlaggedCodec( new FixedArrayCodec( codec1,
                                                                      nel ) )
                     : (Codec) new VariableArrayCodec( codec1 );
            }
        }

        /* No good. */
        logger_.info( "No codec available for class " + clazz.getName() );
        return null;
    }

    /**
     * Logs a warning that unexpected data has been found in the stream
     * during decoding.
     */
    protected void warnCorrupt() {
        if ( ++warnings_ <= 3 ) {
            logger_.warning( "Unexpected stream data - table raw data " +
                            "stream is probably corrupt" );
            if ( warnings_ == 3 ) {
               logger_.warning( "... more" );
            }
        }
    }

    private static class ByteCodec extends Codec {
        public int encode( Object value, DataOutput out ) throws IOException {
            out.writeByte( ((Number) value).byteValue() );
            return 1;
        }
        public Object decodeObject( ByteStoreAccess in ) throws IOException {
            return Byte.valueOf( in.readByte() );
        }
        public int decodeInt( ByteStoreAccess in ) throws IOException {
            return in.readByte();
        }
        public long decodeLong( ByteStoreAccess in ) throws IOException {
            return in.readByte();
        }
        public double decodeDouble( ByteStoreAccess in ) throws IOException {
            return in.readByte();
        }
        public boolean decodeBoolean( ByteStoreAccess in ) throws IOException {
            return in.readByte() != 0;
        }
        public int getItemSize() {
            return 1;
        }
    }

    private static class ShortCodec extends Codec {
        public int encode( Object value, DataOutput out ) throws IOException {
            out.writeShort( ((Number) value).shortValue() );
            return 2;
        }
        public Object decodeObject( ByteStoreAccess in ) throws IOException {
            return Short.valueOf( in.readShort() );
        }
        public int decodeInt( ByteStoreAccess in ) throws IOException {
            return in.readShort();
        }
        public long decodeLong( ByteStoreAccess in ) throws IOException {
            return in.readShort();
        }
        public double decodeDouble( ByteStoreAccess in ) throws IOException {
            return in.readShort();
        }
        public boolean decodeBoolean( ByteStoreAccess in ) throws IOException {
            return in.readShort() != 0;
        }
        public int getItemSize() {
            return 2;
        }
    }

    private static class IntCodec extends Codec {
        public int encode( Object value, DataOutput out ) throws IOException {
            out.writeInt( ((Number) value).intValue() );
            return 4;
        }
        public Object decodeObject( ByteStoreAccess in ) throws IOException {
            return Integer.valueOf( in.readInt() );
        }
        public int decodeInt( ByteStoreAccess in ) throws IOException {
            return in.readInt();
        }
        public long decodeLong( ByteStoreAccess in ) throws IOException {
            return in.readInt();
        }
        public double decodeDouble( ByteStoreAccess in ) throws IOException {
            return in.readInt();
        }
        public boolean decodeBoolean( ByteStoreAccess in ) throws IOException {
            return in.readInt() != 0;
        }
        public int getItemSize() {
            return 4;
        }
    }

    private static class LongCodec extends Codec {
        public int encode( Object value, DataOutput out ) throws IOException {
            out.writeLong( ((Number) value).longValue() );
            return 8;
        }
        public Object decodeObject( ByteStoreAccess in ) throws IOException {
            return Long.valueOf( in.readLong() );
        }
        public int decodeInt( ByteStoreAccess in ) throws IOException {
            return (int) in.readLong();
        }
        public long decodeLong( ByteStoreAccess in ) throws IOException {
            return in.readLong();
        }
        public double decodeDouble( ByteStoreAccess in ) throws IOException {
            return in.readLong();
        }
        public boolean decodeBoolean( ByteStoreAccess in ) throws IOException {
            return in.readLong() != 0;
        }
        public int getItemSize() {
            return 8;
        }
    }

    private static class CharCodec extends Codec {
        public int encode( Object value, DataOutput out ) throws IOException {
            out.writeChar( ((Character) value).charValue() );
            return 2;
        }
        public Object decodeObject( ByteStoreAccess in ) throws IOException {
            return Character.valueOf( in.readChar() );
        }
        public int decodeInt( ByteStoreAccess in ) throws IOException {
            return (int) in.readChar();
        }
        public long decodeLong( ByteStoreAccess in ) throws IOException {
            return (long) in.readChar();
        }
        public double decodeDouble( ByteStoreAccess in ) throws IOException {
            return (double) in.readChar();
        }
        public boolean decodeBoolean( ByteStoreAccess in ) throws IOException {
            return in.readChar() != '\0';
        }
        public int getItemSize() {
            return 2;
        }
    }

    private static class FloatCodec extends Codec {
        public int encode( Object value, DataOutput out ) throws IOException {
            out.writeFloat( value == null ? Float.NaN
                                          : ((Number) value).floatValue() );
            return 4;
        }
        public Object decodeObject( ByteStoreAccess in ) throws IOException {
            return Float.valueOf( in.readFloat() );
        }
        public int decodeInt( ByteStoreAccess in ) throws IOException {
            return (int) in.readFloat();
        }
        public long decodeLong( ByteStoreAccess in ) throws IOException {
            return (long) in.readFloat();
        }
        public double decodeDouble( ByteStoreAccess in ) throws IOException {
            return in.readFloat();
        }
        public boolean decodeBoolean( ByteStoreAccess in ) throws IOException {
            return ! ( in.readFloat() == 0 );
        }
        public int getItemSize() {
            return 4;
        }
    }

    private static class DoubleCodec extends Codec {
        public int encode( Object value, DataOutput out ) throws IOException {
            out.writeDouble( value == null ? Double.NaN
                                           : ((Number) value).doubleValue() );
            return 8;
        }
        public Object decodeObject( ByteStoreAccess in ) throws IOException {
            return Double.valueOf( in.readDouble() );
        }
        public int decodeInt( ByteStoreAccess in ) throws IOException {
            return (int) in.readDouble();
        }
        public long decodeLong( ByteStoreAccess in ) throws IOException {
            return (long) in.readDouble();
        }
        public double decodeDouble( ByteStoreAccess in ) throws IOException {
            return in.readDouble();
        }
        public boolean decodeBoolean( ByteStoreAccess in ) throws IOException {
            return ! ( in.readDouble() == 0 );
        }
        public int getItemSize() {
            return 8;
        }
    }

    private static class BooleanCodec extends Codec {
        public int encode( Object value, DataOutput out ) throws IOException {
            byte brep = value == null 
                      ? (byte) ' '
                      : ( ((Boolean) value).booleanValue() ? (byte) 'T'
                                                           : (byte) 'F' );
            out.writeByte( brep );
            return 1;
        }
        public Boolean decodeObject( ByteStoreAccess in ) throws IOException {
            switch ( in.readByte() ) {
                case (byte) 'T':
                    return Boolean.TRUE;
                case (byte) 'F':
                    return Boolean.FALSE;
                case (byte) ' ':
                    return null;
            }
            this.warnCorrupt();
            return null;
        }
        public int decodeInt( ByteStoreAccess in ) throws IOException {
            return decodeBoolean( in ) ? 1 : 0;
        }
        public long decodeLong( ByteStoreAccess in ) throws IOException {
            return decodeBoolean( in ) ? 1 : 0;
        }
        public double decodeDouble( ByteStoreAccess in ) throws IOException {
            switch ( in.readByte() ) {
                case (byte) 'T':
                    return 1.0;
                case (byte) 'F':
                    return 0.0;
                case (byte) ' ':
                    return Double.NaN;
            }
            this.warnCorrupt();
            return Double.NaN;
        }
        public boolean decodeBoolean( ByteStoreAccess in ) throws IOException {
            return in.readByte() == 'T';
        }
        public int getItemSize() {
            return 1;
        }
    }

    /**
     * Codec implementation which flags null values using a prepended
     * byte in the serialized form.
     */
    private static class FlaggedCodec extends Codec {
        Codec baseCodec_;
        int itemSize_;
        byte[] nullBuffer_;
        static final byte BAD = (byte) 0xff;
        static final byte OK = (byte) 0x00;

        FlaggedCodec( Codec baseCodec ) {
            baseCodec_ = baseCodec;
            itemSize_ = baseCodec.getItemSize() + 1;
            nullBuffer_ = new byte[ itemSize_ ];
            nullBuffer_[ 0 ] = BAD;
        }

        public int encode( Object value, DataOutput out ) throws IOException {
            if ( value == null ) {
                out.write( nullBuffer_ );
            }
            else {
                out.write( OK );
                baseCodec_.encode( value, out );
            }
            return itemSize_;
        }

        public Object decodeObject( ByteStoreAccess in ) throws IOException {
            byte flag = in.readByte();
            switch ( flag ) {
                case OK:
                    return baseCodec_.decodeObject( in );
                case BAD: 
                    in.skip( itemSize_ - 1 );
                    return null;
                default:
                    this.warnCorrupt();
                    return null;
            }
        }

        public int decodeInt( ByteStoreAccess in ) throws IOException {
            byte flag = in.readByte();
            switch ( flag ) {
                case OK:
                    return baseCodec_.decodeInt( in );
                case BAD:
                    in.skip( itemSize_ - 1 );
                    return 0;
                default:
                    this.warnCorrupt();
                    return 0;
            }
        }

        public long decodeLong( ByteStoreAccess in ) throws IOException {
            byte flag = in.readByte();
            switch ( flag ) {
                case OK:
                    return baseCodec_.decodeLong( in );
                case BAD:
                    in.skip( itemSize_ - 1 );
                    return 0;
                default:
                    this.warnCorrupt();
                    return 0;
            }
        }

        public double decodeDouble( ByteStoreAccess in ) throws IOException {
            byte flag = in.readByte();
            switch ( flag ) {
                case OK:
                    return baseCodec_.decodeDouble( in );
                case BAD:
                    in.skip( itemSize_ - 1 );
                    return Double.NaN;
                default:
                    this.warnCorrupt();
                    return Double.NaN;
            }
        }

        public boolean decodeBoolean( ByteStoreAccess in ) throws IOException {
            byte flag = in.readByte();
            switch ( flag ) {
                case OK:
                    return baseCodec_.decodeBoolean( in );
                case BAD:
                    in.skip( itemSize_ - 1 );
                    return false;
                default:
                    this.warnCorrupt();
                    return false;
            }
        }

        public int getItemSize() {
            return itemSize_;
        }
    }

    /**
     * Partial Codec implementation for codecs that are dealing with objects
     * that have no reasonable primitive interpretation.
     */
    private static abstract class ObjectCodec extends Codec {
        public int decodeInt( ByteStoreAccess in ) throws IOException {
            decodeObject( in );
            return 0;
        }
        public long decodeLong( ByteStoreAccess in ) throws IOException {
            decodeObject( in );
            return 0;
        }
        public double decodeDouble( ByteStoreAccess in ) throws IOException {
            decodeObject( in );
            return Double.NaN;
        }
        public boolean decodeBoolean( ByteStoreAccess in ) throws IOException {
            decodeObject( in );
            return false;
        }
    }

    /**
     * Codec which de/serialises fixed-length arrays.
     */
    private static class FixedArrayCodec extends ObjectCodec {
        final int nel_;
        final Codec1 codec1_;
        final int itemSize_;

        FixedArrayCodec( Codec1 codec1, int nel ) {
            codec1_ = codec1;
            nel_ = nel;
            itemSize_ = nel * codec1.getItemSize1();
        }

        public int encode( Object value, DataOutput out ) throws IOException {
            for ( int i = 0; i < nel_; i++ ) {
                codec1_.encode1( value, i, out );
            }
            return itemSize_;
        }

        public Object decodeObject( ByteStoreAccess in ) throws IOException {
            Object value = codec1_.getBuffer( nel_ );
            for ( int i = 0; i < nel_; i++ ) {
                codec1_.decode1( value, i, in );
            }
            return value;
        }

        public int getItemSize() {
            return itemSize_;
        }
    }

    /**
     * Codec which de/serializes variable length arrays.  The number of
     * elements is written as an integer value ahead of the data bytes
     * themselves.
     */
    private static class VariableArrayCodec extends ObjectCodec {
        final Codec1 codec1_;

        VariableArrayCodec( Codec1 codec1 ) {
            codec1_ = codec1;
        }

        public int encode( Object value, DataOutput out ) throws IOException {
            int nel = value == null ? 0 : Array.getLength( value );
            out.writeInt( nel );
            for ( int i = 0; i < nel; i++ ) {
                codec1_.encode1( value, i, out );
            }
            return 4 + nel * codec1_.getItemSize1();
        }

        public Object decodeObject( ByteStoreAccess in ) throws IOException {
            int nel = in.readInt();
            if ( nel < 0 ) {
                this.warnCorrupt();
                return null;
            }
            else if ( nel == 0 ) {
                return null;
            }
            else {
                Object value = codec1_.getBuffer( nel );
                for ( int i = 0; i < nel; i++ ) {
                    codec1_.decode1( value, i, in );
                }
                return value;
            }
        }

        public int getItemSize() {
            return -1;
        }
    }

    private static class FixedStringCodec extends ObjectCodec {

        final int nchar_;

        FixedStringCodec( int nchar ) {
            nchar_ = nchar;
        }

        public int encode( Object value, DataOutput out ) throws IOException {
            String sval = (String) value;
            int ic = 0;
            if ( sval != null ) {
                int leng = sval.length();
                for ( ; ic < nchar_ && ic < leng; ic++ ) {
                    out.writeChar( sval.charAt( ic ) );
                }
            }
            for ( ; ic < nchar_; ic++ ) {
                out.writeChar( (char) 0 );
            }
            return nchar_ * 2;
        }

        public Object decodeObject( ByteStoreAccess in ) throws IOException {
            int lastNonZero = -1;
            char[] cbuf = new char[ nchar_ ];
            for ( int ic = 0; ic < nchar_; ic++ ) {
                char c = in.readChar();
                if ( c != 0 ) {
                    lastNonZero = ic;
                }
                cbuf[ ic ] = c;
            }
            return lastNonZero < 0 ? null
                                   : new String( cbuf, 0, lastNonZero + 1 );
        }
        public int getItemSize() {
            return nchar_ * 2;
        }
    }

    private static class VariableStringCodec extends VariableArrayCodec {
        VariableStringCodec() {
            super( new CharCodec1() );
        }
        public int encode( Object value, DataOutput out ) throws IOException {
            char[] cval = value == null ? null 
                                        : ((String) value).toCharArray();
            return super.encode( cval, out );
        }
        public Object decodeObject( ByteStoreAccess in ) throws IOException {
            char[] cval = (char[]) super.decodeObject( in );
            return cval == null ? null
                                : new String( cval );
        }
    }

    private static class StringArrayCodec extends ObjectCodec {
        public int encode( Object value, DataOutput out ) throws IOException {
            int count = 0;
            String[] strings = value == null ? new String[ 0 ] 
                                             : (String[]) value;
            int nstr = strings.length;
            out.writeInt( nstr );
            count += 4;
            for ( int is = 0; is < nstr; is++ ) {
                String str = strings[ is ] == null ? "" : strings[ is ];
                int leng = str.length();
                out.writeInt( leng );
                count += 4;
                for ( int ic = 0; ic < leng; ic++ ) {
                    out.writeChar( str.charAt( ic ) );
                    count += 2;
                }
            }
            return count;
        }
        public Object decodeObject( ByteStoreAccess in ) throws IOException {
            int nstr = in.readInt();
            if ( nstr < 0 ) {
                warnCorrupt();
                return null;
            }
            String[] strings = new String[ nstr ];
            for ( int is = 0; is < nstr; is++ ) {
                int leng = in.readInt();
                if ( leng < 0 ) {
                    warnCorrupt();
                    return null;
                }
                if ( leng == 0 ) {
                    strings[ is ] = null;
                }
                else {
                    char[] chrs = new char[ leng ];
                    for ( int ic = 0; ic < leng; ic++ ) {
                        chrs[ ic ] = in.readChar();
                    }
                    strings[ is ] = new String( chrs );
                }
            }
            return strings;
        }
        public int getItemSize() {
            return -1;
        }
    }

    /**
     * Abstract helper class defining an object which can serialize
     * and deserialize elements of an array.
     */
    private static abstract class Codec1 {
        private volatile int warnings_;

        /**
         * Serializes an element of an array to a stream.
         *
         * @param  array   buffer containing data
         * @param  index   index of item in <code>array</code> to be serialized
         * @param  out     destination stream
         */
        abstract void encode1( Object array, int index, DataOutput out )
                throws IOException;

        /**
         * Deserializes from a stream writing the result to an element of
         * an array.
         *
         * @param  array  buffer to which item will be deserialized
         * @param  index  position in <code>array/code> to which to write
         * @param  in     source stream
         */
        abstract void decode1( Object array, int index, ByteStoreAccess in )
                throws IOException;

        /**
         * Returns a <code>size</code>-element array suitable for use in
         * the <code>encode1</code> and <code>decode1</code> methods.
         *
         * @param  new array
         */
        abstract Object getBuffer( int size );

        /**
         * Returns the number of bytes written by <code>encode1</code>.
         */
        abstract int getItemSize1();

        /**
         * Logs a warning that unexpected data has been found in the stream
         * during decoding.
         */
        protected void warnCorrupt() {
            if ( ++warnings_ <= 3 ) {
                logger_.warning( "Unexpected stream data - table raw data " +
                                "stream is probably corrupt" );
                if ( warnings_ == 3 ) {
                   logger_.warning( "... more" );
                }
            }
        }
    }

    private static class ByteCodec1 extends Codec1 {
        public void encode1( Object array, int index, DataOutput out )
                throws IOException {
            out.writeByte( ((byte[]) array)[ index ] );
        }
        public void decode1( Object array, int index, ByteStoreAccess in )
                throws IOException {
            ((byte[]) array)[ index ] = in.readByte();
        }
        public int getItemSize1() {
            return 1;
        }
        public Object getBuffer( int size ) {
            return new byte[ size ];
        }
    }

    private static class ShortCodec1 extends Codec1 {
        public void encode1( Object array, int index, DataOutput out )
                throws IOException {
            out.writeShort( ((short[]) array)[ index ] );
        }
        public void decode1( Object array, int index, ByteStoreAccess in )
                throws IOException {
            ((short[]) array)[ index ] = in.readShort();
        }
        public int getItemSize1() {
            return 2;
        }
        public Object getBuffer( int size ) {
            return new short[ size ];
        }
    }

    private static class IntCodec1 extends Codec1 {
        public void encode1( Object array, int index, DataOutput out )
                throws IOException {
            out.writeInt( ((int[]) array)[ index ] );
        }
        public void decode1( Object array, int index, ByteStoreAccess in )
                throws IOException {
            ((int[]) array)[ index ] = in.readInt();
        }
        public int getItemSize1() {
            return 4;
        }
        public Object getBuffer( int size ) {
            return new int[ size ];
        }
    }

    private static class LongCodec1 extends Codec1 {
        public void encode1( Object array, int index, DataOutput out )
                throws IOException {
            out.writeLong( ((long[]) array)[ index ] );
        }
        public void decode1( Object array, int index, ByteStoreAccess in )
                throws IOException {
            ((long[]) array)[ index ] = in.readLong();
        }
        public int getItemSize1() {
            return 8;
        }
        public Object getBuffer( int size ) {
            return new long[ size ];
        }
    }

    private static class FloatCodec1 extends Codec1 {
        public void encode1( Object array, int index, DataOutput out )
                throws IOException {
            out.writeFloat( ((float[]) array)[ index ] );
        }
        public void decode1( Object array, int index, ByteStoreAccess in )
                throws IOException {
            ((float[]) array)[ index ] = in.readFloat();
        }
        public int getItemSize1() {
            return 4;
        }
        public Object getBuffer( int size ) {
            return new float[ size ];
        }
    }

    private static class DoubleCodec1 extends Codec1 {
        public void encode1( Object array, int index, DataOutput out )
                throws IOException {
            out.writeDouble( ((double[]) array)[ index ] );
        }
        public void decode1( Object array, int index, ByteStoreAccess in )
                throws IOException {
            ((double[]) array)[ index ] = in.readDouble();
        }
        public int getItemSize1() {
            return 8;
        }
        public Object getBuffer( int size ) {
            return new double[ size ];
        }
    }

    private static class CharCodec1 extends Codec1 {
        public void encode1( Object array, int index, DataOutput out )
                throws IOException {
            out.writeChar( ((char[]) array)[ index ] );
        }
        public void decode1( Object array, int index, ByteStoreAccess in )
                throws IOException {
            ((char[]) array)[ index ] = in.readChar();
        }
        public int getItemSize1() {
            return 2;
        }
        public Object getBuffer( int size ) {
            return new char[ size ];
        }
    }

    private static class BooleanCodec1 extends Codec1 {
        public void encode1( Object array, int index, DataOutput out )
                throws IOException {
            out.writeByte( ((boolean[]) array)[ index ] ? (byte) 'T' 
                                                        : (byte) 'F' );
        }
        public void decode1( Object array, int index, ByteStoreAccess in )
                throws IOException {
            boolean bval;
            switch ( in.readByte() ) {
                case (byte) 'T':
                    bval = true;
                    break;
                case (byte) 'F':
                    bval = false;
                    break;
                default:
                    bval = false;
                    this.warnCorrupt();
            }
            ((boolean[]) array)[ index ] = bval;
        }
        public int getItemSize1() {
            return 1;
        }
        public Object getBuffer( int size ) {
            return new boolean[ size ];
        }
    }
}
