/*
 * $Id: FilteredIterator.java,v 1.6 2000/05/02 00:45:24 johnr Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.util;
import java.util.Iterator;

/**
 * An iterator that takes another iterator, and applies a filter
 * to each element that it gets.
 *
 * @author John Reekie      (johnr@eecs.berkeley.edu)
 * @version $Revision: 1.6 $
 */
public class FilteredIterator implements Iterator {
    private Filter _filter;
    private Iterator _iterator;
    private Object _nextObject = null;

    public FilteredIterator(Iterator i, Filter f) {
        _iterator = i;
	_filter = f;
    }

    public boolean hasNext() {
        if (_nextObject != null) {
            return true;
        } else {
            getNext();
            if (_nextObject != null) {
                return true;
            }
        }
        return false;
    }

    public Object next() {
        if (_nextObject == null) {
            getNext();
        }
        Object result = _nextObject;
        _nextObject = null;
        return result;
    }

    public void remove() {
      // FIXME: we should probably be able to do this...
        throw new UnsupportedOperationException(
                "Filtered iterator cannot delete element");
    }

    private void getNext() {
        while (_iterator.hasNext()) {
            Object o = _iterator.next();
            if (_filter.accept(o)) {
                _nextObject = o;
                break;
            }
        }
    }
}

