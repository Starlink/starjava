package uk.ac.starlink.topcat.plot2;

import uk.ac.starlink.ttools.plot2.Scale;
import uk.ac.starlink.ttools.plot2.Surface;

/**
 * Defines an object that can characterise a surface as an N-dimensional
 * hypercube.  Its methods are only intended for use on surfaces of
 * a particular type, evident from the context of its origin.
 *
 * @author   Mark Taylor
 * @since    3 Dec 2018
 */
public interface CartesianRanger {

    /**
     * Returns the dimensionality of the hypercube it can characterise.
     *
     * @return  ndim
     */
    int getDimCount();

    /**
     * Returns the limits in data coordinates
     * of the hypercube corresponding to a given plot surface.
     *
     * @param   surf  plot surface
     * @return  ndim-element array of 2-element arrays giving
     *          (lower,upper) bounds in data coordinates
     *          for each axis of hypercube
     */
    double[][] getDataLimits( Surface surf );

    /**
     * Indicates the scaling along the axes
     * of the hypercube corresponding to a given plot surface.
     *
     * @param   surf  plot surface
     * @return   ndim-element array giving scalings
     *           for each axis of hypercube
     */
    Scale[] getScales( Surface surf );

    /**
     * Returns the notional size in pixels for each axis
     * of the hypercube corresponding to a given plot surface.
     * Approximate values are OK, these values are just used to determine
     * numerical precisions for reported positions.
     *
     * @param  surf  plot surface
     * @return  ndim-element array giving notional pixel axis extents
     *          for each axis of hypercube
     */
    int[] getPixelDims( Surface surf );
}
