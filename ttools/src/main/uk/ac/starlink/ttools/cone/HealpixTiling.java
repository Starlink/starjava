package uk.ac.starlink.ttools.cone;

import gov.fnal.eag.healpix.PixTools;
import java.util.Iterator;
import java.util.List;
import javax.vecmath.Vector3d;

/**
 * Tiling implementation based on the HEALPix scheme.
 *
 * @author   Mark Taylor
 * @since    12 Dec 2007
 */
public class HealpixTiling implements SkyTiling {

    private final long nside_;
    private final boolean nest_;
    private final PixTools pixTools_;

    /**
     * Constructor.
     *
     * @param  k  log to base 2 of HEALPix Nside parameter
     * @param  nest  true for nesting scheme, false for ring scheme
     */
    public HealpixTiling( int k, boolean nest ) {
        if ( k > 63 ) {
            throw new IllegalArgumentException( "k " + k + " too large" );
        }
        nside_ = 1L << k;
        nest_ = nest;
        pixTools_ = new PixTools();
    }

    public long getPositionTile( double ra, double dec ) {
        Vector3d vec = toVector( ra, dec );
        return nest_ ? pixTools_.vect2pix_nest( nside_, vec )
                     : pixTools_.vect2pix_ring( nside_, vec );
    }

    public long[] getTileRange( double ra, double dec, double radius ) {
        double radiusRad = Math.toRadians( radius );

        /* Calculate tile indices in the ring scheme, then convert if
         * necessary to the nest scheme.  The query_disc method allows
         * to calculate directly in the nest scheme, but early versions
         * of PixTools gave the wrong answer for this and later versions
         * are much slower. */
        List tileList = pixTools_.query_disc( nside_, toVector( ra, dec ),
                                              radiusRad, 0, 1 );
        if ( nest_ ) {
            int ntile = tileList.size();
            for ( int itile = 0; itile < ntile; itile++ ) {
                long ringIndex = ((Number) tileList.get( itile )).longValue();
                long nestIndex = pixTools_.ring2nest( nside_, ringIndex );
                tileList.set( itile, new Long( nestIndex ) );
            }
        }

        /* query_disc in inclusive mode returns a list which may include
         * more tiles than are actually overlapped by the given cone.
         * This is especially true if radius is much smaller than the
         * tile size.  The PixTools algorithm is basically to find all the
         * tiles whose centres lie within (radius+tileSize/2) - safe but
         * not efficient. */
        long lo = Long.MAX_VALUE;
        long hi = Long.MIN_VALUE;
        for ( Iterator it = tileList.iterator(); it.hasNext(); ) {
            long tile = ((Number) it.next()).longValue();
            if ( discIntersectsTile( ra, dec, radiusRad, tile ) ) {
                lo = Math.min( lo, tile );
                hi = Math.max( hi, tile );
            }
        }
        return lo <= hi ? new long[] { lo, hi }
                        : null;
    }

    /**
     * Turns an ra, dec position into a Vector3d.
     *
     * @param  ra   right ascension in degrees
     * @param  dec  declination in degrees
     * @return  vector
     */
    private Vector3d toVector( double ra, double dec ) {
        double theta = Math.PI * 0.5 - Math.toRadians( dec );
        return pixTools_.Ang2Vec( theta, Math.toRadians( ra ) );
    }

    /**
     * Returns true if the given disc is fully or partially contained in a
     * given tile.  False positives are permitted, but not false negatives.
     * The supplied cone can be assumed to be "close to" the supplied tile.
     *
     * @param   ra   right ascension of disc centre
     * @param   dec  declination of disc centre
     * @param   radius   disc radius in radians
     * @param   tile  tile index
     * @return   true if the disc may be fully or partially within tile;
     *           false if it is definitely outside
     */
    private boolean discIntersectsTile( double ra, double dec, double radiusRad,
                                        long tile ) {

        /* Correct but not optimal return - all cones may be inside tile. */
        return true;
    }
}
