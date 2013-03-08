package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import javax.swing.Icon;
import uk.ac.starlink.ttools.plot.GraphicsBitmap;
import uk.ac.starlink.ttools.plot.Pixellator;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot.TranslatedPixellator;
import uk.ac.starlink.ttools.plot2.Anchor;
import uk.ac.starlink.ttools.plot2.Captioner;
import uk.ac.starlink.ttools.plot2.Equality;

/**
 * Style for LabelPlotter.
 *
 * @author   Mark Taylor
 * @since    15 Feb 2013
 */
@Equality
public class LabelStyle implements Style {

    private final Captioner captioner_;
    private final Anchor anchor_;
    private final Color color_;

    /**
     * Constructor.
     *
     * @param  captioner  renders text to graphics
     * @param  anchor  positions text relative to plot point
     * @param  color  text colour
     */
    public LabelStyle( Captioner captioner, Anchor anchor, Color color ) {
        captioner_ = captioner;
        anchor_ = anchor;
        color_ = color;
    }

    public Icon getLegendIcon() {
        return new LabelStyleIcon();
    }

    public Color getColor() {
        return color_;
    }

    /**
     * Draws the label at the origin without colouring it.
     * The drawing is therefore in the default colour of the graphics context.
     *
     * @param   g  graphics context
     * @param  label  text string
     */
    public void drawLabel( Graphics g, String label ) {
        anchor_.drawCaption( label, 0, 0, captioner_, g );
    }

    /**
     * Returns a pixellator for a given text string.
     *
     * @param   label  text string
     * @return  pixellator; doesn't do anything clever with antialiased text
     */
    public Pixellator getPixelOffsets( String label ) {
        Rectangle box = anchor_.getCaptionBounds( label, 0, 0, captioner_ );
        GraphicsBitmap bitmap = new GraphicsBitmap( box.width, box.height ); 
        Graphics g = bitmap.createGraphics();
        anchor_.drawCaption( label, -box.x, -box.y, captioner_, g );
        return new TranslatedPixellator( bitmap.createPixellator(),
                                         box.x, box.y );
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof LabelStyle ) {
            LabelStyle other = (LabelStyle) o;
            return this.captioner_.equals( other.captioner_ )
                && this.anchor_.equals( other.anchor_ )
                && this.color_.equals( other.color_ );
        }
        else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int code = 773;
        code = 23 * code + captioner_.hashCode();
        code = 23 * code + anchor_.hashCode();
        code = 23 * code + color_.hashCode();
        return code;
    }

    /**
     * Icon for displaying style.
     */
    private class LabelStyleIcon implements Icon {
        private final int width_;
        private final int height_;
        private final int xoff_;
        private final int yoff_;
        private final String text_;

        /**
         * Constructor.
         */
        LabelStyleIcon() {
            text_ = "a";
            Rectangle box0 =
                anchor_.getCaptionBounds( text_, 0, 0, captioner_ );
            int w = Math.max( -box0.x, box0.x + box0.width );
            int h = Math.max( -box0.y, box0.y + box0.height );
            int size = Math.max( w, h );
            assert size > 0;
            xoff_ = size / 2;
            yoff_ = size / 2;
            width_ = size;
            height_ = size;
        }

        public int getIconWidth() {
            return width_;
        }

        public int getIconHeight() {
            return height_;
        }

        public void paintIcon( Component c, Graphics g, int x, int y ) {
            Color color0 = g.getColor();
            g.setColor( color_ );
            int gx = x + xoff_;
            int gy = y + yoff_;
            g.translate( x, y );
            LabelStyle.this.drawLabel( g, text_ );
            g.translate( -x, -y );
            g.setColor( color0 );
        }
    }
}
