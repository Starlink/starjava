package uk.ac.starlink.topcat;

import edu.jhu.pha.sdss.fits.imageio.FITSReaderSpi;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
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
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.util.IconUtils;

/**
 * Window which displays an image using AWT.
 * Image types supported at J2SE1.4 are JPEG, GIF, PNG (I think).
 *
 * @author   Mark Taylor (Starlink)
 * @since    5 Oct 2004
 */
public class ImageWindow extends AuxWindow {

    private final JScrollPane scroller_;
    private final IconLabel label_;
    private PosIcon icon_;

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
        setAutoRequestFocus( false );
        JComponent main = getMainArea();
        main.setLayout( new BorderLayout() );
        main.setBackground( Color.WHITE );
        label_ = new IconLabel();
        scroller_ = new JScrollPane( label_ );
        scroller_.setBackground( Color.WHITE );
        main.setPreferredSize( new Dimension( 240, 240 ) );
        main.add( scroller_, BorderLayout.CENTER );
        addHelp( "imageViewers" );
    }

    /**
     * Creates an image from a location, suitable for use with this window.
     *
     * <p>This method is potentially time-consuming, and should not be
     * invoked on the Event Dispatch Thread.
     *
     * @param  location   file or URL
     * @param  allowSystem  whether to allow system commands in location
     *                      specification - has security implications
     * @return  image
     */
    public static BufferedImage createImage( String location,
                                             boolean allowSystem )
            throws IOException {
        return ImageIO
              .read( DataSource.getInputStream( location, allowSystem ) );
    }

    /**
     * Synchronously configures this window to display an image.
     *
     * <p>This method must be invoked from the Event Dispatch Thread.
     *
     * @param  image  image to install
     */
    public void setImage( Image image ) {
        setImagePoint( image, null );
    }

    /**
     * Resizes this window so that it's the right size to display
     * the current image without scrolling.
     */
    public void resizeToFitImage() {
        scroller_.getViewport().setPreferredSize( label_.getPreferredSize() );
        getMainArea().setPreferredSize( null );
        getMainArea().revalidate();
        pack();
    }

    /**
     * Synchronously configures this window to display an image and
     * indicate a given X,Y point on it.
     *
     * @param  image  image to install
     * @param  point  highlight point
     */
    public void setImagePoint( Image image, Point point ) {
        PosIcon icon = new PosIcon( image, point );
        if ( ! icon.equals( icon_ ) ) {
            label_.setIcon( icon );
        }
        if ( icon.imageContainsPoint() ) {
            scrollToPoint( point );
        }
        icon_ = icon;
    }

    /**
     * Sets the image to load from a given location.
     * This should be called from the event dispatch thread, but will
     * do most of the work out-of-thread to prevent blocking when loading
     * an image.
     *
     * @param  location  image filename or URL
     * @param  allowSystem  whether to allow system commands in location
     *                      specification - has security implications
     */
    public void setImage( final String location, final boolean allowSystem ) {

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
                String txt;
                try {
                    im = createImage( location, allowSystem );
                    txt = null;
                }
                catch ( IOException e ) {
                    im = null;
                    txt = "Can't load image: " + e.getMessage();
                }
                final Icon icon = im == null
                         ? IconUtils.createTextIcon( txt, getGraphics() )
                         : new ImageIcon( im );
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        label_.setIcon( icon );
                    }
                } );
            }
        }.start();
    }

    /**
     * Ensures that the point of interest is visible, as near to the
     * center of the scrollpane viewport as possible.
     *
     * @param  point  point in image coordinates
     */
    private void scrollToPoint( Point point ) {
        Dimension vdim = scroller_.getViewport().getSize();
        Rectangle rect = new Rectangle( point.x - vdim.width / 2,
                                        point.y - vdim.height / 2,
                                        vdim.width, vdim.height );
        label_.scrollRectToVisible( rect );
    }

    /**
     * Simple container for an icon.
     */
    private static class IconLabel extends JComponent {
        Icon icon_;

        /**
         * Sets the icon to display.
         *
         * @param  icon  icon
         */
        public void setIcon( Icon icon ) {
            icon_ = icon;
            revalidate();
            repaint();
        }

        /**
         * Returns the origin of the displayed icon.
         *
         * @return  icon origin position
         */
        public Point getIconPosition() {
            return icon_ == null
                ? new Point( 0, 0 )
                : new Point(
                  Math.max( ( getWidth() - icon_.getIconWidth() ) / 2, 0 ),
                  Math.max( ( getHeight() - icon_.getIconHeight() ) / 2, 0 ) );
        }

        @Override
        public Dimension getPreferredSize() {
            return icon_ == null
                 ? new Dimension( 0, 0 )
                 : new Dimension( icon_.getIconWidth(), icon_.getIconHeight() );
        }

        @Override
        public void paintComponent( Graphics g ) {
            super.paintComponent( g );
            if ( icon_ != null ) {
                Point pos = getIconPosition();
                icon_.paintIcon( this, g, pos.x, pos.y );
            }
        }
    }

    /**
     * Icon which displays an image, optionally with a highlighted position.
     */
    private static class PosIcon implements Icon {
        private final Image image_;
        private final Icon imageIcon_;
        private Point point_;
        private final Color hairColor_ = new Color( 0, 255, 0, 128 );

        /**
         * Constructor.
         *
         * @param  image  image
         * @param  point  point in image coordinates
         */
        PosIcon( Image image, Point point ) {
            image_ = image;
            point_ = point;
            imageIcon_ = image == null ? IconUtils.emptyIcon( 0, 0 )
                                       : new ImageIcon( image );
        }

        /**
         * Returns the image.
         *
         * @return  image
         */
        public Image getImage() {
            return image_;
        }

        /**
         * Returns the highlighted position.
         *
         * @return  position
         */
        public Point getPoint() {
            return point_;
        }

        /**
         * Indicates whether the current highlighted position is within
         * the bounds of the image.
         *
         * @return  true iff position is within image
         */
        public boolean imageContainsPoint() {
            return point_ != null
                && image_ != null
                && point_.x >= 0 && point_.x < getIconWidth()
                && point_.y >= 0 && point_.y < getIconHeight();
        }

        /**
         * Sets the highlighted position.
         *
         * @param  point  new position
         */
        public void setPoint( Point point ) {
            point_ = point;
        }

        public int getIconWidth() {
            return imageIcon_.getIconWidth();
        }

        public int getIconHeight() {
            return imageIcon_.getIconHeight();
        }

        public void paintIcon( Component c, Graphics g, int x, int y ) {
            imageIcon_.paintIcon( c, g, x, y );
            if ( imageContainsPoint() ) {
                Color color0 = g.getColor();
                g.setColor( hairColor_ );
                g.drawLine( x + point_.x, y,
                            x + point_.x, y + getIconHeight() );
                g.drawLine( x, y + point_.y,
                            x + getIconWidth(), y + point_.y );
                g.setColor( color0 );
            }
        }

        @Override
        public int hashCode() {
            int code = 776245;
            code = 23 * PlotUtil.hashCode( image_ );
            code = 23 * PlotUtil.hashCode( point_ );
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof PosIcon ) {
                PosIcon other = (PosIcon) o;
                return PlotUtil.equals( this.image_, other.image_ )
                    && PlotUtil.equals( this.point_, other.point_ );
            }
            else {
                return false;
            }
        }
    }
}
