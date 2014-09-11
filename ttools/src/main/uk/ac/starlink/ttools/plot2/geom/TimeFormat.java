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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import uk.ac.starlink.ttools.func.Times;
import uk.ac.starlink.ttools.plot2.BasicTicker;
import uk.ac.starlink.ttools.plot2.Captioner;
import uk.ac.starlink.ttools.plot2.Equality;
import uk.ac.starlink.ttools.plot2.Orientation;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.PrefixTicker;
import uk.ac.starlink.ttools.plot2.Tick;
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

    /** Returns a list of all the known TimeFormat implementations. */
    private static final TimeFormat[] KNOWN_FORMATS = new TimeFormat[] {
        ISO8601 = new Iso8601TimeFormat( 'T', TimeZone.getTimeZone( "UTC" ),
                                         Locale.UK ),
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
        if ( Math.abs( unixSec ) < 1e10 ) {
            Calendar cal = new GregorianCalendar();
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
        Calendar cal = new GregorianCalendar( year, 0, 1 );
        long millis0 = cal.getTimeInMillis();
        cal.add( Calendar.YEAR, 1 );
        long millis1 = cal.getTimeInMillis();
        long millisInYear = millis1 - millis0;
        double milliOfYear = yearFraction * millisInYear;
        double unixMillis = millis0 + milliOfYear;
        return unixMillis / 1000.0;
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
                        public String indexToLabel( long index ) {
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
                return Long.toString( (long) Math.round( val ) );
            }
            else {
                return PlotUtil.formatNumber( val, "0.0", ndp );
            }
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
                   "ISO 8601 date, of the form yyyy-mm-ddThh:mm:ss.s" );
            levelSet_ = new DateLevelSet( dateSep, tz, locale );
            ticker_ = new Iso8601Ticker( levelSet_ );
        }
  
        public String formatTime( double unixSec, double secPrec ) {

            /* Get a level appropriate for this precision and perform basic
             * formatting down to seconds level. */
            DateLevel level = levelSet_.getLevel( secPrec );
            String txt = level.formatUnixSeconds( unixSec, true, true );

            /* Any formatting for sub-seconds we have to do by hand. */
            if ( secPrec <= 0.1 ) {
                int nSecDp =
                    (int) Math.round( Math.max( 0, -Math.log10( secPrec ) ) );
                long scale = (long) Math.round( Math.pow( 10, nSecDp ) );
                double fracSec = unixSec - Math.floor( unixSec );
                assert fracSec >= 0 && fracSec < 1;
                long digits1 = (long) Math.round( scale * ( 1.  + fracSec ) );
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

        public Ticker getTicker() {
            return ticker_;
        }
    }

    /**
     * Ticker implementation for ISO-8601 dates.
     */
    private static class Iso8601Ticker extends PrefixTicker {
        private final DateLevelSet levelSet_;

        /**
         * Constructor.
         *
         * @param  levelSet  list of date levels which define how ticks
         *                   are done
         */
        Iso8601Ticker( DateLevelSet levelSet ) {
            super( false );
            levelSet_ = levelSet;
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
                    public String indexToPrefix( long index ) {
                        return null;
                    }
                };
            }

            /* If precision is between seconds and years, use the
             * appropriate date level object. */
            else if ( ilevel < levelSet_.levels_.length ) {
                ilevel = Math.max( 0, Math.min( levelSet_.levels_.length - 1,
                                                ilevel + adjust ) );
                return new DateRule( levelSet_.levels_[ ilevel ] );
            }

            /* If precision is sub-second use a custom rule that formats
             * the prefixes as ISO-8601 dates down to minute level,
             * then adds formats the seconds by hand for the suffixes. 
             * You need to be careful with precision here. */
            else {
                final BasicTicker.Rule secondRule =
                    BasicTicker.LINEAR
                   .createRule( dlo, dhi, approxMajorCount, adjust );

                /* This relies on the fact that the BasicTicker
                 * secondRule is known to be linear, and to divide
                 * seconds into an integral number of intervals. */
                final double secPerIndex =
                    secondRule.indexToValue( 1 ) - secondRule.indexToValue( 0 );
                final long indexPerSec = Math.round( 1.0 / secPerIndex );
                return new PrefixRuleAdapter( secondRule ) {
                    @Override
                    public String indexToLabel( long index ) {
                        long minFloorIndex = getMinuteFloorIndex( index );
                        int addSecIndex = (int) ( index - minFloorIndex );
                        String label = secondRule.indexToLabel( addSecIndex );
                        double labelValue = Double.parseDouble( label );
                        assert labelValue >= 0 && labelValue < 60 : label;
                        return label;
                    }
                    public String indexToPrefix( long index ) {
                        double minFloorSec =
                            getMinuteFloorIndex( index ) * secPerIndex;
                        return levelSet_.secLevel_
                              .formatUnixSeconds( minFloorSec, true, false );
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
     * Prefix rule implementation based on a DateLevel object.
     */
    private static class DateRule implements PrefixTicker.Rule {
        private final DateLevel level_;
        private final long majorSec_;
        private final long[] minors_;

        /**
         * Constructor.
         *
         * @param  level  date level
         */
        DateRule( DateLevel level ) {
            level_ = level;
            majorSec_ = level.getMajorSeconds();
            long minorSec = level.getMinorSeconds();
            if ( minorSec > 0 ) {
                int nminor = (int) ( majorSec_ / minorSec );
                minors_ = new long[ nminor - 1 ];
                for ( int i = 0; i < minors_.length; i++ ) {
                    minors_[ i ] = ( i + 1 ) * minorSec;
                }
            }
            else {
                minors_ = new long[ 0 ];
            }
        }

        public long floorIndex( double value ) {
            return level_.floorUnixSeconds( value ) / majorSec_;
        }

        public double[] getMinors( long index ) {
            double base = indexToValue( index );
            double[] minors = new double[ minors_.length ];
            for ( int i = 0; i < minors.length; i++ ) {
                minors[ i ] = base + minors_[ i ];
            }
            return minors;
        }

        public double indexToValue( long index ) {
            return majorSec_ * index;
        }

        public String indexToLabel( long index ) {
            return level_.formatUnixSeconds( indexToValue( index ),
                                             false, true );
        }

        public String indexToPrefix( long index ) {
            return level_.formatUnixSeconds( indexToValue( index ),
                                             true, false );
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
        public String indexToLabel( long index ) {
            return basicRule_.indexToLabel( index );
        }
    }

    /**
     * Instances of this class know how to format and generate round numbers
     * for unix times at a given level of precision.
     */
    private static abstract class DateLevel {
        private final long majorSec_;
        private final long minorSec_;
        private final GregorianCalendar calendar_;
        private final DateFormat prefixFormat_;
        private final DateFormat suffixFormat_;

        /**
         * Constructor.
         *
         * @param   majorSec  major tick interval in seconds
         * @param   minorSec  minor tick interval in seconds
         * @param   prefixPattern  date first part format pattern
         * @param   suffixPattern  date second part format pattern
         * @param   tz      time zone
         * @param   locale  locale
         */
        DateLevel( long majorSec, long minorSec, String prefixPattern,
                   String suffixPattern, TimeZone tz, Locale locale ) {
            majorSec_ = majorSec;
            minorSec_ = minorSec;
            calendar_ = new GregorianCalendar( tz, locale );
            prefixFormat_ = new SimpleDateFormat( prefixPattern );
            prefixFormat_.setTimeZone( tz );
            prefixFormat_.setCalendar( calendar_ );
            suffixFormat_ = new SimpleDateFormat( suffixPattern );
            suffixFormat_.setTimeZone( tz );
            suffixFormat_.setCalendar( calendar_ );
        }

        /**
         * Works out the nearest major tick position not higher than a given
         * time in unix seconds.
         *
         * @param  unixSec   seconds since the unix epoch
         * @return   round number in unix seconds not later than unixSec
         */
        public synchronized long floorUnixSeconds( double unixSec ) {

            /* Method is synchronized since Calendar is not thread-safe. */
            long lsec = (long) Math.floor( unixSec );
            calendar_.clear();
            calendar_.setTimeInMillis( lsec * 1000 );
            roundDown( calendar_ );
            long floorMillis = calendar_.getTimeInMillis();
            assert floorMillis % 1000 == 0;
            return floorMillis / 1000;
        }

        /**
         * Takes a calendar value and rounds it down so it represents the
         * epoch of a major tick not later than the supplied value.
         *
         * @param  cal  calendar to round
         */
        abstract void roundDown( Calendar cal );

        /**
         * Formats a time in a way appropriate for this precision.
         * Prefix and/or suffix parts may be included in the result.
         *
         * @param  unixSec  seconds since the unix epoch
         * @param  prefix   true to include prefix
         * @param  suffix   true to include suffix
         * @return   formatted value
         */
        public synchronized String formatUnixSeconds( double unixSec,
                                                      boolean prefix,
                                                      boolean suffix ) {

            /* Method is synchronized since DateFormat is not thread-safe. */
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

        /**
         * Returns the interval between major ticks in seconds.
         *
         * @return  major tick interval
         */
        public long getMajorSeconds() {
            return majorSec_;
        }

        /**
         * Returns the interval between minor ticks in seconds.
         *
         * @return   minor tick interval
         */
        public long getMinorSeconds() {
            return minorSec_;
        }
    }

    /**
     * Aggregates a list of DateLevel objects which form an ordered sequence,
     * from a precision of years to seconds, but not outside that range.
     */
    private static class DateLevelSet {

        final DateLevel[] levels_;
        final DateLevel secLevel_;

        private static final long MINUTE_SECS = 60;
        private static final long HOUR_SECS = 60 * MINUTE_SECS;
        private static final long DAY_SECS = 24 * HOUR_SECS;
        private static final long WEEK_SECS = 7 * DAY_SECS;
        private static final long YEAR_SECS = ( ( 356*4 + 1 ) * DAY_SECS ) / 4;

        /**
         * Constructor.
         *
         * @param  dateSep  character separating ISO-8601 date from time
         *                  (normally 'T' or ' ')
         * @param  tz   calendar time zone
         * @param  locale  calendar locale
         */
        public DateLevelSet( char dateSep, TimeZone tz, Locale locale ) {
            String datePrefix = "yyyy-MM-dd'" + dateSep + "'";
            levels_ = new DateLevel[] {
                new DateLevel( YEAR_SECS, YEAR_SECS / 4,
                               "", "yyyy",
                               tz, locale ) {
                    void roundDown( Calendar cal ) {
                        int year = cal.get( Calendar.YEAR );
                        cal.clear();
                        cal.set( year, 0, 0 );
                    }
                },

                /* The next two are a bit dodgy - the major tick marks look
                 * like the start of the month, but they are actually a
                 * fixed number of seconds from a floor month start. */
                new DateLevel( YEAR_SECS / 4, YEAR_SECS / 12,
                               "yyyy-", "MM",
                               tz, locale ) {
                    void roundDown( Calendar cal ) {
                        int year = cal.get( Calendar.YEAR );
                        int month = cal.get( Calendar.MONTH );
                        cal.clear();
                        cal.set( year, ( month / 4 ) * 4, 0 );
                    }
                },
                new DateLevel( YEAR_SECS / 12, YEAR_SECS / 48,
                               "yyyy-", "MM",
                               tz, locale ) {
                    void roundDown( Calendar cal ) {
                        int year = cal.get( Calendar.YEAR );
                        int month = cal.get( Calendar.MONTH );
                        cal.clear();
                        cal.set( year, month, 0 );
                    }
                },
                new DateLevel( WEEK_SECS, DAY_SECS,
                               "yyyy-", "MM-dd",
                               tz, locale ) {
                    void roundDown( Calendar cal ) {
                        int year = cal.get( Calendar.YEAR );
                        int week = cal.get( Calendar.WEEK_OF_YEAR );
                        cal.clear();
                        cal.set( Calendar.YEAR, year );
                        cal.set( Calendar.WEEK_OF_YEAR, week );
                        cal.set( Calendar.DAY_OF_WEEK, 1 );
                    }
                },
                new DateLevel( DAY_SECS, HOUR_SECS * 6,
                               "yyyy-MM-", "dd",
                               tz, locale ) {
                    void roundDown( Calendar cal ) {
                        int year = cal.get( Calendar.YEAR );
                        int month = cal.get( Calendar.MONTH );
                        int day = cal.get( Calendar.DAY_OF_MONTH );
                        cal.clear();
                        cal.set( year, month, day );
                    }
                },
                new DateLevel( HOUR_SECS * 6, HOUR_SECS,
                               datePrefix, "HH",
                               tz, locale ) {
                    void roundDown( Calendar cal ) {
                        int year = cal.get( Calendar.YEAR );
                        int month = cal.get( Calendar.MONTH );
                        int day = cal.get( Calendar.DAY_OF_MONTH );
                        int hour = cal.get( Calendar.HOUR_OF_DAY );
                        cal.clear();
                        cal.set( year, month, day, ( hour / 6 ) * 6, 0, 0 );
                    }
                },
                new DateLevel( HOUR_SECS, MINUTE_SECS * 15,
                               datePrefix, "HH",
                               tz, locale ) {
                    void roundDown( Calendar cal ) {
                        int year = cal.get( Calendar.YEAR );
                        int month = cal.get( Calendar.MONTH );
                        int day = cal.get( Calendar.DAY_OF_MONTH );
                        int hour = cal.get( Calendar.HOUR_OF_DAY );
                        cal.clear();
                        cal.set( year, month, day, hour, 0, 0 );
                    }
                },
                new DateLevel( MINUTE_SECS * 15, MINUTE_SECS * 5,
                               datePrefix, "HH:mm",
                               tz, locale ) {
                    void roundDown( Calendar cal ) {
                        int year = cal.get( Calendar.YEAR );
                        int month = cal.get( Calendar.MONTH );
                        int day = cal.get( Calendar.DAY_OF_MONTH );
                        int hour = cal.get( Calendar.HOUR_OF_DAY );
                        int minute = cal.get( Calendar.MINUTE );
                        cal.clear();
                        cal.set( year, month, day,
                                 hour, ( minute / 15 ) * 15, 0 );
                    }
                },
                new DateLevel( MINUTE_SECS * 5, MINUTE_SECS,
                               datePrefix, "HH:mm",
                               tz, locale ) {
                    void roundDown( Calendar cal ) {
                        int year = cal.get( Calendar.YEAR );
                        int month = cal.get( Calendar.MONTH );
                        int day = cal.get( Calendar.DAY_OF_MONTH );
                        int hour = cal.get( Calendar.HOUR_OF_DAY );
                        int minute = cal.get( Calendar.MINUTE );
                        cal.clear();
                        cal.set( year, month, day,
                                 hour, ( minute / 5 ) * 5, 0 );
                    }
                },
                new DateLevel( MINUTE_SECS, 15,
                               datePrefix, "HH:mm",
                               tz, locale ) {
                    void roundDown( Calendar cal ) {
                        int year = cal.get( Calendar.YEAR );
                        int month = cal.get( Calendar.MONTH );
                        int day = cal.get( Calendar.DAY_OF_MONTH );
                        int hour = cal.get( Calendar.HOUR_OF_DAY );
                        int minute = cal.get( Calendar.MINUTE );
                        cal.clear();
                        cal.set( year, month, day, hour, minute, 0 );
                    }
                },
                new DateLevel( 15, 5,
                               datePrefix + "HH:", "mm:ss",
                               tz, locale ) {
                    void roundDown( Calendar cal ) {
                        int year = cal.get( Calendar.YEAR );
                        int month = cal.get( Calendar.MONTH );
                        int day = cal.get( Calendar.DAY_OF_MONTH );
                        int hour = cal.get( Calendar.HOUR_OF_DAY );
                        int minute = cal.get( Calendar.MINUTE );
                        int second = cal.get( Calendar.SECOND );
                        cal.clear();
                        cal.set( year, month, day,
                                 hour, minute, ( second / 15 ) * 15 );
                    }
                },
                new DateLevel( 5, 1,
                               datePrefix + "HH:", "mm:ss",
                               tz, locale ) {
                    void roundDown( Calendar cal ) {
                        int year = cal.get( Calendar.YEAR );
                        int month = cal.get( Calendar.MONTH );
                        int day = cal.get( Calendar.DAY_OF_MONTH );
                        int hour = cal.get( Calendar.HOUR_OF_DAY );
                        int minute = cal.get( Calendar.MINUTE );
                        int second = cal.get( Calendar.SECOND );
                        cal.clear();
                        cal.set( year, month, day,
                                 hour, minute, ( second / 5 ) * 5 );
                    }
                },
                secLevel_ =
                new DateLevel( 1, 0,
                               datePrefix + "HH:mm:", "ss",
                               tz, locale ) {
                    void roundDown( Calendar cal ) {
                        int year = cal.get( Calendar.YEAR );
                        int month = cal.get( Calendar.MONTH );
                        int day = cal.get( Calendar.DAY_OF_MONTH );
                        int hour = cal.get( Calendar.HOUR_OF_DAY );
                        int minute = cal.get( Calendar.MINUTE );
                        int second = cal.get( Calendar.SECOND );
                        cal.clear();
                        cal.set( year, month, day, hour, minute, second );
                    }
                },
            };
            assert secLevel_.getMajorSeconds() == 1;
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
            if ( secPrecision > YEAR_SECS ) {
                return -1;
            }
            else {
                for ( int i = 0; i < levels_.length; i++ ) {
                    DateLevel level = levels_[ i ];
                    if ( secPrecision >= level.getMajorSeconds() ) {
                        return i;
                    }
                }
                return levels_.length;
            }
        }

        /**
         * Returns a DateLevel for a given precision.
         *
         * @param  secPrecision  precision in seconds
         * @return  most appropriate date level, not null
         */
        public DateLevel getLevel( double secPrecision ) {
            int ilevel = Math.min( Math.max( getLevelIndex( secPrecision ),
                                             0 ), levels_.length - 1 );
            return levels_[ ilevel ];
        }
    }
}
