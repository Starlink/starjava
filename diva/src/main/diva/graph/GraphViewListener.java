/*
 * $Id: GraphViewListener.java,v 1.1 2000/07/23 23:32:50 neuendor Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.graph;

/**
 * A listener for changes in a graph's structure or contents,
 * which are communicated through GraphViewEvent objects.  GraphViewListeners
 * register themselves with a GraphViewModel object, and receive events
 * from Nodes and Edges contained by that model's root graph
 * or any of its subgraphs.
 *
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @author Steve Neuendorffer  (neuendor@eecs.berkeley.edu)
 * @version $Revision: 1.1 $
 * @rating Red
 */
public interface GraphViewListener extends java.util.EventListener {

    /**
     */
    public void nodeMoved(GraphViewEvent e);

    /**
     */
    public void edgeRouted(GraphViewEvent e);

    /**
     */
    public void nodeDrawn(GraphViewEvent e);

    /**
     */
    public void edgeDrawn(GraphViewEvent e);
}

