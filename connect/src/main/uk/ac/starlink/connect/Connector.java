package uk.ac.starlink.connect;

import java.util.Map;
import java.io.IOException;
import javax.swing.Icon;

/**
 * Interface for logging in to a remote facility.
 *
 * @author   Mark Taylor (Starlink)
 * @since    18 Feb 2005
 */
public interface Connector {

    /**
     * Returns the name of the type of facility to which this connector
     * can connect.
     */
    String getName();

    /**
     * Returns an icon which labels this connector.
     * The icon should preferably be 20x20 pixels.
     * Null may be returned if you have no suitable icon.
     *
     * @return   icon for this connector
     */
    Icon getIcon();

    /**
     * Returns an array of authorization keys whose values are required
     * to attempt a connection.  These will commonly include name and
     * password keys, but there may be others.
     *
     * @return   authorization keys
     */
    AuthKey[] getKeys();

    /**
     * Attempts to open a connection.
     * The supplied <tt>authValues</tt> map contains an entry for each of
     * the keys returned by {@link #getKeys}, with the entry's value
     * being the value for that key.
     * Thus the values will typically be the user's name, password, etc.
     * The values will be either <tt>String</tt> or <tt>char[]</tt> values
     * or <tt>null</tt> (<tt>char[]</tt> may be used for hidden values for
     * security reasons).
     *
     * @param    authValues   AuthKey->value map containing connection
     *           information
     * @return   a live connection object
     * @throws   IOException  if there was some error, for instance
     *           authorization failure
     */
    Connection logIn( Map authValues ) throws IOException;

}
