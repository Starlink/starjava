package uk.ac.starlink.ttools.plot2.layer;

import uk.ac.starlink.ttools.plot2.Equality;

/**
 * Determines 1-d histogram bin widths from data bounds.
 *
 * @author   Mark Taylor
 * @since    8 Jan 2014
 */
@Equality
public abstract class BinSizer {
    
    /**
     * Provides a bin width value for a given axis data range.
     *
     * @param  xlog  false for linear scaling, true for logarithmic
     * @param  xlo   axis lower bound
     * @param  xhi   axis upper bound
     * @return   additive/multiplicative bin width appropriate for the
     *           given range
     */
    public abstract double getWidth( boolean xlog, double xlo, double xhi );

    /**
     * Returns a bin sizer instance which divides the axis range up into
     * a fixed number of equal intervals.
     *
     * @param   nbin   number of intervals to divide the axis into
     * @return  bin sizer instance
     */
    public static BinSizer createCountBinSizer( int nbin ) {
        return new CountBinSizer( nbin );
    }

    /**
     * Primitive BinSizer implementation that just chops the data range
     * up into a fixed number of equal intervals.
     */
    private static class CountBinSizer extends BinSizer {
        private final int nbin_;

        /**
         * Constructor.
         *
         * @param  nbin  number of intervals
         */
        CountBinSizer( int nbin ) {
            nbin_ = nbin;
        }

        public double getWidth( boolean xlog, double xlo, double xhi ) {
            return xlog ? Math.exp( Math.log( xhi / xlo ) / nbin_ )
                        : ( xhi - xlo ) / nbin_;
        }

        @Override
        public int hashCode() {
            return nbin_ * 44301;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof CountBinSizer ) {
                CountBinSizer other = (CountBinSizer) o;
                return this.nbin_ == other.nbin_;
            }
            else {
                return false;
            }
        }
    }
}
