/*
 * ESO Archive
 *
 * $Id: ImageLookup.java,v 1.4 2002/07/09 13:30:36 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/11/17  Created
 */


package jsky.image;

import jsky.image.operator.*;

import java.awt.image.renderable.*;
import java.awt.image.DataBuffer;
import javax.media.jai.*;
import java.awt.*;

/**
 * Implements various image scaling operations that
 * reduce the source image data to byte range. The source image
 * data is first converted to ushort range, if necessary, and then
 * a lookup table is used to convert to byte range, based on
 * the algorithm chosen.
 *
 * @version $Revision: 1.4 $
 * @author Allan Brighton
 */
public class ImageLookup {

    /** assume 256 colors in the final image */
    protected static final int NCOLORS = 256;

    /** minimum color value used in lookup table */
    protected static final int MIN_COLOR = 0;

    /** maximum color value used in lookup table */
    protected static final int MAX_COLOR = 255;

    /** size of a lookup table for full short range */
    protected static final int LOOKUP_SIZE = 65536;

    /** minimum image value allowed */
    protected static final int LOOKUP_MIN = -32768;

    /** maximum image value allowed */
    protected static final int LOOKUP_MAX = 32767;


    /** Constant to pass to the scale method for linear scaling */
    public static final int LINEAR_SCALE = 0;

    /** Constant to pass to the scale method for square root scaling */
    public static final int SQRT_SCALE = 1;

    /** Constant to pass to the scale method for logarithmic scaling */
    public static final int LOG_SCALE = 2;

    /** Constant to pass to the scale method for histogram equalization scaling */
    public static final int HIST_EQ = 3;


    /** use to convert short data to byte using the selected algorithm */
    protected LookupTableJAI lookupTable;

    /** array used by lookup table */
    protected byte[] lookupArray;

    /** size of the lookup table */
    protected int lookupSize;

    /** lookup table offset (subtract from image value before lookup) */
    protected int lookupOffset;

    /** data type of the image */
    protected int dataType;

    /** low cut value scaled to ushort range */
    protected int scaledLowCut;

    /** high cut value scaled to ushort range */
    protected int scaledHighCut;


    /** default constructor */
    public ImageLookup() {
    }

    /** Return the generated lookup table */
    public LookupTableJAI getLookupTable() {
        return lookupTable;
    }

    /**
     * Scale the given image to short range, if needed, and return the new image.
     * The given low and high cut values are used to scale int and floating point
     * images to within short range so that a 64K lookup table may be used to scale
     * the image further down to byte range for display, using a chosen algorithm.
     * As a side effect, this method also notes the lookup table size and offsets,
     * which are used to create the lookup table later.
     */
    protected PlanarImage scaleToShortRange(PlanarImage im, double lowCut, double highCut) {
        dataType = im.getSampleModel().getDataType();
        lookupSize = LOOKUP_SIZE;
        lookupOffset = 0;	// subtracted from image value before lookup
        scaledLowCut = (int) lowCut;
        scaledHighCut = (int) highCut;

        switch (dataType) {

        case DataBuffer.TYPE_BYTE:
            lookupOffset = -128; // This is subtracted from each image value before lookup
            break;

        case DataBuffer.TYPE_USHORT:
            break;

        case DataBuffer.TYPE_SHORT:
            lookupOffset = LOOKUP_MIN;	// shift to unsigned short range (result must be int)
            break;

        case DataBuffer.TYPE_INT:
        case DataBuffer.TYPE_FLOAT:
        case DataBuffer.TYPE_DOUBLE:
            double scale = lookupSize / (highCut - lowCut); // scale to short range
            lookupOffset = LOOKUP_MIN;	// shift to unsigned short range (result must be int)
            double bias = -((lowCut + highCut) * 0.5) * scale;
            scaledLowCut = (int) (lowCut * scale + bias);
            scaledHighCut = (int) (highCut * scale + bias);
            // specify that the datatype of the rescaled image should be short
            RenderingHints hint = ImageUtil.getSampleModelHint(im.getTileWidth(),
                    im.getTileHeight(),
                    DataBuffer.TYPE_SHORT);
            im = ImageOps.rescale(im, scale, bias, hint);
            break;

        default:
            throw new RuntimeException("Unsupported image data type: " + dataType);
        }

        scaledLowCut -= lookupOffset;
        scaledHighCut -= lookupOffset;

        return im;
    }


    /**
     * Create an empty lookup table for the given image and scale the
     * image to unsigned short range, if necessary, so that we can use
     * a lookup table of at most 65536 bytes.
     * Note that we could base the lookup table size on the range of
     * image values, but that would require knowing the exact range,
     * making it impossible to estimate it efficiently.
     *
     * The return image may have been scaled to ushort range, if needed
     * (This is done for int and floating point images).
     */
    protected PlanarImage makeLookupTable(PlanarImage im, double lowCut, double highCut) {
        im = scaleToShortRange(im, lowCut, highCut);
        lookupArray = new byte[lookupSize];
        lookupTable = new LookupTableJAI(lookupArray, lookupOffset);
        return im;
    }


    /**
     * Set the values in the lookup table from imageVal to imageLim to the
     * given pixel value return the new imageVal index.
     */
    protected int fillLookupTable(int imageVal, int imageLim, int pixVal) {
        // limit to size of lookup table
        if (imageLim > lookupSize)
            imageLim = lookupSize;

        while (imageVal < imageLim) {
            lookupArray[imageVal++] = (byte) pixVal;
        }

        return imageVal;
    }

    /**
     * Install a lookup table to perform a color scale operation on the image using the given cut levels
     * and return the resulting image. Floating point images are first scaled to short range before
     * applying the lookup table.
     * This method only prepares the table and scales floating point images to the correct range if needed.
     * To apply the lookup table to the image, use can use the ImageOps.lookup() method.
     *
     * @param im the input image (for FITS, after applying BZERO and BSCALE, if needed)
     * @param scaleAlgorithm on of the constants defined in this class (LINEAR_SCALE, SQRT_SCALE, LOG_SCALE)
     * @param lowCut ignore image pixel values below this value
     * @param highCut ignore image pixel values above this value
     */
    public PlanarImage scale(PlanarImage im, int scaleAlgorithm, double lowCut, double highCut) {
        return scale(im, scaleAlgorithm, lowCut, highCut, null, null);
    }


    /**
     * Install a lookup table to perform a color scale operation on the image using the given cut levels
     * and return the resulting image. Floating point images are first scaled to short range before
     * applying the lookup table.
     * This method only prepares the table and scales floating point images to the correct range if needed.
     * To apply the lookup table to the image, use can use the ImageOps.lookup() method.
     *
     * @param im the input image (for FITS, after applying BZERO and BSCALE, if needed)
     * @param scaleAlgorithm on of the constants defined in this class (LINEAR_SCALE, SQRT_SCALE, LOG_SCALE, HIST_EQ)
     * @param lowCut ignore image pixel values below this value
     * @param highCut ignore image pixel values above this value
     * @param roi if set, this describes the region of interest to use for histogram equalization.
     * @param imageHistogram if set, this is used along with the ROI to generate the image histogram
     */
    public PlanarImage scale(PlanarImage im, int scaleAlgorithm, double lowCut, double highCut,
                             ROI roi, ImageHistogram imageHistogram) {
        switch (scaleAlgorithm) {
        case LINEAR_SCALE:
            return linearScale(im, lowCut, highCut);
        case SQRT_SCALE:
            return sqrtScale(im, lowCut, highCut);
        case LOG_SCALE:
            return logScale(im, lowCut, highCut);
        case HIST_EQ:
            if (roi != null && imageHistogram != null)
                return histEqScale(im, lowCut, highCut, roi, imageHistogram);
        default:
            return linearScale(im, lowCut, highCut);
        }
    }

    /**
     * Install a lookup table to perform a linear scale operation on
     * the image using the given cut levels and return the resulting
     * image. Floating point images are first scaled to short range
     * before applying the lookup table.  This method only prepares
     * the table and scales floating point images to the correct range
     * if needed.  To apply the lookup table to the image, use can use
     * the ImageOps.lookup() method.
     */
    public PlanarImage linearScale(PlanarImage im, double lowCut, double highCut) {
        im = makeLookupTable(im, lowCut, highCut);
        int imageVal = scaledLowCut;

        // input range / output range yields input cells per output cell
        double scale = (double) (scaledHighCut - scaledLowCut + 1) / NCOLORS;

        // upper bound is ideal edge between colors (offset for rounding)
        double upperBound = scaledLowCut + 0.5;

        int level = MIN_COLOR;
        int pixVal = level;
        int imageLim;

        while (level++ < MAX_COLOR) {
            upperBound += scale;
            imageLim = (int) upperBound;
            imageVal = fillLookupTable(imageVal, imageLim, pixVal);
            if (imageLim > lookupSize)
                break;
            pixVal = level;
        }

        // fill in at top if short of highCut
        fillLookupTable(imageVal, lookupSize, pixVal);

        return im;
    }


    /**
     * Install a lookup table to perform a sqare root scale operation
     * on the image using the given cut levels and return the
     * resulting image. Floating point images are first scaled to
     * short range before applying the lookup table.  This method only
     * prepares the table and scales floating point images to the
     * correct range if needed.  To apply the lookup table to the
     * image, use can use the ImageOps.lookup() method.
     */
    public PlanarImage sqrtScale(PlanarImage im, double lowCut, double highCut) {
        im = makeLookupTable(im, lowCut, highCut);
        int imageVal = scaledLowCut;
        int level = 0;
        int pixVal = 0;
        int imageLim;
        double range = scaledHighCut - scaledLowCut + 1;
        double expo = 10.;  // XXX should be a parameter

        while (level++ < MAX_COLOR) {
            imageLim = scaledLowCut + (int) ((Math.pow(((double) level) / MAX_COLOR, expo) * range) + 0.5);
            if (imageLim > scaledHighCut)
                imageLim = scaledHighCut;
            imageVal = fillLookupTable(imageVal, imageLim, pixVal);
            pixVal = level;
        }

        // fill in at top if short of highCut
        fillLookupTable(imageVal, lookupSize, pixVal);

        return im;
    }

    /**
     * Install a lookup table to perform a logarithmic scale operation
     * on the image using the given cut levels and return the
     * resulting image. Floating point images are first scaled to
     * short range before applying the lookup table.  This method only
     * prepares the table and scales floating point images to the
     * correct range if needed.  To apply the lookup table to the
     * image, use can use the ImageOps.lookup() method.
     */
    public PlanarImage logScale(PlanarImage im, double lowCut, double highCut) {
        im = makeLookupTable(im, lowCut, highCut);
        int imageVal = scaledLowCut;
        int level = 0;
        int pixVal = 0;
        int imageLim;
        double range = scaledHighCut - scaledLowCut + 1;
        double expo = 10.;  // XXX should be a parameter
        double scale;

        // base distribution on e**n as n goes from 0 to expo
        if (expo >= 0) {
            scale = range / (Math.exp(expo) - 1);
        }
        else {
            // negative exponents allocate more levels toward the high values
            scale = range / (1.0 - Math.exp(expo));
        }

        while (level++ < MAX_COLOR) {
            if (expo > 0) {
                imageLim = scaledLowCut + (int) (((Math.exp((((double) level) / MAX_COLOR) * expo) - 1) * scale) + 0.5);
            }
            else {
                imageLim = scaledLowCut + (int) ((1. - Math.exp((((double) level) / MAX_COLOR) * expo) * scale) + 0.5);
            }
            if (imageLim > scaledHighCut)
                imageLim = scaledHighCut;
            imageVal = fillLookupTable(imageVal, imageLim, pixVal);
            pixVal = level;
        }

        // fill in at top if short of highCut
        fillLookupTable(imageVal, lookupSize, pixVal);

        return im;
    }


    /**
     * Install a lookup table to perform a histogram equalization color
     * scale operation on the image using the given cut levels and ROI and
     * return the resulting image.
     * Floating point images are first scaled to short range
     * before applying the lookup table.  This method only prepares
     * the table and scales floating point images to the correct range
     * if needed.  To apply the lookup table to the image, use can use
     * the ImageOps.lookup() method.
     *
     * @param im the input image (for FITS, after applying BZERO and BSCALE, if needed)
     * @param lowCut ignore image pixel values below this value
     * @param highCut ignore image pixel values above this value
     * @param roi if set, this describes the region of interest to use for histogram equalization.
     * @param imageHistogram if set, this is used along with the ROI to generate the image histogram
     */
    public PlanarImage histEqScale(PlanarImage im, double lowCut, double highCut,
                                   ROI roi, ImageHistogram imageHistogram) {
        double n = highCut - lowCut;
        if (n < NCOLORS)
            return linearScale(im, lowCut, highCut);
        int numBins = 2048;
        if (n < numBins)
            numBins = (int) n;

        PlanarImage shortIm = makeLookupTable(im, lowCut, highCut);
        Histogram histogram = imageHistogram.getHistogram(im, numBins, lowCut, highCut,
                roi,
                ImageProcessor.DEFAULT_X_PERIOD,
                ImageProcessor.DEFAULT_Y_PERIOD);

        // find out the maximum number of pixels in a bin
        int[] bins = histogram.getBins(0);
        int binWidth = (int) (scaledHighCut - scaledLowCut) / numBins;
        if (binWidth == 0)
            binWidth = 1;
        int maxCount = 0;
        int totalCount = 0;
        for (int i = 0; i < numBins; i++) {
            totalCount += bins[i];
            if (bins[i] > maxCount)
                maxCount = bins[i];
        }
        if (maxCount == 0)
            return shortIm;

        // distribute the color values in the lookup table based on the histogram
        int imageVal = scaledLowCut;
        double scale = (double) numBins / NCOLORS;
        double upperBound = scaledLowCut + 0.5;
        int level = MIN_COLOR;
        int pixVal = level;
        int imageLim = 0;
        int binIndex = 0;
        int pixelsPerColor = totalCount / NCOLORS;

        //System.out.println("XXX lookup: binWidth = " + binWidth + ", maxCount = " + maxCount);
        //System.out.println("XXX lookup: scaledLowCut = " + scaledLowCut + ", scaledHighCut = " + scaledHighCut);
        //System.out.println("XXX lookup: lowCut = " + lowCut + ", highCut = " + highCut);

        while (level++ < MAX_COLOR) {
            int binCount = 0;
            do {
                binCount += bins[binIndex];
                //System.out.println("XXX lookup: bins[" + binIndex + "] = " + bins[binIndex]);
                upperBound += scale;
                if (binCount >= pixelsPerColor)
                    break;
            } while (binIndex++ < numBins);

            imageLim = (int) upperBound;
            //System.out.println("XXX lookup: " + imageVal + " to " + imageLim + " = " + pixVal);
            imageVal = fillLookupTable(imageVal, imageLim, pixVal);
            if (imageLim > lookupSize)
                break;
            pixVal = level;
        }

        // fill in at top if short of highCut
        fillLookupTable(imageVal, lookupSize, pixVal);

        return shortIm;
    }
}

