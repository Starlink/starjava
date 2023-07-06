package uk.ac.starlink.vo;

/**
 * Describes a service capability interface belonging to a registry resource.
 * This typically provides an access URL at which the service can be found,
 * as well as some other metadata.
 *
 * <p>This class aggregates a vr:interface and its parent vr:capability,
 * thus conflating the VOResource concepts of Capability and Interface.
 * The Capability:Interface relationship is actually 1:many, but
 * when this class was first written, this relationship was nearly always
 * in practice 1:1, so the arrangement was convenient.
 * With declaration of multiple securityMethod-specific interfaces however,
 * multiple interfaces per capability do actually occur.
 * For backward compatibility reasons the class structure stays the same,
 * so if you want to reconstruct a capability you will have to get
 * all the RegCapabilityInterface instances and group them by standardID.
 *
 * <p>This class is mainly intended for use in resource discovery
 * (registry interactions).
 * It does not contain all the capability and interface metadata,
 * for instance the security method ID, which behaves differently between
 * RegTAP 1.0 and 1.1.  Avoiding such version-dependent items simplifies
 * matters, as long as they are not required for service discovery.
 * Subclasses of this class may be used for more specific capability
 * descriptions used in other contexts, without having to update
 * the resource discovery code.
 *
 * <p>Note the term "interface" in the javadocs for this class (mostly)
 * refers to the vr:interface element of the VOResource data model,
 * rather than to the java language interface.
 *
 * @author   Mark Taylor
 * @since    17 Dec 2008
 * @see      <a href="http://www.ivoa.net/documents/VOResource/">VOResource</a>
 */
public interface RegCapabilityInterface {

    /**
     * Returns the access URL for this capability-interface.
     * Although VOResource permits multiple accessURLs per interface,
     * that usage is little-used, and deprecated at VOResource v1.1
     * (in favour of mirrorURL).  So implementations should just return
     * a single value here, probably the first one found.
     *
     * @return  capability/interface/accessURL/text()
     */
    String getAccessUrl();

    /**
     * Returns the standard ID which defines what sort of service this 
     * capability is offering.  This is a URI which may or may not be
     * an IVO ID.
     *
     * @return capability/@standardID
     */
    String getStandardId();

    /**
     * Returns the xsi:type of this capability.
     * Note, this is different from the xsi:type of the interface
     * (this method is not very well named).
     *
     * <p>This seems to provide similar information to that in the standardId,
     * but these fields are used in different ways by different registries.
     *
     * @return  capability/@xsi:type
     */
    String getXsiType();

    /**
     * Returns a textual description of this capability.
     *
     * @return  capability/description/text()
     */
    String getDescription();

    /**
     * Returns a version string for this capability-interface.
     *
     * @return  capability/interface/@version
     */
    String getVersion();
}
