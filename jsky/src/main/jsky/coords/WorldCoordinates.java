/*
 * ESO Archive
 *
 * $Id: WorldCoordinates.java,v 1.4 2002/07/09 13:30:36 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/03  Created, based on C++ version
 */

package jsky.coords;

import java.awt.geom.*;

/**
 * Interface for representing world coordinates (right-ascension, declination,
 * stored as J2000 internally).
 */
public abstract interface WorldCoordinates extends Coordinates {

    /** return the RA value in J2000 */
    public HMS getRA();

    /** return the DEC value in J2000 */
    public DMS getDec();

    /** return the RA value in deg J2000 */
    public double getRaDeg();

    /** return the DEC value in deg J2000 */
    public double getDecDeg();

    /**
     * Return the coordinates as a string in h:m:s [+-]d:m:s format
     */
    public String toString();

    /**
     * Format RA and DEC in h:m:s [+-]d:m:s format in the given equinox
     * and return them as a 2 element String array.
     */
    public String[] format(double equinox);

    /**
     * Format RA and DEC in h:m:s [+-]d:m:s format in J2000
     * and return them as a 2 element String array.
     */
    public String[] format();

    /**
     * Return RA and DEC in degrees as an array of 2 doubles {ra, dec} in the given equinox.
     */
    public double[] getRaDec(double equinox);

    /**
     * return the distance between this position and the given one in arcmin
     * and also set the position angle
     *
     * @param pos The other point.
     *
     * @return An array of 2 doubles ar[2], where ar[0] is the distance in arcmin and
     *         ar[1] is the position angle in degrees (East of North).
     */
    public double[] dispos(WorldCoordinates pos);


    /**
     * Given a radius in arcmin, return an array {pos1, pos2} with the 2 endpoints
     * that form a box with center at "this" position.
     *
     * @param radius The radius in arcmin.
     *
     * @return Array of 2 WorldCoordinates objects that are the endpoints of a box
     *         with the given radius and centered at "this" position.
     */
    public WorldCoordinates[] box(double radius);


    /**
     * Given the endpoints of a box (pos1, pos2), return an array containing the
     * center ra, dec in degrees, as well as the width, height, and radius of the
     * box in arcmin.
     *
     * @param pos1 The first endpoint of the box.
     * @param pos1 The second endpoint of the box.
     *
     * @return An array of 5 doubles: {ra, dec, width, height, radius}
     *         where (ra, dec) gives the center position in deg,
     *         width and height are the size of the box in arcmin,
     *         and radius is the distance from the center to a corner in arcmin.
     */
    public double[] center(WorldCoordinates pos1, WorldCoordinates pos2);
}
