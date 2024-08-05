package uk.ac.starlink.topcat.interop;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import org.astrogrid.samp.client.ClientProfile;
import org.astrogrid.samp.client.DefaultClientProfile;
import org.astrogrid.samp.httpd.HttpServer;
import org.astrogrid.samp.httpd.ResourceHandler;
import org.astrogrid.samp.httpd.ServerResource;
import org.astrogrid.samp.httpd.URLMapperHandler;
import org.astrogrid.samp.httpd.UtilServer;
import uk.ac.starlink.topcat.Driver;
import uk.ac.starlink.util.URLUtils;

/**
 * Provides HTTP server functionality for TOPCAT.
 * This includes a web server for dynamically generated content and an
 * XML-RPC server for use with SAMP.
 * This class is a singleton.
 *
 * @author   Mark Taylor
 * @since    29 Aug 2008
 */
public class TopcatServer {

    private final UtilServer utilServer_;
    private final ClientProfile profile_;
    private final ResourceHandler resourceHandler_;
    private final URL tcPkgUrl_;
    private static TopcatServer instance_;

    private static final int BUFSIZ = 16 * 1024;

    /**
     * Private constructor constructs sole instance.
     */
    private TopcatServer() throws IOException {
        utilServer_ = UtilServer.getInstance();
        profile_ = DefaultClientProfile.getProfile();
        HttpServer httpServer = utilServer_.getServer();

        /* Set up a handler for custom resource handling. */
        resourceHandler_ =
            new ResourceHandler( httpServer,
                                 utilServer_.getBasePath( "/dynamic" ) );
        httpServer.addHandler( resourceHandler_ );

        /* Set up a handler to serve TOPCAT documentation. */
        URLMapperHandler docHandler =
            new URLMapperHandler( httpServer,
                                  utilServer_.getBasePath( "/doc" ),
                                  getDocResource(), true );
        httpServer.addHandler( docHandler );
        tcPkgUrl_ = docHandler.getBaseUrl();
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
     * Returns the URL for a resource available at a path served
     * by this server.
     *
     * @param  subPath   path relative to this server's base URL
     * @return  URL, or null in case of trouble
     */
    public URL getRelativeUrl( String subPath ) {
        try {
            return tcPkgUrl_.toURI().resolve( subPath ).toURL();
        }
        catch ( MalformedURLException | URISyntaxException
                                      | IllegalArgumentException e ) {
            return null;
        }
    }

    /**
     * Indicates whether this server can serve the resource with a given URL.
     *
     * @param   url  URL to enquire about
     * @return   true if a request for <code>url</code> will complete with
     *           non-error status
     */
    public boolean isFound( URL url ) {
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
     * Returns the URL corresponding to the classpath directory
     * "uk.ac.starlink.topcat".
     *
     * @return  internal URL
     */
    private static URL getDocResource() throws MalformedURLException {

        /* Start with a class which is in the classpath package we need. */
        Class<Driver> clazz = Driver.class;

        /* Get the relative resource name of this class. */
        String cname = clazz.getName().replaceFirst( ".*\\.", "" ) + ".class";

        /* Turn it into a URL. */
        URL cUrl = clazz.getResource( cname );

        /* Then strip the name of the class file itself, leaving just the
         * directory. */
        String cRes = cUrl.toString();
        cRes = cRes.substring( 0, cRes.length() - cname.length() );

        /* Return the result as a URL. */
        return URLUtils.newURL( cRes );
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
