package uk.ac.starlink.table.join;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * LinkSet implementation based on a {@link java.util.TreeSet}.
 *
 * @author   Mark Taylor
 * @since    7 Sep 2005
 */
class TreeSetLinkSet implements LinkSet {

    private final SortedSet<RowLink> set_ = new TreeSet<RowLink>();

    public void addLink( RowLink link ) {
        set_.add( link );
    }

    public boolean containsLink( RowLink link ) {
        return set_.contains( link );
    }

    public boolean removeLink( RowLink link ) {
        return set_.remove( link );
    }

    public Iterator<RowLink> iterator() {
        return set_.iterator();
    }

    public int size() {
        return set_.size();
    }

    public Collection<RowLink> toSorted() {
        return Collections.unmodifiableSet( set_ );
    }
}
