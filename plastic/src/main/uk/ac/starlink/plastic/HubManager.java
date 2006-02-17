package uk.ac.starlink.plastic;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.votech.plastic.PlasticHubListener;
import org.votech.plastic.PlasticListener;

/**
 * Class which keeps track of registering and unregistering with the
 * PLASTIC hub on behalf of a listening application.
 * If you want to behave as a plastic 
 * listener, call {@link #register}.  If you want to stop listening,
 * call {@link #unregister}.  Either of these may be called more than
 * once harmlessly.  It is a good idea to call <code>register</code>
 * immediately before sending a plastic message to ensure that the
 * hub is up to date (it may be a different hub server than last time
 * you looked).  This class implements <code>PlasticListener</code>
 * and deals with generic messages as well as unregistering from the
 * hub on JVM shutdown etc.  Concrete subclasses must implement
 * {@link #doPerform} to provide any application-specific services.
 *
 * <p>The generic messages which are handled by this object directly
 * are currently:
 * <ul>
 * <li>ivo://votech.org/info/getName</li>
 * <li>ivo://votech.org/test/echo</li>
 * <li>ivo://votech.org/hub/HubStopping</li>
 * </ul>
 *
 * @author   Mark Taylor
 * @since    15 Feb 2006
 */
public abstract class HubManager implements PlasticListener {

    private final String applicationName_;
    private final URI[] supportedMessages_;
    private final Action registerAct_;
    private final Action unregisterAct_;
    private final JToggleButton.ToggleButtonModel registerToggle_;
    private URI plasticId_;
    private PlasticHubListener hub_;

    private static final URI GET_NAME;
    private static final URI ECHO;
    private static final URI HUB_STOPPING;
    private static final URI[] INTERNAL_SUPPORTED_MESSAGES = new URI[] {
        GET_NAME = createURI( "ivo://votech.org/info/getName" ),
        ECHO = createURI( "ivo://votech.org/test/echo" ),
        HUB_STOPPING = createURI( "ivo://votech.org/hub/event/HubStopping" ),
    };
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.plastic" );

    /**
     * Constructs a new manager.
     * The <code>supportedMessages</code> argument lists the additional 
     * messages for which support is provided.  Some standard ones are
     * supported by the HubManager itself.  If this array is empty, then
     * all messages will be delivered.
     *
     * @param  appName  the name of the application whose connections this
     *         object will manage
     * @param  supportedMessages  URIs of PLASTIC messages supported directly
     *         by this implementation's {@link #doPerform} method
     */
    public HubManager( String appName, URI[] supportedMessages ) {
        applicationName_ = appName;
        supportedMessages_ = (URI[]) supportedMessages.clone();
        registerAct_ = new RegisterAction( true );
        unregisterAct_ = new RegisterAction( false );
        registerToggle_ = new JToggleButton.ToggleButtonModel();
        registerToggle_.setSelected( false );
        registerToggle_.addChangeListener( new ChangeListener() {
            public void stateChanged( ChangeEvent evt ) {
                final boolean reg = registerToggle_.isSelected();
                new Thread( "PLASTIC " + ( reg ? "register" : "unregister" ) ) {
                    public void run() {
                        try {
                            if ( reg ) {
                                register();
                            }
                            else {
                                unregister();
                            }
                        }
                        catch ( IOException e ) {
                        }
                    }
                }.start();
            }
        } );
        updateState( false );
        Runtime.getRuntime()
               .addShutdownHook( new Thread( "PLASTIC hub unregister" ) {
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
                PlasticHubListener hub = PlasticUtils.getLocalHub();
                List supported = new ArrayList();
                if ( supportedMessages_.length > 0 ) {
                    supported.addAll( Arrays.asList( supportedMessages_ ) );
                    supported.addAll( Arrays
                                     .asList( INTERNAL_SUPPORTED_MESSAGES ) );
                }
                URI id = hub.registerRMI( applicationName_, supported, this );
                synchronized ( this ) {
                    hub_ = hub;
                    plasticId_ = id;
                }
                updateState( true );
            }
            catch ( IOException e ) {
                updateState( false );
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
                    try {
                        hub.unregister( id );
                        logger_.info( "PLASTIC unregistration successful" );
                    }
                    catch ( Throwable e ) {
                        logger_.info( "PLASTIC unregistration failed" );
                    }
                    updateState( false );
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
     * Returns a toggle button model which keeps track (as far as possible)
     * of whether this listener is currently registered with a hub.
     * Attempts to register/unregister can be performed by invoking its
     * <code>setSelected</code> method.
     */
    public JToggleButton.ToggleButtonModel getRegisterToggle() {
        return registerToggle_;
    }

    /**
     * Implements the PlasticListener interface.
     */
    public Object perform( URI sender, URI message, List args ) {
        logger_.info( "Received PLASTIC message " + message );
        if ( HUB_STOPPING.equals( message ) ) {
            synchronized ( this ) {
                plasticId_ = null;
                hub_ = null;
            }
            updateState( false );
        }
        if ( supportedMessages_.length == 0 ||
             Arrays.asList( supportedMessages_ ).contains( message ) ) {
            try {
                return doPerform( sender, message, args );
            }
            catch ( IOException e ) {
                logger_.warning( "PLASTIC message failed: " + e );
                return e;
            }
        }
        else if ( HUB_STOPPING.equals( message ) ) {
            return null;
        }
        else if ( ECHO.equals( message ) ) {
            return args.size() > 0 ? args.get( 0 ) : null;
        }
        else if ( GET_NAME.equals( message ) ) {
            return applicationName_;
        }
        else {
            throw new UnsupportedOperationException( "Unsupported message" 
                                                   + message );
        }
    }

    /**
     * Does the work for processing an application-type hub message.
     * Concrete subclasses should implement this method to perform the
     * application-specific listener behaviour for this listener.
     * The <code>sender</code> argument may be any of the URIs which
     * this object presented as the supported messages argument of
     * the <code>HubManager</code> constructor.
     *
     * @param  sender   sender ID
     * @param  message  message ID (determines the action required)
     * @param  args     message argument list
     * @return  return value requested by message
     */
    public abstract Object doPerform( URI sender, URI message, List args )
            throws IOException;

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
     * Updates the currently known state of the connection.
     *
     * @param   registered   true if we know we're currently registered with
     *          the prevailing hub, false if we know we're not
     */
    private void updateState( final boolean registered ) {
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                registerAct_.setEnabled( ! registered );
                unregisterAct_.setEnabled( registered );
                registerToggle_.setSelected( registered );
            }
        } );
    }

    /**
     * Convenience method to turn a String into a URI without throwing
     * any pesky checked exceptions.
     *
     * @param  uri  URI text
     * @return  URI
     * @throws  IllegalArgumentException   if uri doesn't look like a URI
     */
    public static URI createURI( String uri ) {
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
    public static boolean checkArgs( List args, Class[] types )
            throws IOException {
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
                    Object[] errmsg = new String[] {
                        "Registration with PLASTIC hub failed:",
                        e.toString(),
                    };
                    JOptionPane.showMessageDialog( parent, errmsg,
                                                   "PLASTIC Error",
                                                   JOptionPane
                                                  .WARNING_MESSAGE );
                }
            }
            else {
                unregister();
            }
        }
    }
}
