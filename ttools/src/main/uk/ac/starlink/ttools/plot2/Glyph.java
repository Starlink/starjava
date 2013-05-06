package uk.ac.starlink.ttools.plot2;

import java.awt.Graphics;
import java.awt.Rectangle;
import uk.ac.starlink.ttools.plot.Pixellator;

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
     * Gives pixel offsets for the shape, centred on the origin.
     * The returned pixellator must not include any points outside of
     * the given clip rectangle.
     *
     * @param   clip  clip rectangle
     * @return   pixel iterator for the intersection of this glyph's shape
     *           and the given clip
     */
    Pixellator getPixelOffsets( Rectangle clip );
}
