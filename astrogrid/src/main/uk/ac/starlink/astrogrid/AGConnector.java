package uk.ac.starlink.astrogrid;

import org.astrogrid.store.tree.TreeClient;
import org.astrogrid.store.tree.TreeClientException;

/**
 * Interface for an object which can provide a connection to an 
 * AstroGrid community.  Implementations will typically ask the user
 * for login information, perhaps with some sensible defaults,
 * but other implementations are possible, for instance ones which
 * simply have a single fixed TreeClient.
 *
 * @author   Mark Taylor (Starlink)
 * @since    3 Dec 2004
 */
public interface AGConnector {

    /**
     * Returns the user name. 
     * Following a successful call to {@link #getConnection}, this is
     * the user name used to log in.
     *
     * @return   user name
     */
    String getUser();

    /**
     * Returns the community name.
     * Following a successful call to {@link #getConnection}, this is
     * the name of the community that was logged into.
     *
     * @return   community identifier
     */
    String getCommunity();

    /**
     * Returns a TreeClient ready for use (logged in) or <tt>null</tt> if
     * no successful authentication could be performed.
     * A <tt>TreeClientException</tt> may be thrown if some 
     * user-level notification ought to be made of failure.
     * However, <tt>null</tt> should be returned in the case that 
     * the user has indicated (e.g. by hitting a Cancel button) that
     * no connection is what he wants.
     *
     * <p>Implementations which may run in a graphical environment 
     * should be prepared for the possibility that this
     * method will be called from the AWT event dispatch thread.
     *
     * @return   an open client, or <tt>null</tt>
     */
    TreeClient getConnection() throws TreeClientException;
}
