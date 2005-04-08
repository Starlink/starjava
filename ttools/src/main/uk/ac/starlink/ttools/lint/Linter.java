package uk.ac.starlink.ttools.lint;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.StarEntityResolver;

/**
 * Top-level class which performs a VOTable lint.
 *
 * @author    Mark Taylor (Starlink)
 * @since     7 Apr 2005
 */
public class Linter {

    private final LintContext context_;

    /**
     * Main method.
     */
    public static void main( String[] args ) throws IOException, SAXException {
        String usage = "votlint [-debug] [votable]";

        List argList = new ArrayList( Arrays.asList( args ) );
        boolean debug = false;
        String systemId = null;
        for ( Iterator it = argList.iterator(); it.hasNext(); ) {
            String arg = (String) it.next();
            if ( arg.equals( "-debug" ) ) {
                it.remove();
                debug = true;
            }
            else if ( arg.startsWith( "-h" ) ) {
                System.err.println( usage );
                System.exit( 1 );
            }
            else if ( systemId == null ) {
                it.remove();
                systemId = arg;
            }
        }
        if ( ! argList.isEmpty() ) {
            System.err.println( usage );
            System.exit( 1 );
        }

        final LintContext context = new LintContext();
        context.setDebug( debug );

        /* Get the input stream. */
        InputStream in;
        if ( systemId == null ) {
            in = System.in;
        }
        else {
            try {
                in = DataSource.getInputStream( systemId );
            }
            catch ( FileNotFoundException e ) {
                System.err.println( "No such file " + systemId );
                System.exit( 1 );
                throw new AssertionError();
            }
        }

        /* Interpolate the VOTable DOCTYPE declaration if necessary. */
        DoctypeInterpolator interp = new DoctypeInterpolator() {
            public void message( String msg ) {
                context.info( msg );
            }
        };
        in = interp.getStreamWithDoctype( new BufferedInputStream( in ) );
        String vers = interp.getVotableVersion();
        if ( vers != null ) {
            if ( context.getVersion() == null ) {
                context.setVersion( vers );
            }
        }

        /* Turn it into a SAX source. */
        InputSource sax = new InputSource( in );
        sax.setSystemId( systemId );

        /* Perform the parse. */
        new Linter( context ).createParser().parse( sax );
        System.exit( 0 );
    }

    /**
     * Constructor.
     * 
     * @param  context  lint context
     */
    public Linter( LintContext context ) {
        context_ = context;
    }

    /**
     * Constructs a linting parser.  Parsing a SAX stream with this
     * object will perform the lint.
     *
     * @return   parser
     */
    public XMLReader createParser() throws SAXException {

        /* Get a validating parser. */
        XMLReader parser;
        try {
            SAXParserFactory spfact = SAXParserFactory.newInstance();
            spfact.setValidating( true );
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
        LintEntityResolver entityResolver = new LintEntityResolver( context_ );
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

        /* Install an error handler. */
        parser.setErrorHandler( new ErrorHandler() {
            public void warning( SAXParseException e ) {
                context_.error( e.getMessage() );
            }
            public void error( SAXParseException e ) {
                context_.error( e.getMessage() );
            }
            public void fatalError( SAXParseException e ) {
                context_.error( "FATAL: " + e.getMessage() );
            }
        } );

        /* Install a linting content handler. */
        parser.setContentHandler( new LintContentHandler( context_ ) );

        /* Return the parser. */
        return parser;
    }

}
