package uk.ac.starlink.connect;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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

    private final Map keys_;

    /**
     * Constructor.
     * The <tt>keys</tt> argument gives the set of authorization values
     * which was used when opening this connection.
     *
     * @param   keys   AuthKey -> value map
     */
    protected Connection( Map keys ) {
        keys_ = new HashMap( keys );
    }

    /**
     * Returns the value for a given authorization key used when opening
     * this connection.
     *
     * @param  key  authorization key
     * @return   value for <tt>key</tt> (of type <tt>key.getValueType()</tt>)
     */
    public Object getAuthValue( AuthKey key ) {
        return (String) keys_.get( key );
    }

    /**
     * Indicates whether this connection is currently up and running.
     * Hopefully it will return true until {@link #logOut} has been called,
     * but it's possible that the connection may expire for some reason
     * before that.
     */
    public abstract boolean isConnected();

    /**
     * Closes this connection.
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
    
}
