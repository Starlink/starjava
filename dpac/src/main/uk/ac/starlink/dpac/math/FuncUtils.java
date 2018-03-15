package uk.ac.starlink.dpac.math;

/**
 * Utility methods associated with functions.
 * This class contains only static methods.
 *
 * @author   Mark Taylor
 * @since    15 Mar 2018
 */
public class FuncUtils {

    /**
     * Private constructor prevents instantiation.
     */
    private FuncUtils() {
    }

    /**
     * Returns a function which represents an interpolated representation
     * of the given sample set, using quadratic interpolation with
     * Legendre polynomials.
     *
     * @param   nf   input numeric function
     * @return   interpolated version of function
     */
    public static Function interpolateQuadratic( NumericFunction nf ) {
        return new QuadraticInterpolation( nf );
    }

    /**
     * Returns a function which represents an interpolated representation
     * of the given sample set, using splines.
     *
     * @param   nf   input numeric function
     * @return   interpolated version of function
     */
    public static Function interpolateSpline( NumericFunction nf ) {
        return new SplineInterpolation( nf );
    }

    /**
     * Returns a function which represents an interpolated representation
     * of the given sample set, using linear interpolation.
     *
     * @param   nf   input numeric function
     * @return   interpolated version of function
     */
    public static Function interpolateLinear( NumericFunction nf ) {
        return new LinearInterpolation( nf );
    }

    /**
     * Locates the X value where a supplied monotonically non-decreasing
     * function is within a specified tolerance of a given Y value.
     * The bisection method is used.
     * Behaviour is undefined if the supplied function is not monotonic.
     *
     * @param  f  monotonic function
     * @param  xlo  lowest X value to consider
     * @param  xhi  highest X value to consider
     * @param  y0  required Y value
     * @param  ytol  tolerance in Y
     * @return   X value corresonding to <code>y0</code>,
     *           or NaN if it's not in range
     */
    public static double findValueMonotonic( Function f, double xlo,
                                             double xhi, double y0,
                                             double ytol ) {
        double ylo = f.f( xlo );
        double yhi = f.f( xhi );
        if ( y0 < ylo || y0 > yhi ) {
            return Double.NaN;
        }
        double xmid;
        double dy;
        int nit = 0;
        do {
            xmid = 0.5 * ( xlo + xhi );
            double ymid = f.f( xmid );
            dy = ymid - y0;
            if ( dy < 0 ) {
                xlo = xmid;
                ylo = ymid;
            }
            else {
                xhi = xmid;
                yhi = ymid;
            }
        } while ( Math.abs( dy ) > ytol && ++nit < 100 );
        return xmid;
    }

    /**
     * Spline interpolation, as used in CU9 classes.
     */
    private static class SplineInterpolation implements Function {
        private final double xlo_;
        private final double xhi_;
        private final PolynomialSplineFunction spline_;

        /**
         * Constructor.
         *
         * @param  nf  numeric function
         */
        public SplineInterpolation( NumericFunction nf ) {
            int n = nf.getCount();
            double[] xs = new double[ n ];
            double[] ys = new double[ n ];
            for ( int i = 0; i < n; i++ ) {
                xs[ i ] = nf.getX( i );
                ys[ i ] = nf.getY( i );
            }
            xlo_ = xs[ 0 ];
            xhi_ = xs[ n - 1 ];
            spline_ = new SplineInterpolator().interpolate( xs, ys );
        }

        public double f( double x ) {
            return x >= xlo_ && x <= xhi_ ? spline_.value( x )
                                          : Double.NaN;
        }
    }

    /**
     * Interpolation that works directly with a NumericFunction.
     */
    private static abstract class BinInterpolation implements Function {
        final NumericFunction nf_;
        final double xlo_;
        final double xhi_;
        final int n_;

        /**
         * Constructor.
         *
         * @param  nf  numeric function
         */
        protected BinInterpolation( NumericFunction nf ) {
            nf_ = nf;
            n_ = nf.getCount();
            xlo_ = nf.getX( 0 );
            xhi_ = nf.getX( n_ - 1 );
        }

        public double f( double x ) {
            return x >= xlo_ && x <= xhi_ ? interpolate( x ) : Double.NaN;
        }

        /**
         * Returns the interpolated value at a validated X point.
         *
         * @param  x  x position in range
         * @return  interpolated Y value
         */
        private double interpolate( double x ) {
            int ilo = 0;
            int ihi = n_ - 1;
            double xlo = nf_.getX( ilo );
            double xhi = nf_.getX( ihi );
            while ( ihi - ilo > 1 ) {
                int imid = ( ilo + ihi ) / 2;
                double xmid = nf_.getX( imid );
                if ( x == xmid ) {
                    return nf_.getY( imid );
                }
                else if ( x < xmid ) {
                    ihi = imid;
                    xhi = xmid;
                }
                else {
                    ilo = imid;
                    xlo = xmid;
                }
            }
            assert x >= nf_.getX( ilo ) && x <= nf_.getX( ihi );
            return interpolateInBin( x, ilo );
        }

        /**
         * Returns the interpolated value for an X value assumed to be
         * between samples ix and ix+1.
         *
         * @param  x   X value
         * @param  ix  nf sample index just lower than x
         * @return  interpolated Y value for x
         */
        abstract double interpolateInBin( double x, int ix );
    }

    /**
     * Interpolation that uses linear approxiations.
     */
    private static class LinearInterpolation extends BinInterpolation {

        /**
         * Constructor.
         *
         * @param  nf  numeric function
         */
        public LinearInterpolation( NumericFunction nf ) {
            super( nf );
        }

        double interpolateInBin( double x, int ix ) {
            if ( ix >= 0 && ix < n_ - 1 ) {
                double x0 = nf_.getX( ix );
                double x1 = nf_.getX( ix + 1 );
                double y0 = nf_.getY( ix );
                double y1 = nf_.getY( ix + 1 );
                return y0 + ( x - x0 ) * ( y1 - y0 ) / ( x1 - x0 );
            }
            else {
                return Double.NaN;
            }
        }
    }

    /**
     * Interpolation that uses quadratic interpolation (parabolas).
     */
    private static class QuadraticInterpolation extends BinInterpolation {

        /**
         * Constructor.
         *
         * @param  nf  numeric function
         */
        public QuadraticInterpolation( NumericFunction nf ) {
            super( nf );
        }

        double interpolateInBin( double x, int ix ) {
            double e1 = interpolateUsingBins( x, ix - 1, ix, ix + 1 );
            double e2 = interpolateUsingBins( x, ix, ix + 1, ix + 2 );
            boolean ok1 = !Double.isNaN( e1 );
            boolean ok2 = !Double.isNaN( e2 );
            if ( ok1 && ok2 ) {
                return 0.5 * ( e1 + e2 );
            }
            else if ( ok1 ) {
                return e1;
            }
            else {
                return e2;
            }
        }

        /**
         * Attempts an interpolation using three samples of the numeric
         * function.
         * If any of the sample indices is out of range, NaN is returned.
         *
         * @param  x  X position
         * @param  ix1   first sample index
         * @param  ix2   second sample index
         * @param  ix3   third sample index
         * @return  interpolated value at x, or NaN
         */
        private double interpolateUsingBins( double x, int ix1, int ix2,
                                             int ix3 ) {
            if ( ix1 < 0 || ix2 < 0 || ix3 < 0 ||
                 ix1 >= n_ || ix2 >= n_ || ix3 >= n_ ) {
                return Double.NaN;
            }

            /* Lagrange Polynomial Interpolation
             * (copied from the wikipedia page on Simpson's Rule). */
            double x1 = nf_.getX( ix1 );
            double x2 = nf_.getX( ix2 );
            double x3 = nf_.getX( ix3 );
            double y1 = nf_.getY( ix1 );
            double y2 = nf_.getY( ix2 );
            double y3 = nf_.getY( ix3 );
            return y1 * ( x - x2 ) * ( x - x3 ) / ( ( x1 - x2 ) * ( x1 - x3 ) )
                 + y2 * ( x - x1 ) * ( x - x3 ) / ( ( x2 - x1 ) * ( x2 - x3 ) )
                 + y3 * ( x - x1 ) * ( x - x2 ) / ( ( x3 - x1 ) * ( x3 - x2 ) );
        }
    }
}
