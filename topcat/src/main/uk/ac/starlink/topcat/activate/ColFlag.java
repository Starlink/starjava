package uk.ac.starlink.topcat.activate;

/**
 * Used to mark columns in a TopcatModelInfo as apparently having
 * some characteristic or other.
 *
 * @author   Mark Taylor
 * @since    26 Mar 2018
 */
public enum ColFlag {

    /** Column value type is string. */
    STRING,

    /** Column (probably) contains a URL. */
    URL,

    /** Column (probably) contains a Datalink URL. */
    DATALINK,

    /** Column (probably) contains an Image URL. */
    IMAGE,

    /** Column (probably) contains a Spectrum URL. */
    SPECTRUM,

    /** Column (probably) contains a VOTable URL. */
    VOTABLE,

    /** Column (probably) contains a browser-friendly URL. */
    HTML,

    /**
     * Column (probably) contains a reference that can be mapped to a web page.
     * An example is a DOI or Bibcode.
     */
    WEBREF,

    /** Column (probably) contains a MIME type. */
    MIME;

    private final int mask1_;

    /**
     * Constructor.
     */
    ColFlag() {
        assert ordinal() < Integer.SIZE;
        mask1_ = 1 << ordinal();
    }

    /**
     * Returns a bitmask integer corresponding to the presence or
     * absence of this attribute.
     *
     * @param  isSet  true for attribute set, false for unset
     * @return   integer bitmask indicating characteristic status
     */
    int toMask( boolean isSet ) {
        return isSet ? mask1_ : 0;
    }

    /**
     * Indicates whether this characteristic is marked in a given
     * bitmask integer.
     *
     * @param  mask   integer bitmask indicating characteristic status
     * @return   true for attribute set, false for unset
     */
    boolean isSet( int mask ) {
        return ( mask & mask1_ ) != 0;
    }
}
