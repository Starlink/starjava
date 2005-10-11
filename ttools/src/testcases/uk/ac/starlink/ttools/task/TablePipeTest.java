package uk.ac.starlink.ttools.task;

import java.io.IOException;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.ttools.TableTestCase;
import uk.ac.starlink.ttools.QuickTable;
import uk.ac.starlink.ttools.convert.SkySystem;
import uk.ac.starlink.ttools.convert.SkyUnits;

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
        StarTable result = env.getOutputTable( "omode" );
        if ( result != null ) {
            Tables.checkTable( result );
        }
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

        ColumnInfo sizeInfo = apply( "addcol -before 1"
                                   + " -ucd PHYS.SIZE"
                                   + " -desc 'Not important'"
                                   + " -units parsec" 
                                   + " SIZE 99.f" ).getColumnInfo( 0 );
        assertEquals( "SIZE", sizeInfo.getName() );
        assertEquals( "PHYS.SIZE", sizeInfo.getUCD() );
        assertEquals( "parsec", sizeInfo.getUnitString() );
        assertEquals( Float.class, sizeInfo.getContentClass() );
    }

    public void testAddskycoords() throws Exception {
        StarTable skyTable = new QuickTable( 3, new ColumnData[] {
            col( "ra", new double[] { 180, 90, 23 } ),
            col( "dec", new double[] { 45, -30, 23 } ),
        } );

        SkySystem[] systems = SkySystem.getKnownSystems();
        SkyUnits[] units = new SkyUnits[] {
            SkyUnits.getUnitsFor( "deg" ),
            SkyUnits.getUnitsFor( "rad" ),
            SkyUnits.getUnitsFor( "sex6" ),
        };
        for ( int i = 0; i < systems.length; i++ ) {
            SkySystem system = systems[ i ];
            for ( int j = 0; j < units.length; j++ ) {
                SkyUnits unit = units[ j ];
                String cmd = "addskycoords -inunit deg -outunit " + unit
                           + " fk5 " + system + " ra dec c1 c2"
                           + ";\n"
                           + "addskycoords -inunit " + unit + " -outunit deg "
                           + system + " fk5 c1 c2 rax decx";
                StarTable xTable = process( skyTable, cmd );
                assertArrayEquals(
                    new String[] { "ra", "dec", "c1", "c2", "rax", "decx", },
                    getColNames( xTable ) );
                assertArrayEquals( getColData( skyTable, 0 ),
                                   getColData( xTable, 0 ) );
                assertArrayEquals( getColData( skyTable, 1 ),
                                   getColData( xTable, 1 ) );
                assertArrayEquals( unbox( getColData( xTable, 0 ) ),
                                   unbox( getColData( xTable, 4 ) ), 1e-8 );
                assertArrayEquals( unbox( getColData( xTable, 1 ) ),
                                   unbox( getColData( xTable, 5 ) ), 1e-8 );
            }
        }

        StarTable xTable = process( skyTable,
            "addskycoords -inunit deg -outunit sex fk5 fk5 1 2 rax decx" );
        assertArrayEquals(
            new Object[] { "12:00:00.00", "06:00:00.00", "01:32:00.00", },
            getColData( xTable, 2 ) );
        assertArrayEquals(
            new Object[] { "+45:00:00.0", "-30:00:00.0", "+23:00:00.0", },
            getColData( xTable, 3 ) );

        StarTable xTable0 = process( skyTable,
            "addskycoords -inunit deg -outunit sex0 fk5 fk5 1 2 rax decx" );
        assertArrayEquals(
            new Object[] { "12:00:00", "06:00:00", "01:32:00", },
            getColData( xTable0, 2 ) );
        assertArrayEquals(
            new Object[] { "+45:00:00", "-30:00:00", "+23:00:00", },
            getColData( xTable0, 3 ) );

        StarTable xTable3 = process( skyTable,
            "addskycoords -inunit deg -outunit sex3 fk5 fk5 1 2 rax decx" );
        assertArrayEquals(
            new Object[] { "12:00:00.000", "06:00:00.000", "01:32:00.000", },
            getColData( xTable3, 2 ) );
        assertArrayEquals(
            new Object[] { "+45:00:00.00", "-30:00:00.00", "+23:00:00.00", },
            getColData( xTable3, 3 ) );

    }

    public void testBadval() throws Exception {
        assertArrayEquals( new Object[] { new Integer( 1 ),
                                          new Integer( 2 ),
                                          null,
                                          new Integer( 4 ) },
                           getColData( apply( "badval 3 *" ), 0 ) );

        assertArrayEquals( new Object[] { "Mark", null, "Taylor", null },
                           getColData( apply( "badval Beauchamp d" ), 3 ) );

        assertArrayEquals(
            new Object[] { new Float( 1f ), new Float( 2f ),
                           null, new Float( Float.NaN ), },
            getColData( apply( "addcol e (float)b/10; badval 3 *" ), 4 ) );
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

    public void testReplaceval() throws Exception {
        assertArrayEquals(
           new Object[] { "Mark", "Stupendous", "Taylor", "Rex" },
           getColData( apply( "replaceval Beauchamp Stupendous d;"
                            + "replaceval null Rex d;" ), 3 ) );

        assertArrayEquals(
           new Object[] { "Mark", null, "Taylor", null },
           getColData( apply( "replaceval Beauchamp null d" ), 3 ) );

        assertArrayEquals(
           box( new double[] { 10., 20., 30., Math.PI } ),
           getColData( apply( "replaceval null " + Math.PI + " b" ), 1 ) );

        assertArrayEquals(
           box( new double[] { 10., 20., 30., 40. } ),
           getColData( apply( "replaceval null " + Math.PI + " b;"
                            + "replaceval " + Math.PI + " 40 b" ), 1 ) );

        assertArrayEquals(
           box( new float[] { 10f, 20f, 30f, 40f } ),
           getColData( apply( "replacecol b (float)b;"
                            + "replaceval null " + Math.PI + " b;"
                            + "replaceval " + Math.PI + " 40 b" ), 1 ) );
    }

    public void testTablename() throws Exception {
        assertEquals( "Uns Table",
                      apply( "tablename 'Uns Table'" ).getName() );
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

        ColumnInfo info = apply( "replacecol"
                               + " -ucd UCD -units UNITS -desc DESCRIPTION"
                               + " -name NAME"
                               + " b '\"Message\"'" ).getColumnInfo( 1 );
        assertEquals( "NAME", info.getName() );
        assertEquals( "UCD", info.getUCD() );
        assertEquals( "UNITS", info.getUnitString() );
        assertEquals( "DESCRIPTION", info.getDescription() );
        assertEquals( String.class, info.getContentClass() );
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
        assertSameData( inTable_, apply( "sort $0" ) );
        assertSameData( inTable_, apply( "sort $1" ) );
        assertArrayEquals( new Object[] { "Beauchamp", "Mark", "Taylor", null },
                           getColData( apply( "sort d.charAt(2)" ), 3 ) );

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
    }

    public void testTail() throws Exception {
        assertSameData( inTable_, apply( "tail 4" ) );
        assertSameData( inTable_, apply( "tail 10000000" ) );
        assertArrayEquals( new Object[] { "Taylor", null },
                           getColData( apply( "tail 2" ), 3 ) );
    }

}
