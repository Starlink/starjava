package uk.ac.starlink.topcat.plot2;

import uk.ac.starlink.ttools.plot2.config.ConfigMap;

/**
 * Source of a config map.
 *
 * @author  Mark Taylor
 * @since   12 Mar 2013
 */
public interface Configger {

    /**
     * Returns a configuration map.
     * Calling this method will typically gather information from a GUI
     * to return.
     *
     * @return  config map
     */
    ConfigMap getConfig();
}
