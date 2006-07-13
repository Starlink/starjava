package uk.ac.starlink.plastic;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.util.Collections;
import java.util.Vector;
import junit.framework.TestCase;
import org.apache.xmlrpc.WebServer;
import org.apache.xmlrpc.XmlRpcClient;
import org.apache.xmlrpc.XmlRpcException;

public class ClientTest extends TestCase {

    private static final String PREFIX = "prefix";

    public ClientTest( String name ) {
        super( name );
    }

    public void testClients() throws Exception {
        URL surl = startServer();
        exerciseClient( new XmlRpcClient( surl ) );
        exerciseClient( new XmlRpcClient( surl,
                                        new CustomTransportFactory( surl ) ) );
        exerciseClient( PlasticUtils.createXmlRpcClient( surl ) );
    }

    private void exerciseClient( XmlRpcClient client ) throws Exception {
        assertEquals(
            "message 1",
            client.execute( PREFIX + ".echo", argvec( "message 1" ) ) );

        /* Apache xmlrpc-2.0 deals with faults by returning an XmlRpcException
         * from the client.execute() method.  This is not the only, or
         * one argue the most sensible thing for it to do, so if 
         * this package decides to use a different implementation which
         * behaves differently in the future, just change this test to
         * match the new behaviour.  The important thing is to ensure that
         * the text of the exception thrown at the other end 
         * (content of the XML-RPC fault) makes it to the client side in
         * some more-or-(hopefully)-less mangled form, since this is
         * often crucial for debugging. */
        String errMsg = "Syd Barrett was here";
        Object errObj = client.execute( PREFIX + ".fault", argvec( errMsg ) );
        Throwable err = (Throwable) errObj;
        assertEquals( XmlRpcException.class, err.getClass() );
        assertNull( err.getCause() );
        assertTrue( err.getMessage().indexOf( errMsg ) >= 0 );
    }

    public URL startServer() throws Exception {
        int xrPort = PlasticUtils.getUnusedPort( 3009 );
        final WebServer xrServer = new WebServer( xrPort );
        xrServer.start();
        xrServer.addHandler( PREFIX, new Handler() );
        Runtime.getRuntime().addShutdownHook( new Thread() {
            public void run() {
                xrServer.shutdown();
            }
        } );
        return new URL( "http://"
                      + InetAddress.getLocalHost().getHostName()
                      + ":" + xrPort + "/" );
    }

    private static Vector argvec( Object obj ) {
        return new Vector( Collections.singleton( obj ) );
    }

    public static class Handler {
        public String echo( String txt ) {
            return txt;
        }
        public String fault( String txt ) throws IOException {
            throw new IOException( txt );
        }
    }
}
