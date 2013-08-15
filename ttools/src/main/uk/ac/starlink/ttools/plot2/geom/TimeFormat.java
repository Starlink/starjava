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
import uk.ac.starlink.ttools.plot2.Equality;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Tick;

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

    /** Time format for ISO-8601 dates. */
    public static final TimeFormat ISO8601;

    /** Time format for year decimal year. */
    public static final TimeFormat DECIMAL_YEAR;

    /** Time format for Modified Julian Date. */
    public static final TimeFormat MJD;

    /** Time format for seconds since the Unix epoch. */
    public static final TimeFormat UNIX_SECONDS;

    /** Returns a list of all the known TimeFormat implementations. */
    private static final TimeFormat[] KNOWN_FORMATS = new TimeFormat[] {
        ISO8601 = new Iso8601TimeFormat( 'T', TimeZone.getTimeZone( "UTC" ),
                                         Locale.UK ),
        DECIMAL_YEAR = new NumericTimeFormat( "Year" ) {
            public double fromUnixSeconds( double unixSec ) {
                return unixSecondsToDecimalYear( unixSec );
            }
            public double toUnixSeconds( double value ) {
                return decimalYearToUnixSeconds( value );
            }
        },
        MJD = new NumericTimeFormat( "MJD" ) {
            public double fromUnixSeconds( double unixSec ) {
                return Times
                      .decYearToMjd( unixSecondsToDecimalYear( unixSec ) );
            }
            public double toUnixSeconds( double value ) {
                return decimalYearToUnixSeconds( Times.mjdToDecYear( value ) );
            }
        },
        UNIX_SECONDS = new NumericTimeFormat( "Unix" ) {
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
     */
    private TimeFormat( String name ) {
        name_ = name;
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
     * Returns an array of ticks suitable for labelling a time axis
     * over a given range.
     *
     * @param   unixSecMin  time lower bound in unix seconds
     * @param   unixSecMax  time upper bound in unix seconds
     * @param   withMinor   true if include minor ticks are required
     * @param   approxMajorCount   approximate number of major ticks required
     * @return  array of ticks
     */
    public abstract Tick[] getAxisTicks( double unixSecMin, double unixSecMax,
                                         boolean withMinor,
                                         int approxMajorCount );

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
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis( (long) ( unixSec * 1000 ) );
        int year = cal.get( Calendar.YEAR );
        cal.clear();
        cal.set( year, 0, 1 );
        long millis0 = cal.getTimeInMillis();
        cal.add( Calendar.YEAR, 1 );
        long millis1 = cal.getTimeInMillis();
        double milliOfYear = unixSec * 1000 - millis0;
        long millisInYear = millis1 - millis0;
        assert millisInYear > 364.9 * 24 * 60 * 60 * 1000
            && millisInYear < 366.1 * 24 * 60 * 60 * 1000;
        double yearFraction = milliOfYear / millisInYear;
        assert yearFraction >= 0 && yearFraction < 1;
        return year + yearFraction;
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

        /**
         * Constructor.
         *
         * @param   name  format name
         */
        protected NumericTimeFormat( String name ) {
            super( name );
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

        public Tick[] getAxisTicks( double unixSecMin, double unixSecMax,
                                    boolean withMinor, int approxMajorCount ) {

            /* Get the Tick class to do the hard work of calcuating the
             * labels over a given range for this time scale. */
            Tick[] ticks = Tick.getTicks( fromUnixSeconds( unixSecMin ),
                                          fromUnixSeconds( unixSecMax ),
                                          false, approxMajorCount, withMinor );

            /* Then convert the data values to be suitable for this format. */
            for ( int i = 0; i < ticks.length; i++ ) {
                Tick t0 = ticks[ i ];
                ticks[ i ] = new Tick( toUnixSeconds( t0.getValue() ),
                                       t0.getLabel() );
            }
            return ticks;
        }
    }

    /**
     * Time format for ISO-8601 dates.
     */
    private static class Iso8601TimeFormat extends TimeFormat {

        private final DateLevel[] levels_;
        private final DateLevel secLevel_;
        private final Pattern datePattern_;

        /**
         * Constructor.
         *
         * @param  dateSep  character separating ISO-8601 date from time
         *                  (normally 'T' or ' ')
         * @param  tz   calendar time zone
         * @param  locale  calendar locale
         */
        Iso8601TimeFormat( char dateSep, TimeZone tz, Locale locale ) {
            super( "ISO-8601" );
            levels_ = DateLevel.createLevels( dateSep, tz, locale );
            secLevel_ = levels_[ levels_.length - 1 ];
            assert secLevel_.getMajorSeconds() == 1;
            datePattern_ =
                Pattern.compile( "([0-9]+-[0-9][0-9]-[0-9][0-9]"
                                + "[" + dateSep + "])(.+)" );
        }
  
        public String formatTime( double unixSec, double secPrec ) {

            /* Get a level appropriate for this precision and perform basic
             * formatting down to seconds level. */
            DateLevel level = getLevel( secPrec );
            String txt = level.formatUnixSeconds( unixSec );

            /* Any formatting for sub-seconds we have to do by hand. */
            if ( secPrec <= 0.1 ) {
                int nSecDp =
                    (int) Math.round( Math.max( 0, -Math.log10( secPrec ) ) );
                long scale = (long) Math.round( Math.pow( 10, nSecDp ) );
                double fracSec = unixSec - Math.floor( unixSec );
                assert fracSec >= 0 && fracSec < 1;
                long digits1 = (long) ( scale * 1. + fracSec );
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

        public Tick[] getAxisTicks( double unixSecMin, double unixSecMax,
                                    boolean withMinor, int approxMajorCount ) {

            /* Work out the approximate interval in seconds between major
             * ticks. */
            double secPrec = 2 * ( unixSecMax - unixSecMin ) / approxMajorCount;

            /* If we're dealing with years, treat the values like decimals. */
            if ( secPrec > DateLevel.YEAR_SECS ) {
                Tick[] ticks =
                    Tick.getTicks( unixSecondsToDecimalYear( unixSecMin ),
                                   unixSecondsToDecimalYear( unixSecMax ),
                                   false, approxMajorCount,
                                   withMinor
                                   && secPrec > DateLevel.YEAR_SECS * 10 );
                for ( int i = 0; i < ticks.length; i++ ) {
                    Tick t0 = ticks[ i ];
                    ticks[ i ] =
                        new Tick( decimalYearToUnixSeconds( t0.getValue() ),
                                  t0.getLabel() );
                }
                return ticks;
            }

            /* If we're dealing with sub-second quantities, format them down
             * to seconds and treat the sub-second parts like decimals. */
            else if ( secPrec < 1 ) {
                long floor = secLevel_.floorUnixSeconds( unixSecMin );
                Tick[] ticks = Tick.getTicks( unixSecMin - floor,
                                              unixSecMax - floor,
                                              false, approxMajorCount,
                                              withMinor );
                for ( int i = 0; i < ticks.length; i++ ) {
                    Tick t0 = ticks[ i ];
                    double unixSec = t0.getValue() + floor;
                    ticks[ i ] =
                        new Tick( unixSec, formatTime( unixSec, secPrec ) );
                }
                return ticks;
            }

            /* Otherwise use DateLevel instances to break down increments
             * into date-appropriate amounts. */
            DateLevel level = getLevel( secPrec );

            /* Start at a round number, and go up in round-number amounts. */
            long floor = level.floorUnixSeconds( unixSecMin );
            long majorSec = level.getMajorSeconds();
            List<Tick> majorTickList = new ArrayList<Tick>();
            for ( long epoch = floor; epoch <= unixSecMax; epoch += majorSec ) {
                if ( epoch >= unixSecMin ) {
                    majorTickList
                   .add( new Tick( epoch, level.formatUnixSeconds( epoch ) ) );
                }
            }

            /* Perform fine adjustment on major tick labels. */
            Tick[] majorTicks =
                trimMajorTicks( majorTickList.toArray( new Tick[ 0 ] ) );

            /* Minor ticks: do the same thing. */
            if ( withMinor && level.getMinorSeconds() > 0 ) {
                long minorSec = level.getMinorSeconds();
                List<Tick> minorTickList = new ArrayList<Tick>();
                for ( long epoch = floor; epoch <= unixSecMax;
                      epoch += minorSec ) {
                    if ( epoch >= unixSecMin ) {
                        minorTickList.add( new Tick( epoch ) );
                    }
                }
                Tick[] minorTicks = minorTickList.toArray( new Tick[ 0 ] );
                return Tick.combineTicks( majorTicks, minorTicks );
            }
            else {
                return majorTicks;
            }
        }

        /**
         * Returns an object which knows about formatting etc for a given
         * level of time precision.
         *
         * @param  secPrecision   precision level in seconds
         * @return  precision-specific formatter
         */
        private DateLevel getLevel( double secPrecision ) {
            for ( int i = 0; i < levels_.length; i++ ) {
                DateLevel level = levels_[ i ];
                if ( secPrecision >= level.getMajorSeconds() ) {
                    return level;
                }
            }
            return secLevel_;
        }

        /** 
         * Performs fine adjustments on major tick labels.
         *
         * @param  ticks  raw major tick array
         * @return   adjusted major tick array (may be the same)
         */
        private Tick[] trimMajorTicks( Tick[] ticks ) {

            /* ISO-8601 strings can be rather long, and it's unwieldy to
             * use the full strings if the action is at the right hand side,
             * i.e. if most of the leading characters are invariant or
             * nearly invariant over the whole range of ticks.
             * So try to identify if that's the case, and strip the date
             * part if it doesn't change, or if it changes only once. */
            int nDateTick = 0;
            Set<String> uniqueDates = new HashSet<String>();
            Tick[] timeTicks = ticks.clone();
            int iLeadTick = 0;
            for ( int i = 0; i < ticks.length; i++ ) {
                Tick tick = ticks[ i ];
                String label = tick.getLabel();
                if ( label != null ) {
                    Matcher matcher = datePattern_.matcher( label );
                    if ( matcher.matches() ) {
                        String date = matcher.group( 1 );
                        String time = matcher.group( 2 );
                        int oldSize = uniqueDates.size();
                        uniqueDates.add( date );
                        if ( oldSize == 1 && uniqueDates.size() == 2 ) {
                            iLeadTick = i;
                        }
                        timeTicks[ i ] = new Tick( tick.getValue(), time );
                        nDateTick++;
                    }
                }
            }

            /* If the dates don't change much, replace most of the tick
             * labels by time parts only.  Make sure at least one includes
             * the date for context though. */
            int nUniqueDates = uniqueDates.size();
            if ( nUniqueDates <= 2 && nDateTick > nUniqueDates ) {
                timeTicks[ iLeadTick ] = ticks[ iLeadTick ];
                return timeTicks;
            }

            /* Otherwise (several different dates) leave them as input. */
            else {
                return ticks;
            }
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
        private final DateFormat format_;

        private static final long MINUTE_SECS = 60;
        private static final long HOUR_SECS = 60 * MINUTE_SECS;
        private static final long DAY_SECS = 24 * HOUR_SECS;
        private static final long YEAR_SECS = ( ( 356*4 + 1 ) * DAY_SECS ) / 4;
        private static final long MONTH_SECS = YEAR_SECS / 12;

        /**
         * Constructor.
         *
         * @param   majorSec  major tick interval in seconds
         * @param   minorSec  minor tick interval in seconds
         * @param   pattern   date format pattern
         * @param   tz      time zone
         * @param   locale  locale
         */
        DateLevel( long majorSec, long minorSec, String pattern,
                   TimeZone tz, Locale locale ) {
            majorSec_ = majorSec;
            minorSec_ = minorSec;
            calendar_ = new GregorianCalendar( tz, locale );
            format_ = new SimpleDateFormat( pattern );
            format_.setTimeZone( tz );
            format_.setCalendar( calendar_ );
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
         *
         * @param  unixSec  seconds since the unix epoch
         * @return   formatted value
         */
        public synchronized String formatUnixSeconds( double unixSec ) {

            /* Method is synchronized since DateFormat is not thread-safe. */
            long lunixSec = (long) Math.floor( unixSec );
            return format_.format( new Date( lunixSec * 1000 ) );
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

        /**
         * Generates an ordered list of levels spanning the whole range
         * from years to seconds (but not outside that range).
         */
        public static DateLevel[] createLevels( char dateSep, TimeZone tz,
                                                Locale locale ) {
            String yearFormat = "yyyy";
            String monthFormat = yearFormat + "-MM";
            String dayFormat = monthFormat + "-dd";
            String hourFormat = dayFormat + "'" + dateSep + "'" + "HH";
            String minuteFormat = hourFormat + ":mm";
            String secondFormat = minuteFormat + ":ss";
            return new DateLevel[] {
                new DateLevel( YEAR_SECS, MONTH_SECS, yearFormat,
                               tz, locale ) {
                    void roundDown( Calendar cal ) {
                        int year = cal.get( Calendar.YEAR );
                        cal.clear();
                        cal.set( year, 0, 0 );
                    }
                },
                new DateLevel( MONTH_SECS, DAY_SECS, monthFormat,
                               tz, locale ) {
                    void roundDown( Calendar cal ) {
                        int year = cal.get( Calendar.YEAR );
                        int month = cal.get( Calendar.MONTH );
                        cal.clear();
                        cal.set( year, month, 0 );
                    }
                },
                new DateLevel( DAY_SECS, HOUR_SECS * 6, dayFormat,
                               tz, locale ) {
                    void roundDown( Calendar cal ) {
                        int year = cal.get( Calendar.YEAR );
                        int month = cal.get( Calendar.MONTH );
                        int day = cal.get( Calendar.DAY_OF_MONTH );
                        cal.clear();
                        cal.set( year, month, day );
                    }
                },
                new DateLevel( HOUR_SECS * 6, HOUR_SECS, hourFormat,
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
                new DateLevel( HOUR_SECS, MINUTE_SECS * 10, hourFormat,
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
                new DateLevel( MINUTE_SECS * 10, MINUTE_SECS, minuteFormat,
                               tz, locale ) {
                    void roundDown( Calendar cal ) {
                        int year = cal.get( Calendar.YEAR );
                        int month = cal.get( Calendar.MONTH );
                        int day = cal.get( Calendar.DAY_OF_MONTH );
                        int hour = cal.get( Calendar.HOUR_OF_DAY );
                        int minute = cal.get( Calendar.MINUTE );
                        cal.clear();
                        cal.set( year, month, day,
                                 hour, ( minute / 10 ) * 10, 0 );
                    }
                },
                new DateLevel( MINUTE_SECS, 10, minuteFormat,
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
                new DateLevel( 10, 1, secondFormat,
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
                                 hour, minute, ( second / 10 ) * 10 );
                    }
                },
                new DateLevel( 1, 0, secondFormat,
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
        }
    }
}
