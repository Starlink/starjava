/*
 * $Id: LightweightGraph.java,v 1.10 2002/02/05 23:15:51 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 *
 */

package diva.pod.lwgraph;

import diva.util.FilteredArrayIterator;
import diva.util.NullArrayIterator;
import diva.util.IteratorAdapter;

import java.util.Iterator;
import java.util.NoSuchElementException;

/** A light-weight graph data structure. This class requires only that
 * clients implement two very simple interfaces, LWNode and LWEdge (or
 * use the basic implementations provided in this package). Clients
 * add nodes to the graph, and connect them. Clients that want to connect
 * to "ports" on components should use the LightweightNetwork class instead.
 * Clients that want a very fast and compact graph representation should
 * use the low-level classes Topology and Traversal.
 * <p>
 * Light-weight graphs are hierarchical, in a very simple (and light-weight!)
 * way. Every node can have a "parent" node set for it. Some methods
 * are provided for iterating through the "children" of a given node.
 * Clients that want more complicated or rigid notions of containment
 * can implement it over the top of this API.
 *
 * @version $Revision: 1.10 $
 * @author John Reekie
 * @rating Red
 */
public class LightweightGraph {

    // The initial space for node storage
    private static int INITNODEMAX = 64;
    private static int INITEDGEMAX = 64;

    // The current edge id
    private int _edgeid = 0;

    // The number of edges
    private int _edgecount = 0;

    // The current node id
    private int _nodeid = 0;

    // The number of nodes
    private int _nodecount = 0;

    // The topology
    private Topology _topology = new Topology();

    // The array of node objects
    private LWNode[] _nodes = new LWNode[INITNODEMAX];

    // The array of parent node objects
    private LWNode[] _parents = new LWNode[INITNODEMAX];

    // The array of edge objects
    private LWEdge[] _edges = new LWEdge[INITEDGEMAX];

    // The cached successor topology
    private Traversal _succTraversal = null;

    // The cached predecessor topology
    private Traversal _predTraversal = null;

    /** Create a new, empty, light-weight graph
     */
    public LightweightGraph () {
	;
    }

    /** Add an edge object to the graph.
     */
    public void addEdge (LWEdge edge) {
	// Check size and grow if needed
	if (_edgeid == _edges.length) {
	    LWEdge[] temp = new LWEdge[_edges.length * 2];
	    for (int i = 0; i < _edges.length; i++) {
		temp[i] = _edges[i];
	    }
	    _edges = temp;
	}
	int edgeid = _edgeid++;
        edge.setEdgeId(edgeid);
	_edges[edgeid] = edge;
        _edgecount++;
    }

    /** Add a node object to the graph
     */
    public void addNode (LWNode node) {
	// Check size and grow if needed
	if (_nodeid == _nodes.length) {
	    LWNode[] temp1 = new LWNode[_nodes.length * 2];
	    LWNode[] temp2 = new LWNode[_nodes.length * 2];
	    for (int i = 0; i < _nodes.length; i++) {
		temp1[i] = _nodes[i];
		temp2[i] = _parents[i];
	    }
	    _nodes = temp1;
	    _parents = temp2;
	}
	int nodeid = _nodeid++;
	node.setNodeId(nodeid);
	_nodes[nodeid] = node;
        _nodecount++;
    }

    /** Reconstruct the cache of the topology (this is contained in
     * a pair of Traversal objects). This method is automatically
     * called by methods of this class when necessary, so it should
     * not be necessary for the client to call it.
     */
    public void cacheTraversal () {
	if (_succTraversal == null) {
	    _succTraversal = new Traversal(_topology);
	    _topology.reverse();
	    _predTraversal = new Traversal(_topology);
	    _topology.reverse();
	}
    }

    /** Connect the given tail and head nodes using the given edge.
     * The nodes must have been added to this graph previously with
     * the addNode() method. The edge must have previously been added
     * to the graph with the addEdge() method. The same two nodes can
     * be connected by more than one edge. The head and tail nodes
     * cannot be null; if you wish to connect just one end of the
     * edge, use the setHead() or setTail() method
     */
    public void connect (LWEdge edge, LWNode tail, LWNode head) {
	invalidateCache();
	_topology.connect(edge.getEdgeId(), tail.getNodeId(), head.getNodeId());
    }

    /** Return an iterator over the edges of the graph.
     * The remove() method is supported, and is equivalent to calling
     * removeEdge() on the current edge. Removing edges by any
     * other means while in an iterator is a very bad idea.
     */
    public Iterator edges () {
        return new NullArrayIterator (_edges, _topology.getMaxEdgeId()+1) {
            public void remove () {
                removeEdge(_edges[getLastIndex()]);
            }
        };
    }

    /** Get the edge with the given id, or null if the edge
     * does not exist.
     */
    public LWEdge getEdge (int edgeid) {
	return _edges[edgeid];
    }

    /** Get the number of edges in this graph
     */
    public int getEdgeCount () {
	return _edgecount;
    }

    /** Get the head node of the given edge
     */
    public LWNode getHeadNode (LWEdge edge) {
	return _nodes[_topology.getHead(edge.getEdgeId())];
    }

    /** Get the topology used by this graph.
     */
    public Topology getTopology () {
	return _topology;
    }

    /** Get the node with the given id, or null if the edge
     * does not exist.
     */
    public LWNode getNode (int nodeid) {
	return _nodes[nodeid];
    }

    /** Get the number of nodes in this graph
     */
    public int getNodeCount () {
	return _nodecount;
    }

    /** Get the parent node of the given node, or null if the node
     * has no parent (ie is at the root level of the graph).
     */
    public LWNode getParent(LWNode node) {
        return _parents[node.getNodeId()];
    }

    /** Get the tail node of the given edge
     */
    public LWNode getTailNode (LWEdge edge) {
	return _nodes[_topology.getTail(edge.getEdgeId())];
    }

    /** Return an iterator over the edges into the given node.
     */
    public Iterator inEdges (LWNode node) {
        cacheTraversal();
	return new TraversalIterator(
                _predTraversal.getEdges(node.getNodeId()),
                _edges);
    }

    /** Invalidate the cache of the topology. This method is
     * automatically called by methods of this class when necessary,
     * so it should not be necessary for the client to call it.
     */
    public void invalidateCache () {
	_succTraversal = null;
	_predTraversal = null;
    }

    /** Return an iterator over the nodes of the graph.
     * The remove() method is supported, and is equivalent to calling
     * removeNode() on the most recently returned node. Removing nodes
     * by any other means while in an iterator is not a good idea.
     */
    public Iterator nodes () {
        return new NullArrayIterator (_nodes, _topology.getMaxNodeId()+1) {
            public void remove () {
                removeNode(_nodes[getLastIndex()]);
            }
        };
    }

    /** Return an iterator over the nodes that are children of the
     * given node. The remove() method is supported, and is equivalent
     * to calling setParent(null) on the most recently returned node.
     * NOTE: it does not remove the node from the graph!
     */
    public Iterator nodes (final LWNode parent) {
        return new NodeChildrenIterator(parent);
    }

    /** Return an iterator over the edges out of the given node.
     */
    public Iterator outEdges (LWNode node) {
        cacheTraversal();
	return new TraversalIterator(
                _succTraversal.getEdges(node.getNodeId()),
                _edges);
    }

    /** Return an iterator over the predecessors of the given node.
     */
    public Iterator predecessors (LWNode node) {
	cacheTraversal();
	return new TraversalIterator(
                _predTraversal.getSuccessors(node.getNodeId()),
                _nodes);
    }

    /** Remove the given edge.  Will throw an exception
     * of the edge is not in this graph. The edge will of course
     * be disconnected as well as being removed from the graph.
     */
    public void removeEdge (LWEdge edge) {
	invalidateCache();

        int id = edge.getEdgeId();
	_edges[id] = null;
        _topology.removeEdge(id);
        _edgecount--;
    }

    /** Remove the given node. Connected edges will not be affected,
     * it is the client's responsibility to disconnect edges before
     * calling this method.  Will throw an exception of the node is
     * not in this graph.
     */
    public void removeNode (LWNode node) {
	_nodes[node.getNodeId()] = null;
        _nodecount--;
    }

    /** Return an iterator over the root nodes of the graph.
     * (Where a root is a node that has no predecessor.)
     */
    public Iterator roots () {
	cacheTraversal();
	return new TraversalIterator(
                _succTraversal.getRoots(),
                _nodes);
    }

    /** Set the head node of the given edge. If the edge is unknown
     * to this graph, it will be added to it. To disconnect the edge
     * from the node, set the node to null.
     */
    public void setHeadNode (LWEdge edge, LWNode head) {
	invalidateCache();
	_topology.setHead(edge.getEdgeId(), head.getNodeId());
    }

    /** Set the parent node of the given node. Both nodes must
     * have previously been added to the graph.
     */
    public void setParent(LWNode node, LWNode parent) {
        _parents[node.getNodeId()] = parent;
    }

    /** Set the tail node of the given edge. If the edge is unknown
     * to this graph, it will be added to it.  To disconnect the edge
     * from the node, set the node to null.
     */
    public void setTailNode(LWEdge edge, LWNode tail) {
	invalidateCache();
	_topology.setTail(edge.getEdgeId(), tail.getNodeId());
    }

    /** Return an iterator over the successors of the given node.
     */
    public Iterator successors (LWNode node) {
	cacheTraversal();
	return new TraversalIterator(
                _succTraversal.getSuccessors(node.getNodeId()),
                _nodes);
    }



    /** The iterator class that is used to iterate over integer
     * arrays of things and map them into objects. Removal is
     * not supported. A null argument is treated as an empty iterator.
     */
    public class TraversalIterator extends IteratorAdapter {
        private int[] _indexes;
        private Object[] _map;
        private int _lastindex = -1;
        private int _nextindex = -1;

        public TraversalIterator(int[] indexes, Object[] map) {
            _indexes = indexes;
            _map = map;
            if (_indexes != null) {
                advance();
            }
        }

        /** Advance the next index to the next non-null element. Set it
         * to -1 if there are no more elements.
         */
        private void advance() {
            _nextindex++;
            while (_nextindex < _indexes.length && _indexes[_nextindex] == -1) {
                _nextindex++;
            }
            if (_nextindex == _indexes.length) {
                _nextindex = -1;
            }
        }

        /** Return true if there are more elements in the array.
         */
        public boolean hasNext() {
            return _nextindex >= 0;
        }

        /** Return the next element.
         */
        public Object next() {
            if (!hasNext()) {
                throw new NoSuchElementException("No more elements");
            }
            Object result = _map[_indexes[_nextindex]];
            _lastindex = _nextindex;
            advance();
            return result;
        }
    }


    /** An iterator class for iterating over the children of a node.
     * I tried to use an anonymous inner subclass of
     * diva.util.FilteredArrayIterator for this, but Java seemed to
     * have a weird bug where a reference outside the iterator (in
     * this case, to _parents) would just throw a null pointer
     * exception (even though _parents wasn't null... as far as I
     * could tell...)
     */
    private class NodeChildrenIterator implements Iterator {

        int _lastindex = -1;
        int _nextindex = -1;
        LWNode _parent;

        /** Construct the iterator
         */
        public NodeChildrenIterator(LWNode parent) {
            _parent = parent;
            advance();
        }

        /** Advance the next index to the next non-null element. Set it
         * to -1 if there are no more elements.
         */
        protected void advance() {
            _nextindex++;
            while (_nextindex < _nodeid) {
                LWNode n = _nodes[_nextindex];
                if (n != null) {
                    if (_parents[n.getNodeId()] == _parent) {
                        break;
                    }
                }
                _nextindex++;
            }
            if (_nextindex == _nodeid) {
                _nextindex = -1;
            }
        }
    
        /** Return true if there are more non-null elements in the array.
         */
        public boolean hasNext() {
            return _nextindex >= 0;
        }
        
        /** Return the next non-null element in the array.
         */
        public Object next() {
            if (!hasNext()) {
                throw new NoSuchElementException("No more elements");
            }
            Object result = _nodes[_nextindex];
            _lastindex = _nextindex;
            advance();
            return result;
        }

        /** Remove the node from its parent (not from the graph!)
         */
        public void remove () {
            setParent(_nodes[_lastindex], null);
        }
    }
}
