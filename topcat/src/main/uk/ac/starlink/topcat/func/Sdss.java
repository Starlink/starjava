// The doc comments in this class are processed to produce user-visible
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical.

package uk.ac.starlink.topcat.func;

/**
 * Specialist display functions for use with the Sloane Digital Sky Server.
 *
 * @author   Mark Taylor (Starlink)
 * @since    25 Aug 2004
 */
public class Sdss {

    /** Base URL for SkyServer JPEG retrieval service. */
    public static final String BASE_URL = 
        "http://skyservice.pha.jhu.edu/dr2/ImgCutout/getjpeg.aspx";

    /**
     * Private constructor prevents instantiation.
     */
    private Sdss() {
    }

    /**
     * Displays a colour cutout image of a specified size from the 
     * SDSS around a given sky position.  The displayed image is square, 
     * a given number of (0.4arcsec) pixels on each side.
     *
     * @param  ra  Right Ascension in radians
     * @param  dec Declination in radians
     * @param  pixels  size of displayed image in SDSS pixels
     * @return  short log message
     */
    public static String sdssCutout( double ra, double dec, int pixels ) {
        String query = new CgiQuery( BASE_URL )
             .addArgument( "ra", Math.toDegrees( ra ) )
             .addArgument( "dec", Math.toDegrees( dec ) )
             .addArgument( "height", pixels )
             .addArgument( "width", pixels )
             .toString();
        return Image.displayImage( query );
    }

    /**
     * Displays a colour cutout image of a specified size from the SDSS
     * around a given sky position with pixels of a given size.
     * Pixels are square, and their size on the sky is specified by
     * the <code>scale</code> argument.  The displayed image has 
     * <code>pixels</code> pixels along each side.
     *
     * @param  ra  Right Ascension in radians
     * @param  dec Declination in radians
     * @param  pixels  size of displayed image in SDSS pixels
     * @param  scale   pixel size in arcseconds
     * @return  short log message
     */
    public static String sdssCutout( double ra, double dec, int pixels,
                                     double scale ) {
        String query = new CgiQuery( BASE_URL )
             .addArgument( "ra", Math.toDegrees( ra ) )
             .addArgument( "dec", Math.toDegrees( dec ) )
             .addArgument( "height", pixels )
             .addArgument( "width", pixels )
             .addArgument( "scale", scale )
             .toString();
        return Image.displayImage( query );
    }

    /**
     * Displays a colour cutout image of a default size from the SDSS
     * around a given sky position.  The displayed image is 128 pixels
     * square - a pixel is 0.4arcsec.
     *
     * @param  ra  Right Ascension in radians
     * @param  dec Declination in radians
     * @return  short log message
     */
    public static String sdssCutout( double ra, double dec ) {
        return sdssCutout( ra, dec, 128 );
    }

}
