/*
 * $Id: GraphModel.java,v 1.5 2002/09/21 00:08:13 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.graphx;

import diva.graphx.event.GraphEvent;
import diva.graphx.event.GraphEventMulticaster;
import diva.graphx.event.GraphListener;

import java.util.Iterator;
import diva.util.SemanticObjectContainer;
import diva.util.PropertyContainer;
import javax.swing.SwingUtilities;

/**
 * The class that represents a graph. An instance of this class
 * contains one or more NodeAdapters and EdgeAdapters, as returned
 * by the (abstract) getNodeAdapter() and getEdgeAdapter(). To tailor
 * this class to a specific type of graph, it must be subclasses
 * and these two methods over-ridden.
 *
 * <P> Operations on nodes and edges can be performed by accessing
 * the appropriate NodeAdapter or EdgeAdapter and calling their methods.
 * The GraphModel class provides an event notification mechanism
 * over the top of this basic functionality. Specifically, any
 * operations that modify the graph, such as adding a node or edge,
 * should be performed through the interface in GraphModel so that
 * events are generated for listeners. Many methods are also provided
 * that delegate to the appropriate adapter method; in these case,
 * documentation here is incomplete, and the adaptor method is
 * referred to.
 *
 * <P>Event dispatching can also be done explicitly by calling the
 * dispatchEvent() method. This can be used if a series of graph
 * modifications will be performed and it is preferable to just generate
 * a single event. Event dispatching can also be turned on or off with
 * the setDispatchEnabled() method (by default, dispatching is on).
 * 
 * @author Steve Neuendorffer
 * @author John Reekie
 * @version $Revision: 1.5 $
 * @rating Red
 */
public abstract class GraphModel {
    /**
     * Whether or not to dispatch events.
     */
    private boolean _dispatch = true;

    /**
     * The root of the graph contained by this model.
     */
    private Object _root = null;

    /**
     * The list of graph listeners.
     */
    protected GraphEventMulticaster _graphListeners = 
	new GraphEventMulticaster();

    /**
     * Return true if the head of the given edge can be attached to the
     * given node.
     */
    public boolean acceptHead(Object edge, Object node) {
	return getEdgeAdapter(edge).acceptHead(edge, node);
    }

    /**
     * Return true if the tail of the given edge can be attached to the
     * given node.
     */
    public boolean acceptTail(Object edge, Object node) {
	return getEdgeAdapter(edge).acceptTail(edge, node);
    }
    
    /**
     * Add a graph listener to the model.  Graph listeners are
     * notified with a GraphEvent any time the graph is modified.
     */
    public void addGraphListener(GraphListener l) {
	_graphListeners.add(l);
    }

    /**
     * Add a edge to the graph. Graph listeners are notified with an
     * EDGE_ADDED event.
     */
    public void addEdge(Object eventSource, Object edge, Object parent) {
	Object prevParent = getEdgeAdapter(edge).getParent(edge);
	getEdgeAdapter(edge).setParent(edge, parent);
        
        GraphEvent e = new GraphEvent(eventSource, GraphEvent.EDGE_ADDED,
				      edge, prevParent);
        dispatchGraphEvent(e);
    }

    /**
     * Add a node to the graph. Graph listeners are notified with a
     * NODE_ADDED event.
     */
    public void addNode(Object eventSource, Object node, Object parent) {
	Object prevParent = getNodeAdapter(node).getParent(node);
	getNodeAdapter(node).setParent(node, parent);
        
        GraphEvent e = new GraphEvent(eventSource, GraphEvent.NODE_ADDED,
                node, prevParent);
        dispatchGraphEvent(e);
    }

     /**
      * Connect the given edge to the given tail and head nodes,
      * then dispatch events to the listeners.
      *
      * @deprecated
      */
     public void connectEdge(Object eventSource, Object edge,
                           Object tailNode, Object headNode) {
       Object prevTail = getEdgeAdapter(edge).getTail(edge);
         Object prevHead = getEdgeAdapter(edge).getHead(edge);
       getEdgeAdapter(edge).setHead(edge, headNode);
         getEdgeAdapter(edge).setTail(edge, tailNode);

         GraphEvent e1 = new GraphEvent(eventSource,
                                      GraphEvent.EDGE_HEAD_CHANGED,
                                      edge, prevHead);
         dispatchGraphEvent(e1);

         GraphEvent e2 = new GraphEvent(eventSource,
                                      GraphEvent.EDGE_TAIL_CHANGED,
                                      edge, prevTail);
         dispatchGraphEvent(e2);
     }

     /**
      * Disconnect an edge from its two endpoints and notify graph
      * listeners with an EDGE_HEAD_CHANGED and an EDGE_TAIL_CHANGED
      * event.
      *
      * @deprecated
      */
     public void disconnectEdge(Object eventSource, Object edge) {
       EdgeAdapter adapter = getEdgeAdapter(edge);
       Object head = adapter.getHead(edge);
       Object tail = adapter.getTail(edge);
         adapter.setTail(edge, null);
         adapter.setHead(edge, null);
         if(head != null) {
             GraphEvent e = new GraphEvent(eventSource,
                                         GraphEvent.EDGE_HEAD_CHANGED,
                                         edge, head);
             dispatchGraphEvent(e);
         }
         if(tail != null) {
             GraphEvent e = new GraphEvent(eventSource,
                                         GraphEvent.EDGE_TAIL_CHANGED,
                                         edge, tail);
             dispatchGraphEvent(e);
         }
     }

    /**
     * Send an graph event to all of the graph listeners.  This
     * allows manual control of sending graph graph events, or
     * allows the user to send a STRUCTURE_CHANGED after some
     * inner-loop operations.
     * <p>
     * This method furthermore ensures that all graph events are
     * dispatched in the event thread.
     * @see setDispatchEnabled(boolean)
     */
    public void dispatchGraphEvent(final GraphEvent e) {
        if(_dispatch) {
            if(SwingUtilities.isEventDispatchThread()) {
                _graphListeners.dispatchEvent(e);
            } else {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        _graphListeners.dispatchEvent(e);
                    }
                });
            }         
        }
    }

    /** 
     * Return the adapter for the given edge object.
     * If the object is not an edge, then return null.
     */
    public abstract EdgeAdapter getEdgeAdapter (Object edge);

    /**
     * Return the head node of the given edge.
     */
    public Object getHead(Object edge) {
	return getEdgeAdapter(edge).getHead(edge);
    }
		
    /**
     * Return the number of nodes contained in
     * the given node. This will be meaningful only if the
     * given node is a composite.
     */
    public int getNodeCount(Object node) {
	return getNodeAdapter(node).getNodeCount(node);
    }

    /** 
     * Return the adapter for the given node object.
     * If the object is not a node, then return null.
     */
    public abstract NodeAdapter getNodeAdapter (Object node);

    /**
     * Return the parent graph of this node, return
     * null if there is no parent.
     */
     public Object getParent(Object node) {	
	if(node == _root) {
            return null;
        } else {
            return getNodeAdapter(node).getParent(node);
        }
    }

    /**
     * Return the root graph of this graph model.
     */
    public Object getRoot() {
        return _root;
    }

    /**
     * Return the tail node of this edge.
     */
    public Object getTail(Object edge) {
	return getEdgeAdapter(edge).getTail(edge);
    }

    /**
     * Return an iterator over the <i>in</i> edges of this
     * node. This iterator does not support removal operations.
     * If there are no in-edges, an iterator with no elements is
     * returned.
     */
    public Iterator inEdges(Object node) {
	return getNodeAdapter(node).inEdges(node);
    }
    
    /**
     * Return true if the given object is a composite
     * node in this model, i.e. it contains children.
     */
    public boolean isComposite(Object node) {
	return getNodeAdapter(node).isComposite(node);
    }

    /** Test if dispatching is enabled
     */
    public boolean isDispatchEnabled () {
	return _dispatch;
    }

    /**
     * Return true if the given object is a 
     * node in this model.
     */
    public boolean isEdge(Object o) {
        return getEdgeAdapter(o) != null;
    }

    /**
     * Return true if the given object is a 
     * node in this model.
     */
    public boolean isNode(Object o) {
        return getNodeAdapter(o) != null;
    }

    /**
     * Provide an iterator over the nodes in the
     * given node.  This iterator
     * does not necessarily support removal operations. The
     * result will be meaningful only of the given node is
     * a composite.
     */
    public Iterator nodes(Object node) {
	return getNodeAdapter(node).nodes(node);
    }

    /**
     * Return an iterator over the <i>out</i> edges of this
     * node.  This iterator does not support removal operations.
     * If there are no out-edges, an iterator with no elements is
     * returned.
     */
    public Iterator outEdges(Object node) {
	return getNodeAdapter(node).outEdges(node);
    }

    /** 
     * Remove the given listener from this graph model.
     * The listener will no longer be notified of changes
     * to the graph.
     */
    public void removeGraphListener(GraphListener l) {
	_graphListeners.remove(l);
    }

    /**
     * Delete a node from its parent graph and notify
     * graph listeners with a NODE_REMOVED event.  This first removes all the
     * edges that are connected to the given node, or some subnode of that
     * node, and then sets the parent of the node to null.
     */
    public void removeNode(Object eventSource, Object node) {
	// Remove the edges.
	Iterator i = GraphUtilities.partiallyContainedEdges(node, this);
	while(i.hasNext()) {
	    Object edge = i.next();
	    disconnectEdge(eventSource, edge);
	}

        i = outEdges(node);
	while(i.hasNext()) {
	    Object edge = i.next();
	    disconnectEdge(eventSource, edge);
	}

	i = inEdges(node);
	while(i.hasNext()) {
	    Object edge = i.next();
	    disconnectEdge(eventSource, edge);
	}

	// remove the node.
	Object prevParent = getNodeAdapter(node).getParent(node);
        getNodeAdapter(node).setParent(node, null);
        GraphEvent e = new GraphEvent(eventSource, GraphEvent.NODE_REMOVED,
				      node, prevParent);
        dispatchGraphEvent(e);
    }
    
    /**
     * Turn on/off all event dispatches from this graph model, for use
     * in an inner-loop algorithm.  When turning dispatch back on
     * again, if the client has made changes that listeners should
     * know about, he should create an appropriate STRUCTURE_CHANGED
     * and dispatch it using the dispatchGraphEvent() method.
     * 
     * @see dispatchGraphEvent(GraphEvent)
     */
    public void setDispatchEnabled(boolean val) {
        _dispatch = val;
    }

    /**
     * Connect an edge to the given head node. Graph listeners are notified
     * with an EDGE_HEAD_CHANGED event.
     */
    public void setHead(Object eventSource, Object edge, Object head) {
	Object prevHead = getEdgeAdapter(edge).getHead(edge);
        getEdgeAdapter(edge).setHead(edge, head);
        GraphEvent e = new GraphEvent(eventSource,
				      GraphEvent.EDGE_HEAD_CHANGED,
				      edge, prevHead);
        dispatchGraphEvent(e);
    }

    /**
     * Set the root of the graph. For subclass constructor use.
     */
    protected void setRoot (Object root) {
	_root = root;
    }

    /**
     * Connect an edge to the given tail node. Graph listeners are
     * notified with an EDGE_TAIL_CHANGED event.
     */
    public void setTail(Object eventSource, Object edge, Object tail) {
	Object prevTail = getEdgeAdapter(edge).getTail(edge);
        getEdgeAdapter(edge).setTail(edge, tail);
        GraphEvent e = new GraphEvent(eventSource,
				      GraphEvent.EDGE_TAIL_CHANGED,
				      edge, prevTail);
        dispatchGraphEvent(e);
    }
}

