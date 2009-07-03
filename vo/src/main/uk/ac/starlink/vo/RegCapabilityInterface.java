package uk.ac.starlink.vo;

/**
 * Describes a service capability interface belonging to a registry resource.
 * This typically provides an access URL at which the service can be found,
 * as well as some other metadata.
 *
 * <p>This class conflates the concepts of Capability and Interface.
 * In VOResource 1.0 the Capability:Interface relationship is one:many,
 * but in practice it is, as far as I can see, nearly always one:one.
 * In the case of finding an actual one:many relationship, we just use
 * multiple RegCapabilityInterface objects all related to the same
 * Capability.
 *
 * @author   Mark Taylor
 * @since    17 Dec 2008
 */
public interface RegCapabilityInterface {

    /**
     * Returns the access URL.
     *
     * @return  access URL
     */
    String getAccessUrl();

    /**
     * Returns the standard ID which defines what sort of service this 
     * capability is offering.  Some popular values of this are given as
     * public static members of this interface.
     *
     * @return  standard ID identifier URI
     */
    String getStandardId();

    /**
     * Returns the xsi:type of this capability.
     * This seems to provide similar information to that in the standardId,
     * but these fields are used in different ways by different registries.
     *
     * @return  capability/@xsi:type
     */
    String getXsiType();

    /**
     * Returns a textual description of this capability.
     *
     * @return  description
     */
    String getDescription();

    /**
     * Returns a version string associated with this capability.
     *
     * @return  version
     */
    String getVersion();
}
