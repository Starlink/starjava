package uk.ac.starlink.topcat.plot;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
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
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.swing.Icon;
import uk.ac.starlink.topcat.EmptyIcon;
import uk.ac.starlink.util.IntList;

/**
 * Renders error bars.
 * 
 * @author   Mark Taylor
 * @since    20 Feb 2007
 */
public abstract class ErrorRenderer {

    private final String name_;

    /** Error renderer which draws nothing. */
    public static ErrorRenderer NONE = new Blank( "None" );

    /** General purpose error renderer. */
    public static ErrorRenderer DEFAULT = new CappedLine( "Lines", true, 0 );

    /** Error renderer suitable for use in user controls. */
    public static ErrorRenderer EXAMPLE = new CappedLine( "Lines", true, 3 );

    private static final ErrorRenderer[] OPTIONS_2D = new ErrorRenderer[] {
        NONE,
        DEFAULT,
        new CappedLine( "Capped Lines", true, 3 ),
        new CappedLine( "Caps", false, 3 ),
        new OpenEllipse( "Ellipse", false ),
        new OpenEllipse( "Crosshair Ellipse", true ),
        new OpenRectangle( "Rectangle", false ),
        new OpenRectangle( "Crosshair Rectangle", true ),
        new FilledEllipse( "Filled Ellipse" ),
        new FilledRectangle( "Filled Rectangle" ),
    };

    private static final ErrorRenderer[] OPTIONS_3D = new ErrorRenderer[] {
        NONE,
        DEFAULT,
        new CappedLine( "Capped Lines", true, 3 ),
        new CappedLine( "Caps", false, 3 ),
        new MultiPlaneRenderer( new OpenEllipse( "Ellipse", false ) ),
        new MultiPlaneRenderer( new OpenEllipse( "Crosshair Ellipse", true ) ),
        new MultiPlaneRenderer( new OpenRectangle( "Rectangle", false ) ),
        new MultiPlaneRenderer( new OpenRectangle( "Crosshair Rectangle",
                                                   true ) ),
        new MultiPlaneRenderer( new FilledEllipse( "Filled Ellipse" ) ),
        new MultiPlaneRenderer( new FilledRectangle( "Filled Rectangle" ) ),
    };

    private static ErrorRenderer[] OPTIONS_GENERAL = new ErrorRenderer[] {
        NONE,
        DEFAULT,
        new CappedLine( "Capped Lines", true, 3 ),
        new CappedLine( "Caps", false, 3 ),
    };

    private static final Stroke CAP_ROUND =
        new BasicStroke( 1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER );
    private static final Stroke CAP_BUTT = 
        new BasicStroke( 1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER );
    private static final Iterator EMPTY_ITERATOR = new Iterator() {
        public boolean hasNext() {
            return false;
        }
        public Object next() {
            throw new NoSuchElementException();
        }
        public void remove() {
            throw new NoSuchElementException();
        }
    };

    private static final int[] NO_PIXELS = new int[ 0 ];

    private static final int LEGEND_WIDTH = 40;
    private static final int LEGEND_HEIGHT = 16;
    private static final int LEGEND_XPAD = 5;
    private static final int LEGEND_YPAD = 1;

    /**
     * Constructor.
     *
     * @param  name  renderer name
     */
    protected ErrorRenderer( String name ) {
        name_ = name;
    }

    /**
     * Returns an icon giving a general example of what this form looks like.
     *
     * @return  example icon
     */
    public abstract Icon getLegendIcon();

    /**
     * Returns an icon giving an example of what this form looks like in a
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
     * Returns a user-readable name for this style of error rendering.
     *
     * @return   renderer name
     */
    public String getName() {
        return name_;
    }

    /**
     * Indicates whether this renderer is known to produce no output 
     * for a particular set of ErrorModes.  If <code>modes</code> is null,
     * the question is about whether this renderer will produce no output
     * regardless of the error mode context.
     *
     * @param   modes   error mode context, or null
     * @return  true if this renderer can be guaranteed to paint nothing
     */
    public abstract boolean isBlank( ErrorMode[] modes );

    /**
     * Reports whether this form can be used on a given error dimensionality.
     *
     * @param  ndim  number of error dimensions to be used
     * @return  true iff this object can do rendering
     */
    public abstract boolean supportsDimensionality( int ndim );

    /**
     * Renders error bars in one or more dimensions around a given point.
     * The positions of the ends of error bars relative to the the point
     * are given.  There may in general be (2*N) of these, though certain
     * <code>ErrorRenderer</code> implementations may impose restrictions
     * on this count.  They must come in consecutive pairs which describe
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
     * @param  xoffs  X coordinates of error bar limit offsets from (x,y)
     * @param  yoffs  Y coordinates of error bar limit offsets from (x,y)
     */
    public abstract void drawErrors( Graphics g, int x, int y, int[] xoffs,
                                     int[] yoffs );

    /**
     * Returns a rectangle which will contain the rendered error bar 
     * graphics for a given point.
     * The parameters are the same as for {@link #drawErrors},
     *
     * @param  g  graphics context
     * @param  x  data point X coordinate
     * @param  y  data point Y coordinate
     * @param  xoffs  X coordinates of error bar limit offsets from (x,y)
     * @param  yoffs  Y coordinates of error bar limit offsets from (x,y)
     * @return  bounding box
     */
    public abstract Rectangle getBounds( Graphics g, int x, int y, int[] xoffs,
                                         int[] yoffs );

    /**
     * Returns an array of pixel positions which can be used to draw this
     * marker onto a raster.  This can be used as an alternative to 
     * rendering the marker using the {@link #drawErrors} method,
     * for instance in situations where it might be more efficient.
     * The returned value is a 2N-element array giving the coordinates
     * of each painted pixel.  The format is (x1,y1, x2,y2, ...).
     * The assumption is that all the pixels are the same colour.
     *
     * <p>The ErrorRenderer implementation calculates this by painting
     * onto a temporary BufferedImage and then examining the raster to see
     * which pixels have been painted.  This is probably not very efficient.
     * Subclasses are encouraged to override this method if they can 
     * calculate the pixels which will be painted directly.
     *
     * @return  array of pixel coordinates representing the error bar
     *          as a bitmap
     */
    public int[] getPixels( Graphics g, int x, int y, int[] xoffs,
                            int[] yoffs ) {

        /* Work out the size of raster we will need to paint onto. */
        Rectangle bounds = getBounds( g, x, y, xoffs, yoffs )
                          .intersection( g.getClipBounds() );
        int xdim = bounds.width;
        int ydim = bounds.height;

        /* Return an empty array if it has zero size. */
        if ( xdim <= 0 || ydim <= 0 ) {
            return NO_PIXELS;
        }

        /* Prepare an image of a suitable size and a graphics context which
         * will render onto it. */
        BufferedImage im = 
            new BufferedImage( xdim, ydim, BufferedImage.TYPE_INT_ARGB );
        Graphics2D g2 = im.createGraphics();
        g2.translate( -bounds.x, -bounds.y );
        g2.setClip( bounds );

        /* Paint the error bars. */
        drawErrors( g2, x, y, xoffs, yoffs );

        /* Examine each pixel from the raster and write its coordinates 
         * into the coordinate list if it has been painted on (alpha is
         * non-zero). */
        Raster raster = im.getData();
        IntList coordList = new IntList( Math.min( 100, xdim * ydim ) );
        for ( int ix = 0; ix < xdim; ix++ ) {
            for ( int iy = 0; iy < ydim; iy++ ) {
                int alpha = raster.getSample( ix, iy, 3 );
                assert alpha == 0 || alpha == 255;
                if ( alpha > 0 ) {
                    coordList.add( ix + bounds.x );
                    coordList.add( iy + bounds.y );
                }
            }
        }

        /* Convert the result into an array and return. */
        return coordList.toIntArray();
    }

    /**
     * Returns an array of ErrorRenderers which can render 2-dimensional errors.
     *
     * @return  selection of renderers
     */
    public static ErrorRenderer[] getOptions2d() {
        return (ErrorRenderer[]) OPTIONS_2D.clone();
    }

    /**
     * Returns an array of ErrorRenderers which can render 3-dimensional errors.
     *
     * @return  selection of renderers
     */
    public static ErrorRenderer[] getOptions3d() {
        return (ErrorRenderer[]) OPTIONS_3D.clone();
    }

    /**
     * Returns an array of ErrorRenderers which can render errors of arbitrary
     * dimensionality.
     *
     * @return  selection of renderers
     */
    public static ErrorRenderer[] getOptionsGeneral() {
        return (ErrorRenderer[]) OPTIONS_GENERAL.clone();
    }

    /**
     * Icon which represents an ErrorRenderer in a form suitable for a legend.
     */
    private static class ErrorRendererIcon implements Icon {

        private final ErrorRenderer renderer_;
        private final int width_;
        private final int height_;
        private final int[] xoffs_;
        private final int[] yoffs_;

        /**
         * Constructs an icon with default characteristics.
         *
         * @param  renderer  error renderer
         * @param  ndim    dimensionality
         */
        public ErrorRendererIcon( ErrorRenderer renderer, int ndim ) {
            this( renderer, fillModeArray( ndim, ErrorMode.SYMMETRIC ),
                  LEGEND_WIDTH, LEGEND_HEIGHT, LEGEND_XPAD, LEGEND_YPAD );
        }

        /**
         * Constructs an icon with specified characteristics.
         *
         * @param  renderer  error renderer
         * @param   modes  array of ErrorModes, one per error dimension
         * @param   width  total width of icon
         * @param   height total height of icon
         * @param   xpad   internal horizontal padding of icon
         * @param   ypad   internal vertical padding of icon
         */
        public ErrorRendererIcon( ErrorRenderer renderer, ErrorMode[] modes,
                                  int width, int height, int xpad, int ypad ) {
            renderer_ = renderer;
            width_ = width;
            height_ = height;
            int w2 = width / 2 - xpad;
            int h2 = height / 2 - ypad;
            int ndim = modes.length;
            List offList = new ArrayList( ndim );
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
                Point point = (Point) offList.get( ip );
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
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
                                 RenderingHints.VALUE_ANTIALIAS_ON );
            g2.setClip( x, y, width_, height_ );
            renderer_.drawErrors( g2, x + width_ / 2, y + height_ / 2,
                                  xoffs_, yoffs_ );
            g2.dispose();
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

    /**
     * ErrorRenderer which renders an (optional) line from the data point
     * to the given offset, and an (optional) cap normal to that line
     * at its furthest extent.  Works for any dimensionality.
     */
    private static class CappedLine extends ErrorRenderer {

        private final boolean lines_;
        private final int capsize_;
        private final Icon legend_;

        /**
         * Constructor.
         *
         * @param  name   renderer name
         * @param  lines   true iff you want error lines drawn
         * @param  capsize  the number of pixels in each direction the
         *                  cap should extend; zero means no cap
         */
        CappedLine( String name, boolean lines, int capsize ) {
            super( name );
            lines_ = lines;
            capsize_ = capsize;
            legend_ = new ErrorRendererIcon( this, 2 );
        }

        public boolean supportsDimensionality( int ndim ) {
            return ndim > 0;
        }

        public Icon getLegendIcon() {
            return legend_;
        }

        public Icon getLegendIcon( ErrorMode[] modes, int width, int height,
                                   int xpad, int ypad ) {
            return new ErrorRendererIcon( this, modes, width, height,
                                          xpad, ypad );
        }

        public boolean isBlank( ErrorMode[] modes ) {
            return modes != null && ErrorMode.allBlank( modes );
        }

        public void drawErrors( Graphics g, int x, int y, int[] xoffs,
                                int[] yoffs ) {
            drawErrors( g, x, y, xoffs, yoffs, g.getClipBounds(), 
                        lines_, capsize_, false );
        }

        /**
         * Does the work for rendering the errors of a CappedLine style.
         *
         * @param  g  graphics context
         * @param  x  data point X coordinate
         * @param  y  data point Y coordinate
         * @param  xoffs  X coordinates of error bar limit offsets from (x,y)
         * @param  yoffs  Y coordinates of error bar limit offsets from (x,y)
         * @param  clip   bounds of output
         * @param  lines  whether to draw lines
         * @param  capsize  size of capping lines (0 for none)
         * @param  willCover  true if the ends of the radial lines will 
         *                    subsequently be covered by more drawing 
         *                    (affects line capping)
         */
        public static void drawErrors( Graphics g, int x, int y,
                                       int[] xoffs, int[] yoffs, Rectangle clip,
                                       boolean lines, int capsize,
                                       boolean willCover ) {
            Graphics2D g2 = (Graphics2D) g;
            Stroke oldStroke = g2.getStroke();
            g2.setStroke( capsize > 0 || willCover ? CAP_BUTT : CAP_ROUND );
            int xmax = clip.width + 1;
            int ymax = clip.height + 1;
            int np = xoffs.length;
            for ( int ip = 0; ip < np; ip++ ) {
                int xoff = xoffs[ ip ];
                int yoff = yoffs[ ip ];

                /* Only do drawing for a non-blank line. */
                if ( xoff != 0 || yoff != 0 ) {

                    /* If the end coordinate is definitely outside the graphics
                     * clip, shrink the line to something about the right size.
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
                            double shrink =
                                Math.sqrt( ( xmax * xmax + ymax * ymax )
                                         / ( xoff * xoff + yoff * yoff ) );
                            xoff = (int) Math.ceil( shrink * xoff );
                            yoff = (int) Math.ceil( shrink * yoff );
                        }
                    } 

                    /* Draw line if required. */
                    if ( lines ) {
                        g.drawLine( x, y, x + xoff, y + yoff );
                    }

                    /* Draw cap if required. */
                    if ( capsize > 0 && ! clipped ) {
                        g2.setStroke( CAP_ROUND );

                        /* For rectilinear offsets, draw the cap manually. */
                        if ( xoff == 0 ) {
                            g.drawLine( x - capsize, y + yoff,
                                        x + capsize, y + yoff );
                        }
                        else if ( yoff == 0 ) {
                            g.drawLine( x + xoff, y - capsize,
                                        x + xoff, y + capsize );
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
                            g2.drawLine( leng, - capsize, leng, capsize );
                            g2.setTransform( oldTransform );
                        }
                    }
                }
            }
            g2.setStroke( oldStroke );
        }

        public int[] getPixels( Graphics g, int x, int y, int[] xoffs,
                                int[] yoffs ) {
            Drawing drawing = new Drawing( g.getClipBounds() );
            int np = xoffs.length;
            for ( int ip = 0; ip < np; ip++ ) {
                int xoff = xoffs[ ip ];
                int yoff = yoffs[ ip ];
                if ( xoff != 0 || yoff != 0 ) {
                    if ( lines_ ) {
                        drawing.drawLine( x, y, x + xoff, y + yoff );
                    }
                    if ( capsize_ > 0 ) {
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
                }
            }
            return drawing.getPixels();
        }

        public Rectangle getBounds( Graphics g, int x, int y, int[] xoffs,
                                    int[] yoffs ) {
            int xmin = 0;
            int xmax = 0;
            int ymin = 0;
            int ymax = 0;
            boolean empty = true;
            int np = xoffs.length;
            for ( int ip = 0; ip < np; ip++ ) {
                int xoff = xoffs[ ip ];
                int yoff = yoffs[ ip ];
                if ( xoff != 0 || yoff != 0 ) {
                    empty = false;
                    if ( xoff == 0 ) {
                        xmin = Math.min( xmin, - capsize_ );
                        xmax = Math.max( xmax, + capsize_ );
                        ymin = Math.min( ymin, yoff );
                        ymax = Math.max( ymax, yoff );
                    }
                    else if ( yoff == 0 ) {
                        xmin = Math.min( xmin, xoff );
                        xmax = Math.max( xmax, xoff );
                        ymin = Math.min( ymin, - capsize_ );
                        ymax = Math.max( ymax, + capsize_ );
                    }
                    else {
                        xmin = Math.min( xmin, xoff - capsize_ );
                        xmax = Math.max( xmax, xoff + capsize_ );
                        ymin = Math.min( ymin, yoff - capsize_ );
                        ymax = Math.max( ymax, yoff + capsize_ );
                    }
                }
            }
            if ( empty ) {
                return new Rectangle( x, y, 0, 0 );
            }
            else {
                Rectangle box = new Rectangle( x + xmin, y + ymin,
                                               xmax - xmin, ymax - ymin );
                if ( g instanceof Graphics2D ) {
                    box = ((Graphics2D) g).getStroke().createStrokedShape( box )
                                                      .getBounds();
                }
                else {
                    box.width++;
                    box.height++;
                }
                return box;
            }
        }
    }

    /**
     * Generic abstract renderer for cases in which the rendered object is
     * effectively a quadrilateral of some sort.  Concrete subclasses
     * must implement {@link #drawOblong} to mark the space as appropriate.
     * Only works properly for two-dimensional errors.
     */
    private static abstract class Oblong extends ErrorRenderer {

        private final Icon legend_;
        private final boolean withLines_;

        /**
         * Constructor.
         *
         * @param  name   renderer name
         * @param  withLines  true iff you want a crosshair drawn as well as
         *         the basic representation of this renderer
         */
        Oblong( String name, boolean withLines ) {
            super( name );
            withLines_ = withLines;
            legend_ = new ErrorRendererIcon( this, 2 );
        }

        public boolean supportsDimensionality( int ndim ) {
            return ndim == 2;
        }

        public Icon getLegendIcon() {
            return legend_;
        }

        public Icon getLegendIcon( ErrorMode[] modes, int width, int height,
                                   int xpad, int ypad ) {
            return new ErrorRendererIcon( this, modes, width, height,
                                          xpad, ypad );
        }

        public boolean isBlank( ErrorMode[] modes ) {
            return modes != null && ErrorMode.allBlank( modes );
        }

        public void drawErrors( Graphics g, int x, int y, int[] xoffs, 
                                int[] yoffs ) {
            Graphics2D g2 = (Graphics2D) g;
            int noff = xoffs.length;

            /* Restrict the offsets to something sensible, to prevent the
             * graphics system attempting to fill an ellipse with a 
             * kilometre semi-major axis.  This may result in some 
             * distortions for ellipses - too bad. */
            Rectangle clip = g.getClipBounds();
            int maxcoord = Math.max( Math.max( clip.width, clip.height ) * 3,
                                     2000 );
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
                      xoffs[ 2 ] == 0 && xoffs[ 2 ] == 0 ) {
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
                int[] xo = xoffs;
                int[] yo = yoffs;
                double[] m2 = new double[] {
                    x + xo[1] + xo[2], x + xo[0] + xo[3], x + xo[0] + xo[2],
                    y + yo[1] + yo[2], y + yo[0] + yo[3], y + yo[0] + yo[2],
                    1,                 1,                 1,
                };
                if ( Matrices.det( m1 ) != 0 ) {
                    double[] m3 = Matrices.mmMult( m2, Matrices.invert( m1 ) );
                    AffineTransform trans =
                        new AffineTransform( m3[ 0 ], m3[ 3 ],
                                             m3[ 1 ], m3[ 4 ],
                                             m3[ 2 ], m3[ 5 ] );
                    AffineTransform oldTrans = g2.getTransform();
                    g2.transform( trans );
                    drawOblong( g2, 0, 0, (int) Math.round( width ),
                                (int) Math.round( height ) );
                    g2.setTransform( oldTrans );
                }
            }

            /* Draw crosshair if required. */
            if ( withLines_ ) {
                CappedLine.drawErrors( g, x, y, xoffs, yoffs, clip, true, 0,
                                       true );
            }
        }

        public Rectangle getBounds( Graphics g, int x, int y, int[] xoffs,
                                    int[] yoffs ) {
            int xmin = 0;
            int xmax = 0;
            int ymin = 0;
            int ymax = 0;
            int rmax = 0;
            boolean empty = true;
            int np = xoffs.length;
            for ( int ip = 0; ip < np; ip++ ) {
                int xoff = xoffs[ ip ];
                int yoff = yoffs[ ip ];
                if ( xoff != 0 || yoff != 0 ) {
                    empty = false;
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
            if ( empty ) {
                return new Rectangle( x, y, 0, 0 );
            }
            else {
                Rectangle box = new Rectangle( x + xmin, y + ymin,
                                               xmax - xmin, ymax - ymin ); 
                if ( g instanceof Graphics2D ) {
                    box = ((Graphics2D) g).getStroke().createStrokedShape( box )
                                                      .getBounds();
                }
                else {
                    box.width++;
                    box.height++;
                }
                return box;
            }
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
         * @param  name   renderer name
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

        public int[] getPixels( Graphics g, int x, int y, int[] xoffs,
                                int[] yoffs ) {
            if ( xoffs.length != 4 || yoffs.length != 4 ) {
                return NO_PIXELS;
            }
            else if ( yoffs[ 0 ] == 0 && yoffs[ 1 ] == 0 &&
                      xoffs[ 2 ] == 0 && xoffs[ 3 ] == 0 ) {
                int xlo = x + Math.min( xoffs[ 0 ], xoffs[ 1 ] );
                int xhi = x + Math.max( xoffs[ 0 ], xoffs[ 1 ] );
                int ylo = y + Math.min( yoffs[ 2 ], yoffs[ 3 ] );
                int yhi = y + Math.max( yoffs[ 2 ], yoffs[ 3 ] );
                int width = xhi - xlo;
                int height = yhi - ylo;
                Drawing drawing = new Drawing( g.getClipBounds() );
                drawing.drawOval( xlo, ylo, width, height );
                if ( withLines_ ) {
                    for ( int i = 0; i < 4; i++ ) {
                        drawing.drawLine( x, y,
                                          x + xoffs[ i ], y + yoffs[ i ] );
                    }
                }
                return drawing.getPixels();
            }
            else {
                int ax = ( xoffs[ 1 ] - xoffs[ 0 ] ) / 2;
                int ay = ( yoffs[ 1 ] - yoffs[ 0 ] ) / 2;
                int bx = ( xoffs[ 3 ] - xoffs[ 2 ] ) / 2;
                int by = ( yoffs[ 3 ] - yoffs[ 2 ] ) / 2;
                int x0 = x + Math.round( ( xoffs[ 0 ] + xoffs[ 1 ]
                                         + xoffs[ 2 ] + xoffs[ 3 ] ) / 4f );
                int y0 = y + Math.round( ( yoffs[ 0 ] + yoffs[ 1 ]
                                         + yoffs[ 2 ] + yoffs[ 3 ] ) / 4f );
                Drawing drawing = new Drawing( g.getClipBounds() );
                drawing.drawEllipse( x0, y0, ax, ay, bx, by );
                if ( withLines_ ) {
                    for ( int i = 0; i < 4; i++ ) {
                        drawing.drawLine( x, y,
                                          x + xoffs[ i ], y + yoffs[ i ] );
                    }
                }
                return drawing.getPixels();
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
         * @param  name   renderer name
         */
        public FilledEllipse( String name ) {
            super( name, false );
        }

        protected void drawOblong( Graphics g, int x, int y,
                                   int width, int height ) {
            g.fillOval( x, y, width, height );
        }

        public int[] getPixels( Graphics g, int x, int y, int[] xoffs,
                                int[] yoffs ) {
            if ( xoffs.length != 4 || yoffs.length != 4 ) {
                return NO_PIXELS;
            }
            else if ( yoffs[ 0 ] == 0 && yoffs[ 1 ] == 0 &&
                      xoffs[ 2 ] == 0 && xoffs[ 3 ] == 0 ) {
                int xlo = x + Math.min( xoffs[ 0 ], xoffs[ 1 ] );
                int xhi = x + Math.max( xoffs[ 0 ], xoffs[ 1 ] );
                int ylo = y + Math.min( yoffs[ 2 ], yoffs[ 3 ] );
                int yhi = y + Math.max( yoffs[ 2 ], yoffs[ 3 ] );
                int width = xhi - xlo;
                int height = yhi - ylo;
                Drawing drawing = new Drawing( g.getClipBounds() );
                drawing.fillOval( xlo, ylo, width, height );
                return drawing.getPixels();
            }
            else {
                int ax = ( xoffs[ 1 ] - xoffs[ 0 ] ) / 2;
                int ay = ( yoffs[ 1 ] - yoffs[ 0 ] ) / 2;
                int bx = ( xoffs[ 3 ] - xoffs[ 2 ] ) / 2;
                int by = ( yoffs[ 3 ] - yoffs[ 2 ] ) / 2;
                int x0 = x + Math.round( ( xoffs[ 0 ] + xoffs[ 1 ]
                                         + xoffs[ 2 ] + xoffs[ 3 ] ) / 4f );
                int y0 = y + Math.round( ( yoffs[ 0 ] + yoffs[ 1 ]
                                         + yoffs[ 2 ] + yoffs[ 3 ] ) / 4f );
                Drawing drawing = new Drawing( g.getClipBounds() );
                drawing.fillEllipse( x0, y0, ax, ay, bx, by );
                return drawing.getPixels();
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
         * @param  name   renderer name
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

        public int[] getPixels( Graphics g, int x, int y, int[] xoffs,
                                int[] yoffs ) {
            if ( xoffs.length == 4 && yoffs.length == 4 ) {
                int xa = x + xoffs[ 0 ] + xoffs[ 2 ];
                int xb = x + xoffs[ 0 ] + xoffs[ 3 ];
                int xc = x + xoffs[ 1 ] + xoffs[ 3 ];
                int xd = x + xoffs[ 1 ] + xoffs[ 2 ];
                int ya = y + yoffs[ 0 ] + yoffs[ 2 ];
                int yb = y + yoffs[ 0 ] + yoffs[ 3 ];
                int yc = y + yoffs[ 1 ] + yoffs[ 3 ];
                int yd = y + yoffs[ 1 ] + yoffs[ 2 ];
                Drawing drawing = new Drawing( g.getClipBounds() );
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
                return drawing.getPixels();
            }
            else {
                return NO_PIXELS;
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
         * @param  name   renderer name
         */
        public FilledRectangle( String name ) {
            super( name, false );
        }

        protected void drawOblong( Graphics g, int x, int y,
                                   int width, int height ) {
            g.fillRect( x, y, width, height );
        }

        public int[] getPixels( Graphics g, int x, int y, int[] xoffs,
                                int[] yoffs ) {
            if ( xoffs.length != 4 || yoffs.length != 4 ) {
                return NO_PIXELS;
            }
            else if ( yoffs[ 0 ] == 0 && yoffs[ 1 ] == 0 &&
                      xoffs[ 2 ] == 0 && xoffs[ 3 ] == 0 ) {
                int xlo = x + Math.min( xoffs[ 0 ], xoffs[ 1 ] );
                int xhi = x + Math.max( xoffs[ 0 ], xoffs[ 1 ] );
                int ylo = y + Math.min( yoffs[ 2 ], yoffs[ 3 ] );
                int yhi = y + Math.max( yoffs[ 2 ], yoffs[ 3 ] );
                int width = xhi - xlo;
                int height = yhi - ylo;
                Drawing drawing = new Drawing( g.getClipBounds() );
                drawing.fillRect( xlo, ylo, width, height );
                return drawing.getPixels();
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
                Drawing drawing = new Drawing( g.getClipBounds() );
                drawing.fill( poly );
                return drawing.getPixels();
            }
        }
    }

    /**
     * Error renderer which renders N-dimensional (N probably equals 3)
     * error bars by rendering 2-d error bars in each of the N(N-1) 
     * pairs of dimensions.
     */
    private static class MultiPlaneRenderer extends ErrorRenderer {

        private final ErrorRenderer rend2d_;
        private final Icon legend_;

        /**
         * Constructor.
         *
         * @param  rend2d  2-dimensional renderer on which this one is based;
         *         the name is taken from this
         */
        MultiPlaneRenderer( ErrorRenderer rend2d ) {
            super( rend2d.getName() );
            rend2d_ = rend2d;
            legend_ = new ErrorRendererIcon( this, 3 );
        }

        public boolean supportsDimensionality( int ndim ) {
            return ndim == 3 
                || ( ndim < 3 && rend2d_.supportsDimensionality( ndim ) );
        }

        public Icon getLegendIcon() {
            return legend_;
        }

        public Icon getLegendIcon( ErrorMode[] modes, int width, int height,
                                   int xpad, int ypad ) {
            return new ErrorRendererIcon( this, modes, width, height,
                                          xpad, ypad );
        }

        public boolean isBlank( ErrorMode[] modes ) {
            return modes != null && ErrorMode.allBlank( modes );
        }

        public Rectangle getBounds( Graphics g, int x, int y, int[] xoffs,
                                    int[] yoffs ) {
            Rectangle bounds = new Rectangle( x, y, 0, 0 );
            for ( Iterator it = get2dOffsets( xoffs, yoffs ); it.hasNext(); ) {
                int[][] offs = (int[][]) it.next();
                bounds.add( rend2d_.getBounds( g, x, y, 
                                               offs[ 0 ], offs[ 1 ] ) );
            }
            return bounds;
        }

        public void drawErrors( Graphics g, int x, int y, int[] xoffs,
                                int[] yoffs ) {
            for ( Iterator it = get2dOffsets( xoffs, yoffs ); it.hasNext(); ) {
                int[][] offs = (int[][]) it.next();
                rend2d_.drawErrors( g, x, y, offs[ 0 ], offs[ 1 ] );
            }
        }

        public int[] getPixels( Graphics g, int x, int y, int[] xoffs,
                                int[] yoffs ) {
            Set pointSet = new HashSet();
            int iPair = 0;
            for ( Iterator it = get2dOffsets( xoffs, yoffs ); it.hasNext(); ) {
                int[][] offs = (int[][]) it.next();
                int[] xyoffs =
                     rend2d_.getPixels( g, x, y, offs[ 0 ], offs[ 1 ] );

                /* Short cut if there is only one set. */
                if ( iPair++ == 0 && ! it.hasNext() ) {
                    return xyoffs;
                }
                int nxy = xyoffs.length / 2;
                for ( int ixy = 0; ixy < nxy; ixy++ ) {
                    int ox = xyoffs[ ixy * 2 + 0 ];
                    int oy = xyoffs[ ixy * 2 + 1 ];
                    pointSet.add( new Point( ox, oy ) );
                }
            }
            int[] xyoffs = new int[ pointSet.size() * 2 ];
            int ixy = 0;
            for ( Iterator it = pointSet.iterator(); it.hasNext(); ) {
                Point point = (Point) it.next();
                xyoffs[ ixy++ ] = point.x;
                xyoffs[ ixy++ ] = point.y;
            }
            assert ixy == xyoffs.length;
            return xyoffs;
        }

        /**
         * Returns an iterator over pairs of non-null offset dimensions.
         * The iterator returns <code>int[npoint][2]</code> objects, being
         * the X, Y coordinates of points defining the extrema of an
         * error region.  The value of <code>npoint</code> is usually 4,
         * defining 2-dimensional region, but in the case that only a
         * single result is returned it <code>npoint</code> may be 2,
         * representing a 1-dimensional region.
         *
         * @param  xoffs  2*ndim-element X offset point array
         * @param  yoffs  2*ndim-element Y offset point array
         * @return  iterator of X,Y point arrays
         */
        private Iterator get2dOffsets( final int[] xoffs, final int[] yoffs ) {

            /* Number of dimensions is half the number of points (there 
             * must be an upper and lower bound point in each dimension). */
            final int ndim = xoffs.length / 2;

            /* If there are less than 3 dimensions, it's trivial. */
            if ( ndim < 3 ) {
                return Collections.singletonList( new int[][] { xoffs, yoffs } )
                                  .iterator();
            }

            /* Otherwise work out which pairs of dimensions have non-zero
             * extents. */
            else {
                final boolean[] hasPoints = new boolean[ ndim ];
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
                    return EMPTY_ITERATOR;
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
                    return Collections.singletonList( offPairs ).iterator();
                }

                /* Otherwise return an iterator over non-blank 4-point arrays,
                 * each formed from the coorindates from a pair of
                 * non-blank dimensions. */
                else {
                    assert nActiveDim >= 2;
                    return new Iterator() {
                        int[][] offPairs = new int[ 2 ][ 4 ];
                        boolean done;
                        int iActive = 0;
                        int jActive = 1;
                        public Object next() {
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
     * Null error renderer.  It draws nothing.
     */
    private static class Blank extends ErrorRenderer {

        private final Icon legend_;

        /**
         * Constructor.
         *
         * @param  name   renderer name
         */
        Blank( String name ) {
            super( name );
            legend_ = new EmptyIcon( 0, 0 );
        }

        public Icon getLegendIcon() {
            return legend_;
        }

        public Icon getLegendIcon( ErrorMode[] modes, int width, int height,
                                   int xpad, int ypad ) {
            return new EmptyIcon( width, height );
        }

        public boolean supportsDimensionality( int ndim ) {
            return true;
        }

        public boolean isBlank( ErrorMode[] modes ) {
            return true;
        }

        public void drawErrors( Graphics g, int x, int y, int[] xoffs,
                                int[] yoffs ) {
        }

        public int[] getPixels( Graphics g, int x, int y, int[] xoffs,
                                int[] yoffs ) {
            return NO_PIXELS;
        }

        public Rectangle getBounds( Graphics g, int x, int y, int[] xoffs,
                                    int[] yoffs ) {
            return new Rectangle( x, y, 0, 0 );
        }
    }
}
