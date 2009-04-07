package uk.ac.starlink.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
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

        //  Handle switching from HTTP to HTTPS (but not vice-versa, that's
        //  insecure), if a HTTP 30x redirect is returned, as Java doesn't do
        //  this by default.
        if ( connection instanceof HttpURLConnection ) {
            int code = ((HttpURLConnection)connection).getResponseCode();
            if ( code == HttpURLConnection.HTTP_MOVED_PERM ||
                 code == HttpURLConnection.HTTP_MOVED_TEMP ) {
                String newloc = connection.getHeaderField( "Location" );
                URL newurl = new URL( newloc );
                connection = newurl.openConnection();
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
}
