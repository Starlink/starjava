package uk.ac.starlink.topcat.func;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import uk.ac.starlink.pal.AngleDR;
import uk.ac.starlink.pal.Pal;

/**
 * Class containing expressions for angle transformations and manipulations.
 * In particular, methods for translating between radians and HH:MM:SS.S
 * or DDD:MM:SS.S type sexagesimal representations are provided.
 *
 * <p>The public functionality is all in static public methods 
 * (suitable for use within TOPCAT's JEL extensions.
 *
 * @author   Mark Taylor (Starlink)
 * @since    30 Apr 2004
 */
public class Angles {

    private static Pattern dmsPattern = 
        getSexPattern( "[:d ]", "[:m' ]", "[s\"]?" );
    private static Pattern hmsPattern = 
        getSexPattern( "[:h ]", "[:m' ]", "[s\"]?" );

    /**
     * Converts an angle in radians to a formatted degrees:minutes:seconds 
     * string.  No fractional part of the seconds field is given.
     *
     * @param  rad  angle in radians
     * @return  DMS-format string representing <tt>rad</tt>
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
     * @return  HMS-format string representing <tt>rad</tt>
     */
    public static String radiansToDms( double rad, int secFig ) {
        double degrees = radiansToDegrees( rad );
        int sign = degrees >= 0 ? +1 : -1;
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
     * @return  HMS-format string representing <tt>rad</tt>
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
     * @return  HMS-format string representing <tt>rad</tt>
     */
    public static String radiansToHms( double rad, int secFig ) {
        double degrees = radiansToDegrees( rad );
        double hours = degrees / 15.0;
        int sign = hours >= 0 ? +1 : -1;
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
     * <tt>dm[s]</tt>, or some others.
     * Additional spaces and leading +/- are permitted.
     *
     * @param  dms  formatted DMS string
     * @return  angle in radians specified by <tt>dms</tt>
     * @throws  NumberFormatException  if <tt>dms</tt> can't be parsed as
     *          a degrees:minutes:seconds string
     */
    public static double dmsToRadians( String dms ) {
        Matcher matcher = dmsPattern.matcher( dms );
        if ( ! matcher.matches() ) {
            throw new NumberFormatException( dms + 
                                             " not in deg:min:sec format" );
        }
        return dmsToRadians( ! "-".equals( matcher.group( 1 ) ),
                             Integer.parseInt( matcher.group( 2 ) ),
                             Integer.parseInt( matcher.group( 3 ) ),
                             Double.parseDouble( matcher.group( 4 ) ) );
    }

    /**
     * Converts a formatted hours:minutes:seconds string to an angle
     * in radians.  Delimiters may be colon, space, characters 
     * <tt>hm[s]</tt>, or some others.
     * Additional spaces and leading +/- are permitted.
     *
     * @param  hms  formatted HMS string
     * @return  angle in radians specified by <tt>hms</tt>
     * @throws  NumberFormatException  if <tt>dms</tt> can't be parsed as
     *          an hours:minutes:seconds string
     */
    public static double hmsToRadians( String hms ) {
        Matcher matcher = hmsPattern.matcher( hms );
        if ( ! matcher.matches() ) {
            throw new NumberFormatException( hms +
                                             " not in hour:min:sec format" );
        }
        return hmsToRadians( ! "-".equals( matcher.group( 1 ) ),
                             Integer.parseInt( matcher.group( 2 ) ),
                             Integer.parseInt( matcher.group( 3 ) ),
                             Double.parseDouble( matcher.group( 4 ) ) );
    }

    /**
     * Converts degrees, minutes, seconds to an angle in radians.
     *
     * <p>Use with care!  The sign of the result is taken from the
     * first non-zero argument.  It is an error for any but the first
     * non-zero argument to be negative.  It's easy to accidentally
     * read values between 0 and -1 degrees as positive.
     *
     * @param  deg  degrees part of angle
     * @param  min  minutes part of angle
     * @param  sec  seconds part of angle
     * @return  specified angle in radians
     * @throws   IllegalArgumentException if an argument after the first
     *           non-zero one is negative
     */
    public static double dmsToRadians( int deg, int min, double sec ) {
        return dmsToRadians( isPositive( deg, min, sec,
                                         "degrees", "minutes", "seconds" ),
                             Math.abs( deg ), Math.abs( min ),
                             Math.abs( sec ) );
    }

    /**
     * Converts degrees, minutes, seconds to an angle in radians.
     *
     * <p>Use with care!  The sign of the result is taken from the
     * first non-zero argument.  It is an error for any but the first
     * non-zero argument to be negative.  It's easy to accidentally
     * read values between 0 and -1 degrees as positive.
     *
     * @param  hour  degrees part of angle
     * @param  min  minutes part of angle
     * @param  sec  seconds part of angle
     * @return  specified angle in radians
     * @throws   IllegalArgumentException if an argument after the first
     *           non-zero one is negative
     */
    public static double hmsToRadians( int hour, int min, double sec ) {
        return hmsToRadians( isPositive( hour, min, sec,
                                         "hours", "minutes", "seconds" ),
                             Math.abs( hour ), Math.abs( min ),
                             Math.abs( sec ) );
    }

    /**
     * Calculates the separation (distance around a great circle) of
     * two points on the sky.
     *
     * @param  ra1   right ascension of point 1 in radians
     * @param  dec1  declination of point 1 in radians
     * @param  ra2   right ascension of point 2 in radians
     * @param  dec2  declination of point 2 in radians
     * @return  angular distance between point 1 and point 2 in radians
     */
    public static double skyDistance( double ra1, double dec1,
                                      double ra2, double dec2 ) {
        return haversineSeparationFormula( ra1, dec1, ra2, dec2 );
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
    public static double raFK4toFK5( double raFK4, double decFK4 ) {
        return raFK4toFK5( raFK4, decFK4, 1950.0 );
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
    public static double decFK4toFK5( double raFK4, double decFK4 ) {
        return decFK4toFK5( raFK4, decFK4, 1950.0 );
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
    public static double raFK5toFK4( double raFK5, double decFK5 ) {
        return raFK5toFK4( raFK5, decFK5, 1950.0 );
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
    public static double decFK5toFK4( double raFK5, double decFK5 ) {
        return decFK5toFK4( raFK5, decFK5, 1950.0 );
    }

    /**
     * Converts a B1950.0 FK4 position to J2000.0 FK5 yielding Right Ascension.
     * This assumes zero proper motion in the FK5 frame.
     * The <tt>bepoch</tt> parameter is the epoch at which the position in
     * the FK4 frame was determined.
     *
     * @param   raFK4   right ascension in B1950.0 FK4 system (radians)
     * @param   decFK4  declination in B1950.0 FK4 system (radians)
     * @param   bepoch  Besselian epoch
     * @return  right ascension in J2000.0 FK5 system (radians)
     * @see     uk.ac.starlink.pal.Pal#Fk45z
     */
    public static double raFK4toFK5( double raFK4, double decFK4,
                                     double bepoch ) {
        return new Pal()
              .Fk45z( new AngleDR( raFK4, decFK4 ), bepoch )
              .getAlpha();
    }

    /**
     * Converts a B1950.0 FK4 position to J2000.0 FK5 yielding Declination.
     * This assumes zero proper motion in the FK5 frame.
     * The <tt>bepoch</tt> parameter is the epoch at which the position in
     * the FK4 frame was determined.
     *
     * @param   raFK4   right ascension in B1950.0 FK4 system (radians)
     * @param   decFK4  declination in B1950.0 FK4 system (radians)
     * @param   bepoch  Besselian epoch
     * @return  declination in J2000.0 FK5 system (radians)
     * @see     uk.ac.starlink.pal.Pal#Fk45z
     */
    public static double decFK4toFK5( double raFK4, double decFK4,
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
    public static double raFK5toFK4( double raFK5, double decFK5,
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
    public static double decFK5toFK4( double raFK5, double decFK5,
                                      double bepoch ) {
        return new Pal()
              .Fk54z( new AngleDR( raFK5, decFK5 ), bepoch ).getAngle()
              .getDelta();
    }

    /**
     * Determines whether a sexagesimal angle is positive or not.
     * If the three parts are illegal (one of the arguments which is not
     * the first non-zero one is negative) then an IllegalArgumentException
     * is thrown.
     *
     * @param  p1  first numeric part (degrees or hours)
     * @param  p2  second numeric part (minutes)
     * @param  p3  third numeric part (seconds)
     * @param  n1  name of first numeric part
     * @param  n2  name of second numeric part
     * @param  n3  name of third numeric part
     * @return  true iff the angle is &gt;=0
     * @throws  IllegalArgumentException  if the arguments are illegal
     */
    private static boolean isPositive( int p1, int p2, double p3,
                                       String n1, String n2, String n3 ) {
        if ( p1 == 0 ) {
            if ( p2 == 0 ) {
                return p3 >= 0;
            }
            else {
                if ( p3 < 0 ) {
                    throw new IllegalArgumentException( 
                        n3 + " shouldn't be negative for nonzero " + p2 );
                }
                return p2 >= 0;
            }
        }
        else {
            if ( p2 < 0 ) {
                throw new IllegalArgumentException(
                    n2 + " shouldn't be negative for nonzero " + p1 );
            }
            if ( p3 < 0 ) {
                throw new IllegalArgumentException(
                    n3 + " shouldn't be negative for nonzero " + p1 );
            }
            return p1 >= 0;
        }
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
                              + " *([0-9]+) *" + trail1 
                              + " *([0-9]+) *" + trail2
                              + " *([0-9]+\\.?[0-9]*) *" + trail3
                              + " *" );
    }

    /**
     * Haversine formula for spherical trigonometry.
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
    private static double haversineSeparationFormula( double ra1, double dec1,
                                                     double ra2, double dec2 ) {
        double sd2 = Math.sin( 0.5 * ( dec2 - dec1 ) );
        double sr2 = Math.sin( 0.5 * ( ra2 - ra1 ) );
        double a = sd2 * sd2 +
                   sr2 * sr2 * Math.cos( dec1 ) * Math.cos( dec2 );
        return a < 1.0 ? 2.0 * Math.asin( Math.sqrt( a ) )
                       : Math.PI;
    }


    /**
     * Helper class for sexagesimal formatting.
     */
    private static class SexFormat {

        private int dp3;
        private char[] buf;

        private static SexFormat[] hmsFormats = new SexFormat[ 0 ];
        private static SexFormat[] dmsFormats = new SexFormat[ 0 ];
        private static char[] digits0 =
             new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', };
        private static char[] digitsLeading =
             new char[] { ' ', '1', '2', '3', '4', '5', '6', '7', '8', '9', };
        private static char[] digitsMinus = 
             new char[] { '-', '1', '2', '3', '4', '5', '6', '7', '8', '9', };
                              
        /**
         * Constructs a new formatter with a given number of decimal places.
         *
         * @param dp3  number of decimal places in the third field
         * @see  #getHmsFormat
         * @see  #getDmsFormat
         */
        private SexFormat( int dp3 ) {
            this.dp3 = dp3;
            if ( dp3 < 0 ) {
                throw new IllegalArgumentException();
            }
            buf = new char[ 3 + 7 + ( ( dp3 > 0 ) ? 1 : 0 ) + dp3 ];
        }

        /**
         * Formats a sexagesimal angle.
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
            int f3i = (int) f3;
            int pos = 0;
            char sgnChar = positive ? ' ' : '-';
            buf[ pos++ ] = sgnChar;
            buf[ pos++ ] = digitsLeading[ ( f1 / 100 ) % 10 ];
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
                    f3 = f3 * 10.0 + ( i == dp3 - 1 ? 0.5 : 0.0 );
                    buf[ pos++ ] = digits0[ ( (int) f3 ) % 10 ];
                }
                int p = pos;
                while ( buf[ --p ] == '0' ) {
                    buf[ p ] = ' ';
                }
                if ( buf[ p ] == '.' ) {
                    buf[ p ] = ' ';
                }
            }
            if ( sgnChar != ' ' ) {
                for ( int p = 0;
                      buf[ p ] == sgnChar && buf[ p + 1 ] == ' '; p++ ) {
                    buf[ p ] = ' ';
                    buf[ p + 1 ] = sgnChar;
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
                    hmsFormats[ i ] = new SexFormat( i );
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
                    dmsFormats[ i ] = new SexFormat( i );
                }
            }
            return dmsFormats[ dp ];
        }
    }
}
