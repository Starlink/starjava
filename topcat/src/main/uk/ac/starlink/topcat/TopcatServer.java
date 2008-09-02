package uk.ac.starlink.topcat;

import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.astrogrid.samp.client.ClientProfile;
import org.astrogrid.samp.xmlrpc.SampXmlRpcClient;
import org.astrogrid.samp.xmlrpc.SampXmlRpcServer;
import org.astrogrid.samp.xmlrpc.SampXmlRpcServerFactory;
import org.astrogrid.samp.xmlrpc.StandardClientProfile;
import org.astrogrid.samp.xmlrpc.internal.HttpServer;
import org.astrogrid.samp.xmlrpc.internal.InternalClient;
import org.astrogrid.samp.xmlrpc.internal.InternalServer;

/**
 * Provides HTTP server functionality for TOPCAT.
 * This includes a web server for dynamically generated content and an
 * XML-RPC server for use with SAMP.
 * The HTTP server itself is started lazily.
 * This class is a singleton.
 *
 * @author   Mark Taylor
 * @since    29 Aug 2008
 */
public class TopcatServer {

    private final HttpServer httpServer_;
    private final ResourceHandler resourceHandler_;
    private final SampXmlRpcServerFactory xServerFactory_;
    private final SampXmlRpcClient xClient_;
    private final ClientProfile profile_;
    private boolean started_;
    private static TopcatServer instance_;

    /**
     * Private constructor constructs sole instance.
     */
    private TopcatServer() throws IOException {
        httpServer_ = new HttpServer();
        httpServer_.setDaemon( true );
        resourceHandler_ = new ResourceHandler( httpServer_, "/dynamic" );
        httpServer_.addHandler( resourceHandler_ );

        xClient_ = new InternalClient();
        final SampXmlRpcServer xServer =
            new InternalServer( httpServer_, "/xmlrpc" );
        xServerFactory_ = new SampXmlRpcServerFactory() {
            public SampXmlRpcServer getServer() {
                checkStarted();
                return xServer;
            }
        };
        profile_ = new StandardClientProfile( xClient_, xServerFactory_ );
    }

    /**
     * Returns a SAMP client profile.
     *
     * @return   profile
     */
    public ClientProfile getProfile() {
        return profile_;
    }

    /**
     * Returns a SAMP XML-RPC client implementation.
     *
     * @return  SAMP XML-RPC client
     */
    public SampXmlRpcClient getSampClient() {
        return xClient_;
    }

    /**
     * Returns a SAMP XML-RPC server factory implementation.
     *
     * @return SAMP XML-RPC server factory
     */
    public SampXmlRpcServerFactory getSampServerFactory() {
        return xServerFactory_;
    }

    /**
     * Makes a resource available for retrieving from this internal HTTP server.
     * A <code>name</code> may be supplied which will appear at the end of
     * the URL, but this is just for cosmetic purposes.  The URL at which 
     * the resource is available will provided as the return value.
     *
     * @param   name   filename identifying the resource
     * @param   resource   resource to make available
     * @return    URL at which <code>resource</code> can be found
     */
    public URL addResource( String name, Resource resource ) {
        checkStarted();
        return resourceHandler_.addResource( name == null ? "" : name,
                                             resource );
    }

    /**
     * Ensures that the server has been started since it was created.
     * May harmlessly be called multiple times.
     */
    private void checkStarted() {
        if ( ! started_ ) {
            synchronized ( httpServer_ ) {
                if ( ! started_ ) {
                    started_ = true;
                    httpServer_.start();
                }
            }
        }
    }

    /**
     * Returns the sole instance of this class.
     *
     * @return   instance
     */
    public static TopcatServer getInstance() throws IOException {
        if ( instance_ == null ) {
            instance_ = new TopcatServer();
        }
        return instance_;
    }

    /**
     * HttpServer.Handler implementation which implements dynamic resource
     * provision.
     */
    private static class ResourceHandler implements HttpServer.Handler {
        private final String basePath_;
        private final URL serverUrl_;
        private final Map resourceMap_;
        private int iRes_;

        /**
         * Constructor.
         *
         * @param   server   HTTP server
         * @param   basePath   from server root beneath which all resources
         *                     provided by this handler will appear
         */
        public ResourceHandler( HttpServer server, String basePath ) {
            if ( ! basePath.startsWith( "/" ) ) {
                basePath = "/" + basePath;
            }
            if ( ! basePath.endsWith( "/" ) ) {
                basePath = basePath + "/";
            }
            basePath_ = basePath;
            serverUrl_ = server.getBaseUrl();
            resourceMap_ = new HashMap();
        }

        /**
         * Adds a resource to this server.
         *
         * @param   name   resource name, for cosmetic purposes only
         * @param   resource  resource to make available
         * @return   URL at which resource can be found
         */
        public synchronized URL addResource( String name, Resource resource ) {
            String path = basePath_ + Integer.toString( ++iRes_ ) + "/";
            if ( name != null ) {
                path += name;
            }
            resourceMap_.put( path, resource );
            try {
                return new URL( serverUrl_, path );
            }
            catch ( MalformedURLException e ) {
                throw new AssertionError( "Unknown protocol http??" );
            }
        }

        public HttpServer.Response serveRequest( HttpServer.Request request ) {
            String path = request.getUrl();
            if ( ! path.startsWith( basePath_ ) ) {
                return null;
            }
            else if ( resourceMap_.containsKey( path ) ) {
                final Resource resource = (Resource) resourceMap_.get( path );
                Map hdrMap = new HashMap();
                hdrMap.put( "Content-Type", resource.getContentType() );
                long contentLength = resource.getContentLength();
                if ( contentLength >= 0 ) {
                    hdrMap.put( "Content-Length",
                                Long.toString( contentLength ) );
                }
                String method = request.getMethod();
                if ( method.equals( "HEAD" ) ) {
                    return new HttpServer.Response( 200, "OK", hdrMap ) {
                        protected void writeBody( OutputStream out ) {
                        }
                    };
                }
                else if ( method.equals( "GET" ) ) {
                    return new HttpServer.Response( 200, "OK", hdrMap ) {
                        public void writeBody( OutputStream out )
                                throws IOException {
                            resource.writeBody( out );
                        }
                    };
                }
                else {
                    return HttpServer
                          .createErrorResponse( 405, "Unsupported method" );
                }
            }
            else {
                return HttpServer.createErrorResponse( 404, "Not found" );
            }
        }
    }

    /**
     * Defines a resource served by this server.
     */
    public interface Resource {

        /**
         * Returns the MIME type of this resource.
         *
         * @return   value of Content-Type HTTP header
         */
        String getContentType();

        /**
         * Returns the number of bytes in this resource, if known.
         *
         * @return   value of Content-Length HTTP header if known;
         *           otherwise a negative number
         */
        long getContentLength();

        /**
         * Writes resource body.
         *
         * @param  out  destination stream
         */
        void writeBody( OutputStream out ) throws IOException;
    }
}
