/*
 * Copyright 2002 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: FITSDataByte.java,v 1.4 2002/08/18 22:38:10 brighton Exp $
 */

package jsky.image.fits.codec;

import java.awt.image.Raster;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;

import java.awt.image.DataBufferByte;

// -------------------------------------------------------------------
// Note: The FITSData<type> classes can be generated from FITSDataByte
// by typing "make generate" (on UNIX or cygwin systems).
// -------------------------------------------------------------------


/**
 * Used for byte FITS image data (2D).
 *
 * @version $Revision: 1.4 $
 * @author Allan Brighton
 */
public class FITSDataByte extends FITSData {

    // Memory mapped image data, or null if not defined
    private ByteBuffer _mappedBuffer;


    /** 
     * Constructor
     */
    public FITSDataByte(FITSImage fitsImage) {
        super(fitsImage);

	MappedByteBuffer byteBuffer = fitsImage.getByteBuffer();
	if (byteBuffer != null) {
	    _mappedBuffer = byteBuffer;
	}
    }


    /**
     * Fill in the given tile with the appropriate image data.
     * <p>
     * If the memory mapped buffer is not null, the Y axis will be flipped to the normal
     * orientation for display, otherwise this needs to be done later in the display widget.
     *
     * @param tile the tile to fill with data
     * @param subsample the increment to use when zooming out using the mapped byte buffer
     * @param scaledWidth the total image width in pixels (after prescaling, may be different than the "real" width)
     * @param scaledHeight the total image height in pixels (after prescaling, may be different than the "real" height)
     *
     * @return the tile argument
     */
    public Raster getTile(Raster tile, int subsample, int scaledWidth, int scaledHeight) throws IOException {
        DataBufferByte dataBuffer = (DataBufferByte)tile.getDataBuffer();
        byte[] destArray = dataBuffer.getData();
	int tw = tile.getWidth(), 
	    th = tile.getHeight(),
	    x0 = tile.getMinX(), 
	    y0 = tile.getMinY(), 
	    x1 = Math.min(x0 + tw - 1, scaledWidth-1),
	    y1 = Math.min(y0 + th - 1, scaledHeight-1),
	    xWidth = x1 - x0 + 1;

	if (_mappedBuffer != null) {
	    // flip the Y axis while reading, and save time later on
	    int tmpY0 = y0;
	    y0 = scaledHeight - y1 - 1;
	    y1 = scaledHeight - tmpY0 - 1;
	    
	    // use memory mapped buffer
	    if (subsample == 1) { 
		// normal or zoomed in: include all pixels
		for(int j = y1; j >= y0; j--) {
		    _mappedBuffer.position(j * _width + x0);
		    _mappedBuffer.get(destArray, (y1 - j) * tw, xWidth);
		}
	    }
	    else { 
		// zoomed out: skip subsample pixels
		for(int j = y1; j >= y0; j--) {
		    int dst = (y1 - j) * tw;
		    int src = (j * _width + x0) * subsample;
		    for(int i = x0; i <= x1; i++) {
			destArray[dst++] = _mappedBuffer.get(src);
			src += subsample;
		    }
		}
	    }
	}
	else {
	    // use image tiler (slower)
	    fillTile(destArray, x0, y0, tw, th);
	}

        return tile;
    }


    /**
     * Return a prescaled preview image at "1/factor" of the normal size in the given
     * raster tile.
     */
    public Raster getPreviewImage(Raster tile, int factor) throws IOException {
	if (_mappedBuffer != null) {
	    // use the memory mapped buffer (faster)
	    return getTile(tile, factor, tile.getWidth(), tile.getHeight());
	}

	// use the image tiler (slower)
	DataBufferByte dataBuffer = (DataBufferByte)tile.getDataBuffer();
	byte[] destArray = dataBuffer.getData();
	int tw = tile.getWidth(),
	    th = tile.getHeight(),
	    w = tw * factor,
	    h = th * factor,
	    n = 0,
	    m = 0;

	byte[] line = new byte[_width];
	for (int j = 0; j < h; j += factor) {
	    n = m++ * tw;
	    fillTile(line, 0, j, _width, 1);
	    for (int i = 0; i < w; i += factor) {
		destArray[n++] = line[i];
	    }
	}

        return tile;
    }
}

