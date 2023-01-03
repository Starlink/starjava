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

    /** FITS file to be interpreted as image data. */
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
        String standardId = resInfo.getStandardId();
        String contentType = resInfo.getContentType();
        URL url = resInfo.getUrl();
        ContentType ctype = ContentType.parseContentType( contentType );
        if ( standardId != null ) {
            String standardid = standardId.toLowerCase();
            if ( standardid.startsWith( "ivo://ivoa.net/std/datalink" ) ) {
                return DATALINK;
            }
            if ( standardid.startsWith( "ivo://ivoa.net/std/conesearch" ) ||
                 standardid.startsWith( "ivo://ivoa.net/std/tap" ) ) {
                return TABLE;
            }
            if ( standardid.startsWith( "ivo://ivoa.net/std/sia" ) ) {
                if ( ctype != null ) {
                    if ( ctype.matches( "image", "fits" ) ||
                         ctype.matches( "application", "fits" ) ) {
                        return FITS_IMAGE;
                    }
                }
                return IMAGE;
            }
            if ( standardid.startsWith( "ivo://ivoa.net/std/ssa" ) ) {
                return SPECTRUM;
            }
        }
        if ( ctype != null ) {
            if ( "datalink".equalsIgnoreCase( ctype.getParameters()
                                                   .get( "content" ) ) ) {
                return DATALINK;
            }
            if ( "votable".equalsIgnoreCase( ctype.getSubtype() ) ) {
                return TABLE;
            }
            if ( "html".equalsIgnoreCase( ctype.getSubtype() ) ) {
                return WEB;
            }
            if ( ctype.matches( "text", "xml" ) ||
                 ctype.matches( "text", "csv" ) ) {
                return TABLE;
            }
            if ( ctype.matches( "image", "fits" ) ) {
                return FITS_IMAGE;
            }
            if ( "image".equalsIgnoreCase( ctype.getType() ) ) {
                return IMAGE;
            }
        }
        if ( contentType != null ) {
            String contenttype = contentType.toLowerCase();
            if ( contenttype.indexOf( "votable" ) >= 0 ||
                 contenttype.indexOf( "xml" ) >= 0 ) {
                return TABLE;
            }
            if ( contenttype.indexOf( "fits" ) >= 0 ) {
                return FITS_IMAGE;
            }
        }
        if ( url != null ) {
            String path = url.getPath();
            int idot = path.lastIndexOf( '.' );
            if ( idot >= 0 ) {
                String exten = path.substring( idot + 1 ).toLowerCase();
                if ( Arrays.asList( new String[] { "fits", "fit" } )
                           .contains( exten ) ) {
                    return FITS_IMAGE;
                }
                if ( Arrays.asList( new String[] { "jpeg", "jpg",
                                                   "png", "gif" } )
                           .contains( exten ) ) {
                    return IMAGE;
                }
                if ( Arrays.asList( new String[] { "vot", "votable", "xml",
                                                   "csv", } )
                           .contains( exten ) ) {
                    return TABLE;
                }
                if ( Arrays.asList( new String[] { "html", "htm", } )
                           .contains( exten ) ) {
                    return WEB;
                }
            }
        }
        return UNKNOWN;
    }
}
