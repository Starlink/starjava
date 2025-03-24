// The doc comments in this class are processed to produce user-visible
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical.

package uk.ac.starlink.ttools.func;

/**
 * Standard mathematical and trigonometric functions.
 * Trigonometric functions work with angles in radians.
 *
 * @author   Mark Taylor (Starlink)
 * @since    2 Sep 2004
 */
public class Maths {

    /**
     * Euler's number <em>e</em>, the base of natural logarithms.
     */
    public static final double E = 2.7182818284590452354;

    /**
     * <em>Pi</em>, the ratio of the circumference of a circle to its diameter.
     */
    public static final double PI = 3.14159265358979323846;

    /**
     * Positive infinite floating point value.
     */
    public static final double Infinity = Double.POSITIVE_INFINITY;

    /**
     * Not-a-Number floating point value.
     * Use with care; arithmetic and logical operations behave in strange
     * ways near NaN (for instance, <code>NaN!=NaN</code>).
     * For most purposes this is equivalent to the blank value.
     */
    public static final double NaN = Double.NaN;

    /**
     * Private constructor prevents instantiation.
     */
    private Maths() {
    }

    /**
     * Sine of an angle.
     *
     * @param   theta   an angle, in radians.
     * @return  the sine of the argument.
     */
    public static double sin(double theta) {
        return Math.sin( theta );
    }

    /**
     * Cosine of an angle.
     *
     * @param   theta   an angle, in radians.
     * @return  the cosine of the argument.
     */
    public static double cos(double theta) {
        return Math.cos( theta );
    }

    /**
     * Tangent of an angle.
     *
     * @param   theta   an angle, in radians.
     * @return  the tangent of the argument.
     */
    public static double tan(double theta) {
        return Math.tan( theta );
    }

    /**
     * Arc sine of an angle. 
     * The result is in the range of -<em>pi</em>/2 through
     * <em>pi</em>/2.
     *
     * @param   x   the value whose arc sine is to be returned.
     * @return  the arc sine of the argument (radians)
     */
    public static double asin(double x) {
        return Math.asin( x );
    }

    /**
     * Arc cosine of an angle.  
     * The result is in the range of 0.0 through <em>pi</em>.
     *
     * @param   x   the value whose arc cosine is to be returned.
     * @return  the arc cosine of the argument (radians)
     */
    public static double acos(double x) {
        return Math.acos( x );
    }

    /**
     * Arc tangent of an angle.
     * The result is in the range of -<em>pi</em>/2 through <em>pi</em>/2.
     *
     * @param   x   the value whose arc tangent is to be returned.
     * @return  the arc tangent of the argument (radians)
     */
    public static double atan(double x) {
        return Math.atan( x );
    }

    /**
     * Natural logarithm.
     *
     * @param  x  argument
     * @return   log<sub>e</sub>(x)
     */
    public static double ln( double x ) {
        return Math.log( x );
    }

    /**
     * Euler's number <em>e</em> raised to a power.
     *
     * @param   x   the exponent to raise <em>e</em> to.
     * @return  the value <em>e</em><sup>x</sup>,
     *          where <em>e</em> is the base of the natural logarithms.
     */
    public static double exp(double x) {
        return Math.exp( x );
    }

    /**
     * Logarithm to base 10.
     *
     * @param  x  argument
     * @return   log<sub>10</sub>(x)
     */
    public static double log10( double x ) {
        return Math.log10( x );
    }

    /**
     * Power of 10.
     * This convenience function is identical to <code>pow(10,x)</code>.
     *
     * @param  x  argument
     * @return  10<sup>x</sup>
     */
    public static double exp10( double x ) {
        return Math.pow( 10, x );
    }

    /**
     * Square root.  
     * The result is correctly rounded and positive.
     *
     * @param   x   a value.
     * @return  the positive square root of <code>x</code>.
     *          If the argument is NaN or less than zero, the result is NaN.
     */
    public static double sqrt( double x ) {
        return Math.sqrt( x );
    }

    /**
     * Raise to the power 2.
     *
     * @param   x  a value
     * @return  x * x
     */
    public static double square( double x ) {
        return x * x;
    }

    /**
     * Returns the square root of the sum of squares of its arguments.
     * In the 2-argument case, doing it like this may avoid intermediate
     * overflow or underflow.
     *
     * @example <code>hypot(3,4) = 5</code>
     * @example <code>hypot(2,2,2,2) = 4</code>
     *
     * @param   xs  one or more numeric values
     * @return  sqare root of sum of squares of arguments
     */
    public static double hypot( double... xs ) {
        switch ( xs.length ) {
            case 0:
                return 0;
            case 1:
                return xs[ 0 ];
            case 2:
                return Math.hypot( xs[ 0 ], xs[ 1 ] );
            default:
                double s2 = 0;
                for ( double x : xs ) {
                    s2 += x * x;
                }
                return Math.sqrt( s2 );
        }
    }

    /**
     * Converts rectangular coordinates (<code>x</code>,<code>y</code>)
     * to polar (<code>r</code>,<code>theta</code>).
     * This method computes the phase 
     * <code>theta</code> by computing an arc tangent
     * of <code>y/x</code> in the range of -<em>pi</em> to <em>pi</em>.
     *
     * @param   y   the ordinate coordinate
     * @param   x   the abscissa coordinate
     * @return  the <code>theta</code> component (radians) of the point
     *          (<code>r</code>,<code>theta</code>)
     *          in polar coordinates that corresponds to the point
     *          (<code>x</code>,<code>y</code>) in Cartesian coordinates.
     */
    public static double atan2(double y, double x) {
        return Math.atan2( y, x );
    }

    /**
     * Exponentiation. 
     * The result is the value of the first argument raised to 
     * the power of the second argument.
     *
     * @param   a   the base.
     * @param   b   the exponent.
     * @return  the value <code>a<sup>b</sup></code>.
     */
    public static double pow(double a, double b) {
        return Math.pow( a, b );
    }

    /**
     * Hyperbolic sine.
     *
     * @param   x  parameter
     * @return  result
     */
    public static double sinh( double x ) {
        return 0.5 * ( Math.exp( x ) - Math.exp( -x ) );
    }

    /**
     * Hyperbolic cosine.
     *
     * @param   x  parameter
     * @return  result
     */
    public static double cosh( double x ) {
        return 0.5 * ( Math.exp( x ) + Math.exp( -x ) );
    }

    /**
     * Hyperbolic tangent.
     *
     * @param  x  parameter
     * @return  result
     */
    public static double tanh( double x ) {
        return ( Math.exp( x ) - Math.exp( -x ) )
             / ( Math.exp( x ) + Math.exp( -x ) );
    }

    /**
     * Inverse hyperbolic sine.
     *
     * @param  x  parameter
     * @return result
     */
    public static double asinh( double x ) {

        /* The first expression here is mathematically correct,
         * but experiences numerical difficulties for large negative x,
         * so take advantage of the fact that the function is antisymmetric. */
        return ! ( x < 0 ) ? Math.log( x + Math.sqrt( x * x + 1 ) )
                           : - asinh( -x );
    }

    /**
     * Inverse hyperbolic cosine.
     *
     * @param  x  parameter
     * @return   result
     */
    public static double acosh( double x ) {
        return Math.log( x + Math.sqrt( x * x - 1 ) );
    }

    /**
     * Inverse hyperbolic tangent.
     *
     * @param   x  parameter
     * @return  result
     */
    public static double atanh( double x ) {
        return 0.5 * Math.log( ( 1 + x ) / ( 1 - x ) );
    }
}
