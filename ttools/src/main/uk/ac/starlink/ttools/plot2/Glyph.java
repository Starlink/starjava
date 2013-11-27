package uk.ac.starlink.ttools.plot2;

import java.awt.Graphics;
import java.awt.Rectangle;

/**
 * Represents an uncoloured shape to be drawn, typically a small point marker.
 * Any colouring is done outside of methods of this object.
 *
 * @author   Mark Taylor
 * @since    12 Feb 2013
 */
public interface Glyph {

    /**
     * Paints a representation of a point considered to be at the origin.
     * Typically this means the painting is centred at that point.
     *
     * @param   g  graphics context
     */
    void paintGlyph( Graphics g );

    /**
     * Returns an iterator over pixel offsets for this glyph's shape,
     * considered to be at the origin.
     * Typically this means the pixel collection is centred at that point.
     * The returned iterator must not include any points outside of
     * the given clip rectangle.
     * A null return indicates that no pixels fall within the given clip.
     *
     * @param   clip  clip rectangle
     * @return   pixel iterator for the intersection of this glyph's shape
     *           and the given clip, or null for no pixels
     */
    Pixer createPixer( Rectangle clip );
}
