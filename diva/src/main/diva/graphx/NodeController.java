/*
 * $Id: NodeController.java,v 1.4 2002/09/20 02:41:26 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.graphx;

import diva.graphx.event.GraphViewEvent;
import diva.graphx.interaction.NodeInteractor;

import diva.canvas.CanvasComponent;
import diva.canvas.CanvasUtilities;
import diva.canvas.CompositeFigure;
import diva.canvas.Figure;
import diva.canvas.FigureContainer;
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

import java.awt.geom.*;
import java.awt.event.InputEvent;
import java.util.HashMap;
import java.util.Iterator;

/**
 * A node controller is the object responsible for managing
 * all instances of a particular type of node, and the corresponding
 * Figures that are used to represent them on the canvas. Each
 * graph controller contains one or more node controllers.
 *
 * <P> Node controllers assume that each node is represented by a 
 * single Figure on the canvas. (If the node is composite, then
 * of course it's Figure also has child figures, corresponding to
 * each child node.) The mapping between figures and node objects
 * is set up in two ways:
 * <ol>
 * <li> Each Figure has its "user object" set to the corresponding node
 * <li> Each node is an index into a hashtable contained in the
 * node controller, and which references the figure.
 * </ol>
 *
 * <P>The binding between a node and its figure is exposed by the
 * methods bind() and unbind(), which add and remove a binding. These
 * methods are fairly low-level and not really intended for general
 * use. The method getFigure() looks up a binding ie gets the
 * figure representing a node, and is what clients should use anytime
 * they need to get the visual representation of a node.
 * <P>
 * This class has one abstract method which must be implemented by
 * application-specific subclasses, renderNode(). This method
 * creates a Figure given a node. A second method, drawNodeChildren(),
 * should be overridden by any subclass that manages composite
 * nodes.
 * <P>
 * Finally, concrete subclasses that want to change the default
 * interactor will do so in the subclass' constructor. Subclass
 * constructors must always call super() to ensure that the controller
 * is properly initialized.
 * 
 * @author 	Steve Neuendorffer
 * @author 	John Reekie
 * @version	$Revision: 1.4 $
 * @rating      Red
 */
public abstract class NodeController {

    /** The mapping from nodes to figures
     */
    private HashMap _map = new HashMap();

    /** The selection interactor for drag-selecting nodes
     */
    private SelectionDragger _selectionDragger;

    /** The filter for control operations
     */
    private MouseFilter _controlFilter = new MouseFilter (
            InputEvent.BUTTON1_MASK,
            InputEvent.CTRL_MASK);

    // The interactor used to operate on nodes
    private Interactor _interactor;

    // The graph controller this node controller belongs to
    private GraphController _controller;

    /**
     * Create a new node controller with a default node interactor.
     * This interactor is an instance of
     * diva.graphx.interaction.NodeInteractor on the selection model
     * from the GraphController. If the default is not suitable,
     * subclass constructors can call setNodeInteractor after calling super().
     * Note: super() must be called from subclass constructors.
     */
    public NodeController (GraphController controller) {
	_controller = controller;
        SelectionModel sm = controller.getSelectionModel();
        _interactor = new NodeInteractor(controller, sm);       
    }

    /**
     * Create or overwrite a binding from a node to a figure.
     */
    public void bind (Object node, Figure figure) {
	_map.put(node, figure);
	figure.setUserObject(node);
    }

    /** 
     * Add the given node to this graph editor, inside the given parent node
     * and render it at the given location relative to its parent. The
     * children of the nodes are *not* added -- these must be done by using
     * the addNodeChildren(method).
     */
    public void addNode (Object node, Object parent, double x, double y) {
        GraphModel model = _controller.getGraphModel();

	// Add the node to the model. This will generate a NODE_ADDED
	// event to any listeners to the model
	model.addNode(_controller, node, parent);

	// Draw it
	drawNode(node, parent, x, y);
    }

    /** 
     * Draw the given node at the specified location. If the node
     * is composite, then this method will call the drawNodeChildren() method
     * to draw the node's children.
     */
    public void drawNode(Object node, Object parent, double x, double y) {
	FigureContainer container;

	if (parent == _controller.getGraphModel().getRoot()) {
	    container = _controller.getGraphicsPane().getForegroundLayer();
	} else {
	    container = (FigureContainer) getFigure(parent);
	}

	// Render the figure and initialize it
        Figure figure = renderNode(node, parent, container, x, y);
        figure.setInteractor(getNodeInteractor());
	bind(node, figure);

	// Notify anything listening for view events
	_controller.dispatch(new GraphViewEvent(this,
			     GraphViewEvent.NODE_DRAWN, figure, null));

	// If the node is composite, draw the children
	if (_controller.getGraphModel().getNodeAdapter(node).isComposite(node)) {
	    drawNodeChildren(node, figure);
	}
    }

    /** 
     * Draw the children of the given node. The container argument
     * is the Figure that was previously drawn for this node. Subclasses
     * that control composite nodes must override this method to iterate
     * through child nodes and call drawNode() on the appropriate
     * controller for each of them.
     */
    public void drawNodeChildren(Object parent, Figure container) {
	; // By default, do nothing
    }

    /** 
     * Return the graph controller containing this controller.
     */
    public GraphController getController() {
	return _controller;
    }

    /** 
     * Get the figure corresponding to the given node. Returns
     * null if the node is not known to this controller.
     */
    public Figure getFigure (Object node) {
	return (Figure) _map.get(node);
    }

    /** 
     * Return the node interactor associated with this controller.
     */
    public Interactor getNodeInteractor() {
	return _interactor;
    }

    /** 
     * Remove the node and its associated figure. This method assumes that
     * any edges connected to the node <i>or any of its children</i> have already
     * been disconnected. Failure to do so on the part of a client will result
     * in errors.
     */
    public void removeNode (Object node) {
	undrawNode(node);
        _controller.getGraphModel().removeNode(_controller, node);
	_controller.getGraphicsPane().repaint();
     }

    /**
     * Render a visual representation of the given node, at the given
     * location, and add it to the given figure container. If the node
     * is at the top level of the graph, then the container will be a
     * FigureLayer, otherwise it will be a Figure class that also
     * implements the FigureContainer interface.
     */
    public abstract Figure renderNode (Object node,
				       Object parent,
				       FigureContainer container,
				       double x, double y);

    /** 
     * Set the node interactor for this controller
     */
    public void setNodeInteractor (Interactor interactor) {
	_interactor = interactor;
    }

    /**
     * Remove a binding from a node to a figure. Throw an
     * exception if the binding is not valid. (This is very very
     * bad and should not happen. If it does then there is a serious
     * bug somewhere else.)
     */
    public void unbind (Object node, Figure figure) {
	figure.setUserObject(null);
	Figure f = (Figure) _map.remove(node);
	if (figure != f) {
	    throw new RuntimeException("Bad binding in NodeController: "
				       + figure + " is not equal to " + f);
	}
    }

    /** 
     * Remove the figure for the given node. If there are any edges connected
     * to the nodes and their figures have not also been undrawn, then things
     * might get weird, so be careful how you call this. In general, if you
     * want to redraw sections of a graph, you should use
     * GraphController.rerenderSubgraph().
     *
     * @see GraphController#rerenderSubgraph(Iterator);
     */
    public void undrawNode(Object node) {
        Figure figure = getFigure(node);
        if (figure != null) {
	    unbind(node, figure);
	    unrenderNode(node, figure);

	    // Notify anything listening for view events
	    _controller.dispatch(new GraphViewEvent(this,
				 GraphViewEvent.NODE_DRAWN, null, figure));

        }
    }

    /**
     * Remove the visual representation of the given node. Implementors
     * should remove the node from the selection (if this type of node
     * is selectable), then remove the figure from its parent.
     */
    public abstract void unrenderNode (Object node, Figure figure);
}
