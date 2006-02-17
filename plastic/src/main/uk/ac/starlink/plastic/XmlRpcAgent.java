package uk.ac.starlink.plastic;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
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

    private final XmlRpcClient client_;

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
        client_ = new XmlRpcClient( xmlrpcUrl );
    }

    public Object request( URI sender, URI message, List args ) 
            throws IOException {
        Vector argv = new Vector();
        argv.add( sender.toString() );
        argv.add( message.toString() );
        argv.add( doctorArgs( args ) );
        try {
            Object result = client_.execute( "plastic.client.perform", argv );
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

    public void requestAsynch( final URI sender, final URI message,
                               final List args ) {
        new Thread() {
            public void run() {
                try {
                    request( sender, message, args );
                }
                catch ( Throwable e ) {
                }
            }
        }.start();
    }

    /**
     * Turns an object into a version which will be palatable for XML-RPC
     * transport - this basically involves turning collections into Vectors
     * and stringifying some other things.
     *
     * @param   arg   basic argument
     * @return  doctored argument
     */
    static Object doctorArgs( Object arg ) {
        if ( arg == null ) {
            return new Vector();
        }
        else if ( arg instanceof URL ) {
            return arg.toString();
        }
        else if ( arg instanceof URI ) {
            return arg.toString();
        }
        else if ( arg instanceof Collection ) {
            Vector vec = new Vector();
            for ( Iterator it = ((Collection) arg).iterator(); it.hasNext(); ) {
                vec.add( doctorArgs( it.next() ) );
            }
            return vec;
        }
        else if ( arg instanceof Object[] ) {
            Vector vec = new Vector();
            Object[] array = (Object[]) arg;
            for ( int i = 0; i < array.length; i++ ) {
                vec.add( doctorArgs( array[ i ] ) );
            }
            return vec;
        }
        else {
            return arg;
        }
    }
}
