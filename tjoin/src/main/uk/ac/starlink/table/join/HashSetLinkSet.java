package uk.ac.starlink.table.join;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;
import java.util.Set;

/**
 * LinkSet implementation based on a {@link java.util.HashSet}.
 *
 * @author   Mark Taylor
 * @since    10 Mar 2010
 */
class HashSetLinkSet implements LinkSet {

    private Collection<RowLink> links_ = new HashSet<RowLink>();

    public void addLink( RowLink link ) {
        getSet().add( link );
    }

    public boolean containsLink( RowLink link ) {
        return getSet().contains( link );
    }

    public boolean removeLink( RowLink link ) {
        return getSet().remove( link );
    }

    public Iterator<RowLink> iterator() {
        return links_.iterator();
    }

    public int size() {
        return links_.size();
    }

    public boolean sort() {
        try {
            Collections.sort( getList() );
            return true;
        }
        catch ( OutOfMemoryError e ) {
            return false;
        }
    }

    /**
     * Returns the contents of this LinkSet as a List.
     *
     * @return  list view
     */
    private List<RowLink> getList() {
        if ( ! ( links_ instanceof List ) ) {
            List<RowLink> list = new ArrayList<RowLink>();
            transfer( links_, list );
            links_ = list;
        }
        return (List<RowLink>) links_;
    }

    /**
     * Returns the contents of this LinkSet as a Set.
     *
     * @return  set view
     */
    private Set<RowLink> getSet() {
        if ( ! ( links_ instanceof Set ) ) {
            Set<RowLink> set = new HashSet<RowLink>();
            transfer( links_, set );
            links_ = set;
        }
        return (Set<RowLink>) links_;
    }

    /**
     * Transfers the data of this LinkSet from one Collection object to
     * another.  Attempts to do this without use of additional memory;
     * the source collection is destroyed by the operation.
     *
     * @param   src  source collection (empty on exit)
     * @param   dest  destination collection (empty on entry)
     */
    private static void transfer( Collection<RowLink> src,
                                  Collection<RowLink> dest ) {
        int n = src.size();
        if ( src instanceof List && src instanceof RandomAccess ) {
            for ( int i = src.size() - 1; i >= 0; i-- ) {
                dest.add( ((List<RowLink>) src).remove( i ) );
            }
        }
        else {
            for ( Iterator<RowLink> it = src.iterator(); it.hasNext(); ) {
                dest.add( it.next() );
                it.remove();
            }
        }
        assert dest.size() == n;
        assert src.size() == 0;
    }
}
