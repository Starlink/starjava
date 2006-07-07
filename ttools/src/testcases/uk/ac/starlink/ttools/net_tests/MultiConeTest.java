package uk.ac.starlink.ttools.net_tests;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.ttools.TableTestCase;
import uk.ac.starlink.ttools.task.MapEnvironment;
import uk.ac.starlink.ttools.task.MultiCone;

/**
 * Unit tests for MultiCone task.  These tests are timeconsuming and fragile
 * since they make multiple requests to remote services.
 * Various assertions may easily break in the future if the services
 * decide to change what they return.  However, if these tests pass,
 * it's a pretty good indication that MultiCone is basically operational.
 */
public class MultiConeTest extends TableTestCase {

    private static int index_;

    public MultiConeTest( String name ) {
        super( name );
        Logger.getLogger( "uk.ac.starlink.votable" ).setLevel( Level.SEVERE );
        Logger.getLogger( "uk.ac.starlink.table" ).setLevel( Level.WARNING );
        Logger.getLogger( "uk.ac.starlink.vo" ).setLevel( Level.WARNING );
        Logger.getLogger( "uk.ac.starlink.ttools.task" )
              .setLevel( Level.WARNING );
    }

    public void testExample1() throws Exception {
        MapEnvironment env = new MapEnvironment()
            .setResourceBase( MultiConeTest.class )
            .setValue( "serviceurl", "http://archive.stsci.edu/hst/search.php" )
            .setValue( "in", "messier.xml" )
            .setValue( "ra", "RA" )
            .setValue( "dec", "DEC" )
            .setValue( "sr", "0.05" )
            .setValue( "icmd", "head 10" );
        StarTable result = multicone( env );

        assertEquals( 173L, result.getRowCount() );
        assertEquals( 13, result.getColumnCount() );

        assertEquals( "Dataset", result.getColumnInfo( 0 ).getName() );
        assertEquals( "Target Name", result.getColumnInfo( 1 ).getName() );
        assertEquals( "F606W", result.getCell( 14L, 9 ) );
    }

    public void testExample2() throws Exception {
        MapEnvironment env = new MapEnvironment()
            .setResourceBase( MultiConeTest.class )
            .setValue( "serviceurl",
                       "http://www.nofs.navy.mil/cgi-bin/vo_cone.cgi?CAT=NOMAD")
            .setValue( "in", "vizier.xml#7" )
            .setValue( "icmd",
                       "addskycoords -inunit sex fk4 fk5 RAB1950 DEB1950 " +
                       "RAJ2000 DEJ2000" )
            .setValue( "ra", "RAJ2000" )
            .setValue( "dec", "DEJ2000" )
            .setValue( "sr", "0.01" )
            .setValue( "ocmd",
                       "replacecol -units rad " +
                       "RA hmsToRadians(RA[0],RA[1],RA[2])" +
                       ";" +
                       "replacecol -units rad " +
                       "DEC dmsToRadians(DEC[0],DEC[1],DEC[2])" )
            .setValue( "zerometa", "true" );
        StarTable result = multicone( env );

        assertEquals( 462L, result.getRowCount() );
        assertEquals( 15, result.getColumnCount() );

        assertEquals( "id", result.getColumnInfo( 0 ).getName() );
        assertEquals( "RA", result.getColumnInfo( 1 ).getName() );
        assertEquals( "DEC", result.getColumnInfo( 2 ).getName() );
        assertEquals( "1120-0088780",
                      result.getCell( 0L, 0 ).toString().trim() );
    }

    public void testExample3() throws Exception {
        MapEnvironment env = new MapEnvironment()
            .setResourceBase( MultiConeTest.class )
            .setValue( "serviceurl",
                       "http://archive.stsci.edu/iue/search.php?" )
            .setValue( "in", "queries.txt" )
            .setValue( "ifmt", "ascii" )
            .setValue( "ra", "$1" )
            .setValue( "dec", "$2" )
            .setValue( "sr", "$3" )
            .setValue( "copycols", "$4" );
        StarTable result = multicone( env );

        assertEquals( 166L, result.getRowCount() );
        assertEquals( 14, result.getColumnCount() );

        assertEquals( "Name", result.getColumnInfo( 0 ).getName() );
        assertEquals( "fomalhaut", result.getCell( 0L, 0 ) );
        assertEquals( "sirius", result.getCell( result.getRowCount() - 1, 0 ) );
    }

    private StarTable multicone( MapEnvironment env ) throws Exception {
        new MultiCone().createExecutable( env ).execute();
        StarTable result = env.getOutputTable( "omode" );
        if ( result != null ) {
            Tables.checkTable( result );
        }
        File odir = new File( "/mbt/scratch/table" );
        if ( odir.canWrite() ) {
            new StarTableOutput()
               .writeStarTable( result,
                                new File( odir,
                                          "multicone" + ++index_ + ".xml" )
                               .toString(),
                                StarTableOutput.AUTO_HANDLER );
        }
        return result;
    }
}
