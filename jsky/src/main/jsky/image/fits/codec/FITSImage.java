/*
 * Copyright 2002 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: FITSImage.java,v 1.24 2002/08/16 22:21:13 brighton Exp $
 */

package jsky.image.fits.codec;

import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.SeekableStream;

import java.awt.Point;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;

import javax.media.jai.JAI;
import javax.media.jai.RasterFactory;
import javax.media.jai.TileCache;
import javax.media.jai.TiledImage;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.Data;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.ImageData;
import nom.tam.fits.ImageHDU;
import nom.tam.image.ImageTiler;
import nom.tam.util.ArrayDataInput;
import nom.tam.util.BufferedFile;

import ca.nrc.cadc.arch.io.FitsFilterInputStream;

/**
 * This is the core class for JAI FITS support. It handles the conversion between the
 * FITS image data and the display data.
 * <p>
 * This class defines a number of properties that can be accessed by applications
 * via the getProperty method.
 * <p>
 * The value of the property "#fits_image" returns the FITSImage object managing
 * the image data.
 * <p>
 * The value of the property "#num_pages" returns an Integer with the number of HDUs
 * (FITS header/data units).
 * <p>
 * The "#preview_image" property returns a preshrunk preview image suitable for use in
 * a pan window. The size of the preview image may be set by calling the static method
 * FITSImage.setPreviewSize(int).
 *
 * @version $Revision: 1.24 $
 * @author Allan Brighton
 */
public class FITSImage extends jsky.image.SimpleRenderedImage {

    /** Object managing Fits I/O */
    private Fits _fits;

    /** Current image HDU (this class only deals with image HDUs and ignores others) */
    private ImageHDU _hdu;

    /** Index of the current HDU */
    private int _hduIndex = -1;

    /** current Fits Header object */
    private Header _header;

    /** current Fits Data object */
    private Data _data;

    /** the type of the data buffer (DataBuffer.TYPE_BYTE, ...) */
    private int _dataType;

    /** object used to manage data type specific operations */
    private FITSData _fitsData;

    /** array of axes info for the current HDU */
    private int[] _axes;

    /** value of FITS keyword BITPIX */
    private int _bitpix;

    /** Object used to access FITS data tiles */
    private ImageTiler _tiler;

    /** Default tile width */
    private static int _defaultTileWidth = 256;

    /** Default tile height */
    private static int _defaultTileHeight = 256;

    /** Object used to cache image tiles */
    private TileCache _tileCache = JAI.getDefaultInstance().getTileCache();

    /** Contains caller parameters */
    private FITSDecodeParam _param;

    /** Requested size of the preview image (width, height - actual size will vary) */
    private static int _previewSize = 152;

    /** true if the image is empty (such as the primary extension) */
    private boolean _empty = false;

    // The current scale factor (zooming out is handled here for performance reasons).
    // Zooming in still needs to be done by the image widget.
    private float _scale = 1.0F;

    // The increment to use when accessing the image data if _scale is less than 1.
    // A value of 1 means no scaling is done.
    private int _subsample = 1;

    // Memory mapped to the image data, if the FITS input is from a BufferedFile, otherwise null.
    // The FITSData<type> classes then view this buffer as a FloatBuffer, ShortBuffer, etc.
    private MappedByteBuffer _byteBuffer;


    /**
     * Construct a FITSImage.
     *
     * @param input the SeekableStream for the FITS file.
     * @param param the parameter passed to the JAI create method
     * @param page specifies the desired image extension (default: 0, for the primary extension)
     */
    public FITSImage(SeekableStream input, FITSDecodeParam param, int page) throws IOException, FitsException {
        this._param = param;

        // Create a Fits object from the stream
        _fits = new Fits(input);
        _fits.read();

        // Switch to the specified HDU/extension
        setHDU(page);

        // Close the FITS stream and release system resources.
        input.close();
    }

    /**
     * Construct a FITSImage from an image file or URL.
     *
     * @param fileOrURL the file name or URL
     */
    public FITSImage(String fileOrUrl) throws IOException, FitsException {
        // First try to open the file using the Fits classes, since they
        // work more efficiently with thier own I/O classes (they also handle
        // gzipped FITS files).
        try {
            _fits = new Fits(fileOrUrl);
            _fits.read();
        }
        catch (Exception e) {
            // Might be an HCompressed FITS file...
            try {
                _fits = new Fits(new FitsFilterInputStream(_getStream(fileOrUrl)));
                _fits.read();
            }
            catch (Exception e2) {
                //e2.printStackTrace();
                if (e instanceof FitsException)
                    throw (FitsException) e;
                if (e instanceof IOException)
                    throw (IOException) e;
                throw new RuntimeException(e);
            }
        }

        // Switch to the primary HDU/extension
        setHDU(0);
    }


    /**
     * Create a FITSImage from an already existing Fits object.
     *
     * @param fits object managing the FITS file
     * @param param the parameter passed to the JAI create method
     * @param page specifies the desired image extension (default: 0, for the primary extension)
     */
    public FITSImage(Fits fits, FITSDecodeParam param, int page) throws IOException, FitsException {
        this._fits = fits;
        this._param = param;

        // Default to the primary HDU
        setHDU(page);
    }


    /**
     * Create a FITSImage from a primitive array of data.
     *
     * @param ar a 2D array of a primitive numeric type (doubles not supported yet)
     */
    public FITSImage(Object ar) throws IOException, FitsException {
        ImageData data = new ImageData(ar);
        Header header = new Header(data);
        ImageHDU hdu = new ImageHDU(header, data);
        _fits = new Fits();
        _fits.addHDU(hdu);

        // Default to the primary HDU
        setHDU(0);
    }


    /** Return the index of the current image HDU */
    public int getCurrentHDUIndex() {
        return _hduIndex;
    }


    /**
     * Return a BufferedInputStream for the given file or URL.
     */
    private BufferedInputStream _getStream(String fileOrUrl) throws IOException {
        URL url = _getURL(fileOrUrl);
        InputStream stream = url.openStream();
        if (!(stream instanceof BufferedInputStream))
            stream = new BufferedInputStream(stream);
        return (BufferedInputStream) stream;
    }


    /** 
     * Close the FITS input stream. After calling this method, this object should
     * no longer be used.
     */
    public void close() {
	try {
	    _fits.getStream().close();
	}
	catch(Exception e) {
	}
    }


    /**
     * Return a URL for the given file or URL string.
     */
    private URL _getURL(String fileOrUrl) throws MalformedURLException {
        URL url = null;
        if (fileOrUrl.startsWith("http:") || fileOrUrl.startsWith("file:") || fileOrUrl.startsWith("ftp:")) {
            url = new URL(fileOrUrl);
        }
        else {
            File file = new File(fileOrUrl);
            url = file.getAbsoluteFile().toURL();
        }
        return url;
    }


    /** Return the number of FITS HDUs in the current image. */
    public int getNumHDUs() {
        try {
            _fits.read();
        }
        catch (FitsException e) {
            throw new RuntimeException(e);
        }
        return _fits.getNumberOfHDUs();
    }


    /** Return the internal Fits object used to manage the image */
    public Fits getFits() {
        return _fits;
    }


    /** Return the given HDU or null if it can not be accessed. */
    public BasicHDU getHDU(int num) {
        BasicHDU hdu = null;
        // XXX how to handle errors...
        try {
            hdu = _fits.getHDU(num);
        }
        catch (Exception ex) {
        }
        return hdu;
    }


    /**
     * This method should be called after adding a new HDU.
     */
    public void update() {
    }

    /**
     * Move to the given HDU.
     *
     * @param num The HDU number (0 is the primary HDU).
     */
    public void setHDU(int num) throws IOException, FitsException {
	if (_hduIndex == num)
	    return;

        _hduIndex = num;
        _hdu = (ImageHDU) _fits.getHDU(num);
        if (_hdu != null) {
            _tiler = _hdu.getTiler();
            _axes = _hdu.getAxes();
            _header = _hdu.getHeader();
            _data = _hdu.getData();
            _bitpix = _hdu.getBitPix();
            _empty = false;
        }

        if (_hdu == null || _axes == null) {
            _tiler = null;
            _axes = new int[2];
            _axes[0] = 2;
            _axes[1] = 2;
            _header = null;
            _data = null;
            _bitpix = 8;
            _empty = true;
	    _byteBuffer = null;
        }

        minX = 0;
        minY = 0;
        // JEJ - Added support for 3 and 4 axis images
        if (_axes.length > 4) {
            throw new IOException("Expected NAXIS <= 4, but got: " + _axes.length);
        }

	_scale = 1.0F;
	_subsample = 1;
	_initFITSData();
    }

    /** 
     * Returns the real width of the image.
     * This may be different than the value returned by getWidth() if the image is
     * zoomed out.
     */
    public int getRealWidth() {
	return _axes[_axes.length - 1];
    }

    /** 
     * Returns the real height of the image. 
     * This may be different than the value returned by getHeight() if the image is
     * zoomed out.
     */
    public int getRealHeight() {
        return _axes[_axes.length - 2];
    }

    /** 
     * Return the number of axes
     */
    public int getNAXIS() {
        return _axes.length;
    }

    /** Return the object used to access FITS data tiles (used only if MappedByteBuffer can't be used) */
    public ImageTiler getImageTiler() {return _tiler;}


    /** Return true if the image is empty (such as the primary extension) */
    public boolean isEmpty() {return _empty;}


    /**
     * Return the current scale factor (zooming out is handled here for performance reasons).
     * Zooming in still needs to be done by the image widget.
     */
    public float getScale() {return _scale;}

    /**
     * Return the increment to use when accessing the image data if _scale is less than 1.
     * A value of 1 means no scaling is done.
     */
    public int getSubsample() {return _subsample;}

    /**
     * Return the memory mapped to the image data, if the FITS input is from a BufferedFile, otherwise null.
     * The FITSData<type> classes then view this buffer as a FloatBuffer, ShortBuffer, etc.
     */
    public MappedByteBuffer getByteBuffer() {return _byteBuffer;}


    public static void setDefaultTileWidth(int w) {
        _defaultTileWidth = w;
    }

    public static int getDefaultTileWidth() {
        return _defaultTileWidth;
    }

    public static void setDefaultTileHeight(int h) {
        _defaultTileHeight = h;
    }

    public static int getDefaultTileHeight() {
        return _defaultTileHeight;
    }


    /** Try to save memory by clearing out the tile cache */
    public void clearTileCache() {
        _tileCache.flush();
    }


    /**
     * Return the FITS header object
     */
    public Header getHeader() {
        return _header;
    }


    /**
     * Gets a property from the property set of this image.
     *
     * @param name the name of the property to get, as a String.
     * @return a reference to the property value or null if not found.
     */
    public Object getProperty(String name) {
        if (name.equals("#num_pages")) {
            // return the number of HDUs (called pages in the general case?)
            return Integer.toString(getNumHDUs());
        }

        if (name.equals("#preview_image")) {
            return _getPreviewImage(_previewSize);
        }

        if (name.equals("#fits_image")) {
            return this;
        }

        return null;
    }


    /**
     * Returns a list of property names that are recognized by this image.
     *
     * @return an array of Strings containing valid property names.
     */
    public String[] getPropertyNames() {
        String[] names = new String[]{
            "#num_pages",
            "#preview_image",
            "#fits_image"
        };
        return names;
    }


    /**
     * Return the value of the given FITS keyword from the FITS header,
     * or null if not found.
     *
     * @param name a FITS keyword
     * @return the value for the given keyword, or null if not found.
     */
    public Object getKeywordValue(String name) {
        if (_header != null) {
            HeaderCard card = _header.findCard(name);
            if (card != null) {
                String s = card.getValue();
                if (s != null)
                    return s;
                return "";
            }
        }
        return null;
    }

    /**
     * Return the comment for the given FITS keyword from the FITS header,
     * or null if not found.
     *
     * @param name a FITS keyword
     * @return the comment string, or an empty string if not found.
     */
    public String getKeywordComment(String name) {
        HeaderCard card = _header.findCard(name);
        if (card != null) {
            String s = card.getComment();
            if (s != null)
                return s;
        }
        return "";
    }


    /**
     * Return the value of the given FITS keyword as a string,
     * or the given default value, if not found.
     *
     * @param name a FITS keyword
     * @param defaultValue the value to return if the keyword is not found
     * @return the string value for the given keyword
     */
    public String getKeywordValue(String name, String defaultValue) {
        Object o = getKeywordValue(name);
        if (o instanceof String)
            return (String) o;
        return defaultValue;
    }

    /**
     * Return the value of the given FITS keyword as an integer,
     * or the given default value, if not found.
     *
     * @param name a FITS keyword
     * @param defaultValue the value to return if the keyword is not found
     * @return the integer value for the given keyword
     */
    public int getKeywordValue(String name, int defaultValue) {
        Object o = getKeywordValue(name);
        if (o != null) {
            return Integer.parseInt(o.toString());
        }
        return defaultValue;
    }

    /**
     * Return the value of the given FITS keyword as a double,
     * or the given default value, if not found.
     *
     * @param name a FITS keyword
     * @param defaultValue the value to return if the keyword is not found
     * @return the double value for the given keyword
     */
    public double getKeywordValue(String name, double defaultValue) {
        Object o = getKeywordValue(name);
        if (o != null) {
            return Double.parseDouble(o.toString());
        }
        return defaultValue;
    }

    /**
     * Return the value of the given FITS keyword as a float,
     * or the given default value, if not found.
     *
     * @param name a FITS keyword
     * @param defaultValue the value to return if the keyword is not found
     * @return the float value for the given keyword
     */
    public float getKeywordValue(String name, float defaultValue) {
        Object o = getKeywordValue(name);
        if (o != null) {
            return Float.parseFloat(o.toString());
        }
        return defaultValue;
    }


    /**
     * Returns a list of FITS keywords for this image.
     *
     * @return an array of Strings containing FITS keywords.
     */
    public String[] getKeywords() {
        int index = 0;
        int numKeywords = _header.getNumberOfCards();
        String[] names = new String[numKeywords];
        Iterator iter = _header.iterator();
        while (iter.hasNext()) {
            HeaderCard hc = (HeaderCard) iter.next();
            String key = hc.getKey();
            if (key == null)
                key = "";
            names[index++] = key;
        }
        return names;
    }

    /**
     * Return a prescaled PlanarImage that fits entirely in a window of the given size,
     * of null if there are any errors.
     */
    private TiledImage _getPreviewImage(int size) {
        if (size == 0 || _empty)
            return null;

	int w = getRealWidth(), h = getRealHeight();
        int factor = Math.max((w - 1)/size+1, (h-1)/size+1);
        if (factor <= 1) {
            return null;
        }

        int tileWidth = w / factor;
        int tileHeight = h / factor;

	//_dataType = _getDataType();
        SampleModel sampleModel = _initSampleModel(tileWidth, tileHeight);
        ColorModel colorModel = _initColorModel(sampleModel);
        TiledImage tiledImage = new TiledImage(0, 0, tileWidth, tileHeight, 0, 0, sampleModel, colorModel);
        Point origin = new Point(0, 0);
        Raster raster = RasterFactory.createWritableRaster(sampleModel, origin);

        try {
            raster = _fitsData.getPreviewImage(raster, factor);
        }
        catch (EOFException e) {
            //System.out.println("XXX FITSImage._getPreviewImage(): warning: " + e.toString());
        }
        catch (IndexOutOfBoundsException e) {
            //System.out.println("XXX FITSImage._getPreviewImage(): warning: " + e.toString());
        }
        catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        if (raster == null)
            return null;

        tiledImage.setData(raster);
        return tiledImage;
    }


    /**
     * Set the scale (zoom factor) for the image and return true if a new image was generated.
     * <p>
     * Note that <em>zooming out</em> is handled here for performance reasons, to avoid having to 
     * read in whole tiles, only to discard most of the data later. <em>Zooming in</em> should 
     * be handled in the image viewer widget at the end of the image processing chain.
     *
     * @param scale the scale factor (a value of less than 1.0 means the image is zoomed out and is 
     *              handled specially here)
     *
     * @return true if the new scale value caused a new image to be generated, requiring
     *         an image update in the viewer widget
     */
    public boolean setScale(float scale) throws IOException {
	boolean needsUpdate = false;
	if (scale > 1)
	    scale = 1;
	if (_byteBuffer != null && scale != _scale) {
	    needsUpdate = true;
	    _scale = scale;
	    if (_scale < 1) 
		_subsample = Math.round(1.0F/_scale);
	    else 
		_subsample = 1;

	    _tileCache.flush();

	    _initImage();
	}
	return needsUpdate;
    }

    /** 
     * Return true if the Y axis of the image data tiles returned by this class is flipped 
     * (for performance reasons, so it doesn't have to be done afterwards).
     * If this method returns false, the image will normally be flipped before displaying.
     */
    public boolean isYFlipped() {
	return (_byteBuffer != null);
    }


    // Initialize the image dimensions, colormodel, samplemodel, and tile size
    private void _initImage() throws IOException {
	// set variables required by the base class for tiling
	width = _axes[_axes.length - 1];
	height = _axes[_axes.length - 2];

	// initialize an object to do data type specific operations
	if (width != 0 && height != 0) {
	    // handle zoom out here for performance reasons
	    if (_subsample != 1) {
		width /= _subsample;
		height /= _subsample;
	    }

	    // try to choose a reasonable tile size
	    tileWidth = _defaultTileWidth;
	    if (width / tileWidth <= 1)
		tileWidth = width;

	    tileHeight = _defaultTileHeight;
	    if (height / tileHeight <= 1)
		tileHeight = height;

	    // Choose appropriate sample and color models.
	    _dataType = _getDataType();
	    sampleModel = _initSampleModel(tileWidth, tileHeight);
	    colorModel = _initColorModel(sampleModel);

	    if (! _empty)
		_initByteBuffer();
	}
    }


    /**
     * Set the colormodel to use to display FITS images.
     */
    private ColorModel _initColorModel(SampleModel sampleModel) {
        return ImageCodec.createComponentColorModel(sampleModel);
    }


    /**
     * Return a SampleModel for this image with the given tile width and height.
     */
    private SampleModel _initSampleModel(int tileWidth, int tileHeight) {
        int[] bandOffsets = new int[1];
        bandOffsets[0] = 0;
        int pixelStride = 1;
        int scanlineStride = tileWidth;
        return RasterFactory.createPixelInterleavedSampleModel(_dataType,
                tileWidth, tileHeight,
                pixelStride,
                scanlineStride,
                bandOffsets);
    }


    // Initialize a memory mapped byte buffer for accessing the FITS image data, if
    // possible
    private void _initByteBuffer() throws IOException {
	ArrayDataInput arrayDataInput = _fits.getStream();
	if (arrayDataInput instanceof BufferedFile) {
	    //System.out.println("XXX FITSImage: using BufferedFile");
	    long headerSize = _header.getSize();
	    long offset = _hdu.getFileOffset() + headerSize;
	    long size = _hdu.getSize() - headerSize;
	    //System.out.println("XXX FITSImage: HDU offset = " + offset + ", size = " + size);
	    BufferedFile bufferedFile = (BufferedFile)arrayDataInput;
	    FileChannel channel = bufferedFile.getChannel();
	    _byteBuffer = channel.map(FileChannel.MapMode.READ_ONLY, offset, size);
	    //System.out.println("XXX FITSImage: got byteBuffer");
	}
	else {
	    //System.out.println("XXX FITSImage: no byteBuffer access");
	}
    }

    
    /**
     * Create an object to manage the FITS data based on the value of the
     * BITPIX FITS keyword and set the value of the dataType member variable
     * to the correct DataBuffer constant to use for the sample model.
     */
    private void _initFITSData() throws IOException {
	_initImage();

	switch (_dataType) {
	case DataBuffer.TYPE_SHORT:
	    _fitsData = new FITSDataShort(this);
	    break;
	case DataBuffer.TYPE_BYTE:
	    _fitsData = new FITSDataByte(this);
	    break;
	case DataBuffer.TYPE_INT:
	    _fitsData = new FITSDataInt(this);
	    break;
	case DataBuffer.TYPE_FLOAT:
	    _fitsData = new FITSDataFloat(this);
	    break;
	case DataBuffer.TYPE_DOUBLE:
	    _fitsData = new FITSDataDouble(this);
	    break;
	default:
	    throw new RuntimeException("Unknonwn image data type: " + _dataType);
	}
    }


    // Return the data buffer type based on the current bitpix value
    private int _getDataType() {
        if (_empty) {
            _bitpix = BasicHDU.BITPIX_BYTE;
            return DataBuffer.TYPE_BYTE;
        }
        else {
            switch (_bitpix) {
            case BasicHDU.BITPIX_SHORT:
                return DataBuffer.TYPE_SHORT;
            case BasicHDU.BITPIX_BYTE:
                return DataBuffer.TYPE_BYTE;
            case BasicHDU.BITPIX_INT:
                return DataBuffer.TYPE_INT;
            case BasicHDU.BITPIX_FLOAT:
                return DataBuffer.TYPE_FLOAT;
            case BasicHDU.BITPIX_DOUBLE:
                return DataBuffer.TYPE_DOUBLE;
            default:
                throw new RuntimeException("Invalid BITPIX value: " + _bitpix);
            }
        }
    }


    /**
     * Generate and return the given tile (required by the RenderedImage interface).
     * Note that tileX and tileY are indices into the tile array, not pixel locations.
     *
     * @param tileX the X index of the requested tile in the tile array.
     * @param tileY the Y index of the requested tile in the tile array.
     * @return the tile given by (tileX, tileY).
     */
    public synchronized Raster getTile(int tileX, int tileY) {
        if (_empty) {
            return RasterFactory.createWritableRaster(sampleModel, new Point(0, 0));
        }

        Raster tile = _tileCache.getTile(this, tileX, tileY);
        if (tile == null) {
            //System.out.println("XXX FITSImage: get new tile (" + tileX + ", " + tileY + ") at scale = " + _scale);
            Point origin = new Point(tileXToX(tileX), tileYToY(tileY));
            tile = RasterFactory.createWritableRaster(sampleModel, origin);
            _fillTile(tile);
            _tileCache.add(this, tileX, tileY, tile);
        }
        return tile;
    }


    /**
     * This method fills the given tile with the appropriate image data.
     */
    private Raster _fillTile(Raster tile) {
        try {
            _fitsData.getTile(tile, _subsample, width, height);
        }
        catch (EOFException e) {
            //System.out.println("XXX FITSImage.getTile(): warning: " + e.toString());
            // just ignore EOF ???
        }
        catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return tile;
    }


    /** Set the requested size for the preview image */
    public static void setPreviewSize(int i) {
        _previewSize = i;
    }

    /** Return the requested size for the preview image */
    public static int getPreviewSize() {
        return _previewSize;
    }
}

