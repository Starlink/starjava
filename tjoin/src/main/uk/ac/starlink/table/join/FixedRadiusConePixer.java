package uk.ac.starlink.table.join;

/**
 * Can calculate which sky pixels fall within a cone of variable radius.
 *
 * @author   Mark Taylor
 * @since    10 May 2022
 */
public interface FixedRadiusConePixer {

    /**
     * Returns an array of objects representing pixels in a fixed-radius cone
     * around a given sky position.
     * Any pixels overlapping the cone must be returned;
     * additional pixels (false positives) may also be returned.
     * The output objects must implement
     * the <code>equals</code> and <code>hashCode</code> methods
     * appropriately, so that objects returned from one call can be
     * compared for identity with objects returned from a subsequent call.
     *
     * <p>Instances of this class are not thread-safe, and should not be
     * used concurrently from multiple threads.
     *
     * @param  alpha  right ascension of circle centre in radians
     * @param  delta  declination of circle centre in radians
     * @return   array of comparable pixel objects
     */
    Object[] getPixels( double alpha, double delta );
}
