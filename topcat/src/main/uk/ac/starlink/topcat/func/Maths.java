// The doc comments in this class are processed to produce user-visible
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical.

package uk.ac.starlink.topcat.func;

/**
 * Standard mathematical and trigonometric functions.
 *
 * @author   Mark Taylor (Starlink)
 * @since    2 Sep 2004
 */
public class Maths {

    /**
     * The <code>double</code> value that is closer than any other to
     * <em>e</em>, the base of the natural logarithms.
     */
    public static final double E = 2.7182818284590452354;

    /**
     * The <code>double</code> value that is closer than any other to
     * <em>pi</em>, the ratio of the circumference of a circle to its
     * diameter.
     */
    public static final double PI = 3.14159265358979323846;

    private static double LOG10_FACTOR = 1.0 / Math.log( 10.0 );


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
     * The results is in the range of -<em>pi</em>/2 through <em>pi</em>/2.
     *
     * @param   x   the value whose arc tangent is to be returned.
     * @return  the arc tangent of the argument (radians)
     */
    public static double atan(double x) {
        return Math.atan( x );
    }

    /**
     * Euler's number <em>e</em> raised to a power.
     *
     * @param   x   the exponent to raise <em>e</em> to.
     * @return  the value <em>e</em><sup><code>x</code></sup>,
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
        return LOG10_FACTOR * Math.log( x );
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
     * Square root.  
     * The result is correctly rounded and positive.
     *
     * @param   x   a value.
     * @return  the positive square root of <code>x</code>.
     *          If the argument is NaN or less than zero, the result is NaN.
     */
    public static double sqrt(double x) {
        return Math.sqrt( x );
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
}
