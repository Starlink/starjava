/*
 * $Id: BasicNodeAdapter.java,v 1.2 2002/08/23 22:26:02 johnr Exp $
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
 * A node adapter for BasicEdges.
 * 
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @author John Reekie  (johnr@eecs.berkeley.edu)
 * @version $Revision: 1.2 $
 * @rating Red
 */
public class BasicNodeAdapter implements NodeAdapter {
    // The root
    private BasicNode _root;

    /** Create a new adapter.
     */
    public BasicNodeAdapter (BasicNode root) {
	_root = root;
    }

    /**
     * Return an iterator over the edges coming into the given node.
     */
    public Iterator inEdges(Object node) {
	return ((BasicNode) node).inEdges();
    }
	
    /**
     * Return an iterator over the edges coming out of the given node.
     */
    public Iterator outEdges(Object node) {
	BasicNode nodePeer = (BasicNode)node;
	return ((BasicNode) node).outEdges();
    }
	
    /**
     * Return the graph parent of the given node.
     */
    public Object getParent(Object node) {
	return ((BasicNode) node).getParent();
    }
	
    /**
     * Set the graph parent of the given node.  The parent has the
     * child node added to it also.
     */
    public void setParent(Object node, Object parent) {
	((BasicNode) node).setParent((BasicNode) parent);
    }

    /**
     * Return the number of nodes contained in
     * this graph or composite node.
     */
    public int getNodeCount(Object node) {
	return ((BasicNode) node).getNodeCount();
    }
	
    /** Return true. BasicNodes are always composite.
     */
    public boolean isComposite (Object node) {
	return true;
    }

    /**
     * Provide an iterator over the nodes in the
     * given graph or composite node.  This iterator
     * does not necessarily support removal operations.
     */
    public Iterator nodes(Object node) {
	return ((BasicNode) node).nodes();
    }
}
