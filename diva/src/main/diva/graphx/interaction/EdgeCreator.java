/*
 * $Id: EdgeCreator.java,v 1.3 2002/08/23 22:26:01 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.graphx.interaction;

import diva.graphx.*;

import diva.canvas.Figure;
import diva.canvas.FigureLayer;
import diva.canvas.GraphicsPane;
import diva.canvas.Site;

import diva.canvas.connector.*;

import diva.canvas.event.LayerAdapter;
import diva.canvas.event.LayerEvent;
import diva.canvas.event.MouseFilter;

import diva.canvas.interactor.Interactor;
import diva.canvas.interactor.AbstractInteractor;
import diva.canvas.interactor.CompositeInteractor;
import diva.canvas.interactor.GrabHandle;
import diva.canvas.interactor.SelectionInteractor;
import diva.canvas.interactor.SelectionModel;
import diva.canvas.interactor.SelectionDragger;

import diva.util.Filter;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.awt.event.InputEvent;
import java.util.HashMap;

/** An interactor that interactively drags edges from one node
 * to another.
 *
 * @author 	Steve Neuendorffer (neuendor@eecs.berkeley.edu)
 * @version	$Revision: 1.3 $
 * @rating      Red
 */
public abstract class EdgeCreator extends AbstractInteractor {
    // The Controller that this creator is using.
    GraphController _controller;

    public EdgeCreator(GraphController controller) {
	_controller = controller;
    }

    /** Create a new edge, add it to the graph controller and add
     * the connector to the selection.
     */
    public void mousePressed(LayerEvent e) {
	Figure source = e.getFigureSource();
	FigureLayer layer = (FigureLayer) e.getLayerSource();
	
	Object edge = createEdge();
	
	// Add it to the editor
	_controller.addEdge( edge,
			     _controller.getGraphModel().getRoot());
	_controller.setTail(edge, source.getUserObject());

	// We know that the headSite is created at (0,0)
	Connector c = _controller.getEdgeController(edge).getConnector(edge);
	Site headSite = c.getHeadSite();
	headSite.translate(e.getLayerX(), e.getLayerY());
	c.reroute();

	// Add it to the selection so it gets a manipulator, and
	// make events go to the grab-handle under the mouse
	Figure ef = _controller.getEdgeController(edge).getConnector(edge);
	_controller.getSelectionModel().addSelection(ef);
	ConnectorManipulator cm = (ConnectorManipulator) ef.getParent();
	GrabHandle gh = cm.getHeadHandle();
	layer.grabPointer(e, gh);
    }

    /** Create a new Edge.  Subclasses should implement this method to create
     * an object that is consistent with the graph model being used.
     */
    public abstract Object createEdge();
}

