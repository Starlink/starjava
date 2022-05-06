package uk.ac.starlink.auth;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.logging.Logger;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import uk.ac.starlink.auth.ssl.SSLUtil;
import uk.ac.starlink.auth.ssl.X509CertificateChain;

/**
 * IvoaAuthScheme concrete subclass for working with X509 certificates.
 *
 * <p>This implements the {@value SCHEME_NAME} scheme.
 * An X509 certificate is retrieved from the login URL in PEM format
 * and used for subsequent HTTPS connections.
 *
 * @author   Mark Taylor
 * @since    6 May 2022
 */
public class X509IvoaAuthScheme extends IvoaAuthScheme {

    /** Singleton instance. */
    public static final X509IvoaAuthScheme INSTANCE =
        new X509IvoaAuthScheme();

    /** Name and identifier for this scheme ({@value}). */
    public static final String SCHEME_NAME = "ivoa_x509";

    private static final int MAX_CERT_SIZE = 64 * 1024;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.auth" );

    /**
     * Private constructor prevents instantiation.
     */
    private X509IvoaAuthScheme() {
        super( SCHEME_NAME );
    }

    public ContextFactory createContextFactory( LoginProtocol loginProto,
                                                URL loginUrl,
                                                URL challengeUrl ) {
        final ProtectionSpace scope = new ProtectionSpace( challengeUrl, null );
        return new ContextFactory() {
            public AuthContext createContext( UserInterface ui ) {
                X509CertificateChain chain =
                    readAuth( loginProto, X509IvoaAuthScheme.this, loginUrl,
                              ui, X509IvoaAuthScheme::readCertificate );
                return chain == null ? null
                                     : createX509Context( chain );
            }
            public AuthContext createUnauthContext() {
                return createX509Context( null );
            }
            private AuthContext createX509Context( X509CertificateChain chain ){
                return new X509Context( X509IvoaAuthScheme.this,
                                        loginUrl.toString(), scope, chain );
            }
        };
    }

    /**
     * Reads an X509 certificate in PEM format from an HTTP connection.
     *
     * @param  conn  connection
     * @return   certificate, not null
     */
    private static X509CertificateChain readCertificate( HttpURLConnection conn)
            throws IOException {

        // Could check response for content-type: application/x-pem-file?
        // Don't know if that's official.
        ContentType ctype = ContentType.parse( conn.getContentType() );
        StringBuffer sbuf = new StringBuffer()
            .append( "Attempting to read X509 certificate in PEM format from " )
            .append( conn.getURL() );
        if ( ctype != null ) {
            sbuf.append( " (" )
                .append( ctype )
                .append( ")" );
        }
        logger_.info( sbuf.toString() );
        byte[] buf;
        try ( InputStream in = conn.getInputStream() ) {
            buf = readStream( in, MAX_CERT_SIZE );
        }
        try {
            return SSLUtil.readPemCertificateAndKey( buf );
        }
        catch ( GeneralSecurityException e ) {
            throw new IOException( "Error reading certificate", e );
        }
    }

    /**
     * Reads bytes from an input stream up to a given maximum length.
     *
     * @param  in  input stream
     * @param  maxLeng   maximum acceptable length; if the stream is longer,
     *                   an IOException is thrown
     * @return  byte array containing full content of stream,
     *          0 &lt; length &lt;= maxLeng
     */
    private static byte[] readStream( InputStream in, int maxLeng )
            throws IOException {
        byte[] buf = new byte[ maxLeng ];
        int n = 0;
        while ( true ) {
            if ( n >= maxLeng ) {
                throw new IOException( "Certificate content surprisingly large"
                                     + " (>" + maxLeng + ") - bailing out" );
            }
            int c = in.read( buf, n, maxLeng - n );
            if ( c < 0 ) {
                if ( n == 0 ) {
                    throw new IOException( "No certificate data found" );
                }
                byte[] buf1 = new byte[ n ];
                System.arraycopy( buf, 0, buf1, 0, n );
                return buf1;
            }
            n += c;
        }
    }

    /**
     * AuthContext implementation for X509 certificates.
     *
     * <p>Note this uses the concept of a validity scope, which is not
     * currently defined by the ivoa_x509 scheme.
     * But the assumption is made that if this factory is suitable for
     * one URL in a given ProtectionScheme (host+port),
     * it will be suitable for all others.
     */
    private static class X509Context implements AuthContext {

        private final X509IvoaAuthScheme scheme_;
        private final String loginUrl_;
        private final ProtectionSpace scope_;
        private final SSLSocketFactory sslFact_;
        private final long expireTime_;

        /**
         * Constructor.
         *
         * @param  scheme  scheme
         * @param  loginUrl   URL at which the token can be acquired
         * @param  scope   URL range for which this context is valid
         * @param  chain   certificate chain
         */
        X509Context( X509IvoaAuthScheme scheme, String loginUrl,
                     ProtectionSpace scope, X509CertificateChain chain ) {
            scheme_ = scheme;
            loginUrl_ = loginUrl;
            scope_ = scope;
            sslFact_ = chain == null ? null : SSLUtil.getSocketFactory( chain );
            Date expireDate = chain == null ? null : chain.getExpiryDate();
            expireTime_ = expireDate == null ? Long.MAX_VALUE
                                             : expireDate.getTime();
        }

        public AuthScheme getScheme() {
            return scheme_;
        }

        public boolean hasCredentials() {
            return sslFact_ != null;
        }

        public void configureConnection( HttpURLConnection conn )
                throws IOException {
            if ( conn instanceof HttpsURLConnection && sslFact_ != null ) {
                ((HttpsURLConnection) conn).setSSLSocketFactory( sslFact_ );
            }
        }

        public boolean isUrlDomain( URL url ) {
            return scope_.equals( new ProtectionSpace( url, null ) );
        }

        public boolean isChallengeDomain( Challenge challenge, URL url ) {
            try {
                return scheme_.createContextFactory( challenge, url ) != null
                    && loginUrl_.equals( challenge.getParams()
                                                  .get( ACCESSURL_PARAM ) );
            }
            catch ( BadChallengeException e ) {
                return false;
            }
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expireTime_;
        }

        public String[] getCurlArgs( URL url, boolean showSecret ) {

            /* Note: some curl implementations don't provide suitable
             * client certificate support; this manifests as an error
             * "curl: (35) SSL connect error".  Fix is apparently
             * to update curl, or maybe NSS. */
            return hasCredentials() ? new String[] { "--cert", "<PEM-FILE>" }
                                    : new String[ 0 ];
        }
    }
}
