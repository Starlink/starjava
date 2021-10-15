package uk.ac.starlink.ttools.plot2.layer;

import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Stroke;
import uk.ac.starlink.ttools.plot2.Equality;
import uk.ac.starlink.ttools.plot2.Glyph;
import uk.ac.starlink.ttools.plot2.Pixer;

/**
 * XYShape implementation that can produce shapes of variable line thickness.
 *
 * @author   Mark Taylor
 * @since    16 Jan 2015
 */
@Equality
public abstract class BasicXYShape extends XYShape {

    private static final BasicXYShape[] XYSHAPES = createXYShapes();

    /**
     * Constructor.
     *
     * @param  name  shape name
     * @param  maxCacheRadius   maximum size for which shapes are cached
     * @param  pointGlyph   glyph to use for shape (0,0),
     *                      or null for no special casing
     */
    public BasicXYShape( String name, int maxCacheRadius, Glyph pointGlyph ) {
        super( name, maxCacheRadius, pointGlyph );
    }

    /**
     * Returns an XYShape corresponding to this one, but drawn with
     * thicker lines.  In cases where no line drawing is done,
     * for instance filled shapes, this object should be returned,
     * since the drawing will not change with line thickness.
     *
     * @param  nthick  line thickness index &gt;=0
     * @return  drawing shape
     */
    @Equality
    public abstract XYShape toThicker( int nthick );

    /**
     * Returns an array of XY shapes suitable for plotting markers
     * with variable X and Y extents.
     *
     * @return  XY shapes
     */
    public static BasicXYShape[] getXYShapes() {
        return XYSHAPES.clone();
    }

    /**
     * Creates an array of XY shapes suitable for plotting markers
     * with variable X and Y extents.
     *
     * @return  XY shapes
     */
    private static BasicXYShape[] createXYShapes() {
        return new BasicXYShape[] {
            createRectangleShape( "Open Rectangle", false ),
            createTriangleShape( "Open Triangle", false, true ),
            createTriangleShape( "Open Triangle Down", false, false ),
            createDiamondShape( "Open Diamond", false ),
            createEllipseShape( "Open Ellipse", false ),
            createRectangleShape( "Filled Rectangle", true ),
            createTriangleShape( "Filled Triangle", true, true ),
            createTriangleShape( "Filled Triangle Down", true, false ),
            createDiamondShape( "Filled Diamond", true ),
            createEllipseShape( "Filled Ellipse", true ),
        };
    }

    /**
     * Returns the radius threshold below which shapes should be cached,
     * for shapes which use pixel caching.
     *
     * @param  isFill  true for filled shape, false for open
     */
    private static int getPixelMaxCacheRadius( boolean isFill ) {

        /* The value should be large enough that glyphs used often are
         * cached, but large enough that the total cache size doesn't
         * get too big.  Adjust the answer depending on whether figures
         * are filled or not, on the grounds that filled glyphs will be
         * more expensive to keep around (they have more pixels to store). */
        /* Could set this based on PlotUtil.DEFAULT_MAX_PIXELS. */
        return isFill ? 16 : 24;
    }

    /**
     * XYShape representing a filled rectangle.  This is done efficiently.
     * Glyph instances are small and cheap to create.
     */
    private static class BlockShape extends BasicXYShape {

        /**
         * Constructor.
         */
        BlockShape( String name ) {
            super( name, 6, XYShape.POINT );
        }

        public XYShape toThicker( int nthick ) {
            return this;
        }

        protected Glyph createGlyph( short sx, final short sy ) {
            if ( sx == 0 && sy == 0 ) {
                return POINT;
            }
            final int ix = sx;
            final int iy = sy;
            return new Glyph() {
                public void paintGlyph( Graphics g ) {
                    g.fillRect( -ix, -iy, 2 * ix, 2 * iy );
                }
                public Pixer createPixer( Rectangle clip ) {
                    int xmin = Math.max( -ix, clip.x );
                    int ymin = Math.max( -iy, clip.y );
                    int xmax = Math.min( ix, clip.x + clip.width - 1 );
                    int ymax = Math.min( iy, clip.y + clip.height - 1 );
                    return new BlockPixer( xmin, xmax, ymin, ymax );
                }
            };
        }

        /**
         * Pixer for a rectangular block.
         */
        private static class BlockPixer implements Pixer {
            private final int xmin_;
            private final int xmax_;
            private final int ymax_;
            private int x_;
            private int y_;

            /**
             * Constructor.
             *
             * @param  xmin  minimum X coord
             * @param  xmax  maximum X coord
             * @param  ymin  minimum Y coord
             * @param  ymax  maximum Y coord
             */
            BlockPixer( int xmin, int xmax, int ymin, int ymax ) {
                xmin_ = xmin;
                xmax_ = xmax;
                ymax_ = ymax;
                x_ = xmax;
                y_ = ymin - 1;
            }

            public boolean next() {
                if ( ++x_ > xmax_ ) {
                    x_ = xmin_;
                    if ( ++y_ > ymax_ ) {
                        return false;
                    }
                }
                return true;
            }

            public int getX() {
                return x_;
            }

            public int getY() {
                return y_;
            }
        }
    }

    /**
     * Returns an ellipse shape.
     *
     * @param  name  shape name
     * @param  isFill  true for filled, false for open
     * @return   new shape instance
     */
    private static BasicXYShape createEllipseShape( String name,
                                                    final boolean isFill ) {
        int rCache = getPixelMaxCacheRadius( isFill );
        if ( isFill ) {
            return new DrawingShape( name, rCache, false ) {
                public LineGlyph createLineGlyph( final int dx, final int dy ) {
                    return new LineGlyph() {
                        public Rectangle getPixelBounds() {
                            return new Rectangle( -dx, -dy,
                                                  dx * 2 + 1, dy * 2 + 1 );
                        }
                        public void paintGlyph( Graphics g, StrokeKit skit ) {
                            g.fillOval( -dx, -dy, 2 * dx, 2 * dy );
                        }
                        public void drawShape( PixelDrawing d ) {
                            d.fillOval( -dx, -dy, 2 * dx, 2 * dy );
                        }
                    };
                }
            };
        }
        else {
            return new DrawingShape( name, rCache, true ) {
                public LineGlyph createLineGlyph( final int dx, final int dy ) {
                    return new LineGlyph() {
                        public Rectangle getPixelBounds() {
                            return new Rectangle( -dx, -dy,
                                                  dx * 2 + 1, dy * 2 + 1 );
                        }
                        public void paintGlyph( Graphics g, StrokeKit skit ) {
                            Graphics2D g2 = (Graphics2D) g;
                            Stroke stroke0 = g2.getStroke();
                            g2.setStroke( skit.getRound() );
                            g.drawOval( -dx, -dy, 2 * dx, 2 * dy );
                            g2.setStroke( stroke0 );
                        }
                        public void drawShape( PixelDrawing d ) {
                            d.drawOval( -dx, -dy, 2 * dx, 2 * dy );
                        }
                    };
                }
            };
        }
    }

    /**
     * Returns a rectangle shape.
     *
     * @param  name  shape name
     * @param  isFill  true for filled, false for open
     * @return   new shape instance
     */
    private static BasicXYShape createRectangleShape( String name,
                                                      boolean isFill ) {
        if ( isFill ) {
            return new BlockShape( name );
        }
        else {
            PolygonFunction polyFunc =
                (dx, dy) -> new Polygon( new int[] { -dx, -dx, dx, dx },
                                         new int[] { -dy, dy, dy, -dy }, 4 );
            return new OpenPolygonShape( name, polyFunc ) {
                @Override
                public XYShape toThicker( int nthick ) {
                    PixerFactory kernel =
                        LineGlyph
                       .createKernel( MarkerShape.FILLED_SQUARE, nthick );
                    Stroke stroke = new BasicStroke( 1f + 2 * nthick,
                                                     BasicStroke.CAP_BUTT,
                                                     BasicStroke.JOIN_MITER );
                    StrokeKit skit = new StrokeKit( stroke, stroke );
                    return new FatXYShape( this, nthick, kernel, skit );
                }
            };
        }
    }

    /**
     * Returns a triangle shape.
     *
     * @param  name  shape name
     * @param  isFill  true for filled, false for open
     * @param  isUp  true for upward pointing triangle, false for downward
     * @return   new shape instance
     */
    private static BasicXYShape createTriangleShape( String name,
                                                     boolean isFill,
                                                     final boolean isUp ) {
        PolygonFunction polyFunc =
            (dx, dy) -> {
                int[] xoffs = { -dx, dx, 0 };
                int[] yoffs = isUp ? new int[] { dy, dy, -dy }
                                   : new int[] { -dy, -dy, dy };
                return new Polygon( xoffs, yoffs, 3 );
            };
        return isFill ? new FillPolygonShape( name, polyFunc )
                      : new OpenPolygonShape( name, polyFunc );
    }

    /**
     * Returns a diamond shape.
     *
     * @param  name  shape name
     * @param  isFill  true for filled, false for open
     * @return   new shape instance
     */
    private static BasicXYShape createDiamondShape( String name,
                                                    boolean isFill ) {
        PolygonFunction polyFunc =
            (dx, dy) -> new Polygon( new int[] { -dx, 0, dx, 0 },
                                     new int[] { 0, dy, 0, -dy }, 4 );
        return isFill ? new FillPolygonShape( name, polyFunc )
                      : new OpenPolygonShape( name, polyFunc );
    }

    /**
     * Knows how to create polygons from X,Y pairs.
     */
    @FunctionalInterface
    private interface PolygonFunction {

        /**
         * Returns a polygon describing the shape at a given scale.
         *
         * @param  dx  X radius
         * @param  dy  Y radius
         * @return  polygon
         */
        Polygon toPolygon( int dx, int dy );
    }

    /**
     * DrawingShape subclass that draws the outline of a polygon.
     */
    private static class OpenPolygonShape extends DrawingShape {

        private final PolygonFunction polyFunc_;

        /**
         * Constructor.
         *
         * @param  name  shape name
         * @param  polyFunc   creates a polygon from X,Y
         */
        OpenPolygonShape( String name, PolygonFunction polyFunc ) {
            super( name, getPixelMaxCacheRadius( false ), true );
            polyFunc_ = polyFunc;
        }

        public LineGlyph createLineGlyph( int dx, int dy ) {
            final Polygon polygon = polyFunc_.toPolygon( dx, dy );
            return new LineGlyph() {
                public Rectangle getPixelBounds() {
                    return new Rectangle( -dx, -dy, dx * 2 + 1, dy * 2 + 1);
                }
                public void paintGlyph( Graphics g, StrokeKit skit ) {
                    Graphics2D g2 = (Graphics2D) g;
                    Stroke stroke0 = g2.getStroke();
                    g2.setStroke( skit.getRound() );
                    g2.drawPolygon( polygon );
                    g2.setStroke( stroke0 );
                }
                public void drawShape( PixelDrawing d ) {
                    int n = polygon.npoints;
                    int[] xs = polygon.xpoints;
                    int[] ys = polygon.ypoints;
                    for ( int i = 0; i < n - 1; i++ ) {
                        d.drawLine( xs[ i ], ys[ i ],
                                    xs[ i + 1 ], ys[ i + 1 ] );
                    }
                    d.drawLine( xs[ n - 1 ], ys[ n - 1 ],
                                xs[ 0 ], ys[ 0 ] );
                }
            };
        }
    }

    /**
     * BasicXYShape subclass that fills a polygon in.
     */
    private static class FillPolygonShape extends BasicXYShape {

        private final PolygonFunction polyFunc_;

        /**
         * Constructor.
         *
         * @param  name  shape name
         * @param  polyFunc   creates a polygon from X,Y
         */
        FillPolygonShape( String name, PolygonFunction polyFunc ) {
            super( name, 16, XYShape.POINT );
            polyFunc_ = polyFunc;
        }

        public XYShape toThicker( int nthick ) {
            return this;
        }

        protected Glyph createGlyph( short sx, short sy ) {
            if ( sx == 0 && sy == 0 ) {
                return POINT;
            }
            else {
                Polygon poly = polyFunc_.toPolygon( sx, sy );
                final int[] xs = poly.xpoints;
                final int[] ys = poly.ypoints;
                final int np = poly.npoints;
                return new Glyph() {
                    public void paintGlyph( Graphics g ) {
                        g.fillPolygon( xs, ys, np );
                    }
                    public Pixer createPixer( Rectangle clip ) {
                        return new FillPixer( xs, ys, np, clip );
                    }
                };
            }
        }
    }

    /**
     * Partial XYShape implementation which does pixel generation by using an
     * instance of the PixelDrawing class.  This is probably not very efficient,
     * so glyph caching will help.
     */
    private static abstract class DrawingShape extends BasicXYShape {

        private final boolean canFat_;

        /**
         * Constructor.
         *
         * @param  name  shape name
         * @param  maxCacheRadius   max extent size for glyph caching
         */
        DrawingShape( String name, int maxCacheRadius, boolean canFat ) {
            super( name, maxCacheRadius, XYShape.POINT );
            canFat_ = canFat;
        }

        /**
         * Returns a Glypher object that knows how to represent this
         * shape at a particular size.
         *
         * @param   dx  X radius
         * @param   dy  Y radius
         */
        abstract LineGlyph createLineGlyph( int dx, int dy );

        public Glyph createGlyph( short sx, short sy ) {
            final int x = sx;
            final int y = sy;
            final Rectangle bounds = 
                new Rectangle( -x, -y, x * 2 + 1, y * 2 + 1 );
            final LineGlyph lineGlyph = createLineGlyph( sx, sy );

            /* If the glyph will be cached, assume that it may be used
             * multiple times.  In this case, it's worth calculating the
             * pixels once, and storing those for later use.
             * The pixels may in fact never get used, if the glyph is
             * painted rather than pixellated, but for cached glyphs the
             * absolute cost of this preparation is assumed low, since
             * the cache size, and the number of pixels per glyph,
             * are both assumed relatively small. */
            if ( isCached( sx, sy ) ) {
                PixerFactory pfact =
                    Pixers.createPixerCopier( lineGlyph.createPixer( bounds ) );
                return new Glyph() {
                    public void paintGlyph( Graphics g ) {
                        lineGlyph.paintGlyph( g );
                    }
                    public Pixer createPixer( Rectangle clip ) {
                        return Pixers.createClippedPixer( pfact, clip );
                    }
                };
            }

            /* Otherwise, caching the pixel list is probably wasted resources;
             * calculate the pixel list only at painting time. */
            else {
                return lineGlyph;
            }
        }

        public XYShape toThicker( int nthick ) {
            return canFat_ ? new FatXYShape( this, nthick ) : this;
        }
    }

    /**
     * Shape which transforms a DrawingShape by drawing it with thicker lines.
     */
    @Equality
    private static class FatXYShape extends XYShape {

        private final DrawingShape shape_;
        private final int nthick_;
        private final PixerFactory kernel_;
        private final StrokeKit strokeKit_;

        /**
         * Constructs a FatXYShape given a base shape and thickness.
         *
         * @param  shape  basic shape
         * @param  nthick  thickness index &gt;=0
         */
        public FatXYShape( DrawingShape shape, int nthick ) {
            this( shape, nthick,
                  LineGlyph.createThickKernel( nthick ),
                  LineGlyph.createThickStrokeKit( nthick ) );
        }

        /**
         * Constructs a FatXYShape given all details.
         *
         * @param  shape  basic shape
         * @param  nthick  thickness index &gt;=0
         * @param  kernel  smoothing kernel
         * @param  strokeKit  line strokes
         */
        public FatXYShape( DrawingShape shape, int nthick,
                           PixerFactory kernel, StrokeKit strokeKit ) {
            super( shape.getName() + nthick, 10,
                   FatLineXYShape.createPointGlyph( kernel, strokeKit ) );
            shape_ = shape;
            nthick_ = nthick;
            kernel_ = kernel;
            strokeKit_ = strokeKit;
        }

        public Glyph createGlyph( short sx, short sy ) {
            return shape_.createLineGlyph( sx, sy )
                         .toThicker( kernel_, strokeKit_ );
        }

        @Override
        public int hashCode() {
            int code = -66294;
            code = 23 * code + shape_.hashCode();
            code = 23 * code + nthick_;
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof FatXYShape ) {
                FatXYShape other = (FatXYShape) o;
                return this.shape_.equals( other.shape_ )
                    && this.nthick_ == other.nthick_;
            }
            else {
                return false;
            }
        }
    }
}
