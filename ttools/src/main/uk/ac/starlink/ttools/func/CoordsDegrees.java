// The doc comments in this class are processed to produce user-visible
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical.

package uk.ac.starlink.ttools.func;

/**
 * Functions for angle transformations and manipulations, with angles
 * generally in degrees.
 * In particular, methods for translating between degrees and HH:MM:SS.S
 * or DDD:MM:SS.S type sexagesimal representations are provided.
 *
 * @author   Mark Taylor
 * @since    11 Oct 2011
 */
public class CoordsDegrees {

    /**
     * Private constructor prevents instantiation.
     */
    private CoordsDegrees() {
    }

    /**
     * Converts an angle in degrees to a formatted degrees:minutes:seconds
     * string.  No fractional part of the seconds field is given.
     *
     * @param  deg  angle in degrees
     * @return  DMS-format string representing <code>deg</code>
     */
    public static String degreesToDms( double deg ) {
        return CoordsRadians.radiansToDms( degreesToRadians( deg ) );
    }

    /**
     * Converts an angle in degrees to a formatted degrees:minutes:seconds
     * string with a given number of decimal places in the seconds field.
     *
     * @param  deg  angle in degrees
     * @param  secFig  number of decimal places in the seconds field
     * @return  DMS-format string representing <code>deg</code>
     */
    public static String degreesToDms( double deg, int secFig ) {
        return CoordsRadians.radiansToDms( degreesToRadians( deg ), secFig );
    }

    /**
     * Converts an angle in degrees to a formatted hours:minutes:seconds
     * string.  No fractional part of the seconds field is given.
     *
     * @param  deg  angle in degrees
     * @return  HMS-format string representing <code>deg</code>
     */
    public static String degreesToHms( double deg ) {
        return CoordsRadians.radiansToHms( degreesToRadians( deg ) );
    }

    /**
     * Converts an angle in degrees to a formatted hours:minutes:seconds
     * string with a given number of decimal places in the seconds field.
     *
     * @param  deg  angle in degrees
     * @param  secFig  number of decimal places in the seconds field
     * @return  HMS-format string representing <code>deg</code>
     */
    public static String degreesToHms( double deg, int secFig ) {
        return CoordsRadians.radiansToHms( degreesToRadians( deg ), secFig );
    }

    /**
     * Converts a formatted degrees:minutes:seconds string to an angle
     * in degrees.  Delimiters may be colon, space, characters
     * <code>dm[s]</code>, or some others.
     * Additional spaces and leading +/- are permitted.
     * The :seconds part is optional.
     *
     * @param  dms  formatted DMS string
     * @return  angle in degrees specified by <code>dms</code>
     * @throws  NumberFormatException  if <code>dms</code> can't be parsed as
     *          a degrees:minutes:seconds string
     */
    public static double dmsToDegrees( String dms ) {
        return radiansToDegrees( CoordsRadians.dmsToRadians( dms ) );
    }

    /**
     * Converts a formatted hours:minutes:seconds string to an angle
     * in degrees.  Delimiters may be colon, space, characters
     * <code>hm[s]</code>, or some others.
     * Additional spaces and leading +/- are permitted.
     * The :seconds part is optional.
     *
     * @param  hms  formatted HMS string
     * @return  angle in degrees specified by <code>hms</code>
     * @throws  NumberFormatException  if <code>dms</code> can't be parsed as
     *          an hours:minutes:seconds string
     */
    public static double hmsToDegrees( String hms ) {
        return radiansToDegrees( CoordsRadians.hmsToRadians( hms ) );
    }

    /**
     * Converts degrees, minutes, seconds to an angle in degrees.
     *
     * <p>In conversions of this type, one has to be careful to get the
     * sign right in converting angles which are between 0 and -1 degrees.
     * This routine uses the sign bit of the <code>deg</code> argument,
     * taking care to distinguish between +0 and -0 (their internal
     * representations are different for floating point values).
     * It is illegal for the <code>min</code> or <code>sec</code> arguments
     * to be negative.
     *
     * @param  deg  degrees part of angle
     * @param  min  minutes part of angle
     * @param  sec  seconds part of angle
     * @return  specified angle in degrees
     * @throws   IllegalArgumentException if an argument after the first
     *           non-zero one is negative
     */
    public static double dmsToDegrees( double deg, double min, double sec ) {
        return radiansToDegrees( CoordsRadians.dmsToRadians( deg, min, sec ) );
    }

    /**
     * Converts hours, minutes, seconds to an angle in degrees.
     *
     * <p>In conversions of this type, one has to be careful to get the
     * sign right in converting angles which are between 0 and -1 hours.
     * This routine uses the sign bit of the <code>hour</code> argument,
     * taking care to distinguish between +0 and -0 (their internal
     * representations are different for floating point values).
     *
     * @param  hour  degrees part of angle
     * @param  min  minutes part of angle
     * @param  sec  seconds part of angle
     * @return  specified angle in degrees
     * @throws   IllegalArgumentException if an argument after the first
     *           non-zero one is negative
     */
    public static double hmsToDegrees( double hour, double min, double sec ) {
        return radiansToDegrees( CoordsRadians.hmsToRadians( hour, min, sec ) );
    }

    /**
     * Calculates the separation (distance around a great circle) of
     * two points on the sky in degrees.
     *
     * @param  ra1   right ascension of point 1 in degrees
     * @param  dec1  declination of point 1 in degrees
     * @param  ra2   right ascension of point 2 in degrees
     * @param  dec2  declination of point 2 in degrees
     * @return  angular distance between point 1 and point 2 in degrees
     */
    public static double skyDistanceDegrees( double ra1, double dec1,
                                             double ra2, double dec2 ) {
        return radiansToDegrees(
            CoordsRadians.skyDistanceRadians( degreesToRadians( ra1 ),
                                              degreesToRadians( dec1 ),
                                              degreesToRadians( ra2 ),
                                              degreesToRadians( dec2 ) ) );
    }

    /**
     * Converts degrees to radians.
     *
     * @param  deg   angle in degrees
     * @return   angle in radians
     */
    private static double degreesToRadians( double deg ) {
        return Math.toRadians( deg );
    }

    /**
     * Converts radians to degrees.
     *
     * @param  rad  angle in radians
     * @return  angle in degrees
     */
    private static double radiansToDegrees( double rad ) {
        return Math.toDegrees( rad );
    }
}
