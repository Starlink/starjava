package uk.ac.starlink.ttools.plot2.geom;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import uk.ac.starlink.ttools.func.Times;
import uk.ac.starlink.ttools.plot2.BasicTicker;
import uk.ac.starlink.ttools.plot2.Caption;
import uk.ac.starlink.ttools.plot2.Equality;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.PrefixTicker;
import uk.ac.starlink.ttools.plot2.Ticker;

/**
 * Formats numeric values to strings to provide axis ticks and
 * user-viewable coordinate readouts.
 *
 * @author   Mark Taylor
 * @since    12 Jul 2013
 */
@Equality
public abstract class TimeFormat {

    private final String name_;
    private final String description_;

    /** Time format for ISO-8601 dates. */
    public static final TimeFormat ISO8601;

    /** Time format for year decimal year. */
    public static final TimeFormat DECIMAL_YEAR;
    private static final NumericTimeFormat decimalYear_;  // same thing cast

    /** Time format for Modified Julian Date. */
    public static final TimeFormat MJD;

    /** Time format for seconds since the Unix epoch. */
    public static final TimeFormat UNIX_SECONDS;

    /** UTC time zone. */
    private final static TimeZone UTC = TimeZone.getTimeZone( "UTC" );

    /** Returns a list of all the known TimeFormat implementations. */
    private static final TimeFormat[] KNOWN_FORMATS = new TimeFormat[] {
        ISO8601 = new Iso8601TimeFormat( 'T', UTC, Locale.UK ),
        DECIMAL_YEAR = decimalYear_ =
                new NumericTimeFormat( "Year", "Decimal year" ) {
            public double fromUnixSeconds( double unixSec ) {
                return unixSecondsToDecimalYear( unixSec );
            }
            public double toUnixSeconds( double value ) {
                return decimalYearToUnixSeconds( value );
            }
        },
        MJD = new NumericTimeFormat( "MJD", "Modified Julian Date" ) {
            public double fromUnixSeconds( double unixSec ) {
                return Times
                      .decYearToMjd( unixSecondsToDecimalYear( unixSec ) );
            }
            public double toUnixSeconds( double value ) {
                return decimalYearToUnixSeconds( Times.mjdToDecYear( value ) );
            }
        },
        UNIX_SECONDS = new NumericTimeFormat( "Unix",
                                              "Seconds since midnight"
                                            + " of 1 Jan 1970" ) {
            public double fromUnixSeconds( double unixSec ) {
                return unixSec;
            }
            public double toUnixSeconds( double value ) {
                return value;
            }
        },
    };

    /**
     * Constructor.
     *
     * @param  name  format name
     * @param  description  short description
     */
    protected TimeFormat( String name, String description ) {
        name_ = name;
        description_ = description;
    }

    /**
     * Formats a time value to a given precision.
     *
     * @param  unixSec  time value in unix seconds
     * @param  secPrecision  precision of formatted string in seconds
     * @return  formatted time value
     */
    public abstract String formatTime( double unixSec, double secPrecision );

    /**
     * Turns a formatted time string into the equivalent value in
     * unix seconds.  This is the inverse of {@link #formatTime formatTime}.
     *
     * @param  timeStr   formatted time value
     * @return   time in unix seconds
     * @throws   NumberFormatException   if timeStr cannot be parsed to a time
     *                                   in this format
     */
    public abstract double parseTime( String timeStr );

    /**
     * Returns an object for generating ticks to label the time axis.
     *
     * @return  tick calculator
     */
    public abstract Ticker getTicker();

    /**
     * Returns the name of this format.
     *
     * @return  format name
     */
    public String getFormatName() {
        return name_;
    }

    /**
     * Returns a short description of this format.
     *
     * @return  format description
     */
    public String getFormatDescription() {
        return description_;
    }

    @Override
    public String toString() {
        return name_;
    }

    /**
     * Returns a list of all the known implementations of this class.
     *
     * @return  known time formats
     */
    public static TimeFormat[] getKnownFormats() {
        return KNOWN_FORMATS.clone();
    }

    /**
     * Converts unix seconds to decimal year.
     *
     * @param   unixSec   seconds since the Unix epoch
     * @return   years since 0 AD
     */
    public static double unixSecondsToDecimalYear( double unixSec ) {

        /* For sensible dates, get the leap seconds right. */
        double absUnixSec = Math.abs( unixSec );
        if ( absUnixSec < 1e10 && absUnixSec > 1e-1) {
            Calendar cal = new GregorianCalendar( UTC, Locale.UK );
            cal.setTimeInMillis( (long) ( unixSec * 1000 ) );
            int year = cal.get( Calendar.YEAR );
            cal.clear();
            cal.set( year, 0, 1 );
            long millis0 = cal.getTimeInMillis();
            cal.add( Calendar.YEAR, 1 );
            long millis1 = cal.getTimeInMillis();
            long millisInYear = millis1 - millis0;
            assert millisInYear > 364.9 * 24 * 60 * 60 * 1000
                && millisInYear < 366.1 * 24 * 60 * 60 * 1000;
            double milliOfYear = unixSec * 1000 - millis0;
            double yearFraction = milliOfYear / millisInYear;
            assert yearFraction >= 0 && yearFraction <= 1 : yearFraction;
            return year + yearFraction;
        }

        /* For science-fiction dates, approximate to avoid overflow. */
        else {
            return 1970. + unixSec / ( 365.25 * 24 * 60 * 60 );
        }
    }

    /**
     * Converts decimal years to unix seconds.
     *
     * @param   decYear   years since 0 AD
     * @return  seconds since the Unix epoch
     */
    public static double decimalYearToUnixSeconds( double decYear ) {
        int year = (int) decYear;
        double yearFraction = decYear - year;
        Calendar cal = new GregorianCalendar( UTC, Locale.UK );
        cal.clear();
        cal.set( year, 0, 1 );
        long millis0 = cal.getTimeInMillis();
        cal.add( Calendar.YEAR, 1 );
        long millis1 = cal.getTimeInMillis();
        long millisInYear = millis1 - millis0;
        double milliOfYear = yearFraction * millisInYear;
        double unixMillis = millis0 + milliOfYear;
        return unixMillis / 1000.0;
    }

    /**
     * Turns an ISO-8601 string into a Caption object.
     * Some manipulation of the LaTeX is done to improve rendering.
     *
     * @param  iso8601Text   ISO8601-like text string
     * @return   caption to represent text
     */
    private static Caption createTimeCaption( String iso8601Text ) {
        return Caption.createCaption(
            iso8601Text,
            txt -> txt.replaceAll( "([:-])", "\\\\text{$1}" )
        );
    }

    /**
     * Partial time format implementation for formats that are essentially
     * numeric.
     */
    private static abstract class NumericTimeFormat extends TimeFormat {

        private final BasicTicker ticker_;

        /**
         * Constructor.
         *
         * @param   name  format name
         * @param   description  format description
         */
        protected NumericTimeFormat( String name, String description ) {
            super( name, description );
            ticker_ = new BasicTicker( false ) {
                public Rule createRule( double dlo, double dhi,
                                        double approxMajorCount, int adjust ) {
                    final Rule rule =
                        LINEAR.createRule( fromUnixSeconds( dlo ),
                                           fromUnixSeconds( dhi ),
                                           approxMajorCount, adjust );
                    return new Rule() {
                        public long floorIndex( double value ) {
                            return rule
                                  .floorIndex( fromUnixSeconds( value ) );
                        }
                        public double[] getMinors( long index ) {
                            double[] minors = rule.getMinors( index );
                            for ( int i = 0; i < minors.length; i++ ) {
                                minors[ i ] = toUnixSeconds( minors[ i ] );
                            }
                            return minors;
                        }
                        public double indexToValue( long index ) {
                            return toUnixSeconds( rule.indexToValue( index ) );
                        }
                        public Caption indexToLabel( long index ) {
                            return rule.indexToLabel( index );
                        }
                    };
                }
            };
        }

        /**
         * Converts from unix seconds to the numeric form of this format.
         *
         * @param   unixSec  seconds since unix epoch
         * @return  value in this format's scale
         */
        public abstract double fromUnixSeconds( double unixSec );

        /**
         * Converts from the numeric form of this format to unix seconds.
         *
         * @param   value   value in this format's scale
         * @return  seconds since the unix epoch
         */
        public abstract double toUnixSeconds( double value );

        /**
         * Formats a unix time value considered to have a given precision.
         *
         * @param   unixSec  seconds since the unix epoch
         * @param   secPrecision  precision of time value in seconds
         */
        public String formatTime( double unixSec, double secPrecision ) {

            /* Convert to this object's time scale. */
            double val = fromUnixSeconds( unixSec );
            double prec = fromUnixSeconds( unixSec + secPrecision ) - val;

            /* Work out the number of decimal places. */
            int ndp = (int) Math.round( Math.max( 0, -Math.log10( prec ) ) );

            /* Work out the number of significant figures. */
            double aval = Math.abs( val );
            int nsf =
                Math.max( 0, (int) Math.round( -Math.log10( prec / aval ) ) );
            if ( ndp <= -3 ) {
                return PlotUtil.formatNumber( val, "0.#0", nsf - 1 );
            }
            else if ( ndp <= 0 ) {
                return Long.toString( Math.round( val ) );
            }
            else {
                return PlotUtil.formatNumber( val, "0.0", ndp );
            }
        }

        public double parseTime( String timeStr ) {
            return toUnixSeconds( Double.parseDouble( timeStr ) );
        }

        public Ticker getTicker() {
            return ticker_;
        }
    }

    /**
     * Time format for ISO-8601 dates.
     */
    private static class Iso8601TimeFormat extends TimeFormat {

        private final DateLevelSet levelSet_;
        private final Ticker ticker_;
        private final TimeZone tz_;
        private final Locale locale_;

        /**
         * Constructor.
         *
         * @param  dateSep  character separating ISO-8601 date from time
         *                  (normally 'T' or ' ')
         * @param  tz   calendar time zone
         * @param  locale  calendar locale
         */
        Iso8601TimeFormat( char dateSep, TimeZone tz, Locale locale ) {
            super( "ISO-8601",
                   "ISO 8601 date, "
                 + "of the form yyyy-mm-dd" + dateSep + "hh:mm:ss.s" );
            levelSet_ = new DateLevelSet( dateSep );
            ticker_ = new Iso8601Ticker( levelSet_, tz, locale );
            tz_ = tz;
            locale_ = locale;
        }
  
        public String formatTime( double unixSec, double secPrec ) {

            /* Get a level appropriate for this precision and perform basic
             * formatting down to seconds level. */
            DateTickLevel level = levelSet_.getLevel( secPrec );
            String txt = new DateRule( level, tz_, locale_ )
                        .formatUnixSeconds( unixSec, true, true );

            /* Any formatting for sub-seconds we have to do by hand. */
            if ( secPrec <= 0.1 ) {
                int nSecDp =
                    (int) Math.round( Math.max( 0, -Math.log10( secPrec ) ) );
                long scale = Math.round( Math.pow( 10, nSecDp ) );
                double fracSec = unixSec - Math.floor( unixSec );
                assert fracSec >= 0 && fracSec < 1;
                long digits1 = Math.round( scale * ( 1.  + fracSec ) );
                String digits = Long.toString( digits1 ).substring( 1 );
                return new StringBuffer()
                      .append( txt )
                      .append( '.' )
                      .append( digits )
                      .toString();
            }
            else {
                return txt;
            }
        }

        public double parseTime( String timeStr ) {
            final double mjd;
            try {
                mjd = Times.isoToMjd( timeStr );
            }
            catch ( NumberFormatException e ) {
                throw e;
            }
            catch ( RuntimeException e ) {
                throw (NumberFormatException)
                      new NumberFormatException().initCause( e );
            }
            return Times.mjdToUnixMillis( mjd ) * 0.001;
        }

        public Ticker getTicker() {
            return ticker_;
        }
    }

    /**
     * Ticker implementation for ISO-8601 dates.
     */
    private static class Iso8601Ticker extends PrefixTicker {
        private final DateLevelSet levelSet_;
        private final TimeZone tz_;
        private final Locale locale_;

        /**
         * Constructor.
         *
         * @param  levelSet  list of date levels which define how ticks
         *                   are done
         * @param  tz   calendar time zone
         * @param  locale  calendar locale
         */
        Iso8601Ticker( DateLevelSet levelSet, TimeZone tz, Locale locale ) {
            super( false );
            levelSet_ = levelSet;
            tz_ = tz;
            locale_ = locale;
        }

        public Rule createRule( double dlo, double dhi,
                                double approxMajorCount, int adjust ) {
            double secGap = ( dhi - dlo ) / approxMajorCount;
            int ilevel = levelSet_.getLevelIndex( secGap );

            /* If we're dealing with intervals greater than a year,
             * treat the values like decimal years. */
            if ( ilevel < 0 ) {
                BasicTicker.Rule yearRule =
                    decimalYear_.ticker_
                   .createRule( dlo, dhi, approxMajorCount, adjust );
                return new PrefixRuleAdapter( yearRule ) {
                    public Caption indexToPrefix( long index ) {
                        return null;
                    }
                    public Caption indexToSuffix( long index ) {
                        return createTimeCaption( yearRule.indexToLabel( index )
                                                 .toText() );
                    }
                };
            }

            /* If precision is between seconds and years, use the
             * appropriate date level object. */
            else if ( ilevel < levelSet_.levels_.length ) {
                ilevel = Math.max( 0, Math.min( levelSet_.levels_.length - 1,
                                                ilevel + adjust ) );
                return new DateRule( levelSet_.levels_[ ilevel ],
                                     tz_, locale_ );
            }

            /* If precision is sub-second use a custom rule that formats
             * the prefixes as ISO-8601 dates down to minute level,
             * then formats the seconds by hand for the suffixes. 
             * You need to be careful with precision here. */
            else {
                final BasicTicker.Rule secondRule =
                    BasicTicker.LINEAR
                   .createRule( dlo, dhi, approxMajorCount, adjust );
                final DateRule secondDateRule =
                    new DateRule( levelSet_.secondsLevel_, tz_, locale_ );

                /* This relies on the fact that the BasicTicker
                 * secondRule is known to be linear, and to divide
                 * seconds into an integral number of intervals. */
                final double secPerIndex =
                    secondRule.indexToValue( 1 ) - secondRule.indexToValue( 0 );
                final long indexPerSec = Math.round( 1.0 / secPerIndex );
                return new PrefixRuleAdapter( secondRule ) {
                    public Caption indexToSuffix( long index ) {
                        long minFloorIndex = getMinuteFloorIndex( index );
                        int addSecIndex = (int) ( index - minFloorIndex );
                        String txt =
                            secondRule.indexToLabel( addSecIndex ).toText();
                        double labelValue = Double.parseDouble( txt );
                        assert labelValue >= 0 && labelValue < 60 : txt;
                        if ( labelValue >= 0 && labelValue < 10 ) {
                            txt = "0" + txt;
                        }
                        return createTimeCaption( txt );
                    }
                    public Caption indexToPrefix( long index ) {
                        double minFloorSec =
                            getMinuteFloorIndex( index ) * secPerIndex;
                        String txt =
                            secondDateRule
                           .formatUnixSeconds( minFloorSec, true, false );
                        return createTimeCaption( txt );
                    }
                    private long getMinuteFloorIndex( long index ) {
                        long indexPerMin = 60 * indexPerSec;
                        long whole = index / indexPerMin;
                        long part = index % indexPerMin;
                        if ( part < 0 ) {
                            part += indexPerMin;
                            whole -= 1;
                        }
                        return whole * indexPerMin;
                    }
                };
            }
        }
    }

    /**
     * Prefix rule implementation based on a DateTickLevel object.
     */
    private static class DateRule implements PrefixTicker.Rule {

        private final DateTickLevel level_;
        private final TimeZone tz_;
        private final Locale locale_;
        private final DateFormat prefixFormat_;
        private final DateFormat suffixFormat_;
        private final GregorianCalendar calendar_;

        /**
         * Constructor.
         *
         * @param  level  date level
         * @param  tz  time zone
         * @param  locale  locale
         */
        public DateRule( DateTickLevel level, TimeZone tz, Locale locale ) {
            level_ = level;
            tz_ = tz;
            locale_ = locale;
            GregorianCalendar cal = new GregorianCalendar( tz, locale );
            prefixFormat_ = new SimpleDateFormat( level.getPrefixPattern() );
            prefixFormat_.setTimeZone( tz );
            prefixFormat_.setCalendar( cal );
            suffixFormat_ = new SimpleDateFormat( level.getSuffixPattern() );
            suffixFormat_.setTimeZone( tz );
            suffixFormat_.setCalendar( cal );
            calendar_ = new GregorianCalendar( tz, locale );
            calendar_.clear();
        }

        public Caption indexToLabel( long index ) {
            return createTimeCaption( formatUnixSeconds( indexToValue( index ),
                                                         true, true ) );
        }

        public Caption indexToSuffix( long index ) {
            return createTimeCaption( formatUnixSeconds( indexToValue( index ),
                                                         false, true ) );
        }

        public Caption indexToPrefix( long index ) {
            return createTimeCaption( formatUnixSeconds( indexToValue( index ),
                                                         true, false ) );
        }

        public long floorIndex( double value ) {
            double unixSec = value;
            long lsec = (long) Math.floor( unixSec );
            final long index;
            synchronized ( calendar_ ) {
                calendar_.clear();
                calendar_.setTimeInMillis( lsec * 1000 );
                index = level_.calendarFloorIndex( calendar_ );
            }
            return index;
        }

        public double indexToValue( long index ) {
            final long unixMillis;
            synchronized ( calendar_ ) {
                level_.setCalendarToIndex( index, calendar_ );
                unixMillis = calendar_.getTimeInMillis();
            }
            return unixMillis * 1e-3;
        }

        public double[] getMinors( long index ) {
            double baseUnixSec;
            long[] offs;
            synchronized ( calendar_ ) {
                level_.setCalendarToIndex( index, calendar_ );
                baseUnixSec = calendar_.getTimeInMillis() * 1e-3;
                offs = level_.getMinorSecondOffsets( calendar_ );
            }
            int nm = offs.length;
            double[] minors = new double[ nm ];
            for ( int im = 0; im < nm; im++ ) {
                minors[ im ] = baseUnixSec + offs[ im ];
            }
            return minors;
        }

        /**
         * Formats a time in a way appropriate for this rule's precision.
         * Prefix and/or suffix parts may be included in the result.
         *
         * @param  unixSec  seconds since the unix epoch
         * @param  prefix   true to include prefix
         * @param  suffix   true to include suffix
         * @return   formatted value
         */
        synchronized String formatUnixSeconds( double unixSec,
                                               boolean prefix,
                                               boolean suffix ) {
            long lunixSec = (long) Math.floor( unixSec );
            Date date = new Date( lunixSec * 1000 );
            StringBuffer sbuf = new StringBuffer();
            if ( prefix ) {
                sbuf.append( prefixFormat_.format( date ) );
            }
            if ( suffix ) {
                sbuf.append( suffixFormat_.format( date ) );
            }
            return sbuf.toString();
        }
    }

    /**
     * Partial adapter for BasicTicker.Rules to turn them into
     * PrefixTicker.Rules.  You need to supply the
     * <code>indexToPrefix</code> method.
     */
    private static abstract class PrefixRuleAdapter
                                  implements PrefixTicker.Rule {
        private final BasicTicker.Rule basicRule_;

        /**
         * Constructor.
         *
         * @parma   basicRule  basic rule on which the prefix rule is based
         */
        public PrefixRuleAdapter( BasicTicker.Rule basicRule ) {
            basicRule_ = basicRule;
        }
        public long floorIndex( double value ) {
            return basicRule_.floorIndex( value );
        }
        public double[] getMinors( long index ) {
            return basicRule_.getMinors( index );
        }
        public double indexToValue( long index ) {
            return basicRule_.indexToValue( index );
        }
        public Caption indexToLabel( long index ) {
            return basicRule_.indexToLabel( index );
        }
    }

    /**
     * Aggregates a list of DateTickLevel objects
     * which form an ordered sequence,
     * from a precision of years to seconds, but not outside that range.
     */
    private static class DateLevelSet {

        final DateTickLevel[] levels_;
        final DateTickLevel secondsLevel_;
        private final long maxTickSeconds_;

        /**
         * Constructor.
         * 
         * @param  dateSep  character separating ISO-8601 date from time
         *                  (normally 'T' or ' ')
         */                 
        public DateLevelSet( char dateSep ) {
            levels_ = DateTickLevel.createLevels( dateSep );
            long maxTickSeconds = -1;
            DateTickLevel secondsLevel = null;
            for ( DateTickLevel level : levels_ ) {
                long majorSecs = level.getMajorTickSeconds();
                maxTickSeconds = Math.max( majorSecs, maxTickSeconds );
                if ( majorSecs == 1 ) {
                    secondsLevel = level;
                }
            }
            assert secondsLevel != null;
            assert maxTickSeconds > 0;
            maxTickSeconds_ = maxTickSeconds;
            secondsLevel_ = secondsLevel;
        }   

        /**
         * Returns an index into the levels array giving an appropriate
         * date level for a given precision in seconds.
         * The returned value will be negative if the precision is
         * coarser than the lowest entry (1 year) and the length of the
         * levels array if the precision is finer than the highest entry
         * (1 second).
         * 
         * @param  secPrecision  precision in seconds
         * @return  index of appropriate date level
         */
        public int getLevelIndex( double secPrecision ) {
            if ( secPrecision > maxTickSeconds_ ) {
                return -1;
            }   
            else {
                for ( int i = 0; i < levels_.length; i++ ) {
                    DateTickLevel level = levels_[ i ];
                    if ( secPrecision >= level.getMajorTickSeconds() ) {
                        return i;
                    }   
                }
                return levels_.length;
            }
        }

        /**
         * Returns a DateTickLevel for a given precision.
         * 
         * @param  secPrecision  precision in seconds
         * @return  most appropriate date level, not null
         */
        public DateTickLevel getLevel( double secPrecision ) {
            int ilevel = Math.min( Math.max( getLevelIndex( secPrecision ),
                                             0 ), levels_.length - 1 );
            return levels_[ ilevel ];        
        }
    }
}
