package uk.ac.starlink.vo;

/**
 * Characterises a VOResource (capability,interface) pair including
 * information that may be required at invocation time.
 * This extends the RegCapabilityInterface including some more metadata
 * that is not currently required for (and in some cases may be
 * problematic to acquire during) resource discovery.
 *
 * @author   Mark Taylor
 * @since    27 Sep 2018
 * @see   <a href="http://www.ivoa.net/documents/VOResource/">VOResource</a>
 */
public interface StdCapabilityInterface extends RegCapabilityInterface {

    /**
     * Returns the role for this capability-interface.
     * Standard interfaces are supposed to have a value of "std"
     * or starting with "std:".
     *
     * @return  capability/interface/@role
     */
    String getRole();

    /**
     * Returns the xsi:type of this capability-interface.
     * Note this is the xsi:type of the interface, not the capability.
     *
     * @return  capability/interface/@xsi:type
     */
    String getInterfaceType();

    /**
     * Returns the security method IDs declared for this capability-interface.
     * The return value is an array of zero or more strings.
     * If one of the entries is null or empty, or if the list is empty,
     * then an unauthenticated access method is considered to be declared
     * (according to the TAP 1.1 draft I'm looking at right now, volute r5332).
     *
     * @return  capability/interface/securityMethod/@standardId
     */
    String[] getSecurityMethodIds();
}
