/*
 * $Id: AttributeAdapter.java,v 1.1 2002/08/15 03:04:48 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.graphx;

import diva.util.SemanticObjectContainer;
import diva.util.PropertyContainer;
import java.util.Iterator;

/**
 * An adapter to allow the Diva graph interaction code to operate
 * on attributes of graph elements. This is used for two purposes:
 * <ul>
 * <li> For arbitrary attributes or properties of graph nodes and edges.
 * <li> For manipulating properties that the Diva graph editors
 * understand (for example, "location.").
 * </ul>
 *
 * <p> The adaptor basically just maps keyword-value pairs to however
 * the graph elements store this information. In some cases, graph
 * elements may just contain a Map themselves. In others, they
 * may map an attribute to a pair of getter and setter methods.
 *
 * <p> With the editing attributes, there is always the possiblity
 * that attributes expected by the Diva editors may collide with
 * pre-existing attributes in your graph data structure. At
 * present (and possibly forever) the only way around this is to
 * used the AttributeAdapter to rename these attributes. For
 * example, if your nodes have a "location" attribute that
 * means something other than the position of the node's visual
 * representation, then make the AttributeAdapter think it's
 * called eg "fooLocation."
 *
 * @author John Reekie  (johnr@eecs.berkeley.edu)
 * @version $Revision: 1.1 $
 * @rating Red
 */
public interface AttributeAdapter {

    /**
     * Return an iterator over the names of the attributes that
     * this element knows about.
     */
    public Iterator attributeNames (Object element);

    /**
     * Get the value of an attribute. This method should always
     * return an object if the attribute is valid, so that things
     * like visual property editors know what type the object is.
     * If the attribute is invalid, return null.
     */
    public Object getAttribute (Object element, String name);

    /**
     * Get a "proxy" for the named attribute of the given element.
     * If a non-null result is returned, then property editors and
     * so on will be able to perform more sophistcated editing
     * on graph elements. If null is returned, then Diva will
     * just do the best it can with the methods in this
     * interface (AttributeAdapter).
     */
    public AttributeProxy getAttributeProxy (Object element, String name);

    /** 
     * Set the value of an attribute. The value argument should
     * never be null.
     */
    public void setAttribute (Object element, String name, Object value);
}
