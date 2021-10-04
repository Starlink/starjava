package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import uk.ac.starlink.ttools.plot2.Glyph;
import uk.ac.starlink.ttools.plot2.Pixer;

/**
 * Utility class for generating XYShape objects.
 *
 * @author   Mark Taylor
 * @since    16 Jan 2015
 */
public class XYShapes {

    private static final XYShape[] XYSHAPES = createXYShapes();

    /**
     * Private constructor prevents instantiation.
     */
    private XYShapes() {
    }

    /**
     * Returns an array of XY shapes suitable for plotting markers
     * with variable X and Y extents.
     *
     * @return  XY shapes
     */
    public static XYShape[] getXYShapes() {
        return XYSHAPES.clone();
    }

    /**
     * Creates an array of XY shapes suitable for plotting markers
     * with variable X and Y extents.
     *
     * @return  XY shapes
     */
    private static XYShape[] createXYShapes() {
        List<XYShape> shapes = new ArrayList<XYShape>();
        shapes.add( createRectangleShape( "Open Rectangle", false ) );
        shapes.add( createTriangleShape( "Open Triangle", false, true ) );
        shapes.add( createTriangleShape( "Open Triangle Down", false, false ) );
        shapes.add( createDiamondShape( "Open Diamond", false ) );
        shapes.add( createEllipseShape( "Open Ellipse", false ) );
        shapes.add( createRectangleShape( "Filled Rectangle", true ) );
        shapes.add( createTriangleShape( "Filled Triangle", true, true ) );
        shapes.add( createTriangleShape( "Filled Triangle Down", true,
                                         false ) );
        shapes.add( createDiamondShape( "Filled Diamond", true ) );
        shapes.add( createEllipseShape( "Filled Ellipse", true ) );
        return shapes.toArray( new XYShape[ 0 ] );
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
    private static class BlockShape extends XYShape {

        /**
         * Constructor.
         */
        BlockShape( String name ) {
            super( name, 6, XYShape.POINT );
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
    private static XYShape createEllipseShape( String name,
                                               final boolean isFill ) {
        return new DrawingShape( name, getPixelMaxCacheRadius( isFill ) ) {
            public Glypher createGlypher( final int dx, final int dy ) {
                if ( isFill ) {
                    return new Glypher() {
                        public void paintShape( Graphics g ) {
                            g.fillOval( -dx, -dy, 2 * dx, 2 * dy );
                        }
                        public void drawShape( PixelDrawing d ) {
                            d.fillOval( -dx, -dy, 2 * dx, 2 * dy );
                        }
                    };
                }
                else {
                    return new Glypher() {
                        public void paintShape( Graphics g ) {
                            g.drawOval( -dx, -dy, 2 * dx, 2 * dy );
                        }
                        public void drawShape( PixelDrawing d ) {
                            d.drawOval( -dx, -dy, 2 * dx, 2 * dy );
                        }
                    };
                }
            }
        };
    }

    /**
     * Returns a rectangle shape.
     *
     * @param  name  shape name
     * @param  isFill  true for filled, false for open
     * @return   new shape instance
     */
    private static XYShape createRectangleShape( String name, boolean isFill ) {
        if ( isFill ) {
            return new BlockShape( name );
        }
        else {
            return new PolygonShape( name, false ) {
                Polygon createPolygon( int dx, int dy ) {
                    return new Polygon( new int[] { -dx, -dx, dx, dx },
                                        new int[] { -dy, dy, dy, -dy }, 4 );
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
    private static XYShape createTriangleShape( String name, boolean isFill,
                                                final boolean isUp ) {
        return new PolygonShape( name, isFill ) {
            Polygon createPolygon( int dx, int dy ) {
                int[] xoffs = { -dx, dx, 0 };
                int[] yoffs = isUp ? new int[] { dy, dy, -dy }
                                   : new int[] { -dy, -dy, dy };
                return new Polygon( xoffs, yoffs, 3 );
            }
        };
    }

    /**
     * Returns a diamond shape.
     *
     * @param  name  shape name
     * @param  isFill  true for filled, false for open
     * @return   new shape instance
     */
    private static XYShape createDiamondShape( String name, boolean isFill ) {
        return new PolygonShape( name, isFill ) {
            Polygon createPolygon( int dx, int dy ) {
                return new Polygon( new int[] { -dx, 0, dx, 0 },
                                    new int[] { 0, dy, 0, -dy }, 4 );
            }
        };
    }

    /**
     * DrawingShape subclass that works with a polygon.
     */
    private static abstract class PolygonShape extends DrawingShape {
        private boolean isFill_;

        /**
         * Constructor.
         *
         * @param  name  shape name
         * @param  isFill  true for filled polygon, false for empty
         */
        PolygonShape( String name, boolean isFill ) {
            super( name, getPixelMaxCacheRadius( isFill ) );
            isFill_ = isFill;
        }

        /**
         * Returns a polygon describing the shape at a given scale.
         *
         * @param  dx  X radius
         * @param  dy  Y radius
         * @return  polygon
         */
        abstract Polygon createPolygon( int dx, int dy );

        public Glypher createGlypher( int dx, int dy ) {
            final Polygon polygon = createPolygon( dx, dy );
            if ( isFill_ ) {
                return new Glypher() {
                    public void paintShape( Graphics g ) {
                        g.fillPolygon( polygon );
                    }
                    public void drawShape( PixelDrawing d ) {
                        d.fill( polygon );
                    }
                };
            }
            else {
                return new Glypher() {
                    public void paintShape( Graphics g ) {
                        g.drawPolygon( polygon );
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
    }

    /**
     * Partial XYShape implementation which does pixel generation by using an
     * instance of the PixelDrawing class.  This is probably not very efficient,
     * so glyph caching will help.
     */
    private static abstract class DrawingShape extends XYShape {

        /**
         * Constructor.
         *
         * @param  name  shape name
         * @param  maxCacheRadius   max extent size for glyph caching
         */
        DrawingShape( String name, int maxCacheRadius ) {
            super( name, maxCacheRadius, XYShape.POINT );
        }

        /**
         * Returns a Glypher object that knows how to represent this
         * shape at a particular size.
         *
         * @param   dx  X radius
         * @param   dy  Y radius
         */
        abstract Glypher createGlypher( int dx, int dy );

        public Glyph createGlyph( short sx, short sy ) {
            final int x = sx;
            final int y = sy;
            final int[] xoffs = new int[] { x, -x, 0, 0 };
            final int[] yoffs = new int[] { 0, 0, -y, y };
            final Rectangle bounds = 
                new Rectangle( -x, -y, x * 2 + 1, y * 2 + 1 );
            final Glypher glypher = createGlypher( sx, sy );

            /* If the glyph will be cached, assume that it may be used
             * multiple times.  In this case, it's worth calculating the
             * pixels once, and storing those for later use.
             * The pixels may in fact never get used, if the glyph is
             * painted rather than pixellated, but for cached glyphs the
             * absolute cost of this preparation is assumed low, since
             * the cache size, and the number of pixels per glyph,
             * are both assumed relatively small. */
            if ( isCached( sx, sy ) ) {
                final PixelDrawing drawing = new PixelDrawing( bounds );
                glypher.drawShape( drawing );
                PixerFactory pfact = Pixers.copy( drawing );
                return new Glyph() {
                    public void paintGlyph( Graphics g ) {
                        glypher.paintShape( g );
                    }
                    public Pixer createPixer( Rectangle clip ) {
                        return Pixers.createClippedPixer( pfact, clip );
                    }
                };
            }

            /* Otherwise, caching the pixel list is probably wasted resources;
             * calculate the pixel list only at painting time. */
            else {
                return new Glyph() {
                    public void paintGlyph( Graphics g ) {
                        glypher.paintShape( g );
                    }
                    public Pixer createPixer( Rectangle clip ) {
                        Rectangle rect;
                        if ( clip.contains( bounds ) ) {
                            rect = bounds;
                        }
                        else {
                            rect = bounds.intersection( clip );
                        }
                        PixelDrawing drawing = new PixelDrawing( rect );
                        glypher.drawShape( drawing );
                        return drawing.createPixer();
                    }
                };
            }
        }

        /**
         * Defines how to draw this shape at a fixed scale.
         * This is conceptually much like the Glyph interface itself,
         * but adapted for the needs of this DrawingShape abstract class.
         */
        public static interface Glypher {

            /**
             * Paints this glyph to a graphics context, with the reference
             * point at the origin.
             *
             * @param  g  graphics context
             */
            void paintShape( Graphics g );

            /**
             * Draws this glyph to a Drawing object, with the reference
             * point at the origin.
             *
             * @param  drawing  drawing
             */
            void drawShape( PixelDrawing drawing );
        }
    }
}
