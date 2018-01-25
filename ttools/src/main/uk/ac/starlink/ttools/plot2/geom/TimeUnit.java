package uk.ac.starlink.ttools.plot2.geom;

import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.PerUnitConfigKey;
import uk.ac.starlink.ttools.plot2.layer.Unit;

/**
 * Unit implementations representing time intervals measured in seconds.
 *
 * @author   Mark Taylor
 * @since    11 Jan 2018
 */
public class TimeUnit extends Unit {

    /** Microsecond. */
    public static final TimeUnit MICRO;

    /** Millisecond. */
    public static final TimeUnit MILLI;

    /** Second. */
    public static final TimeUnit SECOND;

    /** Minute. */
    public static final TimeUnit MINUTE;

    /** Hour. */
    public static final TimeUnit HOUR;

    /** Day (24 hours). */
    public static final TimeUnit DAY;

    /** Week (7 days). */
    public static final TimeUnit WEEK;

    /** Month (1/12 year). */
    public static final TimeUnit MONTH;

    /** Quarter (1/4 year). */
    public static final TimeUnit QUARTER;

    /** Year (365.25 days). */
    public static final TimeUnit YEAR;
  
    private static final TimeUnit[] VALUES = {
        MICRO = new TimeUnit( "microsec", "microsecond", "us", 1e-6,
                              "microsecond (1e-6 seconds)" ),
        MILLI = new TimeUnit( "millisec", "millisecond", "ms", 1e-3,
                              "millisecond (1e-3 seconds)" ),
        SECOND = new TimeUnit( "second", "second", "s", 1,
                               "second" ),
        MINUTE = new TimeUnit( "minute", "minute", "min", 60,
                               "minute (60 seconds)" ),
        HOUR = new TimeUnit( "hour", "hour", "h", 60 * 60,
                             "hour (3600 seconds)" ),
        DAY = new TimeUnit( "day", "day", "d", 24 * 60 * 60,
                            "day (24 * 3600 seconds)" ),
        WEEK = new TimeUnit( "week", "week", "7d", 7 * 24 * 60 * 60,
                             "week (7 days)" ),
        MONTH = new TimeUnit( "month", "month", "30.4375d",
                               356.25 * 24 * 60 * 60 / 12,
                              "month (1/12 Julian year, 30.4375 days)" ),
        QUARTER = new TimeUnit( "quarter", "quarter", "0.25a",
                                365.25 * 24 * 60 * 60 / 4,
                                "quarter (0.25 Julian year, 91.3125 days)" ),
        YEAR = new TimeUnit( "year", "year", "a", 365.25 * 24 * 60 * 60,
                             "Julian year (365.25 days)" ),
    };

    /**
     * Constructor.
     *
     * @param  label     text to appear in a selection interface
     * @param  textName  text to appear in user-directed descriptive text
     * @param  symbol    text to appear as unit metadata,
     *                   preferably compatible with the VOUnit standard
     * @param  extentInSeconds   extent in units of seconds
     * @param  description  descriptive text for XML documentation
     */
    public TimeUnit( String label, String textName, String symbol,
                     double extentInSeconds, String description ) {
        super( label, textName, symbol, extentInSeconds, description );
    }

    /**
     * Returns the extent in seconds.
     *
     * @return  unit size in seconds
     */
    public double getExtentInSeconds() {
        return getExtent();
    }

    /**
     * Returns a list of known TimeUnit instances.
     *
     * @return   time unit options
     */
    public static TimeUnit[] getKnownValues() {
        return VALUES.clone();
    }

    /**
     * Returns a new config key for choosing a TimeUnit.
     * It is suitable for use with histogram-like plots in which
     * the horizontal axis represents time.
     *
     * @return  time unit config key
     */
    public static PerUnitConfigKey<Unit> createHistogramConfigKey() {
        ConfigMeta meta = new ConfigMeta( "perunit", "Per Unit" );
        meta.setShortDescription( "Time unit for densities" );
        meta.setXmlDescription( new String[] {
            "<p>Defines the amount of time used to scale values.",
            "If the combination mode is density-like",
            "this parameter controls what amount of time",
            "sums or counts are divided by to produce density values.",
            "For unscaled combination modes like sum or mean,",
            "it has no effect.",
            "</p>",
        } );
        return new PerUnitConfigKey<Unit>( meta, Unit.class, VALUES,
                                           TimeUnit.SECOND );
    }
}
