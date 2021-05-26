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
import uk.ac.starlink.ttools.taplint.AdhocCode;
import uk.ac.starlink.votable.VOTableVersion;

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
    private final VersionDetail versionDetail_;
    private final HandlerStack stack_;
    private final Set<String> namespaceSet_;

    private static final Pattern NS_PAT = getNamespaceNamePattern();
    private final static Map<String,String> EMPTY_MAP =
        Collections.unmodifiableMap( new HashMap<String,String>() );


    /**
     * Constructor. 
     *
     * @param   context  context
     */
    public VotLintContentHandler( VotLintContext context ) {
        context_ = context;
        versionDetail_ = VersionDetail.getInstance( context );
        assert versionDetail_ != null;
        stack_ = new HandlerStack();
        namespaceSet_ = new HashSet<String>();
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
        context_.reportUnusedIds();
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
            versionDetail_.createElementHandler( localName, context_ );

        /* Push it on the stack. */
        stack_.push( handler );
        handler.setAncestry( stack_.getAncestry() );

        /* Tell it what attributes it has. */
        int natt = atts.getLength();
        if ( natt > 0 ) {
            Map<String,String> attMap = new HashMap<String,String>();
            for ( int i = 0; i < natt; i++ ) {
                attMap.put( atts.getQName( i ), atts.getValue( i ) );
            }
            handler.setAttributes( attMap );
        }
        else {
            handler.setAttributes( EMPTY_MAP );
        }

        /* Perform custom checking on each attribute as appropriate. */
        Map<String,AttributeChecker> attCheckers =
            versionDetail_.getAttributeCheckers( localName );
        for ( int i = 0; i < natt; i++ ) {
            AttributeChecker checker = attCheckers.get( atts.getQName( i ) );
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
            VOTableVersion contextVersion = context_.getVersion();

            /* Check for consistency with known namespace for the
             * VOTable version under consideration. */
            String versionNamespace = contextVersion.getXmlNamespace();
            if ( declaredNamespace == null ) {
                if ( versionNamespace != null ) {
                    context_.warning( new VotLintCode( "NS0" ),
                                      "Element not namespaced, "
                                    + "should be in " + versionNamespace );
                }
            }
            else if ( ! declaredNamespace.equals( versionNamespace ) ) {
                context_.warning( new VotLintCode( "NSX" ),
                                  "Element in wrong namespace ("
                                + declaredNamespace + " not "
                                + versionNamespace + ")" );
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
        context_.info( new VotLintCode( "PIY" ),
                       "Ignoring processing instruction <?" + target + 
                       " " + data + "?>" );
    }

    public void skippedEntity( String name ) {
        context_.info( new VotLintCode( "ENY" ), "Skipping entity " + name );
    }

    // ErrorHandler implementation.

    public void warning( SAXParseException e ) {
        String msg = e.getMessage();
        if ( msg == null ) {
            msg = e.toString();
        }
        context_.warning( createSaxCode( msg ), msg );
    }

    public void error( SAXParseException e ) {
        if ( ! isNamespaceError( e ) ) {
            String msg = e.getMessage();
            if ( msg == null ) {
                msg = e.toString();
            }
            context_.error( createSaxCode( msg ), msg );
        }
    }

    public void fatalError( SAXParseException e ) {
        String msg = e.getMessage();
        if ( msg == null ) {
            msg = e.toString();
        }
        context_.error( createSaxCode( msg ), msg );
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
     * Generates a VotLintCode corresponding to a given SAX error message.
     * Multiple calls with the same input message will yield the same
     * output code.
     *
     * @param  msg  message text
     * @return  code
     */
    private VotLintCode createSaxCode( String msg ) {
        return new VotLintCode( "J" + AdhocCode.createLabelChars( msg, 2 ) );
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
