/*
 * $Id: FigureFactory.java,v 1.4 2000/05/02 00:43:36 johnr Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.canvas.test;
import diva.util.jester.*;

import diva.canvas.*;

/**
 * The figure factory is an interface that concrete factories
 * must implement.
 *
 * @author John Reekie
 * @version $Revision: 1.4 $
 */
public interface FigureFactory {
  /** Create a figure
   */
  public Figure createFigure ();
}


