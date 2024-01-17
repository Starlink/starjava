package uk.ac.starlink.hapi;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import uk.ac.starlink.table.TimeMapper;

/**
 * Utilities for manipulating time values.
 * Unix seconds are seconds elapsed since 1970-01-01T00:00:00.
 * ISO-8601 refers to the restricted profile used by HAPI.
 *
 * @author   Mark Taylor
 * @since    24 Jan 2024
 */
public class Times {

    private static final TimeZone UTC = TimeZone.getTimeZone( "UTC" );

    /**
     * Private sole constructor prevents instantiation.
     */
    private Times() {
    }

    /**
     * Decodes an ISO-8601 string to Unix seconds.
     *
     * @param  isoTime  epoch specified in ISO-8601
     * @return  epoch in Unix seconds, or NaN if isoTime can't be parsed
     */
    public static double isoToUnixSeconds( String isoTime ) {
        return TimeMapper.ISO_8601.toUnixSeconds( isoTime );
    }

    /**
     * Formats an epoch in Unix seconds using a supplied date format.
     *
     * @param  unixSec  time in unix seconds
     * @param  format   time format
     * @return  formatted date
     */
    public static String formatUnixSeconds( long unixSec, DateFormat format ) {
        return format.format( unixSec * 1000L );
    }

    /**
     * Formats an epoch in Unix seconds using a supplied date pattern.
     *
     * @param  unixSec  time in unix seconds
     * @param  fmt  time format
     * @return  formatted date
     * @see   java.text.SimpleDateFormat
     */
    public static String formatUnixSeconds( long unixSec, String fmt ) {
        return formatUnixSeconds( unixSec, createDateFormat( fmt ) );
    }

    /**
     * Creates a format for rendering dates.
     *
     * @param  pattern  format pattern
     * @return  format
     * @see   java.text.SimpleDateFormat
     */
    public static DateFormat createDateFormat( String pattern ) {
        DateFormat fmt = new SimpleDateFormat( pattern );
        fmt.setTimeZone( UTC );
        fmt.setCalendar( createCalendar() );
        return fmt;
    }

    /**
     * Converts unix seconds to year since 0AD.
     *
     * @param  sec  epoch in unix seconds
     * @return  year in which epoch occurs
     */
    public static int secToYear( long sec ) {
        Calendar cal = createCalendar();
        cal.setTimeInMillis( 1000 * sec );
        return cal.get( Calendar.YEAR );
    }

    /**
     * Converts year since 0AD to unix seconds.
     *
     * @param  year  year
     * @return  unix seconds
     */
    public static long yearToSec( int year ) {
        Calendar cal = createCalendar();
        cal.clear();
        cal.set( Calendar.YEAR, year );
        return cal.getTimeInMillis() / 1000L;
    }

    /**
     * Returns a new Gregorian calendar that uses a standard time zone
     * and locale, initialised to the current time.
     *
     * @return  new calendar
     */
    private static Calendar createCalendar() {
        Calendar cal = new GregorianCalendar( UTC, Locale.UK );
        cal.setLenient( true );
        return cal;
    }
}
