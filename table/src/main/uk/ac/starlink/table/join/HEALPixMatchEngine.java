package uk.ac.starlink.table.join;

import gov.fnal.eag.healpix.PixTools;
import java.util.List;
import java.util.logging.Logger;
import javax.vecmath.Vector3d;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.ValueInfo;

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

    private final PixTools pixTools_;
    private final DescribedValue healpixKParam_;
    private int healpixK_;
    private long nside_;

    private static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.table.join" );
    private static final int SCHEME = 0; // Use HEALPix ring numbering scheme

    /**
     * Scale factor which determines the sky pixel size scale to use,
     * as a multiple of the separation size, if no k value is set explicitly.
     * This is a tuning factor (any value will give correct results,
     * but performance may be affected).
     * The current value may not be optimal.
     */
    private static final double DEFAULT_SCALE_FACTOR = 8;

    /**
     * Constructs a new match engine which considers two points 
     * (RA,Dec tuples) to match if they are within a given angular
     * distance on the celestial sphere.
     *
     * @param   separation  match radius in radians
     * @param   useErrors   if true, per-row errors can be specified as
     *          a third element of the tuples; otherwise only the fixed
     *          separation value counts
     */
    public HEALPixMatchEngine( double separation, boolean useErrors ) {
        super( useErrors );
        pixTools_ = new PixTools();
        healpixKParam_ = new HealpixKParameter();
        setSeparation( separation );
        setHealpixK( -1 );
    }

    public void setSeparation( double separation ) {
        super.setSeparation( separation );
        configureK();
    }

    protected Object[] getBins( double ra, double dec, double err ) {
        double theta = Math.PI * 0.5 - dec;
        Vector3d vec = pixTools_.Ang2Vec( theta, ra );
        List binList = pixTools_.query_disc( nside_, vec, err, SCHEME, 1 );
        return binList.toArray();
    }

    public DescribedValue[] getTuningParameters() {
        return new DescribedValue[] { healpixKParam_ };
    }

    /**
     * Sets the HEALPix k value, which determines sky pixel size,
     * equivalent to log2(nside).
     * May be in the range 0 (60deg) to 20 (0.2").
     * If set to -1, a suitable value will be used based on the separation.
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
     * on separation if no explicit K value has been set hitherto, and
     * a non-zero separation is available.
     *
     * @return  k value used by this engine
     */
    public int getHealpixK() {
        if ( healpixK_ >= 0 ) {
            return healpixK_;
        }
        else {
            double sep = getSeparation();
            return sep > 0 ? calculateDefaultK( sep )
                           : -1;
        }
    }

    /**
     * Updates internal state for the current values of separation and
     * k parameter.
     */
    private void configureK() {
        nside_ = 1 << getHealpixK();
    }

    /**
     * Determines a default value to use for the HEALPix k parameter
     * based on a given separation.
     *
     * @param   sep   max sky separation angle for a match, in radians
     */
    public int calculateDefaultK( double sep ) {

        /* Calculate the HEALPix map resolution parameter appropriate for
         * the requested separation.  The pixel size probably wants to
         * be about the same size as the separation, though I'm not sure
         * exactly what the optimal size is - probably depends on the
         * details of the match being done.  Any value is correct, the
         * size is just a tuning parameter. */
        double pixelSize = DEFAULT_SCALE_FACTOR * sep;
        double pixelSizeArcSec = pixelSize * ( 180. * 60 * 60 / Math.PI );
        long nside = pixTools_.GetNSide( pixelSizeArcSec );
        return (int) Math.round( Math.log( nside ) / Math.log( 2 ) );
    }

    /**
     * Implements the tuning parameter which controls the HEALPix K value,
     * and hence nside.  This determines the absolute size of the bins.
     */
    private class HealpixKParameter extends DescribedValue {
        HealpixKParameter() {
            super( new DefaultValueInfo( "HEALPix k", Integer.class ) );
            DefaultValueInfo info = (DefaultValueInfo) getInfo();
            info.setDescription( "Controls sky pixel size. "
                               + "Legal range 0 (60deg) - 20 (0.2\"). "
                               + "k = log2(nside)." );
            info.setNullable( true );
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
