package uk.ac.starlink.feather;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import uk.ac.bristol.star.feather.BufUtils;
import uk.ac.bristol.star.feather.FeatherType;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;

/**
 * Implementations of StarColumnWriter.
 *
 * @author   Mark Taylor
 * @since    26 Feb 2020
 */
public class StarColumnWriters {

    /** Default pointer size for variable-length column output. */
    public static final VariableStarColumnWriter.PointerSize VAR_POINTER_SIZE =
        VariableStarColumnWriter.PointerSize.I64;
    private static final byte[] FLOAT_NAN;
    private static final byte[] DOUBLE_NAN;

    static {
        ByteArrayOutputStream dout = new ByteArrayOutputStream( 8 );
        ByteArrayOutputStream fout = new ByteArrayOutputStream( 4 );
        try {
            BufUtils.writeLittleEndianDouble( dout, Double.NaN );
            BufUtils.writeLittleEndianFloat( fout, Float.NaN );
        }
        catch ( IOException e ) {
            assert false;
        }
        DOUBLE_NAN = dout.toByteArray();
        FLOAT_NAN = fout.toByteArray();
        assert DOUBLE_NAN.length == 8;
        assert FLOAT_NAN.length == 4;
    }

    /**
     * Private constructor prevents instantiation.
     */
    private StarColumnWriters() {
    }

    /**
     * Returns a StarColumnWriter suitable for a given column of a StarTable.
     *
     * @param  table  table
     * @param  icol   column index
     * @return  column writer, or null if feather output is not possible
     */
    public static StarColumnWriter
            createColumnWriter( StarTable table, int icol ) {
        ColumnInfo info = table.getColumnInfo( icol );
        Class<?> clazz = info.getContentClass();
        if ( clazz == Double.class ) {
            return new NumberStarColumnWriter( table, icol, FeatherType.DOUBLE,
                                               false, DOUBLE_NAN ) {
                public void writeNumber( OutputStream out, Number val )
                        throws IOException {
                    BufUtils.writeLittleEndianDouble( out, val.doubleValue() );
                }
            };
        }
        else if ( clazz == Float.class ) {
            return new NumberStarColumnWriter( table, icol, FeatherType.FLOAT,
                                               false, FLOAT_NAN ) {
                public void writeNumber( OutputStream out, Number val )
                        throws IOException {
                    BufUtils.writeLittleEndianFloat( out, val.floatValue() );
                }
            };
        }
        else if ( clazz == Long.class ) {
            return new NumberStarColumnWriter( table, icol, FeatherType.INT64,
                                               true, new byte[ 8 ] ) {
                public void writeNumber( OutputStream out, Number val )
                        throws IOException {
                    BufUtils.writeLittleEndianLong( out, val.longValue() );
                }
            };
        }
        else if ( clazz == Integer.class ) {
            return new NumberStarColumnWriter( table, icol, FeatherType.INT32,
                                               true, new byte[ 4 ] ) {
                public void writeNumber( OutputStream out, Number val )
                        throws IOException {
                    BufUtils.writeLittleEndianInt( out, val.intValue() );
                }
            };
        }
        else if ( clazz == Short.class ) {
            if ( info.getAuxDatumValue( Tables.UBYTE_FLAG_INFO, Boolean.class )
                 == Boolean.TRUE ) {
                return new NumberStarColumnWriter( table, icol,
                                                   FeatherType.UINT8, true,
                                                   new byte[ 1 ] ) {
                    public void writeNumber( OutputStream out, Number val )
                            throws IOException {
                        out.write( val.shortValue() & 0xff );
                    }
                };
            }
            else {
                return new NumberStarColumnWriter( table, icol,
                                                   FeatherType.INT16, true,
                                                   new byte[ 2 ] ) {
                    public void writeNumber( OutputStream out, Number val )
                            throws IOException {
                        BufUtils.writeLittleEndianShort( out, val.shortValue());
                    }
                };
            }
        }
        else if ( clazz == Byte.class ) {
            return new NumberStarColumnWriter( table, icol, FeatherType.INT8,
                                               true, new byte[ 1 ] ) {
                public void writeNumber( OutputStream out, Number val )
                        throws IOException {
                    out.write( val.byteValue() & 0xff );
                }
            };
        }
        else if ( clazz == Boolean.class ) {
            return new BooleanStarColumnWriter( table, icol );
        }
        else if ( clazz == String.class ) {
            return VariableStarColumnWriter
                  .createStringWriter( table, icol, false, VAR_POINTER_SIZE );
        }
        else if ( clazz == byte[].class ) {
            return VariableStarColumnWriter
                  .createByteArrayWriter( table, icol, true, VAR_POINTER_SIZE );
        }
        else {
            return null;
        }
    }
}
