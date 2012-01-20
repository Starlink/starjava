package uk.ac.starlink.ttools.plottask;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.KeyStroke;
import uk.ac.starlink.ttools.plot.Picture;
import uk.ac.starlink.ttools.plot.TablePlot;

/**
 * Painter subclass which can paint to the screen.
 * It has a {@link #postComponent} method additional to the
 * Painter interface which allows a component to be submitted directly,
 * if one is available.
 *
 * @author   Mark Taylor
 * @since    20 Jan 2012
 */
public class SwingPainter implements Painter {

    private final String winTitle_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.plottask" );

    /**
     * Constructor.
     *
     * @param  winTitle  window title to use for the container frame
     */
    public SwingPainter( String winTitle ) {
        winTitle_ = winTitle;
    }

    public void paintPicture( Picture picture ) {
        postComponent( new JLabel( new PictureIcon( picture ) ) );
    }

    /**
     * Displays a screen component directly.
     *
     * @param  comp  screen component
     */
    public void postComponent( Component comp ) {
        JComponent holder = new JPanel( new BorderLayout() );
        holder.add( comp );
        final JFrame frame = new JFrame( winTitle_ );
        frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
        frame.getContentPane().add( holder );
        Object quitKey = "quit";
        holder.getInputMap().put( KeyStroke.getKeyStroke( 'q' ), quitKey );
        holder.getActionMap().put( quitKey, new AbstractAction() {
            public void actionPerformed( ActionEvent evt ) {
                frame.dispose();
            }
        } );
        frame.pack();
        frame.setVisible( true );
    }

    /**
     * Adapts a Picture to an Icon.
     */
    private static class PictureIcon implements Icon {

        private final Picture picture_;
        private BufferedImage image_;

        /**
         * Constructor.
         *
         * @param   picture   content to be painted by the icon
         */
        PictureIcon( Picture picture ) {
            picture_ = picture;
        }

        public int getIconWidth() {
            return picture_.getPictureWidth();
        }

        public int getIconHeight() {
            return picture_.getPictureHeight();
        }

        public void paintIcon( Component c, Graphics g, int x, int y ) {
            assert ! TablePlot.isVectorContext( g );
            int w = picture_.getPictureWidth();
            int h = picture_.getPictureHeight();
            Graphics2D g2 = (Graphics2D) g.create( x, y, w, h );
            if ( image_ == null ) {
                GraphicsConfiguration gc = g2.getDeviceConfiguration();
                VolatileImage vim = gc.createCompatibleVolatileImage( w, h );
                for ( boolean done = false; ! done; ) {
                    vim.validate( gc );
                    Graphics2D gv = vim.createGraphics();
                    doPaint( gv );
                    image_ = vim.getSnapshot();
                    if ( vim.contentsLost() ) {
                        logger_.info( "Lost volatile image during draw" );
                    }
                    else {
                        done = true;
                    }
                    gv.dispose();
                }
                vim.flush();
            }
            g2.drawImage( image_, 0, 0, null );
        }

        private void doPaint( Graphics2D g ) {
            try {
                picture_.paintPicture( g );
            }
            catch ( IOException e ) {
                logger_.log( Level.WARNING, "Graphic plotting IO error", e );
                g.drawString( e.toString(), 40, 40 );
            }
        }
    }
}
