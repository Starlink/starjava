package uk.ac.starlink.ttools.taplint;

import uk.ac.starlink.vo.TapCapability;

/**
 * Provides TAP service capability information.
 *
 * @author   Mark Taylor
 * @since     27 Jun 2011
 */
public interface CapabilityHolder {

    /**
     * Returns capabilities of a TAP service.
     *
     * @return   table capabilities object
     */
    TapCapability getCapability();
}
