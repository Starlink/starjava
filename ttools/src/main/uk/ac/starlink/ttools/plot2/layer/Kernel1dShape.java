package uk.ac.starlink.ttools.plot2.layer;

/**
 * Factory interface for for Kernel1d smoothing functional forms.
 *
 * <p>Some implementations are provided in the {@link StandardKernel1dShape}
 * class.
 *
 * @author   Mark Taylor
 * @since    2 Mar 2015
 */
public interface Kernel1dShape {

    /**
     * Returns a one-word name for this shape.
     *
     * @return  name
     */
    String getName();

    /**
     * Returns a short description for this shape.
     *
     * @return  description
     */
    String getDescription();

    /**
     * Creates a fixed width kernel with a given nominal width.
     * The width is some kind of characteristic half-width in one direction
     * of the smoothing function.  It is in units of grid points
     * (array element spacing).  It would generally be less than or
     * equal to the kernel's extent.
     *
     * @param  width  half-width
     * @return  new kernel
     */
    Kernel1d createFixedWidthKernel( double width );

    /**
     * Creates an adaptive kernel that uses a K-nearest-neighbours algorithm
     * to determine local smoothing width.
     *
     * @param  k  number of nearest neighbours included in the distance 
     *            that characterises the smoothing
     * @param  maxExtent   the maximum distance over which smoothing will
     *                     take place (only if k is never reached)
     * @return  new kernel
     */
    Kernel1d createKnnKernel( double k, int maxExtent );
}
