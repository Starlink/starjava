package uk.ac.starlink.splat.util;

import org.jdom.Element;

/**
 * Interface for objects that can encode and decode their internal
 * configuration as XML snippets that are rooted in an Element. These
 * objects are expected to take part in a permanent configuration
 * storage system that can identify which class of object has written
 * the information and can hence identify an object of the same class
 * that should re-configure itself to match (or perhaps create an
 * object of the correct class). 
 *
 * @since $Date$
 * @since 26-JUL-2001
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2001 Central Laboratory of the Research Councils 
 */
public interface XMLEncodeAndDecode
{
    /**
     * Encode the internal state of this object into an XML snippet
     * rooted in an Element.
     *
     * @param rootElement the Element within which the object should
     *                    store its configuration.
     */
    public void encode( Element rootElement );

    /**
     * Decode (i.e. restore) the internal state of this object from an
     * XML Element.
     *
     * @param rootElement the element to which a previous object this
     *                    this type has attached its configuration.
     */
    public void decode( Element rootElement );
}
