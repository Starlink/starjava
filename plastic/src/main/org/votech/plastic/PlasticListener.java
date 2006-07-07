package org.votech.plastic;

import java.net.URI;
import java.util.List;


/***
 * The interface that java-rmi Plastic-compatible applications should support.
 * 
 * @see <a href="http://plastic.sourceforge.net/">http://plastic.sourceforge.net</a>
 * @author jdt@roe.ac.uk
 * @date 11-Oct-2005
 * @version 0.2
 * @since 1.3
 */
public interface PlasticListener {
    /***
     * The current version of Plastic defined by this interface.
     */
    String CURRENT_VERSION = "0.4";

    /***
     * Request that the application perform an action based on a message.
     * 
     * @param sender the ID of the originating application.
     * @param message the URI representing the action.
     * @param args any arguments to pass.
     * @return any return value of the action.
     * @xmlrpc the URIs are strings (of the appropriate form) and the List is an array
     * @see <a href="http://plastic.sourceforge.net">http://plastic.sourceforge.net</a>
     */
    public Object perform(URI sender, URI message, List args);

}
