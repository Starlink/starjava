/*
 * $Id: BasicEdgeAdapter.java,v 1.2 2002/08/23 22:26:02 johnr Exp $
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
import diva.util.PropertyContainer;

/**
 * A implementation of edge models for BasicEdges.
 * 
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @author John Reekie  (johnr@eecs.berkeley.edu)
 * @version $Revision: 1.2 $
 * @rating Red
 */
public class BasicEdgeAdapter implements EdgeAdapter {
    // The root
    private BasicNode _root;

    /** Create a new adapter.
     */
    public BasicEdgeAdapter (BasicNode root) {
	_root = root;
    }

    /**
     * Return true if the head of the given edge can be attached to the
     * given node.
     */
    public boolean acceptHead (Object edge, Object node) {
	return ((BasicEdge) edge).acceptHead((BasicNode) node);
    }
	
    /**
     * Return true if the tail of the given edge can be attached to the
     * given node.
     */
    public boolean acceptTail (Object edge, Object node) {
	return ((BasicEdge) edge).acceptTail((BasicNode) node);
    }
	
    /**
     * Return the head node of the given edge.
     */
    public Object getHead (Object edge) {
	return ((BasicEdge) edge).getHead();
    }
		
    /**
     * Return null. BasicEdges cannot be hyperedges.
     */
    public NodeAdapter getHyperContent (Object edge) {
	return null;
    }
		
    /**
     * Return the graph root.
     */
    public Object getParent(Object edge) {
	return _root;
    }
	
    /**
     * Return the tail node of this edge.
     */
    public Object getTail(Object edge) {
	return ((BasicEdge) edge).getTail();
    }
	
    /**
     * Return true. BasicEdges are always directed.
     */
    public boolean isDirected(Object edge) {
	return true;
    }

    /**
     * Return false. BasicEdges cannot by hyperedges.
     */
    public boolean isHyper (Object edge) {
	return false;
    }

    /**
     * Connect an edge to the given head node. The head node will
     * have the edge added to its list of input edges.
     */
    public void setHead(Object edge, Object head) {
	((BasicEdge) edge).setHead((BasicNode) head);
    }

    /** Do nothing. Basic edges always have the graph root as their
     * parent.
     */
    public void setParent (Object edge, Object parent) {
	;
    }

    /**
     * Connect an edge to the given tail node.  The tail node will
     * have the edge added to its list of output edges.
     */
    public void setTail(Object edge, Object tail) {
	((BasicEdge) edge).setTail((BasicNode) tail);
    }
}
