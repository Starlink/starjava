/*
 * $Id: Filter.java,v 1.4 2000/05/02 00:45:24 johnr Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.util;

/**
 * An interface for objects that filter other objects
 *
 * @author John Reekie      (johnr@eecs.berkeley.edu)
 * @version $Revision: 1.4 $
 */
public interface Filter {
  /** Test if an object passes the filter, returning true
   * if it does and false if it does not.
   */
  public boolean accept (Object o);
}

