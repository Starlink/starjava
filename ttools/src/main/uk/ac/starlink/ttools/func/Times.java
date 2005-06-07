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

/**
 * Functions for conversion of time values between various forms.
 * The two main forms used here are Modified Julian Day (MJD) and
 * the string format and underlying calendar model described by
 * ISO 8601.  MJD is a continuous measure in days since
 * midnight at the start of 17 November 1858.
 * ISO 8601 format is a string representation of this of the form
 * <code>yyyy-mm-ddThh:mm:ss.s</code>, where the <code>T</code> is a literal
 * character.  In both cases the time is UTC.
 *
 * <p>Therefore midday on the 25th of October 2004 is 
 * 53303.5 as an MJD value and 
 * <code>2004-10-25T12:00:00</code> in ISO 8601 format.
 * 
 * <p>Currently this implementation does not keep track of values to
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

    /** Regular expression for parsing ISO 8601 dates. */
    private final static Pattern ISO_REGEX = 
        Pattern.compile( "([0-9]+)-([0-9]{1,2})-([0-9]{1,2})" +
                         "(?:[" + DATE_SEP + " ]([0-9]{1,2})" +
                            "(?::([0-9]{1,2})" +
                               "(?::([0-9]{1,2}(?:\\.[0-9]*)?))?" +
                            ")?" +
                         "Z?)?" );

    /** Date of the Unix epoch as a Modified Julian Day. */
    private final static double MJD_EPOCH = 40587.0;

    /** Number of milliseconds per day. */
    private final static double MILLIS_PER_DAY = 1000 * 60 * 60 * 24;

    /**
     * Thread-local copy of a DateKit object.
     * It would be expensive to create new Calendar and DateFormat objects 
     * every time we needed them (probably).  But we can't use single 
     * static instances of these, since they are not thread safe.  
     * By having one per thread, we get the best of both worlds.
     */
    private final static ThreadLocal kitHolder_ = new ThreadLocal() {
        protected Object initialValue() {
            return new DateKit();
        }
    };

    /**
     * Private constructor prevents instantiation.
     */
    private Times() {
    }

    /**
     * Converts an ISO8601 date string to Modified Julian Day.
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
     * @param  isoDate  date in ISO 8601 format
     * @return  modified Julian day corresponding to <code>isoDate</code>
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
                int month = Integer.parseInt( groups[ 1 ] ) - 1;
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
     * Converts a calendar date and time to Modified Julian Day.
     *
     * @param   year    year AD
     * @param   month   zero-based index of month; January is 0, December is 11
     * @param   day     day of month (the first day is 1)
     * @param   hour    hour (0-23)
     * @param   min     minute (0-59)
     * @param   sec     second (0&lt;=sec&lt;60)
     * @return  modified Julian day corresponding to arguments
     */
    public static double dateToMjd( int year, int month, int day, 
                                    int hour, int min, double sec ) {
        Calendar cal = getKit().calendar_;
        cal.clear();
        int intMillis = (int) Math.round( sec * 1000.0 );
        cal.set( year, month, day, hour, min, intMillis / 1000 );
        cal.set( Calendar.MILLISECOND, intMillis % 1000 );
        return unixMillisToMjd( cal.getTimeInMillis() );
    }

    /**
     * Converts a calendar date to Modified Julian Day.
     *
     * @param   year    year AD
     * @param   month   zero-based index of month; January is 0, December is 11
     * @param   day     day of month (the first day is 1)
     * @return  modified Julian day corresponding to 00:00:00 of the date
     *          specified by the arguments
     */
    public static double dateToMjd( int year, int month, int day ) {
        Calendar cal = getKit().calendar_;
        cal.clear();
        cal.set( year, month, day );
        return unixMillisToMjd( cal.getTimeInMillis() );
    }

    /**
     * Converts a Modified Julian Day value to an ISO 8601-format date-time
     * string.  The output format is <code>yyyy-mm-ddThh:mm:ss</code>.
     *
     * @param   mjd  modified Julian day
     * @return  ISO 8601 format date corresponding to <code>mjd</code>
     */
    public static String mjdToIso( double mjd ) {
        return formatMjd( mjd, getKit().isoDateTimeFormat_ );
    }

    /**
     * Converts a Modified Julian Day value to an ISO 8601-format date
     * string.  The output format is <code>yyyy-mm-dd</code>.
     *
     * @param   mjd  modified Julian day
     * @return  ISO 8601 format date corresponding to <code>mjd</code>
     */
    public static String mjdToDate( double mjd ) {
        return formatMjd( mjd, getKit().isoDateFormat_ );
    }

    /**
     * Converts a Modified Julian Day value to an ISO 8601-format time-only
     * string.  The output format is <code>hh:mm:ss</code>.
     *
     * @param   mjd  modified Julian day
     * @return  ISO 8601 format time corresponding to <code>mjd</code>
     */
    public static String mjdToTime( double mjd ) {
        return formatMjd( mjd, getKit().isoTimeFormat_ );
    }

    /**
     * Converts a Modified Julian Day value to a date using a customisable
     * date format.
     * The format is as defined by the 
     * <a href="http://java.sun.com/j2se/1.4.2/docs/api/java/text/SimpleDateFormat.html"
     *    ><code>java.util.SimpleDateFormat</code></a> class.
     * The default output corresponds to the string
     * "<code>yyyy-MM-dd'T'HH:mm:ss</code>"
     *
     * @param   mjd   modified Julian day
     * @param   format   formatting patttern
     * @return  custom formatted time corresponding to <code>mjd</code>
     * @see     java.text.SimpleDateFormat
     */
    public static String formatMjd( double mjd, String format ) {
        return formatMjd( mjd, getFormat( format ) );
    }

    /**
     * Returns the year part of a modified Julian day value.
     *
     * @param   mjd  modified Julian day
     * @return  year AD
     */
    static int mjdYear( double mjd ) {
        return getField( mjd, Calendar.YEAR );
    }

    /**
     * Returns the month part of a modified Julian day value.
     *
     * @param   mjd  modified Julian day
     * @return  zero-based month - 0 is January, 11 is December
     */
    static int mjdMonth( double mjd ) {
        return getField( mjd, Calendar.MONTH );
    }

    /**
     * Returns the day of the month part of a modified Julian day value.
     *
     * @param   mjd  modified Julian day
     * @return  day of the month - the first day in each month is 1
     */
    static int mjdDayOfMonth( double mjd ) {
        return getField( mjd, Calendar.DAY_OF_MONTH );
    }

    /**
     * Returns the hour part of a modified Julian day value.
     *
     * @param   mjd  modified Julian day
     * @return  hour (0-23)
     */
    static int mjdHour( double mjd ) {
        return getField( mjd, Calendar.HOUR_OF_DAY );
    }

    /**
     * Returns the minute part of a modified Julian day value.
     *
     * @param   mjd  modified Julian day
     * @return  minute (0-59)
     */
    static int mjdMinute( double mjd ) {
        return getField( mjd, Calendar.MINUTE );
    }

    /**
     * Returns the seconds part of a modified Julian day value.
     *
     * @param   mjd  modified Julian day
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
     * @param   mjd  modified Julian day
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
     * @param  mjd  modified Julian day
     * @param  format   format object
     * @return  formatted string
     */
    private static String formatMjd( double mjd, DateFormat format ) {
        if ( Double.isNaN( mjd ) || Double.isInfinite( mjd ) ) {
            return null;
        }
        else {
            return format.format( new Date( mjdToUnixMillis( mjd ) ) );
        }
    }

    /**
     * Converts from milliseconds since the Unix epoch (1970-01-01T00:00:00)
     * to a modified Julian day value
     *
     * @param   unixMillis  milliseconds since the Unix epoch
     * @return  modified Julian day
     */
    private static double unixMillisToMjd( long unixMillis ) {
        return ((double) unixMillis) / MILLIS_PER_DAY + MJD_EPOCH;
    }

    /**
     * Converts from modified Julian day to milliseconds since the Unix
     * epoch (1970-01-01T00:00:00).
     *
     * @param   mjd  modified Julian day
     * @return  milliseconds since the Unix epoch
     */
    private static long mjdToUnixMillis( double mjd ) {
        return (long) Math.round( ( mjd - MJD_EPOCH ) * MILLIS_PER_DAY );
    }

    /**
     * Returns a date kit private to the calling thread.
     *
     * @return  date kit
     */
    private static DateKit getKit() {
        return (DateKit) kitHolder_.get();
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
        Map map = getKit().patternMap_;
        if ( ! map.containsKey( pattern ) ) {
            map.put( pattern, newDateFormat( pattern ) );
        }
        return (DateFormat) map.get( pattern );
    }

    /**
     * Creates a new Calendar object suitable for use with this class.
     *
     * @return  new calendar
     */
    private static Calendar newCalendar() {
        GregorianCalendar cal = new GregorianCalendar( UTC, Locale.UK );
        cal.setLenient( false );
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
        final Map patternMap_ = new HashMap();
        final DateFormat isoDateTimeFormat_ =
            newDateFormat( DATE_PATTERN + "'" + DATE_SEP + "'" + TIME_PATTERN );
        final DateFormat isoDateFormat_ = newDateFormat( DATE_PATTERN );
        final DateFormat isoTimeFormat_ = newDateFormat( TIME_PATTERN );
    }
}
