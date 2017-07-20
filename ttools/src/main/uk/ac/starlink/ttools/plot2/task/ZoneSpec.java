package uk.ac.starlink.ttools.plot2.task;

import uk.ac.starlink.ttools.plot2.config.ConfigMap;

/**
 * Specifies a plot zone in sufficient detail to recreate it as part of
 * a STILTS plotting command.
 *
 * @author   Mark Taylor
 * @since    20 Jul 2017
 */
public class ZoneSpec {

    private final ConfigMap config_;
    private final boolean hasAux_;
    private final String title_;
    private final LegendSpec legSpec_;
    private final RampSpec auxSpec_;

    /**
     * Constructor.
     *
     * @param  config   per-zone configuration options
     * @param  hasAux   true iff this zone is going to use a global
     *                  colour ramp
     * @param  title    per-zone  plot title string, may be null
     * @param  legSpec  legend specification, or null for no legend
     * @param  auxSpec  aux axis colour ramp specification, or null for no ramp
     */
    public ZoneSpec( ConfigMap config, boolean hasAux, String title,
                     LegendSpec legSpec, RampSpec auxSpec ) {
        config_ = config;
        hasAux_ = hasAux;
        title_ = title;
        legSpec_ = legSpec;
        auxSpec_ = auxSpec;
    }

    /**
     * Returns per-zone configuration options for this zone.
     *
     * @return   zone config map
     */
    public ConfigMap getConfig() {
        return config_;
    }

    /**
     * Indicates whether this zone (any of the layers in it) is going to
     * use a global colour ramp.
     *
     * @return  true iff aux shading is used
     */
    public boolean getHasAux() {
        return hasAux_;
    }

    /**
     * Returns a title for this zone.
     *
     * @return   zone title or null
     */
    public String getTitle() {
        return title_;
    }

    /**
     * Returns an object characterising the legend display options
     * for this zone.
     *
     * @return  legend specification, or null for no legend
     */
    public LegendSpec getLegendSpec() {
        return legSpec_;
    }

    /**
     * Returns an object characterising the aux axis colour ramp display
     * options for this zone.
     *
     * @return  aux ramp specification, or null for no visible ramp
     */
    public RampSpec getAuxSpec() {
        return auxSpec_;
    }

    /**
     * Specification for legend display options.
     */
    public static class LegendSpec {
        private final boolean hasBorder_;
        private final boolean isOpaque_;
        private final float[] legPos_;

        /**
         * Constructor.
         *
         * @param  hasBorder  true if border is to be drawn
         * @param  isOpaque   true for opaque background
         * @param  legPos     2-element x,y array for fractional internal
         *                    legend position, null for external
         */
        public LegendSpec( boolean hasBorder, boolean isOpaque,
                           float[] legPos ) {
            hasBorder_ = hasBorder;
            isOpaque_ = isOpaque;
            legPos_ = legPos;
        }

        /**
         * Returns border flag.
         *
         * @return   true for border, false for none
         */
        public boolean hasBorder() {
            return hasBorder_;
        }

        /**
         * Returns legend opacity.
         *
         * @return  true for opaque legend, false for transparent
         */
        public boolean isOpaque() {
            return isOpaque_;
        }

        /**
         * Returns fractional legend position.
         *
         * @return   2-element x,y array for fractional internal
         *           legend position, null for external
         */
        public float[] getPosition() {
            return legPos_;
        }
    }

    /**
     * Specification for colour ramp display options.
     */
    public static class RampSpec {
        private final double crowding_;
        private final String label_;

        /**
         * Constructor.
         *
         * @param  crowding  tick crowding indicator, 1 is normal
         * @param  label   axis text label
         */
        public RampSpec( double crowding, String label ) {
            crowding_ = crowding;
            label_ = label;
        }

        /**
         * Returns axis tick crowding factor.
         *
         * @return   crowding factor, 1 is normal
         */
        public double getCrowding() {
            return crowding_;
        }

        /**
         * Returns axis text label.
         *
         * @return   axis text label
         */
        public String getLabel() {
            return label_;
        }
    }
}
