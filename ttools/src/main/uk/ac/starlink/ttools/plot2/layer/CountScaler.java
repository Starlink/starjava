package uk.ac.starlink.ttools.plot2.layer;

import uk.ac.starlink.ttools.func.Maths;
import uk.ac.starlink.ttools.plot2.PlotUtil;

/**
 * Maps an integer count value to a floating point range between 0 and 1
 * with certain constraints.
 * This was written to map a count value to a colour, and the scaling
 * is intended to work well with that.  Differences of size 1 should be
 * visible at the bottom end, but differences between high densities
 * should be visible at the top end.  The asinh function is used to try
 * to achieve this.
 *
 * @author   Mark Taylor
 * @since    18 Feb 2013
 */
public abstract class CountScaler {

    /**
     * Scales an integer value in the range 0-max to the range 0-1.
     * <ul>
     * <li><code>c == 0</code>: returns 0;
     * <li><code>c == 1</code>: returns delta
     * <li><code>c &gt;= cmax</code>: returns max; max <= 1 (1 if saturated)
     * </ul>
     *
     * @param   c  count value, in range 0-max
     * @return  scaled value in range 0-1, as long as <code>c</code> is in range
     */
    public abstract double scale( int c );

    /**
     * Creates an asinh-like scaler.
     * The function starts off linear, and stays linear at least until c = 1,
     * and then turns over to hit 1 at cmax; if it can stay linear all
     * the way it does (and probably never hits the maximum value).
     *
     * @param   cmax is the maximum value to be scaled
     * @param  delta is the output for <code>c = 1</code>
     * @return   new scaler
     */
    public static CountScaler createScaler( int cmax, double delta ) {
        CountScaler basicScaler = cmax * delta <= 1
                                ? new LinearScaler( delta )
                                : new AsinhScaler( delta, cmax );
        return new PinScaler( basicScaler );
    }

    /**
     * Wrapper scaler that pins the result to the range 0-1.
     */
    private static class PinScaler extends CountScaler {
        private final CountScaler baseScaler_;

        /**
         * Constructor.
         *
         * @param  baseScaler  scaler to wrap
         */
        PinScaler( CountScaler baseScaler ) {
            baseScaler_ = baseScaler;
        }
        public double scale( int c ) {
            return Math.max( 0, Math.min( 1, baseScaler_.scale( c ) ) );
        }
    }

    /**
     * Linear value scaler.
     */
    private static class LinearScaler extends CountScaler {
        private final double delta_;

        /**
         * Constructor.
         *
         * @param  delta  output difference for unit input difference
         */
        LinearScaler( double delta ) {
            delta_ = delta;
        }
        public double scale( int c ) {
            return c * delta_;
        }
    }

    /**
     * Scaler which uses the asinh function.
     * It starts off linear and then transitions smoothly to logarithmic.
     */
    private static class AsinhScaler extends CountScaler {
        private final double u_;
        private final double v_;

        /**
         * Constructs an AsinhScaler from constraints.
         *
         * @param   cmax is the maximum value to be scaled
         * @param  delta is the output for <code>c = 1</code>
         * @return   new scaler
         */
        AsinhScaler( double delta, int cmax ) {
            this( calcCoeffs( delta, cmax ) );
            assert scale( 0 ) == 0;
            assert PlotUtil.approxEquals( delta, scale( 1 ) );
            assert scale( cmax ) <= 1;
        }

        /**
         * Constructs an Asinh scaler from calculation coefficients.
         *
         * @param   coeffs  2-element array:
         *                  scaler of output, scaler of argument
         */
        private AsinhScaler( double[] coeffs ) {
            u_ = coeffs[ 0 ];
            v_ = coeffs[ 1 ];
        }

        public double scale( int c ) {
            return u_ * Maths.asinh( v_ * c );
        }

        /**
         * Calculates scaling coefficients for use of the asinh function
         * given the constraints on f(1), and f^-1(1).
         *
         * @param   cmax is the maximum value to be scaled
         * @param  delta is the output for <code>c = 1</code>
         * @return  2-element array: scaler of output, scaler of argument
         */
        private static double[] calcCoeffs( double delta, int cmax ) {

            /* Solve v numerically for sinh(delta*asinh(v*cmax))-v=0
             * using Newton's method.  There may be an analytic way of
             * doing this, but I couldn't work one out. */
            double v0 = 1;
            boolean done = false;
            while ( ! done ) {
                double[] derivs = calcDerivsV( v0, cmax, delta );
                double v1 = v0 - derivs[ 0 ] / derivs[ 1 ];
                done = Math.abs( v1 - v0 ) < 1e-14;
                v0 = v1;
            }
            double v = v0;

            /* Calculate u from v. */
            double u = 1.0 / Maths.asinh( v * cmax );
            return new double[] { u, v };
        }

        /**
         * Calculate zeroth and first derivatives of the function whose root
         * is the V coefficient for Newton's method.
         *
         * @param  v  approximation for V
         * @param   cmax is the maximum value to be scaled
         * @param  delta is the output for <code>c = 1</code>
         * @return  2-element array (zeroth deriv, first deriv)
         */
        private static double[] calcDerivsV( double v, int cmax,
                                             double delta ) {
            double d0 = Maths.sinh( delta * Maths.asinh( v * cmax ) ) - v;
            double d1 = Maths.cosh( delta * Maths.asinh( v * cmax ) ) 
                      * delta * cmax / Math.hypot( v * cmax, 1 ) - 1;
            return new double[] { d0, d1 };
        }
    }

    /**
     * Test.
     */
    public static void main( String[] args ) {
        int cmax = 10;
        double delta = .1;
        CountScaler scaler = createScaler( cmax, delta );
        int[] cs = new int[] { -1, 0, 1, cmax/2, cmax, cmax+1 };
        for ( int i = 0; i < cs.length; i++ ) {
            int c = cs[ i ];
            System.out.println( "f(" + c + ") = " + scaler.scale( c ) );
        }
    }
}
