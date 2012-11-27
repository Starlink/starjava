package uk.ac.starlink.ttools.votlint;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.ValidatorHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import uk.ac.starlink.util.MultiplexInvocationHandler;
import uk.ac.starlink.util.StarEntityResolver;
import uk.ac.starlink.votable.VOTableVersion;

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

        /* Prepare for validation.  It works completely differently
         * for DTD and XSD. */
        VOTableVersion version = context_.getVersion();
        boolean hasDTD = version.getDoctypeDeclaration() != null;
        Schema schema = version.getSchema();
        assert hasDTD ^ ( schema != null );

        /* Get a DTD-validating or non-DTD-validating parser. */
        XMLReader parser;
        try {
            SAXParserFactory spfact = SAXParserFactory.newInstance();
            spfact.setValidating( validate && hasDTD );
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
        if ( hasDTD ) {
            VotLintEntityResolver entityResolver =
                new VotLintEntityResolver( context_ );
            try {
                parser.setProperty( "http://xml.org/sax/properties/"
                                  + "lexical-handler", entityResolver );
                parser.setEntityResolver( entityResolver );
            }
            catch ( SAXException e ) {
                parser.setEntityResolver( StarEntityResolver.getInstance() );
                context_.warning( "Entity trouble - DTD validation may not be "
                                + "done properly (" + e + ")" );
            }
        }

        /* Prepare the content and error handlers for the parse. */
        VotLintContentHandler lintHandler =
            new VotLintContentHandler( context_ );
        final ContentHandler contentHandler;

        /* If schema-validating, multiplex the SAX events to two handlers:
         * the custom votlint handler itself, and a schema-validation one.
         * Otherwise, just send them to the votlint handler. */
        if ( validate && schema != null ) {
            ValidatorHandler validHandler = schema.newValidatorHandler();
            validHandler.setErrorHandler( lintHandler );
            ContentHandler fixValidHandler =
                new FudgeNamespaceContentHandler( validHandler, "VOTABLE",
                                                  version.getXmlNamespace() );
            contentHandler =
                new MultiplexInvocationHandler<ContentHandler>(
                        new ContentHandler[] { fixValidHandler, lintHandler } )
               .createMultiplexer( ContentHandler.class );
        }
        else {
            contentHandler = lintHandler;
        }

        /* Install the custom content handler and error handler. */
        parser.setContentHandler( contentHandler );
        parser.setErrorHandler( lintHandler );

        /* Return the parser. */
        return parser;
    }
}
