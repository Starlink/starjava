/*
 * $Id: BasicNode.java,v 1.1 2002/08/19 07:12:31 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.graphx.toolbox;

import diva.graphx.*;

import diva.util.ArrayIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import diva.util.SemanticObjectContainer;
import diva.util.BasicPropertyContainer;
import diva.util.NullIterator;

/**
 * A basic implementation of a node. This node can be a
 * composite node, that is, it can contain other nodes.
 * The implementation does not enforce whether a node
 * is composite or not (eg by having a flag to say
 * whether it is and disallowing methods that add child
 * nodes or not). Rather, it always returns true to
 * the isComposite() method.
 * 
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.1 $
 * @rating Red
 */
public class BasicNode extends BasicPropertyContainer {
    /**
     * The nodes that this node contains. This is created
     * only the first time a child node is added.
     */
    ArrayList _nodes;

    /**
     * The edges <b>into</b> this node.
     */
    ArrayList _inEdges = new ArrayList();

    /**
     * The edges <b>out of</b> this node.
     */
    ArrayList _outEdges = new ArrayList();
        
    /**
     * The parent of this node.
     */
    BasicNode _parent = null;

    /** Test if the node is a child of this node
     */
    public boolean contains (BasicNode n) {
	return _nodes == null || _nodes.contains(n);
    }

    /** Return the parent node of this node
     */
    public BasicNode getParent () {
	return _parent;
    }
       
    /** Return the number of child nodes
     */
    public int getNodeCount () {
	if (_nodes == null) {
	    return 0;
	} else {
	    return _nodes.size();
	}
    }
 
    /**
     * Return an iterator over the <i>in</i> edges of this
     * node. This iterator does not support removal operations.
     */
    public Iterator inEdges () {
	return new ArrayIterator(_inEdges.toArray());
    }

    /**
     * Return an iterator over the predecessor nodes of this
     * node. This iterator does not support removal operations.
     * This method provides a convenient way of traversing a
     * graph.
     * 
     * FIXME: this will be incorrect if an edge does not
     * have a tail node.
     */
    public Iterator predecessors () {
	return new diva.util.IteratorAdapter () {
                Iterator edges = inEdges();
                public boolean hasNext() {
                    return edges.hasNext();
                }
                public Object next() {
                    return ((BasicEdge)edges.next()).getTail();
                }
            };
    }

    /** Return an iterator over the contained nodes.
     */
    public Iterator nodes () {
	if (_nodes == null || _nodes.size() == 0) {
	    return new NullIterator();
	} else {
	    return _nodes.iterator();
	}
    }

    /**
     * Return an iterator over the <i>out</i> edges of this
     * node.  This iterator does not support removal operations.
     */
    public Iterator outEdges () {
	return new ArrayIterator(_outEdges.toArray());
    }
 
    /**
     * Return an iterator over the successor nodes of this
     * node. This iterator does not support removal operations.
     * This method provides a convenient way of traversing a
     * graph.
     *
     * FIXME: this will be incorrect if an edge does not
     * have a head node.
     */
    public Iterator successors () {
	return new diva.util.IteratorAdapter () {
                Iterator edges = outEdges();
                public boolean hasNext() {
                    return edges.hasNext();
                }
                public Object next() {
                    return ((BasicEdge)edges.next()).getHead();
                }
            };
    }

    /** Set the parent of this node. Do not add the
     * node to the parent (that is, adding nodes to 
     * a node should be done with the addNode() method).
     */
    public void setParent(BasicNode parent) {
	if (parent != null) {
	    // Add to a parent
	    if (_parent != null) {
		// Remove it from the ewxisting parent first
		setParent(null);
	    }
	    if (parent._nodes == null) {
		parent._nodes = new ArrayList();
	    }
	    parent._nodes.add(this);
	} else {
	    // Remove from parent
	    if (_parent != null) {
		_parent._nodes.remove(this);
	    }
	}
	_parent = parent;
    }
}
