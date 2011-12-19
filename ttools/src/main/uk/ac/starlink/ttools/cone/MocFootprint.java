package uk.ac.starlink.ttools.cone;

import cds.moc.HealpixImpl;
import cds.moc.HealpixMoc;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.util.CgiQuery;

/**
 * Footprint based on HEALPix Multi-Order Coverage map, as developed at CDS.
 * The map data is obtained by querying the MOC service operated by CDS,
 * which can take a Cone Search URL as an argument to identify the
 * target service.
 * The lon and lat are ICRS RA and Declination respectively for the
 * footprints returned by this object.
 *
 * <p><strong>Note:</strong> MOCs are cached indefinitely per service,
 * beware that this constitutes a potential memory leak.
 * Some smarter caching scheme may be introduced if this causes problems.
 *
 * @author   Mark Taylor
 * @since    16 Dec 2011
 */
public class MocFootprint implements Footprint {

    private final URL serviceUrl_;
    private volatile boolean isInitialised_;
    private volatile HealpixMoc moc_;

    private static final Map<URL,HealpixMoc> mocMap_ =
        new HashMap<URL,HealpixMoc>();
    
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.cone" );

    private static int nside_ = 512;
    private static HealpixImpl hpi_ = PixtoolsHealpix.getInstance();
    private static final int MOC_DATA_FORMAT = HealpixMoc.FITS;
    public static final String MOC_SERVICE_URL =
        "http://alasky.u-strasbg.fr/footprints/getMoc?";

    /**
     * Constructor.
     *
     * @param  serviceUrl  cone search service URL as recognised by CDS MOC
     *                     service
     */
    public MocFootprint( URL serviceUrl ) {
        serviceUrl_ = serviceUrl;
    }

    public synchronized void initFootprint() throws IOException {
        if ( ! isInitialised_ ) {
            assert moc_ == null;
            try {
                moc_ = getMoc( serviceUrl_ );
            }
            finally {
                isInitialised_ = true;
            }
        }
        assert isFootprintReady();
    }

    public boolean isFootprintReady() {
        return isInitialised_;
    }

    public boolean discOverlaps( double alphaDeg, double deltaDeg,
                                 double radiusDeg ) {
        checkInitialised();
        if ( moc_ == null ) {
            return true;
        }
        HealpixMoc overlapMoc;
        try {
            overlapMoc = moc_.queryDisc( hpi_, alphaDeg, deltaDeg, radiusDeg );
        }
        catch ( Exception e ) {
            logger_.log( Level.WARNING, "Unexpected MOC error - fail safe", e );
            return true;
        }
        return overlapMoc.getSize() > 0;
    }

    /**
     * Returns the MOC object associated with this footprint.
     *
     * @return  moc
     */
    public HealpixMoc getMoc() {
        return moc_;
    }

    /**
     * Checks that this object is initialised, and throws an exception if not.
     */
    private void checkInitialised() {
        if ( ! isInitialised_ ) {
            throw new IllegalStateException( "Not initialised" );
        }
    }

    /**
     * Returns the HEALPix Nside value used when MOCs are requested.
     *
     * @return   nside (a power of 2)
     */
    public static int getNside() {
        return nside_;
    }

    /**
     * Sets the HEALPix Nside value used when MOCs are requested.
     *
     * @param  nside  nside (a power of 2)
     */
    public static void setNside( int nside ) {
        nside_ = nside;
    }

    /**
     * Returns the HEALPix implementation used for MOCs.
     *
     * @return   indexing implementation
     */
    public static HealpixImpl getHealpix() {
        return hpi_;
    }

    /**
     * Sets the HEALPix implementation used for MOCs.
     *
     * @param  hpi  indexing implementation
     */
    public static void setHealpixImpl( HealpixImpl hpi ) {
        hpi_ = hpi;
    }

    /**
     * Returns a MOC for a given cone search service URL.
     * If not cached, it is cached and then returned.
     * If no MOC can be obtained, null is returned.
     *
     * @param  serviceUrl  cone search service URL
     * @return   MOC object, or null
     */
    private static synchronized HealpixMoc getMoc( URL serviceUrl )
            throws IOException {
        if ( ! mocMap_.containsKey( serviceUrl ) ) {
            HealpixMoc moc = null;
            try {
                moc = readMoc( serviceUrl );
            }
            finally {
                mocMap_.put( serviceUrl, moc );
            }
        }
        return mocMap_.get( serviceUrl );
    }

    /**
     * Interrogates the MOC service for a MOC relating to a given cone
     * search URL.  If the MOC service does not know about the given URL,
     * null is returned.
     *
     * @param   serviceUrl   cone search service URL
     * @return  MOC object, or null
     * @throws  IOException if some unexpected error occurred
     */
    private static HealpixMoc readMoc( URL serviceUrl ) throws IOException {
        CgiQuery query = new CgiQuery( MOC_SERVICE_URL );
        query.addArgument( "baseUrl", serviceUrl.toString() );
        if ( nside_ >= 0 ) {
            query.addArgument( "nside", nside_ );
        }
        URL qUrl = query.toURL();
        logger_.info( "Reading footprint information from " + qUrl );
        URLConnection conn = qUrl.openConnection();
        conn.connect();
        if ( conn instanceof HttpURLConnection &&
             ((HttpURLConnection) conn).getResponseCode() == 404 ) {
            logger_.info( "No footprint information available" );
            return null;
        }
        InputStream in = new BufferedInputStream( conn.getInputStream() );
        try {
            HealpixMoc moc = new HealpixMoc( in, MOC_DATA_FORMAT );
            if ( logger_.isLoggable( Level.INFO ) ) {
                logger_.info( "Got MOC footprint: " + summariseMoc( moc ) );
            }
            return moc;
        }
        catch ( IOException e ) {
            throw e;
        }
        catch ( Exception e ) {
            throw (IOException) new IOException( "Footprint error" )
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

    /**
     * Utility method to stringify a MOC.
     *
     * @param  moc  MOC
     * @return  string
     */
    private static String summariseMoc( HealpixMoc moc ) {
        return new StringBuffer()
           .append( "Coverage: " )
           .append( moc.getCoverage() )
           .append( ", " )
           .append( "Pixels: " )
           .append( moc.getSize() )
           .append( ", " )
           .append( "Bytes: " )
           .append( moc.getMem() )
           .toString();
    }

    public static void main( String[] args ) throws IOException {
        MocFootprint fp = new MocFootprint( new URL( args[ 0 ] ) );
        fp.initFootprint();
        System.out.println( summariseMoc( fp.getMoc() ) );
    }
}
