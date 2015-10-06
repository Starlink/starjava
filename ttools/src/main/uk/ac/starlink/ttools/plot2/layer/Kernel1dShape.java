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
     * to determine local smoothing width, so that the width of the kernel
     * is determined by the distance (number of 1-pixel bins) within which
     * the given number <code>k</code> of samples is found.
     *
     * <p>The nearest neighbour search may be symmetric or asymmetric.
     * In the asymmetric case, the kernel width is determined separately
     * for the positive and negative directions along the axis.
     *
     * <p>Minimum and maximum smoothing widths are also supplied as bounds
     * on the smoothing width for the case that the samples are very
     * dense or very spread out (the latter case covers the edge of the
     * data region as well).
     * If <code>minWidth==maxWidth</code>, the result is a fixed-width kernel.
     *
     * @param  k  number of nearest neighbours included in the distance 
     *            that characterises the smoothing
     * @param  isSymmetric  true for bidirectional KNN search,
     *                      false for unidirectional
     * @param  minWidth   minimum smoothing width
     * @param  maxWidth   maximum smoothing width
     * @return  new kernel
     */
    Kernel1d createKnnKernel( double k, boolean isSymmetric,
                              int minWidth, int maxWidth );
}
