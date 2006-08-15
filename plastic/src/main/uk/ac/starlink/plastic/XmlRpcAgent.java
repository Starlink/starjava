package uk.ac.starlink.plastic;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.xmlrpc.XmlRpcClient;
import org.apache.xmlrpc.XmlRpcClientLite;
import org.apache.xmlrpc.XmlRpcException;

/**
 * Agent implementation which uses XML-RPC for communications.
 *
 * @author   Mark Taylor
 * @since    16 Feb 2006
 */
class XmlRpcAgent extends Agent {

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.plastic" );
    private final XmlRpcClient client_;
    private final String connection_;

    /**
     * Constructor.
     *
     * @param   iseq  unique seqence id, used for disambiguating URIs
     * @param   name  generic name of the application
     * @param   xmlrpcUrl   URL of the listening XML-RPC server which can
     *          execute PLASTIC requests
     */
    public XmlRpcAgent( int iseq, String name, URI[] supportedMessages,
                        URL xmlrpcUrl ) {
        super( iseq, name, supportedMessages );
        client_ = PlasticUtils.createXmlRpcClient( xmlrpcUrl );
        connection_ = "XMLRPC: " + xmlrpcUrl;
    }

    public String getConnection() {
        return connection_;
    }

    public Object request( URI sender, URI message, List args ) 
            throws IOException {
        Vector argv = new Vector();
        argv.add( sender.toString() );
        argv.add( message.toString() );
        argv.add( doctorObject( args ) );
        try {
            String method = PlasticUtils.XMLRPC_PREFIX == null 
                          ? "perform"
                          : ( PlasticUtils.XMLRPC_PREFIX + ".perform" );
            Object result = client_.execute( method, argv );
            if ( result instanceof IOException ) {
                throw (IOException) result;
            }
            else if ( result instanceof Throwable ) {
                Throwable err = (Throwable) result;
                throw (IOException) new IOException( err.getMessage() )
                                   .initCause( err );
            }
            else {
                return result;
            }
        }
        catch ( IOException e ) {
            throw e;
        }
        catch ( XmlRpcException e ) {
            return (IOException) new IOException( e.getMessage() )
                                .initCause( e );
        }
    }

    /**
     * Turns an object into a version which will be palatable for XML-RPC
     * transport - this basically involves turning collections into Vectors
     * and stringifying some other things.
     *
     * @param   obj   basic object
     * @return  doctored object
     */
    public static Object doctorObject( Object obj ) {
        if ( obj == null ) {
            return new Vector();
        }
        else if ( obj instanceof URL ) {
            return obj.toString();
        }
        else if ( obj instanceof URI ) {
            return obj.toString();
        }
        else if ( obj instanceof Collection ) {
            Vector vec = new Vector();
            for ( Iterator it = ((Collection) obj).iterator(); it.hasNext(); ) {
                vec.add( doctorObject( it.next() ) );
            }
            return vec;
        }
        else if ( obj instanceof Object[] ) {
            Vector vec = new Vector();
            Object[] array = (Object[]) obj;
            for ( int i = 0; i < array.length; i++ ) {
                vec.add( doctorObject( array[ i ] ) );
            }
            return vec;
        }
        else if ( obj instanceof Map ) {
            Hashtable hash = new Hashtable();
            for ( Iterator it = ((Map) obj).entrySet().iterator();
                  it.hasNext(); ) {
                Map.Entry entry = (Map.Entry) it.next();
                hash.put( String.valueOf( entry.getKey() ),
                          doctorObject( entry.getValue() ) );
            }
            return hash;
        }
        else if ( obj instanceof Throwable ) {
            logger_.log( Level.WARNING, "XML-RPC exception: " + obj,
                         (Throwable) obj );
            return new Vector();
        }
        else if ( obj instanceof Integer ||
                  obj instanceof Boolean ||
                  obj instanceof String ||
                  obj instanceof Double ||
                  obj instanceof Date ||
                  obj instanceof byte[] ) {
            return obj;
        }
        else {
            logger_.warning( "Bad XML-RPC object type "
                           + obj.getClass().getName() + " - using empty list" );
            return new Vector();
        }
    }
}
