package uk.ac.starlink.ttools.plot;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Transparency;
import java.awt.Image;
import java.awt.ImageCapabilities;
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
    private final Integer transparency_;
    private Image cachedImage_;

    private static final int MAX_TRY = 3;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.plot" );

    /**
     * Constructor.
     *
     * @param  picture  picture to paint
     * @param  caching  whether to cache pixel data between paintIcon calls
     * @param  transparency  integer value of Transparency code, or null
     */
    private PictureImageIcon( Picture picture, boolean caching,
                              Integer transparency ) {
        picture_ = picture;
        caching_ = caching;
        if ( transparency != null ) {
            switch ( transparency.intValue() ) {
                case Transparency.OPAQUE:
                case Transparency.BITMASK:
                case Transparency.TRANSLUCENT:
                    break;
                default:
                    throw new IllegalArgumentException( "Unknown transparency "
                                                      + transparency );
            }
        }
        transparency_ = transparency;
    }

    /**
     * Constructs an icon with specific transparency characteristics.
     *
     * @param   picture  picture to paint
     * @param   caching  true iff painted image is cached rather than
     *                   just drawn off-screen every time
     * @param   transparency  transparency mode
     * @see  java.awt.Transparency#OPAQUE
     * @see  java.awt.Transparency#BITMASK
     * @see  java.awt.Transparency#TRANSLUCENT
     */
    public PictureImageIcon( Picture picture, boolean caching,
                             int transparency ) {
        this( picture, caching, new Integer( transparency ) );
    }

    /**
     * Constructs an icon with default transparency.
     *
     * @param   picture  picture to paint
     * @param   caching  true iff painted image is cached rather than
     *                   just drawn off-screen every time
     */
    public PictureImageIcon( Picture picture, boolean caching ) {
        this( picture, caching, null );
    }

    public int getIconWidth() {
        return picture_.getPictureWidth();
    }

    public int getIconHeight() {
        return picture_.getPictureHeight();
    }

    public void paintIcon( Component c, Graphics g, int x, int y ) {
        final Image image;
        if ( cachedImage_ != null ) {
            image = cachedImage_;
        }
        else {
            image = createImage( ((Graphics2D) g).getDeviceConfiguration(),
                                 c.getBackground() );
            if ( caching_ ) {
                cachedImage_ = image;
            }
        }
        g.drawImage( image, x, y, null );
    }

    /**
     * Forces creation of a cached image for display.
     * If this is not called explicitly then in caching mode a cached
     * image will be created when this icon is first painted.
     *
     * @param   gc   graphics config in which this icon will be displayed
     * @param   bg   background colour for image; null is legal but may
     *               lead to unpredictable effects.
     */
    public void cacheImage( GraphicsConfiguration gc, Color bg ) {
        cachedImage_ = createImage( gc, bg );
    }

    /**
     * Creates an image containing the graphic content of this icon,
     * suitable for caching or painting to a graphics context.
     *
     * @param   gc   graphics config in which this icon will be displayed
     * @param   bg   background colour for image; null is legal but may
     *               lead to unpredictable effects.
     * @return  image containing picture graphics
     */
    private BufferedImage createImage( GraphicsConfiguration gc, Color bg ) {
        int w = picture_.getPictureWidth();
        int h = picture_.getPictureHeight();

        /* Try a volatile image.  It may be much faster, though it's
         * harder to handle. */
        VolatileImage vim =
              transparency_ == null
            ? gc.createCompatibleVolatileImage( w, h )
            : gc.createCompatibleVolatileImage( w, h,
                                                transparency_.intValue() );
        ImageCapabilities imCaps = vim.getCapabilities();
        if ( logger_.isLoggable( Level.CONFIG ) ) {
            String msg = new StringBuffer()
                .append( "Painting picture to " )
                .append( caching_ ? "cached " : "" )
                .append( "image: " )
                .append( imCaps.isAccelerated() ? "accelerated"
                                                : "not accelerated" )
                .append( ", " )
                .append( imCaps.isTrueVolatile() ? "volatile"
                                                 : "not volatile" )
                .toString();
            logger_.config( msg );
        }
        for ( int iLost = 0; iLost < MAX_TRY; iLost++ ) {
            vim.validate( gc );
            Graphics2D gv = vim.createGraphics();
            doPaint( gv, bg );
            boolean lost = vim.contentsLost();
            BufferedImage im = lost ? null : vim.getSnapshot();
            lost = lost || vim.contentsLost();
            gv.dispose();
            if ( lost ) {
                logger_.info( "Lost volatile image during draw" );
            }
            else {
                vim.flush();
                return im;
            }
        }
        vim.flush();
        logger_.warning( "Draw to volatile image failed after "
                       + MAX_TRY + " attempts - draw direct" );

        /* If attempts to draw to a volatile image fail repeatedly,
         * fall back to a normal buffered image. */
        BufferedImage bim =
              transparency_ == null
            ? gc.createCompatibleImage( w, h )
            : gc.createCompatibleImage( w, h, transparency_.intValue() );
        Graphics2D gb = bim.createGraphics();
        doPaint( gb, bg );
        gb.dispose();
        return bim;
    }

    /**
     * Does the actual painting.
     *
     * @param  g  graphics context
     * @param  bg  background colour, or null
     */
    private void doPaint( Graphics2D g, Color bg ) {
        if ( bg != null ) {
            Color color = g.getColor();
            g.setColor( bg );
            g.fillRect( 0, 0, picture_.getPictureWidth(),
                              picture_.getPictureHeight() );
            g.setColor( color );
        }
        try {
            picture_.paintPicture( g );
        }
        catch ( IOException e ) {
            logger_.log( Level.WARNING, "Graphic plotting IO error", e );
            g.drawString( e.toString(), 10, picture_.getPictureHeight() / 2 );
        }
    }
}
