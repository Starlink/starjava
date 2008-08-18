package uk.ac.starlink.ttools.plot;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * Pixellator based on an array of {@link Point} objects.
 * This implementation is designed to be particularly efficient for
 * iterating over.
 *
 * @author   Mark Taylor
 * @since    28 Mar 2007
 */
public class PointArrayPixellator implements Pixellator {

    private final Point[] points_;
    private final int np_;
    private final Rectangle bounds_;
    private int ip_ = Integer.MAX_VALUE;

    /**
     * Constructor.
     *
     * @param   points   array of points to iterate over
     */
    public PointArrayPixellator( Point[] points ) {
        points_ = points;
        np_ = points.length;
        if ( np_ > 0 ) {
            bounds_ = new Rectangle( points[ 0 ] );
            for ( int ip = 1; ip < np_; ip++ ) {
                bounds_.add( points[ ip ] );
            }
        }
        else {
            bounds_ = null;
        }
    }

    public Rectangle getBounds() {
        return bounds_ == null ? null : new Rectangle( bounds_ );
    }

    public void start() {
        ip_ = -1;
    }

    public boolean next() {
        return ++ip_ < np_;
    }

    public int getX() {
        return points_[ ip_ ].x;
    }

    public int getY() {
        return points_[ ip_ ].y;
    }
}
