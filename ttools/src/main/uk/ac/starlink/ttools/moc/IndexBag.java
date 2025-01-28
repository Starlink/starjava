package uk.ac.starlink.ttools.moc;

import java.util.PrimitiveIterator;

/**
 * Interface for an object capable of storing a set of distinct integer values.
 * Depending on the implementation, there may be restrictions on the
 * range of indices that can be accommodated.
 *
 * @author   Mark Taylor
 * @since    28 Jan 2025
 */
public interface IndexBag {

    /**
     * Adds an index to this bag.
     * If the index is already present, there is no effect.
     *
     * <p>If the index is outside of the range permitted by this bag,
     * behaviour is undefined.
     *
     * @param  index  integer to add
     */
    void addIndex( long index );

    /**
     * Indicates whether the given index has previously been added to this bag.
     *
     * <p>If the index is outside of the range permitted by this bag,
     * behaviour is undefined.
     *
     * @param  index  integer to check
     * @return   true iff the given index has previously been added
     */
    boolean hasIndex( long index );

    /**
     * Returns an iterator over all the integers added to this bag,
     * supplied in ascending order.
     *
     * @return  sorted iterator over indices
     */
    PrimitiveIterator.OfLong sortedLongIterator();

    /**
     * Returns the number of distinct indices in this bag.
     *
     * @return  count of added integers
     */
    long getCount();
}
