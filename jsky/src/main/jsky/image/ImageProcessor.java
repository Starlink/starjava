/*
 * ESO Archive
 *
 * $Id: ImageProcessor.java,v 1.18 2002/08/16 22:21:13 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/11/17  Created
 * Peter W. Draper 2002/10/01  Commented out use of extrema ImageOp
 *                             as it returns NaN when NaNs are present
 *                             Changed _blank to a double as part of
 *                             support for double precision.
 *                             Added setBlank() to support sub-classing.
 */

package jsky.image;

import com.sun.media.jai.codec.ImageCodec;

import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;

import javax.media.jai.Histogram;
import javax.media.jai.Interpolation;
import javax.media.jai.InterpolationNearest;
import javax.media.jai.JAI;
import javax.media.jai.LookupTableJAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.operator.TransposeDescriptor;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import jsky.image.ImageColormap;
import jsky.image.ImageLookup;
import jsky.image.fits.codec.FITSCodec;
import jsky.image.fits.codec.FITSImage;
import jsky.image.operator.CutLevelDescriptor;
import jsky.image.operator.ImageOps;
import jsky.image.operator.MinMaxDescriptor;
import jsky.util.gui.DialogUtil;

/**
 * Responsible for processing images before they are
 * displayed, including setting cut levels and manipulating colors
 * and lookup tables.
 *
 * @version $Revision: 1.18 $
 * @author Allan Brighton
 */
public class ImageProcessor {

    /** The default number of X pixels to skip for histograms and statistics */
    public static final int DEFAULT_X_PERIOD = 4;

    /** The default number of Y pixels to skip for histograms and statistics */
    public static final int DEFAULT_Y_PERIOD = 4;

    // The original source image 
    private PlanarImage _sourceImage;

    // The original source image after applying BZERO and BSCALE, if needed 
    private PlanarImage _rescaledSourceImage;

    // For grayscale images: The source image, scaled to short range (if needed),
    // so that a cut levels lookup table may be applied 
    private PlanarImage _shortImage;

    // For grayscale images: The image, scaled to byte range (if needed),
    // so that a color lookup table may be applied 
    private PlanarImage _byteImage;

    // For grayscale images: The byte image, after applying a colormap,
    // otherwise the source image 
    private PlanarImage _colorImage;

    // The color image, after applying any necessary transformations
    // (scale, rotate, translate, ...) 
    private PlanarImage _displayImage;

    // If set, this is the region of interest in the image (normally the currently visible area) 
    private ROI _roi = null;

    // If set, this is the region of interest in the image (normally the currently visible area) 
    private Rectangle2D.Double _region;

    // Used to get a histogram of the image data in the visible area. 
    private ImageHistogram _imageHistogram = new ImageHistogram();

    // The number of bands in the source image 
    private int _numBands;

    // The object managing the image colormap 
    private ImageColormap _colormap = new ImageColormap();

    // min and max pixel values 
    private double _minValue = 0., _maxValue = 0.;

    // low and high image cut levels 
    private double _lowCut = 0., _highCut = 0.;

    // The number of X pixels to skip for histograms and statistics 
    private int _xPeriod = DEFAULT_X_PERIOD;

    // The number of Y pixels to skip for histograms and statistics 
    private int _yPeriod = DEFAULT_Y_PERIOD;

    // list of listeners for change events 
    private EventListenerList _listenerList = new EventListenerList();

    // Generic event fired whenever the image is updated 
    private ImageChangeEvent _imageChangeEvent = new ImageChangeEvent(this);

    // True if the user has set the cut levels by hand (then we don't change them) 
    private boolean _userSetCutLevels = false;

    // Rotation angle in radians 
    private double _angle = 0.0;

    // if true, flip the X axis 
    private boolean _flipX = false;

    // if true, flip the Y axis 
    private boolean _flipY = false;

    // if true, reverse the meaning of flipY (for FITS images that were not flipped while reading) 
    protected boolean _reverseY = false; // PWD: changed to protected
                                         // so that sub-classes can
                                         // set without the side
                                         // effects (bad early update
                                         // of graphics).

    // Set to true if the Y axis of the (FITS) image was inverted already while reading. 
    private boolean _invertedYAxis = false;

    // type of interpolation to use for rotating 
    private Interpolation _interpolation = new InterpolationNearest();

    // Used to reduce image data to byte range 
    private LookupTableJAI _scaleLookupTable;

    // Algorithm used to scale image to byte range 
    private int _scaleAlgorithm = ImageLookup.LINEAR_SCALE;

    // A name for this object (for diagnostic purposes) 
    private String _name = "";

    // Value for bad pixels 
    private double _blank = Double.NaN;

    // Value of the DATAMIN property, if defined 
    private double _dataMin = 0.;

    // Value of the DATAMAX property, if defined 
    private double _dataMax = 0.;

    // Value of the DATAMEAN property, if defined 
    private double _dataMean = 0.;

    // Value of the BZERO property, if defined 
    private double _bzero = 0.;

    // Value of the BSCALE property, if defined 
    private double _bscale = 1.;

    // Set to true if something was changed and a call to update() is needed. 
    private boolean _updatePending = false;


    // static initializer
    static {
        // make sure we have the correct JAI version
        String expectedJAIVersion = "jai-1_1";
        try {
            // this method is only available starting with jai-1_1 - allow newer versions
            String jaiVersion = JAI.getDefaultInstance().getBuildVersion();
        }
        catch (NoSuchMethodError e) {
            DialogUtil.error("Error: Incompatible JAI (Java Advanced Imaging) version. Expected "
                    + expectedJAIVersion);
            System.exit(1);
        }

        // add FITS support
        ImageCodec.registerCodec(new FITSCodec());

        // used in some cases to get the min and max pixel values
        MinMaxDescriptor.register();

        // used to calculate the image cut levels
        CutLevelDescriptor.register();
    }


    /**
     * Default constructor.
     *
     * Call setSourceImage(PlanarImage, Rectangle2D.Double) to set the image to process
     * and the region of interest.
     */
    public ImageProcessor() {
    }


    /**
     * Constructor.
     *
     * @param sourceImage The source image to process.
     * @param region the region of interest in the image (usually the visible area)
     *               in the coordinates of the source image (values will be clipped).
     */
    public ImageProcessor(PlanarImage sourceImage, Rectangle2D.Double region) {
        setSourceImage(sourceImage, region);
    }


    /** Returns the current source image. */
    public PlanarImage getSourceImage() {
        return _sourceImage;
    }


    /** Returns the current source image, after applying BZERO and BSCALE, if needed. */
    public PlanarImage getRescaledSourceImage() {
        return _rescaledSourceImage;
    }


    /**
     * Set the source image and copy all of the image processing settings from the
     * given ImageProcessor object. This method may be used, for example, for a pan window
     * that displays a separate, prescaled image, that should have the same
     * image processing settings as the main image.
     */
    public void setSourceImage(PlanarImage sourceImage, ImageProcessor imageProcessor) {
        this._sourceImage = sourceImage;
        _updatePending = true;

        if (_sourceImage == null) {
            _displayImage = _rescaledSourceImage = _sourceImage;
            return;
        }

        SampleModel sampleModel = _sourceImage.getSampleModel();
        if (sampleModel == null) {
            return;
        }
        _numBands = sampleModel.getNumBands();

        // copy the settings from the master image processor
	if (imageProcessor != this)
	    copySettings(imageProcessor);

        // apply bscale and bzero if needed
        _rescaledSourceImage = rescaleImage(_sourceImage);

        // get image into short range
        ImageLookup imageLookup = new ImageLookup();
        _shortImage = imageLookup.scaleToShortRange(_rescaledSourceImage, _lowCut, _highCut);
    }


    /**
     * Set the source image and the region of interest and perform any requested
     * image processing to make the display image.
     *
     * @param region the region of interest in the image (usually the visible area)
     *               in the coordinates of the source image.
     */
    public void setSourceImage(PlanarImage sourceImage, Rectangle2D.Double region) {
        this._sourceImage = sourceImage;
        _updatePending = true;

        if (_sourceImage == null) {
            _displayImage = _rescaledSourceImage = _sourceImage;
            return;
        }

        SampleModel sampleModel = _sourceImage.getSampleModel();
        if (sampleModel == null) {
            return;
        }

        _numBands = sampleModel.getNumBands();
        _minValue = _maxValue = 0.;

        // if it is a FITS file, reverse the Y axis
        // (XXX should be done in FITS codec? But transpose is accelerated in JAI)
        Object o = _sourceImage.getProperty("#fits_image");

        _reverseY = false;
        if (o instanceof FITSImage) {
            FITSImage fitsImage = (FITSImage) o;
            _invertedYAxis = fitsImage.isYFlipped();
            _reverseY = ! _invertedYAxis;

            // check for BZERO and BSCALE keywords and, if needed, apply the values
            _bzero = fitsImage.getKeywordValue("BZERO", 0.);
            _bscale = fitsImage.getKeywordValue("BSCALE", 1.);
            _rescaledSourceImage = rescaleImage(_sourceImage);

            // check for grayscale images...
            if (_numBands == 1) {
                // get value of blank or bad pixels
                _blank = fitsImage.getKeywordValue("BLANK", Double.NaN);
                if (Double.isNaN(_blank))
                    _blank = fitsImage.getKeywordValue("BADPIXEL", Double.NaN);
                if (!Double.isNaN(_blank)) {
                    // assume blank value needs to be rescaled in the same way as the image
                    // (the resulting image is float data, so make sure the "blank" value is treated the same)
                    _blank = _blank * _bscale + _bzero;
                }

                // min/max pixel values, if specified in image properties
                _dataMin = fitsImage.getKeywordValue("DATAMIN", 0.);
                _dataMax = fitsImage.getKeywordValue("DATAMAX", 0.);
                _dataMean = fitsImage.getKeywordValue("DATAMEAN", Double.NaN);
            }
        }
        else {
            _bzero = 0.;
            _bscale = 1.;
            _blank = Double.NaN;
            _dataMin = 0.;
            _dataMax = 0.;
            _dataMean = 0.;
            _rescaledSourceImage = _sourceImage;
        }

        // check for grayscale images...
        if (_numBands == 1) {
            // set the image cut levels, preserving user's choice if needed
            if (!_userSetCutLevels) {
                autoSetCutLevels(region);
            }
            else {
                setRegionOfInterest(region);
                setCutLevels(_lowCut, _highCut, true);
            }
        }
    }


    /** Returns the current display image. */
    public PlanarImage getDisplayImage() {
        return _displayImage;
    }

    /**
     * Process the source image, adding a lookup table and setting appropriate
     * cut levels where needed to improve visibility.
     */
    public void update() {
        if (!_updatePending)
            return;
        _updatePending = false;

        if (_sourceImage == null) {
            _displayImage = _rescaledSourceImage = _sourceImage;
            // notify listeners of changes in the image
            fireChange(_imageChangeEvent);
            return;
        }

        if (_rescaledSourceImage == null) {
            return;
        }

        try {
            if (_numBands == 1) {
                if (_shortImage == null || _scaleLookupTable == null) {
                    return;
                }
                // perform a lookup operation to scale short to byte range.
                // (_shortImage should have been already prepared when cut levels were set)
                _byteImage = ImageOps.lookup(_shortImage, _scaleLookupTable);

                // add a color RGB lookup table so we can manipulate the image colors
                _colorImage = ImageOps.lookup(_byteImage, _colormap.getColorLookupTable());
            }
            else {
                // source image is already color
                _colorImage = _rescaledSourceImage;
            }

            // apply flipX, flipY, if needed
            _displayImage = setTrans(_colorImage);

            // apply rotation, if needed
            _displayImage = rotate(_displayImage);
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }

	//System.out.println("XXX ImageProcessor: " + _name + ": updateImage()");
	
        // notify listeners of changes in the image
        fireChange(_imageChangeEvent);
    }

    /**
     * Rescale the given image, if needed, using the given factor and offset and return the result.
     * The resulting image is converted to float, to avoid loosing data.
     */
    protected PlanarImage rescaleImage(PlanarImage im) {
        if (_bscale != 1. || _bzero != 0.) {
            RenderingHints hints = ImageUtil.getSampleModelHint(im.getTileWidth(),
                    im.getTileHeight(),
                    DataBuffer.TYPE_FLOAT);
            return ImageOps.rescale(im, _bscale, _bzero, hints);
        }
        return im;
    }


    /**
     * Perform a transpose operation on the given image using the current
     * rotate, flipX and flipY settings and return the resulting image.
     */
    protected PlanarImage setTrans(PlanarImage im) {
        if (_flipX)
            im = ImageOps.transpose(im, TransposeDescriptor.FLIP_HORIZONTAL);
        if (_flipY != _reverseY) {
            im = ImageOps.transpose(im, TransposeDescriptor.FLIP_VERTICAL);
	}
        return im;
    }


    /**
     * Rotate the image about the center by the specified angle (in radians).
     */
    protected PlanarImage rotate(PlanarImage im) {
        if (_angle != 0.0F) {
            double x = _sourceImage.getWidth() / 2.0;
            double y = _sourceImage.getHeight() / 2.0;
            im = ImageOps.rotate(im, (float) x, (float) y, (float) _angle, _interpolation, null);
        }
        return im;
    }

    /**
     * Set the current region of interest in the image (normally the
     * currently visible area). This is used by operations that need
     * to gather statistics on the image to avoid having to scan the
     * entire image.
     *
     * @param region the region of interest in the image
     */
    protected void setRegionOfInterest(Rectangle2D.Double region) {
        if (_roi != null && this._region != null && region.equals(this._region))
            return;
        this._region = region;

        // clip to image boundary to form the region of interest
        _region = new Rectangle2D.Double((int) _region.getX(), (int) _region.getY(),
                (int) _region.getWidth(), (int) _region.getHeight());
        _region = (Rectangle2D.Double) _region.createIntersection(new Rectangle2D.Double(_xPeriod, _yPeriod,
                _sourceImage.getWidth() - _xPeriod,
                _sourceImage.getHeight() - _yPeriod));
        _roi = new ROIShape(_region);
    }

    /**
     * Examine the given region of the source image to determine the min
     * and max pixel values as well as the default cut levels (using median filter).
     * As a result, the minValue, maxValue, lowCut, and highCut  member
     * variables are set.
     * This method should be called before calling getMinValue(), getMaxValue(),
     * getLowCut() or getHighCut(). If the same region is specified as in the
     * previous call, nothing is done.
     *
     * @param region the region of interest in the image
     */
    protected void calculateImageStatistics(Rectangle2D.Double region) {
        // clip to image boundary to form the region of interest
        setRegionOfInterest(region);

        // Get the min and max pixel values in the region (or for the image, if known)
        if (_dataMin != _dataMax) {
            // Use property values, if set
            _minValue = _dataMin;
            _maxValue = _dataMax;
        }
// PWD: commented out as extrema doesn't seem to understand NaNs.
//         else if (Float.isNaN(_blank)) {
//             // No blank pixel was defined (or it is NaN), use extrema operation, which should be faster, since
//             // it is implemented in native code.
//             try {
//                 // the extrema op seems to have problems with very small images? (like dummy, blank images?)
//                 double[][] extrema = ImageOps.extrema(_rescaledSourceImage, _roi, _xPeriod, _yPeriod);
//                 _minValue = extrema[0][0];
//                 _maxValue = extrema[1][0];
//             }
//             catch (Exception e) {
//                 _minValue = _maxValue = 0.;
//             }
//             System.out.println( "minValue = " + _minValue );
//             System.out.println( "maxValue = " + _maxValue );
//         }
        else {
            // A blank pixel was defined, use our MinMax operator
            double[] minMax = ImageOps.minMax(_rescaledSourceImage, _roi, _xPeriod, _yPeriod, _blank);
            _minValue = minMax[0];
            _maxValue = minMax[1];
        }

        if (_minValue > _maxValue) {
            throw new IllegalArgumentException("min value > max value.");
        }

        // Try to guess the best cut levels for the image
        if (Double.isNaN(_dataMean)) {
            _dataMean = ImageOps.mean(_rescaledSourceImage, _roi, _xPeriod, _yPeriod)[0];
        }
        double[] cutLevels = ImageOps.cutLevel(_rescaledSourceImage, _roi, _blank, _dataMean);
        _lowCut = cutLevels[0];
        _highCut = cutLevels[1];
    }


    /**
     * Copy the settings from the given ImageProcessor to this one.
     */
    public void copySettings(ImageProcessor ip) {
        // Note: this method tends to be called more often than needed, due to
        // change events being fired and propagated. Try to avoid unnecessary
        // calls to update() by checking if any of the publicly accessible settings
        // have changed.
        if (_lowCut != ip._lowCut
                || _highCut != ip._highCut
                || (!_colormap.equals(ip._colormap))
                || _scaleAlgorithm != ip._scaleAlgorithm
                || _flipX != ip._flipX
                || _flipY != ip._flipY
                || _reverseY != ip._reverseY
                || _invertedYAxis != ip._invertedYAxis
                || _angle != ip._angle) {

            _updatePending = true;

            _minValue = ip._minValue;
            _maxValue = ip._maxValue;
            _dataMin = ip._minValue;
            _dataMax = ip._maxValue;
            _dataMean = ip._dataMean;
            _blank = ip._blank;
            _bzero = ip._bzero;
            _bscale = ip._bscale;
            _lowCut = ip._lowCut;
            _highCut = ip._highCut;
            _userSetCutLevels = true;

            // clone a shallow copy of the colormap object, so that we can compare
            // it later to see if anything changed, and avoid unnecessary updates.
	    _colormap = (ImageColormap) ip._colormap.clone();

            _scaleAlgorithm = ip._scaleAlgorithm;
            _scaleLookupTable = ip._scaleLookupTable;
            _flipX = ip._flipX;
            _flipY = ip._flipY;
            _reverseY = ip._reverseY;
            _invertedYAxis = ip._invertedYAxis;
            _angle = ip._angle;
        }
    }


    /**
     * Set the image cutoff levels.
     *
     * @param lowCut the low cut value
     * @param lowCut the high cut value
     */
    public void setCutLevels(double lowCut, double highCut) {
        setCutLevels(lowCut, highCut, true);
    }


    /**
     * Set the image cutoff levels.
     *
     * @param lowCut the low cut value
     * @param lowCut the high cut value
     * @param userSetCutLevels set to true if the cut levels were set by the user
     *                         (meaning they should not be changed automatically)
     */
    public void setCutLevels(double lowCut, double highCut, boolean userSetCutLevels) {
        if (lowCut > highCut) {
            return;
        }

        this._lowCut = lowCut;
        this._highCut = highCut;

        _updatePending = true;
        this._userSetCutLevels = userSetCutLevels;

        // reduce the image data to byte range with a lookup table
        ImageLookup imageLookup = new ImageLookup();
        _shortImage = imageLookup.scale(_rescaledSourceImage, _scaleAlgorithm, _lowCut, _highCut,
                _roi, _imageHistogram);
        _scaleLookupTable = imageLookup.getLookupTable();
        _imageChangeEvent.setNewCutLevels(true);
    }


    /**
     * Set the low cutoff level
     */
    public void setLowCut(double lowCut) {
        setCutLevels(lowCut, _highCut, true);
    }

    /**
     * Set the high cutoff level
     */
    public void setHighCut(double highCut) {
        setCutLevels(_lowCut, highCut, true);
    }

    /**
     * Get the low cutoff level
     */
    public double getLowCut() {
        return _lowCut;
    }

    /**
     * Get the high cutoff level
     */
    public double getHighCut() {
        return _highCut;
    }


    /**
     * Set the image cut levels automatically using median filtering on the given
     * area of the image.
     *
     * @param region the region of interest in the image
     */
    public void autoSetCutLevels(Rectangle2D.Double region) {
        calculateImageStatistics(region);
        setCutLevels(_lowCut, _highCut, false);
    }


    /**
     * Set the cut levels so that the given percent of pixels
     * in the given region of the image are within the low
     * and high cut values.
     *
     * @param percent value between 0. and 100. indicating percent
     *                of image pixels within the cut levels.
     * @param region the region of interest in the image
     */
    public void autoSetCutLevels(double percent, Rectangle2D.Double region) {
        _userSetCutLevels = false;
        double lowCut = _minValue, highCut = _maxValue;

        calculateImageStatistics(region);

        // get a histogram of the image data
        int dataType = _rescaledSourceImage.getSampleModel().getDataType();
        int numBins = 2048;
        double n = highCut - lowCut;
        if (n <= 0)
            return;

        if (n < numBins && dataType != DataBuffer.TYPE_FLOAT && dataType != DataBuffer.TYPE_DOUBLE)
            numBins = (int) n;

        if (numBins <= 0) {
            return;
        }

        Histogram histogram = _imageHistogram.getHistogram(_rescaledSourceImage, numBins, _minValue, _maxValue, _roi, _xPeriod, _yPeriod);

        // find out how many pixel we actually counted (may be significant numbers of blanks)
        int npixels = 0;
        int[] bins = histogram.getBins(0);
        double binWidth = (_maxValue - _minValue) / numBins;
        for (int i = 0; i < numBins; i++) {
            npixels += bins[i];
        }

        if (npixels > 0) {
            // change to  percent to cut off and split between lowCut and highCut
            int cutoff = (int) ((npixels * (100.0 - percent) / 100.0) / 2.0);

            // set low cut value
            npixels = 0;
            int nprev = 0;
            for (int i = 0; i < numBins; i++) {
                nprev = npixels;
                npixels += bins[i];
                if (npixels >= cutoff) {
                    lowCut = _minValue + i * binWidth;
                    if (i != 0) {
                        // Interpolate between the relevant bins.
                        double interp = (double) (cutoff - nprev) / (npixels - nprev);
                        double d = _minValue + (i - 1) * binWidth;
                        lowCut = d + (lowCut - d) * interp;
                    }
                    break;
                }
            }

            // set high cut value
            npixels = 0;
            nprev = 0;
            for (int i = numBins - 1; i > 0; i--) {
                nprev = npixels;
                npixels += bins[i];
                if (npixels >= cutoff) {
                    highCut = _minValue + i * binWidth;
                    if (i != numBins - 1) {
                        // Interpolate between the relevant bins.
                        double interp = (double) (cutoff - nprev) / (npixels - nprev);
                        double d = _minValue + (i + 1) * binWidth;
                        highCut = d + (d - highCut) * interp;
                    }
                    break;
                }
            }
        }

        setCutLevels(lowCut, highCut, false);
    }

    /** Return true if the user has set the cut levels and they were not automatically set. */
    public boolean isUserSetCutLevels() {
        return _userSetCutLevels;
    }

    /**
     * Set to true if the user has set the cut levels and they were not automatically set.
     * This has the effect that the cut levels will not be automatically set if a new image
     * is loaded.
     */
    public void setUserSetCutLevels(boolean b) {
        _userSetCutLevels = b;
    }

    /**
     * register to receive change events from this object whenever the
     * image or cut levels are changed.
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
     * Notify any listeners of a change in the image or cut levels.
     */
    protected void fireChange(ImageChangeEvent changeEvent) {
        Object[] listeners = _listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ChangeListener.class) {
                ((ChangeListener) listeners[i + 1]).stateChanged(changeEvent);
            }
        }
        changeEvent.reset();
    }


    /**
     * Return the min pixel value in the region specified in the last call
     * to calculateImageStatistics.
     */
    public double getMinValue() {
        return _minValue;
    }


    /**
     * Return the max pixel value in the region specified in the last call
     * to calculateImageStatistics.
     */
    public double getMaxValue() {
        return _maxValue;
    }

    /**
     * Return the value used for bad or blank pixels (taken from "BLANK" image property).
     */
    public double getBlank() {
        return _blank;
    }

    /**
     * Set the value to be used for bad or blank pixels. Used for
     * sub-classing access to _blank.
     */
    protected void setBlank( double blank ) {
        _blank = blank;
    }

    /**
     * Return the rotation angle.
     */
    public double getAngle() {
        return _angle;
    }

    /**
     * Set the rotation angle.
     */
    public void setAngle(double angle) {
        this._angle = angle;
        _imageChangeEvent.setNewAngle(true);
        _updatePending = true;
    }

    /** Return the type of interpolation to use (for rotating). */
    public Interpolation getInterpolation() {
        return _interpolation;
    }

    /** Set the type of interpolation to use (for rotating). */
    public void setInterpolation(Interpolation i) {
        _interpolation = i;
        _updatePending = true;
    }

    /**
     * Toggle the flipping of the X axis
     */
    public void toggleFlipX() {
        _flipX = !_flipX;
        _imageChangeEvent.setNewAngle(true);
        _updatePending = true;
    }

    /**
     * Set the value of the flipX flag.
     */
    public void setFlipX(boolean flipX) {
        if (this._flipX != flipX) {
            this._flipX = flipX;
            _imageChangeEvent.setNewAngle(true);
            _updatePending = true;
        }
    }

    /**
     * Return the value of the flipX flag.
     */
    public boolean getFlipX() {
        return _flipX;
    }

    /**
     * Toggle the flipping of the Y axis
     */
    public void toggleFlipY() {
        _flipY = !_flipY;
        _imageChangeEvent.setNewAngle(true);
        _updatePending = true;
    }

    /**
     * Set the value of the flipY flag.
     */
    public void setFlipY(boolean flipY) {
        if (this._flipY != flipY) {
            this._flipY = flipY;
            _imageChangeEvent.setNewAngle(true);
            _updatePending = true;
        }
    }

    /**
     * Return the value of the flipY flag.
     */
    public boolean getFlipY() {
        return _flipY;
    }


    /**
     * Set the value of the reverseY flag.
     * If this flag is set to true, the value of flipY is reversed (used for FITS images).
     */
    public void setReverseY(boolean reverseY) {
        if (this._reverseY != reverseY) {
            this._reverseY = reverseY;
            _imageChangeEvent.setNewAngle(true);
            _updatePending = true;
        }
    }

    /**
     * Return the value of the reverseY flag.
     * If this flag is true, the value of flipY is reversed (used for FITS images).
     */
    public boolean getReverseY() {
        return _reverseY;
    }

    /**
     * Set to true if the Y axis of the image was inverted while reading.
     * <p>
     * This means that the image doesn't need to be flipped before displaying,
     * but the image coordinate system still needs to be inverted in the Y axis.
     */
    public void setInvertedYAxis(boolean invertedYAxis) {
        if (_invertedYAxis != invertedYAxis) {
            _invertedYAxis = invertedYAxis;
            _imageChangeEvent.setNewAngle(true);
            _updatePending = true;
        }
    }

    /** Return true if the Y axis of the image was inverted while reading. */
    public boolean isInvertedYAxis() {
        return _invertedYAxis;
    }
    

    /**
     * Return a histogram for the image with the given size (number of bins)
     * and region of interest and default settings for the other arguments.
     */
    public Histogram getHistogram(int size, ROI roi) {
        return _imageHistogram.getHistogram(_rescaledSourceImage, size, _lowCut, _highCut, roi, _xPeriod, _yPeriod);
    }


    /** Set the name to use for this object (for testing and debugging). */
    public void setName(String name) {
        this._name = name;
    }

    /** Return the name of this object */
    public String getName() {
        return _name;
    }

    /** Return the lookup table used to scale the image to byte range */
    public LookupTableJAI getScaleLookupTable() {
        return _scaleLookupTable;
    }

    /**
     * Set the algorithm to use to scale the image to byte range.
     *
     * @param scaleAlgorithm one of the public constants defined in the ImageLookup class
     *                       (default: ImageLookup.LINEAR_SCALE).
     */
    public void setScaleAlgorithm(int scaleAlgorithm) {
        this._scaleAlgorithm = scaleAlgorithm;

        // reduce the image data to byte range with a lookup table
        ImageLookup imageLookup = new ImageLookup();
        _shortImage = imageLookup.scale(_rescaledSourceImage, _scaleAlgorithm, _lowCut, _highCut, _roi, _imageHistogram);
        _scaleLookupTable = imageLookup.getLookupTable();

        _imageChangeEvent.setNewColormap(true);
        _updatePending = true;
    }

    /**
     * Return the current image scaling algorithm (one of the constants defined in the
     * ImageLookup class (default: ImageLookup.LINEAR_SCALE).
     */
    public int getScaleAlgorithm() {
        return _scaleAlgorithm;
    }


    /**
     * Create a color RGB lookup table that can be added to the image processing chain,
     * so that we can manipulate the image colors.
     *
     * @param name the name of the colormap table to use. This is currently
     * One of: 	"Background", "Blue", "Heat", "Isophot", "Light", "Pastel",
     * "Ramp", "Real", "Smooth", "Staircase", "Standard".
     * User defined maps will be implemented in a later release.
     */
    public void setColorLookupTable(String name) {
        _colormap.setColorLookupTable(name);
        _imageChangeEvent.setNewColormap(true);
        _updatePending = true;
    }


    /** Return the current lookup table used to add color to a grayscale image. */
    public LookupTableJAI getColorLookupTable() {
        return _colormap.getColorLookupTable();
    }


    /** Return the name of the current color lookup table */
    public String getColorLookupTableName() {
        return _colormap.getColorLookupTableName();
    }


    /** Return the name of the current intensity lookup table */
    public String getIntensityLookupTableName() {
        return _colormap.getIntensityLookupTableName();
    }


    /**
     * Create an intensity lookup table that can be added to the image processing chain
     * to rearrange the order of the colors in the colormap.
     *
     * @param name the name of the intensity lookup table to use. This is currently
     * One of: 	"Equal", "Exponential",	"Gamma", "Jigsaw", "Lasritt", "Logarithmic",
     * "Negative", "Negative Log", "Ramp", "Staircase".
     *
     * User defined intensity lookup tables will be implemented in a later release.
     */
    public void setIntensityLookupTable(String name) {
        _colormap.setIntensityLookupTable(name);
        _imageChangeEvent.setNewColormap(true);
        _updatePending = true;
    }


    /**
     * Rotate the colormap by the given amount.
     */
    public void rotateColormap(int amount) {
        _colormap.rotateColormap(amount);
        _imageChangeEvent.setNewColormap(true);
        _updatePending = true;
    }


    /**
     * Shift the colormap by the given amount.
     */
    public void shiftColormap(int amount) {
        _colormap.shiftColormap(amount);
        _imageChangeEvent.setNewColormap(true);
        _updatePending = true;
    }

    /**
     * Scale the colormap by the given amount.
     */
    public void scaleColormap(int amount) {
        _colormap.scaleColormap(amount);
        _imageChangeEvent.setNewColormap(true);
        _updatePending = true;
    }


    /**
     * Save the current colormap state for the next shift, rotate or scale operation.
     */
    public void saveColormap() {
        _colormap.saveColormap();
    }


    /**
     * Reset the colormap to the default.
     */
    public void resetColormap() {
        _colormap.resetColormap();
        _imageChangeEvent.setNewColormap(true);
        _updatePending = true;
    }


    /**
     * Reset the colormap to the default.
     */
    public void setDefaultColormap() {
        _colormap.setDefaultColormap();
        setScaleAlgorithm(ImageLookup.LINEAR_SCALE);
        _imageChangeEvent.setNewColormap(true);
        _updatePending = true;
    }

    /** Set to true if something was changed and a call to update() is needed. */
    protected void setUpdatePending(boolean b) {
        _updatePending = b;
    }

    /** Return true if something was changed and a call to update() is needed. */
    protected boolean isUpdatePending() {
        return _updatePending;
    }
}

