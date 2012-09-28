package uk.ac.starlink.vo;

/**
 * Characterises a limit which applies to a TAP service.
 * This class can represent values encoded by the TAPRegExt
 * DataLimits and TimeLimits element types.
 *
 * @author   Mark Taylor
 * @since    8 Mar 2011
 */
public class TapLimit {

    private final long value_;
    private final boolean isHard_;
    private final String unit_;

    /** Unit string used always for time limits ({@value}). */
    public static final String SECONDS = "seconds";

    /** Unit string used for a data limit of rows ({@value}). */
    public static final String ROWS = "row";

    /** Unit string used for a data limit of bytes ({@value}). */
    public static final String BYTES = "byte";

    /**
     * Constructor.
     *
     * @param   value   limit value
     * @param   isHard  true for a hard limit, false for a default limit
     * @param   unit    unit for value
     */
    public TapLimit( long value, boolean isHard, String unit ) {
        value_ = value;
        isHard_ = isHard;
        unit_ = unit;
    }

    /**
     * Returns the limit value.
     *
     * @return   limit value
     */
    public long getValue() {
        return value_;
    }

    /**
     * Indicates whether this is a hard or soft (default) limit.
     *
     * @return   true for hard limit, false for default limit
     */
    public boolean isHard() {
        return isHard_;
    }

    /**
     * Returns the limit unit.
     * Should be {@link #SECONDS} for time limits, and either
     * {@link #ROWS} or {@link #BYTES} for data limits.
     *
     * @return   limit unit
     */
    public String getUnit() {
        return unit_;
    }

    public String toString() {
        return "TapLimit:" + value_ + unit_
             + "(" + ( isHard_ ? "hard" : "default" ) + ")";
    }
}
