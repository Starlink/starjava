package uk.ac.starlink.ttools.plot;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Defines the abstract shape of a MarkStyle.  Instances of this class
 * are factories which can produce a family of MarkStyle objects with
 * a shape which is in some sense the same, but of various sizes and
 * colours.
 *
 * @author   Mark Taylor
 * @since    9 Jan 2006
 */
public abstract class MarkShape {

    private final String name_;

    private static final Pixellator[] OPEN_CIRCLE_PIXELS =
        calculateOpenCirclePixels();
    private static final Pixellator[] FILLED_CIRCLE_PIXELS =
        calculateFilledCirclePixels();

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

    /**
     * Constructor.
     *
     * @param   name  shape name
     */
    public MarkShape( String name ) {
        name_ = name;
    }

    /**
     * Factory method which produces a MarkStyle of the shape characteristic
     * of this object with specified colour and nominal size.
     *
     * @param  color   colour of style
     * @param  size    nominal size of style - any integer, or at least any
     *                 integer &gt;0 should give a reasonable image
     */
    public abstract MarkStyle getStyle( Color color, int size );

    public String toString() {
        return name_;
    }

    /** Factory for point-like markers.  The size parameter is ignored. */
    public static final MarkShape POINT = new MarkShape( "point" ) {
        final Object id = new Object();
        public MarkStyle getStyle( Color color, int size ) {
            return new MarkStyle( color, id, this, 0, 1 ) {
                public void drawShape( Graphics g ) {
                    g.fillRect( 0, 0, 1, 1 );
                }
                public void drawLegendShape( Graphics g ) {
                    g.fillRect( 0, 0, 2, 2 );
                }
            };
        }
    };

    /** Factory for open circle markers. */
    public static final MarkShape OPEN_CIRCLE = new MarkShape( "open circle" ) {
        public MarkStyle getStyle( Color color, final int size ) {
            final int off = -size;
            final int diam = size * 2;
            return new MarkStyle( color, Integer.valueOf( size ),
                                  this, size, size + 1 ) {
                public void drawShape( Graphics g ) {
                    g.drawOval( off, off, diam, diam );
                }
                public Pixellator getPixelOffsets() {
                    return size < OPEN_CIRCLE_PIXELS.length 
                         ? OPEN_CIRCLE_PIXELS[ size ]
                         : super.getPixelOffsets();
                }
            };
        }
    };

    /** Factory for filled circle markers. */
    public static final MarkShape FILLED_CIRCLE =
            new MarkShape( "filled circle" ) {
        public MarkStyle getStyle( Color color, final int size ) {
            final int off = -size;
            final int diam = size * 2;
            return new MarkStyle( color, Integer.valueOf( size ), 
                                  this, size, size + 1 ) {
                public void drawShape( Graphics g ) {
                    g.fillOval( off, off, diam, diam );

                    /* In pixel-type graphics contexts, the filled circle is
                     * ugly (asymmetric) if the outline is not painted too. */
                    if ( ((Graphics2D) g)
                        .getRenderingHint( OUTLINE_CIRCLE_HINT )
                             != Boolean.FALSE ) {
                        g.drawOval( off, off, diam, diam );
                    }
                }
                public Pixellator getPixelOffsets() {
                    return size < FILLED_CIRCLE_PIXELS.length
                         ? FILLED_CIRCLE_PIXELS[ size ]
                         : super.getPixelOffsets();
                }
            };
        }
    };

    /** Factory for open square markers. */
    public static final MarkShape OPEN_SQUARE = new MarkShape( "open square" ) {
        public MarkStyle getStyle( Color color, int size ) {
            final int off = -size;
            final int height = size * 2;
            return new MarkStyle( color, Integer.valueOf( size ),
                                  this, size, size + 1 ) {
                public void drawShape( Graphics g ) {
                    g.drawRect( off, off, height, height );
                }
            };
        }
    };

    /** Factory for filled square markers. */
    public static final MarkShape FILLED_SQUARE =
            new MarkShape( "filled square" ) {
        public MarkStyle getStyle( Color color, int size ) {
            final int off = -size;
            final int height = size * 2 + 1;
            return new MarkStyle( color, Integer.valueOf( size ),
                                  this, size, size + 1 ) {
                public void drawShape( Graphics g ) {
                     g.fillRect( off, off, height, height );
                }
            };
        }
    };

    /** Factory for cross-hair markers. */
    public static final MarkShape CROSS = new MarkShape( "cross" ) {
        public MarkStyle getStyle( Color color, int size ) {
            final int off = size;
            return new MarkStyle( color, Integer.valueOf( size ),
                                  this, size, size + 1 ) {
                public void drawShape( Graphics g ) {
                    g.drawLine( -off, 0, off, 0 );
                    g.drawLine( 0, -off, 0, off );
                }
            };
        }
    };

    /** Factory for X-shaped markers. */
    public static final MarkShape CROXX = new MarkShape( "x" ) {
        public MarkStyle getStyle( Color color, int size ) {
            final int off = size;
            return new MarkStyle( color, Integer.valueOf( size ),
                                  this, size, size + 1 ) {
                public void drawShape( Graphics g ) {
                    g.drawLine( -off, -off, off, off );
                    g.drawLine( off, -off, -off, off );
                }
            };
        }
    };

    /** Factory for open diamond shaped markers. */
    public static final MarkShape OPEN_DIAMOND =
            new MarkShape( "open diamond" ) {
        public MarkStyle getStyle( Color color, int size ) {
            return shapeStyle( color, diamond( size ), this, size,
                               true, false );
        }
    };

    /** Factory for filled diamond shaped markers. */
    public static final MarkShape FILLED_DIAMOND =
            new MarkShape( "filled diamond" ) {
        public MarkStyle getStyle( Color color, int size ) {
            return shapeStyle( color, diamond( size ), this, size,
                               true, true );
        }
    };

    /** Factory for open triangle shaped markers with point at the top. */
    public static final MarkShape OPEN_TRIANGLE_UP =
            new MarkShape( "open triangle up" ) {
        public MarkStyle getStyle( Color color, int size ) {
            return shapeStyle( color, triangle( size, true ), this, size,
                               true, false );
        }
    };

    /** Factory for open triangle shaped markers with point at the bottom. */
    public static final MarkShape OPEN_TRIANGLE_DOWN =
            new MarkShape( "open triangle down" ) {
        public MarkStyle getStyle( Color color, int size ) {
            return shapeStyle( color, triangle( size, false ), this, size,
                               true, false );
        }
    };

    /** Factory for filled triangle shaped markers with point at the top. */
    public static final MarkShape FILLED_TRIANGLE_UP =
            new MarkShape( "filled triangle up" ) {
        public MarkStyle getStyle( Color color, int size ) {
            return shapeStyle( color, triangle( size, true ), this, size,
                               false, true );
        }
    };

    /** Factory for filled triangle shaped markers with point at the bottom. */
    public static final MarkShape FILLED_TRIANGLE_DOWN =
            new MarkShape( "filled triangle down" ) {
        public MarkStyle getStyle( Color color, int size ) {
            return shapeStyle( color, triangle( size, false ), this, size,
                               false, true );
        }
    };

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
     * Returns an open shape marker style.
     *
     * @param  color  colour
     * @param  shape  shape
     * @return  marker style
     */
    private static MarkStyle shapeStyle( Color color, final Shape shape,
                                         MarkShape shapeId, int size,
                                         final boolean draw,
                                         final boolean fill ) {
        return new MarkStyle( color, shape,
                              shapeId, size, getMaxRadius( shape ) ) {
            public void drawShape( Graphics g ) {
                if ( g instanceof Graphics2D ) {
                    Graphics2D g2 = (Graphics2D) g;
                    if ( fill ) {
                        g2.fill( shape );
                    }
                    if ( draw ) {
                        g2.draw( shape );
                    }
                }
                else {
                    if ( fill ) {
                        g.fillRect( -1, -1, 2, 2 );
                    }
                    if ( draw ) {
                        g.drawRect( -1, -1, 2, 2 );
                    }
                }
            }
        };
    }

    /**
     * Returns a filled shape marker style.
     *
     * @param  color  colour
     * @param  shape  shape
     * @return  marker style
     */
    private static MarkStyle filledShapeStyle( Color color, final Shape shape,
                                               MarkShape shapeId, int size ) {
        return new MarkStyle( color, shape,
                              shapeId, size, getMaxRadius( shape ) ) {
            public void drawShape( Graphics g ) {
                if ( g instanceof Graphics2D ) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.fill( shape );
                }
                else {
                    g.fillRect( -1, -1, 2, 2 );
                }
            }
        };
    }

    /**
     * Returns a diamond shape.
     *
     * @param  size  approximate diamond radius
     * @return diamond shape
     */
    private static Shape diamond( int size ) {
        return new TestablePolygon( new int[] { -size, 0, size, 0 },
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
        return up ? new TestablePolygon( new int[] { -c, 0, c },
                                         new int[] { -s, r, -s }, 3 )
                  : new TestablePolygon( new int[] { -c, 0, c },
                                         new int[] { s, -r, s }, 3 );
    }

    /**
     * Extension of Polygon which implements <code>equals()</code> so as
     * to spot whether two shapes are the same or not.
     * Polygon itself uses object identity.
     */
    private static class TestablePolygon extends Polygon {
        public TestablePolygon( int[] xpoints, int[] ypoints, int npoints ) {
            super( xpoints, ypoints, npoints );
        }
        public boolean equals( Object other ) {
            if ( other instanceof TestablePolygon ) {
                TestablePolygon o = (TestablePolygon) other;
                return npoints == o.npoints
                    && Arrays.equals( xpoints, o.xpoints )
                    && Arrays.equals( ypoints, o.ypoints );
            }
            else {
                return false;
            }
        }
        public int hashCode() {
            int code = 5555;
            for ( int i = 0; i < npoints; i++ ) {
                code = 23 * code + xpoints[ i ];
                code = 23 * code + ypoints[ i ];
            }
            return code;
        }
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
    private static Pixellator[] calculateOpenCirclePixels() {
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
    private static Pixellator[] calculateFilledCirclePixels() {
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
    private static Pixellator[] rotatePixelLists( int[][] seeds ) {
        Pixellator[] results = new Pixellator[ seeds.length ];
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
            results[ size ] = new PointArrayPixellator( points );
        }
        return results;
    }
}
