package uk.ac.starlink.topcat;

import java.net.URL;

/**
 * Mixed bag of information that can characterise a resource.
 * Used in conjunction with {@link ResourceType}.
 *
 * @author   Mark Taylor
 * @since    3 Jan 2022
 */
public interface ResourceInfo {

    /**
     * Returns the URL of the resource.
     *
     * @return  resource URL
     */
    URL getUrl();

    /**
     * Returns the known or inferred Content-Type string (RFC 2045)
     * for the resource, or null if not known.
     *
     * @return  content-type string, or null
     */
    String getContentType();

    /**
     * Returns the known or inferred IVOA Standard ID for the resource,
     * or null if not known.
     *
     * @return  standardID URI, or null
     */
    String getStandardId();
}
