package uk.ac.starlink.table.join;

import java.util.Iterator;
import java.util.List;

/**
 * Map which can store lists of objects.
 * The keys of the map can be viewed as identifiers of bins, and each
 * bin stores one or more items.  Items are added one at a time, with
 * bins being created as required.  Bins can be queried to find which
 * items they contain.
 *
 * @param <K> type for bin identifiers
 * @param <E> type for elements placed in bins
 *
 * @author   Mark Taylor
 * @since    20 Jan 2010
 */
interface ObjectBinner<K,E> {

    /**
     * Adds an item to a given bin.
     *
     * @param  key  bin identifier
     * @param  item   item to place in the bin
     */
    void addItem( K key, E item );

    /**
     * Returns a list of the items in a given bin.
     * This list may or may not be modifiable, depending on the implementation.
     *
     * @param  key  bin identifier
     * @return   a list of the items which have been added to the bin;
     *           if the bin is empty null may be returned
     */
    List<E> getList( K key );

    /**
     * Removes a bin from this map (optional operation).
     *
     * @param   key  bin identifier
     */
    void remove( K key );

    /**
     * Returns an iterator over the bin identifiers.
     * The <code>remove</code> method of this iterator may or may not
     * be supported.
     *
     * @return  iterator over non-empty bins   
     */
    Iterator<K> getKeyIterator();

    /**
     * Indicates whether a given bin contains a non-zero number of items.
     *
     * @param  key  bin identifier
     * @return  true iff <code>!getList(key).isEmpty()</code>
     */
    boolean containsKey( K key );

    /**
     * Returns the sum of the sizes of all the lists in all the bins.
     *
     * @return   total item count
     */
    long getItemCount();

    /**
     * Returns the number of non-empty bins.
     *
     * @return   bin count
     */
    long getBinCount();

    /**
     * Adds the content of another binner to this one.
     * The result is as if all the {@link #addItem} calls to both
     * binners so far had been called on this one.
     *
     * @param  other  other binner
     */
    void addContent( ObjectBinner<K,E> other );
}
