package uk.ac.starlink.ttools.task;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
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
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.task.BooleanParameter;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Executable;
import uk.ac.starlink.task.ExecutionException;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.copy.VotCopyHandler;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.StarEntityResolver;
import uk.ac.starlink.votable.DataFormat;

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
    private final ChoiceParameter formatParam_;
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
        inParam_.setDescription( new String[] {
            "<p>Location of the input VOTable.",
            "May be a URL, filename, or \"-\" to indicate standard input.",
            "The input table may be compressed using one of the known",
            "compression formats (Unix compress, gzip or bzip2).",
            "</p>",
        } );

        outParam_ = new Parameter( "out" );
        outParam_.setPosition( 2 );
        outParam_.setPrompt( "Output votable" );
        outParam_.setUsage( "<location>" );
        outParam_.setDefault( "-" );
        outParam_.setDescription( new String[] {
            "<p>Location of the output VOTable.",
            "May be a filename or \"-\" to indicate standard output.",
            "</p>",
        } );

        formatParam_ = new ChoiceParameter( "format", new DataFormat[] {
            DataFormat.TABLEDATA, DataFormat.BINARY, DataFormat.FITS,
        } );
        formatParam_.setPosition( 3 );
        formatParam_.setPrompt( "Output votable format" );
        formatParam_.setNullPermitted( true );
        formatParam_.setDefault( DataFormat.TABLEDATA.toString()
                                                     .toLowerCase() );
        formatParam_.setPreferExplicit( true );
        formatParam_.setDescription( new String[] {
            "<p>Determines the encoding format of the table data in the ",
            "output document.",
            "If <code>null</code> is selected, then the tables will be",
            "data-less (will contain no DATA element), leaving only",
            "the document structure.",
            "Data-less tables are legal VOTable elements.",
            "</p>",
        } );

        xencParam_ = new XmlEncodingParameter( "charset" );

        cacheParam_ = new BooleanParameter( "cache" );
        cacheParam_.setPrompt( "Read data into cache before copying?" );
        cacheParam_.setDescription( new String[] {
            "<p>Determines whether the input tables are read into a cache",
            "prior to being written out.", 
            "The default is selected automatically depending on the input",
            "table; so you should normally leave this flag alone.",
            "</p>",
        } );

        hrefParam_ = new BooleanParameter( "href" );
        hrefParam_.setPrompt( "Output FITS/BINARY data external to " +
                              "output document?" );

        baseParam_ = new Parameter( "base" );
        baseParam_.setUsage( "<location>" );
        baseParam_.setPrompt( "Base location for FITS/BINARY href data" );
        baseParam_.setNullPermitted( true );

        hrefParam_.setDescription( new String[] {
            "<p>In the case of BINARY or FITS encoding, this determines",
            "whether the STREAM elements output will contain their data",
            "inline or externally.",
            "If set false, the output document will be self-contained,",
            "with STREAM data inline as base64-encoded characters.",
            "If true, then for each TABLE in the document the binary",
            "data will be written to a separate file and referenced",
            "by an href attribute on the corresponding STREAM element.",
            "The name of these files is usually determined by the name",
            "of the main output file; but see also the <code>",
            baseParam_.getName() + "</code> flag.",
            "</p>",
        } );
        baseParam_.setDescription( new String[] {
            "<p>Determines the name of external output files written when the",
            "<code>" + hrefParam_.getName() + "</code> flag is true.",
            "Normally these are given names based on the name of the",
            "output file.",
            "But if this flag is given, the names will be based on the",
            "<code>&lt;location&gt;</code> string.",
            "This flag is compulsory if",
            "<code>" + hrefParam_.getName() + "</code> is true",
            "and <code>out=-</code> (output is to standard out),",
            "since in this case there is no default base name to use.",
            "</p>",
        } );
    }

    public String getPurpose() {
        return "Transforms between VOTable encodings";
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

    public Executable createExecutable( Environment env ) throws TaskException {
        String inLoc = inParam_.stringValue( env );
        String outLoc = outParam_.stringValue( env );
        DataFormat format = (DataFormat) formatParam_.objectValue( env );
        cacheParam_.setDefault( format == DataFormat.FITS );
        PrintStream pstrm = env.getOutputStream();
        boolean inline;
        if ( format == DataFormat.TABLEDATA ||
             format == null ) {
            inline = true;
        }
        else {
            if ( format == DataFormat.BINARY ) {
                hrefParam_.setDefault( "false" );
            }
            else if ( format == DataFormat.FITS ) {
                hrefParam_.setDefault( "true" );
            }
            else {
                assert false;
            }
            inline = ! hrefParam_.booleanValue( env );
        }
        String base;
        if ( ! inline ) {
            baseParam_.setNullPermitted( false );
            if ( outLoc != null && ! outLoc.equals( "-" ) ) {
                baseParam_.setDefault( new File( outLoc ).getName()
                                      .replaceFirst( "\\.[a-zA-Z0-9]*$", "" ) );
            }
            base = baseParam_.stringValue( env );
        }
        else {
            base = null;
        }
        Charset xenc = xencParam_.charsetValue( env );
        boolean strict = LineTableEnvironment.isStrictVotable( env );
        boolean cache = cacheParam_.booleanValue( env );
        StoragePolicy policy = LineTableEnvironment.getStoragePolicy( env );
        return new VotCopier( inLoc, outLoc, pstrm, xenc, inline, base,
                              format, strict, cache, policy );
    }

    /**
     * Helper class which does the work of the VOTable copy.
     */
    private class VotCopier implements Executable {

        final String inLoc_;
        final String outLoc_;
        final PrintStream pstrm_;
        final Charset xenc_;
        final boolean inline_;
        final String base_;
        final DataFormat format_;
        final boolean strict_;
        final boolean cache_;
        final StoragePolicy policy_;

        VotCopier( String inLoc, String outLoc, PrintStream pstrm, 
                   Charset xenc, boolean inline, String base,
                   DataFormat format, boolean strict, boolean cache,
                   StoragePolicy policy ) {
            inLoc_ = inLoc;
            outLoc_ = outLoc;
            pstrm_ = pstrm;
            xenc_ = xenc;
            inline_ = inline;
            base_ = base;
            format_ = format;
            strict_ = strict;
            cache_ = cache;
            policy_ = policy;
        }

        public void execute() throws IOException, ExecutionException {
            InputStream in = null;
            Writer out = null;
            try {

                /* Get input stream. */
                String systemId = inLoc_.equals( "-" ) ? "." : inLoc_;
                in = DataSource.getInputStream( inLoc_ );
                in = new BufferedInputStream( in );

                /* Get output stream */
                OutputStream ostrm = outLoc_.equals( "-" )
                           ? (OutputStream) pstrm_
                           : (OutputStream) new FileOutputStream( outLoc_ );
                out = xenc_ == null ? new OutputStreamWriter( ostrm )
                                    : new OutputStreamWriter( ostrm, xenc_ );

                /* Construct a handler which can take SAX and SAX-like
                 * events and turn them into XML output. */
                VotCopyHandler handler =
                    new VotCopyHandler( strict_, format_, inline_,
                                        base_, cache_, policy_ );
                handler.setOutput( out );

                /* Output the XML declaration. */
                out.write( "<?xml version=\"1.0\"" );
                if ( xenc_ != null ) {
                    out.write( " encoding=\"" + xenc_ + "\"" );
                }
                out.write( "?>\n" );

                /* Prepare a stream of SAX events. */
                InputSource saxsrc = new InputSource( in );
                saxsrc.setSystemId( systemId );

                /* Process the stream to perform the copy. */
                saxCopy( saxsrc, handler );
                out.flush();
            }
            catch ( SAXException e ) {
                throw new ExecutionException( e.getMessage(), e );
            }
            finally {
                if ( in != null ) {
                    in.close();
                }
                if ( out != null ) {
                    out.close();
                }
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
