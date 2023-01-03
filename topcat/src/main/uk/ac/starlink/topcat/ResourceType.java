package uk.ac.starlink.topcat;

import java.net.URL;
import java.util.Arrays;
import uk.ac.starlink.util.ContentType;

/**
 * Defines different kinds of resources that can be at the end of a URL.
 * This is used for the purposes of deciding what actions to invoke on it,
 * for instance in the context of DataLink.
 *
 * @author   Mark Taylor
 * @since    6 Feb 2018
 */
public enum ResourceType {

    /** Table. */
    TABLE,

    /** DataLink {links}-response table. */
    DATALINK,

    /** FITS file to be interpreted as 2D image data. */
    FITS_IMAGE,

    /** Some kind of spectrum. */
    SPECTRUM,

    /** Graphical image, typically not FITS (PNG, JPEG etc). */
    IMAGE,

    /** Something that can be displayed in a web browser, for instance HTML. */
    WEB,

    /** Unknown resource type. */
    UNKNOWN;

    /**
     * Attempts to determine the type of resource characterised by
     * available information.  Works on a best-efforts basis.
     *
     * @param  resInfo  resource information
     * @return  best guess at the type of resource corresponding to the
     *          given information
     */
    public static ResourceType guessResourceType( ResourceInfo resInfo ) {
        if ( resInfo == null ) {
            return UNKNOWN;
        }

        /* Unpack resource info. */
        String standardId = resInfo.getStandardId();
        String contentType = resInfo.getContentType();
        String contentQualifier = resInfo.getContentQualifier();
        URL url = resInfo.getUrl();

        /* Prepare information in a form convenient for matching. */
        ContentType ctype = ContentType.parseContentType( contentType );
        String contenttype = contentType == null ? null
                                                 : contentType.toLowerCase();
        String standardid = standardId == null ? null
                                               : standardId.toLowerCase();
        boolean isFits = ctype != null
                      && ( ctype.matches( "image", "fits" ) ||
                           ctype.matches( "application", "fits" ) );
        final String extension;
        if ( url != null ) {
            String path = url.getPath();
            int idot = path.lastIndexOf( '.' );
            extension = idot >= 0 ? path.substring( idot + 1 ).toLowerCase()
                                  : null;
        }
        else {
            extension = null;
        }

        /* Note that a product type of e.g. "spectrum" does not necessarily
         * point at a spectrum data file, it could point to another
         * datalink links file describing spectral data. */
        String prodtype = ( contentQualifier != null &&
                            contentQualifier.startsWith( "#" ) )
                         ? contentQualifier.substring( 1 ).trim().toLowerCase()
                         : null;
        boolean isNon2d = prodtype != null && ! "image".equals( prodtype );

        /* Go through the available information and pick the best match.
         * This is an ad hoc and messy business, since there is some
         * freedom in how the metadata is provided, not all items may be
         * present, and the output options are not completely orthogonal.
         * The details may be updated if something else works better. */
        if ( ( ctype != null &&
               "datalink".equalsIgnoreCase( ctype.getParameters()
                                                 .get( "content" ) ) ) ||
             ( standardid != null &&
               standardid.startsWith( "ivo://ivoa.net/std/datalink" ) ) ) {
            return DATALINK;
        }
        if ( ( ctype != null &&
               "votable".equalsIgnoreCase( ctype.getSubtype() ) ) ||
             ( contenttype != null &&
               contenttype.indexOf( "votable" ) >= 0 ) ) {
            return TABLE;
        }
        if ( ctype != null && ctype.matches( "text", "csv" ) ) {
            return TABLE;
        }
        if ( ctype != null && "html".equalsIgnoreCase( ctype.getSubtype() ) ) {
            return WEB;
        }
        if ( extension != null &&
             Arrays.asList( new String[] { "jpeg", "jpg", "png", "gif" } )
                   .contains( extension ) ) {
            return IMAGE;
        }
        if ( extension != null &&
             Arrays.asList( new String[] { "html", "htm", } )
                   .contains( extension ) ) {
            return WEB;
        }
        if ( "spectrum".equals( prodtype ) || "sed".equals( prodtype ) ) {
            return SPECTRUM;
        }
        if ( ctype != null && ctype.matches( "text", "xml" ) ) {
            return TABLE;
        }
        if ( ctype != null && ctype.matches( "image", "fits" ) ) {
            return isNon2d ? UNKNOWN : FITS_IMAGE;
        }
        if ( ctype != null && "image".equalsIgnoreCase( ctype.getType() ) ) {
            return IMAGE;
        }
        if ( standardid != null &&
             ( standardid.startsWith( "ivo://ivoa.net/std/conesearch" ) ||
               standardid.startsWith( "ivo://ivoa.net/std/tap" ) ) ) {
            return TABLE;
        }
        if ( "image".equals( prodtype ) ) {
            return isFits ? FITS_IMAGE : IMAGE;
        }
        if ( contenttype != null && contenttype.indexOf( "xml" ) >= 0 ) {
            return TABLE;
        }
        if ( contenttype != null && contenttype.indexOf( "fits" ) >= 0 ) {
            return isNon2d ? UNKNOWN : FITS_IMAGE;
        }
        if ( extension != null &&
             Arrays.asList( new String[] { "fits", "fit" } )
                   .contains( extension ) ) {
            return FITS_IMAGE;
        }
        if ( extension != null &&
             Arrays.asList( new String[] { "vot", "votable", "xml", "csv", } )
                           .contains( extension ) ) {
            return TABLE;
        }
        if ( "timeseries".equals( prodtype ) ) {
            return TABLE;
        }
        return UNKNOWN;
    }
}
