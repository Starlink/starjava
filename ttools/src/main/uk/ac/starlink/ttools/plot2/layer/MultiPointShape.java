package uk.ac.starlink.ttools.plot2.layer;

import java.awt.BasicStroke;
import java.awt.Color;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;
import javax.swing.Icon;
import uk.ac.starlink.ttools.plot.ErrorMode;
import uk.ac.starlink.ttools.plot.Matrices;
import uk.ac.starlink.ttools.plot2.Glyph;
import uk.ac.starlink.ttools.plot2.Pixer;

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
    private final int iconDim_;
    private final boolean canThick_;
    private Icon icon_;

    /** Multi-point shape which draws nothing. */
    public static final MultiPointShape NONE = new Blank( "None" );

    /** General purpose multi-point shape. */
    public static final MultiPointShape DEFAULT =
        new CappedLine( "Lines", true, null );

    /** Shape suitable for use in user controls. */
    public static final MultiPointShape EXAMPLE =
        new CappedLine( "Capped Lines", true, new BarCapper( 3 ) );

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
     * @param  iconDim  dimensionality to use for basic icon generation
     * @param  canThick  true iff this shape is available in different
     *                   line thicknesses
     */
    protected MultiPointShape( String name, int iconDim, boolean canThick ) {
        name_ = name;
        iconDim_ = iconDim;
        canThick_ = canThick;
    }

    /**
     * Returns an icon giving a general example of what this shape looks like.
     *
     * @return  example icon
     */
    public Icon getLegendIcon() {
        if ( icon_ == null ) {
            icon_ = new MultiPointIcon( this, iconDim_ );
        }
        return icon_;
    }

    /**
     * Returns an icon giving an example of what this shape looks like in a
     * detailed context.
     *
     * @param   scribe   shape painter
     * @param   modes  array of ErrorModes, one per error dimension (x, y, ...)
     * @param   width  total width of icon
     * @param   height total height of icon
     * @param   xpad   internal horizontal padding of icon
     * @param   ypad   internal vertical padding of icon
     */
    public Icon getLegendIcon( MultiPointScribe scribe, ErrorMode[] modes,
                               int width, int height, int xpad, int ypad ) {
        if ( isPadIcon() ) {
            xpad += width / 6;
            ypad += height / 6;
        }
        return new MultiPointIcon( scribe, modes, width, height, xpad, ypad );
    }

    /**
     * Reports whether this shape can be used in a given dimensionality.
     *
     * @param  ndim  number of error dimensions to be used
     * @return  true iff this object can do rendering
     */
    public abstract boolean supportsDimensionality( int ndim );

    /**
     * Returns an object that can turn offset arrays into painted shapes.
     * Line thickness is single pixel where applicable.
     *
     * @return  shape painter
     */
    abstract MultiPointScribe createBasicScribe();

    /**
     * Returns an object that can turn offset arrays into painted shapes
     * with configurable line thickness.
     * If {@link #canThick} returns false, the parameter makes no difference.
     *
     * @param  nthick  non-negative line thickness, 0 is single-pixel
     * @return  shape painter
     */
    public MultiPointScribe createScribe( int nthick ) {
        MultiPointScribe scribe = createBasicScribe();
        return canThick() && nthick > 0 && scribe instanceof LineScribe
             ? new ThickScribe( (LineScribe) scribe, nthick,
                                getKernel( nthick ), getStrokeKit( nthick ) )
             : scribe;
    }

    /**
     * Indicates whether variants of this shape with different line
     * thicknesses are available.  For some shapes, for instance filled
     * rectangles, the line thickness makes no difference.
     *
     * @return  true iff the argument to {@link #createScribe}
     *          makes any difference
     */
    public boolean canThick() {
        return canThick_;
    }

    /**
     * Returns the smoothing kernel to use for thick lines.
     * The default is reasonable, but it may be overridden.
     *
     * @param  nthick  non-negative line thickness, 0 is single-pixel
     * @return   smoothing kernel
     */
    PixerFactory getKernel( int nthick ) {
        return ( nthick == 1 ? MarkerShape.CROSS : MarkerShape.FILLED_CIRCLE )
              .getStyle( Color.BLACK, nthick ).getPixerFactory();
    }

    /**
     * Returns the set of line strokes to use for thick lines.
     * The default is reasonable, but it may be overridden.
     *
     * @param  nthick  non-negative line thickness, 0 is single-pixel
     * @return  stroke painting kit
     */
    StrokeKit getStrokeKit( int nthick ) {
        return new StrokeKit( 1f + 2 * nthick );
    }

    /**
     * Hacky flag that may be set to add a bit of extra padding for
     * legend icons.
     *
     * @return  true for additional padding; default is false
     */
    boolean isPadIcon() {
        return false;
    }
  
    /**
     * Returns a user-readable name for this shape.
     *
     * @return   shape name
     */
    public String getName() {
        return name_;
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
     * Icon which represents a MultiPointShape in a form suitable for a legend.
     */
    private static class MultiPointIcon implements Icon {

        private final int width_;
        private final int height_;
        private final Glyph glyph_;
        private final int xoff_;
        private final int yoff_;

        /**
         * Constructs an icon with default characteristics.
         *
         * @param  shape   shape
         * @param  ndim    dimensionality
         */
        public MultiPointIcon( MultiPointShape shape, int ndim ) {
            this( shape.createBasicScribe(),
                  fillModeArray( ndim, ErrorMode.SYMMETRIC ),
                  LEGEND_WIDTH, LEGEND_HEIGHT, LEGEND_XPAD, LEGEND_YPAD );
        }

        /**
         * Constructs an icon with specified characteristics.
         *
         * @param   scribe shape painter
         * @param   modes  array of ErrorModes, one per error dimension
         * @param   width  total width of icon
         * @param   height total height of icon
         * @param   xpad   internal horizontal padding of icon
         * @param   ypad   internal vertical padding of icon
         */
        public MultiPointIcon( MultiPointScribe scribe, ErrorMode[] modes,
                               int width, int height, int xpad, int ypad ) {
            width_ = width;
            height_ = height;
            int w2 = width / 2 - xpad;
            int h2 = height / 2 - ypad;
            int ndim = modes.length;
            List<Point> offList = new ArrayList<>( ndim );
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
            int[] xoffs = new int[ np ];
            int[] yoffs = new int[ np ];
            for ( int ip = 0; ip < np; ip++ ) {
                Point point = offList.get( ip );
                xoffs[ ip ] = point.x;
                yoffs[ ip ] = point.y;
            }
            glyph_ = scribe.createGlyph( xoffs, yoffs );
            xoff_ = width / 2;
            yoff_ = height / 2;
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
            g2.translate( x + xoff_, y + yoff_ );
            glyph_.paintGlyph( g2 );
            g2.translate( - ( x + xoff_ ), - ( y + yoff_ ) );
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

    /**
     * MultiPointScribe implementation whose createGlyph method
     * returns a LineGlyph.
     * A lambda is supplied at construction time to provide the implemenation.
     */
    private static class LineScribe implements MultiPointScribe {
        private final MultiPointShape shape_;
        private final BiFunction<int[],int[],LineGlyph> createGlyph_;

        /**
         * Constructor.
         *
         * @param  shape   basic shape
         * @param  createGlyph  function corresponding to
         *                      {@link MultiPointScribe#createGlyph}
         */
        LineScribe( MultiPointShape shape,
                    BiFunction<int[],int[],LineGlyph> createGlyph ) {
            shape_ = shape;
            createGlyph_ = createGlyph;
        }

        public LineGlyph createGlyph( int[] xoffs, int[] yoffs ) {
            return createGlyph_.apply( xoffs, yoffs );
        }

        @Override
        public int hashCode() {
            return shape_.hashCode();
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof LineScribe ) {
                LineScribe other = (LineScribe) o;
                return this.shape_.equals( other.shape_ );
            }
            else {
                return false;
            }
        }
    }

    /**
     * MultiPointScribe implementation for painting shapes with thick lines.
     */
    private static class ThickScribe implements MultiPointScribe {

        private final LineScribe baseScribe_;
        private final int nthick_;
        private final PixerFactory kernel_;
        private final StrokeKit strokeKit_;

        /**
         * Constructor.
         *
         * @param  baseScribe  draws basic shape
         * @param  nthick  nominal line thickness for object identification
         * @param  kernel   smoothing kernel for pixel drawings
         * @param  strokeKit  line painting strokes
         */
        ThickScribe( LineScribe baseScribe, int nthick,
                     PixerFactory kernel, StrokeKit strokeKit ) {
            baseScribe_ = baseScribe;
            nthick_ = nthick;
            kernel_ = kernel;
            strokeKit_ = strokeKit;
        }

        public Glyph createGlyph( int[] xoffs, int[] yoffs ) {
            return baseScribe_.createGlyph( xoffs, yoffs )
                              .toThicker( kernel_, strokeKit_ );
        }

        @Override
        public int hashCode() {
            int code = 92342;
            code = 23 * code + baseScribe_.hashCode();
            code = 23 * code + nthick_;
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof ThickScribe ) {
                ThickScribe other = (ThickScribe) o;
                return this.baseScribe_.equals( other.baseScribe_ )
                    && this.nthick_ == other.nthick_;
            }
            else {
                return false;
            }
        }
    }

    /**
     * Null shape.  It draws nothing.
     */
    private static class Blank extends MultiPointShape {

        private final MultiPointScribe scribe_;

        /**
         * Constructor.
         *
         * @param  name   shape name
         */
        Blank( String name ) {
            super( name, 1, false );
            final Glyph glyph = new Glyph() {
                public Pixer createPixer( Rectangle clip ) {
                    return null;
                }
                public void paintGlyph( Graphics g ) {
                }
            };
            scribe_ = new MultiPointScribe() {
                public Glyph createGlyph( int[] xoffs, int[] yoffs ) {
                    return glyph;
                }
            };
        }

        public boolean supportsDimensionality( int ndim ) {
            return true;
        }

        public MultiPointScribe createBasicScribe() {
            return scribe_;
        }
    }

    /**
     * Shape which draws an (optional) line from the data point
     * to the given offset, and an (optional) cap normal to that line
     * at its furthest extent.  Works for any dimensionality.
     */
    private static class CappedLine extends MultiPointShape {

        private final boolean hasLine_;
        private final Capper capper_;

        /**
         * Constructor.
         *
         * @param  name   shape name
         * @param  lines   true iff you want error lines drawn
         * @param  capper  if non-null causes caps to be drawn at end of lines
         */
        CappedLine( String name, boolean hasLine, Capper capper ) {
            super( name, 2, true );
            hasLine_ = hasLine;
            capper_ = capper;
        }

        public boolean supportsDimensionality( int ndim ) {
            return true;
        }

        public MultiPointScribe createBasicScribe() {
            return new LineScribe( this, (xoffs, yoffs) -> new LineGlyph() {
                public Rectangle getPixelBounds() {
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
                    return box;
                }
                public void paintGlyph( Graphics g, StrokeKit strokeKit ) {
                    drawCappedLine( g, xoffs, yoffs, hasLine_, capper_, false,
                                    strokeKit );
                }
                public void drawShape( PixelDrawing drawing ) {
                    int np = xoffs.length;
                    for ( int ip = 0; ip < np; ip++ ) {
                        int xoff = xoffs[ ip ];
                        int yoff = yoffs[ ip ];
                        if ( xoff != 0 || yoff != 0 ) {
                            if ( hasLine_ ) {
                                drawing.drawLine( 0, 0, xoff, yoff );
                            }
                            if ( capper_ != null ) {
                                capper_.drawCap( drawing, xoff, yoff );
                            }
                        }
                    }
                }
            } );
         }

        /**
         * Does the work for drawing a CappedLine to a graphics context.
         *
         * @param  g  graphics context
         * @param  xoffs  X coordinates of shape points
         * @param  yoffs  Y coordinates of shape points
         * @param  hasLine  whether to draw lines
         * @param  capper   cap drawing object, if any
         * @param  willCover  true if the ends of the radial lines will
         *                    subsequently be covered by more drawing
         *                    (affects line capping)
         * @param  strokeKit  line painting stroke supplier
         */
        private static void drawCappedLine( Graphics g,
                                            int[] xoffs, int[] yoffs,
                                            boolean hasLine, Capper capper,
                                            boolean willCover,
                                            StrokeKit strokeKit ) {
            Graphics2D g2 = (Graphics2D) g;
            Stroke oldStroke = g2.getStroke();
            g2.setStroke( capper != null || willCover ? strokeKit.getButt()
                                                      : strokeKit.getRound() );
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
                        g.drawLine( 0, 0, xoff, yoff );
                    }

                     /* Draw cap if required. */
                    if ( capper != null && ! clipped ) {
                        g2.setStroke( strokeKit.getRound() );

                        /* For rectilinear offsets, draw the cap manually. */
                        if ( xoff == 0 ) {
                            capper.drawCapY( g2, yoff );
                        }
                        else if ( yoff == 0 ) {
                            capper.drawCapX( g2, xoff );
                        }

                        /* For more general offsets, transform the graphics
                         * context so that we can draw the cap along an axis.
                         * This is better than calculating the position in
                         * the original orientation because that would require
                         * integer rounding (at least in antialiased contexts
                         * the difference may be visible). */
                        else {
                            AffineTransform oldTransform = g2.getTransform();
                            g2.rotate( Math.atan2( yoff, xoff ) );
                            double l2 = xoff * xoff + yoff * yoff;
                            int leng = (int) Math.round( Math.sqrt( l2 ) );
                            capper.drawCapX( g2, leng );
                            g2.setTransform( oldTransform );
                        }
                    }
                }
            }
            g2.setStroke( oldStroke );
        }
    }

    /**
     * Shape which draws an isosceles triangle with a fixed-length
     * base centered at the data point and a point at the offset point.
     * Works for any dimensionality.
     */
    private static class Dart extends MultiPointShape {

        private final boolean isFill_;
        private final int basepix_;
        private final int[] vys_;

        /**
         * Constructor.
         *
         * @param  name  shape name
         * @param  isFill  true for a filled triangle, false for open
         * @param  basepix  half-length of triangle base in pixels
         */
        public Dart( String name, boolean isFill, int basepix ) {
            super( name, 2, !isFill );
            isFill_ = isFill;
            basepix_ = basepix;
            vys_ = new int[] { basepix_, 0, -basepix_ };
        }

        public boolean supportsDimensionality( int ndim ) {
            return ndim > 0;
        }

        public MultiPointScribe createBasicScribe() {
            return new LineScribe( this, (xoffs, yoffs) -> new LineGlyph() {
                int np = xoffs.length;
                public Rectangle getPixelBounds() {
                    Rectangle box = new Rectangle();
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
                    return box;
                }
                public void drawShape( PixelDrawing drawing ) {
                    for ( int ip = 0; ip < np; ip++ ) {
                        int xoff = xoffs[ ip ];
                        int yoff = yoffs[ ip ];
                        if ( xoff != 0 || yoff != 0 ) {
                            Point bp = getBaseVertex( xoff, yoff );
                            int x1 = xoff;
                            int y1 = yoff;
                            int x2 = + bp.x;
                            int y2 = + bp.y;
                            int x3 = - bp.x;
                            int y3 = - bp.y;
                            if ( isFill_ ) {
                                drawing.fillPolygon( new int[] { x1, x2, x3 },
                                                     new int[] { y1, y2, y3 },
                                                     3 );
                            }
                            else {
                                drawing.drawLine( x1, y1, x2, y2 );
                                drawing.drawLine( x2, y2, x3, y3 );
                                drawing.drawLine( x3, y3, x1, y1 );
                            }
                        }
                    }
                }
                public void paintGlyph( Graphics g, StrokeKit strokeKit ) {
                    Rectangle clip = g.getClipBounds();
                    Graphics2D g2 = (Graphics2D) g;
                    Stroke stroke0 = g2.getStroke();
                    g2.setStroke( strokeKit.getRound() );
                    Dimension size = getApproxGraphicsSize( g2 );
                    double dmax = Math.max( size.width, size.height );
                    int np = xoffs.length;
                    for ( int ip = 0; ip < np; ip++ ) {
                        double dx = xoffs[ ip ];
                        double dy = yoffs[ ip ];
                        if ( dx != 0 || dy != 0 ) {
                            double dleng = Math.min( Math.hypot( dx, dy ),
                                                     dmax );
                            AffineTransform trans0 = g2.getTransform();
                            g2.rotate( Math.atan2( dy, dx ) );
                            int[] xs = { 0, (int) Math.round( dleng ), 0 };
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
            } );
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
     * Shape which renders an isosceles triangle, centered on the data point,
     * with a variable-length base.
     * Used like an ellipse/rectangle/oblong (2d only).
     */
    private static class Triangle extends MultiPointShape {

        private final boolean isFill_;

        /**
         * Constructor.
         *
         * @param  name  shape name
         * @param  isFill  true for a filled triangle, false for open
         */
        public Triangle( String name, boolean isFill ) {
            super( name, 2, !isFill );
            isFill_ = isFill;
        }

        public boolean supportsDimensionality( int ndim ) {
            return ndim == 2;
        }

        public MultiPointScribe createBasicScribe() {
            return new LineScribe( this, (xoffs, yoffs) -> new LineGlyph() {
                final Polygon triangle = getTriangle( xoffs, yoffs );
                public Rectangle getPixelBounds() {
                    return triangle.getBounds();
                }
                public void drawShape( PixelDrawing drawing ) {
                    int[] xs = triangle.xpoints;
                    int[] ys = triangle.ypoints;
                    if ( isFill_ ) {
                        drawing.fillPolygon( xs, ys, 3 );
                    }
                    else {
                        drawing.drawLine( xs[1], ys[1], xs[2], ys[2] );
                        drawing.drawLine( xs[2], ys[2], xs[0], ys[0] );
                        drawing.drawLine( xs[0], ys[0], xs[1], ys[1] );
                    }
                }
                public void paintGlyph( Graphics g, StrokeKit strokeKit ) {
                    Dimension size =
                        getApproxGraphicsSize( (Graphics2D) g );
                    int dmax = Math.max( size.width, size.height );
                    boolean ok = true;
                    for ( int ip = 0; ip < 4 && ok; ip++ ) {
                        ok = ok && Math.abs( xoffs[ ip ] ) < dmax
                                && Math.abs( yoffs[ ip ] ) < dmax;
                    }
                    final Polygon tri;
                    if ( ok ) {
                        tri = triangle;
                    }
                    else {
                        int[] xoffs1 = xoffs.clone();
                        int[] yoffs1 = yoffs.clone();
                        for ( int ip = 0; ip < 4; ip++ ) {
                            xoffs1[ ip ] =
                                Math.min( dmax, Math.max( -dmax, xoffs[ ip ] ));
                            yoffs1[ ip ] =
                                Math.min( dmax, Math.max( -dmax, yoffs[ ip ] ));
                        }
                        tri = getTriangle( xoffs1, yoffs1 );
                    }
                    if ( isFill_ ) {
                        g.fillPolygon( tri );
                    }
                    else {
                        Graphics2D g2 = (Graphics2D) g;
                        Stroke stroke0 = g2.getStroke();
                        g2.setStroke( strokeKit.getRound() );
                        g.drawPolygon( tri );
                        g2.setStroke( stroke0 );
                    }
                }
            } );
        }

        /**
         * Returns a polygon representing the required triangle.
         *
         * @param  xoffs  offset point X coordinates (xlo, ylo, xhi, yhi)
         * @param  yoffs  offset point Y coordinates (xlo, ylo, xhi, yhi)
         */
        private static Polygon getTriangle( int[] xoffs, int[] yoffs ) {
            int[] xs = new int[ 3 ];
            int[] ys = new int[ 3 ];
            xs[ 0 ] = xoffs[ 1 ];
            ys[ 0 ] = yoffs[ 1 ];
            xs[ 1 ] = xoffs[ 0 ] + xoffs[ 2 ];
            ys[ 1 ] = yoffs[ 0 ] + yoffs[ 2 ];
            xs[ 2 ] = xoffs[ 0 ] + xoffs[ 3 ];
            ys[ 2 ] = yoffs[ 0 ] + yoffs[ 3 ];
            return new Polygon( xs, ys, 3 );
        }
    }

    /**
     * Generic abstract shape for cases in which the drawn object is
     * effectively a rectangle of some sort.  Concrete subclasses
     * must implement {@link #drawOblong} to mark the space as appropriate.
     * Only works properly for two-dimensional errors.
     */
    private static abstract class Oblong extends MultiPointShape {

        private final boolean withLines_;

        /**
         * Constructor.
         *
         * @param  name   shape name
         * @param  withLines  true iff you want a crosshair drawn as well as
         *         the basic representation of this shape
         * @param  canThick  true iff this basic shape (without lines)
         *                   is available in different line thicknesses
         */
        Oblong( String name, boolean withLines, boolean canThick ) {
            super( name, 2, withLines || canThick );
            withLines_ = withLines;
        }

        public boolean supportsDimensionality( int ndim ) {
            return ndim == 2;
        }

        /**
         * This abstract class is overridden to return an
         * implementation-specific MultiPointScribe subclass,
         * because it can be, and because it's useful elsewhere.
         */
        @Override
        public abstract LineScribe createBasicScribe();
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
            super( name, withLines, true );
            withLines_ = withLines;
        }

        public LineScribe createBasicScribe() {
            return new LineScribe( this, (xoffs, yoffs) ->
                    new OblongGlyph( xoffs, yoffs, withLines_ ) {
                void paintOblong( Graphics g, int x, int y,
                                  int width, int height ) {
                    g.drawOval( x, y, width, height );
                }
                public void drawShape( PixelDrawing drawing ) {
                    if ( xoffs.length != 4 || yoffs.length != 4 ) {
                        return;
                    }
                    else if ( yoffs[ 0 ] == 0 && yoffs[ 1 ] == 0 &&
                              xoffs[ 2 ] == 0 && xoffs[ 3 ] == 0 ) {
                        int xlo = Math.min( xoffs[ 0 ], xoffs[ 1 ] );
                        int xhi = Math.max( xoffs[ 0 ], xoffs[ 1 ] );
                        int ylo = Math.min( yoffs[ 2 ], yoffs[ 3 ] );
                        int yhi = Math.max( yoffs[ 2 ], yoffs[ 3 ] );
                        int width = xhi - xlo;
                        int height = yhi - ylo;
                        drawing.drawOval( xlo, ylo, width, height );
                    }
                    else {
                        int ax = ( xoffs[ 1 ] - xoffs[ 0 ] ) / 2;
                        int ay = ( yoffs[ 1 ] - yoffs[ 0 ] ) / 2;
                        int bx = ( xoffs[ 3 ] - xoffs[ 2 ] ) / 2;
                        int by = ( yoffs[ 3 ] - yoffs[ 2 ] ) / 2;
                        int x0 = Math.round( ( xoffs[ 0 ] + xoffs[ 1 ]
                                             + xoffs[ 2 ] + xoffs[ 3 ] ) / 2f );
                        int y0 = Math.round( ( yoffs[ 0 ] + yoffs[ 1 ]
                                             + yoffs[ 2 ] + yoffs[ 3 ] ) / 2f );
                        drawing.drawEllipse( x0, y0, ax, ay, bx, by );
                    }
                    if ( withLines_ ) {
                        for ( int i = 0; i < 4; i++ ) {
                            drawing.drawLine( 0, 0, xoffs[ i ], yoffs[ i ] );
                        }
                    }
                }
            } );
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
            super( name, false, false );
        }

        public LineScribe createBasicScribe() {
            return new LineScribe( this, (xoffs, yoffs) ->
                    new OblongGlyph( xoffs, yoffs, false ) {
                void paintOblong( Graphics g, int x, int y,
                                  int width, int height ) {
                    g.fillOval( x, y, width, height );
                }
                public void drawShape( PixelDrawing drawing ) {
                    if ( xoffs.length != 4 || yoffs.length != 4 ) {
                        return;
                    }
                    else if ( yoffs[ 0 ] == 0 && yoffs[ 1 ] == 0 &&
                              xoffs[ 2 ] == 0 && xoffs[ 3 ] == 0 ) {
                        int xlo = Math.min( xoffs[ 0 ], xoffs[ 1 ] );
                        int xhi = Math.max( xoffs[ 0 ], xoffs[ 1 ] );
                        int ylo = Math.min( yoffs[ 2 ], yoffs[ 3 ] );
                        int yhi = Math.max( yoffs[ 2 ], yoffs[ 3 ] );
                        int width = xhi - xlo;
                        int height = yhi - ylo;
                        drawing.fillOval( xlo, ylo, width, height );
                    }
                    else {
                        int ax = ( xoffs[ 1 ] - xoffs[ 0 ] ) / 2;
                        int ay = ( yoffs[ 1 ] - yoffs[ 0 ] ) / 2;
                        int bx = ( xoffs[ 3 ] - xoffs[ 2 ] ) / 2;
                        int by = ( yoffs[ 3 ] - yoffs[ 2 ] ) / 2;
                        int x0 = Math.round( ( xoffs[ 0 ] + xoffs[ 1 ]
                                             + xoffs[ 2 ] + xoffs[ 3 ] ) / 2f );
                        int y0 = Math.round( ( yoffs[ 0 ] + yoffs[ 1 ]
                                             + yoffs[ 2 ] + yoffs[ 3 ] ) / 2f );
                        drawing.fillEllipse( x0, y0, ax, ay, bx, by );
                    }
                }
            } );
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
            super( name, withLines, true );
            withLines_ = withLines;
        }

        public LineScribe createBasicScribe() {
            return new LineScribe( this, (xoffs, yoffs) -> 
                    new OblongGlyph( xoffs, yoffs, withLines_ ) {
                void paintOblong( Graphics g, int x, int y,
                                  int width, int height ) {
                    g.drawRect( x, y, width, height );
                }
                public void drawShape( PixelDrawing drawing ) {
                    if ( xoffs.length != 4 || yoffs.length != 4 ) {
                        return;
                    }
                    int xa = xoffs[ 0 ] + xoffs[ 2 ];
                    int xb = xoffs[ 0 ] + xoffs[ 3 ];
                    int xc = xoffs[ 1 ] + xoffs[ 3 ];
                    int xd = xoffs[ 1 ] + xoffs[ 2 ];
                    int ya = yoffs[ 0 ] + yoffs[ 2 ];
                    int yb = yoffs[ 0 ] + yoffs[ 3 ];
                    int yc = yoffs[ 1 ] + yoffs[ 3 ];
                    int yd = yoffs[ 1 ] + yoffs[ 2 ];
                    drawing.drawLine( xa, ya, xb, yb );
                    drawing.drawLine( xb, yb, xc, yc );
                    drawing.drawLine( xc, yc, xd, yd );
                    drawing.drawLine( xd, yd, xa, ya );
                    if ( withLines_ ) {
                        for ( int i = 0; i < 4; i++ ) {
                            drawing.drawLine( 0, 0, xoffs[ i ], yoffs[ i ] );
                        }
                    }
                }
            } );
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
            super( name, false, false );
        }

        public LineScribe createBasicScribe() {
            return new LineScribe( this, (xoffs, yoffs) ->
                    new OblongGlyph( xoffs, yoffs, false ) {
                void paintOblong( Graphics g, int x, int y,
                                  int width, int height ) {
                    g.fillRect( x, y, width, height );
                }
                public void drawShape( PixelDrawing drawing ) {
                   if ( xoffs.length != 4 || yoffs.length != 4 ) {
                        return;
                    }
                    else if ( yoffs[ 0 ] == 0 && yoffs[ 1 ] == 0 &&
                              xoffs[ 2 ] == 0 && xoffs[ 3 ] == 0 ) {
                        int xlo = Math.min( xoffs[ 0 ], xoffs[ 1 ] );
                        int xhi = Math.max( xoffs[ 0 ], xoffs[ 1 ] );
                        int ylo = Math.min( yoffs[ 2 ], yoffs[ 3 ] );
                        int yhi = Math.max( yoffs[ 2 ], yoffs[ 3 ] );
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
                        drawing.fillPolygon( xof, yof, 4 );
                    }
                }
            } );
        }
    }

    /**
     * Shape which draws a wire-net line/rectangle/cuboid in 1/2/3 dimensions.
     */
    private static class OpenCuboid extends MultiPointShape {

        /**
         * Constructor.
         *
         * @param  name  shape name
         */
        OpenCuboid( String name ) {
            super( name, 3, true );
        }

        @Override
        boolean isPadIcon() {
            return true;
        }

        public boolean supportsDimensionality( int ndim ) {
            return ndim == 3;
        }

        public LineScribe createBasicScribe() {
            return new LineScribe( this, (xoffs, yoffs) -> new LineGlyph() {
                public Rectangle getPixelBounds() {
                    LineBounder bounder = new LineBounder();
                    doLines( bounder );
                    int width = bounder.xhi_ - bounder.xlo_;
                    int height = bounder.yhi_ - bounder.ylo_;
                    if ( width > 0 || height > 0 ) {
                        width++;
                        height++;
                    }
                    return new Rectangle( bounder.xlo_, bounder.ylo_,
                                          width, height );
                }
                public void drawShape( PixelDrawing drawing ) {
                    doLines( (x1, y1, x2, y2) ->
                                  drawing.drawLine( x1, y1, x2, y2 ) );
                }
                public void paintGlyph( Graphics g, StrokeKit strokeKit ) {
                    Graphics2D g2 = (Graphics2D) g;
                    Stroke stroke0 = g2.getStroke();
                    g2.setStroke( strokeKit.getRound() );
                    doLines( (x1, y1, x2, y2) -> g.drawLine( x1, y1, x2, y2 ) );
                    g2.setStroke( stroke0 );
                }
                void doLines( LineConsumer liner ) {
                    doCuboidLines( xoffs, yoffs, liner );
                }
            } );
        }

        /**
         * Presents the coordinates of lines drawn by this shape
         * to a given callback.
         *
         * @param  xoffs  X coordinates of shape limit offsets from (x,y)
         * @param  yoffs  Y coordinates of shape limit offsets from (x,y)
         * @param  liner  destination for line start/end coordinates
         */
        private static void doCuboidLines( int[] xoffs, int[] yoffs,
                                           LineConsumer liner ) {
            int ndim = xoffs.length / 2;
            if ( ndim == 1 ) {
                liner.line( xoffs[ 0 ], yoffs[ 0 ], xoffs[ 1 ], yoffs[ 1 ] );
            }
            else if ( ndim == 2 ) {
                int x00 = xoffs[ 0 ] + xoffs[ 2 ];
                int x01 = xoffs[ 0 ] + xoffs[ 3 ];
                int x11 = xoffs[ 1 ] + xoffs[ 3 ];
                int x10 = xoffs[ 1 ] + xoffs[ 2 ];
                int y00 = yoffs[ 0 ] + yoffs[ 2 ];
                int y01 = yoffs[ 0 ] + yoffs[ 3 ];
                int y11 = yoffs[ 1 ] + yoffs[ 3 ];
                int y10 = yoffs[ 1 ] + yoffs[ 2 ];
                liner.line( x00, y00, x01, y01 );
                liner.line( x01, y01, x11, y11 );
                liner.line( x11, y11, x10, y10 );
                liner.line( x10, y10, x00, y00 );
            }
            else if ( ndim == 3 ) {
                int x000 = xoffs[ 0 ] + xoffs[ 2 ] + xoffs[ 4 ];
                int x001 = xoffs[ 0 ] + xoffs[ 2 ] + xoffs[ 5 ];
                int x010 = xoffs[ 0 ] + xoffs[ 3 ] + xoffs[ 4 ];
                int x011 = xoffs[ 0 ] + xoffs[ 3 ] + xoffs[ 5 ];
                int x100 = xoffs[ 1 ] + xoffs[ 2 ] + xoffs[ 4 ];
                int x101 = xoffs[ 1 ] + xoffs[ 2 ] + xoffs[ 5 ];
                int x110 = xoffs[ 1 ] + xoffs[ 3 ] + xoffs[ 4 ];
                int x111 = xoffs[ 1 ] + xoffs[ 3 ] + xoffs[ 5 ];
                int y000 = yoffs[ 0 ] + yoffs[ 2 ] + yoffs[ 4 ];
                int y001 = yoffs[ 0 ] + yoffs[ 2 ] + yoffs[ 5 ];
                int y010 = yoffs[ 0 ] + yoffs[ 3 ] + yoffs[ 4 ];
                int y011 = yoffs[ 0 ] + yoffs[ 3 ] + yoffs[ 5 ];
                int y100 = yoffs[ 1 ] + yoffs[ 2 ] + yoffs[ 4 ];
                int y101 = yoffs[ 1 ] + yoffs[ 2 ] + yoffs[ 5 ];
                int y110 = yoffs[ 1 ] + yoffs[ 3 ] + yoffs[ 4 ];
                int y111 = yoffs[ 1 ] + yoffs[ 3 ] + yoffs[ 5 ];
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
    private static class MultiPlaneShape extends MultiPointShape {

        private final Oblong shape2d_;

        /**
         * Constructor.
         *
         * @param  shape2d  2-dimensional shape on which this one is based;
         *         the name is taken from this
         */
        MultiPlaneShape( Oblong shape2d ) {
            super( shape2d.getName(), 3, shape2d.canThick() );
            shape2d_ = shape2d;
        }

        public boolean supportsDimensionality( int ndim ) {
            return ndim == 3
                || ( ndim < 3 && shape2d_.supportsDimensionality( ndim ) );
        }

        @Override
        boolean isPadIcon() {
            return true;
        }

        public MultiPointScribe createBasicScribe() {
            LineScribe scribe2d = shape2d_.createBasicScribe();
            return new LineScribe( this, (xoffs, yoffs) -> {
                Iterable<int[][]> offsets = get2dOffsets( xoffs, yoffs );
                final List<LineGlyph> subGlyphs = new ArrayList<>();
                for ( int[][] offs  : get2dOffsets( xoffs, yoffs ) ) {
                    subGlyphs.add( scribe2d
                                  .createGlyph( offs[ 0 ], offs[ 1 ] ) );
                }
                return new LineGlyph() {
                    public Rectangle getPixelBounds() {
                        Rectangle bounds = new Rectangle( 0, 0, 0, 0 );
                        for ( LineGlyph subgl : subGlyphs ) {
                            bounds.add( subgl.getPixelBounds() );
                        }
                        return bounds;
                    }
                    public void drawShape( PixelDrawing drawing ) {
                        for ( LineGlyph subgl : subGlyphs ) {
                            subgl.drawShape( drawing );
                        }
                    }
                    public void paintGlyph( Graphics g, StrokeKit strokeKit ) {
                        for ( LineGlyph subgl : subGlyphs ) {
                            subgl.paintGlyph( g, strokeKit );
                        }
                    }
                };
            } );
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
        private static Iterable<int[][]> get2dOffsets( final int[] xoffs,
                                                       final int[] yoffs ) {

            /* Number of dimensions is half the number of points (there
             * must be an upper and lower bound point in each dimension). */
            final int ndim = xoffs.length / 2;

            /* If there are fewer than 3 dimensions, it's trivial. */
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
                         xoffs[ i2 + 1 ] != 0 || yoffs[ i2 + 1 ] != 0 ) {
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
                 * each formed from the coordinates from a pair of
                 * non-blank dimensions. */
                else {
                    assert nActiveDim >= 2;
                    return () -> new Iterator<int[][]>() {
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
                            int[][] offPairs = new int[ 2 ][ 4 ];
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
     * Defines how caps are drawn on the end of error bar-type lines.
     */
    private interface Capper {

        /**
         * Draws a cap on a horizontal error bar in a graphics context.
         *
         * @param   g  graphics context
         * @param   xoff  X offset of the end of the error bar
         */
        void drawCapX( Graphics g, int xoff );

        /**
         * Draws a cap on a vertical error bar in a graphics context.
         *
         * @param   g  graphics context
         * @param   yoff  Y offset of the end of the error bar
         */
        void drawCapY( Graphics g, int yoff );

        /**
         * Draws a cap on an error bar in a pixel-mapped drawing.
         *
         * @param  drawing  pixel map
         * @param  xoff  X offset of the end of the error bar
         * @param  yoff  Y offset of the end of the error bar
         */
        void drawCap( PixelDrawing drawing, int xoff, int yoff );

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

        public void drawCapX( Graphics g, int xoff ) {
            g.drawLine( xoff, - capsize_, xoff, + capsize_ );
        }

        public void drawCapY( Graphics g, int yoff ) {
            g.drawLine( - capsize_, yoff, + capsize_, yoff );
        }

        public void drawCap( PixelDrawing drawing, int xoff, int yoff ) {
            if ( xoff == 0 ) {
                drawing.drawLine( - capsize_, yoff, + capsize_, yoff );
            }
            else if ( yoff == 0 ) {
                drawing.drawLine( xoff, - capsize_, xoff, + capsize_ );
            }
            else {
                int x0 = xoff;
                int y0 = yoff;
                double r1 = Math.sqrt( xoff * xoff + yoff * yoff );
                double capfact = capsize_ / r1;
                int x1 = (int) Math.round( - capfact * yoff );
                int y1 = (int) Math.round( + capfact * xoff );
                drawing.drawLine( x0 - x1, y0 - y1, x0 + x1, y0 + y1 );
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

        public void drawCapX( Graphics g, int xoff ) {
            int sign = xoff > 0 ? +1 : -1;
            int size = Math.min( capsize_, sign * xoff );
            int xstart = xoff - sign * size;
            xs_[ 0 ] = xstart;
            ys_[ 0 ] = - size;
            xs_[ 1 ] = + xoff;
            ys_[ 1 ] = 0;
            xs_[ 2 ] = xstart;
            ys_[ 2 ] = + size;
            g.drawPolyline( xs_, ys_, 3 );
        }

        public void drawCapY( Graphics g, int yoff ) {
            int sign = yoff > 0 ? +1 : -1;
            int size = Math.min( capsize_, sign * yoff );
            int ystart = yoff - sign * size;
            xs_[ 0 ] = - size;
            ys_[ 0 ] = ystart;
            xs_[ 1 ] = 0;
            ys_[ 1 ] = + yoff;
            xs_[ 2 ] = + size;
            ys_[ 2 ] = ystart;
            g.drawPolyline( xs_, ys_, 3 );
        }

        public void drawCap( PixelDrawing drawing, int xoff, int yoff ) {
            if ( xoff == 0 ) {
                int sign = yoff > 0 ? +1 : -1;
                int size = Math.min( capsize_, sign * yoff );
                int ystart = yoff - sign * size;
                drawing.drawLine( 0, yoff, - size, ystart );
                drawing.drawLine( 0, yoff, + size, ystart );
            }
            else if ( yoff == 0 ) {
                int sign = xoff > 0 ? +1 : -1;
                int size = Math.min( capsize_, sign * xoff );
                int xstart = xoff - sign * size;
                drawing.drawLine( xoff, 0, xstart, - size );
                drawing.drawLine( xoff, 0, xstart, + size );
            }
            else {
                double r1 = Math.sqrt( xoff * xoff + yoff * yoff );
                double size = Math.min( capsize_, r1 );
                double capfact = size / r1;
                int ax = xoff + (int) Math.round( capfact * ( - xoff + yoff ) );
                int ay = yoff + (int) Math.round( capfact * ( - xoff - yoff ) );
                int bx = xoff + (int) Math.round( capfact * ( - xoff - yoff ) );
                int by = yoff + (int) Math.round( capfact * ( + xoff - yoff ) );
                drawing.drawLine( xoff, yoff, ax, ay );
                drawing.drawLine( xoff, yoff, bx, by );
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
     * Partial Glyph implementation for Oblong shapes.
     */
    private static abstract class OblongGlyph extends LineGlyph {

        final int[] xoffs_;
        final int[] yoffs_;
        final boolean withLines_;

        /**
         * Constructor.
         *
         * @param  xoffs  X offsets of radius end points
         * @param  yoffs  Y offsets of radius end points
         * @param  withLines  true to include crosshairs as well as outline
         */
        OblongGlyph( int[] xoffs, int[] yoffs, boolean withLines ) {
            xoffs_ = xoffs;
            yoffs_ = yoffs;
            withLines_ = withLines;
        }

        public void paintGlyph( Graphics g, StrokeKit strokeKit ) {
            Graphics2D g2 = (Graphics2D) g;
            Stroke stroke0 = g2.getStroke();
            g2.setStroke( strokeKit.getRound() );
            int noff = xoffs_.length;

            /* Restrict the offsets to something sensible, to prevent the
             * graphics system attempting to fill an ellipse with a
             * kilometre semi-major axis.  This may result in some
             * distortions for ellipses - too bad. */
            Dimension size = getApproxGraphicsSize( g2 );
            int maxcoord = Math.max( size.width, size.height );
            boolean clipped = false;
            for ( int ioff = 0; ioff < noff && ! clipped; ioff++ ) {
                int xoff = xoffs_[ ioff ];
                int yoff = yoffs_[ ioff ];
                clipped = clipped || xoff < - maxcoord || xoff > + maxcoord
                                  || yoff < - maxcoord || yoff > + maxcoord;
            }
            final int[] xoffs;
            final int[] yoffs;
            if ( clipped ) {
                xoffs = new int[ noff ];
                yoffs = new int[ noff ];
                for ( int ioff = 0; ioff < noff; ioff++ ) {
                    xoffs[ ioff ] =
                        Math.max( - maxcoord,
                                  Math.min( + maxcoord, xoffs[ ioff ] ) );
                    yoffs[ ioff ] =
                        Math.max( - maxcoord,
                                  Math.min( + maxcoord, yoffs[ ioff ] ) );
                }
            }
            else {
                xoffs = xoffs_;
                yoffs = yoffs_;
            }

            /* If there are only 1-dimensional bounds, just draw a line.
             * Actually, we don't claim to support dimensionality other than 2
             * here, so this is probably never used. */
            if ( noff == 2 ) {
                g.drawLine( xoffs[ 0 ], yoffs[ 0 ], xoffs[ 1 ], yoffs[ 1 ] );
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
                    paintOblong( g, xlo, ylo, width, height );
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
                        xo[1] + xo[2], xo[0] + xo[3], xo[0] + xo[2],
                        yo[1] + yo[2], yo[0] + yo[3], yo[0] + yo[2],
                        1,             1,             1,
                    };
                    double[] m3 = Matrices.mmMult( m2, Matrices.invert( m1 ) );
                    AffineTransform trans =
                        new AffineTransform( m3[ 0 ], m3[ 3 ],
                                             m3[ 1 ], m3[ 4 ],
                                             m3[ 2 ], m3[ 5 ] );
                    if ( trans.getDeterminant() != 0 ) {
                        AffineTransform oldTrans = g2.getTransform();
                        g2.transform( trans );
                        paintOblong( g2, 0, 0,
                                    (int) Math.round( width ),
                                    (int) Math.round( height ) );
                        g2.setTransform( oldTrans );
                    }
                }
            }

            /* Draw crosshair if required. */
            if ( withLines_ ) {
                CappedLine.drawCappedLine( g, xoffs, yoffs, true, null, true,
                                           strokeKit );
            }
            g2.setStroke( stroke0 );
        }

        public Rectangle getPixelBounds() {
            int xmin = 0;
            int xmax = 0;
            int ymin = 0;
            int ymax = 0;
            int rmax = 0;
            int np = xoffs_.length;
            for ( int ip = 0; ip < np; ip++ ) {
                int xoff = xoffs_[ ip ];
                int yoff = yoffs_[ ip ];
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
            return new Rectangle( xmin, ymin,
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
        abstract void paintOblong( Graphics g, int x, int y,
                                   int width, int height );
    }
}
