package uk.ac.starlink.hapi;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.util.CgiQuery;
import uk.ac.starlink.util.ContentCoding;
import uk.ac.starlink.util.IOConsumer;
import uk.ac.starlink.util.URLUtils;

/**
 * Encapsulates an HTTP(S) URL that serves the HAPI protocol.
 *
 * @author   Mark Taylor
 * @since    11 Jan 2024
 */
public class HapiService {

    /** Service base url, including a trailing slash. */
    private final String serviceUrl_;
    private final Supplier<ContentCoding> codingSupplier_;

    private static final Pattern HAPI_CODE_REGEX =
        Pattern.compile( ".*[^0-9](1[0-9]{3})[^0-9].*" );
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.hapi" );

    /**
     * Constructor with default content coding.
     *
     * @param   url  service base URL, with or without trailing slash
     * @throws  MalformedURLException if it's not an HTTP or HTTPS URL
     */
    public HapiService( String url ) throws MalformedURLException {
        this( url, null );
    }

    /**
     * Constructor with configurable content coding.
     *
     * @param   url  service base URL, with or without trailing slash
     * @param   codingSupplier  supplier for content coding,
     *                          or null for default
     * @throws  MalformedURLException if it's not an HTTP or HTTPS URL
     */
    public HapiService( String url, Supplier<ContentCoding> codingSupplier )
            throws MalformedURLException {
        if ( ! ( url.startsWith( "http://" ) ||
                 url.startsWith( "https://" ) ) ) {
            throw new MalformedURLException( "Bad HAPI server URL: " + url );
        }
        new URL( url );
        StringBuffer sbuf = new StringBuffer( url );
        if ( sbuf.charAt( sbuf.length() - 1 ) != '/' ) {
            sbuf.append( "/" );
        }
        serviceUrl_ = sbuf.toString();
        codingSupplier_ = codingSupplier == null ? () -> ContentCoding.GZIP
                                                 : codingSupplier;
    }

    /**
     * Returns a URL representing a query to a given endpoint of this service
     * with specified parameters.
     *
     * @param  endpoint  endpoint
     * @param  requestParams  map of key-value pairs giving additional
     *                        request parameters
     * @return  URL for query
     */
    public URL createQuery( HapiEndpoint endpoint,
                            Map<String,String> requestParams ) {
        CgiQuery query = new CgiQuery( serviceUrl_ + endpoint.getEndpoint() );

        /* Colons are percent-encoded by default, but are likely to be
         * safe in the query part of these URLs, are likely to be
         * present in ISO-8601 time strings, and are ugly when
         * percent-encoded if the string is visible to a human.
         * Similarly commas.
         * So make arrangements to include them literally in the URL. */
        query.allowUnencodedChars( ":," );
        if ( requestParams != null ) {
            for ( Map.Entry<String,String> entry : requestParams.entrySet() ) {
                query.addArgument( entry.getKey(), entry.getValue() );
            }
        }
        return query.toURL();
    }

    /**
     * Reads an input stream for a URL pointing at this service.
     * This is a drop-in replacement for {@link java.net.URL#openStream},
     * but handles a few extras like service-sensitive logging,
     * HTTP-level compression and 3xx indirection.
     *
     * @param  url  URL
     * @return   stream from URL
     */
    public InputStream openStream( URL url ) throws IOException {
        String urlTxt = url.toString();
        String endpoint = urlTxt.startsWith( serviceUrl_ )
                        ? urlTxt.substring( serviceUrl_.length() )
                                .replaceFirst( "[/?].*", "" )
                        : null;
        String reqTxt = endpoint == null ? "HAPI request"
                                         : "HAPI " + endpoint + " request";
        logger_.info( reqTxt + ": " + url );
        ContentCoding coding = codingSupplier_.get();
        HttpURLConnection hconn =
            (HttpURLConnection) coding.openConnection( url );
        hconn.setInstanceFollowRedirects( false );
        hconn = (HttpURLConnection) URLUtils.followRedirects( hconn, null );
        hconn.connect();
        int httpCode = hconn.getResponseCode();
        String httpMsg = hconn.getResponseMessage();
        if ( httpCode != 200 ) {
            throw createException( httpCode, httpMsg, urlTxt );
        }
        return coding.getInputStream( hconn );
    }

    /**
     * Opens an input stream from for a HAPI data request
     * that may be broken into several chunks if it encounters
     * a HAPI 1408 "Bad request - too much time or data requested" response.
     *
     * <p>A limit must be supplied for the maximum number of chunks that
     * will be retrieved for a given request.
     * If this limit is exceeded, behaviour depends on the supplied
     * <code>failOnLimit</code> flag.
     * If true, then such a request fails with an IOException.
     * If false, the input stream includes all the data from the
     * maximum number of chunks, and a WARNING is issued through the
     * logging system to indicate that not the whole stream was included.
     *
     * @param  url  HAPI request URL
     * @param  chunkLimit   the maximum number of chunks permitted
     * @param  limitCallback  called with message if chunk limit is exceeded;
     *                        may be null
     */
    public InputStream openChunkedStream( URL url, int chunkLimit,
                                          IOConsumer<String> limitCallback )
            throws IOException {
        return new ChunkStreamer( this, chunkLimit, limitCallback )
              .openMultiChunkStream( url );
    }

    /**
     * Reads a JSON object from an endpoint of this service with
     * no additional request parameters.
     *
     * @param  endpoint  endpoint
     * @return  JSONobject read from endpoint
     */
    public JSONObject readJson( HapiEndpoint endpoint ) throws IOException {
        return readJson( endpoint, null );
    }

    /**
     * Reads a JSON object from an endpoint of this service with
     * supplied additional request parameters.
     *
     * @param  endpoint  endpoint
     * @param  requestParams  map of key-value pairs giving additional
     *                        request parameters
     * @return  JSONobject read from endpoint
     */
    public JSONObject readJson( HapiEndpoint endpoint,
                                Map<String,String> requestParams )
            throws IOException {
        return readJson( createQuery( endpoint, requestParams ) );
    }

    @Override
    public int hashCode() {
        return serviceUrl_.hashCode();
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof HapiService ) {
            HapiService other = (HapiService) o;
            return this.serviceUrl_.equals( other.serviceUrl_ );
        }
        else {
            return false;
        }
    }

    /**
     * Reads a JSON object from a URL.
     *
     * @param  url  URL
     * @return  JSON object, not null
     * @throws  IOException  if no JSON object can be read
     */
    private JSONObject readJson( URL url ) throws IOException {
        try ( InputStream in = openStream( url ) ) {
            Object json;
            try {
                json = new JSONTokener( in ).nextValue();
            }
            catch ( JSONException e ) {
                throw new IOException( "Not a JSON object at: " + url, e );
            }
            if ( json instanceof JSONObject ) {
                return (JSONObject) json;
            }
            else {
                throw new IOException( "Not a JSON object at: " + url );
            }
        }
    }

    /**
     * Parses a HAPI request URL to return a name-value map giving the
     * request parameters.
     *
     * @param  hapiUrl  URL
     * @return   name-value map for query parameter assignments
     *           in URL query part
     */
    public static Map<String,String> getRequestParameters( URL hapiUrl ) {
        Map<String,String> map = new LinkedHashMap<>();
        String query = hapiUrl.getQuery();
        if ( query != null ) {
            for ( String assignment : query.split( "&", -1 ) ) {
                String[] parts = assignment.split( "=", 2 );
                map.put( parts[ 0 ], parts[ 1 ] );
            }
        }
        return map;
    }

    /**
     * Creates an IOException describing a (presumably error status)
     * HTTP response.  If it looks like a HAPI error response,
     * a suitable HapiServiceException will be returned.
     *
     * @param   httpCode  HTTP status code
     * @param   httpMsg   HTTP status message
     * @param   urlTxt   URL
     * @return  new IOException
     */
    private static IOException createException( int httpCode, String httpMsg,
                                                String urlTxt ) {
        if ( httpMsg != null ) {
            Matcher matcher = HAPI_CODE_REGEX.matcher( httpMsg );
            if ( matcher.matches() ) {
                int hapiCode = Integer.parseInt( matcher.group( 1 ) );
                return new HapiServiceException( httpMsg, hapiCode );
            }
            else {
                return new IOException( "HAPI load error: " + httpMsg );
            }
        }
        else {
            return new IOException( "HAPI load error: " + httpCode );
        }
    }
}
