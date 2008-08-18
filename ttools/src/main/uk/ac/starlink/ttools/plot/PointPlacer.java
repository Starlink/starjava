package uk.ac.starlink.ttools.plot;

import java.awt.Point;

/**
 * Maps a data point onto a screen point.
 *
 * @author   Mark Taylor
 * @since    10 Apr 2008
 */
public interface PointPlacer {

    /**
     * Maps a point in data space to the screen.
     * If the supplied point would not be plotted on the currently visible
     * part of the plot surface, return null.
     *
     * @param   coords   n-dimensional point in data space
     * @return   screen position, or null
     */
    Point getXY( double[] coords );
}
