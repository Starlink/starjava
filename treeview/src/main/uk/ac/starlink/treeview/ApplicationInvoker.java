package uk.ac.starlink.treeview;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.logging.Logger;
import javax.xml.rpc.ParameterMode;
import javax.xml.rpc.ServiceException;
import javax.xml.transform.TransformerException;
import org.w3c.dom.Element;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.encoding.XMLType;
import uk.ac.starlink.ndx.Ndx;
import uk.ac.starlink.util.SourceReader;

/**
 * Takes care of invoking RPC calls on remote applications.
 * The available applications are provided as final static members 
 * (also instances) of this class.
 */
class ApplicationInvoker {

    private String name;
    private String displayNDXMethod;
    private String displayNDXParam;
    private URL endpoint;
    private String startupcmd;

    public final static String SPLATDIR_PROPERTY =
        "uk.ac.starlink.treeview.splatdir";
    public final static String SOGDIR_PROPERTY =
        "uk.ac.starlink.treeview.sogdir";

    private static Logger logger = 
        Logger.getLogger( "uk.ac.starlink.treeview" );

    /** Successive delays to wait for service to start up in. */
    private static long[] delays = { 10000, 10000, 10000, 10000, 10000 };

    /** Son Of Gaia external application invoker. */
    public static final ApplicationInvoker SOG = 
        new ApplicationInvoker( "SOG" );

    /** Splat external application invoker. */
    public static final ApplicationInvoker SPLAT = 
        new ApplicationInvoker( "SPLAT" );


    /**
     * Private sole initialiser sets up state for known instances.
     */
    private ApplicationInvoker( String name ) {
        try {

            /* Configure SOG. */
            if ( name.equals( "SOG" ) && Driver.hasAST ) {
                endpoint = new URL( 
                    "http://localhost:8082/services/SOGRemoteServices" );
                String sogdir = System.getProperty( SOGDIR_PROPERTY );
                if ( sogdir != null ) {
                    String sogcmd = sogdir + File.separator + "runSOG";
                    if ( new File( sogcmd ).canRead() ) {
                        startupcmd = sogcmd;
                        displayNDXMethod = "showNDX";
                        displayNDXParam = "element";
                    }
                    else {
                        logger.warning( "No sog executable " + sogcmd );
                    }
                }
                else {
                    logger.warning(
                        "No property " + SOGDIR_PROPERTY + " defined" );
                }
            }

            /* Configure SPLAT. */
            else if ( name.equals( "SPLAT" ) && Driver.hasAST ) {
                endpoint = new URL( 
                    "http://localhost:8081/services/SplatSOAPServices" );
                String splatdir = System.getProperty( SPLATDIR_PROPERTY );
                if ( splatdir != null ) {
                    String splatcmd = splatdir + File.separator + "runSPLAT";
                    if ( new File( splatcmd ).canRead() ) {
                        startupcmd = splatcmd;
                        displayNDXMethod = "displayNDX";
                        displayNDXParam = "element";
                    }
                    else {
                        logger.warning( "No splat executable " + splatcmd );
                    }
                }
                else {
                    logger.warning( 
                        "No property " + SPLATDIR_PROPERTY + " defined" );
                }
            }

        }
        catch ( MalformedURLException e ) {
            throw new AssertionError( e.getMessage() );
        }
    }


    /**
     * Indicates whether the NDX displaying service is available.
     */
    public boolean canDisplayNDX() {
        return displayNDXMethod != null;
    }

    /**
     * Invokes a method for displaying an NDX in the remote application,
     * if one exists.
     *
     * @param  ndx  the NDX object to display - must be persistent
     * @throws  ServiceException  if anything goes wrong, including that
     *          no displayNDX method is provided by the remote application,
     *          or the NDX is not persistent
     */
    public void displayNDX( Ndx ndx ) throws ServiceException {

        /* See if the service is provided. */
        if ( ! canDisplayNDX() ) {
            throw new ServiceException( 
                this + " does not provide displayNDX service" );
        }

        /* Get a DOM for the NDX if possible. */
        if ( ! ndx.isPersistent() ) {
            throw new ServiceException( 
                "Virtual NDX " + ndx + " cannot be displayed externally" );
        }
        Element el;
        try { 
            el = (Element) new SourceReader().getDOM( ndx.toXML( null ) );
        }
        catch ( TransformerException e ) {
            throw new ServiceException( "Error getting XML from NDX", e );
        }

        /* Set up and invoke a call to the remote service. */
        Call call = getCall();
        call.setOperationName( displayNDXMethod );
        call.addParameter( displayNDXParam, XMLType.SOAP_ELEMENT, 
                           ParameterMode.IN );
        call.setReturnType( XMLType.AXIS_VOID );
        doCall( call, new Object[] { el } );
    }


    /**
     * Perform the call.  Do this in a separate thread since we may be
     * in the event dispatcher thread and this may involve hanging
     * around waiting for network connections or starting up services.
     */
    private void doCall( final Call call, final Object[] args ) {
        new Thread() {
            public void run() {
                try {
                    attemptCall();
                }
                catch ( Exception e ) {
                    System.err.println( "Error attempting invocation by " +
                                        ApplicationInvoker.this.name );
                    e.printStackTrace();
                }
            }
            private void attemptCall()
                    throws InterruptedException, RemoteException, IOException {
                for ( int i = 0; i < delays.length; i++ ) {
                    try {
                        logger.info( "Attempt display in " + 
                                     ApplicationInvoker.this.name + ".." );
                        call.invoke( args );
                        logger.info( "..ok" );
                        break;
                    }
                    catch ( RemoteException e ) {

                        /* Looks like the service doesn't exist yet. */
                        if ( e.getCause() instanceof ConnectException ) {
                            logger.info( "..retry" );

                            /* If we haven't attempted to start it yet, do so.*/
                            if ( i == 0 ) {
                                startService();
                            }

                            /* Give it some time (or some more time) to lurch
                             * into action, then loop again to have another
                             * go at invoking the call. */
                            Thread.currentThread().sleep( delays[ i ] );
                        }

                        /* Something else went wrong - rethrow it. */
                        else {
                            logger.warning( "..fail" );
                            throw e;
                        }
                    }
                }
            }
        }.start();
    }


    /**
     * Sets up an RPC call for this object's handled application.
     */
    private Call getCall() throws ServiceException {

        /* Get a service object. */
        Service service = new Service();

        /* Construct and configure the call. */
        Call call = (org.apache.axis.client.Call) service.createCall();
        call.setTargetEndpointAddress( endpoint );
        return call;
    }

    
    public void startService() throws IOException {
        logger.info( "Starting server: " + startupcmd );
        Runtime.getRuntime().exec( startupcmd );
    }

    public String toString() {
        return name;
    }

}
