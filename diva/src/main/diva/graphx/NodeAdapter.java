/*
 * $Id: NodeAdapter.java,v 1.2 2002/08/23 22:26:01 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.graphx;

import diva.util.SemanticObjectContainer;
import diva.util.PropertyContainer;
import java.util.Iterator;

/**
 * A node adapter is used by GraphModel to manipulate node objects in
 * the application's graph data structure. Since GraphModel doesn't
 * know what this data structure is, adapter objects are used to allow
 * it to manipulate this data structure. Typically, an application
 * will implement NodeAdapter once for each distinct type of node
 * in the graph.
 *
 * <P> This adapter interface assumes certain things about the graph
 * data structure. We have tried to make these assumptions such that
 * it will not be too hard to write adapters for almost all types of graph
 * Each node is connected to a single node at its head and tail (except
 * for hyper-edges, see below). Nodes and edges are contained in a
 * parent object, which can be the root graph, a composite node, or
 * a hyper-edge. The adapter interfaces do not assume anything about
 * whether edges can connect to objects outside of their parent object.

 * <P> Sometimes, it will be possible to use the adapter to construct
 * a graph that does not make sense for the native data structure. 
 * Implementations of the adapter can thrown an exception of this
 * happens. However, there is no need to be regimental about it: the
 * application is also responsible for constructing the interaction
 * that takes place on the graph, and as long as that is built correctly
 * then invalid graph structures should not occur.
 *
 * <P> Nodes can be composite, in which case they can contain nodes
 * and edges themselves. Whether or not edges that connect to sub-nodes
 * of the composite are parented by the composite, or by the root graph,
 * is something that the adapter interfaces (and GraphModel) are
 * indifferent to.
 * 
 * @author Michael Shilman
 * @author John Reekie
 * @version $Revision: 1.2 $
 * @rating Red
 */
public interface NodeAdapter {
    /**
     * Return an iterator over the nodes that this graph contains.
     */
    public Iterator nodes (Object composite);

    /**
     * Return the parent of the given node.
     */
    public Object getParent (Object node);

    /**
     * Return an iterator over the edges coming into the given node.
     */
    public Iterator inEdges (Object node);

    /**
     * Return true if the given node is a composite, that is, it
     * contains other nodes.
     */
    public boolean isComposite (Object node);

    /**
     * Return a count of the nodes this graph contains.
     */
    public int getNodeCount (Object composite);

    /**
     * Return an iterator over the edges coming out of the given node.
     */
    public Iterator outEdges (Object node);

    /**
     * Set the graph parent of the given node.  Implementors of this method
     * are also responsible for insuring that it is set properly as
     * the child of the parent.
     */
    public void setParent (Object node, Object parent);
}
