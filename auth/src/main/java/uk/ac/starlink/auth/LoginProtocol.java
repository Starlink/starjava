package uk.ac.starlink.auth;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Defines a way to present credentials for the purpose of subsequent
 * access to a service.  This interface does not know anything about
 * how to interpret the response from such a presentation.
 *
 * @author   Mark Taylor
 * @since    10 Dec 2021
 */
public interface LoginProtocol {

    /**
     * Makes a single attempt at acquiring a response from a login interface.
     * The return value is an open URL connection ready for reading.
     * It is not guaranteed to represent successful authentication;
     * success or failure should be diagnosed based on its content,
     * for instance the HTTP status code.
     *
     * @param   url  target URL
     * @param   userpass  credentials supplied by user
     * @return   open URL connection
     * @throws   IOException  if some communications failed
     */
    HttpURLConnection presentCredentials( URL url, UserPass userpass )
            throws IOException;

    /**
     * Returns a name identifying this protocol.
     *
     * @return   name
     */
    String getProtocolName();
}
