/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: BasicImageDisplay.java,v 1.8 2002/08/16 22:21:13 brighton Exp $
 */

package jsky.image.gui;

import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.media.jai.Interpolation;
import javax.media.jai.PlanarImage;
import javax.swing.JComponent;

import jsky.coords.CoordinateConverter;
import jsky.coords.WorldCoordinateConverter;
import jsky.image.BasicImageReadableProcessor;
import jsky.image.ImageProcessor;

/**
 * This defines the common interface for classes that display an image
 * (a JAI PlanarImage).
 * <p>
 * This interface also assumes that the ImageProcessor class is used to
 * process the source image to produce the actual image to be displayed.
 * <p>
 * Any JAI PlanarImage can be displayed. Grayscale images may be displayed with
 * false colors, depending on the ImageProcessor options specified.
 *
 * @version $Revision: 1.8 $
 * @author Allan Brighton
 * @author Peter W. Draper
 */
public abstract interface BasicImageDisplay extends BasicImageReadableProcessor {

    /**
     * Set the source image to display.
     */
    public void setImage(PlanarImage im);

    /**
     * Return the source image (before processing).
     */
    public PlanarImage getImage();

    /**
     * Return the image being displayed (after image processing).
     */
    public PlanarImage getDisplayImage();

    /** Return true if this widget has been initialized and is displaying an image. */
    public boolean isInitialized();

    /**
     * Update the image display after a change has been made.
     */
    public void updateImage();

    /**
     * Set the image processor to use to get the image to display.
     */
    public void setImageProcessor(ImageProcessor imageProcessor);

    /**
     * Return the image processor object.
     */
    public ImageProcessor getImageProcessor();

    /**
     * Return the width of the source image in pixels
     */
    public int getImageWidth();

    /**
     * Return the height of the source image in pixels
     */
    public int getImageHeight();

    /**
     * Return the origin of the displayed image in canvas coordinates.
     */
    public Point2D.Double getOrigin();

    /**
     * Set the scale (zoom factor) for the image.
     */
    public void setScale(float scale);

    /**
     * Return the current scale (zoom factor) for the image.
     */
    public float getScale();

    /**
     * Set the scaling factor so that the image will fit in the current window.
     * <p>
     * Note that only integer scaling factors are used, for example
     * 2, 1, 1/2, 1/3, etc., for performance reasons.
     */
    public void scaleToFit();

    /**
     * Set to true if the image being displayed has been prescaled (such as
     * for a pan window or thumbnail image).
     * If true, the scale value will only be used to calculate coordinate
     * transformations, but the image will not actually be scaled.
     */
    public void setPrescaled(boolean b);

    /** Return true if the image has been prescaled */
    public boolean isPrescaled();


    /** Set the optional rendering hints for the image scale operation */
    public void setScaleHints(RenderingHints hints);

    /** Return the optional rendering hints used for the image scale operation */
    public RenderingHints getScaleHints();

    /** Set to true (default) to automatically center the image, if it is smaller than the window. */
    public void setAutoCenterImage(boolean b);

    /** Return true if the image is automatically centered when it is smaller than the window. */
    public boolean isAutoCenterImage();

    /**
     * Set the interpolation object used to scale the image (a subclass
     * of Interpolation, such as InterpolationNearest (default), or
     * InterpolationBilinear (better, but slower)).
     */
    public void setInterpolation(Interpolation i);

    /** Return the interpolation object used to scale the image */
    public Interpolation getInterpolation();

    /**
     * Return the value of the pixel in the given band at the given user coordinates
     */
    public double getPixelValue(Point2D.Double p, int band);
    // PWD: made return double

    /**
     * Return a rectangle describing the visible area of the image
     * (in user coordinates).
     */
    public Rectangle2D.Double getVisibleArea();

    /**
     * Set the origin of the image to display in canvas coordinates.
     */
    public void setOrigin(Point2D.Double origin);

    /**
     * Return the image canvas component.
     */
    public JComponent getCanvas();

    /**
     * Set to true if scrolling and other operations should update the image immediately,
     * otherwise only on button release.
     */
    public void setImmediateMode(boolean b);

    /** Return true if immediate mode is turned on. */
    public boolean isImmediateMode();

    /**
     * Return true if the current image supports world coordinates
     * (has the necessary keywords in the header).
     */
    public boolean isWCS();

    /**
     * Return the object used to convert between image and world coordinates,
     * or null if none is available.
     */
    public WorldCoordinateConverter getWCS();

    /**
     * Set the object used to convert between image and world coordinates.
     */
    public void setWCS(WorldCoordinateConverter wcs);

    /** Return the object used to convert coordinates. */
    public CoordinateConverter getCoordinateConverter();

    /**
     * Register as an image graphics handler.
     */
    public void addImageGraphicsHandler(ImageGraphicsHandler igh);

    /**
     * Unregister as an image graphics handler.
     */
    public void removeImageGraphicsHandler(ImageGraphicsHandler igh);
}
