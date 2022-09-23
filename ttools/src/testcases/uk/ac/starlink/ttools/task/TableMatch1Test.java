package uk.ac.starlink.ttools.task;

import java.io.IOException;
import java.util.logging.Level;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.join.FixedSkyMatchEngine;
import uk.ac.starlink.table.join.HtmSkyPixellator;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.TableTestCase;
import uk.ac.starlink.ttools.func.CoordsRadians;
import uk.ac.starlink.util.LogUtils;
import uk.ac.starlink.util.URLDataSource;
import uk.ac.starlink.votable.VOTableBuilder;

public class TableMatch1Test extends TableTestCase {

    private final StarTable messier_;

    public TableMatch1Test( String name ) throws IOException {
        super( name );
        messier_ = new VOTableBuilder()
            .makeStarTable(
                 new URLDataSource( getClass()
                                   .getResource( "../messier.xml" ) ),
                 true, StoragePolicy.PREFER_MEMORY );
        LogUtils.getLogger( "uk.ac.starlink.util" )
                .setLevel( Level.WARNING );
        LogUtils.getLogger( "uk.ac.starlink.ttools" )
                .setLevel( Level.WARNING );
        LogUtils.getLogger( "uk.ac.starlink.table.storage" )
                .setLevel( Level.WARNING );
    }

    public void testMessier1Degree() throws Exception {
        long nrow = messier_.getRowCount();
        double err = 3600;
        int n3 = 3;
        int n2 = 9;
        int n1 = 83;
        assertEquals( nrow, 3 * n3 + 2 * n2 + 1 * n1 );

        assertEquals( n3, match1( err, "wide3" ).getRowCount() );
        StarTable id3 = match1( err, "identify", "select groupSize==3" );
        assertEquals( n3 * 3, id3.getRowCount() );

        assertEquals( n2, match1( err, "wide2" ).getRowCount() );
        StarTable id2 = match1( err, "identify", "select groupSize==2" );
        assertEquals( n2 * 2, id2.getRowCount() );

        StarTable id1 = match1( err, "identify", "select NULL_groupId" );
        assertEquals( n1 * 1, id1.getRowCount() );

        assertEquals( nrow - n2 * 2 - n3 * 3,
                      match1( err, "keep0" ).getRowCount() );
        assertEquals( nrow - n2 * 1 - n3 * 2,
                      match1( err, "keep1" ).getRowCount() );

    }

    public void testMessierPairs() throws Exception {
        for ( double err = 1000; err <= 10000; err += 1000 ) {
            checkMessierPairs( err );
        } 

        checkMessierPairs( 700 );
        assertEquals( 1L, match1( 700, "wide2" ).getRowCount() );
        try {
            checkMessierPairs( 600 );
            fail();
        }
        catch ( TaskException e ) {
            assertTrue( e.getMessage().indexOf( "No matches" ) >= 0 );
        }
    }

    private void checkMessierPairs( double err ) throws Exception {
        StarTable pairSep =
            match1( err, "wide2",
                    "addcol sep skyDistanceDegrees(ra_1,dec_1,ra_2,dec_2);"
                  + "keepcols sep" );
        RowSequence rseq = pairSep.getRowSequence();
        try {
            while ( rseq.next() ) {
                double sep = ((Number) rseq.getCell( 0 )).doubleValue();
                assertTrue( sep <= err );
            }
        }
        finally {
            rseq.close();
        }
    }

    private StarTable match1( double err, String action ) throws Exception {
        StarTable result = match1( err, action, null );
        if ( action.equals( "identify" ) ) {
            assertEquals( messier_.getRowCount(), result.getRowCount() );
            assertEquals( messier_.getColumnCount(),
                          result.getColumnCount() + 2 );
        }
        else if ( action.startsWith( "keep" ) ) {
            assertEquals( messier_.getColumnCount(), result.getColumnCount() );
        }
        else if ( action.startsWith( "wide" ) ) {
            int factor = Integer.parseInt( action.substring( 4 ) );
            assertEquals( messier_.getColumnCount() * factor,
                          result.getColumnCount() );
        }
        else {
            fail( "Unknown action " + action );
        }
        return result;
    }

    private StarTable match1( double err, String action, String ocmd )
            throws Exception {
        MapEnvironment env = new MapEnvironment()
            .setValue( "in", messier_ )
            .setValue( "action", action );
        if ( ocmd != null ) {
            env.setValue( "ocmd", ocmd );
        }

        MapEnvironment skyEnv = new MapEnvironment( env )
            .setValue( "matcher", "sky" )
            .setValue( "values", "ra dec" )
            .setValue( "params", Double.toString( err ) );
        new TableMatch1().createExecutable( skyEnv ).execute();
        StarTable skyResult = skyEnv.getOutputTable( "omode" );

        MapEnvironment sky3dEnv = new MapEnvironment( env ) 
            .setValue( "matcher", "sKy3d" )
            .setValue( "values", "RA DEC 1" )
            .setValue( "params",
                       Double.toString( err * CoordsRadians
                                             .ARC_SECOND_RADIANS ) );
        new TableMatch1().createExecutable( sky3dEnv ).execute();
        StarTable sky3dResult = sky3dEnv.getOutputTable( "omode" );

        MapEnvironment htmEnv = new MapEnvironment( env )
            .setValue( "matcher", TestSkyMatchEngine.class.getName() )
            .setValue( "values", "ucd$POS_EQ_RA DEC*1" )
            .setValue( "params", Double.toString( err ) );
        new TableMatch1().createExecutable( htmEnv ).execute();
        StarTable htmResult = htmEnv.getOutputTable( "omode" );

        assertSameData( skyResult, sky3dResult );
        assertSameData( skyResult, htmResult );
     
        StarTable result = skyResult;
        Tables.checkTable( result );
        result = Tables.randomTable( result );
        Tables.checkTable( result );
        return result;
    }

    public static class TestSkyMatchEngine
            extends FixedSkyMatchEngine.InDegrees {
        public TestSkyMatchEngine() {
            super( new HtmSkyPixellator(), 0.1 );
        }
    }
}
