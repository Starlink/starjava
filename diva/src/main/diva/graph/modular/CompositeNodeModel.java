/*
 * $Id: CompositeNodeModel.java,v 1.1 2000/08/29 00:37:19 neuendor Exp $
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
public interface CompositeNodeModel extends NodeModel, CompositeModel {
}
