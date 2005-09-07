package uk.ac.starlink.table.join;

import java.util.Iterator;

/**
 * Holds a collection of unique {@link RowLink} objects.
 * The set is understood to be mutable - that is the methods
 * (including <code>iterator().remove()</code> should not throw 
 * <code>UnsupportedOperationException</code>s.
 *
 * <p>Although its methods are very similar to those of a 
 * {@link java.util.Set}, this is not a <code>Set</code> implementation.
 * This is because it will have a pretty specialised use - in particular
 * you should usually treat it as though it may contain a very large
 * number of elements and hence think carefully about efficiency
 * of access methods rather than take advantage of the convenience
 * methods available in the Collections package.
 *
 * @author   Mark Taylor
 * @since    7 Sep 2005
 */
public interface LinkSet {

    /**
     * Adds a new link to this set.  If the set has any existing entries
     * equivalent to <code>link</code> (by <code>equals</code>) 
     * they should be removed.
     *
     * @param  link  row link to add
     */
    public void addLink( RowLink link );

    /**
     * Indicates whether this set contains a given link.
     *
     * @param  link   link to test
     * @return  true iff this set contains an entry equal to <code>link</code>
     */
    public boolean containsLink( RowLink link );

    /**
     * Removes an existing link from this set.
     *
     * @param   link  link to remove
     * @return  true iff <code>link</code> was there in the first place
     */
    public boolean removeLink( RowLink link );

    /**
     * Returns an iterator over the elements of this set.
     *
     * @return  iterator, which should have a working <code>remove()</code>
     *          method
     */
    public Iterator iterator();

    /**
     * Returns the number of items in this set.
     *
     * @return  set size
     */
    public int size();

    /**
     * Requests that the set become sorted.  Subsequent to this operation,
     * and before any other modification operations are performed, 
     * the {@link #iterator} method should return an iterator which 
     * iterates over the entries in their natural 
     * ({@link java.util.Comparator}) order.
     * If an implementation is not capable of this action though, it
     * may just return false and do nothing.
     *
     * @return  true iff a subsequent iterator will be sorted
     */
    public boolean sort();
}
