package uk.ac.starlink.util;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a Content Type (MIME type) string.
 * Most of the work is done by the {link #parseContentType} factory method.
 *
 * <p>This takes care of things like optional whitespace and case folding,
 * so for instance if <code>ctypeTxt</code> has the value
 * <pre>
 *    APPLICATION / X-VOTABLE+XML ; content=datalink; CHARSET="iso\-8859\-1"
 * </pre>
 * then
 * <pre>
 *    ContentType ctype = CointentType.parse(ctypeTxt);
 *    assert ctype.matches("application", "x-votable+xml");
 *    assert ctype.getParameter("charset").equals("iso-8859-1");
 * </pre>
 *
 * @author   Mark Taylor
 * @since    17 Nov 2017
 * @see  <a href="https://tools.ietf.org/html/rfc2045">RFC 2045</a>
 */
public class ContentType {

    private final String type_;
    private final String subtype_;
    private final Map<String,String> params_;
    private static final String TOKEN =
        "[\\x21-\\x7e&&[^()<>@,;:\\\\\"/\\[\\]\\?=]]+";
    private static final String QUOTED = "\"(?:[^\"\\\\]|\\\\.)*\"";
    private static final Pattern TOKEN_REGEX =
        Pattern.compile( TOKEN );
    private static final Pattern TYPE_REGEX =
        Pattern.compile( "\\s*(" + TOKEN + ")\\s*/\\s*(" + TOKEN + ")(.*)" );
    private static final Pattern PARAM_REGEX =
        Pattern.compile( "\\s*;\\s*(" + TOKEN + ")\\s*=\\s*"
                       + "(" + TOKEN + "|" + QUOTED + ")" );

    /**
     * Constructs a ContentType from type and subtype strings.
     * Case is normalised (to lower case).
     *
     * @param   type  type part
     * @param   subtype  subtype part
     */
    public ContentType( String type, String subtype ) {
        this( type, subtype, new HashMap<String,String>() );
    }

    /**
     * Constructs a ContentType from its constituent parts.
     * Case is normalised (to lower case) for the case-insensitive parts,
     * that is type, subtype and parameter names.
     *
     * @param   type  type part
     * @param   subtype  subtype part
     * @param   params   map of parameters
     */
    public ContentType( String type, String subtype,
                        Map<String,String> params ) {
        type_ = type.toLowerCase();
        subtype_ = subtype.toLowerCase();
        params_ = new LinkedHashMap<String,String>();
        for ( Map.Entry<String,String> entry : params.entrySet() ) {
            params_.put( entry.getKey().toLowerCase(), entry.getValue() );
        }
    }

    /**
     * Returns the Type part of this content type.
     *
     * @return type
     */
    public String getType() {
        return type_;
    }

    /**
     * Returns the Subtype part of this content type.
     *
     * @return subtype
     */
    public String getSubtype() {
        return subtype_;
    }

    /**
     * Returns the parameter name/value pairs of this content type.
     * The parameter names (keys of the returned map) will always
     * be in lower case.
     *
     * @return  name/value pairs as an ordered map
     */
    public Map<String,String> getParameters() {
        return params_;
    }

    /**
     * Indicates whether the type and subtype match a given pair.
     *
     * @param  type  required type part (case-insensitive)
     * @param  subtype  required subtype part (case-insensitive)
     * @return  true iff type and subtype match those of this content-type
     */
    public boolean matches( String type, String subtype ) {
        return type_.equalsIgnoreCase( type )
            && subtype_.equalsIgnoreCase( subtype );
    }

    /**
     * Returns the value of a parameter of this content type.
     * 
     * @param   paramName  parameter name (case-insensitive)
     * @return  parameter value, or null if no such parameter
     */
    public String getParameter( String paramName ) {
        return params_.get( paramName.toLowerCase() );
    }

    @Override
    public int hashCode() {
        int code = 5501;
        code = 23 * code + type_.hashCode();
        code = 23 * code + subtype_.hashCode();
        code = 23 * code + params_.hashCode();
        return code;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof ContentType ) {
            ContentType other = (ContentType) o;
            return this.type_.equals( other.type_ )
                && this.subtype_.equals( other.subtype_ )
                && this.params_.equals( other.params_ );
        }
        else {
            return false;
        }
    }

    @Override
    public String toString() {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( type_ )
            .append( "/" )
            .append( subtype_ );
        for ( Map.Entry<String,String> entry : params_.entrySet() ) {
            sbuf.append( "; " )
                .append( entry.getKey() )
                .append( "=" )
                .append( quote( entry.getValue() ) );
        }
        return sbuf.toString();
    }

    /**
     * Parses a Content-Type (MIME type) string in accordance with
     * the syntax rules in RFC2045.
     * Such strings look something like
     * "<code>type/subtype[;p1=v1;p2=v2...]</code>".
     * It may not be completely bulletproof, but should do a fairly
     * good job of the parse.  However, it makes no attempt to
     * restrict the type or subtype to IANA-approved values,
     * and it may parse some strings which are not strictly legal.
     *
     * <p>Null is returned if the string cannot be parsed.
     *
     * @see  <a href="https://tools.ietf.org/html/rfc2045#section-5.1"
     *          >RFC 2045, sec 5.1</a>
     * @see  <a href="https://tools.ietf.org/html/rfc822#section-3.3"
     *          >RFC 822, sec 3.3</a>
     * @param  txt   content-type string of the approximate form
     *               <code>type/subtype(;param=value)*</code>
     * @return  ContentType object if <code>txt</code> can be parsed,
     *          otherwise null
     */
    public static ContentType parseContentType( String txt ) {
        if ( txt == null ) {
            return null;
        }
        Matcher tmatcher = TYPE_REGEX.matcher( txt );
        if ( tmatcher.matches() ) {
            String type = tmatcher.group( 1 );
            String subtype = tmatcher.group( 2 );
            String rest = tmatcher.group( 3 );
            Map<String,String> params = new LinkedHashMap<String,String>();
            Matcher pmatcher = PARAM_REGEX.matcher( rest );
            while ( pmatcher.find() ) {
                String name = pmatcher.group( 1 );
                String value = unquote( pmatcher.group( 2 ) );
                params.put( name, value );
            }
            return new ContentType( type, subtype, params );
        }
        else {
            return null;
        }
    }

    /**
     * Extracts the content from an RFC2045 <code>value</code> element.
     * If it's a token, it's returned unchanged, if it's quoted
     * then the quotes are stripped and the characters are unescaped.
     *
     * @param   txt   string assumed to be an RFC2045
     *                <code>token</code> or <code>quoted-string</code>
     * @reuturn   parsed value represented by txt
     */
    private static String unquote( String txt ) {
        if ( txt.startsWith( "\"" ) && txt.endsWith( "\"" ) ) {
            StringBuffer sbuf = new StringBuffer();
            for ( int i = 1; i < txt.length() - 1; ) {
                char c = txt.charAt( i++ );
                sbuf.append( c == '\\' ? txt.charAt( i++ ) : c );
            }
            return sbuf.toString();
        }
        else {
            return txt;
        }
    }

    /**
     * Prepares a string for use as a content-type parameter value.
     * If necessary it will wrap it in quotes, and within the quotes
     * it will escape characters as required.
     * If it can be used unquoted, it will be returned unchanged.
     *
     * @param   txt   parameter value text
     * @return   string suitable for representing a parameter value
     */
    @SuppressWarnings("fallthrough")
    private static String quote( String txt ) {
        if ( TOKEN_REGEX.matcher( txt ).matches() ) {
            return txt;
        }
        else {
            StringBuffer sbuf = new StringBuffer();
            sbuf.append( '"' );
            for ( int i = 0; i < txt.length(); i++ ) {
                char c = txt.charAt( i );
                switch ( c ) {
                    case '"':
                    case '\\':
                    case '\n':
                        sbuf.append( '\\' );
                    default:
                        sbuf.append( c );
                }
            }
            sbuf.append( '"' );
            return sbuf.toString();
        }
    }

    /**
     * Parses a single content-type string supplied on the command line,
     * and prints a representation of the parsed form on standard output.
     */
    public static void main( String[] args ) {
        ContentType ctype = parseContentType( args[ 0 ] );
        System.out.println( ctype == null ? "Bad content-type" : ctype );
    }
}
