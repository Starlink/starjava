/*
 * $Id: EdgeInteractor.java,v 1.7 2000/05/02 00:44:12 johnr Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.graph;

import diva.canvas.*;
import diva.canvas.event.*;
import diva.canvas.interactor.*;
import java.awt.event.*;

/**
 * An interactor for edges.
 *
 * @author 	Michael Shilman (michaels@eecs.berkeley.edu)
 * @author 	John Reekie (johnr@eecs.berkeley.edu)
 * @version	$Revision: 1.7 $
 * @rating Red
 */
public class EdgeInteractor extends SelectionInteractor {

    /** Create a new edge interactor.
     */
    public EdgeInteractor () {
       super();
    }

    /** Create a new edge interactor that belongs to the given
     * controller and that uses the given selection model
     */
    public EdgeInteractor (SelectionModel sm) {
        this();
        super.setSelectionModel(sm);
    }
}

