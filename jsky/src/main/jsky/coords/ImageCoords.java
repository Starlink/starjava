/*
 * ESO Archive
 *
 * $Id: ImageCoords.java,v 1.3 2002/07/09 13:30:36 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/19  Created, based on C++ version
 */

package jsky.coords;

import java.io.Serializable;


/**
 * Class representing world coordinates (right-ascension, ylination,
 * stored as J2000 internally).
 */
public class ImageCoords implements Coordinates, Serializable {

    /** The X coordinate */
    protected double x;

    /** The Y coordinate */
    protected double y;

    /** Default constructor: initialize null coordinates. */
    public ImageCoords() {
    }

    /**
     * Initialize from x and y.
     *
     * @param x The X coordinate
     * @param y The Y coordinate
     */
    public ImageCoords(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Initialize from x and y.
     *
     * @param x The X coordinate
     * @param y The Y coordinate
     */
    public ImageCoords(Double x, Double y) {
        this.x = x.doubleValue();
        this.y = y.doubleValue();
    }

    /*
     * Parse X and Y in string format.
     *
     * @param x The X coordinate in string form
     *
     * @param y The Y coordinate in string form
     */
    public ImageCoords(String x, String y) {
        this.x = Double.parseDouble(x);
        this.y = Double.parseDouble(y);
    }


    /** return the X value */
    public double getX() {
        return x;
    }

    /** return the Y value */
    public double getY() {
        return y;
    }

    /**
     * Return the coordinates as a string in h:m:s [+-]d:m:s format
     */
    public String toString() {
        return Double.toString(x) + " " + Double.toString(y);
    }

    /**
     * Return the distance between the two points.
     * @param x0 The X coordinate of the first point
     * @param y0 The Y coordinate of the first point
     * @param x1 The X coordinate of the second point
     * @param y1 The Y coordinate of the second point
     * @return The distance between the two points.
     */
    public static double dist(double x0, double y0, double x1, double y1) {
        double x = Math.abs(x1 - x0);
        double y = Math.abs(y1 - y0);
        return Math.sqrt(x * x + y * y);
    }

    /**
     * Return the distance between this position and the given one.
     * @param pos The other point.
     * @return The distance to the given point.
     */
    public double dist(ImageCoords pos) {
        return dist(x, y, pos.x, pos.y);
    }

    /**
     * Return the distance between this position and the given one.
     * @param pos The other point.
     * @return The distance to the given point.
     */
    public double dist(Coordinates pos) {
        return dist((ImageCoords) pos);
    }

    /**
     * Given a radius, return an array {pos1, pos2} with the 2 endpoints
     * that form a box with center at "this" position.
     *
     * @param radius The radius.
     *
     * @return Array of 2 ImageCoords objects that are the endpoints of a box
     *         with the given radius and centered at "this" position.
     */
    public ImageCoords[] box(double radius) {
        ImageCoords[] ar = new ImageCoords[2]; // return array

        double w = Math.sqrt((radius * radius) / 2.);
        double x0 = x - w;
        double y0 = y - w;
        double x1 = x + w;
        double y1 = y + w;

        // set the result array
        ar[0] = new ImageCoords(x0, y0);
        ar[1] = new ImageCoords(x1, y1);

        return ar;
    }


    /**
     * Given the endpoints of a box (pos1, pos2), return an array containing the
     * center pos, as well as the width, height, and radius of the box.
     *
     * @param pos1 The first endpoint of the box.
     * @param pos1 The second endpoint of the box.
     *
     * @return An array of 5 doubles: {x, y, width, height, radius}
     *         where (x, y) gives the center position,
     *         width and height are the size of the box,
     *         and radius is the distance from the center to a corner.
     */
    public double[] center(ImageCoords pos1, ImageCoords pos2) {
        double ar[] = new double[5]; // result

        // get center pos
        double x1 = pos1.x, y1 = pos1.y;
        double x2 = pos2.x, y2 = pos2.y;
        ar[0] = (x1 + x2) / 2.0;
        ar[1] = (y1 + y2) / 2.0;

        // get width and height of box
        ar[2] = dist(x1, y1, x2, y1);
        ar[3] = dist(x1, y1, x1, y2);

        // radius is half the distance from pos1 to pos2
        ar[4] = dist(x1, y1, x2, y2) / 2.;

        return ar;
    }


    /** Returns the name of the coordinate system as a string. */
    public String getCoordinateSystemName() {
        return "image";
    }


    /**
     * Test cases
     */
    public static void main(String[] args) {
        ImageCoords c1 = new ImageCoords(100., 200.);
        ImageCoords c2 = new ImageCoords("100.", "200.");

        System.out.println("these coords should each be the same:");
        System.out.println(c1);
        System.out.println(c2);

        // test the "box" method (get 2 points given a radius)
        ImageCoords c3 = new ImageCoords(100, 100), c4, c5;
        ImageCoords[] ar1 = c3.box(10);
        c4 = ar1[0];
        c5 = ar1[1];
        System.out.println("box of radius 10 with center at (100, 100):");
        System.out.println(c4);
        System.out.println(c5);
    }
}
