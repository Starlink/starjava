package uk.ac.starlink.vo;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathFactory;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import uk.ac.starlink.table.ByteStore;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.storage.DiscardByteStore;
import uk.ac.starlink.table.storage.LimitByteStore;
import uk.ac.starlink.util.DOMUtils;
import uk.ac.starlink.votable.DataFormat;
import uk.ac.starlink.votable.TableElement;
import uk.ac.starlink.votable.VOElement;
import uk.ac.starlink.votable.VOElementFactory;
import uk.ac.starlink.votable.VOStarTable;
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

    private final URL serviceUrl_;
    private final String adql_;
    private final Map<String,String> stringMap_;
    private final Map<String,HttpStreamParam> streamMap_;
    private final long uploadLimit_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Constructor.  May throw an IOException if the tables specified for
     * upload exceed the stated upload limit.
     *
     * @param  serviceUrl  base service URL for TAP service
     *                     (excluding "/[a]sync")
     * @param  adql   text of ADQL query
     * @param  extraParams  key->value map for optional parameters;
     *                      if any of these match the names of standard
     *                      parameters (upper case) the standard values will
     *                      be overwritten, so use with care (may be null)
     * @param  uploadMap  name->table map of tables to be uploaded to
     *                    the service for the query (may be null)
     * @param  uploadLimit  maximum number of bytes that may be uploaded;
     *                      if negative, no limit is applied,
     *                      ignored if <code>uploadMap</code> null or empty
     */
    public TapQuery( URL serviceUrl, String adql,
                     Map<String,String> extraParams,
                     Map<String,StarTable> uploadMap, long uploadLimit )
            throws IOException {
        serviceUrl_ = serviceUrl;
        adql_ = adql;
        uploadLimit_ = uploadLimit;

        /* Prepare the map of string parameters. */
        stringMap_ = new LinkedHashMap<String,String>();
        stringMap_.put( "REQUEST", "doQuery" );
        stringMap_.put( "LANG", "ADQL" );
        stringMap_.put( "QUERY", adql );
        if ( extraParams != null ) {
            stringMap_.putAll( extraParams );
        }

        /* Prepare the map of streamed parameters, required for table uploads.
         * This also affects the string parameter map. */
        streamMap_ = new LinkedHashMap<String,HttpStreamParam>();
        StringBuffer ubuf = new StringBuffer();
        if ( uploadMap != null ) {
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
                    createUploadStreamParam( table, uploadLimit );
                streamMap_.put( tlabel, streamParam );
            }
        }
        if ( ubuf.length() > 0 ) {
            stringMap_.put( "UPLOAD", ubuf.toString() );
        }
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
     * Returns the TAP service URL to which this query will be submitted.
     *
     * @return  serviceUrl
     */
    public URL getServiceUrl() {
        return serviceUrl_;
    }

    /**
     * Returns the map of string parameters to be passed to the TAP service.
     *
     * @return   name->value map for TAP string parameters
     */
    public Map<String,String> getStringParams() {
        return stringMap_;
    }

    /**
     * Returns the map of streamed parameters to be passed to the TAP service.
     *
     * @return  name->value map for TAP stream parameters
     */
    public Map<String,HttpStreamParam> getStreamParams() {
        return streamMap_;
    }

    /**
     * Executes this query synchronously and returns the resulting table.
     *
     * @param  storage  storage policy for caching table data
     * @return   result table
     */
    public StarTable executeSync( StoragePolicy storage ) throws IOException {
        HttpURLConnection hconn =
            UwsJob.postForm( new URL( serviceUrl_ + "/sync" ),
                             stringMap_, streamMap_ );
        return readResultVOTable( hconn, storage );
    }

    /**
     * Submits this query asynchronously and returns the corresponding UWS job.
     * The job is not started.
     *
     * @return   new UWS job for this query
     */
    public UwsJob submitAsync() throws IOException {
        try {
            return UwsJob.createJob( serviceUrl_ + "/async",
                                     stringMap_, streamMap_ );
        }
        catch ( UwsJob.UnexpectedResponseException e ) {
            throw asIOException( e, "Synchronous might work?" );
        }
    }

    /**
     * Blocks until the TAP query represented by a given UWS job has completed,
     * then returns a table based on the result.
     * In case of job failure, an exception will be thrown instead.
     *
     * @param  uwsJob  started UWS job representing an async TAP query
     * @param  storage  storage policy for caching table data
     * @param  pollMillis  polling interval in milliseconds
     * @return  result table
     */
    public static StarTable waitForResult( UwsJob uwsJob, StoragePolicy storage,
                                           long pollMillis )
            throws IOException, InterruptedException {
        try {
            String phase = uwsJob.waitForFinish( pollMillis );
            assert UwsStage.forPhase( phase ) == UwsStage.FINISHED;
            if ( "COMPLETED".equals( phase ) ) {
                return getResult( uwsJob, storage );
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
                URL errUrl = new URL( uwsJob.getJobUrl() + "/error" );
                logger_.info( "Read error VOTable from " + errUrl );
                try {
                    errText = readErrorInfo( errUrl.openStream() );
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
        catch ( UwsJob.UnexpectedResponseException e ) {
            throw asIOException( e, null );
        }
    }

    /**
     * Reads and returns the table that resulted from a successful TAP query,
     * represented by a given UWS job.  The query is assumed to have
     * requested output in VOTable format.
     * If the job has not reached COMPLETED phase, an IOException will result.
     *
     * @param  uwsJob  successfully completed UWS job representing 
     *                 an async TAP query
     * @param  storage  storage policy for caching table data
     * @return   the result of reading the TAP result as a table
     */
    public static StarTable getResult( UwsJob uwsJob, StoragePolicy storage )
            throws IOException {
        URL url = new URL( uwsJob.getJobUrl() + "/results/result" );
        return readResultVOTable( url.openConnection(), storage );
    }

    /**
     * Reads table metadata from a TAP service.
     *
     * @param  serviceUrl  base TAP service URL
     * @return   table metadata
     */
    public static TableMeta[] readTableMetadata( URL serviceUrl )
            throws IOException, SAXException {
        URL turl = new URL( serviceUrl + "/tables" );
        logger_.info( "Reading table metadata from " + turl );
        return TableSetSaxHandler.readTableSet( turl );
    }

    /**
     * Reads capability information from a TAP service.
     *
     * @param  serviceUrl  base TAP service URL
     * @return   capability information
     */
    public static TapCapability readTapCapability( URL serviceUrl )
            throws IOException, SAXException {
        URL curl = new URL( serviceUrl + "/capabilities" );
        logger_.info( "Reading capability metadata from " + curl );
        return TapCapability.readTapCapability( curl );
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
     * @return  stream parameter
     */
    private static HttpStreamParam
                   createUploadStreamParam( final StarTable table,
                                            long uploadLimit )
            throws IOException {
        final Map<String,String> headerMap = new LinkedHashMap<String,String>();
        final VOTableWriter vowriter =
            new VOTableWriter( DataFormat.BINARY, true );
        vowriter.setVotableVersion( "1.2" );
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
     * @param  storage  storage policy
     */
    public static StarTable readResultVOTable( URLConnection conn,
                                               StoragePolicy storage )
            throws IOException {

        /* Follow 303 redirects as required. */
        conn = followRedirects( conn );

        /* Get an input stream representing the content of the resource.
         * HttpURLConnection may provide this from the getInputStream or
         * getErrorStream method, depending on the response code. */
        InputStream in = null;
        try { 
            in = conn.getInputStream();
        }
        catch ( IOException e ) {
            if ( conn instanceof HttpURLConnection ) {
                in = ((HttpURLConnection) conn).getErrorStream();
            }
            if ( in == null ) {
                throw e;
            }
        }

        /* Read the result as a VOTable DOM. */
        VOElement voEl;
        try {
            voEl = new VOElementFactory( storage )
                  .makeVOElement( in, conn.getURL().toString() );
        }
        catch ( SAXException e ) {
            throw (IOException)
                  new IOException( "TAP response is not a VOTable" )
                 .initCause( e );
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
                throw new IOException( "No RESOURCE with type='results'" );
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
                throw new IOException( "No TABLE in results resource" );
            }
            return new VOStarTable( tableEl );
        }
    }

    /**
     * Takes a URLConnection and repeatedly follows 303 redirects
     * until a non-303 status is achieved.  Infinite loops are defended
     * against.
     *
     * @param  conn   initial URL connection
     * @return   target URL connection
     *           (if no redirects, the same as <code>hconn</code>)
     */
    public static URLConnection followRedirects( URLConnection conn )
            throws IOException {
        if ( ! ( conn instanceof HttpURLConnection ) ) {
            return conn;
        }
        HttpURLConnection hconn = (HttpURLConnection) conn;
        Set urlSet = new HashSet<String>();
        urlSet.add( hconn.getURL() );
        while ( hconn.getResponseCode() ==
                HttpURLConnection.HTTP_SEE_OTHER ) {   // 303
            URL url0 = hconn.getURL();
            String loc = hconn.getHeaderField( "Location" );
            if ( loc == null || loc.trim().length() == 0 ) {
                throw new IOException( "No Location field for 303 response"
                                     + " from " + url0 );
            }
            URL url1;
            try {
                url1 = new URL( loc );
            }
            catch ( MalformedURLException e ) {
                throw (IOException)
                      new IOException( "Bad Location field for 303 response"
                                     + " from " + url0 )
                     .initCause( e );
            }
            if ( ! urlSet.add( url1 ) ) {
                throw new IOException( "Recursive 303 redirect at " + url1 );
            }
            logger_.info( "HTTP 303 redirect to " + url1 );
            URLConnection conn1 = url1.openConnection();
            if ( ! ( conn1 instanceof HttpURLConnection ) ) {
                return conn1;
            }
            hconn = (HttpURLConnection) conn1; 
        }
        return hconn;
    }
}
