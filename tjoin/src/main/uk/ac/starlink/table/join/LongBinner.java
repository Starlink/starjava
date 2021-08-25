package uk.ac.starlink.table.join;

import java.util.Iterator;

/**
 * Map which can store lists of <code>long</code> integer values.
 * The keys of the map can be viewed as identifiers of bins, and each
 * bin stores one or more integers.  Items are added one at a time, with
 * bins being created as required.
 *
 * @author   Mark Taylor
 * @since    28 Jan 2010
 */
interface LongBinner {

    /**
     * Adds an integer item.
     *
     * @param  key  bin key
     * @param  item  value to add to list in bin
     */
    void addItem( Object key, long item );

    /**
     * Returns the contents of a bin as an array of longs.
     *
     * @param  key  bin key
     * @return   bin contents
     */
    long[] getLongs( Object key );

    /**
     * Returns an iterator over the bin identifiers.
     * The <code>remove</code> method of this iterator may or may not
     * be supported.
     *
     * @return  iterator over non-empty bins
     */
    Iterator<?> getKeyIterator();

    /**
     * Returns the number of non-empty bins used.
     *
     * @return  bin count
     */
    long getBinCount();

    /**
     * Returns a LongBinner equivalent to the combination of this
     * and another compatible one.
     *
     * @param  other  binner compatible with this one
     * @return  binner combining items from this with items from other
     */
    LongBinner combine( LongBinner other );
}
