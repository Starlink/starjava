// The doc comments in this class are processed to produce user-visible
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical.

package uk.ac.starlink.topcat.func;

import java.util.HashMap;
import java.util.Map;
import javax.swing.SwingUtilities;
import uk.ac.starlink.sog.SOG;
import uk.ac.starlink.sog.SOGNavigatorImageDisplay;
import uk.ac.starlink.sog.SOGNavigatorImageDisplayFrame;
import uk.ac.starlink.sog.SOGNavigatorImageDisplayInternalFrame;
import uk.ac.starlink.topcat.TopcatUtils;

/**
 * Functions for display of images in external viewer SOG
 * (<a href="http://www.starlink.ac.uk/sog/">http://www.starlink.ac.uk/sog/</a>).
 *
 * @author   Mark Taylor (Starlink)
 * @since    20 Aug 2004
 */
public class Sog {

    private static SOG sog_;
    private static Map soggers_ = new HashMap();

    /**
     * Private constructor prevents instantiation.
     */
    private Sog() {
    }

    /**
     * Displays the file at a given location as an image
     * in a graphical (SoG) viewer.
     * <code>label</code> may be any string which identifies the window 
     * for display, so that multiple images may be displayed in different
     * windows without getting in each others' way.
     * <code>loc</code> should be a filename or URL, pointing to an image in
     * a format that SOG understands (this includes FITS, compressed FITS,
     * and NDFs).
     *
     * @param  label  identifies the window in which the image will be displayed
     * @param  loc  image location
     * @return  short log message
     */
    public static String sog( String label, String loc ) {
        if ( ! TopcatUtils.canSog() ) {
            return "Error: SOG classes not available";
        }
        if ( loc == null || loc.trim().length() == 0 ) {
            return "no image location";
        }
        SOGNavigatorImageDisplay sog = getSogger( label );
        try {
            sog.setFilename( loc, false );
            return "sog(" + loc + ")";
        }
        catch ( Exception e ) {
            return "<Error: " + e.getMessage() + ">";
        }
    }

    /**
     * Returns a labelled Sog window instance.  If one by this label has
     * been requested before, the old one will be returned, otherwise a
     * new one will be created.
     *
     * @param  label  window label
     */
    private static SOGNavigatorImageDisplay getSogger( String label ) {
        assert TopcatUtils.canSog();
        if ( sog_ == null ) {
            synchronized ( Sog.class ) {
                sog_ = new SOG();
                sog_.setDoExit( false );
            }
        }
        if ( ! soggers_.containsKey( label ) ) {
            SOGNavigatorImageDisplay rootDisplay =
                 (SOGNavigatorImageDisplay) sog_.getImageDisplay();
            SwingUtilities.windowForComponent( rootDisplay )
                          .setVisible( false );
            Object win = rootDisplay.newWindow();
            SOGNavigatorImageDisplay sogger;
            if ( win instanceof SOGNavigatorImageDisplayFrame ) {
                sogger = (SOGNavigatorImageDisplay)
                         ((SOGNavigatorImageDisplayFrame) win)
                        .getImageDisplayControl().getImageDisplay();
            }
            else if ( win instanceof SOGNavigatorImageDisplayInternalFrame ) {
                sogger = (SOGNavigatorImageDisplay)
                         ((SOGNavigatorImageDisplayInternalFrame) win)
                        .getImageDisplayControl().getImageDisplay();
            }
            else {
                throw new AssertionError();
            }
            sogger.setDoExit( false );
            sogger.setTitle( label );
            soggers_.put( label, sogger );
        }
        SOGNavigatorImageDisplay sogger =
            (SOGNavigatorImageDisplay) soggers_.get( label );
        if ( ! sogger.isShowing() ) {
            SwingUtilities.windowForComponent( sogger ).setVisible( true );
        }
        return sogger;
    }
}

