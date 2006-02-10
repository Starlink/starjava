package uk.ac.starlink.topcat;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.SwingUtilities;
import org.votech.plastic.PlasticListener;
import org.votech.plastic.PlasticHubListener;
import uk.ac.starlink.astrogrid.Plastic;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.gui.ErrorDialog;

/**
 * Implements the PlasticListener interface on behalf of the TOPCAT application.
 * Will attempt to unregister with the hub on finalization or JVM shutdown.
 *
 * @author   Mark Taylor
 * @since    8 Feb 2006
 * @see      <a href="http://plastic.sourceforge.net/">PLASTIC</a>
 */
public class TopcatPlasticListener implements PlasticListener {

    private final ControlWindow controlWindow_;
    private PlasticHubListener hub_;
    private URI plasticId_;
    private final Action registerAct_;
    private final Action unregisterAct_;

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
     * @param   controlWindow   control window into which accepted tables
     *          will be loaded etc
     */
    public TopcatPlasticListener( ControlWindow controlWindow ) {
        controlWindow_ = controlWindow;
        registerAct_ = new RegisterAction( true );
        unregisterAct_ = new RegisterAction( false );
        unregisterAct_.setEnabled( false );
        Runtime.getRuntime().addShutdownHook( new Thread() {
            public void run() {
                unregister();
            }
        } );
    }

    /**
     * Returns the ID with which this listener is registered with the
     * prevailing PLASTIC hub.  If this listener is not currently registered,
     * <code>null</code> will be returned.  If you want to do your best
     * to get a non-null ID, call {@link #register} first.
     *
     * @return  plastic ID, or null
     */
    public URI getRegisteredId() {
        return plasticId_;
    }

    /**
     * Returns the hub which this listener is registered with.
     * If this listener is not currently registered,
     * <code>null</code> will be returned.  If you want to do your best
     * to get a non-null hub, call {@link #register} first.
     *
     * @return  hub, or null
     */
    public PlasticHubListener getHub() {
        return hub_;
    }

    /**
     * Attempts to ensure that this listener is registered with the
     * prevailing PLASTIC hub.
     * If it's already registered, a check is made to see that the hub
     * is alive, and if so nothing else is done.
     * If it's not registered, an attempt is made to register.
     * By the end of the call <em>either</em> this listener will be
     * registered with a currently prevailing hub, <em>or</em> an
     * exception will be thrown.
     * It's therefore quite safe to call this method multiple times.
     *
     * @throws  IOException  if no registration can be achieved
     */
    public void register() throws IOException {

        /* Decide in a thread-safe manner whether we need to attempt a new
         * registration. */
        boolean doRegister;
        synchronized ( this ) {
            if ( plasticId_ == null ) {
                assert hub_ == null;
                doRegister = true;
            }
            else {
                assert hub_ != null;
                try {
                    hub_.getHubId();
                    doRegister = false;
                }
                catch ( Throwable e ) {
                    hub_ = null;
                    plasticId_ = null;
                    doRegister = true;
                }
            }
        }
        if ( doRegister ) {

            /* Attempt the registration. */
            assert hub_ == null;
            assert plasticId_ == null;
            try {
                PlasticHubListener hub = Plastic.getLocalHub();
                URI id = hub.registerRMI( APPLICATION_NAME,
                                          Arrays.asList( SUPPORTED_MESSAGES ),
                                          this );
                synchronized ( this ) {
                    hub_ = hub;
                    plasticId_ = id;
                }
                registerAct_.setEnabled( false );
                unregisterAct_.setEnabled( true );
            }
            catch ( IOException e ) {
                registerAct_.setEnabled( true );
                unregisterAct_.setEnabled( false );
                throw e;
            }
        }
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
        final PlasticHubListener hub;
        final URI id;
        synchronized ( this ) {
            if ( plasticId_ != null ) {
                assert hub_ != null;
                doUnregister = true;
                id = plasticId_;
                hub = hub_;
                plasticId_ = null;
                hub_ = null;
            }
            else {
                doUnregister = false;
                id = null;
                hub = null;
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
                    hub.unregister( id );
                    logger_.info( "PLASTIC unregistration successful" );
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            registerAct_.setEnabled( true );
                            unregisterAct_.setEnabled( false );
                        }
                    } );
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
     * Returns an action which will register/unregister with the PLASTIC hub.
     * The enabled status of the returned action is kept up to date.
     *
     * @param  reg   true for register action, false for unregister action
     * @return  action
     */
    public Action getRegisterAction( final boolean reg ) {
        return reg ? registerAct_ : unregisterAct_;
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
    private Object doPerform( URI sender, URI message, List args )
            throws IOException {

        /* Hub shutdown. */
        if ( HUB_STOP.equals( message ) ) {
            synchronized ( this ) {
                plasticId_ = null;
                hub_ = null;
            }
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

    /**
     * Action implementation that registers/unregisters with PLASTIC hub.
     * The enabled status of this action is kept suitably up to date.
     */
    private class RegisterAction extends AbstractAction {
        private final boolean reg_;
        RegisterAction( boolean reg ) {
            super( reg ? "Register with PLASTIC" : "Unregister with PLASTIC" );
            reg_ = reg;
            putValue( SHORT_DESCRIPTION,
                      reg ? "Accept interop requests from other tools"
                          : "Ignore interop requests from other tools" );
        }
        public void actionPerformed( ActionEvent evt ) {
            if ( reg_ ) {
                try {
                    register();
                }
                catch ( IOException e ) {
                    Component parent = evt.getSource() instanceof Component
                                     ? (Component) evt.getSource()
                                     : null;
                    ErrorDialog.showError( parent, "PLASTIC Error", e );
                }
            }
            else {
                unregister();
            }
        }
    }
}
