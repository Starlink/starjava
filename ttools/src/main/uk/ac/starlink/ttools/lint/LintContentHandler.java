package uk.ac.starlink.ttools.lint;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;

/**
 * SAX ContentHandler used for linting VOTables.
 * Its main job is to push and pop ElementHandlers onto a stack as elements
 * go in and out of scope.  It is the ElementHandler objects which do the
 * real element-specific work.
 *
 * @author   Mark Taylor (Starlink)
 * @since    7 Apr 2005
 */
public class LintContentHandler implements ContentHandler {

    private final LintContext context_;
    private final HandlerStack stack_;

    private final static Map EMPTY_MAP =
        Collections.unmodifiableMap( new HashMap() );

    /**
     * Constructor. 
     *
     * @param   context  context
     */
    LintContentHandler( LintContext context ) {
        context_ = context;
        stack_ = new HandlerStack();
    }

    //
    // SAX ContentHandler implementation.
    //

    public void setDocumentLocator( Locator locator ) {
        context_.setLocator( locator );
    }

    public void startDocument() {
    }

    public void endDocument() {
        context_.reportUncheckedRefs();
    }

    public void startPrefixMapping( String prefix, String uri ) {
    }

    public void endPrefixMapping( String prefix ) {
    }

    public void startElement( String namespaceURI, String localName, 
                              String qName, Attributes atts ) {

        /* Create a new handler for this element. */
        ElementHandler handler = context_.createHandler( localName );

        /* Push it on the stack. */
        stack_.push( handler );
        handler.setAncestry( stack_.getAncestry() );

        /* Tell it what attributes it has. */
        int natt = atts.getLength();
        if ( natt > 0 ) {
            Map attMap = new HashMap();
            for ( int i = 0; i < natt; i++ ) {
                attMap.put( atts.getQName( i ), atts.getValue( i ) );
            }
            handler.setAttributes( attMap );
        }
        else {
            handler.setAttributes( EMPTY_MAP );
        }

        /* Perform custom checking on each attribute as appropriate. */
        Map attCheckers = context_.getAttributeCheckers( localName );
        for ( int i = 0; i < natt; i++ ) {
            AttributeChecker checker =
                (AttributeChecker) attCheckers.get( atts.getQName( i ) );
            if ( checker != null ) {
                checker.check( atts.getValue( i ), handler );
            }
        }

        /* Give the handler a chance to perform element-specific actions. */
        handler.startElement();
    }

    public void endElement( String namespaceURI, String localName,
                            String qName ) {
        ElementHandler handler = stack_.top();

        /* Give the handler a chance to perform element-specific actions. */
        handler.endElement();

        /* Remove it from the stack. */
        handler.setAncestry( null );
        stack_.pop();
    }

    public void characters( char[] ch, int start, int length ) {

        /* Allow the handler to process these character data. */
        stack_.top().characters( ch, start, length );
    }

    public void ignorableWhitespace( char[] ch, int start, int length ) {
    }

    public void processingInstruction( String target, String data ) {
        context_.info( "Ignoring processing instruction <?" + target + 
                       " " + data + "?>" );
    }

    public void skippedEntity( String name ) {
        context_.info( "Skipping entity " + name );
    }

}
