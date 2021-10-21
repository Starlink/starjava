package uk.ac.starlink.vo.datalink;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import junit.framework.TestCase;
import uk.ac.starlink.table.ColumnPermutedStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.util.LogUtils;
import uk.ac.starlink.votable.VOElement;
import uk.ac.starlink.votable.VOElementFactory;
import uk.ac.starlink.votable.datalink.ServiceDescriptor;
import uk.ac.starlink.votable.datalink.ServiceParam;

public class DatalinkTest extends TestCase {

    public DatalinkTest() {
        LogUtils.getLogger( "uk.ac.starlink.vo.datalink" )
                .setLevel( Level.SEVERE );
    }

    public void testFile() throws Exception {
        URL url = DatalinkTest.class.getResource( "dl2.vot" );
        VOElement voel = new VOElementFactory( StoragePolicy.PREFER_MEMORY )
                        .makeVOElement( url );
        LinksDoc ldoc = LinksDoc.createLinksDoc( voel );

        StarTable table = ldoc.getResultTable();

        assertTrue( LinksDoc.isLinksResponse( table, 1 ) );
        assertFalse( LinksDoc.isLinksResponse( table, 0 ) );

        StarTable hardlyTable =
            new ColumnPermutedStarTable( table, new int[] { 0, 1 } );
        assertFalse( LinksDoc.isLinksResponse( hardlyTable, 3 ) );
        assertTrue( LinksDoc.isLinksResponse( hardlyTable, 6 ) );

        LinkColMap colmap = ldoc.getColumnMap();
        assertEquals( 5, table.getRowCount() );
        assertEquals( 8, table.getColumnCount() );
        Object[] row0 = table.getRow( 0 );
        Object[] row1 = table.getRow( 1 );
        Object[] row4 = table.getRow( 4 );

        assertEquals( "ivo://org.gavo.dc/~?lswscans/data/part1/"
                      + "Bruceplatten/FITS/B1107b.fits",
                      colmap.getId( row0 ) );
        assertEquals( "http://dc.zah.uni-heidelberg.de/getproduct/lswscans/"
                      + "data/part1/Bruceplatten/FITS/B1107b.fits?scale=4",
                      colmap.getAccessUrl( row0 ) );
        assertEquals( null, colmap.getServiceDef( row0 ) );
        assertEquals( null, colmap.getErrorMessage( row0 ) );
        assertEquals( "FITS, scaled by 1/4", colmap.getDescription( row0 ) );
        assertEquals( "#science", colmap.getSemantics( row0 ) );
        assertEquals( "image/fits", colmap.getContentType( row0 ) );
        assertEquals( new Long( 79792740L ), colmap.getContentLength( row0 ) );

        assertEquals( "ndiwmgwbuupa", colmap.getServiceDef( row1 ) );
        assertEquals( "NotFoundFault", colmap.getErrorMessage( row4 ) );

        assertEquals( 1, ldoc.getServiceDescriptors().length );
        ServiceDescriptor sdesc = ldoc.getServiceDescriptors()[ 0 ];
        String descId = sdesc.getDescriptorId();
        assertEquals( "ndiwmgwbuupa", descId );
        assertEquals( "http://dc.zah.uni-heidelberg.de/lswscans/res/"
                      + "positions/dl/dlget",
                      sdesc.getAccessUrl() );
        assertEquals( null, sdesc.getResourceIdentifier() );
        assertEquals( "ivo://ivoa.net/std/SODA#sync-1.0",
                      sdesc.getStandardId() );
        assertEquals( 10, sdesc.getInputParams().length );

        ServiceInvoker invoker = getServiceInvoker( ldoc, descId );
        assertEquals( 0, invoker.getRowParams().length );
        assertNull( getServiceInvoker( ldoc, "not-a-service" ) );

        ServiceParam[] userParams = invoker.getUserParams();
        Map<ServiceParam,String> argMap =
            new LinkedHashMap<ServiceParam,String>();
        argMap.put( userParams[ 1 ], "42.9 43.1" );
        argMap.put( userParams[ 0 ], "295.0 295.1" );
        argMap.put( userParams[ 8 ], null );
        String ubase = "http://dc.zah.uni-heidelberg.de/lswscans/"
                     + "res/positions/dl/dlget";
        String uargs = "?ID=ivo://org.gavo.dc/~%3flswscans/data/"
                     +  "part1/Bruceplatten/FITS/B1107b.fits"
                     + "&RA=42.9 43.1"
                     + "&DEC=295.0 295.1"
                     + "&KIND=";
        /* I'm not certain this encoding is doing what it should. */
        assertEquals( new URL( ubase + uargs
                                      .replaceAll( "/", "%2f" )
                                      .replaceAll( ":", "%3a" )
                                      .replaceAll( " ", "%20" ) ),
                      invoker.getUrl( row0, argMap ) );
    }

    private ServiceInvoker getServiceInvoker( LinksDoc ldoc, String servId )
            throws IOException {
        StarTable resultTable = ldoc.getResultTable();
        for ( ServiceDescriptor sd : ldoc.getServiceDescriptors() ) {
            if ( servId.equals( sd.getDescriptorId() ) ) {
                return new ServiceInvoker( sd, resultTable );
            }
        }
        return null;
    }
}
