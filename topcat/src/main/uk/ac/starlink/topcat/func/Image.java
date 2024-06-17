// The doc comments in this class are processed to produce user-visible
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical.

package uk.ac.starlink.topcat.func;

/**
 * Functions for display of images in a window.
 * Supported image formats include GIF, JPEG, PNG and FITS,
 * which may be compressed.
 *
 * @author   Mark Taylor (Starlink)
 * @since    1 Oct 2004
 */
public class Image {

    /**
     * Private constructor prevents instantiation.
     */
    private Image() {
    }

    /**
     * Displays the file at the given location in an image viewer.
     *
     * @param  label  identifies the window in which the imag will be
     *         displayed
     * @param  location  image location - may be a filename or URL
     * @return  short log message
     */
    public static String displayImage( String label, String location ) {
        return BasicImageDisplay.displayBasicImage( label, location );
    }
}
