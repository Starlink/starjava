/*
 * $Id: ProxyIterator.java,v 1.2 2000/05/02 00:45:25 johnr Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */

package diva.util;
import java.util.Iterator;

/**
 * An iterator that takes another iterator, and iterates
 * over it.  This meant to be extended by over-riding the
 * next() method.
 *
 * @author Michael Shilman     (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.2 $
 */
public class ProxyIterator implements Iterator {
    private Iterator _iterator;

    public ProxyIterator(Iterator i) {
       _iterator = i;
    }

    public boolean hasNext() {
        return _iterator.hasNext();
    }

    public Object next() {
        return _iterator.next();
    }

    public void remove() {
        _iterator.remove();
    }
}

