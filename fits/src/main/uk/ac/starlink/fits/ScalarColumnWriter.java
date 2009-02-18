package uk.ac.starlink.fits;

import java.io.DataOutput;
import java.io.IOException;
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

    public double getZero() {
        return 0.0;
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
     * @return  new column writer, or null if we don't know how to do it
     */
    public static ScalarColumnWriter
                  createColumnWriter( ColumnInfo cinfo, boolean nullableInt,
                                      boolean allowSignedByte ) {
        Class clazz = cinfo.getContentClass();
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
                    public double getZero() {
                        return -128.0;
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
                              : ' ';
                    stream.writeByte( cval );
                }
            };
        }
        else {
            return null;
        }
    }
}
