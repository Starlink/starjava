package uk.ac.starlink.ttools.plot;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;

/**
 * Adapter class that makes a Picture usable as an Icon, suitable for
 * use in a pixel (non-vector) context.
 * Rendering is done to an off-screen volatile image for performance.
 * This rendered image is optionally cached for subsequent use.
 *
 * @author   Mark Taylor
 * @since    25 Jan 2012
 */
public class PictureImageIcon implements Icon {

    private final Picture picture_;
    private final boolean caching_;
    private BufferedImage image_;

    private static final int MAX_TRY = 3;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.plot" );

    /**
     * Constructor.
     *
     * @param  picture  picture to paint
     * @param  caching  whether to cache pixel data between paintIcon calls
     */
    public PictureImageIcon( Picture picture, boolean caching ) {
        picture_ = picture;
        caching_ = caching;
    }

    public int getIconWidth() {
        return picture_.getPictureWidth();
    }

    public int getIconHeight() {
        return picture_.getPictureHeight();
    }

    public void paintIcon( Component c, Graphics g, int x, int y ) {
        int w = picture_.getPictureWidth();
        int h = picture_.getPictureHeight();
        Graphics2D g2 = (Graphics2D) g.create( x, y, w, h );

        /* If image is cached, just draw it. */
        if ( image_ != null ) {
            g2.drawImage( image_, 0, 0, null );
        }

        /* Otherwise, draw to an off-screen image, possibly cache it,
         * then copy it to the screen. */
        else {
            GraphicsConfiguration gc = g2.getDeviceConfiguration();
            VolatileImage vim = gc.createCompatibleVolatileImage( w, h );
            boolean done = false;
            for ( int iLost = 0; ! done && iLost < MAX_TRY; iLost++ ) {
                vim.validate( gc );
                Graphics2D gv = vim.createGraphics();
                doPaint( gv );
                if ( caching_ ) {
                    image_ = vim.getSnapshot();
                }
                g2.drawImage( vim, 0, 0, null );
                if ( vim.contentsLost() ) {
                    logger_.info( "Lost volatile image during draw" );
                }
                else {
                    done = true;
                }
                gv.dispose();
            }
            vim.flush();
            if ( ! done ) {
                logger_.warning( "Draw to volatile image failed after "
                               + MAX_TRY + " attempts - draw direct" );
                doPaint( g2 );
            }
        }
    }

    /**
     * Does the actual painting.
     *
     * @param  g  graphics context
     */
    private void doPaint( Graphics2D g ) {
        try {
            picture_.paintPicture( g );
        }
        catch ( IOException e ) {
            logger_.log( Level.WARNING, "Graphic plotting IO error", e );
            g.drawString( e.toString(), 10, picture_.getPictureHeight() / 2 );
        }
    }
}
