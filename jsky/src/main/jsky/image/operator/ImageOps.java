/*
 * ESO Archive
 *
 * $Id: ImageOps.java,v 1.3 2002/07/09 13:30:37 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/03  Created
 */

package jsky.image.operator;

import java.awt.image.renderable.*;
import java.awt.image.DataBuffer;
import javax.media.jai.*;
import java.awt.*;
import javax.media.jai.operator.TransposeType;


/**
 * Utility class for performing image operations.
 */
public class ImageOps {

    public ImageOps() {
    }


    /**
     * Perform a minMax operation on the image to get the min and max
     * pixel values.  Only the specified region of the image is
     * examined and any pixels with the given value are ignored. Any
     * NaN values are ignored. A value of Double.NaN may also be
     * specified for the bad pixel value, if none is known, although
     * in that case, the extrema operator may be faster, since it is
     * normally implemented in native code. The return value has type
     * double[2], for holding the min and max values. Note that this
     * operation currently only supports single banded images.
     */
    public static double[] minMax(PlanarImage im, ROI roi, int xPeriod, int yPeriod, double ignore) {
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(im);
        pb.add(roi);
        pb.add(xPeriod);
        pb.add(yPeriod);
        pb.add(ignore);

        im = JAI.create("minmax", pb, null);
        if (im != null) {
            Object o = im.getProperty("minmax");
            if (o != null)
                return (double[]) o;
        }
        return new double[2];
    }


    /**
     * Perform a CutLevel operation on the image to estimate the best
     * low and high cut levels using a median filter algorithm.  Only
     * the specified region of the image is examined and any pixels
     * with the given value are ignored and replaced with the given
     * median value, which should be (minValue+maxValue)/2. Any NaN
     * values are ignored. A value of Double.NaN may also be specified
     * for the bad pixel value, if none is known. The return value has
     * type double[2], for holding the min and max values. Note that
     * this operation currently only supports single banded images.
     */
    public static double[] cutLevel(PlanarImage im, ROI roi, double ignore, double median) {
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(im);
        pb.add(roi);
        pb.add(ignore);
        pb.add(median);

        im = JAI.create("cutlevel", pb, null);
        if (im != null) {
            Object o = im.getProperty("cutlevel");
            if (o != null)
                return (double[]) o;
        }
        return new double[2];
    }


    /**
     * Perform an extrema operation on the image to get the min and max pixel values.
     * Only the specified region of the image is examined. The return value has type
     * double[2][#bands], where the first index is for the min and max values in
     * each band.
     */
    public static double[][] extrema(PlanarImage im, ROI roi, int xPeriod, int yPeriod) {
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(im);
        pb.add(roi);
        pb.add(xPeriod);
        pb.add(yPeriod);

        im = JAI.create("extrema", pb, null);
        if (im != null) {
            Object o = im.getProperty("extrema");
            if (o != null)
                return (double[][]) o;
        }
        return new double[2][1];
    }

    /**
     * Perform a "mean" operation on the image to get the mean pixel value of
     * the given area of teh image.
     * Only the specified region of the image is examined. The return value has type
     * double[#bands].
     */
    public static double[] mean(PlanarImage im, ROI roi, int xPeriod, int yPeriod) {
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(im);
        pb.add(roi);
        pb.add(xPeriod);
        pb.add(yPeriod);

        im = JAI.create("mean", pb, null);
        if (im != null) {
            Object o = im.getProperty("mean");
            if (o != null)
                return (double[]) o;
        }
        return new double[2];
    }


    /**
     * Apply a rescale operation to the image.
     *
     * @param im the source image
     * @param factor factor to multiple pixel values by
     * @param offset value to add to pixel values
     * @param hints optional rendering hint (may be needed to define an ImageLayout object to change the
     *              datatype of the resulting image)
     */
    public static PlanarImage rescale(PlanarImage im, double factor, double offset, RenderingHints hints) {
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(im);
        double[] f = {factor};
        double[] o = {offset};
        pb.add(f);
        pb.add(o);
        return JAI.create("Rescale", pb, hints);
    }


    /**
     * Apply a format operation to the image to convert it to the given
     * data type (by casting and "clamping").
     */
    public static PlanarImage format(PlanarImage im, int dataType) {
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(im);
        pb.add(dataType);
        return JAI.create("Format", pb, null);
    }

    /**
     * Apply a scale operation to the image and return the
     * new image.
     */
    public static PlanarImage scale(PlanarImage im,
                                    float xScale, float yScale,
                                    float xTrans, float yTrans,
                                    Interpolation interpolation,
                                    RenderingHints hints) {
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(im);
        pb.add(xScale);
        pb.add(yScale);
        pb.add(xTrans);
        pb.add(yTrans);
        pb.add(interpolation);
        return JAI.create("Scale", pb, hints);
    }


    /**
     * Apply a rotate operation to the image and return the
     * new image. The
     */
    public static PlanarImage rotate(PlanarImage im,
                                     float x, float y, float angle,
                                     Interpolation interpolation,
                                     RenderingHints hints) {
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(im);
        pb.add(x);
        pb.add(y);
        pb.add(angle);
        pb.add(interpolation);
        return JAI.create("Rotate", pb, hints);
    }


    /**
     * Apply a crop operation to the image and return the
     * new image.
     */
    public static PlanarImage crop(PlanarImage im, float x, float y, float width, float height) {
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(im);
        pb.add(x);
        pb.add(y);
        pb.add(width);
        pb.add(height);
        return JAI.create("Crop", pb, null);
    }


    /**
     * Apply a translate operation to the image and return the
     * new image.
     */
    public static PlanarImage translate(PlanarImage im, float x, float y,
                                        Interpolation interpolation) {
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(im);
        pb.add(x);
        pb.add(y);
        pb.add(interpolation);
        return JAI.create("Translate", pb, null);
    }


    /**
     * Apply a transpose operation to the image and return the
     * new image.
     */
    public static PlanarImage transpose(PlanarImage im, TransposeType type) {
        return JAI.create("Transpose", im, type);
    }


    /**
     * Apply the lookup table to the given image and return the new image.
     */
    public static PlanarImage lookup(PlanarImage im, LookupTableJAI lookupTable) {
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(im);
        pb.add(lookupTable);
        return JAI.create("Lookup", pb, null);
    }


    /**
     * Perform a clamp operation on the image and return the new image.
     */
    public static PlanarImage clamp(PlanarImage im, double low, double high) {
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(im);
        double[] lowAr = {low};
        double[] highAr = {high};
        pb.add(lowAr);
        pb.add(highAr);
        return JAI.create("Clamp", pb, null);
    }

    /**
     * Get statistics on the given image, such as the min and max pixel
     * values.
     */
    public static Histogram histogram(PlanarImage im,
                                      ROI roi, int xPeriod, int yPeriod,
                                      int[] numBins, double lowValue[], double highValue[]) {
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(im);
        // XXX JAI-1.0.2 pb.add(histogram);
        pb.add(roi);
        pb.add(xPeriod);
        pb.add(yPeriod);
        pb.add(numBins);
        pb.add(lowValue);
        pb.add(highValue);

        im = JAI.create("Histogram", pb, null);
        return (Histogram) im.getProperty("histogram");
    }
}
