/*
 * $Id: EdgeController.java,v 1.4 2002/09/20 02:41:25 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.graphx;

import diva.graphx.event.GraphViewEvent;
import diva.graphx.interaction.EdgeInteractor;

import diva.canvas.CanvasComponent;
import diva.canvas.CompositeFigure;
import diva.canvas.FigureContainer;
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
import diva.canvas.connector.Terminal;

import diva.canvas.event.LayerAdapter;
import diva.canvas.event.LayerEvent;
import diva.canvas.event.MouseFilter;

import diva.canvas.interactor.Interactor;
import diva.canvas.interactor.AbstractInteractor;
import diva.canvas.interactor.BasicSelectionRenderer;
import diva.canvas.interactor.GrabHandle;
import diva.canvas.interactor.SelectionInteractor;
import diva.canvas.interactor.SelectionModel;
import diva.canvas.interactor.SelectionDragger;

import diva.util.Filter;

import java.awt.geom.*;
import java.awt.event.InputEvent;
import java.util.HashMap;

/**
 * A edge controller is the object responsible for managing
 * all instances of a particular type of edge, and the corresponding
 * Figures that are used to represent them on the canvas. Each
 * graph controller contains one or more edge controllers.
 *
 * <P> Edge controllers assume that each edge is represented by a 
 * single Connector on the canvas. (If the edge is composite, then
 * of course it's Connector also has sub-connectors, corresponding to
 * child nodes and edges.) The mapping between connectors and edge objects
 * is set up in the same manner as NodeController (@see NodeController).
 * <P>
 * This class has one abstract method which must be implemented by
 * application-specific subclasses, renderEdge(). This method
 * creates a Figure given a edge. A second method, drawEdgeChildren(),
 * should be overridden by any subclass that manages composite
 * edges.
 * <P>
 * Finally, concrete subclasses that want to change the default
 * interactor will do so in the subclass' constructor. Subclass
 * constructors must always call super() to ensure that the controller
 * is properly initialized.
 *
 * @author 	Michael Shilman
 * @author 	John Reekie
 * @version	$Revision: 1.4 $
 * @rating      Red
 */
public abstract class EdgeController  {
 
    /** The mapping from edges to conectors
     */
    private HashMap _map = new HashMap();

   /** The selection interactor for drag-selecting nodes
     */
    private SelectionDragger _selectionDragger;

    /** The connector target
     */
    private ConnectorTarget _connectorTarget;

    /** The filter for control operations
     */
    private MouseFilter _controlFilter = new MouseFilter (
            InputEvent.BUTTON1_MASK,
            InputEvent.CTRL_MASK);

    // The interactor used to operate on edges
    private Interactor _interactor;

    // The graph controller this node controller belongs to
    private GraphController _controller;

    /**
     * Create a new edge controller with basic interaction.  Specifically,
     * this method creates an edge interactor and initializes its menipulator
     * so that edges get attached appropriately.  Furthermore, the edge
     * interactor is initialized with the selection model of the graph 
     * controller.  The manipulator is activated by either a regular click
     * or a control click.  Also initialize a basic connector target that
     * generally attaches to the perimeter of nodes, except that it is 
     * smart enough to properly handle terminals.
     */
    public EdgeController (GraphController controller) {
	_controller = controller;
	SelectionModel sm = controller.getSelectionModel();
	_interactor = new EdgeInteractor(sm);
	
        // Create and set up the manipulator for connectors
        ConnectorManipulator manipulator = new ConnectorManipulator();
        manipulator.setSnapHalo(4.0);
        manipulator.addConnectorListener(new EdgeDropper());
        ((EdgeInteractor)_interactor).setPrototypeDecorator(manipulator);

        // The mouse filter needs to accept regular click or control click
        MouseFilter handleFilter = new MouseFilter(1, 0, 0);
        manipulator.setHandleFilter(handleFilter);

        // Create and set up the target for connectors
	setConnectorTarget(new EdgeTarget());
    }

    /** 
     * Add an edge that to the graph, and draw it on the canvas. The edge
     * is not connected to a node at either end.
     */
    public void addEdge(Object edge, Object parent) {

        FigureLayer layer = _controller.getGraphicsPane().getForegroundLayer();

	// Add to the graph
        GraphModel model = _controller.getGraphModel();
	model.addEdge(_controller, edge, parent);

	// Create head and tail sites
	Site tailSite = new AutonomousSite(layer, 0, 0);          
	Site headSite = new AutonomousSite(layer, 0, 0);          

	// Draw the edge
        drawEdge(edge, parent, tailSite, headSite);
    }

    /** 
     * Add an edge to the model and render it on the canvas. The
     * edge is placed inside the given parent, and connected to the
     * two given tail and head nodes.
     */
    public void addEdge(Object edge, Object parent,
			Object tail, Object head) {

        // Connect the edge. This will generate an EDGE_ADDED event
        GraphModel model = _controller.getGraphModel();
	model.connectEdge(_controller, edge, tail, head);

	// Get the tail and head sites from the node figures
	Connector connector = getConnector(edge);
        Figure tailFigure = _controller.getNodeController(tail).getFigure(tail);
        Figure headFigure = _controller.getNodeController(head).getFigure(head);
       
	Rectangle2D bounds = tailFigure.getBounds();
	Site tailSite = getConnectorTarget().getTailSite(
							 tailFigure,
							 bounds.getCenterX(),
							 bounds.getCenterY());
	bounds = headFigure.getBounds();
	Site headSite = getConnectorTarget().getHeadSite(
							 headFigure,
							 bounds.getCenterX(),
							 bounds.getCenterY());

	// Draw the edge
        drawEdge(edge, parent, tailSite, headSite);
    }

    /**
     * Create or overwrite a binding from an edge to a connector.
     */
    public void bind (Object edge, Connector connector) {
	_map.put(edge, connector);
	connector.setUserObject(edge);
    }

    /** Draw the edge inside the given parent, and between the
     * two given sites. If the edge is a hyperedge, then this
     * method will call the drawEdgeChildren() method to draw
     * contained nodes and edges.
     */
    public void drawEdge (Object edge, Object parent,
			  Site tailSite, Site headSite) {
	FigureContainer container;

	if (parent == _controller.getGraphModel().getRoot()) {
	    container = _controller.getGraphicsPane().getForegroundLayer();
	} else {
	    container = (FigureContainer) _controller.getNodeController(parent).getFigure(parent);
	}

	Connector connector = renderEdge(edge, parent,
				      container, tailSite, headSite);
	connector.setHeadSite(headSite);
	connector.setTailSite(tailSite);
        connector.setInteractor(getEdgeInteractor());
        bind(edge, connector);

	// Notify anything listening for view events
	_controller.dispatch(new GraphViewEvent(this,
			     GraphViewEvent.EDGE_DRAWN, connector, null));

	// If the edge is a hyperedge, draw the children
	if (_controller.getGraphModel().getEdgeAdapter(edge).isHyper(edge)) {
	    drawEdgeChildren(edge, connector);
	}
    }

    /** 
     * Draw the children of the given edge. The container argument
     * is the Figure that was previously drawn for this node. Subclasses
     * that control hyperedges must override this method to iterate
     * through child elements and call drawNode() or drawEdge() on the
     * appropriate controller for each of them.
     */
    public void drawEdgeChildren(Object parent, Figure container) {
	; // By default, do nothing
    }

    /**
     * Get the target used to find sites on nodes to connect to.
     */
    public ConnectorTarget getConnectorTarget () {
        return _connectorTarget;
    }

    /**
     * Get the graph controller that this controller is contained in.
     */
    public GraphController getController() {
	return _controller;
    }

    /** 
     * Get the connector corresponding to the given edge. Returns
     * null if the edge is not known to this controller.
     */
    public Connector getConnector (Object node) {
	return (Connector) _map.get(node);
    }

    /**
     * Get the interactor given to edge figures.
     */
    public Interactor getEdgeInteractor () {
        return _interactor;
    }

    /** 
     * Remove the given edge, and its associated figure. Notify
     * any view listeners.
     */
    public void removeEdge(Object edge) {
	undrawEdge(edge);

	// Notify anything listening for view events
	Connector connector = getConnector(edge);
	_controller.dispatch(new GraphViewEvent(this,
			     GraphViewEvent.EDGE_DRAWN, null, connector));

        GraphModel model = _controller.getGraphModel();
	model.setHead(_controller, edge, null);
	model.setTail(_controller, edge, null);
    }

    /**
     * Set the target used to find sites on nodes to connect to.  This
     * sets the local connector target (which is often used to find the
     * starting point of an edge) and the manipulator's connector target, which
     * is used after the connector is being dragged.
     */
    public void setConnectorTarget (ConnectorTarget t) {
        _connectorTarget = t;
	
	// FIXME: This is rather dangerous because it assumes a 
	// basic selection renderer.
	BasicSelectionRenderer selectionRenderer = (BasicSelectionRenderer)
	    ((EdgeInteractor)_interactor).getSelectionRenderer();
	ConnectorManipulator manipulator = (ConnectorManipulator)
	    selectionRenderer.getDecorator();
	manipulator.setConnectorTarget(t);
    }

    /**
     * Set the interactor given to edge figures.
     */
    public void setEdgeInteractor (Interactor interactor) {
	_interactor = interactor;
    }

    /**
     * Render a visual representation of the given edge, between the given
     * head and tail sites, and add it to the given figure container. If the node
     * is at the top level of the graph, then the container will be a
     * FigureLayer, otherwise it will be a Figure class that also
     * implements the FigureContainer interface.
     */
    public abstract Connector renderEdge (Object edge,
					  Object parent,
					  FigureContainer container,
					  Site tailSite, Site headSite);

    /** 
     * Set the head of an edge to the given node. If the node is the same
     * as the current head, then do nothing. If the node is null, then
     * disconnect the edge and draw the edge to an autonomous site. Otherwise,
     * reconnect the edge to the new head.
     */
    public void setHead(Object edge, Object head) {
        FigureLayer layer = _controller.getGraphicsPane().getForegroundLayer();
        GraphModel model = _controller.getGraphModel();
	Connector connector = getConnector(edge);
	Site site;

	Object oldHead = model.getHead(edge);
	if (oldHead == head) {
	    ; // do nothing
	} else if (head == null) {
	    model.setHead(_controller, edge, null);
	    double x, y;
	    if (oldHead != null) {
		x = connector.getHeadSite().getPoint().getX();
		y = connector.getHeadSite().getPoint().getY();
	    } else {
		x = 0;
		y = 0;
	    }
	    site = new AutonomousSite(layer, x, y);
	    connector.setHeadSite(site);
	    // FIXME dispatch view event
	} else {
	    // FIXME do in model
	    model.setHead(_controller, edge, null); // make sure it's disconnected
	    model.setHead(_controller, edge, head);
	    Figure headFigure = _controller.getNodeController(head).getFigure(head);
	    Rectangle2D bounds = headFigure.getBounds();
	    site = getConnectorTarget().getHeadSite(
						    headFigure,
						    bounds.getCenterX(),
						    bounds.getCenterY());
	    connector.setHeadSite(site);
	    // FIXME dispatch view event
	}
    }

    /** 
     * Set the tail of an edge to the given node. If the node is the same
     * as the current tail, then do nothing. If the node is null, then
     * disconnect the edge and draw the edge to an autonomous site. Otherwise,
     * reconnect the edge to the new tail.
     */
    public void setTail(Object edge, Object tail) {
        FigureLayer layer = _controller.getGraphicsPane().getForegroundLayer();
        GraphModel model = _controller.getGraphModel();
	Connector connector = getConnector(edge);
	Site site;

	// Process the tail end. If the same, do nothing. If nul,
	// disconnect. Otherwise, reconnect to new tail.
	Object oldTail = model.getTail(edge);
	if (tail == oldTail) {
	    ; // do nothing
	} else if (tail == null) {
	    model.setTail(_controller, edge, null);
	    double x, y;
	    if (oldTail != null) {
		x = connector.getTailSite().getPoint().getX();
		y = connector.getTailSite().getPoint().getY();
	    } else {
		x = 0;
		y = 0;
	    }
	    site = new AutonomousSite(layer, x, y);
	    connector.setTailSite(site);
	    // FIXME dispatch view event
	} else {
	    // FIXME do in model
	    model.setTail(_controller, edge, null); // Make sure it's disconnected
	    model.setTail(_controller, edge, tail);
	    Figure tailFigure = _controller.getNodeController(tail).getFigure(tail);
	    Rectangle2D bounds = tailFigure.getBounds();
	    site = getConnectorTarget().getTailSite(
						    tailFigure,
						    bounds.getCenterX(),
						    bounds.getCenterY());
	    connector.setTailSite(site);
	    // FIXME dispatch view event
	}
    }

    /**
     * Remove a binding from an edge to a connector. Throw an
     * exception if the binding is not valid. (This is very very
     * bad and should not happen. If it does then there is a serious
     * bug somewhere else.)
     */
    public void unbind (Object edge, Connector connector) {
	connector.setUserObject(null);
	Connector c = (Connector) _map.remove(edge);
	if (connector != c) {
	    throw new RuntimeException("Bad binding in EdgeController: "
				       + connector + " is not equal to " + c);
	}
    }

    /**
     * Remove the figure associated with the given edge. The model itself
     * is not touched at all.
     */
    public void undrawEdge(Object edge) {
	Connector c = getConnector(edge);
        if(c != null) {
	    FigureContainer container = (FigureContainer) c.getParent();
	    unbind(edge, c);
	    container.remove(c);
        }
    }

    ///////////////////////////////////////////////////////////////
    //// EdgeTarget

    /** An inner class that is used as the target for connectors
     */
    protected class EdgeTarget extends PerimeterTarget {
	// Accept the head if the graph model allows it.
	public boolean acceptHead (Connector c, Figure f) {
	    Object node = f.getUserObject();
	    Object edge = c.getUserObject();
	    GraphModel model = 
		(GraphModel)_controller.getGraphModel();
	    if (model.isNode(node) &&
		model.isEdge(edge) &&
		model.acceptHead(edge, node)) {
		return super.acceptHead(c, f);
	    } else return false;
	} 
	    
	// Accept the tail if the graph model allows it.
	public boolean acceptTail (Connector c, Figure f) {
	    Object node = f.getUserObject();
	    Object edge = c.getUserObject();
	    GraphModel model = 
		(GraphModel)_controller.getGraphModel();
	    if (model.isNode(node) &&
		model.isEdge(edge) &&
		model.acceptTail(edge, node)) {
		return super.acceptTail(c, f);
	    } else return false;
	}

	/** If we have any terminals, then return the connection
	 *  site of the terminal instead of a new perimeter site.
	 */
	public Site getHeadSite(Figure f, double x, double y) {
	    if(f instanceof Terminal) {
		Site site = ((Terminal)f).getConnectSite();
		return site;
	    } else {
		return super.getHeadSite(f, x, y);
	    }
	}
    }

    ///////////////////////////////////////////////////////////////
    //// EdgeDropper

    /** An inner class that handles interactive changes to connectivity.
     */
    protected class EdgeDropper extends ConnectorAdapter {
        /**
         * Called when a connector end is dropped--attach or
         * detach the edge as appropriate.
         */
        public void connectorDropped(ConnectorEvent evt) {
            Connector c = evt.getConnector();
            Figure f = evt.getTarget();

            Object edge = c.getUserObject();
            Object node = (f == null) ? null : f.getUserObject();
            GraphModel model =  
		(GraphModel) _controller.getGraphModel();
	    try {
		switch (evt.getEnd()) {
		case ConnectorEvent.HEAD_END:
		    model.setHead(_controller, edge, node);
		    break;
		case ConnectorEvent.TAIL_END:
		    model.setTail(_controller, edge, node);
		    break;
		default:
		    throw new IllegalStateException(
			"Cannot handle both ends of an edge being dragged.");
		}
	    } catch (GraphException ex) {
		SelectionModel selectionModel = 
		    _controller.getSelectionModel();
		// If it is illegal then blow away the edge.
		if(selectionModel.containsSelection(c)) {
		    selectionModel.removeSelection(c);
		} 
		removeEdge(edge);
		throw ex;
	    }
        }
    }
}


