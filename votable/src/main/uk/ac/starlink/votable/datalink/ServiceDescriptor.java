package uk.ac.starlink.votable.datalink;

/**
 * Describes an invocable service as defined by a DataLink Service Descriptor.
 * This is usually read from a VOTable RESOURCE element
 * with @type="meta" and @utype="adhoc:service".
 *
 * @author   Mark Taylor
 * @since    22 Nov 2017
 * @see   <a href="http://www.ivoa.net/documents/DataLink/"
 *           >DataLink 1.0 or 1.1, sec 4</a>
 */
public interface ServiceDescriptor {

    /**
     * Returns the identifier for this service descriptor.
     * This corresponds to the ID attribute on the RESOURCE.
     *
     * @return  descriptor ID, may be null
     */
    String getDescriptorId();

    /**
     * Returns the access URL defined by this service.
     * It may be a partial/base URL.
     * This corresponds to the PARAM with @name="accessURL".
     *
     * @return  access URL; according to DataLink this is required,
     *          but this interface does not guarantee a non-null value
     */
    String getAccessUrl();

    /**
     * Returns the capability URI corresponding to this service.
     * This corresponds to the PARAM with @name="standardID".
     *
     * @return  standardID, may be null
     */
    String getStandardId();

    /**
     * Returns an IVOA registry identifier for this service.
     * This corresponds to the PARAM with @name="resourceIdentifier".
     *
     * @return   ivoid, may be null
     */
    String getResourceIdentifier();

    /**
     * Returns the MIME type expected for results from this service.
     * This corresponds to the PARAM with @name="contentType"
     * (introduced at DataLink 1.1).
     *
     * @return  service output content type, may be null
     */
    String getContentType();

    /**
     * Returns a name for this service.
     * This could for instance be obtained from the <code>name</code>
     * attribute on the RESOURCE.
     * Provision of this metadata is not discussed by the DataLink standard,
     * but it may be useful to have, especially for service descriptors
     * in "normal" rather than links-response VOTables.
     *
     * @return   service name, may be null
     */
    String getName();

    /**
     * Returns descriptive text for this service.
     * This could for instance be extracted from a suitable
     * <code>DESCRIPTION</code> element.
     * Provision of this metadata is not discussed by the DataLink standard,
     * but it may be useful to have, especially for service descriptors
     * in "normal" rather than links-response VOTables.
     *
     * @return  textual description of service, may be null
     */
    String getDescription();

    /**
     * Returns a list of input parameters associated with this service.
     * This corresponds to the PARAM descendants of a child GROUP
     * with @name="inputParams".
     *
     * @return   service input parameter list
     */
    ServiceParam[] getInputParams();

    /**
     * Returns a list of example invocations associated with this service.
     * This corresponds to PARAM elements with @name="exampleURL"
     * (introduced at DataLink 1.1).
     *
     * @return  list of example invocations; may be empty but not null
     */
    ExampleUrl[] getExampleUrls();
}
