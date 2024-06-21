package uk.ac.starlink.votable;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Content handler for parsing XML to build a DOM in a customisable way.
 * It enables different content handlers to be plugged in at different times;
 * an installed handler may install a different handler to
 * cope with subsequent events.  The idea is that to customisze
 * parsing functionality you should instantiate a new subclass of the 
 * member class <code>DefaultContentHandler</code>
 * or <code>NullContentHandler</code>
 * and plug this in using {@link #setCustomHandler}.
 * <p>
 * I thought there would be this functionality somewhere in the J2SE,
 * but I can't find it, even in the non-public classes.
 * <p>
 * To use this class, install it as the {@link org.xml.sax.ContentHandler}
 * of a SAX parser ({@link org.xml.sax.XMLReader}), do a <code>parse</code>,
 * and get the result of the {@link #getDocument} method.
 * 
 * @author   Mark Taylor (Starlink)
 */
class CustomDOMBuilder implements ContentHandler {

    private final SAXDocumentBuilder builder;
    private final Namespacing namespacing;
    private ContentHandler customHandler = new DefaultContentHandler();
    private Locator locator;

    /**
     * Constructor.
     *
     * @param  strict whether to effect a strict reading of the VOTable standard
     */
    public CustomDOMBuilder( boolean strict ) {
        builder = new VOSAXDocumentBuilder( strict );
        namespacing = Namespacing.getInstance();
    }

    /**
     * Sets the object used for handling SAX events until further notice.
     *
     * @param  handler  new content handler
     */
    public void setCustomHandler( ContentHandler handler ) {
        this.customHandler = handler;
    }

    /**
     * Returns the object currently used for handling SAX events.
     *
     * @return  current content handler
     */
    public ContentHandler getCustomHandler() {
        return customHandler;
    }

    /**
     * Returns the node most recently added to the document.
     * Immediately following a call to the default content handler's
     * <code>startElement</code> method, this will be the element which has
     * just been started.
     *
     * @return   lastest node added to the DOM
     */
    public Node getNewestNode() {
        return builder.getNewestNode();
    }

    /**
     * Returns the locator last passed to the base builder.
     *
     * @return   document locator
     */
    public Locator getLocator() {
        return locator;
    }

    /**
     * Returns the document which has been built by this object.
     * Should be called following a SAX parse invoked using this object
     * as a ContentHandler.
     *
     * @return  DOM corresponding to last SAX parse
     */
    public Document getDocument() {
        return builder.getDocument();
    }

    /**
     * Returns the name of an element as an unqualified string like "TABLE" in
     * the VOTable namespace, given the various name items that
     * SAX provides for a start/end element event.
     *
     * @param  namespaceURI  namespaceURI
     * @param  localName   local name
     * @param  qName   qualified name
     */
    protected String getVOTagName( String namespaceURI, String localName,
                                   String qName ) {
        return namespacing.getVOTagName( namespaceURI, localName, qName );
    }

    /**
     * Returns the name of an element as an unqualified string like "TABLE" in
     * the VOTable namespace, given a DOM element.
     *
     * @param   el  element to test
     * @return   VOTable tag name
     */
    protected String getVOTagName( Element el ) {
        return namespacing.getVOTagName( el );
    }

    /**
     * Returns the value of an attribute.
     *
     * @param  atts  attribute set
     * @param  name  normal VOTable name of the attribute
     * @return  value of attribute <code>name</code> or null if it doesn't exist
     */
    protected String getAttribute( Attributes atts, String name ) {
        String val = atts.getValue( name );
        return val != null ? val : atts.getValue( "", name );
    }

    //
    // Methods delegating implementation to custom handler instance.
    //

    public void setDocumentLocator( Locator locator ) {
        customHandler.setDocumentLocator( locator );
        this.locator = locator;
    }
    public void startDocument() throws SAXException {
        customHandler.startDocument();
    }
    public void endDocument() throws SAXException {
        customHandler.endDocument();
    }
    public void startPrefixMapping( String prefix, String uri )
            throws SAXException {
        customHandler.startPrefixMapping( prefix, uri );
    }
    public void endPrefixMapping( String prefix ) throws SAXException {
        customHandler.endPrefixMapping( prefix );
    }
    public void startElement( String namespaceURI, String localName,
                              String qName, Attributes atts ) 
            throws SAXException {
        customHandler.startElement( namespaceURI, localName, qName, atts );
    }
    public void endElement( String namespaceURI, String localName,
                            String qName ) throws SAXException {
        customHandler.endElement( namespaceURI, localName, qName );
    }
    public void characters( char[] ch, int start, int length )
            throws SAXException {
        customHandler.characters( ch, start, length );
    }
    public void ignorableWhitespace( char[] ch, int start, int length )
            throws SAXException {
        customHandler.ignorableWhitespace( ch, start, length );
    }
    public void processingInstruction( String target, String data )
            throws SAXException {
        customHandler.processingInstruction( target, data );
    }
    public void skippedEntity( String name ) throws SAXException {
        customHandler.skippedEntity( name );
    }


    /**
     * ContentHandler implementation which delegates all methods to
     * the base builder object.  Subclasses should extend this member
     * class with their own member classes to provide custom handlers.
     */
    public class DefaultContentHandler implements ContentHandler {
        public void setDocumentLocator( Locator locator ) {
            builder.setDocumentLocator( locator );
        }
        public void startDocument() throws SAXException {
            builder.startDocument();
        }
        public void endDocument() throws SAXException {
            builder.endDocument();
        }
        public void startPrefixMapping( String prefix, String uri )
                throws SAXException {
            builder.startPrefixMapping( prefix, uri );
        }
        public void endPrefixMapping( String prefix ) throws SAXException {
            builder.endPrefixMapping( prefix );
        }
        public void startElement( String namespaceURI, String localName, 
                                  String qName, Attributes atts )
                throws SAXException {
            builder.startElement( namespaceURI, localName, qName, atts );
        }
        public void endElement( String namespaceURI, String localName,
                                String qName ) throws SAXException {
            builder.endElement( namespaceURI, localName, qName );
        }
        public void characters( char[] ch, int start, int length )
                throws SAXException {
            builder.characters( ch, start, length );
        }
        public void ignorableWhitespace( char[] ch, int start, int length )
                throws SAXException {
            builder.ignorableWhitespace( ch, start, length );
        }
        public void processingInstruction( String target, String data )
                throws SAXException {
            builder.processingInstruction( target, data );
        }
        public void skippedEntity( String name ) throws SAXException {
            builder.skippedEntity( name );
        }
    }

    /**
     * ContentHandler implementation which implements all methods as no-ops.
     */
    public class NullContentHandler implements ContentHandler {
        public void setDocumentLocator( Locator locator ) {
        }
        public void startDocument() throws SAXException {
        }
        public void endDocument() throws SAXException {
        }
        public void startPrefixMapping( String prefix, String uri )
                throws SAXException {
        }
        public void endPrefixMapping( String prefix ) throws SAXException {
        }
        public void startElement( String namespaceURI, String localName, 
                                  String qName, Attributes atts )
                throws SAXException {
        }
        public void endElement( String namespaceURI, String localName,
                                String qName ) throws SAXException {
        }
        public void characters( char[] ch, int start, int length )
                throws SAXException {
        }
        public void ignorableWhitespace( char[] ch, int start, int length )
                throws SAXException {
        }
        public void processingInstruction( String target, String data )
                throws SAXException {
        }
        public void skippedEntity( String name ) throws SAXException {
        }
    }

}
