package uk.ac.starlink.ttools.votlint;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * This class provides a hack so that XSD validation can work even
 * when the wrong namespace is declared in the source document.
 * The point of this is that if the document has made a small namespacing
 * error, schema validation by default will simply refuse to recognise
 * any of the elements and provide no further validation reporting.
 * This class hacks the SAX event stream to its downstream client so
 * that if no namespace declaration has been received in the expected
 * namespace, the default namespace is declared as the expected one.
 * If the expected namespace is declared explicitly, this class makes
 * no change to downstream SAX events.
 *
 * <p>This is not very respectable, but will have the desired effect for
 * most VOTable documents (which do not in any case make much use of
 * namespacing).
 *
 * @author   Mark Taylor
 * @since    22 Nov 2012
 */
public class FudgeNamespaceContentHandler implements ContentHandler {

    private final ContentHandler base_;
    private final String fudgeElement_;
    private final String fudgeNamespace_;
    private int level_;
    private String mappedPrefix_;

    /**
     * Constructor.
     *
     * @param   base  downstream SAX event sink
     * @param   tagName  name of element within which namespace hacking
     *                   should take place
     * @param   ns  namespace to insert
     */ 
    public FudgeNamespaceContentHandler( ContentHandler base,
                                         String tagName, String ns ) {
        base_ = base;
        fudgeElement_ = tagName;
        fudgeNamespace_ = ns;
    }

    public void setDocumentLocator( Locator locator ) {
        base_.setDocumentLocator( locator );
    }

    public void startDocument() throws SAXException {
        base_.startDocument();
    }

    public void endDocument() throws SAXException {
        base_.endDocument();
    }

    public void startPrefixMapping( String prefix, String uri )
            throws SAXException {
        base_.startPrefixMapping( prefix, uri );
        if ( fudgeNamespace_.equals( uri ) ) {
            mappedPrefix_ = prefix;
        }
    }

    public void endPrefixMapping( String prefix )
            throws SAXException {
        if ( prefix.equals( mappedPrefix_ ) ) {
            mappedPrefix_ = null;
        }
        base_.endPrefixMapping( prefix );
    }

    public void startElement( String uri, String localName, String qName,
                              Attributes atts ) throws SAXException {
        if ( fudgeElement_.equals( localName ) ) {
            level_++;
        }
        base_.startElement( fudgeUri( uri ), localName, qName, atts );
    }

    public void endElement( String uri, String localName, String qName )
            throws SAXException {
        base_.endElement( fudgeUri( uri ), localName, qName );
        if ( fudgeElement_.equals( localName ) && --level_ == 0 ) {
            --level_;
        }
    }

    public void characters( char[] ch, int start, int length )
            throws SAXException {
        base_.characters( ch, start, length );
    }

    public void ignorableWhitespace( char[] ch, int start, int length )
            throws SAXException {
        base_.ignorableWhitespace( ch, start, length );
    }

    public void processingInstruction( String target, String data ) 
            throws SAXException {
        base_.processingInstruction( target, data );
    }

    public void skippedEntity( String name ) throws SAXException {
        base_.skippedEntity( name );
    }

    /**
     * Returns a possibly hacked namespace URI.
     *
     * @param  uri  given namespace URI
     * @return  namespace URI to pass on
     */
    private String fudgeUri( String uri ) {
        return ( level_ > 0 && mappedPrefix_ == null ) ? fudgeNamespace_ : uri;
    }
}
