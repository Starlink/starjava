package uk.ac.starlink.ttools.plot;

import java.awt.Rectangle;

/**
 * Pixellator which modifies the behaviour of a base Pixellator by translating
 * it on the XY plane.
 *
 * @author   Mark Taylor 
 * @since    2 Apr 2007
 */
public class TranslatedPixellator implements Pixellator {

    private final Pixellator base_;
    private final int x_;
    private final int y_;

    /**
     * Constructor.
     *
     * @param   base  base pixellator
     * @param   x     distance to translate in X direction
     * @param   y     distance to translate in Y direction
     */
    public TranslatedPixellator( Pixellator base, int x, int y ) {
        base_ = base;
        x_ = x;
        y_ = y;
    }

    public Rectangle getBounds() {
        Rectangle bounds = base_.getBounds();
        if ( bounds != null ) {
            bounds.translate( x_, y_ );
        }
        return bounds;
    }

    public void start() {
        base_.start();
    }

    public boolean next() {
        return base_.next();
    }

    public int getX() {
        return base_.getX() + x_;
    }

    public int getY() {
        return base_.getY() + y_;
    }
}
