/*
 * $Id: ClassBuilder.java,v 1.3 2001/07/22 22:01:51 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.rcl;

import diva.util.xml.*;


/**
 * Translate to and from XML for objects whose only parameters
 * are their class names.
 * 
 * @author Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.3 $
 * @rating Red
 */
public class ClassBuilder extends AbstractXmlBuilder {
    /**
     * Build an instance of the given type using Java reflection.
     */
    public Object build(XmlElement elt, String type) 
            throws Exception {
        if(elt.getChildList().size() > 0) {
            String err = getClass().getName() +
                " expects no children in build()";
            throw new Exception(err);
        }
        Class c = this.getClass().forName(type);
        return c.newInstance();
    }

    /**
     * Generate XML that contains the given object's class name
     * and no children.
     */
    public XmlElement generate(Object in) {
        return new XmlElement(in.getClass().getName());
    }
}

