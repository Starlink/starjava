package uk.ac.starlink.ttools.taplint;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.XMLConstants;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Validation stage for validating a document against a given XML schema.
 *
 * @author   Mark Taylor
 * @since    3 Jun 2011
 */
public abstract class XsdStage implements Stage {

    private final String topElName_;
    private final String topElNamespaceUri_;
    private Result result_;

    /**
     * Constructor.
     *
     * @param  topElNamespaceUri  namespace of required document root element
     * @param  topElName      local name of required document root element
     */
    protected XsdStage( String topElNamespaceUri, String topElName ) {
        topElName_ = topElName;
        topElNamespaceUri_ = topElNamespaceUri;
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
                         "Validating " + docUrl + " as "
                       + topElName_ + " (" + topElNamespaceUri_ + ")" );
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
     * <p>Getting this right seems to be remarkably painful.
     * See comments in the implementation for details.
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

        /* Prepare a validator.  We use a schema factory with no schema
         * sources here (SchemaFactory.newSchema()), which means that
         * any required schemas are read following location hints
         * in the document itself (schemaLocation attributes).
         * The alternative (which used to be here) would be to feed a
         * hardwired schema URL to the schema factory.  The disadvantage of
         * that is that the validator then does not pick up any schemas
         * corresponding to namespaces appearing in the document which
         * have not appeared in the hard-wired schema.  There doesn't
         * appear to be any way to mix and match (feed it a hard-wired
         * schema but also get it to pick up new ones as required).
         * The reason we might want both is because it can be legitimate
         * to include items from a non-IVOA namespace in validated
         * documents, for instance <xs:anyAttribute namespace="##other"/>
         * attributes from VODataService 1.1. */
        final Validator val;
        try {
            val = SchemaFactory
                 .newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI )
                 .newSchema()
                 .newValidator();
        }
        catch ( SAXException e ) {
            reporter.report( ReportType.FAILURE, "XVAL",
                             "Can't prepare validator", e );
            return Result.FAILURE;
        }

        /* Given that we have not fed a hardwired schema to the validator,
         * it's possible that the document is using a non-standard version
         * of the schema for IVOA namespaces.  To ensure that is not the
         * case, install our own ResourceResolver so that given namespaces
         * are reliably mapped to the official locations.
         * I played around with this quite a bit, and this (installing an
         * org.w3c.dom.ls.LSResourceResolver on the validator) seems to be the
         * only way to get it to work.  It would be simpler to just install
         * an org.xml.sax.EntityResolver on a SAX parser and do a validating
         * parse, but in that case the entity resolver doesn't seem to
         * get invoked for schema entities.  Even like this, it only seems
         * to work for java6 and greater (I'm using Oracle J2SE). */
        IvoaSchemaResolver resolver = new IvoaSchemaResolver( reporter );
        val.setResourceResolver( resolver );

        /* Install a reporting error handler on the validator. */
        ReporterErrorHandler errHandler = new ReporterErrorHandler( reporter );
        val.setErrorHandler( errHandler );

        /* Perform the validation, passing the validated output to a custom
         * SAX handler that records the top-level document element. */
        TopElementHandler topelHandler = new TopElementHandler();
        try {
            val.validate( new SAXSource(
                              new InputSource(
                                  new BufferedInputStream( docStream ) ) ),
                          new SAXResult( topelHandler ) );

            /* If we get this far, the validating parse succeeded.
             * Now check to see what the top-level document element was,
             * to ensure that the document is not only valid, but contains
             * the kind of structure we're expecting for this document.
             * I did experiment with testing whether the validated
             * namespace of the top-level element matched the expected one.
             * But frankly, I don't understand under what circumstances
             * the namespaces are supposed to match, so drop that.
             * In principle this means that the document could pass the
             * checks by having a similarly-named element with a completely
             * non-standard definition, but even in that case you'd get
             * a warning that a non-standard schema was being read.
             * I'm not even 100% certain that the top-level element local
             * names have to match, but I think they do. */
            if ( ! topElName_.equals( topelHandler.topName_ ) ) {
                String msg = new StringBuffer()
                   .append( "Wrong top-level element: " )
                   .append( "{" )
                   .append( topelHandler.topUri_ )
                   .append( "}" )
                   .append( topelHandler.topName_ )
                   .append( " != " )
                   .append( "{" )
                   .append( topElNamespaceUri_ )
                   .append( "}" )
                   .append( topElName_ )
                   .toString();
                reporter.report( ReportType.ERROR, "ELDF", msg );
            }

            /* Check that the resolver was actually asked to resolve some
             * schemas.  If it wasn't (resolvedCount==0) there are two
             * likely explanations, either
             * (1) the validated document uses some completely non-standard
             *     namespace(s) (though it might just be misspelt etc)
             * (2) the resolution mechanism was not getting invoked by the
             *     validator
             * If it's (1) I think that's an error in the document, though
             * I'm not entirely sure about the legitimacy of using alternative
             * namespaces for the same schema types.
             * But it might be (2) depending on the details of the XSD
             * validator implementation.  In my tests, Oracle J2SE5 doesn't
             * invoke the entity resolver in a way that makes this work,
             * but java 6 and 7 do.  
             * It looks to me like a bug or misfeature for this to happen,
             * but I don't see JAXP documentation detailed enough to
             * say that's definitely the case.  Java 5 is pretty ancient
             * though, so it's probably not a serious problem. */
            if ( resolver.getResolvedCount() == 0 ) {
                String msg = new StringBuffer()
                    .append( "No resources from known namespaces resolved" )
                    .append( "; resolver not used??" )
                    .append( " (can happen for JRE version<=5)" )
                    .toString();
                reporter.report( ReportType.WARNING, "ZRES", msg );
            }
            return Result.SUCCESS;
        }

        /* IO error during parsing. */
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
     * Returns a new XsdStage suitable for one of the standard TAP XML
     * endpoints.
     *
     * @param  topElName  required local name for top-level document element
     * @param  topElNamespace  expected XML namespace for top-level document
     *                         element (not currently required, which I think
     *                         is correct, but I could be wrong)
     * @param  docUrlSuffix   suffix (include leading /) of TAP service URL
     *                        giving resource endpoint
     * @param  mandatory   true iff resource is REQUIRED by standard
     * @param  resourceDescription  short description of what resource contains
     * @return   new stage for XSD validation
     */
    public static XsdStage createXsdStage( String topElName,
                                           String topElNamespace,
                                           final String docUrlSuffix,
                                           final boolean mandatory,
                                           final String resourceDescription ) {
        return new XsdStage( topElName, topElNamespace ) {
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

    /**
     * SAX content handler that records the top-level element of the
     * document it is parsing.
     */
    private static class TopElementHandler extends DefaultHandler {
        private boolean isTop_;
        String topUri_;
        String topName_;
        @Override
        public void startElement( String uri, String localName,
                                  String qName, Attributes atts ) {
            if ( ! isTop_ ) {
                isTop_ = true;
                topUri_ = uri;
                topName_ = localName;
            }
        }
    }
}
