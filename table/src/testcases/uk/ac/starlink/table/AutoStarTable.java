package uk.ac.starlink.table;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;
import uk.ac.starlink.util.TestCase;

/**
 * A StarTable implementation which supplies its own, reproducible, data.
 */
public class AutoStarTable extends ColumnStarTable {

    final Map columns = new HashMap();
    final long nrow;
    final TestCase testcase = new TestCase( "dummy" );

    final String[] strings = new String[] {
        "fomalhaut", "andromeda", "sol", null,
        "dougal", "florence", "brian", "", 
        "smiles", "courage", "thatchers", "uley",
        "ant & bee", "a < b < c", "<![CDATA[Not a CDATA marked section!]]>",
        "  ",
    };
    final int nstr = strings.length;

    public AutoStarTable( long nrow ) {
        this.nrow = nrow;
    }

    public long getRowCount() {
        return nrow;
    }

    /**
     * Adds a new column to the table.  Only the column info object needs
     * to be specified, the table itself will supply some reproducible
     * test data consistent with the column info.
     *
     * @param  colinfo  column metadata
     */
    public void addColumn( ColumnInfo colinfo ) {
        final Class clazz = colinfo.getContentClass();
        final int[] shape = colinfo.getShape();
        final int esize = colinfo.getElementSize();
        final boolean isArray = colinfo.isArray();
        final int icol = getColumnCount() + 1;
        final boolean isUbyte =
            Boolean.TRUE
           .equals( colinfo.getAuxDatumValue( Tables.UBYTE_FLAG_INFO,
                                              Boolean.class ) );
        int n1 = 1;
        if ( shape != null && shape.length > 0 ) {
            for ( int i = 0; i < shape.length; i++ ) {
                n1 *= shape[ i ];
            }
        }
        if ( n1 < 0 ) {
            n1 = n1 * -1 * ( icol % 3 + ( ( shape.length == 0 ) ? 2 : 1 ) );
        }
        final int nel = n1;
        addColumn( new ColumnData( colinfo ) {
             public Object readValue( long lrow ) {
                int irow = (int) lrow;
                int ival = 0;
                if ( irow % 2 == 0 ) {
                    ival = -irow;
                }
                else { 
                    ival = irow;
                }
                if ( ( irow + icol ) % 10 == 0 ) {
                    return null;
                }
                else if ( clazz == Boolean.class ) {
                    return Boolean.valueOf( icol + irow % 2 == 0 );
                }
                else if ( clazz == Byte.class ) {
                    byte val = (byte) irow;
                    if ( val == -128 || val == 127 ) {
                        val = 0;
                    }
                    return Byte.valueOf( val );
                }
                else if ( clazz == Short.class ) {
                    return isUbyte ? Short.valueOf( (short) ( irow % 256 ) )
                                   : Short.valueOf( (short) irow );
                }
                else if ( clazz == Integer.class ) {
                    return Integer.valueOf( icol + 100 * irow );
                }
                else if ( clazz == Long.class ) {
                    return Long.valueOf( icol + 1000000 * irow );
                }
                else if ( clazz == Float.class ) {
                    if ( irow % 10 == 4 ) {
                        return Float.valueOf( Float.NaN );
                    }
                    return Float.valueOf( icol + 1000 * irow );
                }
                else if ( clazz == Double.class ) {
                    if ( irow % 10 == 6 ) {
                        return Double.valueOf( Double.NaN );
                    }
                    return Double.valueOf( icol + 1000 * irow );
                }
                else if ( clazz == String.class ) {
                    return strings[ Math.abs( nstr + ival ) % nstr ] 
                         + " " + ival; 
                }

                else if ( clazz == boolean[].class ) {
                    boolean[] array = new boolean[ nel ];
                    for ( int i = 0; i < nel; i++ ) {
                        array[ i ] = i % 2 == 0;
                    }
                    return array;
                }
                else if ( clazz == byte[].class ||
                          clazz == short[].class ||
                          clazz == int[].class ||
                          clazz == long[].class ||
                          clazz == float[].class ||
                          clazz == double[].class ) {
                    Object array = Array.newInstance( clazz.getComponentType(),
                                                      nel );
                    testcase.fillCycle( array, -icol - irow, icol + irow );
                    if ( clazz == short[].class && isUbyte ) {
                        short[] sarray = (short[]) array;
                        for ( int i = 0; i < sarray.length; i++ ) {
                            sarray[ i ] =
                               (short) ( Math.abs( sarray[ i ] ) % 256 );
                        }
                    }
                    return array;
                }
                else if ( clazz == String[].class ) {
                    String[] array = new String[ nel ];
                    for ( int i = 0; i < nel; i++ ) {
                        array[ i ] = ( i % 4 == 0 ) ? null : 
                              strings[ Math.abs( ival % nstr )  ] + " " + i;
                    }
                    return array;
                }
                else {
                    throw new AssertionError( "Can't fill this column" );
                }
            }
        } );
    }
}
