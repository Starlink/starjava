/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: WorldCoordinateConverter.java,v 1.4 2002/07/09 13:30:36 brighton Exp $
 */

package jsky.coords;

import java.awt.geom.Point2D;

import javax.swing.event.ChangeListener;


/**
 * This defines the interface for converting between image and world coordinates.
 *
 * @version $Revision: 1.4 $
 * @author Allan Brighton
 */
public abstract interface WorldCoordinateConverter {

    /**
     * Return true if world coordinates conversion is available. This method
     * should be called to check before calling any of the world coordinates
     * conversion methods.
     */
    public boolean isWCS();

    /** Return the equinox used for coordinates (usually the equionx of the image) */
    public double getEquinox();

    /**
     * Convert the given image coordinates to world coordinates degrees in the equinox
     * of the current image.
     *
     * @param p The point to convert.
     * @param isDistance True if p should be interpreted as a distance instead
     *                   of a point.
     */
    public void imageToWorldCoords(Point2D.Double p, boolean isDistance);

    /**
     * Convert the given world coordinates (degrees, in the equinox of the current image)
     * to image coordinates.
     *
     * @param p The point to convert.
     * @param isDistance True if p should be interpreted as a distance instead
     *                   of a point.
     */
    public void worldToImageCoords(Point2D.Double p, boolean isDistance);

    /** Return the center RA,Dec coordinates in degrees. */
    public Point2D.Double getWCSCenter();

    /** Set the center RA,Dec coordinates in degrees. */
    //public void setWCSCenter(Point2D.Double p);

    /** return the width in deg */
    public double getWidthInDeg();

    /** return the height in deg */
    public double getHeightInDeg();

    /** Return the image center coordinates in pixels (image coordinates). */
    public Point2D.Double getImageCenter();

    /** Return the image width in pixels. */
    public double getWidth();

    /** Return the image height in pixels. */
    public double getHeight();
}

