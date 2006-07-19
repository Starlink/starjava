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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Logger;
import javax.swing.JFrame;
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
    private final PrintStream logOut_;
    private final PrintStream warnOut_;
    private boolean stopped_;
    private ApplicationListModel appListModel_;
    private PlasticHubListener hub_;
    private final String hubVersion_;
    private final MessageValidator validator_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.plastic" );

    private static int MAX_ARG_COUNT = 64;
    private static int MAX_ARG_LENG = 256;
    private static int TRUNC_ARG_LENG = Math.min( 64, MAX_ARG_LENG - 3 );

    /**
     * Constructor.
     *
     * @param  name   application name
     * @param  out    logging output stream
     * @param  hubVersion PLASTIC protocol version used by the hub to monitor
     */
    protected PlasticMonitor( String name, PrintStream logOut,
                              PrintStream warnOut, String hubVersion ) {
        name_ = name;
        logOut_ = logOut;
        warnOut_ = warnOut;
        hubVersion_ = hubVersion;
        validator_ = new MessageValidator();
    }

    public String getName() {
        return name_;
    }

    public URI[] getSupportedMessages() {
        float version;
        try {
            version = Float.parseFloat( hubVersion_.trim() );
        }
        catch ( RuntimeException e ) {
            version = -1f;
        }
        if ( version > 0.45 ) {
            URI[] msgs = MessageId.getKnownMessages();
            assert Arrays.asList( msgs ).contains( MessageId.HUB_APPREG );
            assert Arrays.asList( msgs ).contains( MessageId.HUB_APPUNREG );
            assert Arrays.asList( msgs ).contains( MessageId.HUB_STOPPING );
            return msgs;
        }

        /* Hub versions 0.4 and earlier used an empty array as a special
         * case meaning "send me all messages". */
        else {
            return new URI[ 0 ];
        }
    }

    public Object perform( URI sender, URI message, List args ) {
        if ( logOut_ != null ) {
            String summary = new StringBuffer()
                .append( stringify( sender ) )
                .append( ": " )
                .append( stringify( message ) )
                .append( stringify( args ) )
                .toString();
            logOut_.println( summary );
        }
        if ( warnOut_ != null ) {
            String[] warnings = validator_.validate( sender, message, args );
            if ( warnings.length > 0 ) {
                warnOut_.println( "WARNINGS: " );
                for ( int i = 0; i < warnings.length; i++ ) {
                    warnOut_.println( "    " + warnings[ i ] );
                }
            }
        }
        if ( MessageId.HUB_STOPPING.equals( message ) ) {
            stopped_ = true;
            if ( appListModel_ != null ) {
                appListModel_.clear();
            }
            synchronized ( this ) {
                notifyAll();
            }
        }
        if ( appListModel_ != null ) {
            if ( MessageId.HUB_APPREG.equals( message ) && 
                 args.size() > 0 ) {
                try {
                    URI id = new URI( args.get( 0 ).toString() );
                    appListModel_.register( id, hub_.getName( id ), 
                                            hub_.getUnderstoodMessages( id ) );
                }
                catch ( URISyntaxException e ) {
                }
            }
            else if ( MessageId.HUB_APPUNREG.equals( message ) ) {
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
    public static String stringify( Object value ) {
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
                int iel = 0;
                for ( Iterator it = set.iterator(); it.hasNext(); ) {
                    sbuf.append( ' ' );
                    if ( ++iel > MAX_ARG_COUNT ) {
                        sbuf.append( "..." );
                        break;
                    }
                    sbuf.append( stringify( it.next() ) );
                    sbuf.append( it.hasNext() ? ',' : ' ' );
                }
            }
            sbuf.append( ')' );
            return sbuf.toString();
        }
        else {
            String s = value == null ? "null" : value.toString();
            s = s.length() < MAX_ARG_LENG
              ? s
              : ( s.substring( 0, TRUNC_ARG_LENG - 3 ) + "..." );
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
        String usage = "\nUsage:"
                     + "\n       "
                     + PlasticMonitor.class.getName() 
                     + "\n           "
                     + " [-xmlrpc|-rmi]"
                     + " [-gui]"
                     + " [-warn]"
                     + " [-verbose]"
                     + " [-name name]"
                     + "\n";

        /* Process flags. */
        List argv = new ArrayList( Arrays.asList( args ) );
        String mode = "rmi";
        boolean gui = false;
        boolean verbose = false;
        boolean validate = false;
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
            else if ( "-warn".equals( arg ) ) {
                it.remove();
                validate = true;
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

        PrintStream logOut = verbose ? System.out : null;
        PrintStream warnOut = validate ? System.out : null;
        PlasticMonitor mon = new PlasticMonitor( name, logOut, warnOut,
                                                 getHubVersion() );
       
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

        if ( logOut != null ) {
            logOut.println( "Connnecting in " + mode + " mode..." );
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
        if ( logOut != null ) {
            logOut.println( "...connected." );
        }

        /* Wait on the monitor.  If it receives a HUB_STOPPING message
         * it will be notified and execution of this method can complete. */
        try {
            synchronized ( mon ) {
                while ( ! mon.stopped_ ) {
                    mon.wait();
                }
            }
            if ( logOut != null ) {
                logOut.println( "Hub stopped." );
            }
            System.exit( 0 );
        }
        catch ( InterruptedException e ) {
            System.out.println( "Interrupted." );
        }
    }

    /**
     * Returns the hub version as a string number, if it can.
     *
     * @return   hub version string
     */
    public static String getHubVersion() {
        try {
            PlasticHubListener hub = PlasticUtils.getLocalHub(); 
            URI hubId = hub.getHubId();
            URI clientId = hub.registerNoCallBack( "monitor-pre" );
            try {
                Map vMap =
                    hub.requestToSubset( clientId, MessageId.INFO_GETVERSION,
                                         new ArrayList(),
                                         Collections.singletonList( hubId ) );
                return vMap.get( hubId ).toString().trim();
            }
            finally {
                hub.unregister( clientId );
            }
        }
        catch ( Exception e ) {
            logger_.warning( "Unknown hub version: " + e );
            return "???";
        }
    }
}
