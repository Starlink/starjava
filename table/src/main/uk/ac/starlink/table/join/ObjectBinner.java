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
 * @author   Mark Taylor
 * @since    20 Jan 2010
 */
interface ObjectBinner {

    /**
     * Adds an item to a given bin.
     *
     * @param  key  bin identifier
     * @param  item   item to place in the bin
     */
    void addItem( Object key, Object item );

    /**
     * Returns a list of the items in a given bin.
     * This list may or may not be modifiable, depending on the implementation.
     *
     * @param  key  bin identifier
     * @return   a list of the items which have been added to the bin;
     *           if the bin is empty null may be returned
     */
    List getList( Object key );

    /**
     * Removes a bin from this map (optional operation).
     *
     * @param   key  bin identifier
     */
    void remove( Object key );

    /**
     * Returns an iterator over the bin identifiers.
     * The <code>remove</code> method of this iterator may or may not
     * be supported.
     *
     * @return  iterator over non-empty bins   
     */
    Iterator getKeyIterator();

    /**
     * Indicates whether a given bin contains a non-zero number of items.
     *
     * @param  key  bin identifier
     * @return  true iff <code>!getList(key).isEmpty()</code>
     */
    boolean containsKey( Object key );

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
}
