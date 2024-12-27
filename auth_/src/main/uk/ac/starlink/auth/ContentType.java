package uk.ac.starlink.auth;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple abstraction of MIME type.
 * This class represents the basic MIME type/subtype,
 * but does not attempt to represent content-type parameters.
 *
 * <p>This class is a stripped-down version of
 * <code>uk.ac.starlink.util.ContentType</code>.
 *
 * @author   Mark Taylor
 * @since   20 May 2021
 * @see  <a href="https://tools.ietf.org/html/rfc2045">RFC 2045</a>
 */
public class ContentType {

    private final String type_;
    private final String subtype_;
    private static final Pattern TYPE_REGEX =
        Pattern.compile( "\\s*([-A-Za-z0-9_]+)\\s*/\\s*([-A-Za-z0-9_]+).*" );

    /**
     * Constructs a ContentType from type and subtype strings.
     * Case is normalised (to lower case).
     *
     * @param   type  type part
     * @param   subtype  subtype part
     */
    public ContentType( String type, String subtype ) {
        type_ = type;
        subtype_ = subtype;
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

    @Override
    public int hashCode() {
        int code = 5501;
        code = 23 * code + type_.hashCode();
        code = 23 * code + subtype_.hashCode();
        return code;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof ContentType ) {
            ContentType other = (ContentType) o;
            return this.type_.equals( other.type_ )
                && this.subtype_.equals( other.subtype_ );
        }
        else {
            return false;
        }
    }

    @Override
    public String toString() {
        return type_ + "/" + subtype_;
    }

    /**
     * Parses a Content-Type (MIME type) string.
     *
     * @param  txt  text representation of MIME type
     * @return   ContentType instance if syntax matches, else null
     */
    public static ContentType parse( String txt ) {
        if ( txt == null ) {
            return null;
        }
        Matcher matcher = TYPE_REGEX.matcher( txt );
        return matcher.matches()
             ? new ContentType( matcher.group( 1 ), matcher.group( 2 ) )
             : null;
    }
}
