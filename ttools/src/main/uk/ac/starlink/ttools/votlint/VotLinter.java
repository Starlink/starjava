package uk.ac.starlink.ttools.votlint;

import java.util.ArrayList;
import java.util.List;
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
     * Constructs a linting parser.
     * Parsing a SAX stream with this object will perform the validation.
     * A user-supplied content handler may additionally be supplied;
     * if so, all SAX events will be fed to this handler as well as
     * to the linting handlers.
     *
     * @param  userHandler  content handler that receives all SAX events,
     *                      or null if only linting is required
     * @return  parser
     */
    public XMLReader createParser( ContentHandler userHandler )
            throws SAXException {
        boolean validate = context_.isValidating();

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
                context_.warning( new VotLintCode( "DTE" ),
                                  "Entity trouble - DTD validation may not be "
                                + "done properly (" + e + ")" );
            }
        }

        /* Prepare the content and error handlers for the parse.
         * We prepare a list of content handlers which will receive
         * the events, and then multiplex the SAX stream to them. */
        List<ContentHandler> contentHandlers = new ArrayList<ContentHandler>();

        /* The custom validation and error handling is done by
         * a VotLintContentHandler instance. */
        VotLintContentHandler lintHandler =
            new VotLintContentHandler( context_ );
        contentHandlers.add( lintHandler );

        /* If schema-validating, add a schema-validation content handler. */
        if ( validate && schema != null ) {
            ValidatorHandler validHandler = schema.newValidatorHandler();
            validHandler.setErrorHandler( lintHandler );
            ContentHandler fixValidHandler =
                new FudgeNamespaceContentHandler( validHandler, "VOTABLE",
                                                  version.getXmlNamespace() );
            contentHandlers.add( fixValidHandler );
        }

        /* If the user has supplied a content handler, add that one too. */
        if ( userHandler != null ) {
            contentHandlers.add( userHandler );
        }

        /* Construct a single content handler that will multiplex the
         * incoming SAX events to all the target handlers. */
        ContentHandler[] cHandlers =
            contentHandlers.toArray( new ContentHandler[ 0 ] );
        ContentHandler contentHandler =
            new MultiplexInvocationHandler<ContentHandler>( cHandlers )
           .createMultiplexer( ContentHandler.class );

        /* Install the custom content handler and error handler. */
        parser.setContentHandler( contentHandler );
        parser.setErrorHandler( lintHandler );

        /* Return the parser. */
        return parser;
    }
}
