package uk.ac.starlink.vo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Logger;

/**
 * Utilities for manipulating the HTTP <code>User-Agent</code>
 * header from the JVM, following IVOA usage conventions.
 * These conventions are codified in the
 * <a href="https://datatracker.ietf.org/doc/html/rfc7231#section-5.5.3"
 *    >SoftID IVOA Note</a>.
 *
 * <p>Typical usage for a validator client would be:
 * <pre>
 *   String uaComment = UserAgentUtil.COMMENT_TEST;   // "(IVOA-test)"
 *   UserAgentUtil.pushUserAgentToken( uaComment );
 *   ... do validation ...
 *   UserAgentUtil.popUserAgentToken( uaComment );
 * </pre>
 *
 * @author   Mark Taylor
 * @since    12 Apr 2019
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc7231#section-5.5.3"
           >RFC 7231 sec 5.5.3</a>
 * @see <a href="https://www.ivoa.net/documents/Notes/softid/"
 *         >NOTE-softid-1.0</a>
 */
public class UserAgentUtil {

    /** Purpose verb indicating testing/monitoring/validation: {@value}. */
    public static final String PURPOSE_TEST = "test";

    /** Purpose verb indicating copying/mirroring/harvesting: {@value}. */
    public static final String PURPOSE_COPY = "copy";

    /** String prefixed to purpose verb to introduce IVOA operation comment. */
    public static final String IVOA_PREFIX = "IVOA-";

    /** Comment token indicating client performs VO test/monitor/validate. */
    public static final String COMMENT_TEST =
        createOpPurposeComment( PURPOSE_TEST, null );

    /** Comment token indicating client performs VO copy/mirror/harvest. */
    public static final String COMMENT_COPY =
        createOpPurposeComment( PURPOSE_COPY, null );

    /** System property that can be used to manipulate the UserAgent header. */
    public static final String AGENT_PROPNAME = "http.agent";

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Private constructor prevents instantiation.
     */
    private UserAgentUtil() {
    }

    /**
     * Appends a token/comment to the currently used User-Agent string.
     * This does not overwrite existing text, which is in general useful.
     *
     * @param  token  string to add
     */
    public static void pushUserAgentToken( String token ) {
        if ( token != null && token.trim().length() > 0 ) {
            String agent0 = System.getProperty( AGENT_PROPNAME );
            String agent1 = agent0 == null || agent0.trim().length() == 0
                 ? token
                 : agent0.trim() + " " + token;
            System.setProperty( AGENT_PROPNAME, agent1 );
            logger_.info( "Appended User-Agent token: "
                        + AGENT_PROPNAME + " is now \"" + agent1 + "\"" );
        }
    }

    /**
     * Removes a token/comment from the currently-used User-Agent string.
     * This only has effect if the given token is at the end of the
     * current list, that is if it has just been added.
     * If it's not present or not at the end, a warning is logged and
     * there is no other effect.
     *
     * @param  token  previously added string to remove
     */
    public static void popUserAgentToken( String token ) {
        if ( token != null && token.trim().length() > 0 ) {
            String agent0 = System.getProperty( AGENT_PROPNAME );
            int index = agent0 == null
                      ? -1
                      : agent0.indexOf( token );
            if ( index >= 0 ) {
                String agent1 = agent0.substring( 0, index ).trim();
                if ( agent1.length() == 0 ) {
                    System.clearProperty( AGENT_PROPNAME );
                    logger_.info( "Removed User-Agent token: "
                                + AGENT_PROPNAME + " is now empty" );
                }
                else {
                    System.setProperty( AGENT_PROPNAME, agent1 );
                    logger_.info( "Removed User-Agent token: "
                                + AGENT_PROPNAME
                                + " is now \"" + agent1 + "\"" );
                }
            }
            else {
                logger_.warning( "Failed to remove User-Agent token \""
                               + token + "\" - " + AGENT_PROPNAME
                               + " is still \"" + agent0 + "\"" );
            }
        }
    }

    /**
     * Assembles a string of the form
     * <code>(IVOA-&lt;purpose&gt; &lt;extra&gt;)</code>.
     *
     * @param  purpose  operational purpose string; recommended values are
     *                  <code>PURPOSE_*</code> static members of this class
     * @param  extra  free form additional text excluding "(" or ")";
     *                may be null
     * @return  string suitable for User-Agent header comment
     * @throws   IllegalArgumentException  if rudimentary syntax checking
     *           detects errors
     */
    public static String createOpPurposeComment( String purpose,
                                                 String extra ) {
        if ( purpose.indexOf( ' ' ) >= 0 ) {
            throw new IllegalArgumentException( "Purpose contains whitespace" );
        }
        StringBuffer sbuf = new StringBuffer()
              .append( "(" )
              .append( IVOA_PREFIX )
              .append( purpose );
        if ( extra != null && extra.trim().length() > 0 ) {
            if ( extra.indexOf( '(' ) >= 0 ||
                 extra.indexOf( ')' ) >= 0 ) {
                throw new IllegalArgumentException( "Comment text "
                                                  + "contains parentheses" );
            }
            sbuf.append( " " )
            .append( extra.trim() );
        }
        sbuf.append( ")" );
        return sbuf.toString();
    }

    /**
     * Returns the content of the User-Agent header that this client
     * will currently use when making HTTP requests.
     *
     * @return  client User-Agent header content
     */
    public static String getUserAgentText() {
        return System.getProperty( AGENT_PROPNAME );
    }

    /**
     * Parses a products string as found in the HTTP User-Agent or Server
     * header.  See RFC 7231 sec 5.5.3 and RFC 7230 sec 3.2.6 for the
     * relevant syntax productions.
     *
     * @param  productsTxt   string giving product and comment tokens
     *                       as found in User-Agent or Server header
     * @return  array of tokens; each may be either a product or a comment;
     *          comments start with a "("
     * @throws  IllegalArgumentException  if the syntax is not as required
     */
    public static String[] parseProducts( String productsTxt ) {
        final boolean allowComment = true;
        String tchar = "[-!#$%&'*+.^_`|~a-zA-Z0-9]";
        String token = tchar + "+";
        String product = token + "(/" + token + ")?";
        String comment = "\\(" + "([^\\\\)]|\\\\.)*" + "\\)";
        String wordRegex =
            "\\s+(" + product + ( allowComment ? ( "|" + comment ) : "" ) + ")";
        Pattern wordPattern = Pattern.compile( wordRegex );
        List<String> list = new ArrayList<>();
        String txt = " " + productsTxt;
        while ( txt.trim().length() > 0 ) {
            Matcher matcher = wordPattern.matcher( txt );
            if ( matcher.lookingAt() ) {
                String word = matcher.group();
                boolean isComment = word.trim().charAt( 0 ) == '(';
                if ( isComment ) {
                    StringBuffer sbuf = new StringBuffer();
                    for ( int ic = 0; ic < word.length(); ic++ ) {
                        char c = word.charAt( ic );
                        if ( c != '\\' ) {
                            sbuf.append( c );
                        }
                    }
                    list.add( sbuf.toString().trim() );
                }
                else {
                    list.add( word.trim() );
                }
                txt = txt.substring( matcher.end() );
            }
            else {
                throw new IllegalArgumentException( "Bad product syntax" );
            }
        }
        return list.toArray( new String[ 0 ] );
    }
}
