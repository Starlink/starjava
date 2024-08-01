package uk.ac.starlink.ttools.copy;

import java.io.StringWriter;
import java.io.Writer;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * SAX content handler which mostly copies events to text,
 * but eliminates a given attribute from the root element.
 *
 * @author   Mark Taylor
 * @since    19 Nov 2012
 */
public class SquashAttributeHandler extends SAXWriter {

    private final Writer out_;
    private final String attName_;
    private final boolean removeEmptyElement_;
    private StringWriter buffer_;
    private boolean hasContent_;
    private int level_;

    /**
     * Constructor.
     *
     * @param  out  output stream; SAX events are copied as XML text to here
     * @param  attName  name of attribute on level-0 element to remove
     * @param  removeEmptyElement  if true, then if removing the named
     *            attribute from the level-0 element results in an element
     *            with no interesting content and no other attributes,
     *            the element itself is removed from the output
     */
    @SuppressWarnings("this-escape")
    public SquashAttributeHandler( Writer out, String attName,
                                   boolean removeEmptyElement ) {
        out_ = out;
        attName_ = attName;
        removeEmptyElement_ = removeEmptyElement;
        setOutput( out_ );
    }

    @Override
    public void startElement( String namespaceURI, String localName,
                              String qName, Attributes atts )
            throws SAXException {

        /* Only operate on the base-level element. */
        if ( level_++ == 0 ) {

            /* Remove the named attribute if present. */
            int iRemove = getAttIndex( atts, attName_ );
            if ( iRemove >= 0 ) {
                AttributesImpl a2 = new AttributesImpl( atts );
                a2.removeAttribute( iRemove );
                atts = a2;
            }

            /* Set up to cache all content of this element. */
            buffer_ = new StringWriter();
            setOutput( buffer_ );
            if ( atts.getLength() > 0 ) {
                hasContent_ = true;
            }
        }
        super.startElement( namespaceURI, localName, qName, atts );
    }

    @Override
    public void endElement( String namespaceURI, String localName,
                            String qName ) throws SAXException {
        super.endElement( namespaceURI, localName, qName );

        /* End of a level-0 element. */
        if ( --level_ == 0 ) {

            /* Restore normal processing. */
            setOutput( out_ );

            /* Either write the cached output, or, if there is no
             * interesting content and we've been asked to do so,
             * just ditch it. */
            if ( hasContent_ || ! removeEmptyElement_ ) {
                out( buffer_.toString() );
            }
        }
    }

    @Override
    public void characters( char[] ch, int start, int length )
            throws SAXException {
        super.characters( ch, start, length );
        hasContent_ = true;
    }

    @Override
    public void ignorableWhitespace( char[] ch, int start, int length )
            throws SAXException {
        super.ignorableWhitespace( ch, start, length );
    }

    @Override
    public void skippedEntity( String name ) throws SAXException {
        super.skippedEntity( name );
        hasContent_ = true;
    }

    @Override
    public void processingInstruction( String target, String data )
            throws SAXException {
        super.processingInstruction( target, data );
        hasContent_ = true;
    }

    @Override
    public void comment( char[] ch, int start, int length )
            throws SAXException {
         super.comment( ch, start, length );
    }

    @Override
    public void startCDATA() throws SAXException {
        super.startCDATA();
    }

    @Override
    public void endCDATA() throws SAXException {
        super.endCDATA();
    }

    /**
     * Return the index in the attribute list of the attribute with
     * a given name.
     *
     * @param  atts  attribute list
     * @param  attName  attribute to identify
     * @return  index of attribute with name <code>attName</code>,
     *          or -1 if not present
     */
    private static int getAttIndex( Attributes atts, String attName ) {
        for ( int i = 0; i < atts.getLength(); i++ ) {
            if ( attName.equals( atts.getLocalName( i ) ) ||
                 attName.equals( atts.getQName( i ) ) ) {
                return i;
            }
        }
        return -1;
    }
}
