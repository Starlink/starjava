package uk.ac.starlink.ttools.taplint;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.ttools.votlint.VocabChecker;
import uk.ac.starlink.util.Compression;
import uk.ac.starlink.util.ContentType;
import uk.ac.starlink.util.DOMUtils;
import uk.ac.starlink.util.URLUtils;
import uk.ac.starlink.vo.DatalinkVersion;
import uk.ac.starlink.vo.datalink.LinkColMap;
import uk.ac.starlink.vo.datalink.LinksDoc;
import uk.ac.starlink.vo.datalink.ServiceInvoker;
import uk.ac.starlink.votable.ParamElement;
import uk.ac.starlink.votable.TableElement;
import uk.ac.starlink.votable.VODocument;
import uk.ac.starlink.votable.VOElement;
import uk.ac.starlink.votable.VOStarTable;
import uk.ac.starlink.votable.VOTableVersion;
import uk.ac.starlink.votable.datalink.ServiceDescriptor;
import uk.ac.starlink.votable.datalink.ServiceDescriptorFactory;
import uk.ac.starlink.votable.datalink.ServiceParam;

/**
 * Performs validation of DataLink documents.
 * A {@link Reporter} instance is supplied at construction time,
 * and all validation reports are reported via that object.
 *
 * <p>The DataLink document is loaded as a DOM rather than using
 * SAX to stream it.  DataLink documents are expected to be of a
 * fairly manageable size, so this should be OK.
 *
 * @author   Mark Taylor
 * @since    23 Nov 2017
 * @see <a href="http://www.ivoa.net/documents/DataLink/">DataLink</a>
 */
public class DatalinkValidator {

    private final Reporter reporter_;
    private final DatalinkVersion version_;

    /** DataLink-1.0 sec 3.1. */
    private static final String CANONICAL_DL_CTYPE =
        "application/x-votable+xml;content=datalink";

    /** DataLink-1.0 sec 3.4. */
    private static final Pattern FAULT_REGEX =
        Pattern.compile( "(NotFound|Usage|Transient|Fatal|Default)Fault"
                       + "(:.*)?" );

    /** DataLink 1.0 sec 3.2.6. */
    private static final VocabChecker SEMANTICS_CHECKER =
        VocabChecker.DATALINK_CORE;

    /** DataLink 1.1 sec 3.2.9. */
    private static final VocabChecker CONTENTQUALIFIER_CHECKER =
        VocabChecker.PRODUCT_TYPE;

    /**
     * Constructor.
     *
     * @param  reporter  destination for validation methods
     * @param  version   fixed datalink version to validate against;
     *                   may be null if not known/specified
     */
    public DatalinkValidator( Reporter reporter, DatalinkVersion version ) {
        reporter_ = reporter;
        version_ = version;
    }

    /**
     * Performs checks on a DataLink document obtained from a URL.
     *
     * <p>The supplied URL is assumed to refer to a GET request 
     * expected to return a DataLink document, and with no
     * RESPONSEFORMAT parameter (which means that the result must be
     * a TABLEDATA-serialization VOTable).
     * Checks on the HTTP and DALI behaviour are run in addition
     * to the DataLink checks themselves.
     * Additional checks may be performed if the service is asserted
     * to be a DataLink {links} service.
     *
     * @param  url  document URL
     * @param  isLinksService  true iff the service is supposed to
     *         conform to ivo://ivoa.net/std/DataLink#links-1.0
     * @param  mustSucceed   if true, the service is not supposed to return
     *                       an error response
     */
    public void validateDatalink( URL url, boolean isLinksService,
                                  boolean mustSucceed ) {
        reporter_.report( DatalinkCode.I_GEDL,
                          "Retrieving DataLink document from " + url );

        /* Open the URL connection to the target document. */
        URLConnection conn;
        int httpCode;
        try {
            conn = url.openConnection();
            conn = URLUtils.followRedirects( conn, null );
            conn.connect();
            httpCode = conn instanceof HttpURLConnection
                     ? ((HttpURLConnection) conn).getResponseCode()
                     : -1;
        }
        catch ( IOException e ) {
            String msg = new StringBuffer()
                .append( "DataLink invocation failed at " )
                .append( url )
                .append( " (" )
                .append( e )
                .append( ")" )
                .toString();
            reporter_.report( DatalinkCode.E_DCER, msg );
            httpCode = -1;
            return;
        }
        HttpURLConnection hconn = conn instanceof HttpURLConnection
                                ? (HttpURLConnection) conn
                                : null;

        /* Get the input stream. */
        Compression compression = getCompression( conn );
        final InputStream in;
        try {
            in = compression.decompress( conn.getInputStream() );
        }
        catch ( IOException e ) {
            if ( hconn != null && isLinksService ) {
                InputStream err = hconn.getErrorStream();
                if ( err != null ) {
                    checkErrorVOTable( err, url, hconn.getContentType() );
                }
            }
            String msg = new StringBuffer()
                .append( "DataLink invocation " )
                .append( conn.getURL() )
                .append( " failed" )
                .append( httpCode >= 0 ? " " + httpCode : "" )
                .append( " (" )
                .append( e )
                .append( ")" )
                .toString();
            reporter_.report( mustSucceed ? DatalinkCode.E_DCER
                                          : DatalinkCode.W_QERR,
                              msg );
            return;
        }

        /* Parse it as a VOTable.
         * VOTable validation reports are generated if successful. */
        final VODocument vodoc;
        try {
            vodoc = readVODocument( in );
        }
        catch ( IOException e ) {
            String msg = new StringBuffer()
                .append( "Error reading data from " )
                .append( url )
                .append( ": " )
                .append( e )
                .toString();
            reporter_.report( DatalinkCode.E_DCER, msg );
            return;
        }
        catch ( SAXException e ) {
            String msg = new StringBuffer()
                .append( "Badly-formed XML (not VOTable): " )
                .append( url )
                .append( " (" )
                .append( e )
                .append( ")" )
                .toString();
            reporter_.report( DatalinkCode.E_VTSX, msg );
            return;
        }

        /* If it was a VOTable, check the content-type. */
        if ( hconn != null ) {
            checkVOTableContentType( hconn.getContentType(), url,
                                     isLinksService );
        }

        /* Perform the DataLink-specific validation on the VOTable DOM. */
        validateDatalink( vodoc );
    }

    /**
     * Performs checks on a DataLink document read from a given input stream.
     *
     * <p>The supplied InputStream is assumed to be the result of a
     * DataLink request with no RESPONSEFORMAT parameter, which means that
     * the result must a  TABLEDATA-serialization VOTable.
     *
     * @param  in  input stream
     */
    public void validateDatalink( InputStream in ) {
        reporter_.report( DatalinkCode.I_GEDL,
                          "Reading DataLink document from standard input" );
        final VODocument vodoc;
        try {
            vodoc = readVODocument( in );
        }
        catch ( IOException e ) {
            String msg = new StringBuffer()
                .append( "Error reading data: " )
                .append( e )
                .toString();
            reporter_.report( DatalinkCode.E_DCER, msg );
            return;
        }
        catch ( SAXException e ) {
            String msg = new StringBuffer()
                .append( "Badly-formed XML (not VOTable): " )
                .append( e )
                .toString();
            reporter_.report( DatalinkCode.E_VTSX, msg );
            return;
        }
        validateDatalink( vodoc );
    }

    /**
     * Performs DataLink-specific validation on a VOTable DOM.
     *
     * @param  vodoc  DOM assumed to contain a document conforming to
     *                the DataLink standard
     */
    public void validateDatalink( VODocument vodoc ) {
        LinksDoc linksDoc = createLinksDoc( vodoc );
        if ( linksDoc != null ) {
            DatalinkVersion version = getEffectiveVersion( vodoc );
            validateLinksDoc( linksDoc, version );
        }
    }

    /**
     * Performs checks on a LinksDoc object.
     * This performs additional validation on an object which has
     * already been parsed as a DataLink document.
     *
     * @param  linksDoc  object representing DataLink document
     * @param  version   datalink specification version, not null
     */
    public void validateLinksDoc( LinksDoc linksDoc, DatalinkVersion version ) {

        /* Prepare service invokers for the ServiceDescriptors defined
         * by the links doc. */
        Map<String,ServiceInvoker> invokerMap =
            createServiceInvokerMap( linksDoc );

        /* Check the content of each row in the results table. */
        try {
            attemptValidateLinksDocRows( linksDoc, invokerMap, version );
        }
        catch ( IOException e ) {

            /* The table has already been read here, so there shouldn't
             * be any read problems. */
            String msg = new StringBuffer()
                .append( "Unexpected read error for datalinks table: " )
                .append( e )
                .toString();
            reporter_.report( DatalinkCode.F_UNEX, msg );
        }
    }

    /**
     * Parses a VOTable document as a DataLink structure, reporting any
     * validation issues as it does.
     *
     * @param  vodoc   DOM assumed to conform to DataLink rules
     * @return  parsed represntation of the DataLink document
     */
    public LinksDoc createLinksDoc( VODocument vodoc ) {

        /* Extract RESOURCE elements by the value of the @type attribute. */
        Map<String,List<VOElement>> resourceMap = getTypedResources( vodoc );
        if ( resourceMap == null ) {
            return null;
        }

        /* Locate the TABLE element corresponding to the results table. */
        List<VOElement> resultsResources = resourceMap.get( "results" );
        int nres = resultsResources.size();
        if ( nres != 1 ) {
            String msg = new StringBuffer()
                .append( nres == 0 ? "No "
                                   : "Multiple (" + nres + ") "  )
                .append( "<RESOURCE type='results'> elements " )
                .append( "in datalink VOTable" )
                .toString();
            reporter_.report( DatalinkCode.E_URES, msg );
            return null;
        }
        NodeList tableEls = resultsResources.get( 0 )
                           .getElementsByVOTagName( "TABLE" );
        int ntab = tableEls.getLength();
        if ( ntab != 1 ) {
            String msg = new StringBuffer()
                .append( ntab == 0 ? "No "
                                   : "Multiple (" + ntab + ") " )
                .append( "TABLEs in datalink <RESOURCE type='results'> " )
                .append( "element" )
                .toString();

            /* DataLink 3.3.1 doesn't explicitly say this has to be unique,
             * but it's hard to make sense of it otherwise. */
            reporter_.report( DatalinkCode.E_UTAB, msg );
            return null;
        }

        /* Turn it into a StarTable. */
        TableElement tableEl = (TableElement) tableEls.item( 0 );
        VOStarTable resultTable;
        try {
            resultTable = new VOStarTable( tableEl );
        }
        catch ( IOException e ) {
            reporter_.report( DatalinkCode.F_UNEX, "Unexpected error: " + e );
            return null;
        }

        /* Check that it uses the TABLEDATA serialization.
         * This is an explicit requirement of the DataLink standard,
         * unless some other format is explicitly requested (RESPONSEFORMAT). */
        VOElement dataEl = tableEl.getChildByName( "DATA" );
        if ( dataEl != null ) {
            for ( VOElement dataChild : dataEl.getChildren() ) {
                String dname = dataChild.getVOTagName();
                if ( "FITS".equals( dname ) ||
                     "BINARY".equals( dname ) ||
                     "BINARY2".equals( dname ) ) {
                    String msg = new StringBuffer()
                        .append( "Illegal serialization format " )
                        .append( dname )
                        .append( "; must be TABLEDATA " )
                        .append( "except by explicit request" )
                        .toString();
                     reporter_.report( DatalinkCode.E_TDSR, msg );
                }
            }
        }

        /* Locate the DataLink-specific columns in the table,
         * reporting any validation issues. */
        LinkColMap colMap = createColMap( resultTable );

        /* Locate all the RESOURCEs that contain service descriptions,
         * and turn them into a list of ServiceDescriptors. */
        List<ServiceDescriptor> sdList = new ArrayList<ServiceDescriptor>();
        for ( VOElement metaRes : resourceMap.get( "meta" ) ) {
            if ( "adhoc:service".equals( metaRes.getAttribute( "utype" ) ) ) {
                if ( metaRes.getElementsByVOTagName( "TABLE" )
                            .getLength() > 0 ) {
                    reporter_.report( DatalinkCode.W_MTAB,
                                      "TABLE element(s) in "
                                    + "adhoc:service RESOURCE?" );
                }
                ServiceDescriptor sd = createServiceDescriptor( metaRes );
                if ( sd != null ) {
                    sdList.add( sd );
                }
            }
        }
        ServiceDescriptor[] servDescriptors =
            sdList.toArray( new ServiceDescriptor[ 0 ] );
        int nSd = servDescriptors.length;
        int nIdentified = 0;
        Set<String> nameSet = new HashSet<>();
        Set<String> descriptionSet = new HashSet<>();
        for ( ServiceDescriptor sd : servDescriptors ) {
            if ( sd.getDescriptorId() != null ) {
                nIdentified++;
            }
            String name = sd.getName();
            String descrip = sd.getDescription();
            if ( name != null && name.trim().length() > 0 ) {
                nameSet.add( name.trim() );
            }
            if ( descrip != null && descrip.trim().length() > 0 ) {
                descriptionSet.add( descrip.trim() );
            }
        }
        String msg = new StringBuffer()
            .append( "Service descriptors defined: " )
            .append( nIdentified )
            .append( " referenceable, " )
            .append( nSd - nIdentified )
            .append( " anonymous" )
            .toString();
        reporter_.report( DatalinkCode.I_SDDF, msg );

        /* Check if they are labelled with name attribute and DESCRIPTION
         * element as recommended by Datalink 1.1 sec 4.1.
         * Although this is not part of the Datalink 1.0 specification,
         * warn even for DL 1.0 validation, since it's still a good idea. */
        if ( nSd > 1 ) {
            StringBuffer missBuf = new StringBuffer();
            if ( nameSet.size() < nSd ) {
                missBuf.append( "@names" );
            }
            if ( descriptionSet.size() < nSd ) {
                missBuf.append( missBuf.length() == 0 ? "" : " and " )
                       .append( "DESCRIPTIONs" );
            }
            String ndMsg = new StringBuffer()
                .append( "Multiple service descriptors defined, " )
                .append( "but not all have unique " )
                .append( missBuf )
                .toString();
            reporter_.report( DatalinkCode.W_SDND, ndMsg );
        }

        /* Assemble and return a parsed LinksDoc object. */
        return LinksDoc.createLinksDoc( resultTable, colMap, servDescriptors );
    }

    /**
     * Parses an input stream assumed to contain a VOTable document
     * and returns a DOM.  If the parse completes successfully,
     * VOTable validation reports are also issued.
     *
     * @param  in  input stream
     * @return  VOTable DOM
     * @throws  IOException  if there is an input stream read error
     * @throws  SAXException  if there is a fatal XML parse error,
     *                        most likely because the stream isn't XML
     */
    private VODocument readVODocument( InputStream in )
            throws IOException, SAXException {
        boolean doChecks = true;
        VOTableVersion minVotVersion = null;
        HoldReporter holder = new HoldReporter();
        VODocument vodoc =
            VotLintTapRunner
           .readResultDocument( holder, in, doChecks, minVotVersion );

        /* Only issue the VOTable validation reports if the parse was
         * basically successful.  Otherwise the reports are probably
         * meaningless. */
        holder.dumpReports( reporter_ );
        return vodoc;
    }

    /**
     * Returns the DataLink version against which the given document
     * should be validated.  The choice is reported.
     *
     * @param  vodoc  document to validate
     * @return   validation version, not null
     */
    private DatalinkVersion getEffectiveVersion( VODocument vodoc ) {

        /* In principle the document might report its own version.
         * At time of writing however (PR-DataLink-1.1-20230413)
         * it can't actually do that, so ignore the vodoc argument. */
        final DatalinkVersion version;
        final String vtype;
        if ( version_ != null ) {
            version = version_;
            vtype = "requested";
        }
        else {
            version = DatalinkVersion.V11;
            vtype = "assumed";
        }
        String msg = new StringBuffer()
           .append( "Using " )
           .append( vtype )
           .append( " DataLink version " )
           .append( version )
           .append( " for validation" )
           .toString();
        reporter_.report( DatalinkCode.I_DLVR, msg );
        return version;
    }

    /**
     * Identifies the DataLink-defined columns in a table, and reports on
     * any validation issues.
     *
     * @param   table  table assumed to be a DataLink results table
     * @return   object that knows what DataLink columns are where in table
     */
    private LinkColMap createColMap( StarTable table ) {

        /* Iterate over each table column, populating a map from ColDef
         * to column index. */
        Map<LinkColMap.ColDef<?>,Integer> icolMap =
            new HashMap<LinkColMap.ColDef<?>,Integer>();
        int ncol = table.getColumnCount();
        int nExtraCol = 0;
        for ( int ic = 0; ic < ncol; ic++ ) {
            ColumnInfo info = table.getColumnInfo( ic );
            String name = info.getName();

            /* See if it's a DataLink column. */
            LinkColMap.ColDef<?> coldef = LinkColMap.COLDEF_MAP.get( name );
            if ( coldef != null ) {
                String stdUcd = coldef.getUcd();

                /* Check UCD against DataLink specification. */
                String ucd = info.getUCD();
                if ( ucd == null ) {
                    if ( stdUcd != null ) {
                        String msg = new StringBuffer()
                            .append( "Missing UCD for column " )
                            .append( name )
                            .append( "; should be " )
                            .append( stdUcd )
                            .toString();
                        reporter_.report( DatalinkCode.E_RUCD, msg );
                    }
                }
                else if ( ! ucd.equals( stdUcd ) ) {
                    String msg = new StringBuffer()
                        .append( "Wrong UCD for column " )
                        .append( name )
                        .append( "; " )
                        .append( ucd )
                        .append( " != " )
                        .append( stdUcd )
                        .toString();
                    reporter_.report( DatalinkCode.E_RUCD, msg );
                }

                /* Check datatype against DataLink specification. */
                Class<?> clazz = info.getContentClass();
                final Class<?> reqClazz;
                if ( coldef == LinkColMap.COL_CONTENTLENGTH ) {
                    reqClazz = Long.class;
                }
                else {
                    reqClazz = coldef.getContentClass();
                }
                if ( ! reqClazz.isAssignableFrom( clazz ) ) {
                    String datatype =
                        info.getAuxDatumValue( VOStarTable.DATATYPE_INFO,
                                               String.class );
                    String msg = new StringBuffer()
                       .append( "Wrong datatype '" )
                       .append( datatype )
                       .append( "' for column " )
                       .append( name )
                       .append( ", should be " )
                       .append( reqClazz.getSimpleName().toLowerCase() )
                       .toString();
                    reporter_.report( DatalinkCode.E_RTYP, msg );
                }

                /* Only content_length has units ("byte"). */
                if ( LinkColMap.COL_CONTENTLENGTH == coldef ) {
                    if ( ! "byte".equals( info.getUnitString() ) ) {
                        String msg = new StringBuffer()
                            .append( "Wrong units (" )
                            .append( info.getUnitString() )
                            .append( ") for column " )
                            .append( name )
                            .append( " - should be 'byte'" )
                            .toString();
                        reporter_.report( DatalinkCode.E_RUNI, msg );
                    }
                }

                /* If we haven't already seen this one, and as long as it's
                 * got the right data type, store it for use in the map. */
                if ( icolMap.containsKey( coldef ) ) {
                    reporter_.report( DatalinkCode.E_RCOL,
                                      "Multiple columns named " + name );
                }
                else if ( coldef.getContentClass().isAssignableFrom( clazz ) ) {
                    icolMap.put( coldef, new Integer( ic ) );
                }
            }
            else {
                nExtraCol++;
            }
        }

        /* Report on columns found and not found. */
        if ( nExtraCol > 0 ) {
            reporter_.report( DatalinkCode.I_EXCL,
                              "Non-standard columns in results table: "
                            + nExtraCol );
        }
        StringBuffer missingBuf = new StringBuffer();
        for ( LinkColMap.ColDef<?> coldef : LinkColMap.COLDEF_MAP.values() ) {
            if ( coldef.isRequired() && ! icolMap.containsKey( coldef ) ) {
                if ( missingBuf.length() > 0 ) {
                    missingBuf.append( ", " );
                }
                missingBuf.append( coldef.getName() );
            }
        }
        if ( missingBuf.length() > 0 ) {
            reporter_.report( DatalinkCode.E_RCOL,
                              "Missing/unusable required DataLink columns: "
                             + missingBuf );
        }

        /* Return the map. */
        return new LinkColMap( icolMap ) {};
    }

    /**
     * Parses a DOM element as a ServiceDescriptor.
     * The supplied element is expected to be a RESOURCE with
     * with @type="meta" and utype="adhoc:service".
     *
     * @param  resourceEl  DOM element
     * @return   service descriptor
     */
    private ServiceDescriptor createServiceDescriptor( VOElement resourceEl ) {

        /* Use library code to create the service descriptor. */
        ServiceDescriptor sd = new ServiceDescriptorFactory()
                              .createServiceDescriptor( resourceEl );

        /* Report on its characteristics. */
        String sdName = sd.getDescriptorId();
        sdName = sdName == null ? "<unnamed>" : sdName;
        String msg0 = new StringBuffer()
            .append( "Service descriptor " )
            .append( sdName )
            .append( ", " )
            .append( sd.getInputParams().length )
            .append( " input params" )
            .toString();
        reporter_.report( DatalinkCode.I_SDDO, msg0 );
        Map<ParamType,List<String>> inParamsMap =
            new LinkedHashMap<ParamType,List<String>>();
        for ( ParamType ptype : ParamType.values() ) {
            inParamsMap.put( ptype, new ArrayList<String>() );
        }
        for ( ServiceParam param : sd.getInputParams() ) {
            String value = param.getValue();
            final ParamType ptype;
            if ( param.getRef() != null ) {
                ptype = ParamType.ROW;
            }
            else if ( value != null && value.length() > 0 ) {
                ptype = ParamType.FIXED;
            }
            else {
                ptype = ParamType.USER;
            }
            inParamsMap.get( ptype ).add( param.getName() );
        }
        for ( Map.Entry<ParamType,List<String>> entry :
              inParamsMap.entrySet() ) {
            List<String> pnames = entry.getValue();
            int np = pnames.size();
            String msg = new StringBuffer()
                .append( entry.getKey() )
                .append( " parameter count " )
                .append( np )
                .append( np > 0 ? " " + pnames : "" )
                .toString();
            reporter_.report( DatalinkCode.I_SDPR, msg );
        }

        /* But perform some additional checks for things that the
         * library code just ignores. */
        Map<String,Integer> paramCounts = new HashMap<String,Integer>();
        paramCounts.put( "accessURL", new Integer( 0 ) );
        paramCounts.put( "standardID", new Integer( 0 ) );
        paramCounts.put( "resourceIdentifier", new Integer( 0 ) );
        for ( VOElement pEl : resourceEl.getChildrenByName( "PARAM" ) ) {
            ParamElement paramEl = (ParamElement) pEl;
            String name = paramEl.getName();
            Integer count = paramCounts.get( name );
            if ( count != null ) {
                Object value = paramEl.getValue();
                String datatype = paramEl.getDatatype();
                long[] arraysize = paramEl.getArraysize();
                if ( ( ! "char".equals( datatype ) &&
                       ! "unicodeChar".equals( datatype ) ) ||
                     ( arraysize.length != 1 ||
                       arraysize[ 0 ] == 0 || arraysize[ 0 ] == 1 ) ) {
                    String msg = new StringBuffer()
                        .append( "Non-string service descriptor param " )
                        .append( name )
                        .append( ": " )
                        .append( "datatype='" )
                        .append( datatype )
                        .append( "', " )
                        .append( "arraysize='" )
                        .append( paramEl.getAttribute( "arraysize" ) )
                        .append( "'" )
                        .toString();
                    reporter_.report( DatalinkCode.E_PSNS, msg );
                }
                paramCounts.put( name, new Integer( count + 1 ) );
            }
        }
        for ( Map.Entry<String,Integer> entry : paramCounts.entrySet() ) {
            int count = entry.getValue().intValue();
            if ( count > 1 ) {
                String msg = new StringBuffer()
                    .append( "Duplicated service PARAM " )
                    .append( entry.getKey() )
                    .append( " (" )
                    .append( count )
                    .append( " copies)" )
                    .toString();
                reporter_.report( DatalinkCode.W_PSDU, msg );
            }
        }
        Collection<String> inParams = new HashSet<String>();
        for ( ServiceParam inParam : sd.getInputParams() ) {
            String name = inParam.getName();
            if ( ! inParams.add( name ) ) {
                String msg = new StringBuffer()
                    .append( "Duplicated input parameter " )
                    .append( name )
                    .append( " in service descriptor " )
                    .append( sdName )
                    .toString();
                reporter_.report( DatalinkCode.W_PIDU, msg );
            }
        }
        return sd;
    }

    /**
     * Locates RESOURCE elements in a DOM and groups them by legal values
     * of their @type attribute.
     * The returned map contains exactly two keys, "meta" and "results".
     * RESOURCE elements with no @type attribute are ignored.
     * A RESOURCE element with any other type attribute, which is illegal,
     * is also ignored, and will have been complained about by
     * VOTable validation.
     *
     * @param  vodoc  VOTable DOM; if the top-level element is not VOTABLE
     *                an error is reported, no further checks are made,
     *                and null is returned
     * @return   2-entry map from @type value ("meta" or "results")
     *           to list of RESOURCE elements with
     */
    private Map<String,List<VOElement>> getTypedResources( VODocument vodoc ) {

        /* Check the top-level element is VOTABLE. */
        VOElement voEl = (VOElement) vodoc.getDocumentElement();
        if ( ! "VOTABLE".equals( voEl.getVOTagName() ) ) {
            String msg = new StringBuffer()
               .append( "Top-level element of result document is " )
               .append( voEl.getTagName() )
               .append( " not VOTABLE" )
               .toString();
            reporter_.report( DatalinkCode.E_BVOT, msg );
            return null;
        }

        /* Identify "results" and "meta" resources, collect them in a
         * suitable data structure, and return it. */
        Map<String,List<VOElement>> resourceMap =
            new LinkedHashMap<String,List<VOElement>>();
        resourceMap.put( "results", new ArrayList<VOElement>() );
        resourceMap.put( "meta", new ArrayList<VOElement>() );
        NodeList resList = voEl.getElementsByVOTagName( "RESOURCE" );
        int nUntyped = 0;
        for ( int i = 0; i < resList.getLength(); i++ ) {
            VOElement resEl = (VOElement) resList.item( i );
            String type = resEl.hasAttribute( "type" )
                        ? resEl.getAttribute( "type" )
                        : null;
            if ( type == null ) {
                nUntyped++;
            }
            else if ( "results".equals( type ) || "meta".equals( type ) ) {
                resourceMap.get( type ).add( resEl );
            }
            else {
                // VOTable validation elsewhere will have flagged this case
            }
        }
        if ( nUntyped > 0 ) {
            String msg = new StringBuffer()
                .append( nUntyped )
                .append( " RESOURCE elements without type attribute" )
                .toString();
            reporter_.report( DatalinkCode.I_RNTY, msg );
         }
        return resourceMap;
    }

    /**
     * Returns a map from service descriptor ID (service_def) to
     * ServiceInvoker.  For descriptors which are too broken to
     * create a ServiceInvoker, there will be an entry in the returned map,
     * but the value will be null.
     * Descriptors with no ID will not appear in the returned map.
     *
     * <p>This method also serves to validate the ServiceDescriptor objects
     * (with and without descriptor IDs); validation reports will
     * be output as appropriate.
     *
     * <p>This relates to DataLink-1.0 sec 4.
     *
     * @param  linksDoc  representation of DataLink document
     * @return  map from service descriptor ID value to an object
     *          that can invoke that service;  for broken descriptors
     *          there will be an entry but the value will be null;
     *          descriptors with no ID will not appear in the map
     */
    private Map<String,ServiceInvoker>
            createServiceInvokerMap( LinksDoc linksDoc ) {

        /* Prepare a list of IDs attached to FIELDs in the datalink document
         * results table.  These are possible referents of service input
         * parameters. */
        Collection<String> fieldIds = new HashSet<String>();
        StarTable resultTable = linksDoc.getResultTable();
        int ncol = resultTable.getColumnCount();
        for ( int ic = 0; ic < ncol; ic++ ) {
            String ref = resultTable.getColumnInfo( ic )
                        .getAuxDatumValue( VOStarTable.ID_INFO, String.class );
            if ( ref != null && ref.trim().length() > 0 ) {
                fieldIds.add( ref.trim() );
            }
        }

        /* Look at each service descriptor in turn. */
        Map<String,ServiceInvoker> invokerMap =
            new LinkedHashMap<String,ServiceInvoker>();
        for ( ServiceDescriptor sd : linksDoc.getServiceDescriptors() ) {
            boolean canInvoke = true;  // can it be turned into an invoker?
            String sdId = sd.getDescriptorId();
            String sdName = sdId == null ? "<unnamed>" : sdId;
            String accessUrl = sd.getAccessUrl();

            /* Check access URL (mandatory). */
            if ( accessUrl == null ) {
                String msg = new StringBuffer()
                    .append( "Missing accessURL PARAM for service descriptor " )
                    .append( sdName )
                    .toString();
                reporter_.report( DatalinkCode.E_SAC0, msg );
                canInvoke = false;
            }
            else {
                try {
                    new URL( accessUrl );
                }
                catch ( MalformedURLException e ) {
                    String msg = new StringBuffer()
                        .append( "Bad accessURL PARAM for service descriptor " )
                        .append( sdName )
                        .toString();
                    reporter_.report( DatalinkCode.E_SACB, msg );
                    canInvoke = false;
                }
            }

            /* Look at IVOID and standard ID. */
            String ivoid = sd.getResourceIdentifier();
            if ( ivoid != null ) {
                // try to look it up in the registry?
            }
            String standardId = sd.getStandardId();
            if ( standardId != null ) {
                // try to check it??
            }

            /* Look at all the input parameters. */
            for ( ServiceParam sp : sd.getInputParams() ) {

                /* Perform checks on params which reference FIELDs. */
                String ref = sp.getRef();
                if ( ref != null ) {
                    if ( ! fieldIds.contains( ref ) ) {
                        String msg = new StringBuffer()
                            .append( "No FIELD with ID='" )
                            .append( ref )
                            .append( "' required by inputParam " )
                            .append( sp.getName() )
                            .toString();
                        reporter_.report( DatalinkCode.E_SARF, msg );
                        canInvoke = false;
                    }
                    if ( sp.getValue() != null ||
                         sp.getMinMax() != null ||
                         sp.getOptions() != null ) {
                        String msg = new StringBuffer()
                            .append( "VALUES or @value are present " )
                            .append( "alongside @ref in inputParam " )
                            .append( sp.getName() )
                            .toString();
                        reporter_.report( DatalinkCode.W_SAVV, msg );
                    }
                }
            }

            /* Create a corresponding ServiceInvoker if possible. */
            ServiceInvoker invoker;
            try {
                invoker = new ServiceInvoker( sd, resultTable );
            }
            catch ( IOException e ) {
                invoker = null;
            }

            /* This checks our assumptions about what makes a ServiceDescriptor
             * too broken to turn into a ServiceInvoker; if this assertion
             * fails, check the logic above against ServiceInvoker
             * implementation. */
            assert canInvoke == ( invoker != null );

            /* Add to map if it has an ID. */
            if ( sdId != null ) {
                invokerMap.put( sdId, invoker );
            }
        }
        return invokerMap;
    }

    /**
     * Performs validation checks on each row of the results table of
     * a datalink document object.
     *
     * @param  linksDoc   representation of DataLink document
     * @param  invokerMap  map with an entry for each service descriptor;
     *                     key is descriptor ID, and value is ServiceInvoker
     *                     if one could be built, or null otherwise
     * @param  version   version of DataLink standard, not null
     * @throws  IOException   if there is a read error on the table
     */
    private void
            attemptValidateLinksDocRows( LinksDoc linksDoc,
                                         Map<String,ServiceInvoker> invokerMap,
                                         DatalinkVersion version )
            throws IOException {
        LinkColMap colMap = linksDoc.getColumnMap();
        Map<ServiceParam,String> noParams = new HashMap<ServiceParam,String>();

        /* Check requirements for each row. */
        int jrow = 0;
        RowSequence rseq = linksDoc.getResultTable().getRowSequence();
        while ( rseq.next() ) {
            jrow++;
            Object[] row = rseq.getRow();

            /* Check ID column. */
            String id = colMap.getId( row );
            if ( id == null ) {
                reporter_.report( DatalinkCode.E_DRID,
                                  "Missing ID for row " + jrow );
            }

            /* Check there is exactly one of access_url, service_def and
             * error_message columns. */
            String accessUrl = colMap.getAccessUrl( row );
            String serviceDef = colMap.getServiceDef( row );
            String errorMsg = colMap.getErrorMessage( row );
            int nrcol = ( accessUrl == null ? 0 : 1 )
                      + ( serviceDef == null ? 0 : 1 )
                      + ( errorMsg == null ? 0 : 1 );
            if ( nrcol != 1 ) {
                String msg = new StringBuffer()
                    .append( "Not exactly one non-null of " )
                    .append( "access_url (" )
                    .append( accessUrl )
                    .append( "), " )
                    .append( "service_def (" )
                    .append( serviceDef )
                    .append( "), " )
                    .append( "error_message (" )
                    .append( errorMsg )
                    .append( ")" )
                    .append( " at row " )
                    .append( jrow )
                    .toString();
                reporter_.report( DatalinkCode.E_DRR1, msg );
            }

            /* Check access_url content. */
            if ( accessUrl != null ) {
                try {
                    URL url = new URL( accessUrl );
                    String msg = new StringBuffer()
                        .append( "Row " )
                        .append( jrow )
                        .append( ": access URL " )
                        .append( url )
                        .toString();
                    reporter_.report( DatalinkCode.I_IKAC, msg );
                }
                catch ( MalformedURLException e ) {
                    String msg = new StringBuffer()
                        .append( "Bad access URL at row " )
                        .append( jrow )
                        .append( ": " )
                        .append( accessUrl )
                        .append( " (" )
                        .append( e )
                        .append( ")" )
                        .toString();
                    reporter_.report( DatalinkCode.E_DRUR, msg );
                }
            }

            /* Check error_message content. */
            if ( errorMsg != null ) {
                if ( ! FAULT_REGEX.matcher( errorMsg.trim() ).matches() ) {
                    String msg = new StringBuffer()
                        .append( "Bad error message syntax at row " )
                        .append( jrow )
                        .append( ": " )
                        .append( errorMsg )
                        .toString();
                    reporter_.report( DatalinkCode.E_DRER, msg );
                }
                String msg = new StringBuffer()
                    .append( "Row " )
                    .append( jrow )
                    .append( ": " )
                    .append( "error message \"" )
                    .append( errorMsg )
                    .append( "\"" )
                    .toString();
                reporter_.report( DatalinkCode.I_IKER, msg );
            }

            /* Check non-null service_def reference actually exists. */
            if ( serviceDef != null ) {

                /* Note the map may contain a null value for the given key;
                 * that means the definition existed but was broken. */
                if ( ! invokerMap.containsKey( serviceDef ) ) {
                    String msg = new StringBuffer()
                        .append( "No service descriptor resource with ID='" )
                        .append( serviceDef )
                        .append( "' at row " )
                        .append( jrow )
                        .toString();
                    reporter_.report( DatalinkCode.E_DRSI, msg );
                }
                ServiceInvoker invoker = invokerMap.get( serviceDef );
                if ( invoker != null ) {
                    URL url = invoker.getUrl( row, noParams );
                    String msg = new StringBuffer()
                        .append( "Row " )
                        .append( jrow )
                        .append( ": " )
                        .append( "service " )
                        .append( serviceDef )
                        .append( " invocation " )
                        .append( invoker.getUserParams().length == 0
                                     ? "(full) "
                                     : "(partial) " )
                        .append( url )
                        .toString();
                    reporter_.report( DatalinkCode.I_IKSD, msg );
                }
            }

            /* Check semantics column. */
            String semantics = colMap.getSemantics( row );
            if ( semantics == null ) {
                reporter_.report( DatalinkCode.E_DRS0,
                                  "Null semantics entry for row " + jrow );
            }
            else if ( semantics.startsWith( "#" ) ||
                      semantics.startsWith( SEMANTICS_CHECKER
                                           .getVocabularyUri() + "#" ) ) {
                String frag =
                    semantics.substring( semantics.indexOf( '#' ) + 1 );
                checkVocab( SEMANTICS_CHECKER, frag, "semantics", jrow,
                            DatalinkCode.W_SMCO, DatalinkCode.E_SMCO );
            }
            else {
                String msg = new StringBuffer()
                    .append( "Non-core semantics term at row " )
                    .append( jrow )
                    .append( ": " )
                    .append( semantics )
                    .toString();
                reporter_.report( DatalinkCode.I_SMCU, msg );
            }

            /* Check content_type column. */
            String contentType = colMap.getContentType( row );
            if ( contentType != null ) {
                ContentType mimeType =
                    ContentType.parseContentType( contentType );
                if ( mimeType == null ) {
                    String msg = new StringBuffer()
                        .append( "Bad MIME type syntax for content_type" )
                        .append( " at row " )
                        .append( jrow )
                        .append( ": " )
                        .append( contentType )
                        .toString();
                    reporter_.report( DatalinkCode.E_DRCT, msg );
                }
                else if ( contentType.toLowerCase().indexOf( "datalink" ) >= 0
                       && ! CANONICAL_DL_CTYPE.equals( contentType ) ) {
                    String msg = new StringBuffer()
                        .append( "content_type '" )
                        .append( contentType )
                        .append( "' contains 'datalink' but is not " )
                        .append( CANONICAL_DL_CTYPE )
                        .append( " at row " )
                        .append( jrow )
                        .toString();
                    reporter_.report( DatalinkCode.W_DRCT, msg );
                }
            }

            /* Check content_qualifier column. */
            if ( version.is11() ) {
                String cqual = colMap.getContentQualifier( row );
                if ( cqual == null ) {
                    // no problem, it's optional
                }
                else if ( cqual.startsWith( "#" ) ||
                          cqual.startsWith( CONTENTQUALIFIER_CHECKER
                                           .getVocabularyUri() + "#" ) ) {
                    String frag = cqual.substring( cqual.indexOf( '#' ) + 1 );
                    checkVocab( CONTENTQUALIFIER_CHECKER, frag,
                                "content_qualifier", jrow,
                                DatalinkCode.W_CQCO, DatalinkCode.E_CQCO );
                }
                else {
                    String msg = new StringBuffer()
                       .append( "Content qualifier from" )
                       .append( " non-standard vocabulary" )
                       .append( " at row " )
                       .append( jrow )
                       .append( ": " )
                       .append( cqual )
                       .toString();
                    reporter_.report( DatalinkCode.I_CQCU, msg );
                }
            }

            /* Check link_auth column. */
            if ( version.is11() ) {
                String linkAuth = colMap.getLinkAuth( row );
                if ( linkAuth != null &&
                     ! "false".equals( linkAuth ) &&
                     ! "true".equals( linkAuth ) &&
                     ! "optional".equals( linkAuth ) ) {
                    String msg = new StringBuffer()
                       .append( "Bad link_auth value \"" )
                       .append( linkAuth )
                       .append( "\", should be " )
                       .append( "\"false\", \"true\", \"optional\"" )
                       .append( " or null" )
                       .toString();
                    reporter_.report( DatalinkCode.E_LKAZ, msg );
                }
            }
        }
        rseq.close();
        reporter_.report( DatalinkCode.I_DLNR,
                          "Datalink table rows checked: " + jrow );
    }

    /**
     * Checks a string against terms in a VO Vocabulary, and issues
     * validation reports as appropriate.
     *
     * @param  checker  defines the vocabulary to check against
     * @param  term    term to check
     * @param  colName  name of datalink column in which term appears
     * @param  jrow    1-based index of row in which term appears
     * @param  warnCode  reporting code for deprecated/prelminary warnings
     * @param  errCode  reporting code for unknown terms
     */
    private void checkVocab( VocabChecker checker, String term,
                             final String colName, final int jrow,
                             DatalinkCode warnCode, DatalinkCode errCode ) {
        checker.checkTerm( term, new VocabChecker.TermReporter() {
            public void termFound() {
            }
            public void termPreliminary( String msg ) {
                reporter_.report( warnCode, message( "Preliminary", msg ) );
            }
            public void termDeprecated( String msg ) {
                reporter_.report( warnCode, message( "Deprecated", msg ) );
            }
            public void termUnknown( String msg ) {
                reporter_.report( errCode, message( "Unknown", msg ) );
            }
            private String message( String type, String msg ) {
                return new StringBuffer()
                      .append( type )
                      .append( ' ' )
                      .append( colName )
                      .append( " term at row " )
                      .append( jrow )
                      .append( ": " )
                      .append( msg )
                      .toString();
            }
        } );
    }

    /**
     * Performs validation checks on an input stream that is supposed to
     * contain a DALI/DataLink VOTable error document.
     * According to DataLink, a {links} error response in
     * default response format must be a VOTable document.
     *
     * @param   in   input stream assumed to contain DataLink error response
     * @param   url  input URL, used for error messages
     * @param   contentType  value of Content-Type header, if any
     */
    private void checkErrorVOTable( InputStream in, URL url,
                                    String contentType ) {

        /* Read the DOM, generating VOTable validation reports if successful. */
        final VODocument vodoc;
        try {
            vodoc = readVODocument( in );
        }
        catch ( IOException e ) {
            String msg = new StringBuffer()
                .append( "Error reading error VOTable response at " )
                .append( url )
                .append( " (" )
                .append( e )
                .append( ")" )
                .toString();
            reporter_.report( DatalinkCode.E_DCER, msg );
            return;
        }
        catch ( SAXException e ) {
            String msg = new StringBuffer()
                .append( "Badly-formed XML (not VOTable) at " )
                .append( url )
                .append( " (" )
                .append( e )
                .append( ")" )
                .toString();
            reporter_.report( DatalinkCode.E_ENVO, msg );
            return;
        }

        /* If it was a VOTable, check the content-type and
         * report any validation items. */
        checkVOTableContentType( contentType, url, false );

        /* Get unique RESOURCE/@type="results" element. */
        Map<String,List<VOElement>> resourceMap = getTypedResources( vodoc );
        if ( resourceMap == null ) {
            return;
        }
        List<VOElement> resultsResources = resourceMap.get( "results" );
        int nres = resultsResources.size();
        if ( nres != 1 ) {
            String msg = new StringBuffer()
                .append( nres == 0 ? "No "
                                   : "Multiple (" + nres + ") "  )
                .append( "<RESOURCE type='results'> elements " )
                .append( "in error VOTable" )
                .toString();
            reporter_.report( DatalinkCode.E_URES, msg );
            return;
        }
        VOElement resultsEl = resultsResources.get( 0 );

        /* Get unique INFO/@name="QUERY_STATUS" element. */
        List<VOElement> statusEls = new ArrayList<VOElement>();
        NodeList infoEls = resultsEl.getElementsByVOTagName( "INFO" );
        for ( int i = 0; i < infoEls.getLength(); i++ ) {
            VOElement infoEl = (VOElement) infoEls.item( i );
            if ( "QUERY_STATUS".equals( infoEl.getName() ) ) {
                statusEls.add( infoEl );
            }
        }
        int nstat = statusEls.size();
        if ( nstat != 1 ) {
            String msg = new StringBuffer()
                .append( nstat == 0 ? "No "
                                    : "Multiple (" + nstat + ")" )
                .append( "<INFO name='QUERY_STATUS'> elements " )
                .append( "in error VOTable" )
                .toString();
            reporter_.report( DatalinkCode.E_ERDC, msg );
            return;
        }
        VOElement statusEl = statusEls.get( 0 );

        /* Test status value represents an error. */
        String value = statusEl.hasAttribute( "value" )
                     ? statusEl.getAttribute( "value" )
                     : null;
        if ( ! "ERROR".equals( value ) ) {
            String msg = new StringBuffer()
                .append( "Element <INFO name='QUERY_STATUS'> " )
                .append( "in error document " )
                .append( "has non-ERROR value " )
                .append( value )
                .toString();
            reporter_.report( DatalinkCode.E_ERDC, msg );
        }

        /* Check the text content of the element, whose format is
         * constrained by DataLink sec 3.4. */
        String statusText = DOMUtils.getTextContent( statusEl );
        if ( statusText == null || statusText.trim().length() == 0 ) {
            String msg = new StringBuffer()
                .append( "Missing error message in " )
                .append( "<INFO name='QUERY_STATUS' value='ERROR'> element" )
                .toString();
            reporter_.report( DatalinkCode.E_ERDC, msg );
        }
        else if ( ! FAULT_REGEX.matcher( statusText.trim() ).matches() ) {
            String msg = new StringBuffer()
                .append( "Bad fault message syntax in " )
                .append( "<INFO name='QUERY_STATUS' value='ERROR'> element" )
                .toString();
            reporter_.report( DatalinkCode.E_ERDC, msg );
        }
    }

    /**
     * Perform checks on the HTTP Content-Type header from a
     * URL Connection supposed to contain a datalink result.
     * The declaration should report a VOTable document;
     * if the response is asserted to come from a DataLink {links} service,
     * additional constraints are applied to the form of the Content-type.
     *
     * @param   ctypeTxt   content-type header value
     * @param   url     URL of input, used for reports
     * @param   isLinksResult  true iff the connection is supposed to be
     *                         the successful result of a
     *                         DataLink {links} service
     */
    private void checkVOTableContentType( String ctypeTxt, URL url,
                                          boolean isLinksResult ) {

        /* Get the content-type header. */
        if ( ctypeTxt == null || ctypeTxt.trim().length() == 0 ) {
            reporter_.report( DatalinkCode.W_NOCT,
                              "No Content-Type header for " + url );
            return;
        }

        /* Parse it. */
        ContentType ctype = ContentType.parseContentType( ctypeTxt );
        if ( ctype == null ) {
            reporter_.report( DatalinkCode.E_DRCT,
                              "Invalid Content-Type header " + ctypeTxt
                            + " for " + url );
            return;
        }
        assert ctype != null;

        /* Perform checks on its parsed content. */
        /* DataLink sec 3.3 puts specific requirements on the form of the
         * content type. */
        if ( isLinksResult ) {

            if ( ! ctype.matches( "application", "x-votable+xml" ) ||
                 ! "datalink".equals( ctype.getParameter( "content" ) ) ) {
                String msg = new StringBuffer()
                   .append( "Incorrect Content-Type " )
                   .append( ctypeTxt )
                   .append( " for DataLink service " )
                   .append( url )
                   .append( "; should be like " )
                   .append( CANONICAL_DL_CTYPE )
                   .toString();
                reporter_.report( DatalinkCode.E_DLCT, msg );
            }
            else if ( ! CANONICAL_DL_CTYPE.equals( ctypeTxt ) ) {
                String msg = new StringBuffer()
                   .append( "Content-Type " )
                   .append( ctypeTxt )
                   .append( " differs from canonical form " )
                   .append( CANONICAL_DL_CTYPE )
                   .append( " for DataLink service " )
                   .append( url )
                   .toString();
                reporter_.report( DatalinkCode.W_DLCT, msg );
            }
        }

        /* Otherwise, just check it declares as a VOTable. */
        else {
            if ( ! ( ctype.matches( "text", "xml" ) ||
                     ctype.matches( "application", "x-votable+xml" ) ) ) {
                String msg = new StringBuilder()
                   .append( "Bad content type " )
                   .append( ctype )
                   .append( " for HTTP response which should contain " )
                   .append( "VOTable result or error document" )
                   .append( " (" )
                   .append( url )
                   .append( ")" )
                   .toString();
                reporter_.report( DatalinkCode.E_VTCT, msg );
            }
        }
    }

    /**
     * Determine compression as declared by an HTTP Content-Encoding header.
     * This also performs validation checks and reports appropriately.
     *
     * @param   conn   connection in an opened state
     * @return   compression type
     */
    private Compression getCompression( URLConnection conn ) {

        /* RFC2616 sec 3.5. */
        String cCoding = conn.getContentEncoding();
        final Compression compression;
        if ( cCoding == null || cCoding.trim().length() == 0
                             || "identity".equals( cCoding ) ) {
            compression = Compression.NONE;
        }
        else if ( "gzip".equals( cCoding ) || "x-gzip".equals( cCoding ) ) {
            compression = Compression.GZIP;
        }   
        else if ( "compress".equals( cCoding ) ||
                  "x-compress".equals( cCoding ) ) {
            compression = Compression.COMPRESS;
        }
        else {  
            reporter_.report( FixedCode.W_CEUK,
                              "Unknown Content-Encoding " + cCoding
                            + " for " + conn.getURL() );
            compression = Compression.NONE; 
        }
        if ( compression != Compression.NONE ) {
            reporter_.report( FixedCode.W_CEZZ,
                              "Compression with Content-Encoding " + cCoding
                            + " for " + conn.getURL() );
        }
        return compression;
    }

    /** Enumerates parameter types in the inputParameter GROUP. */
    private enum ParamType {
        FIXED,
        ROW,
        USER
    };
}
