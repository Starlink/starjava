/*
 * $Id: AttributeProxy.java,v 1.1 2002/08/15 03:04:49 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.graphx;

import diva.util.SemanticObjectContainer;
import diva.util.PropertyContainer;
import java.util.Iterator;

/**
 * An attribute proxy is an object that is used to allow finer-grained
 * manipulation of attributes of graph elements. If an application
 * does not wish to provide attribute proxies, then it can simply
 * return null from the AttributeAdapter.getAttributeProxy() method,
 * and Diva will do its best with the AttributeAdapter methods. If
 * it does provide AttributeProxies, then manipulation
 * of your graph elements works a little better.
 *
 * @author John Reekie  (johnr@eecs.berkeley.edu)
 * @version $Revision: 1.1 $
 * @rating Red
 */
public interface AttributeProxy {

    /**
     * Get the attribute name.
     */
    public String getName ();

    /**
     * Get the class of the attribute. This is used by attribute
     * editors to correctly set up interactive editors. Any value that
     * is subsequently returned by getValue() must be a subclass
     * of the returned class. <b>Note</b>: be careful not to confuse
     * this method with the getClass() method.
     */
    public Class getType ();

    /**
     * Get the attribute value. This method should return the same
     * result as AttributeAdapter.attributeValue().
     */
    public Object getValue ();

    /** Return true if the value of the given attribute is acceptable.
     * Property editors and the like can use this to provide feedback
     * in a UI as to why an attempted attribute change did not work.
     */
    public boolean isValid (Object value);

    /** Return true if this attribute is "visible." This tells
     * attribute editors and so on whether or not to display
     * this attribute.
     */
    public boolean isVisible ();

    /** Return true if this attribute is writable. This is
     * used by property editors and so on to test whether
     * certain attributes should be made editable.
     */
    public boolean isWritable ();

    /** 
     * Set the attribute value.
     */
    public void setValue (Object value);
}
