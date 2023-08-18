package uk.ac.starlink.ttools.plot2.task;

import uk.ac.starlink.ttools.plot2.config.ConfigMap;

/**
 * Specifies a shade axis in sufficient detail to recreate it as part of
 * a STILTS plotting command.
 *
 * @author   Mark Taylor
 * @since    29 Aug 2023
 */
public class ShadeSpec {

    private final ConfigMap config_;
    private final boolean isVisible_;
    private final String label_;
    private final double crowding_;

    /**
     * Constructor.
     *
     * @param   config  configuration options including aux axis items
     * @param   isVisible   true iff aux axis is displayed
     * @param   label    aux axis label
     * @param   crowding  crowding factor on aux axis
     */
    public ShadeSpec( ConfigMap config, boolean isVisible, String label,
                      double crowding ) {
        config_ = config;
        isVisible_ = isVisible;
        label_ = label;
        crowding_ = crowding;
    }

    /**
     * Returns config map including aux axis items.
     *
     * @return  config map
     */
    public ConfigMap getConfig() {
        return config_;
    }

    /**
     * Indicates whether aux axis is visible.
     *
     * @return   true for visible aux axis, false for invisible
     */
    public boolean isVisible() {
        return isVisible_;
    }

    /**
     * Returns aux axis label.
     *
     * @return   aux axis label
     */
    public String getLabel() {
        return label_;
    }

    /**
     * Returns crowding factor for aux axis.
     *
     * @return  aux axis crowding
     */
    public double getCrowding() {
        return crowding_;
    }
}
