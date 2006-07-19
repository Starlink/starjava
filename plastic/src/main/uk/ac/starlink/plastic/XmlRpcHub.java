package uk.ac.starlink.plastic;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.xmlrpc.XmlRpcClient;
import org.apache.xmlrpc.XmlRpcException;
import org.votech.plastic.PlasticHubListener;
import org.votech.plastic.PlasticListener;

/**
 * PlasticHubListener implementation which does all communication with
 * hub process using XML-RPC.  This could be described as masochistic,
 * since it is much easier to acquire a PlasticHubListener remote object 
 * using RMI rather than implementing one in the usual way.
 * It is only really intended for testing purposes.
 *
 * @author   Mark Taylor
 * @since    25 May 2006
 */
class XmlRpcHub implements PlasticHubListener {

    private final XmlRpcClient client_;
    private final PlasticHubListener properHub_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.plastic" );

    /**
     * Constructs a new hub.
     * As well as the XML-RPC URL of the hub process, a full (presumably RMI)
     * hub object is also passed.  This is required only to implement the
     * {@link #registerRMI} method, which is not easy (possible?) to do 
     * using XML-RPC.  If you don't plan calling <code>registerRMI</code>
     * on this hub, it can be null.
     * 
     * @param   xmlrpcUrl   URL for communication with the hub's XML-RPC server
     * @param   properHub   RMI hub object
     */
    public XmlRpcHub( URL xmlrpcUrl, PlasticHubListener properHub ) {
        client_ = PlasticUtils.createXmlRpcClient( xmlrpcUrl );
        properHub_ = properHub;
    }

    public List getRegisteredIds() {
        return toUriList( (List) xmlrpcCall( "getRegisteredIds",
                                             new Object[ 0 ] ) );
    }

    public URI getHubId() {
        return toUri( xmlrpcCall( "getHubId", new Object[ 0 ] ) );
    }

    public String getName( URI id ) {
        return (String) xmlrpcCall( "getName", new Object[] { id } );
    }

    public List getUnderstoodMessages( URI id ) {
        return toUriList( (List) xmlrpcCall( "getUnderstoodMessages",
                                             new Object[] { id } ) );
    }

    public List getMessageRegisteredIds( URI message ) {
        return toUriList( (List) xmlrpcCall( "getMessageRegisteredIds",
                                             new Object[] { message } ) );
    }

    public URI registerXMLRPC( String name, List supportedMessages,
                               URL callbackURL ) {
        return toUri( xmlrpcCall( "registerXMLRPC",
                                  new Object[] { name, supportedMessages,
                                                 callbackURL } ) );
    }

    public URI registerRMI( String name, List supportedMessages,
                            PlasticListener caller ) {
        return properHub_.registerRMI( name, supportedMessages, caller );
    }

    public URI registerNoCallBack( String name ) {
        return toUri( xmlrpcCall( "registerNoCallBack",
                                  new Object[] { name } ) );
    }

    public URI registerRMI( String name, List supportedMessages,
                            URL callbackUrl ) {
        return toUri( xmlrpcCall( "registerRMI",
                                  new Object[] { name, supportedMessages,
                                                 callbackUrl } ) );
    }

    public void unregister( URI id ) {
        xmlrpcCall( "unregister", new Object[] { id } );
    }

    public Map request( URI sender, URI message, List args ) {
        return toUriMap( (Map) xmlrpcCall( "request",
                                           new Object[] { sender, message,
                                                          args } ) );
    }

    public Map requestToSubset( URI sender, URI message, List args, 
                                List recipientIds ) {
        return toUriMap( (Map) xmlrpcCall( "requestToSubset",
                                           new Object[] { sender, message, args,
                                                          recipientIds } ) );
    }

    public void requestToSubsetAsynch( URI sender, URI message, List args,
                                       List recipientIds ) {
       xmlrpcCall( "requestToSubsetAsynch",
                   new Object[] { sender, message, args, recipientIds } );
    }

    public void requestAsynch( URI sender, URI message, List args ) {
        xmlrpcCall( "requestAsynch",
                    new Object[] { sender, message, args } );
    }

    /**
     * Performs an XML-RPC call.  Arguments are converted as required by
     * the XML-RPC implementation - URIs turned to strings,
     * Lists turned to Vectors etc - before the message is sent.
     *
     * @param   method   method name
     * @param   args     argument array
     * @return  method return value
     * @see   XmlRpcAgent#doctorObject
     */
    private Object xmlrpcCall( String method, Object[] args ) {
        Vector argv = new Vector();
        if ( args != null ) {
            for ( int i = 0; i < args.length; i++ ) {
                argv.add( XmlRpcAgent.doctorObject( args[ i ] ) );
            }
        }
        try {
            Object result = client_.execute( "plastic.hub." + method, argv );
            if ( result instanceof XmlRpcException ) {
                throw (XmlRpcException) result;
            }
            return result;
        }
        catch ( Exception e ) {
            logger_.log( Level.WARNING, "XML-RPC Execution error: " + e, e );
            return null;
        }
    }

    /**
     * Turns an object (probably a string) into a URI.
     * If it can't be done, a warning is logged and null is returned.
     *
     * @param   obj  basis for URI
     * @return  URI
     */
    private static URI toUri( Object obj ) {
        try {
            return new URI( (String) obj );
        }
        catch ( Exception e ) {
            logger_.warning( "Bad URI " + obj + " - returning null" );
            return null;
        }
    }

    /**
     * Converts elements of a list (probably strings) to URIs.
     *
     * @param  list of strings
     * @return  list of URIs
     */
    private static List toUriList( List list ) {
        if ( list == null ) {
            return null;
        }
        int n = list.size();
        for ( int i = 0; i < n; i++ ) {
            list.set( i, toUri( list.get( i ) ) );
        }
        return list;
    }

    /**
     * Converts keys of a map from strings to URIs.
     *
     * @param  map  map with String keys
     * @return   map with URI keys
     */
    private static Map toUriMap( Map map ) {
        if ( map == null ) {
            return null;
        }
        Map fixMap = new Hashtable();
        for ( Iterator it = map.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            it.remove();
            fixMap.put( toUri( entry.getKey() ), entry.getValue() );
        }
        return fixMap;
    }
}
