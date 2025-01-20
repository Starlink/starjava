package uk.ac.starlink.ttools.cone;

import cds.moc.SMoc;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.util.CgiQuery;

/**
 * MOC coverage implementation which reads a MOC from a given URL.
 * MOCs are cached by URL, so the same one won't be read twice.
 *
 * <p><strong>Note:</strong> MOCs are cached indefinitely per service,
 * beware that this constitutes a potential memory leak.
 * Some smarter caching scheme may be introduced if this causes problems.
 *
 * @author   Mark Taylor
 * @since    9 Jun 2014
 */
public class UrlMocCoverage extends MocCoverage {

    private final URL mocUrl_;

    /** Footprint service base URL provided by CDS. */
    public static final String FOOT_SERVICE_URL =
        "http://alasky.u-strasbg.fr/footprints";

    private static final Map<String,SMoc> mocMap_ = new HashMap<>();
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.cone" );

    /**
     * Constructor.
     *
     * @param   mocUrl  URL of MOC file
     */
    public UrlMocCoverage( URL mocUrl ) {
        mocUrl_ = mocUrl;
    }

    @Override
    protected SMoc createMoc() throws IOException {
        return getMoc( mocUrl_ );
    }

    /**
     * Returns an instance which gives coverage for a data service
     * with a given access URL.  This queries the CDS service for MOCs,
     * which at time of writing has information for all the various
     * VizieR cone search services, plus a few other registered cone
     * search services (UKIDSS etc).
     *
     * @param  serviceUrl  URL of cone search service
     * @param  nside   requiested HEALPix nside for MOC, or -1 for default
     */
    public static UrlMocCoverage getServiceMoc( URL serviceUrl, int nside ) {
        CgiQuery query = new CgiQuery( FOOT_SERVICE_URL + "/getMoc" );
        query.addArgument( "baseUrl", serviceUrl.toString() );
        if ( nside >= 0 ) {
            query.addArgument( "nside", nside );
        }
        return new UrlMocCoverage( query.toURL() );
    }

    /**
     * Returns an instance which gives coverage for a named Vizier table.
     * The name may be a vizier table name (like "V/139/sdss9") or alias
     * as used by the CDS Xmatch service (like "SDSS DR9").
     *
     * @param  vizierId  vizier table name or alias
     * @param  nside   requiested HEALPix nside for MOC, or -1 for default
     */
    public static UrlMocCoverage getVizierMoc( String vizierId, int nside ) {

        /* I'm not sure where this service is documented, but Thomas Boch
         * gave me the URL.  The encoding of the URL is similarly unclear;
         * spaces do need to be escaped, but using URLEncoder.encode()
         * is no good, since replacing "/" characters (common in Vizier IDs)
         * with %2F fails.  Thomas seems to think this will be OK. */
        String url = new StringBuffer()
            .append( FOOT_SERVICE_URL )
            .append( "/cats/vizier/" )
            .append( vizierId.replaceAll( " ", "%20" ) )
            .toString();
        CgiQuery query = new CgiQuery( url );
        query.addArgument( "product", "MOC" );
        if ( nside >= 0 ) {
            query.addArgument( "nside", nside );
        }
        return new UrlMocCoverage( query.toURL() );
    }

    /**
     * Returns a MOC for a given cone search service URL.
     * If not cached, it is cached and then returned.
     * If no MOC can be obtained, null is returned.
     *
     * @param   URL of MOC
     * @return   MOC object, or null
     */
    private static synchronized SMoc getMoc( URL mocUrl ) throws IOException {
        String urlKey = mocUrl.toString();
        if ( ! mocMap_.containsKey( urlKey ) ) {
            SMoc moc = null;
            try {
                moc = readMoc( mocUrl );
            }
            finally {
                mocMap_.put( urlKey, moc );
            }
        }
        return mocMap_.get( urlKey );
    }

    /**
     * Reads a MOC from a given URL.  If it's not there, null is returned.
     *
     * @param  mocUrl  MOC url
     * @return  MOC object, or null
     * @throws  IOException if some unexpected error occurred
     */
    private static SMoc readMoc( URL mocUrl ) throws IOException {
        logger_.info( "Attempt to acquire MOC from " + mocUrl );
        URLConnection conn = mocUrl.openConnection();
        conn.connect();
        if ( conn instanceof HttpURLConnection &&
             ((HttpURLConnection) conn).getResponseCode() == 404 ) {
            logger_.info( "No footprint information available" );
            return null;
        }
        InputStream in = new BufferedInputStream( conn.getInputStream() );
        try {
            SMoc moc = new SMoc( in );
            if ( logger_.isLoggable( Level.INFO ) ) {
                logger_.info( "Got MOC footprint: " + summariseMoc( moc ) );
            }
            return moc;
        }
        catch ( IOException e ) {
            throw e;
        }
        catch ( Exception e ) {
            throw (IOException) new IOException( "MOC read error" )
                               .initCause( e );
        }
        finally {
            try {
                in.close();
            }
            catch ( IOException e ) {
            }
        }
    }
}
