/*
 * $Id: MenuFactory.java,v 1.4 2000/05/18 18:43:54 neuendor Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.gui.toolbox;

import javax.swing.*;
import diva.canvas.*;

/**
 * A factory for popup menus.  Use this class in conjuction with
 * a MenuCreator to implement context sensitive menus.
 *
 * @author Steve Neuendorffer (neuendor@eecs.berkeley.edu)
 * @version $Revision: 1.4 $
 */
public abstract class MenuFactory {
    /**
     * Create an instance of the menu associated with this factory.
     * If no menu should be displayed, then return null.
     */
    public abstract JContextMenu create(Figure figure);
}

