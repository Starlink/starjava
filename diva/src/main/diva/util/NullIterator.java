/*
 * $Id: NullIterator.java,v 1.4 2000/05/02 00:45:25 johnr Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An iterator over nothing.
 *
 * @author John Reekie      (johnr@eecs.berkeley.edu)
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.4 $
 */
public class NullIterator implements Iterator {
  public boolean hasNext() {
    return false;
  }
  public Object next() {
    throw new NoSuchElementException("No more elements");
  }
  public void remove() {

    throw new UnsupportedOperationException("Can't remove element");
  }
}

