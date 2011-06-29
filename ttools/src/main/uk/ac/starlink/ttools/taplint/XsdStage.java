package uk.ac.starlink.ttools.taplint;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.xml.XMLConstants;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Validation stage for validating a document against a given XML schema.
 *
 * @author   Mark Taylor
 * @since    3 Jun 2011
 */
public abstract class XsdStage implements Stage {

    private final URL schemaUrl_;
    private Result result_;
    private static final Map<URL,Schema> schemaMap_ = new HashMap<URL,Schema>();
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.taplint" );
    static final String XSDS = "http://www.ivoa.net/xml";

    /**
     * Constructor.
     *
     * @param  schemaUrl  URL of schema to validate against
     */
    protected XsdStage( URL schemaUrl ) {
        schemaUrl_ = schemaUrl;
    }

    /**
     * Returns the URL of the document to validate, given the service URL
     * for the TAP service.
     *
     * @param  serviceUrl   TAP service URL
     * @return   url of XML document to validate
     */
    public abstract String getDocumentUrl( URL serviceUrl );

    public void run( Reporter reporter, URL serviceUrl ) {
        String durl = getDocumentUrl( serviceUrl );
        URL docUrl;
        try {
            docUrl = new URL( durl );
        }
        catch ( MalformedURLException e ) {
            reporter.report( ReportType.FAILURE, "XURL", "Bad document URL" );
            result_ = Result.FAILURE;
            return;
        }
        reporter.report( ReportType.INFO, "VURL",
                         "Validating " + docUrl + " against "
                       + schemaUrl_ );
        result_ = validateDoc( reporter, docUrl );
    }

    /**
     * Returns a token giving the result status last time this stage was run.
     *
     * @return   validation result
     */
    public Result getResult() {
        return result_;
    }

    /**
     * Validates a given document against this stage's schema.
     *
     * @param  reporter  destination for validation messages
     * @param  docUrl   URL of XML document to validate
     */
    private Result validateDoc( Reporter reporter, URL docUrl ) {

        /* Open the document. */
        final InputStream docStream;
        try {
            docStream = docUrl.openStream();
        }
        catch ( FileNotFoundException e ) {
            return Result.NOT_FOUND;
        }
        catch ( IOException e ) {
            reporter.report( ReportType.ERROR, "DCER",
                             "Error reading document", e );
            return Result.FAILURE;
        }

        /* Prepare a validator. */
        final Validator val;
        try {
            val = getSchema( schemaUrl_ ).newValidator();
        }
        catch ( SAXException e ) {
            reporter.report( ReportType.FAILURE, "SCHM",
                             "Can't get/parse schema " + schemaUrl_, e );
            return Result.FAILURE;
        }
        ReporterErrorHandler errHandler = new ReporterErrorHandler( reporter );
        val.setErrorHandler( errHandler );

        /* Perform the validation. */
        try {
            val.validate( new SAXSource(
                              new InputSource(
                                  new BufferedInputStream( docStream ) ) ) );
            return Result.SUCCESS;
        }

        catch ( IOException e ) {
            reporter.report( ReportType.ERROR, "IOER",
                             "Error reading document to parse" );
            return Result.FAILURE;
        }

        /* The validator should only throw a SAX exception in case of
         * a fatal parse error. */
        catch ( SAXException e ) {
            if ( errHandler.getFatalCount() == 0 ) {
                reporter.report( ReportType.FAILURE, "SXER",
                                 "Unexpected document parse error", e );
                return Result.SUCCESS;
            }
            return Result.FAILURE;
        }

        /* Summarise results. */
        finally {
            reporter.summariseUnreportedMessages( reporter.getSectionCode() );
            reporter.report( ReportType.SUMMARY, "VALI",
                             errHandler.getSummary() );
        }
    }

    /**
     * Returns the schema corresponding to a given URL.
     * Schemas are cached in case they are needed again.
     * Since schemas are thread-safe, it's OK for the same one to be in
     * use in multiple places.
     *
     * @param  url  location of XSD schema document
     * @return   compiled schema
     */
    public static Schema getSchema( URL url ) throws SAXException {
        if ( ! schemaMap_.containsKey( url ) ) {
            logger_.info( "Compiling schema " + url );
            Schema schema = SchemaFactory
                           .newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI )
                           .newSchema( url );
            schemaMap_.put( url, schema );
        }
        return schemaMap_.get( url );
    }

    /**
     * Returns a new XsdStage suitable for one of the standard TAP XML
     * endpoints.
     *
     * @param  schemaUrl  URL of XML schema to validate against
     * @param  docUrlSuffix   suffix (include leading /) of TAP service URL
     *                        giving resource endpoint
     * @param  mandatory   true iff resource is REQUIRED by standard
     * @param  resourceDescription  short description of what resource contains
     * @return   new stage for XSD validation
     */
    public static XsdStage createXsdStage( final URL schemaUrl,
                                           final String docUrlSuffix,
                                           final boolean mandatory,
                                           final String resourceDescription ) {
        return new XsdStage( schemaUrl ) {
            public String getDescription() {
                return "Validate " + resourceDescription
                     + " against XML schema";
            }
            public String getDocumentUrl( URL serviceUrl ) {
                return serviceUrl + docUrlSuffix;
            }
            public void run( Reporter reporter, URL serviceUrl ) {
                super.run( reporter, serviceUrl );
                Result result = getResult();
                if ( result == Result.NOT_FOUND ) {
                    if ( mandatory ) {
                        reporter.report( ReportType.ERROR, "GONM",
                                         "Mandatory resource " + docUrlSuffix
                                       + " not present" );
                    }
                    else {
                        reporter.report( ReportType.WARNING, "GONO",
                                         "Optional resource " + docUrlSuffix
                                       + " not present" );
                    }
                }
            }
        };
    }

    /**
     * Enumerates possible results of the parse.
     */
    public static enum Result {

        /** The document did not exist (for instance an HTTP 404). */
        NOT_FOUND,

        /** A serious error prevented the validation from completing. */
        FAILURE,

        /** The validation completed, possibly with some errors or warnings. */
        SUCCESS;
    }
}
