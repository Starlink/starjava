package uk.ac.starlink.table.join;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Provides Binner implementations.
 *
 * @author   Mark Taylor
 * @since    21 Jan 2010
 */
class Binners {

    /**
     * Private sole constructor prevents instantiation.
     */
    private Binners() {
    }

    /**
     * Returns a new binnner which may not support optional operations.
     *
     * @return   new binner
     */
    public static ObjectBinner createObjectBinner() {
        return new CombinationObjectBinner();
    }

    /**
     * Returns a new binner which supports all optional operations;
     * bin lists are modifiable, and the key iterator supports 
     * <code>remove</code>.
     *
     * @return   new binner
     */
    public static ObjectBinner createModifiableObjectBinner() {
        return new StorageListObjectBinner();
    }

    /**
     * Partial ObjectBinner implementation based on a HashMap.
     * Concrete subclasses must arrange for storing and retrieving 
     * "listable" map values as lists.  A listable is an untyped object
     * which this class knows how to treat as if it were a list.
     */
    private static abstract class MapObjectBinner implements ObjectBinner {

        private final Map map_ = new HashMap();
        private long nItem_;

        public void addItem( Object key, Object item ) {
            nItem_++;
            map_.put( key, addToListable( map_.get( key ), item ) );
        }

        public List getList( Object key ) {
            return getListFromListable( map_.get( key ) );
        }

        public boolean containsKey( Object key ) {
            return map_.containsKey( key );
        }

        public void remove( Object key ) {
            map_.remove( key );
        }

        public Iterator getKeyIterator() {
            return map_.keySet().iterator();
        }

        public long getItemCount() {
            return nItem_;
        }

        public long getBinCount() {
            return map_.size();
        }

        /**
         * Takes an existing listable, adds an item to it, and returns 
         * a new listable containing the concatenation.
         * The <code>listable</code> argument will be either on object
         * returned by a previous call to this method,
         * or null indicating a previously empty list.
         *
         * @param   listable  existing listable or null
         * @param   item  object to append
         * @return  new listable including added item
         */
        protected abstract Object addToListable( Object listable, Object item );

        /**
         * Returns a List view of a listable.
         * The <code>listable</code> argument will be either an object
         * returned by a previous call to {@link #addToListable},
         * or null indicating a previously empty list.
         *
         * @param  listable  existing listable or null
         * @return  List containing <code>listable</code>'s items
         */
        protected abstract List getListFromListable( Object listable );
    }

    /**
     * Modifiable ObjectBinner implementation.
     */
    private static class StorageListObjectBinner extends MapObjectBinner {

        protected Object addToListable( Object listable, Object item ) {
            List list = listable == null ? new StorageList()
                                         : (List) listable;
            list.add( item );
            return list;
        }

        protected List getListFromListable( Object listable ) {
            return (List) listable;
        }
    }

    /**
     * Non-modifiable ObjectBinner implementation.
     * Three types of listables are stored as map values:
     * <ul>
     * <li>null means an empty list</li>
     * <li>a StorageList instance contains list items</li>
     * <li>anything else is to be interpreted as a single-item list containing
     *     itself</li>
     * </ul>
     */
    private static class CombinationObjectBinner extends MapObjectBinner {

        protected Object addToListable( Object listable, Object item ) {
            if ( item instanceof StorageList ) {
                throw new IllegalArgumentException(
                    "Can't mix keys with values" );
            }
            if ( listable == null ) {
                return item;
            }
            else if ( listable instanceof StorageList ) {
                ((List) listable).add( item );
                return listable;
            }
            else {
                List list = new StorageList();
                list.add( listable );
                list.add( item );
                return list;
            }
        }

        protected List getListFromListable( Object listable ) {
            if ( listable == null ) {
                return null;
            }
            else if ( listable instanceof StorageList ) {
                return (List) listable;
            }
            else {
                return Collections.singletonList( listable );
            }
        }

    }

    /**
     * Utility class used for the list storage implementation.
     * It has to be private for use here, so that we can distinguish
     * between Lists created/managed by <code>CombinationObjectBinner</code>
     * and those added as items.
     */
    private static class StorageList extends LinkedList {
    }
}
