package uk.ac.starlink.ttools.func;

/**
 * Calculates integrals of a given function, using interpolation
 * (rectangle method).
 *
 * @author   Mark Taylor
 * @since    26 Jan 2007
 */
abstract class Integrator {

    private final double base_;
    private final double step_;
    private double[] plusIntegrals_;
    private double[] minusIntegrals_;
    private static final double OVER_CALC = 0.2;

    /**
     * Constructor.
     * The step value gives the interval which will be used for interpolation
     * (width of rectangles).
     * The base is the lower limit for all definite integrations 
     * which this object can perform;
     * for performance reasons, it should also be a value near (in terms of
     * the step) to which all the evaluations will be carried out.
     *
     * @param   base  limit for definite integrations
     * @param   step  interpolation step
     */
    public Integrator( double base, double step ) {
        base_ = base;
        step_ = step;
        plusIntegrals_ = new double[] { 0. };
        minusIntegrals_ = new double[] { 0. };
    }

    /**
     * Provides the function which is to be integrated.
     *
     * @param   x  independent variable
     * @return   dependent variable
     */
    public abstract double function( double x );

    /**
     * Returns an approximation to the definite integral between this 
     * integrator's base value and a given value.
     *
     * @param  x  upper limit of definite integral
     * @return   definite integral of <code>function</code> from
     *           <code>base</code> to <code>x</code>
     */
    public double integral( double x ) {
        if ( x == base_ ) {
            return 0;
        }
        else if ( x > base_ ) {
            double point = ( x - base_ ) / step_;
            int p0 = (int) point;
            int p1 = p0 + 1;
            double[] plusInts = getPlusIntegrals( p1 );
            double v0 = plusInts[ p0 ];
            double v1 = plusInts[ p1 ];
            return v0 + ( point - p0 ) * ( v1 - v0 );
        }
        else if ( x < base_ ) {
            double point = ( base_ - x ) / step_;
            int p0 = (int) point;
            int p1 = p0 + 1;
            double[] minusInts = getMinusIntegrals( p1 );
            double v0 = minusInts[ p0 ];
            double v1 = minusInts[ p1 ];
            return v0 - ( point - p0 ) * ( v0 - v1 );
        }
        else {
            assert Double.isNaN( x );
            return Double.NaN;
        }
    }

    /**
     * Returns an array of integrated values which covers at least the 
     * range <code>base...base+ip*step</code> inclusive.
     *
     * @param  ip  minimum size of array in steps
     * @return   array of integral values starting from base
     */
    private double[] getPlusIntegrals( int ip ) {
        double[] ints0 = plusIntegrals_;
        int n0 = ints0.length;
        if ( ip < n0 ) {
            return ints0;
        }
        else {
            int n1 = (int) ( ip * ( 1.0 + OVER_CALC ) ) + 1;
            double[] ints1 = new double[ n1 ];
            System.arraycopy( ints0, 0, ints1, 0, n0 );
            for ( int i = n0; i < n1; i++ ) {
                ints1[ i ] = ints1[ i - 1 ]
                           + step_ * function( base_ + ( i - 0.5 ) * step_ );
            }
            synchronized ( this ) {
                plusIntegrals_ = ints1;
            }
            return ints1;
        }
    }

    /**
     * Returns an array of integrated values which covers at least the
     * range <code>base-ip*step...step</code> inclusive.
     *
     * @param   ip  minimum size of array in steps
     * @return   array of integral values starting from base
     */
    private double[] getMinusIntegrals( int ip ) {
        double[] ints0 = minusIntegrals_;
        int n0 = ints0.length;
        if ( ip < n0 ) {
            return ints0;
        }
        else {
            int n1 = (int) ( ip * ( 1.0 + OVER_CALC ) ) + 1;
            double[] ints1 = new double[ n1 ];
            System.arraycopy( ints0, 0, ints1, 0, n0 );
            for ( int i = n0; i < n1; i++ ) {
                ints1[ i ] = ints1[ i - 1 ]
                           - step_ * function( base_ - ( i - 0.5 ) * step_ );
            }
            synchronized ( this ) {
                minusIntegrals_ = ints1;
            }
            return ints1;
        }
    }
}
