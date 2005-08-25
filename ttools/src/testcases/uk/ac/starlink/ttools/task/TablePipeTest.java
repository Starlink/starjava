package uk.ac.starlink.ttools.task;

import java.io.IOException;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.ttools.TableTestCase;
import uk.ac.starlink.ttools.QuickTable;

public class TablePipeTest extends TableTestCase {

    final StarTable inTable_;

    public TablePipeTest( String name ) {
        super( name );

        inTable_ = new QuickTable( 4, new ColumnData[] {
            col( "a", new int[] { 1, 2, 3, 4 } ),
            col( "b", new double[] { 10., 20., 30., Double.NaN, } ),
            col( "c", new boolean[] { true, true, false, false, } ),
            col( "d", new String[] { "Mark", "Beauchamp", "Taylor", null, } ),
        } );
    }

    private StarTable apply( String cmd ) throws Exception {
        return process( inTable_, cmd );
    }

    private StarTable process( StarTable table, String cmd ) throws Exception {
        MapEnvironment env = new MapEnvironment()
                            .setValue( "in", table )
                            .setValue( "cmd", cmd );
        new TablePipe().createExecutable( env ).execute();
        StarTable result = env.getOutputTable( "mode" );
        Tables.checkTable( result );
        return result;
    }

    public void testBox() {
        Object[] a1 = new Double[] { new Double( Math.E ), };
        Object[] a2 = box( new double[] { Math.E, } );
        assertArrayEquals( new Object[] { new Double( Math.E ) },
                           box( new double[] { Math.E } ) );
    }

    public void testAddcol() throws Exception {
        assertArrayEquals(
            new String[] { "a", "b", "c", "d", "interloper", },
            getColNames( apply( "addcol interloper \"a + b\" " ) ) );

        assertArrayEquals(
            new String[] { "a", "interloper", "b", "c", "d", },
            getColNames( apply( "addcol -after a interloper a+b" ) ) );

        assertArrayEquals(
            new String[] { "a", "interloper", "b", "c", "d", },
            getColNames( apply( "addcol -before 2 interloper 'c'" ) ) );

        assertArrayEquals(
            box( new double[] { 11., 22., 33., Double.NaN, } ),
            getColData( apply( "addcol -before 1 XX 'a + b'" ), 0 ) );
    }

    public void testCache() throws Exception {
        assertSameData( inTable_, apply( "cache" ) );
    }

    public void testDelcols() throws Exception {
        assertArrayEquals(
            new String[] { "a", "b", "c", },
            getColNames( apply( "delcols d" ) ) );

        assertArrayEquals(
            new String[] { "a", "c" },
            getColNames( apply( "delcols 'b 4'" ) ) );

        assertArrayEquals(
            new String[] { "d" },
            getColNames( apply( "delcols 1; delcols 1; delcols 1" ) ) );
    }

    public void testEvery() throws Exception {
        assertSameData( inTable_, apply( "every 1" ) );
            
        assertArrayEquals(
            box( new int[] { 1, 3, } ),
            getColData( apply( "every 2" ), 0 ) );

        assertArrayEquals(
            new Object[] { "Mark", null },
            getColData( apply( "every 3" ), 3 ) );
    }

    public void testExplode() throws Exception {
        assertSameData( inTable_, apply( "explodeall" ) );

        try {
            apply( "explodecols 1" );
            fail();
        }
        catch ( IOException e ) {
            // OK - col 1 is not an array
        }

        StarTable multiTable = new QuickTable( 2, new ColumnData[] {
            col( "fix2", new int[][] {
                             new int[] { 101, 102 },
                             new int[] { 201, 202 },
                             new int[] { 301, 302 },
                         } ),
        } );
        ColumnInfo cinfo = multiTable.getColumnInfo( 0 );
        assertTrue( cinfo.isArray() );
        assertEquals( int[].class, cinfo.getContentClass() );

        /* Try with fixed-size array type. */
        cinfo.setShape( new int[] { 2 } );
        assertArrayEquals(
            new String[] { "fix2_1", "fix2_2", },
            getColNames( process( multiTable, "explodeall" ) ) );
        assertArrayEquals(
            new String[] { "fix2_1", "fix2_2", },
            getColNames( process( multiTable, "explodecols 1" ) ) );

        /* Try with variable-size array type - these columns cannot be
         * exploded. */
        cinfo.setShape( new int[] { -1 } );
        assertSameData( multiTable, process( multiTable, "explodeall" ) );
        try {
            process( multiTable, "explodecols 1" );
            fail();
        }
        catch ( IOException e ) {
            assertTrue( e.getMessage().indexOf( "not fixed" ) > 0 );
        }
    }

    public void testHead() throws Exception {
        assertSameData( inTable_, apply( "head 4" ) );
        assertSameData( inTable_, apply( "head 10000000" ) );
        assertArrayEquals( new Object[] { "Mark", "Beauchamp" },
                           getColData( apply( "head 2" ), 3 ) );
    }

    public void testKeepcols() throws Exception {
        assertSameData( inTable_, apply( "keepcols *" ) );
        assertSameData( inTable_, apply( "keepcols '1 2 3 4'" ) );
        assertArrayEquals( new String[] { "a", "b", "b", "a" },
                           getColNames( apply( "keepcols 'a b b a'" ) ) );
        assertArrayEquals( new String[] { "a", "b", "b", "a" },
                           getColNames( apply( "keepcols '1 2 2 1'" ) ) );
        StarTable dup = apply( "keepcols '3 3'" );
        assertArrayEquals( getColData( dup, 0 ), getColData( dup, 1 ) );
        assertArrayEquals( getColData( inTable_, 2 ), getColData( dup, 0 ) );
    }

    public void testTuningOptions() throws Exception {
        assertSameData( inTable_, apply( "random" ) );
        assertSameData( inTable_, apply( "sequential" ) );
        assertSameData( inTable_, apply( "random; sequential" ) );
        assertSameData( inTable_, apply( "random; sequential; cache;random" ) );
           // etc.
    }

    public void testReplaceCol() throws Exception {
        assertSameData( inTable_, apply( "replacecol a a" ) );
        StarTable fixed = apply( "replacecol c b-a" );
        assertArrayEquals( new String[] { "a", "b", "c", "d" },
                           getColNames( fixed ) );
        assertEquals( Boolean.class,
                      inTable_.getColumnInfo( 2 ).getContentClass() );
        assertEquals( Double.class,
                      fixed.getColumnInfo( 2 ).getContentClass() );
        assertArrayEquals( box( new double[] { 9., 18., 27., Double.NaN, } ),
                           getColData( fixed, 2 ) );
    }

    public void testSelect() throws Exception {
        assertSameData( inTable_, apply( "select true" ) );
        assertSameData( inTable_, apply( "select 'a < 1e8 && ! NULL_c'" ) );
        assertSameData( apply( "every 2" ),
                        apply( "select '$0 % 2 == 1'" ) );
        assertEquals( 0L, apply( "select false; cache" ).getRowCount() );
    }

    public void testSort() throws Exception {
        assertSameData( inTable_, apply( "sort a" ) );

        assertArrayEquals(
            box( new int[] { 1, 2, 3, 4 } ),
            getColData( apply( "sort b" ), 0 ) );
        assertArrayEquals(
            box( new int[] { 4, 3, 2, 1 } ),
            getColData( apply( "sort -down b" ), 0 ) );
        assertArrayEquals(
            box( new int[] { 4, 1, 2, 3 } ),
            getColData( apply( "sort -nullsfirst b" ), 0 ) );
        assertArrayEquals(
            box( new int[] { 3, 2, 1, 4 } ),
            getColData( apply( "sort -nullsfirst -down b" ), 0 ) );

        StarTable with = apply( "addcol extra '($0+1)/2'" );
        assertArrayEquals( box( new long[] { 1, 1, 2, 2 } ),
                           getColData( with, 4 ) );
        assertArrayEquals( box( new int[] { 1, 2, 3, 4 } ),
                           getColData( process( with, "sort extra" ), 0 ) );
        assertArrayEquals( box( new int[] { 2, 1, 3, 4 } ),
                           getColData( process( with, "sort 'extra d'" ), 0 ) );
    }

    public void testSortexpr() throws Exception {
        assertSameData( inTable_, apply( "sortexpr $0" ) );
        assertSameData( inTable_, apply( "sortexpr $1" ) );
        assertArrayEquals( new Object[] { "Beauchamp", "Mark", "Taylor", null },
                           getColData( apply( "sortexpr d.charAt(2)" ), 3 ) );

        assertArrayEquals(
            box( new int[] { 1, 2, 3, 4 } ),
            getColData( apply( "sortexpr b" ), 0 ) );
        assertArrayEquals(
            box( new int[] { 4, 3, 2, 1 } ),
            getColData( apply( "sortexpr -down b" ), 0 ) );
        assertArrayEquals(
            box( new int[] { 4, 1, 2, 3 } ),
            getColData( apply( "sortexpr -nullsfirst b" ), 0 ) );
        assertArrayEquals(
            box( new int[] { 3, 2, 1, 4 } ),
            getColData( apply( "sortexpr -nullsfirst -down b" ), 0 ) );
    }

    public void testTail() throws Exception {
        assertSameData( inTable_, apply( "tail 4" ) );
        assertSameData( inTable_, apply( "tail 10000000" ) );
        assertArrayEquals( new Object[] { "Taylor", null },
                           getColData( apply( "tail 2" ), 3 ) );
    }

}
