package uk.ac.starlink.ttools.taplint;

import java.util.Arrays;
import java.net.URL;
import uk.ac.starlink.util.ContentType;

/**
 * Defines a permitted class of MIME types.
 *
 * @author   Mark Taylor
 * @since    24 May 2016
 */
public class ContentTypeOptions {

    private final ContentType[] permittedTypes_;

    /**
     * Constructor.
     *
     * @param  permittedTypes  array of MIME type/subtype strings allowed
     */
    public ContentTypeOptions( ContentType[] permittedTypes ) {
        permittedTypes_ = permittedTypes;
    }

    /**
     * Checks a declared Content-Type string against the permitted
     * values for this object.  Validation messages are reported in
     * case of non-compliance.
     *
     * @param  reporter  destination for validation messages
     * @param  declaredTypeTxt  Content-Type to assess
     * @param  url  source of content, used for report messages
     */
    public void checkType( Reporter reporter, String declaredTypeTxt,
                           URL url ) {
        if ( declaredTypeTxt == null || declaredTypeTxt.trim().length() == 0 ) {
            reporter.report( FixedCode.W_NOCT,
                             "No Content-Type header for " + url );
        }
        else {
            ContentType declaredType =
                ContentType.parseContentType( declaredTypeTxt );
            if ( declaredType == null ) {
                reporter.report( FixedCode.E_BMIM,
                                 "Bad Content-Type syntax: "
                               + declaredTypeTxt );
            }
            else if ( ! isPermitted( declaredType ) ) {
                StringBuffer sbuf = new StringBuffer();
                sbuf.append( "Incorrect Content-Type \"" )
                    .append( declaredTypeTxt )
                    .append( "\"" )
                    .append( ", should match " );
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
    private boolean isPermitted( ContentType declaredType ) {
        for ( ContentType typ : permittedTypes_ ) {
            if ( declaredType.matches( typ.getType(), typ.getSubtype() ) ) {
                return true;
            }
        }
        return false;
    }
}
