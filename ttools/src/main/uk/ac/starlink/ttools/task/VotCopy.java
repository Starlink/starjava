package uk.ac.starlink.ttools.task;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;
import uk.ac.starlink.task.BooleanParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.ExecutionException;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.copy.VotCopyHandler;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.StarEntityResolver;

/**
 * Task which Copies a VOTable XML document intact but with control over the
 * DATA encoding type.
 *
 * @author   Mark Taylor (Starlink)
 * @since    19 Apr 2005
 */
public class VotCopy implements Task {

    private static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.task" );
    private static final String SAX_PROPERTY = "http://xml.org/sax/properties/";

    private final Parameter inParam_;
    private final Parameter outParam_;
    private final VotFormatParameter formatParam_;
    private final XmlEncodingParameter xencParam_;
    private final BooleanParameter cacheParam_;
    private final BooleanParameter hrefParam_;
    private final Parameter baseParam_;

    /**
     * Constructor.
     */
    public VotCopy() {
        inParam_ = new Parameter( "in" );
        inParam_.setPosition( 1 );
        inParam_.setPrompt( "Input votable" );
        inParam_.setUsage( "<location>" );
        inParam_.setDefault( "-" );

        outParam_ = new Parameter( "out" );
        outParam_.setPosition( 2 );
        outParam_.setPrompt( "Output votable" );
        outParam_.setUsage( "<location>" );
        outParam_.setDefault( "-" );

        formatParam_ = new VotFormatParameter( "format" );
        formatParam_.setPosition( 3 );
        formatParam_.setPrompt( "Output votable format" );

        xencParam_ = new XmlEncodingParameter( "charset" );

        cacheParam_ = new BooleanParameter( "cache" );
        cacheParam_.setPrompt( "Read data into cache before copying?" );

        hrefParam_ = new BooleanParameter( "href" );
        hrefParam_.setPrompt( "Output FITS/BINARY data external to " +
                              "output document?" );

        baseParam_ = new Parameter( "base" );
        baseParam_.setPrompt( "Base location for FITS/BINARY href data" );
        baseParam_.setNullPermitted( true );
    }

    public Parameter[] getParameters() {
        return new Parameter[] {
            inParam_,
            outParam_,
            formatParam_,
            xencParam_,
            cacheParam_,
            hrefParam_,
            baseParam_,
        };
    }

    public String getUsage() {
        return null;
    }

    public void invoke( Environment env ) throws TaskException {
        try {
            doInvoke( env );
        }
        catch ( IOException e ) {
            throw new ExecutionException( e.getMessage(), e );
        }
        catch ( SAXException e ) {
            throw new ExecutionException( e.getMessage(), e );
        }
    }

    private void doInvoke( Environment env )
            throws TaskException, IOException, SAXException {
        InputStream in = null;
        Writer out = null;
        try {

            /* Get input stream. */
            String inLoc = inParam_.stringValue( env );
            String systemId = inLoc.equals( "-" ) ? "." : inLoc;
            in = DataSource.getInputStream( inLoc );
            in = new BufferedInputStream( in );

            /* Get output stream */
            String outLoc = outParam_.stringValue( env );
            OutputStream ostrm = outLoc.equals( "-" )
                               ? (OutputStream) env.getPrintStream()
                               : (OutputStream) new FileOutputStream( outLoc );
            Charset xenc = xencParam_.charsetValue( env );
            out = xenc == null ? new OutputStreamWriter( ostrm )
                               : new OutputStreamWriter( ostrm, xenc );

            /* Work out the output characteristics. */
            boolean inline = ! hrefParam_.booleanValue( env );
            String base;
            if ( ! inline ) {
                baseParam_.setNullPermitted( false );
                base = baseParam_.stringValue( env );
            }
            else {
                base = null;
            }

            /* Construct a handler which can take SAX and SAX-like events and
             * turn them into XML output. */
            VotCopyHandler handler =
                new VotCopyHandler( TableEnvironment.isStrictVotable( env ),
                                    formatParam_.formatValue( env ), inline,
                                    base, cacheParam_.booleanValue( env ),
                                    TableEnvironment.getStoragePolicy( env ) );
            handler.setOutput( out );

            /* Output the XML declaration. */
            out.write( "<?xml version=\"1.0\"" );
            if ( xenc != null ) {
                out.write( " encoding=\"" + xenc + "\"" );
            }
            out.write( "?>\n" );

            /* Prepare a stream of SAX events. */
            InputSource saxsrc = new InputSource( in );
            saxsrc.setSystemId( systemId );

            /* Process the stream to perform the copy. */
            saxCopy( saxsrc, handler );
            out.flush();
        }
        finally {
            try {
                if ( in != null ) {
                    in.close();
                }
                if ( out != null ) {
                    out.close();
                }
            }
            catch ( IOException e ) {
                logger_.log( Level.WARNING, "Close error", e );
            }
        }
    }

    /**
     * Copies the SAX stream to the output, writing TABLE DATA elements
     * in a given encoding.
     *
     * @param  saxSrc       SAX input source
     * @param  copyHandler  handler which can consume SAX events - may be
     *                      a LexicalHandler too
     */
    public static void saxCopy( InputSource saxSrc, VotCopyHandler copyHandler )
            throws SAXException, IOException {

        /* Create a suitable parser. */
        XMLReader parser = createParser();

        /* Install the copying content handler. */
        parser.setContentHandler( copyHandler );

        /* Try to set the lexical handler.  If this fails you just lose some
         * lexical details such as comments and CDATA marked sections. */
        try {
            parser.setProperty( SAX_PROPERTY + "lexical-handler",
                                (LexicalHandler) copyHandler );
        }
        catch ( SAXException e ) {
            logger_.info( "Lexical handler not set: " + e );
        }

        /* Do the parse. */
        parser.parse( saxSrc );
    }

    /**
     * Constructs a SAX parser with suitable characteristics for copying
     * SAX events.
     *
     * @return   new parser
     */
    private static XMLReader createParser() throws SAXException {

        /* Create a SAX parser. */
        XMLReader parser;
        try {
            SAXParserFactory spfact = SAXParserFactory.newInstance();
            spfact.setValidating( false );
            spfact.setNamespaceAware( true );
            trySetFeature( spfact, "namespace-prefixes", true );
            trySetFeature( spfact, "external-general-entities", false );
            parser = spfact.newSAXParser().getXMLReader();
        }
        catch ( ParserConfigurationException e ) {
            throw (SAXException) new SAXException( e.getMessage() )
                                .initCause( e );
        }

        /* Install a custom entity resolver. */
        parser.setEntityResolver( StarEntityResolver.getInstance() );

        /* Install an error handler. */
        parser.setErrorHandler( new ErrorHandler() {
            public void error( SAXParseException e ) {
                logger_.warning( e.toString() );
            }
            public void warning( SAXParseException e ) {
                logger_.warning( e.toString() );
            }
            public void fatalError( SAXParseException e ) throws SAXException {
                throw e;
            }
        } );

        /* Return. */
        return parser;
    }

    /**
     * Attempts to set a feature on a SAXParserFactory, but doesn't panic
     * if it can't.
     *
     * @param  spfact   factory
     * @param  feature  feature name <em>excluding</em> the 
     *                  "http://xml.org/sax/features/" part
     */
    private static boolean trySetFeature( SAXParserFactory spfact,
                                          String feature, boolean value ) {
        try {
            spfact.setFeature( "http://xml.org/sax/features/" + feature, 
                               value );
            return true;
        }
        catch ( ParserConfigurationException e ) {
            return false;
        }
        catch ( SAXException e ) {
            return false;
        }
    }

}
