package org.votech.plastic;
 
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;

/***
 * The interface that a Plastic Hub should support. For information on what a Plastic Hub is, and why you'd want one,
 * please see the URL below.
 * 
 * @see <a href="http://plastic.sourceforge.net/">http://plastic.sourceforge.net</a>
 * @author jdt@roe.ac.uk
 * @version 0.3
 * @service plastic.hub
 * @since 1.3
 * @date 11-Oct-2005
 */
public interface PlasticHubListener {
    /***
     * The key used to get the plastic.version out of the Plastic Hub config file.
     * 
     * @see #PLASTIC_CONFIG_FILENAME
     */
    public static final String PLASTIC_VERSION_KEY = "plastic.version";

    /***
     * The key used to get the URL of the xml-rpc server out of the Plastic Hub config file.
     * 
     * @see #PLASTIC_CONFIG_FILENAME
     */
    public static final String PLASTIC_XMLRPC_URL_KEY = "plastic.xmlrpc.url";

    /***
     * The key used to get the RMI port out of the Plastic Hub config file.
     * 
     * @see #PLASTIC_CONFIG_FILENAME
     */
    public static final String PLASTIC_RMI_PORT_KEY = "plastic.rmi.port";

    /***
     * The name of the file containing Plastic Hub config information (placed in ${user.home}). This file currently
     * duplicates the information in the standard ACR config files, but is present to allow for future expansion and to
     * be "organisation-neutral".
     * 
     */
    public static final String PLASTIC_CONFIG_FILENAME = ".plastic";

    /***
     * Get all the IDs of the currently registered applications.
     * 
     * @return see above
     * @xmlrpc returns an array of strings
     */
    public List getRegisteredIds();

    /***
     * Get this hub's ID. The hub "registers with itself", and this method will give you its own Id.
     * @xmlrpc returns a string
     * @return see above
     */
    public URI getHubId();
    
    /***
     * Get the name of this application, as used at registration.
     * @param plid the plastic ID returned at registration
     * @return The user friendly name
     */
    public String getName(URI plid);
    
    /***
     * Get the messages understood by this application.  As usual, an empty list means
     * it will listen to all messages.  Note that just because an application declares itself
     * to be interested in a message, it's no guarantee it will act on it.
     * @param plid the plastic ID returned at registration
     * @return A list of message URIs
     * @xmlrpc returns an array of strings
     */
    public List getUnderstoodMessages(URI plid);    
    
    /***
     * Get all the applications that support a particular message
     * @param message the messageId you're interested in
     * @return a list of plastic ids.
     */
    public List getMessageRegisteredIds( URI message );

    /***
     * Register an application with the hub. Each application that wishes to use the hub should register with it - the
     * hub may not forward messages from applications whose ID it doesn't recognise. There are different register
     * methods dependening on how (and whether) the application wishes to receive messages back from the hub.
     * 
     * @param name An optional string with a short name describing the application. This may be added to the hub
     *            assigned ID, making it more human friendly.
     * @param supportedMessages an array of messages (as URIs) the application is interested in. An empty List signifies
     *            "all".
     * @param callBackURL the application's internal xmlrpc server URL. Used by the hub to send messages to the
     *            application.
     * @return a hub-assigned ID
     * @see <a href="http://plastic.sourceforge.net/">http://plastic.sourceforge.net</a>
     * @see #registerRMI(String, List, PlasticListener)
     * @see #registerNoCallBack(String)
     * @xmlrpc The supportedMessages parameter is an array of strings formatted as URIs.
     */
    public URI registerXMLRPC(String name, List supportedMessages,
            URL callBackURL);

    /***
     * A java-rmi version of {@link #registerXMLRPC(String, List, URL) registerXMLRPC}
     * 
     * @param name see {@link #registerRMI(String, List, PlasticListener) registerRMI}
     * @param caller the PlasticListener that wishes to register
     * @see #registerXMLRPC(String, List, URL) for other parameters
     * @xmlrpc Not available.
     * @example Suppose your application implements {@link PlasticListener PlasticListener}. Then you register with the
     *          hub to receive ALL messages using <code>URI id = hub.registerRMI("MyApp", new ArrayList(),this);</code>
     */
    public URI registerRMI(String name, List supportedMessages,
            PlasticListener caller);

    /***
     * Register this application with the hub, but don't send it any messages in return. This is to allow uncallable
     * applications like scripting environments to register. 
     * spec.
     * 
     * @see #registerXMLRPC(String, List, URL) for parameters
     */
    public URI registerNoCallBack(String name);

//  /***
//   * Register this application with the hub, but store messages for later recovery by polling.
//   * Note that this message is experimental and not part of the Plastic Spec.
//   * @see #registerXMLRPC(String, List, URL) for parameters
//   * @see #pollForMessages(URI)
//   */
//  public URI registerPolling(String name, List supportedMessages);

//  /***
//   * Poll for messages.  Returns a List of messages.  Each List is another
//   * List containing (sender, message, args).
//   * Note that this message is experimental and not part of the Plasti Spec.
//   * @xmlrpc for List, read Array
//   * @see #registerPolling(String, List)
//   */
//  public List pollForMessages(URI id);
    
    /***
     * Unregister the application from the hub.
     * 
     * @param id the application to unregister
     */
    public void unregister(URI id);

    /***
     * Send a message to all registered Plastic applications.
     * 
     * @param sender the id of the originating tool - provided by the hub on 
     * registration.  Note that the hub is at liberty to refused to forward requests that
     *            don't come from an ID that it has registered.
     * @param message the message to send.
     * @param args any arguments to pass with the message
     * @return a Map of application ids to responses
     * @xmlrpc the return object is an array a struct whose elements names are application ids 
     */
    public Map request(URI sender, URI message, List args);

    /***
     * Send a request to listed registered Plastic apps. See {@link #request(URI, URI, List) request} for
     * details of the other parameters.
     * 
     * @param recipientIds a list of target application ids (as URIs)
     * @xmlrpc the list of target application ids is an array of strings
     */
    public Map requestToSubset(URI sender, URI message, List args,
            List recipientIds);

    /***
     * Send a request to listed registered Plastic apps, but don't wait for a response.
     * 
     * @param recipientIds a List of target application ids (as URIs). See {@link #request(URI, URI, List) request} for
     *            details of the other parameters.
     * @xmlrpc the recipientIds is an array of strings formatted as URIs
     */

    public void requestToSubsetAsynch(URI sender, URI message,
            List args, List recipientIds);

    /***
     * Send a request to all registered Plastic apps, but don't wait for a response. See
     * {@link #request(URI, URI, List) request} for details of parameters.
     */
    public void requestAsynch(URI sender, URI message, List args);

}
