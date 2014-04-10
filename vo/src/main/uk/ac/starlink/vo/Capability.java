package uk.ac.starlink.vo;

/**
 * Describes a capability of a registry service.
 *
 * @author   Mark Taylor
 * @since    3 Jul 2009
 */
public class Capability {

    private final String standardId_;
    private final String xsiTypeTail_;

    /** Cone search capability. */
    public static final Capability CONE =
        new Capability( "ivo://ivoa.net/std/ConeSearch", "ConeSearch" );

    /** Simple Image Access capability. */
    public static final Capability SIA =
        new Capability( "ivo://ivoa.net/std/SIA", "SimpleImageAccess" );

    /** Simple Spectral Access capability. */
    public static final Capability SSA =
        new Capability( "ivo://ivoa.net/std/SSA", "SimpleSpectralAccess" );

    /** Table Access Protocol capability. */
    public static final Capability TAP =
        new Capability( "ivo://ivoa.net/std/TAP", "TableAccess" );

    /**
     * Constructor.
     *
     * @param  standardId  capability/@standardID for the capability
     * @param  xsiTypeTail  trailing part of the capability/@xsi:type for
     *         the capability
     */
    public Capability( String standardId, String xsiTypeTail ) {
        standardId_ = standardId;
        xsiTypeTail_ = xsiTypeTail;
    }

    /**
     * Returns the capability/@standardID string for this capability.
     *
     * @return  ivorn for standard
     */
    public String getStandardId() {
        return standardId_;
    }

    /**
     * Returns the final part of the capability/@xsi:type for this capability.
     *
     * @return   xsi:type tail
     */
    public String getXsiTypeTail() {
        return xsiTypeTail_;
    }

    @Override
    public String toString() {
        return xsiTypeTail_;
    }
}
