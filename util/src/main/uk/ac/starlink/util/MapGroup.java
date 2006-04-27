package uk.ac.starlink.util;

import java.lang.Comparable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents an ordered list of {@link java.util.Map}s containing 
 * related data.
 * The same keys may crop up in the maps comprising the groups, and
 * are taken to have the same meaning, so that all the values associated 
 * with a given key in any of the maps in the group are taken to be
 * a related set.  There is no requirement however that a key which
 * appears in one of the maps has to appear in any or all of the others.
 * A single list of the union of all the keys which appear in any
 * of the maps in the group can be obtained.  An ordering may be 
 * imposed on this list.
 *
 * @author   Mark Taylor (Starlink)
 */
public class MapGroup {

    private final List maps = new ArrayList();
    private List ordering;
    private Comparator keyComparator;
    private List knownKeys;

    /**
     * Constructs a new <tt>MapGroup</tt>.
     */
    public MapGroup() {
        setKeyOrder( new ArrayList() );
    }

    /**
     * Adds a new Map to the end of this group.
     *
     * @param  map  the map to add
     */
    public void addMap( Map map ) {
        maps.add( map );
    }

    /**
     * Returns an unmodifiable list of all the maps in this group.
     * It is unmodifiable so that you can't put anything in it which
     * is not a <tt>Map</tt>.
     *
     * @return  a list of the maps
     */
    public List getMaps() {
        return Collections.unmodifiableList( maps );
    }

    /**
     * Returns the number of maps in this group.
     *
     * @return  map count
     */
    public int size() {
       return maps.size();
    }

    /**
     * Mandates an ordering to be imposed on the map keys.
     * The effect of this call is to influence the order of the list
     * returned by subsequent calls of {@link #getKnownKeys}.
     * <p>
     * The supplied argument <tt>ordering</tt> is a list of objects;
     * an object which appears earlier in this list is considered to be
     * earlier in the list of <tt>MapGroup</tt> keys.  Any which do not appear
     * in this list will be ranked in an unspecified order at the end
     * (their natural comparison order will be used if both objects
     * implement {@link java.lang.Comparable}).
     *
     * @param   ordering a list of objects which may appear in this 
     *          group's map keys
     */
    public void setKeyOrder( List ordering ) {

        /* Take a copy in case the caller changes his copy later. */
        this.ordering = new ArrayList( ordering );

        /* Make a comparator from the ordering - this is what we 
         * actually use rather than the stored list. */
        final List order = new ArrayList( this.ordering );
        Collections.reverse( order );
        keyComparator = new Comparator() {
            public int compare( Object key1, Object key2 ) {
                int ik1 = order.indexOf( key1 );
                int ik2 = order.indexOf( key2 );
                if ( ik1 >= 0 || ik2 >= 0 ) {
                    return ik2 - ik1;
                }
                else if ( key1 instanceof Comparable &&
                          key2 instanceof Comparable ) {
                    return ((Comparable) key1).compareTo( key2 );
                }
                else {
                    return System.identityHashCode( key1 )
                         - System.identityHashCode( key2 );
                }
            }
        };
    }

    /**
     * Returns the list which defines ordering for any keys which crop up.
     *
     * @return  current key ordering
     */
    public List getKeyOrder() {
        return ordering;
    }

    /**
     * Removes all entries which have a key in a given collection for 
     * every map in this group.
     *
     * @param  keys  the set of key values whose entries must be removed
     */
    public void removeKeys( Collection keys ) {
        for ( Iterator it = keys.iterator(); it.hasNext(); ) {
            removeKey( it.next() );
        }
    }

    /**
     * Removes all entries with a given key for every map in this group.
     *
     * @param  key  the key whose entries must be removed
     */
    public void removeKey( Object key ) {
        for ( Iterator it = maps.iterator(); it.hasNext(); ) {
            ((Map) it.next()).remove( key );
        }
    }

    /**
     * Removes all entries except those with keys in a given collection for
     * every map in this group.
     *
     * @param  keys  the keys whose entries must be retained
     */
    public void retainKeys( Collection keys ) {
        for ( Iterator it = maps.iterator(); it.hasNext(); ) {
            ((Map) it.next()).keySet().retainAll( keys );
        }
    }

    /**
     * Sets the list of known keys.  This list, if not null, 
     * will be returned by subsequent calls of {@link #getKnownKeys}.
     *
     * @param  keys  collection of keys
     */
    public void setKnownKeys( List keys ) {
        knownKeys = new ArrayList( keys );
    }

    /**
     * Returns a list of all the keys which appear in any of the metadata sets.
     * The order is determined by the most recent call of
     * {@link #setKeyOrder}.  If it has never been called they will
     * be returned in an arbitrary order.
     * If {@link #setKnownKeys} has been called with a non-null argument,
     * that list will be returned instead.
     *
     * @return  the list of map keys
     */
    public List getKnownKeys() {
        if ( knownKeys != null ) {
            return knownKeys;
        }
        else {
            Set keyset= new HashSet();
            for ( Iterator it = maps.iterator(); it.hasNext(); ) {
                Map map = (Map) it.next();
                keyset.addAll( map.keySet() );
            }
            List keylist = new ArrayList( keyset );
            Collections.sort( keylist, keyComparator );
            return keylist;
        }
    }

}
