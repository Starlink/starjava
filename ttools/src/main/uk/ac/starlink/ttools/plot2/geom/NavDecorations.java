package uk.ac.starlink.ttools.plot2.geom;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import javax.swing.Icon;
import uk.ac.starlink.ttools.plot2.Decoration;
import uk.ac.starlink.ttools.plot2.Equality;

/**
 * Utility class supplying visual feedback decorations for
 * two-dimensional plot navigation.
 *
 * @author   Mark Taylor
 * @since    18 Feb 2014
 */
public class NavDecorations {

    private static final int BASE_SIZE = 32;
    private static final int BAND_SIZE = 16;
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
        return center( CENTER, p );
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
            return center( new WheelZoomIcon2d( BASE_SIZE, xfact, yfact ), p );
        }
        else if ( xuse ) {
            return center1d( new WheelZoomIcon1d( BASE_SIZE, false,
                                                  xfact, bounds ),
                             false, p, bounds );
        }
        else if ( yuse ) {
            return center1d( new WheelZoomIcon1d( BASE_SIZE, true,
                                                  yfact, bounds ),
                             true, p, bounds );
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
            return center( new DragZoomIcon2d( BASE_SIZE, xfact, yfact ), p );
        }
        else if ( xuse ) {
            return center1d( new DragZoomIcon1d( BASE_SIZE, false,
                                                 xfact, bounds ),
                             false, p, bounds );
        }
        else if ( yuse ) {
            return center1d( new DragZoomIcon1d( BASE_SIZE, true,
                                                 yfact, bounds ),
                             true, p, bounds );
        }
        else {
            return null;
        }
    }

    /**
     * Returns a decoration appropriate for a 2d frame zoom.
     * The returned object has a target rectangle associated with it,
     * which makes sense for this kind of decoration.
     *
     * @param  p1  drag start point
     * @param  p2  drag (current) end point
     * @param  xuse  true if X zoom is in effect
     * @param  yuse  true if Y zoom is in effect
     * @param  bounds  plot region bounds
     * @return   frame decoration with target rectangle
     */
    public static BandDecoration createBandDecoration( Point p1, Point p2,
                                                       boolean xuse,
                                                       boolean yuse,
                                                       Rectangle bounds ) {
        int dx = p2.x - p1.x;
        int dy = p2.y - p1.y;
        if ( xuse && yuse ) {
            if ( dx > 0 && dy > 0 ) {
                return new PlusBandZoomIcon2d( dx, dy )
                      .createBandDecoration( p1, bounds );
            }
            else if ( dx < 0 && dy <= 0 ||
                      dx <= 0 && dy < 0 ) {
                return new MinusBandZoomIcon2d( BAND_SIZE, -dx, -dy )
                      .createBandDecoration( p1, bounds );
            }
            else {
                return null;
            }
        }
        else if ( xuse && dx != 0) {
            // Interestingly, without the explicit BandIcon cast here,
            // the code compiles without error (J2SE1.5 at least) but
            // throws a NoSuchMethodError at runtime.
            // That must be a JDK or JRE bug.
            return ((BandIcon)
                    ( dx > 0 ? new PlusBandZoomIcon1d( false, dx, bounds )
                             : new MinusBandZoomIcon1d( BAND_SIZE,
                                                        false, -dx, bounds ) ))
                  .createBandDecoration( p1, bounds );
        }
        else if ( yuse && dy != 0 ) {
            return ((BandIcon)
                    ( dy > 0 ? new PlusBandZoomIcon1d( true, dy, bounds )
                             : new MinusBandZoomIcon1d( BAND_SIZE,
                                                        true, -dy, bounds ) ))
                  .createBandDecoration( p1, bounds );
        }
        else {
            return null;
        }
    }

    /**
     * Utility function to center an symmetric icon at a point.
     * The icon must correctly report its dimensions for this to work.
     *
     * @param   icon   icon
     * @param   p  central point
     * @return  centered decoration
     */
    public static Decoration center( Icon icon, Point p ) {
        return new Decoration( icon, p.x - icon.getIconWidth() / 2,
                                     p.y - icon.getIconHeight() / 2 );
    }

    /**
     * Utility function to center one of the 1-dimensional zoom icons
     * about a given point on its axis.
     * The icon must correctly report its dimensions for this to work.
     *
     * @param  icon  icon
     * @param  isY   false for X axis annotation, true for Y axis annotation
     * @param  p     reference point
     * @param  bounds  plot bounds
     * @return  centered decoration
     */
    public static Decoration center1d( Icon icon, boolean isY, Point p,
                                       Rectangle bounds ) {
        Point cp = isY
                 ? new Point( bounds.x, p.y - icon.getIconHeight() / 2 )
                 : new Point( p.x - icon.getIconWidth() / 2, bounds.y );
        return new Decoration( icon, cp.x, cp.y );
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
    public static void drawArrow( Graphics g,
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
     * Sets graphics context ready for navigation decorations.
     * The colour is modified as appropriate.
     * The result is a new graphics context, which does not need to be
     * reset (and should be disposed) when the caller is finished with it.
     *
     * @param   g  supplied graphics context
     * @return  new, adjusted graphics context based on <code>g</code>
     */
    public static Graphics2D prepareGraphics( Graphics g ) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor( COLOR );
        g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
                             RenderingHints.VALUE_ANTIALIAS_ON );
        return g2;
    }

    /**
     * Calculates a target rectangle associated with a negative band zoom
     * decoration.  Such decorations are visually characterised by an
     * outer (movable) and an inner (fixed) rectangle; the amount by which
     * the outer one is larger than the inner one determines how far the
     * outward zoom is intended.
     *
     * @param   outer   outer (movable) rectangle
     * @param   inner   inner (fixed) rectangle
     * @param   bounds  plot boundary
     * @return  target   rectangle giving requested bounds in the current
     *                   graphics coordinates of the region of interest
     */
    private static Rectangle createMinusZoomTarget( Rectangle outer,
                                                    Rectangle inner,
                                                    Rectangle bounds ) {
        int w = (int) ( bounds.width * outer.width * 1.0 / inner.width );
        int h = (int) ( bounds.height * outer.height * 1.0 / inner.height );
        int x = bounds.x + ( bounds.width - w ) / 2;
        int y = bounds.y + ( bounds.height - h ) / 2;
        return new Rectangle( x, y, w, h );
    }

    /**
     * Defines an object that can create a BandDecoration on request.
     */
    private interface BandIcon {

        /**
         * Create a BandDecoration (decoration with associated target
         * rectangle) for a given reference position and plot boundary.
         *
         * @param  origin  reference graphics position
         * @param  bounds  plot bounds
         * @return  decoration with associated target rectangle
         */
        BandDecoration createBandDecoration( Point origin, Rectangle bounds );
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
            g = prepareGraphics( g );
            g.drawLine( x + size_, y, x + size_, y + 2 * size_ );
            g.drawLine( x, y + size_, x + 2 * size_, y + size_ );
            g.dispose();
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
         * Constructs an icon for a destination value given in pixels.
         *
         * @param  baseSize   radial size of fixed part of marker
         * @param  isY   true for Y zoom, false for X
         * @param  cTo   destination value of zoom coordinate
         * @param  bounds   maximum bounds over which the icon should extend
         */
        public ZoomIcon1d( int baseSize, boolean isY, int cTo,
                           Rectangle bounds ) {
            baseSize_ = baseSize;
            isY_ = isY;
            cTo_ = cTo;
            blo_ = isY ? bounds.x : bounds.y;
            bhi_ = isY ? bounds.x + bounds.width : bounds.y + bounds.height;
        }

        /**
         * Constructs an icon for a given zoom factor.
         * The zoom factor is supplied as an array purely to avoid
         * confusion with the other constructor.
         * 
         * @param  baseSize   radial size of fixed part of marker
         * @param  isY   true for Y zoom, false for X
         * @param  cfactor   1-element array giving zoom factor
         * @param  bounds   maximum bounds over which the icon should extend
         */
        public ZoomIcon1d( int baseSize, boolean isY, double[] cfactor,
                           Rectangle bounds ) {
            this( baseSize, isY, (int) Math.round( baseSize * cfactor[ 0 ] ),
                  bounds );
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
            g = prepareGraphics( g );
            doPainting( g, isY_ ? y + getIconHeight() / 2
                                : x + getIconWidth() / 2 );
            g.dispose();
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

        /**
         * Returns a rectangle spanning the whole plot in the perpendicular
         * direction with given lower and upper bounds in zoom direction.
         *
         * @param  clo  lower bound in zoom direction
         * @param  chi  upper bound in zoom direction
         * @return  span rectangle
         */
        Rectangle createSpanRectangle( int clo, int chi ) {
            return isY_ ? new Rectangle( blo_, clo, bhi_ - blo_, chi - clo )
                        : new Rectangle( clo, blo_, chi - clo, bhi_ - blo_ );
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
            super( baseSize, isY,
                   new double[] { Math.pow( factor, WHEEL_MAG ) }, bounds );
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
            super( baseSize, isY, new double[] { factor }, bounds );
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
     * Icon representing a frame-type positive zoom in one dimension.
     */
    private static class PlusBandZoomIcon1d extends ZoomIcon1d
                                            implements BandIcon {

        /**
         * Constructor.
         *
         * @param   isY  true for Y zoom, false for X
         * @param   dc   drag extent in pixels
         * @param   bounds  maximum bounds over which the icon should extend
         */
        PlusBandZoomIcon1d( boolean isY, int dc, Rectangle bounds ) {
            super( 0, isY, dc, bounds );
        }

        void doPainting( Graphics g, int c0 ) {
            drawSpan( g, c0 );
            drawSpan( g, c0 + cTo_ );
        }

        public BandDecoration createBandDecoration( Point origin,
                                                    Rectangle bounds ) {
            int c0 = isY_ ? origin.y : origin.x;
            Rectangle target = createSpanRectangle( c0, c0 + cTo_ );
            return isY_ ? new BandDecoration( this, blo_, c0 - cTo_, target )
                        : new BandDecoration( this, c0 - cTo_, blo_, target );
        }
    }

    /**
     * Icon representing a frame-type negative zoom in one dimension.
     */
    private static class MinusBandZoomIcon1d extends ZoomIcon1d
                                             implements BandIcon {

        /**
         * Constructor.
         *
         * @param  baseSize  radial size of fixed part of icon
         * @param  isY  true for Y zoom, false for X
         * @param  dc   drag extent in pixels
         * @param  bounds   maximum bounds over which the icon should extend
         */
        MinusBandZoomIcon1d( int baseSize, boolean isY, int dc,
                             Rectangle bounds ) {
            super( baseSize, isY, baseSize + dc, bounds );
        }

        void doPainting( Graphics g, int c0 ) {
            drawSpan( g, c0 - baseSize_ );
            drawSpan( g, c0 + baseSize_ );
            drawSpan( g, c0 - cTo_ );
            drawSpan( g, c0 + cTo_ );
        }

        public BandDecoration createBandDecoration( Point origin,
                                                    Rectangle bounds ) {
            int c0 = isY_ ? origin.y : origin.x;
            Rectangle outer = createSpanRectangle( c0 - cTo_, c0 + cTo_ );
            Rectangle inner = createSpanRectangle( c0 - baseSize_,
                                                   c0 + baseSize_ );
            Rectangle target = createMinusZoomTarget( outer, inner, bounds );
            int gx = origin.x - getIconWidth() / 2;
            int gy = origin.y - getIconHeight() / 2;
            return new BandDecoration( this, gx, gy, target );
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
         * Constructs a zoom icon specifying destination position.
         *
         * @param  baseSize  radial size of fixed part of icon
         * @param  xTo   X destination coordinate
         * @param  yTo   Y destination coordinate
         */
        ZoomIcon2d( int baseSize, int xTo, int yTo ) {
            baseSize_ = baseSize;
            xTo_ = xTo;
            yTo_ = yTo;
        }

        /**
         * Constructs a zoom icon specifying zoom area by relative size.
         * The <code>xyFactors</code> parameter is given as an array
         * purely to avoid confusion with the other constructor.
         *
         * @param  baseSize  radial size of fixed part of icon
         * @param xyFactors  2-element array giving X,Y zoom factors
         */
        ZoomIcon2d( int baseSize, double[] xyFactors ) {
            this( baseSize,
                  (int) Math.round( baseSize * xyFactors[ 0 ] ),
                  (int) Math.round( baseSize * xyFactors[ 1 ] ) );
        }

        public int getIconWidth() {
            return 2 * Math.max( baseSize_, xTo_ );
        }

        public int getIconHeight() {
            return 2 * Math.max( baseSize_, yTo_ );
        }

        public void paintIcon( Component c, Graphics g, int x, int y ) {
            g = prepareGraphics( g );
            doPainting( g, x + getIconWidth() / 2, y + getIconHeight() / 2 );
            g.dispose();
        }

        /**
         * Paints this icon at a given point.
         *
         * @param  g  graphics context
         * @param  cx  central X coordinate
         * @param  cy  central Y coordinate
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
            super( baseSize,
                   new double[] { Math.pow( xFact, WHEEL_MAG ),
                                  Math.pow( yFact, WHEEL_MAG ) } );
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
            super( baseSize, new double[] { xFact, yFact } );
        }

        void doPainting( Graphics g, int xc, int yc ) {
            g.drawRect( xc - baseSize_, yc - baseSize_,
                        2 * baseSize_, 2 * baseSize_ );
            g.drawRect( xc - xTo_, yc - yTo_, 2 * xTo_, 2 * yTo_ );
            drawCenterPoint( g, xc, yc );
        }
    }

    /**
     * Icon representing a frame-type positive zoom in two dimensions.
     */
    private static class PlusBandZoomIcon2d extends ZoomIcon2d
                                            implements BandIcon {

        /**
         * Constructor.
         *
         * @param  dx  drag extent in X direction
         * @param  dy  drag extent in Y direction
         */
        PlusBandZoomIcon2d( int dx, int dy ) {
            super( 0, dx, dy );
        }

        void doPainting( Graphics g, int xc, int yc ) {
            g.drawRect( xc - xTo_, yc - yTo_, xTo_, yTo_ );
        }

        public BandDecoration createBandDecoration( Point origin,
                                                    Rectangle bounds ) {
            Rectangle target = new Rectangle( origin.x, origin.y, xTo_, yTo_ );
            return new BandDecoration( this, origin.x, origin.y, target );
        }
    }

    /**
     * Icon representing a frame-type negative zoom in two dimensions.
     */
    private static class MinusBandZoomIcon2d extends ZoomIcon2d
                                             implements BandIcon {

        /**
         * Constructor.
         *
         * @param  baseSize  radial size of fixed part of icon
         * @param  dx  drag extent in X direction
         * @param  dy  drag extent in Y direction
         */
        MinusBandZoomIcon2d( int baseSize, int dx, int dy ) {
            super( baseSize, baseSize + dx, baseSize + dy );
        }

        void doPainting( Graphics g, int xc, int yc ) {
            g.drawRect( xc - baseSize_, yc - baseSize_,
                        2 * baseSize_, 2 * baseSize_ );
            g.drawRect( xc - xTo_, yc - yTo_, 2 * xTo_, 2 * yTo_ );
        }

        public BandDecoration createBandDecoration( Point origin,
                                                    Rectangle bounds ) {
            Rectangle outer =
                new Rectangle( origin.x - xTo_, origin.y - yTo_,
                               2 * xTo_, 2 * yTo_ );
            Rectangle inner =
                new Rectangle( origin.x - baseSize_, origin.y - baseSize_,
                               2 * baseSize_, 2 * baseSize_ );
            Rectangle target = createMinusZoomTarget( outer, inner, bounds );
            return new BandDecoration( this, origin.x - xTo_, origin.y - yTo_,
                                       target );
        }
    }
}
