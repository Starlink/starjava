package uk.ac.starlink.plastic;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JToggleButton;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
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
 * <li>ivo://votech.org/hub/event/ApplicationRegistered</li>
 * <li>ivo://votech.org/hub/event/ApplicationUnregistered</li>
 * </ul>
 *
 * <p>Actions are also provided for registering/unregistering and starting
 * a hub, ready for use in a GUI.
 *
 * @author   Mark Taylor
 * @since    15 Feb 2006
 */
public abstract class HubManager implements PlasticListener {

    private final String applicationName_;
    private final URI[] supportedMessages_;
    private final JToggleButton.ToggleButtonModel registerToggle_;
    private URI plasticId_;
    private PlasticHubListener hub_;
    private ApplicationListModel appListModel_;
    private JFrame hubWindow_;
    private int autoRegInterval_;
    private Thread autoThread_;

    private static final URI[] INTERNAL_SUPPORTED_MESSAGES = new URI[] {
        MessageId.TEST_ECHO,
        MessageId.INFO_GETNAME,
        MessageId.HUB_STOPPING,
        MessageId.HUB_APPREG,
        MessageId.HUB_APPUNREG,
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
        try {
            Runtime.getRuntime()
                   .addShutdownHook( new Thread( "PLASTIC hub unregister" ) {
                public void run() {
                    unregister();
                }
            } );
        }
        catch ( SecurityException e ) {
        }
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
                    setHub( null );
                    assert hub_ == null;
                    plasticId_ = null;
                    doRegister = true;
                }
            }
        }
        try {
            if ( doRegister ) {

                /* Attempt the registration. */
                assert hub_ == null;
                assert plasticId_ == null;
                try {
                    PlasticHubListener hub = PlasticUtils.getLocalHub();
                    Collection supported = new HashSet();
                    if ( supportedMessages_.length > 0 ) {
                        supported.addAll( Arrays.asList( supportedMessages_ ) );
                        supported.addAll(
                            Arrays.asList( INTERNAL_SUPPORTED_MESSAGES ) );
                    }
                    URI id = hub.registerRMI( applicationName_,
                                              new ArrayList( supported ),
                                              this );
                    synchronized ( this ) {
                        setHub( hub );
                        assert hub_ == hub;
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
        finally {
            if ( autoRegInterval_ > 0 ) {
                runAutoRegistration( true );
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

        /* If this is a genuine unregistration, make sure that the 
         * auto-register thread is deactivated.
         * If it's a pseudo-unregistration triggered by the hub stopping
         * (not currently registered) then no action is required. */
        if ( plasticId_ != null ) {
            runAutoRegistration( false );
        }

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
                setHub( null );
                assert hub_ == null;
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
                        logger_.config( "PLASTIC unregistration successful" );
                    }
                    catch ( Throwable e ) {
                        logger_.config( "PLASTIC unregistration failed" );
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
     * Sets the auto registration thread to be running or stopped.
     * Calling this method with the current state is legal and has no effect.
     *
     * @param  active  true iff the auto registration thread should be running
     */
    private synchronized void runAutoRegistration( boolean active ) {
        if ( active && autoThread_ == null ) {
            Thread autoThread = new Thread( "PLASTIC Registration" ) {
                public void run() {
                    try {
                        while ( ! Thread.currentThread().isInterrupted() ) {
                            if ( plasticId_ == null &&
                                 PlasticUtils.isHubRunning() ) {
                                try {
                                    register();
                                }
                                catch ( IOException e ) {
                                }
                                catch ( RuntimeException e ) {
                                }
                                catch ( Error e ) {
                                }
                            }
                            if ( autoRegInterval_ > 0 ) {
                                Thread.sleep( autoRegInterval_ );
                            }
                        }
                    }
                    catch ( InterruptedException e ) {
                    }
                }
            };
            autoThread.setDaemon( true );
            autoThread.start();
            autoThread_ = autoThread;
        }
        else if ( ! active && autoThread_ != null ) {
            Thread autoThread = autoThread_;
            autoThread_ = null;
            autoThread.interrupt();
        }
    }

    /**
     * Controls whether auto registration is used, and how often to attempt
     * a connection if unregistered.
     * If <code>interval&gt;0</code>, then a separate thread will 
     * remain active ensuring that this object is always registered 
     * with a hub, if a hub is present.  It will check every 
     * <code>interval</code> milliseconds that there is a connection to 
     * the hub, and attempt to make one if there is not.
     * If <code>interval&lt;=0</code> (the default), then no autoregistration
     * is performed.
     *
     * @param  interval  autoregistration attempt interval in milliseconds,
     *                   or &lt;=0 to disable registration
     */
    public synchronized void setAutoRegister( int interval ) {
        autoRegInterval_ = interval;
        runAutoRegistration( interval > 0 );
    }

    /**
     * Returns an action which will register/unregister with the PLASTIC hub.
     * The enabled status of the returned action is kept up to date.
     *
     * @param  reg   true for register action, false for unregister action
     * @return  action
     */
    public Action getRegisterAction( final boolean reg ) {
        return new RegisterAction( reg );
    }

    /**
     * Returns an action which will attempt to start up a PLASTIC hub.
     * Depending on the <code>internal</code> flag, the action may either
     * start a hub in this JVM (in which case it will expire when the JVM does)
     * or in a separate process.
     * An attempt is made to keep the enabledness of this action up to date
     * with current knowledge about whether the hub is running or not.
     *
     * @param   internal   true for an in-JVM hub, false for one in a new JVM
     * @return  hub start action
     */
    public Action getHubStartAction( final boolean internal ) {
        final Action hubAct = new AbstractAction() {
            public void actionPerformed( ActionEvent evt ) {
                try {
                    if ( internal ) {
                        PlasticHub.startHub( null, null );
                    }
                    else {
                        PlasticUtils.startExternalHub( true );
                    }
                }
                catch ( Throwable e ) {
                    Component parent = evt.getSource() instanceof Component
                                     ? (Component) evt.getSource()
                                     : null;
                    Object errmsg = new String[] {
                        "Failed to start new PLASTIC hub:",
                        e.toString(),
                    };
                    JOptionPane.showMessageDialog( parent, errmsg,
                                                   "Hub Start Error",
                                                   JOptionPane
                                                  .WARNING_MESSAGE );
                }
            }
        };
        hubAct.putValue( Action.NAME,
                         "Start " + ( internal ? "internal" : "external" ) +
                         " PLASTIC Hub" );
        hubAct.putValue( Action.SHORT_DESCRIPTION,
                         "Start a PLASTIC hub running" +
                         ( internal ? " in this JVM"
                                    : " in a separate process" ) );
        registerToggle_.addChangeListener( new ChangeListener() {
            public void stateChanged( ChangeEvent evt ) {
                if ( ! registerToggle_.isSelected() ) {
                    hubAct.setEnabled( true );
                }
                else {
                    hubAct.setEnabled( ! PlasticUtils.isHubRunning() );
                }
            }
        } );
        if ( ! registerToggle_.isSelected() ) {
            hubAct.setEnabled( true );
        }
        else {
            hubAct.setEnabled( ! PlasticUtils.isHubRunning() );
        }
        return hubAct;
    }

    /**
     * Returns an action that can be used to post a window showing the
     * state of the hub with which this object has a connection.
     *
     * @return   hub watcher window action
     */
    public Action getHubWatchAction() {
        final Action watchAct = new AbstractAction() {
            public void actionPerformed( ActionEvent evt ) {
                if ( hubWindow_ == null ) {
                    hubWindow_ = 
                        new PlasticListWindow( getApplicationListModel() );
                    hubWindow_.setTitle( "PLASTIC apps" );
                    hubWindow_.pack();
                }
                hubWindow_.setVisible( true );
            }
        };
        watchAct.putValue( Action.NAME, "Show Registered Applications" );
        watchAct.putValue( Action.SHORT_DESCRIPTION,
                           "Display applications " +
                           "registered with the PLASTIC hub" );
        return watchAct;
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
     * Returns a ListModel which keeps track of the currently registered
     * applications.  Elements of the returned model are instances of
     * {@link ApplicationItem}.
     *
     * @return  list model of registered applications
     */
    public synchronized ListModel getApplicationListModel() {
        if ( appListModel_ == null ) {
            final ApplicationListModel model = new ApplicationListModel();
            final boolean[] done = new boolean[ 1 ];
            if ( hub_ != null ) {
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        model.setItems( PlasticUtils
                                       .getRegisteredApplications( hub_ ) );
                    }
                } );
            }
            appListModel_ = model;
        }
        return appListModel_;
    }

    /**
     * Sets the current value of this manager's hub.
     *
     * @param  hub  new hub; may be null
     */
    private void setHub( final PlasticHubListener hub ) {
        hub_ = hub;
        final ApplicationListModel model = appListModel_;
        if ( model != null ) {
            SwingUtilities.invokeLater( new Runnable() {
                public void run() {
                    ApplicationItem[] apps = hub == null
                        ? new ApplicationItem[ 0 ]
                        : PlasticUtils.getRegisteredApplications( hub );
                    model.setItems( apps );
                }
            } );
        }
    }

    /**
     * Implements the PlasticListener interface.
     */
    public Object perform( URI sender, URI message, List args ) {
        logger_.config( "Received PLASTIC message " + message );
        if ( MessageId.HUB_STOPPING.equals( message ) ) {
            synchronized ( this ) {
                plasticId_ = null;
                setHub( null );
            }
            updateState( false );
        }
        else if ( MessageId.HUB_APPREG.equals( message ) ) {
            try {
                URI id = createURI( args.get( 0 ).toString() );
                appListModel_.register( id, hub_.getName( id ),
                                        hub_.getUnderstoodMessages( id ) );
            }
            catch ( Exception e ) {
            }
        }
        else if ( MessageId.HUB_APPUNREG.equals( message ) ) {
            try {
                URI id = createURI( args.get( 0 ).toString() );
                appListModel_.unregister( id );
            }
            catch ( Exception e ) {
            }
        }
        if ( supportedMessages_.length == 0 ||
             Arrays.asList( supportedMessages_ ).contains( message ) ) {
            logger_.info( "Processing supported message: " + message + 
                          " from " + sender );
            logger_.config( "Message arguments: "
                          + PlasticMonitor.stringify( args ) );
            try {
                Object ret = doPerform( sender, message, args );
                logger_.config( "Message return: "
                              + PlasticMonitor.stringify( ret ) );
                return ret;
            }
            catch ( IOException e ) {
                logger_.warning( "PLASTIC message failed: " + e );
                return e;
            }
        }
        else if ( MessageId.TEST_ECHO.equals( message ) ) {
            return args.size() > 0 ? args.get( 0 ) : null;
        }
        else if ( MessageId.INFO_GETNAME.equals( message ) ) {
            return applicationName_;
        }
        else if ( Arrays.asList( INTERNAL_SUPPORTED_MESSAGES )
                        .contains( message ) ) {
            return null;
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
     * as required types or more (to permit future extension of message
     * definitions), and each argument is either null or of the
     * class in the correponding position in <code>types</code>, or a
     * subclass of that.
     *
     * @param  args  actual argument list
     * @param  types  classes you want to see represented in the initial
     *                elements of <code>list</code>
     */
    public static boolean checkArgs( List args, Class[] types )
            throws IOException {
        String msg = null;
        if ( args.size() < types.length ) {
            msg = "Not enough arguments";
        }
        else {
            for ( int i = 0; i < types.length; i++ ) {
                Object arg = args.get( i );
                if ( arg != null &&
                     ! types[ i ].isAssignableFrom( arg.getClass() ) ) {
                    msg = "Wrong type of arguments";
                }
            }
        }
        if ( msg == null ) {
            return true;
        }
        else {
            StringBuffer sbuf = new StringBuffer( "PLASTIC: " )
                .append( msg )
                .append( "; (" );
            for ( Iterator it = args.iterator(); it.hasNext(); ) {
                Object arg = it.next();
                sbuf.append( arg == null ? "null" : arg.getClass().getName() );
                if ( it.hasNext() ) {
                    sbuf.append( ", " );
                }
            }
            sbuf.append( ") != (" );
            for ( int i = 0; i < types.length; i++ ) {
                if ( i > 0 ) {
                    sbuf.append( ", " );
                }
                sbuf.append( types[ i ].getName() );
            }
            sbuf.append( ')' );
            throw new IOException( sbuf.toString() );
        }
    }

    /**
     * Action implementation that registers/unregisters with PLASTIC hub.
     * The enabled status of this action is kept suitably up to date.
     */
    private class RegisterAction extends AbstractAction
                                 implements ChangeListener {
        private final boolean reg_;

        RegisterAction( boolean reg ) {
            super( reg ? "Register with PLASTIC" : "Unregister with PLASTIC" );
            reg_ = reg;
            putValue( SHORT_DESCRIPTION,
                      reg ? "Accept interop requests from other tools"
                          : "Ignore interop requests from other tools" );
            registerToggle_.addChangeListener( this );
            stateChanged( null );
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
                    Object errmsg = new String[] {
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

        public void stateChanged( ChangeEvent evt ) {
            setEnabled( reg_ ^ registerToggle_.isSelected() );
        }
    }
}
