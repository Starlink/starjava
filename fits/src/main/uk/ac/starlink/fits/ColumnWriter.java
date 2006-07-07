package uk.ac.starlink.fits;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.Tables;

/**
 * Describes what needs to be done to write an object to a DataOutput
 * in FITS format for a given table column.
 *
 * @author   Mark Taylor (Starlink)
 * @since    12 Dec 2003
 */
abstract class ColumnWriter {

    private final int length_;
    private final int[] dims_;
    private final char formatChar_;
    private String format_;

    /**
     * Constructs a new writer with a given length and dimension array.
     *
     * @param  formatChar  the basic character denoting this writers
     *         format in a FITS header
     * @param  length  number of bytes per element
     * @param  dims  array to be imposed on written value
     */
    public ColumnWriter( char formatChar, int elength, int[] dims ) {
        formatChar_ = formatChar;
        dims_ = dims;
        int nel = 1;
        if ( dims != null ) {
            for ( int i = 0; i < dims.length; i++ ) {
                nel *= dims[ i ];
            }
        }
        length_ = elength * nel;
        format_ = ( nel == 1 ) ? "" + formatChar
                               : ( Integer.toString( nel ) + formatChar );
    }

    /**
     * Constructs a new scalar writer with a given length.
     *
     * @param  formatChar  the basic character denoting this writers
     *         format in a FITS header
     * @param  length  number of bytes per value
     */
    public ColumnWriter( char formatChar, int length ) {
        this( formatChar, length, null );
    }

    /**
     * Writes a value to an output stream.
     *
     * @param  stream to squirt the value's byte serialization into
     * @param  value  the value to write into <tt>stream</tt>
     */
    public abstract void writeValue( DataOutput stream, Object value )
            throws IOException;

    /**
     * Returns the format character appropriate for this writer.
     *
     * @return  format character
     */
    public char getFormatChar() {
        return formatChar_;
    }

    /**
     * Returns the TFORM string appropriate for this writer.
     *
     * @return  format string
     */
    public String getFormat() {
        return format_;
    }

    /**
     * Returns the number of bytes that <tt>writeValue</tt> will write.
     */
    public int getLength() {
        return length_;
    }

    /**
     * Returns the dimensionality (in FITS terms) of the values
     * that this writes.  Null for scalars.
     *
     * @return   dims
     */
    public int[] getDims() {
        return dims_;
    }

    /**
     * Returns zero offset to be used for interpreting values this writes.
     *
     * @param  zero value
     */
    public double getZero() {
        return 0.0;
    }

    /**
     * Returns the scale factor to be used for interpreting values this
     * writes.
     *
     * @param  scale factor
     */
    public double getScale() {
        return 1.0;
    }

    /**
     * Returns the number to be used for blank field output (TNULLn).
     * Only relevant for integer scalar items.
     *
     * @return  magic bad value
     */
    public Number getBadNumber() {
        return null;
    }

    /**
     * Returns a column writer capable of writing a given column to
     * a stream in FITS format.
     *
     * @param   cinfo  describes the column to write
     * @param   shape  shape for array values
     * @param   elementSize  element size
     * @param   nullableInt  true if we are going to have to store nulls in
     *          an integer column
     * @return  a suitable column writer, or <tt>null</tt> if we don't
     *          know how to write this to FITS
     */
    public static ColumnWriter makeColumnWriter( ColumnInfo cinfo, int[] shape,
                                                  int eSize,
                                                  final boolean nullableInt ) {
        Class clazz = cinfo.getContentClass();

        int n1 = 1;
        if ( shape != null ) {
            for ( int i = 0; i < shape.length; i++ ) {
                n1 *= shape[ i ];
            }
        }
        final int nel = n1;

        Number blankNum = null;
        if ( nullableInt ) {
            DescribedValue blankVal =
                cinfo.getAuxDatum( Tables.NULL_VALUE_INFO );
            if ( blankVal != null ) {
                Object blankObj = blankVal.getValue();
                if ( blankObj instanceof Number ) {
                    blankNum = (Number) blankObj;
                }
            }
        }

        if ( clazz == Boolean.class ) {
            return new ColumnWriter( 'L', 1 ) {
                public void writeValue( DataOutput stream, Object value )
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
                    stream.writeByte( b );
                }
            };
        }
        else if ( clazz == Byte.class ) {

            /* Byte is a bit tricky since a FITS byte is unsigned, while
             * a byte in a StarTable (a java byte) is signed. */
            final byte[] buf = new byte[ 1 ];
            final byte badVal = blankNum == null ? (byte) 0
                                                 : blankNum.byteValue();
            return new ColumnWriter( 'B', 1 ) {
                public void writeValue( DataOutput stream, Object value )
                        throws IOException {
                    byte b = (value != null) ? ((Number) value).byteValue()
                                             : badVal;
                    buf[ 0 ] = (byte) ( b ^ (byte) 0x80 );
                    stream.write( buf );
                }
                public double getZero() {
                    return -128.0;
                }
                public Number getBadNumber() {
                    return nullableInt ? new Byte( badVal ) : null;
                }
            };
        }
        else if ( clazz == Short.class ) {
            final short badVal = blankNum == null ? Short.MIN_VALUE
                                                  : blankNum.shortValue();
            return new ColumnWriter( 'I', 2 ) {
                public void writeValue( DataOutput stream, Object value )
                        throws IOException {
                    short sval = ( value != null )
                               ? ((Number) value).shortValue()
                               : badVal;
                    stream.writeShort( sval );
                }
                public Number getBadNumber() {
                    return nullableInt ? new Short( badVal ) : null;
                }
            };
        }
        else if ( clazz == Integer.class ) {
            final int badVal = blankNum == null ? Integer.MIN_VALUE
                                                : blankNum.intValue();
            return new ColumnWriter( 'J', 4 ) {
                public void writeValue( DataOutput stream, Object value )
                        throws IOException {
                    int ival = ( value != null )
                             ? ((Number) value).intValue()
                             : badVal;
                    stream.writeInt( ival );
                }
                public Number getBadNumber() {
                    return nullableInt ? new Integer( badVal ) : null;
                }
            };
        }
        else if ( clazz == Long.class ) {
            final long badVal = blankNum == null ? Long.MIN_VALUE
                                                 : blankNum.longValue();
            return new ColumnWriter( 'K', 8 ) {
                public void writeValue( DataOutput stream, Object value )
                        throws IOException {
                    long lval = ( value != null )
                              ? ((Number) value).longValue()
                              : badVal;
                    stream.writeLong( lval );
                }
                public Number getBadNumber() {
                    return nullableInt ? new Long( badVal ) : null;
                }
            };
        }
        else if ( clazz == Float.class ) {
            return new ColumnWriter( 'E', 4 ) {
                public void writeValue( DataOutput stream, Object value )
                        throws IOException {
                    float fval = ( value != null )
                               ? ((Number) value).floatValue()
                               : Float.NaN;
                    stream.writeFloat( fval );
                }
            };
        }
        else if ( clazz == Double.class ) {
            return new ColumnWriter( 'D', 8 ) {
                public void writeValue( DataOutput stream, Object value )
                        throws IOException {
                    double dval = ( value != null )
                                ? ((Number) value).doubleValue()
                                : Double.NaN;
                    stream.writeDouble( dval );
                }
            };
        }
        else if ( clazz == Character.class ) {
            return new ColumnWriter( 'A', 1 ) {
                public void writeValue( DataOutput stream, Object value )
                        throws IOException {
                    char cval = ( value != null )
                              ? ((Character) value).charValue()
                              : ' ';
                    stream.writeByte( cval );
                }
            };
        }
        else if ( clazz == String.class ) {
            final int maxChars = eSize;
            final byte[] buf = new byte[ maxChars ];
            final byte[] blankBuf = new byte[ maxChars ];
            final byte PAD = (byte) ' ';
            final int[] charDims = new int[] { maxChars };
            Arrays.fill( blankBuf, PAD );
            return new ColumnWriter( 'A', maxChars ) {
                public void writeValue( DataOutput stream, Object value )
                        throws IOException {
                    byte[] bytes;
                    if ( value == null ) {
                        bytes = blankBuf;
                    }
                    else {
                        String sval = (String) value;
                        int i = 0;
                        int leng = Math.min( sval.length(), maxChars );
                        bytes = buf;
                        for ( ; i < leng; i++ ) {
                            bytes[ i ] = (byte) sval.charAt( i );
                        }
                        Arrays.fill( bytes, i, maxChars, PAD );
                    }
                    stream.write( bytes );
                }
                public String getFormat() {
                    return Integer.toString( maxChars ) + 'A';
                }
                public int[] getDims() {
                    return charDims;
                }
            };
        }
        else if ( clazz == boolean[].class ) {
            final byte[] buf = new byte[ nel ];
            final byte PAD = (byte) 0;
            return new ColumnWriter( 'L', 1, shape ) {
                public void writeValue( DataOutput stream, Object value )
                        throws IOException {
                    int i = 0;
                    if ( value != null ) {
                        boolean[] bvals = (boolean[]) value;
                        int leng = Math.min( bvals.length, nel );
                        for ( ; i < leng; i++ ) {
                            buf[ i ] = bvals[ i ] ? (byte) 'T' : (byte) 'F';
                        }
                    }
                    Arrays.fill( buf, i, nel, PAD );
                    stream.write( buf );
                }
            };
        }
        else if ( clazz == byte[].class ) {
            final byte[] buf = new byte[ nel ];
            final byte PAD = (byte) 0;
            return new ColumnWriter( 'B', 1, shape ) {
                public void writeValue( DataOutput stream, Object value )
                        throws IOException {
                    int i = 0;
                    if ( value != null ) {
                        byte[] bvals = (byte[]) value;
                        int leng = Math.min( bvals.length, nel );
                        for ( ; i < leng; i++ ) {
                            buf[ i ] = bvals[ i ];
                        }
                    }
                    Arrays.fill( buf, i, nel, PAD );
                    for ( int j = 0; j < nel; j++ ) {
                        buf[ j ] = (byte) ( buf[ j ] ^ (byte) 0x80 );
                    }
                    stream.write( buf );
                }
                public double getZero() {
                    return -128.0;
                }
            };
        }
        else if ( clazz == short[].class ) {
            final short PAD = (short) 0;
            return new ColumnWriter( 'I', 2, shape ) {
                public void writeValue( DataOutput stream, Object value )
                        throws IOException {
                    int i = 0;
                    if ( value != null ) {
                        short[] svals = (short[]) value;
                        int leng = Math.min( svals.length, nel );
                        for ( ; i < leng; i++ ) {
                            stream.writeShort( svals[ i ] );
                        }
                    }
                    for ( ; i < nel; i++ ) {
                        stream.writeShort( PAD );
                    }
                }
            };
        }
        else if ( clazz == int[].class ) {
            final int PAD = 0;
            return new ColumnWriter( 'J', 4, shape ) {
                public void writeValue( DataOutput stream, Object value )
                        throws IOException {
                    int i = 0;
                    if ( value != null ) {
                        int[] ivals = (int[]) value;
                        int leng = Math.min( ivals.length, nel );
                        for ( ; i < leng; i++ ) {
                            stream.writeInt( ivals[ i ] );
                        }
                    }
                    for ( ; i < nel; i++ ) {
                        stream.writeInt( PAD );
                    }
                }
            };
        }
        else if ( clazz == long[].class ) {
            final long PAD = 0L;
            return new ColumnWriter( 'K', 8, shape ) {
                public void writeValue( DataOutput stream, Object value )
                        throws IOException {
                    int i = 0;
                    if ( value != null ) {
                        long[] lvals = (long[]) value;
                        int leng = Math.min( lvals.length, nel );
                        for ( ; i < leng; i++ ) {
                            stream.writeLong( lvals[ i ] );
                        }
                    }
                    for ( ; i < nel; i++ ) {
                        stream.writeLong( PAD );
                    }
                }
            };
        }
        else if ( clazz == float[].class ) {
            final float PAD = Float.NaN;
            return new ColumnWriter( 'E', 4, shape ) {
                public void writeValue( DataOutput stream, Object value )
                        throws IOException {
                    int i = 0;
                    if ( value != null ) {
                        float[] fvals = (float[]) value;
                        int leng = Math.min( fvals.length, nel );
                        for ( ; i < leng; i++ ) {
                            stream.writeFloat( fvals[ i ] );
                        }
                    }
                    for ( ; i < nel; i++ ) {
                        stream.writeFloat( PAD );
                    }
                }
            };
        }
        else if ( clazz == double[].class ) {
            final double PAD = Double.NaN;
            return new ColumnWriter( 'D', 8, shape ) {
                public void writeValue( DataOutput stream, Object value )
                        throws IOException {
                    int i = 0;
                    if ( value != null ) {
                        double[] dvals = (double[]) value;
                        int leng = Math.min( dvals.length, nel );
                        for ( ; i < leng; i++ ) {
                            stream.writeDouble( dvals[ i ] );
                        }
                    }
                    for  ( ; i < nel; i++ ) {
                        stream.writeDouble( PAD );
                    }
                }
            };
        }
        else if ( clazz == String[].class ) {
            final byte PAD = (byte) ' ';
            final int maxChars = eSize;
            int[] charDims = new int[ shape.length + 1 ];
            charDims[ 0 ] = maxChars;
            System.arraycopy( shape, 0, charDims, 1, shape.length );
            final byte[] buf = new byte[ maxChars ];
            return new ColumnWriter( 'A', 1, charDims ) {
                public void writeValue( DataOutput stream, Object value )
                        throws IOException {
                    int i = 0;
                    if ( value != null ) {
                        String[] svals = (String[]) value;
                        int leng = Math.min( svals.length, nel );
                        for ( ; i < leng; i++ ) {
                            String str = svals[ i ];
                            int j = 0;
                            if ( str != null ) {
                                int sleng = Math.min( str.length(), maxChars );
                                for ( ; j < sleng; j++ ) {
                                    buf[ j ] = (byte) str.charAt( j );
                                }
                            }
                            Arrays.fill( buf, j, maxChars, PAD );
                            stream.write( buf );
                        }
                    }
                    if ( i < nel ) {
                        Arrays.fill( buf, PAD );
                        for ( ; i < nel; i++ ) {
                            stream.write( buf );
                        }
                    }
                }
                public String getFormat() {
                    return Integer.toString( maxChars * nel ) + 'A';
                }
            };
        }
        else {
            return null;
        }
    }
}
