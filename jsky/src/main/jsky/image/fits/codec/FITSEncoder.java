/*
 * ESO Archive
 *
 * $Id: FITSEncoder.java,v 1.3 2002/07/09 13:30:36 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/03  Created
 */

package jsky.image.fits.codec;

import nom.tam.fits.*;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.IOException;
import java.io.OutputStream;

import com.sun.media.jai.codec.ImageEncoderImpl;
import com.sun.media.jai.codec.ImageEncodeParam;

/**
 * An <code>ImageEncoder</code> for the FITS file format.
 *
 * @version $Revision: 1.3 $
 * @author Allan Brighton
 */
public class FITSEncoder extends ImageEncoderImpl {

    public FITSEncoder(OutputStream output, ImageEncodeParam param) {
        super(output, param);
        if (this.param == null) {
            this.param = new FITSEncodeParam();
        }
    }

    /**
     * Encodes a RenderedImage and writes the output to the
     * OutputStream associated with this ImageEncoder.
     */
    public void encode(RenderedImage im) throws IOException {
        throw new RuntimeException("FITSEncoder not implemented");
        /*...
        SampleModel sampleModel = im.getSampleModel();
        int dataType = sampleModel.getTransferType();
        if ((dataType == DataBuffer.TYPE_FLOAT) ||
            (dataType == DataBuffer.TYPE_DOUBLE)) {
            throw new RuntimeException("Source image has float/double data type: not supported yet");
        }
        int width = im.getWidth();
        int height = im.getHeight();

        int numBands = sampleModel.getNumBands();
        if (numBands != 1) {
            throw new RuntimeException("Source image has an unsupported number of bands.");
        }

        // Read parameters
        // if (((FITSEncodeParam)param).getRaw()) ... XXX not impl

	// Grab the pixels
	// Raster src = im.getData(new Rectangle(minX, row, width, rows));
        int[] pixels = new int[width*height*numBands];
	Raster src = im.getData();
	src.getPixels(0, 0, width, height, pixels);

	// get 2D array for writing (XXX need to do this with fewer copies)
	int[][] imageArray = new int[width][height];

	// write it....
	Fits fits = new Fits();
	try {
	    fits.addHDU(new ImageHDU(imageArray));
	    fits.write(output);
	} catch (FitsException e) {
            e.printStackTrace();
            throw new RuntimeException("A FitsException occured while writing the FITS file.");
        }

        // Force all buffered bytes to be written out.
        output.flush();
	...*/
    }
}
