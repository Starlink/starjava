package uk.ac.starlink.votable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.List;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import uk.ac.starlink.votable.dom.DelegatingDocument;

/**
 * Builds a VODocument DOM from SAX events.
 *
 * @author   Mark Taylor (Starlink)
 * @since    9 Sep 2004
 */
class VOSAXDocumentBuilder implements SAXDocumentBuilder {

    private Locator locator_;
    private VODocument doc_;
    private NodeStack nodeStack_ = new NodeStack();
    private Map prefixMap_ = new HashMap();
    private boolean strict_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.votable" );
    private static final String[] ELEMENTS_WITH_ID = new String[] { 
        "VOTABLE",
        "RESOURCE",
        "INFO",
        "PARAM", 
        "TABLE",
        "FIELD",
        "GROUP",
        "VALUES",
        "LINK",
        "COOSYS",
    };

    /**
     * Constructor.
     *
     * @param   strict  whether to effect a strict reading of the
     *          VOTable standard
     */
    public VOSAXDocumentBuilder( boolean strict ) {
        strict_ = strict;
    }

    public Node getNewestNode() {
        return nodeStack_.top();
    }

    public Locator getLocator() {
        return locator_;
    }

    public Document getDocument() {
        return doc_;
    }

    public void setDocumentLocator( Locator locator ) {
        locator_ = locator;
    }

    public void startDocument() throws SAXException {
        Document baseDoc;
        try {
            baseDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                     .newDocument();
        }
        catch ( ParserConfigurationException e ) {
            throw (SAXException) 
                  new SAXException( "Cannot create empty DOM", e )
                 .initCause( e );
        }
        String systemId = locator_ == null ? null : locator_.getSystemId();
        doc_ = new VODocument( baseDoc, systemId, strict_ );
        nodeStack_.push( doc_ );
    }

    public void endDocument() {
        nodeStack_.pop().normalize();
    }

    public void startPrefixMapping( String prefix, String uri ) {
        prefixMap_.put( uri, prefix );
    }

    public void endPrefixMapping( String prefix ) {
        for ( Iterator it = prefixMap_.entrySet().iterator(); it.hasNext(); ) {
            if ( ((Map.Entry) it.next()).getValue().equals( prefix ) ) {
                it.remove();
            }
        }
    }

    public void startElement( String namespaceURI, String localName,
                              String qName, Attributes atts )
            throws SAXParseException {
        try {

            /* Create a DOM element. */
            Element el;
            if ( localName != null && localName.length() > 0 ) {
                String prefix = (String) prefixMap_.get( namespaceURI );
                String qualifiedName = prefix == null || prefix.length() == 0
                                     ? localName
                                     : prefix + ":" + localName;
                el = doc_.createElementNS( namespaceURI, qualifiedName );
            }
            else {
                String tagName = qName;
                el = doc_.createElement( tagName );
            }

            /* Fill in its attributes. */
            for ( int i = 0; i < atts.getLength(); i++ ) {
                String attURI = atts.getURI( i );
                String attLocalName = atts.getLocalName( i );
                String attQName = atts.getQName( i );
                String value = atts.getValue( i );
                String type = atts.getType( i );
                Attr att;
                if ( attURI != null && attURI.length() > 0 &&
                     attQName != null && attQName.length() > 0 ) {
                    att = doc_.createAttributeNS( attURI, attQName );
                    el.setAttributeNodeNS( att );
                }
                else if ( attQName != null && attQName.length() > 0 ) {
                    att = doc_.createAttribute( attQName );
                    el.setAttributeNode( att );
                }
                else {
                    att = doc_.createAttribute( attLocalName );
                    el.setAttributeNode( att );
                }
                att.setValue( value );

                /* If we have an ID attribute, store it in the DOM. */
                if ( type.equals( "ID" ) || isVotableID( att ) ) {
                    if ( doc_.getElementById( value ) != null ) {
                        logger_.warning( "Multiple elements with ID " + value );
                    }
                    doc_.setElementId( el, value );
                }
            }

            /* Insert it into the tree. */
            nodeStack_.top().appendChild( el );

            /* Push it on the stack. */
            nodeStack_.push( el );
        }
        catch ( DOMException e ) {
            throw (SAXParseException) 
                  new SAXParseException( "DOM building error", locator_, e )
                 .initCause( e );
        }
    }

    public void endElement( String namespaceURI, String localName, 
                            String qName ) {
        nodeStack_.pop();
    }

    public void characters( char[] ch, int start, int length )
            throws SAXParseException {

        /* Ignore trailing whitespace. */
        if ( nodeStack_.isEmpty() && 
             new String( ch, start, length ).trim().length() == 0 ) {
            return;
        }
        try {
            nodeStack_.top()
                .appendChild( doc_.createTextNode( new String( ch, start,
                                                               length ) ) );
        }
        catch ( DOMException e ) {
            throw (SAXParseException) 
                  new SAXParseException( "DOM building error", locator_, e )
                 .initCause( e );
        }
    }

    public void ignorableWhitespace( char[] ch, int start, int length ) {
    }

    public void processingInstruction( String target, String data ) {
    }

    public void skippedEntity( String name ) {
        logger_.info( "Skipping entity " + name );
    }

    /**
     * Determines whether an attribute in the DOM represents an ID value,
     * based only on its name and what we know about the attribute names
     * of VOTABLE elements.
     *
     * @param  att attribute in place in the DOM
     * @return  true if it counds as an ID 
     */
    private boolean isVotableID( Attr att ) {
        if ( att.getName().equals( "ID" ) ) {
            String elName = ((VOElement) att.getOwnerElement()).getVOTagName();
            for ( int i = 0; i < ELEMENTS_WITH_ID.length; i++ ) {
                if ( ELEMENTS_WITH_ID[ i ].equals( elName ) ) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Helper class which stores encountered elements as a stack.
     */
    private static class NodeStack {
        private LinkedList stack_ = new LinkedList();

        void push( Node node ) {
            stack_.add( node );
        }

        Node pop() {
            return (Node) stack_.removeLast();
        }

        Node top() {
            return (Node) ( stack_.isEmpty() ? null : stack_.getLast() );
        }

        boolean isEmpty() {
            return stack_.isEmpty();
        }
    }
}
