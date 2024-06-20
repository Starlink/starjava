package uk.ac.starlink.ttools.convert;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.ValueInfo;

/**
 * Converts between Strings in ISO-8601 format and some kind of numeric date.
 *
 * @author   Mark Taylor
 * @since    29 Aug 2006
 */
public abstract class Iso8601Converter implements ValueConverter {

    private final ValueInfo inInfo_;
    private final ValueInfo outInfo_;

    private final static char DATE_SEP = 'T';
    private final static String DATE_PATTERN = "yyyy-MM-dd";
    private final static String TIME_PATTERN = "HH:mm:ss";
    private final static TimeZone UTC = TimeZone.getTimeZone( "UTC" );

    /** Regular expression for parsing ISO 8601 dates. */
    private final static String ISO_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
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
     * Constructor.
     *
     * @param   isoInfo  input value metadata (describing ISO-8601 strings)
     * @param   numInfo  output value metadata (describing numeric date)
     */
    protected Iso8601Converter( ValueInfo isoInfo, ValueInfo numInfo ) {
        if ( ! String.class.isAssignableFrom( isoInfo.getContentClass() ) ) {
            throw new IllegalArgumentException(
                "Input data must be String, not "
               + isoInfo.getContentClass().getName() );
        }
        inInfo_ = isoInfo;
        outInfo_ = numInfo;
    }

    public ValueInfo getInputInfo() {
        return inInfo_;
    }

    public ValueInfo getOutputInfo() {
        return outInfo_;
    }

    public Object convert( Object in ) {
        if ( in instanceof String ) {
            String isoDate = ((String) in).trim();
            if ( isoDate.length() > 0 ) {
                return Double.valueOf( fromMjd( isoToMjd( isoDate ) ) );
            }
            else {
                return null;
            }
        }
        else {
            return null;
        }
    }

    public Object unconvert( Object out ) {
        if ( out instanceof Number ) {
            double number = ((Number) out).doubleValue();
            if ( Double.isNaN( number ) || Double.isInfinite( number ) ) {
                return null;
            }
            else {
                return mjdToIso( toMjd( number ) );
            }
        }
        else {
            return null;
        }
    }

    /**
     * Converts from this converter's public date value to 
     * Modified Julian Date.
     *
     * @param   number date value
     * @return   date as MJD
     */
    protected abstract double toMjd( double number );

    /**
     * Converts from Modified Julian Date to this converter's
     * public date.
     *
     * @param   mjd  date as MJD
     * @return  date value
     */
    protected abstract double fromMjd( double mjd );

    /**
     * Converts an ISO-8601 string to a Modified Julian Date.
     *
     * @param   isoDate  date string
     * @return  MJD
     */
    private static double isoToMjd( String isoDate ) {
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
                return Double.NaN;
            }
        }
        else {
            return Double.NaN;
        }
    }

    /**
     * Converts a calendar date and time to Modified Julian Date.
     *
     * @param   year    year AD
     * @param   month   index of month; January is 1, December is 12
     * @param   day     day of month (the first day is 1)
     * @param   hour    hour (0-23)
     * @param   min     minute (0-59)
     * @param   sec     second (0&lt;=sec&lt;60)
     * @return  modified Julian date corresponding to arguments
     */
    protected static double dateToMjd( int year, int month, int day,
                                       int hour, int min, double sec ) {
        Calendar cal = getKit().calendar_;
        cal.clear();
        int intMillis = (int) Math.round( sec * 1000.0 );
        cal.set( year, month - 1, day, hour, min, intMillis / 1000 );
        cal.set( Calendar.MILLISECOND, intMillis % 1000 );
        try {
            return unixMillisToMjd( cal.getTimeInMillis() );
        }
        catch ( RuntimeException e ) {
            return Double.NaN;
        }
    }

    /**
     * Converts from milliseconds since the Unix epoch (1970-01-01T00:00:00)
     * to a modified Julian date value
     *
     * @param   unixMillis  milliseconds since the Unix epoch
     * @return  modified Julian date
     */
    protected static double unixMillisToMjd( long unixMillis ) {
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
     * Converts a Modified Julian Date value to an ISO 8601-format date-time
     * string.  The output format is <code>yyyy-mm-ddThh:mm:ss</code>.
     *
     * @param   mjd  modified Julian date
     * @return  ISO 8601 format date corresponding to <code>mjd</code>
     */
    private static String mjdToIso( double mjd ) {
        return formatMjd( mjd, getKit().isoDateTimeFormat_ );
    }

    /**
     * Formats an MJD using a given date formatting object.
     *
     * @param  mjd  modified Julian date
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
     * Returns a date kit private to the calling thread.
     *
     * @return  date kit
     */
    protected static DateKit getKit() {
        return kitHolder_.get();
    }

    /**
     * Helper class which contains all the items which are potentially
     * expensive to produce but cannot be shared by different threads.
     * An instance of this class is managed by a ThreadLocal.
     */
    protected static class DateKit {
        final Calendar calendar_ = newCalendar();
        final DateFormat isoDateTimeFormat_ =
            newDateFormat( DATE_PATTERN + "'" + DATE_SEP + "'" + TIME_PATTERN );
    }
}
