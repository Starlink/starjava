/*
 * $Id: MenuFactory.java,v 1.6 2001/07/22 22:01:33 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.gui.toolbox;

import diva.canvas.Figure;

/**
 * A factory for popup menus.  Use this class in conjuction with
 * a MenuCreator to implement context sensitive menus.
 *
 * @author Steve Neuendorffer (neuendor@eecs.berkeley.edu)
 * @version $Revision: 1.6 $
 */
public interface MenuFactory {

    /**
     * Create an instance of the menu associated with this factory.
     * If no menu should be displayed, then return null.
     * @param figure The figure for which to create the menu.
     */
    public JContextMenu create(Figure figure);
}


