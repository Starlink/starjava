/*
 * $Id: SchematicGraphModel.java,v 1.2 2001/07/22 22:01:25 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.graph.schematic;

import diva.graph.*;
import diva.graph.basic.*;

/**
 * A Basic graph model that only allows connections to nodes that are not
 * composite.
 * 
 * @author Steve Neuendorffer (neuendor@eecs.berkeley.edu
 * @version $Revision: 1.2 $
 * @rating Red
 */
public class SchematicGraphModel extends BasicGraphModel {
    /**
     * Return true if the head of the given edge can be attached to the
     * given node.
     */
    public boolean acceptHead(Object edge, Object node) {
	if (isNode(node) &&
	    !isComposite(node)) {
	    return true;
	} else
	    return false;
    }

    /**
     * Return true if the tail of the given edge can be attached to the
     * given node.
     */
    public boolean acceptTail(Object edge, Object node) {
	if (isNode(node) &&
	    !isComposite(node)) {
	    return true;
	} else
	    return false;
    }
}

