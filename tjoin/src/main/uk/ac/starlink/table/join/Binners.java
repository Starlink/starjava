package uk.ac.starlink.table.join;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.util.IntList;
import uk.ac.starlink.util.LongList;

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
    public static <K,E> ObjectBinner<K,E> createObjectBinner() {
        return new CombinationObjectBinner<K,E>();
    }

    /**
     * Returns a new binner which supports all optional operations;
     * bin lists are modifiable, and the key iterator supports 
     * <code>remove</code>.
     *
     * @return   new binner
     */
    public static <K,E> ObjectBinner<K,E> createModifiableObjectBinner() {
        return new StorageListObjectBinner<K,E>();
    }

    /**
     * Returns a new binner for storing long integer values.
     *
     * @return   new LongBinner
     */
    public static LongBinner createLongBinner( long nrow ) {
        return nrow >= 0 && nrow < Integer.MAX_VALUE
             ? (LongBinner) new CombinationIntLongBinner()
             : (LongBinner) new LongListLongBinner();
    }

    /**
     * Partial ObjectBinner implementation based on a HashMap.
     * Concrete subclasses must arrange for storing and retrieving 
     * "listable" map values as lists.  A listable is an untyped object
     * which this class knows how to treat as if it were a list.
     */
    private static abstract class MapObjectBinner<K,E,L>
            implements ObjectBinner<K,E> {

        private final Map<K,L> map_ = new HashMap<>();
        private long nItem_;

        public void addItem( K key, E item ) {
            nItem_++;
            map_.put( key, addToListable( map_.get( key ), item ) );
        }

        public List<E> getList( K key ) {
            return getListFromListable( map_.get( key ) );
        }

        public boolean containsKey( K key ) {
            return map_.containsKey( key );
        }

        public void remove( K key ) {
            map_.remove( key );
        }

        public Iterator<K> getKeyIterator() {
            return map_.keySet().iterator();
        }

        public long getItemCount() {
            return nItem_;
        }

        public long getBinCount() {
            return map_.size();
        }

        public void addContent( ObjectBinner<K,E> other ) {
            for ( Iterator<K> keyIt = other.getKeyIterator();
                  keyIt.hasNext(); ) {
                K key = keyIt.next();
                for ( E item : other.getList( key ) ) {
                    addItem( key, item );
                }
            }
        }

        /**
         * Takes an existing listable, adds an item to it, and returns 
         * a listable containing the concatenation.
         * The <code>listable</code> argument will be either an object
         * returned by a previous call to this method,
         * or null indicating a previously empty list.
         *
         * @param   listable  existing listable or null
         * @param   item  object to append
         * @return  listable including added item;
         *          may or may not be input listable
         */
        protected abstract L addToListable( L listable, E item );

        /**
         * Returns a List view of a listable.
         * The <code>listable</code> argument will be either an object
         * returned by a previous call to {@link #addToListable},
         * or null indicating a previously empty list.
         *
         * @param  listable  existing listable or null
         * @return  List containing <code>listable</code>'s items
         */
        protected abstract List<E> getListFromListable( L listable );
    }

    /**
     * Modifiable ObjectBinner implementation.
     */
    private static class StorageListObjectBinner<K,E>
            extends MapObjectBinner<K,E,StorageList<E>> {

        protected StorageList<E> addToListable( StorageList<E> list, E item ) {
            if ( list == null ) {
                list = new StorageList<E>();
            }
            list.add( item );
            return list;
        }

        protected List<E> getListFromListable( StorageList<E> list ) {
            return list;
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
    private static class CombinationObjectBinner<K,E>
            extends MapObjectBinner<K,E,Object> {

        protected Object addToListable( Object listable, E item ) {
            if ( item instanceof StorageList ) {
                throw new IllegalArgumentException(
                    "Can't mix keys with values" );
            }
            if ( listable == null ) {
                return item;
            }
            else if ( listable instanceof StorageList ) {
                @SuppressWarnings("unchecked")
                StorageList<E> list = (StorageList<E>) listable;
                list.add( item );
                return listable;
            }
            else {
                StorageList<E> list = new StorageList<E>();
                @SuppressWarnings("unchecked")
                E singleElement = (E) listable;
                list.add( singleElement );
                list.add( item );
                return list;
            }
        }

        protected List<E> getListFromListable( Object listable ) {
            if ( listable == null ) {
                return null;
            }
            else if ( listable instanceof StorageList ) {
                @SuppressWarnings("unchecked")
                StorageList<E> list = (StorageList<E>) listable;
                return list;
            }
            else {
                @SuppressWarnings("unchecked")
                E singleElement = (E) listable;
                return Collections.singletonList( singleElement );
            }
        }
    }

    /**
     * LongBinner implementation based on a HashMap.
     * Concrete subclasses must arrange for storing and retreiving
     * "listable" map values as lists of longs.  A listable is an 
     * untyped object which this class knows how to treat as if it were 
     * a list of integer values.
     */
    private static abstract class MapLongBinner<L> implements LongBinner {
        final Map<Object,L> map_ = new HashMap<>();

        public void addItem( Object key, long item ) {
            map_.put( key, addToListable( map_.get( key ), item ) );
        }

        public long[] getLongs( Object key ) {
            return getLongsFromListable( map_.get( key ) );
        }

        public Iterator<?> getKeyIterator() {
            return map_.keySet().iterator();
        }

        public long getBinCount() {
            return map_.size();
        }

        public LongBinner combine( LongBinner o ) {
            @SuppressWarnings("unchecked")
            MapLongBinner<L> other = (MapLongBinner<L>) o;
            if ( this.getBinCount() > other.getBinCount() ) {
                addBinner( other );
                return this;
            }
            else {
                other.addBinner( this );
                return other;
            }
        }

        private void addBinner( MapLongBinner<L> other ) {
            for ( Map.Entry<?,L> entry : other.map_.entrySet() ) {
                Object key = entry.getKey();
                L otherListable = entry.getValue();
                L thisListable = map_.get( key );
                L combinedListable =
                      thisListable == null
                    ? otherListable
                    : combineListables( thisListable, otherListable );
                map_.put( key, combinedListable );
            }
        }

        /**
         * Takes an existing listable, adds an item to it, and returns 
         * a listable containing the concatenation.
         * The <code>listable</code> argument will be either an object
         * returned by a previous call to this method,
         * or null indicating a previously empty list.
         *
         * @param   listable  existing listable or null
         * @param   item  object to append
         * @return  listable including added item;
         *          may or may not be input listable
         */
        protected abstract L addToListable( L listable, long item );

        /**
         * Returns an array view of a listable.
         * The <code>listable</code> argument will be either an object
         * returned by a previous call to {@link #addToListable},
         * or null indicating a previously empty list.
         *
         * @param  listable  existing listable or null
         * @return  array containing <code>listable</code>'s items
         */
        protected abstract long[] getLongsFromListable( L listable );

        /**
         * Returns an object which is the combination of two listables.
         * Each argument will be an object returned by a previous call
         * to {@addToListable}.
         *
         * @param  listable1  first listable, not null
         * @param  listable2  second listable, not null
         * @return   aggregation of arguments, suitable for use as a listable
         */
        protected abstract L combineListables( L listable1, L listable2 );
    }

    /**
     * LongBinner implementation which stores listables as LongLists.
     * Can store long values in any range.
     */
    private static class LongListLongBinner extends MapLongBinner<LongList> {

        protected LongList addToListable( LongList list, long item ) {
            if ( list == null ) {
                list = new LongList( 1 );
            }
            list.add( item );
            return list;
        }

        protected long[] getLongsFromListable( LongList list ) {
            return list == null ? null
                                : list.toLongArray();
        }

        protected LongList combineListables( LongList list1, LongList list2 ) {
            if ( list1.size() > list2.size() ) {
                list1.addAll( list2 );
                return list1;
            }
            else {
                list2.addAll( list1 );
                return list2;
            }
        }
    }

    /**
     * LongBinner implementation which uses a variety of tricks to store
     * values in a compact way.  It can only store values in the range
     * of integers.
     */
    private static class CombinationIntLongBinner
            extends MapLongBinner<Object> {
        private static int MAX_ARRAY_SIZE = 32;
        private Integer lastInt_ = Integer.valueOf( -1 );
        private int[] lastInts_ = new int[ 0 ];

        protected Object addToListable( Object listable, long ltem ) {
            int item = Tables.checkedLongToInt( ltem );
            if ( listable == null ) {
                if ( lastInt_.intValue() != item ) {
                    lastInt_ = new Integer( item );
                }
                return lastInt_;
            }
            else if ( listable instanceof Integer ) {
                int i1 = ((Integer) listable).intValue();
                int i2 = item;
                return ( lastInts_.length == 2 &&
                         lastInts_[ 0 ] == i1 &&
                         lastInts_[ 1 ] == i2 )
                     ? lastInts_
                     : new int[] { i1, i2 };
            }
            else if ( listable instanceof int[] ) {
                int[] oldItems = (int[]) listable;
                int nItem = oldItems.length;
                if ( nItem < MAX_ARRAY_SIZE ) {
                    int[] newItems = new int[ nItem + 1 ];
                    System.arraycopy( oldItems, 0, newItems, 0, nItem );
                    newItems[ nItem ] = item;
                    return Arrays.equals( newItems, lastInts_ )
                         ? lastInts_
                         : newItems;
                }
                else {
                    assert nItem == MAX_ARRAY_SIZE;
                    IntList list = new IntList( oldItems ) {
                        protected int nextCapacity( int currentCapacity ) {
                            return ( currentCapacity * 5 ) / 4 + 1;
                        }
                    };
                    list.add( item );
                    return list;
                }
            }
            else if ( listable instanceof IntList ) {
                IntList list = (IntList) listable;
                list.add( item );
                return list;
            }
            else {
                assert false;
                return null;
            }
        }

        protected long[] getLongsFromListable( Object listable ) {
            if ( listable == null ) {
                return null;
            }
            else if ( listable instanceof Integer ) {
                return new long[] { ((Integer) listable).intValue(), };
            }
            else if ( listable instanceof int[] ) {
                int[] items = (int[]) listable;
                long[] ltems = new long[ items.length ];
                for ( int i = 0; i < items.length; i++ ) {
                    ltems[ i ] = items[ i ];
                }
                return ltems;
            }
            else if ( listable instanceof IntList ) {
                IntList list = (IntList) listable;
                int nItem = list.size();
                long[] ltems = new long[ nItem ];
                for ( int i = 0; i < nItem; i++ ) {
                    ltems[ i ] = (long) list.get( i );
                }
                return ltems;
            }
            else {
                assert false;
                return null;
            }
        }

        protected Object combineListables( Object listable1, Object listable2 ){
            Object result;
            Object addenda;
            if ( getListableLength( listable1 ) >
                 getListableLength( listable2 ) ) {
                result = listable1;
                addenda = listable2;
            }
            else {
                result = listable2;
                addenda = listable1;
            }
            for ( long ltem : getLongsFromListable( addenda ) ) {
                result = addToListable( result, ltem );
            }
            return result;
        }

        /**
         * Returns the number of entries in a listable from this binner.
         *
         * @param  listable
         * @return   listable length
         */
        private int getListableLength( Object listable ) {
            if ( listable == null ) {
                return 0;
            }
            else if ( listable instanceof Integer ) {
                return 1;
            }
            else if ( listable instanceof int[] ) {
                return ((int[]) listable).length;
            }
            else if ( listable instanceof IntList ) {
                return ((IntList) listable).size();
            }
            else {
                assert false;
                return -1;
            }
        }
    }

    /**
     * Utility class used for the list storage implementation.
     * It has to be private for use here, so that we can distinguish
     * between Lists created/managed by <code>CombinationObjectBinner</code>
     * and those added as items.
     */
    private static class StorageList<E> extends LinkedList<E> {
    }
}
