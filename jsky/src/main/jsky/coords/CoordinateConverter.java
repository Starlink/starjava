/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: CoordinateConverter.java,v 1.3 2002/07/09 13:30:36 brighton Exp $
 */

package jsky.coords;

import java.awt.geom.*;


/**
 * This interface may be implemented by classes that display images and can
 * convert between different coordinate systems, optionally including world
 * coordinates. Methods are available for converting between the following
 * coordinate systems:
 * <p>
 * <DL>
 *  <DT>Screen Coordinates</DT>
 *  <DD> The origin (0,0) is always at the upper left corner of the window.
 *       Whole pixels are counted and no transformations are taken into account.
 *  </DD>
 *
 *  <DT>Canvas Coordinates</DT>
 *  <DD> The origin (0,0) is at the upper left corner of the image.
 *       Whole pixels are counted and no transformations are taken into account.
 *  </DD>
 *
 *  <DT>Image Coordinates</DT>
 *  <DD> The origin is at lower left (FITS style) after all transformations are undone.
 *       At mag 1, the origin is (1, 1), otherwise it is a fraction of
 *       a pixel (0.5, 0.5). Image coordinates correspond to the coordinates in a
 *       FITS image.
 *  </DD>
 *
 *  <DT>User Coordinates</DT>
 *  <DD>  The origin (0.0, 0.0) is at upper left after all transformations undone.
 *        User coordinates are like image coordinates, except that the
 *        Y axis is reversed and the origin at mag 1 is (0., 0.) instead of (1. 1).
 *  </DD>
 *
 *  <DT>World Coordinates</DT>
 *  <DD> World coordinates are converted from image coordinates based on the keywords
 *       in the image header, if available.
 *  </DD>
 * </DL>
 *
 * @version $Revision: 1.3 $ $Date: 2002/07/09 13:30:36 $
 * @author Allan Brighton
 */
public abstract interface CoordinateConverter extends WorldCoordinateConverter {

    /** Constant for image coordinates */
    public static final int IMAGE = 0;

    /** Constant for screen coordinates */
    public static final int SCREEN = 1;

    /** Constant for canvas coordinates */
    public static final int CANVAS = 2;

    /** Constant for user coordinates */
    public static final int USER = 3;

    /** Constant for world coordinates (deg, in the equinox of the image) */
    public static final int WORLD = 4;

    /**
     * Convert the given coordinates from inType to outType. The inType and
     * outType arguments should be one of the constants defined in this interface
     * (IMAGE for image coordinates, WCS for world coordinates, etc...).
     *
     * @param p The point to convert.
     * @param inType the type of the input coordinates
     * @param outType the type of the output coordinates
     * @param isDistance True if p should be interpreted as a distance instead
     *                   of a point.
     */
    public void convertCoords(Point2D.Double p, int inType, int outType, boolean isDistance);

    /**
     * Convert the given canvas coordinates to image coordinates.
     *
     * @param p The point to convert.
     * @param isDistance True if p should be interpreted as a distance instead
     *                   of a point.
     */
    public void canvasToImageCoords(Point2D.Double p, boolean isDistance);

    /**
     * Convert the given canvas coordinates to user coordinates.
     *
     * @param p The point to convert.
     * @param isDistance True if p should be interpreted as a distance instead
     *                   of a point.
     */
    public void canvasToUserCoords(Point2D.Double p, boolean isDistance);

    /**
     * Convert the given user coordinates to image coordinates.
     *
     * @param p The point to convert.
     * @param isDistance True if p should be interpreted as a distance instead
     *                   of a point.
     */
    public void userToImageCoords(Point2D.Double p, boolean isDistance);

    /**
     * Convert the given user coordinates to canvas coordinates.
     *
     * @param p The point to convert.
     * @param isDistance True if p should be interpreted as a distance instead
     *                   of a point.
     */
    public void userToCanvasCoords(Point2D.Double p, boolean isDistance);

    /**
     * Convert the given image coordinates to canvas coordinates.
     *
     * @param p The point to convert.
     * @param isDistance True if p should be interpreted as a distance instead
     *                   of a point.
     */
    public void imageToCanvasCoords(Point2D.Double p, boolean isDistance);

    /**
     * Convert the given image coordinates to user coordinates.
     *
     * @param p The point to convert.
     * @param isDistance True if p should be interpreted as a distance instead
     *                   of a point.
     */
    public void imageToUserCoords(Point2D.Double p, boolean isDistance);

    /**
     * Convert the given canvas coordinates to screen coordinates.
     *
     * @param p The point to convert.
     * @param isDistance True if p should be interpreted as a distance instead
     *                   of a point.
     */
    public void canvasToScreenCoords(Point2D.Double p, boolean isDistance);

    /**
     * Convert the given screen coordinates to canvas coordinates.
     *
     * @param p The point to convert.
     * @param isDistance True if p should be interpreted as a distance instead
     *                   of a point.
     */
    public void screenToCanvasCoords(Point2D.Double p, boolean isDistance);

    /**
     * Convert the given screen coordinates to image coordinates.
     *
     * @param p The point to convert.
     * @param isDistance True if p should be interpreted as a distance instead
     *                   of a point.
     */
    public void screenToImageCoords(Point2D.Double p, boolean isDistance);

    /**
     * Convert the given image coordinates to screen coordinates.
     *
     * @param p The point to convert.
     * @param isDistance True if p should be interpreted as a distance instead
     *                   of a point.
     */
    public void imageToScreenCoords(Point2D.Double p, boolean isDistance);


    /**
     * Convert the given screen coordinates to user coordinates.
     *
     * @param p The point to convert.
     * @param isDistance True if p should be interpreted as a distance instead
     *                   of a point.
     */
    public void screenToUserCoords(Point2D.Double p, boolean isDistance);

    /**
     * Convert the given user coordinates to screen coordinates.
     *
     * @param p The point to convert.
     * @param isDistance True if p should be interpreted as a distance instead
     *                   of a point.
     */
    public void userToScreenCoords(Point2D.Double p, boolean isDistance);

    /**
     * Convert the given screen coordinates to world coordinates degrees in the equinox
     * of the current image.
     *
     * @param p The point to convert.
     * @param isDistance True if p should be interpreted as a distance instead
     *                   of a point.
     */
    public void screenToWorldCoords(Point2D.Double p, boolean isDistance);

    /**
     * Convert the given canvas coordinates to world coordinates degrees in the equinox
     * of the current image.
     *
     * @param p The point to convert.
     * @param isDistance True if p should be interpreted as a distance instead
     *                   of a point.
     */
    public void canvasToWorldCoords(Point2D.Double p, boolean isDistance);

    /**
     * Convert the given user coordinates to world coordinates degrees in the equinox
     * of the current image.
     *
     * @param p The point to convert.
     * @param isDistance True if p should be interpreted as a distance instead
     *                   of a point.
     */
    public void userToWorldCoords(Point2D.Double p, boolean isDistance);

    /**
     * Convert the given world coordinates (degrees, in the equinox of the current image)
     * to canvas coordinates.
     *
     * @param p The point to convert.
     * @param isDistance True if p should be interpreted as a distance instead
     *                   of a point.
     */
    public void worldToCanvasCoords(Point2D.Double p, boolean isDistance);

    /**
     * Convert the given world coordinates (degrees, in the equinox of the current image)
     * to screen coordinates.
     *
     * @param p The point to convert.
     * @param isDistance True if p should be interpreted as a distance instead
     *                   of a point.
     */
    public void worldToScreenCoords(Point2D.Double p, boolean isDistance);

    /**
     * Convert the given world coordinates (degrees, in the equinox of the current image)
     * to user coordinates.
     *
     * @param p The point to convert.
     * @param isDistance True if p should be interpreted as a distance instead
     *                   of a point.
     */
    public void worldToUserCoords(Point2D.Double p, boolean isDistance);
}

