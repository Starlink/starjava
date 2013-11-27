package uk.ac.starlink.ttools.plot2;

/**
 * Iterator over pixels.
 *
 * @author   Mark Taylor
 * @since    25 Nov 2013
 * @see      uk.ac.starlink.ttools.plot2.layer.Pixers
 */
public interface Pixer {

    /**
     * Advances to the next point to be dispensed by this iterator.
     * Must be called before any calls to getX/getY.
     *
     * @return   true iff there is another point
     */
    boolean next();

    /**
     * Returns the X coordinate of the current point.
     *
     * @return  X coordinate
     */
    int getX();

    /**
     * Returns the Y coordinate of the current point.
     *
     * @return  Y coordinate
     */
    int getY();
}
