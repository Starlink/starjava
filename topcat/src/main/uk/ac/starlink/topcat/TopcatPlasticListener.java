package uk.ac.starlink.topcat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.votech.plastic.PlasticListener;
import org.votech.plastic.PlasticHubListener;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.util.DataSource;

/**
 * Implements the PlasticListener interface on behalf of the TOPCAT application.
 * Will attempt to unregister with the hub on finalization or JVM shutdown.
 *
 * @author   Mark Taylor
 * @since    8 Feb 2006
 * @see      <a href="http://plastic.sourceforge.net/">PLASTIC</a>
 */
public class TopcatPlasticListener implements PlasticListener {

    private final PlasticHubListener hub_;
    private final ControlWindow controlWindow_;
    private final URI plasticId_;
    private boolean unregistered_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat" );

    private static final String APPLICATION_NAME = "topcat";

    private static final URI VOT_LOAD;
    private static final URI VOT_LOADURL;
    private static final URI GET_NAME;
    private static final URI ECHO;
    private static final URI HUB_STOP;
    private static final URI[] SUPPORTED_MESSAGES = new URI[] {
        VOT_LOAD =    getURI( "ivo://votech.org/votable/load" ),
        VOT_LOADURL = getURI( "ivo://votech.org/votable/loadFromURL" ),
        GET_NAME =    getURI( "ivo://votech.org/info/getName" ),
        ECHO =        getURI( "ivo://votech.org/test/echo" ),
        HUB_STOP =    getURI( "ivo://votech.org/hub/event/HubStopping" ),
    };

    /**
     * Constructs a new listener which will react appropriately to 
     * messages from the hub.
     *
     * @param   hub  plastic hub
     * @param   controlWindow   control window into which accepted tables
     *          will be loaded etc
     */
    public TopcatPlasticListener( PlasticHubListener hub,
                                  ControlWindow controlWindow ) {
        hub_ = hub;
        controlWindow_ = controlWindow;
        plasticId_ = hub.registerRMI( APPLICATION_NAME,
                                      Arrays.asList( SUPPORTED_MESSAGES ),
                                      this );
        Runtime.getRuntime().addShutdownHook( new Thread() {
            public void run() {
                unregister();
            }
        } );
    }

    /**
     * Implements the PlasticListener interface.
     */
    public Object perform( URI sender, URI message, List args ) {
        logger_.info( "Received PLASTIC message " + message );
        try {
            return doPerform( sender, message, args );
        }
        catch ( IOException e ) {
            logger_.warning( "PLASTIC message failed: " + e );
            return e;
        }
    }

    /**
     * Does the work for processing a hub message.
     *
     * @param  sender   sender ID
     * @param  message  message ID (determines the action required)
     * @param  args     message argument list
     * @return  return value requested by message
     */
    public Object doPerform( URI sender, URI message, List args )
            throws IOException {

        /* Hub shutdown. */
        if ( HUB_STOP.equals( message ) ) {
            unregistered_ = true;
            return null;
        }

        /* Echo. */
        else if ( ECHO.equals( message ) &&
             checkArgs( args, new Class[] { String.class } ) ) {
            return args.get( 0 );
        }

        /* Return application name. */
        else if ( GET_NAME.equals( message ) &&
                  checkArgs( args, new Class[ 0 ] ) ) {
            return APPLICATION_NAME;
        }

        /* Load VOTable passed as text in an argument. */
        else if ( VOT_LOAD.equals( message ) &&
                  checkArgs( args, new Class[] { String.class } ) ) {
            votableLoad( sender, (String) args.get( 0 ) );
            return null;
        }

        /* Load VOTable by URL. */
        else if ( VOT_LOADURL.equals( message ) &&
                  checkArgs( args, new Class[] { Object.class } ) ) {
            String url = args.get( 0 ) instanceof String
                       ? (String) args.get( 0 )
                       : args.get( 0 ).toString();
            votableLoadFromURL( sender, url );
            return null;
        }

        /* Unknown message. */
        else {

            /* If we haven't catered for one of the messages in our list,
             * that's a programming error in this class.
            assert ! Arrays.asList( SUPPORTED_MESSAGES ).contains( message );

            /* If we've been sent a message that we never claimed to support,
             * that's the hub's fault. */
            throw new IllegalArgumentException( "Unsupported message " 
                                              + message );
        }
    }

    /**
     * Utility method to check the types of a list of arguments.
     * If they look OK, true is returned, otherwise an IOException is thrown.
     * The check is passed if there are the same number of actual arguments
     * as required types, and each argument is either null or of the
     * class in the correponding position in <code>types</code>, or a 
     * subclass of that.
     *
     * @param  args  actual argument list
     * @param  types  classes you want to see represented in <code>list</code>
     */
    private boolean checkArgs( List args, Class[] types ) throws IOException {
        if ( types.length != args.size() ) {
            throw new IOException( "Wrong number of arguments in "
                                 + "PLASTIC message" );
        }
        for ( int i = 0; i < types.length; i++ ) {
            Object arg = args.get( i );
            if ( arg != null && 
                 ! types[ i ].isAssignableFrom( arg.getClass() ) ) {
                throw new IOException( "Wrong type of arguments in "
                                     + "PLASTIC message" );
            }
        }
        return true;
    }

    /**
     * Does the work for the load-from-string VOTable message.
     *
     * @param  sender  sender ID
     * @param  votText   VOTable text contained in a string, assumed UTF-8
     *                   encoded
     */
    private void votableLoad( URI sender, String votText ) throws IOException {
        final byte[] votBytes;
        try {
            votBytes = votText.getBytes( "UTF-8" );
        }
        catch ( UnsupportedEncodingException e ) {
            throw (AssertionError)
                  new AssertionError( "JVMs are required to support UTF-8" )
                 .initCause( e );
        }
        votText = null;
        DataSource datsrc = new DataSource() {
            public InputStream getRawInputStream() {
                return new ByteArrayInputStream( votBytes );
            }
        };
        loadTable( controlWindow_.getTableFactory()
                  .makeStarTable( datsrc, "votable" ) );
    }

    /**
     * Does the work for the load-from-URL VOTable load message.
     *
     * @param   sender  sender ID
     * @param   url  location of table
     */
    private void votableLoadFromURL( URI sender, String url )
            throws IOException {
        loadTable( controlWindow_.getTableFactory()
                  .makeStarTable( url, "votable" ) );
    }

    /**
     * Loads a StarTable into TOPCAT.
     *
     * @param  table  table to load
     */
    private void loadTable( final StarTable table ) {

        /* Best do it asynchronously since this may not be called from the
         * event dispatch thread. */
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                controlWindow_.addTable( table, "plastic", true );
            }
        } );
    }

    /**
     * Makes an attempt to unregister from the hub if it's not already been
     * done.  If this method is called when the hub connection is already
     * inactive, nothing happens.  This method won't block indefinitely,
     * even if the hub doesn't want to answer for some reason. 
     * This means that the unregister is not guaranteed to complete
     * successfully - too bad, failing to unregister is not a serious crime.
     */
    public void unregister() {

        /* Check safely whether we've already attempted an unregister. */
        boolean doUnregister;
        synchronized ( this ) {
            if ( ! unregistered_ ) {
                doUnregister = true;
                unregistered_ = true;
            }
            else {
                doUnregister = false;
            }
        }

        /* Attempt the unregister in a separate daemon thread, and wait 
         * up to 1 second for it to complete before returning.  This means 
         * that if this method is being called as part of a JVM shutdown
         * sequence, it's guaranteed not to delay shutdown indefinitely,
         * since daemon threads are summarily terminated during JVM shutdown.
         * However, unless that happens, the unregister thread can take
         * as long as it likes.
         * Am I being paranoid?  I don't know how likely it is that 
         * the unregister() call might take long/for ever to return. */
        if ( doUnregister ) {
            Thread unregThread = new Thread( "PLASTIC unregister" ) {
                public void run() {
                    hub_.unregister( plasticId_ );
                    logger_.info( "PLASTIC unregistration successful" );
                }
            };
            unregThread.setDaemon( true );
            unregThread.start();
            try {
                unregThread.join( 1000 );
            }
            catch ( InterruptedException e ) {
            }
        }
    }

    /**
     * Finalizer makes an attempt to unregister from the hub, but won't
     * wait around indefinitely for this to happen.
     */
    public void finalize() throws Throwable {
        try {
            new Thread( "PLASTIC unregister" ) {
                public void run() {
                    unregister();
                }
            }.start();
        }
        finally {
            super.finalize();
        }
    }

    /**
     * Convenience method to turn a String into a URI without throwing
     * any pesky checked exceptions.
     *
     * @param  uri  URI text
     * @return  URI
     * @throws  IllegalArgumentException   if uri doesn't look like a URI
     */
    private static URI getURI( String uri ) {
        try {
            return new URI( uri );
        }
        catch ( URISyntaxException e ) {
            throw (IllegalArgumentException)
                  new IllegalArgumentException( "Bad URI: " + uri )
                 .initCause( e );
        }
    }
}
