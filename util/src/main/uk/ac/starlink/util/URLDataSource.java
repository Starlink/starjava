package uk.ac.starlink.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * A DataSource implementation based on a {@link java.net.URL}.
 * 
 * @author   Mark Taylor (Starlink)
 * @author   Peter W. Draper (JAC, Durham University)
 */
public class URLDataSource extends DataSource {

    private URL url; 

    /**
     * Constructs a DataSource from a URL.
     * If the URL has a ref part (the bit after the '#') it will be 
     * treated as the position attribute of this DataSource.
     */
    public URLDataSource( URL url ) {
        this.url = url;
        setName( url.toString() );
        setPosition( url.getRef() );
    }

    protected InputStream getRawInputStream() throws IOException {

        //  Contact the resource.
        URLConnection connection = url.openConnection();

        /* Handle basic authentication if present. */
        String userInfo = url.getUserInfo();
        setBasicAuth( connection, userInfo );

        //  Handle switching from HTTP to HTTPS (but not vice-versa, that's
        //  insecure), if a HTTP 30x redirect is returned, as Java doesn't do
        //  this by default.
        //  See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4620571.
        if ( connection instanceof HttpURLConnection ) {
            int code = ((HttpURLConnection)connection).getResponseCode();
            if ( code == HttpURLConnection.HTTP_MOVED_PERM ||
                 code == HttpURLConnection.HTTP_SEE_OTHER ||
                 code == HttpURLConnection.HTTP_MOVED_TEMP ) {
                String newloc = connection.getHeaderField( "Location" );
                URL newurl = new URL( newloc );
                connection = newurl.openConnection();
                setBasicAuth( connection, userInfo );
            }
        }

        /* Work around known mark/reset bug in one of the J2SE input stream 
         * implementations (present up to 1.6 at least) used when 
         * invoking connection.getInputStream(). */
        InputStream strm = connection.getInputStream();
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
        return url;
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
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            OutputStream b64out =
                new Base64OutputStream( bout, txt.length() * 2 );
            for ( int i = 0; i < txt.length(); i++ ) {
                b64out.write( (byte) txt.charAt( i ) );
            }
            b64out.close();
            return new String( bout.toByteArray(), "UTF-8" ).trim();
        }
        catch ( IOException e ) {
            throw new AssertionError( e );
        }
    }
}
