/*
 * ESO Archive
 *
 * $Id: FITSDecoder.java,v 1.3 2002/07/09 13:30:36 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/03  Created
 */

package jsky.image.fits.codec;

import nom.tam.fits.*;

import java.io.IOException;
import javax.media.jai.*;

import com.sun.media.jai.codec.*;

import java.awt.image.*;
import java.awt.image.renderable.*;

/**
 * An <code>ImageDecoder</code> for the FITS file format.
 *
 * @version $Revision: 1.3 $
 * @author Allan Brighton
 */
public class FITSDecoder extends ImageDecoderImpl {

    /** Saved reference to the FITSImage object, used to get the number of pages. */
    private FITSImage fitsImage;

    /** Constructor */
    public FITSDecoder(SeekableStream input, ImageDecodeParam param) throws IOException, FitsException {
        super(input, param);
        fitsImage = new FITSImage(input, (FITSDecodeParam) param, 0);
    }


    /**
     * Returns a RenderedImage that contains the decoded contents of the
     * SeekableStream associated with this ImageDecoder. Only the first
     * page of a multi-page image is decoded.
     */
    public RenderedImage decodeAsRenderedImage() throws IOException {
        return fitsImage;
    }


    /**
     * Returns a RenderedImage that contains the decoded contents of
     * the SeekableStream associated with this ImageDecoder. The given
     * page of a multi-page image is decoded. If the page does not
     * exist, an IOException will be thrown. Page numbering begins at
     * zero.
     */
    public RenderedImage decodeAsRenderedImage(int page) throws IOException {
        try {
            fitsImage.setHDU(page);
        }
        catch (FitsException e) {
            throw new IOException(e.toString());
        }
        return fitsImage;
    }


    /**
     * Returns the number of pages (FITS extensions) present in the current stream.
     * <p>
     * Note: The FITS codec defines properties, such as "#num_pages", "#fits_image",
     * and "#preview_image" that give direct access to the FITS HDUs, so that you
     * can tell ahead of time if an HDU contains an image or an ASCII or binary table.
     */
    public int getNumPages() throws java.io.IOException {
        return fitsImage.getNumHDUs();
    }

}

