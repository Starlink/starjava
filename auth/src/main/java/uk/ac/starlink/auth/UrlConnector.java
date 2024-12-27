package uk.ac.starlink.auth;

import java.io.IOException;
import java.net.HttpURLConnection;

/**
 * Defines how a connection is obtained from a URL.
 *
 * @author   Mark Taylor
 * @since    18 Jun 2020
 */
@FunctionalInterface
public interface UrlConnector {

    /**
     * Opens communication to an HTTP resource.
     * The implementation will presumably at least call
     * {@link java.net.URLConnection#connect},
     * but it may optionally perform other actions as well,
     * such as configuring the request headers as required
     * and writing to the connection's output stream,
     * before returning.
     *
     * <p>If this object is being used with AuthManager,
     * it will typically be a good idea to call
     * <code>setInstanceFollowRedirects(false)</code>,
     * since redirection can be handled by AuthManager.
     *
     * @param  hconn  URL connection; on entry connect() has not yet
     *                been called, but on exit it has
     */
    void connect( HttpURLConnection hconn ) throws IOException;
}
