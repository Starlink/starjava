// The doc comments in this class are processed to produce user-visible
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical.

package uk.ac.starlink.topcat.func;

import java.util.ArrayList;
import java.util.List;
import uk.ac.starlink.topcat.ImageWindow;
import uk.ac.starlink.topcat.TopcatUtils;

/**
 * Functions for display of images in a window.
 * Supported image formats include GIF, JPEG, PNG and FITS,
 * which may be compressed.
 * The SoG program 
 * (<a href="http://www.starlink.ac.uk/sog/">http://www.starlink.ac.uk/sog/</a>)
 * will be used if it is available, otherwise a no-frills image viewer
 * will be used instead.
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
        if ( TopcatUtils.canSog() ) {
            return Sog.sog( label, location );
        }
        else {
            return BasicImageDisplay.displayBasicImage( label, location );
        }
    }
}
