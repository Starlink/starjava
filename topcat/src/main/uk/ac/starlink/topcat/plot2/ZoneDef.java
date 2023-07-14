package uk.ac.starlink.topcat.plot2;

import uk.ac.starlink.ttools.plot2.ShadeAxisFactory;
import uk.ac.starlink.ttools.plot2.Span;
import uk.ac.starlink.ttools.plot2.Subrange;
import uk.ac.starlink.ttools.plot2.Trimming;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;

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
    TopcatLayer[] getLayers();

    /**
     * Returns the specification for additional decorations of this zone,
     * if any.
     *
     * @return  zone trimming, or null
     */
    Trimming getTrimming();

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
    Span getShadeFixSpan();

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

    /**
     * Returns the user configuration object for per-zone configuration.
     * Note that much of this information will be redundant with the
     * other items specified here, but it may be required for reconstructing
     * the instructions that led to this zone definition.
     *
     * @return  per-zone configuration items
     */
    ConfigMap getConfig();
}
