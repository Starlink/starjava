package uk.ac.starlink.ttools.plot2.layer;

import uk.ac.starlink.ttools.plot2.Equality;
import uk.ac.starlink.ttools.plot2.Scale;

/**
 * Maps axis values to bin indices.
 *
 * @author   Mark Taylor
 * @since    16 Jan 2017
 */
@Equality
public class BinMapper {

    private final Scale scale_;
    private final double swidth_;
    private final double sfloor_;
    private final double swidth1_;

    /**
     * Constructor.
     *
     * <p>The <code>dpoint</code> parameter is used internally to determine
     * the zero point of the bins.  In principle this should make no
     * difference to behaviour, but in case that the data is situated
     * a very long way from 1,  setting it close to
     * the actual data point locations may avoid rounding errors.
     *
     * @param  scale  axis scaling
     * @param  scaleWidth  extent of bins in scale units
     * @param   binPhase   determines sub-bin boundary shifts along axis,
     *                     normally in range 0..1
     * @param   dpoint   representative data space point on axis
     *                   which bins are situated
     */
    public BinMapper( Scale scale, double scaleWidth, double binPhase,
                      double dpoint ) {
        scale_ = scale;
        swidth_ = scaleWidth;
        swidth1_ = 1.0 / swidth_;
        if ( binPhase > 1 ) {
            binPhase = binPhase % 1;
        }
        if ( binPhase < 0 ) {
            binPhase += 1;
        }
        double spoint = scale.dataToScale( dpoint );
        double f0 = Math.floor( spoint * swidth1_ );
        sfloor_ = ( f0 + binPhase ) * swidth_;
        assert (float) Math.abs( sfloor_ - spoint ) <= (float) swidth_;
    }

    /**
     * Returns the bin index for a given value.
     * In case of an invalid value (NaN, or non-positive for log scale),
     * behaviour is undefined (quite likely zero will be returned).
     *
     * @param   dvalue  valid axis value in data space
     * @return  bin index
     */
    public int getBinIndex( double dvalue ) {
        double svalue = scale_.dataToScale( dvalue );
        return (int) Math.floor( ( svalue - sfloor_ ) * swidth1_ );
    }

    /**
     * Returns the bin limits for a given bin index.
     *
     * @param   index  bin index
     * @return   (lower,upper) bin limits
     */
    public double[] getBinLimits( int index ) {
        double slo = sfloor_ + index * swidth_;
        return new double[] { scale_.scaleToData( slo ),
                              scale_.scaleToData( slo + swidth_ ) };
    }

    @Override
    public int hashCode() {
        int code = 55289;
        code = 23 * code + scale_.hashCode();
        code = 23 * code + Float.floatToIntBits( (float) swidth_ );
        code = 23 * code + Float.floatToIntBits( (float) sfloor_ );
        return code;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof BinMapper ) {
            BinMapper other = (BinMapper) o;
            return this.scale_.equals( other.scale_ )
                && this.swidth_ == other.swidth_
                && this.sfloor_ == other.sfloor_;
        }
        else {
            return false;
        }
    }
}
