// The doc comments in this class are processed to produce user-visible
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical.

package uk.ac.starlink.topcat.func;

import uk.ac.starlink.ttools.func.Strings;

/**
 * Specialist functions for use with data from the the Millennium Galaxy
 * Survey.
 *
 * @author   Mark Taylor (Starlink)
 * @since    15 Oct 2004
 */
public class Mgc {

    /** String prepended to MGC_ID for the FITS image URL. */
    public static final String MGC_IMAGE_BASE =
        "http://www.eso.org/~jliske/mgc/data/pstamps/MGC";

    /** String appended to MGC_ID for the FITS image URL. */
    public static final String MGC_IMAGE_TAIL = "B.fit.gz";

    /**
     * Private constructor prevents instantiation.
     */
    private Mgc() {
    }

    /**
     * Displays the postage stamp FITS image for an MGC object in an
     * image viewer.
     *
     * @param  mgc_id  the MGC_ID number for the object
     * @return  short log string
     */
    public static String imageMgc( int mgc_id ) {
        String url = MGC_IMAGE_BASE 
                   + Strings.padWithZeros( mgc_id, 5 ) 
                   + MGC_IMAGE_TAIL;
        return Image.displayImage( "MGC", url );
    }
}
