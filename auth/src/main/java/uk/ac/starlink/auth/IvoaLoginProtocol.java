package uk.ac.starlink.auth;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Provides SSO-compliant implementations of LoginProtocol.
 *
 * @author   Mark Taylor
 * @since    10 Dec 2021
 */
public abstract class IvoaLoginProtocol implements LoginProtocol {

    public static final String SSO_URL = "ivo://ivoa.net/sso";

    /** Implementation for standard_id="ivo://ivoa.net/sso#tls-with-password".*/
    public static final IvoaLoginProtocol TLS_WITH_PASSWORD;

    /** Implementation for standard_id="ivo://ivoa.net/sso#BasicAA". */
    public static final IvoaLoginProtocol BASIC_AA;

    private static final IvoaLoginProtocol[] INSTANCES = {
        TLS_WITH_PASSWORD = new TlsWithPasswordProtocol(),
        BASIC_AA = new BasicAaProtocol(),
    };

    private final String stdId_;
    private final String name_;

    /**
     * Constructor.
     *
     * @param  idFrag  fragment part of SSO identification URI
     */
    private IvoaLoginProtocol( String idFrag ) {
        stdId_ = SSO_URL + "#" + idFrag;
        name_ = idFrag;
    }

    /**
     * Returns the URI identifying this login method.
     *
     * @return  standard_id parameter value
     */
    public String getStandardId() {
        return stdId_;
    }

    public String getProtocolName() {
        return name_;
    }

    /**
     * Returns an instance for a given standard ID.
     * The supplied value is as found in the <code>standard_id</code>
     * parameter of an SSO challenge.
     *
     * @param  stdId  standard ID
     * @return   matching login protocol, or null
     */
    public static IvoaLoginProtocol getProtocol( String stdId ) {
        for ( IvoaLoginProtocol proto : INSTANCES ) {
            if ( proto.stdId_.equalsIgnoreCase( stdId ) ) {
                return proto;
            }
        }
        return null;
    }

    /**
     * Utility method to open a connection from an assumed
     * HTTP-compatible URL.
     *
     * @param  url  URL
     * @return  open connection
     * @throws  IOException if it doesn't work, including if the
     *          connection turned out not to be HTTP-compatible
     */
    private static HttpURLConnection openHttpConnection( URL url )
            throws IOException {
        URLConnection conn = url.openConnection();
        if ( conn instanceof HttpURLConnection ) {
            return (HttpURLConnection) conn;
        }
        else {
            throw new IOException( "Not HTTP URL: " + url );
        }
    }

    /**
     * LoginProtocol implementation for standard_id="tls-with-password".
     */
    private static class TlsWithPasswordProtocol extends IvoaLoginProtocol {
        private static final String PASSWORD = "password";
        private static final String USERNAME = "username";
        TlsWithPasswordProtocol() {
            super( "tls-with-password" );
        }
        public HttpURLConnection presentCredentials( URL url,
                                                     UserPass userpass )
                throws IOException {
            Map<String,String> postParams = new LinkedHashMap<>();
            postParams.put( USERNAME, userpass.getUsername() );
            postParams.put( PASSWORD, new String( userpass.getPassword() ) );
            HttpURLConnection conn = openHttpConnection( url );
            AuthUtil.postForm( conn, postParams );
            conn.getResponseCode();
            return conn;
        }
    }

    /**
     * LoginProtocol implementation for standard_id="BasicAA".
     */
    private static class BasicAaProtocol extends IvoaLoginProtocol {
        private static final String BASIC = "Basic";
        BasicAaProtocol() {
            super( "BasicAA" );
        }
        public HttpURLConnection presentCredentials( URL url,
                                                     UserPass userpass )
                throws IOException {
            HttpURLConnection conn = openHttpConnection( url );
            String userpass64 =
                BasicAuthScheme.encodeUserPass( userpass.getUsername(),
                                                userpass.getPassword() );
            conn.setRequestProperty( AuthUtil.AUTH_HEADER,
                                     BASIC + " " + userpass64 );
            conn.getResponseCode();
            return conn;
        }
    }
}
