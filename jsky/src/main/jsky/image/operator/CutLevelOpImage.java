/*
 * ESO Archive
 *
 * $Id: CutLevelOpImage.java,v 1.4 2002/07/09 13:30:37 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/03  Created
 * Peter W. Draper 2002/05/13  Added double precision support
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
 * CutLevelOpImage is an extension of StatisticsOpImage that takes
 * a region of interest (ROI), a bad pixel value to ignore, a median value to use
 * in place of bad pixels, and a source image, and calculates the low and high
 * image cut levels, using a median filter algorithm.
 * <p>
 * This class currently only works with single banded images.
 */
class CutLevelOpImage extends StatisticsOpImage {

    /** bad pixel value */
    private double ignore;

    /** Median pixel value to use in place of bad pixels */
    private double median;

    /**
     *  The operation names
     */
    private static final String[] opNames = {
        "cutlevel"
    };


    /**
     * Constructs an CutLevelOpImage.
     *
     * @param source    a RenderedImage.
     * @param layout    an ImageLayout (ignored here)
     * @param roi       The region of interest
     * @param ignore    ignore any pixels with this value
     * @param median    median value to replace bad pixels
     */
    public CutLevelOpImage(RenderedImage source, ImageLayout layout,
                           ROI roi, Double ignore, Double median) {
        // XXX JAI 1.0.2: super(source, roi, 0, 0, 1, 1, source.getWidth(), source.getHeight());
        super(source, roi, 0, 0, 1, 1);
        this.ignore = ignore.doubleValue();
        this.median = median.doubleValue();
    }


    /**
     * Update the low and high cut values for the specified region, using the current parameters.
     *
     * @param name the name of the statistic to be gathered.
     *
     * @param source a Raster containing source pixels.
     *               The dimensions of the Raster will not exceed maxWidth x maxHeight.
     *
     * @param ar an array of two doubles to hold the low and high cut values (created by createStatistics())
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
                short median = (short) this.median;
                getCutLevelsByte(data, ignore, median, x0, y0, x1, y1, w, stats);
            }
            break;

        case DataBuffer.TYPE_SHORT:
            {
                DataBufferShort dataBuffer = (DataBufferShort) source.getDataBuffer();
                short[] data = dataBuffer.getData();
                short ignore = (short) this.ignore;
                short median = (short) this.median;
                getCutLevelsShort(data, ignore, median, x0, y0, x1, y1, w, stats);
            }
            break;

        case DataBuffer.TYPE_USHORT:
            {
                DataBufferUShort dataBuffer = (DataBufferUShort) source.getDataBuffer();
                short[] data = dataBuffer.getData();
                int ignore = (int) this.ignore;
                int median = (int) this.median;
                getCutLevelsUShort(data, ignore, median, x0, y0, x1, y1, w, stats);
            }
            break;

        case DataBuffer.TYPE_INT:
            {
                DataBufferInt dataBuffer = (DataBufferInt) source.getDataBuffer();
                int[] data = dataBuffer.getData();
                int ignore = (int) this.ignore;
                int median = (int) this.median;
                getCutLevelsInt(data, ignore, median, x0, y0, x1, y1, w, stats);
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
                float median = (float) this.median;
                getCutLevelsFloat(data, ignore, median, x0, y0, x1, y1, w, stats);
            }
            break;

	case DataBuffer.TYPE_DOUBLE: 
            {
                Object dataBuffer = source.getDataBuffer();
                double[] data;
                if ( dataBuffer instanceof javax.media.jai.DataBufferDouble ) {
                    // JAI 1.1.1
                    data = ((javax.media.jai.DataBufferDouble) dataBuffer).getData();
                }
                else {
                    // JAI 1.1.2
                    data = ((java.awt.image.DataBufferDouble) dataBuffer).getData();
                }
                getCutLevelsDouble(data, ignore, median, x0, y0, x1, y1, w, stats);
            }
            break;

        default:
            throw new IllegalArgumentException("CutLevel not implemented for this data type");
        }
    }


    /**
     * Get the median low and high pixel values in the given region and write
     * them to the given CutLevel object (Byte version).
     *
     * A median filter algorithm is used here to calculate suitable cut
     * levels for displaying the image.
     *
     * @param data The image data.
     * @param ignore The value of the pixels to ignore
     * @param median The value to use for bad pixels (normally: (max+min)/2.)
     * @param x0, y0, x1, y1 The coordinates of the area to examine.
     * @param w The width of the source image.
     * @param stats array to hold the results.
     */
    void getCutLevelsByte(byte[] data, short ignore, short median, int x0, int y0, int x1, int y1, int w,
                          double[] stats) {

        int nmed = 7;		       // length of median filter
        int xskip = nmed * 3, yskip = 3;
        int i, j, k, l, p = 0;
        short tmp, val, lcut = 0, hcut = 0;
        short[] medary = new short[nmed];

        if (!Double.isNaN(stats[0])) {
            lcut = (short) stats[0];
            hcut = (short) stats[1];
        }
        else {
            lcut = median;
            hcut = median;
        }

        if (x1 - x0 <= nmed || y1 - y0 <= nmed)
            return;

        for (i = y0; i <= y1; i += yskip) {
            for (j = x0; j <= x1; j += xskip) {
                p = i * w + j;

                // get array for finding meadian
                for (k = 0; k < nmed; k++) {
                    medary[k] = (short) (data[p++] & 0xff);
                    // ignore ignore pixels
                    if (medary[k] == ignore) {
                        medary[k] = median;
                    }
                }

                // get meadian value
                for (k = 0; k < nmed; k++) {
                    for (l = k; l < nmed; l++) {
                        if (medary[k] < medary[l]) {
                            tmp = medary[l];
                            medary[l] = medary[k];
                            medary[k] = tmp;
                        }
                    }
                }
                val = medary[nmed / 2];

                // compare meadian with lcut, hcut
                if (val < lcut)
                    lcut = val;
                if (val > hcut)
                    hcut = val;
            }
        }
        stats[0] = lcut;
        stats[1] = hcut;
    }


    /**
     * Get the median low and high pixel values in the given region and write
     * them to the given array (Short version).
     *
     * A median filter algorithm is used here to calculate suitable cut
     * levels for displaying the image.
     *
     * @param data The image data.
     * @param ignore The value of the ignore pixel, if known.
     * @param median The value to use for bad pixels (normally: (max+min)/2.)
     * @param x0, y0, x1, y1 The coordinates of the area to examine.
     * @param w The width of the source image.
     * @param stats array to hold the results.
     */
    void getCutLevelsShort(short[] data, short ignore, short median, int x0, int y0, int x1, int y1, int w,
                           double[] stats) {
        int nmed = 7;		       // length of median filter
        int xskip = nmed * 3, yskip = 3; // skip pixels for speed
        int i, j, k, l, p = 0;
        short tmp, val, lcut = 0, hcut = 0;
        short[] medary = new short[nmed];

        if (!Double.isNaN(stats[0])) {
            lcut = (short) stats[0];
            hcut = (short) stats[1];
        }
        else {
            lcut = median;
            hcut = median;
        }

        x1 -= nmed;
        if (x1 - x0 <= nmed || y1 - y0 <= nmed)
            return;

        for (i = y0; i <= y1; i += yskip) {
            for (j = x0; j <= x1; j += xskip) {
                p = i * w + j;

                // get array for finding meadian
                for (k = 0; k < nmed; k++) {
                    medary[k] = data[p++];
                    // ignore ignore pixels
                    if (medary[k] == ignore) {
                        medary[k] = median;
                    }
                }

                // get meadian value
                for (k = 0; k < nmed; k++) {
                    for (l = k; l < nmed; l++) {
                        if (medary[k] < medary[l]) {
                            tmp = medary[l];
                            medary[l] = medary[k];
                            medary[k] = tmp;
                        }
                    }
                }
                val = medary[nmed / 2];

                // compare meadian with lcut, hcut
                if (val < lcut)
                    lcut = val;
                if (val > hcut)
                    hcut = val;
            }
        }
        stats[0] = lcut;
        stats[1] = hcut;
    }

    /**
     * Get the median low and high pixel values in the given region and write
     * them to the given array (UShort version).
     *
     * A median filter algorithm is used here to calculate suitable cut
     * levels for displaying the image.
     *
     * @param data The image data.
     * @param ignore The value of the ignore pixel, if known.
     * @param median The value to use for bad pixels (normally: (max+min)/2.)
     * @param x0, y0, x1, y1 The coordinates of the area to examine.
     * @param w The width of the source image.
     * @param stats array to hold the results.
     */
    void getCutLevelsUShort(short[] data, int ignore, int median, int x0, int y0, int x1, int y1, int w,
                            double[] stats) {
        int nmed = 7;		       // length of median filter
        int xskip = nmed * 3, yskip = 3; // skip pixels for speed
        int i, j, k, l, p = 0;
        int tmp, val, lcut = 0, hcut = 0;
        int[] medary = new int[nmed];

        if (!Double.isNaN(stats[0])) {
            lcut = (int) stats[0];
            hcut = (int) stats[1];
        }
        else {
            lcut = median;
            hcut = median;
        }

        if (x1 - x0 <= nmed || y1 - y0 <= nmed)
            return;

        for (i = y0; i <= y1; i += yskip) {
            for (j = x0; j <= x1; j += xskip) {
                p = i * w + j;

                // get array for finding meadian
                for (k = 0; k < nmed; k++) {
                    medary[k] = (int) (data[p++] & 0xffff);
                    // ignore ignore pixels
                    if (medary[k] == ignore) {
                        medary[k] = median;
                    }
                }

                // get meadian value
                for (k = 0; k < nmed; k++) {
                    for (l = k; l < nmed; l++) {
                        if (medary[k] < medary[l]) {
                            tmp = medary[l];
                            medary[l] = medary[k];
                            medary[k] = tmp;
                        }
                    }
                }
                val = medary[nmed / 2];

                // compare meadian with lcut, hcut
                if (val < lcut)
                    lcut = val;
                if (val > hcut)
                    hcut = val;
            }
        }
        stats[0] = lcut;
        stats[1] = hcut;
    }


    /**
     * Get the median low and high pixel values in the given region and write
     * them to the given array (Int version).
     *
     * A median filter algorithm is used here to calculate suitable cut
     * levels for displaying the image.
     *
     * @param data The image data.
     * @param ignore The value of the ignore pixel, if known.
     * @param median The value to use for bad pixels (normally: (max+min)/2.)
     * @param x0, y0, x1, y1 The coordinates of the area to examine.
     * @param w The width of the source image.
     * @param stats array to hold the results.
     */
    void getCutLevelsInt(int[] data, int ignore, int median, int x0, int y0, int x1, int y1, int w,
                         double[] stats) {
        int nmed = 7;		       // length of median filter
        int xskip = nmed * 3, yskip = 3; // skip pixels for speed
        int i, j, k, l, p = 0;
        int tmp, val, lcut = 0, hcut = 0;
        int[] medary = new int[nmed];

        if (!Double.isNaN(stats[0])) {
            lcut = (int) stats[0];
            hcut = (int) stats[1];
        }
        else {
            lcut = median;
            hcut = median;
        }

        if (x1 - x0 <= nmed || y1 - y0 <= nmed)
            return;

        for (i = y0; i <= y1; i += yskip) {
            for (j = x0; j <= x1; j += xskip) {
                p = i * w + j;

                // get array for finding meadian
                for (k = 0; k < nmed; k++) {
                    medary[k] = data[p++];
                    // ignore ignore pixels
                    if (medary[k] == ignore) {
                        medary[k] = median;
                    }
                }

                // get meadian value
                for (k = 0; k < nmed; k++) {
                    for (l = k; l < nmed; l++) {
                        if (medary[k] < medary[l]) {
                            tmp = medary[l];
                            medary[l] = medary[k];
                            medary[k] = tmp;
                        }
                    }
                }
                val = medary[nmed / 2];

                // compare meadian with lcut, hcut
                if (val < lcut)
                    lcut = val;
                if (val > hcut)
                    hcut = val;
            }
        }
        stats[0] = lcut;
        stats[1] = hcut;
    }


    /**
     * Get the median low and high pixel values in the given region and write
     * them to the given array (Float version).
     *
     * A median filter algorithm is used here to calculate suitable cut
     * levels for displaying the image.
     *
     * @param data The image data.
     * @param ignore The value of the ignore pixel, if known.
     * @param median The value to use for bad pixels (normally: (max+min)/2.)
     * @param x0, y0, x1, y1 The coordinates of the area to examine.
     * @param w The width of the source image.
     * @param stats array to hold the results.
     */
    void getCutLevelsFloat(float[] data, float ignore, float median, int x0, int y0, int x1, int y1, int w,
                           double[] stats) {
        int nmed = 7;		       // length of median filter
        int xskip = nmed * 3, yskip = 3; // skip pixels for speed
        int i, j, k, l, p = 0;
        float tmp, val, lcut = 0.0f, hcut = 0.0f;
        float[] medary = new float[nmed];

        if (!Double.isNaN(stats[0])) {
            lcut = (float) stats[0];
            hcut = (float) stats[1];
        }
        else {
            lcut = median;
            hcut = median;
        }

        if (x1 - x0 <= nmed || y1 - y0 <= nmed)
            return;

        for (i = y0; i <= y1; i += yskip) {
            for (j = x0; j <= x1; j += xskip) {
                p = i * w + j;

                // get array for finding meadian
                for (k = 0; k < nmed; k++) {
                    medary[k] = data[p++];
                    // ignore ignore pixels
                    if (Float.isNaN(medary[k]) || (medary[k] == ignore)) {
                        medary[k] = median;
                    }
                }

                // get meadian value
                for (k = 0; k < nmed; k++) {
                    for (l = k; l < nmed; l++) {
                        if (medary[k] < medary[l]) {
                            tmp = medary[l];
                            medary[l] = medary[k];
                            medary[k] = tmp;
                        }
                    }
                }
                val = medary[nmed / 2];

                // compare meadian with lcut, hcut
                if (val < lcut)
                    lcut = val;
                if (val > hcut)
                    hcut = val;
            }
        }
        stats[0] = lcut;
        stats[1] = hcut;
    }

    /**
     * Get the median low and high pixel values in the given region and write
     * them to the given array (Double version).
     * 
     * A median filter algorithm is used here to calculate suitable cut
     * levels for displaying the image.
     *
     * @param data The image data.
     * @param ignore The value of the ignore pixel, if known.
     * @param median The value to use for bad pixels (normally: (max+min)/2.)
     * @param x0, y0, x1, y1 The coordinates of the area to examine.
     * @param w The width of the source image.
     * @param stats array to hold the results.
     */
    void getCutLevelsDouble( double[] data, double ignore, double median, 
                             int x0, int y0, int x1, int y1, int w, 
                             double[] stats ) {
	int nmed = 7;		       // length of median filter
	int xskip = nmed*3, yskip = 3; // skip pixels for speed 
	int i, j, k, l, p=0;
	double tmp, val, lcut = 0.0f, hcut = 0.0f; 
	double [] medary = new double[nmed];

	if (! Double.isNaN(stats[0])) {
	    lcut = (float)stats[0];
	    hcut = (float)stats[1];
	}
	else {
	    lcut = median;
	    hcut = median;
	}
        
	if (x1-x0 <= nmed || y1-y0 <= nmed)
	    return;	

	for (i=y0; i<=y1; i+=yskip) {
	    for (j=x0; j<=x1; j+=xskip) {
		p = i*w + j;

		// get array for finding meadian
		for (k=0; k < nmed; k++) {
		    medary[k] = data[p++];
		    // ignore ignore pixels
		    if (Double.isNaN(medary[k]) || (medary[k] == ignore)) {
			medary[k] = median;  
		    }
		}

		// get meadian value 
		for (k=0; k < nmed; k++) {
		    for (l=k; l < nmed; l++) {
			if (medary[k] < medary[l]) {
			    tmp = medary[l];
			    medary[l] = medary[k];
			    medary[k] = tmp;
			}
		    }
		}
		val = medary[nmed/2];

		// compare meadian with lcut, hcut
		if (val < lcut) 
		    lcut = val;
		if (val > hcut)
		    hcut = val;
	    }
	}
	stats[0] = lcut;
	stats[1] = hcut;
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
