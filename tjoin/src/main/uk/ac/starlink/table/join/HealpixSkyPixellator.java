package uk.ac.starlink.table.join;

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
 * @author   Mark Taylor (Starlink)
 */
public abstract class HealpixSkyPixellator implements SkyPixellator {

    private final DescribedValue healpixKParam_;
    private final int maxK_;
    private double scale_;
    private int healpixK_;

    /**
     * Constructor.
     *
     * @param  maxK  the maximum K value permitted by this implementation
     */
    protected HealpixSkyPixellator( int maxK ) {
        maxK_ = maxK;
        String kdesc = "Controls sky pixel size. "
                     + "Legal range 0 - " + maxK + ". "
                     + "0 is 60deg, 20 is 0.2\".";
        DefaultValueInfo kInfo =
            new DefaultValueInfo( "HEALPix k", Integer.class, kdesc );
        kInfo.setNullable( true );
        healpixKParam_ = new HealpixKParameter( kInfo );
        setHealpixK( -1 );
    }

    /**
     * Returns the maximum permissible K value.
     *
     * @return  maximum Healpix K parameter supported by this implementation
     */
    public int getMaxK() {
        return maxK_;
    }

    public void setScale( double scale ) {
        scale_ = scale;
        configureK( getHealpixK() );
    }

    public double getScale() {
        return scale_;
    }

    public DescribedValue getTuningParameter() {
        return healpixKParam_;
    }

    /**
     * Sets the HEALPix k value, which determines sky pixel size,
     * equivalent to log2(nside).
     * May be in the range 0 (60deg) to {@link #getMaxK}.
     * If set to -1, a suitable value will be used based on the scale.
     *
     * @param   healpixK  new k value
     */
    public void setHealpixK( int healpixK ) {
        int maxK = getMaxK();
        if ( healpixK < -1 || healpixK > maxK ) {
            throw new IllegalArgumentException( "HEALPix k " + healpixK
                                              + " out of range 0.." + maxK );
        }
        healpixK_ = healpixK;
        configureK( getHealpixK() );
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
     * Updates internal state for the current value of the k parameter.
     *
     * @param  k   HEALPix order
     */
    protected abstract void configureK( int k );

    /**
     * Determines a default value to use for the HEALPix k parameter
     * based on a given scale.
     *
     * @param   scale   distance scale, in radians
     */
    public abstract int calculateDefaultK( double scale );

    /**
     * Implements the tuning parameter which controls the HEALPix K value,
     * and hence nside.  This determines the absolute size of the bins.
     */
    private class HealpixKParameter extends DescribedValue {
        HealpixKParameter( ValueInfo info ) {
            super( info );
        }
        public Object getValue() {
            int k = getHealpixK();
            return k >= 0 ? Integer.valueOf( k ) : null;
        }
        public void setValue( Object value ) {
            setHealpixK( value == null ? -1 : ((Integer) value).intValue() );
        }
    }
}
