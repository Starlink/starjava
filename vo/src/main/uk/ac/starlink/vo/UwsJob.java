package uk.ac.starlink.vo;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.ByteStore;
import uk.ac.starlink.table.StoragePolicy;

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
    private volatile String phase_;
    private volatile long phaseTime_;
    private boolean deleteAttempted_;
    private Thread deleteThread_;
    private List<Runnable> phaseWatcherList_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );
    private static final String UTF8 = "UTF-8";

    /** Chunk size for HTTP transfer encoding; if &lt;=0, don't chunk. */
    public static int HTTP_CHUNK_SIZE = 1024 * 1024;

    /**
     * Constructor.
     *
     * @param   jobUrl  the UWS {jobs}/(job-id) URL containing 
     *                  the details of this job
     */
    public UwsJob( URL jobUrl ) {
        jobUrl_ = jobUrl;
        phaseWatcherList_ = new ArrayList<Runnable>();
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
     * Returns the most recently read job phase.  Invoking this method
     * does not cause the phase to be read.
     *
     * @return  phase
     */
    public String getLastPhase() {
        return phase_;
    }

    /**
     * Returns the epoch at which the job phase was last read.
     * The return value is the value of <code>System.currentTimeMillis()</code>
     * at that epoch.
     *
     * @return   phase read time epoch in milliseconds
     * @see   #getLastPhase
     */
    public long getLastPhaseTime() {
        return phaseTime_;
    }

    /**
     * Posts a phase for this job.
     *
     * @param  phase  UWS job phase to assign
     */
    public void postPhase( String phase ) throws IOException {
        HttpURLConnection hconn =
            postForm( new URL( jobUrl_ + "/phase" ), "PHASE", phase );
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
     * It is polled at the given interval.
     *
     * @param   poll   polling time in milliseconds to assess job completion
     * @return   final phase
     * @throws   UnexpectedResponseException  if HTTP responses other than
     *           UWS mandated ones occur
     */
    public String waitForFinish( final long poll )
            throws IOException, InterruptedException {
        if ( phase_ == null ) {
            readPhase();
        }
        while ( UwsStage.forPhase( phase_ ) != UwsStage.FINISHED ) {
            String phase = phase_;
            UwsStage stage = UwsStage.forPhase( phase );
            switch ( stage ) {
                case UNSTARTED:
                    throw new IOException( "Job not started"
                                         + " - phase: " + phase );
                case UNKNOWN:
                    logger_.info( "UWS phase " + phase + " reported"
                                + "; wait and poll" );
                    // fall through
                case RUNNING:
                    Thread.sleep( poll );
                    break;
                case FINISHED:
                    return phase;
                case ILLEGAL:
                    throw new IOException( "Illegal UWS job phase: " + phase );
                default:
                    throw new AssertionError();
            }
            assert stage == UwsStage.UNKNOWN || stage == UwsStage.RUNNING;
            readPhase();
        }
        return phase_;
    }

    /**
     * Reads the current UWS phase for this job from the server
     * and stores the result.  The result is available from the
     * {@link #getLastPhase} method, and the associated
     * {@link #getLastPhaseTime getLastPhaseTime} value is also set.
     */
    public void readPhase() throws IOException {
        URL phaseUrl = new URL( jobUrl_ + "/phase" );
        InputStream in = new BufferedInputStream( phaseUrl.openStream() );
        StringBuffer sbuf = new StringBuffer();
        for ( int c; ( c = in.read() ) >= 0; ) {
            sbuf.append( (char) ( c & 0xff ) );
        }
        in.close();
        String phase = sbuf.toString();
        logger_.info( phaseUrl + " phase: " + phase );
        phase_ = phase;
        phaseTime_ = System.currentTimeMillis();
        for ( Runnable watcher : phaseWatcherList_ ) {
            watcher.run();
        }
    }

    /**
     * Adds a callback which will be invoked whenever this job's phase
     * is read by {@link #readPhase}.  Note that the runnable will not
     * in general be invoked from the AWT event dispatch thread.
     *
     * @param  watcher  runnable to be notified on job phase change
     */
    public void addPhaseWatcher( Runnable watcher ) {
        phaseWatcherList_.add( watcher );
    }

    /**
     * Removes a callback previously added by {@link #addPhaseWatcher}.
     * Has no effect if <code>watcher</code> is not currently registered.
     *
     * @param   watcher  runnable to be removed
     */
    public void removePhaseWatcher( Runnable watcher ) {
        phaseWatcherList_.remove( watcher );
    }

    /**
     * Reads XML text from the job URL which describes the server's
     * record of the current state of the job, and returns the result
     * as a UwsJobInfo object.
     *
     * @return  job status
     */
    public UwsJobInfo readJob() throws IOException, SAXException {
        UwsJobInfo[] infos = JobSaxHandler.readJobInfos( jobUrl_ );
        return infos != null && infos.length > 0 ? infos[ 0 ] : null;
    }

    /**
     * Posts deletion of this job to the server.
     *
     * @throws  IOException  if job deletion failed for some reason
     */
    public void postDelete() throws IOException {
        HttpURLConnection hconn = openHttpConnection( jobUrl_ );
        logger_.info( "DELETE " + jobUrl_ );
        hconn.setRequestMethod( "DELETE" );
        hconn.setInstanceFollowRedirects( false );
        final int response;
        try {
            hconn.connect();
            response = hconn.getResponseCode();
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
            postForm( new URL( jobListUrl ), stringParamMap, streamParamMap );
        int code = hconn.getResponseCode();
        if ( code != HttpURLConnection.HTTP_SEE_OTHER ) {  // 303
            String msg = "Non-" + HttpURLConnection.HTTP_SEE_OTHER + " response"
                       + " (" + hconn.getResponseCode() + " "
                       + hconn.getResponseMessage() + ")";
            throw new UnexpectedResponseException( msg, hconn );
        }
        String location = hconn.getHeaderField( "Location" );
        if ( location == null ) {
            throw new IOException( "No Location field in 303 response" );
        }
        logger_.info( "Created UWS job at: " + location );
        return new UwsJob( new URL( location ) );
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
        Map<String,String> paramMap = new HashMap<String,String>();
        paramMap.put( name, value );
        return postUnipartForm( url, paramMap );
    }

    /**
     * General form posting method.
     * It can take zero or more string parameters and zero or more stream
     * parameters, and posts them in an appropriate way.
     *
     * @param   url   destination URL
     * @param   stringParams  name->value map for POST parameters;
     *          values will be URL encoded as required
     * @param   streamParams  name->parameter map for POST parameters
     * @return   URL connection corresponding to the completed POST
     */
    public static HttpURLConnection
                  postForm( URL url, Map<String,String> stringParams,
                            Map<String,HttpStreamParam> streamParams )
            throws IOException {
        return ( streamParams == null || streamParams.isEmpty() )
             ? postUnipartForm( url, stringParams )
             : postMultipartForm( url, stringParams, streamParams, null );
    }

    /**
     * Performs an HTTP form POST with a name->value map of parameters.
     * They are posted with MIME type "application/x-www-form-urlencoded".
     *
     * @param   url  destination URL
     * @param   paramMap   name->value map of parameters; values will be
     *          encoded as required
     * @return   URL connection corresponding to the completed POST
     */
    public static HttpURLConnection
                  postUnipartForm( URL url, Map<String,String> paramMap )
            throws IOException {
        HttpURLConnection hconn = openHttpConnection( url );
        byte[] postBytes = toPostedBytes( paramMap );
        hconn.setRequestMethod( "POST" );
        hconn.setRequestProperty( "Content-Type",
                                  "application/x-www-form-urlencoded" );
        // We could stream this request, which would seem tidier.
        // However, that inhibits automatic handling of 401 Unauthorized
        // responses if a java.net.Authenticator is in use, and the 
        // amount of data posted is not likely to be large, so don't do it.
        // hconn.setFixedLengthStreamingMode( postBytes.length );
        hconn.setInstanceFollowRedirects( false );
        hconn.setDoOutput( true );
        logger_.info( "POST to " + url );
        logger_.config( "POST content: "
                              + ( postBytes.length < 200 
                                    ? new String( postBytes, "utf-8" )
                                    : new String( postBytes, 0, 200, "utf-8" )
                                      + "..." ) );
        hconn.connect();
        OutputStream hout = hconn.getOutputStream();
        hout.write( postBytes );
        hout.close();
        return hconn;
    }

    /**
     * Performs an HTTP form POST with a name->value map and a name->stream
     * map of parameters.  The form is written in multipart/form-data format.
     * See <a href="http://www.ietf.org/rfc/rfc2046.txt">RFC 2046</a> Sec 5.1.
     *
     * @param   stringMap   name->value map of parameters
     * @param   streamMap   name->stream map of parameters
     * @param  boundary  multipart boundary; if null a default value is used
     * @return   URL connection corresponding to the completed POST
     */
    public static HttpURLConnection
                  postMultipartForm( URL url, Map<String,String> stringMap,
                                     Map<String,HttpStreamParam> streamMap,
                                     String boundary )
            throws IOException {
        if ( boundary == null ) {
            boundary = "<<<--------------MULTIPART-BOUNDARY------->>>";
        }
        if ( boundary.length() > 70 ) {
            throw new IllegalArgumentException( "Boundary >70 chars"
                                              + " (see RFC 2046 sec 5.1.1)" );
        }

        /* Prepare for multipart/form-data output. */
        HttpURLConnection hconn = openHttpConnection( url );
        hconn.setRequestMethod( "POST" );
        hconn.setRequestProperty( "Content-Type",
                                  "multipart/form-data"
                                + "; boundary=\"" + boundary + "\"" );
        hconn.setInstanceFollowRedirects( false );
        hconn.setDoOutput( true );
        logger_.info( "POST params to " + url );

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
            writeHttpLine( hout, "--" + boundary );
            writeHttpLine( hout, "Content-Type: text/plain; charset=" + UTF8 );
            writeHttpLine( hout, "Content-Disposition: form-data; "
                               + "name=\"" + pName + "\"" );
            writeHttpLine( hout, "" ); 
            hout.write( toTextPlain( pValue, UTF8 ) );
            writeHttpLine( hout, "" );
        }

        /* Write stream parameters.  See RFC 2388. */
        for ( Map.Entry<String,HttpStreamParam> entry : streamMap.entrySet() ) {
            String pName = entry.getKey();
            HttpStreamParam pStreamer = entry.getValue();
            logger_.config( "POST " + pName + " (streamed data)" );
            writeHttpLine( hout, "--" + boundary );
            writeHttpLine( hout, "Content-Disposition: form-data"
                               + "; name=\"" + pName + "\"" 
                               + "; filename=\"" + pName + "\"" );
            for ( Map.Entry<String,String> header :
                  pStreamer.getHttpHeaders().entrySet() ) {
                writeHttpLine( hout,
                               header.getKey() + ": " + header.getValue() );
                writeHttpLine( hout, "" ); 
            }
            pStreamer.writeContent( hout );
        }

        /* Write trailing delimiter. */
        writeHttpLine( hout, "--" + boundary + "--" );
        hout.close();
        return hconn;
    }

    /**
     * Opens a URL connection as an HttpURLConnection.
     * If the connection is not HTTP, an IOException is thrown.
     *
     * @param   url   URL
     * @return   typed connection
     */
    private static HttpURLConnection openHttpConnection( URL url )
            throws IOException {
        URLConnection connection = url.openConnection();
        try {
            return (HttpURLConnection) connection;
        }
        catch ( ClassCastException e ) {
            throw (IOException) new IOException( "Not an HTTP URL? " + url )
                               .initCause( e );
        }
    }

    /**
     * Encodes a name->value mapping as an array of bytes suitable for
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
            try {
                sbuf.append( URLEncoder.encode( entry.getKey(), UTF8 ) )
                    .append( '=' )
                    .append( URLEncoder.encode( entry.getValue(), UTF8 ) );
            }
            catch ( UnsupportedEncodingException e ) {
                throw new AssertionError( "No " + UTF8 + "??" );
            }
        }
        int nc = sbuf.length();
        byte[] bbuf = new byte[ nc ];
        for ( int i = 0; i < nc; i++ ) {
            char c = sbuf.charAt( i );
            assert (int) c == (int) ( c & 0x7f );
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
}
