/*
 * $Id: BasicGraphController.java,v 1.9 2001/07/22 22:01:19 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.graph.basic;

import diva.graph.*;
import diva.graph.layout.*;

import diva.canvas.Figure;
import diva.canvas.FigureLayer;
import diva.canvas.GraphicsPane;
import diva.canvas.Site;

import diva.canvas.connector.AutonomousSite;
import diva.canvas.connector.CenterSite;
import diva.canvas.connector.PerimeterSite;
import diva.canvas.connector.PerimeterTarget;
import diva.canvas.connector.Connector;
import diva.canvas.connector.ConnectorAdapter;
import diva.canvas.connector.ConnectorManipulator;
import diva.canvas.connector.ConnectorEvent;
import diva.canvas.connector.ConnectorListener;
import diva.canvas.connector.ConnectorTarget;

import diva.canvas.event.LayerAdapter;
import diva.canvas.event.LayerEvent;
import diva.canvas.event.MouseFilter;

import diva.canvas.interactor.Interactor;
import diva.canvas.interactor.AbstractInteractor;
import diva.canvas.interactor.GrabHandle;
import diva.canvas.interactor.SelectionInteractor;
import diva.canvas.interactor.SelectionModel;
import diva.canvas.interactor.SelectionDragger;

import diva.util.Filter;

import java.awt.event.InputEvent;
import java.util.HashMap;

/**
 * A basic implementation of GraphController, which works with
 * simple graphs that have edges connecting simple nodes. It
 * sets up some simple interaction on its view's pane.
 *
 * @author 	Michael Shilman (michaels@eecs.berkeley.edu)
 * @version	$Revision: 1.9 $
 * @rating      Red
 */
public class BasicGraphController extends SimpleGraphController {
    /**
     * The global count for the default node/edge creation.
     */
    private int _globalCount = 0;

    /** The selection interactor for drag-selecting nodes
     */
    private SelectionDragger _selectionDragger;

   /** The interactor for creating new nodes
     */
    private NodeCreator _nodeCreator;

    /** The interactor that interactively creates edges
     */
    private EdgeCreator _edgeCreator;

    /** The filter for control operations
     */
    private MouseFilter _controlFilter = new MouseFilter (
            InputEvent.BUTTON1_MASK,
            InputEvent.CTRL_MASK);

    /**
     * Create a new basic controller with default node and edge controllers.
     */
    public BasicGraphController () {
	NodeController nc = new BasicNodeController(this);
	nc.setNodeRenderer(new BasicNodeRenderer(this));
	setNodeController(nc);
	
	BasicEdgeController ec = new BasicEdgeController(this);
	ec.setEdgeRenderer(new BasicEdgeRenderer());
	setEdgeController(ec);

        //	addGraphViewListener(new IncrementalLayoutListener(new IncrLayoutAdapter(new LevelLayout(new BasicLayoutTarget(this))), null));
    }

    /**
     * Initialize all interaction on the graph pane. This method
     * is called by the setGraphPane() method of the superclass.
     * This initialization cannot be done in the constructor because
     * the controller does not yet have a reference to its pane
     * at that time.
     */
    protected void initializeInteraction () {
        GraphPane pane = getGraphPane();

        // Create and set up the selection dragger
        _selectionDragger = new SelectionDragger(pane);
        _selectionDragger.addSelectionInteractor(
                (SelectionInteractor)getEdgeController().getEdgeInteractor());
	_selectionDragger.addSelectionInteractor(
                (SelectionInteractor)getNodeController().getNodeInteractor());

        // Create a listener that creates new nodes
        _nodeCreator = new NodeCreator();
        _nodeCreator.setMouseFilter(_controlFilter);
        pane.getBackgroundEventLayer().addInteractor(_nodeCreator);

        // Create the interactor that drags new edges.
	_edgeCreator = new EdgeCreator(this) {
	    public Object createEdge() {	
		Object semanticObject = Integer.valueOf(_globalCount++);
		BasicGraphModel bgm = (BasicGraphModel)getGraphModel();
		return bgm.createEdge(semanticObject);
	    }
	};
	_edgeCreator.setMouseFilter(_controlFilter);
	((NodeInteractor)getNodeController().getNodeInteractor()).addInteractor(_edgeCreator);
    }

    ///////////////////////////////////////////////////////////////
    //// NodeCreator

    /** An inner class that places a node at the clicked-on point
     * on the screen, if control-clicked with mouse button 1. This
     * needs to be made more customizable.
     */
    protected class NodeCreator extends AbstractInteractor {
        public void mousePressed(LayerEvent e) {
            Object semanticObject = Integer.valueOf(_globalCount++);
            BasicGraphModel bgm = (BasicGraphModel)getGraphModel();
            Object node = bgm.createNode(semanticObject);
	    addNode(node,  e.getLayerX(), e.getLayerY());
	}
    }
}


