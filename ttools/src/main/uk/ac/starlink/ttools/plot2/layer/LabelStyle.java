package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import javax.swing.Icon;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.Anchor;
import uk.ac.starlink.ttools.plot2.Caption;
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
    private final int spacing_;
    private final byte crowdLimit_;

    /**
     * Constructor.
     *
     * @param  captioner  renders text to graphics
     * @param  anchor  positions text relative to plot point
     * @param  color  text colour
     * @param  spacing  minimum pixel distance between labels
     * @param  crowdLimit  number of labels allowed within spacing
     */
    public LabelStyle( Captioner captioner, Anchor anchor, Color color,
                       int spacing, byte crowdLimit ) {
        captioner_ = captioner;
        anchor_ = anchor;
        color_ = color;
        spacing_ = spacing;
        crowdLimit_ = crowdLimit;
    }

    public Icon getLegendIcon() {
        return new LabelStyleIcon();
    }

    /**
     * Returns the captioner used by this style.
     *
     * @return  captioner
     */
    public Captioner getCaptioner() {
        return captioner_;
    }

    /**
     * Returns the positioning anchor used by this style.
     *
     * @return  anchor
     */
    public Anchor getAnchor() {
        return anchor_;
    }

    /**
     * Returns the colour used by this style.
     *
     * @return   colour
     */
    public Color getColor() {
        return color_;
    }

    /**
     * Returns the minimum pixel spacing permitted between labels.
     *
     * @return  label spacing in pixels
     */
    public int getSpacing() {
        return spacing_;
    }

    /**
     * Returns the number of labels allowed within spacing pixels.
     *
     * @return  crowd limit
     */
    public byte getCrowdLimit() {
        return crowdLimit_;
    }

    /**
     * Draws the label at the origin without colouring it.
     * The drawing is therefore in the default colour of the graphics context.
     *
     * @param   g  graphics context
     * @param  label  text content
     */
    public void drawLabel( Graphics g, Caption label ) {
        anchor_.drawCaption( label, 0, 0, captioner_, g );
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof LabelStyle ) {
            LabelStyle other = (LabelStyle) o;
            return this.captioner_.equals( other.captioner_ )
                && this.anchor_.equals( other.anchor_ )
                && this.color_.equals( other.color_ )
                && this.spacing_ == other.spacing_
                && this.crowdLimit_ == other.crowdLimit_;
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
        code = 23 * code + spacing_;
        code = 23 * code + crowdLimit_;
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
        private final Caption label_;

        /**
         * Constructor.
         */
        LabelStyleIcon() {
            label_ = Caption.createCaption( "a" );
            Rectangle box0 =
                anchor_.getCaptionBounds( label_, 0, 0, captioner_ );
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
            g.translate( gx, gy );
            LabelStyle.this.drawLabel( g, label_ );
            g.translate( -gx, -gy );
            g.setColor( color0 );
        }
    }
}
