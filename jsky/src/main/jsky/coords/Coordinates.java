/*
 * ESO Archive
 *
 * $Id: Coordinates.java,v 1.2 2002/07/09 13:30:36 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/18  Created
 */

package jsky.coords;

/**
 * Abstract interface for coordinates. This interface defines
 * the common methods for all image coordinate systems used.
 */
public abstract interface Coordinates {

    /** Returns the name of the coordinate system as a string. */
    public String getCoordinateSystemName();

    /**
     * Return the coordinates as a string.
     */
    public String toString();

    /**
     * Return the distance between this position and the given one in
     * the standard units of the coordinate system being used (arcmin
     * for WorldCoords, pixels for ImageCoords, ...).
     *
     * @param pos The other point.
     *
     * @return The distance to the given point.
     */
    public double dist(Coordinates pos);

    /** return the X coordinate as a double */
    public double getX();

    /** return the Y coordinate as a double */
    public double getY();
}
