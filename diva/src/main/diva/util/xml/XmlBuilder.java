/*
 * $Id: XmlBuilder.java,v 1.4 2000/05/02 00:45:34 johnr Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.util.xml;

/**
 * An XmlBuilder is an interface that can be implemented by classes
 * that convert between XmlElements and some internal
 * data representation. The main reason for doing so is to allow
 * other builders to "reuse" parts of an XML DTD. For example,
 * we could have a builder that builds Java2D objects, such as
 * "line" and "polygon". Then some other builder, that for example
 * builds libraries of graphical icons, can use an instance of the
 * Java2D builder internally -- if it does not recognize the type
 * of an XML element, it calls the Java2D builder to see if it can
 * get a low-level graphical object.
 *
 * @author John Reekie (johnr@eecs.berkeley.edu)
 * @version $Revision: 1.4 $
 */
public interface XmlBuilder {

    /** Given an XmlElement, create and return an internal representtion
     * of it. Implementors should also provide a more
     * type-specific version of this method:
     * <pre>
     *   public Graph build (XmlELement elt);
     * </pre> 
     */
    public Object build (XmlElement elt, String type);

    /** Given an object, produce an XML representation of it.
     */
    public XmlElement generate (Object obj);
}

