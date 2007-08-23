package uk.ac.starlink.topcat.plot;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
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
public abstract class TextPixellatorFactory {

    /**
     * Returns a pixellator which will draw a given piece of text labelling
     * based at a given position.  This pixellator is only guaranteed to
     * remain usable until the next invocation of this method.
     *
     * @param  text  label text
     * @param  x   X coordinate of text origin
     * @param  y   Y coordinate of text origin
     */
    public abstract Pixellator getTextPixellator( String text, int x, int y );

    /**
     * Returns an instance of this class.
     *
     * @param   g  graphics context onto which bitmap will be copied
     */
    public static TextPixellatorFactory createInstance( Graphics g ) {
        Graphics2D g2 = (Graphics2D) g;

        /* Currently use an implementation based on use of a Graphics object
         * backed by a bitmap buffer.  When tested this was considerably
         * faster than the one based on GlyphVector shapes.  But might want
         * to modify the implementation again one day. */
        if ( true ) {
            return new GraphicsTextPixellatorFactory( g.getFontMetrics() );
        }
        else {
            return new GlyphTextPixellatorFactory( g2.getFont(),
                                                   g2.getFontRenderContext(),
                                                   g2.getClipBounds() );
        }
    }

    /**
     * TextPixellatorFactory implementation based on GlyphVectors.
     * Seems quite slow in JREs I've tried.
     */
    private static class GlyphTextPixellatorFactory
                         extends TextPixellatorFactory {

        private final Font font_;
        private final FontRenderContext frc_;
        private final Rectangle clip_;

        /**
         * Constructor.
         *
         * @param  font  font in which text will be drawn
         * @param  frc   render context to which text will be drawn
         * @param  clip  clipping region
         */
        public GlyphTextPixellatorFactory( Font font, FontRenderContext frc,
                                           Rectangle clip ) {
            font_ = font;
            frc_ = frc;
            clip_ = new Rectangle( clip );
        }

        public Pixellator getTextPixellator( String text, int x, int y ) {
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
    }

    /**
     * TextPixellatorFactory implementation based on a GraphicsPixellator.
     * Seems quite fast, and I don't *think* that it's relying on anything
     * hardware-specific.  Could be wrong though.
     */
    public static class GraphicsTextPixellatorFactory
                        extends TextPixellatorFactory {

        private final FontMetrics fm_;
        private final int ascent_;
        private final int descent_;
        private GraphicsPixellator gpixer_;

        /**
         * Constructor.
         *
         * @param   fm  font metrics
         */
        public GraphicsTextPixellatorFactory( FontMetrics fm ) {
            fm_ = fm;
            ascent_ = fm.getMaxAscent();
            descent_ = fm.getMaxDescent();
        }

        public Pixellator getTextPixellator( String text, int x, int y ) {
            GraphicsPixellator gpixer =
                getGraphicsPixellator( fm_.stringWidth( text ) );
            gpixer.clear();
            gpixer.getGraphics().drawString( text, 0, 0 );
            return new TranslatedPixellator( gpixer, x, y - ascent_ );
        }

        /**
         * Returns a GraphicsPixellator of at least a given width.
         *
         * @param   width   buffer with in pixels
         */
        private GraphicsPixellator getGraphicsPixellator( int width ) {
            if ( gpixer_ == null || gpixer_.getBounds().width < width ) {
                Rectangle bounds =
                    new Rectangle( 0, -ascent_, width, ascent_ + descent_ );
                gpixer_ = new GraphicsPixellator( bounds );
            }
            return gpixer_;
        }
    }
}
