package uk.ac.starlink.ttools.plot2.task;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Icon;
import uk.ac.starlink.ttools.plot2.Decoration;
import uk.ac.starlink.ttools.plot2.PlotUtil;

/**
 * Icon used for point highlighting.
 *
 * @author   Mark Taylor
 * @since    30 Sep 2020
 */
public class HighlightIcon implements Icon {

    private final int size_;
    private final int size2_;
    private final Stroke stroke_ =
        new BasicStroke( 2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND );
    private final Map<RenderingHints.Key,Object> hints_;
    private final Color color1_ = new Color( 0xffffff );
    private final Color color2_ = new Color( 0x000000 );

    /** Standard instance. */
    public static final HighlightIcon INSTANCE = new HighlightIcon( 6 );

    /**
     * Constructor.
     *
     * @param  size   icon radius
     */
    public HighlightIcon( int size ) {
        size_ = size;
        size2_ = size_ * 2 + 1;
        hints_ = new HashMap<RenderingHints.Key,Object>();
        hints_.put( RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY );
    }

    public int getIconWidth() {
        return size2_;
    }

    public int getIconHeight() {
        return size2_;
    }

    public void paintIcon( Component c, Graphics g, int x, int y ) {
        Graphics2D g2 = (Graphics2D) g;
        Stroke stroke0 = g2.getStroke();
        Color color0 = g2.getColor();
        RenderingHints hints0 = g2.getRenderingHints();
        g2.setRenderingHints( hints_ );
        g2.setStroke( stroke_ );
        int xoff = x + size_;
        int yoff = y + size_;
        g2.translate( xoff, yoff );
        g2.setColor( color1_ );
        drawTarget( g2, size_ - 1 );
        g2.setColor( color2_ );
        drawTarget( g2, size_ );
        g2.translate( -xoff, -yoff );
        g2.setColor( color0 );
        g2.setStroke( stroke0 );
        g2.setRenderingHints( hints0 );
    }

    /**
     * Creates a decoration consisting of this icon centered on a given
     * graphics position.
     *
     * @param  gp  central graphics position
     * @return   decoration
     */
    public Decoration createDecoration( Point2D gp ) {
        int xoff = getIconWidth() / 2;
        int yoff = getIconWidth() / 2;
        int gx = PlotUtil.ifloor( gp.getX() - xoff );
        int gy = PlotUtil.ifloor( gp.getY() - yoff );
        return new Decoration( this, gx, gy );
    }

    /**
     * Does the actual drawing of the target-like figure at the origin
     * of the graphics context.
     *
     * @param   g  graphics context
     * @param   size   target size
     */
    private static void drawTarget( Graphics g, int size ) {
        int size2 = size * 2 + 1;
        int s = size - 2;
        int s2 = s * 2;
        g.drawOval( -size, -size, size2, size2 );
        g.drawLine( 0, +s, 0, +s2 );
        g.drawLine( 0, -s, 0, -s2 );
        g.drawLine( +s, 0, +s2, 0 );
        g.drawLine( -s, 0, -s2, 0 );
    }
}
