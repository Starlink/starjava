package uk.ac.starlink.table.join;

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

    SortedSet set_ = new TreeSet();

    public void addLink( RowLink link ) {
        set_.add( link );
    }

    public boolean containsLink( RowLink link ) {
        return set_.contains( link );
    }

    public boolean removeLink( RowLink link ) {
        return set_.remove( link );
    }

    public Iterator iterator() {
        return set_.iterator();
    }

    public int size() {
        return set_.size();
    }

    public boolean sort() {
        // no action required - the underlying set is sorted
        return true;
    }
}
