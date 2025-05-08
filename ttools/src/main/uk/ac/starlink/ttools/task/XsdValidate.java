package uk.ac.starlink.ttools.task;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;
import uk.ac.starlink.task.BooleanParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Executable;
import uk.ac.starlink.task.ExecutionException;
import uk.ac.starlink.task.InputStreamParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.StringParameter;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.taplint.IvoaSchemaResolver;
import uk.ac.starlink.ttools.task.StringMultiParameter;
import uk.ac.starlink.util.URLUtils;

/**
 * Utility task for XML Schema validation.
 * Of course the hard work is done by classes in javax.xml.validation,
 * but this task handles reporting and, especially, provides some facilities
 * for custom schema file location.
 *
 * <p>This doesn't seem like it should be a job for STILTS, but it seems
 * there aren't too many other easily accessible XSD validation tools
 * out there, and this can at least offer local copies of some of the
 * IVOA schemas.
 *
 * @author   Mark Taylor
 * @since    15 Mar 2022
 */
public class XsdValidate implements Task {

    private final Parameter<InputStream> docParam_;
    private final Parameter<String[]> schemalocsParam_;
    private final Parameter<String> topelParam_;
    private final BooleanParameter verboseParam_;
    private final BooleanParameter usevolocalsParam_;
    private final BooleanParameter nsurldfltParam_;

    public XsdValidate() {
        docParam_ = new InputStreamParameter( "doc" );
        docParam_.setPosition( 1 );
        docParam_.setPrompt( "Document to validate" );
        docParam_.setDescription( new String[] {
            "<p>Location of XML document to validate.",
            "</p>",
        } );

        schemalocsParam_ = new StringMultiParameter( "schemaloc", ' ' );
        schemalocsParam_.setUsage( "<namespace>=<location> ..." );
        schemalocsParam_.setPrompt( "Schema location overrides" );
        schemalocsParam_.setDescription( new String[] {
            "<p>Assignments of override schema locations to XML namespaces.",
            "One or more assignments may be supplied, each of the form",
            "<code>&lt;namespace&gt;=&lt;location&gt;</code>",
            "where the location may be a filename or URL.",
            "Multiple assignments may be made by supplying the parameter",
            "multiple times, or using a space character as a separator.",
            "</p>",
            "<p>Each assignment causes any reference to the given namespace",
            "in the validated document to be validated with reference to",
            "the XSD schema at the given location rather than to",
            "a schema acquired in the default way",
            "(using <code>xsi:schemaLocation</code> attributes",
            "or using the namespace as a retrieval URL).",
            "</p>",
        } );
        schemalocsParam_.setNullPermitted( true );

        verboseParam_ = new BooleanParameter( "verbose" );
        verboseParam_.setPrompt( "Display informative comments?" );
        verboseParam_.setDescription( new String[] {
            "<p>If true, some INFO reports will be displayed alongside",
            "any ERROR reports resulting from the parse.",
            "This may be useful for diagnosis or reassurance.",
            "</p>",
        } );
        verboseParam_.setBooleanDefault( false );

        usevolocalsParam_ = new BooleanParameter( "uselocals" );
        usevolocalsParam_.setPrompt( "Use local VO schema copies?" );
        usevolocalsParam_.setDescription( new String[] {
            "<p>Whether to use local copies of VO schemas where available.",
            "If true, copies of some IVOA schemas stored within the",
            "application are used instead of retrieving them from",
            "their http://www.ivoa.net/ URLs.",
            "Setting this true is generally faster and more robust",
            "against network issues, though it may risk retrieving",
            "out of date copies of the schemas.",
            "</p>",
        } );
        usevolocalsParam_.setBooleanDefault( false );

        nsurldfltParam_ = new BooleanParameter( "nsurl" );
        nsurldfltParam_.setPrompt( "Use namespace as URL by default?" );
        nsurldfltParam_.setDescription( new String[] {
            "<p>Whether to use the namespace URI itself as a",
            "dereferencable URL to download a schema",
            "if no location has been supplied from elsewhere.",
            "For a namespace <code>http://example.com/ns1</code>",
            "this is like assuming the presence of a",
            "<code>xsi:schemaLocation="
                + "\"http://example.com/ns1 http://example.com/ns1\"</code>",
            "entry.",
            "Of course the XSD must be available at the location given",
            "by the namespace URI for this to work.",
            "</p>",
            "<p>Setting this true usually does the right thing,",
            "but it may risk hitting schema-hosting web servers too hard,",
            "and maybe it's too lenient on XML documents that don't have",
            "the right <code>xsi:schemaLocation</code> attributes;",
            "but <webref url='https://www.w3.org/TR/xmlschema-1/#schema-loc'",
            ">XMLSchema-1 Sec 4.3.2</webref>",
            "says it's up to the processor (i.e. this command)",
            "to figure out where to get schemas from.",
            "</p>",
        } );
        nsurldfltParam_.setBooleanDefault( true );

        topelParam_ = new StringParameter( "topel" );
        topelParam_.setUsage( "[{<ns-uri>}][<local-name>]" );
        topelParam_.setPrompt( "Local name of required top-level element" );
        topelParam_.setDescription( new String[] {
            "<p>Local name of the top-level element expected",
            "in the parsed document.",
            "If the actual parsed top-level element has a local name",
            "differing from this, an error will be reported.",
            "If no value is specified (the default) no checking is done.",
            "</p>",
        } );
        topelParam_.setNullPermitted( true );
    }

    public String getPurpose() {
        return "Validates against XML Schema";
    }

    public Parameter<?>[] getParameters() {
        return new Parameter<?>[] {
            docParam_,
            schemalocsParam_,
            topelParam_,
            verboseParam_,
            usevolocalsParam_,
            nsurldfltParam_,
        };
    }

    public Executable createExecutable( Environment env ) throws TaskException {

        /* Acquire parameter values. */
        final InputStream in = docParam_.objectValue( env );
        String topel = topelParam_.stringValue( env );
        final QName topQName;
        if ( topel == null || topel.trim().length() == 0 ) {
            topQName = null;
        }
        else {
            try {
                topQName = QName.valueOf( topel );
            }
            catch ( RuntimeException e ) {
                throw new ParameterValueException( topelParam_,
                                                   "Not in QName format "
                                                 + "local{uri}" );
            }
        }
        String[] schemalocs = schemalocsParam_.objectValue( env );
        boolean isVerbose = verboseParam_.booleanValue( env );
        boolean useVoLocals = usevolocalsParam_.booleanValue( env );
        boolean nsUrlDflt = nsurldfltParam_.booleanValue( env );

        /* Prepare reporting object. */
        final Recorder recorder =
            new Recorder( isVerbose, env.getOutputStream() );

        /* Prepare schema resolution.
         * You might think that installing a custom org.xml.sax.EntityResolver
         * on the SAX parser would be a more straightforward way to do this,
         * than providing a custom org.w3c.dom.ls.LSResourceResolver,
         * but I haven't had much luck with it.  There is more discussion
         * in the comments in uk.ac.starlink.ttools.taplint.XsdValidation. */
        final Map<String,URL> customSchemaMap = new LinkedHashMap<>();
        Pattern kvRegex = Pattern.compile( "([^=]+)=(.+)" );
        if ( schemalocs != null ) {
            for ( String schemaloc : schemalocs ) {
                Matcher matcher = kvRegex.matcher( schemaloc );
                if ( matcher.matches() ) {
                    String ns = matcher.group( 1 );
                    URL url = URLUtils.makeURL( matcher.group( 2 ) );
                    if ( url == null ) {
                        throw new ParameterValueException(
                                schemalocsParam_,
                                "Can't interpret as file or URL \""
                               + matcher.group( 2 ) + "\"" );
                    }
                    customSchemaMap.put( ns, url );
                }
                else {
                    throw new ParameterValueException(
                            schemalocsParam_,
                            "Not of form " + "<namespace>=<location>: \"" 
                                           + schemaloc + "\"" );
                }
            }
        }
        final Map<String,URL> localSchemaMap = new LinkedHashMap<>();
        localSchemaMap.putAll( IvoaSchemaResolver.W3C_SCHEMA_MAP );
        if ( useVoLocals ) {
            localSchemaMap.putAll( IvoaSchemaResolver.IVOA_SCHEMA_MAP );
        }
        final Map<String,URL> schemaMap = new LinkedHashMap<>();
        schemaMap.putAll( localSchemaMap );
        schemaMap.putAll( customSchemaMap );
        IvoaSchemaResolver resolver =
            new IvoaSchemaResolver( schemaMap, nsUrlDflt );

        /* Prepare validator.
         * Schemas are taken only from their declarations in the validated
         * document, not supplied to the validator.  It seems this is the
         * only way to supply custom schema resolution of some
         * (not necessarily all) schemas referenced. */
        final Validator val;
        try {
            val = SchemaFactory
                 .newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI )
                 .newSchema()
                 .newValidator();
        }
        catch ( SAXException e ) {
            throw new ExecutionException( "Can't prepare validator", e );
        }
        ValidateErrorHandler errorHandler = new ValidateErrorHandler( recorder);
        ValidateContentHandler contentHandler = new ValidateContentHandler();
        val.setResourceResolver( resolver );
        val.setErrorHandler( errorHandler );

        /* Return executable. */
        return () -> {

            /* Perform validation. */
            try {
                val.validate( new SAXSource(
                                  new InputSource(
                                      new BufferedInputStream( in ) ) ),
                              new SAXResult( contentHandler ) );
            }
    
            /* IO error during parsing. */
            catch ( IOException e ) {
                recorder.error( "Error reading input document: " + e );
            }

            /* The validator should only throw a SAX exception in case of a
             * fatal parse error, which ought already to have been recorded. */
            catch ( SAXException e ) {
                if ( errorHandler.fatalCount_ == 0 ) {
                    recorder.error( "Unexpected document parse error: " + e );
                }
            }

            /* Report on schema namespaces used. */
            for ( String ns : resolver.getResolvedNamespaces() ) {
                if ( customSchemaMap.containsKey( ns ) ) {
                    recorder.info( "Custom schema location: " + ns );
                }
                else if ( localSchemaMap.containsKey( ns ) ) {
                    recorder.info( "Local schema location: " + ns );
                }
                else if ( nsUrlDflt ) {
                    recorder.info( "Schema location from namespace: " + ns );
                }
                else {
                    assert false : ns;
                }
            }
            for ( String ns : resolver.getUnresolvedNamespaces() ) {
                recorder.info( "Default schema location: " + ns );
            }
            Collection<String> unusedCustoms =
                new LinkedHashSet<>( customSchemaMap.keySet() );
            unusedCustoms.removeAll( resolver.getResolvedNamespaces() );
            for ( String ns : unusedCustoms ) {
                recorder.warning( "Unused custom schema: " + ns );
            }

            /* Report on top-level element. */
            recorder.info( "Top-level element: "
                         + new QName( contentHandler.topUri_,
                                      contentHandler.topLocal_ ) );
            if ( topQName != null ) {
                String reqLocal = topQName.getLocalPart();
                String reqUri = topQName.getNamespaceURI();
                if ( reqLocal != null && reqLocal.trim().length() > 0 &&
                     !reqLocal.equals( contentHandler.topLocal_ ) ) {
                    recorder.error( "Wrong top-level element local name ("
                                  + contentHandler.topLocal_ + " != " + reqLocal
                                  + ")" );
                }
                else if ( reqUri != null && reqUri.trim().length() > 0 &&
                          !reqUri.equals( contentHandler.topUri_ ) ) {
                    recorder.error( "Wrong top-level element URI ("
                                  + contentHandler.topUri_ + " != " + reqUri
                                  + ")" );
                }
            }

            /* Assess validity and return accordingly. */
            boolean success = recorder.errorCount_ == 0;
            recorder.info( success ? "Validation OK" : "Validation failed" );
            if ( ! success ) {
                throw new ExecutionException( "Validation failed" );
            }
        };
    }

    /**
     * Handles status reporting.
     */
    private static class Recorder {
        private final boolean reportInfo_;
        private final PrintStream out_;
        private final MessageCounter counter_;
        int errorCount_;

        /**
         * Constructor.
         *
         * @param  reportInfo  if true, non-error messages are output
         * @param  out  destination stream
         */
        Recorder( boolean reportInfo, PrintStream out ) {
            out_ = out;
            reportInfo_ = reportInfo;
            counter_ = new MessageCounter( 1 );
        }

        /**
         * Records a validation error.
         * The text is reported to the user,
         * and the number of such errors is recorded.
         *
         * @param  msg  message
         */
        public void error( String msg ) {
            errorCount_++;
            record( "ERROR: " + msg );
        }

        /**
         * Records a warning.
         * The text is reported to the user.
         *
         * @param  msg  message
         */
        public void warning( String msg ) {
            record( "WARNING: " + msg );
        }

        /**
         * Records an informative message.
         * This may be reported to the user depending on configuration.
         *
         * @param  msg  message
         */
        public void info( String msg ) {
            if ( reportInfo_ ) {
                record( "INFO: " + msg );
            }
        }

        /**
         * Sends a raw message to the output, subject to repeat count.
         *
         * @param  msg  output message
         */
        private void record( String msg ) {
            String report = counter_.getReport( msg );
            if ( report != null ) {
                out_.println( report );
            }
        }
    }

    /**
     * SAX content handler for use with this class.
     * It does one non-default job, which is to record the top-level element.
     */
    private static class ValidateContentHandler extends DefaultHandler {
        private boolean isTop_;
        String topUri_;
        String topLocal_;
        @Override
        public void startElement( String uri, String localName,
                                  String qName, Attributes atts )
                throws SAXException {
            if ( ! isTop_ ) {
                isTop_ = true;
                topUri_ = uri;
                topLocal_ = localName;
            }
            super.startElement( uri, localName, qName, atts );
        }
    }

    /**
     * SAX error handler for use with this class.
     */
    private static class ValidateErrorHandler implements ErrorHandler {

        private final Recorder recorder_;
        private final MessageCounter counter_;
        int warningCount_;
        int errorCount_;
        int fatalCount_;

        /**
         * Constructor.
         *
         * @param  recorder  status reporting interface
         */
        ValidateErrorHandler( Recorder recorder ) {
            recorder_ = recorder;
            counter_ = new MessageCounter( 1 );
        }

        public void warning( SAXParseException err ) {

            /* Mark this as an error since a missing schema seems to show up
             * here, in which case schema validation won't have happened. */
            errorCount_++;
            recordException( err );
        }

        public void error( SAXParseException err ) {
            errorCount_++;
            recordException( err );
        }

        public void fatalError( SAXParseException err ) {
            fatalCount_++;
            recordException( err );
        }

        /**
         * Reports an exception through the reporting interface.
         */
        private void recordException( SAXParseException err ) {
            String msg = err.getMessage();
            if ( msg == null ) {
                msg = err.toString();
            }
            String report = counter_.getReport( msg );
            if ( counter_.isRepeat( report ) ) {
                recorder_.error( report );
            }
            else if ( report != null ) {
                StringBuffer sbuf = new StringBuffer();
                int il = err.getLineNumber();
                int ic = err.getColumnNumber();
                if ( il > 0 ) {
                    sbuf.append( "(l." )
                        .append( il );
                    if ( ic > 0 ) {
                        sbuf.append( ", c." )
                            .append( ic );
                    }
                    sbuf.append( ")" );
                }
                sbuf.append( ": " )
                    .append( msg );
                recorder_.error( sbuf.toString() );
            }
        }
    }

    /**
     * Keeps track of repeated messages so that multiple repeat reports
     * can be avoided.
     */
    private static class MessageCounter {

        private final Map<String,Integer> map_;
        private final int nmax_;
        private static final String REPEAT_PREFIX = "[repeated] ";

        /**
         * Constructor.
         *
         * @param  nmax  maximum number of identical messages reported
         */
        MessageCounter( int nmax ) {
            nmax_ = nmax;
            map_ = new HashMap<String,Integer>();
        }

        /**
         * Takes a message and returns the text that should actually be
         * reported.  If the repeat limit has not been reached, the input
         * message will be returned; if the repeat limit is reached
         * a string indicating that fact is returned; if the repeat limit
         * is exceeded a null string is returned.
         *
         * @param   msg  input message
         * @return  message to be reported, may be null
         */
        String getReport( String msg ) {
            int count = map_.containsKey( msg )
                      ? map_.get( msg ).intValue()
                      : 0;
            map_.put( msg, Integer.valueOf( count + 1 ) );
            if ( count < nmax_ ) {
                return msg;
            }
            else if ( count == nmax_ ) {
                return REPEAT_PREFIX + msg;
            }
            else {
                return null;
            }
        }

        /**
         * Indicates whether a given output from the {@link #getReport} method
         * is a repeat indicator.
         *
         * @return  report string
         * @return   true if it indicates the repeat limit has been reached
         */
        boolean isRepeat( String report ) {
            return report != null && report.startsWith( REPEAT_PREFIX );
        }
    }
}
