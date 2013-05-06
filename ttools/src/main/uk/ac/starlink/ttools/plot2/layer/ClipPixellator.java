package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Rectangle;
import uk.ac.starlink.ttools.plot.Pixellator;

/**
 * Pixellator implementation that wraps another pixellator and
 * only retains the pixels that fall within a given clip rectangle.
 *
 * <p>Note: Pixellator bounds are reported such that
 * the points of the pixellator are included, but not the outer bounds
 * of the 1-pixel-wide lines they generate are included.
 * This means that the pixellator bounds need effectively to be
 * extended by one pixel down and right before comparing with
 * a graphics clip.  This is probably an unfortunate/wrong part of
 * the pixellator API, but for now we just deal with it here.
 *
 * <p>In most cases you should call the {@link #clip} factory method
 * rather than the constructor of this class.
 *
 * @author   Mark Taylor
 * @since    12 Feb 2013
 */
public class ClipPixellator implements Pixellator {

    private final Pixellator base_;
    private final Rectangle clip_;
    private final Rectangle bounds_;
    private static final Pixellator EMPTY = new EmptyPixellator();

    /**
     * Constructor.
     *
     * @param   base  base pixellator
     * @param   clip  clipping rectangle to impose on base
     */
    public ClipPixellator( Pixellator base, Rectangle clip ) {
        base_ = base;
        clip_ = clip;
        Rectangle baseBounds = new Rectangle( base.getBounds() );
        baseBounds.width++;
        baseBounds.height++;
        bounds_ = baseBounds.intersection( clip );
    }

    public Rectangle getBounds() {
        return bounds_;
    }

    public void start() {
        base_.start();
    }

    public boolean next() {
        while ( base_.next() ) {
            if ( bounds_.contains( base_.getX(), base_.getY() ) ) {
                return true;
            }
        }
        return false;
    }

    public int getX() {
        return base_.getX();
    }

    public int getY() {
        return base_.getY();
    }

    /**
     * Returns a pixellator which is guaranteed to fall within a given clip.
     * If the supplied base pixellator is already within that clip, it
     * is returned unchanged, otherwise a new ClipPixellator will be
     * created and returned.
     *
     * @param  pixellator   base pixellator
     * @return   clip region
     */
    public static Pixellator clip( Pixellator pixellator, Rectangle clip ) {
        Rectangle pBounds = pixellator.getBounds();
        if ( rectContains( clip, pBounds ) ) {
            return pixellator;
        }
        else if ( pBounds.intersection( clip ).isEmpty() ) {
            return EMPTY;
        }
        else {
            return new ClipPixellator( pixellator, clip );
        }
    }

    /**
     * Indicates whether one rectangle is completely contained within another.
     * You might think that
     * {@link java.awt.Rectangle#contains(java.awt.Rectangle)} should do that,
     * but it behaves surprisingly if one of the rectangles has zero size.
     *
     * @param  outer  outer rectangle
     * @param  inner  inner rectangle
     * @return  true iff outer contains inner, including if inner is empty
     */
    private static boolean rectContains( Rectangle outer, Rectangle inner ) {
        return outer.x <= inner.x
            && outer.y <= inner.y
            && outer.x + outer.width > inner.x + inner.width
            && outer.y + outer.height > inner.y + inner.height;
    }

    /**
     * Pixellator implementation that returns no pixels.
     */
    private static class EmptyPixellator implements Pixellator {
        private final Rectangle bounds_ = new Rectangle();
        public Rectangle getBounds() {
            return bounds_;
        }
        public void start() {
        }
        public boolean next() {
            return false;
        }
        public int getX() {
            throw new IllegalStateException();
        }
        public int getY() {
            throw new IllegalStateException();
        }
    }
}
