package uk.ac.starlink.util.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;

/**
 * Component which displays JVM memory usage.
 * Clicking on it will call {@link java.lang.System#gc}.
 *
 * @author   Mark Taylor
 * @since    22 Dec 2009
 */
public class MemoryMonitor extends JComponent {

    private Color maxColor_;
    private Color totalColor_;
    private Color usedColor_;
    private boolean vertical_;
    private boolean reverse_;
    private boolean showText_;
    private double textAngle_;
    private final Timer timer_;
    private final Runtime runtime_;

    /**
     * Constructor.
     */
    @SuppressWarnings("this-escape")
    public MemoryMonitor() {
        maxColor_ = new Color( 0xc0f0ff );
        totalColor_ = new Color( 0x30c0ff );
        usedColor_ = new Color( 0x5080f0 );
        runtime_ = Runtime.getRuntime();
        timer_ = new Timer( 1000, new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                repaint();
            }
        } );
        addMouseListener( new MouseAdapter() {
            public void mouseClicked( MouseEvent evt ) {
                new Thread() {
                    public void run() {
                        runtime_.gc();
                        SwingUtilities.invokeLater( new Runnable() {
                            public void run() {
                                MemoryMonitor.this.repaint();
                            }
                        } );
                    }
                }.start();
            }
        } );
        timer_.start();
        showText_ = true;
        setFont( UIManager.getDefaults().getFont( "Label.font" ) );
        setBorder( UIManager.getDefaults().getBorder( "TextField.border" ) );
    }

    /**
     * Returns the timer which performs regular repaints of this component.
     *
     * @return   timer
     */
    public Timer getTimer() {
        return timer_;
    }

    /**
     * Sets the orientation for this component.
     *
     * @param  vertical   true for vertical movement of bars
     */
    public void setVertical( boolean vertical ) {
        vertical_ = vertical;
        repaint();
    }

    /**
     * Returns the orientation of this component.
     *
     * @return  true for vertical movement of bars
     */
    public boolean getVertical() {
        return vertical_;
    }

    /**
     * Sets bar movement to the opposite sense.
     *
     * @param  reverse   true to set the zero level to the non-default end
     */
    public void setReverse( boolean reverse ) {
        reverse_ = reverse;
        repaint();
    }

    /**
     * Returns whether bar movement is in the opposite sense.
     *
     * @return  true if the zero level is at the non-default end
     */
    public boolean getReverse() {
        return reverse_;
    }

    /**
     * Sets whether the text should be displayed.
     *
     * @param  showText   true to display the result of {@link #getText}
     */
    public void setShowText( boolean showText ) {
        showText_ = showText;
        repaint();
    }

    /**
     * Returns whether text is being displayed.
     *
     * @return  true if the result of {@link #getText} is being displayed
     */
    public boolean getShowText() {
        return showText_;
    }

    /**
     * Set the colour scheme.
     *
     * @param   maxColor  colour for max memory bar
     * @param   totalColor  colour for total memory bar
     * @param   usedColor   colour for used memory bar
     */
    public void setColors( Color maxColor, Color totalColor, Color usedColor ) {
        maxColor_ = maxColor;
        totalColor_ = totalColor;
        usedColor_ = usedColor;
        repaint();
    }

    /**
     * Returns the colour scheme.
     *
     * @return  3-element array of colours:
     *          max memory, total memory and used memory
     */
    public Color[] getColors() {
        return new Color[] { maxColor_, totalColor_, usedColor_, };
    }

    public void paintComponent( Graphics g ) {
        long freeMem = runtime_.freeMemory();
        long maxMem = runtime_.maxMemory();
        long totalMem = runtime_.totalMemory();
        long usedMem = totalMem - freeMem;
        if ( maxMem == Long.MAX_VALUE ) {
            fillRect( g, 1.0, totalColor_ );
            fillRect( g, usedMem / (double) totalMem, usedColor_ );
        }
        else {
            fillRect( g, 1.0, maxColor_ );
            fillRect( g, totalMem / (double) maxMem, totalColor_ );
            fillRect( g, usedMem / (double) maxMem, usedColor_ );
        }
        if ( showText_ ) {
            String text = getText();
            if ( text != null && text.trim().length() > 0 ) {
                paintText( g, text );
            }
        }
    }

    /**
     * Paints text onto the middle of this component.
     *
     * @param   g  graphics context
     * @parma  text  text to paint
     */
    private void paintText( Graphics g, String text ) {
        Insets insets = getInsets();
        int x = insets.left;
        int y = insets.top;
        int w = getWidth() - insets.left - insets.right;
        int h = getHeight() - insets.top - insets.bottom;
        int cx = x + w / 2;
        int cy = y + h / 2;
        Graphics2D g2 = (Graphics2D) g;
        AffineTransform trans0 = g2.getTransform();
        g2.translate( cx, cy );
        g2.rotate( textAngle_ );
        Rectangle box =
            g2.getFontMetrics().getStringBounds( text, g2 ).getBounds();
        int tx = - box.x - box.width / 2;
        int ty = - box.y - box.height / 2;
        g2.drawString( text, tx, ty );
        g2.setTransform( trans0 );
    }

    /**
     * Returns the text which is painted onto this component.
     * It's some indication of used and available memory, but can be overridden.
     *
     * @return   text
     */
    public String getText() {
        long freeMem = runtime_.freeMemory();
        long maxMem = runtime_.maxMemory();
        long totalMem = runtime_.totalMemory();
        long totmem = maxMem == Long.MAX_VALUE ? totalMem : maxMem;
        long usemem = totalMem - freeMem;
        int totMegas = (int) Math.round( (double) totmem / 1024 / 1024 );
        int useMegas = (int) Math.round( (double) usemem / 1024 / 1024 );
        String totStr = Integer.toString( totMegas );
        String useStr = Integer.toString( useMegas );
        StringBuffer sbuf = new StringBuffer();
        for ( int i = 0; i < totStr.length() - useStr.length(); i++ ) {
            sbuf.append( ' ' );
        }
        sbuf.append( useStr );
        sbuf.append( " / " );
        sbuf.append( totStr );
        sbuf.append( " M" );
        return sbuf.toString();
    }

    /**
     * Fills a part of the area of this monitor with a given colour.
     *
     * @param   g   graphics context
     * @param fraction  proportion of the area to fill
     * @param  color  colour to use
     */
    private void fillRect( Graphics g, double fraction, Color color ) {
        Color col0 = g.getColor();
        Insets insets = getInsets();
        int x0 = insets.left;
        int y0 = insets.right;
        int w0 = getWidth() - insets.left - insets.right;
        int h0 = getHeight() - insets.top - insets.bottom;
        double fw = vertical_ ? 1.0 : fraction;
        double fh = vertical_ ? fraction : 1.0;
        int w1 = (int) Math.round( fw * w0 );
        int h1 = (int) Math.round( fh * h0 );
        int x1 = x0 + ( reverse_ ? w0 - w1 : 0 );
        int y1 = y0 + ( reverse_ ? h0 - h1 : 0 );
        g.setColor( color );
        g.fillRect( x1, y1, w1, h1 );
        g.setColor( col0 );
    }
}
