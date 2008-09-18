package uk.ac.starlink.topcat.interop;

import java.io.IOException;
import javax.swing.JComboBox;

/**
 * Activity for sending an image of some sort to load.
 *
 * @author   Mark Taylor
 * @since    18 Sep 2008
 */
public interface ImageActivity extends Activity {

    /** Format string indicating FITS image. */
    public static final String FORMAT_FITS = "FITS";

    /** Format string indicating JPEG image. */
    public static final String FORMAT_JPEG = "JPEG";

    /** Format string indicating GIF image. */
    public static final String FORMAT_GIF = "GIF";

    /** Format string indicating PNG image. */
    public static final String FORMAT_PNG = "PNG";

    /** Array of all known formats. */
    public static final String[] KNOWN_FORMATS = new String[] {
        FORMAT_FITS, FORMAT_JPEG, FORMAT_GIF, FORMAT_PNG,
    };

    /**
     * Returns a combo box for selecting image format.
     * A suitable list of entries for this box is {@link #KNOWN_FORMATS}.
     *
     * @return  format selector
     */
    JComboBox getFormatSelector();

    /**
     * Displays an image according to the current selections of the 
     * components owned by this activity.
     *
     * @param   location   string giving file name or URL location of file
     * @param   label   label for display target
     */
    void displayImage( String location, String label ) throws IOException;
}
