package uk.ac.starlink.ttools.cone;

import cds.moc.HealpixImpl;
import gov.fnal.eag.healpix.PixTools;
import java.util.Iterator;
import java.util.List;
import javax.vecmath.Vector3d;

/**
 * Healpix implementation for use with MOC library based on Kuropatkin's
 * PixTools library.
 * Use {@link #getInstance} to obtain the singleton instance of this class.
 *
 * <p><strong>Implementation Note:</strong>
 * In the current implementation all methods are synchronized because
 * use of the PixTools <em>class</em> (not just instances of it)
 * is not threadsafe.
 * This implementation may be changed if the underlying bug is fixed.
 * The singleton instance of this class is in any case threadsafe.
 *
 * @author   Mark Taylor
 * @since    16 Dec 2011
 * @see      <a href="http://home.fnal.gov/~kuropat/HEALPIX/PixTools.html"
 *                  >PixTools</a>
 */
public class PixtoolsHealpix implements HealpixImpl {

    private final PixTools pixTools_;
    private static final PixtoolsHealpix instance_ = new PixtoolsHealpix();

    /**
     * Private constructor prevents public instantiation of this singleton.
     */
    private PixtoolsHealpix() {
        pixTools_ = new PixTools();
    }

    public synchronized long ang2pix( int order, double lonDeg,
                                      double latDeg ) {
        long nside = orderToNside( order );
        double thetaRad = Math.toRadians( 90 - latDeg );
        double phiRad = Math.toRadians( lonDeg );
        return getPixtools().ang2pix_nest( nside, thetaRad, phiRad );
    }

    public synchronized double[] pix2ang( int order, long ipix ) {
        long nside = orderToNside( order );
        double[] coords = getPixtools().pix2ang_nest( nside, ipix );
        double thetaRad = coords[ 0 ];
        double phiRad = coords[ 1 ];
        double latDeg = 90 - Math.toDegrees( thetaRad );
        double lonDeg = Math.toDegrees( phiRad );
        coords[ 0 ] = lonDeg;
        coords[ 1 ] = latDeg;
        return coords;
    }

    public synchronized long[] queryDisc( int order, double lonDeg,
                                          double latDeg, double radiusDeg ) {
        long nside = orderToNside( order );
        double thetaRad = Math.toRadians( 90 - latDeg );
        double phiRad = Math.toRadians( lonDeg );
        double radiusRad = Math.toRadians( radiusDeg );
        PixTools pixTools = getPixtools();
        Vector3d vec = pixTools.Ang2Vec( thetaRad, phiRad );
        List pixList = pixTools.query_disc( nside, vec, radiusRad, 1, 1 );
        long[] pixs = new long[ pixList.size() ];
        int ip = 0;
        for ( Iterator it = pixList.iterator(); it.hasNext(); ) {
            pixs[ ip++ ] = ((Number) it.next()).longValue();
        }
        assert ip == pixs.length;
        return pixs;
    }

    /**
     * Returns a PixTools instance which can be used for calculations.
     * At present a single implementation is always used, on the grounds
     * that its uses are always synchronized.  If the PixTools library 
     * is fixed so that different instances may be used simulataneously,
     * the implementation of this method should be changed, perhaps to
     * use a {@link java.lang.ThreadLocal}.
     *
     * @return   PixTools instance
     */
    private PixTools getPixtools() {
        return pixTools_;
    }

    /**
     * Turns the MOC "order" parameter into the HEALPix Nside parameter.
     *
     * @param  order
     * @return   nside
     */
    private static long orderToNside( int order ) {
        return 1L << order;
    }

    /**
     * Returns the sole instance of this singleton class.
     *
     * @return  instance
     */
    public static PixtoolsHealpix getInstance() {
        return instance_;
    }
}
