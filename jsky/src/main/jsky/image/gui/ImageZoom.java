/*
 * ESO Archive
 *
 * $Id: ImageZoom.java,v 1.8 2002/08/16 22:21:13 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/03  Created
 */

package jsky.image.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jsky.image.ImageChangeEvent;
import jsky.image.ImageUtil;

/**
 * This widget is for displaying a magnified section of the image
 * at the mouse pointer.
 *
 * @version $Revision: 1.8 $
 * @author Allan Brighton
 */
public class ImageZoom extends JComponent
        implements ChangeListener, ImageGraphicsHandler {

    /** The default size for the zoom window */
    public static final int DEFAULT_SIZE = ImagePanner.DEFAULT_SIZE;

    /** The target image display being magnified */
    private MainImageDisplay _mainImageDisplay;

    /** The zoom image display */
    private ImageDisplay _imageDisplay;

    /** width of the zoom window */
    private int _zoomWidth;

    /** height of the zoom window */
    private int _zoomHeight;

    /** The zoom (magnification) factor */
    private float _zoomFactor;

    /** True if zooming is enabled */
    private boolean _enabled = true;

    /** True if changes in the mouse position should be tracked and the image updated.  */
    private boolean _active = true;

    /** If true (default), changes in the main image scale are propagated to the zoom window.  */
    private boolean _propagateScale = true;

    /**
     * Coordinates of rectangles to display indicating the position of
     * the mouse pointer in the main image (in screen coords: one
     * black and one white, for better visibility).
     */
    private Rectangle2D.Double[] _rect = new Rectangle2D.Double[2];


    /**
     * Constructor
     *
     * @param mainImageDisplay The target image display being controlled.
     * @param width The desired width of the zoom window
     * @param height The desired height of the zoom window
     * @param factor The zoom (magnification) factor
     */
    public ImageZoom(MainImageDisplay mainImageDisplay, int width, int height, float factor) {
        _imageDisplay = new ImageDisplay("zoom window");
        _imageDisplay.addImageGraphicsHandler(this);
        _imageDisplay.setAutoCenterImage(false);

        _zoomFactor = factor;
        _zoomWidth = width;
        _zoomHeight = height;

        _rect[0] = new Rectangle2D.Double();
        _rect[1] = new Rectangle2D.Double();

        // Don't use tiles for the zoom window
        _imageDisplay.setScaleHints(ImageUtil.getTileCacheHint(0, _zoomWidth, _zoomHeight));

        JComponent c = (JComponent) _imageDisplay;
        c.setPreferredSize(new Dimension(_zoomWidth, _zoomHeight));
        setLayout(new BorderLayout());
        add(c, BorderLayout.CENTER);

        setMainImageDisplay(mainImageDisplay);
    }


    /**
     * Constructor
     *
     * @param mainImageDisplay The target image display being controlled.
     */
    public ImageZoom(MainImageDisplay mainImageDisplay) {
        this(mainImageDisplay, DEFAULT_SIZE, DEFAULT_SIZE, 4);
    }


    /**
     * Default Constructor (must call setMainImageDisplay() later).
     */
    public ImageZoom() {
        this(null);
    }


    /** Set the zoom factor */
    public void setZoomFactor(float factor) {
        if (factor < 1)
            return;
        _zoomFactor = factor;
    }

    /** Return the zoom factor */
    public float getZoomFactor() {
        return _zoomFactor;
    }


    /**
     * Set the target image display that we are monitoring.
     */
    public void setMainImageDisplay(MainImageDisplay mainImageDisplay) {
        _mainImageDisplay = mainImageDisplay;

        if (_mainImageDisplay != null) {
            if (_propagateScale) {
		if (_mainImageDisplay.isPrescaled())
		    _imageDisplay.setScale(_zoomFactor, _mainImageDisplay.getScale() * _zoomFactor);
		else
		    _imageDisplay.setScale(_mainImageDisplay.getScale() * _zoomFactor);
	    }
            _imageDisplay.setImageProcessor(_mainImageDisplay.getImageProcessor());

            // register to receive notification when the image changes
            _mainImageDisplay.addChangeListener(this);
            ((Component) _mainImageDisplay).addMouseMotionListener(new MouseMotionAdapter() {

                public void mouseMoved(MouseEvent e) {
                    zoom(e.getX(), e.getY(), false);
                }

                public void mouseDragged(MouseEvent e) {
                    zoom(e.getX(), e.getY(), false);
                }
            });
        }
    }


    /** Return the zoom image display. */
    public BasicImageDisplay getImageDisplay() {
        return _imageDisplay;
    }


    /**
     * Return the target ImageDisplay that we are monitoring.
     */
    public MainImageDisplay getMainImageDisplay() {
        return _mainImageDisplay;
    }


    /** Enable or disable the zoom window */
    public void setEnabled(boolean b) {
        _enabled = b;
    }

    /** Return the enabled state of the zoom window */
    public boolean isEnabled() {
        return _enabled;
    }

    /**
     * Set the active state to false to stop the zoom window from updating when the
     * mouse is moved over the main image.
     */
    public void setActive(boolean b) {
        _active = b;
    }

    /** Return the active state of the zoom window */
    public boolean isActive() {
        return _active;
    }

    /** If true (default), changes in the main image scale are propagated to the zoom window.  */
    public void setPropagateScale(boolean b) {
        _propagateScale = b;
    }

    /** Return true if changes in the main image scale are propagated to the zoom window.*/
    public boolean isPropagateScale() {
        return _propagateScale;
    }

    /**
     * This method is called when the mouse is moved over the target image.
     * @param x the X screen coordinate in the main image
     * @param y the Y screen coordinate in the main image
     * @param force if true, do the update even if the "active" flag is set to false
     */
    public void zoom(int x, int y, boolean force) {
        if (!_active && !force)
            return;
        Point2D.Double origin = new Point2D.Double(x, y);
        _mainImageDisplay.getCoordinateConverter().screenToUserCoords(origin, false);
        _imageDisplay.getCoordinateConverter().userToCanvasCoords(origin, false);
        origin.x -= _zoomWidth / 2.;
        origin.y -= _zoomHeight / 2.;
        _imageDisplay.setOrigin(origin);
        _imageDisplay.updateImage();
    }


    /**
     * Called when the main image changes in some way. The change event
     * (ImageChangeEvent) describes what changed.
     */
    public void stateChanged(ChangeEvent ce) {
        ImageChangeEvent e = (ImageChangeEvent) ce;
        if (e.isNewScale() || (e.isNewImage() && !e.isBefore())) {
            if (!_propagateScale) {
                setZoomFactor(_imageDisplay.getScale() / _mainImageDisplay.getScale());
            }

	    if (_mainImageDisplay.isPrescaled())
		_imageDisplay.setScale(_zoomFactor, _mainImageDisplay.getScale() * _zoomFactor);
	    else 
		_imageDisplay.setScale(_mainImageDisplay.getScale() * _zoomFactor);

            updateRect();
        }
    }


    /**
     * Update the rect object with the coordinates of the mouse pointer
     * in the target image.
     */
    public void updateRect() {
        if (!_imageDisplay.isInitialized() || !_mainImageDisplay.isInitialized()) {
            return;
        }

        double f = _mainImageDisplay.getScale();
        if (f <= 1) {
            // box around pixel
            f = _zoomFactor;
            _rect[0].x = getWidth() / 2.0;
            _rect[0].y = getHeight() / 2.0;
            _rect[0].width = _rect[0].height = f;
        }
        else {
            // box part of pixel
            f *= _zoomFactor;
            _rect[0].x = getWidth() / 2.0 - f / 2.0;
            _rect[0].y = getHeight() / 2.0 - f / 2.0;
            _rect[0].width = _rect[0].height = f;
        }
        _rect[1].x = _rect[0].x - 1;
        _rect[1].y = _rect[0].y - 1;
        _rect[1].width = _rect[1].height = _rect[0].width + 2;
    }


    /**
     * Called each time the image is repainted to draw a rectangle on
     * the image marking the position of the mouse pointer in the
     * target image.
     */
    public void drawImageGraphics(BasicImageDisplay imageDisplay, Graphics2D g) {
        g.setColor(Color.white); // for better visibility: use black & white rects
        g.draw(_rect[0]);
        g.setColor(Color.black);
        g.draw(_rect[1]);
    }
}
