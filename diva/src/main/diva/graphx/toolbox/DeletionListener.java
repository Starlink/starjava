/*
 * $Id: DeletionListener.java,v 1.4 2002/09/21 00:08:15 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.graphx.toolbox;

import diva.graphx.*;

import diva.canvas.interactor.SelectionModel;
import diva.canvas.Figure;
import diva.canvas.JCanvas;
import diva.canvas.GraphicsPane;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import java.util.Iterator;

import javax.swing.JComponent;
import javax.swing.KeyStroke;

/**
 * This class provides deletion support for most simple graph
 * editors.  Associate this class with some action (such as a key
 * press on an instance of the JCanvas class).  Any nodes or edges in
 * the selection model of the graph pane's controller will be removed.
 *
 * @author Steve Neuendorffer, John Reekie
 * @version $Revision: 1.4 $
 * @rating Red
 */
public class DeletionListener implements ActionListener {

    // The graph controller this listener is associated with
    private GraphController _controller;

    // The deletion policy
    private DeletionPolicy _policy = new DefaultDeletionPolicy();

    /** Create a new listener but don't attach it to anything.
     */
    public DeletionListener (GraphController gc) {
	_controller = gc;
    }

    /** Create a new listener and attach it to the given JCanvas.
     */
    public DeletionListener (GraphController gc, JCanvas canvas) {
	_controller = gc;

        canvas.registerKeyboardAction(this, "Delete",
                KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        canvas.setRequestFocusEnabled(true);
    }

    /** 
     * Delete any nodes or edges from the graph that are currently
     * selected.
     */
    public void actionPerformed (ActionEvent e) {
	GraphModel graphModel = _controller.getGraphModel();
	SelectionModel selectionModel = _controller.getSelectionModel();
	Object figures[] = selectionModel.getSelectionAsArray();

	// Clear the selection.
	selectionModel.clearSelection();

	// Remove the edges
	for(int i = 0; i < figures.length; i++) {
	    Object userObject = ((Figure) figures[i]).getUserObject();
	    if(graphModel.isEdge(userObject)) {
		_controller.removeEdge(userObject);
	    }
	}

	// Remove nodes. Before removing a node, check if it has any edges
	// connected to it, and if so, call the deletion policy to handle
	for(int i = 0; i < figures.length; i++) {
	    Object userObject = ((Figure) figures[i]).getUserObject();
	    if(graphModel.isNode(userObject)) {
		Iterator edges = graphModel.inEdges(userObject);
		while (edges.hasNext()) {
		    _policy.connectedHead(_controller, graphModel,
					  edges.next(), userObject);
		}
		edges = graphModel.outEdges(userObject);
		while (edges.hasNext()) {
		    _policy.connectedTail(_controller, graphModel,
					  edges.next(), userObject);
		}
		_controller.removeNode(userObject);
	    }
	}
    }

    /** Get the delation policy
     */
    public DeletionPolicy getDeletionPolicy () {
	return _policy;
    }

    /** Set the deletion policy
     */
    public void setDeletionPolicy (DeletionPolicy p) {
	_policy = p;
    }
}
