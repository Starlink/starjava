/*
 * $Id: Graph.java,v 1.4 2000/07/13 00:33:53 neuendor Exp $
 *
 * Copyright (c) 2000 The Regents of the University of California.
 * All rights reserved.  See the file COPYRIGHT for details.
 */
package diva.graph.modular;
import diva.util.SemanticObjectContainer;
import diva.util.VisualObjectContainer;
import diva.util.PropertyContainer;
import java.util.Iterator;

/**
 * A graph is an object that contains nodes and
 * edges.  Edges are accessed through the nodes that
 * they connect.
 * 
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.4 $
 * @rating Red
 */
public interface Graph extends SemanticObjectContainer, PropertyContainer {
    /**
     * Return an iterator over the nodes that this graph contains.
     */
    public Iterator nodes();

    /**
     * Return a count of the nodes this graph contains.
     */
    public int getNodeCount();
}
