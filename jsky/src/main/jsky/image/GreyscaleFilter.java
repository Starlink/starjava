//=== File Prolog =============================================================
//	This code was developed by NASA, Goddard Space Flight Center, Code 588
//	for the Scientist's Expert Assistant (SEA) project.
//
//--- Contents ----------------------------------------------------------------
//	class GreyscaleFilter
//
//--- Description -------------------------------------------------------------
//	Filters an image by converting colors to greyscale.
//
//--- Notes -------------------------------------------------------------------
//
//--- Development History -----------------------------------------------------
//
//	07/30/98	J. Jones / 588
//
//		Original implementation.
//
//--- Warning -----------------------------------------------------------------
//	This software is property of the National Aeronautics and Space
//	Administration.  Unauthorized use or duplication of this software is
//	strictly prohibited.  Authorized users are subject to the following
//	restrictions:
//	*	Neither the author, their corporation, nor NASA is responsible for
//		any consequence of the use of this software.
//	*	The origin of this software must not be misrepresented either by
//		explicit claim or by omission.
//	*	Altered versions of this software must be plainly marked as such.
//	*	This notice may not be removed or altered.
//
//=== End File Prolog =========================================================

//package GOV.nasa.gsfc.sea.util.image;

package jsky.image;

import java.awt.image.*;

/**
 * Filters an image by converting colors to greyscale.
 *
 * <P>This code was developed by NASA, Goddard Space Flight Center, Code 588
 * for the Scientist's Expert Assistant (SEA) project.
 *
 * @version		07/30/98
 * @author		J. Jones / 588
 **/
public class GreyscaleFilter extends RGBImageFilter {

    /**
     * The only constructor.
     **/
    public GreyscaleFilter() {
        super();

        // The filter's operation does not depend on the
        // pixel's location, so IndexColorModels can be
        // filtered directly.
        canFilterIndexColorModel = true;
    }

    /**
     * This method is never called because we override filterRGBPixels()
     * (this is done for efficiency, since don't have to call this method
     * for every pixel).  Normally this method would return a single filtered
     * pixel value.
     **/
    public int filterRGB(int x, int y, int rgb) {
        return rgb;
    }

    /**
     * This method performs the actual modification of the image.
     * It is overridden for efficiency, since the filterRGB() method is
     * normally called for each individual pixel in the image.
     **/
    public void filterRGBPixels(int x, int y, int w, int h,
                                int pixels[], int off, int scansize) {
        int index = off;
        int red, green, blue, grey;
        for (int cy = 0; cy < h; cy++) {
            for (int cx = 0; cx < w; cx++) {
                red = (pixels[index] & 0x00ff0000) >> 16;
                green = (pixels[index] & 0x0000ff00) >> 8;
                blue = (pixels[index] & 0x000000ff);

                // Only bother changing if not already greyscale
                if ((red != green) || (green != blue)) {
                    grey = (int) (Math.round((red + green + blue) / 3.0));

                    pixels[index] = ((pixels[index] & 0xff000000)
                            | (grey << 16)
                            | (grey << 8)
                            | (grey));
                }

                index++;
            }
            index += scansize - w;
        }

        consumer.setPixels(x, y, w, h, ColorModel.getRGBdefault(),
                pixels, off, scansize);
    }
}
