/*
 * $Id: CompositeModel.java,v 1.1 2000/08/29 00:37:18 neuendor Exp $
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
 * @version $Revision: 1.1 $
 * @rating Red
 */
public interface CompositeModel {
    /**
     * Return an iterator over the nodes that this graph contains.
     */
    public Iterator nodes(Object composite);

    /**
     * Return a count of the nodes this graph contains.
     */
    public int getNodeCount(Object composite);
}
