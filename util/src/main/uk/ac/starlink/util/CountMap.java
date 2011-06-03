package uk.ac.starlink.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Keeps track of the number of times an item of type T has been added.
 *
 * @author   Mark Taylor
 * @since    3 Jun 2011
 */
public class CountMap<T> {

    private final Map<T,int[]> map_;

    /**
     * Constructor.
     */
    public CountMap() {
        map_ = new LinkedHashMap<T,int[]>();
    }

    /**
     * Counts an item.
     *
     * @param   item  item to count
     */
    public int addItem( T item ) {
        if ( ! map_.containsKey( item ) ) {
            map_.put( item, new int[ 1 ] );
        }
        return ++map_.get( item )[ 0 ];
    }

    /**
     * Returns the number of times a given item has been added.
     *
     * @param  key  item to count
     */
    public int getCount( T key ) {
        return map_.containsKey( key ) ? map_.get( key )[ 0 ]
                                       : 0;
    }

    /**
     * Returns a set of all the items with a count of at least one.
     *
     * @return   key set
     */
    public Set<T> keySet() {
        return map_.keySet();
    }

    /**
     * Sets all the item counts to zero.
     */
    public void clear() {
        map_.clear();
    }
}
