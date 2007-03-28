package uk.ac.starlink.topcat.plot;

/**
 * Reusable iterator over pixel positions.
 * Implementations will not in general be thread-safe.
 *
 * @author   Mark Taylor
 * @since    28 Mar 2007
 */
public interface Pixellator {

    /** 
     * Makes this object ready to iterate.  Should be called before any
     * call to {@link #next}.
     */
    public void start();

    /**
     * Moves to the next point in the sequence.  Must be called before any
     * call to {@link #getX}/{@link #getY}.  Returns value indicates whether
     * there is a next point.
     *
     * @return  next  true iff there are more points
     */
    public boolean next();

    /**
     * Returns the X value for the current point.
     *
     * @return  x
     */
    public int getX();

    /**
     * Returns the Y value for the current point.
     *
     * @return  y
     */
    public int getY();
}
