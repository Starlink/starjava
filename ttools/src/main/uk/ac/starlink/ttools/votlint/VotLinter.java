package uk.ac.starlink.ttools.votlint;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import uk.ac.starlink.util.StarEntityResolver;

/**
 * Main class for performing a VOTable lint.
 *
 * @author    Mark Taylor (Starlink)
 * @since     7 Apr 2005
 */
public class VotLinter {

    private final VotLintContext context_;

    /**
     * Constructor.
     * 
     * @param  context  lint context
     */
    public VotLinter( VotLintContext context ) {
        context_ = context;
    }

    /**
     * Constructs a linting parser.  Parsing a SAX stream with this
     * object will perform the lint.
     *
     * @param    validate  whether you want a validating parser
     * @return   parser
     */
    public XMLReader createParser( boolean validate ) throws SAXException {

        /* Get a validating or non-validating parser. */
        XMLReader parser;
        try {
            SAXParserFactory spfact = SAXParserFactory.newInstance();
            spfact.setValidating( validate );
            spfact.setNamespaceAware( true );
            parser = spfact.newSAXParser().getXMLReader();
        }
        catch ( ParserConfigurationException e ) {
            throw (SAXException) new SAXException( e.getMessage() )
                                .initCause( e );
        }

        /* Install a custom entity resolver.  This is also installed as
         * a lexical handler, to guarantee that whatever is named in the
         * DOCTYPE declaration is actually interpreted as the VOTable DTD. */
        VotLintEntityResolver entityResolver =
            new VotLintEntityResolver( context_ );
        try {
            parser.setProperty( "http://xml.org/sax/properties/lexical-handler",
                                entityResolver );
            parser.setEntityResolver( entityResolver );
        }
        catch ( SAXException e ) {
            parser.setEntityResolver( StarEntityResolver.getInstance() );
            context_.warning( "Entity trouble - DTD validation may not be " +
                              "done properly (" + e + ")" );
        }

        /* Install the custom content handler and error handler. */
        VotLintContentHandler lintHandler =
            new VotLintContentHandler( context_ );
        parser.setContentHandler( lintHandler );
        parser.setErrorHandler( lintHandler );

        /* Return the parser. */
        return parser;
    }
}
