/*
 * ESO Archive
 *
 * $Id: ImageDisplay.java,v 1.15 2002/08/20 09:57:58 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/03  Created
 * Peter W. Draper 2002/10/02  Made getPixelValue return double precision
 */

package jsky.image.gui;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;

import javax.media.jai.Interpolation;
import javax.media.jai.InterpolationNearest;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import jsky.coords.CoordinateConverter;
import jsky.coords.WorldCoordinateConverter;
import jsky.image.EmptyRenderedImage;
import jsky.image.ImageProcessor;
import jsky.image.operator.ImageOps;
import jsky.util.gui.BasicWindowMonitor;


/**
 * Implements a JAI (Java Advanced Imaging) based image
 * display window. Any JAI PlanarImage can be displayed. Grayscale
 * images may be displayed with false colors, depending on the
 * ImageProcessor options specified.  Coordinate conversion is
 * supported, although, since this class is not intended to be the
 * main image class, world coordinates are only supported if setWCS()
 * is called to set the world coordinates information.
 *
 * @see MainImageDisplay
 * @see ImageZoom
 * @see ImagePanner
 *
 * @version $Revision: 1.15 $
 * @author Allan Brighton
 */
public class ImageDisplay extends JComponent
        implements BasicImageDisplay, ChangeListener {

    /** Image to use when there is no image to display */
    private static final PlanarImage _EMPTY_IMAGE = PlanarImage.wrapRenderedImage(new EmptyRenderedImage());

    /** Object used to prepare image for display. */
    private ImageProcessor _imageProcessor;

    /** The image being displayed, after processing. */
    private PlanarImage _displayImage;

    /** Used to delay image loading until window is visible. */
    private PlanarImage _pendingImage;

    /** The image's SampleModel. */
    private SampleModel _sampleModel;

    /** The image's ColorModel or one we supply. */
    private ColorModel _colorModel;

    /** Object used to convert coordinates */
    private CoordinateConverter _coordinateConverter;

    /** Zoom factor */
    private float _scale = 1.0f;

    /** The actual Zoom factor (used when zooming a prescaled image to lie about the scale factor) */
    private float _actualScale = 1.0f;

    /** Set to true if the image has been prescaled (pan window, thumbnail image). */
    private boolean _prescaled = false;

    /** Optional rendering hint for the scale operation. */
    private RenderingHints _scaleHints;

    /** The image display origin in canvas coords */
    private Point2D.Double _origin = new Point2D.Double(0, 0);

    /** true if image was centered in the window */
    private boolean _centered = false;

    /** Set to true (default) to automatically center the image, if it is smaller than the window. */
    private boolean _autoCenterImage = true;

    /** type of _interpolation to use for scaling */
    private Interpolation _interpolation = new InterpolationNearest();

    /** The image's min X tile. */
    private int _minTileX;

    /** The image's max X tile. */
    private int _maxTileX;

    /** The image's min Y tile. */
    private int _minTileY;

    /** The image's max Y tile. */
    private int _maxTileY;

    /** The image's tile width. */
    private int _tileWidth;

    /** The image's tile height. */
    private int _tileHeight;

    /** The image's tile grid X offset. */
    private int _tileGridXOffset;

    /** The image's tile grid Y offset. */
    private int _tileGridYOffset;

    /** if true, update image immediately for scrolling and other operations, otherwise on button release */
    private boolean _immediateMode = false;

    /** Object used to convert between image and world coordinates, when supported */
    private WorldCoordinateConverter _wcs;

    /** list of event listeners (ImageGraphicsHandlers) */
    private EventListenerList _listenerList = new EventListenerList();


    /**
     * Construct an image display widget with the given name to display
     * the output of the given image processor.
     *
     * @param imageProcessor Object managing the image
     * @param name name to associate with this instance
     */
    public ImageDisplay(ImageProcessor imageProcessor, String name) {
        setName(name);
        setImageProcessor(imageProcessor);
        _imageProcessor.setName(name);
        _coordinateConverter = new ImageCoordinateConverter(this);

        setBackground(Color.black);
        setPreferredSize(new Dimension(255, 255));	// default size

        // handle resize events
        addComponentListener(new ComponentAdapter() {

            public void componentResized(ComponentEvent e) {
                if (_pendingImage != null) {
                    setImage(_pendingImage);
                    _pendingImage = null;
                }
                updateImage();
            }
        });
    }


    /**
     * Construct an image display widget with the given name.
     *
     * @param name name to associate with this instance
     */
    public ImageDisplay(String name) {
        this(new ImageProcessor(), name);
    }


    /**
     * Construct an image display widget.
     */
    public ImageDisplay() {
        this("Image Display");
    }


    /**
     * Set the image processor to use to get the image to display.
     */
    public void setImageProcessor(ImageProcessor imageProcessor) {
        _imageProcessor = imageProcessor;

        // register to receive notification when the image changes
        _imageProcessor.removeChangeListener(this);
        _imageProcessor.addChangeListener(this);
    }

    /** Called for changes in the image processor settings */
    public void stateChanged(ChangeEvent ce) {
        updateImage();
    }

    /** Return the image processor object. */
    public ImageProcessor getImageProcessor() {
        return _imageProcessor;
    }


    /** Return the source image (before processing). */
    public PlanarImage getImage() {
        return _imageProcessor.getSourceImage();
    }


    /** Return the image being displayed (after image processing). */
    public PlanarImage getDisplayImage() {
        return _displayImage;
    }

    /** Return the object used to convert coordinates. */
    public CoordinateConverter getCoordinateConverter() {
        return _coordinateConverter;
    }


    /**
     * Set the image to display.
     */
    public void setImage(PlanarImage im) {
        // Note: we need to get the visible area of the image, but can't
        // use getVisibleArea(), since the image hasn't been displayed yet.
        // We know that the image will be centered when first loaded, so use
        // that area.
        int cw = getWidth(), ch = getHeight();
        if (cw == 0) {
            // System.out.println("XXX " + getName() + ": ImageDisplay.setImage can't get display size yet, will check again later...");
            _pendingImage = im;
            return;
        }
        double dw = im.getWidth() * _scale, dh = im.getHeight() * _scale;
        double x = Math.max((dw - cw) / 2.0, 0.);
        double y = Math.max((dh - ch) / 2.0, 0.);
        Rectangle2D.Double region = new Rectangle2D.Double(x / _scale, y / _scale, cw / _scale, ch / _scale);
        //System.out.println("XXX " + getName() + ": ImageDisplay.setImage region = : " + region);

        _newImage(true);		// call once before loading new image
        _imageProcessor.setSourceImage(im, region);
        _imageProcessor.update();
        _newImage(false);	// call once after loading new image
    }


    /**
     * Return the width of the source image in pixels
     */
    public int getImageWidth() {
        PlanarImage image = getImage();
        if (image != null) {
            if (_prescaled) // return original image size before prescaling
                return (int) (image.getWidth() / _scale);
            return image.getWidth();
        }
        return 0;
    }


    /**
     * Return the height of the source image in pixels
     */
    public int getImageHeight() {
        PlanarImage image = getImage();
        if (image != null) {
            if (_prescaled) // return original image size before prescaling
                return (int) (image.getHeight() / _scale);
            return image.getHeight();
        }
        return 0;
    }

    /**
     * This method is called before and after loading a new image.
     * @param before If true, it is before loading the image, otherwise afterwards.
     */
    private void _newImage(boolean before) {
        if (before) {	       // before loading image
            _centered = true;
        }
    }

    /**
     * Clear the image display, freeing up any resources currently used by the image
     * so that they can be garbage collected.
     */
    public void clear() {
        setImage(_EMPTY_IMAGE);
    }

    /**
     * Return true if the image has been cleared.
     */
    public boolean isClear() {
        PlanarImage im = _imageProcessor.getSourceImage();
        return (im == _EMPTY_IMAGE);
    }

    /** called when the image has changed to update the display */
    public void updateImage() {
        _updateImage(_imageProcessor.getDisplayImage());
    }


    /**
     * This method updates the source image for this window, which is
     * scaled to the correct magnification before displaying.
     */
    private void _updateImage(PlanarImage im) {
        if (im == null) {
            return;
        }

        // center the image if it is smaller than the window
        _centerImage(im);

        // scale and translate the image as needed
        _displayImage = _scale(im);

        _sampleModel = _displayImage.getSampleModel();
        if (_sampleModel == null)
            return;

        // First check whether the opimage has already set a suitable ColorModel
        _colorModel = _displayImage.getColorModel();
        if (_colorModel == null) {
            // If not, then create one.
            _colorModel = PlanarImage.createColorModel(_sampleModel);
            if (_colorModel == null) {
                throw new IllegalArgumentException("no color model");
            }
        }
        _minTileX = _displayImage.getMinTileX();
        _maxTileX = _displayImage.getMinTileX() + _displayImage.getNumXTiles() - 1;
        _minTileY = _displayImage.getMinTileY();
        _maxTileY = _displayImage.getMinTileY() + _displayImage.getNumYTiles() - 1;
        _tileWidth = _displayImage.getTileWidth();
        _tileHeight = _displayImage.getTileHeight();
        _tileGridXOffset = _displayImage.getTileGridXOffset();
        _tileGridYOffset = _displayImage.getTileGridYOffset();

        repaint();
    }

    /**
     * Return true if the image supports world coordinates (has the necessary keywords in the
     * header).
     */
    public boolean isWCS() {
        return _wcs != null;
    }

    /**
     * Return the object used to convert between image and world coordinates,
     * or null if none was set.
     */
    public WorldCoordinateConverter getWCS() {
        return _wcs;
    }


    /**
     * Set the object used to convert between image and world coordinates.
     */
    public void setWCS(WorldCoordinateConverter wcs) {
        _wcs = wcs;
    }


    /** Set to true (default) to automatically center the image, if it is smaller than the window. */
    public void setAutoCenterImage(boolean b) {
        _autoCenterImage = b;
    }

    /** Return true if the image is automatically centered when it is smaller than the window. */
    public boolean isAutoCenterImage() {
        return _autoCenterImage;
    }


    /**
     * If a new image was just loaded, center the image in it's window,
     * otherwise only center it if it is smaller than the display window.
     */
    private void _centerImage(PlanarImage im) {
        if (!_autoCenterImage)
            return;

        float scale = _scale;
        if (_prescaled)
            scale = 1.0F;
        int cw = getWidth(), ch = getHeight();
        double dw = im.getWidth() * scale, dh = im.getHeight() * scale;
        double x = _origin.getX(), y = _origin.getY();
        boolean center = false;

        // not a new image, only center if image is smaller than window
        if (dw < cw || _centered) {
            x = (dw - cw) / 2.0;
            center = true;
        }

        if (dh < ch || _centered) {
            y = (dh - ch) / 2.0;
            center = true;
        }
        if (center) {
            //System.out.println("XXX " + getName() + ": centerImage");
            _origin.setLocation(x, y);
            _centered = true;
        }
    }


    private final int _XtoTileX(int x) {
        return (int) Math.floor((double) (x - _tileGridXOffset) / _tileWidth);
    }

    private final int _YtoTileY(int y) {
        return (int) Math.floor((double) (y - _tileGridYOffset) / _tileHeight);
    }

    private final int _TileXtoX(int tx) {
        return tx * _tileWidth + _tileGridXOffset;
    }

    private final int _TileYtoY(int ty) {
        return ty * _tileHeight + _tileGridYOffset;
    }

    // speed up math min and max by inlining
    private final int _maxInt(int a, int b) {
        return a > b ? a : b;
    }

    private final int _minInt(int a, int b) {
        return (a <= b) ? a : b;
    }


    /**
     * Paint the image onto a Graphics object.  The painting is
     * performed tile-by-tile, and includes a grey region covering the
     * unused portion of image tiles as well as the general
     * background.  At this point the image must be byte data.
     */
    public synchronized void paintComponent(Graphics g) {
        Graphics2D g2D = (Graphics2D) g;

        int componentWidth = getWidth();
        int componentHeight = getHeight();

        g2D.setComposite(AlphaComposite.Src);
        g2D.setColor(getBackground());
        g2D.fillRect(0, 0, componentWidth, componentHeight);

        // if _displayImage is null, it's just a component
        if (_displayImage == null || _sampleModel == null)
            return;

        // Get the clipping rectangle and translate it into image coordinates.
        Rectangle clipBounds = g2D.getClipBounds();
        if (clipBounds == null) {
            clipBounds = new Rectangle(0, 0, componentWidth, componentHeight);
        }

        // Determine the extent of the clipping region in tile coordinates.
        int txmin, txmax, tymin, tymax;
        int ti, tj;

        txmin = _XtoTileX(clipBounds.x);
        txmin = _maxInt(txmin, _minTileX);
        txmin = _minInt(txmin, _maxTileX);

        txmax = _XtoTileX(clipBounds.x + clipBounds.width - 1);
        txmax = _maxInt(txmax, _minTileX);
        txmax = _minInt(txmax, _maxTileX);

        tymin = _YtoTileY(clipBounds.y);
        tymin = _maxInt(tymin, _minTileY);
        tymin = _minInt(tymin, _maxTileY);

        tymax = _YtoTileY(clipBounds.y + clipBounds.height - 1);
        tymax = _maxInt(tymax, _minTileY);
        tymax = _minInt(tymax, _maxTileY);
        Insets insets = getInsets();

        long time = System.currentTimeMillis();
        int numTiles = (txmax - txmin) * (tymax - tymin);

        // Loop over tiles within the clipping region
        for (tj = tymin; tj <= tymax; tj++) {
            for (ti = txmin; ti <= txmax; ti++) {

                int tx = _TileXtoX(ti);
                int ty = _TileYtoY(tj);

                Raster tile = _displayImage.getTile(ti, tj);
                if (tile == null)
                    return;
                DataBuffer dataBuffer = tile.getDataBuffer();
                if (dataBuffer == null)
                    return;

		//System.out.println("XXX " + getName() + ": paint image tile (" + ti + ", " + tj + ")");

                WritableRaster wr = tile.createWritableRaster(_sampleModel, dataBuffer, null);
                BufferedImage bi = new BufferedImage(_colorModel, wr, _colorModel.isAlphaPremultiplied(), null);

                // correctly handles band offsets
                g2D.drawRenderedImage(bi, AffineTransform.getTranslateInstance(tx + insets.left, ty + insets.top));
            }
        }

        // derived classes may want to insert some graphics here
        _notifyGraphicsHandlers(g2D);
    }


    /**
     * Register as an image graphics handler.
     */
    public void addImageGraphicsHandler(ImageGraphicsHandler igh) {
        _listenerList.add(ImageGraphicsHandler.class, igh);
    }

    /**
     * Unregister as an image graphics handler.
     */
    public void removeImageGraphicsHandler(ImageGraphicsHandler igh) {
        _listenerList.remove(ImageGraphicsHandler.class, igh);
    }

    /**
     * Notify any ImageGraphicsHandlers
     */
    private void _notifyGraphicsHandlers(Graphics2D g) {
        Object[] listeners = _listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ImageGraphicsHandler.class) {
                ((ImageGraphicsHandler) listeners[i + 1]).drawImageGraphics(this, g);
            }
        }
    }


    /**
     * Set the origin of the image to display in canvas coordinates.
     */
    public void setOrigin(Point2D.Double origin) {
        _origin = origin;
        _centered = false;
    }


    /**
     * Return the origin of the displayed image in canvas coordinates.
     */
    public Point2D.Double getOrigin() {
        return new Point2D.Double(_origin.x, _origin.y);
    }

    /**
     * Return the image canvas component.
     */
    public JComponent getCanvas() {
        return this;
    }


    /**
     * Set the scale (zoom factor) for the image.
     * This also adjusts the origin so that the center of the image remains about the same.
     */
    public void setScale(float scale) {
        _coordinateConverter.canvasToUserCoords(_origin, false);
        _scale = _actualScale = scale;
        _coordinateConverter.userToCanvasCoords(_origin, false);
    }

    /**
     * Set the scale (zoom factor) for the image, but lie about it later when asked.
     * This method is needed when working with images that have been prescaled by the
     * image reader, so that the correct coordinates can still be displayed.
     *
     * @param actualScale this is the scale value that is actually applied to the image data
     * @param apparentScale this is the scale value that will be reported by getScale()
     */
    public void setScale(float actualScale, float apparentScale) {
        _coordinateConverter.canvasToUserCoords(_origin, false);
        _scale = apparentScale;
        _actualScale = actualScale;
        _coordinateConverter.userToCanvasCoords(_origin, false);
    }


    /**
     * Return the current scale (zoom factor) for the image.
     */
    public float getScale() {
        return _scale;
    }

    /**
     * Set the interpolation object used to scale the image (a subclass
     * of Interpolation, such as InterpolationNearest (default), or
     * InterpolationBilinear (better, but slower)).
     */
    public void setInterpolation(Interpolation i) {
        _interpolation = i;
    }


    /** Return the interpolation object used to scale the image */
    public Interpolation getInterpolation() {
        return _interpolation;
    }


    /**
     * Return the value of the pixel in the given band at the given user coordinates
     */
    public double getPixelValue(Point2D.Double p, int band) {
        PlanarImage im = _imageProcessor.getRescaledSourceImage();
        if (im != null) {
            int ix = (int) p.getX(), iy = (int) p.getY();
            if (ix < 0 || ix > im.getWidth() || iy < 0 || iy > im.getHeight())
                return 0.0;
            int x = _XtoTileX(ix);
            int y = _YtoTileY(iy);
            if (x < 0 || y < 0)
                return 0.0;
            Raster tile = im.getTile(x, y);
            return tile.getSampleDouble(ix, iy, band); // PWD: made double
        }
        return 0.0;
    }

    /**
     * Set to true if the image being displayed has been prescaled (such as
     * for a pan window or thumbnail image).
     * If true, the scale value will only be used to calculate coordinate
     * transformations, but the image will not actually be scaled.
     */
    public void setPrescaled(boolean b) {
        _prescaled = b;
    }

    /** Return true if the image has been prescaled */
    public boolean isPrescaled() {
        return _prescaled;
    }


    /** Set the optional rendering hints for the image scale operation */
    public void setScaleHints(RenderingHints hints) {
        _scaleHints = hints;
    }

    /** Return the optional rendering hints used for the image scale operation */
    public RenderingHints getScaleHints() {
        return _scaleHints;
    }


    /**
     * Perform a translate/scale operation on the given image using the current
     * settings and return the resulting image. The input image is assumed to be
     * at mag 1.
     */
    private PlanarImage _scale(PlanarImage im) {
        if (im != null) {
            float tx = (float) _origin.getX();
            float ty = (float) _origin.getY();

            if (_actualScale == 1 || _prescaled) {
                if (tx != 0 || ty != 0) {
                    im = ImageOps.translate(im, -tx, -ty, _interpolation);
                }
            }
            else {
                im = ImageOps.scale(im, _actualScale, _actualScale, -tx, -ty, _interpolation, _scaleHints);
            }
        }
        return im;
    }


    /**
     * Set the scaling factor so that the image will fit in a window of the
     * given size.
     * <p>
     * Note that only integer scaling factors are used, for example
     * 2, 1, 1/2, 1/3, etc., for performance reasons.
     */
    protected void scaleToFit(int width, int height) {
        float w = getImageWidth(), h = getImageHeight();
        if (w != 0. && h != 0.) {
	    float scale = (float)(Math.min(width/w, height/h));
	    if (scale >= 1.0F) 
		scale = (float)Math.round(scale);
	    else 
		scale = 1.0F/(float)Math.round(1.0F/scale);
            setScale(scale);
	}
    }


    /**
     * Set the scaling factor so that the image will fit in the current window.
     * <p>
     * Note that only integer scaling factors are used, for example
     * 2, 1, 1/2, 1/3, etc., for performance reasons.
     */
    public void scaleToFit() {
        int cw = getWidth(), ch = getHeight();
        if (cw != 0) {
	    scaleToFit(cw, ch);
	}
    }


    /**
     * Return a rectangle describing the visible area of the image
     * (in user coordinates).
     */
    public Rectangle2D.Double getVisibleArea() {
        Rectangle2D.Double r = new Rectangle2D.Double();
	Point2D.Double p = new Point2D.Double(0, 0);
	_coordinateConverter.screenToUserCoords(p, false);

	Point2D.Double size = new Point2D.Double(getWidth(), getHeight());
	_coordinateConverter.screenToUserCoords(size, true);

	r.setRect(p.getX(), p.getY(), size.getX(), size.getY());
        return r;
    }


    /** Return true if this widget has been initialized and is displaying an image. */
    public boolean isInitialized() {
        return _displayImage != null;
    }

    /**
     * Set to true if scrolling and other operations should update the image immediately,
     * otherwise only on button release.
     */
    public void setImmediateMode(boolean b) {
        _immediateMode = b;
    }


    /** Return true if immediate mode is turned on. */
    public boolean isImmediateMode() {
        return _immediateMode;
    }


    /**
     * test main: usage: java ImageDisplay <filename>.
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame("ImageDisplay");
        ImageDisplay display = new ImageDisplay();
        if (args.length > 0) {
            try {
                display.setImage(JAI.create("fileload", args[0]));
            }
            catch (Exception e) {
                System.out.println("error: " + e.toString());
                System.exit(1);
            }
        }
        frame.getContentPane().add(display, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
        frame.addWindowListener(new BasicWindowMonitor());
    }
}
