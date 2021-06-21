package uk.ac.starlink.ttools.taplint;

import org.w3c.dom.Element;
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
     * Returns the top-level (presumably capabilities) element of the
     * capabilities document.
     *
     * @return  document element
     */
    Element getElement();

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

    /**
     * Returns the content of the HTTP "Server" header attached to the
     * response that supplied the capabilities.
     *
     * @return  server header value, may be null
     */
    String getServerHeader();
}
