package uk.ac.starlink.table.join;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Utility class which permits storage of list contents in object container.
 * Intended for use with {@link java.util.Collection}s.
 * Effectively provides "boxing" for list-like objects.
 * The implementation may be tweaked for performance.
 *
 * @author   Mark Taylor
 * @since    3 Dec 2007
 */
class ListStore {

    /**
     * Returns the value of an object as a List.
     * <code>value</code> should either be <code>null</code>, or an object
     * returned by this object's {@link #addItem} method.
     * The returned list may not be mutable.
     *
     * @param   value  list storage element
     * @return  list representing the content of <code>value</code>
     */
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

    /**
     * Adds an item to a list storage element.
     * <code>value</code> should either be <code>null</code>, or an object
     * returned by an earlier invocation of this method.
     *
     * @param   old value  list storage element
     * @param   item   object to append to the list
     * @return  new list storage element
     */
    public Object addItem( Object value, Object item ) {
        if ( item instanceof StorageList ) {
            throw new IllegalArgumentException( "Can't do that!" );
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

    /**
     * Utility class used for the list storage implementation.
     * It has to be private to the <code>ListStore</code> class,
     * so that we can distinguish between Lists created/managed by this
     * class and those added as items.
     */
    private static class StorageList extends LinkedList {
    }
}
