package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.Rectangle;
import uk.ac.starlink.ttools.plot2.Equality;
import uk.ac.starlink.ttools.plot2.Glyph;
import uk.ac.starlink.ttools.plot2.Pixer;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType2D;
import uk.ac.starlink.ttools.plot2.paper.PaperType3D;

/**
 * Defines how polygon vertices are turned into graphics.
 *
 * @author   Mark Taylor
 * @since    6 Mar 2019
 */
public class PolygonMode {

    private final String name_;
    private final String description_;
    private final Glypher fastGlypher_;
    private final Glypher carefulGlypher_;

    private static final Painter OUTLINE_PAINTER = new Painter() {
        public void paintPolygon( Graphics g, int x0, int y0,
                                  int[] xs, int[] ys, int np ) {
            g.drawPolygon( xs, ys, np );
        }
    };
    private static final Painter FILL_PAINTER = new Painter() {
        public void paintPolygon( Graphics g, int x0, int y0,
                                  int[] xs, int[] ys, int np ) {
            g.fillPolygon( xs, ys, np );
        }
    };
    private static final Painter CROSS_PAINTER = new Painter() {
        public void paintPolygon( Graphics g, int x0, int y0,
                                  int[] xs, int[] ys, int np ) {
            g.drawPolygon( xs, ys, np );
            for ( int ip = 0; ip < np; ip++ ) {
                for ( int ip2 = ip + 2; ip2 < np; ip2++ ) {
                    g.drawLine( xs[ ip ], ys[ ip ], xs[ ip2 ], ys[ ip2 ] );
                }
            }
        }
    };
    private static final Painter STAR_PAINTER = new Painter() {
        public void paintPolygon( Graphics g, int x0, int y0,
                                  int[] xs, int[] ys, int np ) {
            g.drawPolygon( xs, ys, np );
            for ( int ip = 0; ip < np; ip++ ) {
                g.drawLine( x0, y0, xs[ ip ], ys[ ip ] );
            }
        }
    };

    private static final Glypher OUTLINE_CALC_GLYPHER = new LinesGlypher();
    private static final Glypher FILL_CALC_GLYPHER =
            new SingleGlypher( FILL_PAINTER ) {
        Pixer createPolygonPixer( int x0, int y0,
                                  int[] xs, int[] ys, int np, Rectangle bds ) {
            return createCalcFillPixer( xs, ys, np, bds );
        }
    };
    private static final Glypher OUTLINE_DRAW_GLYPHER =
            new DrawingGlypher( OUTLINE_PAINTER ) {
        void drawPolygon( PixelDrawing d, int x0, int y0,
                          int[] xs, int[] ys, int np ) {
            for ( int ip = 0; ip < np; ip++ ) {
                int ip1 = ( ip + 1 ) % np;
                d.drawLine( xs[ ip ], ys[ ip ], xs[ ip1 ], ys[ ip1 ] );
            }
        }
    };
    private static final Glypher FILL_DRAW_GLYPHER =
            new DrawingGlypher( FILL_PAINTER ) {
        void drawPolygon( PixelDrawing d, int x0, int y0,
                          int[] xs, int[] ys, int np ) {
            d.fill( new Polygon( xs, ys, np ) );
        }
    };
    private static final Glypher CROSS_DRAW_GLYPHER =
            new DrawingGlypher( CROSS_PAINTER ) {
        void drawPolygon( PixelDrawing d, int x0, int y0,
                          int[] xs, int[] ys, int np ) {
            for ( int ip = 0; ip < np; ip++ ) {
                int ip1 = ( ip + 1 ) % np;
                d.drawLine( xs[ ip ], ys[ ip ], xs[ ip1 ], ys[ ip1 ] );
                for ( int ip2 = ip + 2; ip2 < np; ip2++ ) {
                    d.drawLine( xs[ ip ], ys[ ip ], xs[ ip2 ], ys[ ip2 ] );
                }
            }
        }
    };
    private static final Glypher STAR_DRAW_GLYPHER =
            new DrawingGlypher( STAR_PAINTER ) {
        void drawPolygon( PixelDrawing d, int x0, int y0,
                          int[] xs, int[] ys, int np ) {
            for ( int ip = 0; ip < np; ip++ ) {
                int ip1 = ( ip + 1 ) % np;
                d.drawLine( xs[ ip ], ys[ ip ], xs[ ip1 ], ys[ ip1 ] );
                d.drawLine( xs[ ip ], ys[ ip ], x0, y0 );
            }
        }
    };

    /** Outline. */
    public static final PolygonMode OUTLINE =
        new PolygonMode( "outline",
                         "draws a line round the outside of the polygon",
                         OUTLINE_CALC_GLYPHER, OUTLINE_DRAW_GLYPHER );

    /** Fill. */
    public static final PolygonMode FILL =
        new PolygonMode( "fill",
                         "fills the interior of the polygon",
                         FILL_CALC_GLYPHER, FILL_DRAW_GLYPHER );

    /** Cross. */
    public static final PolygonMode CROSS =
        new PolygonMode( "cross",
                         "draws a line round the outside of the polygon"
                       + " and lines between all the vertices",
                          CROSS_DRAW_GLYPHER, CROSS_DRAW_GLYPHER );

    /** Star. */
    public static final PolygonMode STAR =
        new PolygonMode( "star",
                         "draws a line round the outside of the polygon"
                       + " and lines from the nominal center to each vertex",
                         STAR_DRAW_GLYPHER, STAR_DRAW_GLYPHER );

    /** Available instances. */
    public static final PolygonMode[] MODES = new PolygonMode[] {
        OUTLINE, FILL, CROSS, STAR,
    };

    /**
     * Constructor.
     *
     * @param  name  user-visible name
     * @param  description   short XML-friendly description
     */
    private PolygonMode( String name, String description,
                         Glypher fastGlypher, Glypher carefulGlypher ) {
        name_ = name;
        description_ = description;
        fastGlypher_ = fastGlypher;
        carefulGlypher_ = carefulGlypher;
    }

    /**
     * Returns a glypher that can paint polygons according to this mode.
     *
     * @param  isFast  if true, favour a faster mode,
     *                 if false favour a more careful mode
     */
    public Glypher getGlypher( boolean isFast ) {
        return isFast ? fastGlypher_ : carefulGlypher_;
    }

    /**
     * Returns a short user-visible description of this mode.
     *
     * @return  XML-friengly description text (not wrapped in an element)
     */
    public String getDescription() {
        return description_;
    }

    @Override
    public String toString() {
        return name_;
    }

    /**
     * Knows how to place polygons as glyphs.
     */
    @Equality
    public interface Glypher {

        /**
         * Paints a polygon with the given vertices in the style of this
         * outliner to a graphics context.
         *
         * @param  np  number of vertices
         * @param  x0  X coordinate of nominal center
         * @param  y0  Y coordinate of nominal center
         * @param  xs  X coordinates of vertices
         * @param  ys  Y coordinates of vertices
         */
        void paintPolygon( Graphics g, int x0, int y0,
                           int[] xs, int[] ys, int np );

        /**
         * Places zero or more glyphs on a given 2D paper instance
         * corresponding to the supplied graphics vertices of a polygon.
         *
         * @param   ptype  paper type
         * @param   paper  paper object
         * @param   gx0   graphics X coordinate of nominal center
         * @param   gy0   graphics Y coordinate of nominal center
         * @param   gxs   np-element array giving graphics X vertex coordinates
         * @param   gys   np-element array giving graphics Y vertex coordinates
         * @param   np    number of polygon vertices
         * @param   color   colour with which to place glyph
         */
        public abstract void placeGlyphs2D( PaperType2D ptype, Paper paper,
                                            int gx0, int gy0,
                                            int[] gxs, int[] gys, int np,
                                            Color color );

        /**
         * Places zero or more glyphs on a given 3D paper instance
         * corresponding to the supplied graphics vertices of a polygon.
         *
         * @param   ptype  paper type
         * @param   paper  paper object
         * @param   gx0   graphics X coordinate of nominal center
         * @param   gy0   graphics Y coordinate of nominal center
         * @param   gxs   np-element array giving graphics X vertex coordinates
         * @param   gys   np-element array giving graphics Y vertex coordinates
         * @param   np    number of polygon vertices
         * @param   gz    graphics Z coordinate for polygon
         * @param   color   colour with which to place glyph
         */
        public abstract void placeGlyphs3D( PaperType3D ptype, Paper paper,
                                            int gx0, int gy0,
                                            int[] gxs, int[] gys, int np,
                                            double gz, Color color );
    }

    /**
     * Knows how to paint a polygon to a graphics context.
     */
    private interface Painter {

        /**
         * Paints a polygon with the given vertices in the style of this
         * outliner to a graphics context.
         *
         * @param  np  number of vertices
         * @param  x0  X coordinate of nominal center
         * @param  y0  Y coordinate of nominal center
         * @param  xs  X coordinates of vertices
         * @param  ys  Y coordinates of vertices
         */
        void paintPolygon( Graphics g, int x0, int y0,
                           int[] xs, int[] ys, int np );
    }

    /**
     * Glypher implemention for drawing the outlines of a polygon using
     * multiple LineXYShape glyphs.  Should be most efficient for some
     * drawing modes, but may repeat pixels occasionally.
     */
    private static class LinesGlypher implements Glypher {

        /* Use a line drawing shape that omits the first pixel of
         * each line drawn.  That means that pixels at the corners of
         * a 'normal' shaped polygon will not get painted twice.
         * That's not just good for efficiency, it's desirable when
         * a density-like shading mode is in use, so that lines don't
         * get flagged as hit more than once for a given plot symbol.
         * However, this is not perfect - rather acute angles will
         * still end up getting hit twice.  Oh well. */
        private final XYShape lineShape1 = LineXYShape.INSTANCE_SKIP1;

        public void placeGlyphs2D( PaperType2D ptype, Paper paper,
                                   int gx0, int gy0,
                                   int[] gxs, int[] gys, int np, Color color ) {
            for ( int ip = 0; ip < np; ip++ ) {
                int ip1 = ( ip + 1 ) % np;
                int gx = gxs[ ip ];
                int gy = gys[ ip ];
                short sx = (short) ( gxs[ ip1 ] - gx );
                short sy = (short) ( gys[ ip1 ] - gy );
                Glyph glyph = lineShape1.getGlyph( sx, sy );
                if ( glyph != null ) {
                    ptype.placeGlyph( paper, gx, gy, glyph, color );
                }
            }
        }

        public void placeGlyphs3D( PaperType3D ptype, Paper paper,
                                   int gx0, int gy0,
                                   int[] gxs, int[] gys, int np, double gz,
                                   Color color ) {
            for ( int ip = 0; ip < np; ip++ ) {
                int ip1 = ( ip + 1 ) % np;
                int gx = gxs[ ip ];
                int gy = gys[ ip ];
                short sx = (short) ( gxs[ ip1 ] - gx );
                short sy = (short) ( gys[ ip1 ] - gy );
                Glyph glyph = lineShape1.getGlyph( sx, sy );
                if ( glyph != null ) {
                    ptype.placeGlyph( paper, gx, gy, gz, glyph, color );
                }
            }
        }

        public void paintPolygon( Graphics g, int x0, int y0,
                                  int[] xs, int[] ys, int np ) {
            OUTLINE_PAINTER.paintPolygon( g, x0, y0, xs, ys, np );
        }
    }

    /**
     * Partial Glypher implementation that places a single glyph.
     * Concrete subclasses have to come up with the Pixer implementation.
     */
    private static abstract class SingleGlypher implements Glypher {

        private final Painter painter_;
        private static final int LINE_THICK = 1;

        /**
         * Constructor.
         *
         * @param   painter  can paint polygon to graphics context
         */
        SingleGlypher( Painter painter ) {
            painter_ = painter;
        }

        public void paintPolygon( Graphics g, int x0, int y0,
                                  int[] xs, int[] ys, int np ) {
            painter_.paintPolygon( g, x0, y0, xs, ys, np );
        }

        /**
         * Returns a pixer that can enumerate all the pixel positions
         * within the intersection of a given graphics-space polygon
         * and bounding rectangle.  The supplied bounds can be assumed
         * to be no larger than required.
         *
         * @param   x0   X coordinate of nominal center
         * @param   y0   Y coordinate of nominal center
         * @param   xs   np-element array giving X coordinates of polygon
         * @param   ys   np-element array giving X coordinates of polygon
         * @param   np   number of vertices in polygon
         * @param   bounds  rectangle within which pixels are required;
         *                  no pixels outside this rectangle are permitted
         * @return  new pixer
         */
        abstract Pixer createPolygonPixer( int x0, int y0,
                                           int[] xs, int[] ys, int np,
                                           Rectangle bounds );

        public void placeGlyphs2D( PaperType2D ptype, Paper paper,
                                   int gx0, int gy0,
                                   int[] gxs, int[] gys, int np, Color color ) {
            Glyph glyph = createGlyph( gx0, gy0, gxs, gys, np );
            ptype.placeGlyph( paper, 0, 0, glyph, color );
        }

        public void placeGlyphs3D( PaperType3D ptype, Paper paper,
                                   int gx0, int gy0,
                                   int[] gxs, int[] gys, int np, double gz,
                                   Color color ) {
            Glyph glyph = createGlyph( gx0, gy0, gxs, gys, np );
            ptype.placeGlyph( paper, 0, 0, gz, glyph, color );
        }

        private Glyph createGlyph( final int x0, final int y0,
                                   final int[] xs, final int[] ys,
                                   final int np ) {
            return new Glyph() {
                public void paintGlyph( Graphics g ) {
                    painter_.paintPolygon( g, x0, y0, xs, ys, np );
                }
                public Pixer createPixer( Rectangle clip ) {
                    int xmin = Integer.MAX_VALUE;
                    int ymin = Integer.MAX_VALUE;
                    int xmax = Integer.MIN_VALUE;
                    int ymax = Integer.MIN_VALUE;
                    for ( int ip = 0; ip < np; ip++ ) {
                        int x = xs[ ip ];
                        int y = ys[ ip ];
                        xmin = Math.min( xmin, x );
                        xmax = Math.max( xmax, x ) + LINE_THICK;
                        ymin = Math.min( ymin, y );
                        ymax = Math.max( ymax, y ) + LINE_THICK;
                    }
                    Rectangle bounds =
                        new Rectangle( xmin, ymin, xmax - xmin, ymax - ymin );
                    Rectangle rect = clip.contains( bounds )
                                   ? bounds
                                   : bounds.intersection( clip );
                    return rect.height > 0 && rect.width > 0
                         ? createPolygonPixer( x0, y0, xs, ys, np, rect )
                         : null;
                }
            };
        }
    }

    /**
     * Partial Glypher implementation for which concrete subclasses
     * just have to be able to render the shape to a PixelDrawing.
     */
    private static abstract class DrawingGlypher extends SingleGlypher {

        /**
         * Constructor.
         *
         * @param   painter  can paint polygon to graphics context
         */
        DrawingGlypher( Painter painter ) {
            super( painter );
        }

        /**
         * Renders the required polygon shape to a PixelDrawing object.
         *
         * @param   d  drawing
         * @param   x0  X coordinate of nominal center
         * @param   y0  Y coordinate of nominal center
         * @param   xs  array of vertex X coordinates
         * @param   xs  array of vertex Y coordinates
         * @param   np  number of vertices
         */
        abstract void drawPolygon( PixelDrawing d, int x0, int y0,
                                   int[] xs, int[] ys, int np );

        Pixer createPolygonPixer( int x0, int y0, int[] xs, int[] ys, int np,
                                  Rectangle bounds ) {
            final PixelDrawing drawing = new PixelDrawing( bounds );
            drawPolygon( drawing, x0, y0, xs, ys, np );
            return drawing.createPixer();
        }
    }

    /**
     * Pixer implementation for filling a polygon by hand.
     * Cheap on memory, should be quite fast.
     * Only handles convex polygons.
     *
     * @param   xs   np-element array giving graphics X vertex coordinates
     * @param   ys   np-element array giving graphics Y vertex coordinates
     * @param  np  number of vertices
     * @param  bounds  actual bounds within which pixels are required
     */
    private static Pixer createCalcFillPixer( final int[] xs, final int[] ys,
                                              final int np, Rectangle bounds ) {

        /* Store bounds of the rectangle over which we will iterate. */
        final int xmin = bounds.x;
        final int xmax = bounds.x + bounds.width;
        final int ymin = bounds.y;
        final int ymax = bounds.y + bounds.height;

        /* Calculate the gradient for each edge. */
        final double[] grads = new double[ np ];
        for ( int ip = 0; ip < np; ip++ ) {
            int ip1 = ( ip + 1 ) % np;
            grads[ ip ] = ( xs[ ip1 ] - xs[ ip ] )
               / (double) ( ys[ ip1 ] - ys[ ip ] );
        }
        return atLeastPixer( xmin, ymin, new Pixer() {
            int xhi_;
            int y_ = ymin - 1;
            int x_ = Integer.MAX_VALUE / 2;

            /* For each horizontal row of pixels in the required Y range,
             * identify two edges which cross the row, and find out where
             * they cross it (not necessarily in the required X range).
             * For each X position in the required X range, return its
             * coordinates if it falls between the two crossing points.
             * All the work is done in the iterator's next() method. */
            public boolean next() {
                if ( ++x_ > xhi_ ) {
                    int[] xlimits = null;
                    while ( xlimits == null ) {
                        if ( ++y_ >= ymax ) {
                            return false;
                        }
                        xlimits = getXlimits( y_ );
                    }
                    x_ = xlimits[ 0 ];
                    xhi_ = xlimits[ 1 ];
                }
                return true;
            }
            public int getX() {
                return x_;
            }
            public int getY() {
                return y_;
            }

            /**
             * Returns the bounds of the X range which falls within the
             * polygon for a given Y value.
             *
             * @param  y  Y coordinate
             * @return  2-element (xlow,xhigh) array giving lower and upper
             *          bounds of filled-in region, or null if empty
             */
            private int[] getXlimits( int y ) {
                int[] ies = getEdgeIndices( y );
                if ( ies == null ) {
                    return null;
                }
                int ieA = ies[ 0 ];
                int ieB = ies[ 1 ];
                int xA = xs[ ieA ]
                       + PlotUtil.ifloor( ( y_ - ys[ ieA ] ) * grads[ ieA ] );
                int xB = xs[ ieB ]
                       + PlotUtil.ifloor( ( y_ - ys[ ieB ] ) * grads[ ieB ] );
                int xL;
                int xH;
                if ( xA <= xB ) {
                    xL = xA;
                    xH = xB;
                }
                else {
                    xL = xB;
                    xH = xA;
                }
                int xlo = Math.max( xL, xmin );
                int xhi = Math.min( xH, xmax - 1 );
                return xlo < xhi ? new int[] { xlo, xhi }
                                 : null;
            }

            /**
             * Returns the indices of the (first) two edges that cross a
             * given Y value.  If the horizontal line corresponding to Y
             * is tangent to the polygon, null is returned.
             *
             * @param  y  Y coordinate
             * @return   2-element array of indices into polygon edge array
             *           giving edges that cross the y coord
             */
            private int[] getEdgeIndices( int y ) {
                int ieA = Integer.MIN_VALUE;
                for ( int ip = 0; ip < np; ip++ ) {
                    int ip1 = ( ip + 1 ) % np;
                    int dy0 = y - ys[ ip ];
                    int dy1 = y - ys[ ip1 ];
                    int det = ( y - ys[ ip ] ) * ( y - ys[ ip1 ] );

                    /* Test if this edge crosses. */
                    if ( dy0 * dy1 <= 0 ) {

                        /* It's the first crossing edge; store it
                         * and continue. */
                        if ( ieA == Integer.MIN_VALUE ) {
                            ieA = ip;

                            /* If this is a non-tangential edge, we don't want
                             * the next one (which will correspond to the
                             * same crossing line), so bump the loop variable
                             * to avoid choosing it. */
                            if ( dy1 == 0 ) {
                                ip++;
                            }
                        }

                        /* It's the second crossing edge; return the result.
                         * This assumes a convex polygon
                         * (only crosses twice). */
                        else {
                            return new int[] { ieA, ip };
                        }
                    }
                }
                return null;
            }
        } );
    }

    /**
     * Wraps a pixer instance to make sure it returns at least one point.
     * The behaviour is just the same as that of the supplied base instance
     * as long as that instance supplies at least one point.  If its
     * iteration terminates with no pixels however, the instance returned
     * by this method intervenes to make it return the single point (x,y).
     *
     * @param  x  backstop X coordinate 
     * @param  y  backstop Y coordinate
     * @param  base  base pixer
     * @return  modified pixer
     */
    private static Pixer atLeastPixer( final int x, final int y,
                                       final Pixer base ) {
        return new Pixer() {
            private int count_;
            boolean isPoint_;
            public boolean next() {
                if ( base.next() ) {
                    count_++;
                    return true;
                }
                else if ( count_ == 0 ) {
                    count_++;
                    isPoint_ = true;
                    return true;
                }
                else {
                    return false;
                }
            }
            public int getX() {
                return isPoint_ ? x : base.getX();
            }
            public int getY() {
                return isPoint_ ? y : base.getY();
            }
        };
    }
}
