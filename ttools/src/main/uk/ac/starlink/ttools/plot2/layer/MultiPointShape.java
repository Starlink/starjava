package uk.ac.starlink.ttools.plot2.layer;

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import javax.swing.Icon;
import uk.ac.starlink.ttools.plot.ErrorMode;
import uk.ac.starlink.ttools.plot.Matrices;
import uk.ac.starlink.ttools.plot2.Pixer;
import uk.ac.starlink.util.IconUtils;

/**
 * Defines a graphical shape with coordinates supplied by a number of
 * offset points.
 *
 * <p>This code is a reworked version of the class
 * {@link uk.ac.starlink.ttools.plot.ErrorRenderer},
 * originally intended for drawing error bars,
 * but the usage is more general than that.
 *
 * @author   Mark Taylor
 * @since    21 Sep 2021
 */
public abstract class MultiPointShape {

    private final String name_;

    /** Multi-point shape which draws nothing. */
    public static final MultiPointShape NONE = new Blank( "None" );

    /** General purpose multi-point shape. */
    public static final MultiPointShape DEFAULT =
        new CappedLine( "Lines", true, null );

    /** Shape suitable for use in user controls. */
    public static final MultiPointShape EXAMPLE =
        new CappedLine( "Capped Lines", true, new BarCapper( 3 ) );

    private static final Stroke CAP_ROUND =
        new BasicStroke( 1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER );
    private static final Stroke CAP_BUTT =
        new BasicStroke( 1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER );
    private static final int DUMMY_SIZE = 10000;
    private static final int LEGEND_WIDTH = 40;
    private static final int LEGEND_HEIGHT = 16;
    private static final int LEGEND_XPAD = 5;
    private static final int LEGEND_YPAD = 1;
    private static final double SQRT2 = Math.sqrt( 2.0 );

    private static final MultiPointShape[] OPTIONS_1D = {
        NONE,
        DEFAULT,
        EXAMPLE,
        new CappedLine( "Caps", false, new BarCapper( 3 ) ),
        new CappedLine( "Arrows", true, new ArrowCapper( 3 ) ),
    };
    private static final MultiPointShape[] OPTIONS_2D = {
        NONE,
        DEFAULT,
        EXAMPLE,
        new CappedLine( "Caps", false, new BarCapper( 3 ) ),
        new CappedLine( "Arrows", true, new ArrowCapper( 3 ) ),
        new OpenEllipse( "Ellipse", false ),
        new OpenEllipse( "Crosshair Ellipse", true ),
        new OpenRectangle( "Rectangle", false ),
        new OpenRectangle( "Crosshair Rectangle", true ),
        new FilledEllipse( "Filled Ellipse" ),
        new FilledRectangle( "Filled Rectangle" ),
    };
    private static final MultiPointShape[] OPTIONS_3D = {
        NONE,
        DEFAULT,
        EXAMPLE,
        new CappedLine( "Caps", false, new BarCapper( 3 ) ),
        new CappedLine( "Arrows", true, new ArrowCapper( 3 ) ),
        new OpenCuboid( "Cuboid" ),
        new MultiPlaneShape( new OpenEllipse( "Ellipse", false ) ),
        new MultiPlaneShape( new OpenEllipse( "Crosshair Ellipse", true ) ),
        new MultiPlaneShape( new OpenRectangle( "Rectangle", false ) ),
        new MultiPlaneShape( new OpenRectangle( "Crosshair Rectangle", true ) ),
        new MultiPlaneShape( new FilledEllipse( "Filled Ellipse" ) ),
        new MultiPlaneShape( new FilledRectangle( "Filled Rectangle" ) ),
    };
    private static final MultiPointShape[] OPTIONS_VECTOR = {
        new CappedLine( "Small Arrow", true, new ArrowCapper( 3 ) ),
        new CappedLine( "Medium Arrow", true, new ArrowCapper( 4 ) ),
        new CappedLine( "Large Arrow", true, new ArrowCapper( 5 ) ),
        new Dart( "Small Open Dart", false, 2 ),
        new Dart( "Medium Open Dart", false, 4 ),
        new Dart( "Large Open Dart", false, 6 ),
        new Dart( "Small Filled Dart", true, 2 ),
        new Dart( "Medium Filled Dart", true, 4 ),
        new Dart( "Large Filled Dart", true, 6 ),
        DEFAULT,
        EXAMPLE,
    };
    private static final MultiPointShape[] OPTIONS_ELLIPSE = {
        new OpenEllipse( "Ellipse", false ),
        new OpenEllipse( "Crosshair Ellipse", true ),
        new FilledEllipse( "Filled Ellipse" ),
        new OpenRectangle( "Rectangle", false ),
        new OpenRectangle( "Crosshair Rectangle", true ),
        new FilledRectangle( "Filled Rectangle" ),
        new Triangle( "Open Triangle", false ),
        new Triangle( "Filled Triangle", true ),
        DEFAULT,
        EXAMPLE,
        new CappedLine( "Arrows", true, new ArrowCapper( 3 ) ),
    };

    /**
     * Constructor.
     *
     * @param  name  user-directed shape name
     */
    protected MultiPointShape( String name ) {
        name_ = name;
    }

    /**
     * Returns an icon giving a general example of what this shape looks like.
     *
     * @return  example icon
     */
    public abstract Icon getLegendIcon();

    /**
     * Returns an icon giving an example of what this shape looks like in a
     * detailed context.
     *
     * @param   modes  array of ErrorModes, one per error dimension (x, y, ...)
     * @param   width  total width of icon
     * @param   height total height of icon
     * @param   xpad   internal horizontal padding of icon
     * @param   ypad   internal vertical padding of icon
     */
    public abstract Icon getLegendIcon( ErrorMode[] modes, int width,
                                        int height, int xpad, int ypad );

    /**
     * Returns a user-readable name for this shape.
     *
     * @return   shape name
     */
    public String getName() {
        return name_;
    }

    /**
     * Reports whether this shape can be used in a given dimensionality.
     *
     * @param  ndim  number of error dimensions to be used
     * @return  true iff this object can do rendering
     */
    public abstract boolean supportsDimensionality( int ndim );
        
    /**
     * Returns a rectangle which will contain the rendered shape graphics
     * for a given point.  This may be oversized, but should not be too
     * much so for efficiency reasons.
     *      
     * @param  x  data point X coordinate
     * @param  y  data point Y coordinate
     * @param  xoffs  X coordinates of shape limit offsets from (x,y)
     * @param  yoffs  Y coordinates of shape limit offsets from (x,y)
     * @return  bounding box
     */ 
    public abstract Rectangle getBounds( int x, int y, int[] xoffs,
                                         int[] yoffs );

    /**
     * Draws a multi-point shape around a given point.
     * The positions defining the shape relative to the the point
     * are given.  There may in general be (2*N) of these, though certain
     * <code>MultiPointShape</code> implementations may impose restrictions
     * on this count.  Error bars come in consecutive pairs which describe
     * error bars along the same axis in different directions.
     * Missing error bars are represented as (0,0).  The values must come
     * in axis order where that makes sense, but note in some contexts
     * (e.g. 3D) these may be data axes rather than graphics plane axes.
     *
     * <p>This method is quite likely to get called from time to time with
     * ridiculously large offset arrays.  Implementations should try to
     * ensure that they don't attempt graphics operations which may
     * cause the graphics system undue grief, such as filling an ellipse
     * the size of a village.
     *
     * @param  g  graphics context
     * @param  x  data point X coordinate
     * @param  y  data point Y coordinate
     * @param  xoffs  X coordinates of shape offsets from (x,y)
     * @param  yoffs  Y coordinates of shape offsets from (x,y)
     */
    public abstract void drawShape( Graphics g,
                                    int x, int y, int[] xoffs, int[] yoffs );

    /**
     * Returns an factory for pixel positions which can be used to draw this
     * marker onto a raster.  This can be used as an alternative to
     * rendering the marker using the {@link #drawShape} method,
     * for instance in situations where it might be more efficient.
     * The assumption is that all the pixels are the same colour.
     *
     * <p>The default implementation calculates this by painting
     * onto a temporary BufferedImage and then examining the raster to see
     * which pixels have been painted.  This is probably not very efficient.
     * Subclasses are encouraged to override this method if they can
     * calculate the pixels which will be painted directly.
     *
     * @param  clip  clipping region
     * @param  x  data point X coordinate
     * @param  y  data point Y coordinate
     * @param  xoffs  X coordinates of error bar limit offsets from (x,y)
     * @param  yoffs  Y coordinates of error bar limit offsets from (x,y)
     * @return  factory for Pixers, or null if nothing to be drawn
     */
    public PixerFactory createPixerFactory( Rectangle clip, int x, int y,
                                            int[] xoffs, int[] yoffs ) {

        /* Work out the size of raster we will need to paint onto. */
        Rectangle bounds = getBounds( x, y, xoffs, yoffs ).intersection( clip );
        int xdim = bounds.width;
        int ydim = bounds.height;

        /* Return null if it has zero size. */
        if ( xdim <= 0 || ydim <= 0 ) {
            return null;
        }

        /* Prepare an image of a suitable size and a graphics context which
         * will render onto it. */
        BufferedImage im =
            new BufferedImage( xdim, ydim, BufferedImage.TYPE_INT_ARGB );
        Graphics2D g2 = im.createGraphics();
        g2.translate( -bounds.x, -bounds.y );
        g2.setClip( bounds );

        /* Paint the shape. */
        drawShape( g2, x, y, xoffs, yoffs );

        /* Examine each pixel from the raster and write its coordinates
         * into the coordinate list if it has been painted on (alpha is
         * non-zero). */
        Raster raster = im.getData();
        PixelDrawing drawing = new PixelDrawing( bounds );
        for ( int ix = 0; ix < xdim; ix++ ) {
            for ( int iy = 0; iy < ydim; iy++ ) {
                int alpha = raster.getSample( ix, iy, 3 );
                assert alpha == 0 || alpha == 255;
                if ( alpha > 0 ) {
                    drawing.addPixelUnchecked( ix + bounds.x, iy + bounds.y );
                }
            }
        }

        /* Return the result as a PixerFactory. */
        return drawing;
    }

    @Override
    public String toString() {
        return getName();
    }

    /**
     * Returns an array of instances which can render 1-dimensional
     * (vertical) errors.
     *
     * @return  selection of shapes
     */
    public static MultiPointShape[] getOptionsError1d() {
        return OPTIONS_1D.clone();
    }

    /**
     * Returns an array of instances which can render 2-dimensional errors.
     *
     * @return  selection of shapes
     */
    public static MultiPointShape[] getOptionsError2d() {
        return OPTIONS_2D.clone();
    }

    /**
     * Returns an array of instances which can render 3-dimensional errors.
     *
     * @return  selection of shapes
     */
    public static MultiPointShape[] getOptionsError3d() {
        return OPTIONS_3D.clone();
    }

    /**
     * Returns an array of instances which is suitable for 2d
     * ellipse-like applications.
     *
     * @return  selection of shapes
     */
    public static MultiPointShape[] getOptionsEllipse() {
        return OPTIONS_ELLIPSE.clone();
    }

    /**
     * Returns an array of instances which is suitable for
     * vector-like applications.
     *
     * @return  selection of shapes
     */
    public static MultiPointShape[] getOptionsVector() {
        return OPTIONS_VECTOR.clone();
    }

    /**
     * This is supposed to return the dimensions of the target plotting area.
     * It is used to assess whether lines etc are (much) too long to make an
     * attempt at plotting them, since attempting to plot lines that
     * are several kilometers in length can make the graphics system grind
     * to a halt.  So the returned value is not critical, and erring on
     * the large side is preferred.
     *
     * @param  g  graphics context
     * @return   approximate size of visible graphics canvas
     */
    private static Dimension getApproxGraphicsSize( Graphics2D g ) {

        /* The right way to do this looks like to use the
         * GraphicsConfiguration object associated with the graphics context.
         * However, in the headless case, this sometimes has a dummy size
         * (width=height=1).  That looks like a bug, but I'm not sure.
         * In any case, try to work round it; if the size looks silly,
         * return a spurious large value instead.
         * This isn't great, since making it too large may result
         * in poor performance, but if it's too small the graphics
         * may come out a bit wrong.  Don't know what else to do though. */
        Dimension size = g.getDeviceConfiguration().getBounds().getSize();
        return size.width > 1 && size.height > 1
             ? size
             : new Dimension( DUMMY_SIZE, DUMMY_SIZE );
    }

    /**
     * MultiPointShape partial implementation that generates PixerFactory
     * using a PixelDrawing.  This is likely to be more efficient than
     * the default implementation and generally makes sense.
     */
    private static abstract class DrawingShape extends MultiPointShape {

        /**
         * Constructor.
         *
         * @param  name  shape name
         */
        DrawingShape( String name ) {
            super( name );
        }

        @Override
        public PixerFactory createPixerFactory( Rectangle clip, int x, int y,
                                                int[] xoffs, int[] yoffs ) {
            Rectangle bounds = getBounds( x, y, xoffs, yoffs );
            int xlo = Math.max( clip.x, bounds.x );
            int xhi = Math.min( clip.x + clip.width, bounds.x + bounds.width );
            int dw = xhi - xlo;
            if ( dw > 0 ) {
                int ylo = Math.max( clip.y, bounds.y );
                int yhi = Math.min( clip.y + clip.height,
                                    bounds.y + bounds.height );
                int dh = yhi - ylo;
                if ( dh > 0 ) {
                    PixelDrawing drawing = new PixelDrawing( xlo, ylo, dw, dh );
                    drawShape( drawing, x, y, xoffs, yoffs );
                    return drawing;
                }
            }
            return null;
        }

        /**
         * Draws this shape onto a given drawing, whose bounds have
         * been set appropriately.
         *
         * @param  drawing   target for graphics output
         * @param  x  data point X coordinate
         * @param  y  data point Y coordinate
         * @param  xoffs  X coordinates of error bar limit offsets from (x,y)
         * @param  yoffs  Y coordinates of error bar limit offsets from (x,y)
         */
        abstract void drawShape( PixelDrawing drawing,
                                 int x, int y, int[] xoffs, int[] yoffs );
    }

    /**
     * Null shape.  It draws nothing.
     */
    private static class Blank extends MultiPointShape {

        /**
         * Constructor.
         *
         * @param  name   shape name
         */
        Blank( String name ) {
            super( name );
        }

        public boolean supportsDimensionality( int ndim ) {
            return true;
        }

        public Icon getLegendIcon() {
            return IconUtils.emptyIcon( 0, 0 );
        }

        public Icon getLegendIcon( ErrorMode[] modes, int width, int height,
                                   int xpad, int ypad ) {
            return IconUtils.emptyIcon( width, height );
        }

        public void drawShape( Graphics g, int x, int y,
                               int[] xoffs, int[] yoffs ) {
        }

        public PixerFactory createPixerFactory( Rectangle clip, int x, int y,
                                                int[] xoffs, int[] yoffs ) {
            return null;
        }

        public Rectangle getBounds( int x, int y, int[] xoffs, int[] yoffs ) {
            return new Rectangle( x, y, 0, 0 );
        }
    }

    /**
     * Shape which draws an (optional) line from the data point
     * to the given offset, and an (optional) cap normal to that line
     * at its furthest extent.  Works for any dimensionality.
     */
    private static class CappedLine extends DrawingShape {

        private final boolean hasLine_;
        private final Capper capper_;
        private final Icon icon_;

        /**
         * Constructor.
         *
         * @param  name   shape name
         * @param  lines   true iff you want error lines drawn
         * @param  capper  if non-null causes caps to be drawn at end of lines
         */
        CappedLine( String name, boolean hasLine, Capper capper ) {
            super( name );
            hasLine_ = hasLine;
            capper_ = capper;
            icon_ = new MultiPointIcon( this, 2 );
        }

        public boolean supportsDimensionality( int ndim ) {
            return true;
        }

        public Rectangle getBounds( int x, int y, int[] xoffs, int[] yoffs ) {
            int np = xoffs.length;
            boolean empty = true;
            Rectangle box = new Rectangle();
            for ( int ip = 0; ip < np; ip++ ) {
                int xoff = xoffs[ ip ];
                int yoff = yoffs[ ip ];
                if ( xoff != 0 || yoff != 0 ) {
                    empty = false;
                    box.add( xoff, yoff );
                    if ( capper_ != null ) {
                        capper_.extendBounds( box, xoff, yoff );
                    }
                }
            }
            if ( ! empty ) {
                box.width++;
                box.height++;
            }
            box.x += x;
            box.y += y;
            return box;
        }

        public Icon getLegendIcon() {
            return icon_;
        }

        public Icon getLegendIcon( ErrorMode[] modes, int width, int height,
                                   int xpad, int ypad ) {
            return new MultiPointIcon( this, modes, width, height, xpad, ypad );
        }
                                       

        public void drawShape( Graphics g,
                               int x, int y, int[] xoffs, int[] yoffs ) {
            drawShape( g, x, y, xoffs, yoffs, hasLine_, capper_, false );
        }

 
        public void drawShape( PixelDrawing drawing,
                               int x, int y, int[] xoffs, int[] yoffs ) {
            int np = xoffs.length;
            for ( int ip = 0; ip < np; ip++ ) {
                int xoff = xoffs[ ip ];
                int yoff = yoffs[ ip ];
                if ( xoff != 0 || yoff != 0 ) {
                    if ( hasLine_ ) {
                        drawing.drawLine( x, y, x + xoff, y + yoff );
                    }
                    if ( capper_ != null ) {
                        capper_.drawCap( drawing, x, y, xoff, yoff );
                    }
                }
            }
        }

        /**
         * Does the work for drawing a CappedLine to a graphics context.
         *
         * @param  g  graphics context
         * @param  x  data point X coordinate
         * @param  y  data point Y coordinate
         * @param  xoffs  X coordinates of error bar limit offsets from (x,y)
         * @param  yoffs  Y coordinates of error bar limit offsets from (x,y)
         * @param  hasLine  whether to draw lines
         * @param  capper   cap drawing object, if any
         * @param  willCover  true if the ends of the radial lines will
         *                    subsequently be covered by more drawing
         *                    (affects line capping)
         */
        private static void drawShape( Graphics g, int x, int y,
                                      int[] xoffs, int[] yoffs,
                                      boolean hasLine, Capper capper,
                                      boolean willCover ) {
            Graphics2D g2 = (Graphics2D) g;
            Stroke oldStroke = g2.getStroke();
            g2.setStroke( capper != null || willCover ? CAP_BUTT : CAP_ROUND );
            Dimension size = getApproxGraphicsSize( g2 );
            int xmax = size.width;
            int ymax = size.height;
            int np = xoffs.length;
            for ( int ip = 0; ip < np; ip++ ) {
                int xoff = xoffs[ ip ];
                int yoff = yoffs[ ip ];

                /* Only do drawing for a non-blank line. */
                if ( xoff != 0 || yoff != 0 ) {

                    /* If the end coordinate is definitely outside the graphics
                     * bounds, shrink the line to something about the right
                     * size.
                     * This is here to defend against the case in which the
                     * error bound is way off the screen - trying to draw a
                     * kilometre long line can have adverse effects on some
                     * graphics systems. */
                    boolean xlo = xoff < - xmax;
                    boolean xhi = xoff > + xmax;
                    boolean ylo = yoff < - ymax;
                    boolean yhi = yoff > + ymax;
                    boolean clipped = xlo || xhi || ylo || yhi;
                    if ( clipped ) {
                        if ( xlo && yoff == 0 ) {
                            xoff = - xmax;
                        }
                        else if ( xhi && yoff == 0 ) {
                            xoff = + xmax;
                        }
                        else if ( ylo && xoff == 0 ) {
                            yoff = - ymax;
                        }
                        else if ( yhi && xoff == 0 ) {
                            yoff = + ymax;
                        }
                        else {
                            double s2 = (double) ( xmax * xmax + ymax * ymax )
                                      / (double) ( xoff * xoff + yoff * yoff );
                            double shrink = Math.sqrt( s2 );
                            xoff = (int) Math.ceil( shrink * xoff );
                            yoff = (int) Math.ceil( shrink * yoff );
                        }
                    }

                    /* Draw line if required. */
                    if ( hasLine ) {
                        g.drawLine( x, y, x + xoff, y + yoff );
                    }

                     /* Draw cap if required. */
                    if ( capper != null && ! clipped ) {
                        g2.setStroke( CAP_ROUND );

                        /* For rectilinear offsets, draw the cap manually. */
                        if ( xoff == 0 ) {
                            capper.drawCapY( g2, x, y, yoff );
                        }
                        else if ( yoff == 0 ) {
                            capper.drawCapX( g2, x, y, xoff );
                        }

                        /* For more general offsets, transform the graphics
                         * context so that we can draw the cap along an axis.
                         * This is better than calculating the position in
                         * the original orientation because that would require
                         * integer rounding (at least in antialiased contexts
                         * the difference may be visible). */
                        else {
                            AffineTransform oldTransform = g2.getTransform();
                            g2.translate( x, y );
                            g2.rotate( Math.atan2( yoff, xoff ) );
                            double l2 = xoff * xoff + yoff * yoff;
                            int leng = (int) Math.round( Math.sqrt( l2 ) );
                            capper.drawCapX( g2, 0, 0, leng );
                            g2.setTransform( oldTransform );
                        }
                    }
                }
            }
            g2.setStroke( oldStroke );
        }
    }

    /**
     * MultiPointShape which draws an isosceles triangle with a fixed-length
     * base centered at the data point and a point at the offset point.
     * Works for any dimensionality.
     */
    private static class Dart extends DrawingShape {

        private final boolean isFill_;
        private final int basepix_;
        private final Icon legend_;
        private final int[] vys_;
        private final Stroke stroke_;

        /**
         * Constructor.
         *
         * @param  name  shape name
         * @param  isFill  true for a filled triangle, false for open
         * @param  basepix  half-length of triangle base in pixels
         */
        public Dart( String name, boolean isFill, int basepix ) {
            super( name );
            isFill_ = isFill;
            basepix_ = basepix;
            legend_ = new MultiPointIcon( this, 2 );
            vys_ = new int[] { basepix_, 0, -basepix_ };
            stroke_ = new BasicStroke( 1, BasicStroke.CAP_ROUND,
                                       BasicStroke.JOIN_ROUND );
        }

        public boolean supportsDimensionality( int ndim ) {
            return ndim > 0;
        }

        public Icon getLegendIcon() {
            return legend_;
        }

        public Icon getLegendIcon( ErrorMode[] modes, int width, int height,
                                   int xpad, int ypad ) {
            return new MultiPointIcon( this, modes, width, height, xpad, ypad );
        }

        public Rectangle getBounds( int x, int y, int[] xoffs, int[] yoffs ) {
            Rectangle box = new Rectangle();
            int np = xoffs.length;
            for ( int ip = 0; ip < np; ip++ ) {
                int xoff = xoffs[ ip ];
                int yoff = yoffs[ ip ];
                if ( xoff != 0 || yoff != 0 ) {
                    box.add( xoff, yoff );
                    Point bp = getBaseVertex( xoff, yoff );
                    box.add( bp.x, bp.y );
                    box.add( -bp.x, -bp.y );
                    box.width++;
                    box.height++;
                }
            }
            box.x += x;
            box.y += y;
            return box;
        }

        public void drawShape( PixelDrawing drawing,
                               int x, int y, int[] xoffs, int[] yoffs ) {
            int np = xoffs.length;
            for ( int ip = 0; ip < np; ip++ ) {
                int xoff = xoffs[ ip ];
                int yoff = yoffs[ ip ];
                if ( xoff != 0 || yoff != 0 ) {
                    Point bp = getBaseVertex( xoff, yoff );
                    int x1 = x + xoff;
                    int y1 = y + yoff;
                    int x2 = x + bp.x;
                    int y2 = y + bp.y;
                    int x3 = x - bp.x;
                    int y3 = y - bp.y;
                    if ( isFill_ ) {
                        drawing.fill( new Polygon( new int[] { x1, x2, x3 },
                                                   new int[] { y1, y2, y3 },
                                                   3 ) );
                    }
                    else {
                        drawing.drawLine( x1, y1, x2, y2 );
                        drawing.drawLine( x2, y2, x3, y3 );
                        drawing.drawLine( x3, y3, x1, y1 );
                    }
                }
            }
        }

        public void drawShape( Graphics g, int x, int y,
                               int[] xoffs, int[] yoffs ) {
            Rectangle clip = g.getClipBounds();
            Graphics2D g2 = (Graphics2D) g;
            Stroke stroke0 = g2.getStroke();
            g2.setStroke( stroke_ );
            Dimension size = getApproxGraphicsSize( g2 );
            double dmax = Math.max( size.width, size.height );
            int np = xoffs.length;
            for ( int ip = 0; ip < np; ip++ ) {
                double dx = xoffs[ ip ];
                double dy = yoffs[ ip ];
                if ( dx != 0 || dy != 0 ) {
                    double dleng = Math.min( Math.hypot( dx, dy ), dmax );
                    AffineTransform trans0 = g2.getTransform();
                    g2.translate( x, y );
                    g2.rotate( Math.atan2( dy, dx ) );
                    int[] xs = new int[] { 0, (int) Math.round( dleng ), 0 };
                    if ( isFill_ ) {
                        g2.fillPolygon( xs, vys_, 3 );
                    }
                    else {
                        g2.drawPolygon( xs, vys_, 3 );
                    }
                    g2.setTransform( trans0 );
                }
            }
            g2.setStroke( stroke0 );
        }

        /**
         * Returns the coordinates of one vertex on the base of the triangle,
         * given the coordinates of its apex.  The base line is considered
         * to be centered on the origin.  The coordinates of the other
         * vertex is determined by negating both the returned X and Y
         * coordinates.
         *
         * @param  xoff  apex X coordinate
         * @param  yoff  apex Y coordinate
         * @return   coordinates of one base vertex
         */
        private Point getBaseVertex( int xoff, int yoff ) {
            double dx = xoff;
            double dy = yoff;
            double dscale = 1.0 / Math.hypot( dx, dy );
            return new Point( - (int) Math.round( basepix_ * dy * dscale ),
                              + (int) Math.round( basepix_ * dx * dscale ) );
        }
    }

    /**
     * MultiPointShape which renders an isosceles triangle, centered
     * on the data point, with a variable-length base.
     * Used like an ellipse/rectangle/oblong (2d only).
     */
    private static class Triangle extends DrawingShape {

        private final boolean isFill_;
        private final Icon legend_;
        private final Stroke stroke_;

        /**
         * Constructor.
         *
         * @param  name  shape name
         * @param  isFill  true for a filled triangle, false for open
         */
        public Triangle( String name, boolean isFill ) {
            super( name );
            isFill_ = isFill;
            legend_ = new MultiPointIcon( this, 2 );
            stroke_ = new BasicStroke( 1, BasicStroke.CAP_ROUND,
                                       BasicStroke.JOIN_ROUND );
        }

        public boolean supportsDimensionality( int ndim ) {
            return ndim == 2;
        }

        public Icon getLegendIcon() {
            return legend_;
        }

        public Icon getLegendIcon( ErrorMode[] modes, int width, int height,
                                   int xpad, int ypad ) {
            return new MultiPointIcon( this, modes, width, height, xpad, ypad );
        }


        public Rectangle getBounds( int x, int y, int[] xoffs, int[] yoffs ) {
            return getTriangle( x, y, xoffs, yoffs ).getBounds();
        }

        public void drawShape( PixelDrawing drawing,
                               int x, int y, int[] xoffs, int[] yoffs ) {
            Polygon triangle = getTriangle( x, y, xoffs, yoffs );
            if ( isFill_ ) {
                drawing.fill( triangle );
            }
            else {
                int[] xs = triangle.xpoints;
                int[] ys = triangle.ypoints;
                drawing.drawLine( xs[ 1 ], ys[ 1 ], xs[ 2 ], ys[ 2 ] );
                drawing.drawLine( xs[ 2 ], ys[ 2 ], xs[ 0 ], ys[ 0 ] );
                drawing.drawLine( xs[ 0 ], ys[ 0 ], xs[ 1 ], ys[ 1 ] );
            }
        }

        public void drawShape( Graphics g, int x, int y,
                               int[] xoffs, int[] yoffs ) {
            Dimension size = getApproxGraphicsSize( (Graphics2D) g );
            int dmax = Math.max( size.width, size.height );
            boolean ok = true;
            for ( int ip = 0; ip < 4 && ok; ip++ ) {
                ok = ok && Math.abs( xoffs[ ip ] ) < dmax
                        && Math.abs( yoffs[ ip ] ) < dmax;
            }
            if ( ! ok ) {
                xoffs = xoffs.clone();
                yoffs = yoffs.clone();
                for ( int ip = 0; ip < 4; ip++ ) {
                    xoffs[ ip ] =
                        Math.min( dmax, Math.max( -dmax, xoffs[ ip ] ) );
                    yoffs[ ip ] =
                        Math.min( dmax, Math.max( -dmax, yoffs[ ip ] ) );
                }
            }
            Polygon triangle = getTriangle( x, y, xoffs, yoffs );
            if ( isFill_ ) {
                g.fillPolygon( triangle );
            }
            else {
                g.drawPolygon( triangle );
            }
        }

        /**
         * Returns a polygon representing the required triangle.
         *
         * @param   x  data point X coordinate
         * @param   y  data point Y coordinate
         * @param  xoffs  offset point X coordinates (xlo, ylo, xhi, yhi)
         * @param  yoffs  offset point Y coordinates (xlo, ylo, xhi, yhi)
         */
        private Polygon getTriangle( int x, int y, int[] xoffs, int[] yoffs ) {
            int[] xs = new int[ 3 ];
            int[] ys = new int[ 3 ];
            xs[ 0 ] = x + xoffs[ 1 ];
            ys[ 0 ] = y + yoffs[ 1 ];
            xs[ 1 ] = x + xoffs[ 0 ] + xoffs[ 2 ];
            ys[ 1 ] = y + yoffs[ 0 ] + yoffs[ 2 ];
            xs[ 2 ] = x + xoffs[ 0 ] + xoffs[ 3 ];
            ys[ 2 ] = y + yoffs[ 0 ] + yoffs[ 3 ];
            return new Polygon( xs, ys, 3 );
        }
    }

    /**
     * Defines how caps are drawn on the end of error bar-type lines.
     */
    private interface Capper {

        /**
         * Draws a cap on a horizontal error bar in a graphics context.
         *
         * @param   g  graphics context
         * @param   x  X position of data point
         * @param   y  Y position of data point
         * @param   xoff  X offset of the end of the error bar
         */
        void drawCapX( Graphics g, int x, int y, int xoff );
    
        /**
         * Draws a cap on a vertical error bar in a graphics context.
         * 
         * @param   g  graphics context
         * @param   x  X position of data point
         * @param   y  Y position of data point
         * @param   yoff  Y offset of the end of the error bar
         */
        void drawCapY( Graphics g, int x, int y, int yoff );

        /**
         * Draws a cap on an error bar in a pixel-mapped drawing.
         *
         * @param  drawing  pixel map
         * @param  x  X position of data point
         * @param  y  Y position of data point
         * @param  xoff  X offset of the end of the error bar
         * @param  yoff  Y offset of the end of the error bar
         */
        void drawCap( PixelDrawing drawing, int x, int y, int xoff, int yoff );

        /**
         * Notes the bounds of the caps of an error bar.
         * The supplied <code>bounds</code> rectangle is extended to include
         * any drawing associated with capping the given error offset
         * (the data point is assumed to be at the origin).
         * It is permissible to extend the bounds too far.
         *
         * @param   bounds  bounds rectangle, to be increased in size
         *          as necessary
         * @param   xoff  X offset of the end of the error bar (from origin)
         * @param   yoff  Y offset of the end of the error bar (from origin)
         */
        void extendBounds( Rectangle bounds, int xoff, int yoff );
    }

    /**
     * Capper implementation which simply draws a perpendicular bar.
     */
    private static class BarCapper implements Capper {

        private final int capsize_;

        /**
         * Constructor.
         *
         * @param   capsize  number of pixels in each direction that
         *          bar is drawn
         */
        public BarCapper( int capsize ) {
            capsize_ = capsize;
        }

        public void drawCapX( Graphics g, int x, int y, int xoff ) {
            g.drawLine( x + xoff, y - capsize_, x + xoff, y + capsize_ );
        }

        public void drawCapY( Graphics g, int x, int y, int yoff ) {
            g.drawLine( x - capsize_, y + yoff, x + capsize_, y + yoff );
        }

        public void drawCap( PixelDrawing drawing,
                             int x, int y, int xoff, int yoff ) {
            if ( xoff == 0 ) {
                drawing.drawLine( x - capsize_, y + yoff,
                                  x + capsize_, y + yoff );
            }
            else if ( yoff == 0 ) {
                drawing.drawLine( x + xoff, y - capsize_,
                                  x + xoff, y + capsize_ );
            }
            else {
                int x0 = x + xoff;
                int y0 = y + yoff;
                double r1 = Math.sqrt( xoff * xoff + yoff * yoff );
                double capfact = capsize_ / r1;
                int x1 = (int) Math.round( - capfact * yoff );
                int y1 = (int) Math.round( + capfact * xoff );
                drawing.drawLine( x0 - x1, y0 - y1,
                                  x0 + x1, y0 + y1 );
            }
        }

        public void extendBounds( Rectangle bounds, int xoff, int yoff ) {
            int cs = capsize_ + 1;
            if ( xoff == 0 ) {
                bounds.add( - cs, yoff );
                bounds.add( + cs, yoff );
            }
            else if ( yoff == 0 ) {
                bounds.add( xoff, - cs );
                bounds.add( xoff, + cs );
            }
            else {
                bounds.add( xoff - cs, yoff - cs );
                bounds.add( xoff - cs, yoff + cs );
                bounds.add( xoff + cs, yoff - cs );
                bounds.add( xoff + cs, yoff + cs );
            }
        }
    }

    /**
     * Capper implementation which draws an outward-pointing open arrow.
     */
    private static class ArrowCapper implements Capper {

        private final int capsize_;
        private final int[] xs_;
        private final int[] ys_;

        /**
         * Constructor.
         *
         * @param   capsize  number of pixels in each direction
         */
        ArrowCapper( int capsize ) {
            capsize_ = capsize;
            xs_ = new int[ 3 ];
            ys_ = new int[ 3 ];
        }

        public void drawCapX( Graphics g, int x, int y, int xoff ) {
            int sign = xoff > 0 ? +1 : -1;
            int size = Math.min( capsize_, sign * xoff );
            int xstart = x + xoff - sign * size;
            xs_[ 0 ] = xstart;
            ys_[ 0 ] = y - size;
            xs_[ 1 ] = x + xoff;
            ys_[ 1 ] = y;
            xs_[ 2 ] = xstart;
            ys_[ 2 ] = y + size;
            g.drawPolyline( xs_, ys_, 3 );
        }

        public void drawCapY( Graphics g, int x, int y, int yoff ) {
            int sign = yoff > 0 ? +1 : -1;
            int size = Math.min( capsize_, sign * yoff );
            int ystart = y + yoff - sign * size;
            xs_[ 0 ] = x - size;
            ys_[ 0 ] = ystart;
            xs_[ 1 ] = x;
            ys_[ 1 ] = y + yoff;
            xs_[ 2 ] = x + size;
            ys_[ 2 ] = ystart;
            g.drawPolyline( xs_, ys_, 3 );
        }

        public void drawCap( PixelDrawing drawing,
                             int x, int y, int xoff, int yoff ) {
            if ( xoff == 0 ) {
                int sign = yoff > 0 ? +1 : -1;
                int size = Math.min( capsize_, sign * yoff );
                int ystart = y + yoff - sign * size;
                drawing.drawLine( x, y + yoff, x - size, ystart );
                drawing.drawLine( x, y + yoff, x + size, ystart );
            }
            else if ( yoff == 0 ) {
                int sign = xoff > 0 ? +1 : -1;
                int size = Math.min( capsize_, sign * xoff );
                int xstart = x + xoff - sign * size;
                drawing.drawLine( x + xoff, y, xstart, y - size );
                drawing.drawLine( x + xoff, y, xstart, y + size );
            }
            else {
                double r1 = Math.sqrt( xoff * xoff + yoff * yoff );
                double size = Math.min( capsize_, r1 );
                double capfact = size / r1;
                int ax = xoff + (int) Math.round( capfact * ( - xoff + yoff ) );
                int ay = yoff + (int) Math.round( capfact * ( - xoff - yoff ) );
                int bx = xoff + (int) Math.round( capfact * ( - xoff - yoff ) );
                int by = yoff + (int) Math.round( capfact * ( + xoff - yoff ) );
                drawing.drawLine( x + xoff, y + yoff, x + ax, y + ay );
                drawing.drawLine( x + xoff, y + yoff, x + bx, y + by );
            }
        }

        public void extendBounds( Rectangle bounds, int xoff, int yoff ) {
            int cs = (int) Math.ceil( 1.5 * capsize_ );
            bounds.add( xoff + cs, yoff );
            bounds.add( xoff - cs, yoff );
            bounds.add( xoff, yoff + cs );
            bounds.add( xoff, yoff - cs );
        }
    }

    /**
     * Generic abstract shape for cases in which the drawn object is
     * effectively a rectangle of some sort.  Concrete subclasses
     * must implement {@link #drawOblong} to mark the space as appropriate.
     * Only works properly for two-dimensional errors.
     */
    private static abstract class Oblong extends DrawingShape {

        private final Icon legend_;
        private final boolean withLines_;

        /**
         * Constructor.
         *
         * @param  name   shape name
         * @param  withLines  true iff you want a crosshair drawn as well as
         *         the basic representation of this shape
         */
        Oblong( String name, boolean withLines ) {
            super( name );
            withLines_ = withLines;
            legend_ = new MultiPointIcon( this, 2 );
        }

        public boolean supportsDimensionality( int ndim ) {
            return ndim == 2;
        }

        public Icon getLegendIcon() {
            return legend_;
        }

        public Icon getLegendIcon( ErrorMode[] modes, int width, int height,
                                   int xpad, int ypad ) {
            return new MultiPointIcon( this, modes, width, height, xpad, ypad );
        }

        public void drawShape( Graphics g, int x, int y,
                               int[] xoffs, int[] yoffs ) {
            Graphics2D g2 = (Graphics2D) g;
            int noff = xoffs.length;

            /* Restrict the offsets to something sensible, to prevent the
             * graphics system attempting to fill an ellipse with a
             * kilometre semi-major axis.  This may result in some
             * distortions for ellipses - too bad. */
            Dimension size = getApproxGraphicsSize( g2 );
            int maxcoord = Math.max( size.width, size.height );
            boolean clipped = false;
            for ( int ioff = 0; ioff < noff && ! clipped; ioff++ ) {
                int xoff = xoffs[ ioff ];
                int yoff = yoffs[ ioff ];
                clipped = clipped || xoff < - maxcoord || xoff > + maxcoord
                                  || yoff < - maxcoord || yoff > + maxcoord;
            }
            if ( clipped ) {
                int[] xo = new int[ noff ];
                int[] yo = new int[ noff ];
                for ( int ioff = 0; ioff < noff; ioff++ ) {
                    xo[ ioff ] =
                        Math.max( - maxcoord,
                                  Math.min( + maxcoord, xoffs[ ioff ] ) );
                    yo[ ioff ] =
                        Math.max( - maxcoord,
                                  Math.min( + maxcoord, yoffs[ ioff ] ) );
                }
                xoffs = xo;
                yoffs = yo;
            }

            /* If there are only 1-dimensional bounds, just draw a line.
             * Actually, we don't claim to support dimensionality other than 2
             * here, so this is probably never used. */
            if ( noff == 2 ) {
                g.drawLine( x + xoffs[ 0 ], y + yoffs[ 0 ],
                            x + xoffs[ 1 ], y + yoffs[ 1 ] );
            }

            /* Otherwise we better have two dimensions. */
            else if ( noff != 4 || yoffs.length != 4 ||
                      ! ( g instanceof Graphics2D ) ) {
                return;
            }

            /* If the X and Y offsets are aligned along X and Y axes we
             * can do it easily. */
            else if ( yoffs[ 0 ] == 0 && yoffs[ 1 ] == 0 &&
                      xoffs[ 2 ] == 0 && xoffs[ 3 ] == 0 ) {
                int xlo = Math.min( xoffs[ 0 ], xoffs[ 1 ] );
                int xhi = Math.max( xoffs[ 0 ], xoffs[ 1 ] );
                int ylo = Math.min( yoffs[ 2 ], yoffs[ 3 ] );
                int yhi = Math.max( yoffs[ 2 ], yoffs[ 3 ] );
                int width = xhi - xlo;
                int height = yhi - ylo;
                if ( width > 0 || height > 0 ) {
                    drawOblong( g, x + xlo, y + ylo, width, height );
                }
            }

            /* Otherwise transform the space so that the error bounds are
             * contained in a rectangle aligned along the axes. */
            else {
                double dx1 = xoffs[ 1 ] - xoffs[ 0 ];
                double dy1 = yoffs[ 1 ] - yoffs[ 0 ];
                double dx2 = xoffs[ 3 ] - xoffs[ 2 ];
                double dy2 = yoffs[ 3 ] - yoffs[ 2 ];
                double width = Math.sqrt( dx1 * dx1 + dy1 * dy1 );
                double height = Math.sqrt( dx2 * dx2 + dy2 * dy2 );
                double[] m1 = new double[] {
                    width, 0,      0,
                    0,     height, 0,
                    1,     1,      1,
                };
                if ( Matrices.det( m1 ) != 0 ) {
                    int[] xo = xoffs;
                    int[] yo = yoffs;
                    double[] m2 = new double[] {
                        x + xo[1] + xo[2], x + xo[0] + xo[3], x + xo[0] + xo[2],
                        y + yo[1] + yo[2], y + yo[0] + yo[3], y + yo[0] + yo[2],
                        1,                 1,                 1,
                    };
                    double[] m3 = Matrices.mmMult( m2, Matrices.invert( m1 ) );
                    AffineTransform trans =
                        new AffineTransform( m3[ 0 ], m3[ 3 ],
                                             m3[ 1 ], m3[ 4 ],
                                             m3[ 2 ], m3[ 5 ] );
                    if ( trans.getDeterminant() != 0 ) {
                        AffineTransform oldTrans = g2.getTransform();
                        g2.transform( trans );
                        drawOblong( g2, 0, 0, (int) Math.round( width ),
                                    (int) Math.round( height ) );
                        g2.setTransform( oldTrans );
                    }
                }
            }

            /* Draw crosshair if required. */
            if ( withLines_ ) {
                CappedLine.drawShape( g, x, y, xoffs, yoffs, true, null, true );
            }
        }

        public Rectangle getBounds( int x, int y, int[] xoffs, int[] yoffs ) {
            int xmin = 0;
            int xmax = 0;
            int ymin = 0;
            int ymax = 0;
            int rmax = 0;
            int np = xoffs.length;
            for ( int ip = 0; ip < np; ip++ ) {
                int xoff = xoffs[ ip ];
                int yoff = yoffs[ ip ];
                if ( xoff != 0 || yoff != 0 ) {
                    if ( xoff == 0 ) {
                        ymin = Math.min( ymin, yoff );
                        ymax = Math.max( ymax, yoff );
                    }
                    else if ( yoff == 0 ) {
                        xmin = Math.min( xmin, xoff );
                        xmax = Math.max( xmax, xoff );
                    }
                    else {
                        rmax = Math.max( rmax,
                                         Math.abs( xoff ) + Math.abs( yoff ) );
                    }
                }
            }
            if ( rmax > 0 ) {
                xmin = Math.min( xmin, - rmax );
                xmax = Math.max( xmax, + rmax );
                ymin = Math.min( ymin, - rmax );
                ymax = Math.max( ymax, + rmax );
            }
            return new Rectangle( x + xmin, y + ymin,
                                  xmax - xmin + 1, ymax - ymin + 1 );
        }

        /**
         * Does the actual drawing of the error region.
         * The region covers the range (x..x+width, y..y+height).
         *
         * @param   g  graphics context
         * @param   x  X coordinate of origin
         * @param   y  Y coordinate of origin
         * @param   width   X extent of region
         * @param   height  Y extent of region
         */
        protected abstract void drawOblong( Graphics g, int x, int y,
                                            int width, int height );
    }

    /**
     * Oblong using an open ellipse.
     */
    private static class OpenEllipse extends Oblong {

        private final boolean withLines_;

        /**
         * Constructor.
         *
         * @param  name   shape name
         * @param  withLines  true iff you want a crosshair drawn as well as
         *         the ellipse
         */
        public OpenEllipse( String name, boolean withLines ) {
            super( name, withLines );
            withLines_ = withLines;
        }

        protected void drawOblong( Graphics g, int x, int y,
                                   int width, int height ) {
            g.drawOval( x, y, width, height );
        }

        public void drawShape( PixelDrawing drawing,
                               int x, int y, int[] xoffs, int[] yoffs ) {
            if ( xoffs.length != 4 || yoffs.length != 4 ) {
                return;
            }
            else if ( yoffs[ 0 ] == 0 && yoffs[ 1 ] == 0 &&
                      xoffs[ 2 ] == 0 && xoffs[ 3 ] == 0 ) {
                int xlo = x + Math.min( xoffs[ 0 ], xoffs[ 1 ] );
                int xhi = x + Math.max( xoffs[ 0 ], xoffs[ 1 ] );
                int ylo = y + Math.min( yoffs[ 2 ], yoffs[ 3 ] );
                int yhi = y + Math.max( yoffs[ 2 ], yoffs[ 3 ] );
                int width = xhi - xlo;
                int height = yhi - ylo;
                drawing.drawOval( xlo, ylo, width, height );
                if ( withLines_ ) {
                    for ( int i = 0; i < 4; i++ ) {
                        drawing.drawLine( x, y,
                                          x + xoffs[ i ], y + yoffs[ i ] );
                    }
                }
            }
            else {
                int ax = ( xoffs[ 1 ] - xoffs[ 0 ] ) / 2;
                int ay = ( yoffs[ 1 ] - yoffs[ 0 ] ) / 2;
                int bx = ( xoffs[ 3 ] - xoffs[ 2 ] ) / 2;
                int by = ( yoffs[ 3 ] - yoffs[ 2 ] ) / 2;
                int x0 = x + Math.round( ( xoffs[ 0 ] + xoffs[ 1 ]
                                         + xoffs[ 2 ] + xoffs[ 3 ] ) / 2f );
                int y0 = y + Math.round( ( yoffs[ 0 ] + yoffs[ 1 ]
                                         + yoffs[ 2 ] + yoffs[ 3 ] ) / 2f );
                if ( drawing != null ) {
                    drawing.drawEllipse( x0, y0, ax, ay, bx, by );
                    if ( withLines_ ) {
                        for ( int i = 0; i < 4; i++ ) {
                            drawing.drawLine( x, y,
                                              x + xoffs[ i ], y + yoffs[ i ] );
                        }
                    }
                }
            }
        }
    }

    /**
     * Oblong using a filled ellipse.
     */
    private static class FilledEllipse extends Oblong {

        /**
         * Constructor.
         *
         * @param  name   shape name
         */
        public FilledEllipse( String name ) {
            super( name, false );
        }

        protected void drawOblong( Graphics g, int x, int y,
                                   int width, int height ) {
            g.fillOval( x, y, width, height );
        }

        public void drawShape( PixelDrawing drawing,
                               int x, int y, int[] xoffs, int[] yoffs ) {
            if ( xoffs.length != 4 || yoffs.length != 4 ) {
                return;
            }
            else if ( yoffs[ 0 ] == 0 && yoffs[ 1 ] == 0 &&
                      xoffs[ 2 ] == 0 && xoffs[ 3 ] == 0 ) {
                int xlo = x + Math.min( xoffs[ 0 ], xoffs[ 1 ] );
                int xhi = x + Math.max( xoffs[ 0 ], xoffs[ 1 ] );
                int ylo = y + Math.min( yoffs[ 2 ], yoffs[ 3 ] );
                int yhi = y + Math.max( yoffs[ 2 ], yoffs[ 3 ] );
                int width = xhi - xlo;
                int height = yhi - ylo;
                drawing.fillOval( xlo, ylo, width, height );
            }
            else {
                int ax = ( xoffs[ 1 ] - xoffs[ 0 ] ) / 2;
                int ay = ( yoffs[ 1 ] - yoffs[ 0 ] ) / 2;
                int bx = ( xoffs[ 3 ] - xoffs[ 2 ] ) / 2;
                int by = ( yoffs[ 3 ] - yoffs[ 2 ] ) / 2;
                int x0 = x + Math.round( ( xoffs[ 0 ] + xoffs[ 1 ]
                                         + xoffs[ 2 ] + xoffs[ 3 ] ) / 2f );
                int y0 = y + Math.round( ( yoffs[ 0 ] + yoffs[ 1 ]
                                         + yoffs[ 2 ] + yoffs[ 3 ] ) / 2f );
                drawing.fillEllipse( x0, y0, ax, ay, bx, by );
            }
        }
    }

    /**
     * Oblong using an open rectangle.
     */
    private static class OpenRectangle extends Oblong {

        private final boolean withLines_;

        /**
         * Constructor.
         *
         * @param  name   shape name
         * @param  withLines  true iff you want a crosshair drawn as well as
         *         the rectangle
         */
        public OpenRectangle( String name, boolean withLines ) {
            super( name, withLines );
            withLines_ = withLines;
        }

        protected void drawOblong( Graphics g, int x, int y,
                                   int width, int height ) {
            g.drawRect( x, y, width, height );
        }

        public boolean supportsDimensionality( int ndim ) {
            return ndim == 2;
        }

        public void drawShape( PixelDrawing drawing,
                               int x, int y, int[] xoffs, int[] yoffs ) {
            if ( xoffs.length == 4 && yoffs.length == 4 ) {
                int xa = x + xoffs[ 0 ] + xoffs[ 2 ];
                int xb = x + xoffs[ 0 ] + xoffs[ 3 ];
                int xc = x + xoffs[ 1 ] + xoffs[ 3 ];
                int xd = x + xoffs[ 1 ] + xoffs[ 2 ];
                int ya = y + yoffs[ 0 ] + yoffs[ 2 ];
                int yb = y + yoffs[ 0 ] + yoffs[ 3 ];
                int yc = y + yoffs[ 1 ] + yoffs[ 3 ];
                int yd = y + yoffs[ 1 ] + yoffs[ 2 ];
                drawing.drawLine( xa, ya, xb, yb );
                drawing.drawLine( xb, yb, xc, yc );
                drawing.drawLine( xc, yc, xd, yd );
                drawing.drawLine( xd, yd, xa, ya );
                if ( withLines_ ) {
                    for ( int i = 0; i < 4; i++ ) {
                        drawing.drawLine( x, y,
                                          x + xoffs[ i ], y + yoffs[ i ] );
                    }
                }
            }
        }
    }

    /**
     * Oblong using a filled rectangle.
     */
    private static class FilledRectangle extends Oblong {

        /**
         * Constructor.
         *
         * @param  name   shape name
         */
        public FilledRectangle( String name ) {
            super( name, false );
        }

        protected void drawOblong( Graphics g, int x, int y,
                                   int width, int height ) {
            g.fillRect( x, y, width, height );
        }

        public void drawShape( PixelDrawing drawing,
                               int x, int y, int[] xoffs, int[] yoffs ) {
            if ( xoffs.length != 4 || yoffs.length != 4 ) {
                return;
            }
            else if ( yoffs[ 0 ] == 0 && yoffs[ 1 ] == 0 &&
                      xoffs[ 2 ] == 0 && xoffs[ 3 ] == 0 ) {
                int xlo = x + Math.min( xoffs[ 0 ], xoffs[ 1 ] );
                int xhi = x + Math.max( xoffs[ 0 ], xoffs[ 1 ] );
                int ylo = y + Math.min( yoffs[ 2 ], yoffs[ 3 ] );
                int yhi = y + Math.max( yoffs[ 2 ], yoffs[ 3 ] );
                int width = xhi - xlo;
                int height = yhi - ylo;
                drawing.fillRect( xlo, ylo, width, height );
            }
            else {
                int[] xof = { xoffs[ 0 ] + xoffs[ 2 ],
                              xoffs[ 1 ] + xoffs[ 2 ],
                              xoffs[ 1 ] + xoffs[ 3 ],
                              xoffs[ 0 ] + xoffs[ 3 ], };
                int[] yof = { yoffs[ 0 ] + yoffs[ 2 ],
                              yoffs[ 1 ] + yoffs[ 2 ],
                              yoffs[ 1 ] + yoffs[ 3 ],
                              yoffs[ 0 ] + yoffs[ 3 ], };
                Polygon poly = new Polygon( xof, yof, 4 );
                poly.translate( x, y );
                drawing.fill( poly );
            }
        }
    }

    /**
     * Shape which draws a wire-net line/rectangle/cuboid in 1/2/3 dimensions.
     */
    private static class OpenCuboid extends DrawingShape {

        private final Icon legendIcon_;

        /**
         * Constructor.
         *
         * @param  name  shape name
         */
        OpenCuboid( String name ) {
            super( name );
            legendIcon_ = new MultiPointIcon( this, 3 );
        }

        public boolean supportsDimensionality( int ndim ) {
            return ndim == 3;
        }

        public Icon getLegendIcon() {
            return legendIcon_;
        }

        public Icon getLegendIcon( ErrorMode[] modes, int width, int height,
                                   int xpad, int ypad ) {
            return new MultiPointIcon( this, modes, width, height,
                                       xpad + width / 6, ypad + height / 6 );
        }

        public Rectangle getBounds( int x, int y, int[] xoffs, int[] yoffs ) {
            LineBounder bounder = new LineBounder();
            doLines( x, y, xoffs, yoffs, bounder );
            int width = bounder.xhi_ - bounder.xlo_;
            int height = bounder.yhi_ - bounder.ylo_;
            if ( width > 0 || height > 0 ) {
                width++;
                height++;
            }
            return new Rectangle( bounder.xlo_, bounder.ylo_, width, height );
        }

        public void drawShape( Graphics g, int x, int y,
                               int[] xoffs, int[] yoffs ) {
            Graphics2D g2 = (Graphics2D) g;
            Stroke oldStroke = g2.getStroke();
            g2.setStroke( CAP_ROUND );
            doLines( x, y, xoffs, yoffs,
                     (x1, y1, x2, y2) -> g.drawLine( x1, y1, x2, y2 ) );
            g2.setStroke( oldStroke );
        }

        public void drawShape( PixelDrawing drawing,
                               int x, int y, int[] xoffs, int[] yoffs ) {
            doLines( x, y, xoffs, yoffs, 
                     (x1, y1, x2, y2) -> drawing.drawLine( x1, y1, x2, y2 ) );
        }

        /**
         * Presents the coordinates of lines drawn by this shape
         * to a given callback.
         *
         * @param  x  data point X coordinate
         * @param  y  data point Y coordinate
         * @param  xoffs  X coordinates of shape limit offsets from (x,y)
         * @param  yoffs  Y coordinates of shape limit offsets from (x,y)
         * @param  liner  destination for line start/end coordinates
         */
        private void doLines( int x, int y, int[] xoffs, int[] yoffs,
                              LineConsumer liner ) {
            int ndim = xoffs.length / 2;
            if ( ndim == 1 ) {
                liner.line( x + xoffs[ 0 ], y + yoffs[ 0 ],
                            x + xoffs[ 1 ], y + yoffs[ 1 ] );
            }
            else if ( ndim == 2 ) {
                int x00 = x + xoffs[ 0 ] + xoffs[ 2 ];
                int x01 = x + xoffs[ 0 ] + xoffs[ 3 ];
                int x11 = x + xoffs[ 1 ] + xoffs[ 3 ];
                int x10 = x + xoffs[ 1 ] + xoffs[ 2 ];
                int y00 = y + yoffs[ 0 ] + yoffs[ 2 ];
                int y01 = y + yoffs[ 0 ] + yoffs[ 3 ];
                int y11 = y + yoffs[ 1 ] + yoffs[ 3 ];
                int y10 = y + yoffs[ 1 ] + yoffs[ 2 ];
                liner.line( x00, y00, x01, y01 );
                liner.line( x01, y01, x11, y11 );
                liner.line( x11, y11, x10, y10 );
                liner.line( x10, y10, x00, y00 );
            }
            else if ( ndim == 3 ) {
                int x000 = x + xoffs[ 0 ] + xoffs[ 2 ] + xoffs[ 4 ];
                int x001 = x + xoffs[ 0 ] + xoffs[ 2 ] + xoffs[ 5 ];
                int x010 = x + xoffs[ 0 ] + xoffs[ 3 ] + xoffs[ 4 ];
                int x011 = x + xoffs[ 0 ] + xoffs[ 3 ] + xoffs[ 5 ];
                int x100 = x + xoffs[ 1 ] + xoffs[ 2 ] + xoffs[ 4 ];
                int x101 = x + xoffs[ 1 ] + xoffs[ 2 ] + xoffs[ 5 ];
                int x110 = x + xoffs[ 1 ] + xoffs[ 3 ] + xoffs[ 4 ];
                int x111 = x + xoffs[ 1 ] + xoffs[ 3 ] + xoffs[ 5 ];
                int y000 = y + yoffs[ 0 ] + yoffs[ 2 ] + yoffs[ 4 ];
                int y001 = y + yoffs[ 0 ] + yoffs[ 2 ] + yoffs[ 5 ];
                int y010 = y + yoffs[ 0 ] + yoffs[ 3 ] + yoffs[ 4 ];
                int y011 = y + yoffs[ 0 ] + yoffs[ 3 ] + yoffs[ 5 ];
                int y100 = y + yoffs[ 1 ] + yoffs[ 2 ] + yoffs[ 4 ];
                int y101 = y + yoffs[ 1 ] + yoffs[ 2 ] + yoffs[ 5 ];
                int y110 = y + yoffs[ 1 ] + yoffs[ 3 ] + yoffs[ 4 ];
                int y111 = y + yoffs[ 1 ] + yoffs[ 3 ] + yoffs[ 5 ];
                liner.line( x000, y000, x001, y001 );
                liner.line( x000, y000, x010, y010 );
                liner.line( x000, y000, x100, y100 );
                liner.line( x001, y001, x011, y011 );
                liner.line( x001, y001, x101, y101 );
                liner.line( x010, y010, x011, y011 );
                liner.line( x010, y010, x110, y110 );
                liner.line( x100, y100, x101, y101 );
                liner.line( x100, y100, x110, y110 );
                liner.line( x011, y011, x111, y111 );
                liner.line( x101, y101, x111, y111 );
                liner.line( x110, y110, x111, y111 );
            }
        }

        /**
         * Does something with the start and end coordinates of a line.
         */
        @FunctionalInterface
        private static interface LineConsumer {

            /**
             * Processes a line.
             *
             * @param  x1  start point X coordinate
             * @param  y1  start point Y coordinate
             * @param  x2  end point X coordinate
             * @param  y2  end point Y coordinate
             */
            void line( int x1, int y1, int x2, int y2 );
        }

        /**
         * Accumulates the bounding coordinates for a series of lines.
         */
        private static class LineBounder implements LineConsumer {
            int xlo_ = Integer.MAX_VALUE;
            int ylo_ = Integer.MAX_VALUE;
            int xhi_ = Integer.MIN_VALUE;
            int yhi_ = Integer.MIN_VALUE;
            public void line( int x1, int y1, int x2, int y2 ) {
                xlo_ = Math.min( xlo_, Math.min( x1, x2 ) );
                ylo_ = Math.min( ylo_, Math.min( y1, y2 ) );
                xhi_ = Math.max( xhi_, Math.max( x1, x2 ) );
                yhi_ = Math.max( yhi_, Math.max( y1, y2 ) );
            }
        }
    }

    /**
     * Shape which draws N-dimensional (N probably equals 3)
     * error bars by rendering 2-d error bars in each of the N(N-1)
     * pairs of dimensions.
     */ 
    private static class MultiPlaneShape extends DrawingShape {

        private final DrawingShape shape2d_;
        private final Icon legend_;

        /**
         * Constructor.
         *
         * @param  shape2d  2-dimensional shape on which this one is based;
         *         the name is taken from this
         */
        MultiPlaneShape( DrawingShape shape2d ) {
            super( shape2d.getName() );
            shape2d_ = shape2d;
            legend_ = new MultiPointIcon( this, 3 );
        }
                        
        public boolean supportsDimensionality( int ndim ) {
            return ndim == 3
                || ( ndim < 3 && shape2d_.supportsDimensionality( ndim ) );
        }
         
        public Icon getLegendIcon() {
            return legend_; 
        }

        public Icon getLegendIcon( ErrorMode[] modes, int width, int height,
                                   int xpad, int ypad ) {
            return new MultiPointIcon( this, modes, width, height,
                                       xpad + width / 6,
                                       ypad + height / 6 );
        }

        public Rectangle getBounds( int x, int y, int[] xoffs, int[] yoffs ) {
            Rectangle bounds = new Rectangle( x, y, 0, 0 );
            for ( int[][] offs : get2dOffsets( xoffs, yoffs ) ) {
                bounds.add( shape2d_.getBounds( x, y, offs[ 0 ], offs[ 1 ] ) );
            }
            return bounds;
        }

        public void drawShape( Graphics g, int x, int y, int[] xoffs,
                               int[] yoffs ) {
            for ( int[][] offs : get2dOffsets( xoffs, yoffs ) ) {
                shape2d_.drawShape( g, x, y, offs[ 0 ], offs[ 1 ] );
            }
        }

        public void drawShape( PixelDrawing drawing,
                               int x, int y, int[] xoffs, int[] yoffs ) {
            for ( int[][] offs : get2dOffsets( xoffs, yoffs ) ) {
                shape2d_.drawShape( drawing, x, y, offs[ 0 ], offs[ 1 ] );
            }
        }

        /**
         * Returns an iterable over pairs of non-null offset dimensions.
         * The iterator returns <code>int[npoint][2]</code> objects, being
         * the X, Y coordinates of points defining the extrema of an
         * error region.  The value of <code>npoint</code> is usually 4,
         * defining 2-dimensional region, but in the case that only a
         * single result is returned it <code>npoint</code> may be 2,
         * representing a 1-dimensional region.
         *
         * @param  xoffs  2*ndim-element X offset point array
         * @param  yoffs  2*ndim-element Y offset point array
         * @return  iterable of X,Y point arrays
         */
        private Iterable<int[][]> get2dOffsets( final int[] xoffs,
                                                final int[] yoffs ) {

            /* Number of dimensions is half the number of points (there
             * must be an upper and lower bound point in each dimension). */
            final int ndim = xoffs.length / 2;

            /* If there are less than 3 dimensions, it's trivial. */
            if ( ndim < 3 ) {
                return Collections
                      .singletonList( new int[][] { xoffs, yoffs } );
            }

            /* Otherwise work out which pairs of dimensions have non-zero
             * extents. */
            else {
                final int[] activeDims = new int[ ndim ];
                int iActiveDim = 0;
                for ( int idim = 0; idim < ndim; idim++ ) {
                    int i2 = idim * 2;
                    if ( xoffs[ i2 + 0 ] != 0 || yoffs[ i2 + 0 ] != 0 ||
                         xoffs[ i2 + 1 ] != 0 || yoffs[ i2 + 0 ] != 0 ) {
                        activeDims[ iActiveDim++ ] = idim;
                    }
                }
                final int nActiveDim = iActiveDim;

                /* If there are none, no points are returned. */
                if ( nActiveDim == 0 ) {
                    return Collections.<int[][]>emptyList();
                }

                /* If there's one, return a singleton iterator over the
                 * 2-point coordinate arrays. */
                else if ( nActiveDim == 1 ) {
                    int[][] offPairs = new int[ 2 ][ 2 ];
                    int i2 = activeDims[ 0 ] * 2;
                    offPairs[ 0 ][ 0 ] = xoffs[ i2 + 0 ];
                    offPairs[ 1 ][ 0 ] = yoffs[ i2 + 0 ];
                    offPairs[ 0 ][ 1 ] = xoffs[ i2 + 1 ];
                    offPairs[ 1 ][ 1 ] = yoffs[ i2 + 1 ];
                    return Collections.singletonList( offPairs );
                }

                /* Otherwise return an iterator over non-blank 4-point arrays,
                 * each formed from the coorindates from a pair of
                 * non-blank dimensions. */
                else {
                    assert nActiveDim >= 2;
                    return () -> new Iterator<int[][]>() {
                        int[][] offPairs = new int[ 2 ][ 4 ];
                        boolean done;
                        int iActive = 0;
                        int jActive = 1;
                        public int[][] next() {
                            if ( done ) {
                                throw new NoSuchElementException();
                            }
                            int i2 = activeDims[ iActive ] * 2;
                            int j2 = activeDims[ jActive ] * 2;
                            if ( ++iActive >= jActive ) {
                                iActive = 0;
                                if ( ++jActive >= nActiveDim ) {
                                    done = true;
                                }
                            }
                            offPairs[ 0 ][ 0 ] = xoffs[ i2 + 0 ];
                            offPairs[ 1 ][ 0 ] = yoffs[ i2 + 0 ];
                            offPairs[ 0 ][ 1 ] = xoffs[ i2 + 1 ];
                            offPairs[ 1 ][ 1 ] = yoffs[ i2 + 1 ];
                            offPairs[ 0 ][ 2 ] = xoffs[ j2 + 0 ];
                            offPairs[ 1 ][ 2 ] = yoffs[ j2 + 0 ];
                            offPairs[ 0 ][ 3 ] = xoffs[ j2 + 1 ];
                            offPairs[ 1 ][ 3 ] = yoffs[ j2 + 1 ];
                            return offPairs;
                        }

                        public boolean hasNext() {
                            return ! done;
                        }

                        public void remove() {
                            throw new UnsupportedOperationException();
                        }
                    };
                }
            }
        }
    }

    /**
     * Icon which represents a MultiPointShape in a form suitable for a legend.
     */
    private static class MultiPointIcon implements Icon {

        private final MultiPointShape shape_;
        private final int width_;
        private final int height_;
        private final int[] xoffs_;
        private final int[] yoffs_;

        /**
         * Constructs an icon with default characteristics.
         *
         * @param  shape   shape
         * @param  ndim    dimensionality
         */
        public MultiPointIcon( MultiPointShape shape, int ndim ) {
            this( shape, fillModeArray( ndim, ErrorMode.SYMMETRIC ),
                  LEGEND_WIDTH, LEGEND_HEIGHT, LEGEND_XPAD, LEGEND_YPAD );
        }

        /**
         * Constructs an icon with specified characteristics.
         *
         * @param  shape   shape
         * @param   modes  array of ErrorModes, one per error dimension
         * @param   width  total width of icon
         * @param   height total height of icon
         * @param   xpad   internal horizontal padding of icon
         * @param   ypad   internal vertical padding of icon
         */
        public MultiPointIcon( MultiPointShape shape, ErrorMode[] modes,
                               int width, int height, int xpad, int ypad ) {
            shape_ = shape;
            width_ = width;
            height_ = height;
            int w2 = width / 2 - xpad;
            int h2 = height / 2 - ypad;
            int ndim = modes.length;
            List<Point> offList = new ArrayList<Point>( ndim );
            if ( ndim > 0 ) {
                ErrorMode xmode = modes[ 0 ];
                if ( ! ErrorMode.NONE.equals( xmode ) ) {
                    float xlo = (float) xmode.getExampleLower();
                    float xhi = (float) xmode.getExampleUpper();
                    offList.add( new Point( Math.round( - xlo * w2 ), 0 ) );
                    offList.add( new Point( Math.round( + xhi * w2 ), 0 ) );
                }
            }
            if ( ndim > 1 ) {
                ErrorMode ymode = modes[ 1 ];
                if ( ! ErrorMode.NONE.equals( ymode ) ) {
                    float ylo = (float) ymode.getExampleLower();
                    float yhi = (float) ymode.getExampleUpper();
                    offList.add( new Point( 0, Math.round( + ylo * h2 ) ) );
                    offList.add( new Point( 0, Math.round( - yhi * h2 ) ) );
                }
            }
            if ( ndim > 2 ) {
                ErrorMode zmode = modes[ 2 ];
                if ( ! ErrorMode.NONE.equals( zmode ) ) {
                    float zlo = (float) zmode.getExampleLower();
                    float zhi = (float) zmode.getExampleUpper();
                    float theta = (float) Math.toRadians( 40 );
                    float slant = 0.8f;
                    float c = (float) Math.cos( theta ) * slant;
                    float s = (float) Math.sin( theta ) * slant;
                    offList.add( new Point( Math.round( - c * zlo * w2 ),
                                            Math.round( + s * zlo * h2 ) ) );
                    offList.add( new Point( Math.round( + c * zhi * w2 ),
                                            Math.round( - s * zhi * h2 ) ) );
                }
            }
            int np = offList.size();
            xoffs_ = new int[ np ];
            yoffs_ = new int[ np ];
            for ( int ip = 0; ip < np; ip++ ) {
                Point point = offList.get( ip );
                xoffs_[ ip ] = point.x;
                yoffs_[ ip ] = point.y;
            }
        }

        public int getIconWidth() {
            return width_;
        }

        public int getIconHeight() {
            return height_;
        }

        public void paintIcon( Component c, Graphics g, int x, int y ) {
            Graphics2D g2 = (Graphics2D) g;
            Object aaHint =
                g2.getRenderingHint( RenderingHints.KEY_ANTIALIASING );
            g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
                                 RenderingHints.VALUE_ANTIALIAS_ON );
            shape_.drawShape( g2, x + width_ / 2, y + height_ / 2,
                              xoffs_, yoffs_ );
            g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, aaHint );
        }

        /**
         * Utility method to return an array containing all the same modes.
         *
         * @param  leng  number of elements
         * @param  mode  content of each element
         * @return array filled with <code>mode</code>
         */
        private static ErrorMode[] fillModeArray( int leng, ErrorMode mode ) {
            ErrorMode[] modes = new ErrorMode[ leng ];
            Arrays.fill( modes, mode );
            return modes;
        }
    }
}
