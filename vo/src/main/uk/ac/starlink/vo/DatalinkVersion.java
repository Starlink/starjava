package uk.ac.starlink.vo;

/**
 * Version of the DataLink protocol.
 *
 * @author   Mark Taylor
 * @since    17 Apr 2023
 * @see  <a href="https://www.ivoa.net/documents/DataLink/">DataLink</a>
 */
public enum DatalinkVersion {

    /** DataLink version 1.0. */
    V10( "1.0", "V1.0", "REC-DataLink-1.0" ),

    /** DataLink version 1.1. */
    V11( "1.1", "V1.1-PR", "PR-DataLink-1.1-20230413" );

    private final String number_;
    private final String name_;
    private final String fullName_;

    /**
     * Constructor.
     *
     * @param  number  numeric version, in form A.B
     * @param  name  human-readable version, may contain more information
     * @param  fullname  full version specification
     */
    private DatalinkVersion( String number, String name, String fullName ) {
        number_ = number;
        name_ = name;
        fullName_ = fullName;
    }

    /**
     * Returns the basic version number in the form MAJOR.MINOR.
     *
     * @return  version number
     */
    public String getNumber() {
        return number_;
    }

    /**
     * Returns a human-readable, possibly adorned, version name.
     *
     * @return  version name
     */
    public String getName() {
        return name_;
    }

    /**
     * Returns full specification of version.
     *
     * @return  full specification name
     */
    public String getFullName() {
        return fullName_;
    }

    /**
     * True if this version is greater than or equal to DataLink version 1.1.
     *
     * @return  true for V1.1+
     */
    public boolean is11() {
        return compareTo( V11 ) >= 0;
    }

    @Override
    public String toString() {
        return name_;
    }
}
