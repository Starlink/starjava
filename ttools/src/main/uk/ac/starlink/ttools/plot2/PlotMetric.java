package uk.ac.starlink.ttools.plot2;

import java.awt.geom.Point2D;

/**
 * Calculates distances in data space between points on a plot surface.
 *
 * @author   Mark Taylor
 * @since    23 Jan 2019
 */
public interface PlotMetric {

    /**
     * Returns a list of zero or more labelled line segments that
     * indicate measures of distance between two user-selected points.
     * The line labels should be human-readable indications of
     * distances in data space.
     * The returned lines may for instance include a vector between
     * the supplied positions, or components of such a vector.
     *
     * <p>Behaviour is undefined if the surface is not the type expected by
     * this metric.
     * 
     * @param  surf  plot surface
     * @param  gpos0  first point
     * @param  gpos1  second point
     * @return   array of zero or more labelled line segments
     */
    LabelledLine[] getMeasures( Surface surf, Point2D gpos0, Point2D gpos1 );
}
