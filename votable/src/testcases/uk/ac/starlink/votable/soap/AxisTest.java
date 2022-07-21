package uk.ac.starlink.votable.soap;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import javax.xml.rpc.ParameterMode;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.encoding.XMLType;
import uk.ac.starlink.soap.AppHttpSOAPServer;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.util.LogUtils;
import uk.ac.starlink.votable.AutoStarTable;
import uk.ac.starlink.votable.TableTestCase;

public class AxisTest extends TableTestCase {

    public static final int PORT = 2323;

    public AxisTest( String name ) {
        super( name );
        LogUtils.getLogger( "org.mortbay" ).setLevel( Level.WARNING );
        LogUtils.getLogger( "uk.ac.starlink.soap" ).setLevel( Level.WARNING );
        LogUtils.getLogger( "org.apache.axis" ).setLevel( Level.SEVERE );
        LogUtils.getLogger( "uk.ac.starlink.table" ).setLevel( Level.WARNING );
    }

    public void testAxis() throws Exception {
        if ( AxisOK.isOK() ) {
            StarTable inTable = AutoStarTable.getDemoTable( 100 );
            StarTable outTable = copyTableUsingAxis( inTable );
            assertVOTableEquals( inTable, outTable, true );
        }
    }

    public StarTable copyTableUsingAxis( StarTable inTable )
            throws Exception {
        URL deployURL = getClass().getResource( "test.wsdd" );
        AppHttpSOAPServer server = new AppHttpSOAPServer( PORT );
        server.start();
        server.addSOAPService( deployURL );
        // assertEquals( 0, server.getRequests() );
 
        int port = server.getPort();
        String endpoint = "http://localhost:" + port + "/services/VotableTest";

        Call call = (Call) new Service().createCall();
        call.setTargetEndpointAddress( endpoint );
        VOTableSerialization.configureCall( call );
        call.setOperationName( "copy" );
        call.addParameter( "table", VOTableSerialization.QNAME_VOTABLE,
                           ParameterMode.IN );
        call.setReturnType( VOTableSerialization.QNAME_VOTABLE );

        StarTable outTable = 
            (StarTable) call.invoke( new Object[] { inTable } );
        server.stop();
        server.destroy();
        return outTable;
    }
}
