package uk.ac.starlink.topcat;

import edu.jhu.pha.sdss.fits.imageio.FITSReaderSpi;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import uk.ac.starlink.util.DataSource;

/**
 * Window which displays an image using AWT.
 * Image types supported at J2SE1.4 are JPEG, GIF, PNG (I think).
 *
 * @author   Mark Taylor (Starlink)
 * @since    5 Oct 2004
 */
public class ImageWindow extends AuxWindow {

    JLabel label_;

    static {
        IIORegistry.getDefaultInstance()
                   .registerServiceProvider( new FITSReaderSpi() );
    }

    /**
     * Constructs a new image window.
     *
     * @param  parent window
     */
    public ImageWindow( Component parent ) {
        super( "Image Viewer", parent );
        JComponent main = getMainArea();
        main.setLayout( new BorderLayout() );
        main.setBackground( Color.WHITE );
        label_ = new JLabel();
        label_.setHorizontalAlignment( SwingConstants.CENTER );
        label_.setVerticalAlignment( SwingConstants.CENTER );
        JScrollPane scroller = new JScrollPane( label_ );
        scroller.setBackground( Color.WHITE );
        main.setPreferredSize( new Dimension( 240, 240 ) );
        main.add( scroller, BorderLayout.CENTER );
        addHelp( "imageViewers" );
    }

    /**
     * Sets the image to load from a given location.
     * This should be called from the event dispatch thread, but will
     * do most of the work out-of-thread to prevent blocking when loading
     * an image.
     *
     * @param  location  image filename or URL
     */
    public void setImage( final String location ) {

        /* Read the stream out of the event dispatch thread and when the data
         * has arrived set the icon to display it.  I know that ImageIcon
         * itself provides a lot of this functionality, but there are two
         * reasons I don't want to use that: 1. it doesn't decode compressed
         * streams (though image formats do not usually have extra compression,
         * so this doesn't matter too much) and 2. ImageIcon does its own
         * caching of image data, which doesn't get freed, so you can end
         * up running out of memory if you view a lot of images.
         * We get more control doing it like this. */
        new Thread( "Image Loader(" + location + ")" ) {
            Image im;
            public void run() {
                byte[] buf;
                String txt;
                try {
                    DataSource datsrc = DataSource.makeDataSource( location );
                    im = ImageIO.read( datsrc.getInputStream() );
                    txt = null;
                }
                catch ( IOException e ) {
                    im = null;
                    txt = "Can't load image: " + e.getMessage();
                }
                final Icon icon = im == null ? null : new ImageIcon( im );
                final String text = txt;
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        label_.setText( text );
                        label_.setIcon( icon );
                    }
                } );
            }
        }.start();
    }
}
