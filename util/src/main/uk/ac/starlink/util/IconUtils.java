package uk.ac.starlink.util;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Graphics2D;
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
     * Returns an image got by drawing an Icon.
     *
     * @param  icon
     * @return  image
     */
    public static BufferedImage createImage( Icon icon ) {
        int w = icon.getIconWidth();
        int h = icon.getIconHeight();

        /* Create an image to draw on. */
        BufferedImage image =
            new BufferedImage( w, h, BufferedImage.TYPE_INT_ARGB );
        Graphics2D g2 = image.createGraphics();

        /* Clear it to transparent white. */
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
}
