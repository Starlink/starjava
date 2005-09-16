package uk.ac.starlink.ttools.mode;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.rmi.RemoteException;
import java.util.Iterator;
import javax.xml.rpc.ParameterMode;
import javax.xml.rpc.ServiceException;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.encoding.XMLType;
import uk.ac.starlink.soap.util.RemoteUtilities;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.ttools.TableConsumer;
import uk.ac.starlink.votable.soap.VOTableSerialization;

/**
 * Processing mode for displaying the streamed table in TOPCAT.
 * If a TOPCAT server is currently running, a remote display message
 * will be sent.  Otherwise, a new TOPCAT server will be started and
 * a remote message will be sent to that.
 * At time of writing the message transport uses AXIS 1.1 which puts
 * everything into a DOM at at least one end, which means it doesn't
 * work well for large tables, but with a better SOAP implementation
 * (AXIS 1.2?) it would sort itself out.
 *
 * @author   Mark Taylor (Starlink)
 * @since    24 Mar 2005
 */
public class TopcatMode implements ProcessingMode, TableConsumer {

    /**
     * Fail with an undeclared throwable at load time if this class isn't
     * going to be able to function.
     */
    static {
        checkRequisites();
    }

    public Parameter[] getAssociatedParameters() {
        return new Parameter[ 0 ];
    }

    public String getDescription() {
        return new StringBuffer()
       .append( "Displays the output table directly in\n" )
       .append( "<webref url=\"http://www.starlink.ac.uk/topcat/\"\n" )
       .append( ">TOPCAT</webref>.\n" )
       .append( "If a TOPCAT instance (version 1.6 or later) is already\n" )
       .append( "running on the local host, the table will be opened in\n" )
       .append( "that, otherwise a new TOPCAT instance will be launched\n" )
       .append( "for display.\n" )
       .append( "The latter mode only works if the TOPCAT classes are\n" )
       .append( "on the class path.\n" )
       .append( "There are currently limits to the size of table\n" )
       .append( "that can be transmitted to the application in this way -\n" )
       .append( "it is hoped that this can be improved in a future\n" )
       .append( "release.\n" )
       .toString();
    }

    public TableConsumer createConsumer( Environment env ) {
        return this;
    }

    public void consume( StarTable table ) throws IOException {
        try {
            try {
                remoteDisplay( table );
            }
            catch ( ConnectException e ) {
                startServer();
                remoteDisplay( table );
            }
        }
        catch ( ServiceException e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }
    }

    /**
     * Attempts to display a table in a running TOPCAT server.
     *
     * @param  table  table to display
     */
    private void remoteDisplay( StarTable table )
            throws ConnectException, ServiceException, IOException {

        String srcName = table.getName();
        if ( srcName == null || srcName.trim().length() == 0 ) {
            srcName = "(streamed)";
        }

        Object[] tcServ = RemoteUtilities.readContactFile( "topcat" );
        if ( tcServ == null ) {
            throw new ConnectException( "No contact file - looks like " 
                                      + "no TOPCAT server is running" );
        }
        String host = (String) tcServ[ 0 ];
        int port = ((Integer) tcServ[ 1 ]).intValue();
        String cookie = (String) tcServ[ 2 ];
        String endpoint = "http://" + host + ":" + port + 
                          "/services/TopcatSOAPServices";

        Call call = (Call) new Service().createCall();
        call.setTargetEndpointAddress( endpoint );
        VOTableSerialization.configureCall( call );
        call.setOperationName( "displayTable" );
        call.addParameter( "cookie", XMLType.SOAP_STRING, ParameterMode.IN );
        call.addParameter( "table", VOTableSerialization.QNAME_VOTABLE,
                           ParameterMode.IN );
        call.addParameter( "location", XMLType.SOAP_STRING, ParameterMode.IN );
        call.setReturnType( XMLType.AXIS_VOID );
        try {
            call.invoke( new Object[] { cookie, table, srcName } );
        }
        catch ( RemoteException e ) {
            Throwable e2 = e.getCause();
            if ( e2 instanceof ConnectException ) {
                String msg = "Connection refused: TOPCAT server not running?";
                throw (ConnectException) new ConnectException( msg )
                                        .initCause( e2 );
            }
            else if ( e2 instanceof IOException ) {
                throw (IOException) e2;
            }
            else {
                throw e;
            }
        }
    }

    /**
     * Starts up a TOPCAT server.
     */
    private void startServer() throws IOException {
        try {
            Class clazz = Class.forName( "uk.ac.starlink.topcat.Driver" );
            final Method main =
                clazz.getMethod( "main", new Class[] { String[].class } );
            main.invoke( null, new Object[] { new String[ 0 ] } );
        }
        catch ( ClassNotFoundException e ) {
            throw (IOException) new IOException( "TOPCAT not available" )
                               .initCause( e );
        }
        catch ( Exception e ) {
            throw (IOException) new IOException( "Can't start TOPCAT" )
                               .initCause( e );
        }
    }

    /**
     * Throws an exception if there aren't enough classes on the classpath
     * to be able to attempt processing in this mode.
     */
    private static void checkRequisites() {
        RemoteUtilities.class.getName();
        org.apache.axis.encoding.Target.class.getName();
    }
}
