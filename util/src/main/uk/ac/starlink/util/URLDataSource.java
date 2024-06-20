package uk.ac.starlink.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import uk.ac.starlink.auth.AuthManager;
import uk.ac.starlink.auth.Redirector;

/**
 * A DataSource implementation based on a {@link java.net.URL}.
 *
 * <p>This uses an {@link uk.ac.starlink.auth.AuthManager}
 * to manage authentication and redirects.
 * 
 * @author   Mark Taylor (Starlink)
 * @author   Peter W. Draper (JAC, Durham University)
 */
public class URLDataSource extends DataSource {

    private final URL url_; 
    private final ContentCoding coding_;
    private final AuthManager authManager_;

    /**
     * Constructs a DataSource from a URL with default content coding
     * and AuthManager.
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
        this( url, coding, AuthManager.getInstance() );
    }

    /**
     * Constructs a DataSource from a URL with given content coding policy
     * and AuthManager.
     * If the URL has a ref part (the bit after the '#') it will be 
     * treated as the position attribute of this DataSource.
     *
     * @param  url  URL
     * @param  coding  configures HTTP compression; may be overridden
     *                 if inapplicable or security concerns apply
     * @param  authManager   authentication manager
     */
    public URLDataSource( URL url, ContentCoding coding,
                          AuthManager authManager ) {
        url_ = url;
        authManager_ = authManager;

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

        /* Get basic input stream, negotiating authentication and redirects. */
        InputStream in =
            coding_
           .getInputStream( authManager_
                           .connect( url_, coding_, Redirector.DEFAULT ) );

        /* Work around known mark/reset bug in one of the J2SE input stream 
         * implementations (present up to 1.6 at least) used when 
         * invoking connection.getInputStream(). */
        return new FilterInputStream( in ) {
            public boolean markSupported() {
                return false;
            }
        };
    }

    /**
     * Returns the URL on which this <code>URLDataSource</code> is based.
     *
     * @return  the URL
     */
    public URL getURL() {
        return url_;
    }
}
