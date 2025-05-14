package uk.ac.starlink.auth;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents and parses challenge specifications as defined by RFC7235.
 *
 * @author   Mark Taylor
 * @since    9 Jun 2020
 * @see   <a href="https://tools.ietf.org/html/rfc7235">RFC 7235</a>
 */
public class Challenge {

    private final String schemeName_;
    private final String realm_;
    private final Map<String,String> params_;
    private final String token68_;

    /* See RFC7235 section 2.1, and refs in sec 1.2. */
    private static final String TOKEN = "[-!#$%&'*+.^_`|~0-9A-Za-z]+";
    private static final String TOKEN68 = "[-._~+/0-9A-Za-z]+=*";
    private static final String OWS = "[ \\t]*";
    private static final String BWS = OWS;
    private static final String QUOTED_STRING = "\"(?:[^\"]|\\\\.)*\"";
    private static final String AUTH_PARAM =
        TOKEN + BWS + "=" + BWS + "(?:" + TOKEN + "|" + QUOTED_STRING + ")";
    private static final String CHALLENGE =
           OWS
         + "(" + TOKEN + ")" 
         + "("
           + "" + "|"
           + " +" + TOKEN68 + "|"
           + " +" + "(?:" + AUTH_PARAM
                          + "(?:" + OWS + "," + OWS + AUTH_PARAM + ")*)"
         + ")"
         + OWS + "(?:,|$)";
    private static final String PPAIR =
          "(" + TOKEN + ")"
        + BWS + "=" + BWS
        + "(" + TOKEN + "|" + QUOTED_STRING + ")"
        + OWS + "(?:,|$)";
    private static final Pattern CHALLENGE_REGEX = Pattern.compile( CHALLENGE );
    private static final Pattern TOKEN68_REGEX = Pattern.compile( TOKEN68 );
    private static final Pattern PPAIR_REGEX = Pattern.compile( PPAIR );

    /**
     * Constructs a challenge with an optional realm and auth parameters.
     *
     * @param  schemeName  scheme name, case insensitive
     * @param  realm   specified realm value, or null
     * @param  params  additional parameters (excluding realm);
     *                 keys are case insensitive and will be mapped to lower
     */
    public Challenge( String schemeName, String realm,
                      Map<String,String> params ) {
        schemeName_ = schemeName;
        realm_ = realm;
        Map<String,String> pmap = new LinkedHashMap<>();
        for ( Map.Entry<String,String> entry : params.entrySet() ) {
            pmap.put( entry.getKey().toLowerCase(), entry.getValue() );
        }
        params_ = Collections.unmodifiableMap( pmap );
        token68_ = null;
    }

    /**
     * Constructs a challenge with  token68 string
     *
     * @param  schemeName  scheme name, case insensitive
     * @param  token68   token68 string
     */
    public Challenge( String schemeName, String token68 ) {
        schemeName_ = schemeName;
        realm_ = null;
        params_ = Collections.emptyMap();
        token68_ = token68;
    }

    /**
     * Returns the authentication scheme name.
     *
     * @return  scheme name, case insensitive
     */
    public String getSchemeName() {
        return schemeName_;
    }

    /**
     * Returns the authentication realm if defined.
     *
     * @return  realm or null
     */
    public String getRealm() {
        return realm_;
    }

    /**
     * Returns the token68 value if defined.
     * If this is non-null, then there will be no parameters or realm.
     *
     * @return  token68 or null
     */
    public String getToken68() {
        return token68_;
    }

    /**
     * Returns any auth parameters that form part of this challenge
     * <em>excluding</em> the realm.
     *
     * @return  name-&gt;value map giving parameters;
     *          parameter names have been normalised to lower case
     */
    public Map<String,String> getParams() {
        return params_;
    }

    /**
     * Utility method giving a non-empty realm for this challenge.
     * If no realm has been defined, a BadChallengeException is thrown.
     *
     * @return   non-empty realm string
     * @throws   BadChallengeException if there is no realm
     */
    public String getRequiredRealm() throws BadChallengeException {
        if ( realm_ != null && realm_.trim().length() > 0 ) {
            return realm_;
        }
        else {
            throw new BadChallengeException( "No realm specified in challenge");
        }
    }

    /**
     * Utility method giving a non-empty value for a named parameter
     * of this challenge.
     * If the named parameter has not been specified,
     * a BadChallengeException is thrown.
     *
     * @param   key  parameter name, case-insensitive
     * @return   non-empty value for parameter <code>key</code>
     * @throws   BadChallengeException if no value is specified
     *                                 for <code>key</code>
     */
    public String getRequiredParameter( String key )
            throws BadChallengeException {
        for ( String k : params_.keySet() ) {
            if ( key.equalsIgnoreCase( k ) ) {
                String value = params_.get( k );
                if ( value != null && value.trim().length() > 0 ) {
                    return value;
                }
            }
        }
        throw new BadChallengeException( "No parameter \"" + key + "\""
                                       + " specified in challenge" );
    }

    /**
     * Utility method giving a non-null URL value
     * for a named parameter of this challenge.
     * If the named parameter is not specified or cannot be turned into a URL,
     * a BadChallengeException is thrown.
     *
     * @param   key  parameter name, case-insensitive
     * @return   non-null URL for parameter <code>key</code>
     * @throws   BadChallengeException if no URL value is specified
     *                                 for <code>key</code>
     */
    public URL getRequiredParameterUrl( String key )
            throws BadChallengeException {
        String value = getRequiredParameter( key );
        try {
            return new URI( value ).toURL();
        }
        catch ( MalformedURLException | URISyntaxException
                                      | IllegalArgumentException e ) {
            throw new BadChallengeException( "Parameter \"" + key + "\""
                                           + " is not a URL" );
        }
    }

    @Override
    public int hashCode() {
        int code = 5523221;
        code = 23 * code + schemeName_.toLowerCase().hashCode();
        code = 23 * code + Objects.hashCode( realm_ );
        code = 23 * code + params_.hashCode();
        return code;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof Challenge ) {
            Challenge other = (Challenge) o;
            return this.schemeName_.equalsIgnoreCase( other.schemeName_ )
                && Objects.equals( this.realm_, other.realm_ )
                && this.params_.equals( other.params_ );
        }
        else {
            return false;
        }
    }

    @Override
    public String toString() {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( schemeName_ );
        if ( realm_ != null ) {
            sbuf.append( ", " )
                .append( "realm=\"" )
                .append( realm_ )
                .append( "\"" );
        }
        for ( Map.Entry<String,String> entry : params_.entrySet() ) {
            sbuf.append( ", " )
                .append( entry.getKey() )
                .append( "=" )
                .append( entry.getValue() );
        }
        return sbuf.toString();
    }

    /**
     * Parses the content of a WWW-Authenticate header as a sequence
     * of RFC7235 challenges.
     * According to RFC7235 there should be at least one value in
     * the result, but syntactically invalid challenge text is just
     * ignored, so the result is not guaranteed to be non-empty.
     *
     * @param  challengeTxt   text as value of WWW-Authenticate header
     * @return   list of parsed challenges
     */
    public static List<Challenge> parseChallenges( String challengeTxt ) {
        List<Challenge> list = new ArrayList<>();
        Matcher matcher = CHALLENGE_REGEX.matcher( challengeTxt );
        while ( matcher.find() ) {
            list.add( createChallenge( matcher.group( 1 ).trim(),
                                       matcher.group( 2 ).trim() ) );
        }
        return list;
    }

    /**
     * Creates a challenge object given a scheme name and the additional
     * text that follows it in the WWW-Authenticate header.
     *
     * @param   schemeName  scheme name
     * @param   extraTxt   additional text containing parameters etc
     * @return  challenge
     */
    private static Challenge createChallenge( String schemeName,
                                              String extraTxt ) {
        if ( TOKEN68_REGEX.matcher( extraTxt ).matches() ) {
            return new Challenge( schemeName, extraTxt );
        }
        else {
            Matcher matcher = PPAIR_REGEX.matcher( extraTxt );
            String realm = null;
            Map<String,String> params = new LinkedHashMap<>();
            while ( matcher.find() ) {
                String key = matcher.group( 1 );
                String value = decodeValue( matcher.group( 2 ) );
                if ( "realm".equalsIgnoreCase( key ) ) {
                    realm = value;
                }
                else {
                    params.put( key, value );
                }
            }
            return new Challenge( schemeName, realm, params );
        }
    }

    /**
     * Extracts the string value from a string assumed to be an RFC7230
     * token or quoted-string.  If the input matches neither of those
     * productions it will probably give a sensible result, but it's
     * not guaranteed.
     *
     * @param   value  text assumed to be a token or quoted-string
     * @return  unquoted content
     */
    private static String decodeValue( String value ) {
        if ( value.length() > 0 && value.startsWith( "\"" ) ) {
            StringBuffer sbuf = new StringBuffer();
            for ( int i = 1; i < value.length() - 1; i++ ) {
                char c = value.charAt( i );
                sbuf.append( c == '\\' ? value.charAt( ++i ) : c );
            }
            return sbuf.toString();
        }
        else {
            return value;
        }
    }

    /**
     * Will parse a WWW-Authenticate string on the command line and
     * print out the parsed challenges.
     */
    public static void main( String[] args ) {
        int i = 0;
        for ( Challenge ch : parseChallenges( args[ 0 ] ) ) {
            System.out.println( "  " + ++i + ": " +  ch );
        }
    }
}
