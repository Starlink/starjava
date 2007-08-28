package uk.ac.starlink.topcat.doc;

import Acme.JPM.Encoders.GifEncoder;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.imageio.ImageIO;

/**
 * Generates some icons for the TOPCAT desktop.
 * Currently not used at TOPCAT run or build time.
 *
 * @author   Mark Taylor 
 * @since    28 Aug 2007
 */
public class ButtonIcon {

    private static final int SIZE = 24;
    private final BufferedImage image_;

    /**
     * Constructs a ButtonIcon of standard size.
     */
    public ButtonIcon() {
        image_ = new BufferedImage( 24, 24, BufferedImage.TYPE_INT_ARGB );
    }

    /**
     * Returns a graphics context for drawing on this image.
     *
     * @return   new graphics context suitable for drawing
     */
    public Graphics2D createGraphics() {
        Graphics2D g2 = image_.createGraphics();
        g2.setRenderingHint( RenderingHints.KEY_ALPHA_INTERPOLATION,
                             RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY );
        g2.setRenderingHint( RenderingHints.KEY_RENDERING,
                             RenderingHints.VALUE_RENDER_QUALITY );
        g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
                             RenderingHints.VALUE_ANTIALIAS_ON );
        return g2;
    }

    /**
     * Writes the image as currently painted to a GIF file with a given name.
     *
     * @param   fname  filename
     */
    public void writeGif( String fname ) throws IOException {
        OutputStream out =
            new BufferedOutputStream( new FileOutputStream( fname ) );
        new GifEncoder( image_, out ).encode();
        out.close();
        System.out.println( "Output image: " + fname );
    }

    /**
     * Icon for radial coordinate display.
     *
     * @return  radial button icon
     */
    public static ButtonIcon createRadial() {
        ButtonIcon bi = new ButtonIcon();
        Graphics2D g2 = bi.createGraphics();
        g2.translate( SIZE / 2, SIZE / 2 );
        g2.setColor( new Color( 190, 190, 190 ) );
        g2.fillOval( -10, -10, 20, 20 );
        g2.setColor( new Color( 128, 128, 128 ) );
        g2.drawOval( -10, -10, 20, 20 );
        g2.setColor( Color.BLACK );
        g2.drawOval( -11, -11, 22, 22 );
        g2.setColor( new Color( 0, 0, 96 ) );
        drawArrow( g2, Math.toRadians( 45 ), 9 );
        drawArrow( g2, Math.toRadians( 270 ), 7 );
        drawArrow( g2, Math.toRadians( 135 ), 5 );
        return bi;
    }

    /**
     * Draws a little arrow from the origin.
     *
     * @param   g2  graphics context
     * @param   theta   direction of arrow in radians
     * @param   leng   length of arrow in pixels
     */
    private static void drawArrow( Graphics2D g2, double theta, int leng ) {
        final int head = 2;
        g2.rotate( theta );
        g2.drawLine( 0, 0, 0, leng );
        g2.drawLine( 0, leng, - head, leng - head );
        g2.drawLine( 0, leng, + head, leng - head );
        g2.rotate( -theta );
    }

    /**
     * Main method.  Writes images to the current directory.
     *
     * @param  args  ignored
     */
    public static void main( String[] args ) throws IOException {
        createRadial().writeGif( "clock1.gif" );
    }
}
