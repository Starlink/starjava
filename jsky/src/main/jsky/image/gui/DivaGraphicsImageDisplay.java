/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: DivaGraphicsImageDisplay.java,v 1.39 2002/08/20 15:44:23 brighton Exp $
 */

package jsky.image.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.IOException;

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
import jsky.coords.WCSTransform;
import jsky.coords.WorldCoordinateConverter;
import jsky.graphics.CanvasGraphics;
import jsky.image.EmptyRenderedImage;
import jsky.image.ImageChangeEvent;
import jsky.image.ImageProcessor;
import jsky.image.fits.FITSKeywordProvider;
import jsky.image.fits.codec.FITSImage;
import jsky.image.graphics.DivaImageGraphics;
import jsky.image.graphics.ImageLayer;
import jsky.image.operator.ImageOps;
import jsky.util.gui.BasicWindowMonitor;
import jsky.util.gui.DialogUtil;

import diva.canvas.GraphicsPane;
import diva.canvas.JCanvas;

/**
 * Implements a JAI based image display window with extra support for 2D
 * graphics, based on the Diva package. This class is derived from the Diva
 * JCanvas class. The image is painted on the background of the canvas while
 * graphics can be drawn on the foreground using methods preovided here.
 *<p>
 * Any JAI PlanarImage can be displayed. Grayscale images may be displayed with
 * false colors, depending on the ImageProcessor options specified.
 *<p>
 * This class also provides methods for converting image coordinates. See the
 * CoordinateConverter interface for details. World coordinates are supported
 * if the information is present in the header (for FITS files).
 *
 * @version $Revision: 1.39 $
 * @author Allan Brighton
 * @author Peter W. Draper
 */
public class DivaGraphicsImageDisplay extends JCanvas implements GraphicsImageDisplay {

    /** Object used to prepare image for display */
    private ImageProcessor _imageProcessor;

    /** The source image */
    private PlanarImage _displayImage;

    /** Used to delay image loading until window is visible */
    private PlanarImage _pendingImage;

    /** The image's SampleModel. */
    private SampleModel _sampleModel;

    /** The image's ColorModel or one we supply. */
    private ColorModel _colorModel;

    /** Object used to convert coordinates */
    private CoordinateConverter _coordinateConverter;

    /** Object used for drawing graphics over the image */
    private CanvasGraphics _canvasGraphics;

    /** The Diva graphics layer containing the image */
    private ImageLayer _imageLayer;

    /** Object used to convert between image and world coordinates, when supported */
    private WorldCoordinateConverter _wcs;

    /** Flag: if true, don't reinitialize the wcs info when a new image is loaded. */
    private boolean _noInitWCS = false;

    /** Transform used for graphics */
    private AffineTransform _affineTransform;

    /** zoom factor */
    private float _scale = 1.0f;

    /** Set to true if the image has been prescaled (pan window, thumbnail image). */
    private boolean _prescaled = false;

    /** Optional rendering hint for the scale operation. */
    private RenderingHints _scaleHints;

    /** The image display origin in canvas coords */
    private Point2D.Double _origin = new Point2D.Double(0, 0);

    /** Set to true (default) to automatically center the image, if it is smaller than the window. */
    private boolean _autoCenterImage = true;

    /** true if image was centered in the window */
    private boolean _centered = false;

    /** type of interpolation to use for scaling */
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

    /** Handle to FITSImage object, if image is a FITS image */
    private FITSImage _fitsImage;

    /** if true, update image immediately for scrolling and other operations, otherwise on button release */
    private boolean _immediateMode = false;

    /** if true, update the transformations for graphics objects */
    private boolean _updateGraphicsFlag = false;

    /** list of listeners for change events and image graphics handlers */
    private EventListenerList _listenerList = new EventListenerList();

    /** Saved image width for comparison */
    private int _savedImageWidth;

    /** Saved image height for comparison */
    private int _savedImageHeight;

    /** Saved image scale for comparison */
    private float _savedScale;


    /**
     * Construct an image display widget with the given graphics pane and name to display
     * the output of the given image processor.
     *
     * @param pane the Diva GraphicsPane to use (contains the layers used to display the
     *             image and graphics)
     * @param imageProcessor Object managing the image
     * @param name name to associate with this instance
     */
    public DivaGraphicsImageDisplay(GraphicsPane pane, ImageProcessor imageProcessor, String name) {
        super(pane);
        setName(name);
        setImageProcessor(imageProcessor);
        imageProcessor.setName(name);
        _coordinateConverter = new ImageCoordinateConverter(this);
        setBackground(Color.black);
        setPreferredSize(new Dimension(255, 255));	// default size
        _canvasGraphics = makeCanvasGraphics();

        // handle resize events
        addComponentListener(new ComponentAdapter() {

            public void componentResized(ComponentEvent e) {
                if (_pendingImage != null) {
                    setImage(_pendingImage);
                    _pendingImage = null;
                }
            }
        });
    }


    /**
     * Construct an image display widget with the given name to display
     * the output of the given image processor.
     *
     * @param imageProcessor Object managing the image
     * @param name name to associate with this instance
     */
    public DivaGraphicsImageDisplay(ImageProcessor imageProcessor, String name) {
        this(new GraphicsPane(), imageProcessor, name);
    }


    /**
     * Construct an image display widget with the given name.
     *
     * @param name name to associate with this instance
     */
    public DivaGraphicsImageDisplay(String name) {
        this(new ImageProcessor(), name);
    }


    /**
     * Construct an image display widget.
     */
    public DivaGraphicsImageDisplay() {
        this("Image Display");
    }


    /**
     * Make and return the CanvasGraphics object.
     */
    protected CanvasGraphics makeCanvasGraphics() {
        DivaImageGraphics g = new DivaImageGraphics(this);
        _imageLayer = g.getImageLayer();
        return g;
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

    /** Return an object to be used for drawing persistent graphics over the image */
    public CanvasGraphics getCanvasGraphics() {
        return _canvasGraphics;
    }

    /**
     * Set the image processor to use to get the image to display.
     */
    public void setImageProcessor(ImageProcessor imageProcessor) {
        _imageProcessor = imageProcessor;
        _wcs = null;

        // register to receive notification when the image changes
        imageProcessor.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent ce) {
                ImageChangeEvent e = (ImageChangeEvent) ce;
                if (e.isNewAngle())
                    _updateGraphicsFlag = true;
                updateImage();
            }
        });
    }


    /**
     * Return the image processor object.
     */
    public ImageProcessor getImageProcessor() {
        return _imageProcessor;
    }


    /**
     * Set the image to display.
     */
    public void setImage(PlanarImage im) {
        if (!_noInitWCS)
            _imageLayer.setVisible(true);

        // Note: we need to get the visible area of the image, but can't
        // use getVisibleArea(), since the image hasn't been displayed yet.
        // We know that the image will be centered when first loaded, so use
        // that area.
        int cw = getWidth(), ch = getHeight();
        if (cw == 0) {
	    // Can't get the component size, since it hasn't been displayed yet: 
	    // Put it off until later.
            _pendingImage = im;
            return;
        }
        double dw = im.getWidth() * _scale, dh = im.getHeight() * _scale;
        double x = Math.max((dw - cw) / 2.0, 0.);
        double y = Math.max((dh - ch) / 2.0, 0.);
        Rectangle2D.Double region = new Rectangle2D.Double(x / _scale, y / _scale, cw / _scale, ch / _scale);

        newImage(true);		// call once before loading new image
        _imageProcessor.setSourceImage(im, region);
        _imageProcessor.update();
        newImage(false);	// call once after loading new image
    }


    /**
     * Set the FITS image to display.
     */
    public void setImage(FITSImage fitsImage) {
	_fitsImage = fitsImage;

	float scale = _fitsImage.getScale();
	if (scale < 1.0F)
	    _scale = scale;
	_prescaled = (_fitsImage.getSubsample() != 1);

	setImage(PlanarImage.wrapRenderedImage(_fitsImage));
    }


    /**
     * Return the width of the source image in pixels
     */
    public int getImageWidth() {
	if (_fitsImage != null) 
	    return _fitsImage.getRealWidth();

        PlanarImage image = getImage();
        if (image != null) {
            return image.getWidth();
        }
        return 0;
    }


    /**
     * Return the height of the source image in pixels
     */
    public int getImageHeight() {
	if (_fitsImage != null) 
	    return _fitsImage.getRealHeight();

        PlanarImage image = getImage();
        if (image != null) {
            return image.getHeight();
        }
        return 0;
    }


    /**
     * If the current image is in FITS format, return the FITSImage object managing it,
     * otherwise return null. (The FITSImage object is available via the "#fits_image"
     * property from the FITS codec, which implements FITS support for JAI.)
     */
    public FITSImage getFitsImage() {
        return _fitsImage;
    }


    /**
     * This method is called before and after loading a new image.
     * @param before If true, it is before loading the image, otherwise afterwards.
     */
    protected void newImage(boolean before) {
        if (before) {	       // before loading image
            if (_fitsImage != null) {
                _fitsImage.clearTileCache();
	    }
            _fitsImage = null;
            _centered = true;
            if (!_noInitWCS)
                _wcs = null;
        }
        else {			// after loading image
	    _affineTransform = getAffineTransform();
            PlanarImage im = getImage();
            if (im != null) {
		// Check if it is a FITS image, and if so, get the FITSImage object,
		// which is needed to initialize WCS and check for FITS extensions.
                Object o = im.getProperty("#fits_image");
                if (o != null && (o instanceof FITSImage)) {
                    _fitsImage = (FITSImage) o;
                    // Check for WCS
                    if (!_noInitWCS)
                        initWCS();
                }
            }
            _updateGraphicsFlag = true;
        }
    }


    /**
     * Initialize the world coordinate system, if the image properties (keywords) support it
     */
    protected void initWCS() {
        // Currently WCS info is only supplied by FITS images
        if (_fitsImage == null)
            return;

        // only initialize once per image
        if (_wcs != null)
            return;

	try {
	    _wcs = new WCSTransform(new FITSKeywordProvider(_fitsImage));
	}
	catch (IllegalArgumentException e) {
	    _wcs = null;
	    return;
	}
	if (!_wcs.isWCS())
	    _wcs = null;
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


    /**
     * Clear the image display.
     */
    public void clear() {
        if (isWCS()) {
            Point2D.Double p = _wcs.getWCSCenter();
            blankImage(p.x, p.y);
        }
        else {
            blankImage(0.0, 0.0);
        }
    }

    /**
     * Return true if the image has been cleared.
     */
    public boolean isClear() {
        return _displayImage == null || _imageLayer.isVisible() == false;
    }


    /**
     * Display a blank image with the given center coordinates
     * (15' * 60 seconds/minutes).
     *
     * @param ra  RA center coordinate in deg J2000
     * @param dec Dec center coordinate in deg J2000
     */
    public void blankImage(double ra, double dec) {
        // 15' * 60 seconds/minute
        int w = 15 * 60;
        _wcs = new WCSTransform(ra, dec, 1.7, 1.7, w / 2., w / 2., w, w, 180., 2000, 2000., "FK5");
        _noInitWCS = true;
        try {
	    _scale = 1.0F;
            setImage(PlanarImage.wrapRenderedImage(new EmptyRenderedImage(w, w)));
	    //setScale(1.0F);
	    //updateImage();
            _imageLayer.setVisible(false);
        }
        catch (Exception e) {
            DialogUtil.error(e);
        }
        _noInitWCS = false;
    }


    /** Called when the image has changed to update the display */
    public synchronized void updateImage() {
        updateImage(_imageProcessor.getDisplayImage());
        if (_updateGraphicsFlag) {
            _updateGraphicsFlag = false;
            transformGraphics();
        }
        else {
            // just keep the affineTransform up to date, for transforming graphics later
            _affineTransform = getAffineTransform();
        }
    }

    /**
     * This method updates the source image for this window, which is
     * scaled to the correct magnification before displaying.
     */
    protected void updateImage(PlanarImage im) {
        if (im == null) {
            return;
        }

        // center the image if it is smaller than the window
        centerImage(im);

        // scale and translate the image as needed
        _displayImage = scale(im);

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
        //System.out.println("XXX _tileGridXOffset = " + _tileGridXOffset + ", _tileGridYOffset = " + _tileGridYOffset);
        //System.out.println("XXX _tileWidth = " + _tileWidth + ", _tileHeight = " + _tileHeight);

        repaint();
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
    protected void centerImage(PlanarImage im) {
        if (!_autoCenterImage)
            return;
        float scale = _scale;
        if (_prescaled)
            scale = 1.0F;

        int w = im.getWidth();
        int h = im.getHeight();
        if (w == _savedImageWidth && h == _savedImageHeight && scale == _savedScale)
            return; // don't center if this image is same size as previous one
        _savedImageWidth = w;
        _savedImageHeight = h;
        _savedScale = scale;

        double dw = w * scale;
        double dh = h * scale;

        int cw = getWidth(), ch = getHeight();
        double x = _origin.x, y = _origin.y;
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
            _origin.x = x;
            _origin.y = y;
            _centered = true;
        }
    }


    /** Return the X tile index for the given X screen coordinate */
    private final int XtoTileX(int x) {
        return (int) Math.floor((double) (x - _tileGridXOffset) / _tileWidth);
    }

    /** Return the Y tile index for the given Y screen coordinate */
    private final int YtoTileY(int y) {
        return (int) Math.floor((double) (y - _tileGridYOffset) / _tileHeight);
    }

    /** Return the X screen coordinate for the given X tile index  */
    private final int TileXtoX(int tx) {
        return tx * _tileWidth + _tileGridXOffset;
    }

    /** Return the Y screen coordinate for the given Y tile index  */
    private final int TileYtoY(int ty) {
        return ty * _tileHeight + _tileGridYOffset;
    }

    // speed up math min and max by inlining
    private final int maxInt(int a, int b) {
        return a > b ? a : b;
    }

    private final int minInt(int a, int b) {
        return (a <= b) ? a : b;
    }

    /**
     * Paint the image onto a Graphics object.  The painting is
     * performed tile-by-tile, and includes a grey region covering the
     * unused portion of image tiles as well as the general
     * background.  At this point the image must be byte data.
     *
     * @param g2D the graphics context
     * @param region if not null, the region to paint
     */
    public synchronized void paintLayer(Graphics2D g2D, Rectangle2D region) {
        int componentWidth = getWidth();
        int componentHeight = getHeight();

        g2D.setColor(getBackground());
        g2D.fillRect(0, 0, componentWidth, componentHeight);

        // if _displayImage is null, it's just a component
        if (_displayImage == null || _sampleModel == null || !_imageLayer.isVisible())
            return;

        // Get the clipping rectangle and translate it into image coordinates.
        Rectangle2D clipBounds = g2D.getClipBounds();
        if (region == null) {
            region = clipBounds;
        }
        else if (clipBounds != null) {
            region = region.createUnion(clipBounds);
        }
        int x, y, w, h;
        if (region != null) {
            x = (int) region.getX();
            y = (int) region.getY();
            w = (int) region.getWidth();
            h = (int) region.getHeight();
        }
        else {
            x = 0;
            y = 0;
            w = componentWidth;
            h = componentHeight;
        }

        // Determine the extent of the clipping region in tile coordinates.
        int txmin, txmax, tymin, tymax;
        int ti, tj;

        txmin = XtoTileX(x);
        txmin = maxInt(txmin, _minTileX);
        txmin = minInt(txmin, _maxTileX);

        txmax = XtoTileX(x + w - 1);
        txmax = maxInt(txmax, _minTileX);
        txmax = minInt(txmax, _maxTileX);

        tymin = YtoTileY(y);
        tymin = maxInt(tymin, _minTileY);
        tymin = minInt(tymin, _maxTileY);

        tymax = YtoTileY(y + h - 1);
        tymax = maxInt(tymax, _minTileY);
        tymax = minInt(tymax, _maxTileY);
        Insets insets = getInsets();

        long time = System.currentTimeMillis();
        int numTiles = (txmax - txmin) * (tymax - tymin);

        // Loop over tiles within the clipping region
        for (tj = tymin; tj <= tymax; tj++) {
            for (ti = txmin; ti <= txmax; ti++) {
                int tx = TileXtoX(ti);
                int ty = TileYtoY(tj);

                Raster tile = _displayImage.getTile(ti, tj);
                if (tile == null)
                    return;
                DataBuffer dataBuffer = tile.getDataBuffer();
                if (dataBuffer == null)
                    return;
		
                WritableRaster wr = tile.createWritableRaster(_sampleModel, dataBuffer, null);
                BufferedImage bi = new BufferedImage(_colorModel, wr, _colorModel.isAlphaPremultiplied(), null);

                // correctly handles band offsets
                g2D.drawRenderedImage(bi, AffineTransform.getTranslateInstance(tx + insets.left, ty + insets.top));
            }
        }
        notifyGraphicsHandlers(g2D);
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
    protected void notifyGraphicsHandlers(Graphics2D g) {
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
        _updateGraphicsFlag = true;
    }


    /**
     * Return the origin of the displayed image in canvas coordinates.
     */
    public Point2D.Double getOrigin() {
        return _origin;
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
	boolean needsUpdate = false;

	// optimize zooming out for FITS image files
	if (_fitsImage != null) {
	    try {
		needsUpdate = _fitsImage.setScale(scale);
		_prescaled = (_fitsImage.getSubsample() != 1);
	    }
	    catch(IOException e) {
		DialogUtil.error(e);
	    }
	}
	if (!needsUpdate && scale == _scale)
	    return;

	// keep the same center point when zooming in or out
        int cw = getWidth(), ch = getHeight();
	Point2D.Double p = new Point2D.Double(_origin.x + cw/2., _origin.y + ch/2.);
	_coordinateConverter.canvasToUserCoords(p, false);
	_scale = scale;
	_coordinateConverter.userToCanvasCoords(p, false);
	_origin.x = p.x - cw/2.;
	_origin.y = p.y - ch/2.;
	_updateGraphicsFlag = true;

	if (needsUpdate) {
	    _imageProcessor.setSourceImage(PlanarImage.wrapRenderedImage(_fitsImage), _imageProcessor);
	    _imageProcessor.update();
	}
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
     *
     * @param p the user coordinates of the point to get
     * @param band the bad of the image (0 for FITS files)
     */
    public double getPixelValue(Point2D.Double p, int band) {
        // get the "rescaled" image, so the values displayed are after applying
        // BSCALE and BZERO, if applicable
        PlanarImage im = _imageProcessor.getRescaledSourceImage();
        if (im != null) {
            return _getPixelValue(im, (int) p.getX(), (int) p.getY(),
				  im.getWidth(), im.getHeight(),
				  band);
	}
        return 0.0;
    }
    // PWD: made return double


    /**
     * Return the value of the pixel in the given band at the given user coordinates
     *
     * @param im the (rescaled) image to get the value from
     * @param ix the X user coordinate of the point to get
     * @param iy the Y user coordinate of the point to get
     * @param w  the width of the image in user coordinat pixels
     * @param h  the height of the image in user coordinat pixels
     * @param band the bad of the image (0 for FITS files)
     */
   protected double _getPixelValue(PlanarImage im, int ix, int iy, int w, int h, int band) {
	// Handle FITS I/O optimizations
	int subsample = 1;
	if (_fitsImage != null) {
	    subsample = _fitsImage.getSubsample();
            ix = ix / subsample;
            iy = iy / subsample;
	}

        if (! _imageProcessor.isInvertedYAxis())
            iy = h - 1 - iy;

        if (ix < 0 || ix >= w || iy < 0 || iy >= h) {
            return 0.0;
        }

        // XXX don't use XtoTileX(ix) here, since XtoTileX refers to the display image,
        // while we are referencing the rescaled image here (source image after applying
        // BSCALE and BZERO, for FITS files)
        int x = (int) ((double) ix / _tileWidth);
        int y = (int) ((double) iy / _tileHeight);
        if (x < 0 || y < 0) {
            return 0.0;
        }
	
        Raster tile = im.getTile(x, y);
	if (tile != null) {
	    try {
		return tile.getSampleDouble(ix, iy, band);
	    }
	    catch(Exception e) {
	    }
	}
	return 0.0;
    }
    // PWD: made return double and protected for sub-classing. 
    // Modified so that sub-sampling scale is applied 
    // immediately for zoom out.


    /**
     * Return an array containing the values of the pixels in the given band in the given
     * user coordinates region. The pixel values are converted to float. Any pixels outside
     * the image bounds are set to the mean image value, if known, otherwise 0.
     *
     * @param region describes the region of the image to get in user coordinates
     * @param band the band of the image to get
     */
    public double[] getPixelValues(Rectangle region, int band) {
        // get the "rescaled" image, so the values displayed are after applying
        // BSCALE and BZERO, if applicable
        PlanarImage im = _imageProcessor.getRescaledSourceImage();
        if (im != null) {
            double[] ar = new double[region.width * region.height];
            int w = im.getWidth(), h = im.getHeight();
            int n = 0;
            int minX = region.x,
		minY = region.y,
		maxX = minX + region.width - 1,
		maxY = minY + region.height - 1;

	    for (int j = maxY; j >= minY; j--) {
		for (int i = minX; i <= maxX; i++) {
		    ar[n++] = _getPixelValue(im, i, j, w, h, band);
		}
	    }
            return ar;
        }
        return null;
    }
    // PWD: made return double[]

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
    protected PlanarImage scale(PlanarImage im) {
        if (im != null) {
            float tx = (float) _origin.x;
            float ty = (float) _origin.y;

            if (_scale == 1 || _prescaled) {
                if (tx != 0 || ty != 0) {
                    im = ImageOps.translate(im, -tx, -ty, _interpolation);
                }
            }
            else {
                im = ImageOps.scale(im, _scale, _scale, -tx, -ty, _interpolation, _scaleHints);
            }
        }
        return im;
    }


    /**
     * Transform the graphics in the foreground layer according to the current
     * image transformations.
     */
    protected void transformGraphics() {
        PlanarImage im = _imageProcessor.getSourceImage();
        if (im == null)
            return;

        // need to first reverse previous transform, so that we can move the
        // graphics objects from where they are now...
        AffineTransform trans;
        try {
            trans = _affineTransform.createInverse();
        }
        catch (NoninvertibleTransformException e) {
            System.out.println("warning: " + ": " + e.toString());
            return;
        }

        _affineTransform = getAffineTransform();

        if (!_affineTransform.isIdentity()) {
            trans.preConcatenate(_affineTransform);
        }

        if (!trans.isIdentity()) {
            transformGraphics(trans);
        }
    }


    /**
     * Transform the image graphics using the given AffineTransform.
     */
    protected void transformGraphics(AffineTransform trans) {
        _canvasGraphics.transform(trans);
    }


    /**
     * Return an AffineTransform based on the current transformations.
     */
    protected AffineTransform getAffineTransform() {
        // Flip, Rotate, translate and scale, in the same order done for the image.
        // Note: The difference here is that we are dealing with screen coords, whereas
        // in JAI you are only dealing with user coords.
        AffineTransform trans = new AffineTransform();

        // check the flip X/Y settings
        if (_imageProcessor.getFlipX()) {
            trans.concatenate(new AffineTransform(-1., 0., 0., 1., (double) getWidth(), 0.));
        }
        if (_imageProcessor.getFlipY()) {
            trans.concatenate(new AffineTransform(1., 0., 0., -1., 0., (double) getHeight()));
        }

        // rotate about the image center
        double angle = _imageProcessor.getAngle();
        if (angle != 0.0) {
            Point2D.Double center = new Point2D.Double(getImageWidth() / 2.0, getImageHeight() / 2.0);
            _coordinateConverter.userToScreenCoords(center, false);
            trans.rotate(angle, center.x, center.y);
        }

        // translate to image origin
        if (_origin.x != 0. || _origin.y != 0.) {
            trans.translate(-_origin.x, -_origin.y);
        }

        // scale
        if (_scale != 1.0F) {
            trans.scale(_scale, _scale);
        }

        return trans;
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
     * test main: usage: java DivaGraphicsImageDisplay <filename>.
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame("DivaGraphicsImageDisplay");
        DivaGraphicsImageDisplay display = new DivaGraphicsImageDisplay();
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
