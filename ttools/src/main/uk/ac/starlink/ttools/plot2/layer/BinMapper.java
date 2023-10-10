package uk.ac.starlink.ttools.plot2.layer;

import uk.ac.starlink.ttools.plot2.Equality;

/**
 * Maps axis values to bin indices.
 *
 * @author   Mark Taylor
 * @since    16 Jan 2017
 */
@Equality
public abstract class BinMapper {

    /**
     * Returns the bin index for a given value.
     * In case of an invalid value (NaN, or non-positive for log mapper),
     * behaviour is undefined (quite likely zero will be returned).
     *
     * @param   value  valid axis value
     * @return  bin index
     */
    public abstract int getBinIndex( double value );

    /**
     * Returns the bin limits for a given bin index.
     *
     * @param   index  bin index
     * @return   (lower,upper) bin limits
     */
    public abstract double[] getBinLimits( int index );

    /**
     * Returns a BinMapper instance.
     *
     * <p>Notional bin boundaries are for <code>log=false</code>:
     * <pre>
     *   binWidth*(0+binPhase), binWidth*(1+binPhase), ...
     * </pre>
     * and for <code>log=true</code>:
     * <pre>
     *   binWidth**(0+binPhase), binWidth**(1+binPhase), ...
     * </pre>
     *
     * <p>The <code>point</code> parameter is used internally to determine
     * the zero point of the bins.  In principle this should make no
     * difference to behaviour, but in case that the data is situated
     * a very long way from 1,  setting it close to
     * the actual data point locations may avoid rounding errors.
     *
     * @param   log   false for linear axis scaling, true for logarithmic
     * @param   binWidth   width of each bin; this is additive for linear
     *                     and multiplicative for logarithmic scaling
     * @param   binPhase   determines sub-bin boundary shifts along axis,
     *                     normally in range 0..1
     * @param   point   representative point on axis near which bins are
     *                  situated
     */
    public static BinMapper createMapper( boolean log, double binWidth,
                                          double binPhase, double point ) {
        binPhase = binPhase % 1;
        if ( binPhase < 0 ) {
            binPhase += 1;
        }
        return log ? new LogBinMapper( binWidth, binPhase, point )
                   : new LinearBinMapper( binWidth, binPhase, point );
    }

    /**
     * Logarithm function, used for transforming values on logarithmic X axis.
     *
     * @param  val  value
     * @return  log to base 10 of <code>val</code>
     */
    public static double log( double val ) {
        return Math.log10( val );
    }

    /**
     * BinMapper implementation for linear axis scaling.
     */
    private static class LinearBinMapper extends BinMapper {
        private final double width_;
        private final double width1_;
        private final double floor_;

        /**
         * Constructor.
         *
         * @param  width  additive bin width
         * @param  phase  bin phase in range 0..1
         * @param  point    representative point
         */
        LinearBinMapper( double width, double phase, double point ) {
            width_ = width;
            width1_ = 1.0 / width;
            double f0 = Math.floor( point / width );
            floor_ = ( f0 + phase ) * width;
            assert Math.abs( floor_ - point ) <= width;
        }

        public int getBinIndex( double value ) {
            return (int) Math.floor( ( value - floor_ ) * width1_ );
        }

        public double[] getBinLimits( int index ) {
            double lo = floor_ + index * width_;
            return new double[] { lo, lo + width_ };
        }

        @Override
        public int hashCode() {
            int code = 55289;
            code = 23 * code + Float.floatToIntBits( (float) width_ );
            code = 23 * code + Float.floatToIntBits( (float) floor_ );
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof LinearBinMapper ) {
                LinearBinMapper other = (LinearBinMapper) o;
                return this.width_ == other.width_
                    && this.floor_ == other.floor_;
            }
            else {
                return false;
            }
        }
    }

    /**
     * BinMapper implementation for logarithmic axis scaling.
     */
    private static class LogBinMapper extends BinMapper {
        private final double width_;
        private final double floor_;
        private final double logWidth1_;
        private final double logFloor_;

        /**
         * Constructor.
         *
         * @param  width  multiplicative bin width
         * @param  phase  bin phase in range 0..1
         * @param  point    representative point
         */
        LogBinMapper( double width, double phase, double point ) {
            width_ = width;
            if ( point <= 0 ) {
                point = 1;
            }
            double f0 = Math.floor( log( point ) / log( width ) );
            floor_ = Math.pow( width, f0 + phase );
            logFloor_ = log( floor_ );
            assert Math.abs( logFloor_ - log( point ) ) <= width;
            logWidth1_ = 1. / log( width );
        }

        public int getBinIndex( double value ) {
            return (int) Math.floor( (log( value ) - logFloor_) * logWidth1_ );
        }

        public double[] getBinLimits( int index ) {
            double lo = floor_ * Math.pow( width_, index );
            return new double[] { lo, lo * width_ };
        }

        @Override
        public int hashCode() {
            int code = 44178;
            code = 23 * Float.floatToIntBits( (float) width_ );
            code = 23 * Float.floatToIntBits( (float) floor_ );
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof LogBinMapper ) {
                LogBinMapper other = (LogBinMapper) o;
                return this.width_ == other.width_
                    && this.floor_ == other.floor_;
            }
            else {
                return false;
            }
        }
    }
}
