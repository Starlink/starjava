/*
 * $Id: Filter.java,v 1.5 2001/07/22 22:02:03 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.util;

/**
 * An interface for objects that filter other objects
 *
 * @author John Reekie      (johnr@eecs.berkeley.edu)
 * @version $Revision: 1.5 $
 */
public interface Filter {
  /** Test if an object passes the filter, returning true
   * if it does and false if it does not.
   */
  public boolean accept (Object o);
}


