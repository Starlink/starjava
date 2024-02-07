// The doc comments in this class are processed to produce user-visible
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical.

package uk.ac.starlink.ttools.func;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import uk.ac.starlink.pal.AngleDR;
import uk.ac.starlink.pal.Pal;

/**
 * Functions for angle transformations and manipulations, based on
 * radians rather than degrees.
 * In particular, methods for translating between radians and HH:MM:SS.S
 * or DDD:MM:SS.S type sexagesimal representations are provided.
 *
 * @author   Mark Taylor (Starlink)
 * @since    30 Apr 2004
 */
public class CoordsRadians {

    private static final Pattern dmsPattern = 
        getSexPattern( "[:d ]", "[:m' ]", "[s\"]?" );
    private static final Pattern hmsPattern = 
        getSexPattern( "[:h ]", "[:m' ]", "[s\"]?" );
    private static final boolean USE_VINCENTY = false;
   
    /** The size of one degree in radians. */
    public static final double DEGREE_RADIANS = Math.PI / 180;

    /** The size of one hour of right ascension in radians. */
    public static final double HOUR_RADIANS = Math.PI / 180 * 15;

    /** The size of one arcminute in radians. */
    public static final double ARC_MINUTE_RADIANS = Math.PI / 180 / 60;

    /** The size of one arcsecond in radians. */
    public static final double ARC_SECOND_RADIANS = Math.PI / 180 / 60 / 60;

    /**
     * Private constructor prevents instantiation.
     */
    private CoordsRadians() {
    }

    /**
     * Converts an angle in radians to a formatted degrees:minutes:seconds 
     * string.  No fractional part of the seconds field is given.
     *
     * @param  rad  angle in radians
     * @return  DMS-format string representing <code>rad</code>
     */
    public static String radiansToDms( double rad ) {
        return radiansToDms( rad, 0 );
    }

    /**
     * Converts an angle in radians to a formatted degrees:minutes:seconds
     * string with a given number of decimal places in the seconds field.
     *
     * @param  rad  angle in radians
     * @param  secFig  number of decimal places in the seconds field
     * @return  DMS-format string representing <code>rad</code>
     */
    public static String radiansToDms( double rad, int secFig ) {
        if ( Double.isNaN( rad ) ) {
            return null;
        }
        double degrees = radiansToDegrees( rad );
        int sign = degrees >= 0 ? +1 : -1;
        double round = 0.5 / 60.0 / 60.0 * sign;
        for ( int i = 0; i < secFig; i++ ) {
            round *= 0.1; 
        }
        degrees += round;
        degrees *= sign;
        int d = (int) degrees;
        double minutes = ( degrees - d ) * 60.0;
        int m = (int) minutes;
        double seconds = ( minutes - m ) * 60.0;
        return formatDms( sign == 1, d, m, seconds, secFig );
    }

    /**
     * Converts an angle in radians to a formatted hours:minutes:seconds
     * string.  No fractional part of the seconds field is given.
     *
     * @param  rad  angle in radians
     * @return  HMS-format string representing <code>rad</code>
     */
    public static String radiansToHms( double rad ) {
        return radiansToHms( rad, 0 );
    }

    /**
     * Converts an angle in radians to a formatted hours:minutes:seconds
     * string with a given number of decimal places in the seconds field.
     *
     * @param  rad  angle in radians
     * @param  secFig  number of decimal places in the seconds field
     * @return  HMS-format string representing <code>rad</code>
     */
    public static String radiansToHms( double rad, int secFig ) {
        if ( Double.isNaN( rad ) ) {
            return null;
        }
        while ( rad < 0 ) {
            rad += Math.PI * 2;
        }
        double degrees = radiansToDegrees( rad );
        int sign = degrees >= 0 ? +1 : -1;
        double hours = degrees / 15.0;
        double round = 0.5 / 60.0 / 60.0 * sign;
        for ( int i = 0; i < secFig; i++ ) {
            round *= 0.1;
        }
        hours += round;
        hours *= sign;
        int h = (int) hours;
        double minutes = ( hours - h ) * 60.0;
        int m = (int) minutes;
        double seconds = ( minutes - m ) * 60.0;
        return formatHms( sign == 1, h, m, seconds, secFig );
    }

    /**
     * Converts a formatted degrees:minutes:seconds string to an angle
     * in radians.  Delimiters may be colon, space, characters 
     * <code>dm[s]</code>, or some others.
     * Additional spaces and leading +/- are permitted.
     * The :seconds part is optional.
     *
     * @param  dms  formatted DMS string
     * @return  angle in radians specified by <code>dms</code>
     * @throws  NumberFormatException  if <code>dms</code> can't be parsed as
     *          a degrees:minutes:seconds string
     */
    public static double dmsToRadians( String dms ) {
        Matcher matcher = dmsPattern.matcher( dms );
        if ( ! matcher.matches() ) {
            throw new NumberFormatException( dms + 
                                             " not in deg:min:sec format" );
        }
        boolean positive = ! "-".equals( matcher.group( 1 ) );
        int hour = Integer.parseInt( matcher.group( 2 ) );
        int min = Integer.parseInt( matcher.group( 3 ) );
        double sec = matcher.group( 4 ) == null
                   ? 0
                   : Double.parseDouble( matcher.group( 4 ) );
        if ( min < 0 || min > 60 ) {
            return Double.NaN;
        }
        if ( sec < 0 || sec > 60 ) {
            return Double.NaN;
        }
        return dmsToRadians( positive, hour, min, sec );
    }

    /**
     * Converts a formatted hours:minutes:seconds string to an angle
     * in radians.  Delimiters may be colon, space, characters 
     * <code>hm[s]</code>, or some others.
     * Additional spaces and leading +/- are permitted.
     * The :seconds part is optional.
     *
     * @param  hms  formatted HMS string
     * @return  angle in radians specified by <code>hms</code>
     * @throws  NumberFormatException  if <code>dms</code> can't be parsed as
     *          an hours:minutes:seconds string
     */
    public static double hmsToRadians( String hms ) {
        Matcher matcher = hmsPattern.matcher( hms );
        if ( ! matcher.matches() ) {
            throw new NumberFormatException( hms +
                                             " not in hour:min:sec format" );
        }
        boolean positive = ! "-".equals( matcher.group( 1 ) );
        int hour = Integer.parseInt( matcher.group( 2 ) );
        int min = Integer.parseInt( matcher.group( 3 ) );
        double sec = matcher.group( 4 ) == null
                   ? 0
                   : Double.parseDouble( matcher.group( 4 ) );
        if ( min < 0 || min > 60 ) {
            return Double.NaN;
        }
        if ( sec < 0 || sec > 60 ) {
            return Double.NaN;
        }
        return hmsToRadians( positive, hour, min, sec );
    }

    /**
     * Converts degrees, minutes, seconds to an angle in radians.
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
     * @return  specified angle in radians
     * @throws   IllegalArgumentException if an argument after the first
     *           non-zero one is negative
     */
    public static double dmsToRadians( double deg, double min, double sec ) {
        if ( min < 0 || min > 60 || (int) min != min) {
            throw new IllegalArgumentException( 
                          "Minutes argument " + min + 
                          " must be an integer between 0 and 59" );
        }
        if ( sec < 0 || sec > 60 ) {
            throw new IllegalArgumentException( 
                          "Seconds argument " + sec + 
                          " must be between 0 and 60" );
        }
        return dmsToRadians( ! isNegative( deg ), 
                             (int) Math.abs( deg ), (int) min, sec );
    }

    /**
     * Converts hours, minutes, seconds to an angle in radians.
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
     * @return  specified angle in radians
     * @throws   IllegalArgumentException if an argument after the first
     *           non-zero one is negative
     */
    public static double hmsToRadians( double hour, double min, double sec ) {
        if ( min < 0 || min > 60 || (int) min != min ) {
            throw new IllegalArgumentException(
                          "Minutes argument " + min +
                          " must be an integer between 0 and 59" );
        }
        if ( sec < 0 || sec > 60 ) {
            throw new IllegalArgumentException(
                          "Seconds argument " + sec + 
                          " must be between 0 and 60" );
        }
        return hmsToRadians( ! isNegative( hour ), 
                             (int) Math.abs( hour ), (int) min, sec );
    }

    /**
     * Calculates the separation (distance around a great circle) of
     * two points on the sky in radians.
     *
     * @param  ra1   right ascension of point 1 in radians
     * @param  dec1  declination of point 1 in radians
     * @param  ra2   right ascension of point 2 in radians
     * @param  dec2  declination of point 2 in radians
     * @return  angular distance between point 1 and point 2 in radians
     */
    public static double skyDistanceRadians( double ra1, double dec1,
                                             double ra2, double dec2 ) {
        return USE_VINCENTY
             ? vincentySeparationFormula( ra1, dec1, ra2, dec2 )
             : haversineSeparationFormula( ra1, dec1, ra2, dec2 );
    }

    /**
     * Calculates the position angle between two points on the sky in radians.
     * The result is in the range +/-pi.
     * If point 2 is due east of point 1, the result is +pi/2.
     * Zero is returned if the points are coincident.
     *
     * @param  ra1   right ascension of point 1 in radians
     * @param  dec1  declination of point 1 in radians
     * @param  ra2   right ascension of point 2 in radians
     * @param  dec2  declination of point 2 in radians
     * @return  bearing in radians of point 2 from point 1
     */
    public static double posAngRadians( double ra1, double dec1,
                                        double ra2, double dec2 ) {

        /* This code is written with reference to the source code of
         * SLA_DBEAR in (the FORTRAN) SLALIB. */
        double dra = ra2 - ra1;
        double y = Math.sin( dra ) * Math.cos( dec2 );
        double x = Math.sin( dec2 ) * Math.cos( dec1 )
                 - Math.cos( dec2 ) * Math.sin( dec1 ) * Math.cos( dra );
        return x == 0 && y == 0
             ? 0
             : Math.atan2( y, x );
    }

    /**
     * Calculates the distance in three dimensional space
     * between two points specified in spherical polar coordinates.
     *
     * @param   ra1      right ascension of point 1 in radians
     * @param   dec1     declination of point1 in radians
     * @param   radius1  distance from origin of point1
     * @param   ra2      right ascension of point 2 in radians
     * @param   dec2     declination of point2 in radians
     * @param   radius2  distance from origin of point2
     * @return  the linear distance between point1 and point2;
     *          units are as for <code>radius1</code> and <code>radius2</code>
     */
    public static double
            polarDistanceRadians( double ra1, double dec1, double radius1,
                                  double ra2, double dec2, double radius2 ) {
        double theta1 = 0.5 * Math.PI - dec1;
        double theta2 = 0.5 * Math.PI - dec2;
        double sd1 = Math.sin( theta1 );
        double sd2 = Math.sin( theta2 );
        double x1 = radius1 * Math.cos( ra1 ) * sd1;
        double x2 = radius2 * Math.cos( ra2 ) * sd2;
        double y1 = radius1 * Math.sin( ra1 ) * sd1;
        double y2 = radius2 * Math.sin( ra2 ) * sd2;
        double z1 = radius1 * Math.cos( theta1 );
        double z2 = radius2 * Math.cos( theta2 );
        double dx = x1 - x2;
        double dy = y1 - y2;
        double dz = z1 - z2;
        return Math.sqrt( dx * dx + dy * dy + dz * dz );
    }

    /**
     * Converts hours to radians.
     *
     * @param  hours   angle in hours
     * @return   angle in radians
     */
    public static double hoursToRadians( double hours ) {
        return degreesToRadians( hours * 15 );
    }

    /**
     * Converts degrees to radians.
     *
     * @param  deg   angle in degrees
     * @return   angle in radians
     */
    public static double degreesToRadians( double deg ) {
        return Math.toRadians( deg );
    }

    /**
     * Converts radians to degrees.
     *
     * @param  rad  angle in radians
     * @return  angle in degrees
     */
    public static double radiansToDegrees( double rad ) {
        return Math.toDegrees( rad );
    }

    /**
     * Converts a B1950.0 FK4 position to J2000.0 FK5 at an epoch of B1950.0
     * yielding Right Ascension.
     * This assumes zero proper motion in the FK5 frame.
     *
     * @param   raFK4   right ascension in B1950.0 FK4 system (radians)
     * @param   decFK4  declination in B1950.0 FK4 system (radians)
     * @return  right ascension in J2000.0 FK5 system (radians)
     * @see     uk.ac.starlink.pal.Pal#Fk45z
     */
    public static double raFK4toFK5radians( double raFK4, double decFK4 ) {
        return raFK4toFK5Radians( raFK4, decFK4, 1950.0 );
    }

    /**
     * Converts a B1950.0 FK4 position to J2000.0 FK5 at an epoch of B1950.0
     * yielding Declination
     * This assumes zero proper motion in the FK5 frame.
     *
     * @param   raFK4   right ascension in B1950.0 FK4 system (radians)
     * @param   decFK4  declination in B1950.0 FK4 system (radians)
     * @return  declination in J2000.0 FK5 system (radians)
     * @see     uk.ac.starlink.pal.Pal#Fk45z
     */
    public static double decFK4toFK5radians( double raFK4, double decFK4 ) {
        return decFK4toFK5Radians( raFK4, decFK4, 1950.0 );
    }

    /**
     * Converts a J2000.0 FK5 position to B1950.0 FK4 at an epoch of B1950.0
     * yielding Declination.
     * This assumes zero proper motion, parallax and radial velocity in
     * the FK5 frame.
     *
     * @param   raFK5   right ascension in J2000.0 FK5 system (radians)
     * @param   decFK5  declination in J2000.0 FK5 system (radians)
     * @return  right ascension in the FK4 system (radians)
     * @see     uk.ac.starlink.pal.Pal#Fk54z
     */
    public static double raFK5toFK4radians( double raFK5, double decFK5 ) {
        return raFK5toFK4Radians( raFK5, decFK5, 1950.0 );
    }

    /**
     * Converts a J2000.0 FK5 position to B1950.0 FK4 at an epoch of B1950.0
     * yielding Declination.
     * This assumes zero proper motion, parallax and radial velocity in
     * the FK5 frame.
     *
     * @param   raFK5   right ascension in J2000.0 FK5 system (radians)
     * @param   decFK5  declination in J2000.0 FK5 system (radians)
     * @return  right ascension in the FK4 system (radians)
     * @see     uk.ac.starlink.pal.Pal#Fk54z
     */
    public static double decFK5toFK4radians( double raFK5, double decFK5 ) {
        return decFK5toFK4Radians( raFK5, decFK5, 1950.0 );
    }

    /**
     * Converts a B1950.0 FK4 position to J2000.0 FK5 yielding Right Ascension.
     * This assumes zero proper motion in the FK5 frame.
     * The <code>bepoch</code> parameter is the epoch at which the position in
     * the FK4 frame was determined.
     *
     * @param   raFK4   right ascension in B1950.0 FK4 system (radians)
     * @param   decFK4  declination in B1950.0 FK4 system (radians)
     * @param   bepoch  Besselian epoch
     * @return  right ascension in J2000.0 FK5 system (radians)
     * @see     uk.ac.starlink.pal.Pal#Fk45z
     */
    public static double raFK4toFK5Radians( double raFK4, double decFK4,
                                            double bepoch ) {
        return new Pal()
              .Fk45z( new AngleDR( raFK4, decFK4 ), bepoch )
              .getAlpha();
    }

    /**
     * Converts a B1950.0 FK4 position to J2000.0 FK5 yielding Declination.
     * This assumes zero proper motion in the FK5 frame.
     * The <code>bepoch</code> parameter is the epoch at which the position in
     * the FK4 frame was determined.
     *
     * @param   raFK4   right ascension in B1950.0 FK4 system (radians)
     * @param   decFK4  declination in B1950.0 FK4 system (radians)
     * @param   bepoch  Besselian epoch
     * @return  declination in J2000.0 FK5 system (radians)
     * @see     uk.ac.starlink.pal.Pal#Fk45z
     */
    public static double decFK4toFK5Radians( double raFK4, double decFK4,
                                             double bepoch ) {
        return new Pal()
              .Fk45z( new AngleDR( raFK4, decFK4 ), bepoch )
              .getDelta();
    }

    /**
     * Converts a J2000.0 FK5 position to B1950.0 FK4 yielding Declination.
     * This assumes zero proper motion, parallax and radial velocity in
     * the FK5 frame.
     *
     * @param   raFK5   right ascension in J2000.0 FK5 system (radians)
     * @param   decFK5  declination in J2000.0 FK5 system (radians)
     * @param   bepoch  Besselian epoch
     * @return  right ascension in the FK4 system (radians)
     * @see     uk.ac.starlink.pal.Pal#Fk54z
     */
    public static double raFK5toFK4Radians( double raFK5, double decFK5,
                                            double bepoch ) {
        return new Pal()
              .Fk54z( new AngleDR( raFK5, decFK5 ), bepoch ).getAngle()
              .getAlpha();
    }

    /**
     * Converts a J2000.0 FK5 position to B1950.0 FK4 yielding Declination.
     * This assumes zero proper motion, parallax and radial velocity in
     * the FK5 frame.
     *
     * @param   raFK5   right ascension in J2000.0 FK5 system (radians)
     * @param   decFK5  declination in J2000.0 FK5 system (radians)
     * @param   bepoch  Besselian epoch
     * @return  right ascension in the FK4 system (radians)
     * @see     uk.ac.starlink.pal.Pal#Fk54z
     */
    public static double decFK5toFK4Radians( double raFK5, double decFK5,
                                             double bepoch ) {
        return new Pal()
              .Fk54z( new AngleDR( raFK5, decFK5 ), bepoch ).getAngle()
              .getDelta();
    }

    /**
     * Indicates whether a double precision number is positive or negative
     * based on its sign bit.  Note that this distinguishes positive from
     * negative zero.
     *
     * @param  value  value for testing
     * @return  true iff <code>value</code>'s sign bit is set
     */
    private static boolean isNegative( double value ) {
        return ( Double.doubleToLongBits( value ) & 0x8000000000000000L ) != 0;
    }

    /**
     * Convert an angle in degrees, minutes, seconds with an explicit sign
     * to radians.
     *
     * @param  positive  true iff the angle is &gt;= 0
     * @param  deg  non-negative degrees part
     * @param  min  non-negative minutes part
     * @param  sec  non-negative seconds part
     * @return  angle in radians
     */
    private static double dmsToRadians( boolean positive,
                                        int deg, int min, double sec ) {
        assert deg >= 0;
        assert min >= 0;
        assert sec >= 0;
        double radians = degreesToRadians( deg + ( min + sec / 60.0 ) / 60.0 ); 
        return positive ? radians
                        : -1.0 * radians;
    }

    /**
     * Convert an angle in hours, minutes, seconds with an explicit sign
     * to radians.
     *
     * @param  positive  true iff the angle is &gt;= 0
     * @param  hour  non-negative hours part
     * @param  min  non-negative minutes part
     * @param  sec  non-negative seconds part
     * @return  angle in radians
     */
    private static double hmsToRadians( boolean positive,
                                        int hour, int min, double sec ) {
        return 15.0 * dmsToRadians( positive, hour, min, sec );
    }

    /**
     * Produce formatted string for angle in degrees, minutes and seconds.
     *
     * @param  positive  true iff the angle is &gt;=0
     * @param  deg   non-negative degrees part
     * @param  min   non-negative minutes part
     * @param  sec   non-negative seconds part
     * @param  dp   number of decimal places for seconds
     * @return  formatted DMS string representing angle
     */
    private static String formatDms( boolean positive,
                                     int deg, int min, double sec, int dp ) {
        assert deg >= 0;
        assert min >= 0;
 
        return SexFormat.getDmsFormat( dp ).format( positive, deg, min, sec );
    }

    /**
     * Produce formatted string for angle in hours, minutes and seconds.
     *
     * @param  positive  true iff the angle is &gt;=0
     * @param  hour  non-negative hours part
     * @param  min   non-negative minutes part
     * @param  sec   non-negative seconds part
     * @param  dp  number of decimal places for seconds
     * @return  formatted HMS string representing angle
     */
    private static String formatHms( boolean positive, 
                                     int hour, int min, double sec, int dp ) {
        return SexFormat.getHmsFormat( dp ).format( positive, hour, min, sec );
    }

    /**
     * Returns a compiled regex pattern for sexagesimal angles.
     * This expression has four groups:
     * <ol>
     * <li>sign (+ or - or blank)
     * <li>first part (non-negative integer)
     * <li>second part (non-negative integer)
     * <li>third part (non-negative floating point numeric)
     * </ol>
     *
     * @param  trail1  regex matching the string that follows the first number
     * @param  trail2  regex matching the string that follows the second number
     * @param  trail3  regex matching the string that follows the third number
     * @return  sexagesimal match pattern
     */
    private static Pattern getSexPattern( String trail1, String trail2,
                                          String trail3 ) {
        return Pattern.compile( " *([+\\-]?)"
                              + " *([0-9]+) *"
                              + trail1 
                              + " *([0-9]+) *"
                              + "(?:" + trail2
                                      + " *([0-9]+\\.?[0-9]*) *" + trail3 + ")?"
                              + " *" );
    }

    /**
     * Haversine formula for separation between two points on the sphere.
     * This does not have the numerical instabilities of the cosine formula
     * at small angles.
     * <p>
     * This implementation derives from Bob Chamberlain's contribution
     * to the comp.infosystems.gis FAQ; he cites
     * R.W.Sinnott, "Virtues of the Haversine", Sky and Telescope vol.68,
     * no.2, 1984, p159.
     *
     * @param   ra1  right ascension of point 1 in radians
     * @param   dec1 declination of point 1 in radians
     * @param   ra2  right ascension of point 2 in radians
     * @param   dec2 declination of point 2 in radians
     * @return  angular separation of point 1 and point 2 in radians
     * @see  <http://www.census.gov/geo/www/gis-faq.txt>
     */
    static double haversineSeparationFormula( double ra1, double dec1,
                                              double ra2, double dec2 ) {
        double sd2 = Math.sin( 0.5 * ( dec2 - dec1 ) );
        double sr2 = Math.sin( 0.5 * ( ra2 - ra1 ) );
        double a = sd2 * sd2 +
                   sr2 * sr2 * Math.cos( dec1 ) * Math.cos( dec2 );
        if ( Double.isNaN( a ) ) {
            return Double.NaN;
        }
        return a < 1.0 ? 2.0 * Math.asin( Math.sqrt( a ) )
                       : Math.PI;
    }

    /**
     * Vincenty formula for separation between two points on the sphere.
     * This is a special case of the Vincenty formula for distance between
     * two points on an ellipsoid.
     * It is stable for all angles, unlike the Cosine formula for
     * small angles and the Haversine formula for angles near Pi.
     * Benchmarking reports it about 50% slower than Haversine.
     *
     * @param   ra1  right ascension of point 1 in radians
     * @param   dec1 declination of point 1 in radians
     * @param   ra2  right ascension of point 2 in radians
     * @param   dec2 declination of point 2 in radians
     * @return  angular separation of point 1 and point 2 in radians
     */
     static double vincentySeparationFormula( double ra1, double dec1,
                                              double ra2, double dec2 ) {
        double cd1 = Math.cos( dec1 );
        double sd1 = Math.sin( dec1 );
        double cd2 = Math.cos( dec2 );
        double sd2 = Math.sin( dec2 );
        double dra = ra2 - ra1;
        double cdr = Math.cos( dra );
        double sdr = Math.sin( dra );
        return Math.atan2( Math.hypot( cd2 * sdr, cd1 * sd2 - sd1 * cd2 * cdr ),
                           sd1 * sd2 + cd1 * cd2 * cdr );
    }

    /**
     * Helper class for sexagesimal formatting.
     */
    private static class SexFormat {

        private boolean useSign;
        private int sf1;
        private int dp3;
        private char[] buf;

        private static SexFormat[] hmsFormats = new SexFormat[ 0 ];
        private static SexFormat[] dmsFormats = new SexFormat[ 0 ];
        private static char[] digits0 =
             new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', };
        private static char[] digitsLeading =
             new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', };
        private static char[] digitsMinus = 
             new char[] { '-', '1', '2', '3', '4', '5', '6', '7', '8', '9', };
                              
        /**
         * Constructs a new formatter with a given number of decimal places.
         *
         * @param useSign  whether a sign character is to be used
         * @param dp3  number of decimal places in the third field
         * @see  #getHmsFormat
         * @see  #getDmsFormat
         */
        private SexFormat( boolean useSign, int dp3 ) {
            this.useSign = useSign;
            this.dp3 = dp3;
            if ( dp3 < 0 ) {
                throw new IllegalArgumentException();
            }
            buf = new char[ ( useSign ? 1 : 0 ) + 2 + 6 + 
                            ( ( dp3 > 0 ) ? 1 : 0 ) + dp3 ];
        }

        /**
         * Formats a sexagesimal angle.  Note all parts are truncated;
         * no rounding up is performed.
         *
         * @param   positive  true  iff the angle is &gt;=0
         * @param   f1   non-negative first part (degrees or hours) 
         * @param   f2   non-negative second part (minutes)
         * @param   f3   non-negative third part (seconds)
         * @return  formatted angle
         */
        public String format( boolean positive, int f1, int f2, double f3 ) {
            if ( f1 < 0 || f2 < 0 || f3 < 0 ) {
                throw new IllegalArgumentException( "Can't be negative" );
            }
            if ( Double.isNaN( f3 ) ) {
                return null;
            }
            int f3i = (int) f3;
            int pos = 0;
            if ( useSign ) {
                char sgnChar = positive ? '+' : '-';
                buf[ pos++ ] = sgnChar;
            }
            buf[ pos++ ] = digitsLeading[ ( f1 / 10 ) % 10 ];
            buf[ pos++ ] = digits0[ f1 % 10 ];
            buf[ pos++ ] = ':';
            buf[ pos++ ] = digits0[ ( f2 / 10 ) % 10 ];
            buf[ pos++ ] = digits0[ f2 % 10 ];
            buf[ pos++ ] = ':';
            buf[ pos++ ] = digits0[ ( f3i / 10 ) % 10 ];
            buf[ pos++ ] = digits0[ f3i % 10 ];
            if ( dp3 > 0 ) {
                buf[ pos++ ] = '.';
                for ( int i = 0; i < dp3; i++ ) {
                    f3 = f3 * 10.0;
                    buf[ pos++ ] = digits0[ ( (int) f3 ) % 10 ];
                }
            }
            return new String( buf );
        }

        /**
         * Returns an HMS-type formatter with a given number of decimal 
         * places.
         * 
         * @param  dp  number of decimal places in seconds field
         * @return  formatter, not necessarily a new one
         */
        public static SexFormat getHmsFormat( int dp ) {
            int nf = hmsFormats.length;
            if ( dp >= nf ) {
                hmsFormats = new SexFormat[ dp + 1 ];
                for ( int i = 0; i <= dp; i++ ) {
                    hmsFormats[ i ] = new SexFormat( false, i );
                }
            }
            return hmsFormats[ dp ];
        }

        /**
         * Returns a DMS-type formatter with a given number of decimal
         * places.
         *
         * @param  dp  number of decimal places in seconds field
         * @return  formatter, not necessarily a new one
         */
        public static SexFormat getDmsFormat( int dp ) {
            int nf = dmsFormats.length;
            if ( dp >= nf ) {
                dmsFormats = new SexFormat[ dp + 1 ];
                for ( int i = 0; i <= dp; i++ ) {
                    dmsFormats[ i ] = new SexFormat( true, i );
                }
            }
            return dmsFormats[ dp ];
        }
    }
}
