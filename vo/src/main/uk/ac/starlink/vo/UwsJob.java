package uk.ac.starlink.vo;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.xml.sax.SAXException;
import uk.ac.starlink.auth.AuthConnection;
import uk.ac.starlink.auth.AuthContext;
import uk.ac.starlink.auth.AuthManager;
import uk.ac.starlink.auth.AuthUtil;
import uk.ac.starlink.auth.Redirector;
import uk.ac.starlink.auth.UrlConnector;
import uk.ac.starlink.table.ByteStore;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.util.ContentCoding;
import uk.ac.starlink.util.URLUtils;

/**
 * Job submitted using the Universal Worker Service pattern.
 * Instances of this class represent UWS jobs which have been created.
 *
 * @author   Mark Taylor
 * @since    18 Jan 2011
 * @see <a href="http://www.ivoa.net/Documents/UWS/">IVOA UWS Recommendation</a>
 */
public class UwsJob {

    private final URL jobUrl_;
    private UwsJobInfo info_;
    private boolean deleteAttempted_;
    private Thread deleteThread_;
    private List<JobWatcher> watcherList_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );
    private static final String UTF8 = "UTF-8";

    /** Chunk size for HTTP transfer encoding; if &lt;=0, don't chunk. */
    public static int HTTP_CHUNK_SIZE = 1024 * 1024;

    /**
     * Whether to trim whitespace from line text responses (like job/phase).
     * I'm not sure whether (trailing) whitespace is permitted in service
     * responses in this context, but the ESAC GACS service appends "\r\n"
     * to its phase endpoint result.
     * I asked (17-Sep-2014) on the grid@ivoa.net mailing list what's the
     * right answer, but no response, so accept trailing whitespace for now.
     */
    public static boolean TRIM_TEXT = true;

    /**
     * Constructor.
     *
     * @param   jobUrl  the UWS {jobs}/(job-id) URL containing
     *                  the details of this job
     */
    public UwsJob( URL jobUrl ) {
        jobUrl_ = jobUrl;
        watcherList_ = new ArrayList<JobWatcher>();
    }

    /**
     * Returns the URL for this job.  This will normally be a child of the
     * job list URL, and contains a representation of the job state,
     * as well as providing the base URL for further access to the job.
     *
     * @return   job URL
     */
    public URL getJobUrl() {
        return jobUrl_;
    }

    /**
     * Adds a callback which will be invoked whenever this job's phase
     * is found to have changed.  Note that the runnable will not
     * in general be invoked from the AWT event dispatch thread.
     *
     * @param  watcher  runnable to be notified on job phase change
     */
    public void addJobWatcher( JobWatcher watcher ) {
        watcherList_.add( watcher );
    }

    /**
     * Removes a callback previously added by {@link #addJobWatcher}.
     * Has no effect if <code>watcher</code> is not currently registered.
     *
     * @param   watcher  runnable to be removed
     */
    public void removeJobWatcher( JobWatcher watcher ) {
        watcherList_.remove( watcher );
    }

    /**
     * Returns the most recently read job state.
     * Invoking this method does not cause the state to be read.
     *
     * @return   job state object
     */
    public UwsJobInfo getLastInfo() {
        return info_;
    }

    /**
     * Posts a phase for this job.
     *
     * @param  phase  UWS job phase to assign
     */
    public void postPhase( String phase ) throws IOException {
        postUwsParameter( "/phase", "PHASE", phase );
    }

    /**
     * Posts a value of the destruction time for this job.
     * The service is not obliged to accept it; completion without error
     * does not necessarily mean that it has done.
     * May not work after job has started.
     *
     * @param  epoch   destruction time which should be an ISO-8601 string;
     *                 it is passed directly to the service
     */
    public void postDestruction( String epoch ) throws IOException {
        postUwsParameter( "/destruction", "DESTRUCTION", epoch );
    }

    /**
     * Posts a value of the execution duration parameter for this job.
     * The service is not obliged to accept it; completion without error
     * does not necessarily mean that it has done.
     * May not work after job has started.
     *
     * @param   nsec   number of elapsed seconds for which job is permitted
     *                 to run; zero is supposed to mean unlimited
     */
    public void postExecutionDuration( long nsec ) throws IOException {
        postUwsParameter( "/executionduration", "EXECUTIONDURATION",
                          Long.toString( nsec ) );
    }

    /**
     * Posts a parameter value to this UWS job.
     *
     * @param   relativeLocation  parameter posting endpoint relative to
     *                            this job's endpoint (include a leading "/"
     *                            if it's a sub-resource)
     * @param   paramName   name of job parameter
     * @param   paramValue  new value of job parameter
     */
    private void postUwsParameter( String relativeLocation, String paramName,
                                   String paramValue )
            throws IOException {
        URL postUrl = URLUtils.newURL( jobUrl_ + relativeLocation );
        HttpURLConnection hconn = postForm( postUrl, paramName, paramValue );
        int code = hconn.getResponseCode();
        if ( code != HttpURLConnection.HTTP_SEE_OTHER ) {
            throw new IOException( "Non-303 response: " + code + " " +
                                   hconn.getResponseMessage() );
        }
    }

    /**
     * Starts the job by posting the RUN phase.
     *
     * @throws   UnexpectedResponseException  if HTTP responses other than
     *           UWS mandated ones occur
     */
    public void start() throws IOException {
        postPhase( "RUN" );
    }

    /**
     * Blocks until the job has reached a completion phase.
     * Depending on the service's capabilities, this may be done
     * using polling or a blocking call.
     *
     * @param   pollMillis   polling time in milliseconds to assess
     *                       job completion, if polling is required
     * @return   job info corresponding to a completion state
     * @throws   UnexpectedResponseException  if HTTP responses other than
     *           UWS mandated ones occur
     */
    public UwsJobInfo waitForFinish( final long pollMillis )
            throws IOException, InterruptedException {
        UwsJobInfo info = info_;
        if ( info == null ) {
            info = readInfo();
        }
        while ( UwsStage.forPhase( info.getPhase() ) != UwsStage.FINISHED ) {
            info = rereadInfo( info, pollMillis );
            String phase = info.getPhase();
            switch ( UwsStage.forPhase( phase ) ) {
                case UNSTARTED:
                    throw new IOException( "Job not started"
                                         + " - phase: " + phase );
                case ILLEGAL:
                    throw new IOException( "Illegal UWS job phase: " + phase );
                case UNKNOWN:
                    logger_.info( "Unknown UWS phase " + phase + " reported"
                                + "; poll again" );
                    break;
                case RUNNING:
                    break;
                case FINISHED:
                    break;
                default:
                    throw new AssertionError();
            }
        }
        return info;
    }

    /**
     * Reads the current status document for this job from the server
     * and both stores and returns the result.
     * The result becomes the new value of the {@link #getLastInfo} method.
     *
     * @return  job status
     */
    public UwsJobInfo readInfo() throws IOException {
        return readInfoQuery( "" );
    }

    /**
     * Makes a blocking call to read the current status document for this job
     * from the server
     * and both stores and returns the result.
     * The result becomes the new value of the {@link #getLastInfo} method.
     *
     * @param   timeoutSec  maximum advised timeout in seconds
     * @param   lastInfo    last known job status
     * @return  job status
     */
    public UwsJobInfo readInfoBlocking( int timeoutSec, UwsJobInfo lastInfo )
            throws IOException {
        StringBuffer qbuf = new StringBuffer()
            .append( "?WAIT=" )
            .append( timeoutSec );
        String lastPhase = lastInfo == null ? null : lastInfo.getPhase();
        UwsStage lastStage = lastPhase == null
                           ? null
                           : UwsStage.forPhase( lastPhase );
        if ( lastStage == UwsStage.RUNNING ||
             lastStage == UwsStage.UNSTARTED ) {
            qbuf.append( "&PHASE=" )
                .append( lastPhase );
        }
        return readInfoQuery( qbuf.toString() );
    }

    /**
     * Reads the current status document for this job from the server,
     * given the results of a previous successful such read.
     * Certain failures will be tolerated, on the ground that if it's
     * worked once, it's reasonable to suppose that it might do again
     * in the future, even if it doesn't do so every time.
     *
     * <p>This is particularly to defend against something like a
     * temporary network outage or server reset, which in the context
     * of a UWS job might reasonably represent only a temporary issue.
     * 
     * @param  lastInfo   successfully 
     */
    private UwsJobInfo rereadInfo( UwsJobInfo lastInfo, long pollMillis )
            throws IOException, InterruptedException {
        boolean useBlocking = hasBlocking( lastInfo );
        while ( true ) {
            boolean hasWaited = false;
            try {
                if ( useBlocking ) {
                    logger_.info( "Blocking read of UWS job" );
                    return readInfoBlocking( -1, lastInfo );
                }
                else {
                    logger_.info( "Poll UWS job after " + pollMillis + "ms" );
                    Thread.sleep( pollMillis );
                    hasWaited = true;
                    return readInfo();
                }
            }

            /* Probably I ought also to catch HTTP 500s and
             * Retry-After-bearing HTTP 503s here (see RFC2616).
             * Maybe some other things too. But pulling out HTTP
             * response codes (and headers) is not so easy,
             * so wait until somebody complains before doing it. */
            catch ( IOException e ) {
                if ( e instanceof SocketException ||
                     e instanceof UnknownHostException ) {
                    String msg = "Connection failure - keep trying"
                               + " (" + e + ")";
                    logger_.log( Level.WARNING, msg, e );
                    if ( ! hasWaited ) {
                        Thread.sleep( pollMillis );
                    }
                }
                else {
                    throw e;
                }
            }
        }
    }

    /**
     * Reads the current status document for this job from the server,
     * appending a supplied query string to the basic job URL,
     * and both stores and returns the result.
     * The result becomes the new value of the {@link #getLastInfo} method.
     *
     * @param   queryPart   text to be appended to job URL before submission
     * @return  job status
     */
    private UwsJobInfo readInfoQuery( String queryPart ) throws IOException {
        URL url = URLUtils.newURL( jobUrl_ + queryPart );
        logger_.info( "Read UWS job: " + url );
        UwsJobInfo[] infos;
        try {
            infos = JobSaxHandler.readJobInfos( url );
        }
        catch ( SAXException e ) {
            throw (IOException)
                  new IOException( "Parse error in UWS job document at " + url )
                 .initCause( e );
        }
        if ( infos != null && infos.length > 0 ) {
            UwsJobInfo info = infos[ 0 ];
            gotInfo( info );
            return info;
        }
        else {
            throw new IOException( "No UWS job document at " + url );
        }
    }

    /**
     * Performs housekeeping duties on a newly acquired job status document;
     * logs the fact, stores it for later use, and notifies watchers.
     *
     * @param   info   job status
     */
    private void gotInfo( final UwsJobInfo info ) {
        UwsJobInfo info0 = info_;
        String phase0 = info0 == null ? null : info0.getPhase();
        String phase1 = info == null ? null : info.getPhase();
        info_ = info;
        if ( TRIM_TEXT ) {
            phase0 = phase0 == null ? null : phase0.trim();
            phase1 = phase1 == null ? null : phase1.trim();
        }
        logger_.info( "UWS job phase: " + phase1 );
        boolean phaseChanged = phase1 == null ? phase0 != null
                                              : ! phase1.equals( phase0 );
        if ( phaseChanged ) {
            for ( JobWatcher watcher : watcherList_ ) {
                watcher.jobUpdated( this, info );
            }
        }   
    }

    /**
     * Posts deletion of this job to the server.
     *
     * @throws  IOException  if job deletion failed for some reason
     */
    public void postDelete() throws IOException {
        UrlConnector connector = hconn -> {
            hconn.setRequestMethod( "DELETE" );
            hconn.setInstanceFollowRedirects( false );
            hconn.connect();
        };
        final int response;
        logger_.info( "DELETE " + jobUrl_ );
        try {
            URLConnection conn = 
                AuthManager.getInstance()
               .connect( jobUrl_, connector, Redirector.NO_REDIRECT );
            if ( conn instanceof HttpURLConnection ) {
                HttpURLConnection hconn = (HttpURLConnection) conn;
                response = hconn.getResponseCode();
            }
            else {
                throw new IOException( "Can't POST to non-HTTP URL "
                                     + jobUrl_ );
            }
        }
        finally {
            synchronized ( this ) {
                deleteAttempted_ = true;
            }
        }
        int tapDeleteCode = HttpURLConnection.HTTP_SEE_OTHER; // 303
        if ( response != tapDeleteCode ) {
            throw new IOException( "Response " + response + " not "
                                 + tapDeleteCode );
        }
    }

    /**
     * Attempts to delete this query's UWS job.
     * This may harmlessly be called multiple times; calls following the
     * first one have no effect.
     * In case of failure a message is logged through the logging system.
     */
    public void attemptDelete() {
        synchronized ( this ) {
            if ( deleteAttempted_ ) {
                return;
            }
            else {
                deleteAttempted_ = true;
            }
            setDeleteOnExit( false );
        }
        try {
            postDelete();
            logger_.info( "UWS job " + jobUrl_ + " deleted" );
        }
        catch ( IOException e ) {
            logger_.warning( "UWS job deletion failed for " + jobUrl_
                           + " - " + e.toString() );
        }
    }

    /**
     * Determines whether this job will be deleted when the JVM exits,
     * if it has not been deleted before.
     *
     * @param  delete  true to delete on exit, false otherwise
     */
    public synchronized void setDeleteOnExit( boolean delete ) {
        if ( delete && deleteThread_ == null && ! deleteAttempted_ ) {
            deleteThread_ = new Thread( "UWS job deletion" ) {
                public void run() {
                    synchronized ( UwsJob.this ) {
                        deleteThread_ = null;
                    }
                    attemptDelete();
                }
            };
            Runtime.getRuntime().addShutdownHook( deleteThread_ );
        }
        else if ( ! delete && deleteThread_ != null ) {
            Runtime.getRuntime().removeShutdownHook( deleteThread_ );
            deleteThread_ = null;
        }
    }

    /**
     * Indicates whether this job will be deleted when the JVM exits,
     * if it has not been deleted before.
     *
     * @return   true iff delete on exit
     */
    public boolean getDeleteOnExit() {
        return deleteThread_ != null;
    }

    /**
     * Returns the server-assigned job-id for this job.
     * It is the final part of the Job URL.
     *
     * @return   job ID
     */
    public String getJobId() {
        return jobUrl_.getPath().replaceAll( "^.*/", "" );
    }

    public String toString() {
        return getJobId();
    }

    /**
     * Submits a job to a UWS service and returns a new UwsJob object.
     * No status is posted.
     * The phase following this method is expected to be PENDING.
     *
     * @param  jobListUrl  base (job list) URL for UWS service
     * @param  stringParamMap  map of text parameters
     * @param  streamParamMap  map of streamed parameters
     * @return   new UWS job
     * @throws  UnexpectedResponseException  if a non-303 response was received
     * @throws  IOException  if some other IOException occurs
     */
    public static UwsJob createJob( String jobListUrl,
                                    Map<String,String> stringParamMap,
                                    Map<String,HttpStreamParam> streamParamMap )
            throws IOException {
        HttpURLConnection hconn =
            postForm( URLUtils.newURL( jobListUrl ), ContentCoding.NONE,
                      stringParamMap, streamParamMap );
        int code = hconn.getResponseCode();
        String codeMsg = "(" + code + " " + hconn.getResponseMessage() + ")";

        /* See UWS 1.1 section 2.2.3.1.  In case of success, there should be a
         * 303 (See Other) response.  Job rejection should provoke a
         * 403 (Forbidden).  Other responses are possible, and maybe even
         * legitimate, but not mentioned by UWS, so flagged for possible
         * special treatment. */
        if ( code == 303 ) {
            String location = hconn.getHeaderField( "Location" );
            if ( location == null ) {
                throw new IOException( "No Location field in 303 response" );
            }
            logger_.info( "Created UWS job at: " + location );
            return new UwsJob( URLUtils.newURL( location ) );
        }
        else if ( code == 403 ) {
            throw new IOException( "UWS job creation rejected " + codeMsg );
        }
        else {
            throw new UnexpectedResponseException( "Non-303 response "
                                                 + codeMsg, hconn );
        }
    }

    /**
     * Performs an HTTP form POST with a single name, value pair.
     * This convenience method invokes {@link #postUnipartForm}.
     *
     * @param   url   destination URL
     * @param   name   parameter name
     * @param   value  parameter value
     * @return   URL connection corresponding to the completed POST
     */
    private static HttpURLConnection postForm( URL url, String name,
                                               String value )
            throws IOException {
        Map<String,String> paramMap = new LinkedHashMap<String,String>();
        paramMap.put( name, value );
        return postUnipartForm( url, ContentCoding.NONE, paramMap );
    }

    /**
     * General form posting method.
     * It can take zero or more string parameters and zero or more stream
     * parameters, and posts them in an appropriate way.
     *
     * <p>Authentication may be handled, but there will be no 3xx
     * redirection.
     *
     * @param   url   destination URL
     * @param   coding  HTTP content coding; connection output should be
     *                  decoded using the same value
     * @param   stringParams  name-&gt;value map for POST parameters;
     *          values will be URL encoded as required
     * @param   streamParams  name-&gt;parameter map for POST parameters
     * @return   URL connection corresponding to the completed POST
     */
    public static HttpURLConnection
                  postForm( URL url, ContentCoding coding,
                            Map<String,String> stringParams,
                            Map<String,HttpStreamParam> streamParams )
            throws IOException {
        return ( streamParams == null || streamParams.isEmpty() )
             ? postUnipartForm( url, coding, stringParams )
             : postMultipartForm( url, coding, stringParams, streamParams,
                                  null );
    }

    /**
     * Performs an HTTP form POST with a name-&gt;value map of parameters.
     * They are posted with MIME type "application/x-www-form-urlencoded".
     *
     * <p>Authentication may be handled, but there will be no 3xx
     * redirection.
     *
     * @param   url  destination URL
     * @param   coding  HTTP content coding; connection output should be
     *                  decoded using the same value
     * @param   paramMap   name-&gt;value map of parameters; values will be
     *          encoded as required
     * @return   URL connection corresponding to the completed POST
     */
    public static HttpURLConnection
                  postUnipartForm( URL url, final ContentCoding coding,
                                   Map<String,String> paramMap )
            throws IOException {
        final byte[] postBytes = toPostedBytes( paramMap );
        UrlConnector connector = hconn -> {
            hconn.setRequestMethod( "POST" );
            hconn.setRequestProperty( "Content-Type",
                                      "application/x-www-form-urlencoded" );
            coding.prepareRequest( hconn );
            hconn.setFixedLengthStreamingMode( postBytes.length );
            hconn.setInstanceFollowRedirects( false );
            hconn.setDoOutput( true );
            hconn.connect();
            OutputStream hout = hconn.getOutputStream();
            hout.write( postBytes );
            hout.close();
        };
        logger_.info( "POST to " + url );
        logger_.config( "POST content: "
                              + ( postBytes.length < 200
                                    ? new String( postBytes, "utf-8" )
                                    : new String( postBytes, 0, 200, "utf-8" )
                                      + "..." ) );
        AuthConnection aconn =
            AuthManager
           .getInstance()
           .makeConnection( url, connector, Redirector.NO_REDIRECT );
        if ( logger_.isLoggable( Level.CONFIG ) ) {
            logger_
           .config( "Did something like: "
                  + getCurlPostEquivalent(
                        url, coding, aconn.getContext(), AuthUtil.LOG_SECRETS,
                        paramMap, new HashMap<String,HttpStreamParam>() ) );
        }
        URLConnection conn = aconn.getConnection();
        if ( conn instanceof HttpURLConnection ) {
            return (HttpURLConnection) conn;
        }
        else {
            throw new IOException( "Can't POST to non-HTTP URL " + url );
        }
    }

    /**
     * Performs an HTTP form POST with a name-&gt;value map and a
     * name-&gt;stream map of parameters.
     * The form is written in multipart/form-data format.
     * See <a href="http://www.ietf.org/rfc/rfc2046.txt">RFC 2046</a> Sec 5.1.
     *
     * <p>Authentication may be handled, but there will be no 3xx
     * redirection.
     *
     * @param   url  destination URL
     * @param   coding  HTTP content coding; connection output should be
     *                  decoded using the same value
     * @param   stringMap   name-&gt;value map of parameters
     * @param   streamMap   name-&gt;stream map of parameters
     * @param  boundary  multipart boundary; if null a default value is used
     * @return   URL connection corresponding to the completed POST
     */
    public static HttpURLConnection
                postMultipartForm( URL url, final ContentCoding coding,
                                   final Map<String,String> stringMap,
                                   final Map<String,HttpStreamParam> streamMap,
                                   String boundary )
            throws IOException {
        final String boundary0 =
            boundary == null ? "<<<--------------MULTIPART-BOUNDARY------->>>"
                             : boundary;
        if ( boundary0.length() > 70 ) {
            throw new IllegalArgumentException( "Boundary >70 chars"
                                              + " (see RFC 2046 sec 5.1.1)" );
        }
        UrlConnector connector = hconn -> {
            connectMultipartForm( hconn, coding, stringMap, streamMap,
                                  boundary0 );
        };
        AuthConnection aconn =
            AuthManager
           .getInstance()
           .makeConnection( url, connector, Redirector.NO_REDIRECT );
        if ( logger_.isLoggable( Level.CONFIG ) ) {
            logger_
           .config( "Did something like: "
                  + getCurlPostEquivalent( url, coding, aconn.getContext(),
                                           AuthUtil.LOG_SECRETS,
                                           stringMap, streamMap ) );
        }
        URLConnection conn = aconn.getConnection();
        if ( conn instanceof HttpURLConnection ) {
            return (HttpURLConnection) conn;
        }
        else {
            throw new IOException( "Can't POST to non-HTTP URL " + url );
        }
    }

    /**
     * Opens a connection corresponding to POSTing a multipart/form-data form.
     *
     * @param   hconn   unconnected connection ready for configuration,
     *                  on exit form will have been posted
     * @param   coding  HTTP content coding; connection output should be
     *                  decoded using the same value
     * @param   stringMap   name-&gt;value map of parameters
     * @param   streamMap   name-&gt;stream map of parameters
     * @param  boundary  multipart boundary; if null a default value is used
     */
    private static void
                connectMultipartForm( HttpURLConnection hconn,
                                      ContentCoding coding,
                                      Map<String,String> stringMap,
                                      Map<String,HttpStreamParam> streamMap,
                                      String boundary )
            throws IOException {
        hconn.setRequestMethod( "POST" );
        hconn.setRequestProperty( "Content-Type",
                                  "multipart/form-data"
                                + "; boundary=\"" + boundary + "\"" );
        coding.prepareRequest( hconn );
        hconn.setInstanceFollowRedirects( false );
        hconn.setDoOutput( true );
        logger_.info( "POST params to " + hconn.getURL() );

        /* Open and buffer stream for POST content.  If we simply write to
         * the connection's output stream, the content will be buffered
         * in memory by the HttpURLConnection implementation, which may
         * cause an OutOfMemoryError in the case of large uploads.
         * So arrange to stream the data by doing one of two things:
         * either use chunked HTTP transfer encoding, or buffer the
         * data up front using a StoragePolicy prior to the write.
         * Chunking is generally preferable, but it's possible that some
         * servers don't support it (though RFC2616 sec 3.6.1 says they
         * should do for HTTP 1.1). */
        OutputStream hout =
            HTTP_CHUNK_SIZE > 0
                ? createChunkedHttpStream( hconn, HTTP_CHUNK_SIZE )
                : createStoredHttpStream( hconn,
                                          StoragePolicy.getDefaultPolicy() );
        hout = new BufferedOutputStream( hout );

        /* Write string parameters.  See RFC 2046 Sec 4.1. */
        for ( Map.Entry<String,String> entry : stringMap.entrySet() ) {
            String pName = entry.getKey();
            String pValue = entry.getValue();
            logger_.config( "POST " + pName + "=" + pValue );
            writeBoundary( hout, boundary, false );
            writeHttpLine( hout, "Content-Type: text/plain; charset=" + UTF8 );
            writeHttpLine( hout, "Content-Disposition: form-data; "
                               + "name=\"" + pName + "\"" );
            writeHttpLine( hout, "" );
            hout.write( toTextPlain( pValue, UTF8 ) );
        }

        /* Write stream parameters.  See RFC 2388. */
        for ( Map.Entry<String,HttpStreamParam> entry : streamMap.entrySet() ) {
            String pName = entry.getKey();
            HttpStreamParam pStreamer = entry.getValue();
            logger_.config( "POST " + pName + " (streamed data)" );
            writeBoundary( hout, boundary, false );
            writeHttpLine( hout, "Content-Disposition: form-data"
                               + "; name=\"" + pName + "\""
                               + "; filename=\"" + pName + "\"" );
            for ( Map.Entry<String,String> header :
                  pStreamer.getHttpHeaders().entrySet() ) {
                writeHttpLine( hout,
                               header.getKey() + ": " + header.getValue() );
            }
            writeHttpLine( hout, "" );
            pStreamer.writeContent( hout );
        }

        /* Write trailing delimiter. */
        writeBoundary( hout, boundary, true );
        hout.close();
    }

    /**
     * Returns a snippet of text representing a shell invocation of the
     * <code>curl(1)</code> command that posts a query corresponding
     * to the given parameters.
     *
     * <p>This can be a useful diagnostic tool when attempting to
     * reproduce service invocations.  Use with care however,
     * the returned string is not guaranteed to do exactly what
     * this java code does.
     *
     * @param   url   destination URL
     * @param   coding  HTTP content coding; connection output should be
     *                  decoded using the same value
     * @param   context  authentication context in use
     * @param   showSecret  if true, sensitive information like passwords
     *                      may appear in the output;
     *                      if false they must be omitted
     * @param   stringParams  name-&gt;value map for POST parameters;
     *          values will be URL encoded as required
     * @param   streamParams  name-&gt;parameter map for POST parameters
     * @return   line of pseudo-shell script giving curl invocation
     * @see   <a href="http://curl.haxx.se/">curl</a>
     * @see   #postForm
     */
    public static String
            getCurlPostEquivalent( URL url, ContentCoding coding,
                                   AuthContext context, boolean showSecret,
                                   Map<String,String> stringParams,
                                   Map<String,HttpStreamParam> streamParams ) {
        StringBuffer sbuf = new StringBuffer()
            .append( "curl" )
            .append( " --url " )
            .append( url )
            .append( " --location " );
        if ( coding == ContentCoding.GZIP ) {
            sbuf.append( " --compressed" );
        }
        if ( context != null ) {
            for ( String carg : context.getCurlArgs( url, showSecret ) ) {
                sbuf.append( ' ' )
                    .append( shellEscape( carg ) );
            }
        }
        for ( Map.Entry<String,String> entry : stringParams.entrySet() ) {
            sbuf.append( " --form " )
                .append( entry.getKey() )
                .append( '=' )
                .append( shellEscape( entry.getValue() ) );
        }
        for ( String key : streamParams.keySet() ) {
            sbuf.append( " --form " )
                .append( key )
                .append( "=" )
                .append( "@<..data..>" );
        }
        return sbuf.toString();
    }

    /**
     * Escapes a given text string to make it usable as a word in a
     * (generic) un*x shell-scripting language.
     * Implementation may not be bullet-proof.
     *
     * @param  txt  raw text
     * @return  escaped text
     */
    private static String shellEscape( String txt ) {
        txt = txt.trim().replaceAll( "\\s+", " " );
        if ( ! txt.matches( ".*[ $?'\"].*" ) ) {
            return txt;
        }
        if ( txt.indexOf( "'" ) < 0 ) {
            return "'" + txt + "'";
        }
        if ( txt.indexOf( '"' ) < 0 ) {
            return '"' + txt + '"';
        }
        return "'" + txt.replaceAll( "'", "'\"'\"'" ) + "'";
    }

    /**
     * Encodes a name-&gt;value mapping as an array of bytes suitable for
     * "application/x-www-form-urlencoded" transmission.
     *
     * @param   paramMap  name-&gt;value mapping
     * @return   byte array suitable for POSTing
     */
    public static byte[] toPostedBytes( Map<String,String> paramMap ) {
        StringBuffer sbuf = new StringBuffer();
        for ( Map.Entry<String,String> entry : paramMap.entrySet() ) {
            if ( sbuf.length() != 0 ) {
                sbuf.append( '&' );
            }
            sbuf.append( URLUtils.urlEncode( entry.getKey() ) )
                .append( '=' )
                .append( URLUtils.urlEncode( entry.getValue() ) );
        }
        int nc = sbuf.length();
        byte[] bbuf = new byte[ nc ];
        for ( int i = 0; i < nc; i++ ) {
            char c = sbuf.charAt( i );
            assert (int) c == ( c & 0x7f );
            bbuf[ i ] = (byte) sbuf.charAt( i );
        }
        return bbuf;
    }

    /**
     * Turns a string into text/plain content using a given character set.
     * As well as charset issues, this makes sure that all line breaks
     * are CRLF, as required by RFC 2046 sec 4.1.1.
     *
     * @param  text  input string
     * @param  charset  character set name
     * @return  output string
     */
    static byte[] toTextPlain( String text, String charset )
            throws IOException {

        /* Surprisingly fiddly to get right; see Goldfarb's First Law of
         * Text Processing. */
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        String[] lines =
            Pattern.compile( "$", Pattern.MULTILINE ).split( text );
        Pattern contentPattern = Pattern.compile( "(.+)" );
        for ( int i = 0; i < lines.length; i++ ) {
            String lineWithDelim = lines[ i ];
            Matcher contentMatcher = contentPattern.matcher( lineWithDelim );
            String lineContent = contentMatcher.find()
                               ? contentMatcher.group( 1 )
                               : "";
            if ( lineContent.length() < lineWithDelim.length() ) {
                bos.write( '\r' );
                bos.write( '\n' );
            }
            bos.write( lineContent.getBytes( charset ) );
        }
        return bos.toByteArray();
    }

    /**
     * Writes a line to an HTTP connection.
     * The correct line terminator (CRLF) is added.
     *
     * @param  out  output stream
     * @param   line  line of text
     */
    static void writeHttpLine( OutputStream out, String line )
            throws IOException {
        int leng = line.length();
        byte[] buf = new byte[ leng + 2 ];
        for ( int i = 0; i < leng; i++ ) {
            int c = line.charAt( i );
            if ( c < 32 || c > 126 ) {
                throw new IOException( "Bad character for HTTP "
                                     + "0x" + Integer.toHexString( c ) );
            }
            buf[ i ] = (byte) c;
        }
        buf[ leng + 0 ] = (byte) '\r';
        buf[ leng + 1 ] = (byte) '\n';
        out.write( buf );
    }

    /**
     * Writes a multipart boundary given a delimiter string,
     * in accordance with RFC2046 section 5.1.1.
     * The required preceding CRLF and minus signs are included.
     *
     * @param  out  HTTP output stream
     * @param  delim  basic boundary delimiter string (without --s)
     * @param  isEnd  true iff the boundary marks the end of the multipart
     *                document
     */
    private static void writeBoundary( OutputStream out, String delim,
                                       boolean isEnd )
            throws IOException {

        /* Write the preceding CRLF.  This is considered part of the
         * boundary delimiter itself. */
        out.write( '\r' );
        out.write( '\n' );

        /* Prepare the boundary string itself. */
        StringBuffer sbuf = new StringBuffer()
            .append( "--" )
            .append( delim );
        if ( isEnd ) {
            sbuf.append( "--" );
        }
        String line = sbuf.toString();

        /* Write the boundary string with a trailing CRLF. */
        writeHttpLine( out, line );
    }

    /**
     * Returns a new output stream for writing to a URL connection,
     * with chunking.
     * The connection must be unconnected when this is called.
     * A chunked transfer encoding will be used, with the chunk size
     * as given.
     *
     * @param   hconn  unconnected URL connection
     * @param   chunkSize  chunk size in bytes
     * @return  destination stream for HTTP content
     */
    private static OutputStream
                   createChunkedHttpStream( HttpURLConnection hconn,
                                            int chunkSize )
            throws IOException {
        hconn.setChunkedStreamingMode( chunkSize );
        hconn.connect();
        return hconn.getOutputStream();
    }

    /**
     * Returns a new output stream for writing to a URL connection,
     * with buffering managed by a given storage policy.
     * The connection must be unconnected when this is called.
     * The content will be buffered using a ByteStore obtained from the
     * supplied storage profile, and the actual write (as for an
     * unstreamed HttpURLConnection) will be done only when the returned
     * output stream is closed.
     *
     * @param  hconn  unconnected URL connection
     * @param  storage   storage policy used for buffering content
     * @return  destination stream for HTTP content
     */
    private static OutputStream
                   createStoredHttpStream( final HttpURLConnection hconn,
                                           StoragePolicy storage ) {
        final ByteStore hbuf = storage.makeByteStore();
        return new FilterOutputStream( hbuf.getOutputStream() ) {
            public void close() throws IOException {
                super.close();
                long hleng = hbuf.getLength();
                if ( hleng > Integer.MAX_VALUE ) {
                    // Could be worked round, but TAP service providers wouldn't
                    // thank me for it.
                    throw new IOException( "Uploads are too big" );
                }
                hconn.setFixedLengthStreamingMode( (int) hleng );
                hconn.connect();
                OutputStream hcout = hconn.getOutputStream();
                hbuf.copy( hcout );
                hcout.close();
            }
        };
    }

    /**
     * Indicates whether the job represented by a given status object
     * supports UWS 1.1-style blocking calls.
     *
     * @param  info   job status object
     * @return   true if the job is known to support blocking
     */
    private static boolean hasBlocking( UwsJobInfo info ) {
        int[] majMin = getVersion( info );
        if ( majMin != null ) {
            int maj = majMin[ 0 ];
            int min = majMin[ 1 ];
            return maj == 1 && min >= 1
                || maj > 1;
        }
        else {
            return false;
        }
    }

    /**
     * Parses and returns the version information from a job status object.
     *
     * @param  info   job status object
     * @return   2-element array giving (major,minor) version numbers,
     *           or null if parsing fails
     */
    private static int[] getVersion( UwsJobInfo info ) {
        if ( info == null ) {
            return null;
        }
        else {
            String version = info.getUwsVersion();
            if ( version == null ) {
                return new int[] { 1, 0 };
            }
            else {
                Matcher matcher =
                    Pattern.compile( "^\\s*([0-9]+)\\.([0-9]+).*$" )
                           .matcher( version );
                if ( matcher.matches() ) {
                    try {
                        return new int[] {
                            Integer.parseInt( matcher.group( 1 ) ),
                            Integer.parseInt( matcher.group( 2 ) ),
                        };
                    }
                    catch ( NumberFormatException e ) {
                        return null;
                    }
                }
                else {
                    return null;
                }
            }
        }
    }

    /**
     * Exception which may be thrown if a UWS HTTP request receives a
     * response code which is not as mandated by UWS, but not obviously
     * an error.  An example is getting a 200 rather than 303 from a
     * job creation attempt.
     */
    public static class UnexpectedResponseException extends IOException {
        private final HttpURLConnection hconn_;

        /**
         * Constructor.
         *
         * @param   msg   error message
         * @param   hconn  connection which supplied the response
         */
        private UnexpectedResponseException( String msg,
                                             HttpURLConnection hconn ) {
            super( msg );
            hconn_ = hconn;
        }

        /**
         * Returns the HTTP connection which supplied the response.
         * The response code, but probably not the content, has already been
         * read.
         *
         * @return  connection
         */
        public HttpURLConnection getConnection() {
            return hconn_;
        }
    }

    /**
     * Callback interface for objects wanting to be notified of job status
     * changes.
     */
    public interface JobWatcher {

        /**
         * Called when the job status has changed.
         * Usually, this means a change of the Phase.
         *
         * @param  job  job in question
         * @param  info   new status
         */
        void jobUpdated( UwsJob job, UwsJobInfo info );
    }
}
