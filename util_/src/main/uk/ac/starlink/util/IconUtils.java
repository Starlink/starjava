package uk.ac.starlink.util;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

/**
 * Utility functions related to Icons.
 *
 * @author   Mark Taylor
 * @since    6 Mar 2013
 */
public class IconUtils {

    private static Component dummyComponent_;

    /**
     * Private constructor prevents instantiation.
     */
    private IconUtils() {
    }

    /**
     * Returns an icon with a given size and no content.
     *
     * @param  width   icon width
     * @param  height  icon height
     */
    public static Icon emptyIcon( final int width, final int height ) {
        return new Icon() {
            public int getIconWidth() {
                return width;
            }
            public int getIconHeight() {
                return height;
            }
            public void paintIcon( Component c, Graphics g, int x, int y ) {
            }
        };
    }

    /**
     * Modifies an existing icon by changing its colour.
     * The colour attribute of the graphics context is changed before the
     * icon is painted.
     *
     * @param  icon  input icon
     * @param  color   colour to use as default for painting
     * @return  output icon
     */
    public static Icon colorIcon( final Icon icon, final Color color ) {
        return new WrapperIcon( icon ) {
            @Override
            public void paintIcon( Component c, Graphics g, int x, int y ) {
                Color color0 = g.getColor();
                g.setColor( color );
                super.paintIcon( c, g, x, y );
                g.setColor( color0 );
            }
        };
    }

    /**
     * Returns an ImageIcon based on a given Icon object.  If the supplied
     * <code>icon</code> is already an ImageIcon, it is returned.  Otherwise,
     * it is painted to an Image and an ImageIcon is constructed from that.
     * The reason this is useful is that some Swing components will only
     * grey out disabled icons if they are ImageIcon subclasses (which is
     * naughty).
     *
     * @param  icon  input icon
     * @return   image icon
     */
    public static ImageIcon toImageIcon( Icon icon ) {
        if ( icon instanceof ImageIcon ) {
            return (ImageIcon) icon;
        }
        else {
            return new ImageIcon( createImage( icon ) );
        }
    }

    /**
     * Returns an icon that paints a line of text.
     *
     * @param  line  text string
     * @param  g    graphics context
     * @return  icon
     */
    public static Icon createTextIcon( final String line, Graphics g ) {
        final Rectangle bounds =
            g.getFontMetrics().getStringBounds( line, g ).getBounds();
        return new Icon() {
            public int getIconWidth() {
                return bounds.width;
            }
            public int getIconHeight() {
                return bounds.height;
            }
            public void paintIcon( Component c, Graphics g, int x, int y ) {
                g.drawString( line, -bounds.x, -bounds.y );
            }
        };
    }

    /**
     * Returns an image got by drawing an Icon.
     *
     * @param  icon   icon
     * @return  image
     */
    public static BufferedImage createImage( Icon icon ) {
        int w = icon.getIconWidth();
        int h = icon.getIconHeight();

        /* Create an image to draw on. */
        BufferedImage image =
            new BufferedImage( w, h, BufferedImage.TYPE_INT_ARGB );
        Graphics2D g2 = image.createGraphics();

        /* This ought to clear it to transparent white (00ffffff).
         * But in fact it seems to remain transparent black (00000000).
         * That shouldn't make a difference, but in some cases PNGs
         * written with a transparent background can end up displaying
         * with black backgrounds when they end up in PDFs (probably
         * a bug in Fop?).  I have wasted a vast amount of time trying
         * to get this right and so far failed.  But if you just want
         * to copy an existing image you can use AlphaComposite.SRC
         * for the actual painting. */
        Color color = g2.getColor();
        Composite compos = g2.getComposite();
        g2.setComposite( AlphaComposite.getInstance( AlphaComposite.SRC ) );
        g2.setColor( new Color( 1f, 1f, 1f, 0f ) );
        g2.fillRect( 0, 0, w, h );
        g2.setColor( color );
        g2.setComposite( compos );

        /* Paint the icon. */
        icon.paintIcon( getDummyComponent(), g2, 0, 0 );

        /* Tidy up and return the image. */
        g2.dispose();
        return image;
    }

    /**
     * Provides an empty component.
     *
     * @return   lazily constructed component
     */
    private static Component getDummyComponent() {
        if ( dummyComponent_ == null ) {
            dummyComponent_ = new JPanel();
        }
        return dummyComponent_;
    }

    /**
     * Wrapper implementation for Icon.
     * All methods are deferred to base.
     * Convenience skeleton class for altering behaviour.
     */
    private static class WrapperIcon implements Icon {
        private final Icon base_;

        /**
         * Constructor.
         *
         * @param  base  base icon
         */
        WrapperIcon( Icon base ) {
            base_ = base;
        }
        public int getIconWidth() {
            return base_.getIconWidth();
        }
        public int getIconHeight() {
            return base_.getIconHeight();
        }
        public void paintIcon( Component c, Graphics g, int x, int y ) {
            base_.paintIcon( c, g, x, y );
        }
    }
}
