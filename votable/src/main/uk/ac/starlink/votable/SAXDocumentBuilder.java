package uk.ac.starlink.votable;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;

/**
 * Defines a SAX content handler which builds a DOM.
 * The DOM node which it has just built can be got using the
 * {@link #getNewestNode} method.
 * <p>
 * Although this functionality has to exist somewhere in the J2SE,
 * I can't see any way in the public API to obtain one, so we have to
 * implement one ourselves or pinch one from elsewhere.
 * 
 * @author   Mark Taylor (Starlink)
 */
interface SAXDocumentBuilder extends ContentHandler {

    /**
     * Returns the DOM node most recently built by this handler.
     *
     * @return  node in built DOM
     */
    Node getNewestNode();

    /**
     * Returns the locator most recently set on this handler.
     *
     * @return  stream locator
     */
    Locator getLocator();

    /**
     * Returns the DOM document that this builder is building.
     *
     * @return  DOM document
     */
    Document getDocument();
}
