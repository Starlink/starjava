/*
 * $Id: BasicLWNode.java,v 1.2 2002/02/06 03:27:44 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 *
 */

package diva.pod.lwgraph;

import diva.util.PropertyContainer;

import java.util.HashMap;
import java.util.Iterator;


/** A basic implementation of the light-weight node interface.
 * This class implements a node that contains a set of attributes.
 *
 * @version $Revision: 1.2 $
 * @author John Reekie
 * @rating Red
 */
public class BasicLWNode implements LWNode, PropertyContainer {

    // Integer id
    private int _id = -1;

    // Properties
    private HashMap _properties;

    /** Create a new node
     */
    public BasicLWNode () {
    }

    /** Return the integer id of this node
     */
    public int getNodeId () {
	return _id;
    }

    /**
     * Return the property corresponding to
     * the given key, or null if no such property
     * exists.
     */
    public Object getProperty (String key) {
	if (_properties == null) {
	    return null;
	}
	return _properties.get(key);
    }

    /** Set the integer id of this node.
     */
    public void setNodeId (int id) {
	_id = id;
    }

    /** Return an iteration of the names of the properties
     */
    public Iterator propertyNames() {
        return _properties.keySet().iterator();
    }

    /**
     * Set the property corresponding to
     * the given key.
     */
    public void setProperty (String key, Object value) {
	if (_properties == null) {
	    _properties = new HashMap();
	}
	_properties.put(key, value);
    }
}
