package uk.ac.starlink.ttools.task;

import java.io.IOException;
import java.util.logging.Level;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ColumnPermutedStarTable;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.task.ExecutionException;
import uk.ac.starlink.ttools.QuickTable;
import uk.ac.starlink.ttools.TableTestCase;
import uk.ac.starlink.ttools.func.CoordsDegrees;
import uk.ac.starlink.ttools.func.CoordsRadians;
import uk.ac.starlink.util.LogUtils;

public class SkyMatchTest extends TableTestCase {

    private final StarTable t0;
    private final StarTable t1;
    private final StarTable t2;
    private static final double ARCSEC_PER_DEGREE = 60 * 60;
    private static int NROW = 1000;
    static {
        LogUtils.getLogger( "uk.ac.starlink.util" ).setLevel( Level.WARNING );
        LogUtils.getLogger( "uk.ac.starlink.ttools.join" )
                .setLevel( Level.WARNING );
        LogUtils.getLogger( "uk.ac.starlink.ttools.task" )
                .setLevel( Level.WARNING );
    }

    public SkyMatchTest( String name ) {
        super( name );

        t0 = createTestTable();
        t1 = new ColumnPermutedStarTable( t0, new int[] { 0, 1, 2, 5 } );
        t2 = new ColumnPermutedStarTable( t0, new int[] { 0, 3, 4, 5 } );
    }

    public void testCounts() throws Exception {

        /* These are regression tests and also check whether the result is
         * the same for different matchers.  A more stringent test could
         * be done by rotating the positions on the sky and seeing if the
         * results stayed the same - see EllipseMatchTest for an example. */
        assertEquals( 1000, countMatches( 10., true ) );
        assertEquals( 1000, countMatches( 20., false ) );
        assertEquals( 550, countMatches( 5., true ) );
        assertEquals( 228, countMatches( 2., false ) );
        assertEquals( 118, countMatches( 1., true ) );
        assertEquals( 1, countMatches( 1e-3, false ) );
    }

    public void testErrMatch() throws Exception {
        MapEnvironment env = new MapEnvironment();
        env.setValue( "in1", t1 );
        env.setValue( "in2", t2 );
        env.setValue( "matcher", "skyerr" );
        env.setValue( "params", "10" );
        env.setValue( "join", "1and2" );

        StarTable result = tmatch2( env, t1, "ra1 dec1 known_error*0.49",
                                         t2, "ra2 dec2 known_error*0.49" );
        assertEquals( 1, tmatch2( env, t1, "ra1 dec1 known_error*0.49",
                                       t2, "ra2 dec2 known_error*0.49" )
                        .getRowCount() );

        assertEquals( 1000, tmatch2( env, t1, "ra1 dec1 known_error*0.51",
                                          t2, "ra2 dec2 known_error*0.51" )
                           .getRowCount() );

        env.setValue( "params", "5" );
        assertEquals( 1000, tmatch2( env, t1, "ra1 dec1 known_error*0.51",
                                          t2, "ra2 dec2 known_error*0.51" )
                           .getRowCount() );

        assertEquals( 550,
                      tmatch2( env, t1, "ra1 dec1 min(known_error*0.51,2.5)",
                                    t2, "ra2 dec2 min(known_error*0.51,2.5)" )
                     .getRowCount() );
    }

    public void testSkyColumns() throws Exception {
        StarTable ta = createTestTable();
        StarTable tb = createTestTable();

        ColumnInfo ra1col = ta.getColumnInfo( 1 );
        ColumnInfo dec1col = ta.getColumnInfo( 2 );
        ColumnInfo ra2col = tb.getColumnInfo( 3 );
        ColumnInfo dec2col = tb.getColumnInfo( 4 );
        assertEquals( "RA1", ra1col.getName() );
        assertEquals( "DEC1", dec1col.getName() );
        assertEquals( "RA2", ra2col.getName() );
        assertEquals( "DEC2", dec2col.getName() );

        assertEquals( 550,
                      skyCount( ta, "ra1", "dec1", tb, "ra2", "dec2", 5 ) );
        try {
            skyCount( ta, null, null, tb, "RA2", "DEC2", 5 );
            fail();
        }
        catch ( ExecutionException e ) {
            assertTrue( e.getMessage().indexOf( "Failed to identify" ) >= 0 );
        }

        ra1col.setUCD( "POS_EQ_RA" );
        dec1col.setUCD( "POS_EQ_DEC_MAIN" );
        assertEquals( 550,
                      skyCount( ta, null, null, tb, "RA2+0", "DEC2*1", 5 ) );

        ra2col.setName( "RA" );
        dec2col.setName( "DEC" );
        assertEquals( 550, skyCount( ta, null, null, tb, "", "", 5 ) );

        ra2col.setUnitString( "radians" );
        assertEquals( 0, skyCount( ta, "ra1", "dec1",
                                   tb, "", "", 5 ) );
    }

    public void testRoundZero() throws Exception {
        double[] decs = new double[] { 0, 45, -30, -1, -89, 89.9, 90 };
        for ( int idec = 0; idec < decs.length; idec++ ) {
            double dec = decs[ idec ];
            StarTable ta = new QuickTable( 5, new ColumnData[] {
                col( "a_ra", new double[] { 359.8, 359.9, 0, 0.1, 0.2 } ),
                col( "a_dec", new double[] { dec, dec, dec, dec, dec } ),
            } );
            for ( int i = -1; i < 2; i++ ) {
                StarTable tb = new QuickTable( 1, new ColumnData[] {
                    col( "b_ra", new double[] { 0 + ( i * 0.001 ) } ),
                    col( "b_dec", new double[] { dec } ),
                } );
    
                MapEnvironment sky3dEnv = new MapEnvironment()
                    .setValue( "params", "3600" )
                    .setValue( "matcher", "sky3d" )
                    .setValue( "find", "all" );
                StarTable sky3dResult =
                    tmatch2( sky3dEnv, ta, "a_ra a_dec 1", tb, "b_ra b_dec 1" );
                assertEquals( 5, sky3dResult.getRowCount() );
    
                MapEnvironment skyEnv = new MapEnvironment()
                    .setValue( "params", "3600" )
                    .setValue( "matcher", "sky" )
                    .setValue( "find", "all" );
                StarTable skyResult =
                    tmatch2( skyEnv, ta, "a_ra a_dec", tb, "b_ra b_dec" );
                assertEquals( 5, skyResult.getRowCount() ); 
            }
        }
    }

    private int skyCount( StarTable table1, String ra1, String dec1,
                          StarTable table2, String ra2, String dec2,
                          double error )
            throws Exception {
        MapEnvironment env = new MapEnvironment()
            .setValue( "in1", table1 )
            .setValue( "in2", table2 )
            .setValue( "ra1", ra1 )
            .setValue( "dec1", dec1 )
            .setValue( "ra2", ra2 )
            .setValue( "dec2", dec2 )
            .setValue( "error", Double.toString( error ) );
        new SkyMatch2().createExecutable( env ).execute();
        StarTable match = env.getOutputTable( "omode" );
        return (int) match.getRowCount();
    }

    private StarTable tmatch2( MapEnvironment env,
                               StarTable table1, String values1,
                               StarTable table2, String values2 )
            throws Exception {
        MapEnvironment env12 = new MapEnvironment( env );
        env12.setValue( "in1", table1 );
        env12.setValue( "in2", table2 );
        env12.setValue( "values1", values1 );
        env12.setValue( "values2", values2 );
        new TableMatch2().createExecutable( env12 ).execute();
        StarTable m12 = env12.getOutputTable( "omode" );

        MapEnvironment env21 = new MapEnvironment( env );
        env21.setValue( "in2", table1 );
        env21.setValue( "in1", table2 );
        env21.setValue( "values2", values1 );
        env21.setValue( "values1", values2 );
        new TableMatch2().createExecutable( env21 ).execute();
        StarTable m21 = env21.getOutputTable( "omode" );

        assertTrue( m21 != null );
        assertEquals( m12.getRowCount(), m21.getRowCount() );
        assertEquals( m12.getColumnCount(), m21.getColumnCount() );
        Tables.checkTable( m12 );
        Tables.checkTable( m21 );
        return m12;
    }

    private StarTable skymatch2( MapEnvironment env,
                                 StarTable table1, String ra1, String dec1,
                                 StarTable table2, String ra2, String dec2,
                                 double error )
            throws Exception {
        MapEnvironment env12 = new MapEnvironment( env );
        env12.setValue( "in1", table1 );
        env12.setValue( "in2", table2 );
        env12.setValue( "ra1", ra1 );
        env12.setValue( "dec1", dec1 );
        env12.setValue( "ra2", ra2 );
        env12.setValue( "dec2", dec2 );
        env12.setValue( "error", Double.toString( error ) );
        new SkyMatch2().createExecutable( env12 ).execute();
        StarTable m12 = env12.getOutputTable( "omode" );

        MapEnvironment env21 = new MapEnvironment( env );
        env21.setValue( "in2", table1 );
        env21.setValue( "in1", table2 );
        env21.setValue( "ra2", ra1 );
        env21.setValue( "dec2", dec1 );
        env21.setValue( "ra1", ra2 );
        env21.setValue( "dec1", dec2 );
        env21.setValue( "error", Double.toString( error ) );
        new SkyMatch2().createExecutable( env21 ).execute();
        StarTable m21 = env21.getOutputTable( "omode" );

        for ( String runner : new String[] {
                 "classic", "sequential", "parallel", "partest" } ) {
            env21.setValue( "runner", runner );
            new SkyMatch2().createExecutable( env21 ).execute();
            StarTable m21v = env21.getOutputTable( "omode" );
            assertSameData( m21, env21.getOutputTable( "omode" ) );
        }

        assertTrue( m21 != null );
        assertEquals( m12.getRowCount(), m21.getRowCount() );
        assertEquals( m12.getColumnCount(), m21.getColumnCount() );
        Tables.checkTable( m12 );
        Tables.checkTable( m21 );
        return m12;
    }

    private int countMatches( double tol, boolean withChecks )
            throws Exception {
        MapEnvironment tskyEnv = new MapEnvironment()
           .setValue( "params", Double.toString( tol ) )
           .setValue( "matcher", "sky" );
        StarTable tskyResult =
            tmatch2( tskyEnv, t1, "ra1 dec1", t2, "ra2 dec2" );

        /* Do physically the same sky match using three different match engines.
         * This is a good test because the implementations are quite different,
         * especially sky and sky3d, which are using geometrically quite
         * different criteria. */
        if ( withChecks ) {
            MapEnvironment thtmEnv = new MapEnvironment()
               .setValue( "params", Double.toString( tol ) )
               .setValue( "matcher", "htm" );
            StarTable thtmResult =
                tmatch2( thtmEnv, t1, "ra1 dec1", t2, "ra2 dec2" );

            MapEnvironment tsky3dEnv = new MapEnvironment()
               .setValue( "params",
                          Double.toString( tol * CoordsRadians
                                                .ARC_SECOND_RADIANS ) )
               .setValue( "matcher", "sky3d" );
            StarTable tsky3dResult =
                tmatch2( tsky3dEnv, t1, "ra1 dec1 1", t2, "ra2 dec2 1" );

            MapEnvironment tskyEllipseEnv = new MapEnvironment()
               .setValue( "params", Double.toString( tol ) )
               .setValue( "matcher", "skyellipse-nocirc" );
            String ellipseSpec = ( tol * 0.5 ) + " "
                               + ( tol * 0.5 ) + " "
                               + "$0%360-180";
            StarTable tskyEllipseResult =
                tmatch2( tskyEllipseEnv, t1, "ra1 dec1 " + ellipseSpec,
                                         t2, "ra2 dec2 " + ellipseSpec );

            MapEnvironment tskyCircleEnv = new MapEnvironment()
               .setValue( "params", Double.toString( tol ) )
               .setValue( "matcher", "skyellipse" );
            String circleSpec = ( tol * 0.5 ) + " "
                              + ( tol * 0.5 ) + " "
                              + "$0%360-180";
            StarTable tskyCircleResult =
                tmatch2( tskyCircleEnv, t1, "ra1 dec1 " + circleSpec,
                                        t2, "ra2 dec2 " + circleSpec );

            StarTable skyResult =
                skymatch2( new MapEnvironment(), t1, "ra1", "dec1",
                                                 t2, "ra2", "dec2", tol );

            assertSameData( tskyResult, skyResult );
            assertSameData( tskyResult, thtmResult );

            // Sky3d matcher output will be different since the separation
            // column is not the same.  It will have the same number of
            // rows and columns though.
            assertEquals( tskyResult.getColumnCount(),
                          tsky3dResult.getColumnCount() );
            assertEquals( tskyResult.getRowCount(),
                          tsky3dResult.getRowCount() );

            // Ditto sky ellipse matcher.
            assertEquals( tskyResult.getColumnCount(),
                          tskyEllipseResult.getColumnCount() );
            assertEquals( tskyResult.getRowCount(),
                          tskyEllipseResult.getRowCount() );

            // These should be very similar but not identical, since one uses
            // numerical approximations.
            assertEquals( tskyEllipseResult.getColumnCount(),
                          tskyCircleResult.getColumnCount() );
            assertEquals( tskyEllipseResult.getRowCount(),
                          tskyCircleResult.getRowCount() );
            int sepCol = getColIndex( tskyEllipseResult, "Separation" );
            assertArrayEquals( unbox( getColData( tskyEllipseResult, sepCol ) ),
                               unbox( getColData( tskyCircleResult, sepCol ) ),
                               1e-8 );
        }

        RowSequence rseq = tskyResult.getRowSequence();

        assertEquals( "ID_1", tskyResult.getColumnInfo( 0 ).getName() );
        assertEquals( "ID_2", tskyResult.getColumnInfo( 4 ).getName() );
        boolean[] got = new boolean[ NROW ];
        while ( rseq.next() ) {
            Object[] row = rseq.getRow();

            /* Check IDs. */
            assertEquals( row[ 0 ], row[ 4 ] );
            int id = ((Integer) row[ 0 ]).intValue();
            assertTrue( ! got[ id ] );
            got[ id ] = true;
        }
        assertTrue( ! rseq.next() );
        rseq.close();

        return (int) tskyResult.getRowCount();
    }

    public static StarTable createTestTable() {
        DefaultValueInfo idInfo =
            new DefaultValueInfo( "ID", Integer.class );

        DefaultValueInfo ra1Info =
            new DefaultValueInfo( "RA1", Double.class );
        DefaultValueInfo dec1Info =
            new DefaultValueInfo( "DEC1", Double.class );

        DefaultValueInfo ra2Info =
            new DefaultValueInfo( "RA2", Double.class );
        DefaultValueInfo dec2Info =
            new DefaultValueInfo( "DEC2", Double.class );

        DefaultValueInfo errInfo =
            new DefaultValueInfo( "KNOWN_ERROR", Double.class );

        ra1Info.setUnitString( "degrees" );
        dec1Info.setUnitString( "degrees" );
        ra2Info.setUnitString( "degrees" );
        dec2Info.setUnitString( "degrees" );
        errInfo.setUnitString( "arcsec" );

        final ColumnData idcol = new ColumnData( idInfo ) {
            public Object readValue( long irow ) {
                return Integer.valueOf( (int) irow );
            }
        };
        final ColumnData ra1col = new ColumnData( ra1Info ) {
            public Object readValue( long irow ) {
                return Double.valueOf( ( 9. + irow / 10. ) % 360. );
            }
        };
        final ColumnData dec1col = new ColumnData( dec1Info ) {
            public Object readValue( long irow ) {
                return Double.valueOf( -45. + irow / 60. );
            }
        };
        final ColumnData ra2col = new ColumnData( ra2Info ) {
            public Object readValue( long irow ) throws IOException {
                double theta = irow * 0.1;
                double dist = irow / ARCSEC_PER_DEGREE / 100.;
                double ra1 = ((Double) ra1col.readValue( irow )).doubleValue();
                return Double.valueOf( ra1 + Math.sin( theta ) * dist );
            }
        };
        final ColumnData dec2col = new ColumnData( dec2Info ) {
            public Object readValue( long irow ) throws IOException {
                double theta = irow * 0.1;
                double dist = irow / ARCSEC_PER_DEGREE / 100.;
                double dec1 =((Double) dec1col.readValue( irow )).doubleValue();
                return Double.valueOf( dec1 + Math.cos( theta ) * dist );
            }
        };
        final ColumnData errcol = new ColumnData( errInfo ) {
            public Object readValue( long irow ) throws IOException {
                double ra1 = ((Double) ra1col.readValue( irow )).doubleValue();
                double dec1 =((Double) dec1col.readValue( irow )).doubleValue();
                double ra2 = ((Double) ra2col.readValue( irow )).doubleValue();
                double dec2 =((Double) dec2col.readValue( irow )).doubleValue();
                return Double
                      .valueOf( CoordsDegrees
                               .skyDistanceDegrees( ra1, dec1, ra2, dec2 )
                                * ARCSEC_PER_DEGREE );
            };
        };
        
        ColumnStarTable table = ColumnStarTable.makeTableWithRows( NROW );
        table.addColumn( idcol );
        table.addColumn( ra1col );
        table.addColumn( dec1col );
        table.addColumn( ra2col );
        table.addColumn( dec2col );
        table.addColumn( errcol );

        return table;
    }

    public static void main( String[] args ) throws IOException {
        new StarTableOutput().writeStarTable( createTestTable(), "-",
                                              "votable-binary" );
    }
    
}
