package uk.ac.starlink.datanode.nodes;

import java.util.Collections;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a group of items containing related sets of metadata.
 * Each set of metadata is a key/value map, in which the key is a string.
 * If different items have metadata entries with the same key, they 
 * may be supposed to represent the same kind of quantity.
 * A single, automatically maintained list is therefore kept of the 
 * keys which crop up in entries in any of the items' metadata sets.
 * An ordering may be imposed on this list.
 * 
 * @author   Mark Taylor (Starlink)
 */
public class MetamapGroup {

    private final List knownKeys;
    private final Map[] metamaps;
    private Comparator keyComparator;
    private List ordering = new ArrayList();

    /**
     * Initialises a MetamapGroup which will contain a given number of items.
     *
     * @param  nitem  the number of items in the group
     */
    public MetamapGroup( int nitem ) {
        knownKeys = Collections.synchronizedList( new ArrayList() );
        metamaps = new Map[ nitem ];
        for ( int i = 0; i < nitem; i++ ) {
            metamaps[ i ] = Collections.synchronizedMap( new HashMap() );
        }
    }

    /**
     * Adds an entry to one of the metadata sets.
     * No entry is added if <code>value==null</code> or if 
     * <code>value.toString().length()==0</code>.
     * 
     * @param  item   the index of the set to which the entry should be added
     * @param  key    the metadatum key
     * @param  value  the metadatum value
     */
    public void addEntry( int item, String key, Object value ) {
        if ( value != null && value.toString().length() > 0 ) {
            if ( ! knownKeys.contains( key ) ) {
                knownKeys.add( key );
            }
            metamaps[ item ].put( key, value );
        }
    }

    /**
     * Retrieves an entry from one of the metadata sets by key.
     * <code>null</code> is returned if no such entry exists.
     *
     * @param  item  the index of the set from which the entry should be got
     * @param  key   the metadatum key
     * @return  the value of the entry associated with <code>key</code>,
     *          or <code>null</code> if there isn't one
     */
    public Object getEntry( int item, String key ) {
        return metamaps[ item ].get( key );
    }

    /**
     * Indicates whether an entry with a given key is present in one of
     * the metadata sets.
     *
     * @param  item  the index of the set from which the entry should be got
     * @param  key   the metadatum key
     * @return   true iff the entry corresponding to <code>key</code> exists
     *           in set number <code>item</code>
     */
    public boolean hasEntry( int item, String key ) {
        return metamaps[ item ].containsKey( key );
    }

    /**
     * Mandates an ordering to be imposed on the metadata keys.
     * The effect of this call is to influence the order of the list 
     * returned by subsequent calls of {@link #getKnownKeys}.
     * <p>
     * The supplied argument <code>ordering</code> is a list of strings;
     * a string which appears earlier in this list is considered to be
     * earlier in the list of metadata keys.  Any which do not appear 
     * in this list will be ranked in an unspecified order at the end.
     *
     * @param  ordering a list of strings which may appear in the metadata keys
     */
    public void setKeyOrder( List ordering ) {
        this.ordering = new ArrayList( ordering );
        final List order = new ArrayList( ordering );
        Collections.reverse( order );
        keyComparator = new Comparator() {
            public int compare( Object key1, Object key2 ) {
                return order.indexOf( key2 ) - order.indexOf( key1 );
            }
        };
    }

    /**
     * Returns the list which defines ordering for any keys which crop up.
     * This will have the same contents as the argument of the last call
     * to {@link #setKeyOrder}, or an empty list if that method has 
     * not been called.
     *
     * @return  current key ordering 
     */
    public List getKeyOrder() {
        return ordering;
    }

    /**
     * Returns a list of all the keys which appear in any of the metadata sets.
     * The order is determined by the most recent call of
     * {@link #setKeyOrder}.  If it has never been called they will
     * be returned in insertion order.
     *
     * @return  the list of map keys
     */
    public List getKnownKeys() {
        if ( keyComparator != null ) {
            Collections.sort( knownKeys, keyComparator );
        }
        return new ArrayList( knownKeys );
    }

    /**
     * Returns the array of metadata maps.
     *
     * @return   an array in which the n'th element is the n'th item's
     *           metadata map
     */
    public Map[] getMetamaps() {
        return metamaps;
    }

    /**
     * Returns the number of metadata maps.
     *
     * @return  the number of maps in this group
     */
    public int getNumMaps() {
        return metamaps.length;
    }
}
