/*
 * $Id: SimpleGraphController.java,v 1.4 2002/09/21 00:08:16 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.graphx.tutorial;

import diva.graphx.GraphController;
import diva.graphx.GraphModel;
import diva.graphx.NodeController;
import diva.graphx.EdgeController;

import diva.graphx.interaction.EdgeCreator;
import diva.graphx.interaction.NodeInteractor; 

import diva.graphx.toolbox.BasicEdge;
import diva.graphx.toolbox.BasicNode;
import diva.graphx.toolbox.BasicGraphModel;
import diva.graphx.toolbox.DeletionListener;
import diva.graphx.toolbox.ClearDeletionPolicy;

// import diva.graph.layout.*;

import diva.canvas.Figure;
import diva.canvas.FigureLayer;
import diva.canvas.GraphicsPane;
import diva.canvas.JCanvas;
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
 * A concrete subclass of GraphController that we use in the tutorial
 * examples. This subclasses uses BasicEdgeAdapter and BasicNodeAdapter.
 * It also sets up some default interaction on the pane which works
 * for creating nodes and edges between them.
 *
 * @author 	Michael Shilman (michaels@eecs.berkeley.edu)
 * @author 	John Reekie (johnr@eecs.berkeley.edu)
 * @version	$Revision: 1.4 $
 * @rating      Red
 */
public class SimpleGraphController extends GraphController {

    /** The selection interactor for drag-selecting nodes
     */
    private SelectionDragger _selectionDragger;

   /** The interactor for creating new nodes
     */
    private NodeCreator _nodeCreator;

    /** The interactor that interactively creates edges
     */
    private EdgeCreator _edgeCreator;

    /** The single node controller
     */
    private NodeController _nodeController;
    
    /** The single edge controller
     */
    private EdgeController _edgeController;


    /** The filter for control operations
     */
    private MouseFilter _controlFilter = new MouseFilter (
            InputEvent.BUTTON1_MASK,
            InputEvent.CTRL_MASK);

    /**
     * Create a new controller with default node and edge controllers.
     */
    public SimpleGraphController (GraphicsPane pane, GraphModel model) {
	setGraphicsPane(pane);
	setGraphModel(model);

	// Create sub controllers
	_nodeController = new SimpleNodeController(this);
	_edgeController = new SimpleEdgeController(this);

        // Create and set up the selection dragger
        _selectionDragger = new SelectionDragger(pane);
        _selectionDragger.addSelectionInteractor(
                (SelectionInteractor)_edgeController.getEdgeInteractor());
	_selectionDragger.addSelectionInteractor(
                (SelectionInteractor)_nodeController.getNodeInteractor());

        // Create a listener that creates new nodes
        _nodeCreator = new NodeCreator();
        _nodeCreator.setMouseFilter(_controlFilter);
        pane.getBackgroundEventLayer().addInteractor(_nodeCreator);

        // Create the interactor that drags new edges.
	_edgeCreator = new EdgeCreator(this) {
	    public Object createEdge() {	
		return new BasicEdge();
	    }
	};
	_edgeCreator.setMouseFilter(_controlFilter);
	((NodeInteractor)_nodeController.getNodeInteractor()).addInteractor(_edgeCreator);

	// Create a key listener that deletes nodes
	JCanvas canvas = pane.getCanvas();
	DeletionListener deletionListener = new DeletionListener(this, canvas);
	deletionListener.setDeletionPolicy(new ClearDeletionPolicy());
    }

    /**
     * Return the edge controller.
     */
    public EdgeController getEdgeController(Object edge) {
        return _edgeController;
    }    

    /**
     * Return the node controller.
     */
    public NodeController getNodeController(Object node) {
        return _nodeController;
    }

    /**
     * Initialize interaction on the graph pane. In this controller,
     * we set up interaction for the usual dragging and selection.
     * Also, control-click in the pane creates a new node, and
     * control-click on a node creates and starts dragging a new
     * edge.
     */
    protected void initializeInteraction () {
        GraphicsPane pane = getGraphicsPane();

        // Create and set up the selection dragger
        _selectionDragger = new SelectionDragger(pane);
        _selectionDragger.addSelectionInteractor(
                (SelectionInteractor)_edgeController.getEdgeInteractor());
	_selectionDragger.addSelectionInteractor(
                (SelectionInteractor)_nodeController.getNodeInteractor());

        // Create a listener that creates new nodes
        _nodeCreator = new NodeCreator();
        _nodeCreator.setMouseFilter(_controlFilter);
        pane.getBackgroundEventLayer().addInteractor(_nodeCreator);

        // Create the interactor that drags new edges.
	_edgeCreator = new EdgeCreator(this) {
	    public Object createEdge() {	
		return new BasicEdge();
	    }
	};
	_edgeCreator.setMouseFilter(_controlFilter);
	((NodeInteractor)_nodeController.getNodeInteractor()).addInteractor(_edgeCreator);
    }

    ///////////////////////////////////////////////////////////////
    //// NodeCreator

    /** An inner class that places a node at the clicked-on point
     * on the screen, if control-clicked with mouse button 1. This
     * needs to be made more customizable.
     */
    protected class NodeCreator extends AbstractInteractor {
        public void mousePressed(LayerEvent e) {
	    Object node = new BasicNode();
	    addNode(node,  e.getLayerX(), e.getLayerY());
	}
    }
}
