package uk.ac.starlink.topcat.plot2;

import java.awt.Point;
import uk.ac.starlink.ttools.plot2.Surface;

/**
 * Defines how a figure is constructed from a user-supplied set of
 * vertices in graphics space.
 *
 * @author   Mark Taylor
 * @since    14 Sep 2018
 */
public interface FigureMode {

    /**
     * Returns a figure given a set of user-supplied graphics
     * points on a plot surface.  If the points are not appropriate or
     * sufficient to define an area for this mode, null is returned.
     * However a non-null Figure does not guarantee representation
     * of a non-empty area.
     *
     * @param  surf   plotting surface
     * @param  points  vertices in graphics space defining the area
     * @return  defined figure
     */
    Figure createFigure( Surface surf, Point[] points );

    /**
     * Returns a name by which this mode can be presented to the user.
     * It should distinguish this object from other options that may be
     * available in the same context, but not necessarily from all other
     * possible instances.
     *
     * @return  user-directed name
     */
    String getName();
}
