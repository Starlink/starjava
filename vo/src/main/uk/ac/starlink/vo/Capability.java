package uk.ac.starlink.vo;

/**
 * Describes a capability of a registry service.
 *
 * @author   Mark Taylor
 * @since    3 Jul 2009
 */
public abstract class Capability {

    /** Cone search capability. */
    public static final Capability CONE =
        createCapability( "ivo://ivoa.net/std/ConeSearch", "ConeSearch" );

    /** Simple Image Access capability. */
    public static final Capability SIA =
        createCapability( "ivo://ivoa.net/std/SIA", "SimpleImageAccess" );

    /** Simple Spectral Access capability. */
    public static final Capability SSA =
        createCapability( "ivo://ivoa.net/std/SSA", "SimpleSpectralAccess" );

    /** Table Access Protocol capability. */
    public static final Capability TAP =
        createCapability( "ivo://ivoa.net/std/TAP", "TableAccess" );

    /**
     * Returns an ADQL query which can be used to search for capabilities
     * of this type in the registry.
     *
     * @return  ADQL search query
     */
    public abstract String getAdql();

    /**
     * Indicates whether a given RegCapabilityInterface object is an
     * instance of this capability.
     *
     * @param  cap  registry object
     * @return  true iff <code>cap</code> represents a capability of this type
     */
    public abstract boolean isInstance( RegCapabilityInterface cap );

    /**
     * Constructs a capability object.
     *
     * @param  standardId  capability/@standardID for the capability
     * @param  xsiTypeTail  trailing part of the capability/@xsi:type for
     *         the capability
     */
    public static Capability createCapability( final String standardId,
                                               final String xsiTypeTail ) {
        StringBuffer abuf = new StringBuffer();
        int nterm = 0;
        if ( abuf.length() > 0 ) {
            abuf.append( " OR " );
        }
        abuf.append( "( capability/@standardID = '" )
            .append( standardId )
            .append( "' )" );
        nterm++;

        /* Some say that matching the xsiType is a good way to spot a
         * capability.  Others disagree.  Since there doesn't currently
         * seem to be any registry which works with this strategy but
         * not without it, omit this test for now. */
        //  if ( abuf.length() > 0 ) {
        //      abuf.append( " OR " );
        //  }
        //  abuf.append( "( capability/@xsi:type LIKE '" )
        //      .append( "%" )
        //      .append( xsiTypeTail )
        //      .append( "' )" );
        //  nterm++;

        /* Prepare the final ADQL search term. */
        final String adql = nterm > 1 ?
                            "( " + abuf.toString() + " )"
                          : abuf.toString();
        return new Capability() {
            public String getAdql() {
                return adql;
            }
            public boolean isInstance( RegCapabilityInterface cap ) {
                String xsiType = cap.getXsiType();
                return standardId.equals( cap.getStandardId() )
                    || ( xsiType != null && xsiTypeTail != null 
                                         && xsiType.endsWith( xsiTypeTail ) );
            }
            public String toString() {
                return xsiTypeTail;
            }
        };
    }
}
