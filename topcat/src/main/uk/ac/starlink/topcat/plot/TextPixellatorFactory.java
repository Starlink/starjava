package uk.ac.starlink.topcat.plot;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.font.FontRenderContext;

/**
 * Object which can produce pixel iterators to draw text labels.
 *
 * @author   Mark Taylor
 * @since    22 Aug 2007
 */
public class TextPixellatorFactory {

    private final Font font_;
    private final FontRenderContext frc_;
    private final Rectangle clip_;

    /**
     * Utility constructor based on the current state of a given 
     * graphics context.
     *
     * @param   g2  graphics context
     */
    public TextPixellatorFactory( Graphics2D g2 ) {
        this( g2.getFont(), g2.getFontRenderContext(), g2.getClipBounds() );
    }

    /**
     * Constructor.
     *
     * @param  font  font in which text will be drawn
     * @param  frc   render context to which text will be drawn
     * @param  clip  clipping region
     */
    public TextPixellatorFactory( Font font, FontRenderContext frc,
                                  Rectangle clip ) {
        font_ = font;
        frc_ = frc;
        clip_ = new Rectangle( clip );
    }

    /**
     * Returns a pixellator which will draw a given piece of text labelling
     * based at a given position.
     *
     * @param  text  label text
     * @param  x   X coordinate of text origin
     * @param  y   Y coordinate of text origin
     */
    public Pixellator createTextPixellator( String text, int x, int y ) {
        Shape outline =
            font_.createGlyphVector( frc_, text ).getOutline( x, y );
        Rectangle box = outline.getBounds().intersection( clip_ );
        if ( box.isEmpty() ) {
            return null;
        }
        else {
            Drawing drawing = new Drawing( box );
            drawing.fill( outline );
            return drawing;
        }
    }

    /**
     * Releases resources held by this object.
     */
    public void dispose() {
    }
}
