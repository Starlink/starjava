/*
 * Copyright 2002 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: ImageCoordinateConverter.java,v 1.9 2002/08/16 22:21:13 brighton Exp $
 */

package jsky.image.gui;

import java.awt.geom.Point2D;

import javax.media.jai.PlanarImage;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import jsky.coords.CoordinateConverter;
import jsky.image.ImageProcessor;


/**
 * This utility class may be used by classes that display images to convert
 * between different coordinate systems, optionally including world
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
 * @version $Revision: 1.9 $
 * @author Allan Brighton
 */
public class ImageCoordinateConverter implements CoordinateConverter {

    /** The target image display */
    private BasicImageDisplay _imageDisplay;

    /** list of listeners for change events */
    private EventListenerList _listenerList = new EventListenerList();

    /** Event fired whenever the WCS info is changed. */
    private ChangeEvent _changeEvent = new ChangeEvent(this);


    /*
     * Construct an object for converting coordinates for the given
     * image display.
     */
    public ImageCoordinateConverter(BasicImageDisplay imageDisplay) {
        this._imageDisplay = imageDisplay;
    }

    /** Return the target image display */
    public BasicImageDisplay getImageDisplay() {
        return _imageDisplay;
    }

    /**
     * Register to receive change events from this object whenever the
     * the WCS information is changed.
     */
    public void addChangeListener(ChangeListener l) {
        _listenerList.add(ChangeListener.class, l);
    }

    /**
     * Stop receiving change events from this object.
     */
    public void removeChangeListener(ChangeListener l) {
        _listenerList.remove(ChangeListener.class, l);
    }

    /**
     * Notify any listeners of a change in the WCS information.
     */
    protected void fireChange() {
        Object[] listeners = _listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ChangeListener.class) {
                ((ChangeListener) listeners[i + 1]).stateChanged(_changeEvent);
            }
        }
    }

    /**
     * Return true if world coordinates conversion is available. This method
     * should be called to check before calling any of the world coordinates
     * conversion methods.
     */
    public boolean isWCS() {
        return _imageDisplay.getWCS() != null;
    }

    /** Return the center RA,Dec coordinates in degrees. */
    public Point2D.Double getWCSCenter() {
        return _imageDisplay.getWCS().getWCSCenter();
    }

    /** Set the center RA,Dec coordinates in degrees in the current equinox. */
    //public void setWCSCenter(Point2D.Double p) {
    //imageDisplay.getWCS().setWCSCenter(p);
    //fireChange();
    //}

    /** Return the center coordinates in image pixels. */
    public Point2D.Double getImageCenter() {
        return _imageDisplay.getWCS().getImageCenter();
    }

    /** Return the equinox used for coordinates (usually the equionx of the image) */
    public double getEquinox() {
        if (isWCS()) {
            return _imageDisplay.getWCS().getEquinox();
        }
        return 2000.;
    }

    /** Return the width in deg */
    public double getWidthInDeg() {
        return _imageDisplay.getWCS().getWidthInDeg();
    }

    /** Return the height in deg */
    public double getHeightInDeg() {
        return _imageDisplay.getWCS().getHeightInDeg();
    }

    /** Return the width in pixels */
    public double getWidth() {
        return _imageDisplay.getImageWidth();
    }

    /** Return the height in pixels */
    public double getHeight() {
        return _imageDisplay.getImageHeight();
    }

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
    public void convertCoords(Point2D.Double p, int inType, int outType, boolean isDistance) {
        switch (inType) {
        case IMAGE:
            switch (outType) {
            case IMAGE:
                break;
            case SCREEN:
                imageToScreenCoords(p, isDistance);
                break;
            case CANVAS:
                imageToCanvasCoords(p, isDistance);
                break;
            case USER:
                imageToUserCoords(p, isDistance);
                break;
            case WORLD:
                imageToWorldCoords(p, isDistance);
                break;
            }
            break;
        case SCREEN:
            switch (outType) {
            case IMAGE:
                screenToImageCoords(p, isDistance);
                break;
            case SCREEN:
                break;
            case CANVAS:
                screenToCanvasCoords(p, isDistance);
                break;
            case USER:
                screenToUserCoords(p, isDistance);
                break;
            case WORLD:
                screenToWorldCoords(p, isDistance);
                break;
            }
            break;
        case CANVAS:
            switch (outType) {
            case IMAGE:
                canvasToImageCoords(p, isDistance);
                break;
            case SCREEN:
                canvasToScreenCoords(p, isDistance);
                break;
            case CANVAS:
                break;
            case USER:
                canvasToUserCoords(p, isDistance);
                break;
            case WORLD:
                canvasToWorldCoords(p, isDistance);
                break;
            }
            break;
        case USER:
            switch (outType) {
            case IMAGE:
                userToImageCoords(p, isDistance);
                break;
            case SCREEN:
                userToScreenCoords(p, isDistance);
                break;
            case CANVAS:
                userToCanvasCoords(p, isDistance);
                break;
            case USER:
                break;
            case WORLD:
                userToWorldCoords(p, isDistance);
                break;
            }
            break;
        case WORLD:
            switch (outType) {
            case IMAGE:
                worldToImageCoords(p, isDistance);
                break;
            case SCREEN:
                worldToScreenCoords(p, isDistance);
                break;
            case CANVAS:
                worldToCanvasCoords(p, isDistance);
                break;
            case USER:
                worldToUserCoords(p, isDistance);
                break;
            case WORLD:
                break;
            }
            break;
        }
    }


    /**
     * Convert the given canvas coordinates to image coordinates.
     *
     * @param p The point to convert.
     * @param isDistance True if p should be interpreted as a distance instead
     *                   of a point.
     */
    public void canvasToImageCoords(Point2D.Double p, boolean isDistance) {
        float scale = _imageDisplay.getScale();
        p.x /= scale;
        p.y /= scale;
        if (!isDistance) {
            rotate(p, -1);
            flip(p);
            // Use FITS style coords: start at 1,1 (0.5 is left side of pixel when zoomed)
            double f = (scale > 1.0f) ? 0.5 : 1.0;
            p.x += f;
            p.y += f;
        }
    }


    /**
     * Convert the given user coordinates to image coordinates.
     *
     * @param p The point to convert.
     * @param isDistance True if p should be interpreted as a distance instead
     *                   of a point.
     */
    public void userToImageCoords(Point2D.Double p, boolean isDistance) {
        userToCanvasCoords(p, isDistance);
        canvasToImageCoords(p, isDistance);
    }


    /**
     * Convert the given canvas coordinates to user coordinates.
     *
     * @param p The point to convert.
     * @param isDistance True if p should be interpreted as a distance instead
     *                   of a point.
     */
    public void canvasToUserCoords(Point2D.Double p, boolean isDistance) {
        float scale = _imageDisplay.getScale();
        p.x /= scale;
        p.y /= scale;
        if (!isDistance)
            rotate(p, -1);
    }


    /**
     * Convert the given user coordinates to canvas coordinates.
     *
     * @param p The point to convert.
     * @param isDistance True if p should be interpreted as a distance instead
     *                   of a point.
     */
    public void userToCanvasCoords(Point2D.Double p, boolean isDistance) {
        if (!isDistance)
            rotate(p, 1);
        float scale = _imageDisplay.getScale();
        p.x *= scale;
        p.y *= scale;
    }


    /**
     * Convert the given image coordinates to canvas coordinates.
     *
     * @param p The point to convert.
     * @param isDistance True if p should be interpreted as a distance instead
     *                   of a point.
     */
    public void imageToCanvasCoords(Point2D.Double p, boolean isDistance) {
        float scale = _imageDisplay.getScale();
        if (!isDistance) {
            // Use FITS style coords: start at 1,1 (0.5 is left side of pixel when zoomed)
            double f = (scale > 1.0f) ? 0.5 : 1.0;
            p.x -= f;
            p.y -= f;
            flip(p);
            rotate(p, 1);
        }

        p.x *= scale;
        p.y *= scale;
    }


    /**
     * Convert the given image coordinates to user coordinates.
     *
     * @param p The point to convert.
     * @param isDistance True if p should be interpreted as a distance instead
     *                   of a point.
     */
    public void imageToUserCoords(Point2D.Double p, boolean isDistance) {
        imageToCanvasCoords(p, isDistance);
        canvasToUserCoords(p, isDistance);
    }


    /**
     * Convert the given canvas coordinates to screen coordinates.
     *
     * @param p The point to convert.
     * @param isDistance True if p should be interpreted as a distance instead
     *                   of a point.
     */
    public void canvasToScreenCoords(Point2D.Double p, boolean isDistance) {
        PlanarImage displayImage = _imageDisplay.getDisplayImage();
        if (!isDistance && displayImage != null) {
            p.x += displayImage.getMinX();
            p.y += displayImage.getMinY();
        }
    }


    /**
     * Convert the given screen coordinates to canvas coordinates.
     *
     * @param p The point to convert.
     * @param isDistance True if p should be interpreted as a distance instead
     *                   of a point.
     */
    public void screenToCanvasCoords(Point2D.Double p, boolean isDistance) {
        PlanarImage displayImage = _imageDisplay.getDisplayImage();
        if (!isDistance && displayImage != null) {
            p.x -= displayImage.getMinX();
            p.y -= displayImage.getMinY();
        }
    }


    /**
     * Convert the given screen coordinates to image coordinates.
     *
     * @param p The point to convert.
     * @param isDistance True if p should be interpreted as a distance instead
     *                   of a point.
     */
    public void screenToImageCoords(Point2D.Double p, boolean isDistance) {
        screenToCanvasCoords(p, isDistance);
        canvasToImageCoords(p, isDistance);
    }

    /**
     * Convert the given image coordinates to screen coordinates.
     *
     * @param p The point to convert.
     * @param isDistance True if p should be interpreted as a distance instead
     *                   of a point.
     */
    public void imageToScreenCoords(Point2D.Double p, boolean isDistance) {
        imageToCanvasCoords(p, isDistance);
        canvasToScreenCoords(p, isDistance);
    }


    /**
     * Convert the given screen coordinates to user coordinates.
     *
     * @param p The point to convert.
     * @param isDistance True if p should be interpreted as a distance instead
     *                   of a point.
     */
    public void screenToUserCoords(Point2D.Double p, boolean isDistance) {
        screenToCanvasCoords(p, isDistance);
        canvasToUserCoords(p, isDistance);
    }

    /**
     * Convert the given user coordinates to screen coordinates.
     *
     * @param p The point to convert.
     * @param isDistance True if p should be interpreted as a distance instead
     *                   of a point.
     */
    public void userToScreenCoords(Point2D.Double p, boolean isDistance) {
        userToCanvasCoords(p, isDistance);
        canvasToScreenCoords(p, isDistance);
    }


    /**
     * Convert the given image coordinates to world coordinates degrees in the equinox
     * of the current image.
     *
     * @param p The point to convert.
     * @param isDistance True if p should be interpreted as a distance instead
     *                   of a point.
     */
    public void imageToWorldCoords(Point2D.Double p, boolean isDistance) {
        assertWCS();
        _imageDisplay.getWCS().imageToWorldCoords(p, isDistance);
    }


    /**
     * Convert the given screen coordinates to world coordinates degrees in the equinox
     * of the current image.
     *
     * @param p The point to convert.
     * @param isDistance True if p should be interpreted as a distance instead
     *                   of a point.
     */
    public void screenToWorldCoords(Point2D.Double p, boolean isDistance) {
        assertWCS();
        screenToImageCoords(p, isDistance);
        imageToWorldCoords(p, isDistance);
    }


    /**
     * Convert the given canvas coordinates to world coordinates degrees in the equinox
     * of the current image.
     *
     * @param p The point to convert.
     * @param isDistance True if p should be interpreted as a distance instead
     *                   of a point.
     */
    public void canvasToWorldCoords(Point2D.Double p, boolean isDistance) {
        assertWCS();
        canvasToImageCoords(p, isDistance);
        imageToWorldCoords(p, isDistance);
    }


    /**
     * Convert the given user coordinates to world coordinates degrees in the equinox
     * of the current image.
     *
     * @param p The point to convert.
     * @param isDistance True if p should be interpreted as a distance instead
     *                   of a point.
     */
    public void userToWorldCoords(Point2D.Double p, boolean isDistance) {
        assertWCS();
        userToImageCoords(p, isDistance);
        imageToWorldCoords(p, isDistance);
    }


    /**
     * Convert the given world coordinates (degrees, in the equinox of the current image)
     * to image coordinates.
     *
     * @param p The point to convert.
     * @param isDistance True if p should be interpreted as a distance instead
     *                   of a point.
     */
    public void worldToImageCoords(Point2D.Double p, boolean isDistance) {
        assertWCS();
        _imageDisplay.getWCS().worldToImageCoords(p, isDistance);
    }


    /**
     * Convert the given world coordinates (degrees, in the equinox of the current image)
     * to canvas coordinates.
     *
     * @param p The point to convert.
     * @param isDistance True if p should be interpreted as a distance instead
     *                   of a point.
     */
    public void worldToCanvasCoords(Point2D.Double p, boolean isDistance) {
        assertWCS();
        worldToImageCoords(p, isDistance);
        imageToCanvasCoords(p, isDistance);
    }


    /**
     * Convert the given world coordinates (degrees, in the equinox of the current image)
     * to screen coordinates.
     *
     * @param p The point to convert.
     * @param isDistance True if p should be interpreted as a distance instead
     *                   of a point.
     */
    public void worldToScreenCoords(Point2D.Double p, boolean isDistance) {
        assertWCS();
        worldToImageCoords(p, isDistance);
        imageToScreenCoords(p, isDistance);
    }


    /**
     * Convert the given world coordinates (degrees, in the equinox of the current image)
     * to user coordinates.
     *
     * @param p The point to convert.
     * @param isDistance True if p should be interpreted as a distance instead
     *                   of a point.
     */
    public void worldToUserCoords(Point2D.Double p, boolean isDistance) {
        assertWCS();
        worldToImageCoords(p, isDistance);
        imageToUserCoords(p, isDistance);
    }

    /**
     * Flip the x,y coordinates of the given point based on the current settings.
     * This assumes that the input coordinates are in terms of the source
     * image (i.e.: with no scaling applied).
     * Note that the meaning of flipY is reversed here to conform to FITS coords,
     * where the origin is at lower left.
     */
    public void flip(Point2D.Double p) {
        // when scale is > 1, use decimal coords, otherwise integer
        float scale = _imageDisplay.getScale();
        int c = (scale > 1.0f) ? 0 : 1;
        double x = p.x, y = p.y;

        int width = _imageDisplay.getImageWidth(),
	    height = _imageDisplay.getImageHeight();

        ImageProcessor imageProcessor = _imageDisplay.getImageProcessor();
	boolean flipY = (imageProcessor.getFlipY() != imageProcessor.getReverseY());
	if (imageProcessor.isInvertedYAxis())
	    flipY = !flipY;
        if  (flipY) 
            y = height - c - y;

        if (imageProcessor.getFlipX())
            x = width - c - x;

        p.setLocation(x, y);
    }


    /**
     * Rotate the given point about the image center by the current rotation angle,
     * multiplied by the given factor.
     * This assumes that the input coordinates are in terms of the source
     * image (i.e.: with no scaling applied).
     *
     * @param p the point to rotate
     * @param factor set to 1 to rotate, -1 to undo the rotation
     */
    public void rotate(Point2D.Double p, int factor) {
        ImageProcessor imageProcessor = _imageDisplay.getImageProcessor();

        // angle is in rad
        double angle = imageProcessor.getAngle() * -factor;
        if (angle == 0.0)
            return;

        double cx = _imageDisplay.getImageWidth() / 2.0;
        double cy = _imageDisplay.getImageHeight() / 2.0;

        p.x -= cx;
        p.y -= cy;
        double tmp = p.x;
        double cosa = Math.cos(angle);
        double sina = Math.sin(angle);
        p.x = p.x * cosa + p.y * sina + cx;
        p.y = -tmp * sina + p.y * cosa + cy;
    }

    /** Throw an exception is WCS information is not available */
    public void assertWCS() {
        if (!isWCS())
            throw new RuntimeException("No world coordinate information available.");
    }
}
