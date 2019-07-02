package uk.ac.starlink.ttools.plot2.geom;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.Icon;
import uk.ac.starlink.ttools.plot.Corner;
import uk.ac.starlink.ttools.plot2.Decoration;
import uk.ac.starlink.ttools.plot2.Equality;

/**
 * Utility class supplying visual feedback decorations for 
 * three-dimensional plot navigation.
 *
 * @author   Mark Taylor
 * @since    24 Feb 2014
 */
public class NavDecorations3D {

    /**
     * Private constructor prevents instantiation.
     */
    private NavDecorations3D() {
    }

    /**
     * Returns a decoration suitable for a 3d drag zoom centered on the
     * cube center.
     *
     * @param  csurf  plotting surface
     * @param  zoomFactor  zoom factor
     * @param  useFlags  3-element array indicating if X,Y,Z directions
     *                   are zoomed
     * @return  decoration
     */
    public static Decoration createCenterDragDecoration( CubeSurface csurf,
                                                         double zoomFactor,
                                                         boolean[] useFlags ) {
        double[] factors = new double[ 3 ];
        for ( int i = 0; i < 3; i++ ) {
            factors[ i ] = useFlags[ i ] ? zoomFactor : 1;
        }
        return NavDecorations
              .center( new DragIcon3D( csurf, factors ),
                       projectNorm( csurf, new double[ 3 ] ) );
    }

    /**
     * Returns a decoration suitable for a wheel zoom centered on the
     * cube center.
     *
     * @param  csurf  plotting surface
     * @param  zoomFactor  zoom factor
     * @param  useFlags  3-element array indicating if X,Y,Z directions
     *                   are zoomed
     * @return  decoration
     */
    public static Decoration createCenterWheelDecoration( CubeSurface csurf,
                                                          double zoomFactor,
                                                          boolean[] useFlags ) {
        return NavDecorations
              .center( new WheelIcon3D( csurf, zoomFactor, useFlags ),
                       projectNorm( csurf, new double[ 3 ] ) );
    }

    /**
     * Returns a decoration suitable for a drag zoom in the two facing
     * directions.  The zoom directions are determined by logic in the
     * supplied cube surface.
     *
     * @param  csurf  plotting surface
     * @param  pos    screen position around which surface is zoomed
     * @param  xf     zoom factor in mostly-horizontal direction
     * @param  yf     zoom factor in mostly-vertical direction
     * @return   decoration
     */
    public static Decoration create2dZoomDecoration( CubeSurface csurf,
                                                     Point pos,
                                                     double xf, double yf ) {
        return NavDecorations.center( new DragIcon2D( csurf, xf, yf ), pos );
    }

    /**
     * Returns a decoration suitable for a drag pan operation in the two
     * facing directions.  The pan directions are determined by logic in the
     * supplied cube surface.
     *
     * @param   csurf  plotting surface
     * @param   pos  reference position for drag
     */
    public static Decoration create2dPanDecoration( CubeSurface csurf,
                                                    Point pos ) {
        return NavDecorations.center( new GrabIcon2D( csurf ), pos );
    }

    /**
     * Returns a decoration to indicated recentering from a given screen
     * position to the surface center.
     *
     * @param  csurf  plotting surface
     * @param  pos   reference position for new center
     */
    public static Decoration createRecenterDecoration( CubeSurface csurf,
                                                       Point pos ) {
        RecenterIcon icon = new RecenterIcon( csurf, pos );
        return new Decoration( icon, icon.xoff_, icon.yoff_ );
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
    private static Graphics2D prepareGraphics( Graphics g ) {
        return NavDecorations.prepareGraphics( g );
    }

    /**
     * Marks a point with a circle.
     *
     * @param   g  graphics context
     * @param   x  screen X coordinate
     * @param   y  screen Y coordinate
     */
    private static void markPoint( Graphics g, int x, int y ) {
        final int r = 3;
        g.drawOval( x - r, y - r, 2 * r, 2 * r );
    }

    /**
     * Icon indicating a grab/pan state in the two facing screen directions.
     * The reference position is in the center of the icon.
     */
    @Equality
    private static class GrabIcon2D implements Icon {

        private final Parallelogram pgram_;

        /**
         * Constructor.
         *
         * @param   csurf  plotting surface
         */
        GrabIcon2D( CubeSurface csurf ) {

            /* Get surface center. */
            Point c = projectNorm( csurf, new double[ 3 ] );

            /* Work out which 3d unit vectors are mostly-horizontal
             * and mostly-vertical. */
            int[] dirs = csurf.getScreenDirections();

            /* Calculate and store the corresponding 2d screen vectors. */
            double baseNormSize = 0.1;
            double[] xv = new double[ 3 ];
            xv[ dirs[ 0 ] ] = baseNormSize;
            Point px = projectNorm( csurf, xv );
            double[] yv = new double[ 3 ];
            yv[ dirs[ 1 ] ] = baseNormSize;
            Point py = projectNorm( csurf, yv );
            pgram_ = new Parallelogram( new Point( px.x - c.x, px.y - c.y ),
                                        new Point( py.x - c.x, py.y - c.y ) );
        }

        public int getIconWidth() {
            return 2 * ( pgram_.xv_.x + pgram_.yv_.x );
        }

        public int getIconHeight() {
            return 2 * ( pgram_.xv_.y + pgram_.yv_.y );
        }

        public void paintIcon( Component c, Graphics g, int x, int y ) {
            g = prepareGraphics( g );
            int x0 = x + getIconWidth() / 2;
            int y0 = y + getIconHeight() / 2;
            Point xv = pgram_.xv_;
            Point yv = pgram_.yv_;
            g.translate( x0, y0 );
            g.drawLine( -xv.x, -xv.y, xv.x, xv.y );
            g.drawLine( -yv.x, -yv.y, yv.x, yv.y );
            g.translate( -x0, -y0 );
            g.dispose();
        }

        @Override
        public int hashCode() {
            return pgram_.hashCode();
        }

        @Override
        public boolean equals( Object o ) {
            return o instanceof GrabIcon2D
                && this.pgram_.equals( ((GrabIcon2D) o).pgram_ );
        }
    }

    /**
     * Icon indicating a warp from a given position to the surface center.
     */
    @Equality
    private static class RecenterIcon implements Icon {

        private final Point p0_;
        private final Point p1_;
        private final int xoff_;
        private final int yoff_;

        /**
         * Constructor.
         *
         * @param  csurf  plotting surface
         * @param  pos    warp start screen position
         */
        RecenterIcon( CubeSurface csurf, Point pos ) {

            /* Get surface center in screen coordinates. */
            Point center = projectNorm( csurf, new double[ 3 ] );

            /* Calculate the start and end positions of the warp vector.
             * This is a bit fiddly because of the way Icons are defined
             * (xmin, ymin, width, height) - need to treat cases differently
             * according to whether the line is increasing or decreasing
             * in X and Y directions.  For the same reason we also need
             * to store the X and Y offset positions so we can position it
             * correctly later. */
            final int x0;
            final int y0;
            final int x1;
            final int y1;
            if ( pos.x < center.x ) {
                xoff_ = pos.x;
                x0 = 0;
                x1 = center.x - pos.x;
            }
            else {
                xoff_ = center.x;
                x0 = pos.x - center.x;
                x1 = 0;
            }
            if ( pos.y < center.y ) {
                yoff_ = pos.y;
                y0 = 0;
                y1 = center.y - pos.y;
            }
            else {
                yoff_ = center.y;
                y0 = pos.y - center.y;
                y1 = 0;
            }
            p0_ = new Point( x0, y0 );
            p1_ = new Point( x1, y1 );
        }

        public int getIconWidth() {
            return Math.abs( p1_.x - p0_.x );
        }

        public int getIconHeight() {
            return Math.abs( p1_.y - p0_.y );
        }

        public void paintIcon( Component c, Graphics g, int x, int y ) {
            g = prepareGraphics( g );
            markPoint( g, x + p0_.x, y + p0_.y );
            NavDecorations.drawArrow( g, x + p0_.x, y + p0_.y,
                                         x + p1_.x, y + p1_.y );
            g.dispose();
        }

        @Override
        public int hashCode() {
            int code = 55821;
            code = 23 * code + p0_.hashCode();
            code = 23 * code + p1_.hashCode();
            code = 23 * code + xoff_;
            code = 23 * code + yoff_;
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof RecenterIcon ) {
                RecenterIcon other = (RecenterIcon) o;
                return this.p0_.equals( other.p0_ )
                    && this.p1_.equals( other.p1_ )
                    && this.xoff_ == other.xoff_
                    && this.yoff_ == other.yoff_;
            }
            else {
                return false;
            }
        }
    }

    /**
     * Icon indicating a wheel zoom gesture centered on the surface center.
     */
    @Equality
    private static class WheelIcon3D implements Icon {

        private final Point center_;
        private final Line[] lines_;

        /**
         * Constructor.
         *
         * @param  csurf  plotting surface
         * @param  zoomFactor  zoom factor
         * @param  useFlags  3-element array indicating if X,Y,Z directions
         *                   are zoomed
         */
        public WheelIcon3D( CubeSurface csurf, double zoomFactor,
                            boolean[] useFlags ) {
            double baseNormSize = 0.25;
            double lineFactor = Math.pow( zoomFactor, 3 );

            /* Get cube center in screen coordinates. */
            Point center = projectNorm( csurf, new double[ 3 ] );
            center_ = center;

            /* Prepare a list of the lines indicating zoom directions
             * to draw. */
            List<Line> lineList = new ArrayList<Line>();
            for ( int i = 0; i < 3; i++ ) {
                if ( useFlags[ i ] ) {
                    double[] npos = new double[ 3 ];
                    npos[ i ] = baseNormSize;
                    Point p0 = projectNorm( csurf, npos );
                    npos[ i ] *= lineFactor;
                    Point p1 = projectNorm( csurf, npos );
                    if ( ! p0.equals( p1 ) ) {
                        p0.x -= center.x;
                        p0.y -= center.y;
                        p1.x -= center.x;
                        p1.y -= center.y;
                        lineList.add( new Line( p0, p1 ) );
                    }
                }
            }
            lines_ = lineList.toArray( new Line[ 0 ] );
        } 

        public int getIconWidth() {
            int w2 = 0;
            for ( int i = 0; i < lines_.length; i++ ) {
                Line line = lines_[ i ];
                w2 = Math.max( w2, Math.abs( line.p0_.x ) );
                w2 = Math.max( w2, Math.abs( line.p1_.x ) );
            }
            return 2 * w2;
        }

        public int getIconHeight() {
            int h2 = 0;
            for ( int i = 0; i < lines_.length; i++ ) {
                Line line = lines_[ i ];
                h2 = Math.max( h2, Math.abs( line.p0_.y ) );
                h2 = Math.max( h2, Math.abs( line.p1_.y ) );
            }
            return 2 * h2;
        }

        public void paintIcon( Component c, Graphics g, int x, int y ) {
            g = prepareGraphics( g );
            int x0 = x + getIconWidth() / 2;
            int y0 = y + getIconHeight() / 2;
            markPoint( g, center_.x, center_.y );
            for ( int i = 0; i < lines_.length; i++ ) {
                Line line = lines_[ i ];
                drawArrow( g, x0 + line.p0_.x, y0 + line.p0_.y,
                              x0 + line.p1_.x, y0 + line.p1_.y );
                drawArrow( g, x0 - line.p0_.x, y0 - line.p0_.y,
                              x0 - line.p1_.x, y0 - line.p1_.y );
            }
            g.dispose();
        }

        @Override
        public int hashCode() {
            int code = 248482;
            code = 23 * code + center_.hashCode();
            code = 23 * code + Arrays.hashCode( lines_ );
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof WheelIcon3D ) {
                WheelIcon3D other = (WheelIcon3D) o;
                return this.center_.equals( other.center_ )
                    && Arrays.equals( this.lines_, other.lines_ );
            }
            else {
                return false;
            }
        }

        /**
         * Draws a directed line between two points.
         * Currently this draws an elongated triangle because it's easier
         * to get looking right in 3D than something with an arrow head.
         *
         * @param   g  graphics context
         * @param  x0  start X coordinate
         * @param  y0  start Y coordinate
         * @param  x1  end X coordinate
         * @param  y1  end Y coordinate
         */
        private static void drawArrow( Graphics g, int x0, int y0,
                                                   int x1, int y1 ) {
            if ( x0 != x1 || y0 != y1 ) {
                Graphics2D g2 = (Graphics2D) g;
                AffineTransform trans0 = g2.getTransform();
                g2.translate( x0, y0 );
                g2.rotate( Math.atan2( y1 - y0, x1 - x0 ) );
                int leng =
                    (int) Math.round( Math.sqrt( ( x1 - x0 ) * ( x1 - x0 )
                                               + ( y1 - y0 ) * ( y1 - y0 ) ) );
                Polygon poly = new Polygon( new int[] { 0, 0, leng },
                                            new int[] { -5, +5, 0 }, 3 );
                g2.drawPolygon( poly );
                g2.drawLine( 0, 0, leng, 0 );
                g2.setTransform( trans0 );
            }
        }
    }

    /**
     * Icon indicating a 2d drag zoom in the two facing screen directions
     * This is one fixed and one variable-sized rectangle.
     */
    @Equality
    private static class DragIcon2D implements Icon {

        private final Parallelogram pgram0_;
        private final Parallelogram pgram1_;

        /**
         * Constructor.
         *
         * @param  csurf  plotting surface
         * @param  xf     zoom factor in mostly-horizontal direction
         * @param  yf     zoom factor in mostly-vertical direction
         */
        public DragIcon2D( CubeSurface csurf, double xf, double yf ) {
            double baseNormSize = 0.2;

            /* Get surface center. */
            Point c = projectNorm( csurf, new double[ 3 ] );

            /* Work out which 3d unit vectors are mostly-horizontal
             * and mostly-vertical. */
            int[] dirs = csurf.getScreenDirections();

            /* Cacluate and store the corresponding screen vectors. */
            int jd0 = dirs[ 0 ];
            int jd1 = dirs[ 1 ];
            double[] xv = new double[ 3 ];
            xv[ jd0 ] = baseNormSize;
            Point x0 = projectNorm( csurf, xv );
            xv[ jd0 ] *= xf;
            Point x1 = projectNorm( csurf, xv );
            double[] yv = new double[ 3 ];
            yv[ jd1 ] = baseNormSize;
            Point y0 = projectNorm( csurf, yv );
            yv[ jd1 ] *= yf;
            Point y1 = projectNorm( csurf, yv );
            pgram0_ = new Parallelogram( new Point( x0.x - c.x, x0.y - c.y ),
                                         new Point( y0.x - c.x, y0.y - c.y ) );
            pgram1_ = new Parallelogram( new Point( x1.x - c.x, x1.y - c.y ),
                                         new Point( y1.x - c.x, y1.y - c.y ) );
        }

        public int getIconWidth() {
            return 2 * Math.max( pgram0_.xv_.x + pgram0_.yv_.x,
                                 pgram1_.xv_.x + pgram1_.yv_.x );
        }

        public int getIconHeight() {
            return 2 * Math.max( pgram0_.xv_.y + pgram0_.yv_.y,
                                 pgram1_.xv_.y + pgram1_.yv_.y );
        }

        public void paintIcon( Component c, Graphics g, int x, int y ) {
            g = prepareGraphics( g );
            int x0 = x + getIconWidth() / 2;
            int y0 = y + getIconHeight() / 2;
            g.translate( x0, y0 );
            drawCross( g, pgram0_, 0.25 );
            drawParallelogram( g, pgram0_ );
            drawParallelogram( g, pgram1_ );
            g.translate( -x0, -y0 );
            g.dispose();
        }

        @Override
        public int hashCode() {
            int code = 324;
            code = 23 * code + pgram0_.hashCode();
            code = 23 * code + pgram1_.hashCode();
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof DragIcon2D ) {
                 DragIcon2D other = (DragIcon2D) o;
                 return this.pgram0_.equals( other.pgram0_ )
                     && this.pgram1_.equals( other.pgram1_ );
            }
            else {
                return false;
            }
        }

        /**
         * Draws a parallelogram centered on the graphics origin.
         *
         * @param  g  graphics context
         * @param  pg  parallelogram
         */
        private static void drawParallelogram( Graphics g, Parallelogram pg ) {
            Point xv = pg.xv_;
            Point yv = pg.yv_;
            int[] xs = { +xv.x +yv.x, +xv.x -yv.x, -xv.x -yv.x, -xv.x +yv.x };
            int[] ys = { +xv.y +yv.y, +xv.y -yv.y, -xv.y -yv.y, -xv.y +yv.y };
            g.drawPolygon( xs, ys, 4 );
        }

        /**
         * Draws a little cross parallel to given parallelogram vectors,
         * centered on the graphics origin.
         *
         * @param  g  graphics context
         * @param  pg  parallelogram defining cross arm directions
         * @param  frac  fraction of parallelogram sides giving
         *               cross arm lengths
         */
        private static void drawCross( Graphics g, Parallelogram pg,
                                       double frac ) {
            int x0 = (int) Math.round( pg.xv_.x * frac );
            int y0 = (int) Math.round( pg.xv_.y * frac );
            int x1 = (int) Math.round( pg.yv_.x * frac );
            int y1 = (int) Math.round( pg.yv_.y * frac );
            g.drawLine( -x0, -y0, +x0, +y0 );
            g.drawLine( -x1, -y1, +x1, +y1 );
        }
    }

    /**
     * Icon indicating a 3d drag zoom.
     * This is one fixed and one variable-sized cuboid.
     */
    @Equality
    private static class DragIcon3D implements Icon {

        private final Parallelepiped pped0_;
        private final Parallelepiped pped1_;

        /**
         * Constructor.
         *
         * @param  csurf  plotting surface
         * @param  factors  3-element array giving X,Y,Z zoom factors
         */
        public DragIcon3D( CubeSurface csurf, double[] factors ) {
            double baseNormSize = 0.2;

            /* Get surface center. */
            Point c = projectNorm( csurf, new double[ 3 ] );

            /* Calculate and store the screen positions of the vectors
             * defining the dimensions of the two cuboids - one at a
             * fixed size and one at the zoomed size. */
            Point[] xyz0 = new Point[ 3 ];
            Point[] xyz1 = new Point[ 3 ];
            for ( int i = 0; i < 3; i++ ) {
                double[] pn0 = new double[ 3 ];
                double[] pn1 = new double[ 3 ];
                pn0[ i ] = baseNormSize;
                pn1[ i ] = baseNormSize * factors[ i ];
                Point p0 = projectNorm( csurf, pn0 );
                Point p1 = projectNorm( csurf, pn1 );
                xyz0[ i ] = new Point( p0.x - c.x, p0.y - c.y );
                xyz1[ i ] = new Point( p1.x - c.x, p1.y - c.y );
            }
            pped0_ = new Parallelepiped( xyz0 );
            pped1_ = new Parallelepiped( xyz1 );
        }

        public int getIconWidth() {
            int w = 0;
            for ( int ic = 0; ic < Corner.COUNT; ic++ ) {
                Corner corner = Corner.getCorner( ic );
                w = Math.max( w, Math.abs( getVertex( pped0_, corner ).x ) );
                w = Math.max( w, Math.abs( getVertex( pped1_, corner ).x ) );
            }
            return 2 * w;
        }

        public int getIconHeight() {
            int h = 0;
            for ( int ic = 0; ic < Corner.COUNT; ic++ ) {
                Corner corner = Corner.getCorner( ic );
                h = Math.max( h, Math.abs( getVertex( pped0_, corner ).y ) );
                h = Math.max( h, Math.abs( getVertex( pped1_, corner ).y ) );
            }
            return 2 * h;
        }

        public void paintIcon( Component c, Graphics g, int x, int y ) {
            g = prepareGraphics( g );
            int x0 = x + getIconWidth() / 2;
            int y0 = y + getIconHeight() / 2;
            g.translate( x0, y0 );
            markPoint( g, 0, 0 );
            drawParallelepiped( g, pped0_ );
            drawParallelepiped( g, pped1_ );
            g.translate( -x0, -y0 );
            g.dispose();
        }

        @Override
        public int hashCode() {
            int code = 245;
            code = 23 * code + pped0_.hashCode();
            code = 23 * code + pped1_.hashCode();
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof DragIcon3D ) {
                DragIcon3D other = (DragIcon3D) o;
                return this.pped0_.equals( other.pped0_ )
                    && this.pped1_.equals( other.pped1_ );
            }
            else {
                return false;
            }
        }

        /**
         * Draws a parallepiped centered on the graphics origin.
         *
         * @param  g  graphics context
         * @param  pg  parallelepiped
         */
        private static void drawParallelepiped( Graphics g,
                                                Parallelepiped pped ) {
            for ( int ic = 0; ic < Corner.COUNT; ic++ ) {
                Corner c0 = Corner.getCorner( ic );
                Corner[] others = c0.getAdjacent();
                for ( int jc = 0; jc < others.length; jc++ ) {
                    Corner c1 = others[ jc ];
                    if ( c1.compareTo( c0 ) > 0 ) {
                        Point p0 = getVertex( pped, c0 );
                        Point p1 = getVertex( pped, c1 );
                        g.drawLine( p0.x, p0.y, p1.x, p1.y );
                    }
                }
            }
        }

        /**
         * Returns the screen position of one corner of a parallelepiped.
         *
         * @param  pped  parallelepiped
         * @param  corner  corner identifier
         */
        private static Point getVertex( Parallelepiped pped, Corner corner ) {
            boolean[] flags = corner.getFlags();
            int dx = 0;
            int dy = 0;
            for ( int i = 0; i < 3; i++ ) {
                int f = flags[ i ] ? +1 : -1;
                Point p = pped.xyz_[ i ];
                dx += f * p.x;
                dy += f * p.y;
            }
            return new Point( dx, dy );
        }
    }

    /**
     * Calculcates the screen position of a 3-d vector
     * in normalised coordinates.
     *
     * @param  csurf  plotting surface
     * @param  normPos  3-element array giving normalised X,Y,Z coordinates
     * @return   screen position
     */
    private static Point projectNorm( CubeSurface csurf, double[] normPos ) {
        Point2D.Double p = csurf.projectNormalisedPos( normPos );
        return new Point( (int) Math.round( p.getX() ),
                          (int) Math.round( p.getY() ) );
    }

    @Equality
    private static class Line {
        final Point p0_;
        final Point p1_;

        Line( Point p0, Point p1 ) {
            p0_ = p0;
            p1_ = p1;
        }

        public String toString() {
            return p0_ + " -> " + p1_;
        }

        @Override
        public int hashCode() {
            int code = 234;
            code = 23 * code + p0_.hashCode();
            code = 23 * code + p1_.hashCode();
            return code;
        }

        @Override
        public boolean equals( Object o ) { 
            if ( o instanceof Line ) {
                Line other = (Line) o;
                return this.p0_.equals( other.p0_ )
                    && this.p1_.equals( other.p1_ );
            }
            else {
                return false;
            }
        }
    }

    @Equality
    private static class Parallelogram {
        final Point xv_;
        final Point yv_;

        Parallelogram( Point xv, Point yv ) {
            xv_ = xv;
            yv_ = yv;
        }

        @Override
        public int hashCode() {
            int code = 567;
            code = 23 * code + xv_.hashCode();
            code = 23 * code + yv_.hashCode();
            return code;
        }

        @Override
        public boolean equals( Object o ) { 
            if ( o instanceof Parallelogram ) {
                Parallelogram other = (Parallelogram) o;
                return this.xv_.equals( other.xv_ )
                    && this.yv_.equals( other.yv_ );
            }
            else {
                return false;
            }
        }
    }

    @Equality
    private static class Parallelepiped {
        final Point[] xyz_;

        Parallelepiped( Point[] xyz ) {
            xyz_ = xyz;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode( xyz_ );
        }

        @Override
        public boolean equals( Object o ) {
            return o instanceof Parallelepiped
                && Arrays.equals( this.xyz_, ((Parallelepiped) o).xyz_ );
        }
    }
}
