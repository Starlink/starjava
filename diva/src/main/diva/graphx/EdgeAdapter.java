/*
 * $Id: EdgeAdapter.java,v 1.2 2002/08/23 22:26:01 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.graphx;

import diva.util.SemanticObjectContainer;
import diva.util.PropertyContainer;
import java.util.Iterator;

/**
 * An edge adapter is used by GraphModel to manipulate edge objects in
 * the application's graph data structure. Since GraphModel doesn't
 * know what this data structure is, adapter objects are used to allow
 * it to manipulate this data structure. Typically, an application
 * will implement EdgeAdapter once for each distinct type of edge
 * in the graph.
 *
 * <P> Edges can be hyperedges. In this case, the methods that get a
 * head or tail node may return an unpredictable result. Methods that
 * set a head or tail node should do nothing. The method getHyperContents()
 * will return a node adapter, which can be used to get nodes of the
 * hyperedge; edges in the hyperedge should then be connected to these
 * sub-nodes.
 *
 * <p> One aspect of the edge adapter that may cause some confusion
 * is the fact that edges must be added to and removed from their parent.
 * Although some graph data structure do not have an explicit notion of
 * edge parents, some do. We decided that explicitly including these
 * methods in the interface provided the most generic support mechanism.
 * Applications in which edges do not have a notion of parent should
 * simply implement setParent() as a null method, and implement getParent()
 * to always return the root graph.
 *
 * <p>See also the documentation for the NodeAdapter interface for
 * additional notes about node and edge adapters.
 * 
 * @author Michael Shilman
 * @author John Reekie
 * @version $Revision: 1.2 $
 * @rating Red
 * @see NodeAdapter
 */
public interface EdgeAdapter {
    /**
     * Return whether or not the given node is a valid
     * head of this edge.
     */
    public boolean acceptHead (Object edge, Object head);

    /**
     * Return whether or not the given node is a valid
     * tail of this edge.
     */
    public boolean acceptTail (Object edge, Object tail);

    /**
     * Return the node at the head of this edge. The meaning of this method
     * is undefined if this edge is a hyperedge.
     */
    public Object getHead (Object edge);

    /**
     * Get the node adapter for the content of a hyper-edge. This method
     * should return null is isHyper() is false. The returned node adapter
     * provides access to the internal nodes of the hyper-edge.
     */
    public NodeAdapter getHyperContent (Object edge);

    /**
     * Return the parent of the given edge.
     */
    public Object getParent (Object edge);

    /**
     * Return the node at the tail of this edge. The meaning of this method
     * is undefined if this edge is a hyperedge.
     */
    public Object getTail (Object edge);

    /**
     * Return whether or not this edge is directed.
     */
    public boolean isDirected (Object edge);

    /**
     * Return whether or not this edge is a hyperedge. A hyperedge
     * itself contains nodes, which can be accessed using the
     * getHyperContent() method.
     */
    public boolean isHyper (Object edge);

    /**
     * Set the node that this edge points to.  Implementors
     * of this method are also responsible for insuring
     * that it is set properly as an "incoming" edge of
     * the node, and that it is removed as an incoming
     * edge from its previous head node. The meaning of this method
     * is undefined if this edge is a hyperedge.
     */
    public void setHead (Object edge, Object head);

    /**
     * Set the graph parent of the given edge.  Implementors of this method
     * are also responsible for insuring that it is set properly as
     * the child of the parent.
     */
    public void setParent (Object edge, Object parent);

    /**
     * Set the node that this edge stems from.  Implementors
     * of this method are also responsible for insuring
     * that it is set properly as an "outgoing" edge of
     * the node, and that it is removed as an outgoing
     * edge from its previous tail node. The meaning of this method
     * is undefined if this edge is a hyperedge.
     */
    public void setTail (Object edge, Object tail);
}

