package uk.ac.starlink.table.join;

import gov.fnal.eag.healpix.PixTools;
import java.util.List;
import java.util.logging.Logger;
import javax.vecmath.Vector3d;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.ValueInfo;

/**
 * Implements sky pixellisation using the HEALPix scheme.
 * This works better than the HTM-based one for two reasons:
 * <ol>
 * <li>It tends to be much faster to calculate a list of small pixels
 *     in a region, since the calculations are not hierarchical</li>
 * <li>The pixels are square which means they generally have fewer
 *     neighbours than triangular HTM ones of a similar size</li>
 * </ol>
 *
 * <p>This implementation is based on the PixTools Java HEALPix classes
 * written by Nickolai Kouropatkine at Fermilab.
 * @author   Mark Taylor (Starlink)
 * @author   Nickolai Kouropatkine (EAG, Fermilab)
 * @see      <a href="http://home.fnal.gov/~kuropat/HEALPIX/PixTools.html"
 *                   >http://home.fnal.gov/~kuropat/HEALPIX/PixTools.html</a>
 */
public class HealpixSkyPixellator implements SkyPixellator {

    private final PixTools pixTools_;
    private final int scheme_;
    private final DescribedValue healpixKParam_;
    private double scale_;
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

    private static DefaultValueInfo K_INFO =
        new DefaultValueInfo( "HEALPix k", Integer.class,
                              "Controls sky pixel size. "
                            + "Legal range 0 (60deg) - 20 (0.2\"). "
                            + "k = log2(nside)." );
    static {
        K_INFO.setNullable( true );
    }

    /**
     * Constructs a pixellator using either the RING or NESTED HEALPix scheme.
     *
     * @param  nested  false for RING scheme, true for NESTED
     */
    public HealpixSkyPixellator( boolean nested ) {
        scheme_ = nested ? 1 : 0;
        pixTools_ = new PixTools();
        healpixKParam_ = new HealpixKParameter();
        setHealpixK( -1 );
    }

    /**
     * Constructs a pixellator using the default scheme (RING).
     */
    public HealpixSkyPixellator() {
        this( false );
    }

    public void setScale( double scale ) {
        scale_ = scale;
        configureK();
    }

    public double getScale() {
        return scale_;
    }

    public DescribedValue getTuningParameter() {
        return healpixKParam_;
    }

    public Object[] getPixels( double alpha, double delta, double radius ) {
        double theta = Math.PI * 0.5 - delta;
        Vector3d vec = pixTools_.Ang2Vec( theta, alpha );
        List pixList = pixTools_.query_disc( nside_, vec, radius, scheme_, 1 );
        return pixList.toArray();
    }

    /**
     * Sets the HEALPix k value, which determines sky pixel size,
     * equivalent to log2(nside).
     * May be in the range 0 (60deg) to 20 (0.2").
     * If set to -1, a suitable value will be used based on the scale.
     *
     * @param   healpixK  new k value
     */
    public void setHealpixK( int healpixK ) {
        if ( healpixK < -1 || healpixK > 20 ) {
            throw new IllegalArgumentException( "HEALPix k " + healpixK
                                              + " out of range 0..20" );
        }
        healpixK_ = healpixK;
        configureK();
    }

    /**
     * Returns the HEALPix k value, which determines sky pixel size,
     * equivalent to log2(nside).
     * The returned may be the result of a default determination based
     * on scale if no explicit K value has been set hitherto, and
     * a non-zero scale is available.
     *
     * @return  k value used by this engine
     */
    public int getHealpixK() {
        if ( healpixK_ >= 0 ) {
            return healpixK_;
        }
        else {
            double scale = getScale();
            return scale > 0 ? calculateDefaultK( scale )
                             : -1;
        }
    }

    /**
     * Updates internal state for the current values of scale and
     * k parameter.
     */
    private void configureK() {
        nside_ = 1 << getHealpixK();
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

    /**
     * Implements the tuning parameter which controls the HEALPix K value,
     * and hence nside.  This determines the absolute size of the bins.
     */
    private class HealpixKParameter extends DescribedValue {
        HealpixKParameter() {
            super( K_INFO );
        }
        public Object getValue() {
            int k = getHealpixK();
            return k >= 0 ? new Integer( k ) : null;
        }
        public void setValue( Object value ) {
            setHealpixK( value == null ? -1 : ((Integer) value).intValue() );
        }
    }
}
