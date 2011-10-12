// The doc comments in this class are processed to produce user-visible
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical.

package uk.ac.starlink.ttools.func;

/**
 * Standard trigonometric functions with angles in degrees.
 *
 * @author   Mark Taylor (Starlink)
 * @since    2 Sep 2004
 */
public class TrigDegrees {

    /**
     * Private constructor prevents instantiation.
     */
    private TrigDegrees() {
    }

    /**
     * Sine of an angle.
     *
     * @param   theta   an angle, in degrees
     * @return  the sine of the argument
     */
    public static double sinDeg(double theta) {
        return Math.sin( Math.toRadians( theta ) );
    }

    /**
     * Cosine of an angle.
     *
     * @param   theta  an angle, in degrees
     * @return  the cosine of the argument
     */
    public static double cosDeg(double theta) {
        return Math.cos( Math.toRadians( theta ) );
    }

    /**
     * Tangent of an angle.
     *
     * @param   theta   an angle, in degrees
     * @return  the tangent of the argument.
     */
    public static double tanDeg(double theta) {
        return Math.tan( Math.toRadians( theta ) );
    }

    /**
     * Arc sine.
     * The result is in the range of -90 through 90.
     *
     * @param   x   the value whose arc sine is to be returned.
     * @return  the arc sine of the argument in degrees
     */
    public static double asinDeg(double x) {
        return Math.toDegrees( Math.asin( x ) );
    }

    /**
     * Arc cosine.
     * The result is in the range of 0.0 through 180.
     *
     * @param   x   the value whose arc cosine is to be returned.
     * @return  the arc cosine of the argument in degrees
     */
    public static double acosDeg(double x) {
        return Math.toDegrees( Math.acos( x ) );
    }

    /**
     * Arc tangent.
     * The result is in the range of -90 through 90.
     *
     * @param   x   the value whose arc tangent is to be returned.
     * @return  the arc tangent of the argument in degrees
     */
    public static double atanDeg(double x) {
        return Math.toDegrees( Math.atan( x ) );
    }

    /**
     * Converts rectangular coordinates (<code>x</code>,<code>y</code>)
     * to polar (<code>r</code>,<code>theta</code>).
     * This method computes the phase 
     * <code>theta</code> by computing an arc tangent
     * of <code>y/x</code> in the range of -180 to 180.
     *
     * @param   y   the ordinate coordinate
     * @param   x   the abscissa coordinate
     * @return  the <code>theta</code> component in degrees of the point
     *          (<code>r</code>,<code>theta</code>)
     *          in polar coordinates that corresponds to the point
     *          (<code>x</code>,<code>y</code>) in Cartesian coordinates.
     */
    public static double atan2Deg(double y, double x) {
        return Math.toDegrees( Math.atan2( y, x ) );
    }
}
