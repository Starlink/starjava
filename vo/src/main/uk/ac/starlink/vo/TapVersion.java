package uk.ac.starlink.vo;

/**
 * Version of the TAP protocol.
 *
 * @author   Mark Taylor
 * @since    5 Oct 2018
 * @see   <a href="http://www.ivoa.net/documents/TAP/">Table Access Protocol</a>
 */
public enum TapVersion {

    /** TAP version 1.0. */
    V10( "1.0" ),

    /** TAP version 1.1. */
    V11( "1.1" );

    private final String number_;

    /**
     * Constructor.
     *
     * @param  number  string representation of version number
     */
    TapVersion( String number ) {
        number_ = number;
    }

    /**
     * Returns a numeric text string corresponding to this version.
     *
     * @return numeric string, for instance "1.0"
     */
    public String getNumber() {
        return number_;
    }

    /**
     * True if this version is greater than or equal to TAP version 1.1.
     *
     * @return  true for v1.1+
     */
    public boolean is11() {
        return compareTo( V11 ) >= 0;
    }

    /**
     * Returns the best guess at a defined TapVersion value
     * for a given version string.
     * "1.1" maps to {@link #V11} and "1.0" (as well as most other things)
     * map to {@link #V10}.
     *
     * <p>TAP 1.1 sec 2.4 says
     * <em>"clients should treat a missing version attribute as
     *     equivalent to version='1.0'."</em>
     *
     * @param  txt  version string, may be null
     * @return   TapVersion instance, not null
     */
    public static TapVersion fromString( String txt ) {
        return txt != null && txt.trim().matches( "1[.][1-9]+" )
             ? V11
             : V10;
    }

    @Override
    public String toString() {
        return "V" + number_;
    }
}
