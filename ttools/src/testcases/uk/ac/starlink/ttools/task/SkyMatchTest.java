package uk.ac.starlink.ttools.task;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnPermutedStarTable;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
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

        t0 = getTestTable();
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

        StarTable result = match2( env, t1, "ra1 dec1 known_error*0.49",
                                        t2, "ra2 dec2 known_error*0.49" );
        assertEquals( 1, match2( env, t1, "ra1 dec1 known_error*0.49",
                                      t2, "ra2 dec2 known_error*0.49" )
                        .getRowCount() );

        assertEquals( 1000, match2( env, t1, "ra1 dec1 known_error*0.51",
                                         t2, "ra2 dec2 known_error*0.51" )
                           .getRowCount() );

        env.setValue( "params", "5" );
        assertEquals( 550, match2( env, t1, "ra1 dec1 known_error*0.51",
                                        t2, "ra2 dec2 known_error*0.51" )
                          .getRowCount() );

        assertEquals( 550, match2( env, t1, "ra1 dec1 known_error*1.01",
                                        t2, "ra2 dec2 0" )
                          .getRowCount() );

        assertEquals( 550, match2( env, t1, "ra1 dec1 known_error*1.01",
                                        t2, "ra2 dec2 NULL" )
                          .getRowCount() );
    }

    private StarTable match2( MapEnvironment env, StarTable table1,
                              String values1, StarTable table2, String values2 )
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

    private int countMatches( double tol ) throws Exception {
        MapEnvironment env = new MapEnvironment()
           .setValue( "params", Double.toString( tol ) )
           .setValue( "matcher", "sky" );
        StarTable result = match2( env, t1, "ra1 dec1", t2, "ra2 dec2" );

        RowSequence rseq = result.getRowSequence();

        assertEquals( "ID_1", result.getColumnInfo( 0 ).getName() );
        assertEquals( "ID_2", result.getColumnInfo( 4 ).getName() );
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

        return (int) result.getRowCount();
    }

    public static StarTable getTestTable() {
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
        new StarTableOutput().writeStarTable( getTestTable(), "-",
                                              "votable-binary" );
    }
    
}
