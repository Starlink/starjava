package uk.ac.starlink.votable;

import org.apache.crimson.tree.XmlDocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * Crimson implementation of SAXDocumentBuilder.
 * This makes use of protected members of a class from Apache Crimson
 * which is present but not documented in Sun's J2SE1.4.
 * This is obviously not ideal, and may not work in other JREs - in fact
 * it's known not to work in Sun's J2SE1.5beta1.
 * So I really need to find/write an implementation which does not depend
 * on undocumented items.  It's probably not too hard to write one
 * using only JAXP classes.
 *
 * @author   Mark Taylor (Starlink)
 */
class CrimsonSAXDocumentBuilder implements SAXDocumentBuilder {

    /**
     * Extend the Crimson document builder class to access some of
     * its protected members.
     */
    private static class CrimsonBuilder extends XmlDocumentBuilder {
        Node getNewestNode() {
            return elementStack[ topOfStack ];
        }
        Locator getLocator() {
            return locator;
        }
    }
    private CrimsonBuilder crimsonBuilder = new CrimsonBuilder();

    public Node getNewestNode() {
        return crimsonBuilder.getNewestNode();
    }

    public Locator getLocator() {
        return crimsonBuilder.getLocator();
    }

    public Document getDocument() {
        return crimsonBuilder.getDocument();
    }

    // ContentHandler interface.

    public void setDocumentLocator( Locator locator ) {
        crimsonBuilder.setDocumentLocator( locator );
    }
    public void startDocument() throws SAXException {
        crimsonBuilder.startDocument();
    }
    public void endDocument() throws SAXException {
        crimsonBuilder.endDocument();
    }
    public void startPrefixMapping( String prefix, String uri )
            throws SAXException {
        crimsonBuilder.startPrefixMapping( prefix, uri );
    }
    public void endPrefixMapping( String prefix ) throws SAXException {
        crimsonBuilder.endPrefixMapping( prefix );
    }
    public void startElement( String namespaceURI, String localName,
                              String qName, Attributes atts )
            throws SAXException {
        crimsonBuilder.startElement( namespaceURI, localName, qName, atts );
    }
    public void endElement( String namespaceURI, String localName,
                            String qName ) throws SAXException {
        crimsonBuilder.endElement( namespaceURI, localName, qName );
    }
    public void characters( char[] ch, int start, int length )
            throws SAXException {
        crimsonBuilder.characters( ch, start, length );
    }
    public void ignorableWhitespace( char[] ch, int start, int length )
            throws SAXException {
        crimsonBuilder.ignorableWhitespace( ch, start, length );
    }
    public void processingInstruction( String target, String data )
            throws SAXException {
        crimsonBuilder.processingInstruction( target, data );
    }
    public void skippedEntity( String name ) throws SAXException {
        crimsonBuilder.skippedEntity( name );
    }

}
