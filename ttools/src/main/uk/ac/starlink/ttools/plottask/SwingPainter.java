package uk.ac.starlink.ttools.plottask;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.KeyStroke;
import uk.ac.starlink.ttools.plot.Picture;
import uk.ac.starlink.ttools.plot.PictureImageIcon;

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

    /**
     * Constructor.
     *
     * @param  winTitle  window title to use for the container frame
     */
    public SwingPainter( String winTitle ) {
        winTitle_ = winTitle;
    }

    public void paintPicture( Picture picture ) {
        postComponent( new JLabel( new PictureImageIcon( picture, true ) ) );
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
}
