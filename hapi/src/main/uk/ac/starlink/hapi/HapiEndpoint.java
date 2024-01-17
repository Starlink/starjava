package uk.ac.starlink.hapi;

/**
 * Programmatic endpoints defined by the HAPI protocol.
 *
 * @author   Mark Taylor
 * @since    11 Jan 2024
 * @see  <a href="https://github.com/hapi-server/data-specification/blob/master/hapi-3.1.0/HAPI-data-access-spec-3.1.0.md#3-endpoints"
 *          >HAPI 3.1.0 Sec 3</a>
 */
public enum HapiEndpoint {

    ABOUT( "about" ),
    CAPABILITIES( "capabilities" ),
    CATALOG( "catalog" ),
    INFO( "info" ),
    DATA( "data" );

    private final String endpoint_;

    /**
     * Constructor.
     *
     * @param  endpoint  endpoint string
     */
    HapiEndpoint( String endpoint ) {
        endpoint_ = endpoint;
    }

    /**
     * Returns the endpoint string.
     *
     * @return   endpoint string
     */
    public String getEndpoint() {
        return endpoint_;
    }

    /**
     * Returns the endpoint string.
     * This just calls {@link #getEndpoint}.
     *
     * @return  endpoint string
     */
    @Override
    public String toString() {
        return endpoint_;
    }
}
