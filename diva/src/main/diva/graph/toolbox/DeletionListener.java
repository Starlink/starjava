/*
 * $Id: DeletionListener.java,v 1.4 2001/07/22 22:01:26 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.graph.toolbox;

import diva.canvas.interactor.SelectionModel;
import diva.canvas.Figure;
import diva.graph.*;
import diva.gui.*;
import diva.gui.toolbox.*;

import java.awt.event.*;
import java.util.*;

import javax.swing.*;

/**
 * This class provides deletion support for most simple JGraph
 * components.  Associate this class with some action (such as a key
 * press on an instance of the JGraph class).  Any nodes or edges in
 * the selection model of the graph pane's controller will be removed.
 *
 * @author Steve Neuendorffer
 * @version $Revision: 1.4 $
 * @rating Red
 */
public class DeletionListener implements ActionListener {
    /** 
     * Delete any nodes or edges from the graph that are currently
     * selected.  In addition, delete any edges that are connected to
     * any deleted nodes.
     */
    public void actionPerformed(ActionEvent e) {
	JGraph jgraph = (JGraph) e.getSource();
	GraphPane graphPane = jgraph.getGraphPane();
	GraphController controller =
	    (GraphController)graphPane.getGraphController();
	GraphModel graphModel = controller.getGraphModel();
	SelectionModel model = controller.getSelectionModel();
	Object selection[] = model.getSelectionAsArray();
	Object userObjects[] = new Object[selection.length];
	// First remove the selection.
	for(int i = 0; i < selection.length; i++) {
	    userObjects[i] = ((Figure)selection[i]).getUserObject();
	    model.removeSelection(selection[i]);
	}

	// Remove all the edges first, since if we remove the nodes first,
	// then removing the nodes might remove some of the edges.
	for(int i = 0; i < userObjects.length; i++) {
	    Object userObject = userObjects[i];
	    if(graphModel.isEdge(userObject)) {
		controller.removeEdge(userObject);
	    }
	}
	for(int i = 0; i < selection.length; i++) {
	    Object userObject = userObjects[i];
	    if(graphModel.isNode(userObject)) {
		controller.removeNode(userObject);
	    }
	}
    }
}

