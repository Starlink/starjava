package uk.ac.starlink.topcat.soap;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import uk.ac.starlink.soap.AppHttpSOAPServer;
import uk.ac.starlink.soap.util.RemoteUtilities;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.topcat.ControlWindow;

/**
 * Implements the SOAP services offered by the TOPCAT application.
 * In most cases, you should call {@link #initServices} at the start of
 * the application and {@link #getServer} subsequently.
 *
 * @author   Mark Taylor (Starlink)
 * @since    23 Mar 2005
 */
public class TopcatSOAPServer {

    /** Application name. */
    public static final String APP_NAME = "topcat";

    /**
     * Default port number (if it's occupied, the actual port will be 
     * some free port above this one.
     */
    public static final int DEFAULT_PORT = 8085;

    private static TopcatSOAPServer instance_;
    private static final Logger logger_ = 
        Logger.getLogger( "uk.ac.starlink.topcat.soap" );

    private final ControlWindow control_;
    private final AppHttpSOAPServer server_;
    private final int port_;
    private final String cookie_;

    /**
     * Constructs a new server relating to a given control window.
     *
     * @param   control  TOPCAT window to which tables will be added
     */
    public TopcatSOAPServer( ControlWindow control ) throws Exception {
        control_ = control;

        /* Deploy the server. */
        URL deployURL = TopcatSOAPServer.class.getResource( "topcat.wsdd" );
        server_ = new AppHttpSOAPServer( DEFAULT_PORT );
        server_.start();
        server_.addSOAPService( deployURL );

        /* Record the port and cookie for this server. */
        port_ = server_.getPort();
        logger_.info( "SOAP services offered on port " + port_ );
        cookie_ = RemoteUtilities.writeContactFile( port_, APP_NAME );
    }

    /**
     * Adds a table to the control window given its location.
     *
     * @param   cookie  cookie for this server
     * @param   location  table location (filename or URL)
     * @param   handler  name of table handler (or null for auto-detect)
     */
    public void displayTableByLocation( String cookie, String location, 
                                        String handler ) throws IOException {
        checkCookie( cookie );
        StarTableFactory factory = control_.getTableFactory();
        assert factory.requireRandom();
        StarTable table = factory.makeStarTable( location, handler );
        addRandomTable( table, location );
    }

    /**
     * Adds a table directly to the control window.
     *
     * @param   cookie  cookie for this server
     * @param   table   table to display
     * @param   location  string indicating the table's source
     */
    public void displayTable( String cookie, StarTable table, String location )
            throws IOException {
        checkCookie( cookie );
        table = control_.getTableFactory().randomTable( table );
        addRandomTable( table, location );
    }

    /**
     * Causes a table to be displayed in this server's control window.
     *
     * @param   table  table with random access
     * @param   location  string indicating table's provenance
     */
    private void addRandomTable( final StarTable table, 
                                 final String location ) {
        assert table.isRandom();
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                control_.addTable( table, location, true );
            }
        } );
    }

    /**
     * Checks that a cookie matches that of this server.
     *
     * @param  cookie   test cookie
     * @throws  IOException   if <tt>cookie</tt> is wrong
     */
    private void checkCookie( String cookie ) throws IOException {
        if ( cookie == null || ! cookie.equals( cookie_ ) ) {
            throw new IOException( "Attempt to call SOAP service " +
                                   "with bad cookie" );
        }
    }

    /**
     * Initializes a server.
     *
     * @param   controlWindow   the TOPCAT main window associated with
     *          the services to be provided
     */
    public static void initServices( ControlWindow controlWindow ) {
        try {
            instance_ = new TopcatSOAPServer( controlWindow );
        }
        catch ( Exception e ) {
            logger_.warning( "SOAP service unavailable: " + e );
        }
    }

    /**
     * Returns the default instance of this class.
     * This will return <tt>null</tt> unless {@link initServices} has
     * been successfully called.
     *
     * @return  server instance if the server is working
     */
    public static TopcatSOAPServer getInstance() {
        return instance_;
    }
}
