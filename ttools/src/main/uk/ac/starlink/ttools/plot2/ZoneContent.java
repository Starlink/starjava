package uk.ac.starlink.ttools.plot2;

import javax.swing.Icon;

/**
 * Aggregates a list of items describing what appears in one zone of
 * a Gang.
 *
 * @author   Mark Taylor
 * @since    25 Jan 2016
 */
public class ZoneContent {

    private final PlotLayer[] layers_;
    private final Icon legend_;
    private final float[] legPos_;
    private final String title_;

    /**
     * Constructor.
     *
     * @param   layers   plot layers to be painted
     * @param   legend   legend icon if required, or null
     * @param   legPos  legend position if intenal legend is required;
     *                  2-element (x,y) array, each element in range 0-1
     * @param   title   title text, or null
     */
    public ZoneContent( PlotLayer[] layers, Icon legend, float[] legPos,
                        String title ) {
        layers_ = layers.clone();
        legend_ = legend;
        legPos_ = legPos == null ? null : legPos.clone();
        title_ = title;
    }

    /**
     * Returns plot layers.
     *
     * @return   layers to paint in zone
     */
    public PlotLayer[] getLayers() {
        return layers_.clone();
    }

    /**
     * Returns legend icon.
     *
     * @return  legend icon, or null
     */
    public Icon getLegend() {
        return legend_;
    }

    /**
     * Returns legend position if internal legend is required.
     *
     * @return   2-element (x,y) array, each element in range 0-1, or null
     */
    public float[] getLegendPosition() {
        return legPos_ == null ? null : legPos_.clone();
    }

    /**
     * Returns plot title if required.
     *
     * @return   title text, or null
     */
    public String getTitle() {
        return title_;
    }
}
