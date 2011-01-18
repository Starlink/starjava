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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import uk.ac.starlink.util.CgiQuery;

/**
 * Job submitted using the Universal Worker Service pattern.
 *
 * @author   Mark Taylor
 * @since    18 Jan 2011
 * @see <a href="http://www.ivoa.net/Documents/UWS/">IVOA UWS Recommendation</a>
 */
public class UwsJob {

    private final String jobListUrl_;
    private final Map<String,String> paramMap_;
    private URL jobUrl_;
    private boolean created_;
    private volatile String phase_;
    private volatile long phaseTime_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Constructs a UWS job with no parameters.
     *
     * @param  jobListUrl  base URL for UWS service (URL of job list)
     */
    public UwsJob( String jobListUrl ) {
        this( jobListUrl, new HashMap<String,String>() );
    }

    /**
     * Constructs a UWS job with a parameter map.
     *
     * @param  jobListUrl  base URL for UWS service (URL of job list)
     * @param  paramMap   name-&gt;value map of UWS parameters
     */
    public UwsJob( String jobListUrl, Map paramMap ) {
        jobListUrl_ = jobListUrl;
        paramMap_ = new LinkedHashMap<String,String>();
        paramMap_.putAll( paramMap );
    }

    /**
     * Sets a parameter with a string value.
     *
     * @param   name   parameter name
     * @param   value  parameter value
     */
    public void setParameter( String name, String value ) {
        paramMap_.put( name, value );
    }

    /**
     * Sets a parameter with an integer value.
     *
     * @param   name  parameter name
     * @param   value  parameter value
     */
    public void setParameter( String name, long value ) {
        setParameter( name, Long.toString( value ) );
    }

    /**
     * Sets a parameter with a double precision floating point value.
     * An effort is made to encode the value in a way unlikely to be#
     * misunderstood by the service.
     *
     * @param  name   parameter name
     * @param  value   parameter value
     */
    public void setParameter( String name, double value ) {
        setParameter( name, CgiQuery.formatDouble( value ) );
    }

    /**
     * Sets a parameter with a single precision floating point value.
     * An effort is made to encode the value in a way unlikely to be#
     * misunderstood by the service.
     *
     * @param  name   parameter name
     * @param  value   parameter value
     */
    public void setParameter( String name, float value ) {
        setParameter( name, CgiQuery.formatFloat( value ) );
    }

    /**
     * Returns the current parameter map.  This may be manipulated directly.
     *
     * @return  live parameter map
     */
    public Map<String,String> getParameterMap() {
        return paramMap_;
    }

    /**
     * Returns the base URL for the UWS service (the job list URL).
     *
     * @return  job list URL
     */
    public String getJobListUrl() {
        return jobListUrl_;
    }

    /**
     * Returns the URL for this job.  This will normally be a child of the
     * job list URL, and contains a representation of the job state.
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
     * Creates this job within UWS.  The current parameter set is posted
     * and a job URL is assigned.  No phase is posted.
     * The phase following this method is expected to be PENDING.
     *
     * @throws  IllegalArgumentException  if creation has already occurred
     * @throws  UnexpectedResponseException  if a non-303 response was received
     */
    public void postCreate() throws IOException {
        synchronized ( this ) {
            if ( created_ ) {
                throw new IllegalStateException( "Job already created" );
            }
            created_ = true;
        }
        HttpURLConnection hconn = postForm( jobListUrl_, paramMap_ );
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
        logger_.info( "UWS job at: " + location );
        jobUrl_ = new URL( location );
    }

    /**
     * Posts a phase for this job.
     * The job must have been created first.
     *
     * @param  phase  UWS job phase to assign
     */
    public void postPhase( String phase ) throws IOException {
        Map paramMap = new HashMap();
        paramMap.put( "PHASE", phase );
        HttpURLConnection hconn =
            postForm( jobUrl_ + "/phase", "PHASE", phase );
        int code = hconn.getResponseCode();
        if ( code != HttpURLConnection.HTTP_SEE_OTHER ) {
            throw new IOException( "Non-303 response: " + code + " " +
                                   hconn.getResponseMessage() );
        }
    }

    /**
     * Creates, runs this job and blocks until it has completed with
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
        postCreate();
        postPhase( "RUN" );
        Object lock = new Object();
        synchronized ( lock ) {
            while ( ! isCompletionPhase( readPhase() ) ) {
                lock.wait( poll );
            }
        }
        return phase_;
    }

    /**
     * Reads the current UWS phase for this job from the server.
     * The {@link #getLastPhase lastPhase} and associated
     * {@link #getLastPhaseTime lastPhaseTime} are also set.
     *
     * @return   current phase
     */
    public String readPhase() throws IOException {
        URL phaseUrl = new URL( jobUrl_ + "/phase" );
        InputStream in = new BufferedInputStream( phaseUrl.openStream() );
        StringBuffer sbuf = new StringBuffer();
        for ( int c; ( c = in.read() ) >= 0; ) {
            sbuf.append( (char) ( c & 0xff ) );
        }
        in.close();
        String phase = sbuf.toString();
        phase_ = phase;
        phaseTime_ = System.currentTimeMillis();
        return phase;
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
