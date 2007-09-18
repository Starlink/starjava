package uk.ac.starlink.xdoc.fig;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.FontMetrics;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Base class for drawings.
 * As well as implemnting {@link javax.swing.Icon} it provides some 
 * utility methods for output.
 *
 * @author   Mark Taylor
 * @since    18 Sep 2007
 */
public abstract class FigureIcon implements Icon {

    private final int width_;
    private final int height_;

    /**
     * Constructor.
     *
     * @param   width   figure width in pixels
     * @param   height  figure height in pixels
     */
    protected FigureIcon( int width, int height ) {
        width_ = width;
        height_ = height;
    }

    /**
     * Implement this method to draw the figure content.
     *
     * @param  g2  graphics context
     */
    protected abstract void doDrawing( Graphics2D g2 );

    public int getIconWidth() {
        return width_;
    }

    public int getIconHeight() {
        return height_;
    }

    public void paintIcon( Component c, Graphics g, int x, int y ) {
        doDrawing( (Graphics2D) g.create() );
    }

    /**
     * Displays the figure in a Swing window.
     */
    public void display() {
        JFrame frame = new JFrame();
        JComponent container = new JPanel();
        container.setBackground( Color.WHITE );
        container.setOpaque( true );
        container.add( new JLabel( this ) );
        frame.getContentPane().add( container );
        frame.pack();
        frame.setVisible( true );
    }
}
