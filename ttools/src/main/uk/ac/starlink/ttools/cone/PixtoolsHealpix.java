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
        pixTools_ = PixTools.getInstance();
    }

    public long ang2pix( int order, double lonDeg, double latDeg ) {
        long nside = orderToNside( order );
        double thetaRad = Math.toRadians( 90 - latDeg );
        double phiRad = Math.toRadians( lonDeg );
        return pixTools_.ang2pix_nest( nside, thetaRad, phiRad );
    }

    public double[] pix2ang( int order, long ipix ) {
        long nside = orderToNside( order );
        double[] coords = pixTools_.pix2ang_nest( nside, ipix );
        double thetaRad = coords[ 0 ];
        double phiRad = coords[ 1 ];
        double latDeg = 90 - Math.toDegrees( thetaRad );
        double lonDeg = Math.toDegrees( phiRad );
        coords[ 0 ] = lonDeg;
        coords[ 1 ] = latDeg;
        return coords;
    }

    public long[] queryDisc( int order, double lonDeg, double latDeg,
                             double radiusDeg ) {
        long nside = orderToNside( order );
        double thetaRad = Math.toRadians( 90 - latDeg );
        double phiRad = Math.toRadians( lonDeg );
        double radiusRad = Math.toRadians( radiusDeg );
        Vector3d vec = pixTools_.Ang2Vec( thetaRad, phiRad );
        List pixList = pixTools_.query_disc( nside, vec, radiusRad, 1, 1 );
        long[] pixs = new long[ pixList.size() ];
        int ip = 0;
        for ( Iterator it = pixList.iterator(); it.hasNext(); ) {
            pixs[ ip++ ] = ((Number) it.next()).longValue();
        }
        assert ip == pixs.length;
        return pixs;
    }

    /**
     * Returns an Nside value corresponding to a given angular size.
     *
     * @param  sizeDeg  size in degrees
     * @return  nside
     */
    public int sizeToNside( double sizeDeg ) {
        return (int) pixTools_.GetNSide( sizeDeg * 3600. );
    }

    /**
     * Turns the MOC "order" parameter into the HEALPix Nside parameter.
     *
     * @param  order
     * @return   nside
     */
    public static long orderToNside( int order ) {
        return 1L << order;
    }

    /**
     * Turns the HEALPix Nside parameter into the MOC "order" parameter.
     *
     * @param  nside  Nside
     * @return  order
     * @throws  IllegalArgumentException  if nside is not suitable
     */
    public static int nsideToOrder( long nside ) {
        double order = Math.log( nside ) / Math.log( 2 );
        int iorder = (int) order;
        if ( iorder == order ) {
            return iorder;
        }
        else {
            throw new IllegalArgumentException( "nside " + nside
                                              + " not a power of 2" );
        }
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
