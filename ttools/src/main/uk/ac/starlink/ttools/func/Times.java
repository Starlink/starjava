// The doc comments in this class are processed to produce user-visible
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical.

package uk.ac.starlink.ttools.func;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import uk.ac.starlink.pal.Pal;

/**
 * Functions for conversion of time values between various forms.
 * The forms used are
 * <dl>
 * <dt>Modified Julian Date (MJD)</dt>
 * <dd><p>A continuous measure in days since midnight at the start of
 *     17 November 1858.  Based on UTC.
 *     </p></dd>
 * <dt>Julian Day (JD)</dt>
 * <dd><p>MJD plus a fixed offset of 2400000.5.
 *     The number of days since the notional creation of the universe,
 *     midday on 1 Jan 4713 BC.
 *     </p></dd>
 * <dt>ISO 8601</dt>
 * <dd><p>A string representation of the form 
 *     <code>yyyy-mm-ddThh:mm:ss.s</code>, where the <code>T</code>
 *     is a literal character (a space character may be used instead).
 *     Based on UTC.
 *     </p></dd> 
 * <dt>Julian Epoch</dt>
 * <dd><p>A continuous measure based on a Julian year of exactly 365.25 days.
 *     For approximate purposes this resembles the fractional number
 *     of years AD represented by the date.  Sometimes (but not here)
 *     represented by prefixing a 'J'; J2000.0 is defined as
 *     2000 January 1.5 in the TT timescale.
 *     </p></dd>
 * <dt>Besselian Epoch</dt>
 * <dd><p>A continuous measure based on a tropical year of about 365.2422 days.
 *     For approximate purposes this resembles the fractional number of
 *     years AD represented by the date.  Sometimes (but not here)
 *     represented by prefixing a 'B'.
 *     </p></dd>
 * <dt>Decimal Year</dt>
 * <dd><p>Fractional number of years AD represented by the date.
 *     2000.0, or equivalently 1999.99recurring, is midnight at the start
 *     of the first of January 2000.  Because of leap years, the size of
 *     a unit depends on what year it is in.
 *     </p></dd>
 * </dl>
 *
 * <p>Therefore midday on the 25th of October 2004 is 
 * <code>2004-10-25T12:00:00</code> in ISO 8601 format,
 * 53303.5 as an MJD value,
 * 2004.81588 as a Julian Epoch and
 * 2004.81726 as a Besselian Epoch.
 * 
 * <p>Currently this implementation cannot be relied upon to
 * better than a millisecond.
 *
 * @author   Mark Taylor (Starlink)
 * @since    2 Jun 2005
 * @see      <a href="http://www.cl.cam.ac.uk/~mgk25/iso-time.html"
 *              >ISO 8601 overview</a>
 */
public class Times {

    private final static char DATE_SEP = 'T';
    private final static String DATE_PATTERN = "yyyy-MM-dd";
    private final static String TIME_PATTERN = "HH:mm:ss";
    private final static TimeZone UTC = TimeZone.getTimeZone( "UTC" );
    private final static Pal pal = new Pal();
    private final static long BCE_BOUNDARY_UNIXMILLIS = -62135769600000L;

    /** Regular expression for parsing ISO 8601 dates. */
    private final static Pattern ISO_REGEX = 
        Pattern.compile( "([0-9]+)-([0-9]{1,2})-([0-9]{1,2})" +
                         "(?:[" + DATE_SEP + " ]([0-9]{1,2})" +
                            "(?::([0-9]{1,2})" +
                               "(?::([0-9]{1,2}(?:\\.[0-9]*)?))?" +
                            ")?" +
                         "Z?)?" );

    /** Date of the Unix epoch as a Modified Julian Date. */
    private final static double MJD_EPOCH = 40587.0;

    /** Number of milliseconds per day. */
    private final static double MILLIS_PER_DAY = 1000 * 60 * 60 * 24;

    /** Number of seconds per day. */
    private final static double SEC_PER_DAY = 60 * 60 * 24;

    /** JD value for MJD = 0 (=2400000.5). */
    public static final double MJD_OFFSET = 2400000.5;

    /**
     * Thread-local copy of a DateKit object.
     * It would be expensive to create new Calendar and DateFormat objects 
     * every time we needed them (probably).  But we can't use single 
     * static instances of these, since they are not thread safe.  
     * By having one per thread, we get the best of both worlds.
     */
    private final static ThreadLocal<DateKit> kitHolder_ =
            new ThreadLocal<DateKit>() {
        protected DateKit initialValue() {
            return new DateKit();
        }
    };

    /**
     * Private constructor prevents instantiation.
     */
    private Times() {
    }

    /**
     * Converts an ISO8601 date string to Modified Julian Date.
     * The basic format of the <code>isoDate</code> argument is
     * <code>yyyy-mm-ddThh:mm:ss.s</code>, though some deviations
     * from this form are permitted:
     * <ul>
     * <li>The '<code>T</code>' which separates date from time 
     *     can be replaced by a space</li>
     * <li>The seconds, minutes and/or hours can be omitted</li>
     * <li>The decimal part of the seconds can be any length, 
     *     and is optional</li>
     * <li>A '<code>Z</code>' (which indicates UTC) may be appended
     *     to the time</li>
     * </ul>
     * Some legal examples are therefore:
     * "<code>1994-12-21T14:18:23.2</code>",
     * "<code>1968-01-14</code>", and
     * "<code>2112-05-25 16:45Z</code>".
     *
     * @example   <code>isoToMjd("2004-10-25T18:00:00") = 53303.75</code>
     * @example   <code>isoToMjd("1970-01-01") = 40587.0</code>
     * 
     * @param  isoDate  date in ISO 8601 format
     * @return  modified Julian date corresponding to <code>isoDate</code>
     */
    public static double isoToMjd( String isoDate ) {
        if ( isoDate == null || isoDate.trim().length() == 0 ) {
            return Double.NaN;
        }
        Matcher matcher = ISO_REGEX.matcher( isoDate );
        if ( matcher.matches() ) {
            try {
                String[] groups = new String[ 6 ];
                int ng = matcher.groupCount();
                for ( int i = 0; i < ng; i++ ) {
                    groups[ i ] = matcher.group( i + 1 );
                }
                int year = Integer.parseInt( groups[ 0 ] );
                int month = Integer.parseInt( groups[ 1 ] );
                int dom = Integer.parseInt( groups[ 2 ] );
                int hour = 
                    groups[ 3 ] == null ? 0 : Integer.parseInt( groups[ 3 ] );
                int min =
                    groups[ 4 ] == null ? 0 : Integer.parseInt( groups[ 4 ] );
                double sec = 
                    groups[ 5 ] == null ? 0.0 
                                        : Double.parseDouble( groups[ 5 ] );
                return dateToMjd( year, month, dom, hour, min, sec );
            }
            catch ( NumberFormatException e ) {
                throw (IllegalArgumentException) 
                      new IllegalArgumentException( "Bad ISO-8601 date " +
                                                    isoDate )
                     .initCause( e );
            }
        }
        else {
            throw new IllegalArgumentException( "Bad ISO-8601 date " +
                                                isoDate );
        }
    }

    /**
     * Converts a calendar date and time to Modified Julian Date.
     *
     * @example  <code>dateToMjd(1999, 12, 31, 23, 59, 59.) = 51543.99998</code>
     *
     * @param   year    year AD
     * @param   month   index of month; January is 1, December is 12
     * @param   day     day of month (the first day is 1)
     * @param   hour    hour (0-23)
     * @param   min     minute (0-59)
     * @param   sec     second (0&lt;=sec&lt;60)
     * @return  modified Julian date corresponding to arguments
     */
    public static double dateToMjd( int year, int month, int day, 
                                    int hour, int min, double sec ) {
        Calendar cal = getKit().calendar_;
        cal.clear();
        int intMillis = (int) Math.round( sec * 1000.0 );
        cal.set( year, month - 1, day, hour, min, intMillis / 1000 );
        cal.set( Calendar.MILLISECOND, intMillis % 1000 );
        return unixMillisToMjd( cal.getTimeInMillis() );
    }

    /**
     * Converts a calendar date to Modified Julian Date.
     *
     * @example  <code>dateToMjd(1999, 12, 31) = 51543.0</code>
     *
     * @param   year    year AD
     * @param   month   index of month; January is 1, December is 12
     * @param   day     day of month (the first day is 1)
     * @return  modified Julian date corresponding to 00:00:00 of the date
     *          specified by the arguments
     */
    public static double dateToMjd( int year, int month, int day ) {
        Calendar cal = getKit().calendar_;
        cal.clear();
        cal.set( year, month - 1, day );
        return unixMillisToMjd( cal.getTimeInMillis() );
    }

    /**
     * Converts a Decimal Year to a Modified Julian Date.
     * 
     * @example   <code>decYearToMjd(2000.0) = 51544.0</code>
     *
     * @param    decYear  decimal year
     * @return   modified Julian Date
     */
    public static double decYearToMjd( double decYear ) {
        int year = (int) Math.floor( decYear );
        double frac = decYear - year;
        Calendar cal = getKit().calendar_;
        cal.clear();
        cal.set( Calendar.YEAR, year );
        long y0 = cal.getTimeInMillis();
        cal.set( Calendar.YEAR, year + 1 );
        long y1 = cal.getTimeInMillis();
        long t = y0 + Math.round( frac * ( y1 - y0 ) );
        return unixMillisToMjd( t );
    }

    /**
     * Converts an ISO8601 date string to seconds since 1970-01-01.
     * The basic format of the <code>isoDate</code> argument is
     * <code>yyyy-mm-ddThh:mm:ss.s</code>, though some deviations
     * from this form are permitted:
     * <ul>
     * <li>The '<code>T</code>' which separates date from time 
     *     can be replaced by a space</li>
     * <li>The seconds, minutes and/or hours can be omitted</li>
     * <li>The decimal part of the seconds can be any length, 
     *     and is optional</li>
     * <li>A '<code>Z</code>' (which indicates UTC) may be appended
     *     to the time</li>
     * </ul>
     * Some legal examples are therefore:
     * "<code>1994-12-21T14:18:23.2</code>",
     * "<code>1968-01-14</code>", and
     * "<code>2112-05-25 16:45Z</code>".
     *
     * @example   <code>isoToUnixSec("2004-10-25T18:00:00") =  1098727200</code>
     * @example   <code>isoToMjd("1970-01-01") = 0</code>
     * 
     * @param  isoDate  date in ISO 8601 format
     * @return  seconds since the Unix epoch
     */
    public static double isoToUnixSec( String isoDate ) {
        return mjdToUnixSec( isoToMjd( isoDate ) );
    }

    /**
     * Converts a Decimal Year to seconds since 1970-01-01.
     * 
     * @example   <code>decYearToUnixSec(2000.0) = 946684800</code>
     * @example   <code>decYearToUnixSec(1970) = 0</code>
     *
     * @param    decYear  decimal year
     * @return   seconds since the Unix epoch
     */
    public static double decYearToUnixSec( double decYear ) {
        return mjdToUnixSec( decYearToMjd( decYear ) );
    }

    /**
     * Converts a Modified Julian Date to seconds since 1970-01-01.
     *
     * @param  mjd  modified Julian date
     * @return  seconds since the Unix epoch
     */
    public static double mjdToUnixSec( double mjd ) {
        return ( mjd - MJD_EPOCH ) * SEC_PER_DAY;
    }

    /**
     * Converts a Julian day to seconds since 1970-01-01.
     *
     * @param  jd  Julian day
     * @return   seconds since the Unix epoch
     */
    public static double jdToUnixSec( double jd ) {
        return mjdToUnixSec( jdToMjd( jd ) );
    }

    /**
     * Converts a Modified Julian Date value to an ISO 8601-format date-time
     * string.  The output format is <code>yyyy-mm-ddThh:mm:ss</code>.
     * If the result predates the Common Era, the string "(BCE)" is prepended.
     *
     * @example  <code>mjdToIso(53551.72917) = "2005-06-30T17:30:00"</code>
     *
     * @param   mjd  modified Julian date
     * @return  ISO 8601 format date corresponding to <code>mjd</code>
     */
    public static String mjdToIso( double mjd ) {
        return formatMjd( mjd, getKit().isoDateTimeFormat_, true );
    }

    /**
     * Converts a Modified Julian Date value to an ISO 8601-format date
     * string.  The output format is <code>yyyy-mm-dd</code>.
     * If the result predates the Common Era, the string "(BCE)" is prepended.
     *
     * @example  <code>mjdToDate(53551.72917) = "2005-06-30"</code>
     *
     * @param   mjd  modified Julian date
     * @return  ISO 8601 format date corresponding to <code>mjd</code>
     */
    public static String mjdToDate( double mjd ) {
        return formatMjd( mjd, getKit().isoDateFormat_, true );
    }

    /**
     * Converts a Modified Julian Date value to an ISO 8601-format time-only
     * string.  The output format is <code>hh:mm:ss</code>.
     *
     * @example  <code>mjdToTime(53551.72917) = "17:30:00"</code>
     *
     * @param   mjd  modified Julian date
     * @return  ISO 8601 format time corresponding to <code>mjd</code>
     */
    public static String mjdToTime( double mjd ) {
        return formatMjd( mjd, getKit().isoTimeFormat_, false );
    }

    /**
     * Converts a Modified Julian Date to Decimal Year.
     *
     * @example  <code>mjdToDecYear(0.0) = 1858.87671</code>
     *
     * @param   mjd  modified Julian Date
     * @return  decimal year
     */
    public static double mjdToDecYear( double mjd ) {
        Calendar cal = getKit().calendar_;
        cal.clear();
        long t = mjdToUnixMillis( mjd );
        cal.setTimeInMillis( t );
        int year = cal.get( Calendar.YEAR );
        cal.clear();
        cal.set( Calendar.YEAR, year );
        long y0 = cal.getTimeInMillis();
        cal.set( Calendar.YEAR, year + 1 );
        long y1 = cal.getTimeInMillis();
        return (double) year + (double) ( t - y0 ) / (double) ( y1 - y0 );
    }

    /**
     * Converts a Modified Julian Date value to a date using a customisable
     * date format.
     * The format is as defined by the 
     * <a href="http://docs.oracle.com/javase/6/docs/api/java/text/SimpleDateFormat.html"
     *    ><code>java.text.SimpleDateFormat</code></a> class.
     * The default output corresponds to the string
     * "<code>yyyy-MM-dd'T'HH:mm:ss</code>"
     *
     * <p>Note that the output from certain formatting characters
     * (such as <code>MMM</code> for month, <code>EEE</code> for day of week)
     * is dependent on your locale (system language settings).
     * The output time zone however always corresponds to UTC.
     *
     * @example   <code>formatMjd(50000.3, "EEE dd, MMM, yy") 
     *                  = "Tue 10 Oct, 95"</code>
     * @example   <code>formatMjd(50000.1234, "'time 'H:mm:ss.SSS")
     *                  = "time 2:57:41.760"</code>
     *
     * @param   mjd   modified Julian date
     * @param   format   formatting patttern
     * @return  custom formatted time corresponding to <code>mjd</code>
     * @see     java.text.SimpleDateFormat
     */
    public static String formatMjd( double mjd, String format ) {
        return formatMjd( mjd, getFormat( format ), false );
    }

    /**
     * Converts a Julian Day to Modified Julian Date.
     * The calculation is simply <code>jd-2400000.5</code>.
     *
     * @param   jd  Julian day number
     * @return   MJD value
     */
    public static double jdToMjd( double jd ) {
        return jd - MJD_OFFSET;
    }

    /**
     * Converts a Modified Julian Date to Julian Day.
     * The calculation is simply <code>jd+2400000.5</code>.
     *
     * @param  mjd  MJD value
     * @return  Julian day number
     */
    public static double mjdToJd( double mjd ) {
        return mjd + MJD_OFFSET;
    }

    /**
     * Converts a Modified Julian Date to Julian Epoch.
     * For approximate purposes, the result
     * of this routine consists of an integral part which gives the
     * year AD and a fractional part which represents the distance
     * through that year, so that for instance 2000.5 is approximately
     * 1 July 2000.
     *
     * @example   <code>mjdToJulian(0.0) = 1858.87885</code>
     *
     * @param  mjd  modified Julian date
     * @return  Julian epoch
     */
    public static double mjdToJulian( double mjd ) {
        return pal.Epj( mjd );
    }

    /**
     * Converts a Julian Epoch to Modified Julian Date.
     * For approximate purposes, the argument
     * of this routine consists of an integral part which gives the
     * year AD and a fractional part which represents the distance
     * through that year, so that for instance 2000.5 is approximately
     * 1 July 2000.
     *
     * @example   <code>julianToMjd(2000.0) = 51544.5</code>
     *
     * @param  julianEpoch  Julian epoch
     * @return   modified Julian date
     */
    public static double julianToMjd( double julianEpoch ) {
        return pal.Epj2d( julianEpoch );
    }

    /**
     * Converts Modified Julian Date to Besselian Epoch.
     * For approximate purposes, the result
     * of this routine consists of an integral part which gives the
     * year AD and a fractional part which represents the distance
     * through that year, so that for instance 1950.5 is approximately
     * 1 July 1950.
     *
     * @example  <code>mjdToBesselian(0.0) = 1858.87711</code>
     *
     * @param  mjd  modified Julian date
     * @return  Besselian epoch
     */
    public static double mjdToBesselian( double mjd ) {
        return pal.Epb( mjd );
    }

    /**
     * Converts Besselian Epoch to Modified Julian Date.
     * For approximate purposes, the argument
     * of this routine consists of an integral part which gives the
     * year AD and a fractional part which represents the distance
     * through that year, so that for instance 1950.5 is approximately
     * 1 July 1950.
     *
     * @example  <code>besselianToMjd(1950.0) = 33281.92346</code>
     *
     * @param  besselianEpoch  Besselian epoch
     * @return modified Julian date
     */
    public static double besselianToMjd( double besselianEpoch ) {
        return pal.Epb2d( besselianEpoch );
    }

    /**
     * Returns the year part of a modified Julian date value.
     *
     * @param   mjd  modified Julian date
     * @return  year AD
     */
    static int mjdYear( double mjd ) {
        return getField( mjd, Calendar.YEAR );
    }

    /**
     * Returns the month part of a modified Julian date value.
     *
     * @param   mjd  modified Julian date
     * @return  month index - 1 is January, 12 is December
     */
    static int mjdMonth( double mjd ) {
        return getField( mjd, Calendar.MONTH ) + 1;
    }

    /**
     * Returns the day of the month part of a modified Julian date value.
     *
     * @param   mjd  modified Julian date
     * @return  day of the month - the first day in each month is 1
     */
    static int mjdDayOfMonth( double mjd ) {
        return getField( mjd, Calendar.DAY_OF_MONTH );
    }

    /**
     * Returns the hour part of a modified Julian date value.
     *
     * @param   mjd  modified Julian date
     * @return  hour (0-23)
     */
    static int mjdHour( double mjd ) {
        return getField( mjd, Calendar.HOUR_OF_DAY );
    }

    /**
     * Returns the minute part of a modified Julian date value.
     *
     * @param   mjd  modified Julian date
     * @return  minute (0-59)
     */
    static int mjdMinute( double mjd ) {
        return getField( mjd, Calendar.MINUTE );
    }

    /**
     * Returns the seconds part of a modified Julian date value.
     *
     * @param   mjd  modified Julian date
     * @return  seconds  (0&lt;=sec&lt;60)
     */
    static double mjdSecond( double mjd ) {
        double minutes = mjd * 60.0 * 24.0;
        double minFrac = minutes - Math.floor( minutes );
        return minFrac * 60.0;
    }

    /**
     * Returns an integer given by one of the defined calendar fields
     * for an MJD.
     *
     * @param   mjd  modified Julian date
     * @param   field  one of the field constants defined in 
     *          {@link java.util.Calendar}
     * @return  value of <code>field</code> for <code>mjd</code>
     */
    private static int getField( double mjd, int field ) {
        Calendar cal = getKit().calendar_;
        cal.setTimeInMillis( mjdToUnixMillis( mjd ) );
        return cal.get( field );
    }

    /**
     * Formats an MJD using a given date formatting object.
     *
     * <p>Optionally, the string "<code>(BCE)</code>"
     * can be prepended to the output for dates before the Common Era.
     * This is rarely likely to be activated for astronomical data,
     * except in the (perhaps not so rare) case of erroneous
     * numeric date values present in data.  If this option is not used,
     * a BCE date looks indistinguishable from a CE date.
     * The choice of prepending and parenthesising the marker string is used
     * firstly to make it visually obvious what's going on, and
     * secondly so that BCE dates sort lexicographically before CE ones.
     *
     * @param  mjd  modified Julian date
     * @param  format   format object
     * @param  prependBce  prepend "(BCE)" for dates before the common era
     *         
     * @return  formatted string
     */
    private static String formatMjd( double mjd, DateFormat format,
                                     boolean prependBce ) {
        if ( Double.isNaN( mjd ) || Double.isInfinite( mjd ) ) {
            return null;
        }
        else {
            long unixMillis = mjdToUnixMillis( mjd );
            String txt = format.format( new Date( unixMillis ) );

            /* I don't think there's a way of getting the DateFormat to
             * do the BCE formatting itself; format GG adds "AD" or "BC",
             * but we really just want the marker for the (exceptional)
             * case in which it's pre-CE, without adding an AD or
             * additional string manipulation in the vast majority of
             * normal cases. */
            if ( prependBce && unixMillis < BCE_BOUNDARY_UNIXMILLIS ) {
                txt = "(BCE)" + txt;
            }
            return txt;
        }
    }

    /**
     * Converts from milliseconds since the Unix epoch (1970-01-01T00:00:00)
     * to a modified Julian date value
     *
     * @param   unixMillis  milliseconds since the Unix epoch
     * @return  modified Julian date
     */
    public static double unixMillisToMjd( long unixMillis ) {
        return ((double) unixMillis) / MILLIS_PER_DAY + MJD_EPOCH;
    }

    /**
     * Converts from modified Julian date to milliseconds since the Unix
     * epoch (1970-01-01T00:00:00).
     *
     * @param   mjd  modified Julian date
     * @return  milliseconds since the Unix epoch
     */
    public static long mjdToUnixMillis( double mjd ) {
        return Math.round( ( mjd - MJD_EPOCH ) * MILLIS_PER_DAY );
    }

    /**
     * Returns a date kit private to the calling thread.
     *
     * @return  date kit
     */
    private static DateKit getKit() {
        return kitHolder_.get();
    }

    /**
     * Returns a DateFormat object defined by the given formatting pattern.
     * The return value is local to this thread, but created lazily;
     * if this thread has already asked for one with the same pattern,
     * it gets the same one.
     *
     * @param  pattern  {@link java.text.SimpleDateFormat} pattern
     * @return   new or old date format
     */
    private static DateFormat getFormat( String pattern ) {
        Map<String,DateFormat> map = getKit().patternMap_;
        if ( ! map.containsKey( pattern ) ) {
            map.put( pattern, newDateFormat( pattern ) );
        }
        return map.get( pattern );
    }

    /**
     * Creates a new Calendar object suitable for use with this class.
     *
     * @return  new calendar
     */
    private static Calendar newCalendar() {
        GregorianCalendar cal = new GregorianCalendar( UTC, Locale.UK );
        cal.setLenient( true );
        return cal;
    }

    /**
     * Creates a new DateFormat object suitable for use with this class.
     *
     * @param  pattern  {@link java.text.SimpleDateFormat} pattern
     * @return   date format corresponding to <code>pattern</code>
     */
    private static DateFormat newDateFormat( String pattern ) {
        DateFormat fmt = new SimpleDateFormat( pattern );
        fmt.setTimeZone( UTC );
        fmt.setCalendar( newCalendar() );
        return fmt;
    }

    /**
     * Helper class which contains all the items which are potentially 
     * expensive to produce but cannot be shared by different threads.
     * An instance of this class is managed by a ThreadLocal.
     */
    private static class DateKit {
        final Calendar calendar_ = newCalendar();
        final Map<String,DateFormat> patternMap_ =
            new HashMap<String,DateFormat>();
        final DateFormat isoDateTimeFormat_ =
            newDateFormat( DATE_PATTERN + "'" + DATE_SEP + "'" + TIME_PATTERN );
        final DateFormat isoDateFormat_ = newDateFormat( DATE_PATTERN );
        final DateFormat isoTimeFormat_ = newDateFormat( TIME_PATTERN );
    }
}
