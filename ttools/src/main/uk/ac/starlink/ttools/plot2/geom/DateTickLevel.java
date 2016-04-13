package uk.ac.starlink.ttools.plot2.geom;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Instances of this class know how to format and generate round numbers
 * for unix times at a given level of precision.
 *
 * @author   Mark Taylor
 * @since    13 Apr 2016
 */
abstract class DateTickLevel {

    private final long majorSec_;
    private final String prefixPattern_;
    private final String suffixPattern_;
    private final String name_;

    private static final int MINUTE_SECS = 60;
    private static final int HOUR_SECS = 60 * MINUTE_SECS;
    private static final int DAY_SECS = 24 * HOUR_SECS;
    private static final int DAY_MILLIS = 1000 * DAY_SECS;

    /**
     * Constructor.
     *
     * @param   prefixPattern  date first part format pattern
     * @param   suffixPattern  date second part format pattern
     * @param   majorSec  approximate major tick interval in seconds
     */
    protected DateTickLevel( String prefixPattern, String suffixPattern,
                             long majorSec ) {
        prefixPattern_ = prefixPattern;
        suffixPattern_ = suffixPattern;
        majorSec_ = majorSec;
        name_ = prefixPattern + "/" + suffixPattern;
    }

    /**
     * Returns the DateFormat-friendly formatting pattern for the
     * prefix part of the formatted time.
     *
     * @return  prefix pattern string
     */
    public String getPrefixPattern() {
        return prefixPattern_;
    }

    /**
     * Returns the DateFormat-friendly formatting pattern for the
     * suffix part of the formatted time.
     *
     * @return  suffix pattern string
     */
    public String getSuffixPattern() {
        return suffixPattern_;
    }

    /**
     * Returns the approximate interval between major ticks in seconds.
     *
     * @return  major tick interval
     */
    public long getMajorTickSeconds() {
        return majorSec_;
    }

    /**
     * Calculates the highest major tick index not later than the time
     * represented by the current state of a given calendar object.
     *
     * @param  cal   calendar positioned at date of interest
     * @return  tick index
     */
    public abstract long calendarFloorIndex( GregorianCalendar cal );

    /**
     * Sets the state of a supplied calendar to the date corresponding
     * to a given major tick index.
     *
     * @param   index   tick index
     * @param   cal   calendar to update
     */
    public abstract void setCalendarToIndex( long index,
                                             GregorianCalendar cal );

    /**
     * Returns offsets in seconds from a major tick at which the
     * corresponding batch of minor ticks (up to the next major tick)
     * should be drawn.  The supplied calendar object gives the
     * position of a major tick.  Behaviour is undefined if
     * the positioning is not at a major tick.
     *
     * @param  cal  calendar specifying major tick epoch;
     *              state may be disrupted by this method
     * @return   list of offsets in seconds from calendar position
     */
    public abstract long[] getMinorSecondOffsets( GregorianCalendar cal );

    /**
     * Integer division, rounding towards negative infinity.
     *
     * @param  value  value to divide
     * @param  divisor  value to divide by
     * @return   maximum integer value not higher than value/divisor
     */
    static long floorDiv( long value, int divisor ) {
        long q = value / divisor;
        if ( value >= 0 ) {
            return q;
        }
        else if ( q * divisor == value ) {
            return q;
        }
        else {
            return q - 1;
        }
    }

    /**
     * Minimal non-negative result of modulus operation.
     *
     * @param   value to find remainder of
     * @param   divisor   modulus
     * @return   non-negative remainder of modulus division
     */
    static int remainder( long value, int divisor ) {
        long remainder = value % divisor;
        long result = remainder >= 0 ? remainder : remainder + divisor;
        assert result >= 0 && result < divisor;
        return (int) result;
    }

    /**
     * Returns a list of offsets from the current position of a calendar
     * obtained by successively setting a given field to a sequence of
     * integer values.
     *
     * @param  cal  calendar specifying base value;
     *              state may be disrupted by this method
     * @param  field   Calendar field identifier
     * @param  values   integer values for field
     * @return   array of offsets in seconds from original calendar time;
     *           same length as <code>values</code> array
     * @see   java.util.Calendar
     */
    private static long[] getSecondOffsets( GregorianCalendar cal, int field,
                                            int[] values ) {
        int noff = values.length;
        long[] secOffsets = new long[ noff ];
        long baseMillis = cal.getTimeInMillis();
        for ( int i = 0; i < noff; i++ ) {
            cal.set( field, values[ i ] );
            secOffsets[ i ] = ( cal.getTimeInMillis() - baseMillis ) / 1000;
        }
        return secOffsets;
    }

    /**
     * DateTickLevel implementation in which minor tick levels offsets
     * are independent of index.
     */
    private static abstract class FixedMinorLevel extends DateTickLevel {

        private final long[] minors_;

        /**
         * Constructor.
         *
         * @param   prefixPattern  date first part format pattern
         * @param   suffixPattern  date second part format pattern
         * @param   majorSec  approximate major tick interval in seconds
         * @param   minorSec  minor tick interval in seconds;
         *                    if &lt;=0 there are no minor ticks
         */
        FixedMinorLevel( String prefixPattern, String suffixPattern,
                         long majorSec, int minorSec ) {
            super( prefixPattern, suffixPattern, majorSec );
            if ( minorSec > 0 ) {
                int nminor = (int) ( majorSec / minorSec );
                minors_ = new long[ nminor - 1 ];
                for ( int i = 0; i < minors_.length; i++ ) {
                    minors_[ i ] = ( i + 1 ) * minorSec;
                }
            }
            else {
                minors_ = new long[ 0 ];
            }
        }

        public long[] getMinorSecondOffsets( GregorianCalendar cal ) {
            return minors_.clone();
        }
    }

    /**
     * DateTickLevel implementation in which ticks are placed linearly
     * along the line of unix seconds, with one at the Unix epoch.
     */
    private static class RegularLevel extends FixedMinorLevel {
        private final int majorMillis_;

        /**
         * Constructor.
         *
         * @param   prefixPattern  date first part format pattern
         * @param   suffixPattern  date second part format pattern
         * @param   majorSec  approximate major tick interval in seconds
         * @param   minorSec  minor tick interval in seconds
         */
        RegularLevel( String prefixPattern, String suffixPattern,
                      int majorSec, int minorSec ) {
            super( prefixPattern, suffixPattern, majorSec, minorSec );
            majorMillis_ = majorSec * 1000;
        }

        public long calendarFloorIndex( GregorianCalendar cal ) {
            return floorDiv( cal.getTimeInMillis(), majorMillis_ );
        }

        public void setCalendarToIndex( long ix, GregorianCalendar cal ) {
            cal.clear();
            cal.setTimeInMillis( ix * majorMillis_ );
        }
    }

    /**
     * Returns a list of DateTickLevel instances, in decreasing order
     * of major tick interval, which can be used for labelling time plots.
     * They range from (approximately) 1 year to (exactly) 1 second.
     *
     * @param  dateSep  character separating ISO-8601 date from time
     *                  (normally 'T' or ' ')
     * @return   ordered list of levels
     */
    public static DateTickLevel[] createLevels( char dateSep ) {
        String datePrefix = "yyyy-MM-dd'" + dateSep + "'";
        return new DateTickLevel[] {
            new DateTickLevel( "", "yyyy", (int) ( 365.25 * DAY_SECS ) ) {
                public long calendarFloorIndex( GregorianCalendar cal ) {
                    return cal.get( Calendar.YEAR );
                }
                public void setCalendarToIndex( long index,
                                                GregorianCalendar cal ) {
                    cal.clear();
                    cal.set( Calendar.YEAR, (int) index );
                }
                public long[] getMinorSecondOffsets( GregorianCalendar cal ) {
                    return getSecondOffsets( cal, Calendar.MONTH,
                                             new int[] { 3, 6, 9 } );
                }
            },
            new DateTickLevel( "yyyy-", "MM", DAY_SECS * 365 / 4 ) {
                public long calendarFloorIndex( GregorianCalendar cal ) {
                    int year = cal.get( Calendar.YEAR );
                    int quarter = cal.get( Calendar.MONTH ) / 3;
                    assert quarter >= 0 && quarter < 4;
                    return 4 * year + quarter;
                }
                public void setCalendarToIndex( long index,
                                                GregorianCalendar cal ) {
                    int year = (int) floorDiv( index, 4 );
                    int quarter = remainder( index, 4 );
                    cal.clear();
                    cal.set( Calendar.YEAR, year );
                    cal.set( Calendar.MONTH, quarter * 3 );
                }
                public long[] getMinorSecondOffsets( GregorianCalendar cal ) {
                    int month = cal.get( Calendar.MONTH );
                    int[] monthOffs = { month + 1, month + 2, };
                    return getSecondOffsets( cal, Calendar.MONTH,
                                             monthOffs );
                }
            },
            new DateTickLevel( "yyyy-", "MM", DAY_SECS * 364 / 12 ) {
                public long calendarFloorIndex( GregorianCalendar cal ) {
                    int year = cal.get( Calendar.YEAR );
                    int month = cal.get( Calendar.MONTH );
                    assert month >= 0 && month < 12;
                    return 12 * year + month;
                }
                public void setCalendarToIndex( long index,
                                                GregorianCalendar cal ) {
                    int year = (int) floorDiv( index, 12 );
                    int month = remainder( index, 12 );
                    cal.clear();
                    cal.set( Calendar.YEAR, year );
                    cal.set( Calendar.MONTH, month );
                }
                public long[] getMinorSecondOffsets( GregorianCalendar cal ) {
                    boolean has28orLess =
                        cal.getActualMaximum( Calendar.DAY_OF_MONTH ) < 29;
                    int[] days =
                          cal.getActualMaximum( Calendar.DAY_OF_MONTH ) < 29
                        ? new int[] { 8, 15, 22 }
                        : new int[] { 8, 15, 22, 29 };
                    return getSecondOffsets( cal, Calendar.DAY_OF_MONTH,
                                             days );
                }
            },
            new FixedMinorLevel( "yyyy-", "MM-dd", DAY_SECS * 7, DAY_SECS ) {
                public long calendarFloorIndex( GregorianCalendar cal ) {
                    long unixMillis = cal.getTimeInMillis();
                    long unixDay = floorDiv( unixMillis, DAY_MILLIS );
                    // epoch fell on a Thursday.  Major ticks are Monday.
                    long unixWeek = floorDiv( unixDay - 4, 7 );
                    return unixWeek;
                }
                public void setCalendarToIndex( long index,
                                                GregorianCalendar cal ) {
                    long unixDay = index * 7 + 4;
                    cal.clear();
                    cal.setTimeInMillis( unixDay * DAY_MILLIS );
                }
            },
            new RegularLevel( "yyyy-", "MM-dd", DAY_SECS, DAY_SECS / 4 ),
            new RegularLevel( datePrefix, "HH", HOUR_SECS * 6, HOUR_SECS ),
            new RegularLevel( datePrefix, "HH", HOUR_SECS, HOUR_SECS / 4 ),
            new RegularLevel( datePrefix, "HH:mm",
                              MINUTE_SECS * 15, MINUTE_SECS * 5 ),
            new RegularLevel( datePrefix, "HH:mm",
                              MINUTE_SECS * 5, MINUTE_SECS ),
            new RegularLevel( datePrefix, "HH:mm", MINUTE_SECS, 15 ),
            new RegularLevel( datePrefix + "HH:mm:", "ss", 15, 5 ),
            new RegularLevel( datePrefix + "HH:mm:", "ss", 5, 1 ),
            new RegularLevel( datePrefix + "HH:mm:", "ss", 1, 0 ),
        };
    }
}
