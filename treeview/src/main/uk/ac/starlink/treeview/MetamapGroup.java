package uk.ac.starlink.treeview;

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
     * No entry is added if <tt>value==null</tt> or if 
     * <tt>value.toString().length()==0</tt>.
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
     * Mandates an ordering to be imposed on the metadata keys.
     * The supplied list is a 
     * The effect of this call is to influence the order of the list 
     * returned by subsequent calls of {@link #getKnownKeys}.
     * <p>
     * The supplied argument <tt>ordering</tt> is a list of strings;
     * a string which appears earlier in this list is considered to be
     * earlier in the list of metadata keys.  Any which do not appear 
     * in this list will be ranked in an unspecified order at the end.
     *
     * @param   a list of strings which may appear in the metadata keys
     */
    public void setKeyOrder( List ordering ) {
        final List order = new ArrayList( ordering );
        Collections.reverse( order );
        keyComparator = new Comparator() {
            public int compare( Object key1, Object key2 ) {
                return order.indexOf( key2 ) - order.indexOf( key1 );
            }
        };
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
        return knownKeys;
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
}
