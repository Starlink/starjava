package uk.ac.starlink.ttools.filter;

/**
 * Defines an ordering of integers.
 * This is just like {@link java.util.Comparator} but the items compared
 * are primitive integer values rather than Objects.
 *
 * @author   Mark Taylor
 * @since    3 Mar 2025
 */
@FunctionalInterface
public interface IntComparator {

    /**
     * Compares its two arguments for order.
     * Returns a negative integer, zero, or a positive integer
     * as the first argument is considered less than, equal to,
     * or greater than the second.
     *
     * @param   i1  first integer to compare
     * @param   i2  second integer to compare
     * @return  a negative integer, zero, or a positive integer
     *          depending on comparison
     */
    public int compare( int i1, int i2 );
}
