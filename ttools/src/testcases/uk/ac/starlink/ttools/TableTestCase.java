package uk.ac.starlink.ttools;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import uk.ac.starlink.table.ArrayColumn;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.util.TestCase;

/**
 * TestCase subclass which provides some utility methods useful for
 * doing testing on tables.
 *
 * @author   Mark Taylor
 * @since    25 Aug 2005
 */
public class TableTestCase extends TestCase {

    public TableTestCase() {
        super();
    }

    public TableTestCase( String name ) {
        super( name );
    }

    /**
     * Creates a new column with a given name and data array.
     *
     * @param  name  column name
     * @param  data  data array
     * @return  ColumnData object
     */
    public static ColumnData col( String name, Object data ) {
        return ArrayColumn.makeColumn( name, data );
    }

    /**
     * Returns the index of the column with a given name in a table.
     * An exception is thrown if the column name is not unique.
     *
     * @param  table  table
     * @param  colName  column name
     * @return  column index, or -1 if no such column
     */
    public static int getColIndex( StarTable table, String colName ) {
        int icol = -1;
        for ( int ic = 0; ic < table.getColumnCount(); ic++ ) {
            if ( colName.equals( table.getColumnInfo( ic ).getName() ) ) {
                if ( icol < 0 ) {
                    icol = ic;
                }
                else {
                    fail( "Duplicate column name " + colName );
                }
            }
        }
        return icol;
    }

    /**
     * Returns the elements of a table column as an <code>Object[]</code>
     * array.
     *
     * @param   table   table
     * @param   icol    column index
     * @return   column data
     */
    public static Object[] getColData( StarTable table, int icol ) 
            throws IOException {
        RowSequence rseq = table.getRowSequence();
        List dataList = new ArrayList();
        for ( long lrow = 0; rseq.next(); lrow++ ) {
            Object datum = rseq.getCell( icol );
            assertEquals( datum, rseq.getRow()[ icol ] );
            if ( table.isRandom() ) {
                assertEquals( datum, table.getCell( lrow, icol ) );
            }
            dataList.add( datum );
        }
        rseq.close();
        Object[] data = dataList.toArray( new Object[ 0 ] );
        return data;
    }

    /**
     * Asserts that two tables have the same column names and table data.
     *
     * @param  table1  comparison table
     * @param  table2  test table
     * @throws   junit.framework.AssertionFailedError  if they don't match
     */
    public void assertSameData( StarTable table1, StarTable table2 )
            throws IOException {
        assertArrayEquals( getColNames( table1 ), getColNames( table2 ) );
        for ( int icol = 0; icol < table1.getColumnCount(); icol++ ) {
            assertArrayEquals( getColData( table1, icol ),
                               getColData( table2, icol ) );
        }
    }

    /**
     * Returns an array giving the names of the columns in a table.
     *
     * @param   table  table
     * @return  string array with one entry for each column name
     */
    public static String[] getColNames( StarTable table ) {
        String[] names = new String[ table.getColumnCount() ];
        for ( int i = 0; i < names.length; i++ ) {
            names[ i ] = table.getColumnInfo( i ).getName();
        }
        return names;
    }

    /**
     * "Boxes" an array, e.g. turning a <code>double[]</code> array into
     * an equivalent <code>Object[]</code> array containing 
     * <code>Double</code>s.
     *
     * @param  pArray  primitive array
     * @return  object array
     */
    public static Object[] box( Object pArray ) {
        List oList = new ArrayList();
        for ( int i = 0; i < Array.getLength( pArray ); i++ ) {
            oList.add( Array.get( pArray, i ) );
        }
        // Class clazz = oList.get( 0 ).getClass();
        Class clazz = Object.class;
        return oList.toArray( (Object[]) Array.newInstance( clazz,
                                                            oList.size() ) );
    }

    public static Object unbox( Object[] oArray ) {
        int nel = oArray.length;
        Class clazz = null;
        for ( int i = 0; i < oArray.length && clazz == null; i++ ) {
            if ( oArray[ 0 ] != null ) {
                clazz = oArray[ 0 ].getClass();
            }
        }
        if ( clazz == Double.class ) {
            double[] pArray = new double[ nel ];
            for ( int i = 0; i < nel; i++ ) {
                pArray[ i ] = oArray[ i ] == null 
                            ? Double.NaN
                            : ((Double) oArray[ i ]).doubleValue();
            }
            return pArray;
        }
        else if ( clazz == Float.class ) {
            float[] pArray = new float[ nel ];
            for ( int i = 0; i < nel; i++ ) {
                pArray[ i ] = oArray[ i ] == null
                            ? Float.NaN
                            : ((Float) oArray[ i ]).floatValue();
            }
            return pArray;
        }
        else if ( clazz == Integer.class ) {
            int[] pArray = new int[ nel ];
            for ( int i = 0; i < nel; i++ ) {
                pArray[ i ] = ((Integer) oArray[ i ]).intValue();
            }
            return pArray;
        }
        else if ( clazz == Long.class ) {
            long[] pArray = new long[ nel ];
            for ( int i = 0; i < nel; i++ ) {
                pArray[ i ] = ((Long) oArray[ i ]).longValue();
            }
            return pArray;
        }
        else {
            throw new IllegalArgumentException( "Can't unbox " + clazz );
        }
    }
}
