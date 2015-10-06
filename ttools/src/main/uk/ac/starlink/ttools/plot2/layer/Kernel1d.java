package uk.ac.starlink.ttools.plot2.layer;

import uk.ac.starlink.ttools.plot2.Equality;

/**
 * Defines a smoothing function used for smoothing 1-dimensional
 * uniformly gridded data represented by an array.
 *
 * @author   Mark Taylor
 * @since    27 Feb 2015
 */
@Equality
public interface Kernel1d {

    /**
     * Returns the number of pixels in each direction over which the
     * central point will be smoothed.
     * If the extent is <i>e</i>,
     * only the (2<i>e</i>+1) input data values with indices
     * <i>x-e</i>..<i>x+e</i> will contribute to the data output
     * value with index <i>x</i>.
     *
     * @return  convolution extent half-width
     */
    int getExtent();

    /**
     * Applies this kernel to a data array.
     * Note, edge effects will cause distortion of the values within
     * <code>getExtent</code> pixels of the start and end of the
     * returned data array, so this method should be called on
     * an input array with sufficient padding at either end that
     * this effect can be ignored.
     *
     * @param  data  input data array
     * @return  output data array, same dimensions as input,
     *          but containing convolved data
     */
    double[] convolve( double[] data );

    /**
     * Indicates whether this kernel has features which are intentionally
     * non-smooth and should be portrayed as such.
     * This non-smoothness applies either within the extent or at its edge.
     *
     * @return   true iff there are non-smooth features that should be visible
     */
    boolean isSquare();
}
