/*
 * $Id: AbstractGlobalLayout.java,v 1.1 2000/07/23 23:32:50 neuendor Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.graph.layout;

/**
 * An abstract implementation of the GlobalLayout interface.
 *
 * @author Steve Neuendorffer (neuendor@eecs.berkeley.edu
 * @version $Revision: 1.1 $
 * @rating Red
 */
public abstract class AbstractGlobalLayout implements GlobalLayout {
    
    LayoutTarget _layoutTarget;

    /** Create a new global layout that uses the given layout target.
     */
    public AbstractGlobalLayout(LayoutTarget target) {
	_layoutTarget = target;
    } 

    /** Return the layout target.
     */
     public LayoutTarget getLayoutTarget() {
	return _layoutTarget;
    }
    
    /** Set the layout target.
     */
    public void setLayoutTarget(LayoutTarget target) {
	_layoutTarget = target;
    }

    /**
     * Layout the graph model in the viewport
     * specified by the layout target environment.
     */
    public abstract void layout(Object composite);
}

