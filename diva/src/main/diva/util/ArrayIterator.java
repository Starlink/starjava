/*
 * $Id: ArrayIterator.java,v 1.2 2000/05/02 00:45:24 johnr Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.util;
import java.util.NoSuchElementException;

/**
 * An iterator over a given array.  Treats "null" as an
 * empty array.
 *
 * @author Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.2 $
 */
public class ArrayIterator extends IteratorAdapter {
    private Object[] _array;
    private int _i;

    public ArrayIterator(Object[] array) {
        _array = array;
        _i = 0;
    }

    public boolean hasNext() {
        if(_array == null) {
            return false;
        }
        else {
            return _i < _array.length;
        }
    }

    public Object next() {
        if(hasNext()) {
            return _array[_i++];
        }
        else {
            throw new NoSuchElementException("No more elements");
        }
    }

    public void remove() {
        throw new UnsupportedOperationException("Can't remove element");
    }
}

