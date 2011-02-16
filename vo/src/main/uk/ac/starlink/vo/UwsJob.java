package uk.ac.starlink.vo;

import java.io.BufferedInputStream;
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
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

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
     * Constructs a UwsJob with information about the submission parameters.
     *
     * @param   jobUrl  the UWS {jobs}/(job-id) URL containing 
     *                  the details of this job
     * @param  paramMap  UWS parameters submitted to create this job
     */
    public UwsJob( URL jobUrl, Map<String,String> paramMap ) {
        this( jobUrl );
        setParameters( paramMap );
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
            postForm( jobUrl_ + "/phase", "PHASE", phase );
        int code = hconn.getResponseCode();
        if ( code != HttpURLConnection.HTTP_SEE_OTHER ) {
            throw new IOException( "Non-303 response: " + code + " " +
                                   hconn.getResponseMessage() );
        }
    }

    /**
     * Starts running this job and blocks until it has completed with
     * a success or error code.  This convenience method is a one-stop-shop
     * for running a UWS job.
     *
     * @param   poll   polling time in milliseconds to assess job completion
     * @return   final phase
     * @throws   UnexpectedResponseException  if HTTP responses other than
     *           UWS mandated ones occur
     */
    public String runToCompletion( final long poll )
            throws IOException, InterruptedException {
        postPhase( "RUN" );
        Object lock = new Object();
        synchronized ( lock ) {
            while ( ! isCompletionPhase( phase_ ) ) {
                readPhase();
                if ( ! isCompletionPhase( phase_ ) ) {
                    lock.wait( poll );
                }
            }
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
        logger_.info( "Read job phase from " + phaseUrl );
        InputStream in = new BufferedInputStream( phaseUrl.openStream() );
        StringBuffer sbuf = new StringBuffer();
        for ( int c; ( c = in.read() ) >= 0; ) {
            sbuf.append( (char) ( c & 0xff ) );
        }
        in.close();
        String phase = sbuf.toString();
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
     * @return   the URL connection corresponding to the DELETE request;
     *           it is possible, but not required, to query this for
     *           the response code etc
     */
    public HttpURLConnection postDelete() throws IOException {
        URLConnection connection = jobUrl_.openConnection();
        if ( ! ( connection instanceof HttpURLConnection ) ) {
            throw new IOException( "Not an HTTP URL?" );
        }
        logger_.info( "DELETE " + jobUrl_ );
        HttpURLConnection hconn = (HttpURLConnection) connection;
        hconn.setRequestMethod( "DELETE" );
        hconn.setInstanceFollowRedirects( false );
        hconn.connect();

        /* Read the response code, otherwise the request doesn't seem
         * to take place. */
        int code = hconn.getResponseCode();
        return hconn;
    }

    /**
     * Convenience method to assess whether a UWS phase is a completion phase.
     * A completion phase is defined as one which will not change further
     * without some stimulus from the client.
     *
     * @param   phase  phase text
     * @return   true iff <code>phase</code> is final
     */
    public static boolean isCompletionPhase( String phase ) {
        return "COMPLETED".equals( phase )
            || "ERROR".equals( phase )
            || "ABORTED".equals( phase )
            || "HELD".equals( phase );
    }

    /**
     * Submits a job to a UWS service and returns a new UwsJob object.
     * No status is posted.
     * The phase following this method is expected to be PENDING.
     *
     * @param  jobListUrl  base (job list) URL for UWS service
     * @param  paramMap  name->value map of UWS parameters
     * @throws  UnexpectedResponseException  if a non-303 response was received
     * @throws  IOException  if some other IOException occurs
     */
    public static UwsJob createJob( String jobListUrl,
                                    Map<String,String> paramMap )
            throws IOException {
        HttpURLConnection hconn = postForm( jobListUrl, paramMap );
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
        return new UwsJob( new URL( location ), paramMap );
    }

    /**
     * Performs an HTTP form POST with a single name, value pair.
     * This convenience method invokes {@link #postForm}.
     *
     * @param   url   destination URL
     * @param   name   parameter name
     * @param   value  parameter value
     */
    private static HttpURLConnection postForm( String url, String name,
                                               String value )
            throws IOException {
        Map paramMap = new HashMap();
        paramMap.put( name, value );
        return postForm( url, paramMap );
    }

    /**
     * Performs an HTTP form POST with a name-&gt;value map of parameters.
     * They are posted with MIME type "application/x-www-form-urlencoded".
     *
     * @param   url  destination URL
     * @param   paramMap   name, value map of parameters
     */
    private static HttpURLConnection postForm( String url,
                                               Map<String,String> paramMap )
            throws IOException {
        URLConnection connection = new URL( url ).openConnection();
        if ( ! ( connection instanceof HttpURLConnection ) ) {
            throw new IOException( "Not an HTTP URL?" );
        }
        HttpURLConnection hconn = (HttpURLConnection) connection;
        byte[] postBytes = toPostedBytes( paramMap );
        hconn.setRequestMethod( "POST" );
        hconn.setRequestProperty( "Content-Type",
                                  "application/x-www-form-urlencoded" );
        hconn.setRequestProperty( "Content-Length",
                                  Integer.toString( postBytes.length ) );
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
     * Encodes a name-&gt;value mapping as an array of bytes suitable for
     * "application/x-www-form-urlencoded" transmission.
     *
     * @param   paramMap  name-&gt;value mapping
     */
    private static byte[] toPostedBytes( Map<String,String> paramMap ) {
        String utf8 = "UTF-8";
        StringBuffer sbuf = new StringBuffer();
        for ( Map.Entry<String,String> entry : paramMap.entrySet() ) {
            if ( sbuf.length() != 0 ) {
                sbuf.append( '&' );
            }
            try {
                sbuf.append( URLEncoder.encode( entry.getKey(), utf8 ) )
                    .append( '=' )
                    .append( URLEncoder.encode( entry.getValue(), utf8 ) );
            }
            catch ( UnsupportedEncodingException e ) {
                throw new AssertionError( "No " + utf8 + "??" );
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
