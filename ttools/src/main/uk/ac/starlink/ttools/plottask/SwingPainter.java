package uk.ac.starlink.ttools.plottask;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
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
            Graphics2D g2 = (Graphics2D) g.create();
            g2.translate( x, y );
            try {
                picture_.paintPicture( g2 );
            }
            catch ( IOException e ) {
                logger_.log( Level.WARNING, "Graphic plotting IO error", e );
                g.drawString( e.toString(), x, y );
            }
        }
    }
}
