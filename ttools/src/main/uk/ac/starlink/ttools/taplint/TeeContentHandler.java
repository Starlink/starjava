package uk.ac.starlink.ttools.taplint;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * ContentHandler implementation which multiplexes SAX events to
 * both of two supplied daughter handlers.  No other action is taken.
 *
 * @author   Mark Taylor
 * @since    10 Jun 2011
 */
class TeeContentHandler implements ContentHandler {

    private final ContentHandler th1_;
    private final ContentHandler th2_;

    /**
     * Constructor.
     *
     * @param   th1  first subordinate handler
     * @param   th2  second subordinate handler
     */
    public TeeContentHandler( ContentHandler th1, ContentHandler th2 ) {
        th1_ = th1;
        th2_ = th2;
    }

    public void setDocumentLocator( Locator locator ) {
        th1_.setDocumentLocator( locator );
        th2_.setDocumentLocator( locator );
    }

    public void startDocument() throws SAXException {
        th1_.startDocument();
        th2_.startDocument();
    }

    public void endDocument() throws SAXException {
        th1_.endDocument();
        th2_.endDocument();
    }

    public void startPrefixMapping( String prefix, String uri )
            throws SAXException {
        th1_.startPrefixMapping( prefix, uri );
        th2_.startPrefixMapping( prefix, uri );
    }

    public void endPrefixMapping( String prefix ) throws SAXException {
        th1_.endPrefixMapping( prefix );
        th2_.endPrefixMapping( prefix );
    }

    public void startElement( String uri, String localName, String qName,
                              Attributes atts ) throws SAXException {
        th1_.startElement( uri, localName, qName, atts );
        th2_.startElement( uri, localName, qName, atts );
    }

    public void endElement( String uri, String localName, String qName )
            throws SAXException {
        th1_.endElement( uri, localName, qName );
        th2_.endElement( uri, localName, qName );
    }

    public void characters( char[] ch, int start, int length )
            throws SAXException {
        th1_.characters( ch, start, length );
        th2_.characters( ch, start, length );
    }

    public void ignorableWhitespace( char[] ch, int start, int length )
            throws SAXException {
        th1_.ignorableWhitespace( ch, start, length );
        th2_.ignorableWhitespace( ch, start, length );
    }

    public void processingInstruction( String target, String data )
            throws SAXException {
        th1_.processingInstruction( target, data );
        th2_.processingInstruction( target, data );
    }

    public void skippedEntity( String name ) throws SAXException {
        th1_.skippedEntity( name );
        th2_.skippedEntity( name );
    }
}
