/*
 * $Id: CompositeNode.java,v 1.2 2000/07/02 03:23:47 michaels Exp $
 *
 * Copyright (c) 2000 The Regents of the University of California.
 * All rights reserved.  See the file COPYRIGHT for details.
 */
package diva.graph.modular;

/**
 * A node that is also a graph, i.e. it can contain other nodes.
 * 
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.2 $
 * @rating Red
 */
public interface CompositeNode extends Node, Graph {
}
