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
    V11( "1.1-PR" );

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
     * True if this version is greater than or equal to TAP version 1.1.
     *
     * @return  true for v1.1+
     */
    public boolean is11() {
        return compareTo( V11 ) >= 0;
    }

    @Override
    public String toString() {
        return "V" + number_;
    }
}
