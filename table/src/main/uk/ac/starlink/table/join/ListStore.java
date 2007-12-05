package uk.ac.starlink.table.join;

import java.util.List;

/**
 * Defines how to store list contents in an object container.
 * Intended for use with {@link java.util.Collection}s.
 * Effectively provides "boxing" for list-like objects.
 *
 * @author   Mark Taylor
 * @since    3 Dec 2007
 */
public interface ListStore {

    /**
     * Returns the value of an object as a List.
     * <code>value</code> should either be <code>null</code>, or an object
     * returned by this object's {@link #addItem} method.
     *
     * @param   value  list storage element
     * @return  list representing the content of <code>value</code>
     */
    List getList( Object value );

    /**
     * Adds an item to a list storage element.
     * <code>value</code> should either be <code>null</code>, or an object
     * returned by an earlier invocation of this method.
     *
     * @param   value  list storage element
     * @param   item   object to append to the list
     * @return  new list storage element
     */
    Object addItem( Object value, Object item );
}
