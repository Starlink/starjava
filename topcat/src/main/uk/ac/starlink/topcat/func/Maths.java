package uk.ac.starlink.topcat.func;

/**
 * Standard mathematical and trigonometric functions.
 * Most of the functionality here is taken directly from the
 * {@link java.lang.Math} class - the documentation here has been simplified
 * somewhat to make it easier to read.
 *
 * @author   Mark Taylor (Starlink)
 * @since    2 Sep 2004
 */
public class Maths {

    /**
     * The <code>double</code> value that is closer than any other to
     * <i>e</i>, the base of the natural logarithms.
     */
    public static final double E = 2.7182818284590452354;

    /**
     * The <code>double</code> value that is closer than any other to
     * <i>pi</i>, the ratio of the circumference of a circle to its
     * diameter.
     */
    public static final double PI = 3.14159265358979323846;

    private static double LOG10_FACTOR = 1.0 / Math.log( 10.0 );


    /**
     * Returns the trigonometric sine of an angle.
     *
     * @param   theta   an angle, in radians.
     * @return  the sine of the argument.
     */
    public static double sin(double theta) {
        return Math.sin( theta );
    }

    /**
     * Returns the trigonometric cosine of an angle.
     *
     * @param   theta   an angle, in radians.
     * @return  the cosine of the argument.
     */
    public static double cos(double theta) {
        return Math.cos( theta );
    }

    /**
     * Returns the trigonometric tangent of an angle.
     *
     * @param   theta   an angle, in radians.
     * @return  the tangent of the argument.
     */
    public static double tan(double theta) {
        return Math.tan( theta );
    }

    /**
     * Returns the arc sine of an angle, in the range of -<i>pi</i>/2 through
     * <i>pi</i>/2.
     *
     * @param   x   the value whose arc sine is to be returned.
     * @return  the arc sine of the argument.
     */
    public static double asin(double x) {
        return Math.asin( x );
    }

    /**
     * Returns the arc cosine of an angle, in the range of 0.0 through
     * <i>pi</i>.
     *
     * @param   x   the value whose arc cosine is to be returned.
     * @return  the arc cosine of the argument.
     */
    public static double acos(double x) {
        return Math.acos( x );
    }

    /**
     * Returns the arc tangent of an angle, in the range of -<i>pi</i>/2
     * through <i>pi</i>/2.
     *
     * @param   x   the value whose arc tangent is to be returned.
     * @return  the arc tangent of the argument.
     */
    public static double atan(double x) {
        return Math.atan( x );
    }

    /**
     * Returns Euler's number <i>e</i> raised to the power of a
     * <code>double</code> value.
     *
     * @param   x   the exponent to raise <i>e</i> to.
     * @return  the value <i>e</i><sup><code>x</code></sup>,
     *          where <i>e</i> is the base of the natural logarithms.
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
     * Returns the correctly rounded positive square root of a
     * <code>double</code> value.
     *
     * @param   x   a value.
     * @return  the positive square root of <code>x</code>.
     *          If the argument is NaN or less than zero, the result is NaN.
     */
    public static double sqrt(double x) {
        return Math.sqrt( x );
    }

    /**
     * Converts rectangular coordinates (<code>x</code>,&nbsp;<code>y</code>)
     * to polar (r,&nbsp;<i>theta</i>).
     * This method computes the phase <i>theta</i> by computing an arc tangent
     * of <code>y/x</code> in the range of -<i>pi</i> to <i>pi</i>.
     *
     * @param   y   the ordinate coordinate
     * @param   x   the abscissa coordinate
     * @return  the <i>theta</i> component of the point
     *          (<i>r</i>,&nbsp;<i>theta</i>)
     *          in polar coordinates that corresponds to the point
     *          (<i>x</i>,&nbsp;<i>y</i>) in Cartesian coordinates.
     */
    public static double atan2(double y, double x) {
        return Math.atan2( y, x );
    }

    /**
     * Returns of value of the first argument raised to the power of the
     * second argument.
     *
     * @param   a   the base.
     * @param   b   the exponent.
     * @return  the value <code>a<sup>b</sup></code>.
     */
    public static double pow(double a, double b) {
        return Math.pow( a, b );
    }
}
