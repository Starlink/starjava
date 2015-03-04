package uk.ac.starlink.ttools.plot2.layer;

import java.util.Arrays;

/**
 * Utility class containing various implementations of the Kernel1d
 * interface.  The kernels returned by the factory methods in this
 * class are in general normalised to unity.
 *
 * @author   Mark Taylor
 * @since    2 Mar 2015
 */
public class Kernel1ds {

    /**
     * Delta function kernel.
     * Convolution of a function with this kernel leaves it unaffected.
     */
    public static final Kernel1d DELTA = new Kernel1d() {
        public double[] convolve( double[] in ) {
            return in;
        }
        public int getExtent() {
            return 0;
        }
        public boolean isSquare() {
            return true;
        }
    };

    /**
     * Private constructor prevents instantiation.
     */
    private Kernel1ds() {
    }

    /**
     * Returns a rectangular kernel with a given extent.
     *
     * @param  extent   half-width of rectangle
     * @return  new kernel
     */
    public static Kernel1d createSquareKernel( int extent ) {
        double[] levels = new double[ extent + 1 ];
        for ( int i = 0; i < levels.length; i++ ) {
            levels[ i ] = 1;
        }
        return createSymmetricNormalisedKernel( levels, true );
    }

    /**
     * Returns a kernel based on the cosine squared function.
     *
     * @param  width   half-width of kernel, corresponding to PI/2
     * @return  new kernel
     */
    public static Kernel1d createCos2Kernel( double width ) {
        double[] levels = new double[ (int) Math.ceil( width ) ];
        double xscale = 0.5 * Math.PI / width;
        for ( int i = 0; i < levels.length; i++ ) {
            double x = xscale * i;
            assert x >= 0 && x <= 0.5 * Math.PI;
            double c = Math.cos( x );
            levels[ i ] = c * c;
        }
        return createSymmetricNormalisedKernel( levels, false );
    }

    /**
     * Returns a kernel based on the cosine function.
     *
     * @param  width   half-width of kernel, corresponding to PI/2
     * @return  new kernel
     */
    public static Kernel1d createCosKernel( double width ) {
        double[] levels = new double[ (int) Math.ceil( width ) ];
        double xscale = 0.5 * Math.PI / width;
        for ( int i = 0; i < levels.length; i++ ) {
            double x = xscale * i;
            assert x >= 0 && x <= 0.5 * Math.PI;
            double c = Math.cos( x );
            levels[ i ] = c;
        }
        return createSymmetricNormalisedKernel( levels, false );
    }

    /**
     * Returns a kernel based on the Gaussian function with truncation
     * at a given extent.
     *
     * @param  sigma  standard deviation of kernel
     * @param  extent  extent of kernel; values beyond this are effectively zero
     * @return  new kernel
     */
    public static Kernel1d createTruncatedGaussianKernel( double sigma,
                                                          int extent ) {
        if ( sigma == 0 ) {
            return DELTA;
        }
        else {
            double[] levels = new double[ extent + 1 ];
            double sr2 = 1.0 / ( 2 * sigma * sigma );
            for ( int i = 0; i < extent + 1; i++ ) {
                levels[ i ] = Math.exp( - i * i * sr2 );
            }
            return createSymmetricNormalisedKernel( levels, true );
        }
    }

    /**
     * Returns an untruncated Gaussian kernel.  The extent is unlimited,
     * so use it with care to avoid performance issues.
     *
     * @param  sigma  standard deviation
     * @return   new kernel
     */
    public static Kernel1d createGaussianKernel( double sigma ) {
        return sigma == 0 ? DELTA : new GaussianKernel( sigma );
    }

    /**
     * Creates a symmetric normalised kernel based on a fixed array of
     * function values.  The <code>levels</code> array gives a list of
     * the values at x=0, 1 (and -1), 2 (and -2), ....
     *
     * @param  levels  kernel function values on 1d grid starting from 0
     * @param  isSquare  true iff the kernel is considered non-smooth
     * @return   new kernel
     */
    public static Kernel1d createSymmetricNormalisedKernel( double[] levels,
                                                            boolean isSquare ) {
        if ( levels.length <= 1 ) {
            return DELTA;
        }
        int offset = levels.length - 1;
        double[] weights = new double[ 2 * offset + 1 ];
        for ( int il = 0; il < levels.length; il++ ) {
            weights[ offset + il ] = levels[ il ];
            weights[ offset - il ] = levels[ il ];
        }
        double sum = 0;
        for ( int iw = 0; iw < weights.length; iw++ ) {
            sum += weights[ iw ];
        }
        double scale = 1.0 / sum;
        for ( int iw = 0; iw < weights.length; iw++ ) {
            weights[ iw ] *= scale;
        }
        FixedKernel kernel = new FixedKernel( weights, offset, isSquare );
        assert kernel.getExtent() == levels.length - 1;
        return kernel;
    }

    /**
     * Kernel implementation based on an array given gridded function values.
     * This is not necessarily normalised or symmetric.
     */
    private static class FixedKernel implements Kernel1d {

        private final double[] weights_;
        private final int offset_;
        private final boolean isSquare_;

        /**
         * Constructor.
         *
         * @param   weights  function values on 1d grid
         * @param   offset  index into weight corresponding to x=0
         * @param  isSquare  true iff the kernel is considered non-smooth
         */
        public FixedKernel( double[] weights, int offset, boolean isSquare ) {
            weights_ = weights.clone();
            offset_ = offset;
            isSquare_ = isSquare;
        }

        public int getExtent() {
            return Math.max( offset_, weights_.length - 1 - offset_ );
        }

        public double[] convolve( double[] in ) {
            int ns = in.length;
            int nw = weights_.length;
            double[] out = new double[ ns ];
            for ( int iw = 0; iw < nw; iw++ ) {
                double weight = weights_[ iw ];
                int is0 = Math.max( 0, offset_ - iw );
                int is1 = Math.min( ns, ns + offset_ - iw );
                for ( int is = is0; is < is1; is++ ) {
                    out[ is ] += in[ is + iw - offset_ ] * weight;
                }
            }
            return out;
        }

        public boolean isSquare() {
            return isSquare_;
        }

        @Override
        public int hashCode() {
            int code = 9902;
            code = 23 * code + Arrays.hashCode( weights_ );
            code = 23 * code + offset_;
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof FixedKernel ) {
                FixedKernel other = (FixedKernel) o;
                return Arrays.equals( this.weights_, other.weights_ )
                    && this.offset_ == other.offset_;
            }
            else {
                return false;
            }
        }
    }

    /**
     * Kernel implementation based on an analytic function.
     */
    private abstract static class FunctionKernel implements Kernel1d {

        private final int extent_;

        /**
         * Constructor.
         *
         * @param  extent  extent of effect (use Integer.MAX_VALUE if infinite)
         */
        public FunctionKernel( int extent ) {
            extent_ = extent;
        }

        public int getExtent() {
            return extent_;
        }

        /**
         * Returns the value of the function at a grid point dx units from
         * the origin.  Note this should in general give the value of
         * the integral from dx-0.5 to dx+0-5 of the underlying analytic
         * function.
         *
         * @param  dx  grid point index
         */
        public abstract double function( int dx );

        public double[] convolve( double[] in ) {
            int ns = in.length;
            double[] out = new double[ ns ];
            for ( int is = 0; is < ns; is++ ) {
                for ( int js = 0; js < ns; js++ ) {
                    out[ js ] += function( js - is ) * in[ is ];
                }
            }
            return out;
        }
    }

    /**
     * Untruncated symmetric normalised Gaussian kernel.
     */
    private static class GaussianKernel extends FunctionKernel {

        private final double sigma_;
        private final double a_;
        private final double b_;
        private final double c_;

        private static final double A1 =  0.254829592;
        private static final double A2 = -0.284496736;
        private static final double A3 =  1.421413741;
        private static final double A4 = -1.453152027;
        private static final double A5 =  1.061405429;
        private static final double P  =  0.3275911;

        /**
         * Constructor.
         *
         * @param   sigma   standard deviation
         */
        public GaussianKernel( double sigma ) {
            super( Integer.MAX_VALUE );
            sigma_ = sigma;
            a_ = 1.0 / ( sigma_ * Math.sqrt( 2.0 * Math.PI ) );
            b_ = 0.5 / ( sigma_ * sigma_ );
            c_ = Math.sqrt( 0.5 ) / sigma_;
        }

        public boolean isSquare() {
            return false;
        }

        public double function( int dx ) {

            // Note that the Gaussian function itself would not be quite
            // correct here, we need to integrate it over a unit length
            // centered at this point for the normalisation to work out
            // correctly.
            return integratedGaussian( dx, 1.0 );
        }

        /**
         * Returns the gaussian function at a given value of x.
         *
         * @param  x  point for evaluation
         * @return  g(x)
         */
        private double gaussian( double x ) {
            return a_ * Math.exp( - b_ * x * x );
        }

        /**
         * Returns the integral of the Gaussian function over an interval
         * delta centered on a tiven value of x.
         *
         * @param  x  central point for evaluation
         * @param  delta  width of integration interval
         * @return  integral of g(x)dx from x-delta/2 to x+delta/2
         */
        private double integratedGaussian( double x, double delta ) {
            double d2 = delta * 0.5;
            return 0.5 * ( erf( ( x + d2 ) * c_ ) - erf( ( x - d2 ) * c_ ) )
                       / delta;
        }

        @Override
        public int hashCode() {
            int code = 2459;
            code = 23 * code + Float.floatToIntBits( (float) sigma_ );
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof GaussianKernel ) {
                GaussianKernel other = (GaussianKernel) o;
                return this.sigma_ == other.sigma_;
            }
            else {
                return false;
            }
        }

        /**
         * Error function, numerical approximation, pinched from the
         * Picomath library.
         *
         * @see  <a href="http://picomath.org/java/Erf.java.html">picomath</a>
         */
        public static double erf( double x ) {
            double sign = 1;
            if (x < 0) {
                sign = -1;
            }
            x = Math.abs(x);
            double t = 1.0/(1.0 + P*x);
            double y =
                1.0 - (((((A5*t + A4)*t) + A3)*t + A2)*t + A1)*t*Math.exp(-x*x);
            return sign*y;
        }
    }
}
