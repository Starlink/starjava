package uk.ac.starlink.ttools.task;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.TableTestCase;
import uk.ac.starlink.ttools.QuickTable;
import uk.ac.starlink.ttools.convert.SkySystem;
import uk.ac.starlink.ttools.convert.SkyUnits;
import uk.ac.starlink.ttools.filter.ArgException;
import uk.ac.starlink.ttools.filter.AssertException;

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

        Logger.getLogger( "uk.ac.starlink.ttools.filter" )
              .setLevel( Level.WARNING );
        Logger.getLogger( "uk.ac.starlink.table.storage" )
              .setLevel( Level.WARNING );
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

        assertArrayEquals(
            box( new int[] { 5, 5, 5, 5, } ),
            getColData( apply( "setparam fiver 5;"
                             + "addcol -before 1 COL_FIVE param$fiver" ), 0 ) );

        assertArrayEquals(
            box( new int[] { 5, 5, 5, 5, } ),
            getColData( apply( "setparam fiver 5;"
                             + "addcol -before 1 COL_FIVE PARAM$fiver" ), 0 ) );

        try {
            apply( "setparam fiver 5;"
                 + "clearparams fiver;"
                 + "addcol -before 1 COL_FIVE param$fiver" );
            fail();
        }
        catch ( IOException e ) {
            assertTrue( e.getMessage().indexOf( "param$fiver" ) >= 0 );
        }

        StarTable withSize = apply( "addcol -before 1"
                                  + " -ucd PHYS.SIZE"
                                  + " -desc 'Not important'"
                                  + " -units parsec" 
                                  + " SIZE 99.f" );
        ColumnInfo sizeInfo = withSize.getColumnInfo( 0 );
        assertEquals( "SIZE", sizeInfo.getName() );
        assertEquals( "PHYS.SIZE", sizeInfo.getUCD() );
        assertEquals( "parsec", sizeInfo.getUnitString() );
        assertEquals( Float.class, sizeInfo.getContentClass() );

        assertEquals( new Float( 99f ),
                      process( withSize, "keepcols ucd$PHYS_SIZE" )
                     .getCell( 1L, 0 ) );
        assertEquals( new Float( 99f ),
                      process( withSize, "keepcols ucd$PHYS_" )
                     .getCell( 1L, 0 ) );

        assertEquals(
            new Double( 100 ),
            process( withSize,
                     "addcol sizzle ucd$PHYS_SIZE+1.; keepcols sizzle" )
                     .getCell( 2L, 0 ) );
        assertEquals(
            new Double( 100 ),
            process( withSize,
                     "addcol sizzle ucd$PHYS_+1.; keepcols sizzle" )
                     .getCell( 2L, 0 ) );

        StarTable justSize = process( withSize, "keepcols ucd$PHYS_SIZE" );
        ColumnInfo sInfo = justSize.getColumnInfo( 0 );
        assertEquals( "SIZE", sInfo.getName() );
        assertEquals( "PHYS.SIZE", sInfo.getUCD() );
        assertEquals( "parsec", sInfo.getUnitString() );
        assertEquals( Float.class, sInfo.getContentClass() );
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
                           + " fk5 " + system.getName() + " ra dec c1 c2"
                           + ";\n"
                           + "addskycoords -inunit " + unit + " -outunit deg "
                           + system.getName() + " fk5 c1 c2 rax decx";
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

    public void testAssert() throws Exception {
        apply( "select index<4; assert '((int) a+b) % 11 == 0'" );
        try {
            apply( "select index<4; assert '((int) a+b) % 12 == 0'" );
            fail();
        }
        catch ( AssertException e ) {
        }
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

    public void testClearparams() throws Exception {
        assertNotNull(
            apply( "setparam flavour smokeybacon" )
           .getParameterByName( "flavour" ) );
        assertNotNull(
            apply( "setparam flavour smokeybacon; clearparams taste" )
           .getParameterByName( "flavour" ) );
        assertNotNull(
            apply( "setparam flavour smokeybacon; clearparams t*" )
           .getParameterByName( "flavour" ) );
        assertNull(
            apply( "setparam flavour smokeybacon; clearparams flavour" )
           .getParameterByName( "flavour" ) );
        assertNull(
            apply( "setparam flavour smokeybacon; clearparams *" )
           .getParameterByName( "flavour" ) );
        assertNull(
            apply( "setparam flavour smokeybacon; clearparams f*r" )
           .getParameterByName( "flavour" ) );

        assertEquals(
            2,
            apply( "clearparams *; setparam xx 100; setparam yx 200" )
           .getParameters().size() );
        assertEquals(
            2,
            apply( "clearparams *; setparam xx 100; setparam yx 200;"
                 + "clearparams x" ) 
           .getParameters().size() );
        assertEquals(
            1,
            apply( "clearparams *; setparam xx 100; setparam yx 200;"
                 + "clearparams xx" )
           .getParameters().size() );
        assertEquals(
            0,
            apply( "clearparams *; setparam xx 100; setparam yx 200;"
                 + "clearparams *x" )
           .getParameters().size() );
    }

    public void testColmeta() throws Exception {
        assertArrayEquals(
            new String[] { "Index", "Name", "Class" },
            getColNames( apply( "colmeta c; meta" ) ) );
        assertArrayEquals(
            new String[] { "Index", "Name", "Class", "UCD", "UCD_desc" },
            getColNames( apply( "colmeta -ucd TIME_EPOCH a; meta" ) ) );
        assertArrayEquals(
            new String[] { "Index", "Name", "Class", "Units", "Description" },
            getColNames( apply( "colmeta  -units m -desc 'some numbers' c;"
                              + "meta" ) ) );
        assertArrayEquals(
            new String[] { "x", "b", "c", "x" },
            getColNames( apply( "colmeta -name rename_a a;" +
                                "colmeta -name rename_d d;" +
                                "colmeta -name x rename_*;" ) ) );
        try {
            apply( "colmeta -do_what a" );
            fail();
        }
        catch ( TaskException e ) {
            assert e.getCause() instanceof ArgException;
        }
    }

    public void testMeta() throws Exception {
        assertArrayEquals(
            new String[] { "Index", "Name", "Class" },
            getColNames( apply( "meta" ) ) );
        assertArrayEquals(
            new String[] { "Index", "Name", "Class", "Units", },
            getColNames( apply( "addcol -units feet bf b; meta" ) ) );
        assertArrayEquals(
            new String[] { "colour", "Name", "smell", "taste", },
            getColNames( apply( "meta colour name smell taste" ) ) );
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

    public void testFixNames() throws Exception {
        assertSameData( inTable_, apply( "fixcolnames" ) );
        StarTable t1 = apply( "addcol 'a b c' 99" );
        t1.setParameter( new DescribedValue(
                             new DefaultValueInfo( "d e f", Integer.class ),
                             new Integer( 2112 ) ) );
        StarTable t2 = process( t1, "fixcolnames" );
        assertEquals( "a_b_c", getColNames( t2 )[ inTable_.getColumnCount() ] );
        assertEquals( new Integer( 2112 ),
                      t2.getParameterByName( "d_e_f" ).getValue() );
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

    public void testRepeat() throws Exception {
        long nrow = inTable_.getRowCount();
        assertTrue( nrow > 2 );
        assertSameData( inTable_, apply( "repeat 1" ) );
        assertSameData( inTable_, apply( "repeat 1000000; head " + nrow ) );
        assertSameData( inTable_, apply( "repeat 999; tail " + nrow ) );
        assertSameData( inTable_,
                        apply( "repeat 10; rowrange " + ( nrow + 1 )
                                                      + " +" + nrow ) );
        assertSameData( inTable_,
                        apply( "repeat 10; rowrange " + ( nrow * 4 + 1 )
                                                      + " +" + nrow ) );
        assertEquals( nrow * 99, apply( "repeat 99" ).getRowCount() );
        assertEquals( 0, apply( "repeat 0" ).getRowCount() );
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

        assertArrayEquals(
           box( new double[] { 99, 1, 0.5, 1./3. } ),
           getColData( apply( "addcol r_a 1.0/(a-1);"
                            + "replaceval Infinity 99 r_a" ), 4 ) );

        assertArrayEquals(
           box( new float[] { -99f, -1f, -0.5f, -1f/3f } ),
           getColData( apply( "addcol mr_a -1f/(a-1);"
                            + "replaceval -Infinity -99 mr_a" ), 4 ) );
    }

    public void testSetparam() throws Exception {
        DescribedValue dval =
            apply( "setparam -descrip 'What it tastes like' "
                           + "flavour salt+vinegar" )
           .getParameterByName( "flavour" );
        assertEquals( "flavour", dval.getInfo().getName() );
        assertEquals( "What it tastes like", dval.getInfo().getDescription() );
        assertEquals( String.class, dval.getInfo().getContentClass() );
        assertEquals( "salt+vinegar", dval.getValue() );

        assertEquals(
            new Double( 3.1 ),
            apply( "setparam x 3.1" ).getParameterByName( "x" ).getValue() );
        assertEquals(
            new Integer( 19 ),
            apply( "setparam x 19" ).getParameterByName( "x" ).getValue() );
        assertEquals(
            Boolean.FALSE,
            apply( "setparam x false" ).getParameterByName( "x" ).getValue() );

        assertEquals(
            new Double( 3.0 ),
            apply( "setparam -type double x 3" ).getParameterByName( "x" )
                                                .getValue() );
        assertEquals(
            new Float( 3.0f ),
            apply( "setparam -type float x 3" ).getParameterByName( "x" )
                                               .getValue() );
        assertEquals(
            new Long( 3L ),
            apply( "setparam -type long x 3" ).getParameterByName( "x" )
                                              .getValue() );
        assertEquals(
            new Integer( 3 ),
            apply( "setparam -type int x 3" ).getParameterByName( "x" )
                                             .getValue() );
        assertEquals(
            new Short( (short) 3 ),
            apply( "setparam -type short x 3" ).getParameterByName( "x" )
                                               .getValue() );
        assertEquals(
            new Byte( (byte) 3 ),
            apply( "setparam -type byte x 3" ).getParameterByName( "x" )
                                              .getValue() );
        assertEquals(
            "3",
            apply( "setparam -type string x 3" ).getParameterByName( "x" )
                                                .getValue() );
        try {
            apply( "setparam -type int 3.1415" );
            fail();
        }
        catch ( TaskException e ) {
            assertTrue( e.getCause() instanceof ArgException );
        }
    }

    public void testTablename() throws Exception {
        assertEquals( "Uns Table",
                      apply( "tablename 'Uns Table'" ).getName() );
    }

    public void testTuningOptions() throws Exception {
        assertSameData( inTable_, apply( "randomview" ) );
        assertSameData( inTable_, apply( "seqview" ) );
        assertSameData( inTable_, apply( "randomview; seqview" ) );
        assertSameData( inTable_, apply( "randomview; seqview; "
                                       + "cache; randomview" ) );
           // etc.
    }

    public void testRandom() throws Exception {
        try {
            apply( "seqview; randomview; every 1" );
            fail();
        }
        catch ( IOException e ) {
        }
        assertSameData( inTable_,
                        apply( "seqview; random; randomview; every 1" ) );
        assertSameData( inTable_,
                        apply( "seqview; cache; randomview; every 1" ) );
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

    public void testRowRange() throws Exception {
        assertSameData( inTable_, apply( "rowrange 1 4" ) );
        assertSameData( inTable_, apply( "rowrange 1 999" ) );
        assertArrayEquals( new Object[] { "Beauchamp", "Taylor", },
                           getColData( apply( "rowrange 2 3" ), 3 ) );
        assertSameData( apply( "rowrange 2 4" ), apply( "rowrange 2 +3" ) );
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

        assertArrayEquals( box( new int[] { 1, 2, 3, 4 } ),
                           getColData( apply( "sort ($0+1)/2" ), 0 ) );
        assertArrayEquals( box( new int[] { 2, 1, 3, 4 } ),
                           getColData( apply( "sort '($0+1)/2 d'" ), 0 ) );

        assertArrayEquals( new Object[] { "Beauchamp", "Mark", "Taylor", null },
                           getColData( apply( "sort d.charAt(2)" ), 3 ) );

        assertSameData( inTable_, apply( "sort $0" ) );
        assertSameData( inTable_, apply( "sort $1" ) );
        assertSameData( inTable_, apply( "sort '$0 $1'" ) );
    }

    public void testSortHead() throws Exception {
        workSortHead( inTable_, "", "d" );
        workSortHead( inTable_, "-down", "a b" );
    }

    private void workSortHead( StarTable table, String flags, String keys )
            throws Exception {
        StarTable sorted = process( table, 
                                    "sort " + flags + " '" + keys + "'" );
        for ( int i = 1; i <= table.getRowCount(); i++ ) {
            assertSameData( process( sorted, "head " + i ),
                            process( table, "sorthead " + flags + " " + i 
                                          + " '" + keys + "'" ) );
            assertSameData( process( sorted, "tail " + i ),
                            process( table, "sorthead -tail " + flags + " " + i
                                          + " '" + keys + "'" ) );
        }
    }

    public void testStats() throws Exception {
        assertArrayEquals(
            new Object[] { "a", "b", "c", "d", },
            getColData( apply( "stats name mean variance" ), 0 ) );
        assertArrayEquals(
            new float[] { 2.5f, 20.0f, 0.5f, Float.NaN, },
            unbox( getColData( apply( "stats name mean variance" ), 1 ) ) );
        assertArrayEquals(
            new float[] { 1.25f, (float)(200/3.), Float.NaN, Float.NaN },
            unbox( getColData( apply( "stats name mean variance" ), 2 ) ),
            1e-6 );
        assertArrayEquals(
            unbox( getColData( apply( "stats stdev" ), 0 ) ),
            unbox( getColData( apply( "stats variance;"
                               + "replacecol variance (float)sqrt(variance)" ),
                               0 ) ) );
        assertArrayEquals(
            new float[] { -0.237960f, -0.159546f, Float.NaN, Float.NaN, },
            unbox( getColData( apply( "replacecol a sqrt(a);"
                                    + "replacecol b sqrt(b);" 
                                    + "stats skew" ),
                               0 ) ),
            1e-5 );
        assertArrayEquals(
            new float[] { -1.307984f, -1.5f, Float.NaN, Float.NaN, },
            unbox( getColData( apply( "replacecol a sqrt(a);"
                                    + "replacecol b sqrt(b);"
                                    + "stats kurtosis" ), 0 ) ),
            1e-5 );
        assertArrayEquals(
            new long[] { 4L, 3L, 4L, 3L },
            unbox( getColData( apply( "stats ngood" ), 0 ) ) );
        assertArrayEquals(
            new long[] { 0L, 1L, 0L, 1L },
            unbox( getColData( apply( "stats nbad" ), 0 ) ) );
        assertArrayEquals(
            new Object[] { new Integer( 1 ), new Double( 10. ), null, null, },
            getColData( apply( "stats minimum" ), 0 ) );
        assertArrayEquals(
            new Object[] { new Integer( 4 ), new Double( 30. ), null, null, },
            getColData( apply( "stats maximum" ), 0 ) );
        assertArrayEquals(
            new double[] { 10., 60., 2., Double.NaN, },
            unbox( getColData( apply( "stats sum" ), 0 ) ) );
        assertArrayEquals(
            new Object[] { new Long( 1 ), new Long( 1 ), null, null, },
            getColData( apply( "stats minpos" ), 0 ) );
        assertArrayEquals(
            new Object[] { new Long( 4 ), new Long( 3 ), null, null, },
            getColData( apply( "stats maxpos" ), 0 ) );
        assertArrayEquals(
            new int[] { 4, 3, 2, 3 },
            unbox( getColData( apply( "stats cardinality" ), 0 ) ) );

        // n.b. the Floats here could/should be Doubles.
        assertArrayEquals(
            new Object[] { new Integer( 1 ), new Float( 10. ), null, null, },
            getColData( apply( "stats q.01 median q.99" ), 0 ) );
        assertArrayEquals(
            new Object[] { new Integer( 3 ), new Float( 20. ), null, null, },
            getColData( apply( "stats q.01 median q.99" ), 1 ) );
        assertArrayEquals(
            new Object[] { new Integer( 4 ), new Float( 30. ), null, null, },
            getColData( apply( "stats q.01 median q.99" ), 2 ) );

        assertArrayEquals(
            new String[] { "Name", "Mean", "StDev", "Minimum",
                           "Maximum", "NGood", },
            getColNames( apply( "stats" ) ) );
    }

    public void testTail() throws Exception {
        assertSameData( inTable_, apply( "tail 4" ) );
        assertSameData( inTable_, apply( "tail 10000000" ) );
        assertArrayEquals( new Object[] { "Taylor", null },
                           getColData( apply( "tail 2" ), 3 ) );
    }

    public void testUniq() throws Exception {
        assertEquals( 4L, Tables.randomTable( apply( "uniq" ) )
                         .getRowCount() );
        assertEquals( 2L, Tables.randomTable( apply( "uniq c" ) )
                         .getRowCount() );
        assertArrayEquals(
            new int[] { 1, 1, 1, 1, },
            unbox( getColData( apply( "uniq -count" ), 0 ) ) );
        assertArrayEquals(
            new int[] { 2, 2 },
            unbox( getColData( apply( "uniq -count c" ), 0 ) ) );
        assertArrayEquals(
            new String[] { "a", "b", "c", "d", },
            getColNames( apply( "uniq" ) ) );
        assertArrayEquals(
            new String[] { "DupCount", "a", "b", "c", "d" },
            getColNames( apply( "uniq -count" ) ) );
    }

}
