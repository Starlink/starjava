/*
 * ESO Archive
 *
 * $Id: CoordinateRadius.java,v 1.4 2002/07/09 13:30:36 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/18  Created
 */

package jsky.coords;

import java.io.Serializable;


/**
 * Represents a generic center coordinate and radius range (or width
 * and height), Some methods are provided to check if another
 * coordinate point lies within the range. The coordinates and radius
 * (or width and height) values are treated in a generic way.
 */
public class CoordinateRadius implements Serializable {

    /** Center position in a supported coordinate system */
    protected Coordinates centerPosition;

    /** Min radius from center */
    protected double minRadius;

    /** Max radius from center  */
    protected double maxRadius;

    /** Width about the center (optional) */
    protected double width;

    /** Height about the center (optional) */
    protected double height;

    /**
     * Initialize from the given center position and radius range.
     * The units of the radius values depend on the type of the coordinates,
     * for example arcmin for world coordinates, pixel for image coordinates.
     *
     * @param pos the center position.
     * @param minRadius the minimum radius from the center position
     * @param maxRadius the maximum radius from the center position
     */
    public CoordinateRadius(Coordinates pos, double minRadius, double maxRadius) {
        this.centerPosition = pos;
        this.minRadius = Math.min(minRadius, maxRadius);
        this.maxRadius = Math.max(minRadius, maxRadius);
        width = height = 2.0 * Math.sqrt(0.5 * maxRadius * maxRadius);
    }

    /**
     * Initialize from the given center position and radius.
     * The units of the radius values depend on the type of the coordinates,
     * for example arcmin for world coordinates, pixel for image coordinates.
     *
     * @param pos the center position.
     * @param maxRadius the maximum radius from the center position
     */
    public CoordinateRadius(Coordinates pos, double maxRadius) {
        this.centerPosition = pos;
        this.minRadius = 0.0;
        this.maxRadius = maxRadius;
        width = height = 2.0 * Math.sqrt(0.5 * maxRadius * maxRadius);
    }

    /**
     * Initialize from the given center position, width and height.
     * The units of the width and height depend on the type of the coordinates,
     * for example arcmin for world coordinates, pixel for image coordinates.
     *
     * @param pos the center position.
     * @param width the width of the image area, about the center
     * @param height the height of the image area, about the center
     * @param maxRadius the maximum radius from the center position
     */
    public CoordinateRadius(Coordinates pos, double maxRadius, double width, double height) {
        this.centerPosition = pos;
        this.width = width;
        this.height = height;
        this.minRadius = 0.0;
        this.maxRadius = maxRadius;
    }

    /** Return the center position. */
    public Coordinates getCenterPosition() {
        return centerPosition;
    }

    /** Return the min radius value. */
    public double getMinRadius() {
        return minRadius;
    }

    /** Return the max radius value. */
    public double getMaxRadius() {
        return maxRadius;
    }

    /** Return the width, if set, otherwise 0. */
    public double getWidth() {
        return width;
    }

    /** Return the height, if set, otherwise 0. */
    public double getHeight() {
        return height;
    }

    /**
     * Return true if the given coordinate position lies within the
     * region defined by this object.
     */
    public boolean contains(Coordinates pos) {
        double dist = centerPosition.dist(pos);
        return (dist >= minRadius && dist <= maxRadius);
    }

    /**
     * Return coordinates and radius as a string.
     */
    public String toString() {
        return "(" + centerPosition + ", " + minRadius + ", " + maxRadius + ")";
    }
}

