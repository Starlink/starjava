package uk.ac.starlink.auth;

import java.net.URLConnection;

/**
 * Aggregates a URLConnection and the AuthContext used to make the connection.
 *
 * @author   Mark Taylor
 * @since    7 Jul 2020
 */
public class AuthConnection {

    private final URLConnection connection_;
    private final AuthContext context_;

    /**
     * Constructor.
     *
     * @param  connection  URLConnection, not null
     * @param  context   auth context used to open connection
     */
    public AuthConnection( URLConnection connection, AuthContext context ) {
        connection_ = connection;
        context_ = context;
    }

    /**
     * Returns this object's URLConnection.
     *
     * @return  connection, not null
     */
    public URLConnection getConnection() {
        return connection_;
    }

    /**
     * Returns this objects AuthContext, as used to make the connection.
     *
     * @return  auth context, possibly null
     */
    public AuthContext getContext() {
        return context_;
    }
}
