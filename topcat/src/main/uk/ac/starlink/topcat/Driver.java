package uk.ac.starlink.topcat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.astrogrid.samp.ErrInfo;
import org.astrogrid.samp.JSamp;
import org.astrogrid.samp.Response;
import org.astrogrid.samp.client.DefaultClientProfile;
import org.astrogrid.samp.client.SampException;
import org.astrogrid.samp.httpd.UtilServer;
import uk.ac.starlink.auth.AuthManager;
import uk.ac.starlink.plastic.PlasticHub;
import uk.ac.starlink.plastic.PlasticUtils;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.TableSequence;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.gui.TableLoadClient;
import uk.ac.starlink.table.gui.TableLoader;
import uk.ac.starlink.table.jdbc.TextModelsAuthenticator;
import uk.ac.starlink.task.InvokeUtils;
import uk.ac.starlink.topcat.interop.TopcatCommunicator;
import uk.ac.starlink.ttools.Stilts;
import uk.ac.starlink.util.gui.ErrorDialog;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.Loader;
import uk.ac.starlink.util.LogUtils;
import uk.ac.starlink.util.URLDataSource;
import uk.ac.starlink.util.URLUtils;
import uk.ac.starlink.votable.VOElementFactory;

/**
 * Main class for invoking the TOPCAT application from scratch.
 * Contains some useful static configuration-type methods as well
 * as the {@link #main} method itself.
 *
 * @author   Mark Taylor (Starlink)
 * @since    9 Mar 2004
 */
public class Driver {

    private static boolean standalone = false;
    private static StarTable[] demoTables;
    private static Logger logger = Logger.getLogger( "uk.ac.starlink.topcat" );
    private static StarTableFactory tabfact;
    private static ControlWindow control;
    private static final ValueInfo DEMOLOC_INFO = 
        new DefaultValueInfo( "DemoLoc", String.class, "Demo file location" );
    private static final int DEFAULT_SERVER_PORT = 2525;

    /**
     * Determines whether TableViewers associated with this class should
     * act as a standalone application.  If <code>standalone</code> is set
     * true, then it will be possible to exit the JVM using menu items
     * etc in the viewer.  Otherwise, no normal activity within the
     * TableViewer GUI will cause a JVM exit.
     *
     * @param  standalone  whether this class should act as a standalone
     *         application
     */
    public static void setStandalone( boolean standalone ) {
        Driver.standalone = standalone;
    }

    /**
     * Indicates whether the TableViewer application is standalone or not.
     *
     * @return  whether this should act as a standalone application.
     */
    public static boolean isStandalone() {
        return standalone;
    }

    /**
     * Main method for TOPCAT invocation.
     * Under normal circumstances this will pop up a ControlWindow and
     * populate it with tables named in the arguments.
     *
     * @param  args  list of flags and table specifications
     */
    public static void main( String[] args ) throws SampException, IOException {
        try {
            Loader.checkJ2se();
        }
        catch ( ClassNotFoundException e ) {
            for ( int i = 0; i < args.length; i++ ) {
                if ( args[ i ].toLowerCase().startsWith( "-debug" ) ) {
                    e.printStackTrace( System.err );
                }
            }
            System.err.println( e.getMessage() );
            return;
        }
        runMain( args );
    }

    /**
     * Does the work for the <code>main</code> method, but may throw
     * throwables.
     *
     * @param  args  list of table specifications
     */
    private static void runMain( String[] args )
            throws SampException, IOException {
        String cmdname;
        try {
            Loader.loadProperties();
            cmdname = System.getProperty( "uk.ac.starlink.topcat.cmdname" );
        }
        catch ( SecurityException e ) {
            // never mind
            cmdname = null;
        }
        Loader.tweakGuiForMac();
        Loader.setHttpAgent( TopcatUtils.getHttpUserAgent() );
        Loader.setDefaultProperty( "java.awt.Window.locationByPlatform",
                                   "true" );

        /* Fine tune the logging - we don't need HDS or AST here, so 
         * stop them complaining when they can't be loaded. */
        try {
            LogUtils.getLogger( "uk.ac.starlink.hds" ).setLevel( Level.OFF );
            LogUtils.getLogger( "uk.ac.starlink.ast" ).setLevel( Level.OFF );
        }
        catch ( SecurityException e ) {
            // If running in a sandbox, this may be blocked - never mind.
        }

        /* Prepare basic usage message. */
        if ( cmdname == null ) {
            cmdname = "topcat";
        }
        String pre = "Usage: " + cmdname;
        String pad = "\n" + pre.replaceAll( ".", " " );
        String usage = 
              pre + " [-help] [-version]"
            + pad + " [-stilts <stilts-args>|-jsamp <jsamp-args>]"
            + pad + " [-verbose] [-debug] [-demo] [-running] [-memory|-disk]"
            + pad + " [-[no]hub|-exthub|-noserv] [-samp|-plastic]"
            + pad + " [[-f <format>] table ...]";

        /* Create default table factory. */
        tabfact = TopcatPreparation.createFactory();

        /* Standalone execution (e.g. System.exit() may be called). */
        setStandalone( true );

        /* Process flags. */
        List<String> argList = new ArrayList<String>( Arrays.asList( args ) );
        boolean demo = false;
        int verbosity = 0;
        boolean debug = false;
        boolean interopServe = true;
        boolean internalHub = false;
        boolean externalHub = false;
        boolean noHub = false;
        boolean checkVersion = true;
        boolean useRunning = false;
        for ( Iterator<String> it = argList.iterator(); it.hasNext(); ) {
            String arg = it.next();
            if ( arg.equals( "-h" ) || arg.equals( "-help" ) ) {
                System.out.println( getHelp( cmdname ) );
                return;
            }
            else if ( arg.equals( "-version" ) ) {
                it.remove();
                logger.setLevel( Level.WARNING );
                String[] about = TopcatUtils.getAbout();
                System.out.println();
                for ( int i = 0; i < about.length; i++ ) {
                    System.out.println( "    " + about[ i ] );
                }
                System.out.println();
                return;
            }
            else if ( arg.equals( "-stilts" ) ) {
                it.remove();
                Stilts.main( argList.toArray( new String[ 0 ] ) );
                return;
            }
            else if ( arg.equals( "-jsamp" ) ) {
                it.remove();
                JSamp.main( argList.toArray( new String[ 0 ] ) );
                return;
            }
            else if ( arg.equals( "-v" ) || arg.equals( "-verbose" ) ) {
                it.remove();
                verbosity++;
            }
            else if ( arg.equals( "-debug" ) ) {
                it.remove();
                debug = true;
            }
            else if ( arg.equals( "-demo" ) ) {
                it.remove();
                demo = true;
            }
            else if ( arg.equals( "-memory" ) ) {
                it.remove();
                tabfact.setStoragePolicy( StoragePolicy.PREFER_MEMORY );
            }
            else if ( arg.equals( "-running" ) ) {
                it.remove();
                useRunning = true;
            }
            else if ( arg.equals( "-norunning" ) ) {
                it.remove();
                useRunning = false;
            }
            else if ( arg.equals( "-disk" ) ) {
                it.remove();
                tabfact.setStoragePolicy( StoragePolicy.PREFER_DISK );
            }
            else if ( arg.equals( "-hub" ) ) {
                it.remove();
                internalHub = true;
            }
            else if ( arg.equals( "-nohub" ) ) {
                it.remove();
                noHub = true;
            }
            else if ( arg.equals( "-exthub" ) ) {
                it.remove();
                externalHub = true;
            }
            else if ( arg.equals( "-samp" ) ) {
                it.remove();
                interopServe = true;
                ControlWindow.interopType_ = "samp";
            }
            else if ( arg.equals( "-plastic" ) ) {
                it.remove();
                interopServe = true;
                ControlWindow.interopType_ = "plastic";
            }
            else if ( arg.equals( "-noplastic" ) ) { // deprecated
                it.remove();
                interopServe = false;
            }
            else if ( arg.startsWith( "-noserv" ) ) {
                it.remove();
                interopServe = false;
                ControlWindow.interopType_ = "none";
            }
            else if ( arg.startsWith( "-checkvers" ) ) {
                it.remove();
                checkVersion = true;
            }
            else if ( arg.startsWith( "-nocheckvers" ) ) {
                it.remove();
                checkVersion = false;
            }
            else if ( arg.equals( "-f" ) || arg.equals( "-format" ) ) {
                // leave this for this later
            }
            else if ( arg.startsWith( "-" ) && arg.length() > 1 ) {
                System.err.println( usage );
                System.exit( 1 );
            }
        }

        /* Configure logging. */
        InvokeUtils.configureLogging( verbosity, debug );
        Logger rootLogger = LogUtils.getLogger( "" );
        rootLogger.setLevel( Level.CONFIG );
        rootLogger.addHandler( LogHandler.getInstance() );

        /* Check JRE vendor and report on concerns. */
        Loader.checkJ2seVendor();

        /* Configure default port number for SAMP-related services. */
        try {
            String portnum = System.getProperty( UtilServer.PORT_PROP );
            if ( portnum == null || portnum.trim().length() == 0 ) {
                System.setProperty( UtilServer.PORT_PROP,
                                    Integer.toString( DEFAULT_SERVER_PORT ) );
            }
        }
        catch ( SecurityException e ) {
            // Never mind.
        }

        /* Configure factory. */
        tabfact.getJDBCHandler()
               .setAuthenticator( new TextModelsAuthenticator() );

        /* Install custom URL handlers. */
        URLUtils.installCustomHandlers();

        /* Configure authentication to use a GUI. */
        AuthManager.getInstance().setUserInterface( new TopcatAuthUi() );

        /* Assemble pairs of (table name, handler name) to be loaded. */
        final List<DataSourceLoader> loaderList =
            new ArrayList<DataSourceLoader>();
        String handler = null;
        for ( Iterator<String> it = argList.iterator(); it.hasNext(); ) {
            String arg = it.next();
            if ( arg.equals( "-f" ) || arg.equals( "-format" ) ) {
                if ( it.hasNext() ) {
                    handler = it.next();
                    if ( handler.equals( "auto" ) ) {
                        handler = null;
                    }
                }
                else {
                    System.err.println( usage );
                    System.exit( 1 );
                }
            }
            else if ( arg.startsWith( "-" ) && arg.length() > 1 ) {
                System.err.println( usage );
                System.exit( 1 );
            }
            else {
                loaderList.add( new DataSourceLoader( arg, handler ) );
            }
        }

        /* Attempt to contact existing TOPCAT if requested. */
        if ( useRunning ) {
            TopcatSender sender =
                TopcatSender.createSender( DefaultClientProfile.getProfile() );
            if ( sender != null ) {
                if ( loaderList.isEmpty() ) {
                    logger.warning( "Running TOPCAT exists; "
                                  + "no tables to send to it" );
                }
                try {
                    for ( DataSourceLoader loader : loaderList ) {
                        logger.info( "Sending table to running TOPCAT: "
                                   + loader.loc_ );
                        Response response =
                            sender.sendTable( loader.loc_, loader.format_ );
                        String status = response.getStatus();
                        ErrInfo errInfo = response.getErrInfo();
                        String errmsg = errInfo == null ? null
                                                        : errInfo.getUsertxt();
                        if ( Response.ERROR_STATUS.equals( status ) ) {
                            throw new IOException( "Table send failed: "
                                                 + errmsg );
                        }
                        else if ( ! response.isOK() ) {
                            logger.warning( "Table send warning: " + errmsg );
                        }
                    } 
                }
                finally {
                    sender.close();
                }
                return;
            }
        }

        /* Start up the GUI now. */
        final ControlWindow control = getControlWindow();
        TopcatUtils.setAboutHandler( () -> TopcatUtils.showAbout( control ) );

        /* Start up with demo data if requested. */
        if ( demo ) {
            StarTable[] demoTables = getDemoTables();
            for ( int i = 0; i < demoTables.length; i++ ) {
                final StarTable table = demoTables[ i ];
                if ( table != null ) {
                    final String loc =
                        table.getParameterByName( DEMOLOC_INFO.getName() )
                             .getValue().toString();
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            control.addTable( table, "[Demo]:" + loc, false );
                        }
                    } );
                }
            }
        }

        /* Load the requested tables. */
        boolean argsLoadSequential = true;
        if ( ! loaderList.isEmpty() ) {
            if ( argsLoadSequential ) {
                new Thread( "Command-line Table Loader" ) {
                    public void run() {
                        loadTableList( loaderList, control, true );
                    }
                }.start();
            }
            else {
                loadTableList( loaderList, control, false );
            }
        }

        /* Anything after this point does not delay GUI startup. */
        final StartupConfig config = new StartupConfig();
        config.interopServe_ = interopServe;
        config.internalHub_ = internalHub;
        config.externalHub_ = externalHub;
        config.noHub_ = noHub;
        config.checkVersion_ = checkVersion;
        Thread extraThread = new Thread( "Non-critical Startup" ) {
            public void run() {
                extraStartupFunctions( config );
            }
        };
        extraThread.setDaemon( true );
        extraThread.start();
    }
       
    /**
     * Performs startup functions which do not need to complete before
     * the main application is ready for use.
     *
     * @param  config  configuration options
     */
    private static void extraStartupFunctions( StartupConfig config ) {

        /* Start up remote services. */
        if ( config.interopServe_ ) {
            TopcatCommunicator communicator =
                getControlWindow().getCommunicator();

            /* Start SAMP/PLASTIC hub if requested. */
            try {
                if ( config.internalHub_ || config.externalHub_ ) {
                    boolean isExternal = config.externalHub_;
                    communicator.startHub( isExternal );
                }
                else if ( ! config.noHub_ ) {
                    communicator.maybeStartHub();
                }
            }
            catch ( IOException e ) {
                logger.log( Level.WARNING,
                            "Can't start " + communicator.getProtocolName()
                          + " hub", e );
            }

            /* Start SAMP/PLASTIC client. */
            boolean isReg = communicator.setActive();
            if ( isReg ) {
                logger.info( "Registered as " + communicator.getProtocolName()
                           + " client" );
            }
            else {
                logger.info( communicator.getProtocolName()
                           + " registration atttempt failed" );
            }
        }

        /* Investigate whether we have a recent version. */
        if ( config.checkVersion_ ) {
            TopcatUtils.enquireLatestVersion();
        }
    }

    /**
     * Returns the ControlWindow used by this application.  It is
     * constructed lazily, which means if it's never needed (say if 
     * we're just printing a usage message), the GUI
     * never has to start up.
     *
     * @return  control window
     */
    private static ControlWindow getControlWindow() {
        if ( control == null ) {
            control = ControlWindow.getInstance();
            control.setTableFactory( tabfact );
        }
        return control;
    }

    /**
     * Returns a set of example StarTables suitable for demonstration
     * purposes.  They will all have random access.
     * If one of the demo tables can't be created for some
     * reason (e.g. the required resource is missing) the corresponding
     * element in the returned array will be <code>null</code>.
     *
     * @return  array of demo tables
     */
    static StarTable[] getDemoTables() {
        String base = TopcatUtils.DEMO_LOCATION + '/';
        String[] demoNames = new String[] {
            "6dfgs_mini.xml.bz2",
            // "863sub.fits",
            // "vizier.xml.gz#6",
            "messier.xml",
            // "tables.fit.gz#2",
        };
        int ntab = demoNames.length;
        if ( demoTables == null ) {
            demoTables = new StarTable[ ntab ];
            StarTableFactory demoFactory = TopcatPreparation.createFactory();
            for ( int i = 0; i < ntab; i++ ) {
                final String demoName = demoNames[ i ];
                try {
                    int fragIndex = demoName.indexOf( '#' );
                    String name; 
                    String frag;
                    if ( fragIndex > 0 ) {
                        name = demoName.substring( 0, fragIndex );
                        frag = demoName.substring( fragIndex + 1 );
                    }
                    else {
                        name = demoName;
                        frag = null;
                    }
                    URL url = Driver.class.getClassLoader()
                                    .getResource( base + name );
                    if ( url != null ) {
                        DataSource datsrc = 
                            DataSource.makeDataSource( url.toString() );
                        if ( frag != null ) {
                            datsrc.setPosition( frag );
                        }
                        StarTable table = demoFactory.makeStarTable( datsrc );
                        table.getParameters()
                             .add( new DescribedValue( DEMOLOC_INFO,
                                                       demoName ) );
                        demoTables[ i ] = demoFactory.randomTable( table );
                    }
                    else {
                        logger.warning( "Demo table resource not located: " +
                                        base + demoName );
                    }
                }
                catch ( IOException e ) {
                    logger.warning( "Demo table " + demoName + " not loaded: "
                                  + e.toString() );
                }
            }
        }
        return demoTables;
    }

    /**
     * Returns a full help message.
     *
     * @param  cmdname  command name for this application
     * @return help
     */
    private static String getHelp( String cmdname ) {
        StringBuffer buf = new StringBuffer();
        String p1 = "\n\n    ";
        String p2 = "\n        ";

        /* Basic usage. */
        buf.append( "Usage: " )
           .append( cmdname )
           .append( " <flags> [[-f <format>] <table> ...]" );

        /* General flags. */
        buf.append( p1 + "General flags:" )
           .append( p2 + "-help          print this message and exit" )
           .append( p2 + "-version       print component versions etc "
                                         + "and exit" )
           .append( p2 + "-verbose       increase verbosity of "
                                         + "reports to console" )
           .append( p2 + "-debug         add debugging information to "
                                         + "console log messages" )
           .append( p2 + "-demo          start with demo data" )
           .append( p2 + "-running       use existing TOPCAT instance "
                                         + "if one is running" )
           .append( p2 + "-memory        use memory storage for tables" )
           .append( p2 + "-disk          use disk backing store for "
                                         + "large tables" ) 
           .append( p2 + "-samp          use SAMP for tool interoperability" )
           .append( p2 + "-plastic       use PLASTIC for "
                                         + "tool interoperability" )
           .append( p2 + "-[no]hub       [don't] run internal "
                                         + "SAMP/PLASTIC hub" )
           .append( p2 + "-exthub        run external SAMP/PLASTIC hub" )
           .append( p2 + "-noserv        don't run any services"
                                         + " (PLASTIC or SAMP)" )
           .append( p2 + "-nocheckvers   don't check latest version" )
           .append( p2 + "-stilts <args> run STILTS not TOPCAT" )
           .append( p2 + "-jsamp <args>  run JSAMP not TOPCAT" );

        /* Java flags. */
        buf.append( p1 + "Useful Java flags:" )
           .append( p2 )
           .append( "-classpath jar1:jar2..  specify additional classes" )
           .append( p2 )
           .append( "-XmxnnnM                use nnn megabytes of memory" )
           .append( p2 )
           .append( "-Dname=value            set system property" );

        /* Auto-detected formats. */
        buf.append( p1 + "Auto-detected formats: " )
           .append( p2 );
        for ( Iterator<TableBuilder> it =
                  tabfact.getDefaultBuilders().iterator();
              it.hasNext(); ) {
            buf.append( it.next().getFormatName().toLowerCase() );
            if ( it.hasNext() ) {
                buf.append( ", " );
            }
        }

        /* All known formats. */
        buf.append( p1 + "All known formats:" )
           .append( p2 );
        for ( Iterator<String> it = tabfact.getKnownFormats().iterator();
              it.hasNext(); ) {
            buf.append( it.next().toLowerCase() );
            if ( it.hasNext() ) {
                buf.append( ", " );
            }
        }

        /* System properties. */
        buf.append( p1 + "Useful system properties " 
                       + "(-Dname=value - lists are colon-separated):" )
           .append( p2 )
           .append( "java.io.tmpdir          temporary filespace directory" )
           .append( p2 )
           .append( "jdbc.drivers            JDBC driver classes" )
           .append( p2 )
           .append( "jel.classes             " +
                    "custom algebraic function classes" )
           .append( p2 )
           .append( "jel.classes.activation  custom action function classes" )
           .append( p2 )
           .append( "star.connectors         custom remote filestore classes" )
           .append( p2 )
           .append( "startable.load.dialogs  custom load dialogue classes" )
           .append( p2 )
           .append( "startable.readers       custom table input handlers" )
           .append( p2 )
           .append( "startable.writers       custom table output handlers" )
           .append( p2 )
           .append( "startable.storage       storage policy" )
           .append( p2 )
           .append( "mark.workaround         work around mark/reset bug" )
           .append( p2 )
           .append( "    (see topcat -jsamp -help for more)" )
           .append( "" );

        /* Return. */
        return "\n" + buf.toString() + "\n";
    }

    /**
     * Does loading of tables from the command line.
     *
     * @param   loaderList  list of table loaders providing tables
     * @param   controlWin  control window
     * @param   sequential  true for sequential loading (synchronous operation),
     *                      false for parallel (aysnchronous)
     */
    private static void loadTableList( List<? extends TableLoader> loaderList,
                                       ControlWindow controlWin,
                                       final boolean sequential ) {
        for ( TableLoader loader : loaderList ) {
            final boolean[] doneHolder = new boolean[ 1 ];
            final TableLoadClient loadClient;
            if ( sequential ) {
                loadClient = new TopcatLoadClient( null, controlWin ) {
                    public void endSequence( boolean cancelled ) {
                        synchronized ( doneHolder ) {
                            doneHolder[ 0 ] = true;
                            doneHolder.notifyAll();
                        }
                        super.endSequence( cancelled );
                    }
                };
            }
            else {
                loadClient = new TopcatLoadClient( null, controlWin );
            }
            controlWin.runLoading( loader, loadClient, null );
            if ( sequential ) {
                try {
                    synchronized ( doneHolder ) {
                        while ( ! doneHolder[ 0 ] ) {
                            doneHolder.wait();
                        }
                    }
                }
                catch ( InterruptedException e ) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Defines configuration options for non-critical startup functions.
     */
    private static class StartupConfig {
        boolean interopServe_;
        boolean internalHub_;
        boolean externalHub_;
        boolean noHub_;
        boolean checkVersion_;
    }

    /**
     * Describes a table to be loaded on the command line.
     */
    private static class DataSourceLoader implements TableLoader {
        private final String loc_;
        private final String format_;

        /**
         * Constructor.
         *
         * @param    loc   location
         * @param    format  format name
         */
        DataSourceLoader( String loc, String format ) {
            loc_ = loc;
            format_ = format;
        }

        public String getLabel() {
            return loc_;
        }

        public TableSequence loadTables( StarTableFactory tfact )
                throws IOException {
            final TableSequence tseq = tfact.makeStarTables( loc_, format_ );
            return new TableSequence() {
                int ix_;
                public StarTable nextTable() throws IOException {
                    StarTable table = tseq.nextTable();
                    if ( table != null ) {
                        String label = loc_;
                        if ( ix_++ > 0 ) {
                            label += "-" + ix_;
                        }
                        table.setParameter(
                            new DescribedValue( TableLoader.SOURCE_INFO,
                                                label ) );
                    }
                    return table;
                }
            };
        }
    }
}
