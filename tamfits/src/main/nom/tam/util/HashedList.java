package nom.tam.util;

/* Copyright: Thomas McGlynn 1999.
 * This code may be used for any purpose, non-commercial
 * or commercial so long as this copyright notice is retained
 * in the source code or included in or referred to in any
 * derived software.
 */


/** This class implements a structure which can
 *  be accessed either through a hash or
 *  as linear list.  Only some elements may have
 *  a hash key.
 * 
 *  This class is motivated by the FITS header
 *  structure where a user may wish to go through
 *  the header element by element, or jump directly
 *  to a given keyword.  It assumes that all
 *  keys are unique.  However, all elements in the
 *  structure need not have a key.
 * 
 *  This class does only the search structure
 *  and knows nothing of the semantics of the
 *  referenced objects.
 * 
 *  Users may wish to access the HashedList using
 *  HashedListIterator's which extends the Iterator
 *  interface to allow adding and deleting entries.
 * 
 */
import java.util.*;
import java.lang.reflect.Array;
import nom.tam.util.ArrayFuncs;



/** This class defines the elements used to
 *  construct the HashedList.
 *  External users should not need to see this class.
 */
class HashedListElement {
	/** The reference to the actual object being indexed. */
    Object 			reference;
	/** The key to the object -- may be null. */
    Object 			key;
	/** A pointer to the next object in the list */
    HashedListElement 		next;
	/** A pointer to the previous object in the list */
    HashedListElement   	prev;
}

public class HashedList implements Collection {
    
    /** This inner class defines an iterator over the hashed list.
     *  An iterator need not start at the beginning of the list.
     *  <p>
     *  This class can be used by external users to both add
     *  and delete elements from the list.  It implements the
     *  standard Iterator interface but also provides methods
     *  to add keyed or unkeyed elements at the current location.
     *  <p>
     *  Users may move either direction in the list using the
     *  next and prev calls.  Note that a call to prev followed
     *  by a call to next (or vice versa) will return the same
     *  element twice.
     *  <p>
     *  The class is implemented as an inner class so that it can
     *  easily access the state of the associated HashedList.
     */
    public class HashedListIterator implements Cursor {
	
	HashedListIterator(HashedListElement start) {
	    current = start;
	}
	
	/** The element that will be returned by next. */
	private HashedListElement current;
	/** The last element that was returned by next. */
	private HashedListElement last;
	
	/** Is there another element? */
	public boolean hasNext() {
	    return current != null;
	}
	
	/** Is there a previous element? */
	public boolean hasPrev() {
	    if (current == null) {
		return last != null;
	    } else {
		return current.prev != null;
	    }
	}
	
	/** Get the next entry. */
	public Object next() throws NoSuchElementException {
	    
	    if (current != null) {
		last = current;
		current = current.next;
		return last.reference;
	    } else {
		throw new NoSuchElementException("Beyond end of list");
	    }
	}
	
	/** Get the previous entry. */
	public Object prev() throws NoSuchElementException {
	    last = null;
	    if (current == null) {
		if (last == null) {
		    throw new NoSuchElementException("Empty list");
		} else {
		    current = last;
		    return current.reference;
		}
	    } else {
		if (current.prev == null) {
		    throw new NoSuchElementException("Before beginning of list");
		} else {
		    current= current.prev;
		    return current.reference;
		}
	    }
	}
	
	/** Remove the last retrieved entry. Note that remove can
	 *  be called only after a call to next, and only once per such
	 *  call.  Remove cannot be called after a call to prev.
	 */
	public void remove() {
	    if (last == null) {
		throw new IllegalStateException("Removed called in invalid iterator state");
	    } else {
		HashedList.this.remove(last);
		last = null;
	    }
	}
	
	/** Add an entry at the current location. The new entry goes before
	 *  the entry that would be returned in the next 'next' call, and
	 *  that call will not be affected by the insertion. 
	 *  Note: this method is not in the Iterator interface.
	 */
	public void add(Object ref) {
	    HashedListElement newObj = HashedList.this.add(current, null, ref);
	    current = newObj.next;
	}
	
	/** Add a keyed entry at the current location. The new entry is inserted
	 *  before the entry that would be returned in the next invocation of
	 *  'next'.  The return value for that call is unaffected.
	 *  Note: this method is not in the Iterator interface.
	 */
	public void add(Object key, Object ref) {
	    HashedListElement newObj = HashedList.this.add(current, key, ref);
	    current = newObj.next;
	}
	
	/** Point the iterator to a particular keyed entry.  This
	 *  method is not in the Iterator interface.
	 *  @param key
	 */
	public void setKey(Object key) {
	    if (HashedList.this.containsKey(key)) {
		setCurrent((HashedListElement) hash.get(key));
	    } else {
		current = null;
	    }
	}
	
	/** Point the iterator to a specific entry. */
	void setCurrent(HashedListElement newPos) {
	    current = newPos;
	}
	    
    }
    
    /*****      *** Instance Variables ***      *****/
		
    /** The HashMap of keyed indices. */
    private HashMap hash = new HashMap();

    /** The first element of the list. */
    private HashedListElement first = null;
    
    /** The last element of the list. */
    private HashedListElement last  = null;
    
    /** The number of elements in the list. */
    private int numElements = 0;
    
    /****      *** Methods ***                *****/
    
    /** Add an element to the end of the list. */
    public boolean add(Object reference) {
	add(null, null, reference);
	return true;
	
    }
    
    /** Add a keyed element to the end of the list. */
    public boolean add(Object key, Object reference) {
	add(null, key, reference);
	return true;
    }
    
    /** Add an element to the list.
     *  @param pos    The element before which the current element 
     *                be placed.  If pos is null put the element at
     *                the end of the list.
     *  @param key    The hash key for the new object.  This may be null
     *                for an unkeyed entry.
     *  @param reference The actual object being stored.
     */
    HashedListElement add(HashedListElement pos, Object key, Object reference) {
	
	// First check if we need to delete another reference.
	if (key != null) {
	    // Does not do anything if key not found.
	    remove((HashedListElement) hash.get(key));   
	}
	
	HashedListElement e = new HashedListElement();
	e.key       = key;
	e.reference = reference;
	
	// Now put it in the list.
	if (pos == null) {
	    
	    // At the end...
	    e.prev = last;
	    if (last != null) {
		last.next = e;
	    } else {
		// Empty list...
		first = e;
	    }
	    
	    last = e;
	    e.next = null;
	    
	} else {
	    
	    if (pos.prev == null) {
		
		// At the beginning...
		e.next   = pos;
		e.prev   = null;
		pos.prev = e;
		first    = e;
		
	    } else {
		
		// In the middle...
		
		e.next = pos;
		e.prev = pos.prev;
		pos.prev = e;
		e.prev.next = e;
	    }
	}
	
	numElements += 1;
	
	// Put a pointer in the hash.
	if (key != null) {
	    hash.put(key, e);
	}
	return e;
    }
    
    /** Remove a keyed object from the list.  Unkeyed
     *  objects can be removed from the list using a
     *  HashedListIterator or using the remove(Object)
     *  method.
     */
    public boolean removeKey(Object key) {
	HashedListElement h = (HashedListElement) hash.get(key);
	if (h != null) {
	    remove(h);
	    return true;
	} else {
	    return false;
	}
    }
    
    /** Remove an object from the list.
     */
    public boolean remove(Object o) {
	Iterator iter = iterator();
	while(iter.hasNext()) {
	    if (iter.next().equals(o)) {
		iter.remove();
		return true;
	    }
	}
	return false;
    }
    
    /** Remove an element from the list.
     *  This method is also called by the HashedListIterator.
     *  @param e The element to be removed.
     */
    private void remove(HashedListElement e) {
	if (e == null) {
	    return;
	}
	if (e.prev != null) {
	    e.prev.next = e.next;
	} else {
	    first = e.next;
	}
	
	if (e.next != null) {
	    e.next.prev = e.prev;
	} else {
	    last = e.prev;
	}
	
	if (e.key != null) {
	    hash.remove(e.key);
	}
	
	numElements -= 1;
    }
    
    /** Return an iterator over the entire list.
     *  The iterator may be used to delete
     *  entries as well as to retrieve existing
     *  entries.  A knowledgeable user can
     *  cast this to a HashedListIterator and
     *  use it to add as well as delete entries.
     */
    public Iterator iterator() {
	return new HashedListIterator(first);
    }
    
    
    /** Return an iterator over the list starting
     *  with the entry with a given key. 
     */
    public HashedListIterator iterator(Object key) throws NoSuchElementException {
	return new HashedListIterator( (HashedListElement) hash.get(key));
    }
    
    /** Return an iterator starting with the n'th
     *  entry.
     */
    public HashedListIterator iterator(int n) throws NoSuchElementException {
	
	if (n == 0  && numElements == 0) {
	    return new HashedListIterator(first);
	}
	
	HashedListElement e = getElement(n);
	HashedListIterator i = (HashedListIterator) iterator();
	i.setCurrent(e);
	return i;
    }
    
    /** Return the value of a keyed entry.  Non-keyed
     *  entries may be returned by requesting an iterator.
     */
    public Object get(Object key) {
	
	if (!hash.containsKey(key)) {
	    return null;
	}
	    
	return ((HashedListElement)hash.get(key)).reference;
    }
    
    /** Return the n'th entry from the beginning. */
    public Object get(int n) throws NoSuchElementException {
	
	HashedListElement e = getElement(n);
	return e.reference;
    }
    
    /** Replace the key of a given element.
     *  @param  oldKey  The previous key.  This key must
     *                  be present in the hash.
     *  @param  newKey  The new key.  This key
     *                  must not be present in the hash.
     *  @return if the replacement was successful.
     */
    public boolean replaceKey(Object oldKey, Object newKey) {
	
	if (!hash.containsKey(oldKey) || hash.containsKey(newKey)) {
	    return false;
	}
	
	HashedListElement e = (HashedListElement) hash.get(oldKey);
	e.key = newKey;
	hash.remove(oldKey);
	hash.put(newKey, e);
	return true;
    }
    
    
    /** Get the n'th element of the list */
    HashedListElement getElement(int n) throws NoSuchElementException {
	
	if (n < 0) {
	    throw new NoSuchElementException ("Invalid index");
	} else {
	    HashedListElement current = first;
	    
	    while (n > 0 && current.next != null) {
		n -= 1;
		current = current.next;
	    }
	    
	    if (n > 0) {
		throw new NoSuchElementException("Index beyond end of list");
	    }
	    
	    if (current == null) {
		throw new NoSuchElementException("Empty list");
	    }
	    
	    return current;
	}
    }
    
    /** Check if the key is included in the list */
    public boolean containsKey(Object key) {
	return hash.containsKey(key);
    }
    
    /** Return the number of elements in the list. */
    public int size() {
	return numElements;
    }
    
    /** Add another collection to this one list.
     *  All entries are added as unkeyed entries to the end of the list.
     */
    public boolean addAll(Collection c) {
	Object[] array = c.toArray();
	for (int i=0; i<array.length; i += 1) {
	    add(array[i]);
	}
	return true;
    }
    
    /** Clear the collection */
    public void clear() {
	
	numElements = 0;
	first = null;
	last = null;
	hash.clear();
    }
    
    /** Does the HashedList contain this element? */
    public boolean contains(Object o) {
	
	Iterator iter = iterator();
	while(iter.hasNext()) {
	    if (iter.next().equals(o)) {
		return true;
	    }
	}
	return false;
    }
    
    /** Does the HashedList contain all the elements
     *  of this other collection.
     */
    public boolean containsAll(Collection c) {
	
	Object[] o = c.toArray();
	for (int i=0; i<o.length; i += 1) {
	    if (!contains(o[i])) {
		return false;
	    }
	}
	return true;
    }
    
    /** Is the HashedList empty? */
    public boolean isEmpty() {
	return numElements == 0;
    }

    /** Remove all the elements that are found in another collection. */
    public boolean removeAll(Collection c) {
	
	Object[] o = c.toArray();
	boolean result = false;
	for (int i=0; i<o.length; i += 1) {
	    result = result|remove(o[i]);
	}
	return result;
    }
    
    /** Retain only elements contained in another collection  */
    public boolean retainAll(Collection c) {
	
	Iterator iter = iterator();
	boolean result = false;
	while (iter.hasNext()) {
	    if (!c.contains(iter.next())) {
		iter.remove();
		result = true;
	    }
	}
	return result;
    }
    
    /** Convert to an array of objects */
    public Object[] toArray() {
	
	Object[] o = new Object[numElements];
	return toArray(o);
    }
    
    /** Convert to an array of objects of
     *  a specified type.
     */
    public Object[] toArray(Object[] o) {
	
	
	// This is not entirely correct.
	// The getBaseClass function will recurse down
	// a multi-dimensional array and it really should
	// go only a single level.  Getting the type
	// of the first element also will not work.
	// o.length may be 0, and o[0] might be a
	// more specific type than the array holding
	// it or null.  getBaseClass should work fine
	// unless we try to put arrays into the list.
	// (E.g., suppose the elements of the HashedList
	// are int[]s.  Then, getBaseClass will
	// return int.class and we will eventually
	// try to assign an object to a primitive.)
	// 
	// java.lang.reflect.Array should really include
	// the method we want.  Sigh....
	
	if (Array.getLength(o) < numElements) {
	    o = (Object[]) nom.tam.util.ArrayFuncs.newInstance(
		   nom.tam.util.ArrayFuncs.getBaseClass(o), numElements);
	}
	
	Iterator iter = iterator();
	int p = 0;
	while(iter.hasNext()) {
	    o[p] = iter.next();
	    p   += 1;
	}
	if (o.length > numElements) {
	    o[numElements] = null;
	}
	
	return o;
    }
    
}
	
