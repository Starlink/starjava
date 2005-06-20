package uk.ac.starlink.table.join;

import gov.fnal.eag.healpix.PixTools;
import java.util.List;
import java.util.logging.Logger;
import javax.vecmath.Vector3d;

/**
 * SkyMatchEngine implementation which works using the HEALPix pixelisation
 * scheme.  This works better than the HTM-based one for two reasons:
 * <ol>
 * <li>It tends to be much faster to calculate a list of small pixels 
 *     in a region, since the calculations are not hierarchical</li>
 * <li>The pixels are square which means they generally have fewer 
 *     neighbours than triangular HTM ones of a similar size</li>
 * </ol>
 *
 * <p>This implementation is based on the PixTools Java HEALPix classes
 * written by Nickolai Kouropatkine at Fermilab.
 *
 * @author   Mark Taylor (Starlink)
 * @author   Nickolai Kouropatkine (EAG, Fermilab)
 * @see      <a href="http://home.fnal.gov/~kuropat/HEALPIX/PixTools.html"
 *                   >http://home.fnal.gov/~kuropat/HEALPIX/PixTools.html</a>
 */
public class HEALPixMatchEngine extends SkyMatchEngine {

    private long nside_;
    private double separation_;
    private final PixTools pixTools_;
    private static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.table.join" );
    private static final int SCHEME = 0; // Use HEALPix ring numbering scheme

    /**
     * Constructs a new match engine which considers two points 
     * (RA,Dec tuples) to match if they are within a given angular
     * distance on the celestial sphere.
     *
     * @param   separation  match radius in radians
     */
    public HEALPixMatchEngine( double separation ) {
        super( separation );
        separation_ = separation;
        pixTools_ = new PixTools();
    }

    public void setSeparation( double separation ) {
        super.setSeparation( separation );

        /* Calculate the HEALPix map resolution parameter appropriate for
         * the requested separation.  The pixel size probably wants to
         * be about the same size as the separation, though I'm not sure
         * exactly what the optimal size is - probably depends on the
         * details of the match being done.  Any value is correct, the
         * size is just a tuning parameter. */
        double pixelSize = 2 * separation;
        double pixelSizeArcSec = pixelSize * ( 180. * 60 * 60 / Math.PI );
        nside_ = new PixTools().GetNSide( pixelSizeArcSec );
    }

    public Object[] getBins( Object[] radec ) {
        if ( radec[ 0 ] instanceof Number && radec[ 1 ] instanceof Number ) {
            double ra = ((Number) radec[ 0 ]).doubleValue();
            double dec = ((Number) radec[ 1 ]).doubleValue();
            double theta = Math.PI * 0.5 - dec;
            Vector3d vec = pixTools_.Ang2Vec( theta, ra );
            List binList = pixTools_.query_disc( nside_, vec, separation_, 
                                                 SCHEME, 1 );
            return binList.toArray();
        }
        else {
            return NO_BINS;
        }
    }
}
