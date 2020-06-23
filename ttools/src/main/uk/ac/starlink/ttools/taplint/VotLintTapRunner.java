package uk.ac.starlink.ttools.taplint;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.w3c.dom.Node;
import uk.ac.starlink.auth.AuthManager;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.ttools.votlint.SaxMessager;
import uk.ac.starlink.ttools.votlint.VersionDetector;
import uk.ac.starlink.ttools.votlint.VotLintContentHandler;
import uk.ac.starlink.ttools.votlint.VotLintContext;
import uk.ac.starlink.ttools.votlint.VotLinter;
import uk.ac.starlink.util.Compression;
import uk.ac.starlink.util.ContentCoding;
import uk.ac.starlink.util.DOMUtils;
import uk.ac.starlink.util.StarEntityResolver;
import uk.ac.starlink.vo.TapQuery;
import uk.ac.starlink.vo.UwsJob;
import uk.ac.starlink.vo.UwsJobInfo;
import uk.ac.starlink.votable.TableElement;
import uk.ac.starlink.votable.VODocument;
import uk.ac.starlink.votable.VOElement;
import uk.ac.starlink.votable.VOStarTable;
import uk.ac.starlink.votable.VOTableDOMBuilder;
import uk.ac.starlink.votable.VOTableVersion;

/**
 * TapRunner implementation which uses the VotLint validation classes
 * to check the query's result VOTable.
 *
 * @author   Mark Taylor
 * @since    10 Jun 2011
 */
public abstract class VotLintTapRunner extends TapRunner {

    private final boolean doChecks_;

    /** Result table parameter set if table was marked overflowed. */
    public static ValueInfo OVERFLOW_INFO =
        new DefaultValueInfo( "OVERFLOW", Boolean.class,
                              "Is TAP result overflow status set?" );

    /** Minimum VOTable version required by TAP (TAP 1.0 sec 2.9). */
    private static final VOTableVersion TAP_VOT_VERSION = VOTableVersion.V12;

    /**
     * Constructor.
     *
     * @param   name  name for this instance
     * @param   doChecks  true to perform various checks on the result VOTable
     *                    (including linting) and report them, false to be
     *                    mostly silent and only report serious errors
     */
    protected VotLintTapRunner( String name, boolean doChecks ) {
        super( name );
        doChecks_ = doChecks;
    }

    /**
     * Indicates if the given table, which must have been retrieved from
     * this object's {@link #readResultVOTable} method, was marked as
     * an overflow result.
     *
     * @param   table  TAP result table read by this object
     * @return  true iff overflow
     */
    public boolean isOverflow( StarTable table ) {
        DescribedValue ovParam =
            table.getParameterByName( OVERFLOW_INFO.getName() );
        if ( ovParam != null && ovParam.getValue() instanceof Boolean ) {
            return ((Boolean) ovParam.getValue()).booleanValue();
        }
        else {
            throw new IllegalArgumentException( "Not produced by me!" );
        }
    }

    /**
     * Execute a TAP query and return a URL connection giving its result.
     *
     * @param  reporter  validation message destination
     * @param  tq  query
     * @return   result data source
     */
    protected abstract URLConnection getResultConnection( Reporter reporter,
                                                          TapQuery tq )
            throws IOException;

    @Override
    protected StarTable executeQuery( Reporter reporter, TapQuery tq )
            throws IOException, SAXException {
        InputStream in = readResultInputStream( reporter, tq );
        VODocument doc = readResultDocument( reporter, in );
        return readResultVOTable( reporter, doc );
    }

    /**
     * Returns an input stream which should containing the result VOTable
     * from a TAP query, performing checks and making reports as appropriate
     * on the way.
     *
     * @param  reporter  validation message destination
     * @param  tq  query
     * @return  result input stream
     */
    public InputStream readResultInputStream( Reporter reporter, TapQuery tq )
            throws IOException, SAXException {
        URLConnection conn = getResultConnection( reporter, tq );
        conn = TapQuery.followRedirects( conn );
        conn.connect();
        String ctype = conn.getContentType();
        if ( doChecks_ ) {
            if ( ctype == null || ctype.trim().length() == 0 ) {
                reporter.report( FixedCode.W_NOCT,
                                 "No Content-Type header for "
                               + conn.getURL() );
            }
            else if ( ! ( ctype.startsWith( "text/xml" ) ||
                          ctype.startsWith( "application/xml" ) ||
                          ctype.startsWith( "application/x-votable+xml" ) ||
                          ctype.startsWith( "text/x-votable+xml" ) ) ) {
                String msg = new StringBuilder()
                   .append( "Bad content type " )
                   .append( ctype )
                   .append( " for HTTP response which should contain " )
                   .append( "VOTable result or error document" )
                   .append( " (" )
                   .append( conn.getURL() )
                   .append( ")" )
                   .toString();
                reporter.report( FixedCode.E_VOCT, msg );
            }
        }

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
            reporter.report( FixedCode.W_CEUK,
                             "Unknown Content-Encoding " + cCoding 
                           + " for " + conn.getURL() );
            compression = Compression.NONE;
        }
        if ( doChecks_ && compression != Compression.NONE ) {
            reporter.report( FixedCode.W_CEZZ,
                             "Compression with Content-Encoding " + cCoding
                           + " for " + conn.getURL() );
        }

        try {
            return compression.decompress( conn.getInputStream() );
        }
        catch ( IOException e ) {
            if ( conn instanceof HttpURLConnection ) {
                InputStream err = ((HttpURLConnection) conn).getErrorStream();
                if ( err != null ) {
                    return err;
                }
            }
            throw e;
        }
    }

    /**
     * Reads a TAP result VODocument from an input stream,
     * checking it and reporting messages as required.
     *
     * @param  reporter  validation message destination
     * @param  baseIn  VOTable input stream
     * @return VOTable-aware DOM
     */
    public VODocument readResultDocument( Reporter reporter,
                                          InputStream baseIn )
            throws IOException, SAXException {
        return readResultDocument( reporter, baseIn, doChecks_,
                                   TAP_VOT_VERSION );
    }

    /**
     * Utility method to read a VODocument from an input stream,
     * checking it and reporting messages as required.
     *
     * @param  reporter  validation message destination
     * @param  baseIn  VOTable input stream
     * @param   doChecks  true to perform various checks on the result VOTable
     *                    (including linting) and report them, false to be
     *                    mostly silent and only report serious errors
     * @param  minVotVersion  minimum required VOTable version;
     *                        may be null if any will do
     * @return VOTable-aware DOM
     */
    public static VODocument readResultDocument( Reporter reporter,
                                                 InputStream baseIn,
                                                 boolean doChecks,
                                                 VOTableVersion minVotVersion )
            throws IOException, SAXException {
        final VOTableVersion version;
        BufferedInputStream in = new BufferedInputStream( baseIn );
        String versionString = VersionDetector.getVersionString( in );
        if ( versionString == null ) {
            version = TAP_VOT_VERSION;
            reporter.report( FixedCode.I_VVNL,
                             "Undeclared VOTable version; assuming v"
                           + version );
        }
        else {
            Map<String,VOTableVersion> vmap = VOTableVersion.getKnownVersions();
            if ( vmap.containsKey( versionString ) ) {
                List<VOTableVersion> vlist =
                    new ArrayList<VOTableVersion>( vmap.values() );
                version = vmap.get( versionString );
                if ( minVotVersion != null &&
                     vlist.indexOf( version )
                     < vlist.indexOf( minVotVersion ) ) {
                    reporter.report( FixedCode.E_VVLO,
                                     "Declared VOTable version " + versionString
                                   + "<" + minVotVersion );
                }
            }
            else {
                version = TAP_VOT_VERSION;
                reporter.report( FixedCode.I_VVUN,
                                 "Unknown declared VOTable version '"
                               + versionString + "' - assuming v" + version );
            }
        }

        /* Set up a SAX event handler to create a DOM. */
        VOTableDOMBuilder domHandler =
            new VOTableDOMBuilder( StoragePolicy.getDefaultPolicy(), true );

        /* Set up a parser which will feed SAX events to the dom builder,
         * and may or may not generate logging messages through the
         * reporter as it progresses. */
        final XMLReader parser;
        if ( doChecks ) {
            SaxMessager messager = new ReporterSaxMessager( reporter );
            VotLintContext vlContext =
                new VotLintContext( version, true, messager );

            /* Unit and UCD checks will be performed explicitly elsewhere;
             * including those reports from every returned VOTable
             * would unnecessarily clutter the output, so turn them off. */
            vlContext.setCheckUcd( false );
            vlContext.setCheckUnit( false );
            parser = new VotLinter( vlContext ).createParser( domHandler );
        }
        else {
            parser = createBasicParser( reporter, domHandler );
        }

        /* Perform the parse and retrieve the resulting DOM. */
        parser.parse( new InputSource( in ) );
        return domHandler.getDocument();
    }

    /**
     * Reads a VOTable result from a DOM that has come from a TAP result
     * document.
     * The resulting table will have the {@link #OVERFLOW_INFO} parameter
     * present and set/unset appropriately.
     *
     * @param  reporter  validation message destination
     * @param  doc   VOTable-aware DOM
     * @return  table result
     */
    private StarTable readResultVOTable( Reporter reporter, VODocument doc )
            throws IOException {
        VOElement resultsEl = getResultsResourceElement( reporter, doc );

        /* Look for preceding and possible trailing QUERY_STATUS INFOs
         * and the TABLE. */
        VOElement preStatusInfo = null;
        VOElement postStatusInfo = null;
        TableElement tableEl = null;
        for ( Node node = resultsEl.getFirstChild(); node != null;
              node = node.getNextSibling() ) {
            if ( node instanceof VOElement ) {
                VOElement el = (VOElement) node;
                String name = el.getVOTagName();
                boolean isStatusInfo =
                    "INFO".equals( name ) &&
                    "QUERY_STATUS".equals( el.getAttribute( "name" ) );
                boolean isTable =
                    "TABLE".equals( name );
                if ( isStatusInfo ) {
                    if ( tableEl == null ) {
                        if ( preStatusInfo == null ) {
                            preStatusInfo = el;
                        }
                        else if ( doChecks_ ) {
                            reporter.report( FixedCode.E_QST1,
                                             "Multiple pre-table INFOs with "
                                           + "name='QUERY_STATUS'" );
                        }
                    }
                    else {
                        if ( postStatusInfo == null ) {
                            postStatusInfo = el;
                        }
                        else if ( doChecks_ ) {
                            reporter.report( FixedCode.E_QST2,
                                             "Multiple post-table INFOs with "
                                           + "name='QUERY_STATUS'" );
                        }
                    }
                }
                if ( isTable ) {
                    if ( tableEl == null ) {
                        tableEl = (TableElement) el;
                    }
                    else {
                        reporter.report( FixedCode.E_TTOO,
                                         "Multiple TABLEs in results "
                                       + "RESOURCE" );
                    }
                }
            }
        }

        /* Check pre-table status INFO. */
        boolean overflow = false;
        final String preStatus;
        if ( preStatusInfo == null ) {
            preStatus = null;
            if ( doChecks_ ) {
                reporter.report( FixedCode.E_NOST,
                                 "Missing <INFO name='QUERY_STATUS'> element "
                               + "before TABLE" );
            }
        }
        else {
            preStatus = preStatusInfo.getAttribute( "value" );
            if ( "ERROR".equals( preStatus ) ) {
                String err = DOMUtils.getTextContent( preStatusInfo );
                if ( err == null || err.trim().length() == 0 ) {
                    if ( doChecks_ ) {
                        reporter.report( FixedCode.W_NOMS,
                                         "<INFO name='QUERY_STATUS' "
                                       + "value='ERROR'> "
                                       + "element has no message content" );
                    }
                    err = "Unknown TAP result error";
                }
                throw new IOException( "Service error: \"" + err + "\"" );
            }
            else if ( "OVERFLOW".equals( preStatus ) ) {
                overflow = true;
            }
            else if ( "OK".equals( preStatus ) ) {
                // ok
            }
            else {
                if ( doChecks_ ) {
                    String msg = new StringBuffer()
                        .append( "Pre-table QUERY_STATUS INFO " )
                        .append( "has unknown value " )
                        .append( preStatus )
                        .append( " is not OK/ERROR/OVERFLOW" )
                        .toString();
                    reporter.report( FixedCode.E_DQUS, msg );
                }
            }
        }

        /* Check post-table status INFO. */
        if ( postStatusInfo != null ) {
            String postStatus = postStatusInfo.getAttribute( "value" );
            if ( "ERROR".equals( postStatus ) ) {
                String err = DOMUtils.getTextContent( postStatusInfo );
                if ( err == null || err.trim().length() == 0 ) {
                    if ( doChecks_ ) {
                        reporter.report( FixedCode.W_NOMS,
                                         "<INFO name='QUERY_STATUS' "
                                       + "value='ERROR'> "
                                       + "element has no message content" );
                    }
                    err = "Unknown TAP result error";
                }
                throw new IOException( err );
            }
            else if ( "OVERFLOW".equals( postStatus ) ||
                      "OK".equals( postStatus ) ) {
                boolean isOver = "OVERFLOW".equals( postStatus );
                if ( doChecks_ ) {
                    if ( postStatus.equals( preStatus ) ) {
                        reporter.report( FixedCode.I_DQUR,
                                         "Redundant post-table repetition of "
                                       + "pre-table " + preStatus + " status" );
                    }
                    if ( !isOver && "OVERFLOW".equals( preStatus ) ) {
                        reporter.report( FixedCode.E_DQUM,
                                         "Pre-table status OVERFLOW "
                                       + "contradicts post-table status OK" );
                    }
                }
                overflow = overflow || isOver;
            }
            else {
                if ( doChecks_ ) {
                    String msg = new StringBuffer()
                        .append( "Post-table QUERY_STATUS INFO " )
                        .append( "has unknown value " )
                        .append( postStatus )
                        .append( " is not OK/ERROR/OVERFLOW" )
                        .toString();
                    reporter.report( FixedCode.W_DQU2, msg );
                }
            }
        }

        /* Return table if present. */
        if ( tableEl == null ) {
            throw new IOException( "No TABLE element in results RESOURCE" );
        }
        StarTable table = new VOStarTable( tableEl );
        table.setParameter( new DescribedValue( OVERFLOW_INFO,
                                                Boolean.valueOf( overflow ) ) );
        return table;
    }

    /**
     * Returns the RESOURCE element marked with type="results" from a given
     * VODocument, or the best guess at it.
     *
     * @param  reporter   validation message destination
     * @param  doc   TAP result DOM
     */
    public VOElement getResultsResourceElement( Reporter reporter,
                                                VODocument doc )
            throws IOException {

        /* Check the top-level element. */
        VOElement voEl = (VOElement) doc.getDocumentElement();
        if ( ! "VOTABLE".equals( voEl.getVOTagName() ) ) {
            String msg = new StringBuffer()
               .append( "Top-level element of result document is " )
               .append( voEl.getTagName() )
               .append( " not VOTABLE" )
               .toString();
            throw new IOException( msg );
        }

        /* Attempt to find the results element. */
        VOElement[] resourceEls = voEl.getChildrenByName( "RESOURCE" );
        for ( int ie = 0; ie < resourceEls.length; ie++ ) {
            VOElement el = resourceEls[ ie ];
            if ( "results".equals( el.getAttribute( "type" ) ) ) {
                return el;
            }
        }
        if ( resourceEls.length == 1 ) {
            if ( doChecks_ ) {
                reporter.report( FixedCode.E_RRES,
                                 "TAP response document RESOURCE element "
                               + "is not marked type='results'" );
            }
            return resourceEls[ 0 ];
        }
        else {
            throw new IOException( "No RESOURCE with type='results'" );
        }
    }

    /**
     * Returns a basic SAX parser that uses a supplied content handler.
     * Any SAX messages are ignored, not reported.
     *
     * <p>If the SAX parser can't be created, an error will be reported
     * and an exception will be thrown.
     *
     * @param  reporter  validation message destination
     * @return  basically configured SAX parser
     */
    private static XMLReader createBasicParser( Reporter reporter,
                                                ContentHandler contentHandler )
            throws SAXException {

        /* Create an XMLReader. */
        final XMLReader parser;
        try {
            SAXParserFactory spfact = SAXParserFactory.newInstance();
            spfact.setValidating( false );
            spfact.setNamespaceAware( true );
            parser = spfact.newSAXParser().getXMLReader();
        }
        catch ( ParserConfigurationException e ) {
            reporter.report( FixedCode.F_CAPC,
                             "Trouble setting up XML parse", e );
            throw (SAXException) new SAXException( e.getMessage() )
                                .initCause( e );
        }

        /* Configure it as required. */
        parser.setContentHandler( contentHandler );
        parser.setEntityResolver( StarEntityResolver.getInstance() );
        parser.setErrorHandler( new ErrorHandler() {
            public void warning( SAXParseException err ) {}
            public void error( SAXParseException err ) {}
            public void fatalError( SAXParseException err ) {}
        } );
        return parser;
    }

    /**
     * Returns a new instance which uses HTTP POST to make synchronous queries.
     *
     * @param  doChecks   true for detailed VOTable checking
     * @return  new TapRunner
     */
    public static VotLintTapRunner createPostSyncRunner( boolean doChecks ) {
        return new VotLintTapRunner( "sync POST", doChecks ) {
            @Override
            protected URLConnection getResultConnection( Reporter reporter,
                                                         TapQuery tq )
                    throws IOException {
                return UwsJob.postForm( tq.getTapService().getSyncEndpoint(),
                                        ContentCoding.NONE,
                                        tq.getStringParams(),
                                        tq.getStreamParams() );
            }
        };
    }

    /**
     * Returns a new instance which uses HTTP GET to make synchronous queries.
     *
     * @param  doChecks   true for detailed VOTable checking
     * @return  new TapRunner
     */
    public static VotLintTapRunner
            createGetSyncRunner( final boolean doChecks ) {
        return new VotLintTapRunner( "sync GET", doChecks ) {
            @Override
            protected URLConnection getResultConnection( Reporter reporter,
                                                         TapQuery tq )
                    throws IOException {
                URL syncEndpoint = tq.getTapService().getSyncEndpoint();
                if ( tq.getStreamParams() == null ||
                     tq.getStreamParams().isEmpty() ) {
                    String ptxt =
                        new String( UwsJob
                                   .toPostedBytes( tq.getStringParams() ),
                                    "utf-8" );
                    URL qurl = new URL( syncEndpoint + "?" + ptxt );
                    if ( doChecks ) {
                        reporter.report( FixedCode.I_QGET,
                                         "Query GET URL: " + qurl );
                    }
                    return AuthManager.getInstance().connect( qurl );
                }
                else {
                    return UwsJob.postForm( syncEndpoint, ContentCoding.NONE,
                                            tq.getStringParams(),
                                            tq.getStreamParams() );
                }
            }
        };
    }

    /**
     * Returns a new instance which makes asynchronous queries.
     * This instance does not do exhaustive validation.
     *
     * @param  pollMillis  polling interval in milliseconds
     * @param  doChecks   true for detailed VOTable checking
     * @return  new TapRunner
     */
    public static VotLintTapRunner createAsyncRunner( final long pollMillis,
                                                      boolean doChecks ) {
        return new VotLintTapRunner( "async", doChecks ) {
            @Override
            protected URLConnection getResultConnection( Reporter reporter,
                                                         TapQuery tq )
                    throws IOException {
                UwsJob uwsJob =
                    UwsJob.createJob( tq.getTapService().getAsyncEndpoint()
                                                        .toString(),
                                      tq.getStringParams(),
                                      tq.getStreamParams() );
                URL jobUrl = uwsJob.getJobUrl();
                reporter.report( FixedCode.I_QJOB,
                                 "Submitted query at " + jobUrl );
                uwsJob.start();
                UwsJobInfo info;
                try {
                    info = uwsJob.waitForFinish( pollMillis );
                }
                catch ( InterruptedException e ) {
                    throw (IOException)
                          new InterruptedIOException( "interrupted" )
                         .initCause( e );
                }
                String phase = info.getPhase();
                if ( "COMPLETED".equals( phase ) ) {
                    uwsJob.setDeleteOnExit( true );
                    return AuthManager.getInstance()
                          .connect(  new URL( jobUrl + "/results/result" ) );
                }
                else if ( "ERROR".equals( phase ) ) {
                    return AuthManager.getInstance()
                          .connect( new URL( jobUrl + "/error" ) );
                }
                else {
                    throw new IOException( "Unexpected UWS phase " + phase );
                }
            }
        };
    }
}
