package uk.ac.starlink.vo;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import uk.ac.starlink.util.DOMUtils;

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
    private volatile Map paramMap_;
    private volatile String phase_;
    private volatile long phaseTime_;
    private boolean deleteAttempted_;
    private Thread deleteThread_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );
    private static final String UTF8 = "UTF-8";

    /**
     * Constructor.
     *
     * @param   jobUrl  the UWS {jobs}/(job-id) URL containing 
     *                  the details of this job
     */
    public UwsJob( URL jobUrl ) {
        jobUrl_ = jobUrl;
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
     * Returns the currently stored parameter map.  This may be null if
     * parameters are not known.
     * Use the {@link #readParameters} method to update the return value
     * of this method by reading the parameters from the server.
     * 
     * @return   name->value map of parameters
     * @see   #readParameters
     */
    public Map<String,String> getParameters() {
        return paramMap_;
    }

    /**
     * Sets the currently known parameter map for this job to a given value.
     * Invoked at construction time or by {@link #readParameters},
     * not usually otherwise.
     *
     * @param  paramMap  name->value map of parameters
     */
    protected void setParameters( Map<String,String> paramMap ) {
        paramMap_ = paramMap == null
            ? null
            : Collections.unmodifiableMap( new LinkedHashMap( paramMap ) );
    }

    /**
     * Reads the job parameters from the UWS server record for this job.
     *
     * @see  #getParameters
     */
    public void readParameters() throws IOException {
        String paramsUrl = jobUrl_ + "/parameters";
        logger_.info( "Read job parameters from " + paramsUrl );
        try {
            Document paramDoc =
                DocumentBuilderFactory.newInstance()
                                      .newDocumentBuilder()
                                      .parse( paramsUrl );
            NodeList els = paramDoc.getElementsByTagName( "*" );
            Map<String,String> paramMap = new LinkedHashMap();
            for ( int i = 0; i < els.getLength(); i++ ) {
                Element el = (Element) els.item( i );
                String tagName = el.getTagName();
                if ( tagName.equals( "parameter" ) ||
                     tagName.endsWith( ":parameter" ) ) {
                    String key = el.getAttribute( "id" );
                    String value = DOMUtils.getTextContent( el );
                    if ( key != null && value != null ) {
                        paramMap.put( key, value );
                    }
                }
            }
            setParameters( paramMap );
        }
        catch ( ParserConfigurationException e ) {
            throw (IOException) new IOException( "XML parse trouble" )
                               .initCause( e );
        }
        catch ( SAXException e ) {
            throw (IOException) new IOException( "XML parse trouble" )
                               .initCause( e );
        }
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
                    logger_.warning( "Unknown UWS phase " + phase );
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
    }

    /**
     * Returns the DOM document representing the current state of this job.
     * This is read from the XML text at the job URL.
     *
     * @return  job status
     */
    public Document readJob() throws IOException {
        try {
            return DocumentBuilderFactory.newInstance()
                                         .newDocumentBuilder()
                                         .parse( jobUrl_.toString() );
        }
        catch ( SAXException e ) {
            throw (IOException) new IOException( "Job document parse failure" )
                               .initCause( e );
        }
        catch ( ParserConfigurationException e ) {
            throw (IOException) new IOException( "Job document parse failure" )
                               .initCause( e );
        }
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
     * @param   stringMap  name->value map for POST parameters;
     *          values will be URL encoded as required
     * @param   streamMap  name->parameter map for POST parameters
     * @return   URL connection corresponding to the completed POST
     */
    private static HttpURLConnection
                   postForm( URL url, Map<String,String> stringMap,
                             Map<String,HttpStreamParam> streamMap )
            throws IOException {
        return ( streamMap == null || streamMap.isEmpty() )
             ? postUnipartForm( url, stringMap )
             : postMultipartForm( url, stringMap, streamMap, null );
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
    private static HttpURLConnection
                   postUnipartForm( URL url, Map<String,String> paramMap )
            throws IOException {
        HttpURLConnection hconn = openHttpConnection( url );
        byte[] postBytes = toPostedBytes( paramMap );
        hconn.setRequestMethod( "POST" );
        hconn.setRequestProperty( "Content-Type",
                                  "application/x-www-form-urlencoded" );
        hconn.setFixedLengthStreamingMode( postBytes.length );
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
    private static HttpURLConnection
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

        /* Open and buffer stream for POST content. */
        hconn.connect();
        OutputStream hout = new BufferedOutputStream( hconn.getOutputStream() );

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
    static byte[] toPostedBytes( Map<String,String> paramMap ) {
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
