package uk.ac.starlink.table.join;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * LinkSet implementation based on a {@link java.util.HashSet}.
 *
 * @author   Mark Taylor
 * @since    10 Mar 2010
 */
class HashSetLinkSet implements LinkSet {

    private final Set<RowLink> links_ = new HashSet<RowLink>();

    public void addLink( RowLink link ) {
        links_.add( link );
    }

    public boolean containsLink( RowLink link ) {
        return links_.contains( link );
    }

    public boolean removeLink( RowLink link ) {
        return links_.remove( link );
    }

    public Iterator<RowLink> iterator() {
        return links_.iterator();
    }

    public int size() {
        return links_.size();
    }

    public Collection<RowLink> toSorted() {
        RowLink[] array = new RowLink[ links_.size() ];
        int i = 0;
        for ( RowLink link : links_ ) {
            array[ i++ ] = link;
        }
        assert i == array.length;
        Arrays.parallelSort( array );
        return Collections.unmodifiableList( Arrays.asList( array ) );
    }
}
