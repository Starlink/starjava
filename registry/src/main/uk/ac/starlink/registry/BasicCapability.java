package uk.ac.starlink.registry;

/**
 * Describes a service capability interface belonging to a registry resource.
 * This typically provides an access URL at which the service can be found,
 * as well as some other metadata.
 *
 * <p>This class conflates the concepts of Capability and Interface.
 * In VOResource 1.0 the Capability:Interface relationship is one:many,
 * but in practice it is, as far as I can see, nearly always one:one.
 * In the case of finding an actual one:many relationship, we just use
 * multiple {@link BasicCapability} objects all related to the same
 * Capability.
 *
 * @author   Mark Taylor
 */
public class BasicCapability {

    private String accessUrl_;
    private String description_;
    private String standardId_;
    private String version_;
    private String xsiType_;

    /**
     * Sets the access URL.
     *
     * @param  accessUrl  access URL
     */
    public void setAccessUrl( String accessUrl ) {
        accessUrl_ = accessUrl;
    }

    /**
     * Returns the access URL.
     *
     * @return  access URL
     */
    public String getAccessUrl() {
        return accessUrl_;
    }

    /**
     * Sets the standard ID.
     *
     * @param  standardId  standard ID identifier URI
     */
    public void setStandardId( String standardId ) {
        standardId_ = standardId;
    }

    /**
     * Returns the standard ID which defines what sort of service this 
     * capability is offering.
     *
     * @return  standard ID identifier URI
     */
    public String getStandardId() {
        return standardId_;
    }

    /**
     * Sets the xsi:type.
     *
     * @param  xsiType  capability/@xsi:type
     */
    public void setXsiType( String xsiType ) {
        xsiType_ = xsiType;
    }

    /**
     * Returns the xsi:type of this capability.
     * This seems to provide similar information to that in the standardId,
     * but these fields may be used in different ways by different registries.
     *
     * @return  capability/@xsi:type
     */
    public String getXsiType() {
        return xsiType_;
    }

    /**
     * Sets the description.
     *
     * @param  description  description
     */
    public void setDescription( String description ) {
        description_ = description;
    }

    /**
     * Returns a textual description of this capability.
     *
     * @return  description
     */
    public String getDescription() {
        return description_;
    }

    /**
     * Sets the version.
     *
     * @param  version   version string
     */
    public void setVersion( String version ) {
        version_ = version;
    }

    /**
     * Returns a version string associated with this capability.
     *
     * @return  version
     */
    public String getVersion() {
        return version_;
    }
}
