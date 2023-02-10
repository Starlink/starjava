package uk.ac.starlink.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * A DataSource implementation based on a {@link java.net.URL}.
 * 
 * @author   Mark Taylor (Starlink)
 * @author   Peter W. Draper (JAC, Durham University)
 */
public class URLDataSource extends DataSource {

    private final URL url_; 
    private final ContentCoding coding_;

    /**
     * Constructs a DataSource from a URL with default content coding.
     * If the URL has a ref part (the bit after the '#') it will be 
     * treated as the position attribute of this DataSource.
     *
     * @param  url  URL
     */
    public URLDataSource( URL url ) {
        this( url, ContentCoding.GZIP );
    }

    /**
     * Constructs a DataSource from a URL with given content coding policy.
     * If the URL has a ref part (the bit after the '#') it will be 
     * treated as the position attribute of this DataSource.
     *
     * @param  url  URL
     * @param  coding  configures HTTP compression; may be overridden
     *                 if inapplicable or security concerns apply
     */
    public URLDataSource( URL url, ContentCoding coding ) {
        url_ = url;

        /* There are security issues around content-coding;
         * they are probably not relevant but I don't understand them,
         * so be cautious/paranoid. */
        if ( coding != null &&
             "http".equals( url.getProtocol() ) &&
             url.getUserInfo() == null ) {
            coding_ = coding;
        }
        else {
            coding_ = ContentCoding.NONE;
        }
        setName( url_.toString() );
        setPosition( url_.getRef() );
    }

    protected InputStream getRawInputStream() throws IOException {

        //  Contact the resource.
        URLConnection connection = url_.openConnection();

        /* Handle basic authentication if present. */
        String userInfo = url_.getUserInfo();
        setBasicAuth( connection, userInfo );

        /* Use content-coding to control HTTP-level compression. */
        ContentCoding coding = userInfo == null ? coding_ : ContentCoding.NONE;
        if ( coding != null ) {
            coding.prepareRequest( connection );
        }

        //  Handle switching from HTTP to HTTPS (but not vice-versa, that's
        //  insecure), if a HTTP 30x redirect is returned, as Java doesn't do
        //  this by default.
        //  See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4620571.
        //
        //  This code is should really use URLUtils.followRedirects for
        //  consistency, and because it only handles one round of redirection;
        //  however that method currently does not handle authentication
        //  or content coding.  That code will be reworked when the
        //  long-delayed authentication update is merged, so for now
        //  leave this in place; I don't think it is used in most cases anyway
        //  (DataSource static methods are used instead).
        if ( connection instanceof HttpURLConnection ) {
            int code = ((HttpURLConnection)connection).getResponseCode();
            if ( code == 301 || code == 303 || code == 302 ||
                 code == 307 || code == 308 ) {
                String newloc = connection.getHeaderField( "Location" );
                URL newurl = new URL( newloc );
                connection = newurl.openConnection();
                if ( coding != null ) {
                    coding.prepareRequest( connection );
                }
                setBasicAuth( connection, userInfo );
            }
        }

        /* Work around known mark/reset bug in one of the J2SE input stream 
         * implementations (present up to 1.6 at least) used when 
         * invoking connection.getInputStream(). */
        InputStream strm = coding.getInputStream( connection );
        return new FilterInputStream( strm ) {
            public boolean markSupported() {
                return false;
            }
        };
    }

    /**
     * Returns the URL on which this <tt>URLDataSource</tt> is based.
     *
     * @return  the URL
     */
    public URL getURL() {
        return url_;
    }

    /**
     * Sets the basic authorization parameter on a URL request if a
     * userinfo part is present.
     *
     * @param  connection  connection to adjust (must not have been opened yet)
     * @param  userInfo  user info (user:pass) part of URL, or null
     */
    private static void setBasicAuth( URLConnection connection,
                                      String userInfo ) {
        if ( userInfo != null && userInfo.trim().length() > 0 ) {
            connection.setRequestProperty( "Authorization",
                                           "Basic " + b64encode( userInfo ) );
        }
    }

    /**
     * Encodes a string into base64, without line breaks.
     *
     * @param   txt  unencoded
     * @return   encoded version of txt
     */
    private static String b64encode( String txt ) {
        byte[] inBuf = txt.getBytes( StandardCharsets.UTF_8 );
        byte[] outBuf = Base64.getEncoder().encode( inBuf );
        return new String( outBuf, StandardCharsets.US_ASCII );
    }
}
