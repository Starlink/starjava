/*
 * ESO Archive
 *
 * $Id: MinMaxOpImage.java,v 1.4 2002/07/09 13:30:37 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/03  Created
 * Peter W. Draper 2002/10/01  Added double precision support
 * Mark Taylor     2002/10/01  Fixed selection of initial minimum value
 *                             when bad values are present
 * Mark Taylor     2003/06/11  Modified for JAI 1.1.2 compatibility
 */

package jsky.image.operator;

import java.awt.geom.Rectangle2D;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;

import javax.media.jai.ImageLayout;
import javax.media.jai.ROI;
import javax.media.jai.StatisticsOpImage;


/**
 * This operation is used to get the min and max pixel values, like the JAI extrema
 * operation, except that it ignores pixels with a given bad pixel value and is designed
 * (currently) only to work with single banded images. The bad pixel value is specified
 * as a double. If the value is Double.NaN, it is ignored. Also, any pixel values
 * that are NaNs are ignored.
 * <p>
 * MinMaxOpImage is an extension of StatisticsOpImage that takes
 * a region of interest (ROI), two integer parameters (xPeriod and yPeriod),
 * a bad pixel value (ignore), and one source image and calculates image min
 * and max values, ignoring any bad pixels.
 */
class MinMaxOpImage extends StatisticsOpImage {

    // Note: abount the base class, Daniel Rice <Daniel.Rice@Eng.Sun.COM> wrote:
    // xStart, yStart, xPeriod, and yPeriod define a uniform but sparse set of
    // sample points. You might want to accumulate statistics on several regions
    // of interest, but each time using the same subgrid for consistency.
    // maxWidth and maxHeight don't affect the results, they just set up a contract
    // with the future caller of accumulateStatictics which may be of use to the
    // implementer of a particular statistics op who need to allocate temporary
    // storage.

    /** value of the pixels to ignore */
    private double ignore;

    /**
     *  The statistics operation names
     */
    private static final String[] opNames = {
        "minmax"
    };


    /**
     * Constructs an MinMaxOpImage.
     *
     * @param source    a RenderedImage.
     * @param layout    an ImageLayout (ignored here)
     * @param roi       The region of interest
     * @param xPeriod   skip this many pixels in the X direction
     * @param yPeriod   skip this many pixels in the Y direction
     * @param ignore    ignore any pixels with this value
     */
    public MinMaxOpImage(RenderedImage source, ImageLayout layout,
                         ROI roi, Integer xPeriod, Integer yPeriod,
                         Double ignore) {
        // XXX JAI 1.0.2 super(source, roi, 0, 0, xPeriod.intValue(), yPeriod.intValue(), source.getWidth(), source.getHeight());
        super(source, roi, 0, 0, xPeriod.intValue(), yPeriod.intValue());
        this.ignore = ignore.doubleValue();
    }


    /**
     * Update the min and max values for the specified region, using the current parameters.
     *
     * @param name the name of the statistic to be gathered.
     *
     * @param source a Raster containing source pixels.
     *               The dimensions of the Raster will not exceed maxWidth x maxHeight.
     *
     * @param ar an array of two doubles to hold the min and max values (created by createStatistics())
     */
    protected void accumulateStatistics(String name, Raster source, Object ar) {

        double[] stats = (double[]) ar;
        DataBuffer dbuf = source.getDataBuffer();

        // clip the region to the intersection of the ROI with the source tile
        Rectangle2D rect = roi.getBounds().createIntersection(source.getBounds());
        //System.out.println("XXX accumulateStatistics: ROI = " + roi.getBounds() + ", source = " + source.getBounds() + ", intersect = " + rect);

        int x0 = Math.max((int) rect.getX() - source.getMinX(), 0);
        int y0 = Math.max((int) rect.getY() - source.getMinY(), 0);
        int x1 = x0 + (int) rect.getWidth() - 1;
        int y1 = y0 + (int) rect.getHeight() - 1;
        int w = source.getWidth();
        int h = source.getHeight();

        // ignore pixels from the border
        if (xPeriod < width / 2 && yPeriod < height / 2) {
            x0 += xPeriod;
            y0 += yPeriod;
            x1 -= xPeriod;
            y1 -= yPeriod;
        }

        // XXX for now, only do the default bank. (How to treat multiple banks?)
        switch (dbuf.getDataType()) {

        case DataBuffer.TYPE_BYTE:
            {
                DataBufferByte dataBuffer = (DataBufferByte) source.getDataBuffer();
                byte[] data = dataBuffer.getData();
                short ignore = (short) this.ignore;
                getMinMaxByte(data, ignore, x0, y0, x1, y1, w, stats);
            }
            break;

        case DataBuffer.TYPE_SHORT:
            {
                DataBufferShort dataBuffer = (DataBufferShort) source.getDataBuffer();
                short[] data = dataBuffer.getData();
                short ignore = (short) this.ignore;
                getMinMaxShort(data, ignore, x0, y0, x1, y1, w, stats);
            }
            break;

        case DataBuffer.TYPE_USHORT:
            {
                DataBufferUShort dataBuffer = (DataBufferUShort) source.getDataBuffer();
                short[] data = dataBuffer.getData();
                int ignore = (int) this.ignore;
                getMinMaxUShort(data, ignore, x0, y0, x1, y1, w, stats);
            }
            break;

        case DataBuffer.TYPE_INT:
            {
                DataBufferInt dataBuffer = (DataBufferInt) source.getDataBuffer();
                int[] data = dataBuffer.getData();
                int ignore = (int) this.ignore;
                getMinMaxInt(data, ignore, x0, y0, x1, y1, w, stats);
            }
            break;

        case DataBuffer.TYPE_FLOAT:
            {
                Object dataBuffer = source.getDataBuffer();
                float[] data;
                if ( dataBuffer instanceof javax.media.jai.DataBufferFloat ) {
                    // JAI 1.1.1
                    data = ((javax.media.jai.DataBufferFloat) dataBuffer).getData();
                }
                else {
                    // JAI 1.1.2
                    data = ((java.awt.image.DataBufferFloat) dataBuffer).getData();
                }
                float ignore = (float) this.ignore;
                getMinMaxFloat(data, ignore, x0, y0, x1, y1, w, stats);
            }
            break;

        case DataBuffer.TYPE_DOUBLE:
            {
                Object dataBuffer = source.getDataBuffer();
                double[] data;
                if ( dataBuffer instanceof javax.media.jai.DataBufferDouble ) {
                    // JAI 1.1.1.
                    data = ((javax.media.jai.DataBufferDouble) dataBuffer).getData();
                }
                else {
                    // JAI 1.1.2
                    data = ((java.awt.image.DataBufferDouble) dataBuffer).getData();
                }
                double ignore = (double) this.ignore;
                getMinMaxDouble(data, ignore, x0, y0, x1, y1, w, stats);
            }
            break;

        default:
            throw new IllegalArgumentException("MinMax not implemented for this data type");
        }
    }

    /**
     * Get the min and max pixel values in the given region and write
     * them to the given array (Byte version).
     *
     * @param data The image data.
     * @param ignore The value of the pixels to ignore
     * @param x0, y0, x1, y1 The coordinates of the area to examine.
     * @param w The width of the source image.
     * @param stats array to hold the results.
     */
    void getMinMaxByte(byte[] data, short ignore, int x0, int y0, int x1, int y1, int w,
                       double[] stats) {
        short min, max;
        if (!Double.isNaN(stats[0])) {
            min = (short) stats[0];
            max = (short) stats[1];
        }
        else {
            min = data[0];
            // check for ignores
            if (min == ignore) {
                done:
                for (int i = x0; i <= x1; i += xPeriod) {
                    for (int j = y0; j <= y1; j += yPeriod) {
                        min = data[j * w + i];
                        if (min == ignore)
                            continue;
                        break done;
                    }
                }
            }
            if (min == ignore) {
                min = 0;
            }
            max = min;
        }

        for (int i = x0; i <= x1; i += xPeriod) {
            for (int j = y0; j <= y1; j += yPeriod) {
                byte val = data[j * w + i];
                if (val == ignore)
                    continue;
                if (val < min)
                    min = val;
                else if (val > max) {
                    max = val;
                }
            }
        }
        stats[0] = min;
        stats[1] = max;
    }

    /**
     * Get the min and max pixel values in the given region and write
     * them to the given array (Short version).
     *
     * @param data The image data.
     * @param ignore The value of the pixels to ignore
     * @param x0, y0, x1, y1 The coordinates of the area to examine.
     * @param w The width of the source image.
     * @param stats array to hold the results.
     */
    void getMinMaxShort(short[] data, short ignore, int x0, int y0, int x1, int y1, int w,
                        double[] stats) {

        short min, max;
        if (!Double.isNaN(stats[0])) {
            min = (short) stats[0];
            max = (short) stats[1];
        }
        else {
            min = data[0];
            // check for ignores
            if (min == ignore) {
                done:
                for (int i = x0; i <= x1; i += xPeriod) {
                    for (int j = y0; j <= y1; j += yPeriod) {
                        min = data[j * w + i];
                        if (min == ignore)
                            continue;
                        break done;
                    }
                }
            }
            if (min == ignore) {
                min = 0;
            }
            max = min;
        }

        for (int i = x0; i <= x1; i += xPeriod) {
            for (int j = y0; j <= y1; j += yPeriod) {
                short val = data[j * w + i];
                if (val == ignore)
                    continue;
                if (val < min)
                    min = val;
                else if (val > max) {
                    max = val;
                }
            }
        }
        stats[0] = min;
        stats[1] = max;
    }

    /**
     * Get the min and max pixel values in the given region and write
     * them to the given array (UShort version).
     *
     * @param data The image data.
     * @param ignore The value of the pixels to ignore
     * @param x0, y0, x1, y1 The coordinates of the area to examine.
     * @param w The width of the source image.
     * @param stats array to hold the results.
     */
    void getMinMaxUShort(short[] data, int ignore, int x0, int y0, int x1, int y1, int w,
                         double[] stats) {

        int min, max;
        if (!Double.isNaN(stats[0])) {
            min = (int) stats[0];
            max = (int) stats[1];
        }
        else {
            min = data[0] & 0xffff;

            // check for ignores
            if (min == ignore) {
                done:
                for (int i = x0; i <= x1; i += xPeriod) {
                    for (int j = y0; j <= y1; j += yPeriod) {
                        min = data[j * w + i];
                        if (min == ignore)
                            continue;
                        break done;
                    }
                }
            }
            if (min == ignore) {
                min = 0;
            }
            max = min;
        }

        for (int i = x0; i <= x1; i += xPeriod) {
            for (int j = y0; j <= y1; j += yPeriod) {
                int val = data[j * w + i] & 0xffff;
                if (val == ignore)
                    continue;
                if (val < min)
                    min = val;
                else if (val > max) {
                    max = val;
                }
            }
        }
        stats[0] = min;
        stats[1] = max;
    }


    /**
     * Get the min and max pixel values in the given region and write
     * them to the given array (Int version).
     *
     * @param data The image data.
     * @param ignore The value of the pixels to ignore
     * @param x0, y0, x1, y1 The coordinates of the area to examine.
     * @param w The width of the source image.
     * @param stats array to hold the results.
     */
    void getMinMaxInt(int[] data, int ignore, int x0, int y0, int x1, int y1, int w,
                      double[] stats) {
        int min, max;
        if (!Double.isNaN(stats[0])) {
            min = (int) stats[0];
            max = (int) stats[1];
        }
        else {
            min = data[0];
            // check for ignores
            if (min == ignore) {
                done:
                for (int i = x0; i <= x1; i += xPeriod) {
                    for (int j = y0; j <= y1; j += yPeriod) {
                        min = data[j * w + i];
                        if (min == ignore)
                            continue;
                        break done;
                    }
                }
            }
            if (min == ignore) {
                min = 0;
            }
            max = min;
        }

        for (int i = x0; i <= x1; i += xPeriod) {
            for (int j = y0; j <= y1; j += yPeriod) {
                int val = data[j * w + i];
                if (val == ignore)
                    continue;
                if (val < min)
                    min = val;
                else if (val > max) {
                    max = val;
                }
            }
        }
        stats[0] = min;
        stats[1] = max;
    }

    /**
     * Get the min and max pixel values in the given region and write
     * them to the given array (Float version).
     *
     * @param data The image data.
     * @param ignore The value of the pixels to ignore
     * @param x0, y0, x1, y1 The coordinates of the area to examine.
     * @param w The width of the source image.
     * @param stats array to hold the results.
     */
    void getMinMaxFloat(float[] data, float ignore, int x0, int y0, int x1, int y1, int w,
                        double[] stats) {
        float min, max;
        if (!Double.isNaN(stats[0])) {
            min = (float) stats[0];
            max = (float) stats[1];
        }
        else {
            min = data[0];

            // check for NaNs and ignores
            if (Float.isNaN(min) || (min == ignore)) {
                done:
                for (int i = x0; i <= x1; i += xPeriod) {
                    for (int j = y0; j <= y1; j += yPeriod) {
                        min = data[j * w + i];
                        if (Float.isNaN(min) || (min == ignore))
                            continue;
                        break done;
                    }
                }
            }
            if (Float.isNaN(min) || (min == ignore)) {
                min = 0.0f;
            }
            max = min;
        }

        for (int i = x0; i <= x1; i += xPeriod) {
            for (int j = y0; j <= y1; j += yPeriod) {
                float val = data[j * w + i];
                if (Float.isNaN(val) || (val == ignore))
                    continue;
                if (val < min)
                    min = val;
                else if (val > max) {
                    max = val;
                }
            }
        }
        stats[0] = min;
        stats[1] = max;
    }

    /**
     * Get the min and max pixel values in the given region and write
     * them to the given array (Double version).
     *
     * @param data The image data.
     * @param ignore The value of the pixels to ignore
     * @param x0, y0, x1, y1 The coordinates of the area to examine.
     * @param w The width of the source image.
     * @param stats array to hold the results.
     */
    void getMinMaxDouble(double[] data, double ignore, int x0, int y0, int x1, int y1, int w,
                         double[] stats) {
        double min, max;
        if (!Double.isNaN(stats[0])) {
            min = stats[0];
            max = stats[1];
        }
        else {
            min = data[0];

            // check for NaNs and ignores
            if (Double.isNaN(min) || (min == ignore)) {
                done:
                for (int i = x0; i <= x1; i += xPeriod) {
                    for (int j = y0; j <= y1; j += yPeriod) {
                        min = data[j * w + i];
                        if (Double.isNaN(min) || (min == ignore))
                            continue;
                        break done;
                    }
                }
            }
            if (Double.isNaN(min) || (min == ignore)) {
                min = 0.0;
            }
            max = min;
        }

        for (int i = x0; i <= x1; i += xPeriod) {
            for (int j = y0; j <= y1; j += yPeriod) {
                double val = data[j * w + i];
                if (Double.isNaN(val) || (val == ignore))
                    continue;
                if (val < min)
                    min = val;
                else if (val > max) {
                    max = val;
                }
            }
        }
        stats[0] = min;
        stats[1] = max;
    }


    /**
     * Returns an object that will be used to gather the named statistic.
     *
     * @param name the name of the statistic to be gathered.
     */
    protected Object createStatistics(java.lang.String name) {
        double[] ar = new double[2];
        ar[0] = ar[1] = Double.NaN; // initial values are undefined
        return ar;
    }

    /**
     * Returns a list of names of statistics understood by this image.
     */
    protected String[] getStatisticsNames() {
        return opNames;
    }
}
