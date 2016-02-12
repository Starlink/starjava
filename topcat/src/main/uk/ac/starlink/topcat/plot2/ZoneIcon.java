package uk.ac.starlink.topcat.plot2;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import javax.swing.Icon;
import uk.ac.starlink.ttools.plot2.Gang;
import uk.ac.starlink.ttools.plot2.PlotUtil;

/**
 * Icon that displays the position of a Zone within a Gang.
 * This is supposed to be suitable for inclusion into a GUI component
 * like a combo box renderer.
 *
 * @author   Mark Taylor
 * @since    12 Feb 2016
 */
public class ZoneIcon implements Icon {

    private final Dimension size_;
    private final Rectangle[] rects_;
    private final int iHighlight_;
    private static final Color FILL0 = Color.LIGHT_GRAY;
    private static final Color DRAW0 = Color.DARK_GRAY;
    private static final Color FILL1 = Color.DARK_GRAY;
    private static final Color DRAW1 = Color.BLACK;

    /**
     * Constructor.
     *
     * @param   size   icon size
     * @param   rects  zone rectangles
     * @param   iHighlight   index into <code>rects</code> giving the one to
     *                       highlight (may be negative for no highlight)
     * @see  #createZoneIcon
     */
    public ZoneIcon( Dimension size, Rectangle[] rects, int iHighlight ) {
        size_ = size;
        rects_ = rects;
        iHighlight_ = iHighlight;
    }

    public int getIconWidth() {
        return size_.width;
    }

    public int getIconHeight() {
        return size_.height;
    }

    public void paintIcon( Component c, Graphics g, int x, int y ) {
        Color color0 = g.getColor();
        for ( Rectangle r : rects_ ) {
            paintRect( g, x, y, r, false );
        }
        if ( iHighlight_ >= 0 && iHighlight_ < rects_.length ) {
            paintRect( g, x, y, rects_[ iHighlight_ ], true );
        }
        g.setColor( color0 );
    }

    /**
     * Paints a highlighted or unhighlighted rectangle.
     *
     * @param  g  graphics context
     * @param  x  x offset
     * @param  y  y offset
     * @param  r  rectangle
     * @param  isHighlight   true to hightlight
     */
    private void paintRect( Graphics g, int x, int y, Rectangle r,
                            boolean isHighlight ) {
        g.setColor( isHighlight ? FILL1 : FILL0 );
        g.fillRect( x + r.x, y + r.y, r.width, r.height );
        g.setColor( isHighlight ? DRAW1 : DRAW0 );
        g.drawRect( x + r.x, y + r.y, r.width, r.height );
    }

    /**
     * Creates a zone icon from a gang.
     *
     * @param  size  total icon size
     * @param  border   empty border in pixels around all sides
     * @param  gang    gang defining zone positions
     * @param  izone   index of zone in gang to highlight
     *                 (may be null for no highlight)
     * @return   icon
     */
    public static Icon createZoneIcon( Dimension size, int border,
                                       Gang gang, int izone ) {
        Rectangle gb = PlotUtil.getGangBounds( gang );
        if ( gb == null ) {
            return new ZoneIcon( size, new Rectangle[ 0 ], -1 );
        }
        else {
            double xf = ( size.width - 2 * border ) * 1.0 / gb.width;
            double yf = ( size.height - 2 * border ) * 1.0 / gb.height;
            int nz = gang.getZoneCount();
            Rectangle[] rects = new Rectangle[ nz ];
            for ( int iz = 0; iz < nz; iz++ ) {
                Rectangle zb = gang.getZonePlotBounds( iz );
                int x = border + (int) Math.floor( xf * ( zb.x - gb.x ) );
                int y = border + (int) Math.floor( yf * ( zb.y - gb.y ) );
                int width = (int) Math.ceil( xf * zb.width );
                int height = (int) Math.ceil( yf * zb.height );
                rects[ iz ] = new Rectangle( x, y, width, height );
            }
            return new ZoneIcon( size, rects, izone );
        }
    }
}
