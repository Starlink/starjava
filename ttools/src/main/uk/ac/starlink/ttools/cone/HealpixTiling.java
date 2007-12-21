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
     * @param  log2nside  log to base 2 of HEALPix Nside parameter
     * @param  nest  true for nesting scheme, false for ring scheme
     */
    public HealpixTiling( int log2nside, boolean nest ) {
        if ( log2nside > 63 ) {
            throw new IllegalArgumentException( "log2nside " + log2nside
                                              + " too large" );
        }
        nside_ = 1L << log2nside;
        nest_ = nest;
        pixTools_ = new PixTools();
    }

    public long getPositionTile( double ra, double dec ) {
        Vector3d vec = toVector( ra, dec );
        return nest_ ? pixTools_.vect2pix_nest( nside_, vec )
                     : pixTools_.vect2pix_ring( nside_, vec );
    }

    public long[] getTileRange( double ra, double dec, double radius ) {
        List tileList;
        if ( nest_ ) {

            /* The following code should do the trick, but tends to generate 
             * an exception:
             *    java.lang.IllegalArgumentException: 
             *        xy2pix_nest: iy out of range [0, nside-1]
             *    at gov.fnal.eag.healpix.PixTools.xy2pix_nest(Unknown Source)
             * in the current version of PixTools. */
            // tileList = pixTools_.query_disc( nside_, toVector( ra, dec ),
            //                                  Math.toRadians( radius ), 
            //                                  1, 1 );

            /* So work around it by calculating in the ring scheme and
             * translating pixel by pixel to nest. */
            tileList = pixTools_.query_disc( nside_, toVector( ra, dec ),
                                             Math.toRadians( radius ), 
                                             0, 1 );
            int ntile = tileList.size();
            for ( int itile = 0; itile < ntile; itile++ ) {
                long ringIndex = ((Number) tileList.get( itile )).longValue();
                long nestIndex = pixTools_.ring2nest( nside_, ringIndex );
                tileList.set( itile, new Long( nestIndex ) );
            }
        }
        else {
            tileList = pixTools_.query_disc( nside_, toVector( ra, dec ),
                                             Math.toRadians( radius ),
                                             0, 1 );
        }
        long lo = Long.MAX_VALUE;
        long hi = Long.MIN_VALUE;
        for ( Iterator it = tileList.iterator(); it.hasNext(); ) {
            long tile = ((Number) it.next()).longValue();
            lo = Math.min( lo, tile );
            hi = Math.max( hi, tile );
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
}
