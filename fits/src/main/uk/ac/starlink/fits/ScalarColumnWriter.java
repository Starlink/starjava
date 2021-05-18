package uk.ac.starlink.fits;

import java.io.DataOutput;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.logging.Logger;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.Tables;

/**
 * ColumnWriter for writing scalar values.
 * Abstract class with a factory method for providing implementations.
 *
 * @author   Mark Taylor
 * @since    10 Jul 2008
 */
abstract class ScalarColumnWriter implements ColumnWriter {

    private final char formatChar_;
    private final int nbyte_;
    private final Number badNumber_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.fits" );

    /**
     * Constructor.
     *
     * @param  formatChar  TFORM type-specific format character
     * @param  nbyte   number of bytes per scalar written
     * @param  badNumber  numeric value used for bad values, or null
     */
    protected ScalarColumnWriter( char formatChar, int nbyte,
                                  Number badNumber ) {
        formatChar_ = formatChar;
        nbyte_ = nbyte;
        badNumber_ = badNumber;
    }

    public String getFormat() {
        return new String( new char[] { formatChar_ } );
    }

    public char getFormatChar() {
        return formatChar_;
    }

    public int getLength() {
        return nbyte_;
    }

    public int[] getDims() {
        return null;
    }

    public BigDecimal getZero() {
        return BigDecimal.ZERO;
    }

    public double getScale() {
        return 1.0;
    }

    public Number getBadNumber() {
        return badNumber_;
    }

    /**
     * Constructs ScalarColumnWriters for specific column metadata.
     *
     * @param  cinfo   column metadata for column to be written
     * @param  nullableInt  true if the column contains integer values which
     *                      may be null
     * @param   allowSignedByte  if true, bytes written as FITS signed bytes
     *          (TZERO=-128), if false bytes written as signed shorts
     * @param   padChar   padding character for undersized character arrays
     * @return  new column writer, or null if we don't know how to do it
     */
    public static ScalarColumnWriter
                  createColumnWriter( ColumnInfo cinfo, boolean nullableInt,
                                      boolean allowSignedByte,
                                      final byte padChar ) {
        Class<?> clazz = cinfo.getContentClass();
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
        boolean isUbyte =
            Boolean.TRUE
           .equals( cinfo.getAuxDatumValue( Tables.UBYTE_FLAG_INFO,
                                            Boolean.class ) );
        final BigInteger longOffset = getLongOffset( cinfo );

        if ( isUbyte && clazz == Short.class ) {
            final short badVal = blankNum == null ? (short) 0xff
                                                  : blankNum.shortValue();
            return new ScalarColumnWriter( 'B', 1,
                                           nullableInt ? new Short( badVal )
                                                       : null ) {
                public void writeValue( DataOutput stream, Object value )
                        throws IOException {
                    short bval = ( value != null )
                               ? ((Number) value).shortValue()
                               : badVal;
                    stream.writeByte( bval );
                }
            }; 
        }
        else if ( longOffset != null && clazz == String.class ) {
            final long badVal = blankNum == null ? Long.MIN_VALUE
                                                 : blankNum.longValue();
            final BigDecimal zeroNum = new BigDecimal( longOffset );
            return new ScalarColumnWriter( 'K', 8,
                                           nullableInt ? new Long( badVal )
                                                       : null ) {
                @Override
                public BigDecimal getZero() {
                    return zeroNum;
                }
                public void writeValue( DataOutput stream, Object value )
                        throws IOException {
                    long lval = value instanceof String
                              ? getOffsetLongValue( (String) value, longOffset,
                                                    badVal )
                              : badVal;
                    stream.writeLong( lval );
                }
            };
        }
        else if ( clazz == Boolean.class ) {
            return new ScalarColumnWriter( 'L', 1, null ) {
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
            if ( allowSignedByte ) {
                final byte[] buf = new byte[ 1 ];
                final byte badVal = blankNum == null ? (byte) 0
                                                     : blankNum.byteValue();
                final BigDecimal zeroByte = new BigDecimal( -128 );
                return new ScalarColumnWriter( 'B', 1,
                                               nullableInt ? new Byte( badVal )
                                                           : null ) {
                    public void writeValue( DataOutput stream, Object value )
                            throws IOException {
                        byte b = (value != null) ? ((Number) value).byteValue()
                                                 : badVal;
                        buf[ 0 ] = (byte) ( b ^ (byte) 0x80 );
                        stream.write( buf );
                    }
                    public BigDecimal getZero() {
                        return zeroByte;
                    }
                };
            }
            else {
                final short badVal = blankNum == null
                                   ? (short) ( Byte.MIN_VALUE - (short) 1 )
                                   : blankNum.shortValue();
                return new ScalarColumnWriter( 'I', 2,
                                               nullableInt ? new Short( badVal )
                                                           : null ) {
                    public void writeValue( DataOutput stream, Object value )
                            throws IOException {
                        short sval = ( value != null )
                                ? ((Number) value).shortValue()
                                : badVal;
                        stream.writeShort( sval );
                    }
                };
            }
        }
        else if ( clazz == Short.class ) {
            final short badVal = blankNum == null ? Short.MIN_VALUE
                                                  : blankNum.shortValue();
            return new ScalarColumnWriter( 'I', 2,
                                           nullableInt ? new Short( badVal )
                                                       : null ) {
                public void writeValue( DataOutput stream, Object value )
                        throws IOException {
                    short sval = ( value != null )
                               ? ((Number) value).shortValue()
                               : badVal;
                    stream.writeShort( sval );
                }
            };
        }
        else if ( clazz == Integer.class ) {
            final int badVal = blankNum == null ? Integer.MIN_VALUE
                                                : blankNum.intValue();
            return new ScalarColumnWriter( 'J', 4,
                                           nullableInt ? new Integer( badVal )
                                                       : null ) {
                public void writeValue( DataOutput stream, Object value )
                        throws IOException {
                    int ival = ( value != null )
                             ? ((Number) value).intValue()
                             : badVal;
                    stream.writeInt( ival );
                }
            };
        }
        else if ( clazz == Long.class ) {
            final long badVal = blankNum == null ? Long.MIN_VALUE
                                                 : blankNum.longValue();
            return new ScalarColumnWriter( 'K', 8,
                                           nullableInt ? new Long( badVal )
                                                       : null ) {
                 public void writeValue( DataOutput stream, Object value )
                        throws IOException {
                    long lval = ( value != null )
                              ? ((Number) value).longValue()
                              : badVal;
                    stream.writeLong( lval );
                }
            };
        }
        else if ( clazz == Float.class ) {
            return new ScalarColumnWriter( 'E', 4, null ) {
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
            return new ScalarColumnWriter( 'D', 8, null ) {
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
            return new ScalarColumnWriter( 'A', 1, null ) {
                public void writeValue( DataOutput stream, Object value )
                        throws IOException {
                    char cval = ( value != null )
                              ? ((Character) value).charValue()
                              : (char) padChar;
                    stream.writeByte( cval );
                }
            };
        }
        else {
            return null;
        }
    }

    /**
     * Acquires the long offset value for a column as a big integer,
     * if it has one.
     *
     * @param  cinfo   column metadata
     * @return  BigInteger representation of long offset, or null
     */
    static BigInteger getLongOffset( ColumnInfo cinfo ) {
        Class<?> clazz = cinfo.getContentClass();
        if ( String.class.equals( clazz ) ||
             String[].class.equals( clazz ) ) {
            String longoffTxt =
                cinfo.getAuxDatumValue( BintableStarTable.LONGOFF_INFO,
                                        String.class );
            if ( longoffTxt != null ) {
                try {
                    return new BigInteger( longoffTxt );
                }
                catch ( NumberFormatException e ) {
                }
            }
        }
        return null;
    }

    /**
     * Offsets a string value representing an integer by a given
     * BigInteger offset to give a long result.
     * If the string is unsuitable or the result is out of range
     * for a long, a supplied bad value is returned instead.
     *
     * @param  txt  string representation of value
     * @param  longOffset  offset value
     * @param  badVal   result for case of error
     * @return   long representation of offset result, or <code>badVal</code>
     */
    static long getOffsetLongValue( String txt, BigInteger longOffset,
                                    long badVal ) {
        if ( txt == null ) {
            return badVal;
        }

        /* This could be coded more efficiently (exceptions are expensive),
         * but it's not expected that the conversion will fail under
         * the most common circumstances (FITS table column round-tripping). */
        try {
            return new BigInteger( txt )
                  .subtract( longOffset )
                  .longValueExact();
        }
        catch ( NumberFormatException e ) {  // not numeric string
            return badVal;
        }
        catch ( ArithmeticException e ) {    // out of range for long
            return badVal;
        }
    }
}
