package uk.ac.starlink.topcat.interop;

import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.astrogrid.samp.client.ClientProfile;
import org.astrogrid.samp.xmlrpc.SampXmlRpcClientFactory;
import org.astrogrid.samp.xmlrpc.SampXmlRpcServer;
import org.astrogrid.samp.xmlrpc.SampXmlRpcServerFactory;
import org.astrogrid.samp.xmlrpc.StandardClientProfile;
import org.astrogrid.samp.xmlrpc.internal.HttpServer;
import org.astrogrid.samp.xmlrpc.internal.InternalClientFactory;
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
    private final SampXmlRpcClientFactory xClientFactory_;
    private final ClientProfile profile_;
    private boolean started_;
    private static TopcatServer instance_;
    private static Logger logger_ =
        Logger.getLogger( TopcatServer.class.getName() );

    /**
     * Private constructor constructs sole instance.
     */
    private TopcatServer() throws IOException {
        httpServer_ = new HttpServer();
        httpServer_.setDaemon( true );
        resourceHandler_ = new ResourceHandler( httpServer_, "/dynamic" );
        httpServer_.addHandler( resourceHandler_ );

        xClientFactory_ = new InternalClientFactory();
        final SampXmlRpcServer xServer =
            new InternalServer( httpServer_, "/xmlrpc" );
        xServerFactory_ = new SampXmlRpcServerFactory() {
            public SampXmlRpcServer getServer() {
                checkStarted();
                return xServer;
            }
        };
        profile_ = new StandardClientProfile( xClientFactory_,
                                              xServerFactory_ );
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
    public SampXmlRpcClientFactory getSampClientFactory() {
        return xClientFactory_;
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
    public URL addResource( String name, ServerResource resource ) {
        checkStarted();
        return resourceHandler_.addResource( name == null ? "" : name,
                                             resource );
    }

    /**
     * Removes a resource from this server.
     *
     * @param  url  URL returned by a previous addResource call
     */
    public void expireResource( URL url ) {
        resourceHandler_.expireResource( url );
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

        /** Dummy resource indicating a withdrawn item. */
        private static final ServerResource EXPIRED = new ServerResource() {
            public String getContentType() {
                throw new AssertionError();
            }
            public long getContentLength() {
                throw new AssertionError();
            }
            public void writeBody( OutputStream out ) {
                throw new AssertionError();
            }
        };

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
        public synchronized URL addResource( String name,
                                             ServerResource resource ) {
            String path = basePath_ + Integer.toString( ++iRes_ ) + "/";
            if ( name != null ) {
                path += name;
            }
            resourceMap_.put( path, resource );
            try {
                URL url = new URL( serverUrl_, path );
                logger_.info( "Resource added: " + url );
                return new URL( serverUrl_, path );
            }
            catch ( MalformedURLException e ) {
                throw new AssertionError( "Unknown protocol http??" );
            }
        }

        /**
         * Removes a resource from this server.
         *
         * @param  url  URL returned by a previous addResource call
         */
        public synchronized void expireResource( URL url ) {
            String path = url.getPath();
            if ( resourceMap_.containsKey( path ) ) {
                logger_.info( "Resource expired: " + url );
                resourceMap_.put( path, EXPIRED );
            } 
            else {
                throw new IllegalArgumentException( "Unknown URL to expire: "
                                                  + url );
            }
        }

        public HttpServer.Response serveRequest( HttpServer.Request request ) {
            String path = request.getUrl();
            if ( ! path.startsWith( basePath_ ) ) {
                return null;
            }
            final ServerResource resource =
                (ServerResource) resourceMap_.get( path );
            if ( resource == EXPIRED ) {
                return HttpServer.createErrorResponse( 410, "Gone" );
            }
            else if ( resource != null ) {
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
}
