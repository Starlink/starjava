package uk.ac.starlink.tfcat;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Represents a TFCat TimeCoords object.
 *
 * @author   Mark Taylor
 * @since    9 Feb 2022
 */
public abstract class TimeCoords {

    /** Collection of permitted time scale values. */
    public static final Collection<String> TIME_SCALES =
            Collections
           .unmodifiableSet( new LinkedHashSet<String>( Arrays.asList(
        "GPS", "TAI", "TCB", "TCG", "TDB", "TT", "UT", "UTC", "UNKNOWN",
        "SCET", "SCLK"
    ) ) );

    /** Regex for legal TFCat/DALI ISO-8601 time representations. */
    public static final Pattern TIME_ORIGIN_REGEX =
        Pattern.compile( "(-?[0-9]+)-([0-9]{2})-([0-9]{2})" +
                         "(?:T([0-9]{2})" +
                            "(?::([0-9]{2})" +
                               "(?::([0-9]{2}(?:\\.[0-9]*)?))?" +
                            ")?" +
                         ")?" );

    /** Predefined TimeCoords instance for Unix timestamp. */
    public static final TimeCoords UNIX;

    /** Predefined TimeCoords instance for Julian Day. */
    public static final TimeCoords JD;

    /** Predefined TimeCoords instance for Modified Julian Day. */
    public static final TimeCoords MJD;

    /** Predefined TimeCoords instance for NASA Modified Julian Day. */
    public static final TimeCoords MJD_NASA;

    /** Predefined TimeCoords instance for CNES Modified Julian Day. */
    public static final TimeCoords MJD_CNES;

    /** Predefined TimeCoords instance for CDF Epoch TT2000. */
    public static final TimeCoords CDF_TT2000;

    /** Map of predefined time_coords_id strings to TimeCoord instances. */
    public static final Map<String,TimeCoords> PREDEF_MAP =
            Collections.unmodifiableMap( Arrays.asList( new TimeCoords[] {
        UNIX = createTimeCoords( "unix", "Unix Timestamp",
                                 "1970-01-01T00:00:00", "s", "UTC" ),
        JD = createTimeCoords( "jd", "Julian Day",
                               "-4712-01-01T12:00:00", "d", "UTC" ),
        MJD = createTimeCoords( "mjd", "Modified Julian Day",
                                "1858-11-17T00:00:00", "d", "UTC" ),
        MJD_NASA = createTimeCoords( "mjd_nasa", "NASA Modified Julian Day",
                                     "1968-05-24T00:00:00", "d", "UTC" ),
        MJD_CNES = createTimeCoords( "mjd_cnes", "CNES Modified Julian Day",
                                     "1950-01-01T00:00:00", "d", "UTC" ),
        CDF_TT2000 = createTimeCoords( "cdf_tt2000", "CDF Epoch TT2000",
                                       "2000-01-01T00:00:00", "ns", "TT" ),
    } ).stream().collect( Collectors.toMap( TimeCoords::toString, t -> t,
                                            ( t1, t2 ) -> t1,
                                            LinkedHashMap::new ) ) );

    /**
     * Returns this system's name.
     *
     * @return  value of name member
     */
    public abstract String getName();

    /**
     * Returns this system's units.
     *
     * @return  value of unit member
     */
    public abstract String getUnit();

    /**
     * Returns this system's time origin.
     * Should be, but is not guaranteed to be,
     * conformant to {@link #TIME_ORIGIN_REGEX}
     *
     * @return  value of time_origin member
     */
    public abstract String getTimeOrigin();

    /**
     * Returns the identifier for this system's time scale.
     * Should be, but is not guaranteed to be, one of the entries from
     * {@link #TIME_SCALES}.
     *
     * @return  value of time_scale member
     */
    public abstract String getTimeScale();

    /**
     * Creates a TimeCoords instance.
     *
     * @param   id   time_coords_id
     * @param   description  description
     * @param   origin  time_origin
     * @param   unit   unit
     * @param   scale  time_scale
     * @return  TimeCoords instance
     * @throws  AssertionError if values are not legal.
     */
    private static TimeCoords createTimeCoords( String id, String description,
                                                String origin, String unit,
                                                String scale ) {
        boolean ok = id != null && id.trim().length() > 0
                  && description != null && description.trim().length() > 0
                  && TIME_ORIGIN_REGEX.matcher( origin ).matches()
                  && ( "d".equals( unit ) ||
                       "s".equals( unit ) ||
                       "ns".equals( unit ) );
        if ( ! ok ) {
            throw new AssertionError( "Bad predefined TimeCoords: " + id );
        }
        return new TimeCoords() {
            public String getName() {
                return description;
            }
            public String getUnit() {
                return unit;
            }
            public String getTimeOrigin() {
                return origin;
            }
            public String getTimeScale() {
                return scale;
            }
            @Override
            public String toString() {
                return id;
            }
        };
    }
}
