package uk.ac.starlink.auth;

import java.net.URL;

/**
 * Protection Space is a concept defined in RFC7235,
 * aggregating the canonical root URI and an optional realm.
 * It defines the domain within which the same authentication credentials
 * can be applied.
 *
 * @author   Mark Taylor
 * @since    8 Jun 2020
 * @see  <a href="https://tools.ietf.org/html/rfc7235#section-2.2"
 *          >RFC 7235, section 2.2</a>
 */
public class ProtectionSpace {

    // Note protocol is called "scheme" in RFC7235
    private final String proto_;
    private final String authority_;
    private final String realm_;

    /**
     * Constructs a ProtectionSpace using URL parts.
     *
     * @param  proto     protocol part of URL, for instance "http"
     * @param  authority   authority part of URL
     * @param  realm    challenge realm, or null
     */
    public ProtectionSpace( String proto, String authority, String realm ) {
        proto_ = proto;
        authority_ = authority;
        realm_ = realm;
    }

    /**
     * Constructs a ProtectionSpace using a URL.
     *
     * @param   url  URL
     * @param   realm   challenge realm, or null
     */
    public ProtectionSpace( URL url, String realm ) {
        this( url.getProtocol(), url.getAuthority(), realm );
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals( Object other ) {
        return other instanceof ProtectionSpace
            && this.toString().equals( other.toString() );
    }

    @Override
    public String toString() {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( proto_ )
            .append( "://" )
            .append( authority_ );
        if ( realm_ != null ) {
            sbuf.append( '[' )
                .append( realm_ )
                .append( ']' )
                .toString();
        }
        return sbuf.toString();
    }
}
