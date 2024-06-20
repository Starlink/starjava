package uk.ac.starlink.votable;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.util.TestCase;

/**
 * A StarTable implementation which supplies its own, reproducible, data.
 */
public class AutoStarTable extends ColumnStarTable {

    final Map columns = new HashMap();
    final long nrow;
    final TestCase testcase = new TestCase( "dummy" );

    private static final DefaultValueInfo DRINK_INFO =
        new DefaultValueInfo( "Drink", String.class, "Favourite drink" );
    private static final DefaultValueInfo NAMES_INFO =
        new DefaultValueInfo( "Names", String[].class, "Triple of names" );
    private static final DefaultValueInfo MATRIX_INFO =
        new DefaultValueInfo( "Matrix", int[].class, "2xN matrix" );
    private static final DefaultValueInfo SIZE_INFO =
        new DefaultValueInfo( "Size", Double.class, null );
    private static final DescribedValue UBYTE_AUXDATUM =
        new DescribedValue( Tables.UBYTE_FLAG_INFO, Boolean.TRUE );
    static {
        NAMES_INFO.setElementSize( 16 );
    }

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
        final boolean isUbyte =
            Boolean.TRUE
           .equals( colinfo.getAuxDatumValue( Tables.UBYTE_FLAG_INFO,
                                              Boolean.class ) );
                   
        final int icol = getColumnCount() + 1;
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
                else if ( clazz == Short.class && isUbyte  ) {
                    return Short.valueOf( (short) ( irow % 256 ) );
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
                    return Short.valueOf( (short) irow );
                }
                else if ( clazz == Integer.class ) {
                    return Integer.valueOf( icol + 100 * irow );
                }
                else if ( clazz == Long.class ) {
                    return Long.valueOf( icol + 1000 * irow );
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

    public static StarTable getDemoTable( long nrows ) {
        AutoStarTable table = new AutoStarTable( nrows );

        table.setName( "Test Table" );
        List params = new ArrayList();
        params.add( new DescribedValue( NAMES_INFO,
                                        new String[] { "Test", "Table",
                                                       "x" } ) );
        params.add( new DescribedValue( DRINK_INFO, "Cider" ) );
        params.add( new DescribedValue( MATRIX_INFO,
                                        new int[] { 4, 5, } ) );
        table.setParameters( params );

        table.addColumn( new ColumnData( DRINK_INFO ) {
            public Object readValue( long irow ) {
                return "Drink " + irow;
            }
        } );
        table.addColumn( new ColumnData( NAMES_INFO ) {
            String[] first = { "Ichabod", "Candice", "Rowland" };
            String[] middle = { "Beauchamp", "Burbidge", "Milburn", "X" };
            String[] last = { "Percy", "Neville", "Stanley", "Fitzalan",
                              "Courtenay" };
            public Object readValue( long lrow ) {
                int irow = (int) lrow;
                return new String[] { first[ irow % first.length ],
                                      middle[ irow % middle.length ],
                                      last[ irow % last.length ], };
            }
        } );

        Class[] ptypes = { byte.class, short.class, short.class, int.class,
                           long.class, float.class, double.class, };
        for ( int i = 0; i < ptypes.length; i++ ) {
            final Class ptype = ptypes[ i ];
            String pname = ptype.getName();
            ColumnInfo colinfo = new ColumnInfo( MATRIX_INFO );
            if ( i == 1 ) {
                assert short.class.equals( ptype );
                pname = "ubyte";
                colinfo.setAuxDatum( UBYTE_AUXDATUM );
            }
            colinfo.setContentClass( Array.newInstance( ptype, 0 ).getClass() );
            colinfo.setName( pname + "_matrix" );
            table.addColumn( colinfo );
            ColumnInfo colinfo2 = new ColumnInfo( colinfo );
            colinfo2.setName( pname + "_vector" );
            final int nel = ( i + 2 ) % 4 + 2;
            colinfo2.setShape( new int[] { nel } );
            final int bs = i;
            table.addColumn( colinfo2 );
        }

        Class[] stypes = { Byte.class, Short.class, Short.class, Integer.class,
                           Long.class, Float.class, Double.class,
                           String.class };
        for ( int i = 0; i < stypes.length; i++ ) {
            final int itype = i;
            final Class stype = stypes[ i ];
            String name = stype.getName().replaceFirst( "java.lang.", "" );
            ColumnInfo colinfo = new ColumnInfo( name + "Scalar", stype,
                                                 name + " scalar data" );
            if ( i == 1 ) {
                assert Short.class.equals( stype );
                colinfo.setAuxDatum( UBYTE_AUXDATUM );
                colinfo.setName( "ubyteScalar" );
                colinfo.setDescription( "Unsigned byte scalar data" );
            }
            table.addColumn( colinfo );
        }

        return table;
    }
}
