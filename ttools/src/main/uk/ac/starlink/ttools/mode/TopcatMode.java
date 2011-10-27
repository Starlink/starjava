package uk.ac.starlink.ttools.mode;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Iterator;
import java.util.Random;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.astrogrid.samp.Message;
import org.astrogrid.samp.Metadata;
import org.astrogrid.samp.Response;
import org.astrogrid.samp.SampUtils;
import org.astrogrid.samp.client.DefaultClientProfile;
import org.astrogrid.samp.client.HubConnection;
import org.astrogrid.samp.httpd.HttpServer;
import org.astrogrid.samp.httpd.ResourceHandler;
import org.astrogrid.samp.hub.Hub;
import org.astrogrid.samp.hub.HubProfile;
import org.astrogrid.samp.hub.HubServiceMode;
import org.astrogrid.samp.xmlrpc.StandardHubProfile;
import org.votech.plastic.PlasticHubListener;
import uk.ac.starlink.plastic.PlasticUtils;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableWriter;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.ttools.DocUtils;
import uk.ac.starlink.ttools.Stilts;
import uk.ac.starlink.ttools.TableConsumer;
import uk.ac.starlink.ttools.task.LineTableEnvironment;
import uk.ac.starlink.votable.DataFormat;
import uk.ac.starlink.votable.VOTableWriter;
import uk.ac.starlink.votable.soap.VOTableSerialization;

/**
 * Processing mode for displaying the streamed table in TOPCAT.
 * If a TOPCAT server is currently running, a remote display message
 * will be sent.  Otherwise, a new TOPCAT server will be started and
 * a remote message will be sent to that.
 *
 * @author   Mark Taylor (Starlink)
 * @since    24 Mar 2005
 */
public class TopcatMode implements ProcessingMode {

    private static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.tttools.mode" );

    public Parameter[] getAssociatedParameters() {
        return new Parameter[ 0 ];
    }

    public String getDescription() {
        return DocUtils.join( new String[] {
            "<p>Attempts to display the output table directly in",
            "<webref url='http://www.starlink.ac.uk/topcat/'>TOPCAT</webref>.",
            "If a TOPCAT instance is already",
            "running on the local host, an attempt will be made to open",
            "the table in that.",
            "A variety of mechanisms are used to attempt communication",
            "with an existing TOPCAT instance.  In order:",
            "<ol>",
            "<li>SAMP using existing hub",
                 " (TOPCAT v3.4+ only, requires SAMP hub to be running)</li>",
            "<li>PLASTIC using existing hub",
                 " (requires PLASTIC hub to be running)</li>",
            "<li>SOAP",
                 " (requires TOPCAT to run with somewhat deprecated",
                 " <code>-soap</code> flag,",
                 " may be limitations on table size)</li>",
            "<li>SAMP using internal, short-lived hub",
                 " (TOPCAT v3.4+ only, running hub not required,",
                 " but may be slow.  It's better to start an external hub,",
                 " e.g. <code>topcat -exthub</code>)</li>",
            "</ol>",
            "Failing that, an attempt will be made to launch",
            "a new TOPCAT instance for display.",
            "This only works if the TOPCAT classes are on the class path.",
            "</p>",
            "<p>If large tables are involved, starting TOPCAT with the",
            "<code>-disk</code> flag is probably a good idea.",
            "</p>",
        } );
    }

    public TableConsumer createConsumer( Environment env ) {
        final StoragePolicy policy =
            LineTableEnvironment.getStoragePolicy( env );
        final PrintStream out = env.getOutputStream();
        return new TableConsumer() {
            public void consume( StarTable table ) throws IOException {
                toTopcat( table, policy, out );
            }
        };
    }

    /**
     * Attempts to load a table into TOPCAT.
     *
     * @param   table   table to display
     * @param   policy  storage policy
     * @param   out   output stream for logging messages
     */
    private void toTopcat( StarTable table, StoragePolicy policy,
                           PrintStream out ) throws IOException {
        String srcName = table.getName();
        if ( srcName == null || srcName.trim().length() == 0 ) {
            srcName = "(stilts)";
        }

        boolean done = false;
        if ( ! done ) {
            try {
                logger_.info( "Trying SAMP ..." );
                sampDisplay( table );
                logger_.info( "... sent via SAMP" );
                done = true;
            }
            catch ( IOException e ) {
                logger_.info( "... SAMP broadcast failed " + e );
            }
        }

        if ( ! done ) {
            try {
                logger_.info( "Trying PLASTIC ..." );
                plasticDisplay( table, policy );
                logger_.info( "... sent via PLASTIC" );
                done = true;
            }
            catch ( IOException e ) {
                logger_.info( "... PLASTIC broadcast failed " + e );
            }
        }

        if ( ! done ) {
            try {
                logger_.info( "Trying SAMP with short-lived internal hub ..." );
                sampHubDisplay( table );
                logger_.info( "... sent via SAMP with internal hub" );
                logger_.warning( "This would be more efficient "
                               + "with an external SAMP hub running." );
                done = true;
            }
            catch ( IOException e ) {
                logger_.info( "... SAMP with internal hub failed " + e );
            }
        }

        if ( ! done ) {
            try {
                logger_.info( "Trying local JVM using reflection ..." );
                table = Tables.randomTable( table );
                internalDisplay( table, srcName );
                logger_.info( "... loaded in local JVM" );
                done = true;
            }
            catch ( IOException e ) {
                logger_.info( "... TOPCAT startup failed " + e );
            }
        }

        if ( ! done ) {
            out.println( "Couldn't contact or start TOPCAT" );
        }
    }

    /**
     * Attempts to display a table in a TOPCAT which is registered with
     * a running PLASTIC hub.
     *
     * @param  table  table to display
     * @param  policy   storage policy
     */
    private void plasticDisplay( StarTable table, StoragePolicy policy )
            throws IOException {
        PlasticHubListener hub = PlasticUtils.getLocalHub();
        URI plasticId = hub.registerNoCallBack( "stilts" );
        PlasticMode.broadcast( table, PlasticMode.MSG_BYURL, hub, plasticId,
                               policy, "topcat", null );
    }

    /**
     * Attempts to display a table in a TOPCAT which is registered with
     * a running SAMP hub.
     *
     * @param  table  table to display
     */
    private void sampDisplay( StarTable table ) throws IOException {
        StarTableWriter vowriter =
            new VOTableWriter( DataFormat.BINARY, true );
        SampMode.TableTransmitter transmitter =
            new SampMode.TableTransmitter( new String[] { "votable" },
                                           new StarTableWriter[] { vowriter },
                                           table, "topcat", null );
        transmitter.run();
        transmitter.close();
    }

    /**
     * Attempts to display a table in a running TOPCAT by starting a 
     * short-lived internal SAMP hub, waiting for a TOPCAT to connect to it,
     * and sending the message, before shutting the hub down again.
     *
     * @param  table  table to display
     */
    private void sampHubDisplay( StarTable table ) throws IOException {

        /* Start a hub. */
        Hub hub = Hub.runHub( HubServiceMode.NO_GUI,
                              new HubProfile[] { new StandardHubProfile() },
                              new HubProfile[ 0 ] );

        /* Register with it. */
        HubConnection connection =
            DefaultClientProfile.getProfile().register();
        if ( connection == null ) {
            throw new IOException( "Failed to register"
                                 + " with specially started hub?" );
        }
        Metadata meta = SampMode.getStiltsMetadata();
        meta.setIconUrl( Stilts.class.getResource( "images/stilts_icon.gif" )
                                     .toString() );
        connection.declareMetadata( meta );
        ClientRegWatcher tcWatcher = new ClientRegWatcher( connection );
        connection.setCallable( tcWatcher );
        connection.declareSubscriptions( tcWatcher.getSubscriptions() );

        /* Wait for a few seconds to see if a TOPCAT connects automatically
         * to the hub.  Normally it checks for a new hub every couple of
         *  seconds. */
        String tcId = tcWatcher.waitForIdFromName( "topcat", 5000 );
        if ( tcId == null ) {
            throw new IOException( "No TOPCAT found" );
        }

        /* If we have a TOPCAT, prepare to serve the table. */
        HttpServer httpd = new HttpServer();
        httpd.setDaemon( true );
        ResourceHandler resHandler = new ResourceHandler( httpd, "table" );
        httpd.addHandler( resHandler );
        Message msg =
           SampMode.createSendMessage( table, "votable",
                                       new VOTableWriter( DataFormat.BINARY,
                                                          true ),
                                       resHandler );
        httpd.start();

        /* Send the table load message and wait for the response. */
        String msgTag = "table-send";
        connection.call( tcId, msgTag, msg );
        Response response = tcWatcher.waitForResponse( msgTag );

        /* Tidy up. */
        connection.unregister();
        hub.shutdown();
        httpd.stop();
    }

    /**
     * Starts up a new TOPCAT instance and displays the given table in it.
     *
     * @param  table  randomTable to display, must have random access
     * @param  srcName   label for the table's source
     */
    private void internalDisplay( StarTable randomTable, final String srcName )
            throws IOException {
        TopcatLoader loader;
        try {
            loader = new TopcatLoader( randomTable, srcName, false );
        }
        catch ( Throwable e ) {
            throw (IOException)
                  new IOException( "TOPCAT classes not available" )
                 .initCause( e );
        }
        try {
            synchronized ( loader ) {
                SwingUtilities.invokeLater( loader );
                while ( ! loader.finished_ ) {
                    loader.wait();
                }
            }
            if ( ! loader.success_.booleanValue() ) {
                Throwable error = loader.error_;
                if ( error instanceof IOException ) {
                    throw (IOException) error;
                }
                else {
                    throw (IOException)
                          new IOException( "TOPCAT start/load failed" )
                             .initCause( error );
                }
            }
        }
        catch ( InterruptedException e ) {
            logger_.info( "Interrupted waiting for table load" );
        }
    }

    /**
     * Class that uses reflection to start up TOPCAT and load a table into it.
     * The run() method does the load.
     */
    static class TopcatLoader implements Runnable {
        private static Class driverClazz_;
        private static Class controlClazz_;
        private static Method main_;
        private static Method getControlWindow_;
        private static Method addTable_;
        private static Method setStandalone_;

        private final Object[] addTableArgs_;
        private Throwable error_;
        private Boolean success_;
        private boolean finished_;
        
        /**
         * Constructs a new loader which can load one table.
         *
         * @param  randomTable  table, must have random access
         * @param  srcName    table source name
         * @param  display  whether to select the new table in TOPCAT
         */
        public TopcatLoader( StarTable randomTable, String srcName,
                             boolean display )
                throws ClassNotFoundException, NoSuchMethodException {
            reflect();
            addTableArgs_ = new Object[] { randomTable, srcName,
                                           Boolean.valueOf( display ) };
        }

        /**
         * Starts up TOPCAT, loads the table, updates state, and then
         * does a notify.
         */
        public synchronized void run() {
            try {
                setStandalone_.invoke( null, new Object[] { Boolean.TRUE } );
                main_.invoke( null,
                              new Object[] { new String[] { "-plastic" } } );
                Object controlWindow =
                    getControlWindow_.invoke( null, new Object[ 0 ] );
                addTable_.invoke( controlWindow, addTableArgs_ );
                success_ = Boolean.TRUE;
            }
            catch ( InvocationTargetException e ) {
                error_ = e.getCause();
                success_ = Boolean.FALSE; 
            }
            catch ( Throwable e ) {
                error_ = e;
                success_ = Boolean.FALSE;
            }
            finished_ = true;
            notifyAll();
        }

        /**
         * Performs all the required reflection.  This method is designed
         * to be called from test cases.
         */
        public static void reflect()
                throws ClassNotFoundException, NoSuchMethodException {
            driverClazz_ = Class.forName( "uk.ac.starlink.topcat.Driver" );
            controlClazz_ =
                Class.forName( "uk.ac.starlink.topcat.ControlWindow" );
            main_ = driverClazz_
                   .getMethod( "main", new Class[] { String[].class } );
            setStandalone_ = 
                driverClazz_.getMethod( "setStandalone",
                                        new Class[] { boolean.class } );
            addTable_ =
                controlClazz_.getMethod( "addTable",
                                         new Class[] { StarTable.class,
                                                       String.class,
                                                       boolean.class } );
            getControlWindow_ =
                controlClazz_.getMethod( "getInstance", new Class[ 0 ] );
        }
    }
}
