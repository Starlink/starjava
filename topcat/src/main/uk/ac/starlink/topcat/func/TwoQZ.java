// The doc comments in this class are processed to produce user-visible
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical.

package uk.ac.starlink.topcat.func;

/**
 * Specialist functions for use with data from the the 2QZ survey.
 * Spectral data are taken directly from the 2QZ web site at
 * <a href="http://www.2dfquasar.org/">http://www.2dfquasar.org/</a>.
 *
 * @author   Mark Taylor (Starlink)
 * @since    3 Sep 2004
 */
public class TwoQZ {

    /** String prepended to the object NAME for the FITS spectra file URL. */
    public static final String TWOQZ_SPEC_BASE =
        "http://www.2dfquasar.org/fits/";

    /** String appended to the object NAME for the FITS spectra file URL. */
    public static final String TWOQZ_SPEC_TAIL = ".fits.gz";

    /** String prepended to the object NAME for the FITS postage stamp URL. */
    public static final String TWOQZ_FITS_IMAGE_BASE = 
        "http://www.2dfquasar.org/postfits/";

    /** String appended to the object NAME for the FITS postage stamp URL. */
    public static final String TWOQZ_FITS_IMAGE_TAIL = ".fits.gz";

    /** String prepended to the object NAME for the JPEG postage stamp URL. */
    public static final String TWOQZ_JPEG_IMAGE_BASE =
        "http://www.2dfquasar.org/postjpg/";

    /** String appended to the object NAME for the JPEG postage stamp URL. */
    public static final String TWOQZ_JPEG_IMAGE_TAIL = ".jpg";

    /**
     * Private constructor prevents instantiation.
     */
    private TwoQZ() {
    }

    /**
     * Displays the postage stamp FITS image for a 2QZ object in an
     * image viewer.
     *
     * @param  name  object name (NAME column)
     * @return  short log message
     */
    public static String image2QZ( String name ) {
        String loc = TWOQZ_FITS_IMAGE_BASE + getSubdir( name ) + name +
                     TWOQZ_FITS_IMAGE_TAIL;
        return Image.displayImage( "2QZ", loc );
    }

    /**
     * Displays the postage stamp JPEG image for a 2QZ object in an 
     * external viewer.
     *
     * @param  name  object name (NAME column)
     * @return  short log message
     */
    public static String jpeg2QZ( String name ) {
        String loc = TWOQZ_JPEG_IMAGE_BASE + getSubdir( name ) + name +
                     TWOQZ_JPEG_IMAGE_TAIL;
        return Image.displayImage( "2QZ", loc );
    }

    /**
     * Returns the name of the subdirectory (such as "ra03_04") for a 
     * given 2QZ object name (ID).
     * 
     * @param  name  ID of object within the 2QZ catalogue
     *         (like J120437.7-021003)
     * @return  subdirectory name
     */
    public static String get2qzSubdir( String name ) {
        return getSubdir( name );
    }

    private static String getSubdir( String name ) {
        int rah = Integer.parseInt( name.substring( 1, 3 ) );
        int rah1 = rah + 1;
        return "ra" + format2( rah ) + '_' + format2( rah1 ) + '/';
    }

    private static String format2( int num ) {
        String out = Integer.toString( num );
        return out.length() == 2 ? out
                                 : "0" + out.charAt( 0 );
    }

}
