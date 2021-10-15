package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Rectangle;
import uk.ac.starlink.ttools.plot2.Glyph;
import uk.ac.starlink.ttools.plot2.Pixer;

/**
 * Abstract Glyph subclass that uses a PixelDrawing.
 *
 * @author   Mark Taylor
 * @since    7 Oct 2021
 */
public abstract class DrawingGlyph implements Glyph {

    /**
     * Returns a drawing that can dispense pixers for this glyph.
     *
     * @param  clip  clip shape
     */
    public abstract PixelDrawing createPixerFactory( Rectangle clip );

    public final Pixer createPixer( Rectangle clip ) {
        PixerFactory drawing = createPixerFactory( clip );
        return drawing == null ? null : drawing.createPixer();
    }
}
