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
    private final double resolution_;

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
        resolution_ = pixTools_.PixRes( nside_ ) / 3600;
    }

    public long getPositionTile( double ra, double dec ) {
        Vector3d vec = toVector( ra, dec );
        return nest_ ? pixTools_.vect2pix_nest( nside_, vec )
                     : pixTools_.vect2pix_ring( nside_, vec );
    }

    public long[] getTileRange( double ra, double dec, double radius ) {

        /* If the radius is too big, don't attempt the calculation -
         * range determination might be slow. */
        if ( radius > resolution_ * 50 ) {
            return null;
        }

        /* Calculate tile indices in the ring scheme, then convert if
         * necessary to the nest scheme.  The query_disc method allows
         * to calculate directly in the nest scheme, but early versions
         * of PixTools gave the wrong answer for this and later versions
         * are much slower. */
        List tileList = pixTools_.query_disc( nside_, toVector( ra, dec ),
                                              Math.toRadians( radius ), 0, 1 );
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
            if ( discIntersectsTile( ra, dec, radius, tile ) ) {
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
     * @param   ra   right ascension of disc centre in degrees
     * @param   dec  declination of disc centre in degrees
     * @param   radius   disc radius in degrees
     * @param   tile  tile index
     * @return   true if the disc may be fully or partially within tile;
     *           false if it is definitely outside
     */
    private boolean discIntersectsTile( double ra, double dec, double radius,
                                        long tile ) {

    // Experimental code - it does cut out a few pixels, but not many,
    // and I'm not certain that it's correct.  The spherical geometry is
    // hard (pixel boundaries are not even geodesics).  Comment this out
    // for now, maybe return to it (or write from scratch) if this looks
    // like a bottleneck which is really worth fixing.

    //  /* If the disc is large compared to the resulution of this tiling,
    //   * assume that the given tile is probably correct. */
    //  if ( radius > resolution_ * 2 ) {
    //      return true;
    //  }
    //
    //  /* Otherwise, work out the maximum radius of the tile if it were a
    //   * disc rather than a sort of square by looking at its vertices.
    //   * This is inferior to doing the spherical geometry properly,
    //   * but easier (at least, I don't know how to do it properly). */
    //  double[] centreAng = nest_ ? pixTools_.pix2ang_nest( nside_, tile )
    //                             : pixTools_.pix2ang_ring( nside_, tile );
    //  double centreRa = centreAng[ 1 ];
    //  double centreDec = Math.PI * 0.5 - centreAng[ 0 ];
    //  double tileMaxRadius = 0.0;
    //  double[][] vertices = nest_ ? pixTools_.pix2vertex_nest( nside_, tile )
    //                              : pixTools_.pix2vertex_ring( nside_, tile );
    //  for ( int i = 0; i < 4; i++ ) {
    //      Vector3d vec = new Vector3d( vertices[ 0 ][ i ],
    //                                   vertices[ 1 ][ i ],
    //                                   vertices[ 2 ][ i ] );
    //      double[] ang = pixTools_.Vect2Ang( vec );
    //      double vertRa = ang[ 1 ];
    //      double vertDec = Math.PI * 0.5 - ang[ 0 ];
    //      double vertRadius =
    //          Coords.skyDistance( centreRa, centreDec, vertRa, vertDec );
    //      tileMaxRadius = Math.max( tileMaxRadius, vertRadius );
    //  }
    //  if ( Coords.skyDistance( Math.toRadians( ra ), Math.toRadians( dec ),
    //                           centreRa, centreDec )
    //       > tileMaxRadius + Math.toRadians( radius ) ) {
    //      return false;
    //  }

        /* If no reason to reject so far, accept. */
        return true;
    }
}
