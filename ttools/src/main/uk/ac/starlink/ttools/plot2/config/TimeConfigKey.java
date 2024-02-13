package uk.ac.starlink.ttools.plot2.config;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import uk.ac.starlink.ttools.func.Times;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.geom.TimeFormat;

/**
 * Config key for values in the time domain.
 * The value returned is a time in the domain defined by
 * {@link uk.ac.starlink.table.TimeMapper}, that is unix seconds.
 *
 * @author   Mark Taylor
 * @since    15 Aug 2013
 */
public class TimeConfigKey extends ConfigKey<Double> {

    /** XML &lt;p&gt; element describing the text input format. */
    public static final String FORMAT_XML =
        PlotUtil.concatLines( new String[] {
            "<p>The value may be set with a string that can be interpreted as",
            "a decimal year",
            "(e.g. \"<code>2007.521</code>\")",
            "or an ISO-8601 string",
            "(e.g. \"<code>2007-07-10T03:57:36</code>\",",
                  "\"<code>2007-07-10T03</code>\"",
               "or \"<code>2007-07-10</code>\").",
            "Note however that the numeric value of this configuration item",
            "if accessed programmatically is seconds since 1 Jan 1970.",
            "</p>",
        } );

    private static final TimeZone UTC = TimeZone.getTimeZone( "UTC" );

    /**
     * Constructs a key with no default value.
     *
     * @param   meta  metadata
     */
    public TimeConfigKey( ConfigMeta meta ) {
        this( meta, Double.NaN );
        if ( meta.getStringUsage() == null ) {
            meta.setStringUsage( "<year-or-iso8601>" );
        }
    }

    /**
     * Constructs a key with a given default value.
     *
     * @param   meta  metadata
     * @param   dfltUnixSeconds  default value as seconds since Unix epoch
     */
    public TimeConfigKey( ConfigMeta meta, double dfltUnixSeconds ) {
        super( meta, Double.class, new Double( dfltUnixSeconds ) );
    }

    public String valueToString( Double value ) {
        double stime = value == null ? Double.NaN : value.doubleValue();
        return Double.isNaN( stime ) ? ""
                                     : formatTime( stime );
    }

    public Double stringToValue( String txt ) throws ConfigException {
        if ( txt == null || txt.trim().length() == 0 ) {
            return new Double( Double.NaN );
        }
        txt = txt.trim();
        double dval;
        try {
            dval = Double.parseDouble( txt );
        }
        catch ( NumberFormatException e ) {
            dval = Double.NaN;
        }
        if ( ! Double.isNaN( dval ) ) {
            return TimeFormat.decimalYearToUnixSeconds( dval );
        }
        double mjd;
        try {
            mjd = Times.isoToMjd( txt );
        }
        catch ( RuntimeException e ) {
            mjd = Double.NaN;
        }
        if ( ! Double.isNaN( mjd ) ) {
            return Times.mjdToUnixMillis( mjd ) * 0.001;
        }
        else {
            String msg = "Can't parse \"" + txt
                       + "\" as decimal year or ISO-8601 time";
            throw new ConfigException( this, msg );
        }
    }

    public Specifier<Double> createSpecifier() {
        return new TextFieldSpecifier<Double>( this, new Double( Double.NaN ) );
    }

    /**
     * Formats a time in unix seconds to a string.
     *
     * @param  unixSec  time in seconds since unix epoch
     * @return   formatted value (currently ISO-8601)
     */
    private String formatTime( double unixSec ) {
        SimpleDateFormat fmt = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss" );
        fmt.setTimeZone( UTC );
        fmt.setCalendar( new GregorianCalendar( UTC, Locale.UK ) );
        String ftime = fmt.format( new Date( Math.round( unixSec * 1000 ) ) );
        while ( ftime.endsWith( ":00" ) ) {
            ftime = ftime.substring( 0, ftime.length() - 3 );
        }
        if ( ftime.endsWith( "T" ) ) {
            ftime = ftime.substring( 0, ftime.length() - 1 );
        }
        if ( ftime.endsWith( "-01-01" ) ) {
            ftime = ftime.substring( 0, ftime.length() - 6 );
        }
        return ftime;
    }
}
