package uk.ac.starlink.topcat.plot;

import java.awt.Point;
import java.awt.Shape;

/**
 * Defines the mapping between graphics space and data space.
 * Graphics space is referenced in integer coordinates and refers to the
 * coordinates you deal with when you have a {@link java.awt.Graphics} object,
 * and data space is referenced in double coordinates and is
 * the space in which the data points live.
 *
 * @author   Mark Taylor (Starlink)
 * @since    21 June 2004
 */
public interface GraphicsSurface {

    /**
     * Converts a point in data space to graphics space.
     * If the point does not lie within the currently visible plotting
     * area, <tt>null</tt> should be returned.
     *
     * @param  x  data space X coordinate
     * @param  y  data space Y coordinate
     * @return  point in graphics space corresponding to (x,y), or <tt>null</tt>
     */
    Point dataToGraphics( double x, double y );

    /**
     * Returns the clip region in which points may be plotted.
     * The returned shape should be the sort which can be passed to
     * {@link java.awt.Graphics#setClip(java.awt.Shape)} - i.e. probably
     * a <tt>Rectangle</tt>.
     *
     * @return   clip region representing data zone
     */
    Shape getClip();
}
