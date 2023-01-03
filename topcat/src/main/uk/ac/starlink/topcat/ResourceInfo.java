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
     * Returns the content qualifier for the resource, or null if not known.
     * This is typically a term (prefixed with "<code>#</code>") from the
     * <a href="https://www.ivoa.net/rdf/product-type/">Data Product Type</a>
     * vocabulary, as provided for instance by the
     * <code>content_qualifier</code> entry in a DataLink 1.1 Links table.
     *
     * @return   content qualifier, or null
     */
    String getContentQualifier();

    /**
     * Returns the known or inferred IVOA Standard ID for the resource,
     * or null if not known.
     *
     * @return  standardID URI, or null
     */
    String getStandardId();
}
