/*
 * $Id: BasicEdge.java,v 1.1 2002/08/19 07:12:30 johnr Exp $
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
 * A basic implementation of an edge.
 * 
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.1 $
 * @rating Red
 */
public class BasicEdge extends BasicPropertyContainer {
    /**
     * The head of the edge.
     */
    private BasicNode _head = null;

    /**
     * The tail of the edge.
     */
    private BasicNode _tail = null;

    /**
     * Create a new edge with no tail or head
     */
    public BasicEdge() {
	;
    }

    /**
     * Create a new edge with the specified tail and head
     */
    public BasicEdge(BasicNode tail, BasicNode head) {
	setTail(tail);
	setHead(head);
    }

    /** Return true
     */
    public boolean acceptHead(BasicNode head) {
	return true;
    }

    /** Return true
     */
    public boolean acceptTail(BasicNode tail) {
	return true;
    }

    /** Return the head node
     */
    public BasicNode getHead() {
	return _head;
    }
    
    /** Return the tail node
     */
    public BasicNode getTail() {
	return _tail;
    }

    /** Set the head node. Disconnect the edge from its current
     * head node (if it has one) and reconnect it to the new head.
     * If the head is null then the edge is just disconnected.
     */
    public void setHead(BasicNode head) {
	if(head != null) {
	    if (_head != null) {
		setHead(null);
	    }
	    head._inEdges.add(this);
	} else {
	    if (_head != null) {
		_head._inEdges.remove(this);
	    }
	}
	_head = head;
    }

    /** Set the tail node. Disconnect the edge from its current
     * tail node (if it has one) and reconnect it to the new tail.
     * If the tail is null then the edge is just disconnected.
     */
    public void setTail(BasicNode tail) {
	if(tail != null) {
	    if (_tail != null) {
		setTail(null);
	    }
	    tail._outEdges.add(this);
	} else {
	    if (_tail != null) {
		_tail._outEdges.remove(this);
	    }
	}
	_tail = tail;
    }
}

