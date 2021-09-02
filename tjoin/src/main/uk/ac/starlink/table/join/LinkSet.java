package uk.ac.starlink.table.join;

import java.util.Collection;
import java.util.Iterator;

/**
 * Holds an unordered collection of unique {@link RowLink} objects.
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
public interface LinkSet extends Iterable<RowLink> {

    /**
     * Adds a new link to this set.  If the set has any existing entries
     * equivalent to <code>link</code> (by <code>equals</code>) 
     * they should be removed.
     *
     * @param  link  row link to add
     */
    void addLink( RowLink link );

    /**
     * Indicates whether this set contains a given link.
     *
     * @param  link   link to test
     * @return  true iff this set contains an entry equal to <code>link</code>
     */
    boolean containsLink( RowLink link );

    /**
     * Removes an existing link from this set.
     *
     * @param   link  link to remove
     * @return  true iff <code>link</code> was there in the first place
     */
    boolean removeLink( RowLink link );

    /**
     * Returns an iterator over the elements of this set.
     *
     * @return  iterator, which should have a working <code>remove()</code>
     *          method
     */
    Iterator<RowLink> iterator();

    /**
     * Returns the number of items in this set.
     *
     * @return  set size
     */
    int size();

    /**
     * Returns a collection of RowLink objects that has the same
     * content as this LinkSet, but with entries in their natural
     * (<code>Comparable</code>) order.
     *
     * <p>The result is not intended for modification;
     * it may or may not be backed by this object, so subsequent
     * modifications of this LinkSet while it's in use are not recommended.
     *
     * @return  unmodifiable sorted collection of the row links in this set
     */
    Collection<RowLink> toSorted();
}
