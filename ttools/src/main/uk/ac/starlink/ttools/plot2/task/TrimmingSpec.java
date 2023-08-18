package uk.ac.starlink.ttools.plot2.task;

import uk.ac.starlink.ttools.plot2.config.ConfigMap;

/**
 * Specifies trimming items in sufficient detail to recreate them as part of
 * a STILTS plotting command.
 *
 * @author   Mark Taylor
 * @since    29 Aug 2023
 */
public class TrimmingSpec {

    private final ConfigMap config_;
    private final String title_;
    private final LegendSpec legSpec_;

    /**
     * Constructor.
     *
     * @param  config  configuration options including trimming items
     * @param  title   title text, may be null
     * @param  legSpec   specification for displayed legend,
     *                   or null for no legend
     */
    public TrimmingSpec( ConfigMap config, String title, LegendSpec legSpec ) {
        config_ = config;
        title_ = title;
        legSpec_ = legSpec;
    }

    /**
     * Returns config map including trimming items.
     *
     * @return  configuration map including legend items
     */
    public ConfigMap getConfig() {
        return config_;
    }

    /**
     * Returns the title text.
     *
     * @return  title text or null
     */
    public String getTitle() {
        return title_;
    }

    /**
     * Returns the legend specification.
     *
     * @return   legend specification, or null for no legend
     */
    public LegendSpec getLegendSpec() {
        return legSpec_;
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
}
