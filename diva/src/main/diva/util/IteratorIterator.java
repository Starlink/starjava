/*
 * $Id: IteratorIterator.java,v 1.2 2000/05/02 00:45:25 johnr Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.util;
import java.util.Iterator;

/**
 * An iterator that takes an iterator over objects that themselves
 * produce iterators, and which iterators over the elements in
 * the sub-iterators. To get the iterators from the objects
 * iterated over, the method iterator() must be over-ridden to
 * convert a given result from the top-level iterator into a
 * sub-iterator.
 *
 * @author John Reekie      (johnr@eecs.berkeley.edu)
 * @version $Revision: 1.2 $
 */
public abstract class IteratorIterator implements Iterator {
    private Iterator _iterator;
    private Iterator _subiterator = null;

    /* Construct a new iterator over the contents of the given iterator
     */
    public IteratorIterator(Iterator i) {
        _iterator = i;
        if (_iterator.hasNext()) {
            _subiterator = iterator(_iterator.next());
        }
    }

    /* Test if there are more elements. This will return false if
     * the top-level iterator has no more elements.
     */
    public boolean hasNext() {
        if (!_subiterator.hasNext()) {
            while (!_subiterator.hasNext()) {
                _subiterator = iterator(_iterator.next());
            }
        }
        return (_subiterator != null);
    }

    /* Convert an object returned by the top-level iterator
     * into a sub-itreator.
     */
    abstract protected Iterator iterator (Object o);

    /* Return the next object.
     */
    public Object next() {
        while (!_subiterator.hasNext()) {
            _subiterator = iterator(_iterator.next());
        }
        if (_subiterator == null) {
            throw new RuntimeException("Ack! No more elements");
        }
        return (_subiterator.next());
    }

    public void remove() {
        // FIXME: we should probably be able to do this...
        throw new UnsupportedOperationException(
                "Iterator iterator cannot delete element");
    }
}

