package uk.ac.starlink.votable.soap;

import org.apache.axis.message.SOAPHandler;
import org.apache.axis.encoding.DeserializationContext;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * SOAPHandler subclass which delegates the SAX and SOAPHandler callbacks
 * to a given ContentHandler.
 * The way this class is implemented relies on some assumptions about
 * what it's going to be used for (custom deserialization within 
 * an AXIS context) - those assumptions are not very natural ones for
 * the way that SAX content handlers are written within this package,
 * which makes the implementation a bit fiddly.  The main difference
 * between the AXIS way and how I've done it is that AXIS has a new
 * ContentHandler (acutally, a new SOAPHandler) for each element, 
 * while I have one for the whole document.
 *
 * @author   Mark Taylor (Starlink)
 * @since    23 Mar 2005
 */
class DelegatingSOAPHandler extends SOAPHandler {

    private final ContentHandler baseHandler_;

    /**
     * Constructs a new handler from a delegate.  The delegate will receive
     * SAX-like callbacks for each SAX event encountered by this 
     * SOAP handler.
     *
     * @param   baseHandler   delegate content handler
     */
    public DelegatingSOAPHandler( ContentHandler baseHandler ) {
        baseHandler_ = baseHandler;
    }


    /*
     * Override SOAPHandler methods for custom behaviour.
     */

    public void startElement( String namespaceURI, String localName,
                              String prefix, Attributes atts,
                              DeserializationContext context )
            throws SAXException {
        startElement( namespaceURI, localName, getQName( prefix, localName ),
                      atts );
    }

    public void endElement( String namespaceURI, String localName,
                            String prefix, Attributes atts,
                            DeserializationContext context ) 
            throws SAXException {
        endElement( namespaceURI, localName, getQName( prefix, localName ) );
    }

    public SOAPHandler onStartChild( String namespace, String localName,
                                     String prefix, Attributes atts,
                                     DeserializationContext context ) {

        /* Return this object to indicate that the same object is used
         * to handle the current element's children as to handle the
         * element itself. */
        return this;
    }

    public void onEndChild( String namespaceURI, String localName,
                            DeserializationContext context )
            throws SAXException {

        /* Make sure endElement is called - AXIS doesn't do it. */
        endElement( namespaceURI, localName, (String) null );
    }

    /*
     * Override ContentHandler methods, passing them through to the delegate.
     */

    public void startDocument() throws SAXException {
        baseHandler_.startDocument();
    }

    public void endDocument() throws SAXException {
        baseHandler_.endDocument();
    }

    public void startElement( String namespaceURI, String localName,
                              String qName, Attributes atts )
            throws SAXException {
        baseHandler_.startElement( namespaceURI, localName, qName, atts );
    }

    public void endElement( String namespaceURI, String localName,
                            String qName ) throws SAXException {
        baseHandler_.endElement( namespaceURI, localName, qName );
    }

    public void characters( char[] ch, int start, int length )
            throws SAXException {
        baseHandler_.characters( ch, start, length );
    }

    public void processingInstruction( String target, String data )
            throws SAXException {
        baseHandler_.processingInstruction( target, data );
    }

    public void startPrefixMapping( String prefix, String uri )
            throws SAXException {
        baseHandler_.startPrefixMapping( prefix, uri );
    }

    public void endPrefixMapping( String prefix ) throws SAXException {
        baseHandler_.endPrefixMapping( prefix );
    }

    public void setDocumentLocator( Locator locator ) {
        baseHandler_.setDocumentLocator( locator );
    }

    public void skippedEntity( String name ) throws SAXException {
        baseHandler_.skippedEntity( name );
    }

    public void ignorableWhitespace( char[] ch, int start, int length ) 
            throws SAXException {
        baseHandler_.ignorableWhitespace( ch, start, length );
    }


    /**
     * Get a qname string (as used by ContentHandler methods) from a 
     * prefix and a localname.
     *
     * @param   prefix  prefix
     * @param   localName  local part of name
     * @return   string for use in XML text
     */
    private static String getQName( String prefix, String localName ) {

        /* Is this right, or should localName be null sometimes? */
        return ( prefix == null || prefix.length() == 0 )
             ? localName
             : prefix + ":" + localName;
    }
}
