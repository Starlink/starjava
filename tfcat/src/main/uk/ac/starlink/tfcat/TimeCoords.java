package uk.ac.starlink.tfcat;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.regex.Pattern;

/**
 * Represents a TFCat TimeCoords object.
 *
 * @author   Mark Taylor
 * @since    9 Feb 2022
 */
public interface TimeCoords {

    /** Collection of permitted time scale values. */
    public static final Collection<String> TIME_SCALES =
            Collections
           .unmodifiableSet( new LinkedHashSet<String>( Arrays.asList(
        "GPS", "TAI", "TCB", "TCG", "TDB", "TT", "UT", "UTC", "UNKNOWN",
        "SCET", "SCLK"
    ) ) );

    /** Regex for legal TFCat/DALI ISO-8601 time representations. */
    public static final Pattern TIME_ORIGIN_REGEX =
        Pattern.compile( "([0-9]+)-([0-9]{1,2})-([0-9]{1,2})" +
                         "(?:[T ]([0-9]{1,2})" +
                            "(?::([0-9]{1,2})" +
                               "(?::([0-9]{1,2}(?:\\.[0-9]*)?))?" +
                            ")?" +
                         "Z?)?" );

    /**
     * Returns this system's identifier.
     *
     * @return  value of id member
     */
    String getId();

    /**
     * Returns this system's name.
     *
     * @return  value of name member
     */
    String getName();

    /**
     * Returns this system's units.
     *
     * @return  value of unit member
     */
    String getUnit();

    /**
     * Returns this system's time origin.
     * Should be, but is not guaranteed to be,
     * conformant to {@link #TIME_ORIGIN_REGEX}
     *
     * @return  value of time_origin member
     */
    String getTimeOrigin();

    /**
     * Returns the identifier for this system's time scale.
     * Should be, but is not guaranteed to be, one of the entries from
     * {@link #TIME_SCALES}.
     *
     * @return  value of time_scale member
     */
    String getTimeScale();
}
