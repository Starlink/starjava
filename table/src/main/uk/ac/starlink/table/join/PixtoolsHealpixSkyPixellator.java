package uk.ac.starlink.table.join;

import gov.fnal.eag.healpix.PixTools;
import java.util.List;
import java.util.logging.Logger;
import javax.vecmath.Vector3d;

/**
 * HEALPix sky pixellator using the PixTools library.
 * This implementation is based on the PixTools Java HEALPix classes
 * written by Nickolai Kouropatkine at Fermilab.
 * The maximum K value is 20.
 *
 * @author   Mark Taylor (Starlink)
 * @author   Nickolai Kouropatkine (EAG, Fermilab)
 * @see      <a href="http://home.fnal.gov/~kuropat/HEALPIX/PixTools.html"
 *                   >http://home.fnal.gov/~kuropat/HEALPIX/PixTools.html</a>
 */
public class PixtoolsHealpixSkyPixellator extends HealpixSkyPixellator {

    private final PixTools pixTools_;
    private final int scheme_;
    private int healpixK_;
    private long nside_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.table.join" );

    /**
     * Scale factor which determines the sky pixel size to use,
     * as a multiple of the angular scale, if no k value is set explicitly.
     * This is a tuning factor (any value will give correct results,
     * but performance may be affected).
     * The current value may not be optimal.
     */
    private static final int DEFAULT_SCALE_FACTOR = 8;

    /**
     * Constructs a pixellator using either the RING or NESTED HEALPix scheme.
     *
     * @param  nested  false for RING scheme, true for NESTED
     */
    public PixtoolsHealpixSkyPixellator( boolean nested ) {
        super( 20 );
        scheme_ = nested ? 1 : 0;
        pixTools_ = PixTools.getInstance();
    }

    /**
     * Constructs a pixellator using the default scheme (RING).
     */
    public PixtoolsHealpixSkyPixellator() {
        this( false );
    }

    public Object[] getPixels( double alpha, double delta, double radius ) {
        double theta = Math.PI * 0.5 - delta;
        Vector3d vec = pixTools_.Ang2Vec( theta, alpha );
        List pixList = pixTools_.query_disc( nside_, vec, radius, scheme_, 1 );
        return pixList.toArray();
    }

    protected void configureK( int k ) {
        nside_ = 1 << k;
    }

    /**
     * Determines a default value to use for the HEALPix k parameter
     * based on a given scale.
     *
     * @param   scale   distance scale, in radians
     */
    public int calculateDefaultK( double scale ) {

        /* Calculate the HEALPix map resolution parameter appropriate for
         * the requested scale.  Any value is correct, the scale is just
         * a tuning parameter. */
        double pixelSize = DEFAULT_SCALE_FACTOR * scale;
        double pixelSizeArcSec = pixelSize * ( 180. * 60 * 60 / Math.PI );

        /* Put a limit on the value.  If a value smaller than this is
         * used, the result is the same (20), but PixTools writes a warning
         * to standard output.  Emit the same message through the logging
         * system instead. */
        if ( pixelSizeArcSec < 0.21) {
            pixelSizeArcSec = 0.21;
            logger_.info( "pixtools: nside cannot be bigger than 1048576" );
        }
        long nside = pixTools_.GetNSide( pixelSizeArcSec );
        return (int) Math.round( Math.log( nside ) / Math.log( 2 ) );
    }
}
