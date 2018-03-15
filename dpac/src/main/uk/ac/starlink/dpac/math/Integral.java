package uk.ac.starlink.dpac.math;

/**
 * Performs indefinite numerical integration using an adaptive Simpson's rule.
 * An instance of this class represents the definite integral over a
 * given interval, but it provides methods that permit adaptive
 * integration and the extraction of (a numerical form) of the antiderivative
 * function.
 *
 * @author   Mark Taylor
 * @since    15 Mar 2018
 */
public class Integral {

    private static final double SIXTH = 1.0 / 6.0;

    private final Function f_;
    private final double lo_;
    private final double hi_;
    private final double mid_;
    private final double flo_;
    private final double fhi_;
    private final double fmid_;
    private final double area_;
    private Integral[] subs_;

    /**
     * Constructs an integral with given bounds and supplied function
     * evaluations.
     *
     * @param   f   function to integrate
     * @param   lo  lower bound of integration interval
     * @param   hi  upper bound of integration interval
     * @param   flo  function evaluattion at <code>lo</code>
     * @param   fhi  function evaluattion at <code>hi</code>
     */
    public Integral( Function f, double lo, double hi,
                     double flo, double fhi ) {
        if ( ! ( lo <= hi ) ) {
            throw new IllegalArgumentException( lo + ">=" + hi );
        }
        f_ = f;
        lo_ = lo;
        hi_ = hi;
        mid_ = 0.5 * ( lo + hi );
        flo_ = flo;
        fhi_ = fhi;
        fmid_ = f.f( mid_ );
        area_ = SIXTH * ( hi_ - lo_ ) * ( flo_ + 4.0 * fmid_ + fhi_ );
    }

    /**
     * Constructs an integral with given bounds.
     *
     * @param   f   function to integrate
     * @param   lo  lower bound of integration interval
     * @param   hi  upper bound of integration interval
     */
    public Integral( Function f, double lo, double hi ) {
        this( f, lo, hi, f.f( lo ), f.f( hi ) );
    }

    /**
     * Constructs an integral with a number of sub-integrals
     * covering the interval of interest.
     *
     * @param   f   function to integrate
     * @param   subs  contiguous sub-intervals;
     *                it is the responsibility of the caller to
     *                populate these with their own recursive sub-intervals
     *                as required
     */
    public Integral( Function f, Integral[] subs ) {
        this( f, subs[ 0 ].lo_, subs[ subs.length - 1 ].hi_ );
        for ( int i = 1; i < subs.length; i++ ) {
            if ( subs[ i - 1 ].hi_ != subs[ i ].lo_ ) {
                throw new IllegalArgumentException();
            }
        }
        subs_ = subs;
    }

    /**
     * Recursively populates the child nodes of this integral so as
     * to achieve a given tolerance in calculation accuracy.
     *
     * @param  tol  tolerance
     */
    public void fill( double tol ) {
        Integral sub1 = new Integral( f_, lo_, mid_, flo_, fmid_ );
        Integral sub2 = new Integral( f_, mid_, hi_, fmid_, fhi_ );
        subs_ = new Integral[] { sub1, sub2 };
        if ( Math.abs( sub1.area_ + sub2.area_ - area_ ) > tol ) {
            sub1.fill( tol );
            sub2.fill( tol );
        }
    }

    /**
     * Returns the number of nodes whose recursive data provides the
     * output of this integral function.
     */
    public int count() {
        if ( subs_ == null ) {
            return 1;
        }
        else {
            int n = 0;
            for ( Integral sub : subs_ ) {
                n += sub.count();
            }
            return n;
        }
    }

    /**
     * Flattens the recursive information contained by this integral
     * into a pair of arrays represting (x,y) samples giving the
     * antiderivative function.
     *
     * @param   x   array to populate with X values
     * @param   y   array to populate with Y (integrated) values
     * @param   index   index of the <code>x</code> and <code>y</code>
     *                  arrays from which the results should be inserted
     * @return   number of samples inserted
     */
    public int store( double[] x, double[] y, int index ) { 
        if ( subs_ != null ) {
            int n = 0;
            for ( Integral sub : subs_ ) {
                n += sub.store( x, y, index + n );
            }
            return n;
        }
        else {
            x[ index ] = hi_;
            y[ index ] = area_;
            return 1;
        }
    }

    /**
     * Adaptively integrates a given function with the calculation
     * broken up into sub-intervals at supplied points.
     * The result will be aggregated from sub-integrals with boundaries
     * at each of the X points supplied, and further broken up within
     * each such sub-region as the adaptive algorithm requires to
     * reach the supplied tolerance.
     *
     * @param   f  function to integrate
     * @param   points   ordered list of samples on X axis at which
     *                   evaluations will be forced
     * @param   tol   tolerance parameter controlling recursion
     * @return   numeric function giving integral of <code>f</code>
     *           over the range given by the first and last elements
     *           of <code>points</code>
     */
    public static NumericFunction integrate( Function f, double[] points,
                                             double tol ) {

        /* Perform adaptive quadrature on each requested interval. */
        int nSect = points.length - 1;
        Integral[] integs = new Integral[ nSect ];
        for ( int i = 0; i < nSect; i++ ) {
            integs[ i ] = new Integral( f, points[ i ], points[ i + 1 ] );
            integs[ i ].fill( tol );
        }

        /* Flatten the results to an x/y array pair. */
        Integral root = new Integral( f, integs );
        int nNode = root.count();
        final int nNode1 = nNode + 1;
        final double[] x = new double[ nNode1 ];
        final double[] y = new double[ nNode1 ];
        x[ 0 ] = 0;
        y[ 0 ] = 0;
        int nn = root.store( x, y, 1 );
        assert nn == nNode;

        /* Cumulate. */
        for ( int i = 1; i < nNode1; i++ ) {
            y[ i ] += y[ i - 1 ];
        }

        /* Return as a numeric function. */
        return new NumericFunction() {
            public int getCount() {
                return nNode1;
            }
            public double getX( int i ) {
                return x[ i ];
            }
            public double getY( int i ) {
                return y[ i ];
            }
        };
    }
}
