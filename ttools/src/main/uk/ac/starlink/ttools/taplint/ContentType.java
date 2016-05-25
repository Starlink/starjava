package uk.ac.starlink.ttools.taplint;

import java.util.Arrays;
import java.net.URL;

/**
 * Defines a permitted class of MIME types.
 *
 * @author   Mark Taylor
 * @since    24 May 2016
 */
public class ContentType {

    private final String[] permittedTypes_;

    /**
     * Constructor.
     *
     * @param  permittedTypes  array of MIME type/subtype strings allowed
     */
    public ContentType( String[] permittedTypes ) {
        permittedTypes_ = permittedTypes;
    }

    /**
     * Checks a declared Content-Type string against the permitted
     * values for this object.  Validation messages are reported in
     * case of non-compliance.
     *
     * @param  reporter  destination for validation messages
     * @param  declaredType  Content-Type to assess
     * @param  url  source of content, used for report messages
     */
    public void checkType( Reporter reporter, String declaredType, URL url ) {
        if ( declaredType == null || declaredType.trim().length() == 0 ) {
            reporter.report( FixedCode.W_NOCT,
                             "No Content-Type header for " + url );
        }
        else if ( ! isPermitted( declaredType ) ) {
            StringBuffer sbuf = new StringBuffer();
            sbuf.append( "Incorrect Content-Type \"" )
                .append( declaredType )
                .append( "\"" )
                .append( ", should be " );
            if ( permittedTypes_.length == 1 ) {
                sbuf.append( permittedTypes_[ 0 ] );
            }
            else {
                sbuf.append( "one of " )
                    .append( Arrays.asList( permittedTypes_ ) );
            }
            sbuf.append( " for " )
                .append( url );
            reporter.report( FixedCode.E_GMIM, sbuf.toString() );
        }
    }

    /**
     * Indicates whether the given content-type matches one of the
     * ones permitted for this object.  Matching is currently startsWith,
     * so parameter assignments (and possibly illegal suffixes)
     * are ignored.
     *
     * @param  declaredType  content-type to assess
     * @return  isPermitted  true iff it's OK
     */
    private boolean isPermitted( String declaredType ) {
        for ( String typ : permittedTypes_ ) {
            if ( declaredType.startsWith( typ ) ) {
                return true;
            }
        }
        return false;
    }
}
