package uk.ac.starlink.ttools.plot2.geom;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import javax.swing.Icon;
import uk.ac.starlink.ttools.plot2.Decoration;
import uk.ac.starlink.ttools.plot2.Equality;

/**
 * Utility class for supplying navigation visual feedback decorations.
 *
 * @author   Mark Taylor
 * @since    18 Feb 2014
 */
public class NavDecorations {

    private static final int BASE_SIZE = 32;
    private static final int CENTER_SIZE = 5;
    private static final double WHEEL_MAG = 2.0;
    private static final Color COLOR = new Color( 0x3030f0 );
    private static final Icon CENTER = new CenterIcon( CENTER_SIZE );

    /**
     * Private constructor prevents instantiation.
     */
    private NavDecorations() {
    }

    /**
     * Returns a simple marker at a point.
     *
     * @param  p  position
     * @return   position marker centered at <code>p</code>
     */
    public static Decoration createCenterDecoration( Point p ) {
        return center( p, CENTER );
    }

    /**
     * Returns a decoration appropriate for a 2d wheel zoom.
     *
     * @param   p  reference point
     * @param  xfact  X direction zoom factor
     * @param  yfact  Y direction zoom factor
     * @param  xuse   true if X zoom is in effect
     * @param  yuse   true if Y zoom is in effect
     * @param  bounds   plot region bounds
     * @return  marker centered at p
     */
    public static Decoration createWheelDecoration( Point p,
                                                    double xfact, double yfact,
                                                    boolean xuse, boolean yuse,
                                                    Rectangle bounds ) {
        if ( xuse && yuse ) {
            return center( p, new WheelZoomIcon2d( BASE_SIZE, xfact, yfact ) );
        }
        else if ( xuse ) {
            Icon icon = new WheelZoomIcon1d( BASE_SIZE, false, xfact, bounds );
            return new Decoration( icon,
                                   p.x - icon.getIconWidth() / 2, bounds.y );
        }
        else if ( yuse ) {
            Icon icon = new WheelZoomIcon1d( BASE_SIZE, true, yfact, bounds );
            return new Decoration( icon,
                                   bounds.x, p.y - icon.getIconHeight() / 2 );
        }
        else {
            return null;
        }
    }

    /**
     * Returns a decoration appropriate for a 2d drag zoom.
     *
     * @param   p  reference point
     * @param  xfact  X direction zoom factor
     * @param  yfact  Y direction zoom factor
     * @param  xuse   true if X zoom is in effect
     * @param  yuse   true if Y zoom is in effect
     * @param  bounds   plot region bounds
     * @return  marker centered at p
     */
    public static Decoration createDragDecoration( Point p,
                                                   double xfact, double yfact,
                                                   boolean xuse, boolean yuse,
                                                   Rectangle bounds ) {
        if ( xuse && yuse ) {
            return center( p, new DragZoomIcon2d( BASE_SIZE, xfact, yfact ) );
        }
        else if ( xuse ) {
            Icon icon = new DragZoomIcon1d( BASE_SIZE, false, xfact, bounds );
            return new Decoration( icon,
                                   p.x - icon.getIconWidth() / 2, bounds.y );
        }
        else if ( yuse ) {
            Icon icon = new DragZoomIcon1d( BASE_SIZE, true, yfact, bounds );
            return new Decoration( icon,
                                   bounds.x, p.y - icon.getIconHeight() / 2 );
        }
        else {
            return null;
        }
    }

    /**
     * Utility function to center an symmetric icon at a point.
     * The icon must correctly report its dimensions for this to work.
     *
     * @param   p  central point
     * @param   icon   icon
     * @return  centered decoration
     */
    private static Decoration center( Point p, Icon icon ) {
        return new Decoration( icon, p.x - icon.getIconWidth() / 2,
                                     p.y - icon.getIconHeight() / 2 );
    }

    /**
     * Draws a line with an arrow head at one end.
     *
     * @param  g  graphics context
     * @param  x0  start X coordinate
     * @param  y0  start Y coordinate
     * @param  x1  end (arrow) X coordinate
     * @param  y1  end (arrow) Y coordinate
     */
    private static void drawArrow( Graphics g,
                                   int x0, int y0, int x1, int y1 ) {
        if ( x0 != x1 || y0 != y1 ) {
            Graphics2D g2 = (Graphics2D) g;
            AffineTransform trans0 = g2.getTransform();
            g2.drawLine( x0, y0, x1, y1 );
            g2.translate( x1, y1 );
            g2.rotate( Math.atan2( y1 - y0, x1 - x0 ) );
            g2.drawPolyline( new int[] { -5, 0, -5 },
                             new int[] { -5, 0, 5 }, 3 );
            g2.setTransform( trans0 );
        }
    }

    /**
     * Simple marker icon.
     */
    @Equality
    private static class CenterIcon implements Icon {
        final int size_;

        /**
         * Constructor.
         *
         * @param  size  radial size
         */
        CenterIcon( int size ) {
            size_ = size;
        }

        public int getIconWidth() {
            return 2 * size_;
        }

        public int getIconHeight() {
            return 2 * size_;
        }

        public void paintIcon( Component c, Graphics g, int x, int y ) {
            Color color0 = g.getColor();
            g.setColor( COLOR );
            g.drawLine( x + size_, y, x + size_, y + 2 * size_ );
            g.drawLine( x, y + size_, x + 2 * size_, y + size_ );
            g.setColor( color0 );
        }

        public String toString() {
            return "center";
        }
    }

    /**
     * Abstract icon to represent a zoom action in 1 dimension.
     */
    @Equality
    private static abstract class ZoomIcon1d implements Icon {

        final int baseSize_;
        final boolean isY_;
        final int cTo_;
        final int blo_;
        final int bhi_;
        private static final Stroke CENTER_STROKE =
            new BasicStroke( 1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER,
                             10, new float[] { 5, 5 }, 2 );

        /**
         * Constructor.
         *
         * @param  baseSize   radial size
         * @param  isY   true for Y zoom, false for X
         * @param  factor   zoom factor
         * @param  bounds   maximum bounds over which the icon should extend
         */
        public ZoomIcon1d( int baseSize, boolean isY, double factor,
                           Rectangle bounds ) {
            baseSize_ = baseSize;
            isY_ = isY;
            cTo_ = (int) Math.round( baseSize_ * factor );
            blo_ = isY ? bounds.x : bounds.y;
            bhi_ = isY ? bounds.x + bounds.width : bounds.y + bounds.height;
        }

        public int getIconWidth() {
            return isY_ ? bhi_ - blo_
                        : 2 * Math.max( baseSize_, cTo_ );
        }

        public int getIconHeight() {
            return isY_ ? 2 * Math.max( baseSize_, cTo_ )
                        : bhi_ - blo_;
        }

        public void paintIcon( Component c, Graphics g, int x, int y ) {
            Color color0 = g.getColor();
            g.setColor( COLOR );
            doPainting( g, isY_ ? y + getIconHeight() / 2
                                : x + getIconWidth() / 2 );
            g.setColor( color0 );
        }

        /**
         * Paints this icon at a given point.
         *
         * @param  g   graphics context
         * @param  c0  reference coordinate in zoom direction
         */
        abstract void doPainting( Graphics g, int c0 );

        /**
         * Draws a line at a given zoom direction coordinate
         * with appropriate bounds in the perpendicular direction.
         *
         * @param  g  graphics context
         * @param  c1  line coordinate in zoom direction
         */
        void drawSpan( Graphics g, int c1 ) {
            if ( isY_ ) {
                g.drawLine( blo_, c1, bhi_, c1 );
            }
            else {
                g.drawLine( c1, blo_, c1, bhi_ );
            }
        }

        /**
         * Draws a reference line.
         *
         * @param  g  graphics context
         * @param  c0  coordinate in zoom direction
         */
        void drawCenterLine( Graphics g, int c0 ) {
            Graphics2D g2 = (Graphics2D) g;
            Stroke stroke0 = g2.getStroke();
            g2.setStroke( CENTER_STROKE );
            drawSpan( g, c0 );
            g2.setStroke( stroke0 );
        }

        @Override
        public int hashCode() {
            int code = 65552;
            code = 23 * code + getClass().hashCode();
            code = 23 * code + baseSize_;
            code = 23 * code + cTo_;
            code = 23 * code + blo_;
            code = 23 * code + bhi_;
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof ZoomIcon1d ) {
                ZoomIcon1d other = (ZoomIcon1d) o;
                return this.getClass().equals( other.getClass() )
                    && this.baseSize_ == other.baseSize_
                    && this.cTo_ == other.cTo_
                    && this.blo_ == other.blo_
                    && this.bhi_ == other.bhi_;
            }
            else {
                return false;
            }
        }
    }

    /**
     * Icon representing a wheel zoom in one dimension.
     */
    private static class WheelZoomIcon1d extends ZoomIcon1d {

        /**
         * Constructor.
         *
         * @param  baseSize   radial size
         * @param  isY   true for Y zoom, false for X
         * @param  factor   zoom factor
         * @param  bounds   maximum bounds over which the icon should extend
         */
        WheelZoomIcon1d( int baseSize, boolean isY, double factor,
                         Rectangle bounds ) {

            /* Multiply the factor here because the default tends to give
             * arrows that are too small to see easily.  This means that the
             * visual representation shows the result of WHEEL_MAG wheel
             * clicks not one. */
            super( baseSize, isY, Math.pow( factor, WHEEL_MAG ), bounds );
        }

        void doPainting( Graphics g, int c0 ) {
            drawCenterLine( g, c0 );
            drawSpan( g, c0 - baseSize_ );
            drawSpan( g, c0 + baseSize_ );
            if ( isY_ ) {
                int x0 = ( bhi_ + blo_ ) / 2;
                drawArrow( g, x0, c0 - baseSize_, x0, c0 - cTo_ );
                drawArrow( g, x0, c0 + baseSize_, x0, c0 + cTo_ );
            }
            else {
                int y0 = ( bhi_ + blo_ ) / 2;
                drawArrow( g, c0 - baseSize_, y0, c0 - cTo_, y0 );
                drawArrow( g, c0 + baseSize_, y0, c0 + cTo_, y0 );
            }
        }
    }

    /**
     * Icon representing a drag zoom in one dimension.
     */
    private static class DragZoomIcon1d extends ZoomIcon1d {

        /**
         * Constructor.
         *
         * @param  baseSize   radial size
         * @param  isY   true for Y zoom, false for X
         * @param  factor   zoom factor
         * @param  bounds   maximum bounds over which the icon should extend
         */
        DragZoomIcon1d( int baseSize, boolean isY, double factor,
                        Rectangle bounds ) {
            super( baseSize, isY, factor, bounds );
        }

        void doPainting( Graphics g, int c0 ) {
            drawCenterLine( g, c0 );
            drawSpan( g, c0 - baseSize_ );
            drawSpan( g, c0 + baseSize_ );
            drawSpan( g, c0 - cTo_ );
            drawSpan( g, c0 + cTo_ );
        }
    }

    /**
     * Abstract icon to represent a zoom action in 2 dimensions.
     */
    @Equality
    private static abstract class ZoomIcon2d implements Icon {

        final int baseSize_;
        final int xTo_;
        final int yTo_;

        /**
         * Constructor.
         *
         * @param  baseSize  radial size
         * @param  xFact   zoom factor in X direction
         * @param  yFact   zoom factor in Y direction
         */
        ZoomIcon2d( int baseSize, double xFact, double yFact ) {
            baseSize_ = baseSize;
            xTo_ = (int) Math.round( baseSize_ * xFact );
            yTo_ = (int) Math.round( baseSize_ * yFact );
        }

        public int getIconWidth() {
            return 2 * Math.max( baseSize_, xTo_ );
        }

        public int getIconHeight() {
            return 2 * Math.max( baseSize_, yTo_ );
        }

        public void paintIcon( Component c, Graphics g, int x, int y ) {
            Color color0 = g.getColor();
            g.setColor( COLOR );
            doPainting( g, x + getIconWidth() / 2, y + getIconHeight() / 2 );
            g.setColor( color0 );
        }

        /**
         * Paints this icon at a given point.
         *
         * @param  g  graphics context
         * @param  cx  central X coordinate
         * @parma  cy  central Y coordinate
         */
        abstract void doPainting( Graphics g, int cx, int cy );

        /**
         * Draws a reference point.
         *
         * @param  g  graphics context
         * @param  xc  X coordinate
         * @param  yc  Y coordinate
         */
        void drawCenterPoint( Graphics g, int xc, int yc ) {
            g.drawLine( xc, yc - CENTER_SIZE, xc, yc + CENTER_SIZE );
            g.drawLine( xc - CENTER_SIZE, yc, xc + CENTER_SIZE, yc );
        }

        @Override
        public int hashCode() {
            int code = 44321;
            code = 23 * code + getClass().hashCode();
            code = 23 * code + baseSize_;
            code = 23 * code + xTo_;
            code = 23 * code + yTo_;
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof ZoomIcon2d ) {
                ZoomIcon2d other = (ZoomIcon2d) o;
                return this.getClass().equals( other.getClass() )
                    && this.baseSize_ == other.baseSize_
                    && this.xTo_ == other.xTo_
                    && this.yTo_ == other.yTo_;
            }
            else {
                return false;
            }
        }
    }

    /**
     * Icon representing a wheel zoom in two dimensions.
     */
    private static class WheelZoomIcon2d extends ZoomIcon2d {

        /**
         * Constructor.
         *
         * @param  baseSize  radial size
         * @param  xFact   zoom factor in X direction
         * @param  yFact   zoom factor in Y direction
         */
        WheelZoomIcon2d( int baseSize, double xFact, double yFact ) {

            /* Multiply the factor here because the default tends to give
             * arrows that are too small to see easily.  This means that the
             * visual representation shows the result of WHEEL_MAG wheel
             * clicks not one. */
            super( baseSize, Math.pow( xFact, WHEEL_MAG ),
                             Math.pow( yFact, WHEEL_MAG ) );
        }

        void doPainting( Graphics g, int xc, int yc ) {
            drawCenterPoint( g, xc, yc );
            g.drawRect( xc - baseSize_, yc - baseSize_,
                        2 * baseSize_, 2 * baseSize_ );
            drawArrow( g, xc - baseSize_, yc, xc - xTo_, yc );
            drawArrow( g, xc + baseSize_, yc, xc + xTo_, yc );
            drawArrow( g, xc, yc - baseSize_, xc, yc - yTo_ );
            drawArrow( g, xc, yc + baseSize_, xc, yc + yTo_ );
        }
    }

    /**
     * Icon representing a drag zoom in two dimensions.
     */
    private static class DragZoomIcon2d extends ZoomIcon2d {

        /**
         * Constructor.
         *
         * @param  baseSize  radial size
         * @param  xFact   zoom factor in X direction
         * @param  yFact   zoom factor in Y direction
         */
        DragZoomIcon2d( int baseSize, double xFact, double yFact ) {
            super( baseSize, xFact, yFact );
        }

        void doPainting( Graphics g, int xc, int yc ) {
            g.drawRect( xc - baseSize_, yc - baseSize_,
                        2 * baseSize_, 2 * baseSize_ );
            g.drawRect( xc - xTo_, yc - yTo_, 2 * xTo_, 2 * yTo_ );
            drawCenterPoint( g, xc, yc );
        }
    }
}
