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

    private Collection links_ = new HashSet();

    public void addLink( RowLink link ) {
        getSet().add( link );
    }

    public boolean containsLink( RowLink link ) {
        return getSet().contains( link );
    }

    public boolean removeLink( RowLink link ) {
        return getSet().remove( link );
    }

    public Iterator iterator() {
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
    private List getList() {
        if ( ! ( links_ instanceof List ) ) {
            List list = new ArrayList();
            transfer( links_, list );
            links_ = list;
        }
        return (List) links_;
    }

    /**
     * Returns the contents of this LinkSet as a Set.
     *
     * @return  set view
     */
    private Set getSet() {
        if ( ! ( links_ instanceof Set ) ) {
            Set set = new HashSet();
            transfer( links_, set );
            links_ = set;
        }
        return (Set) links_;
    }

    /**
     * Transfers the data of this LinkSet from one Collection object to
     * another.  Attempts to do this without use of additional memory;
     * the source collection is destroyed by the operation.
     *
     * @param   src  source collection (empty on exit)
     * @param   dest  destination collection (empty on entry)
     */
    private static void transfer( Collection src, Collection dest ) {
        int n = src.size();
        if ( src instanceof List && src instanceof RandomAccess ) {
            for ( int i = src.size() - 1; i >= 0; i-- ) {
                dest.add( ((List) src).remove( i ) );
            }
        }
        else {
            for ( Iterator it = src.iterator(); it.hasNext(); ) {
                dest.add( it.next() );
                it.remove();
            }
        }
        assert dest.size() == n;
        assert src.size() == 0;
    }
}
