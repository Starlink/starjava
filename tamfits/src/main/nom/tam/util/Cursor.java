package nom.tam.util;


/** This interface extends the Iterator interface
 *  to allow insertion of data and move to previous entries
 *  in a collection.
 */

public interface Cursor extends java.util.Iterator {
    
    /** Is there a previous element in the collection? */
    public abstract boolean hasPrev();
    
    /** Get the previous element */
    public abstract Object  prev() throws java.util.NoSuchElementException;
    
    /** Point the list at a particular element.
     *  Point to the end of the list if the key is not found.
     */
    public abstract void setKey(Object key);
    
    /** Add an unkeyed element to the collection.
     *  The new element is placed such that it will be called
     *  by a prev() call, but not a next() call.
     */
    public abstract void add(Object reference);
    
    /** Add a keyed element to the collection.
     *  The new element is placed such that it will be called
     *  by a prev() call, but not a next() call.
     */
    public abstract void add(Object key, Object reference);
}
