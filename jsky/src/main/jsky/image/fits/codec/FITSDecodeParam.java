/*
 * ESO Archive
 *
 * $Id: FITSDecodeParam.java,v 1.2 2002/07/09 13:30:36 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/11/17  Created
 */

package jsky.image.fits.codec;

import com.sun.media.jai.codec.ImageDecodeParam;

public class FITSDecodeParam implements ImageDecodeParam {

    /**
     * This specifies the size of the preview image. The default is 0,
     * meaning that no preview image will be generated. If a value > 0
     * is specified, it is taken to be the size of the window in which
     * the preview image should fit.
     */
    private int previewSize = 0;

    public FITSDecodeParam() {
    }

    /**
     * Set the size of the window used to display the prescaled preview image.
     * This determines the scale factor.
     */
    public void setPreviewSize(int i) {
        previewSize = i;
    }

    /** Return the value of the previewSize property. */
    public int getPreviewSize() {
        return previewSize;
    }
}

