/*
 * ESO Archive
 *
 * $Id: RowCoordinates.java,v 1.5 2002/07/09 13:30:36 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/10  Created
 */

package jsky.catalog;


import java.util.*;

import jsky.coords.Coordinates;
import jsky.coords.ImageCoords;
import jsky.coords.WorldCoords;

import java.lang.NumberFormatException;

/**
 * Stores information about which columns in a table contain
 * the coordinates for that row, and whether world coordinates or
 * image coordinates are supported. In addition, the index of the
 * id column may be stored here, if known.
 */
public class RowCoordinates {

    /** The column containing the object id, or -1 if there isn't one */
    protected int idCol = -1;

    /** The column index for the X or RA coordinate */
    protected int xCol = -1;

    /** The column index for the Y or DEC coordinate */
    protected int yCol = -1;

    /** True if the table has RA and DEC coordinate columns */
    protected boolean isWcs = false;

    /** Value of the equinox for world coordinates, if it is known */
    protected double equinox = 2000.;

    /** Index of a column containing the equnox for each row, or -1 if it is constant. */
    protected int equinoxCol = -1;

    /** True if using image coordinates */
    protected boolean isPix = false;

    /**
     * Create an object that will extract a WorldCoords object from a
     * row using the given ra,dec indexes and the given equinox.
     */
    public RowCoordinates(int raCol, int decCol, double equinox) {
        this.xCol = raCol;
        this.yCol = decCol;
        this.equinox = equinox;
        isWcs = true;
    }

    /**
     * Create an object that will extract a WorldCoords object from a
     * row using the given ra,dec and equinox indexes.
     */
    public RowCoordinates(int raCol, int decCol, int equinoxCol) {
        this.xCol = raCol;
        this.yCol = decCol;
        this.equinoxCol = equinoxCol;
        isWcs = true;
    }


    /**
     * Create an object that will extract an ImageCoords object from a
     * row using the given x,y indexes.
     */
    public RowCoordinates(int xCol, int yCol) {
        this.xCol = xCol;
        this.yCol = yCol;
        isPix = true;
    }

    /**
     * This constructor should be used when there are no coordinate columns.
     */
    public RowCoordinates() {
    }


    /** Return true if coordinate columns, such as (ra, dec), were defined */
    public boolean hasCoordinates() {
        return (isWcs || isPix);
    }


    /** Return true if the catalog has RA and DEC coordinate columns */
    public boolean isWCS() {
        return isWcs;
    }

    /** Return the equinox used for world coordinates */
    public double equinox() {
        return equinox;
    }

    /** Return true if the catalog has X and Y columns (assumed to be image pixel coordinates) */
    public boolean isPix() {
        return isPix;
    }


    /** Return a Coordinates object for the given row vector, or null if not found */
    public Coordinates getCoordinates(Vector row) {
        try {
            if (isWcs) {
                Object ra = row.get(xCol), dec = row.get(yCol);
                if (ra != null && dec != null) {
                    if (ra instanceof String && dec instanceof String
                            && ((String) ra).length() != 0
                            && ((String) dec).length() != 0) {
                        return new WorldCoords((String) ra, (String) dec, equinox);
                    }
                    else if (ra instanceof Double && dec instanceof Double) {
                        return new WorldCoords((Double) ra, (Double) dec, equinox);
                    }
                    else if (ra instanceof Float && dec instanceof Float) {
                        return new WorldCoords(((Float) ra).floatValue(), ((Float) dec).floatValue(), equinox);
                    }
                }
            }
            else if (isPix) {
                Object x = row.get(xCol), y = row.get(yCol);
                if (x != null && y != null) {
                    if (x instanceof Double && y instanceof Double)
                        return new ImageCoords((Double) x, (Double) y);
                    else if (x instanceof Float && y instanceof Float)
                        return new ImageCoords(((Float) x).floatValue(), ((Float) y).floatValue());
                }
            }
        }
        catch (NumberFormatException e) {
            // return null if there was a bad value
        }
        return null;
    }

    /** Return the column index for the X coordinate, or -1 if not known. */
    public int getXCol() {
        return xCol;
    }

    /** Return the column index for the Y coordinate, or -1 if not known. */
    public int getYCol() {
        return yCol;
    }

    /** Return the column index for the RA coordinate, or -1 if not known. */
    public int getRaCol() {
        return xCol;
    }

    /** Return the column index for the DEC coordinate, or -1 if not known. */
    public int getDecCol() {
        return yCol;
    }

    /** Return the value of the equinox for world coordinates, if it is known (default: 2000). */
    public double getEquinox() {
        return equinox;
    }

    /** Return the Index of a column containing the equnox for each row, or -1 if it is constant. */
    public int getEquinoxCol() {
        return equinoxCol;
    }

    /** Return the column containing the object id, or -1 if there isn't one. */
    public int getIdCol() {
        return idCol;
    }

    /** Set the column containing the object id (-1 if there isn't one). */
    public void setIdCol(int idCol) {
        this.idCol = idCol;
    }
}

