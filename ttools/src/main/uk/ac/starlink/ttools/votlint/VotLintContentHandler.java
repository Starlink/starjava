package uk.ac.starlink.ttools.votlint;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXParseException;

/**
 * SAX ContentHandler used for linting VOTables.
 * Its main job is to push and pop ElementHandlers onto a stack as elements
 * go in and out of scope.  It is the ElementHandler objects which do the
 * real element-specific work.
 *
 * @author   Mark Taylor (Starlink)
 * @since    7 Apr 2005
 */
public class VotLintContentHandler implements ContentHandler, ErrorHandler {

    private final VotLintContext context_;
    private final HandlerStack stack_;
    private final Set namespaceSet_;

    private static final Pattern NS_PAT = getNamespaceNamePattern();
    private final static Map EMPTY_MAP =
        Collections.unmodifiableMap( new HashMap() );


    /**
     * Constructor. 
     *
     * @param   context  context
     */
    public VotLintContentHandler( VotLintContext context ) {
        context_ = context;
        stack_ = new HandlerStack();
        namespaceSet_ = new HashSet();
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
        context_.getNamespaceMap().put( prefix, uri );
    }

    public void endPrefixMapping( String prefix ) {
        context_.getNamespaceMap().remove( prefix );
    }

    public void startElement( String namespaceURI, String localName, 
                              String qName, Attributes atts ) {

        /* Create a new handler for this element. */
        ElementHandler handler =
            context_.getEffectiveVersion()
                    .createElementHandler( localName, context_ );

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
        Map attCheckers =
            context_.getEffectiveVersion().getAttributeCheckers( localName );
        for ( int i = 0; i < natt; i++ ) {
            AttributeChecker checker =
                (AttributeChecker) attCheckers.get( atts.getQName( i ) );
            if ( checker != null ) {
                checker.check( atts.getValue( i ), handler );
            }
        }

        /* Check XML namespace for element. */
        String declaredNamespace = namespaceURI.trim().length() == 0
                                 ? null
                                 : namespaceURI;
        if ( ! namespaceSet_.contains( declaredNamespace ) ) {
            namespaceSet_.add( declaredNamespace );
            VotableVersion contextVersion = context_.getVersion();

            /* If the version has been declared, check for consistency. */
            if ( contextVersion != null ) {
                String versionNamespace = contextVersion.getNamespaceUri();
                if ( declaredNamespace == null ) {
                    if ( versionNamespace != null ) {
                        context_.warning( "Element not namespaced, "
                                        + "should be in " + versionNamespace );
                    }
                }
                else if ( ! declaredNamespace.equals( versionNamespace ) ) {
                    context_.warning( "Element in wrong namespace ("
                                    + declaredNamespace + " not "
                                    + versionNamespace + ")" );
                }
            }

            /* Otherwise, try to work out the correct version from the
             * namespace. */
            else {
                if ( declaredNamespace == null ) {
                    // strictly speaking, this should imply VOTable 1.0,
                    // but in practice it's more likely that the namespacing
                    // has just been omitted.  Ignore.
                }
                else {
                    VotableVersion nsVersion =
                        VotableVersion
                       .getVersionByNamespace( declaredNamespace );
                    if ( nsVersion == null ) {
                        context_.warning( "Element in unknown namespace "
                                        + declaredNamespace );
                    }
                    else if ( nsVersion != contextVersion ) {
                        context_.info( "Inferring VOTable version " + nsVersion
                                     + " from namespace " + declaredNamespace );
                        context_.setVersion( nsVersion );
                    }
                }
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

    // ErrorHandler implementation.

    public void warning( SAXParseException e ) {
        context_.message( "WARN", null, e );
    }

    public void error( SAXParseException e ) {
        if ( ! isNamespaceError( e ) ) {
            context_.message( "ERROR", null, e );
        }
    }

    public void fatalError( SAXParseException e ) {
        context_.message( "FATAL", null, e );
    }

    /**
     * This method checks if an error message looks like it relates to a
     * namespace-qualified element or attribute and if it does returns true.
     * This is required since many common validation errors can result from
     * a basically healthy VOTable document (e.g. xsi:schemaLocation=...).
     * Necessarily, the checking done here is very ad hoc and not very
     * satisfactory.
     *
     * @param   e  error
     * @return  true iff <tt>e</tt> looks like a namespace validation error
     *          (and hence should not be reported to the user)
     */
    private boolean isNamespaceError( SAXParseException e ) {
        String msg = e.getMessage();
        if ( msg != null ) {
            if ( msg.indexOf( "xmlns" ) >= 0 ) {
                return true;
            }
            Matcher matcher = NS_PAT.matcher( msg );
            if ( matcher.lookingAt() ) {
                String prefix = matcher.group( 1 );
                String localName = matcher.group( 2 );
                if ( prefix.toLowerCase().startsWith( "xml" ) ) {
                    return true;
                }
                else if ( context_.getNamespaceMap().containsKey( prefix ) ) {
                    return true;
                }
                else {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns a pattern which matches a namespace-qualified XML name.
     * It contains two groups, the first giving the prefix and the second
     * giving the localName.
     *
     * @param  XML name pattern
     */
    private static Pattern getNamespaceNamePattern() {
        String charPat = "\\p{Lu}\\p{Ll}_";
        String startCharPat = charPat + "0-9\\-\\.";
        String pats = ".*?"
                    + "([" + startCharPat + "]"
                    + "[" + charPat + "]*"
                    + "):("
                    + "[" + charPat + "]+"
                    + ")";
        return Pattern.compile( pats );
    }

}
