package uk.ac.starlink.ttools.task;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import uk.ac.starlink.ttools.TableTestCase;
import uk.ac.starlink.ttools.func.Coords;

public class SkyMatchTest extends TableTestCase {

    private final StarTable t0;
    private final StarTable t1;
    private final StarTable t2;
    private static int NROW = 1000;
    static {
        Logger.getLogger( "uk.ac.starlink.ttools" ).setLevel( Level.WARNING );
    }

    public SkyMatchTest( String name ) {
        super( name );

        t0 = createTestTable();
        t1 = new ColumnPermutedStarTable( t0, new int[] { 0, 1, 2, 5 } );
        t2 = new ColumnPermutedStarTable( t0, new int[] { 0, 3, 4, 5 } );
    }

    public void testCounts() throws Exception {

        /* Currently, these aren't much more than regression tests.
         * I can think of some more rigorous tests to do, but my spherical
         * trigonometry would need some polishing to get there. */
        assertEquals( 1000, countMatches( 10. ) );
        assertEquals( 1000, countMatches( 20. ) );
        assertEquals( 550, countMatches( 5. ) );
        assertEquals( 228, countMatches( 2. ) );
        assertEquals( 118, countMatches( 1. ) );
        assertEquals( 1, countMatches( 1e-3 ) );
    }

    public void testErrMatch() throws Exception {
        MapEnvironment env = new MapEnvironment();
        env.setValue( "in1", t1 );
        env.setValue( "in2", t2 );
        env.setValue( "matcher", "skyerr" );
        env.setValue( "values1", "ra1 dec1 known_error*0.49" );
        env.setValue( "values2", "ra2 dec2 known_error*0.49" );
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
        assertEquals( 550, tmatch2( env, t1, "ra1 dec1 known_error*0.51",
                                         t2, "ra2 dec2 known_error*0.51" )
                          .getRowCount() );

        assertEquals( 550, tmatch2( env, t1, "ra1 dec1 known_error*1.01",
                                         t2, "ra2 dec2 0" )
                          .getRowCount() );

        assertEquals( 550, tmatch2( env, t1, "ra1 dec1 known_error*1.01",
                                         t2, "ra2 dec2 NULL" )
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
        dec2col.setUnitString( "radians" );
        assertEquals( 0, skyCount( ta, "ra1", "dec1",
                                   tb, "", "", 5 ) );
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

        assertTrue( m21 != null );
        assertEquals( m12.getRowCount(), m21.getRowCount() );
        assertEquals( m12.getColumnCount(), m21.getColumnCount() );
        Tables.checkTable( m12 );
        Tables.checkTable( m21 );
        return m12;
    }

    private int countMatches( double tol ) throws Exception {
        MapEnvironment env = new MapEnvironment()
           .setValue( "params", Double.toString( tol ) )
           .setValue( "matcher", "sky" );
        StarTable tResult = tmatch2( env, t1, "ra1 dec1", t2, "ra2 dec2" );
        StarTable skyResult =
            skymatch2( new MapEnvironment(), t1, "ra1", "dec1",
                                             t2, "ra2", "dec2", tol );
        assertSameData( tResult, skyResult );

        RowSequence rseq = tResult.getRowSequence();

        assertEquals( "ID_1", tResult.getColumnInfo( 0 ).getName() );
        assertEquals( "ID_2", tResult.getColumnInfo( 4 ).getName() );
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

        return (int) tResult.getRowCount();
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
                return new Integer( (int) irow );
            }
        };
        final ColumnData ra1col = new ColumnData( ra1Info ) {
            public Object readValue( long irow ) {
                return new Double( ( 9. + irow / 10. ) % 360. );
            }
        };
        final ColumnData dec1col = new ColumnData( dec1Info ) {
            public Object readValue( long irow ) {
                return new Double( -45. + irow / 60. );
            }
        };
        final ColumnData ra2col = new ColumnData( ra2Info ) {
            public Object readValue( long irow ) throws IOException {
                double theta = irow * 0.1;
                double dist = irow / Coords.DEGREE * Coords.ARC_SECOND / 100.;
                double ra1 = ((Double) ra1col.readValue( irow )).doubleValue();
                return new Double( ra1 + Math.sin( theta ) * dist );
            }
        };
        final ColumnData dec2col = new ColumnData( dec2Info ) {
            public Object readValue( long irow ) throws IOException {
                double theta = irow * 0.1;
                double dist = irow / Coords.DEGREE * Coords.ARC_SECOND / 100.;
                double dec1 =((Double) dec1col.readValue( irow )).doubleValue();
                return new Double( dec1 + Math.cos( theta ) * dist );
            }
        };
        final ColumnData errcol = new ColumnData( errInfo ) {
            public Object readValue( long irow ) throws IOException {
                double ra1 = ((Double) ra1col.readValue( irow )).doubleValue();
                double dec1 =((Double) dec1col.readValue( irow )).doubleValue();
                double ra2 = ((Double) ra2col.readValue( irow )).doubleValue();
                double dec2 =((Double) dec2col.readValue( irow )).doubleValue();
                return new Double( Coords
                                  .skyDistanceDegrees( ra1, dec1, ra2, dec2 )
                                   * Coords.DEGREE / Coords.ARC_SECOND );
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
