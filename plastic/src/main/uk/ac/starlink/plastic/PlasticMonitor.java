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
    private final boolean multiclient_;
    private final PrintStream logOut_;
    private final PrintStream warnOut_;
    private boolean stopped_;
    private ApplicationListModel appListModel_;
    private PlasticHubListener hub_;
    private final MessageValidator validator_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.plastic" );

    private static int MAX_ARG_COUNT = 64;
    private static int MAX_ARG_LENG = 256;
    private static int TRUNC_ARG_LENG = Math.min( 64, MAX_ARG_LENG - 3 );
    private static final Object NULL = new Vector();

    /**
     * Constructor.
     *
     * @param  name   application name
     * @param  multiclient  true if you want it to attempt to implement
     *                      all known messages
     * @param  logOut    logging output stream
     * @param  warnOut   warning output stream
     */
    public PlasticMonitor( String name, boolean multiclient,
                           PrintStream logOut, PrintStream warnOut ) {
        name_ = name;
        multiclient_ = multiclient;
        logOut_ = logOut;
        warnOut_ = warnOut;
        validator_ = new MessageValidator();
    }

    public String getName() {
        return name_;
    }

    public URI[] getSupportedMessages() {
        return multiclient_ ? MessageId.getKnownMessages()
                            : new URI[] {
                                  MessageId.HUB_APPREG, 
                                  MessageId.HUB_APPUNREG,
                                  MessageId.HUB_STOPPING,
                                  MessageId.INFO_GETNAME,
                                  MessageId.INFO_GETDESCRIPTION,
                                  MessageId.INFO_GETICONURL,
                                  MessageId.INFO_GETVERSION,
                                  MessageId.TEST_ECHO, 
                              };
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
            String[] warnings =
                validator_.validateRequest( sender, message, args );
            if ( warnings.length > 0 ) {
                warnOut_.println( "WARNINGS: " );
                for ( int i = 0; i < warnings.length; i++ ) {
                    warnOut_.println( "    " + warnings[ i ] );
                }
            }
        }
        Object result = doPerform( sender, message, args );
        if ( logOut_ != null ) {
            logOut_.println(
                "\t->" +
                ( result == null
                     ? "null"
                     : "(" + result.getClass().getName() + "): " + result ) );
        }
        if ( warnOut_ != null ) {
            String[] warnings = validator_.validateResponse( message, result );
            if ( warnings.length > 0 ) {
                warnOut_.println( "WARNINGS: " );
                for ( int i = 0; i < warnings.length; i++ ) {
                    warnOut_.println( "    " + warnings[ i ] );
                }
            }
        }
        return result;
    }

    /**
     * Does the actual message handling.
     *
     * @param  sender  sender ID
     * @param  message  message ID
     * @param  args   message argument list
     */
    private Object doPerform( URI sender, URI message, List args ) {
        if ( MessageId.HUB_STOPPING.equals( message ) ) {
            stopped_ = true;
            if ( appListModel_ != null ) {
                appListModel_.clear();
            }
            synchronized ( this ) {
                notifyAll();
            }
            return NULL;
        }
        else if ( MessageId.INFO_GETNAME.equals( message ) ) {
            return name_;
        }
        else if ( MessageId.INFO_GETDESCRIPTION.equals( message ) ) {
            return "Plastic message monitor";
        }
        else if ( MessageId.INFO_GETICONURL.equals( message ) ) {
            return "http://www.star.bris.ac.uk/~mbt/plastic/images/eye.gif";
        }
        else if ( MessageId.INFO_GETVERSION.equals( message ) ) {
            return PlasticUtils.PLASTIC_VERSION;
        }
        else if ( MessageId.TEST_ECHO.equals( message ) ) {
            return args.size() > 0 ? args.get( 0 ) : "";
        }
        else if ( MessageId.HUB_APPREG.equals( message ) ) {
            if ( appListModel_ != null && args.size() > 0 ) {
                try {
                    URI id = new URI( args.get( 0 ).toString() );
                    String name = hub_.getName( id );
                    List msgs = hub_.getUnderstoodMessages( id );
                    if ( id != null && name != null && msgs != null ) {
                        appListModel_.register( id, name, msgs );
                    }
                }
                catch ( URISyntaxException e ) {
                }
            }
            return NULL;
        }
        else if ( MessageId.HUB_APPUNREG.equals( message ) ) {
            if ( appListModel_ != null && args.size() > 0 ) {
                try {
                    URI id = new URI( args.get( 0 ).toString() );
                    appListModel_.unregister( id );
                }
                catch ( URISyntaxException e ) {
                }
            }
            return NULL;
        }
        else if ( multiclient_ ) {
            MessageDefinition def = MessageDefinition.getMessage( message );
            if ( def != null ) {
                return def.getReturnType().getBlankValue();
            }
        }

        /* Message not known. */
        if ( warnOut_ != null ) {
            warnOut_.println( "Unsolicited message " + message );
        }
        return NULL;
    }

    /**
     * Sets the hub this monitor is listening to.
     *
     * @param  hub  hub
     */
    public void setHub( PlasticHubListener hub ) {
        hub_ = hub;
    }

    /**
     * Sets a list model this monitor should keep up to date.
     *
     * @param   listModel  model of registered applications
     */
    public void setListModel( ApplicationListModel listModel ) {
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
     * <dt>-warn</dt>
     * <dd>Writes a log to standard output of illegal or questionable
     *     conditions</dt>
     * <dt>-multi</dt>
     * <dd>Attempt to implement as many messages as possible
     *     (a dummy implementation is provided for every message know by
     *     the {@link MessageDefinition} class)</dd>
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
                     + " [-verbose]"
                     + " [-warn]"
                     + " [-multi]"
                     + " [-name name]"
                     + "\n";

        /* Process flags. */
        List argv = new ArrayList( Arrays.asList( args ) );
        String mode = "rmi";
        boolean gui = false;
        boolean verbose = false;
        boolean validate = false;
        boolean multiclient = false;
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
            else if ( "-multi".equals( arg ) ) {
                it.remove();
                multiclient = true;
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
        PlasticMonitor mon =
            new PlasticMonitor( name, multiclient, logOut, warnOut );
        if ( gui ) {
            PlasticHubListener hub = PlasticUtils.getLocalHub();
            ApplicationItem[] regApps =
                PlasticUtils.getRegisteredApplications( hub );
            ApplicationListModel appsList = new ApplicationListModel( regApps );
            mon.setListModel( appsList );
            mon.setHub( hub );
            JFrame window = new PlasticListWindow( appsList );
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
}
