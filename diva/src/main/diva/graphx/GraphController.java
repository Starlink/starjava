/*
 * $Id: GraphController.java,v 1.5 2002/09/21 00:27:52 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.graphx;

import diva.graphx.event.GraphEvent;
import diva.graphx.event.GraphViewEvent;
import diva.graphx.event.GraphListener;
import diva.graphx.event.GraphViewListener;

import diva.canvas.*;
import diva.canvas.connector.*;
import diva.canvas.interactor.*;
import diva.canvas.event.*;
import diva.canvas.toolbox.*;
import diva.util.*;

import java.awt.geom.*;
import java.util.*;

/**
 * A graph controller manages graph elements, such as nodes and
 * edges, and their visual representations on a canvas. An instance
 * of this class <b>is</b>, for all intents and purposes, a graph
 * editor.
 *
 * <p>Each instance of GraphController contains one or more instances
 * of NodeController, and one or more instances of EdgeController.
 * Simple graph editors only need one of each; more complex graph
 * editors will need more than one of each. For example, a graph
 * editor for hierarchical graphs might have two node controllers:
 * one for top-level nodes, and one for child nodes.
 * 
 * <P>Concrete subclasses must implement the following methods (which
 * are abstract in this class):
 * <ul>
 * <li> getNodeController() and getEdgeController(). These methods
 * return an appropriate controller for a given node or edge. Typically,
 * there will be a different controller for each node or edge type.
 * </ul>
 *
 * <p>In addition, subclasses will use the constructor to set up interaction
 * on the canvas that the graph is being drawn on. The recommended structure
 * of a subclass constructor is like this:
 * <pre>
 *   public MyGraphController (GraphicsPane pane, GraphModel model) {
 *	setGraphicsPane(pane);
 *	setGraphModel(model);
 *      ... initialize all interaction ...
 *   }
 * </pre>
 * 
 * <P>GraphController is intended to be the primary access point
 * for programmatic control of a graph editor. Many of its methods
 * simply delegate to a NodeController or EdgeController. In these cases
 * the documentation in this class is minimal and there is a reference
 * to the fully documented method in the relevant class.
 *
 * @author 	John Reekie
 * @author 	Michael Shilman (michaels@eecs.berkeley.edu)
 * @author 	Steve Neuendorffer (neuendor@eecs.berkeley.edu)
 * @version	$Revision: 1.5 $
 * @rating      Red
 */
public abstract class GraphController {

    /** Map semantic objects to their figure representations
     */
    private HashMap _map = new HashMap();
    
    /**
     * The graphics pane that this is drawing on
     */
    private GraphicsPane _pane;

    /**
     * The graph that is being displayed.
     */
    private GraphModel _model;

    /** The default selection model
     */
    private SelectionModel _selectionModel = new BasicSelectionModel();

    /** The listener of graph events.
     */
    private ChangeListener _localListener = new ChangeListener();

    /** The list of view listeners.
     */
    private List _graphViewListenerList = new LinkedList();

    /** 
     * Add an edge to the model and render it on the canvas.
     *
     * @see EdgeController#addEdge(Object, Object)
     */
    public void addEdge(Object edge, Object parent) {
	getEdgeController(edge).addEdge(edge, parent);
    }

    /** 
     * Add an edge to the model and render it on the canvas.
     *
     * @see EdgeController#addEdge(Object, Object, Object, Object)
     */
    public void addEdge(Object edge, Object parent,
			Object tail, Object head) {
	getEdgeController(edge).addEdge(edge, parent, tail, head);
    }

    /**
     */
    public void addGraphViewListener(GraphViewListener l) {
	_graphViewListenerList.add(l);
    }

    /** 
     * Add the node to this graph editor and render it
     * at the given location.
     *
     * @see NodeController#addNode(Object, Object, double, double)
     */
    public void addNode(Object node, double x, double y) {
	addNode(node, _model.getRoot(), x, y);
    }

    /** 
     * Add the node to this graph editor, inside the given parent node
     * and render it at the given location relative to its parent.
     *
     * @see NodeController#addNode(Object, Object, double, double)
     */
    public void addNode(Object node, Object parent, double x, double y) {
        getNodeController(node).addNode(node, parent, x, y);
    }

    /**
     * Given an edge, return the controller associated with that
     * edge.
     */
    public abstract EdgeController getEdgeController(Object edge);
  
    /**
     * Given an node, return the controller associated with that
     * node.
     */
    public abstract NodeController getNodeController(Object node);

    /**
     * Return the graph being viewed.
     */
    public GraphModel getGraphModel() {
        return _model;
    }

    /**
     * Return the graphics pane of this controller
     */
    public GraphicsPane getGraphicsPane() {
        return _pane;
    }

    /**
     * Get the default selection model
     */
    public SelectionModel getSelectionModel () {
        return _selectionModel;
    }

    /** Register a graph controller with its canvas pane. This is
     * a hack that we use because we no longer have a way to
     * get from a graph editing canvas to a 

    /**
     * Remove the given edge.  Find the edge controller associated with that
     * edge and delegate to that edge controller.
     */
    public void removeEdge(Object edge) {
	getEdgeController(edge).removeEdge(edge);
    }

    /** Remove the given view listener.
     */
    public void removeGraphViewListener(GraphViewListener l) {
	_graphViewListenerList.remove(l);
    }

    /**
     * Remove the given node.  Find the node controller associated with that
     * node and delegate to that node controller.
     *
     * @see NodeController#removeNode(Object)
     */
    public void removeNode(Object node) {
	getNodeController(node).removeNode(node);
    }

    /**
     * Render the complete graph. This utility method should be called only
     * when a non-empty GraphModel is set for the first time. Once anything
     * has been drawn, this method will not work.
     */
    public void render () {
	// FIXME;
    }

    /**
     * Re-render the complete graph. This utility method is provided for those
     * times when it's simply not possible to intelligently redraw relevant
     * portions of the graph. Responding to a STRUCTURE_CHANGED would
     * be one such situation. The selection is cleared before performing
     * this, so it's not a good idea to use this method unless it's
     * really necessary.
     */
    public void rerender () {
	// FIXME rerenderSubgraph(getGraphModel().nodes());
    }

    /**
     * Rerender a subgraph. This method finds the set of all nodes
     * returned by the given iterator, and the set of edges that includes
     * all edges returned by the iterator and all edges connected to
     * the set of nodes. Then it removes the connectors for all edges,
     * then the figures for all nodes, then it redraws them all. Any
     * nodes or edges that were originally in the selection get
     * put back into the selection. <b>Note</b> however that this
     * method might not always get it right in the presence of
     * multiple selections or hierarchy, so be warned.
     */
    public void rerenderSubGraph (Iterator elements) {
	return; // FIXME
    }

    /**
     * Set the graph model being viewed. This protected method is for
     * use only by subclass constructors.
     */
    protected final void setGraphModel(GraphModel model) {
	_model = model;
    }

    /**
     * Set the graphics pane that the controller operates on. This protected
     * method is for use only by subclass constructors.
     */
    protected final void setGraphicsPane (GraphicsPane pane) {
        _pane = pane;
    }

   /** 
     * Set the head of the edge to the given node.
     *
     * @see EdgeController#setHead(Object, Object)
     */
    public void setHead(Object edge, Object head) {
	getEdgeController(edge).setHead(edge, head);
    }

    /**
     * Set the default selection model. The caller is expected to ensure
     * that the old model is empty before calling this.
     */
    public void setSelectionModel (SelectionModel m){
        _selectionModel = m;
    }

   /** 
     * Set the tail of the edge to the given node.
     *
     * @see EdgeController#setTail(Object, Object)
     */
    public void setTail(Object edge, Object tail) {
	getEdgeController(edge).setTail(edge, tail);
    }

    /**
     * Dispatch the given graph view event to all registered grpah view 
     * listeners.  This method is generally only called by subclasses and 
     * representatives of thse subclasses, such as a node controller or
     * an edge controller.
     */
    public void dispatch(GraphViewEvent e) {
	for(Iterator i = _graphViewListenerList.iterator(); i.hasNext(); ) {
	    GraphViewListener l = (GraphViewListener)i.next();
	    switch(e.getID()) {
            case GraphViewEvent.NODE_MOVED:
                l.nodeMoved(e);
                break;
            case GraphViewEvent.EDGE_ROUTED:
                l.edgeRouted(e);
                break;
            case GraphViewEvent.NODE_DRAWN:
                l.nodeDrawn(e);
                break;
            case GraphViewEvent.EDGE_DRAWN:
                l.edgeDrawn(e);
                break;
	    }
	}    
    }

    /**
     * Initialize all interaction on the graph pane. This method
     * is called by the setGraphPane() method, and must be overridden
     * by subclasses.
     * This initialization cannot be done in the constructor because
     * the controller does not yet have a reference to its pane
     * at that time.
     */
    protected abstract void initializeInteraction ();

    /**
     * Debugging output.
     */
    private void debug(String s) {
        System.err.println("GraphController " + s);
    }

    /**
     * This inner class responds to changes in the graph
     * we are controlling.
     */
    private class ChangeListener implements GraphListener {
        /**
         * An edge's head has been changed in a registered
         * graph or one of its subgraphs.  The added edge
         * is the "source" of the event.  The previous head
         * is accessible via e.getOldValue().
         */
        public void edgeHeadChanged(GraphEvent e) {
            if(e.getSource() != GraphController.this) {
                // FIXME rerenderSubgraph(new UnitIterator(e.getTarget()));
            }
        }
        
        /**
         * An edge's tail has been changed in a registered
         * graph or one of its subgraphs.  The added edge
         * is the "source" of the event.  The previous tail
         * is accessible via e.getOldValue().
         */
        public void edgeTailChanged(GraphEvent e) {
            if(e.getSource() != GraphController.this) {
                // FIXME rerenderSubgraph(new UnitIterator(e.getTarget()));
	    }
        }
        
        /**
         * A edge has been been added to the registered
         * graph or one of its subgraphs.  The added edge
         * is the "source" of the event.
         */
        public void edgeAdded(GraphEvent e) {
            if(e.getSource() != GraphController.this) {
		// FIXME
                // getEdgeController(e.getTarget()).drawEdge(
		//			      e.getTarget());// FIXME
	    }
        }
        
        /**
         * A edge has been been deleted from the registered
         * graphs or one of its subgraphs.  The deleted edge
         * is the "source" of the event.  The previous parent
         * graph is accessible via e.getOldValue().
         */
        public void edgeRemoved(GraphEvent e) {
            if(e.getSource() != GraphController.this) {
                //Remove the figure from the view
                getEdgeController(e.getTarget()).undrawEdge(e.getTarget());
	    }
        }

        /**
         * A node has been been added to the registered
         * graph or one of its subgraphs.  The added node
         * is the "source" of the event.
         */
        public void nodeAdded(GraphEvent e) {
            if(e.getSource() != GraphController.this) {
                getNodeController(e.getTarget()).drawNode(
							  e.getTarget(),
							  null, 0, 0);// FIXME
	    }
        }
        
        /**
         * A node has been been deleted from the registered
         * graphs or one of its subgraphs.  The deleted node
         * is the "source" of the event.  The previous parent
         * graph is accessible via e.getOldValue().
         */
        public void nodeRemoved(GraphEvent e) {
            if(e.getSource() != GraphController.this) {
                //Remove the figure from the view
                getNodeController(e.getTarget()).undrawNode(e.getTarget());
	    }
        }

        /**
         * The structure of the event's "source" graph has
         * been drastically changed in some way, and this
         * event signals the listener to refresh its view
         * of that graph from model.
         */
        public void structureChanged(GraphEvent e) {
            if(e.getSource() != GraphController.this) {
		rerender();
		/* Object root = e.getTarget();
                
                //FIXME - this could be optimized--
                //        we may not need to rerender every
                //        node.
            
                for(Iterator i = _model.nodes(root); i.hasNext(); ) {
                    rerenderNode(i.next());
                }
                for(Iterator i = GraphUtilities.totallyContainedEdges(root, _model); 
		    i.hasNext(); ) {
                    rerenderEdge(i.next());
                }
		*/
	    }
        }
    }
}
