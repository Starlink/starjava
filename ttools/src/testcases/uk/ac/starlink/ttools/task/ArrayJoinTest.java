package uk.ac.starlink.ttools.task;

import java.lang.reflect.Array;
import java.util.logging.Level;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.ttools.TableTestCase;
import uk.ac.starlink.util.LogUtils;

public class ArrayJoinTest extends TableTestCase {

    public ArrayJoinTest() {
        LogUtils.getLogger( "uk.ac.starlink" ).setLevel( Level.WARNING );
    }

    public void testArrayJoin() throws Exception {
        exerciseArrayJoin( true );
        exerciseArrayJoin( false );
    }

    private void exerciseArrayJoin( boolean isCached ) throws Exception {
        MapEnvironment env = new MapEnvironment();
        env.setValue( "in", ":loop:6" );
        env.setValue( "atable", "$0%3==1?\"x\":(\":loop:\"+$0)" );
        env.setValue( "acmd", "addcol x (int)$0; addcol y 1.0*x*x" );
        env.setValue( "cache", Boolean.valueOf( isCached ) );
        env.setValue( "fixcols", "dups" );
        env.setValue( "suffixarray", "_array" );
        new ArrayJoin().createExecutable( env ).execute();
        StarTable tout = env.getOutputTable( "omode" );
        assertTrue( tout.isRandom() == isCached );
        Tables.checkTable( tout );
        tout = Tables.randomTable( tout );
        assertEquals( 6, tout.getRowCount() );
        assertArrayEquals( new String[] { "i", "i_array", "x", "y" },
                          getColNames( tout ) );

        Object[] row3 = tout.getRow( 3 );
        assertArrayEquals(
            new Object[] { Integer.valueOf( 3 ), null, null, null },
            row3 );

        Object[] row2 = tout.getRow( 2 );
        assertEquals( Integer.valueOf( 2 ), row2[ 0 ] );
        assertArrayEquals( new int[] { 0, 1, 2, }, row2[ 1 ] );
        assertArrayEquals( new int[] { 1, 2, 3, }, row2[ 2 ] );
        assertArrayEquals( new double[] { 1., 4., 9., }, row2[ 3 ] );

        for ( int ir = 4; ir < 6; ir++ ) {
            for ( int ic = 1; ic < 4; ic++ ) {
                assertEquals( ir + 1,
                              Array.getLength( tout.getCell( ir, ic ) ) );
            }
        }
    }
}
