package uk.ac.starlink.treeview;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import javax.xml.rpc.ParameterMode;
import javax.xml.rpc.ServiceException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.encoding.XMLType;
import org.w3c.dom.Element;
import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.datanode.nodes.NodeUtil;
import uk.ac.starlink.hdx.HdxException;
import uk.ac.starlink.ndx.Ndx;
import uk.ac.starlink.ndx.NdxIO;
import uk.ac.starlink.util.SourceReader;

/**
 * Abstract class which encapsulates a service that can display NDXs.
 * The display can in general be achieved in two ways - using SOAP
 * communication to send the NDX as XML over the wire to a suitable 
 * existing SOAP endpoint, or entirely within the same JVM by manipulating
 * the Ndx object in some way.
 */
public abstract class NdxDisplayer {

    private URL endpoint;
    private String displayMethod;

    /**
     * Constructs a new NdxDisplayer, describing the SOAP service
     * which can be used for RPC.
     * <p>
     * The arguments <tt>endpoint</tt> and <tt>displayMethod</tt>
     * relate to the SOAP sercice for RPC display. 
     * If any are <tt>null</tt>, it is assumed that no
     * SOAP display service is available, and only local display will
     * be attempted.
     *
     * @param   endpoint the endpoint for the SOAP communication
     *          (must be a valid URL)
     * @param   displayMethod  the operation name for the display service
     */
    protected NdxDisplayer( String endpoint, String displayMethod ) {
        try {
            this.endpoint = new URL( endpoint );
        }
        catch ( MalformedURLException e ) {
            throw (IllegalArgumentException) 
                  new IllegalArgumentException( "Bad URL " + endpoint )
                 .initCause( e );
        }
        this.displayMethod = displayMethod;
    }

    /**
     * Attempts to display the given NDX using a locally created
     * object. If this is for use in an embedded sense then embedded
     * should be true (in this case local created objects should
     * expect to be not remotely controlled and not take charge of
     * application exit).
     *
     * @param  ndx  the NDX to display
     * @param  embedded is the local object to be embedded
     * @return  true if the display was successful
     */
    public abstract boolean localDisplay( Ndx ndx, boolean embedded );

    /**
     * Indicates whether this displayer can be expected to display the
     * given NDX.  The NdxDisplayer implementation returns true.
     *
     * @param   ndx  the Ndx to display
     * @return   whether it's worth trying to call {@link #display}
     */
    public boolean canDisplay( Ndx ndx ) {
        return true;
    }

    /**
     * Attempts to display the given NDX.
     * Display may be in the same or a different JVM depending on
     * whether the remote service is available and works. If the
     * display takes place in the local JVM then the embedded
     * parameter is used to control whether the displayer takes charge
     * of application exit (it may also activate a full set of
     * services, such as remote control).
     *
     * @param  ndx  the NDX to display
     * @param  embedded whether a local displayer should not assume it is
     *                  the full application instance, or not
     * @return  true if the display was successful
     */
    public boolean display( Ndx ndx, boolean embedded ) {
        return soapDisplay( ndx )
            || localDisplay( ndx, embedded );
    }

    /**
     * Attempts to display the given NDX using remote communication.
     *
     * @param  ndx  the NDX to display
     * @return  true iff the display was successful
     */
    public boolean soapDisplay( Ndx ndx ) {
        if ( endpoint == null || 
             displayMethod == null ) {
            return false;
        }
        if ( ! ndx.isPersistent() ) {
            return false;
        }

        /* Get a DOM from the NDX if possible. */
        Element el;
        try {
            // Source xsrc = ndx.toXML( null );  // deprecated
            Source xsrc = ndx.getHdxFacade().getSource( null );
            el = (Element) new SourceReader().getDOM( xsrc );
        }
        catch ( TransformerException e ) {
            return false;
        }
        catch ( HdxException e ) {
            return false;
        }

        /* Try to invoke a call to the remote service. */
        try {
            attemptCall( el );
            return true;
        }
        catch ( Exception e ) {
            return false;
        }
    }

    private void attemptCall( Element el )
            throws RemoteException, ServiceException {
        Call call = getCall();
        call.setOperationName( displayMethod );
        call.addParameter( "element", XMLType.SOAP_ELEMENT, ParameterMode.IN );
        call.setReturnType( XMLType.AXIS_VOID );
        call.invoke( new Object[] { el } );
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

    /**
     * Attempts to display one or more NDXs using an appropriate viewer.
     * Currently, it will pass a 1-d NDX to SPLAT and a 2-d one to SoG.
     * If there is a problem displaying any of the NDXs a message will
     * be printed to standard error.
     *
     * @param  args  an array of NDX names
     */
    public static void main( String[] args ) {
        NdxIO ndxIO = new NdxIO();
        boolean trouble = false;
        for ( int i = 0; i < args.length; i++ ) {
            String arg = args[ i ];
            try {
                Ndx ndx = ndxIO.makeNdx( arg, AccessMode.READ );
                String failmsg = null;
                if ( ndx == null ) {
                    failmsg = "not an NDX";
                }
                else {
                    NDShape shape = ndx.getImage().getShape();
                    int ndim = shape.getNumDims();
                    if ( ndim == 1 ) {
                        if ( ! SplatNdxDisplayer.getInstance()
                                                .display( ndx, false ) ) {
                            failmsg = "SPLAT failed to display";
                        }
                    }
                    else if ( ndim == 2 ) {
                        if ( ! SogNdxDisplayer.getInstance()
                                              .display( ndx, false ) ) {
                            failmsg = "SoG failed to display";
                        }
                    }
                    else {
                        failmsg = "No displayer for " + ndim + " dimensions";
                    }
                }
                if ( failmsg != null ) {
                    System.err.println( arg + ": " + failmsg );
                    trouble = true;
                }
            }
            catch ( IOException e ) {
                System.err.println( arg + ": " + e.toString() );
            }
        }
    }
}
