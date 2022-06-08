package uk.ac.starlink.table.join;

/**
 * Represents an area on the sky using HEALPix tesselation.
 * On construction, the area is empty.
 *
 * <p>This interface defines what is required for use by the
 * {@link SkyCoverage} class.
 * The defined behaviour is somewhat like a MOC,
 * and can be implemented using a MOC, but standard MOC implementations
 * may not have suitable performance characteristics;
 * in particular the {@link #addPixel} method ought to be fast.
 *
 * @author   Mark Taylor
 * @since    8 Jun 2022
 */
public interface HealpixMask {

    /**
     * Returns true if this mask's area is empty.
     *
     * @return  true iff the {@link #createPixelTester} test
     *          is guaranteed to return false
     */
    boolean isEmpty();

    /**
     * Narrows this area to represent the intersection of this mask
     * and another compatible mask.
     *
     * @param  other  different mask of a type assumed compatible with this one
     */
    void intersection( HealpixMask other );

    /**
     * Extends this area to represent the union of this mask
     * and another compatible mask.
     *
     * @param  other  different mask of a type assumed compatible with this one
     */
    void union( HealpixMask other );

    /**
     * Returns the fraction of the sky currently covered by this mask.
     *
     * @return  sky fraction between 0 and 1
     */
    double getSkyFraction();

    /**
     * Adds the area corresponding to a HEALPix pixel to this mask.
     *
     * @param  order  HEALPix order
     * @param  ipix  HEALPix pixel index at order <code>order</code>
     */
    void addPixel( int order, long ipix );

    /**
     * Returns an object that can test inclusion in the sky area defined
     * by the current state of this mask.
     *
     * @return  thread-safe test for sky area inclusion
     */
    PixelTester createPixelTester();

    /**
     * Defines a way to test inclusion of HEALPix pixels in an area.
     */
    @FunctionalInterface
    public interface PixelTester {

        /**
         * Tests whether an area contains all or part of a given
         * HEALPix pixel.
         *
         * <p>Note this method must be thread-safe, it may be called
         * from multiple threads concurrently.
         *
         * @param  order  HEALPix order
         * @param  ipix  HEALPix pixel index at order <code>order</code>
         * @return  true iff the indicated pixel is wholly or partially
         *          covered by the area; false positives are permitted
         */
        boolean containsPixel( int order, long ipix );
    }
}
