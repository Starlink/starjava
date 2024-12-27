package uk.ac.starlink.util;

import java.lang.reflect.Array;

/**
 * Provides an extendable list of primitive values.
 * This provides an abstract superclass for concrete implementations which do
 * roughly the same job for numeric primitive types that 
 * {@link java.lang.StringBuffer} does for <code>char</code>s and 
 * {@link java.util.List} does for <code>Object</code>s.
 *
 * @author   Mark Taylor
 * @since    12 Oct 2006
 */
public abstract class PrimitiveList {

    private final Class<?> componentType_;
    private int size_;
    Object array_;
    static final int DEFAULT_SIZE = 16;

    /**
     * Constructs a list from an initial array object, whose component
     * type determines the primitive type which this list will work with,
     * and a size, which indicates number of elements of the array
     * which are <em>initially</em> considered to constitute the 
     * contents of this list.
     *
     * @param   array   array object
     * @param   size  initial list size (note, not capacity)
     */
    protected PrimitiveList( Object array, int size ) {
        componentType_ = array.getClass().getComponentType();
        size_ = size;
        array_ = array;
    }

    /**
     * Returns the current size of this list.
     *
     * @return number of elements which have been stored
     */
    public int size() {
        return size_;
    }

    /**
     * Removes all of the elements from this list (optional operation).
     * The list will be empty after this call returns.
     */
    public void clear() {
        size_ = 0;
    }

    /**
     * Creates and returns a copy of the contents of this list, in the
     * form of a primitive array of the right length to hold all the
     * elements it currently contains.
     *
     * @return   array containing contents of this list
     */
    public Object toArray() {
        Object array = Array.newInstance( componentType_, size_ );
        System.arraycopy( array_, 0, array, 0, size_ );
        return array;
    }

    /**
     * Ensures that the capacity of this list is at least a given size.
     *
     * @param  minCapacity  minimum capacity
     */
    void ensureCapacity( int minCapacity ) {
        if ( minCapacity > Array.getLength( array_ ) ) {
            expandCapacity( minCapacity );
        }
    }

    /**
     * Increases the size of the list by a given increment.
     *
     * @param  inc  size increment
     */
    void expandSize( int inc ) {
        ensureCapacity( size_ + inc );
        size_ += inc;
    }

    /**
     * Checks that a given index is accessible, that is, represents a
     * defined element of this list.
     *
     * @param   i   index to check
     * @throws   IndexOutOfBoundsException  unless <code>i&lt;size()</code>
     */
    void checkIndex( int i ) {
        if ( i >= size_ ) {
            throw new IndexOutOfBoundsException();
        }
    }

    /**
     * Appends all the elements of a second list to this list.
     *
     * <p>The other list must be of the same type as this one;
     * that constraint should be enforced by type-specific concrete subclasses
     * of PrimitiveList.
     *
     * @param  other  other list
     * @return   true iff this collection changed as a result of the call
     */
    boolean addAll( PrimitiveList other ) {
        return addArrayElements( other.array_, other.size() );
    }

    /**
     * Appends elements from a given array to this list.
     *
     * <p>The supplied array's type must match the primitive array type
     * associated with this list;
     * that constraint should be enforced by type-specific concrete subclasses
     * of PrimitiveList.
     *
     * @param  other  array from which to copy
     * @param  n   number of elements (starting at 0) to copy
     * @return   true iff this collection changed as a result of the call
     */
    boolean addArrayElements( Object array, int n ) {
        if ( n > 0 ) {
            int pos = size();
            expandSize( n );
            System.arraycopy( array, 0, array_, pos, n );
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Determines by how much the storage array will grow if it needs to
     * expand.  
     * Any return value is legal; if a value less than the 
     * <code>currentCapacity</code> is returned, Integer.MAX_VALUE will be
     * used.  The effect of this is that implementations probably do not
     * need to worry about integer arithmetic overflow.
     * May be overridden by subclasses.
     *
     * @param  currentCapacity  initial size of buffer
     * @return   next size up
     */
    protected int nextCapacity( int currentCapacity ) {
        return ( currentCapacity * 3 ) / 2 + 1;
    }

    /**
     * Expands the capacity of this list so that it is at least a given
     * size.
     *
     * @param   minCapacity  minimum capacity required
     */
    private void expandCapacity( int minCapacity ) {
        int newCapacity = nextCapacity( Array.getLength( array_ ) );
        if ( newCapacity < 0 ) {
            newCapacity = Integer.MAX_VALUE;
        }
        else if ( minCapacity > newCapacity ) {
            newCapacity = minCapacity;
        }
        Object array = Array.newInstance( componentType_, newCapacity );
        System.arraycopy( array_, 0, array, 0, size_ );
        array_ = array;
    }
}
