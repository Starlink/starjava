package uk.ac.starlink.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * A DataSource implementation based on a {@link java.net.URL}.
 * 
 * @author   Mark Taylor (Starlink)
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
        return url.openStream();
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
