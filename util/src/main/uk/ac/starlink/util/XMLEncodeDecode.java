/*
 * Copyright (C) 2001 Central Laboratory of the Research Councils 
 */
package uk.ac.starlink.util;

import org.w3c.dom.Element;

/**
 * Interface for objects that can encode and decode their internal
 * configuration within the content of an {@link org.w3c.dom.Element}.
 * <p>
 * Once encoded into an Element such objects can be represented as
 * simple XML and either transmitted and re-created in some other
 * process, or stored permanently in a file.
 * <p>
 * For an abstract base class that implements lots of useful functions
 * that support this interface see 
 * {@link PrimitiveXMLEncodeDecode} and for one that also supports
 * awt primitives (Fonts and Colors) see
 * <code>uk.ac.starlink.ast.gui.AbstractPlotControlsModel</code>.
 *
 * @since $Date$
 * @since 26-JUL-2001
 * @author Peter W. Draper
 * @version $Id$
 */
public interface XMLEncodeDecode
{
    /**
     * Encode the internal state of this object so that it is rooted
     * in the given Element.
     *
     * @param rootElement the Element within which the object should
     *                    store its configuration.
     */
    public void encode( Element rootElement );

    /**
     * Decode (ie, restore) the internal state of this object from an
     * Element.
     *
     * @param rootElement the element to which a previous object this
     *                    this type has attached its configuration.
     */
    public void decode( Element rootElement );

    /**
     * Return a name for the Element that will be the parent of any
     * contents that can be encoded and decoded (ie, name of
     * rootElement);
     * 
     * @return the name of the root element.
     */
    public String getTagName();

    // XXX deal with namespaces.
}
