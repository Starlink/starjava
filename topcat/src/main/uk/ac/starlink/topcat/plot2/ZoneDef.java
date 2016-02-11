package uk.ac.starlink.topcat.plot2;

import javax.swing.Icon;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.ShadeAxisFactory;
import uk.ac.starlink.ttools.plot2.Subrange;

/**
 * Supplies information about the content and configuration
 * of a plot on a single plot surface.
 *
 * @author   Mark Taylor
 * @since    28 Jan 2016
 */
public interface ZoneDef<P,A> {

    /**
     * Returns the zone identifier object for this zone.
     *
     * @return  zone id
     */
    public ZoneId getZoneId();

    /**
     * Returns the axis control GUI component for this zone.
     *
     * @return  axis controller
     */
    AxisController<P,A> getAxisController();

    /**
     * Returns the layers to be plotted on this zone.
     *
     * @return   plot layer array
     */
    PlotLayer[] getLayers();

    /**
     * Returns the legend icon associated with this zone, if any.
     *
     * @return  legend icon, or null
     */
    Icon getLegend();

    /**
     * Returns an array indicating the fractional position of the legend
     * within the plot surface.  A null value indicates that the legend,
     * if any, is to be displayed externally to the plot.
     *
     * @return   2-element x,y fractional location in range 0..1, or null
     */
    float[] getLegendPosition();

    /**
     * Returns a title string associated with this zone, if any.
     *
     * @return  title string, or null
     */
    String getTitle();

    /**
     * Returns the shade axis factory for this zone.
     *
     * @return  shade axis factory
     */
    ShadeAxisFactory getShadeAxisFactory();

    /**
     * Fixed range for shading coordinate if known.  May be definite,
     * partial (one-ended) or null.
     *
     * @return  aux fixed range if known
     */
    Range getShadeFixRange();

    /**
     * Subrange for shading coordinate.
     *
     * @return  aux shade subrange
     */
    Subrange getShadeSubrange();

    /**
     * Log flag for shade axis.
     *
     * @return  true for log aux scaling, false for linear
     */
    boolean isShadeLog();
}
