package uk.ac.starlink.topcat.soap;

import java.io.IOException;
import java.net.ConnectException;
import javax.xml.rpc.ParameterMode;
import javax.xml.rpc.ServiceException;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.encoding.XMLType;
import uk.ac.starlink.soap.util.RemoteUtilities;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.votable.soap.VOTableSerialization;

/**
 * Serves as an access point for accessing TOPCAT's SOAP services.
 * If you don't have access to the topcat classes of course you can't
 * use this, but you can use the code as a template; it does not rely
 * in important ways on having the topcat classes present at compile-
 * or run-time.
 *
 * @author   Mark Taylor (Starlink)
 * @since    24 Mar 2005
 */
public class TopcatSOAPClient {

    private String cookie_;
    private String endpoint_;

    /**
     * Constructs a new client, getting server details from contact file.
     */
    public TopcatSOAPClient() throws ConnectException {
        Object[] tcServ = 
            RemoteUtilities.readContactFile( TopcatSOAPServer.APP_NAME );
        if ( tcServ == null ) {
            throw new ConnectException( "No contact file for TOPCAT" );
        }
        String host = (String) tcServ[ 0 ];
        int port = ((Integer) tcServ[ 1 ]).intValue();
        String cookie = (String) tcServ[ 2 ];
        init( host, port, cookie );
    }

    /**
     * Constructs a new client with given server details.
     *
     * @param  host  host name
     * @param  port  server port
     * @param  cookie   cookie for server interaction
     */
    public TopcatSOAPClient( String host, int port, String cookie ) {
        init( host, port, cookie );
    }

    /**
     * Initializes this client - called from constructor.
     *
     * @param  host  host name
     * @param  port  server port
     * @param  cookie   cookie for server interaction
     */
    private void init( String host, int port, String cookie ) {
        cookie_ = cookie;
        endpoint_ = "http://" + host + ":" + port + 
                    "/services/TopcatSOAPServices";
    }

    /**
     * Creates and configures a call to the Topcat server.
     * Any error is transformed to an IOException for convenience.
     */
    private Call createTopcatCall() throws IOException {
        try {
            Call call = (Call) new Service().createCall();
            call.setTargetEndpointAddress( endpoint_ );
            return call;
        }
        catch ( ServiceException e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }
    }

    /**
     * Displays a table in the TOPCAT application given its location.
     *
     * @param   location  table location (filename or URL)
     * @param   handler  name of table handler (or null for auto-detect)
     */
    public void displayTableByLocation( String location, String handler ) 
            throws IOException {
        Call call = createTopcatCall();
        call.setOperationName( "displayTableByLocation" );
        call.addParameter( "cookie", XMLType.SOAP_STRING, ParameterMode.IN );
        call.addParameter( "location", XMLType.SOAP_STRING, ParameterMode.IN );
        call.addParameter( "handler", XMLType.SOAP_STRING, ParameterMode.IN );
        call.setReturnType( XMLType.AXIS_VOID );
        call.invoke( new Object[] { cookie_, location, handler } );
    }

    /**
     * Displays a table directly in the TOPCAT application.
     * The SOAP transport is done using custom VOTable serialization.
     *
     * @param   table   table to display
     * @param   location  string indicating the table's source
     */
    public void displayTable( StarTable table, String location )
            throws IOException {
        Call call = createTopcatCall();
        VOTableSerialization.configureCall( call );
        call.setOperationName( "displayTable" );
        call.addParameter( "cookie", XMLType.SOAP_STRING, ParameterMode.IN );
        call.addParameter( "table", VOTableSerialization.QNAME_VOTABLE,
                           ParameterMode.IN );
        call.addParameter( "location", XMLType.SOAP_STRING, ParameterMode.IN );
        call.setReturnType( XMLType.AXIS_VOID );
        call.invoke( new Object[] { cookie_, table, location } );
    }

}
