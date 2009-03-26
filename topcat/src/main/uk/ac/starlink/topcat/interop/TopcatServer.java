package uk.ac.starlink.topcat.interop;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.astrogrid.samp.client.ClientProfile;
import org.astrogrid.samp.httpd.URLMapperHandler;
import org.astrogrid.samp.httpd.HttpServer;
import org.astrogrid.samp.httpd.ResourceHandler;
import org.astrogrid.samp.httpd.ServerResource;
import org.astrogrid.samp.xmlrpc.SampXmlRpcClientFactory;
import org.astrogrid.samp.xmlrpc.SampXmlRpcServer;
import org.astrogrid.samp.xmlrpc.SampXmlRpcServerFactory;
import org.astrogrid.samp.xmlrpc.StandardClientProfile;
import org.astrogrid.samp.xmlrpc.internal.InternalClientFactory;
import org.astrogrid.samp.xmlrpc.internal.InternalServer;
import uk.ac.starlink.topcat.Driver;

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
    private final URL tcPkgUrl_;
    private boolean started_;
    private static TopcatServer instance_;
    private static final int BUFSIZ = 16 * 1024;

    /**
     * Private constructor constructs sole instance.
     */
    private TopcatServer() throws IOException {
        httpServer_ = new HttpServer();
        httpServer_.setDaemon( true );

        /* Set up handler for custom resource serving. */
        resourceHandler_ = new ResourceHandler( httpServer_, "/dynamic" );
        httpServer_.addHandler( resourceHandler_ );

        /* Set up handler to serve TOPCAT documentation. */
        URL docResource = getDocResource();
        URLMapperHandler docHandler =
            new URLMapperHandler( httpServer_, "/doc", docResource, true );
        httpServer_.addHandler( docHandler );
        tcPkgUrl_ = docHandler.getBaseUrl();

        /* Set up handler for XML-RPC. */
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
    public void removeResource( URL url ) {
        resourceHandler_.removeResource( url );
    }

    /**
     * Returns the URL corresponding to the classpath for the package
     * uk.ac.starlink.topcat.
     *
     * @return   documentation URL
     */
    public URL getTopcatPackageUrl() {
        return tcPkgUrl_;
    }

    /**
     * Indicates whether this server can serve the resource with a given URL.
     *
     * @param   url  URL to enquire about
     * @return   true if a request for <code>url</code> will complete with
     *           non-error status
     */
    public boolean isFound( URL url ) {
        checkStarted();
        try {
            URLConnection connection = url.openConnection();
            if ( connection instanceof HttpURLConnection ) {
                HttpURLConnection hconn = (HttpURLConnection) connection;
                hconn.setRequestMethod( "HEAD" );
                hconn.setDoOutput( false );
                hconn.connect();
                InputStream in = connection.getInputStream();
                byte[] buf = new byte[ BUFSIZ ];
                while ( in.read( buf ) >= 0 ) {
                }
                in.close();
                return hconn.getResponseCode() == 200;
            }
            else {
                connection.connect();
                InputStream in = connection.getInputStream();
                byte[] buf = new byte[ BUFSIZ ];
                while( in.read( buf ) >= 0 ) {
                }
                in.close();
                return true;
            }
        }
        catch ( IOException e ) {
            return false;
        }
    }

    /**
     * Indicates whether this server is currently serving requests.
     *
     * @return  true iff server is running
     */
    public boolean isRunning() {
        return httpServer_.isRunning();
    }

    /**
     * Executes a runnable object after first ensuring that the server
     * is running.  If the server is not currently running, it is started
     * and the runnable invoked in a separate thread.  Otherwise it's
     * done concurrently.
     * Thus this method should not take (much) longer than the given 
     * runnable's run method to return.
     *
     * @param   runnable   item to run
     */
    public void invokeWhenStarted( final Runnable runnable ) {
        if ( isRunning() ) {
            runnable.run();
        }
        else {
            Thread waiter = new Thread( "server waiter" ) {
                public void run() {
                    checkStarted();
                    runnable.run();
                }
            };
            waiter.setDaemon( true );
            waiter.start();
        }
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
     * Returns the URL corresponding to the classpath directory 
     * "uk.ac.starlink.topcat".
     *
     * @return  internal URL
     */
    private static URL getDocResource() throws MalformedURLException {

        /* Start with a class which is in the classpath package we need. */
        Class clazz = Driver.class;

        /* Get the relative resource name of this class. */
        String cname = clazz.getName().replaceFirst( ".*\\.", "" ) + ".class";

        /* Turn it into a URL. */
        URL cUrl = clazz.getResource( cname );

        /* Then strip the name of the class file itself, leaving just the
         * directory. */
        String cRes = cUrl.toString();
        cRes = cRes.substring( 0, cRes.length() - cname.length() );

        /* Return the result as a URL. */
        return new URL( cRes );
    }

    /**
     * Returns the sole instance of this class.
     *
     * @return   instance
     */
    public static TopcatServer getInstance() throws IOException {
        if ( instance_ == null ) {
            synchronized ( TopcatServer.class ) {
                if ( instance_ == null ) {
                    instance_ = new TopcatServer();
                }
            }
        }
        return instance_;
    }
}
