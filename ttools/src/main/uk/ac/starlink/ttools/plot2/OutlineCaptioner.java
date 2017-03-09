package uk.ac.starlink.ttools.plot2;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

/**
 * Wrapper Captioner implementation that draws an outline around the
 * caption bounding box as well as actually drawing it.
 * Only intended for debugging.
 *
 * @author   Mark Taylor
 * @since    9 Mar 2017
 */
public class OutlineCaptioner implements Captioner {

    private final Captioner base_;
    private final Color color_;

    /**
     * Constructor.
     *
     * @param  base   base captioner
     * @param  color   outline colour
     */
    public OutlineCaptioner( Captioner base, Color color ) {
        base_ = base;
        color_ = color;
    }

    public Rectangle getCaptionBounds( String label ) {
        return base_.getCaptionBounds( label );
    }

    public void drawCaption( String label, Graphics g ) {
        base_.drawCaption( label, g );
        Color color0 = g.getColor();
        g.setColor( color_ );
        Rectangle bounds = getCaptionBounds( label );
        g.drawRect( bounds.x, bounds.y, bounds.width, bounds.height );
        g.drawLine( bounds.x, bounds.y,
                    bounds.x + bounds.width, bounds.y + bounds.height );
        g.drawLine( bounds.x, bounds.y + bounds.height,
                    bounds.x + bounds.width, bounds.y );
        g.setColor( color0 );
    }

    public int getPad() {
        return base_.getPad();
    }

    @Override
    public int hashCode() {
        int code = -66205;
        code = code + 23 * base_.hashCode();
        code = code + 23 * color_.hashCode();
        return code;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof OutlineCaptioner ) {
            OutlineCaptioner other = (OutlineCaptioner) o;
            return this.base_.equals( other.base_ )
                && this.color_.equals( other.color_ );
        }
        else {
            return false;
        }
    }
}
