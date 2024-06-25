package uk.ac.starlink.connect;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Represents a connection to a remote resource.
 * Currently this resource is defined to provide access to a virtual 
 * filesystem as provided by the {@link #getRoot} method.
 * This functionality may be broadened or narrowed in the future.
 * 
 * <p>When constructed, a Connection should be live, and hopefully remain
 * so until {@link #logOut} is called.
 *
 * @author   Mark Taylor (Starlink)
 * @since    18 Feb 2005
 */
public abstract class Connection {

    private final Map<AuthKey,?> keys_;
    private final Connector connector_;
    private boolean tidy_;
    private LogoutThread shutdownHook_;
    private static Logger logger_ = 
        Logger.getLogger( "uk.ac.starlink.connect" );

    /**
     * Constructs a connection with no information.
     */
    protected Connection() {
        this( null, new HashMap<AuthKey,Object>() );
    }

    /**
     * Constructs a connection recording the circumstances under which
     * it was created.
     *
     * @param   connector   Connector which generated this connection
     * @param   keys   map giving the set of authorization values
     *                 used when opening this connection
     */
    protected Connection( Connector connector, Map<AuthKey,?> keys ) {
        connector_ = connector;
        keys_ = keys == null ? new HashMap<AuthKey,Object>()
                             : new HashMap<AuthKey,Object>( keys );
    }

    /**
     * Returns the value for a given authorization key used when opening
     * this connection, if known
     *
     * @param  key  authorization key
     * @return   value for <code>key</code>
     *           (of type <code>key.getValueType()</code>),
     *           or null
     */
    public Object getAuthValue( AuthKey key ) {
        return keys_.get( key );
    }

    /**
     * Returns the connector which generated this connection, if known.
     *
     * @return   connector, or null
     */
    public Connector getConnector() {
        return connector_;
    }

    /**
     * Indicates whether this connection is currently up and running.
     * Hopefully it will return true until {@link #logOut} has been called,
     * but it's possible that the connection may expire for some reason
     * before that.
     *
     * @return   true iff connection is up
     */
    public abstract boolean isConnected();

    /**
     * Closes this connection.
     *
     * @throws   IOException  if something went wrong
     */
    public abstract void logOut() throws IOException;

    /**
     * Returns the root of the remote filesystem provided by this connection.
     * This method should not do work, but only return an existing branch
     * (acquired at login time); that is the root branch should not
     * be constructed lazily.
     *
     * @return   root of the virtual filesystem associated with this connection
     */
    public abstract Branch getRoot();
    
    /**
     * Controls whether an attempt is made to shut down this connection
     * when the JVM exits if it has not been done within the program.
     * If set true, at system exit if {@link #isConnected} returns true
     * an attempt is made to call {@link #logOut}.  Note this might
     * cause JVM shutdown to be prolonged.  This is set false by
     * default.
     *
     * @param  tidy  true if you want this connection to be shut down on exit
     */
    public synchronized void setLogoutOnExit( boolean tidy ) {
        if ( tidy && shutdownHook_ == null ) {
            shutdownHook_ = new LogoutThread();
            Runtime.getRuntime().addShutdownHook( shutdownHook_ );
        }
        else if ( ! tidy && shutdownHook_ != null ) {
            Runtime.getRuntime().removeShutdownHook( shutdownHook_ );
            shutdownHook_ = null;
        }
    }

    public String toString() {
        return connector_ == null ? super.toString()
                                  : connector_.getName() + " connection";
    }

    /**
     * Class which performs logging out on system shutdown.
     */
    private class LogoutThread extends Thread {
        LogoutThread() {
            super( "Logout from " + Connection.this.toString() );
        }
        public void run() {
            if ( isConnected() ) {
                logger_.info( "Logging out from " + 
                              Connection.this.toString() );
                try {
                    logOut();
                }
                catch ( IOException e ) {
                    logger_.warning( "Logout error: " + e.getMessage() );
                }
            }
        }
    }
}
