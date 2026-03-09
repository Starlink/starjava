package uk.ac.starlink.ttools.mode;

import uk.ac.starlink.table.TimeMapper;

/**
 * Defines how a range in epoch can be defined by one or two supplied
 * quantities.
 *
 * @author   Mark Taylor
 * @since    10 Feb 2026
 */
public abstract class TimeIntervalType {

    private final String name_;
    private final boolean hasT1_;

    /**
     * Single epoch interval, zero extent.
     */
    public static final TimeIntervalType POINT;

    /**
     * Interval between two given epochs.
     */
    public static final TimeIntervalType RANGE;

    /**
     * Interval starting at given epoch,
     * extent given in days by secondary coordinate.
     */
    public static final TimeIntervalType DAY_EXTENT;

    /**
     * Interval centered on given epoch,
     * half-extent given in days by secondary coordinate.
     */
    public static final TimeIntervalType DAY_RADIUS;

    /**
     * Interval starting at given epoch,
     * extent given in seconds by secondary coordinate.
     */
    public static final TimeIntervalType SEC_EXTENT;

    /**
     * Interval centered on given epoch,
     * half-extent given in seconds by secondary coordinate.
     */
    public static final TimeIntervalType SEC_RADIUS;

    private static final double DAYS_PER_SEC = 1.0 / ( 60 * 60 * 24 );

    private static final TimeIntervalType[] TYPES = new TimeIntervalType[] {
        POINT = createPointInterval( "point" ),
        RANGE = createRangeInterval( "range" ),
        DAY_EXTENT = createExtentInterval( "day-extent", 1.0, "day" ),
        DAY_RADIUS = createRadiusInterval( "day-radius", 1.0, "day" ),
        SEC_EXTENT = createExtentInterval( "sec-extent", DAYS_PER_SEC,
                                           "second" ),
        SEC_RADIUS = createRadiusInterval( "sec-radius", DAYS_PER_SEC,
                                           "second" ),
    };

    /**
     * Constructor.
     *
     * @param  name  interval type name
     * @param  hasT1   true iff this interval type uses the secondary
     *                 time coordinate
     */
    protected TimeIntervalType( String name, boolean hasT1 ) {
        name_ = name;
        hasT1_ = hasT1;
    }

    /**
     * Returns the name of this interval type.
     *
     * @return  name
     */
    public String getName() {
        return name_;
    }

    /**
     * Indicates whether this interval type makes use of the secondary
     * time coordinate.
     *
     * @return  true for 2-coordinate intervals
     */
    public boolean hasT1() {
        return hasT1_;
    }

    /**
     * Returns a description of this interval type,
     * based on the UI parameter names of the primary and secondary
     * time coordinates.
     *
     * @param  t0name  name of the primary time coordinate parameter
     * @param  t1name  name of the secondary time coordinate parameter
     * @return  XML-friendly user-readable description
     */
    public abstract String getDescription( String t0name, String t1name );

    /**
     * Converts the supplied values into an interval in Julian Date.
     *
     * @param  jd0  primary time coordinate, giving the primary epoch, not NaN
     * @param  t1data  secondary time coordinate, semantics dependent on
     *                 this implementation
     * @param  mapper  mapper from user-supplied coordinate values to epoch
     * @return   2-element array giving (start, end) epoch in JD,
     *           or null if input invalid
     */
    public abstract double[] toJdInterval( double jd0, Object t1data,
                                           TimeMapper mapper );

    @Override
    public String toString() {
        return name_;
    }

    /**
     * Returns a list of useful instances of this class.
     *
     * @return   TimeIntervalType instances
     */
    public static TimeIntervalType[] getTypes() {
        return TYPES.clone();
    }

    /**
     * Returns a zero-extent interval.
     *
     * @param  name  name
     * @return  instance
     */
    private static TimeIntervalType createPointInterval( String name ) {
        return new TimeIntervalType( name, false ) {
            public String getDescription( String t0name, String t1name ) {
                return "zero-extent interval; "
                     + "<code>" + t0name + "</code> gives the epoch and "
                     + "<code>" + t1name + "</code> is not used";
            }
            public double[] toJdInterval( double jd0, Object t1data,
                                          TimeMapper mapper ) {
                return Double.isNaN( jd0 ) ? null : new double[] { jd0, jd0 };
            }
        };
    }

    /**
     * Returns a range interval.
     *
     * @param  name  name
     * @return  instance
     */
    private static TimeIntervalType createRangeInterval( String name ) {
        return new TimeIntervalType( name, true ) {
            public String getDescription( String t0name, String t1name ) {
                return "interval between two given epochs, "
                     + "given by <code>" + t0name + "</code> "
                     + "and <code>" + t1name + "</code>";
            }
            public double[] toJdInterval( double jd0, Object t1data,
                                          TimeMapper mapper ) {
                double jd1 = mapper.toJd( t1data );
                return jd0 <= jd1 ? new double[] { jd0, jd1 }
                                  : null;
            }
        };
    }

    /**
     * Returns an interval with a user-specifiable extent
     * starting at the primary epoch.
     *
     * @param  name  name
     * @param  unitDays   size of unit in days
     * @param  unitName   user-readable name of unit
     * @return  instance
     */
    private static TimeIntervalType createExtentInterval( String name,
                                                          double unitDays,
                                                          String unitName ) {
        return new TimeIntervalType( name, true ) {
            public String getDescription( String t0name, String t1name ) {
                return "interval starting at epoch <code>" + t0name + "</code> "
                     + "with an extent given in units of " + unitName
                     + " by <code>" + t1name + "</code>";
            }
            public double[] toJdInterval( double jd0, Object t1data,
                                          TimeMapper mapper ) {
                double dExtent = t1data instanceof Number
                               ? ((Number) t1data).doubleValue() * unitDays
                               : Double.NaN;
                return dExtent >= 0 && !Double.isNaN( jd0 )
                     ? new double[] { jd0, jd0 + dExtent }
                     : null;
            }
        };
    }

    /**
     * Returns an interval with a user-specifiable extent
     * centered on the primary epoch.
     *
     * @param  name  name
     * @param  unitDays   size of unit in days
     * @param  unitName   user-readable name of unit
     * @return   instance
     */
    private static TimeIntervalType createRadiusInterval( String name,
                                                          double unitDays,
                                                          String unitName ) {
        return new TimeIntervalType( name, true ) {
            public String getDescription( String t0name, String t1name ) {
                return "interval centered on epoch <code>" + t0name + "</code> "
                     + "with a half-extent given in units of " + unitName
                     + " by <code>" + t1name + "</code>";
            }
            public double[] toJdInterval( double jd0, Object t1data,
                                          TimeMapper mapper ) {
                double dRadius = t1data instanceof Number
                               ? ((Number) t1data).doubleValue() * unitDays
                               : Double.NaN;
                return dRadius >= 0 && !Double.isNaN( jd0 )
                     ? new double[] { jd0 - dRadius, jd0 + dRadius }
                     : null;
            }
        };
    }
}
