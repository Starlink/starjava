/*
 * Copyright 2002 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: FITSData.java,v 1.3 2002/08/16 22:21:13 brighton Exp $
 */

package jsky.image.fits.codec;

import java.awt.image.Raster;
import java.io.*;
import java.lang.reflect.Array;

import nom.tam.image.*;

/**
 * An abstract base class for performing data type specific operations
 * on 2D FITS data.
 *
 * @version $Revision: 1.3 $
 * @author Allan Brighton
 */
public abstract class FITSData {

    /** Reference to the class managing the image access */
    protected FITSImage _fitsImage;

    /** Object used to access the image tiles */
    private ImageTiler _tiler;

    /** Number of axes (Currently only the width and height are considerred) */
    protected int _naxis;

    /** The number of FITS pixels in the X direction */
    protected int _width;

    /** The number of FITS pixels in the Y direction */
    protected int _height;


    /**
     * Constructor.
     *
     * @param tiler the FITS image tiler
     * @param axes an array containing the dimensions of the image
     */
    public FITSData(FITSImage fitsImage) {
        this._fitsImage = fitsImage;
        _tiler = _fitsImage.getImageTiler();
        _naxis = _fitsImage.getNAXIS();
	_width = _fitsImage.getRealWidth();
	_height = _fitsImage.getRealHeight();
    }

    /**
     * Fill the given array with image data starting at the given offsets
     * and with the given width and height in image pixels.
     *
     * @param destArray the image data array
     * @param x the x offset in the image data
     * @param y the y offset in the image data
     * @param w the width of the data to get
     * @param h the height of the data to get
     */
    protected void fillTile(Object destArray, int x, int y, int w, int h) throws IOException {
        int[] corners, lengths;
        switch (_naxis) {
        case 2:
            corners = new int[]{y, x};
            lengths = new int[]{h, w};
            break;
        case 3:
            corners = new int[]{0, y, x};
            lengths = new int[]{0, h, w};
            break;
        case 4:
            corners = new int[]{0, 0, y, x};
            lengths = new int[]{0, 0, h, w};
            break;
        default:
            throw new RuntimeException("Unsupported number of axes");
        }

        // The dimensions of the arguments to tiler.getTile() are dependent on NAXIS.
        _tiler.getTile(destArray, corners, lengths);
    }

    /**
     * Fill in the given tile with the appropriate image data.
     *
     * @param tile the tile to fill with data
     * @param subsample the increment to use when zooming out using the mapped byte buffer
     * @param width the total image width in pixels
     * @param height the total image height in pixels
     *
     * @return the tile argument
     */
    public abstract Raster getTile(Raster tile, int subsample, int width, int height) throws IOException;

    /**
     * Return a prescaled preview image at "1/factor" of the normal size in the given
     * raster tile.
     */
    public abstract Raster getPreviewImage(Raster tile, int factor) throws IOException;
}



