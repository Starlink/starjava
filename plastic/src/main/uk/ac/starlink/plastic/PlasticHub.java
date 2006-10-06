package uk.ac.starlink.plastic;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.JFrame;
import javax.swing.ListModel;
import org.votech.plastic.PlasticHubListener;

/**
 * Plastic hub implementation which provides some user friendly featuers
 * such as logging, message validation,
 * a {@link javax.swing.ListModel} which keeps track of
 * registered applications, and a {@link #main} method to start it up.
 *
 * @author   Mark Taylor
 * @since    17 Feb 2006
 */
public class PlasticHub extends MinimalHub {

    private final Map agentMap_;
    private PrintStream logOut_;
    private PrintStream warnOut_;
    private boolean verbose_;
    private boolean warnings_;
    private ApplicationListModel listModel_;
    private MessageValidator validator_;
    private int nReq_;

    /**
     * Constructs a new hub, given running server objects.
     *
     * @param   servers   object encapsulating listening servers
     */
    public PlasticHub( ServerSet servers ) throws RemoteException {
        super( servers );
        agentMap_ = getAgentMap();  // synchronized map
    }

    void register( Agent agent ) {
        if ( verbose_ ) {
            log( "Register: " + agent );
            log( "    ID:" );
            log( "        " + agent.getId() );
            log( "    Connection:" );
            log( "        " + agent.getConnection() );
            URI[] msgs = agent.getSupportedMessages();
            if ( msgs.length > 0 ) {
                log( "    Supported Messages:" );
                for ( int i = 0; i < msgs.length; i++ ) {
                    log( "        " + msgs[ i ] );
                }
            }
        }
        if ( warnings_ ) {
            URI[] msgs = agent.getSupportedMessages();
            for ( int i = 0; i < msgs.length; i++ ) {
                if ( validator_.getDefinition( msgs[ i ] ) == null ) {
                    warn( "Unknown message: " + msgs[ i ] );
                }
            }
        }
        if ( verbose_ ) {
            log( "" );
        }
        if ( listModel_ != null ) {
            listModel_.register( agent.getId(), agent.getName(),
                                 Arrays.asList( agent
                                               .getSupportedMessages() ) );
        }
        super.register( agent );
    }

    public void unregister( URI id ) {
        Agent agent = (Agent) agentMap_.get( id );
        if ( agent != null ) {
            if ( listModel_ != null ) {
                listModel_.unregister( id );
            }
            if ( verbose_ ) {
                log( "Unregister: " + agent );
                log( "" );
            }
        }
        else {
            if ( warnings_ ) {
                warn( "Attempt to unregister unknown listener: "
                    + id );
            }
        }
        super.unregister( id );
    }

    public String getName( URI id ) {
        String result = super.getName( id );
        if ( warnings_ && result == null ) {
            warn( "getName() request for unknown listener: " + id );
        }
        return result;
    }

    public List getUnderstoodMessages( URI id ) {
        List result = super.getUnderstoodMessages( id );
        if ( warnings_ && result == null ) {
            warn( "getUnderstoodMessages() request for unknown listener: "
                + id );
        }
        return result;
    }

    /**
     * Returns a ListModel which represents the listener applications
     * currently registered with this hub.  The model will be updated
     * as applications register and unregister themselves.
     *
     * @return  list model reflecting hub state
     */
    public synchronized ListModel getApplicationListModel() {

        /* Construct the model lazily so that we don't start firing off
         * Swing events if nobody is going to be using them. */
        if ( listModel_ == null ) {

            /* Prepare an intial list of applications. */
            List appList = new ArrayList();
            synchronized ( agentMap_ ) {
                for ( Iterator it = agentMap_.entrySet().iterator();
                      it.hasNext(); ) {
                    Agent agent = (Agent) ((Map.Entry) it.next()).getValue();
                    List msgList =
                        Arrays.asList( agent.getSupportedMessages() );
                    appList.add( new ApplicationItem( agent.getId(),
                                                      agent.getName(),
                                                      msgList ) );
                }
            }
            ApplicationItem[] apps =
                (ApplicationItem[]) appList.toArray( new ApplicationItem[ 0 ] );

            /* Construct a model based on these. */
            listModel_ = new ApplicationListModel( apps );
        }
        return listModel_;
    }

    Map requestTo( URI sender, URI message, List args, Agent[] agents ) {
        logRequest( sender, message, args, agents, true );
        return super.requestTo( sender, message, args, agents );
    }

    void requestAsynchTo( URI sender, URI message, List args, Agent[] agents ) {
        logRequest( sender, message, args, agents, false );
        super.requestAsynchTo( sender, message, args, agents );
    }

    /** 
     * Logs information as per current configuration about a message request
     * which is about to be sent.
     *
     * @param  sender  sender ID
     * @param  message  message ID
     * @param  args    message arguments
     * @param  agents  list of agents to which the request will be multiplexed
     * @param  isSynch  true iff the message will be sent synchronously
     */
    private void logRequest( URI sender, URI message, List args,
                             Agent[] agents, boolean isSynch ) {

        /* Log request with unique ID. */
        final int reqId = ++nReq_;
        if ( verbose_ ) {
            log( ( isSynch ? "Synchronous" : "Asynchronous" )
               + " request " + reqId );
            log( "    Sender:  " + stringify( sender ) );
            log( "    Message: " + message );
            if ( args.size() > 0 ) {
                log( "    Args:    " + stringify( args ) );
            }
        }
        if ( warnings_ ) {
            if ( ! isRegistered( sender ) ) {
                warn( "    request from unknown listener: " + sender );
            }
            String[] warnlines =
                validator_.validateRequest( sender, message, args );
            for ( int i = 0; i < warnlines.length; i++ ) {
                warn( "    !! " + warnlines[ i ] );
            }
        }
        if ( verbose_ ) {
            log( "" );
        }
    }

    RequestThread createRequestThread( final Agent agent, URI sender,
                                       final URI message, List args ) {
        return new RequestThread( agent, sender, message, args ) {
            public void run() {
                if ( verbose_ ) {
                    log( "        -> " + agent );
                }
                super.run();
                if ( verbose_ ) {
                    String result;
                    try {
                        result = stringify( getResult() );
                    }
                    catch ( IOException e ) {
                        result = stringify( e );
                    }
                    log( "        <- " + agent + ": " + result );
                }
                if ( warnings_ ) {
                    try { 
                        String[] warnlines = 
                            validator_.validateResponse( message, getResult() );
                        for ( int i = 0; i < warnlines.length; i++ ) {
                            warn( "    !! " + warnlines[ i ] );
                        }
                    }
                    catch ( IOException e ) {
                        // error getting result - this will already have
                        // been logged
                    }
                }
            }
        };
    }

    public void stop() {
        if ( verbose_ && ! isStopped() ) {
            log( "Hub stopped." );
        }
        super.stop();
    }

    /**
     * Sets a stream for this hub to perform logging to. 
     * If <code>out</code> is null (the default), no logging is performed.
     *
     * @param  out  logging print stream
     */
    public void setLogStream( PrintStream out ) {
        verbose_ = out != null;
        logOut_ = out;
    }

    /**
     * Sets a stream for this hub to log warnings (about validation of
     * messages etc) to.
     * If <code>out</code> is null (the default), no logging is performed.
     *
     * @param  out  warning print stream
     */
    public void setWarningStream( PrintStream out ) {
        warnings_ = out != null;
        warnOut_ = out;
        if ( warnings_ && validator_ == null ) {
            validator_ = new MessageValidator();
        }
    }

    /**
     * Writes a line of logging output.
     *
     * @param   line   line to write
     */
    private void log( String line ) {
        logOut_.println( line );
    }

    /**
     * Writes a line of warning output.
     *
     * @param  line  line to write
     */
    private void warn( String line ) {
        warnOut_.println( line );
    }

    /**
     * Turns a value into a human-readable string.
     * This is used for serialising argument lists etc prior to writing
     * them to a logging stream.
     *
     * @param  value  value
     * @return  stringified form
     */
    private String stringify( Object value ) {
        if ( value == null ) {
            return "null";
        }
        else if ( value instanceof URI ) {
            Object agent = agentMap_.get( value );
            if ( agent instanceof Agent ) {
                return "id:" + agent.toString();
            }
            else if ( value.equals( getHubId() ) ) {
                return "id:" + "hub";
            }
            else {
                return value.toString();
            }
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
        else if ( value instanceof Throwable ) {
            ((Throwable) value).printStackTrace();
            return value.toString();
        }
        else {
            String s = value == null ? "null" : value.toString();
            s = s.length() < 60 ? s : ( s.substring( 0, 57 ) + "..." );
            s = s.replaceAll( "\n", "\\n" );
            return s;
        }
    }

    /**
     * Determines whether a client ID represents a currently registered
     * application.
     *
     * @param  id  client ID
     */
    private boolean isRegistered( URI id ) {
        return agentMap_.containsKey( id );
    }

    /**
     * Creates and starts a PlasticHub running, writing its config information
     * to the default file and optionally logging output to a print stream.
     * The config file will be deleted automatically if the hub stops running.
     *
     * @param  logOut  logging output stream (may be null for no logging)
     * @param  warnOut  logging stream for warnings (may be null for no logging)
     */
    public static PlasticHub startHub( PrintStream logOut, PrintStream warnOut )
            throws IOException, RemoteException {
        return startHub( logOut, warnOut,
                         new File( System.getProperty( "user.home" ),
                         PLASTIC_CONFIG_FILENAME ) );
    }

    /**
     * Creates and starts a PlasticHub running, optionally
     * writing the config information into a given file and
     * logging output to a print stream.
     * The config file is usually 
     * {@link org.votech.plastic.PlasticHubListener#PLASTIC_CONFIG_FILENAME}
     * in the user's home directory.  This file will be deleted automatically
     * under normal circumstances.
     *
     * @param   configFile  file to write setup information to,
     *          if null no file is written
     * @param  logOut  logging output stream (may be null for no logging)
     * @param  warnOut  logging stream for warnings (may be null for no logging)
     */
    public static PlasticHub startHub( PrintStream logOut, PrintStream warnOut,
                                       File configFile )
            throws RemoteException, IOException {
        final ServerSet servers = new ServerSet( configFile );
        final PlasticHub hub = new PlasticHub( servers );
        hub.setLogStream( logOut );
        hub.setWarningStream( warnOut );
        if ( logOut != null ) {
            logOut.println( "Hub started." );
        }
        Runtime.getRuntime().addShutdownHook( new Thread() {
            public void run() {
                hub.stop();
            }
        } );
        return hub;
    }

    /**
     * Starts a hub.
     *
     * <h2>Flags</h2>
     * <dl>
     * <dt>-verbose</dt>
     * <dd>Causes verbose messages to be written to standard output
     *     logging hub operations.</dd>
     * <dt>-warn</dt>
     * <dd>Causes extra validation and warning messages to be output
     *     in the case of hub interactions which do not follow the letter
     *     of the protocol.</dd>
     * <dt>-gui</dt>
     * <dd>Pops up a graphical window which monitors applications currently
     *     registered.  The hub will terminate if this window is closed.</dd>
     * <dt>-help</dt>
     * <dd>Prints a help message and exits.</dd>
     * </dl>
     */
    public static void main( String[] args )
            throws RemoteException, IOException {
        String usage = "\nUsage:"
                     + "\n       "
                     + PlasticHub.class.getName()
                     + "\n           "
                     + " [-verbose]"
                     + " [-warn]"
                     + " [-gui]"
                     + "\n";
        PrintStream logOut = null;
        PrintStream warnOut = null;

        List argList = new ArrayList( Arrays.asList( args ) );
        boolean gui = false;
        for ( Iterator it = argList.iterator(); it.hasNext(); ) {
            String arg = (String) it.next();
            if ( arg.equals( "-verbose" ) ) {
                it.remove();
                logOut = System.out;
            }
            if ( arg.equals( "-warn" ) ) {
                it.remove();
                warnOut = System.out;
            }
            else if ( arg.equals( "-gui" ) ) {
                it.remove();
                gui = true;
            }
            else if ( arg.startsWith( "-h" ) ) {
                System.out.println( usage );
                return;
            }
        }
        if ( ! argList.isEmpty() ) {
            System.err.println( usage );
            System.exit( 1 );
        }
        PlasticHub hub =
            startHub( logOut, warnOut,
                      new File( System.getProperty( "user.home" ),
                      PlasticHubListener.PLASTIC_CONFIG_FILENAME ) );
        if ( gui ) {
            JFrame window = 
                new PlasticListWindow( hub.getApplicationListModel() );
            window.setTitle( "PlasticHub" );
            window.pack();
            window.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
            window.setVisible( true );
        }
    }
}
