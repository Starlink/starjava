package uk.ac.starlink.vo;

import java.net.URL;

/**
 * Aggregates the set of related HTTP endpoints that together provide
 * services relating to a notional TAP service.
 * In general (though not necessarily for use in a particular context)
 * any of the various endpoints returned here may be null,
 * if there is nowhere that such a resource can be found.
 * However, non-null endpoints do not guarantee that the corresponding
 * resource actually exists.
 *
 * @author   Mark Taylor
 * @since    18 Mar 2016
 * @see     Endpoints
 */
public interface EndpointSet {

    /**
     * Returns a label for this service.
     * It should usually be the base URL for the TAP service, if one is known.
     *
     * @return  tap service identity string
     */
    String getIdentity();

    /**
     * Returns the endpoint for synchronous TAP queries.
     * Typically <code>&lt;baseUrl&gt;/sync</code>.
     *
     * @return  sync query endpoint
     */
    URL getSyncEndpoint();

    /**
     * Returns the endpoint for asynchronous TAP queries.
     * Typically <code>&lt;baseUrl&gt;/async</code>.
     *
     * @return  async query endpoint
     */
    URL getAsyncEndpoint();

    /**
     * Returns the endpoint at which the VOSI tableset document may be found.
     * Typically <code>&lt;baseUrl&gt;/tables</code>.
     *
     * @return  tables endpoint
     */
    URL getTablesEndpoint();

    /**
     * Returns the endpoint at which the VOSI capabilities document
     * may be found.
     * Typically <code>&lt;baseUrl&gt;/capabilities</code>.
     *
     * @return  capabilities endpoint
     */
    URL getCapabilitiesEndpoint();

    /**
     * Returns the endpoint at which the VOSI availability document
     * may be found.
     * Typically <code>&lt;baseUrl&gt;/availability</code>.
     */
    URL getAvailabilityEndpoint();

    /**
     * Returns the endpoint at which a DALI/TAP examples document may be found.
     * Typically <code>&lt;baseUrl&gt;/examples</code>.
     *
     * @return  examples endpoint
     */
    URL getExamplesEndpoint();
}
