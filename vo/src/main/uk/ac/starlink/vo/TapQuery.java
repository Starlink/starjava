package uk.ac.starlink.vo;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import uk.ac.starlink.auth.AuthManager;
import uk.ac.starlink.auth.Redirector;
import uk.ac.starlink.auth.UrlConnector;
import uk.ac.starlink.table.ByteStore;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.storage.DiscardByteStore;
import uk.ac.starlink.table.storage.LimitByteStore;
import uk.ac.starlink.util.ContentCoding;
import uk.ac.starlink.util.DOMUtils;
import uk.ac.starlink.util.HeadBufferInputStream;
import uk.ac.starlink.util.URLUtils;
import uk.ac.starlink.votable.DataFormat;
import uk.ac.starlink.votable.TableElement;
import uk.ac.starlink.votable.VOElement;
import uk.ac.starlink.votable.VOElementFactory;
import uk.ac.starlink.votable.VOStarTable;
import uk.ac.starlink.votable.VOTableVersion;
import uk.ac.starlink.votable.VOTableWriter;

/**
 * Represents a query to be made to a Table Access Protocol service.
 * This class aggregates the information which needs to be sent to
 * make such a query and provides methods to submit the query
 * synchronously or asynchronously.
 * It also contains some static methods to perform other TAP-related operations.
 *
 * @author   Mark Taylor
 * @since    8 Apr 2011
 * @see <a href="http://www.ivoa.net/Documents/TAP/">IVOA TAP Recommendation</a>
 */
public class TapQuery {

    private final TapService service_;
    private final String adql_;
    private final Map<String,String> stringMap_;
    private final Map<String,HttpStreamParam> streamMap_;
    private final long uploadLimit_;
    private static final Redirector redir303_ = 
        Redirector.createStandardInstance( new int[] { 303 } );
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Default VOTable serialization format for uploading VOTables
     * to a TAP service.
     * The value is currently
     * {@link uk.ac.starlink.votable.DataFormat#TABLEDATA}.
     * BINARY would be more efficient and ought to be OK,
     * but at time of writing at least CADC, and under some circumstances
     * other services, work properly with TABLEDATA but not BINARY
     * uploaded tables, so for now conform to the lowest common denominator.
     */
    public static final DataFormat DFLT_UPLOAD_SER = DataFormat.TABLEDATA;

    /**
     * Private constructor, performs common initialisation and
     * invoked by public constructors.
     *
     * @param  service   TAP service description
     * @param  adql   text of ADQL query
     * @param  extraParams  key->value map for optional parameters;
     * @param  uploadLimit  maximum number of bytes that may be uploaded;
     */
    private TapQuery( TapService service, String adql,
                      Map<String,String> extraParams, long uploadLimit ) {
        service_ = service;
        adql_ = adql;
        uploadLimit_ = uploadLimit;

        /* Prepare the parameter maps. */
        stringMap_ = new LinkedHashMap<String,String>();
        if ( ! service.getTapVersion().is11() ) {
            stringMap_.put( "REQUEST", "doQuery" );
        }
        stringMap_.put( "LANG", "ADQL" );
        stringMap_.put( "QUERY", adql );
        if ( extraParams != null ) {
            stringMap_.putAll( extraParams );
        }
        streamMap_ = new LinkedHashMap<String,HttpStreamParam>();
    }

    /**
     * Constructs a query with no uploaded tables.
     *
     * @param  service   TAP service description
     * @param  adql   text of ADQL query
     * @param  extraParams  key-&gt;value map for optional parameters;
     *                      if any of these match the names of standard
     *                      parameters (upper case) the standard values will
     *                      be overwritten, so use with care (may be null)
     */
    public TapQuery( TapService service, String adql,
                     Map<String,String> extraParams ) {
        this( service, adql, extraParams, -1 );
    }

    /**
     * Constructs a query with uploaded tables.
     * May throw an IOException if the tables specified for
     * upload exceed the stated upload limit.
     *
     * @param  service   TAP service description
     * @param  adql   text of ADQL query
     * @param  extraParams  key-&gt;value map for optional parameters;
     *                      if any of these match the names of standard
     *                      parameters (upper case) the standard values will
     *                      be overwritten, so use with care (may be null)
     * @param  uploadMap  name-&gt;table map of tables to be uploaded to
     *                    the service for the query (may be null)
     * @param  uploadLimit  maximum number of bytes that may be uploaded;
     *                      if negative, no limit is applied,
     *                      ignored if <code>uploadMap</code> null or empty
     * @param  vowriter   serializer for producing content of uploaded tables;
     *                    ignored if <code>uploadMap</code> null or empty,
     *                    if null a default value is used
     * @throws   IOException   if upload tables exceed the upload limit
     */
    public TapQuery( TapService service, String adql,
                     Map<String,String> extraParams,
                     Map<String,StarTable> uploadMap,
                     long uploadLimit, VOTableWriter vowriter )
            throws IOException {
        this( service, adql, extraParams, uploadLimit );

        /* Prepare the map of streamed parameters, required for table uploads.
         * This also affects the string parameter map. */
        StringBuffer ubuf = new StringBuffer();
        if ( uploadMap != null ) {
            if ( vowriter == null ) {
                vowriter = new VOTableWriter( DFLT_UPLOAD_SER, true,
                                              VOTableVersion.V12 );
            }
            for ( Map.Entry<String,StarTable> upload : uploadMap.entrySet() ) {
                String tname = upload.getKey();
                String tlabel = toParamLabel( tname );
                StarTable table = upload.getValue();
                if ( ubuf.length() != 0 ) {
                    ubuf.append( ';' );
                }
                ubuf.append( tname )
                    .append( ',' )
                    .append( "param:" )
                    .append( tlabel );
                HttpStreamParam streamParam =
                    createUploadStreamParam( table, uploadLimit, vowriter );
                streamMap_.put( tlabel, streamParam );
                logger_.info( "Preparing upload parameter " + tlabel
                            + " using VOTable serializer " + vowriter );
            }
        }
        if ( ubuf.length() > 0 ) {
            stringMap_.put( "UPLOAD", ubuf.toString() );
        }
    }

    /**
     * Convenience constructor that uses a URL rather than a TapService object.
     * This just uses {@link TapServices#createDefaultTapService}
     * and then invokes one of the other constructors.
     *
     * <p>This form is mildly deprecated, it is preferred to create
     * your own TapService as above and submit that to one of the
     * other constructors instead.  It's present because other classes
     * rely on it, but there is no intention to add URL-based
     * constructors corresponding to the other TapService-based forms.
     *
     * @param  serviceUrl   base URL of TAP service
     * @param  adql   text of ADQL query
     * @param  extraParams  key-&gt;value map for optional parameters;
     *                      if any of these match the names of standard
     *                      parameters (upper case) the standard values will
     *                      be overwritten, so use with care (may be null)
     */
    public TapQuery( URL serviceUrl, String adql,
                     Map<String,String> extraParams ) {
        this( TapServices.createDefaultTapService( serviceUrl ),
              adql, extraParams );
    }

    /**
     * Returns the text of the ADQL query for this object.
     *
     * @return  ADQL query text
     */
    public String getAdql() {
        return adql_;
    }

    /**
     * Returns the TAP service object to which this query will be submitted.
     *
     * @return  service locations
     */
    public TapService getTapService() {
        return service_;
    }

    /**
     * Returns the map of string parameters to be passed to the TAP service.
     *
     * @return   name-&gt;value map for TAP string parameters
     */
    public Map<String,String> getStringParams() {
        return stringMap_;
    }

    /**
     * Returns the map of streamed parameters to be passed to the TAP service.
     *
     * @return  name-&gt;value map for TAP stream parameters
     */
    public Map<String,HttpStreamParam> getStreamParams() {
        return streamMap_;
    }

    /**
     * Executes this query synchronously and returns the resulting table.
     *
     * @param  storage  storage policy for caching table data
     * @param  coding   configures HTTP compression
     * @return   result table
     */
    public StarTable executeSync( StoragePolicy storage, ContentCoding coding )
            throws IOException {
        return readResultVOTable( createSyncConnection( coding ), coding,
                                  storage );
    }

    /**
     * Executes this query synchronously and streams the resulting table
     * to a table sink.
     * If the result is a TAP error document, it will be presented as
     * an exception thrown from this method.
     * Overflow status of a successful result is provided by the return value.
     *
     * @param  sink  table destination
     * @param  coding   configures HTTP compression
     * @return   true iff the result was marked as overflowed
     */
    public boolean executeSync( TableSink sink, ContentCoding coding )
            throws IOException, SAXException {
        return streamResultVOTable( createSyncConnection( coding ), coding,
                                    sink );
    }

    /**
     * Opens a URL connection for the result of synchronously executing
     * this query.
     *
     * @param   coding  HTTP content-coding policy
     *                  result should be interpreted with same coding
     * @return   HTTP connection containing query result
     */
    public HttpURLConnection createSyncConnection( ContentCoding coding )
            throws IOException {
        URL url = service_.getSyncEndpoint();
        if ( url == null ) {
            throw new IOException( "No known sync endpoint?" );
        }
        return UwsJob.postForm( url, coding, stringMap_, streamMap_ );
    }

    /**
     * Submits this query asynchronously and returns the corresponding UWS job.
     * The job is not started.
     *
     * @return   new UWS job for this query
     */
    public UwsJob submitAsync() throws IOException {
        URL url = service_.getAsyncEndpoint();
        if ( url == null ) {
            throw new IOException( "No known async endpoint?" );
        }
        try {
            return UwsJob.createJob( url.toString(), stringMap_, streamMap_ );
        }
        catch ( UwsJob.UnexpectedResponseException e ) {
            int code;
            try {
                code = e.getConnection().getResponseCode();
            }
            catch ( Throwable e2 ) {
                code = -1;
            }
            if ( code != 401 && code != 403 ) {
                throw asIOException( e, "Synchronous might work?" );
            }
            else {
                throw e;
            }
        }
    }

    /**
     * Blocks until the TAP query represented by a given UWS job has completed,
     * then returns the URL from which the successful result can be obtained.
     * If the job does not complete successfully, an IOException is thrown
     * instead.
     *
     * @param  uwsJob  started UWS job representing an async TAP query
     * @param  pollMillis  polling interval in milliseconds
     * @return   open URL connection to result stream
     */
    public static URL waitForResultUrl( UwsJob uwsJob, long pollMillis )
            throws IOException, InterruptedException {
        UwsJobInfo info = uwsJob.waitForFinish( pollMillis );
        String phase = info.getPhase();
        assert UwsStage.forPhase( phase ) == UwsStage.FINISHED;
        if ( "COMPLETED".equals( phase ) ) {
            return URLUtils.newURL( uwsJob.getJobUrl() + "/results/result" );
        }
        else if ( "ABORTED".equals( phase ) ) {
            throw new IOException( "TAP query did not complete ("
                                 + phase + ")" );
        }
        else if ( "ERROR".equals( phase ) ) {
            String errText = null;

            /* We read the error text from the /error job resource,
             * which TAP says has to be a VOTable.  This is always
             * present.  In some cases it might be better to read
             * from the errorSummary element of the job resource
             * (e.g. xpath /job/errorSummary/message/text()), since
             * that might contain a shorter and more human-friendly
             * message, but (a) many/most TAP servers do not supply
             * this at time of writing and (b) it's difficult to
             * navigate the UWS document in the presence of namespaces
             * which often refer to different/older versions of the
             * UWS protocol. */
            URL errUrl = URLUtils.newURL( uwsJob.getJobUrl() + "/error" );
            logger_.info( "Read error VOTable from " + errUrl );
            try {
                errText = readErrorInfo( AuthManager.getInstance()
                                                    .openStream( errUrl ) );
            }
            catch ( Throwable e ) {
                throw (IOException)
                      new IOException( "TAP Execution error"
                                     + " (can't get detail)" )
                     .initCause( e );
            }
            throw new IOException( "TAP execution error: " + errText );
        }
        else {
            throw new IOException( "Unknown UWS execution phase " + phase );
        }
    }

    /**
     * Blocks until the TAP query represented by a given UWS job has completed,
     * then returns a table based on the result.
     * In case of job failure, an exception will be thrown instead.
     *
     * @param  uwsJob  started UWS job representing an async TAP query
     * @param  coding  configures HTTP compression
     * @param  storage  storage policy for caching table data
     * @param  pollMillis  polling interval in milliseconds
     * @return  result table
     */
    public static StarTable waitForResult( UwsJob uwsJob, ContentCoding coding,
                                           StoragePolicy storage,
                                           long pollMillis )
            throws IOException, InterruptedException {
        URL resultUrl;
        try {
            resultUrl = waitForResultUrl( uwsJob, pollMillis );
        }
        catch ( UwsJob.UnexpectedResponseException e ) {
            throw asIOException( e, null );
        }
        return readResultVOTable( AuthManager.getInstance()
                                             .connect( resultUrl, coding ),
                                  coding, storage );
    }

    /**
     * Reads and returns the table that resulted from a successful TAP query,
     * represented by a given UWS job.  The query is assumed to have
     * requested output in VOTable format.
     * If the job has not reached COMPLETED phase, an IOException will result.
     *
     * @param  uwsJob  successfully completed UWS job representing 
     *                 an async TAP query
     * @param  coding  configures HTTP compression
     * @param  storage  storage policy for caching table data
     * @return   the result of reading the TAP result as a table
     */
    public static StarTable getResult( UwsJob uwsJob, ContentCoding coding,
                                       StoragePolicy storage )
            throws IOException {
        URL url = URLUtils.newURL( uwsJob.getJobUrl() + "/results/result" );
        return readResultVOTable( AuthManager.getInstance()
                                             .connect( url, coding ),
                                  coding, storage );
    }

    /**
     * Utility method to obtain a single-cell table as the result of a
     * synchronous TAP query.
     *
     * @param   service   TAP service description
     * @param   adql   query string
     * @param   clazz   class of required value
     * @return   single value, or null if no rows
     * @throws   IOException  if required result cannot be got
     */
    public static <T> T scalarQuery( TapService service, String adql,
                                     Class<T> clazz )
            throws IOException {
        TapQuery tq = new TapQuery( service, adql, null );
        StarTable result = tq.executeSync( StoragePolicy.PREFER_MEMORY,
                                           ContentCoding.NONE );
        int ncol = result.getColumnCount();
        if ( ncol != 1 ) {
            throw new IOException( "Unexpected column count: "
                                 + ncol + " != 1" );
        }
        result = Tables.randomTable( result );
        long nrow = result.getRowCount();
        if ( nrow == 0 ) {
            return null;
        }
        else if ( nrow == 1 ) {
            Object cell = result.getCell( 0, 0 );
            if ( cell == null || clazz.isInstance( cell ) ) {
                @SuppressWarnings("unchecked")
                T tcell = clazz.cast( cell );
                return tcell;
            }
            else {
                throw new IOException( "Unexpected type "
                                     + cell.getClass().getName() + " not "
                                     + clazz.getName() );
            }
        }
        else {
            throw new IOException( "Unexpected row count: " + nrow + " > 0 " );
        }
    }

    /**
     * Reads the error text as encoded in a TAP VOTable error document.
     *
     * @return   plain text error message
     */
    private static String readErrorInfo( InputStream in ) throws IOException {
        try {
            Document errDoc = DocumentBuilderFactory.newInstance()
                             .newDocumentBuilder()
                             .parse( new BufferedInputStream( in ) );
            XPath xpath = XPathFactory.newInstance().newXPath();
            return xpath.evaluate( "VOTABLE/RESOURCE[@type='results']"
                                 + "/INFO[@name='QUERY_STATUS']/text()",
                                   errDoc );
        }
        catch ( ParserConfigurationException e ) {
            throw (IOException) new IOException( "Error doc parse failure" )
                               .initCause( e );
        }
        catch ( SAXException e ) {
            throw (IOException) new IOException( "Error doc parse failure" )
                               .initCause( e );
        }
        catch ( XPathException e ) {
            throw (IOException) new IOException( "Error doc parse failure" )
                               .initCause( e );
        }
    }

    /**
     * Takes an UnexpectedResponseException generated in response to a TAP
     * query and turns it into an IOException with a helpful error message.
     * The main thing to do is to take the body content, which ought(?) to
     * be an error-bearing VOTable, and extract the error text.
     *
     * @param  error  input error
     * @param  extra   extra text to append to message; may be null
     * @return   better error
     */
    private static IOException
            asIOException( UwsJob.UnexpectedResponseException error,
                           String extra ) {
        HttpURLConnection hconn = error.getConnection();

        /* Get an input stream for the response body.  Depending on the
         * response code HttpURLConnection may make this available as
         * the input or error stream. */
        InputStream bodyIn;
        try {
            bodyIn = hconn.getInputStream();
        }
        catch ( IOException e ) {
            bodyIn = hconn.getErrorStream();
        }

        /* Try to turn the response body into an intelligible error
         * message */
        String errMsg = null;
        if ( bodyIn != null ) {
            try {
                errMsg = readErrorInfo( bodyIn );
            }
            catch ( IOException e ) {
            }
        }

        /* Fall back to the cause's message if necessary. */
        if ( errMsg == null || errMsg.length() == 0 ) {
            errMsg = error.getMessage();
        }

        /* Add extra text if requested. */
        if ( extra != null && extra.length() > 0 ) {
            errMsg += " -  " + extra;
        }

        /* Return an exception with the correct type, message and cause. */
        return (IOException) new IOException( errMsg ).initCause( error );
    }

    /**
     * Returns a label to be used for identifying a table which has been
     * assigned a given name.  This function could return the input string,
     * but doesn't just to make it clearer what's going on with the POSTed
     * parameters.
     *
     * @param   assigned table name
     * @return  label for identifying table in posted query
     */
    private static String toParamLabel( String tname ) {
        return "upload_" + tname.replaceAll( "[^a-zA-Z0-9]", "" );
    }

    /**
     * Creates a new stream parameter based on a given table.
     *
     * @param   table  table to upload
     * @param   uploadLimit  maximum number of bytes permitted; -1 if no limit
     * @param   vowriter   serializer for producing content of uploaded tables
     * @return  stream parameter
     */
    private static HttpStreamParam
                   createUploadStreamParam( final StarTable table,
                                            long uploadLimit,
                                            final VOTableWriter vowriter )
            throws IOException {
        final Map<String,String> headerMap = new LinkedHashMap<String,String>();
        headerMap.put( "Content-Type", "application/x-votable+xml" );
        if ( uploadLimit < 0 ) {
            return new HttpStreamParam() {
                public Map<String,String> getHttpHeaders() {
                    return headerMap;
                }
                public void writeContent( OutputStream out )
                        throws IOException {
                    vowriter.writeStarTable( table, out );
                }
                public long getContentLength() {
                    return -1;
                }
            };
        }

        /* If there's an upload limit, write the data to a limited-size
         * buffer which will throw an IOException if the limit is exceeded.
         * The written bytes are discarded at this stage.  We could take
         * the opportunity to cache the resulting output so we didn't
         * have to regenerate it later, but likely the effort taken to
         * regenerate it is not enough to warrant the potential
         * inconvenience of caching the bytes. */
        else {
            final ByteStore hbuf =
                new LimitByteStore( new DiscardByteStore(), uploadLimit );
            OutputStream tout = hbuf.getOutputStream();
            vowriter.writeStarTable( table, tout );
            tout.close();
            final long count = hbuf.getLength();
            assert count <= uploadLimit;
            return new HttpStreamParam() {
                public Map<String,String> getHttpHeaders() {
                    return headerMap;
                }
                public void writeContent( OutputStream out )
                        throws IOException {
                    vowriter.writeStarTable( table, out );
                }
                public long getContentLength() {
                    return count;
                }
            };
        }
    }

    /**
     * Reads a VOTable which may represent a successful result or an error.
     * If it represents an error (in accordance with the TAP rules for
     * expressing this), an exception will be thrown.
     *
     * @param   conn  connection to table resource
     * @param  coding  HTTP content coding policy used to prepare connection
     * @param  storage  storage policy
     * @return   table result of successful query
     */
    public static StarTable readResultVOTable( URLConnection conn,
                                               ContentCoding coding,
                                               StoragePolicy storage )
            throws IOException {

        /* Get input stream. */
        int headSize = 2048;
        HeadBufferInputStream in =
            new HeadBufferInputStream( getVOTableStream( conn, coding ),
                                       headSize );

        /* Read the result as a VOTable DOM. */
        VOElement voEl;
        try {
            voEl = new VOElementFactory( storage )
                  .makeVOElement( in, conn.getURL().toString() );
        }
        catch ( SAXException e ) {
            String msg =
                createBadResponseMessage( "TAP response is not a VOTable", in );
            throw (IOException) new IOException( msg ).initCause( e );
        }
        finally {
            in.close();
        }

        /* Navigate the DOM to find the status and table of interest. */
        VOElement[] resourceEls = voEl.getChildrenByName( "RESOURCE" );
        VOElement resultsEl = null;
        for ( int ie = 0; ie < resourceEls.length; ie++ ) {
            VOElement el = resourceEls[ ie ];
            if ( "results".equals( el.getAttribute( "type" ) ) ) {
                resultsEl = el;
            }
        }
        if ( resultsEl == null ) {
            if ( resourceEls.length == 1 ) {
                resultsEl = resourceEls[ 0 ];
                logger_.warning( "TAP response document RESOURCE element "
                               + "not marked type='results'" );
            }
            else {
                String tagName = voEl.getTagName();
                String txt = "VOTABLE".equalsIgnoreCase( tagName )
                           ? "No RESOURCE with type='results'"
                           : "TAP response is not VOTable (" + tagName + ")";
                String msg = createBadResponseMessage( txt, in );
                throw new IOException( createBadResponseMessage( txt, in ) );
            }
        }
        VOElement[] infoEls = resultsEl.getChildrenByName( "INFO" );
        VOElement statusEl = null;
        for ( int ie = 0; statusEl == null && ie < infoEls.length; ie++ ) {
            VOElement el = infoEls[ ie ];
            if ( "QUERY_STATUS".equals( el.getAttribute( "name" ) ) ) {
                statusEl = el;
            }
        }
        String status = statusEl != null ? statusEl.getAttribute( "value" )
                                         : null;
        if ( "ERROR".equals( status ) ) {
            throw new IOException( DOMUtils.getTextContent( statusEl ) );
        }
        else {
            if ( ! "OK".equals( status ) ) {
                logger_.warning( "Missing/incorrect <INFO name='QUERY_STATUS'>"
                               + " element in TAP response" );
            }
            TableElement tableEl =
                (TableElement) resultsEl.getChildByName( "TABLE" );
            if ( tableEl == null ) {
                String msg = "No TABLE in results resource";
                throw new IOException( createBadResponseMessage( msg, in ) );
            }
            return new VOStarTable( tableEl );
        }
    }

    /**
     * Streams a VOTable document which may represent a successful result
     * or an error.
     * If it represents an error (in accordance with the TAP rules for
     * expressing this), an exception will be thrown.
     * Overflow status of a successful result is provided by the return value.
     *
     * <p><strong>Note:</strong> any XML that comes after the TABLE
     * element of the result table is ignored for the purposes of
     * reporting the table metadata.  The only thing after the end
     * of the TABLE that affects the result of this method is the
     * overflow flag, which affects the return value.
     * So if you need to pick up items which might be in trailing elements,
     * for instance Service Descriptors in later RESOURCE elements,
     * you will have to use a different method.
     *
     * @param   conn  connection to table resource
     * @param  coding  HTTP content coding policy used to prepare connection
     * @param   sink   destination for table result of succesful query
     * @return   true iff the result was marked as overflowed
     */
    public static boolean streamResultVOTable( URLConnection conn,
                                               ContentCoding coding,
                                               TableSink sink )
            throws IOException, SAXException {
        InputStream in = getVOTableStream( conn, coding );
        boolean overflow =
            DalResultStreamer.streamResultTable( new InputSource( in ), sink );
        return overflow;
    }

    /**
     * Gets an input stream from a URL connection that should contain
     * a VOTable.
     *
     * @param  conn  connection to result of TAP service call
     * @param  coding  HTTP content coding policy used to prepare connection
     * @return  stream containing a response table (error or result)
     */
    public static InputStream getVOTableStream( URLConnection conn,
                                                ContentCoding coding )
            throws IOException {

        /* Follow 303 redirects as required. */
        conn = AuthManager.getInstance()
              .followRedirects( conn, coding, redir303_ );

        /* Get an input stream representing the content of the resource.
         * HttpURLConnection may provide this from the getInputStream or
         * getErrorStream method, depending on the response code. */
        try { 
            return coding.getInputStream( conn );
        }
        catch ( IOException e ) {

            /* In case of an error (non-200 response code), the connection's
             * error stream really should contain a VOTable.
             * But sometimes it doesn't, either because of incorrect
             * implementation or because the error is generated at the
             * HTTP rather than TAP level.  If the content-type looks like
             * it is a VOTable, return it in a stream.
             * Otherwise, grab all the useful information from
             * the connection and bundle it up as the message content
             * of an IOException. */
            InputStream errStrm = coding.getErrorStream( conn );
            if ( isVOTableType( conn.getContentType() ) && errStrm != null ) {
                return errStrm;
            }
            else {
                StringBuffer sbuf = new StringBuffer()
                    .append( "Non-VOTable service error response" );
                if ( conn instanceof HttpURLConnection ) {
                    HttpURLConnection hconn = (HttpURLConnection) conn;
                    sbuf.append( " " )
                        .append( hconn.getResponseCode() )
                        .append( ": " )
                        .append( hconn.getResponseMessage() );
                }
                if ( errStrm != null ) {
                    byte[] buf = new byte[ 2048 ];
                    int count = errStrm.read( buf );
                    if ( count > 0 ) {
                        sbuf.append( " - " )
                            .append( new String( buf, 0, count, "UTF-8" ) );
                        if ( errStrm.read() >= 0 ) {
                            sbuf.append( " ..." );
                        }
                    }
                    try {
                        errStrm.close();
                    }
                    catch ( IOException e2 ) {
                        // never mind
                    }
                }
                throw (IOException) new IOException( sbuf.toString() )
                                   .initCause( e );
            }
        }
    }

    /**
     * Returns a string characterising a bad TAP response.
     * As well as a supplied text message, this includes at least part of
     * the actual response text; this is likely to be fairly incomprehensible
     * to a normal user, but it probably makes sense to an expert and is
     * in any case more use than no indication at all.
     *
     * @param  txt   user-readable explanation of what went wrong
     * @param  in    caching input stream withing which response was sought
     * @return   full bad response message; this may be quite long,
     *           but is limited to HeadBufferInputStream buffer size
     */
    private static String createBadResponseMessage( String txt,
                                                    HeadBufferInputStream in ) {
        StringBuffer sbuf = new StringBuffer( txt );
        byte[] buf = in.getHeadBuffer();
        int nb = Math.min( (int) in.getReadCount(), buf.length );
        if ( nb > 0 ) {
            String bufstr;
            try {
                bufstr = new String( buf, 0, nb, "UTF-8" );
            }
            catch ( UnsupportedEncodingException e ) {
                assert false;
                bufstr = new String( buf, 0, nb );
            }
            sbuf.append( " - " )
                .append( bufstr );
            if ( nb == buf.length ) {
                sbuf.append( " ..." );
            }
        }
        return sbuf.toString();
    }

    /**
     * Tries to determines whether a MIME type (probably) indicates VOTable
     * content or not.
     *
     * @param  contentType  content-type string
     * @return  false if the contentType really doesn't look like VOTable
     */
    private static boolean isVOTableType( String contentType ) {
        if ( contentType == null ) {
            return true;
        }
        String ctype = contentType.trim().toLowerCase();
        return ctype.indexOf( "xml" ) >= 0
            || ctype.indexOf( "votable" ) >= 0;
    }

    /**
     * Takes a URLConnection and repeatedly follows 303 redirects
     * until a non-303 status is achieved.  Infinite loops are defended
     * against.  The Accept-Encoding header, if present, is propagated
     * to redirect targets.
     *
     * @param  conn   initial URL connection
     * @return   target URL connection
     *           (if no redirects, the same as <code>hconn</code>)
     */
    public static URLConnection followRedirects( URLConnection conn )
            throws IOException {
        return AuthManager.getInstance() 
              .followRedirects( conn, (UrlConnector) null, redir303_ );
    }
}
