package uk.ac.starlink.vo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilities for manipulating the HTTP <code>User-Agent</code>
 * header from the JVM, following IVOA usage conventions.
 * These conventions are codified in the
 * <a href="https://www.ivoa.net/documents/Notes/softid/">SoftID IVOA Note</a>.
 *
 * @author   Mark Taylor
 * @since    12 Apr 2019
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc7231#section-5.5.3"
           >RFC 7231 sec 5.5.3</a>
 * @see <a href="https://www.ivoa.net/documents/Notes/softid/"
 *         >NOTE-softid-1.0</a>
 */
public class UserAgentUtil {

    /** Purpose term indicating default science operations: currently null. */
    public static final String PURPOSE_DFLT = null;

    /** Purpose term indicating testing/monitoring/validation: {@value}. */
    public static final String PURPOSE_TEST = "test";

    /** Purpose term indicating copying/mirroring/harvesting: {@value}. */
    public static final String PURPOSE_COPY = "copy";

    /** String prefixed to purpose term to introduce IVOA operation comment. */
    public static final String IVOA_PREFIX = "IVOA-";

    /** System property that can be used to manipulate the UserAgent header. */
    public static final String AGENT_PROPNAME = "http.agent";

    /**
     * Private constructor prevents instantiation.
     */
    private UserAgentUtil() {
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
