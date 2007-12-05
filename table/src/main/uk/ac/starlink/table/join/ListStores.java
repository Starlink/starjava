package uk.ac.starlink.table.join;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Provides {@link ListStore} implementations.
 *
 * @author   Mark Taylor
 * @since    3 Dec 2007
 */
public class ListStores {

    /**
     * Private sole constructor prevents instantiation.
     */
    private ListStores() {
    }

    /**
     * Constructs a list store with no particular constraints on the lists
     * it uses (they may or may not be modifiable).
     *
     * @return   list store  
     */
    public static ListStore createListStore() {
        return new CombinationListStore();
    }

    /**
     * Constructs a list store which returns modifiable lists as far as 
     * possible.
     *
     * @return  list store
     */
    public static ListStore createModifiableListStore() {
        return new LinkedListListStore();
    }

    /**
     * List store implementation which uses various different storage
     * methods.  Some benchmarking could probably lead to better performance.
     */
    private static class CombinationListStore implements ListStore {

        public List getList( Object value ) {
            if ( value == null ) {
                return Collections.EMPTY_LIST;
            }
            else if ( value instanceof StorageList ) {
                return (List) value;
            }
            else {
                return Collections.singletonList( value );
            }
        }

        public Object addItem( Object value, Object item ) {
            if ( item instanceof StorageList ) {
                throw new IllegalArgumentException( "Can't mix keys "
                                                  + "with values!" );
            }
            if ( value == null ) {
                return item;
            }
            else if ( value instanceof StorageList ) {
                ((List) value).add( item );
                return value;
            }
            else {
                List list = new StorageList();
                list.add( value );
                list.add( item );
                return list;
            }
        }
    }

    /**
     * List store implementation which always uses LinkedLists.
     * Returned lists are always modifiable.
     * Note however that getList() can return null.
     */
    private static class LinkedListListStore implements ListStore {

        public List getList( Object value ) {
            return (List) value;
        }

        public Object addItem( Object value, Object item ) {
            List list = value == null
                      ? new LinkedList()
                      : (List) value;
            list.add( item );
            return list;
        }
    }

    /**
     * Utility class used for the list storage implementation.
     * It has to be private to this <code>ListStore</code> class,
     * so that we can distinguish between Lists created/managed by this
     * class and those added as items.
     */
    private static class StorageList extends LinkedList {
    }
}
