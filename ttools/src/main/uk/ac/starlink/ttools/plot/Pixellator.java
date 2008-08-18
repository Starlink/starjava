package uk.ac.starlink.ttools.plot;

import java.awt.Rectangle;

/**
 * Reusable iterator over pixel positions.
 * Implementations will not in general be thread-safe.
 *
 * @author   Mark Taylor
 * @since    28 Mar 2007
 */
public interface Pixellator {

    /**
     * Returns a copy of the bounding rectangle for this pixellator.
     * All points iterated over by this object will fall within this rectangle.
     * If this object has no points, <code>null</code> may be returned.
     *
     * @return  bounds
     */
    Rectangle getBounds();

    /** 
     * Makes this object ready to iterate.  Should be called before any
     * call to {@link #next}.
     */
    void start();

    /**
     * Moves to the next point in the sequence.  Must be called before any
     * call to {@link #getX}/{@link #getY}.  Returns value indicates whether
     * there is a next point.
     *
     * @return  next  true iff there are more points
     */
    boolean next();

    /**
     * Returns the X value for the current point.
     *
     * @return  x
     */
    int getX();

    /**
     * Returns the Y value for the current point.
     *
     * @return  y
     */
    int getY();
}
