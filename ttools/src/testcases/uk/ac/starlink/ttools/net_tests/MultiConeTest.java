package uk.ac.starlink.ttools.net_tests;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.RowListStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.ttools.TableTestCase;
import uk.ac.starlink.ttools.task.MapEnvironment;
import uk.ac.starlink.ttools.task.MultiCone;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.LogUtils;
import uk.ac.starlink.util.URLDataSource;
import uk.ac.starlink.vo.ResolverInfo;
import uk.ac.starlink.votable.VOTableBuilder;

/**
 * Unit tests for MultiCone (coneskymatch) task.  
 * These tests are timeconsuming and fragile
 * since they make multiple requests to remote services.
 * Various assertions may easily break in the future if the services
 * decide to change what they return.  However, if these tests pass,
 * it's a pretty good indication that MultiCone is basically operational.
 */
public class MultiConeTest extends TableTestCase {

    private static int index_;

    public MultiConeTest( String name ) {
        super( name );
        LogUtils.getLogger( "uk.ac.starlink.votable" ).setLevel( Level.SEVERE );
        LogUtils.getLogger( "uk.ac.starlink.table" ).setLevel( Level.WARNING );
        LogUtils.getLogger( "uk.ac.starlink.vo" ).setLevel( Level.WARNING );
        LogUtils.getLogger( "uk.ac.starlink.ttools.cone" )
                .setLevel( Level.SEVERE );
        LogUtils.getLogger( "uk.ac.starlink.ttools.task" )
                .setLevel( Level.WARNING );
    }

    public void testCone1() throws Exception {
        MapEnvironment env = new MapEnvironment()
            .setResourceBase( MultiConeTest.class )
            .setValue( "serviceurl", "http://archive.stsci.edu/hst/search.php" )
            .setValue( "in", "messier.xml" )
            .setValue( "ra", "RA" )
            .setValue( "dec", "DEC" )
            .setValue( "sr", "0.05" )
            .setValue( "copycols", "" )
            .setValue( "emptyok", "false" )
            .setValue( "icmd", "head 10" );
        StarTable result = multicone( env, new int[] { 1, } );

        assertEquals( 245L, result.getRowCount() );  // was 173 rows
        int ncol = result.getColumnCount();
        assertTrue( ncol > 10 && ncol < 100 );

        assertEquals( "Dataset", result.getColumnInfo( 0 ).getName() );
        assertEquals( "Target Name", result.getColumnInfo( 1 ).getName() );
        assertEquals( "F606W", result.getCell( 14L, 10 ) );
    }

    public void testCone2() throws Exception {
        DataSource v7src = new URLDataSource( TableTestCase.class
                                             .getResource( "vizier.xml" ) );
        v7src.setPosition( "7" );
        StarTable v7 =
            new VOTableBuilder()
           .makeStarTable( v7src, true, StoragePolicy.PREFER_MEMORY );
        MapEnvironment env = new MapEnvironment()
            .setResourceBase( MultiConeTest.class )
            .setValue( "serviceurl",
                       "http://www.nofs.navy.mil/cgi-bin/vo_cone.cgi?CAT=NOMAD")
            .setValue( "in", v7 )
            .setValue( "icmd",
                       "addskycoords -inunit sex fk4 fk5 RAB1950 DEB1950 " +
                       "RAJ2000 DEJ2000" )
            .setValue( "ra", "RAJ2000" )
            .setValue( "dec", "DEJ2000" )
            .setValue( "sr", "0.01" )
            .setValue( "scorecol", "OFFSET" )
            .setValue( "copycols", "name" )
            .setValue( "zerometa", "true" );
        StarTable result = multicone( env, new int[] { 5, 8, } );
        int ncol = result.getColumnCount();

        assertEquals( 462L, result.getRowCount() );
        assertEquals( 19, ncol );

        assertEquals( "name", result.getColumnInfo( 0 ).getName() );
        assertEquals( "id", result.getColumnInfo( 1 ).getName() );
        assertEquals( "RA", result.getColumnInfo( 2 ).getName() );
        assertEquals( "DEC", result.getColumnInfo( 3 ).getName() );
        assertEquals( "OFFSET", result.getColumnInfo( ncol - 1 ).getName() );
        assertEquals( "CRAB",
                      result.getCell( 0L, 0 ).toString().trim() );
        assertEquals( "1120-0088780",
                      result.getCell( 0L, 1 ).toString().trim() );
        assertEquals( 0.00872,
                      ((Number) result.getCell( 0L, ncol - 1 )).doubleValue(),
                      0.00001 );
    }

    public void testCone3() throws Exception {
        MapEnvironment env = new MapEnvironment()
            .setResourceBase( MultiConeTest.class )
            .setValue( "serviceurl",
                       "http://archive.stsci.edu/iue/search.php?" )
            .setValue( "in", "queries.txt" )
            .setValue( "ifmt", "ascii" )
            .setValue( "ra", "$1" )
            .setValue( "dec", "$2" )
            .setValue( "sr", "$3" )
            .setValue( "scorecol", null )
            .setValue( "copycols", "$4" )
            .setValue( "ocmd", "delcols Category" );
        StarTable result = multicone( env, new int[] { 1, 2, 3, } );

        assertEquals( 699L, result.getRowCount() );  // was 451 rows
        assertEquals( 14, result.getColumnCount() );

        assertEquals( "Name", result.getColumnInfo( 0 ).getName() );
        assertEquals( "fomalhaut", result.getCell( 0L, 0 ) );
        assertEquals( "sirius", result.getCell( result.getRowCount() - 1, 0 ) );
    }

    public void testFindModes() throws Exception {
        MapEnvironment env = new MapEnvironment()
            .setResourceBase( MultiConeTest.class )
            .setValue( "serviceurl", "http://archive.stsci.edu/hst/search.php" )
            .setValue( "in", "messier.xml" )
            .setValue( "ra", "RA" )
            .setValue( "dec", "DEC" )
            .setValue( "sr", "0.04" )
            .setValue( "copycols", "" )
            .setValue( "emptyok", "false" )
            .setValue( "icmd", "rowrange 2 10" );
        StarTable bestResult =
            multicone( new MapEnvironment( env ).setValue( "find", "best" ),
                       new int[] { 9, } );
        StarTable allResult =
            multicone( new MapEnvironment( env ).setValue( "find", "all" ),
                       new int[] { 9, } );
        StarTable eachResult =
            multicone( new MapEnvironment( env ).setValue( "find", "each" ),
                       new int[] { 9, } );
        assertEquals( 2, bestResult.getRowCount() );
        assertEquals( 94, allResult.getRowCount() );
        assertEquals( 9, eachResult.getRowCount() );

    }

    public void testSia() throws Exception {
        int nq = 10;
        MapEnvironment env = new MapEnvironment()
            .setResourceBase( MultiConeTest.class )
            .setValue( "servicetype", "sia" )
            .setValue( "serviceurl",
                       "http://irsa.ipac.caltech.edu/cgi-bin/2MASS/"
                     + "IM/nph-im_sia?type=at&ds=asky" )
            .setValue( "in", "messier.xml" )
            .setValue( "ra", "RA" )
            .setValue( "dec", "DEC" )
            .setValue( "find", "best" )
            .setValue( "dataformat", "image/fits" )
            .setValue( "icmd", "head " + nq );
        StarTable result = multicone( env, new int[] { 5 } );
        assertEquals( nq, result.getRowCount() );
        Map ucdMap = new HashMap();
        for ( int icol = 0; icol < result.getColumnCount(); icol++ ) {
            ucdMap.put( result.getColumnInfo( icol ).getUCD(),
                        Integer.valueOf( icol ) );
        }
        assertTrue( ucdMap.containsKey( "POS_EQ_RA_MAIN" ) );
        assertTrue( ucdMap.containsKey( "VOX:Image_AccessReference" ) );
        assertTrue( ucdMap.containsKey( "VOX:Image_Format" ) );
        int fmtCol = ((Integer) ucdMap.get( "VOX:Image_Format" )).intValue();
        RowSequence rseq = result.getRowSequence();
        try {
            while ( rseq.next() ) {
                assertEquals( "image/fits", rseq.getCell( fmtCol ) );
            }
        }
        finally {
            rseq.close();
        }
    }

    public void testSsa() throws Exception {
        ColumnInfo[] infos = new ColumnInfo[] {
            new ColumnInfo( "Name", String.class, null ),
            new ColumnInfo( "RA", Double.class, null ), 
            new ColumnInfo( "Dec", Double.class, null ),
        };
        RowListStarTable table = new RowListStarTable( infos );
        String[] targets = new String[] { "3c273", "algol", };
        int nq = targets.length;
        for ( int i = 0; i < targets.length; i++ ) {
            String target = targets[ i ];
            ResolverInfo rinfo = ResolverInfo.resolve( target );
            double ra = rinfo.getRaDegrees();
            double dec = rinfo.getDecDegrees();
            table.addRow( new Object[] { target, Double.valueOf( ra ),
                                                 Double.valueOf( dec ), } );
        }
  
        MapEnvironment env = new MapEnvironment()
            .setValue( "servicetype", "ssa" )
            .setValue( "serviceurl",
                       "http://archive.eso.org/apps/ssaserver/EsoProxySsap" )
            .setValue( "in", table )
            .setValue( "ra", "RA" )
            .setValue( "dec", "Dec" )
            .setValue( "sr", "0.01" )
            .setValue( "find", "best" )
            .setValue( "dataformat", "votable" );
        StarTable result = multicone( env, new int[] { 1 } );
        assertEquals( 1, result.getRowCount() );  // algol not known
        Map ucdMap = new HashMap();
        Map utypeMap = new HashMap();
        for ( int icol = 0; icol < result.getColumnCount(); icol++ ) {
            ColumnInfo colInfo = result.getColumnInfo( icol );
            ucdMap.put( colInfo.getUCD(), Integer.valueOf( icol ) );
            utypeMap.put( colInfo.getUtype(), Integer.valueOf( icol ) );
        }
        assertTrue( ucdMap.containsKey( "pos.angDistance" ) );
        int isepCol = ((Integer) ucdMap.get( "pos.angDistance" )).intValue();
        assertTrue( utypeMap.containsKey( "ssa:Access.Reference" ) );
        assertTrue( utypeMap.containsKey( "ssa:Access.Format" ) );
        assertTrue( ((Number) result.getCell( 0, isepCol )).doubleValue()
                    < 0.1 );
    }

    public void testFootprints() throws Exception {
        MapEnvironment env = new MapEnvironment()
            .setResourceBase( MultiConeTest.class )
            .setValue( "serviceurl",
                       "https://vizier.cds.unistra.fr/viz-bin/votable/"
                     + "-A?-source=VIII/58&" )
            .setValue( "in", "messier.xml" )
            .setValue( "ra", "RA" )
            .setValue( "dec", "Dec" )
            .setValue( "sr", "0.1" )
            .setValue( "find", "all" )
            .setValue( "copycols", "" )
            .setValue( "emptyok", "false" );
        MapEnvironment fEnv = new MapEnvironment( env )
            .setValue( "usefoot", "true" );
        MapEnvironment nEnv = new MapEnvironment( env )
            .setValue( "usefoot", "false" );
        StarTable fResult = multicone( env, new int[] { 1, 2 } );
        assertEquals( 3, fResult.getRowCount() );
        StarTable nResult = multicone( env, new int[] { 5 } );
        assertSameData( fResult, nResult );
    }

    private StarTable multicone( MapEnvironment env,
                                 int[] parallelisms ) throws Exception {
        StarTable result = null;
        for ( int ip = 0; ip < parallelisms.length; ip++ ) {
            MapEnvironment penv = new MapEnvironment( env );
            penv.setValue( "parallel", Integer.toString( parallelisms[ ip ] ) );
            new MultiCone().createExecutable( penv ).execute();
            StarTable res =
                Tables.randomTable( penv.getOutputTable( "omode" ) );
            if ( result == null ) {
                result = res;
                File odir = new File( "/mbt/scratch/table" );
                if ( odir.canWrite() ) {
                    new StarTableOutput()
                       .writeStarTable( result,
                                        new File( odir, "multicone" + ++index_
                                                  + ".xml" ).toString(),
                                        StarTableOutput.AUTO_HANDLER );
                }
            }
            else {
                assertSameData( result, res );
            }
        }
        return result;
    }
}
