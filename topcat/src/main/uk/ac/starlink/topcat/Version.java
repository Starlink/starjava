package uk.ac.starlink.topcat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Comparable class which can compare version strings.
 * Versions are of the form A.B[-C[x]], where the square bracketed parts
 * are optional.  A, B and C must be integers if present.  x may be freeform.
 *
 * <p>For comparison, only A, B and C parts are considered.  If C is missing,
 * it is considered equal to zero.  For equality, the original version
 * string is used.
 *
 * <p><strong>Note:</strong>
 * this class has a natural ordering that is inconsistent with equals.
 *
 * @author   Mark Taylor
 * @since    20 Dec 2010
 */
public class Version implements Comparable {

    private static Pattern VERSION_REGEX =
        Pattern.compile( "([0-9+])\\.([0-9+])(?:-([0-9+]))?(.*)" );
    private final String vstr_;
    private final int major_;
    private final int minor_;
    private final int update_;
    private final String trail_;

    /**
     * Constructor.
     *
     * @param  vstr   version string
     */
    Version( String vstr ) {
        vstr_ = vstr;
        Matcher matcher = VERSION_REGEX.matcher( vstr );
        if ( ! matcher.matches() ) {
            throw new IllegalArgumentException( "Bad version number format: "
                                              + vstr );
        }
        try {
            major_ = Integer.parseInt( matcher.group( 1 ) );
            minor_ = Integer.parseInt( matcher.group( 2 ) );
            String supd = matcher.group( 3 );
            update_ = supd == null || supd.trim().length() == 0
                    ? 0
                    : Integer.parseInt( matcher.group( 3 ) );
            trail_ = matcher.group( 4 );
        }
        catch ( RuntimeException e ) {
            throw (IllegalArgumentException)
                  new IllegalArgumentException( "Bad version number format: "
                                              + vstr )
                 .initCause( e );
        }
    }

    public boolean equals( Object o ) {
        return o instanceof Version && ((Version) o).vstr_.equals( this.vstr_ );
    }

    public int hashCode() {
        return vstr_.hashCode();
    }

    public int compareTo( Object o ) {
        Version other = (Version) o;
        int cmp = this.major_ - other.major_;
        if ( cmp == 0 ) {
            cmp = this.minor_ - other.minor_;
            if ( cmp == 0 ) {
                cmp = this.update_ - other.update_;
            }
        }
        return cmp;
    }
}
