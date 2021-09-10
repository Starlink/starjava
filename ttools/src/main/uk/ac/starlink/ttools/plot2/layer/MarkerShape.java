package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import uk.ac.starlink.ttools.plot2.Pixer;

/**
 * Defines the abstract shape of a MarkerStyle.  Instances of this class
 * are factories which can produce a family of MarkerStyle objects with
 * a shape which is in some sense the same, but of various sizes and
 * colours.
 *
 * @author   Mark Taylor
 * @since    8 Sep 2021
 */
public abstract class MarkerShape {

    private final String name_;

    /**
     * Rendering hint concerning whether to draw outlines on filled circles.
     * In bitmap contexts, such circles typically look ugly if outlines are
     * not drawn.  However, in some contexts (vector graphics,
     * especially of transparent markers) it is a bad idea.
     * If this hint is set to Boolean.TRUE outlines will be drawn,
     * and if it is set to Boolean.FALSE, they will not.
     * If it is not set, the default policy will be followed,
     * which currently means they *will* be drawn
     * (this represents the historical behaviour).
     */
    public static RenderingHints.Key OUTLINE_CIRCLE_HINT =
            new RenderingHints.Key( 1 ) {
        public boolean isCompatibleValue( Object obj ) {
            return obj instanceof Boolean;
        }
    };

    private static final List<Supplier<Pixer>> OPEN_CIRCLE_PIXERS =
        calculateOpenCirclePixers();
    private static final List<Supplier<Pixer>> FILLED_CIRCLE_PIXERS =
        calculateFilledCirclePixers();

    /**
     * Constructor.
     *
     * @param   name  shape name
     */
    protected MarkerShape( String name ) {
        name_ = name;
    }

    /**
     * Factory method which produces a MarkerStyle of the shape characteristic
     * of this object with specified colour and nominal size.
     *
     * @param  color   colour of style
     * @param  size    nominal size of style - any non-negative integer
     *                 should give a reasonable image
     */
    public abstract MarkerStyle getStyle( Color color, int size );

    @Override
    public String toString() {
        return name_;
    }

    /** Factory for point-like markers.  The size parameter is ignored. */
    public static final MarkerShape POINT = new MarkerShape( "point" ) {
        public MarkerStyle getStyle( Color color, int size ) {
            return new MarkerStyle( this, color, 0, 1,
                                    g -> g.fillRect( 0, 0, 1, 1 ) );
        }
    };

    /** Factory for open circle markers. */
    public static final MarkerShape OPEN_CIRCLE =
            new MarkerShape( "open circle" ) {
        public MarkerStyle getStyle( Color color, final int size ) {
            final int off = -size;
            final int d = size * 2;
            Consumer<Graphics> drawShape = g -> g.drawOval( off, off, d, d );
            Supplier<Pixer> pixerFact = size < OPEN_CIRCLE_PIXERS.size()
                                      ? OPEN_CIRCLE_PIXERS.get( size )
                                      : null;
            return pixerFact == null
                 ? new MarkerStyle( this, color, size, size + 1, drawShape )
                 : new MarkerStyle( this, color, size, drawShape,
                                    pixerFact.get() );
        }
    };

    /** Factory for filled circle markers. */
    public static final MarkerShape FILLED_CIRCLE =
            new MarkerShape( "filled circle" ) {
        public MarkerStyle getStyle( Color color, final int size ) {
            final int off = -size;
            final int d = size * 2;
            Consumer<Graphics> drawShape = g -> {
                g.fillOval( off, off, d, d );

                /* In pixel-type graphics contexts, the filled circle is
                 * ugly (asymmetric) if the outline is not painted too. */
                if ( ((Graphics2D) g).getRenderingHint( OUTLINE_CIRCLE_HINT )
                     != Boolean.FALSE ) {
                    g.drawOval( off, off, d, d );
                }
            };
            Supplier<Pixer> pixerFact = size < FILLED_CIRCLE_PIXERS.size()
                                      ? FILLED_CIRCLE_PIXERS.get( size )
                                      : null;
            return pixerFact == null
                 ? new MarkerStyle( this, color, size, size + 1, drawShape )
                 : new MarkerStyle( this, color, size, drawShape,
                                    pixerFact.get() );
        }
    };

    /** Factory for open square markers. */
    public static final MarkerShape OPEN_SQUARE =
            new MarkerShape( "open square" ) {
        public MarkerStyle getStyle( Color color, int size ) {
            final int off = -size;
            final int h = size * 2;
            return new MarkerStyle( this, color, size, size + 1,
                                    g -> g.drawRect( off, off, h, h ) );
        }
    };

    /** Factory for filled square markers. */
    public static final MarkerShape FILLED_SQUARE =
            new MarkerShape( "filled square" ) {
        public MarkerStyle getStyle( Color color, int size ) {
            final int off = -size;
            final int h = size * 2 + 1;
            return new MarkerStyle( this, color, size, size + 1,
                                    g -> g.fillRect( off, off, h, h ) );
        }
    };

    /** Factory for cross-hair markers. */
    public static final MarkerShape CROSS = new MarkerShape( "cross" ) {
        public MarkerStyle getStyle( Color color, int size ) {
            final int off = size;
            return new MarkerStyle( this, color, size, size + 1,
                                    g -> {
                                        g.drawLine( -off, 0, off, 0 );
                                        g.drawLine( 0, -off, 0, off );
                                    } );
        }
    };

    /** Factory for X-shaped markers. */
    public static final MarkerShape CROXX = new MarkerShape( "x" ) {
        public MarkerStyle getStyle( Color color, int size ) {
            final int off = size;
            return new MarkerStyle( this, color, size, size + 1,
                                    g -> {
                                        g.drawLine( -off, -off, off, off );
                                        g.drawLine( off, -off, -off, off );
                                    } );
        }
    };

    /** Factory for open diamond shaped markers. */
    public static final MarkerShape OPEN_DIAMOND =
            new MarkerShape( "open diamond" ) {
        public MarkerStyle getStyle( Color color, int size ) {
            return shapeStyle( this, color, size, diamond( size ),
                               true, false );
        }
    };

    /** Factory for open diamond shaped markers. */
    public static final MarkerShape FILLED_DIAMOND =
            new MarkerShape( "filled diamond" ) {
        public MarkerStyle getStyle( Color color, int size ) {
            return shapeStyle( this, color, size, diamond( size ),
                               true, true );
        }
    };

    /** Factory for open triangle shaped markers with point at the top. */
    public static final MarkerShape OPEN_TRIANGLE_UP =
            new MarkerShape( "open triangle up" ) {
        public MarkerStyle getStyle( Color color, int size ) {
            return shapeStyle( this, color, size, triangle( size, true ),
                               true, false );
        }
    };

    /** Factory for open triangle shaped markers with point at the bottom. */
    public static final MarkerShape OPEN_TRIANGLE_DOWN =
            new MarkerShape( "open triangle down" ) {
        public MarkerStyle getStyle( Color color, int size ) {
            return shapeStyle( this, color, size, triangle( size, false ),
                               true, false );
        }
    };

    /** Factory for filled triangle shaped markers with point at the top. */
    public static final MarkerShape FILLED_TRIANGLE_UP =
            new MarkerShape( "filled triangle up" ) {
        public MarkerStyle getStyle( Color color, int size ) {
            return shapeStyle( this, color, size, triangle( size, true ),
                               false, true );
        }
    };

    /** Factory for filled triangle shaped markers with point at the bottom. */
    public static final MarkerShape FILLED_TRIANGLE_DOWN =
            new MarkerShape( "filled triangle down" ) {
        public MarkerStyle getStyle( Color color, int size ) {
            return shapeStyle( this, color, size, triangle( size, false ),
                               false, true );
        }
    };

   /**
     * Returns a marker style based on a given Shape object.
     *
     * @param  shapeId  marker shape object
     * @param  color  colour
     * @param  size   nominal size
     * @param  shape  shape
     * @param  draw   true to draw outline
     * @param  fill   true to fill body
     * @return  marker style
     */
    private static MarkerStyle shapeStyle( MarkerShape shapeId, Color color,
                                           int size, final Shape shape,
                                           boolean draw, boolean fill ) {
        final Consumer<Graphics> drawShape;
        if ( draw && fill ) {
            drawShape = g -> {
                Graphics2D g2 = (Graphics2D) g;
                g2.fill( shape );
                g2.draw( shape );
            };
        }
        else if ( draw ) {
            drawShape = g -> ((Graphics2D) g).draw( shape );
        }
        else if ( fill ) {
            drawShape = g -> ((Graphics2D) g).fill( shape );
        }
        else {
            drawShape = g -> {};
        }
        return new MarkerStyle( shapeId, color, size, getMaxRadius( shape ),
                                drawShape );
    }

    /**
     * Works out an upper bound for the radius associated with a given shape.
     *                  
     * @param   shape  shape to assess
     * @return  upper bound on shape's radius
     */
    private static int getMaxRadius( Shape shape ) {
        Rectangle rect = shape.getBounds();
        int[] bounds = new int[] { rect.x, rect.x + rect.width,
                                   rect.y, rect.y + rect.height };
        int maxr = 1;
        for ( int i = 0; i < bounds.length; i++ ) {
            maxr = Math.max( maxr, Math.abs( bounds[ i ] ) );
        }
        return maxr;
    }

    /**
     * Returns a diamond shape.
     *
     * @param  size  approximate diamond radius
     * @return diamond shape
     */
    private static Shape diamond( int size ) {
        return new Polygon( new int[] { -size, 0, size, 0 },
                            new int[] { 0, -size, 0, size }, 4 );
    }

    /**
     * Returns a triangle shape.
     *
     * @param  size  approximate triangle radius
     * @param  up    true for pointing upwards, false for pointing down
     * @return  triangle shape
     */
    private static Shape triangle( int size, boolean up ) {
        double scale = size * 1.5;
        int c = (int) Math.round( scale * Math.sqrt( 3 ) / 2.0 ); // s.cos(30)
        int s = (int) Math.round( scale * 0.5 );                  // s.sin(30)
        int r = (int) Math.round( scale );
        return up
            ? new Polygon( new int[] { -c, 0, c }, new int[] { -s, r, -s }, 3 )
            : new Polygon( new int[] { -c, 0, c }, new int[] { s, -r, s }, 3 );
    }

    /**
     * Returns the x,y pixel offsets for open circles of various sizes.
     * For some reason the pixels you get when you call drawOval() with a
     * small radius on a graphics got from a BufferedImage is ugly
     * (not very circular or symmetrical), so we construct these by hand.
     * The returned value is an array of offset pixel iterators;
     * the first element is for size 0, the second for size 1, etc.
     *
     * @return  array of pixel offset iterators for progressively increasing
     *          sizes of open circle
     */
    private static List<Supplier<Pixer>> calculateOpenCirclePixers() {
        return rotatePixelLists( new int[][] {
            { 0,0, },
            { 0,1, 1,1 },
            { 0,2, 1,2, },
            { 0,3, 1,3, 2,2, },
            { 0,4, 1,4, 2,4, 3,3, },
            { 0,5, 1,5, 2,5, 3,4, },
        } );
    }

    /**
     * Returns the x,y pixel offsets for filled circles of various sizes.
     * For some reason the pixels you get when you call fillOval() with a
     * small radius on a graphics got from a BufferedImage is ugly
     * (not very circular or symmetrical), so we construct these by hand.
     * The returned value is an array of offset pixel iterators;
     * the first element is for size 0, the second for size 1, etc.
     *
     * @return  array of pixel offset iterators for progressively increasing
     *          sizes of filled circle
     */
    private static List<Supplier<Pixer>> calculateFilledCirclePixers() {
        return rotatePixelLists( new int[][] {
            { 0,0, },
            { 0,0, 0,1, 1,1 },
            { 0,0, 0,1, 0,2, 1,1, 1,2, },
            { 0,0, 0,1, 0,2, 0,3, 1,1, 1,2, 1,3, 2,2, },
            { 0,0, 0,1, 0,2, 0,3, 0,4, 1,1, 1,2, 1,3, 1,4, 2,2, 2,3, 2,4, 3,3 },
            { 0,0, 0,1, 0,2, 0,3, 0,4, 0,5, 1,1, 1,2, 1,3, 1,4, 1,5,
              2,2, 2,3, 2,4, 2,5, 3,3, 3,4 },
        } );
    }

    /**
     * Takes a list of arrays of X,Y positions and returns a list of arrays
     * containing the same data plus all their eight symmetrical images.
     * All coordinate pairs in the output list are distinct.
     * The input list is in the form of an array of
     * int arrays; these int arrays contain coordinates in the form
     * (x0,y0, x1,y1, x2,y2,...).
     *
     * @param  seeds   input list of coordinate arrays
     * @return  output array of pixellators, one for each input coordinate array
     */
    private static List<Supplier<Pixer>> rotatePixelLists( int[][] seeds ) {
        List<Supplier<Pixer>> results = new ArrayList<>( seeds.length );
        for ( int size = 0; size < seeds.length; size++ ) {
            int[] seed = seeds[ size ];
            Set<Point> pointSet = new HashSet<Point>();
            for ( int ip = 0; ip < seed.length / 2; ip++ ) {
                int a = seed[ ip * 2 + 0 ];
                int b = seed[ ip * 2 + 1 ];
                pointSet.add( new Point( +a, +b ) );
                pointSet.add( new Point( -b, +a ) );
                pointSet.add( new Point( -a, -b ) );
                pointSet.add( new Point( +b, -a ) );
                pointSet.add( new Point( +b, +a ) );
                pointSet.add( new Point( -a, +b ) );
                pointSet.add( new Point( -b, -a ) );
                pointSet.add( new Point( +a, -b ) );
            }
            Point[] points = pointSet.toArray( new Point[ 0 ] );
            results.add( () -> Pixers.createPointsPixer( points ) );
        }
        return results;
    }
}
