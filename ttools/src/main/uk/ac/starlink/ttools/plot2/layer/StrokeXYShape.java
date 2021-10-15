package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.util.logging.Logger;
import uk.ac.starlink.ttools.plot2.Glyph;
import uk.ac.starlink.ttools.plot2.Pixer;

/**
 * XYShape that draws a line using a Stroke object.
 * This is less efficient than LineXYShape, but it can draw lines with
 * more than 1-pixel thickness.
 *
 * @author   Mark Taylor
 * @since    23 Jul 2018
 */
public class StrokeXYShape extends XYShape {

    private final Stroke stroke_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.plot2.layer" );

    /**
     * Constructor.
     *
     * @param  stroke  drawing stroke
     */
    public StrokeXYShape( Stroke stroke ) {
        super( "Stroke", 16, XYShape.POINT );
        stroke_ = stroke;
    }

    /**
     * Returns the stroke used by this shape.
     *
     * @return  drawing stroke
     */
    public Stroke getStroke() {
        return stroke_;
    }

    protected Glyph createGlyph( short sx, short sy ) {
        return isCached( sx, sy ) ? new CachedStrokeGlyph( stroke_, sx, sy )
                                  : new UncachedStrokeGlyph( stroke_, sx, sy );
    }

    /**
     * Returns the maximum padding width required for drawing a line
     * with a given stroke.
     *
     * @param  stroke  line drawing stroke
     * @return  maximum required padding in pixels
     */
    private static int getFatness( Stroke stroke ) {
        Rectangle bounds =
            stroke.createStrokedShape( new Line2D.Double( 0, 0, 0, 0 ) )
                  .getBounds();
        return Math.max( Math.max( -bounds.x, bounds.width + bounds.x ),
                         Math.max( -bounds.y, bounds.height + bounds.y ) );
    }

    /**
     * Returns a Pixer for drawing a line from the origin to a given offset
     * using a given stroke.
     *
     * @param  stroke  line drawing stroke
     * @param  x  X offset
     * @param  y  Y offset
     * @return  pixer
     */
    private static Pixer createStrokePixer( Stroke stroke, short x, short y ) {
        int fat = getFatness( stroke );
        int xmin = Math.min( x-fat, -fat ); 
        int xmax = Math.max( x+fat, +fat );
        int ymin = Math.min( y-fat, -fat );
        int ymax = Math.max( y+fat, +fat );
        int x0 = fat - Math.min( 0, x );
        int y0 = fat - Math.min( 0, y );
        GreyImage bitmap =
            GreyImage.createGreyImage( xmax - xmin, ymax - ymin );
        Graphics2D g = bitmap.getImage().createGraphics();
        g.setStroke( stroke );
        g.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_OFF );
        g.drawLine( x0, y0, x0 + x, y0 + y );
        return Pixers.translate( bitmap.createPixer(), -x0, -y0 );
    }

    /**
     * Abstract superclass for glyphs that can draw a line from the origin
     * to a fixed X,Y offset.
     */
    private static abstract class StrokeGlyph implements Glyph {
        final Stroke stroke_;
        final short x_;
        final short y_;

        /**
         * Constructor.
         *
         * @param  stroke  line drawing stroke
         * @param  x  X offset
         * @param  y  Y offset
         */
        StrokeGlyph( Stroke stroke, short x, short y ) {
            stroke_ = stroke;
            x_ = x;
            y_ = y;
        }

        public void paintGlyph( Graphics g ) {
            Graphics2D g2 = (Graphics2D) g;
            Stroke stroke0 = g2.getStroke();
            g2.setStroke( stroke_ );
            g2.drawLine( 0, 0, x_, y_ );
            g2.setStroke( stroke0 );
        }
    }

    /**
     * StrokeGlyph concrete subclass suitable for use when there may be
     * many uses for a single instance.
     */
    private static class CachedStrokeGlyph extends StrokeGlyph {
        final PixerFactory pixerFact_;

        /**
         * Constructor.
         *
         * @param  stroke  line drawing stroke
         * @param  x  X offset
         * @param  y  Y offset
         */
        CachedStrokeGlyph( Stroke stroke, short x, short y ) {
            super( stroke, x, y );
            pixerFact_ =
                Pixers.createPixerCopier( createStrokePixer( stroke, x, y ) );
        }

        public Pixer createPixer( Rectangle clip ) {
            return Pixers.clip( pixerFact_.createPixer(), clip );
        }
    }

    /**
     * StrokedGlyph concrete subclass suitable for use when a single
     * instance is only likely to be used once.
     */
    private static class UncachedStrokeGlyph extends StrokeGlyph {

        /**
         * Constructor.
         *
         * @param  stroke  line drawing stroke
         * @param  x  X offset
         * @param  y  Y offset
         */
        UncachedStrokeGlyph( Stroke stroke, short x, short y ) {
            super( stroke, x, y );
        }

        public Pixer createPixer( Rectangle clip ) {
            int fat = getFatness( stroke_ );
            int gxmin = clip.x;
            int gxmax = clip.x + clip.width;
            int gymin = clip.y;
            int gymax = clip.y + clip.height;

            /* If the line is not too long (does not extend outside the
             * clip bounds), use the same method as for the cached lines. */
            if ( x_ - fat >= gxmin && x_ + fat <= gxmax &&
                 y_ - fat >= gymin && y_ + fat <= gymax ) {
                return createStrokePixer( stroke_, x_, y_ );
            }
            else {

                /* I don't think this code will be used at present.
                 * If it is, it should probably be rewritten to be more
                 * efficient (not require a potentially overlarge buffer).
                 * For now, just put a warning in there in case it becomes
                 * used. */
                logger_.warning( "Long line creation may be inefficient" );
                return createStrokePixer( stroke_, x_, y_ );
            }
        }
    }
}
