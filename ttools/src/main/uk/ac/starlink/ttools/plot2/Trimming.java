package uk.ac.starlink.ttools.plot2;

import javax.swing.Icon;

/**
 * Aggregates static items that decorate a rectangular plotting area.
 * The area may be that occupied by a single plot zone or
 * by a collection of zones.
 *
 * @author   Mark Taylor
 * @since   14 Jul 2023
 */
public class Trimming {

    private final Icon legend_;
    private final float[] legPos_;
    private final String title_;

    /**
     * Constructor.
     *
     * @param   legend   legend icon if required, or null
     * @param   legPos  legend position if internal legend is required;
     *                  2-element (x,y) array, each element in range 0-1
     * @param   title   title text, or null
     */
    public Trimming( Icon legend, float[] legPos, String title ) {
        legend_ = legend;
        legPos_ = legPos == null ? null : legPos.clone();
        title_ = title;
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
     * The numeric values refer to the fractional distance in each
     * dimension at which the legend is positioned within the
     * target rectangular region.
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
