package uk.ac.starlink.plastic;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import javax.swing.JFrame;
import org.apache.xmlrpc.WebServer;
import org.apache.xmlrpc.XmlRpcClient;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcHandler;
import org.votech.plastic.PlasticHubListener;

/**
 * Watches and reports on messages sent over a PLASTIC message bus.
 * Designed principally to aid with debugging, both of PLASTIC infrastructure
 * and of PLASTIC-aware applications.
 *
 * <p>This class is intended to be used standalone from its {@link #main}
 * method.
 *
 * @author   Mark Taylor
 * @since    17 Feb 2006
 */
public class PlasticMonitor implements PlasticApplication {

    private final String name_;
    private final PrintStream out_;
    private boolean stopped_;
    private ApplicationListModel appListModel_;
    private PlasticHubListener hub_;

    /**
     * Constructor.
     *
     * @param  name   application name
     * @param  out    logging output stream
     */
    protected PlasticMonitor( String name, PrintStream out ) {
        name_ = name;
        out_ = out;
    }

    public String getName() {
        return name_;
    }

    public URI[] getSupportedMessages() {
        return new URI[ 0 ];
    }

    public Object perform( URI sender, URI message, List args ) {
        String summary = new StringBuffer()
            .append( stringify( sender ) )
            .append( ": " )
            .append( stringify( message ) )
            .append( stringify( args ) )
            .toString();
        if ( out_ != null ) {
            out_.println( summary );
        }
        if ( PlasticHub.HUB_STOPPING.equals( message ) ) {
            stopped_ = true;
            if ( appListModel_ != null ) {
                appListModel_.clear();
            }
            synchronized ( this ) {
                notifyAll();
            }
        }
        if ( appListModel_ != null ) {
            if ( PlasticHub.APP_REG.equals( message ) && 
                 args.size() > 0 ) {
                try {
                    URI id = new URI( args.get( 0 ).toString() );
                    appListModel_.register( id, hub_.getName( id ), 
                                            hub_.getUnderstoodMessages( id ) );
                }
                catch ( URISyntaxException e ) {
                }
            }
            else if ( PlasticHub.APP_UNREG.equals( message ) ) {
                try {
                    URI id = new URI( args.get( 0 ).toString() );
                    appListModel_.unregister( id );
                }
                catch ( URISyntaxException e ) {
                }
            }
        }
        return Boolean.FALSE;
    }

    /**
     * Sets the hub this monitor is listening to.
     *
     * @param  hub  hub
     */
    private void setHub( PlasticHubListener hub ) {
        hub_ = hub;
    }

    /**
     * Sets a list model this monitor should keep up to date.
     *
     * @param   listModel  model of registered applications
     */
    private void setListModel( ApplicationListModel listModel ) {
        appListModel_ = listModel;
    }

    /**
     * Stringifies an object for logging purposes.
     *
     * @param  value  object to stringify
     * @return  human-readable version of value
     */
    private String stringify( Object value ) {
        if ( value == null ) {
            return "null";
        }
        else if ( value instanceof URI ) {
            return value.toString();
        }
        else if ( value instanceof Collection ) {
            Collection set = (Collection) value;
            StringBuffer sbuf = new StringBuffer();
            sbuf.append( '(' );
            if ( ! set.isEmpty() ) {
                for ( Iterator it = set.iterator(); it.hasNext(); ) {
                    sbuf.append( ' ' );
                    sbuf.append( stringify( it.next() ) );
                    sbuf.append( it.hasNext() ? ',' : ' ' );
                }
            }
            sbuf.append( ')' );
            return sbuf.toString();
        }
        else {
            String s = value == null ? "null" : value.toString();
            s = s.length() < 40 ? s : ( s.substring( 0, 37 ) + "..." );
            s = s.replaceAll( "\n", "\\n" );
            return s;
        }
    }

    /**
     * Starts a monitor of the PLASTIC message bus which logs message
     * descriptions to standard output.
     * A choice of Java-RMI and XML-RPC communication is offered.
     *
     * <h2>Flags</h2>
     * <dl>
     * <dt>-rmi</dt>
     * <dd>Use Java-RMI for communications (default)</dd>
     * <dt>-xmlrpc</dt>
     * <dd>Use XML-RPC for communications</dd>
     * <dt>-gui</dt>
     * <dd>Pops up a window monitoring currently registered applications</dd>
     * <dt>-verbose</dt>
     * <dd>Writes a log to standard output of all PLASTIC traffic</dd>
     * <dt>-name name</dt>
     * <dd>Supply application name which monitor will register under</dd>
     * </dl>
     */
    public static void main( String[] args ) throws IOException {
        String usage = "\nUsage: " + PlasticMonitor.class.getName() 
                     + " [-xmlrpc|-rmi]"
                     + " [-gui]"
                     + " [-verbose]"
                     + " [-name name]"
                     + "\n";

        /* Process flags. */
        List argv = new ArrayList( Arrays.asList( args ) );
        String mode = "rmi";
        boolean gui = false;
        boolean verbose = false;
        String name = "monitor";
        for ( Iterator it = argv.iterator(); it.hasNext(); ) {
            String arg = (String) it.next();
            if ( "-xmlrpc".equals( arg ) ) {
                it.remove();
                mode = "xmlrpc";
            }
            else if ( "-rmi".equals( arg ) ) {
                it.remove();
                mode = "rmi";
            }
            else if ( "-gui".equals( arg ) ) {
                it.remove();
                gui = true;
            }
            else if ( "-verbose".equals( arg ) ) {
                it.remove();
                verbose = true;
            }
            else if ( "-name".equals( arg ) && it.hasNext() ) {
                it.remove();
                name = (String) it.next();
                it.remove();
            }
            else if ( arg.startsWith( "-h" ) ) {
                System.out.println( usage );
                return;
            }
        }
        if ( ! argv.isEmpty() ) {
            System.err.println( usage );
            System.exit( 1 );
        }

        PrintStream out = verbose ? System.out : null;
        PlasticMonitor mon = new PlasticMonitor( name, out );
       
        if ( gui ) {
            PlasticHubListener hub = PlasticUtils.getLocalHub();
            ApplicationItem[] regApps =
                PlasticUtils.getRegisteredApplications( hub );
            ApplicationListModel appsList = new ApplicationListModel( regApps );
            mon.setListModel( appsList );
            mon.setHub( hub );
            JFrame window = new ListWindow( appsList );
            window.setTitle( "PlasticMonitor" );
            window.pack();
            window.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
            window.setVisible( true );
        }

        if ( out != null ) {
            out.println( "Connnecting in " + mode + " mode..." );
        }
        if ( "rmi".equals( mode ) ) {
            PlasticUtils.registerRMI( mon );
        }
        else if ( "xmlrpc".equals( mode ) ) {
            PlasticUtils.registerXMLRPC( mon );
        }
        else {
            assert false;
        }
        if ( out != null ) {
            out.println( "...connected." );
        }

        /* Wait on the monitor.  If it receives a HUB_STOPPING message
         * it will be notified and execution of this method can complete. */
        try {
            synchronized ( mon ) {
                while ( ! mon.stopped_ ) {
                    mon.wait();
                }
            }
            if ( out != null ) {
                out.println( "Hub stopped." );
            }
            System.exit( 0 );
        }
        catch ( InterruptedException e ) {
            System.out.println( "Interrupted." );
        }
    }
}
