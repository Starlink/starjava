package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import uk.ac.starlink.ttools.plot2.Equality;
import uk.ac.starlink.ttools.plot2.Glyph;
import uk.ac.starlink.ttools.plot2.Pixer;

/**
 * Object that knows how to draw a polygon.
 *
 * @author   Mark Taylor
 * @since    5 Oct 2021
 */
@Equality
public abstract class PolygonShape {

    private final String name_;
    private final String description_;

    /** Array of known shape instances. */
    public static final PolygonShape[] POLYSHAPES = createPolygonShapes();

    /**
     * Constructor.
     *
     * @param  name   name
     * @param  description  short human-readable description
     */
    protected PolygonShape( String name, String description ) {
        name_ = name;
        description_ = description;
    }

    /**
     * Returns a glyph representing a polygon with the given vertices.
     *
     * @param  x0  X coordinate of nominal center
     * @param  y0  Y coordinate of nominal center
     * @param  xs  X coordinates of vertices
     * @param  ys  Y coordinates of vertices
     * @param  np  number of vertices
     */
    public abstract Glyph createPolygonGlyph( int x0, int y0,
                                              int[] xs, int[] ys, int np );

    /**
     * Returns a version of this shape with thicker lines;
     * may be this object if line thickening would have no effect.
     *
     * @param  nthick  line thickness index &gt;=0
     * @return  thicker shape
     */
    public abstract PolygonShape toThicker( int nthick );

    /**
     * Returns this shape's name.
     *
     * @return  name
     */
    public String getName() {
        return name_;
    }

    /**
     * Returns a short description for this shape.
     *
     * @return  human-readable plain-text description
     */
    public String getDescription() {
        return description_;
    }

    @Override
    public String toString() {
        return name_;
    }

    /**
     * Returns the bounding rectangle for a polygon.
     *
     * @param  xs  polygon vertex X coordinates
     * @param  ys  polygon vertex Y coordinates
     * @param  np  number of vertices
     */
    private static Rectangle getBounds( int[] xs, int[] ys, int np ) {
        if ( np > 0 ) {
            int xlo = xs[ 0 ];
            int ylo = ys[ 0 ];
            int xhi = xs[ 0 ];
            int yhi = ys[ 0 ];
            for ( int ip = 0; ip < np; ip++ ) {
                xlo = Math.min( xlo, xs[ ip ] );
                ylo = Math.min( ylo, ys[ ip ] );
                xhi = Math.max( xhi, xs[ ip ] );
                yhi = Math.max( yhi, ys[ ip ] );
            }
            return new Rectangle( xlo, ylo, xhi - xlo + 1, yhi - ylo + 1 );
        }
        else {
            return new Rectangle();
        }
    }

    /**
     * Provides a list of all the preset instances of this class.
     *
     * @return  shape list
     */
    private static PolygonShape[] createPolygonShapes() {
        return new PolygonShape[] {
            createOutlineShape( "outline",
                                "draws a line round the outside of the polygon",
                                false ),
            createOutlineShape( "border",
                                "draws a line butting up to the outside "
                              + "of the polygon; "
                              + "may look better for adjacent shapes, "
                              + "but more expensive to draw",
                                true ),
            createFillShape( "fill", "fills the interior of the polygon" ),
            createCrossShape( "cross",
                              "draws a line round the outside of the polygon "
                            + "and lines between all the vertices" ),
            createStarShape( "star",
                             "draws a line round the outside of the polygon "
                           + "and lines from the nominal center "
                           + "to each vertex" ),
        };
    }

    /**
     * Returns a shape instance that draws round the outline of a polygon.
     * Clipping is optional because it's expensive and not strictly
     * necessary, though clipped and unclipped give different appearances
     * for thick lines.
     *
     * @param  name   name
     * @param  descrip  short human-readable description
     * @param  isClip  if true, the result will be clipped to the interior
     *                 of the polygon for nthick>0
     * @return  new shape
     */
    private static PolygonShape createOutlineShape( String name, String descrip,
                                                    boolean isClip ) {
        return new LineShape( name, descrip, isClip ) {
            public LineGlyph createPolygonGlyph( int x0, int y0,
                                                 int[] xs, int[] ys, int np ) {
                return new LineGlyph() {
                    public void paintGlyph( Graphics g, StrokeKit strokeKit ) {
                        Graphics2D g2 = (Graphics2D) g;
                        Stroke stroke0 = g2.getStroke();
                        g2.setStroke( strokeKit.getRound() );
                        g2.drawPolygon( xs, ys, np );
                        g2.setStroke( stroke0 );
                    }
                    public Rectangle getPixelBounds() {
                        return getBounds( xs, ys, np );
                    }
                    public void drawShape( PixelDrawing drawing ) {
                        for ( int ip = 0; ip < np; ip++ ) {
                            int ip1 = ( ip + 1 ) % np;
                            drawing.drawLine( xs[ ip ], ys[ ip ],
                                              xs[ ip1 ], ys[ ip1 ] );
                        }
                    }
                };
            }
        };
    }

    /**
     * Returns a shape instance that draws lines between all vertices.
     * The interior decoration is clipped because for re-entrant shapes
     * the lines would go outside the bounds.
     *
     * @param  name   name
     * @param  descrip  short human-readable description
     * @return  new shape
     */
    private static PolygonShape createCrossShape( String name, String descrip ){
        return new LineShape( name, descrip, false ) {
            public LineGlyph createPolygonGlyph( int x0, int y0,
                                                 int[] xs, int[] ys, int np ) {
                return new ClipLineGlyph( xs, ys, np ) {
                    public void paintInterior( Graphics g ) {
                        for ( int ip = 0; ip < np; ip++ ) {
                            for ( int ip2 = ip + 2; ip2 < np; ip2++ ) {
                                g.drawLine( xs[ ip ], ys[ ip ],
                                            xs[ ip2 ], ys[ ip2 ] );
                            }
                        }
                    }
                    public void drawInterior( PixelDrawing d ) {
                        for ( int ip = 0; ip < np; ip++ ) {
                            for ( int ip2 = ip + 2; ip2 < np; ip2++ ) {
                                d.drawLine( xs[ ip ], ys[ ip ],
                                            xs[ ip2 ], ys[ ip2 ] );
                            }
                        }
                    }
                };
            }
        };
    }

    /**
     * Returns a shape instance that draws radial lines.
     * The interior decoration is clipped because for funny shapes
     * the lines might go outside the polygon.
     *
     * @param  name   name
     * @param  descrip  short human-readable description
     * @return  new shape
     */
    private static PolygonShape createStarShape( String name, String descrip ) {
        return new LineShape( name, descrip, false ) {
            public LineGlyph createPolygonGlyph( int x0, int y0,
                                                 int[] xs, int[] ys, int np ) {
                return new ClipLineGlyph( xs, ys, np ) {
                    public void paintInterior( Graphics g ) {
                        for ( int ip = 0; ip < np; ip++ ) {
                            g.drawLine( x0, y0, xs[ ip ], ys[ ip ] );
                        }
                    }
                    public void drawInterior( PixelDrawing d ) {
                        for ( int ip = 0; ip < np; ip++ ) {
                            d.drawLine( x0, y0, xs[ ip ], ys[ ip ] );
                        }
                    }
                };
            }
        };
    }

    /**
     * Returns a shape instance that fills in the polygon.
     *
     * @param  name   name
     * @param  descrip  short human-readable description
     * @return  new shape
     */
    private static PolygonShape createFillShape( String name, String descrip ) {
        return new PolygonShape( name, descrip ) {
            public Glyph createPolygonGlyph( int x0, int y0,
                                             int[] xs, int[] ys, int np ) {
                return new Glyph() {
                    public void paintGlyph( Graphics g ) {
                        g.fillPolygon( xs, ys, np );
                    }
                    public Pixer createPixer( Rectangle clip ) {
                        return getBounds( xs, ys, np ).intersects( clip )
                             ? new FillPixer( xs, ys, np, clip )
                             : null;
                    }
                };
            }
            public PolygonShape toThicker( int nthick ) {
                return this;
            }
        };
    }

    /**
     * Partial PolygonShape implementation that draws lines.
     */
    private static abstract class LineShape extends PolygonShape {

        private final boolean isClip_;

        /**
         * Constructor.
         *
         * @param  name   name
         * @param  description  short human-readable description
         * @param  isClip  true iff clipping should be applied to
         *                 thicker shapes
         */
        LineShape( String name, String description, boolean isClip ) {
            super( name, description );
            isClip_ = isClip;
        }

        /**
         * Overridden to return a LineGlyph.
         */
        @Override
        public abstract LineGlyph createPolygonGlyph( int x0, int y0,
                                                      int[] xs, int[] ys,
                                                      int np );

        public FatShape toThicker( int nthick ) {
            return new FatShape( this, nthick, isClip_ );
        }
    }

    /**
     * PolygonShape instance based on a LineShape but with
     * thicker drawing lines.
     */
    private static class FatShape extends PolygonShape {

        private final LineShape lineShape_;
        private final int nthick_;
        private final boolean isClip_;
        private final PixerFactory kernel_;
        private final StrokeKit strokeKit_;

        /**
         * Constructor.
         *
         * @param  baseShape  basic shape
         * @param  nthick  thickness index &gt;=0
         * @param  isClip  true if clipping should be applied to the output
         */
        FatShape( LineShape lineShape, int nthick, boolean isClip ) {
            super( lineShape.getName() + nthick,
                   lineShape.getDescription() + " at line thickness " +nthick );
            lineShape_ = lineShape;
            nthick_ = nthick;
            isClip_ = isClip;
            kernel_ = LineGlyph.createThickKernel( nthick );
            strokeKit_ = LineGlyph.createThickStrokeKit( nthick );
        }

        public Glyph createPolygonGlyph( int x0, int y0, int[] xs, int[] ys,
                                         int np ) {
            DrawingGlyph thickGlyph = lineShape_
                                     .createPolygonGlyph( x0, y0, xs, ys, np )
                                     .toThicker( kernel_, strokeKit_ );
            return isClip_ ? new ClipGlyph( thickGlyph, xs, ys, np )
                           : thickGlyph;
        }

        public PolygonShape toThicker( int nthick ) {
            return this;
        }

        @Override
        public int hashCode() {
            int code = -558294;
            code = 23 * code + lineShape_.hashCode();
            code = 23 * code + nthick_;
            code = 23 * code + ( isClip_ ? 11 : 17 );
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof FatShape ) {
                FatShape other = (FatShape) o;
                return this.lineShape_.equals( other.lineShape_ )
                    && this.nthick_ == other.nthick_
                    && this.isClip_ == other.isClip_;
            }
            else {
                return false;
            }
        }
    }

    /**
     * Applies a clipping polygon to a DrawingGlyph.
     */
    private static class ClipGlyph implements Glyph {

        private final DrawingGlyph baseGlyph_;
        private final int[] xs_;
        private final int[] ys_;
        private final int np_;

        /**
         * Constructor.
         *
         * @param  baseGlyph  base glyph
         * @param  xs   vertex X coordinate array
         * @param  ys   vertex Y coordinate array
         * @param  np   vertex count
         */
        ClipGlyph( DrawingGlyph baseGlyph, int[] xs, int[] ys, int np ) {
            baseGlyph_ = baseGlyph;
            xs_ = xs;
            ys_ = ys;
            np_ = np;
        }

        public void paintGlyph( Graphics g ) {
            Graphics2D g2 = (Graphics2D) g;
            Shape clip0 = g2.getClip();
            g2.clip( new Polygon( xs_, ys_, np_ ) );
            baseGlyph_.paintGlyph( g2 );
            g2.setClip( clip0 );
        }

        public Pixer createPixer( Rectangle clip ) {
            PixelDrawing drawing = baseGlyph_.createPixerFactory( clip );
            if ( drawing == null ) {
                return null;
            }
            PixelDrawing mask =
                new PixelDrawing( drawing.getMinX(), drawing.getMinY(),
                                  drawing.getMaxX() - drawing.getMinX() + 1,
                                  drawing.getMaxY() - drawing.getMinY() + 1 );
            mask.fillPolygon( xs_, ys_, np_ );
            drawing.getPixels().and( mask.getPixels() );
            return drawing.createPixer();
        }
    }

    /**
     * Partial LineGlyph that draws the outside of a polygon and
     * clips supplied interior decorations to the polygon bounds.
     */
    private static abstract class ClipLineGlyph extends LineGlyph {

        private final int[] xs_;
        private final int[] ys_;
        private final int np_;

        /**
         * Constructor.
         *
         * @param  xs   vertex X coordinate array
         * @param  ys   vertex Y coordinate array
         * @param  np   vertex count
         */
        ClipLineGlyph( int[] xs, int[] ys, int np ) {
            xs_ = xs;
            ys_ = ys;
            np_ = np;
        }

        /**
         * Paints interior decoration of a polygon.
         * This will be clipped.
         *
         * @param  g  graphics context
         */
        abstract void paintInterior( Graphics g );

        /**
         * Draws interior pixels for a polygon.
         * This will be clipped.
         *
         * @param  drawing  graphics destination
         */
        abstract void drawInterior( PixelDrawing drawing );

        public Rectangle getPixelBounds() {
            return getBounds( xs_, ys_, np_ );
        }

        public void paintGlyph( Graphics g, StrokeKit strokeKit ) {
            Graphics2D g2 = (Graphics2D) g;
            Stroke stroke0 = g2.getStroke();
            Shape clip0 = g2.getClip();
            g2.setStroke( strokeKit.getRound() );
            g2.clip( new Polygon( xs_, ys_, np_ ) );
            paintInterior( g2 );
            g2.setClip( clip0 );
            g2.drawPolygon( xs_, ys_, np_ );
            g2.setStroke( stroke0 );
        }

        public void drawShape( PixelDrawing drawing ) {
            PixelDrawing mask =
                new PixelDrawing( drawing.getMinX(), drawing.getMinY(),
                                  drawing.getMaxX() - drawing.getMinX() + 1,
                                  drawing.getMaxY() - drawing.getMinY() + 1 );
            mask.fillPolygon( xs_, ys_, np_ );
            drawInterior( drawing );
            drawing.getPixels().and( mask.getPixels() );
            for ( int ip = 0; ip < np_; ip++ ) {
                int ip1 = ( ip + 1 ) % np_;
                drawing.drawLine( xs_[ ip ], ys_[ ip ], xs_[ ip1 ], ys_[ ip1 ]);
            }
        }
    }
}
