package uk.ac.starlink.ttools.task;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.logging.Level;
import uk.ac.starlink.fits.AbstractFitsTableWriter;
import uk.ac.starlink.fits.FitsHeader;
import uk.ac.starlink.fits.FitsUtil;
import uk.ac.starlink.fits.HealpixFitsTableWriter;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.LoopTableScheme;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.TableTestCase;
import uk.ac.starlink.ttools.QuickTable;
import uk.ac.starlink.ttools.convert.SkySystem;
import uk.ac.starlink.ttools.convert.SkyUnits;
import uk.ac.starlink.ttools.filter.ArgException;
import uk.ac.starlink.ttools.filter.AssertException;
import uk.ac.starlink.ttools.scheme.SkySimScheme;
import uk.ac.starlink.util.LogUtils;
import uk.ac.starlink.votable.UnifiedFitsTableWriter;

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

        LogUtils.getLogger( "uk.ac.starlink.ttools.filter" )
                .setLevel( Level.SEVERE );
        LogUtils.getLogger( "uk.ac.starlink.table.storage" )
                .setLevel( Level.WARNING );
        LogUtils.getLogger( "uk.ac.starlink.fits" )
                .setLevel( Level.WARNING );
        LogUtils.getLogger( "uk.ac.starlink.util" )
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
        Object[] a1 = new Double[] { Double.valueOf( Math.E ), };
        Object[] a2 = box( new double[] { Math.E, } );
        assertArrayEquals( new Object[] { Double.valueOf( Math.E ) },
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
                                  + " -utype tc:size"
                                  + " -xtype tc:Chubba"
                                  + " -desc 'Not important'"
                                  + " -units parsec" 
                                  + " SIZE 99.f" );
        ColumnInfo sizeInfo = withSize.getColumnInfo( 0 );
        assertEquals( "SIZE", sizeInfo.getName() );
        assertEquals( "PHYS.SIZE", sizeInfo.getUCD() );
        assertEquals( "tc:size", sizeInfo.getUtype() );
        assertEquals( "tc:Chubba", sizeInfo.getXtype() );
        assertEquals( "parsec", sizeInfo.getUnitString() );
        assertEquals( Float.class, sizeInfo.getContentClass() );

        assertEquals( Float.valueOf( 99f ),
                      process( withSize, "keepcols ucd$PHYS_SIZE" )
                     .getCell( 1L, 0 ) );
        assertEquals( Float.valueOf( 99f ),
                      process( withSize, "keepcols ucd$PHYS_" )
                     .getCell( 1L, 0 ) );

        assertEquals(
            Double.valueOf( 100 ),
            process( withSize,
                     "addcol sizzle ucd$PHYS_SIZE+1.; keepcols sizzle" )
                     .getCell( 2L, 0 ) );
        assertEquals(
            Double.valueOf( 100 ),
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
        apply( "select index<4; assert '((int) a+b) % 11 == 0'" );
        try {
            apply( "select index<4;"
                 + "assert '((int) a+b) % 12 == 0' "
                        + "'\"Value(\"+a+\",\"+b+\")\"'" );
            fail();
        }
        catch ( AssertException e ) {
            assertTrue( e.getMessage().indexOf( "row 1" ) > 0 );
            assertTrue( e.getMessage().indexOf( "Value(1,10.0)" ) > 0 );
        }
    }

    public void testBadval() throws Exception {
        assertArrayEquals( new Object[] { Integer.valueOf( 1 ),
                                          Integer.valueOf( 2 ),
                                          null,
                                          Integer.valueOf( 4 ) },
                           getColData( apply( "badval 3 *" ), 0 ) );

        assertArrayEquals( new Object[] { "Mark", null, "Taylor", null },
                           getColData( apply( "badval Beauchamp d" ), 3 ) );

        assertArrayEquals(
            new Object[] { Float.valueOf( 1f ), Float.valueOf( 2f ),
                           null, Float.valueOf( Float.NaN ), },
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
            new String[] { "Index", "Name", "Class", "UCD" },
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

    public void testConstcol() throws Exception {
        int nr = 10_000;
        StarTable t1 =
            process( new LoopTableScheme().createTable( Integer.toString( nr )),
                     String.join( ";",
                         "addcol flag $0%100==99",
                         "addcol c1 23.0",
                         "addcol c2 NaN",
                         "addcol ai1 intArray(1,2,3,4)",
                         "addcol ai2 intArray(0,(int)$0,0)",
                         "addcol ad1 array(0,1,PI,E,NaN)",
                         "addcol ad3 flag?ad1:array(NaN,NaN,NaN,NaN,NaN)",
                         "addcol c3 flag?(int)99:NULL"
                     ) );
        StarTable t2 = process( t1, "constcol -parallel -noacceptnull" );
        assertArrayEquals( new String[] { "i", "flag",  "ai2", "ad3", "c3" },
                           getColNames( t2 ) );
        assertArrayEquals( new int[] { 1, 2, 3, 4, },
                           t2.getParameterByName( "ai1" ).getValue() );
        StarTable t3 = process( t1, "constcol -parallel -acceptnull 'a* c3'" );
        assertArrayEquals( new String[] { "i", "flag",
                                          "c1", "c2", "ai2", "ad3", },
                           getColNames( t3 ) );
        assertEquals( 99, ((Integer) t3.getParameterByName( "c3" ).getValue())
                         .intValue() );
        StarTable t4 = new SkySimScheme().createTable( "10000" );
        assertSameData( t4, process( t4, "constcol -parallel" ) );
        assertSameData( t4, process( t4, "constcol -noparallel" ) );
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

    public void testHealpixMetadata() throws Exception {
        StarTable pixTable = new QuickTable( 12, new ColumnData[] {
            col( "HPX0", new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, } ),
            col( "VALUE", new float[] { 5, 6, 7, 6, 5, 4, 3, 2, 1, 2, 3, 4, } ),
        } );
        StarTable p1 =
            process( pixTable,
                     "healpixmeta -level 0 -implicit -csys G -nested" );
        {
            UnifiedFitsTableWriter writer = new UnifiedFitsTableWriter();
            writer.setPrimaryType( UnifiedFitsTableWriter.PrimaryType.BASIC );
            FitsHeader hdr1 = getFitsHeaders( p1, writer );
            assertEquals( "HEALPIX", hdr1.getStringValue( "PIXTYPE" ) );
            assertEquals( "NESTED", hdr1.getStringValue( "ORDERING" ) );
            assertEquals( "G", hdr1.getStringValue( "COORDSYS" ) );
            assertEquals( "IMPLICIT", hdr1.getStringValue( "INDXSCHM" ) );
            assertEquals( Integer.valueOf( 1 ), hdr1.getIntValue( "NSIDE" ) );
            assertEquals( Integer.valueOf( 0 ),
                          hdr1.getIntValue( "FIRSTPIX" ) );
            assertEquals( Integer.valueOf( 11 ),
                          hdr1.getIntValue( "LASTPIX" ) );
            assertEquals( null, hdr1.getIntValue( "OBS_NPIX" ) );
        }
        StarTable p2 = process( p1, "healpixmeta -csys c -column hpx0" );
        {
            UnifiedFitsTableWriter writer = new UnifiedFitsTableWriter();
            writer.setPrimaryType( UnifiedFitsTableWriter.VOTABLE_PRIMARY_TYPE);
            FitsHeader hdr2 = getFitsHeaders( p2, writer );
            assertEquals( "HEALPIX", hdr2.getStringValue( "PIXTYPE" ) );
            assertEquals( "NESTED", hdr2.getStringValue( "ORDERING" ) );
            assertEquals( "C", hdr2.getStringValue( "COORDSYS" ) );
            assertEquals( "EXPLICIT", hdr2.getStringValue( "INDXSCHM" ) );
            assertEquals( Integer.valueOf( 1 ), hdr2.getIntValue( "NSIDE" ) );
            assertEquals( Integer.valueOf( 12 ),
                          hdr2.getIntValue( "OBS_NPIX" ) );
            assertEquals( null, hdr2.getIntValue( "FIRSTPIX" ) );
        }
        StarTable p3 = process( pixTable,
                                "keepcols 'VALUE HPX0';"
                              + "healpixmeta -level 0 -column HPX0 -ring" );
        {
            FitsHeader hdr3 =
                getFitsHeaders( p3, new HealpixFitsTableWriter() );
            assertEquals( Integer.valueOf( 2 ), hdr3.getIntValue( "TFIELDS" ) );
            assertEquals( "PIXEL", hdr3.getStringValue( "TTYPE1" ) );
            assertEquals( "VALUE", hdr3.getStringValue( "TTYPE2" ) );
            assertEquals( "HEALPIX", hdr3.getStringValue( "PIXTYPE" ) );
            assertEquals( "RING", hdr3.getStringValue( "ORDERING" ) );
            assertEquals( null, hdr3.getStringValue( "COORDSYS" ) );
            assertEquals( "EXPLICIT", hdr3.getStringValue( "INDXSCHM" ) );
            assertEquals( Integer.valueOf( 1 ), hdr3.getIntValue( "NSIDE" ) );
            assertEquals( Integer.valueOf( 12 ),
                          hdr3.getIntValue( "OBS_NPIX" ) );
            assertEquals( null, hdr3.getIntValue( "FIRSTPIX" ) );
        }
    }

    private static FitsHeader
            getFitsHeaders( StarTable table,
                            AbstractFitsTableWriter fitsWriter )
            throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        fitsWriter.writeStarTable( table, bout );
        bout.close();
        byte[] buf = bout.toByteArray();
        try ( InputStream in = new ByteArrayInputStream( buf ) ) {
            FitsUtil.skipHDUs( in, 1 );
            return FitsUtil.readHeader( in );
        }
    }

    public void testImplode() throws Exception {
        assertArrayEquals(
            new Object[] { new int[] {1,10}, new int[] {2,20},
                           new int[] {3,30}, new int[] {4,0} },
            getColData( apply( "collapsecols ab a 2" ), 0 ) );
        assertArrayEquals(
            new Object[] { new double[] {10,1}, new double[] {20,2},
                           new double[] {30,3}, new double[] {Double.NaN,4} },
            getColData( apply( "keepcols \"b a\";"
                             + "collapsecols -keepscalars ba b 2" ), 2 ) );
        assertArrayEquals(
            new Object[] { new String[] { "Ma", "Mark" },
                           new String[] { "Be", "Beauchamp" },
                           new String[] { "Ta", "Taylor" },
                           new String[] { null, null }, },
            getColData( apply( "addcol xx substring(d,0,2);"
                             + "keepcols \"xx d\";"
                             + "collapsecols xxd $1 2" ), 0 ) );
        assertArrayEquals(
            new Object[] { new boolean[] { true, false },
                           new boolean[] { true, false },
                           new boolean[] { false, true },
                           new boolean[] { false, true }, },
            getColData( apply( "addcol -after c nc !c;"
                             + "collapsecols flags c 2" ), 2 ) );
    }

    public void testFixNames() throws Exception {
        assertSameData( inTable_, apply( "fixcolnames" ) );
        StarTable t1 = apply( "addcol 'a b c' 99;"
                            + "addcol XX 20;"
                            + "addcol xx 21" );
        t1.setParameter( new DescribedValue(
                             new DefaultValueInfo( "d e f", Integer.class ),
                             Integer.valueOf( 2112 ) ) );
        StarTable t2 = process( t1, "fixcolnames" );
        int ncol0 = inTable_.getColumnCount();
        assertEquals( "a_b_c", getColNames( t2 )[ ncol0 ] );
        assertEquals( "XX", getColNames( t2 )[ ncol0 + 1 ] );
        assertEquals( "xx_1", getColNames( t2 )[ ncol0 + 2 ] );
        assertEquals( Integer.valueOf( 2112 ),
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
        assertSameData( inTable_, apply( "keepcols 1-4" ) );
        assertSameData( inTable_, apply( "keepcols 1-" ) );
        assertSameData( inTable_, apply( "keepcols -$4" ) );
        assertArrayEquals( new String[] { "a", "b", "b", "a" },
                           getColNames( apply( "keepcols 'a b b a'" ) ) );
        assertArrayEquals( new String[] { "a", "b", "b", "a" },
                           getColNames( apply( "keepcols '1 2 2 1'" ) ) );
        assertArrayEquals( new String[] { "b", "c", "d", "c", },
                           getColNames( apply( "keepcols 'b-d c'" ) ) );
        StarTable dup = apply( "keepcols '3 3'" );
        assertArrayEquals( getColData( dup, 0 ), getColData( dup, 1 ) );
        assertArrayEquals( getColData( inTable_, 2 ), getColData( dup, 0 ) );
    }

    public void testRepeat() throws Exception {
        long nrow = inTable_.getRowCount();
        assertTrue( nrow > 2 );
        assertSameData( inTable_, apply( "repeat 1" ) );
        assertSameData( inTable_, apply( "repeat 1000000; head " + nrow ) );
        assertSameData( inTable_, apply( "repeat -table 999; tail " + nrow ) );
        assertSameData( inTable_,
                        apply( "repeat 10; rowrange " + ( nrow + 1 )
                                                      + " +" + nrow ) );
        assertSameData( inTable_,
                        apply( "repeat 10; rowrange " + ( nrow * 4 + 1 )
                                                      + " +" + nrow ) );
        assertSameData( inTable_,
                        apply( "repeat -row 3*3; every 9" ) );
        assertEquals( nrow * 99, apply( "repeat -table 99" ).getRowCount() );
        assertEquals( nrow * 99, apply( "repeat -row 99" ).getRowCount() );
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
            Long.valueOf( 23 ),
            apply( "setparam -type long x 20; setparam y 3+param$x" )
           .getParameterByName( "y" ).getValue() );

        assertEquals(
            Double.valueOf( 3.1 ),
            apply( "setparam x 3.1" ).getParameterByName( "x" ).getValue() );
        assertEquals(
            Integer.valueOf( 19 ),
            apply( "setparam x 19" ).getParameterByName( "x" ).getValue() );
        assertEquals(
            Boolean.FALSE,
            apply( "setparam x false" ).getParameterByName( "x" ).getValue() );

        assertEquals(
            Double.valueOf( 1.0 ),
            apply( "setparam -type double x cos(0)" ).getParameterByName( "x" )
                                                     .getValue() );
        assertEquals(
            Float.valueOf( 3.0f ),
            apply( "setparam -type float x 3" ).getParameterByName( "x" )
                                               .getValue() );
        assertEquals(
            Long.valueOf( 3L ),
            apply( "setparam -type long x 3" ).getParameterByName( "x" )
                                              .getValue() );
        assertEquals(
            Integer.valueOf( 3 ),
            apply( "setparam -type int x 1+1+1" ).getParameterByName( "x" )
                                             .getValue() );
        assertEquals(
            Short.valueOf( (short) 3 ),
            apply( "setparam -type short x 3" ).getParameterByName( "x" )
                                               .getValue() );
        assertEquals(
            Byte.valueOf( (byte) 3 ),
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

    public void testRandomNumbersIndexed() throws Exception {
        int nr = 10_000;
        StarTable rtable =
            process( new LoopTableScheme().createTable( Integer.toString( nr )),
                     "addcol r random($0); addcol g randomGaussian($0)" );
        StarTable stable =
            process( rtable,
                     "stats name mean stDev skew kurtosis"
                   + " minimum maximum nGood" );
        Object[] rRow = stable.getRow( 1 );
        Object[] gRow = stable.getRow( 2 );
        double rMean = ((Number) rRow[ 1 ]).doubleValue();
        double rStdev = ((Number) rRow[ 2 ]).doubleValue();
        double rSkew = ((Number) rRow[ 3 ]).doubleValue();
        double rKurt = ((Number) rRow[ 4 ]).doubleValue();
        double rMin = ((Number) rRow[ 5 ]).doubleValue();
        double rMax = ((Number) rRow[ 6 ]).doubleValue();
        int rN = ((Number) rRow[ 7 ]).intValue();
        double gMean = ((Number) gRow[ 1 ]).doubleValue();
        double gStdev = ((Number) gRow[ 2 ]).doubleValue();
        double gSkew = ((Number) gRow[ 3 ]).doubleValue();
        double gKurt = ((Number) gRow[ 4 ]).doubleValue();
        double gMin = ((Number) gRow[ 5 ]).doubleValue();
        double gMax = ((Number) gRow[ 6 ]).doubleValue();
        int gN = ((Number) gRow[ 7 ]).intValue();

        assertEquals( 0.5, rMean, 0.001 );
        assertEquals( Math.sqrt(1./12.), rStdev, .001 );
        assertEquals( 0.0, rSkew, 0.01 );
        assertEquals( -1.2, rKurt, 0.01 );
        assertEquals( 0.0, rMin, 0.001 );
        assertEquals( 1.0, rMax, 0.001 );
        assertEquals( nr, rN );

        assertEquals( 0.0, gMean, 0.02 );
        assertEquals( 1.0, gStdev, 0.02 );
        assertEquals( 0.0, gSkew, 0.1 );
        assertEquals( 0.0, gKurt, 0.1 );
        assertTrue( rMin > -6 && gMin < -3 );
        assertTrue( gMax > 3 && gMax < 6 );
        assertEquals( nr, gN );
    }

    public void testRandomNumbersSequential() throws Exception {
        int nr = 10_000;
        StarTable table =
            new LoopTableScheme().createTable( Integer.toString( nr ) );
        String cmd =
               "addcol r nextRandom();"
             + "addcol g nextRandomGaussian();"
             + "stats name mean stDev skew kurtosis minimum maximum nGood";
        MapEnvironment env = new MapEnvironment()
                            .setValue( "in", table )
                            .setValue( "cmd", cmd );
        new TablePipe().createExecutable( env ).execute();
        StarTable stable = env.getOutputTable( "omode" );
        Object[] rRow = stable.getRow( 1 );
        Object[] gRow = stable.getRow( 2 );
        double rMean = ((Number) rRow[ 1 ]).doubleValue();
        double rStdev = ((Number) rRow[ 2 ]).doubleValue();
        double rSkew = ((Number) rRow[ 3 ]).doubleValue();
        double rKurt = ((Number) rRow[ 4 ]).doubleValue();
        double rMin = ((Number) rRow[ 5 ]).doubleValue();
        double rMax = ((Number) rRow[ 6 ]).doubleValue();
        int rN = ((Number) rRow[ 7 ]).intValue();
        double gMean = ((Number) gRow[ 1 ]).doubleValue();
        double gStdev = ((Number) gRow[ 2 ]).doubleValue();
        double gSkew = ((Number) gRow[ 3 ]).doubleValue();
        double gKurt = ((Number) gRow[ 4 ]).doubleValue();
        double gMin = ((Number) gRow[ 5 ]).doubleValue();
        double gMax = ((Number) gRow[ 6 ]).doubleValue();
        int gN = ((Number) gRow[ 7 ]).intValue();

        /* We can't control the seed for these random numbers, so we are
         * at the mercy of what gets generated at runtime time by
         * ThreadLocalRandom.  That means these tests might pass usually
         * but fail one day.  So set the tolerances rather low.
         * If a failure is observed, increase the tolerances some more.
         * I haven't done the statistics to work out what are really
         * reasonable numbers for the row count I'm using. */
        assertEquals( 0.5, rMean, 0.1 );
        assertEquals( Math.sqrt(1./12.), rStdev, 0.1 );
        assertEquals( 0.0, rSkew, 0.1 );
        assertEquals( -1.2, rKurt, 0.1 );
        assertEquals( 0.0, rMin, 0.1 );
        assertEquals( 1.0, rMax, 0.1 );
        assertEquals( nr, rN );

        assertEquals( 0.0, gMean, 0.1 );
        assertEquals( 1.0, gStdev, 0.1 );
        assertEquals( 0.0, gSkew, 0.2 );
        assertEquals( 0.0, gKurt, 0.2 );
        assertTrue( rMin > -8 && gMin < -2 );
        assertTrue( gMax > 2 && gMax < 8 );
        assertEquals( nr, gN );
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
                               + " -utype UTYPE -xtype XTYPE"
                               + " -name NAME"
                               + " b '\"Message\"'" ).getColumnInfo( 1 );
        assertEquals( "NAME", info.getName() );
        assertEquals( "UCD", info.getUCD() );
        assertEquals( "UNITS", info.getUnitString() );
        assertEquals( "DESCRIPTION", info.getDescription() );
        assertEquals( "UTYPE", info.getUtype() );
        assertEquals( "XTYPE", info.getXtype() );
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

    public void testShuffle() throws Exception {
        assertSameData( apply( "stats" ), apply( "shuffle; stats" ) );
        assertSameData( apply( "addcol i $0" ),
                        apply( "addcol i $0; shuffle; sort i" ) );
        assertFalse( Arrays.equals(
                        getColData( inTable_, 0 ),
                        getColData( apply( "shuffle -seed 9999" ), 0 ) ) );
        assertFalse( Arrays.equals(
                        getColData( apply( "shuffle -seed 9999" ), 0 ),
                        getColData( apply( "shuffle -seed 7777" ), 0 ) ) );
        assertTrue( Arrays.equals(
                        getColData( apply( "shuffle -seed 9999" ), 0 ),
                        getColData( apply( "shuffle -seed 9999" ), 0 ) ) );
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
            new Object[] { Integer.valueOf( 1 ), Double.valueOf( 10. ),
                           null, "Beauchamp", },
            getColData( apply( "stats minimum" ), 0 ) );
        assertArrayEquals(
            new Object[] { Integer.valueOf( 4 ), Double.valueOf( 30. ),
                           null, "Taylor", },
            getColData( apply( "stats maximum" ), 0 ) );
        assertArrayEquals(
            new double[] { 10., 60., 2., Double.NaN, },
            unbox( getColData( apply( "stats sum" ), 0 ) ) );
        assertArrayEquals(
            new Object[] { Long.valueOf( 1 ), Long.valueOf( 1 ), null,
                           Long.valueOf( 2 ), },
            getColData( apply( "stats minpos" ), 0 ) );
        assertArrayEquals(
            new Object[] { Long.valueOf( 4 ), Long.valueOf( 3 ), null,
                           Long.valueOf( 3 ), },
            getColData( apply( "stats maxpos" ), 0 ) );
        assertArrayEquals(
            new int[] { 4, 3, 2, 3 },
            unbox( getColData( apply( "stats cardinality" ), 0 ) ) );

        // n.b. the floats here could/should be doubles
        assertArrayEquals(
            new float[] { 1f, 10f, Float.NaN, Float.NaN, },
            unbox( getColData( apply( "stats q.00 median q.9999" ), 0 ) ) );
        assertArrayEquals(
            new float[] { 2.5f, 20f, Float.NaN, Float.NaN, },
            unbox( getColData( apply( "stats q.00 median q.9999" ), 1 ) ) );
        assertArrayEquals(
            new float[] { 4f, 30f, Float.NaN, Float.NaN, },
            unbox( getColData( apply( "stats q.00 median q.9999" ), 2 ) ),
            1e-2 );

        assertArrayEquals(
            new String[] { "Name", "Mean", "StDev", "Minimum",
                           "Maximum", "NGood", },
            getColNames( apply( "stats" ) ) );
    }

    public void testQuantile() throws Exception {
        StarTable t1 = new QuickTable( 11, new ColumnData[] {
            col( "a", new int[] { 0, 5, 5, 10, 9, 5, 1, 2, 5, 5, 5, } ),
            col( "b", new double[] { -10.0, 0, 10,
                                     Double.NaN, Double.NaN, 
                                     100.0, 70.0, 50, 50,
                                     Double.NaN, Double.NaN } ),
        } );
        assertEquals( Float.valueOf( 5 ),
                      process( t1, "stats median" ).getCell( 0, 0 ) );
        assertEquals( Float.valueOf( 2 ),
                      process( t1, "stats q.2" ).getCell( 0, 0 ) );
        assertEquals( Float.valueOf( 1 ),
                      process( t1, "stats q.1" ).getCell( 0, 0 ) );
        assertEquals( Float.valueOf( 50 ),
                      process( t1, "stats median" ).getCell( 1, 0 ) );
        assertEquals( 70., ((Number) process( t1, "stats q.8333333" )
                                    .getCell( 1, 0 ) ).doubleValue(),
                      1e-4 );
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
