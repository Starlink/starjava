package uk.ac.starlink.ttools.taplint;

import java.net.URL;
import uk.ac.starlink.vo.EndpointSet;

/**
 * Validation stage for validating a document against a given XML schema.
 *
 * @author   Mark Taylor
 * @since    3 Jun 2011
 */
public abstract class XsdStage implements Stage {

    private final String topElName_;
    private final String topElNamespaceUri_;
    private final boolean isMandatory_;
    private final String description_;
    private XsdValidation.Result result_;

    /**
     * Constructor.
     *
     * @param  topElNamespaceUri  namespace of required document root element
     * @param  topElName      local name of required document root element
     * @param  isMandatory   true iff resource is REQUIRED by standard
     * @param  resourceDescription  short description of what resource contains
     */
    protected XsdStage( String topElNamespaceUri, String topElName,
                        boolean isMandatory, String resourceDescription ) {
        topElName_ = topElName;
        topElNamespaceUri_ = topElNamespaceUri;
        isMandatory_ = isMandatory;
        description_ = "Validate " + resourceDescription
                     + " against XML schema";
    }

    public String getDescription() {
        return description_;
    }

    /**
     * Returns the URL of the document to validate, given the service URL
     * for the TAP service.
     *
     * @param  endpointSet  TAP endpoint locations
     * @return   url of XML document to validate
     */
    public abstract URL getDocumentUrl( EndpointSet endpointSet );

    public void run( Reporter reporter, EndpointSet endpointSet ) {
        URL docUrl = getDocumentUrl( endpointSet );
        reporter.report( FixedCode.I_VURL,
                         "Validating " + docUrl + " as "
                       + topElName_ + " (" + topElNamespaceUri_ + ")" );
        boolean includeSummary = true;
        result_ = XsdValidation
                 .validateDoc( reporter, docUrl, topElName_,
                               topElNamespaceUri_, includeSummary );
        if ( result_ == XsdValidation.Result.NOT_FOUND ) {
            if ( isMandatory_ ) {
                reporter.report( FixedCode.E_GONM,
                                 "Mandatory resource " + docUrl
                               + " not present" );
            }
            else {
                reporter.report( FixedCode.W_GONO,
                                 "Optional resource " + docUrl
                               + " not present" );
            }
        }
    }

    /**
     * Returns a token giving the result status last time this stage was run.
     *
     * @return   validation result
     */
    public XsdValidation.Result getResult() {
        return result_;
    }
}
