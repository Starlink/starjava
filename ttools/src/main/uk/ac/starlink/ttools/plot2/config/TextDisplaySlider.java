package uk.ac.starlink.ttools.plot2.config;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import javax.swing.JSlider;
import javax.swing.UIManager;

/**
 * JSlider that displays the currently selected value when the slider
 * is being dragged.  This is quite useful, but it's not currently
 * displayed nicely (hard to know where to put the displayed value).
 * Not currently in use, but may come in useful some time.
 *
 * @author   Mark Taylor
 * @since    1 Apr 2015
 */
public class TextDisplaySlider extends JSlider {

    private boolean isShow_;

    /**
     * Constructor.
     *
     * @param  min   minimum value
     * @param  max   maximum value
     */
    @SuppressWarnings("this-escape")
    public TextDisplaySlider( int min, int max ) {
        super( min, max );
        addMouseMotionListener( new MouseMotionAdapter() {
            public void mouseDragged( MouseEvent evt ) {
                setShowValue( true );
            }
        } );
        addMouseListener( new MouseAdapter() {
            public void mousePressed( MouseEvent evt ) {
                setShowValue( true );
            }
            public void mouseReleased( MouseEvent evt ) {
                setShowValue( false );
            }
        } );
    }

    /**
     * Returns the text to display.
     * May be overridden by subclasses.
     *
     * @return  display value
     */
    public String getDisplayValue() {
        return Integer.toString( getValue() );
    }

    @Override
    protected void paintComponent( Graphics g ) {
        super.paintComponent( g );
        if ( isShow_ ) {
            Color color0 = g.getColor();
            Font font0 = g.getFont();
            java.awt.Shape clip0 = g.getClip();
            g.setColor( UIManager.getColor( "ToolTip.foreground" ) );
            g.setFont( UIManager.getFont( "ToolTip.font" ) );
            g.drawString( getDisplayValue(), 0, getHeight() );
            g.setColor( color0 );
            g.setFont( font0 );
            g.setClip( clip0 );
        }
    }

    /**
     * Sets whether the value will be painted, and triggers a repaint.
     *
     * @param  isShow  true to display, false to hide
     */
    private void setShowValue( boolean isShow ) {
        isShow_ = isShow;
        repaint();
    }
}
