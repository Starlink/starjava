/*
 * $Id: ArrayIterator.java,v 1.4 2002/01/12 00:06:41 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.util;
import java.util.NoSuchElementException;

/**
 * An iterator over a given array.  Treats "null" as an
 * empty array.
 *
 * @author Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.4 $
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
}
