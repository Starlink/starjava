// The doc comments in this class are processed to produce user-visible
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical.

package uk.ac.starlink.topcat.func;

import java.util.HashMap;
import java.util.Map;
import uk.ac.starlink.topcat.ImageWindow;

/**
 * Functions for display of graphics-format images in a no-frills 
 * viewing window (an <code>ImageWindow</code>).
 * Supported image formats include GIF, JPEG, PNG and FITS,
 * which may be compressed.
 *
 * @author   Mark Taylor (Starlink)
 * @since    1 Oct 2004
 */
public class BasicImageDisplay {

    private static Map viewers_ = new HashMap();

    /**
     * Private constructor prevents instantiation.
     */
    private BasicImageDisplay() {
    }

    /**
     * Displays the file at a given location as an image
     * in a graphical viewer.
     * <code>label</code> may be any string which identifies the window
     * for display, so that multiple images may be displayed in different
     * windows without getting in each others' way.
     * <code>loc</code> should be a filename or URL, pointing to an image in
     * a format that this viewer understands.
     *
     * @param  label  identifies the window in which the image will be displayed
     * @param  loc  image location
     * @return  short log message
     */
    public static String displayBasicImage( String label, String loc ) {
        getImageWindow( label ).setImage( loc );
        return "viewImage(" + loc + ")";
    }

    /**
     * Returns a labelled image window.
     * If one with this label has been requested before, the old one will
     * be returned, otherwise a new one will be created.
     *
     * @param  label  window label
     * @return  new or old viewer
     */
    private static ImageWindow getImageWindow( String label ) {
        if ( ! viewers_.containsKey( label ) ) {
            ImageWindow viewer = new ImageWindow( null );
            viewer.setTitle( label );
            viewers_.put( label, viewer );
        }
        ImageWindow viewer = (ImageWindow) viewers_.get( label );
        if ( ! viewer.isShowing() ) {
            viewer.setVisible( true );
        }
        return viewer;
    }
}
