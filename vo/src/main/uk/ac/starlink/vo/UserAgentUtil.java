package uk.ac.starlink.vo;

import java.util.logging.Logger;

/**
 * Utilities for manipulating the HTTP <code>User-Agent</code>
 * header from the JVM, following IVOA usage conventions.
 * At time of writing, these conventions are sketched in the
 * UserAgentUsage page on the IVOA wiki; they may be amended and/or
 * written up more formally in the future.
 *
 * <p>Typical usage would be:
 * <pre>
 *   String uaComment = UserAgentUtil.COMMENT_VALIDATE;   // "(IVOA-validate)"
 *   UserAgentUtil.pushUserAgentToken( uaComment );
 *   ... do validation ...
 *   UserAgentUtil.popUserAgentToken( uaComment );
 * </pre>
 *
 * @author   Mark Taylor
 * @since    12 Apr 2019
 * @see <a
 *      href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.43"
 *      >RFC 2616 sec 14.43</a>
 * @see <a href="https://wiki.ivoa.net/twiki/bin/view/IVOA/UserAgentUsage"
 *      >UserAgentUsage IVOA wiki page</a>
 */
public class UserAgentUtil {

    /** Purpose verb indicating validation: {@value}. */
    public static final String PURPOSE_VALIDATE = "validate";

    /** Purpose verb indicating service monitoring: {@value}. */
    public static final String PURPOSE_MONITOR = "monitor";

    /** Purpose verb indicating service harvesting: {@value}. */
    public static final String PURPOSE_HARVEST = "harvest";

    /** String prefixed to purpose verb to introduce IVOA operation comment. */
    public static final String IVOA_PREFIX = "IVOA-";

    /** Comment token indicating client performs VO validation. */
    public static final String COMMENT_VALIDATE =
        createOpPurposeComment( PURPOSE_VALIDATE, null );

    /** Comment token indicating client performs VO monitoring. */
    public static final String COMMENT_MONITOR =
        createOpPurposeComment( PURPOSE_MONITOR, null );

    /** Comment token indicating client performs VO harvesting. */
    public static final String COMMENT_HARVEST =
        createOpPurposeComment( PURPOSE_HARVEST, null );

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
}
