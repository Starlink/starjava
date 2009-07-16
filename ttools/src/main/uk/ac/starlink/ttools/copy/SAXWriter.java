package uk.ac.starlink.ttools.copy;

import java.io.IOException;
import java.io.Writer;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.LexicalHandler;

/**
 * SAX handler which does its best to reproduce an XML document by writing
 * SAX events to an output stream.
 * 
 * @author   Mark Taylor (Starlink)
 * @since    18 Apr 2005
 */
class SAXWriter implements ContentHandler, LexicalHandler {

    private Writer out_;
    private Locator locator_;
    private String pendingTag_;
    private boolean cdata_;

    /**
     * Sets the destination stream.  This method must be called before
     * the handler is used.
     *
     * @param   out  output stream
     */
    public void setOutput( Writer out ) {
        out_ = out;
    }

    public void setDocumentLocator( Locator locator ) {
        locator_ = locator;
    }

    public void startDocument() throws SAXException {
    }

    public void endDocument() throws SAXException {
        out( "\n" );
        try {
            out_.flush();
        }
        catch ( IOException e ) {
            throw wrapException( e );
        }
        locator_ = null;
    }

    public void startElement( String namespaceURI, String localName,
                              String qName, Attributes atts )
            throws SAXException {
        flushTag();
        StringBuffer buf = new StringBuffer( "<" + qName );
        for ( int i = 0; i < atts.getLength(); i++ ) {
            buf.append( ' ' )
               .append( atts.getQName( i ) )
               .append( "=\"" );
            String value = atts.getValue( i );
            for ( int j = 0; j < value.length(); j++ ) {
                char c = value.charAt( j );
                switch ( c ) {
                    case '<':
                        buf.append( "&lt;" );
                        break;
                    case '>':
                        buf.append( "&gt;" );
                        break;
                    case '&':
                        buf.append( "&amp;" );
                        break;
                    case '"':
                        buf.append( "&quot;" );
                        break;
                    default:
                        buf.append( c );
                }
            }
            buf.append( '"' );
        }
        pendingTag_ = buf.toString();
    }

    public void endElement( String namespaceURI, String localName,
                            String qName ) throws SAXException {
        if ( pendingTag_ != null ) {
            out( pendingTag_ );
            out( "/>" );
            pendingTag_ = null;
        }
        else {
            out( "</" + qName + ">" );
        }
    }

    public void characters( char[] ch, int start, int length ) 
            throws SAXException {
        flushTag();
        escapeOut( ch, start, length );
    }

    public void ignorableWhitespace( char[] ch, int start, int length )
            throws SAXException {
        flushTag();
        out( ch, start, length );
    }

    public void skippedEntity( String name ) throws SAXException {
        flushTag();
        out( "&" + name + ";" );
    }

    public void processingInstruction( String target, String data ) 
            throws SAXException {
        flushTag();
        out( "<?" + target + " " );
        out( data );
        out( "?>" );
        out( "\n" );
    }

    public void startPrefixMapping( String prefix, String uri ) {
    }

    public void endPrefixMapping( String prefix ) {
    }

    public void comment( char[] ch, int start, int length )
            throws SAXException {
        flushTag();
        out( "<!--" );
        out( ch, start, length );
        out( "-->" );
    }

    public void startCDATA() throws SAXException {
        flushTag();
        out( "<![CDATA[" );
        cdata_ = true;
    }

    public void endCDATA() throws SAXException {
        out( "]]>" );
        cdata_ = false;
    }

    public void startDTD( String name, String publicId, String systemId ) {
    }

    public void endDTD() {
    }

    public void startEntity( String name ) {
    }

    public void endEntity( String name ) {
    }

    /**
     * Ensures that any pending output has been written.  
     * If other classes wish to write directly to this writer's 
     * destination stream, they must call this method first.
     */
    public void flush() throws SAXException {
        flushTag();
    }

    /**
     * Ensures that any pending start tag has been output.
     */
    private void flushTag() throws SAXException {
        if ( pendingTag_ != null ) {
            out( pendingTag_ );
            out( '>' );
            pendingTag_ = null;
        }
    }

    /**
     * Convenience method to returns a SAXException which wraps an IOException.
     *
     * @param   e  exception
     * @return  exception containing e
     */
    private SAXException wrapException( IOException e ) {
        return (SAXException)
               new SAXParseException( e.getMessage(), locator_ )
              .initCause( e );
    }

    /**
     * Writes a single raw character.
     */
    private void out( char c ) throws SAXException {
        try {
            out_.write( c );
        }
        catch ( IOException e ) {
            throw wrapException( e );
        }
    }

    /**
     * Writes a section of an array raw.
     *
     * @param   ch  array
     * @param   start   first element to write
     * @param   leng    number of elements to write
     */
    private void out( char[] ch, int start, int leng ) throws SAXException {
        try {
            out_.write( ch, start, leng );
        }
        catch ( IOException e ) {
            throw wrapException( e );
        }
    }
 
    /** 
     * Writes a string raw.
     *
     * @param   text   string to write
     */
    private void out( String text ) throws SAXException {
        try {
            out_.write( text );
        }
        catch ( IOException e ) {
            throw wrapException( e );
        }
    }

    /**
     * Writes a string, with special characters escaped if necessary.
     *
     * @param  text   string to write
     */
    private void escapeOut( String text ) throws SAXException {
        escapeOut( text.toCharArray(), 0, text.length() );
    }

    /**
     * Writes a section of array, with special characters escaped if necessary.
     * 
     * @param   ch  array
     * @param   start   first element to write
     * @param   leng    number of elements to write
     */
    private void escapeOut( char[] ch, int start, int leng )
            throws SAXException {
        try {
            if ( cdata_ ) {
                out( ch, start, leng );
            }
            else {
                while ( leng-- > 0 ) {
                    char c = ch[ start++ ];
                    switch ( c ) {
                        case '<':
                            out_.write( "&lt;" );
                            break;
                        case '>':
                            out_.write( "&gt;" );
                            break;
                        case '&':
                            out_.write( "&amp;" );
                            break;
                        case '\"':
                            out_.write( "&quot;" );
                            break;
                        default:
                            out_.write( c );
                    }
                }
            }
        }
        catch ( IOException e ) {
            throw wrapException( e );
        }
    }
}
