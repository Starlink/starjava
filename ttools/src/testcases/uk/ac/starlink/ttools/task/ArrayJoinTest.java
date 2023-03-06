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
        exerciseArrayJoin( false, false );
        exerciseArrayJoin( false, true );
        exerciseArrayJoin( true, false );
        exerciseArrayJoin( true, true );
    }

    private void exerciseArrayJoin( boolean isCached, boolean keepAll )
            throws Exception {
        MapEnvironment env = new MapEnvironment();
        env.setValue( "in", ":loop:6" );
        env.setValue( "atable", "$0%3==1?\"x\":(\":loop:\"+$0)" );
        env.setValue( "acmd", String.join( ";",
            "addcol x (int)$0",
            "addcol y 1.0*x*x",
            "setparam foo 100",
            "setparam bar 0.5"
        ) );
        env.setValue( "keepall", Boolean.valueOf( keepAll ) );
        env.setValue( "cache", Boolean.valueOf( isCached ) );
        env.setValue( "fixcols", "dups" );
        env.setValue( "suffixarray", "_array" );
        MapEnvironment env0 = new MapEnvironment( env );
        new ArrayJoin().createExecutable( env ).execute();
        StarTable tout = env.getOutputTable( "omode" );
        assertTrue( tout.isRandom() == ( isCached && keepAll ) );
        Tables.checkTable( tout );
        assertArrayEquals( new int[] { -1 },
                           tout.getColumnInfo( 2 ).getShape() );
        tout = Tables.randomTable( tout );
        assertEquals( keepAll ? 6 : 4, tout.getRowCount() );
        assertArrayEquals( new String[] { "i", "i_array", "x", "y" },
                           getColNames( tout ) );

        Object[] row3 = tout.getRow( 3 );
        if ( keepAll ) {
            assertArrayEquals( row3,
                new Object[] { Integer.valueOf( 3 ), null, null, null } );
        }

        Object[] row4 = tout.getRow( keepAll ? 4 : 2 );
        assertEquals( Integer.valueOf( 4 ), row4[ 0 ] );
        assertArrayEquals( new int[] { 0, 1, 2, 3, 4, }, row4[ 1 ] );
        assertArrayEquals( new int[] { 1, 2, 3, 4, 5, }, row4[ 2 ] );
        assertArrayEquals( new double[] { 1., 4., 9., 16., 25., }, row4[ 3 ] );

        int na = 0;
        for ( int ir = 0; ir < tout.getRowCount(); ir++ ) {
            int jrow = ((Number) tout.getCell( ir, 0 )).intValue();
            boolean hasArrays = !keepAll || jrow % 3 != 0;
            if ( hasArrays ) {
                na++;
            }
            for ( int ic = 1; ic < 4; ic++ ) {
                Object array = tout.getCell( ir, ic );
                if ( hasArrays ) {
                    assertEquals( jrow + 1, Array.getLength( array ) );
                }
                else {
                    assertNull( array );
                }
            }
        }
        assertEquals( 4, na );

        MapEnvironment env1 = new MapEnvironment( env0 );
        env1.setValue( "aparams", "foo" );
        new ArrayJoin().createExecutable( env1 ).execute();
        StarTable tp1 = env1.getOutputTable( "omode" );
        Tables.checkTable( tp1 );
        assertArrayEquals( new String[] { "i", "i_array", "x", "y",
                                          "foo", },
                           getColNames( tp1 ) );
        tp1 = Tables.randomTable( tp1 );
        if ( keepAll ) {
            assertTrue( Tables.isBlank( tp1.getCell( 3, 4 ) ) );
        }
        else {
            assertEquals( Integer.valueOf( 100 ), tp1.getCell( 3, 4 ) );
        }

        MapEnvironment env2 = new MapEnvironment( env0 );
        env2.setValue( "aparams", "*" );
        new ArrayJoin().createExecutable( env2 ).execute();
        StarTable tp2 = env2.getOutputTable( "omode" );
        Tables.checkTable( tp2 );
        assertArrayEquals( new String[] { "i", "i_array", "x", "y",
                                          "foo", "bar", },
                           getColNames( tp2 ) );
        tp2 = Tables.randomTable( tp2 );
        if ( keepAll ) {
            assertTrue( Tables.isBlank( tp2.getCell( 3, 5 ) ) );
        }
        else {
            assertEquals( Double.valueOf( 0.5 ), tp2.getCell( 3, 5 ) );
        }

        MapEnvironment env3 = new MapEnvironment();
        env3.setValue( "in", ":loop:6" );
        env3.setValue( "atable", "$0%3==1?\"x\":(\":loop:4\")" );
        new ArrayJoin().createExecutable( env3 ).execute();
        StarTable tout3 = env3.getOutputTable( "omode" );
        if ( isCached ) {
            assertArrayEquals( new int[] { 4 },
                               tout3.getColumnInfo( 1 ).getShape() );
        }
    }
}
