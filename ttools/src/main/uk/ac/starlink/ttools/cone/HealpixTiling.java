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
     * @param  nside  HEALPix Nside parameter
     * @param  nest  true for nesting scheme, false for ring scheme
     */
    public HealpixTiling( long nside, boolean nest ) {
        nside_ = nside;
        nest_ = nest;
        pixTools_ = new PixTools();
    }

    public long getPositionTile( double ra, double dec ) {
        Vector3d vec = toVector( ra, dec );
        return nest_ ? pixTools_.vect2pix_nest( nside_, vec )
                     : pixTools_.vect2pix_ring( nside_, vec );
    }

    public long[] getTileRange( double ra, double dec, double radius ) {
        List tileList =
            pixTools_.query_disc( nside_, toVector( ra, dec ),
                                  Math.toRadians( radius ), 
                                  nest_ ? 1 : 0, 1 );
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
