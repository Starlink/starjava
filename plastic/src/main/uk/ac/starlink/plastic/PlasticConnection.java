package uk.ac.starlink.plastic;

import java.net.URI;

/**
 * Simple interface describing a connnection to a hub.
 * Well-behaved implementations should ensure that the connection is 
 * closed even if {@link #unregister} is not explicitly called, for
 * instance in the finalizer and/or at JVM shutdown.
 *
 * @author  Mark Taylor
 * @since   7 Aug 2006
 */
public interface PlasticConnection {

    /**
     * Returns the client ID which identifies this connection to the hub.
     *
     * @return  client ID
     */
    URI getId();

    /**
     * Ensures that this connection is no longer registered with the hub.
     * This method may be called multiple times; any time after the 
     * first has no effect.
     */
    void unregister();
}
