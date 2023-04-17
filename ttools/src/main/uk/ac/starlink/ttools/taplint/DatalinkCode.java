package uk.ac.starlink.ttools.taplint;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Enumerates known ReportCode instances for Datalink validation.
 *
 * @author   Mark Taylor
 * @since    27 Nov 2017
 */
public enum DatalinkCode implements ReportCode {

    E_BFLT( "Illegal error message syntax", Doc.DATALINK, "3.4" ),
    E_BVOT( "Datalink response is not VOTable" ),
    E_CQCO( "Unknown content_qualifier term", Doc.DATALINK11, "3.2.9" ),
    E_DCER( "Document load error" ),
    E_DLCT( "Bad datalink Content-Type", Doc.DATALINK, "3.3" ),
    E_DRCT( "Bad Content-Type syntax", Doc.RFC2045, "5.1" ),
    E_DRER( "Error message syntax", Doc.DATALINK, "3.4" ),
    E_DRID( "Missing datalink ID entry", Doc.DATALINK, "3.2" ),
    E_DRR1( "Illegal Datalink row values", Doc.DATALINK, "3.2" ),
    E_DRS0( "Blank semantics entry", Doc.DATALINK, "3.2.6" ),
    E_DRSI( "Reference nonexistent service def", Doc.DATALINK, "3.2.3" ),
    E_DRUR( "Bad access_url", Doc.DATALINK, "3.2.2" ),
    E_DSTD( "Missing standardID INFO", Doc.DATALINK11, "3.3.1" ),
    E_ENVO( "Error document not VOTable", Doc.DATALINK, "3.4" ),
    E_ERDC( "Bad VOTable error document" ),
    E_IDSQ( "Non-contiguous IDs", Doc.DATALINK11, "3.2" ),
    E_LKAZ( "Bad link_auth value", Doc.DATALINK11, "3.2.11" ),
    E_PSNS( "Non-string standard param", Doc.DATALINK, "4.1" ),
    E_RCOL( "Wrong datalink columns" ),
    E_RTYP( "Incorrect column type", Doc.DATALINK, "3.2" ),
    E_RUCD( "Incorrect UCD", Doc.DATALINK, "3.2" ),
    E_RUNI( "Incorrect column units", Doc.DATALINK, "3.2.8" ),
    E_SAC0( "Missing service descriptor Access URL", Doc.DATALINK, "4.1" ),
    E_SACB( "Bad service descriptor Access URL", Doc.DATALINK, "4.1" ),
    E_SARF( "No FIELD with inputParam ID", Doc.DATALINK, "4.1" ),
    E_SMCO( "Unknown semantics term", Doc.DATALINK, "3.2.6" ),
    E_TDSR( "Non-TABLEDATA serialization", Doc.DATALINK, "3.3.1" ),
    E_URES( "No unique results resource", Doc.DALI, "4.4" ),
    E_UTAB( "No unique result table", Doc.DATALINK, "3.3.1" ),
    E_VTCT( "Bad VOTable Content-Type", Doc.VOTABLE, "8" ),
    E_VTSX( "VOTable parse error" ),

    F_UNEX( "Unexpected validation failure" ),

    I_CQCU( "Custom content_qualifier term", Doc.DATALINK11, "3.2.9" ),
    I_DLNR( "DataLink rows checked" ),
    I_DLVR( "Report DataLink validation version" ),
    I_EXCL( "Extra columns in results table" ),
    I_GEDL( "Retrieving DataLink document" ),
    I_IKAC( "Row access URL" ),
    I_IKER( "Row error message" ),
    I_IKSD( "Row service descriptor invocation URL" ),
    I_RNTY( "RESOURCE elements with no @type" ),
    I_SDDF( "Service descriptors summary" ),
    I_SDDO( "Service descriptor defined" ),
    I_SDPR( "Service decriptor input parameters" ),
    I_SMCU( "Custom semantics term", Doc.DATALINK, "3.2.6" ),

    W_CQCO( "Preliminary/deprecated content_qualifier term",
            Doc.DATALINK11, "3.2.9" ),
    W_DLCT( "Non-canonical service Content-Type", Doc.DATALINK, "3.3" ),
    W_DRCT( "Non-canonical link Content-Type?", Doc.DATALINK, "3.2.7" ),
    W_MTAB( "TABLE in meta RESOURCE", Doc.DATALINK, "4.1" ),
    W_NOCT( "No Content-Type header" ),
    W_PIDU( "Duplicated input param", Doc.DATALINK, "4.1" ),
    W_PSDU( "Duplicated standard param", Doc.DATALINK, "4.1" ),
    W_QERR( "Datalink query failed" ),
    W_SAVV( "Both ref and value/values in inputParam" ),
    W_SDND( "Insufficient @name/DESCRIPTION metadata", Doc.DATALINK11, "4.1" ),
    W_SMCO( "Preliminary/deprecated semantics term", Doc.DATALINK, "3.2.6" );

    private final String description_;
    private final Doc document_;
    private final String docsec_;

    /**
     * Constructs a code with just a description.
     *
     * @param  description  short description of report semantics
     */
    DatalinkCode( String description ) {
        this( description, null, null );
    }

    /**
     * Constructs a code with a description and a document reference.
     *
     * @param  description  short description of report semantics
     * @param  doc   standards document containing the standards text
     *               to which this report code relates
     * @param  docsec  name of a section or location within <code>doc</code>
     *                 to which this report code relates
     */
    DatalinkCode( String description, Doc document, String docsec ) {
        description_ = description;
        document_ = document;
        docsec_ = docsec;
    }

    public ReportType getType() {
        return ReportType.forChar( name().charAt( 0 ) );
    }

    public String getLabel() {
        return name().substring( 2 );
    }

    /**
     * Returns a short textual description of the use of this code.
     * It may not be very precise; if the message put through the reporting
     * system is available, that should be used in preference.
     *
     * @return  description
     */
    public String getDescription() {
        return description_;
    }

    /**
     * Returns a reference to the standards document to which this code
     * refers, if applicable.
     *
     * @return   standards document reference,
     *           or null if inapplicable or unknown
     */
    public Doc getDocument() {
        return document_;
    }

    /**
     * Returns an indication of the location within the standards document
     * to which this code refers.
     *
     * @return  standards document section name/number/etc,
     *          or null if inapplicable or unknown
     */
    public String getSection() {
        return docsec_;
    }

    /**
     * Enumerates standards documents.
     * This makes it definite what specifications the error codes
     * are referring to.
     */
    public static class Doc {
        public static final Doc DATALINK =
            new Doc( "DataLink-1.0",
                     "http://www.ivoa.net/documents/DataLink/20150617/" );
        public static final Doc DATALINK11 =
            new Doc( "DataLink-1.1-PR-20230413",
                     "https://www.ivoa.net/documents/DataLink/20230413/" );
        public static final Doc DALI =
            new Doc( "DALI-1.1",
                     "http://www.ivoa.net/documents/DALI/20170517/" );
        public static final Doc VOTABLE =
            new Doc( "VOTable-1.3",
                     "http://www.ivoa.net/documents/VOTable/20130920/" );
        public static final Doc RFC2045 =
            new Doc( "RFC2045",
                     "https://tools.ietf.org/html/rfc2045" );

        private final String name_;
        private final URL url_;

        /**
         * Constructor.
         *
         * @param  name  short designation of document, including
         *               version number if appropriate
         */
        public Doc( String name, String url ) {
            name_ = name;
            try {
                url_ = new URL( url );
            }
            catch ( MalformedURLException e ) {
                // shouldn't happen.
                throw new IllegalArgumentException( "Bad URL " + url );
            }
        }

        /**
         * Returns this document's name.
         *
         * @return  short name designation, including version string
         *          if appropriate
         */
        public String getName() {
            return name_;
        }

        /**
         * Returns a URL to the content of this document.
         *
         * @return  document URL
         */
        public URL getUrl() {
            return url_;
        }
    }
}
