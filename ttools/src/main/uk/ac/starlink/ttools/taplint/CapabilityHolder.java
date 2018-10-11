package uk.ac.starlink.ttools.taplint;

import uk.ac.starlink.vo.StdCapabilityInterface;
import uk.ac.starlink.vo.TapCapability;

/**
 * Provides TAP service capability information.
 *
 * @author   Mark Taylor
 * @since     27 Jun 2011
 */
public interface CapabilityHolder {

    /**
     * Returns TAPRegExt capability information for a TAP service.
     *
     * @return   table capabilities object; may be null if not available
     */
    TapCapability getCapability();

    /**
     * Returns the list of declared capability/interface elements
     * from a TAP service's capabilities endpoint.
     *
     * @return  interfaces, or null if not available
     */
    StdCapabilityInterface[] getInterfaces();
}
