/*
 * ESO Archive
 *
 * $Id: ImagePanner.java,v 1.13 2002/08/16 22:21:13 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/03  Created
 * Peter W. Draper 2002/09/20  Worked around problem with supporting 
 *                             non-FITS Planar images.
 */

package jsky.image.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.media.jai.PlanarImage;
import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MouseInputListener;

import jsky.coords.CoordinateConverter;
import jsky.coords.WorldCoordinateConverter;
import jsky.image.ImageChangeEvent;
import jsky.image.ImageProcessor;
import jsky.image.fits.codec.FITSImage;
import jsky.image.graphics.ShapeUtil;
import jsky.image.operator.ImageOps;

/**
 * This widget displays a "view" of an image at a low magnification,
 * so that the entire image is visible in a small window. A rectangle
 * displayed over the image can be used to pan or move the image when
 * the target image is to large to be viewed at all at once in its
 * window. The rectangle is always notified of changes in the target
 * image or its window, so it always displays the relative size of the
 * visible image to the entire image.
 * <p>
 * Since it is not known ahead of time how large or small an image
 * will be, the pan window is given a maximum size when created. When
 * an image is loaded, it shrinks the image by equal integer factors
 * until it fits in the window. Then it fits the window around the
 * image, so as not to leave a blank (black) space around it. Rotated
 * images are also displayed as rotated and flipped in the pan window.
 * Only the scale factor remains fixed.
 *
 * @version $Revision: 1.13 $
 * @author Allan Brighton
 */
public class ImagePanner extends JComponent
        implements MouseInputListener, ChangeListener, ImageGraphicsHandler {

    /** The default size for the pan window */
    public static final int DEFAULT_SIZE = 152;

    /** The target image display being controlled */
    private MainImageDisplay _mainImageDisplay;

    /** The pan image display */
    private BasicImageDisplay _imageDisplay;

    /** A prescaled image to use for the pan window */
    private PlanarImage _pannerImage;

    /** Width of pan window */
    private int _panWidth;

    /** Height of pan window */
    private int _panHeight;

    /** starting position of mouse drag in screen coords */
    private Point2D.Double _mark;

    /** offset from mouse drag starting pos in screen coords */
    private Point2D.Double _offset;

    /**
     * Coordinates of rectangle to display indicating visible area of image
     * (in screen coords)
     */
    private Rectangle2D.Double _rect;

    /** Shape of the compass symbol to display WCS north and east, if known */
    private Shape _compass;

    /** position of N label for compass */
    private Point2D.Double _north;

    /** position of E label for compass */
    private Point2D.Double _east;

    /** Font to use for compass labels */
    private Font _compassFont;

    /** Set to true while dragging the pan rect to ignore change events from other sources */
    private boolean _ignoreStateChanges = false;


    /**
     * Constructor
     *
     * @param mainImageDisplay The target image display being controlled.
     * @param width The desired width of the pan window
     * @param height The desired height of the pan window
     */
    public ImagePanner(MainImageDisplay mainImageDisplay, int width, int height) {
        _imageDisplay = new ImageDisplay("pan window");
        _imageDisplay.setPrescaled(true);
        _imageDisplay.addImageGraphicsHandler(this);

        ImageProcessor ip = _imageDisplay.getImageProcessor();
        ip.removeChangeListener((ChangeListener) _imageDisplay); // not needed here
        ip.setName("Panner ip"); // for debugging

        JComponent c = (JComponent) _imageDisplay;
        _panWidth = width;
        _panHeight = height;
        c.setPreferredSize(new Dimension(_panWidth, _panHeight));
        c.addMouseListener(this);
        c.addMouseMotionListener(this);
        setLayout(new BorderLayout());
        add(c, BorderLayout.CENTER);

        setMainImageDisplay(mainImageDisplay);
    }


    /**
     * Constructor
     *
     * @param mainImageDisplay The target image display being controlled.
     */
    public ImagePanner(MainImageDisplay mainImageDisplay) {
        this(mainImageDisplay, DEFAULT_SIZE, DEFAULT_SIZE);
    }


    /**
     * Default Constructor (Must call setMainImageDisplay() later).
     */
    public ImagePanner() {
        this(null);
    }


    /**
     * Set the target ImageDisplay that we are controlling.
     */
    public void setMainImageDisplay(MainImageDisplay mainImageDisplay) {
        _mainImageDisplay = mainImageDisplay;

        // monitor resize events on the target image
        ((Component) _mainImageDisplay).addComponentListener(new ComponentAdapter() {

            public void componentResized(ComponentEvent e) {
                _updateRect();
            }
        });

        // register to receive notification when the image changes
        _mainImageDisplay.addChangeListener(this);
        _mainImageDisplay.getImageProcessor().addChangeListener(this);
    }


    /**
     * Called when the main image changes in some way. The change event
     * (ImageChangeEvent) describes what changed.
     */
    public void stateChanged(ChangeEvent ce) {
        if (_ignoreStateChanges)
            return;

        ImageChangeEvent e = (ImageChangeEvent) ce;
        if (e.getSource() == _mainImageDisplay) {
            if (e.isNewImage()) {
                if (e.isBefore()) {
                    _pannerImage = null;
                }
                else {
                    _updateImage();
                    _updateRect();
                    _updateCompass();
                }
            }
            else {
                _updateRect();
                _updateCompass();
            }
        }
        else {
            if (e.isImageDataChanged()) {
                _pannerImage = null;
            }
            _updateImage();
            if (e.isNewAngle()) {
                _updateCompass();
            }
        }
    }


    /**
     * Return a scaled down image to use for the pan window
     */
    private PlanarImage _getPannerImage() {
        PlanarImage im = _mainImageDisplay.getImageProcessor().getSourceImage();
        if (im == null)
            return null;

        FITSImage.setPreviewSize(_panWidth);

        // try to get a prescaled preview image (provided by the FITS codec)
        float scale = 1.0F;
        float w = im.getWidth(), h = im.getHeight();
        Object o = im.getProperty("#preview_image");
        if (o != null && o instanceof PlanarImage) {
            // The preview image is scaled down by an integer value, sich as 1/2x, 1/3x, ...
	    PlanarImage preview = (PlanarImage) o;

            Object fi = im.getProperty( "#fits_image" );
            if ( fi != null && fi instanceof FITSImage ) { // PWD: Check is
                                                           // FITSImage before casting.
                FITSImage fitsImage = (FITSImage)fi;
		w = fitsImage.getRealWidth();
		h = fitsImage.getRealHeight();
	    }
	    scale = (float) (Math.min(preview.getWidth()/w, preview.getHeight()/h));
	    _imageDisplay.setScale(scale);
	    return preview;
        }

	// otherwise scale the source image with JAI and return it
	// (XXX Problem: The JAI scale operator does not support all source data types...)
	scale = (float) (Math.min(_panWidth / w, _panHeight / h));
	if (scale >= 1) {
	    // don't enlarge pan image
	    scale = 1.0F;
	}
	_imageDisplay.setScale(scale);
	return ImageOps.scale(im, scale, scale, 0.0F, 0.0F, _imageDisplay.getInterpolation(), null);
    }


    /** called when the image has changed to update the display */
    private void _updateImage() {
        if (_mainImageDisplay == null) {
            return;
        }

        // try to save time by getting a prescaled thumbnail image
        if (_pannerImage == null) {
            _pannerImage = _getPannerImage();
        }

        ImageProcessor ip = _imageDisplay.getImageProcessor();
        ip.setSourceImage(_pannerImage, _mainImageDisplay.getImageProcessor());
        ip.update();

        _imageDisplay.updateImage();
    }


    /**
     * Update the rect object with the coordinates of the visible area
     * of the target image.
     */
    private void _updateRect() {
        if (!_imageDisplay.isInitialized() || !_mainImageDisplay.isInitialized()) {
            return;
        }

        _rect = _mainImageDisplay.getVisibleArea();
        Point2D.Double p = new Point2D.Double(_rect.getX(), _rect.getY());
        Point2D.Double d = new Point2D.Double(_rect.getWidth(), _rect.getHeight());
        CoordinateConverter cc = _imageDisplay.getCoordinateConverter();
        cc.userToScreenCoords(p, false);
        cc.userToScreenCoords(d, true);
        _rect.setRect(p.getX(), p.getY(), d.getX(), d.getY());
    }

    /**
     * Update the rect object with the coordinates of the visible area
     * of the target image.
     */
    private void _updateCompass() {
        WorldCoordinateConverter wcs = _mainImageDisplay.getWCS();
        if (wcs == null) {
            _compass = null;
            return;
        }
        _imageDisplay.setWCS(wcs);

        double wcsw = wcs.getWidthInDeg();
        double wcsh = wcs.getHeightInDeg();
        double sizeInDeg = Math.min(wcsw, wcsh) / 4.;
        double equinox = wcs.getEquinox();
        Point2D.Double center = wcs.getWCSCenter();
        _north = new Point2D.Double(center.x, center.y);
        _east = new Point2D.Double(center.x, center.y);

        // check if at north or south pole, since that is a special case
        if (90 - Math.abs(center.y) < wcsh) {
            // skip this if at the pole (for now)
            return;
        }

        // get end points of compass
        _east.x = center.x + sizeInDeg / Math.cos((center.y / 180.) * Math.PI);
        if (_east.x < 0.)
            _east.x = 360. + _east.x;

        _north.y = center.y + sizeInDeg;
        if (_north.y >= 90.)
            _north.y = 180. - _north.y;

        CoordinateConverter cc = _imageDisplay.getCoordinateConverter();
        cc.worldToScreenCoords(center, false);
        cc.worldToScreenCoords(_north, false);
        cc.worldToScreenCoords(_east, false);

        GeneralPath path = new GeneralPath();
        ShapeUtil.addArrowLine(path, center, _north);
        ShapeUtil.addArrowLine(path, center, _east);
        _compass = path;

        // factor for positioning N and E labels
        double f = 0.25;
        _east.x += (_east.x - center.x) * f;
        _east.y += (_east.y - center.y) * f + 5;
        _north.x += (_north.x - center.x) * f - 3;
        _north.y += (_north.y - center.y) * f;
    }


    /**
     * Called each time the image is repainted to draw a rectangle on
     * the image marking the visible area of the target image.
     */
    public void drawImageGraphics(BasicImageDisplay imageDisplay, Graphics2D g) {
        if (_compass != null) {
            // draw a compass showing WCS north and east
            g.setColor(Color.white); // XXX visibility: use XOR? black & white rects?
            g.draw(_compass);
            if (_compassFont == null) {
                _compassFont = g.getFont().deriveFont(Font.BOLD);
            }
            g.setFont(_compassFont);
            g.drawString("N", (float) _north.x, (float) _north.y);
            g.drawString("E", (float) _east.x, (float) _east.y);
        }

        if (_rect != null) {
            // draw a rect showing the visible area of the image
            g.setColor(Color.yellow); // XXX visibility: use XOR? black & white rects?
            g.draw(_rect);
        }
    }

    /**
     * Pan the image to the selected location.
     */
    private void pan() {
        Point2D.Double origin = new Point2D.Double(_rect.getX(), _rect.getY());
        _imageDisplay.getCoordinateConverter().screenToUserCoords(origin, false);
        _mainImageDisplay.getCoordinateConverter().userToCanvasCoords(origin, false);
        _mainImageDisplay.setOrigin(origin);
        _mainImageDisplay.updateImage();
    }


    /**
     * Invoked when a mouse button is pressed on the image
     */
    public void mousePressed(MouseEvent e) {
        _ignoreStateChanges = true;
        _mark = new Point2D.Double(e.getX(), e.getY());
        _offset = new Point2D.Double(0, 0);
    }


    /**
     * Invoked when a mouse button is pressed on the image and then
     * dragged.
     */
    public void mouseDragged(MouseEvent e) {
        Point2D.Double d = new Point2D.Double(e.getX() - (_mark.getX() + _offset.getX()),
                e.getY() - (_mark.getY() + _offset.getY()));
        _offset.setLocation(e.getX() - _mark.getX(), e.getY() - _mark.getY());

        if (d.getX() != 0. || d.getY() != 0.) {
            _rect.setRect(_rect.getX() + d.getX(),
                    _rect.getY() + d.getY(),
                    _rect.getWidth(),
                    _rect.getHeight());

            if (_mainImageDisplay.isImmediateMode()) {
                pan();
            }
            else {
                ((JComponent) _imageDisplay).repaint();
            }
        }
    }


    /**
     * Invoked when a mouse button is released
     */
    public void mouseReleased(MouseEvent e) {
        _ignoreStateChanges = false;
        if (!_mainImageDisplay.isImmediateMode()) {
            pan();
        }
    }


    /** These are not currently used */
    public void mouseMoved(MouseEvent e) {
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }
}


