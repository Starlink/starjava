package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import uk.ac.starlink.ttools.plot2.Glyph;
import uk.ac.starlink.ttools.plot2.Pixer;

/**
 * Glyph partial implementation that facilitates making drawings with
 * variable-thickness lines.
 * The {@link #toThicker} method is supplied to generate another glyph
 * based on this one but with thicker drawing lines.
 *
 * <p>Concrete implementations have to be able to supply bounds,
 * and draw the shape onto a supplied PixelDrawing,
 * or to a Graphics context with a given StrokeSet.
 *
 * @author   Mark Taylor
 * @since    24 Sep 2021
 */
public abstract class LineGlyph extends DrawingGlyph {

    /**
     * Returns the boundary rectangle containing the whole of
     * the shape that will be drawn to a PixelDrawing.
     * This is allowed to be an overestimate (though efficiency may suffer),
     * but not an underestimate.
     *
     * <p>This method is called by {@link #createPixerFactory},
     * but not by {@link #drawShape}.
     *
     * @return  bounding box
     */
    public abstract Rectangle getPixelBounds();

    /**
     * Draws this shape onto a given drawing, whose bounds have
     * been set appropriately.  Since a suitable clip will be in place,
     * and PixelDrawing methods will take account of this to avoid
     * unnecessary work, implementations do not generally have to 
     * worry concerning correctness or efficiency about graphics operations
     * with potentially oversized coordinate values.
     *
     * @param  drawing  graphics destination
     */
    public abstract void drawShape( PixelDrawing drawing );

    /**
     * Paints this shape onto a graphics context with given line strokes.
     * If any lines are drawn, they should in general be done with
     * strokes supplied from the given StrokeKit, giving client classes
     * a chance to customise line drawing.
     *
     * @param  g  graphics context
     * @param  strokeKit  supplier of line drawing strokes
     */
    public abstract void paintGlyph( Graphics g, StrokeKit strokeKit );

    /**
     * Returns a PixerFactory that produces the pixels for this glyph.
     *
     * @param  clip  clip bounds
     */
    public PixelDrawing createPixerFactory( Rectangle clip ) {
        Rectangle bounds = getPixelBounds();
        int xlo = Math.max( clip.x, bounds.x );
        int xhi = Math.min( clip.x + clip.width, bounds.x + bounds.width );
        int dw = xhi - xlo;
        if ( dw > 0 ) {
            int ylo = Math.max( clip.y, bounds.y );
            int yhi = Math.min( clip.y + clip.height,
                                bounds.y + bounds.height );
            int dh = yhi - ylo;
            if ( dh > 0 ) {
                PixelDrawing drawing = new PixelDrawing( xlo, ylo, dw, dh );
                drawShape( drawing );
                return drawing;
            }
        }
        return null;
    }

    public void paintGlyph( Graphics g ) {
        paintGlyph( g, StrokeKit.DEFAULT );
    }

    /**
     * Returns a glyph that is the same as this one, but with thicker
     * pen strokes.  The pixel drawing stroke changes are effected by
     * convolving the output drawing with a supplied smoothing kernel,
     * and the graphics context painting is done using a supplied
     * StrokeKit.  It is the responsibility of the calling class to
     * ensure that these two are consistent with each other.
     * Note the effect may not be exactly the same for painted and pixellated
     * output even so, since only stroked lines will be thicker in the
     * painting, while all output graphics will be smoothed in the pixels.
     *
     * @param  kernel  smoothing kernel
     * @param  strokeKit  line strokes to use for painting
     * @return   derived glyph
     */
    public DrawingGlyph toThicker( PixerFactory kernel, StrokeKit strokeKit ) {
        return new ThickerGlyph( this, kernel, strokeKit );
    }

    /**
     * Returns a (roughly circular) smoothing kernel for a thick line.
     *
     * @param  nthick  line thickness, &gt;=0
     * @return  standard smoothing kernel
     */
    public static PixerFactory createThickKernel( int nthick ) {
        return createKernel( nthick == 1 ? MarkerShape.CROSS
                                         : MarkerShape.FILLED_CIRCLE,
                             nthick );
    }

    /**
     * Returns a smoothing kernel derived from a given marker shape.
     *
     * @param  shape   shape
     * @param  nthick  thickness index &gt;=0
     * @return  smoothing kernel
     */
    public static PixerFactory createKernel( MarkerShape shape, int nthick ) {
        return shape.getStyle( Color.BLACK, nthick ).getPixerFactory();
    }

    /**
     * Returns a stroke kit for drawing a thick line.
     *
     * @param  nthick  line thickness, &gt;=0
     * @return   thick stroke
     */
    public static StrokeKit createThickStrokeKit( int nthick ) {
        return new StrokeKit( 1f + 2 * nthick );
    }

    /**
     * Glyph implementation that adjusts the pixel and stroked shapes
     * drawn by a LineGlyph to look as if they were drawn with
     * a fatter pen.
     */
    private static class ThickerGlyph extends DrawingGlyph {

        private final LineGlyph base_;
        private final PixerFactory kernel_;
        private final StrokeKit strokeKit_;

        /**
         * Constructor.
         *
         * @param   base   base glyph
         * @param   kernel  blurring kernel used to modify pixel drawing
         * @param   strokeKit  line stroke supplier used to paint lines
         */
        public ThickerGlyph( LineGlyph base, PixerFactory kernel,
                             final StrokeKit strokeKit ) {
            base_ = base;
            kernel_ = kernel;
            strokeKit_ = strokeKit;
        }

        public void paintGlyph( Graphics g ) {
            base_.paintGlyph( g, strokeKit_ );
        }

        public PixelDrawing createPixerFactory( Rectangle clip ) {
            Rectangle baseBounds = base_.getPixelBounds();
            int xlo = Math.max( baseBounds.x + kernel_.getMinX(),
                                clip.x );
            int xhi = Math.min( baseBounds.x + baseBounds.width
                                             + kernel_.getMaxX(),
                                clip.x + clip.width );
            int dw = xhi - xlo;
            if ( dw > 0 ) {
                int ylo = Math.max( baseBounds.y + kernel_.getMinY(),
                                    clip.y );
                int yhi = Math.min( baseBounds.y + baseBounds.height
                                                 + kernel_.getMaxY(),
                                    clip.y + clip.height );
                int dh = yhi - ylo;
                if ( dh > 0 ) {
                    PixelDrawing drawing = new PixelDrawing( xlo, ylo, dw, dh );
                    base_.drawShape( drawing );
                    return Pixers.convolve( drawing, kernel_, clip );
                }
            }
            return null;
        }
    }
}
