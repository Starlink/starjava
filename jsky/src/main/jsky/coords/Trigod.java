/*
 * ESO Archive
 *
 * $Id: Trigod.java,v 1.2 2002/07/09 13:30:36 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/10  port of trigo.c, Francois Ochsenbein [ESO-IPG]
 */

package jsky.coords;

/**
 * Trigonometric Function in degrees.
 * <p>
 * This class is based on C routintes by Francois Ochsenbein [ESO-IPG].
 */
public class Trigod {

    /** max floating value */
    static final double DOUBLE_MAX = 1.7e38;

    /** radians to degrees */
    static final double DEG = (180.e0 / Math.PI);

    /**
     * Return the fractional part of d.
     */
    private static double fractionalPart(double d) {
        return Math.abs(d - (int) d);
    }

    /**
     * Computation of cosine (argument in degrees).
     * @param x argument in degrees
     * @return cosine (double).
     */
    public static double cosd(double x) {
        int sign = 0;
        double argument = fractionalPart(x / 360.e0);
        if (argument > .5e0)
            argument = 1.e0 - argument;
        if (argument > .25e0) {
            argument = .5e0 - argument;
            sign = 1;
        }
        if (argument > .125e0)
            argument = Math.sin((Math.PI * 2) * (.25e0 - argument));
        else
            argument = Math.cos((Math.PI * 2) * argument);
        if (sign != 0)
            argument = -argument;
        return argument;
    }

    /**
     * Computes the sine (argument in degrees)
     * @return sine of argument (double)
     */
    public static double sind(double x) {
        double argument = fractionalPart(x / 360.e0);
        int sign = (x >= 0.e0 ? 0 : 1);
        if (argument > .5e0) {
            argument = 1.e0 - argument;
            sign ^= 1;
        }
        if (argument > .25e0)
            argument = .5e0 - argument;
        if (argument > .125e0)
            argument = Math.cos((Math.PI * 2) * (.25e0 - argument));
        else
            argument = Math.sin((Math.PI * 2) * argument);
        if (sign != 0)
            argument = -argument;
        return argument;
    }


    /**
     * Computes the tangent (argument in degrees).
     * For +90 degrees, DOUBLE_MAX is returned;
     * For -90 degrees, -DOUBLE_MAX is returned
     * @return tangent of argument (double)
     */
    public static double tand(double x) {
        double argument = fractionalPart(x / 180.e0);
        if (argument == .5e0)
            argument = DOUBLE_MAX;
        else
            argument = Math.tan(Math.PI * argument);
        return (x > 0.e0 ? argument: -argument);
    }

    /*
     * Computes the Arc tan in degrees.
     * @return Arc tangent of argument (double), in range [-90, 90].
     */
    public static double atand(double x) {
        return (DEG * Math.atan(x));
    }

    /**
     * Cartesian to polar.
     * @param x X argument in degrees.
     * @param y Y argument in degrees.
     * @return Angle in range [-180, 180].
     */
    public static double atan2d(double x, double y) {
        return (DEG * Math.atan2(x, y));
    }

    /**
     * Computes the Arc cos in degrees (Range of argument [-1, +1]).
     * @return Arc cosine of argument (double), in range [0, 180].
     */
    public static double acosd(double x) {
        return (DEG * Math.acos(x));
    }

    /**
     * Computes the Arc sine in degrees (Range of argument [-1, +1]).
     * @return Arc tangent of argument (double) in range [-90, 90].
     */
    double asind(double x) {
        return (DEG * Math.asin(x));
    }
}
