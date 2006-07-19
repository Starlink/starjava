package uk.ac.starlink.plastic;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import org.apache.xmlrpc.XmlRpcClientException;
import org.apache.xmlrpc.XmlRpcTransport;
import org.apache.xmlrpc.XmlRpcTransportFactory;

/**
 * Implementation of <code>XmlTransportFactory</code> that does better
 * than the xmlrpc-2.0 
 * {@link org.apache.xmlrpc.DefaultXmlRpcTransportFactory} 
 * at handling errors.
 *
 * <p>The main place this differs from the default implementation is in
 * handling of XML-RPC faults.  The XML-RPC spec specifies that when
 * a fault is returned, the HTTP response code should be 200 OK.
 * However, some XML-RPC implementations do not honour this and
 * accompany the fault report with a 500 Error code.  In this case the
 * default XmlRpcTransportFactory implementation (at least, in 
 * conjunction with Sun's J2SE1.4, I haven't tested it elsewhere) 
 * just returns the error code as the text of the resulting IOException.
 * This implementation attempts to extract content from the HTTP 
 * response even in the case of an error status code, which can reveal
 * valuable debugging information.
 * This implementation is also a bit tidier about logging errors
 * when closing not-really-open URL connection streams.
 *
 * <p>Supports <code>TRANSPORT_AUTH</code> and <code>TRANSPORT_URL</code>
 * keys but not all the facilities offered by 
 * <code>DefaultXmlRpcTransportFactory</code>.  Could easily be
 * extended to do so (inherit or crib from Apache source code).
 *
 * <p>Use this class by supplying an instance of it to the
 * {@link org.apache.xmlrpc.XmlRpcClient#XmlRpcClient(java.net.URL,
 *           org.apache.xmlrpc.XmlRpcTransportFactory)}
 * constructor.
 *
 * @author   Mark Taylor
 */
class CustomTransportFactory implements XmlRpcTransportFactory {

    private URL url_;
    private String auth_;

    /**
     * Constructs a transport factory ready to create transports for a 
     * given URL.
     *
     * @param   url   URL of created transports
     */
    public CustomTransportFactory( URL url ) {
        url_ = url;
    }

    public void setProperty( String key, Object value ) {
        if ( TRANSPORT_AUTH.equals( key ) ) {
            auth_ = (String) value;
        }
        else if ( TRANSPORT_URL.equals( key ) ) {
            url_ = (URL) value;
        }
    }

    public XmlRpcTransport createTransport() {
        return new Transport( url_, auth_ );
    }

    /**
     * Transport implementation used by this factory.
     */
    private static class Transport implements XmlRpcTransport {

        private final URL url_;
        private final String auth_;
        private InputStream connStrm_;

        /**
         * Constructs a new transport for a given URL and authorization
         * string.
         *
         * @param   url   XMLRPC server URL
         * @param   auth  authorization string
         */
        public Transport( URL url, String auth ) {
            url_ = url;
            auth_ = auth;
        }

        public InputStream sendXmlRpc( byte[] request ) throws IOException {

            /* Except as noted, this code is based on the xmlrpc-2.0
             * org.apache.xmlrpc.DefaultXmlRpcTransport source. */
            URLConnection conn = url_.openConnection();
            conn.setDoInput( true );
            conn.setDoOutput( true );
            conn.setUseCaches( false );
            conn.setAllowUserInteraction( false );
            conn.setRequestProperty( "Content-Length",
                                      Integer.toString( request.length ) );
            conn.setRequestProperty( "Content-Type", "text/xml" );
            if ( auth_ != null ) {
                conn.setRequestProperty( "Authorization", "Basic " + auth_ );
            }
            OutputStream out = conn.getOutputStream();
            out.write( request );
            out.flush();
            out.close();
            try {
                connStrm_ = conn.getInputStream();
                return connStrm_;
            }
            catch ( IOException e ) {

                /* HttpURLConnection.getInputStream will throw an exception
                 * if the HTTP response code is not OK.  However, the 
                 * response text is available by calling getErrorStream 
                 * in this case.  Here, we try to take advantage of that. */
                if ( conn instanceof HttpURLConnection ) {
                    InputStream err =
                        ((HttpURLConnection) conn).getErrorStream();
                    if ( err != null ) {
                        connStrm_ = err;
                        return err;
                    }
                }
                throw e;
            }
        }

        public void endClientRequest() throws XmlRpcClientException {

            /* Don't try to close it if it's not open. */
            if ( connStrm_ != null ) {
                try {
                    connStrm_.close();
                }
                catch ( IOException e ) {
                    throw new XmlRpcClientException(
                        "Trouble closing URLConnection: " + e.getMessage(), e );
                }
                finally {
                    connStrm_ = null;
                }
            }
        }
    }
}
