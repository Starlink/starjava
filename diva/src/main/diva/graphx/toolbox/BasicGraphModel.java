/*
 * $Id: BasicGraphModel.java,v 1.2 2002/08/23 22:26:02 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.graphx.toolbox;

import diva.graphx.*;

import diva.util.ArrayIterator;
import diva.graph.toolbox.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import diva.util.SemanticObjectContainer;
import diva.util.BasicPropertyContainer;

/**
 * A basic implementation of a graph model that uses
 * BasicNodes and BasicEdges objects as its graph structure.
 * 
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @author John Reekie  (johnr@eecs.berkeley.edu)
 * @version $Revision: 1.2 $
 * @rating Red
 */
public class BasicGraphModel extends GraphModel {

    /** The node model
     */
    private BasicNodeAdapter _nodeAdapter;

    /** The edge model
     */
    private BasicEdgeAdapter _edgeAdapter;

    /** The root
     */
    private BasicNode _root = new BasicNode();

    /**
     * Construct an empty graph model.
     */
    public BasicGraphModel() {
	BasicNode root = new BasicNode();
	_edgeAdapter = new BasicEdgeAdapter(root);
	_nodeAdapter = new BasicNodeAdapter(root);
	super.setRoot(root);
    }

    /** 
     * Return the model for the given edge object.  If the object is not
     * an edge, then return null.
     */
    public EdgeAdapter getEdgeAdapter(Object edge) {
	if(edge instanceof BasicEdge) {
	    return _edgeAdapter;
	} else {
	    return null;
	}
    }

    /** 
     * Return the node model for the given object.  If the object is not
     * a node, then return null.
     */
    public NodeAdapter getNodeAdapter(Object node) {
	if(node instanceof BasicNode) {
	    return _nodeAdapter;
	} else {
	    return null;
	}
    }
}

