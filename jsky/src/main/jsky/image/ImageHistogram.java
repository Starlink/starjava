/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: ImageHistogram.java,v 1.2 2002/07/09 13:30:36 brighton Exp $
 */

package jsky.image;

import javax.media.jai.Histogram;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;

import jsky.image.operator.ImageOps;


/**
 * Utility class used to avoid creating the same Histogram twice
 * (Once for auto-setting cut levels and once for histogram equalization, if selected).
 * The previous arguments and resulting histogram are cached and compared the
 * next time getHistogram is called. If the arguments have not changed, the cached
 * Histogram is returned.
 */
public class ImageHistogram {

    private Histogram _histogram;
    private PlanarImage _im;
    private int _size;
    private double _lowCut;
    private double _highCut;
    private ROI _roi;
    private int _xPeriod;
    private int _yPeriod;

    /** Default Constructor */
    public ImageHistogram() {
    }

    /**
     * Return a histogram for the given image with the given size (number of bins).
     * The arguments are the same as those for the JAI histogram image operation.
     */
    public Histogram getHistogram(PlanarImage im, int size, double lowCut,
                                  double highCut, ROI roi, int xPeriod, int yPeriod) {

        // check if we can use the cached histogram again
        if (_histogram != null
                && im == _im
                && size == _size
                && lowCut == _lowCut
                && highCut == _highCut
                && roi.equals(_roi)
                && xPeriod == _xPeriod
                && yPeriod == _yPeriod) {
            return _histogram;
        }
        _im = im;
        _size = size;
        _lowCut = lowCut;
        _highCut = highCut;
        _roi = roi;
        _xPeriod = xPeriod;
        _yPeriod = yPeriod;

        int numBands = im.getSampleModel().getNumBands();

        int[] numBins = new int[numBands];
        double[] lowValue = new double[numBands];
        double[] highValue = new double[numBands];

        for (int i = 0; i < numBands; i++) {
            numBins[i] = size;
            lowValue[i] = lowCut;
            highValue[i] = highCut;
        }

        _histogram = ImageOps.histogram(im, roi, xPeriod, yPeriod, numBins, lowValue, highValue);
        return _histogram;
    }
}
