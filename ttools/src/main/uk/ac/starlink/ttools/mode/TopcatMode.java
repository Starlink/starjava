package uk.ac.starlink.ttools.mode;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.net.URI;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.xml.rpc.ParameterMode;
import javax.xml.rpc.ServiceException;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.encoding.XMLType;
import org.votech.plastic.PlasticHubListener;
import uk.ac.starlink.plastic.PlasticUtils;
import uk.ac.starlink.soap.util.RemoteUtilities;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableWriter;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.ttools.DocUtils;
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
        return DocUtils.join( new String[] {
            "<p>Displays the output table directly in",
            "<webref url='http://www.starlink.ac.uk/topcat/'>TOPCAT</webref>.",
            "If a TOPCAT instance (version 1.6 or later) is already",
            "running on the local host, the table will be opened in that,",
            "otherwise a new TOPCAT instance will be launched for display.",
            "The latter mode only works if the TOPCAT classes are",
            "on the class path.",
            "</p>",
            "<p>A variety of mechanisms (e.g. PLASTIC and SOAP) are attempted",
            "to transfer the table, depending on what running instances",
            "of TOPCAT can be found.",
            "Depending on the transport mechanism used, there may be limits",
            "to the size of table which can be transmitted to the application",
            "in this way.",
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
                logger_.info( "Trying SOAP ..." );
                soapDisplay( table, srcName );
                logger_.info( "... sent via SOAP" );
                done = true;
            }
            catch ( ConnectException e ) {
                logger_.info( "... SOAP connection failed " + e );
            }
            catch ( ServiceException e ) {
                throw (IOException) new IOException( e.getMessage() )
                                   .initCause( e );
            }
            catch ( Throwable e ) {
                logger_.info( "... SOAP connection failed " + e );
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
     * Attempts to display a table in a running TOPCAT SOAP server.
     *
     * @param  table  table to display
     * @param  srcName   label for the table's source
     */
    private void soapDisplay( StarTable table, String srcName )
            throws ConnectException, ServiceException, IOException {

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
     * Throws an exception if there aren't enough classes on the classpath
     * to be able to attempt processing in this mode.
     */
    private static void checkRequisites() {
        RemoteUtilities.class.getName();
        org.apache.axis.encoding.Target.class.getName();
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
