package uk.ac.starlink.topcat.plot;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;

/**
 * Defines a style of marker for plotting.
 * A number of static factory methods generating useful MarkStyle 
 * instances are provided.
 *
 * @author   Mark Taylor (Starlink)
 * @since    16 Jun 2004
 */
public abstract class MarkStyle {

    private Color color_;

    private static Color[] COLORS = new Color[] {
        Color.red, Color.blue, Color.green,
        Color.cyan, Color.magenta,
        Color.orange, Color.pink,
    };

    /**
     * Constructs a marker with a default colour.
     */
    protected MarkStyle() {
        this( Color.BLACK );
    }

    /**
     * Constructs a marker with a given colour.
     *
     * @param  color  colour
     */
    protected MarkStyle( Color color ) {
        color_ = color;
    }

    /**
     * Draws this marker's shape at a given point in a graphics context.
     * Implementing classes don't need to worry about the colour.
     *
     * @param  g  graphics context
     * @param  x  x position
     * @param  y  y position
     */
    protected abstract void drawShape( Graphics g, int x, int y );

    /**
     * Returns the maximum radius of a marker drawn by this class.
     * It is permissible to return a (gross) overestimate if no sensible
     * maximum can be guaranteed.
     *
     * @return   maximum distance from the specified <tt>x</tt>,<tt>y</tt>
     *           point that <tt>drawMarker</tt> might draw
     */
    public abstract int getMaximumRadius();

    /**
     * Draws this marker at a given position.  This method sets the colour
     * of the graphics context and then calls {@link #drawShape}.
     *
     * @param  g  graphics context
     * @param  x  x position
     * @param  y  y position
     */
    public void drawMarker( Graphics g, int x, int y ) {
         Color col = g.getColor();
         g.setColor( color_ );
         drawShape( g, x, y );
         g.setColor( col );
    }

    /**
     * Returns this marker's colour.
     *
     * @return  colour
     */
    public Color getColor() {
        return color_;
    }

    /**
     * Sets this marker's colour.
     *
     * @param  color  colour
     */
    public void setColor( Color color ) {
        color_ = color;
    }

    /**
     * Checks equivalence of class and colour.
     */
    public boolean equals( Object o ) {
        if ( o instanceof MarkStyle ) {
            MarkStyle other = (MarkStyle) o;
            return getClass().equals( other.getClass() ) 
                && color_.equals( other.color_ );
        }
        else {
            return false;
        }
    }

    public int hashCode() {
        int code = 5;
        code = code * 23 + getClass().hashCode();
        code = code * 23 + color_.hashCode();
        return code;
    }

    /**
     * Provides some kind of sensible marker.  The same marker will always
     * be returned for a given value of <tt>type</tt>.
     *
     * @param  type  distinguisher for marker types
     */
    public static MarkStyle defaultStyle( int type ) {
        Color color = COLORS[ Math.abs( type % COLORS.length ) ];
        switch ( Math.abs( type ) % 4 ) {
            case 0:
               return filledCircleStyle( color, 2 );
            case 1:
               return filledSquareStyle( color, 2 );
            case 2:
               return filledDiamondStyle( color, 2 );
            case 3:
               return crossStyle( color, 2 );
            default:
               throw new AssertionError();
        }
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
            maxr = Math.max( maxr, Math.max( bounds[ i ], - bounds[ i ] ) );
        }
        return maxr;
    }

    /**
     * Convenience implementation of MarkStyle used by some of the 
     * factory methods in this class.  They implement the <tt>equals</tt>
     * and <tt>hashCode</tt> methods using a single final object
     * <tt>otherAtts</tt> which characterises everything apart from the
     * class and colour which distinguishes an instance from another.
     */
    private static abstract class ConvenienceMarkStyle extends MarkStyle {
        final Object otherAtts_;
        final int maxr_;

        ConvenienceMarkStyle( Color color, Object otherAtts, int maxr ) {
            super( color );
            otherAtts_ = otherAtts;
            maxr_ = maxr;
        }

        public int getMaximumRadius() {
            return maxr_;
        }

        public boolean equals( Object o ) {
            return super.equals( o )
                 ? ((ConvenienceMarkStyle) o).otherAtts_.equals( otherAtts_ )
                 : false;
        }

        public int hashCode() {
            return super.hashCode() * 23 + otherAtts_.hashCode();
        }
    }

    /**
     * Returns an open circle marker style.
     *
     * @param  color  colour
     * @param  size   approximate circle radius
     * @return  marker style
     */
    public static MarkStyle openCircleStyle( Color color, final int size ) {
        final int off = size;
        final int diam = size * 2;
        return new ConvenienceMarkStyle( color, new Integer( size ), off + 1 ) {
            protected void drawShape( Graphics g, int x, int y ) {
                g.drawOval( x - off, y - off, diam, diam );
            }
        };
    }

    /**
     * Returns a filled circle marker style.
     *
     * @param  color   colour
     * @param  size    approximate circle radius
     * @return  marker style
     */
    public static MarkStyle filledCircleStyle( Color color, final int size ) {
        final int off = size;
        final int diam = size * 2;
        return new ConvenienceMarkStyle( color, new Integer( size ), off + 1 ) {
            protected void drawShape( Graphics g, int x, int y ) {
                int xo = x - off;
                int yo = y - off;
                g.fillOval( xo, yo, diam, diam );

                /* In pixel-type graphics contexts, the filled circle is
                 * ugly (asymmetric) if the outline is not painted too. */
                g.drawOval( xo, yo, diam, diam );
            }
        };
    }

    /**
     * Returns an open square marker style.
     * 
     * @param  color  colour
     * @param  size   approximate square radius
     * @return  marker style
     */
    public static MarkStyle openSquareStyle( Color color, final int size ) {
        final int off = size;
        final int height = size * 2;
        return new ConvenienceMarkStyle( color, new Integer( size ), off + 1 ) {
            protected void drawShape( Graphics g, int x, int y ) {
                g.drawRect( x - off, y - off, height, height );
            }
        };
    }

    /**
     * Returns a filled square marker style.
     *
     * @param  color   colour
     * @param  size    approximate square radius
     * @return  marker style
     */
    public static MarkStyle filledSquareStyle( Color color, final int size ) {
        final int off = size;
        final int height = size * 2 + 1;
        return new ConvenienceMarkStyle( color, new Integer( size ), off + 1 ) {
           protected void drawShape( Graphics g, int x, int y ) {
                g.fillRect( x - off, y - off, height, height );
            }
        };
    }

    /**
     * Returns a crosshair marker style.
     *
     * @param  color   colour
     * @param  size    approximate cross radius
     * @return  marker style
     */
    public static MarkStyle crossStyle( Color color, final int size ) {
        final int off = size;
        return new ConvenienceMarkStyle( color, new Integer( size ), off + 1 ) {
            protected void drawShape( Graphics g, int x, int y ) {
                g.drawLine( x - off, y, x + off, y );
                g.drawLine( x, y - off, x, y + off );
            }
        };
    }

    /**
     * Returns a marker style like an X.
     *
     * @param color  colour
     * @param size   approximate cross radius
     * @return  marker style
     */
    public static MarkStyle xStyle( Color color, final int size ) {
        final int off = size;
        return new ConvenienceMarkStyle( color, new Integer( size ), off + 1 ) {
            protected void drawShape( Graphics g, int x, int y ) {
                g.drawLine( x - off, y - off, x + off, y + off );
                g.drawLine( x + off, y - off, x - off, y + off );
            }
        };
    }

    /**
     * Returns an open diamond marker style.
     *
     * @param  color   colour
     * @param  size    approximate diamond radius
     * @return   marker style
     */
    public static MarkStyle openDiamondStyle( Color color, final int size ) {
        int off = size;
        Shape di = new Polygon( new int[] { -off, 0, off, 0 },
                                new int[] { 0, -off, 0, off }, 4 );
        return openShapeStyle( color, di );
    }

    /**
     * Returns a filled diamond marker style.
     *
     * @param  color  colour
     * @param  size   approximate diamond radius
     * @return  marker style
     */
    public static MarkStyle filledDiamondStyle( Color color, final int size ) {
        int off = size;
        final Shape di = new Polygon( new int[] { -off, 0, off, 0 },
                                      new int[] { 0, -off, 0, off }, 4 );
        return new ConvenienceMarkStyle( color, new Integer( size ), off + 1 ) {
            protected void drawShape( Graphics g, int x, int y ) {
                if ( g instanceof Graphics2D ) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.translate( x, y );

                    /* In pixel-like graphics contexts, the diamond is ugly
                     * if you just fill it. */
                    g2.fill( di );
                    g2.draw( di );
                    g2.translate( -x, -y );
                }
                else {
                    g.drawRect( x, y, 3, 3 );
                }
            }
        };
    }

    /**
     * Returns an open shape marker style.
     *
     * @param  color  colour
     * @param  shape  shape
     * @return  marker style
     */
    public static MarkStyle openShapeStyle( Color color, final Shape shape ) {
        return new ConvenienceMarkStyle( color, shape,
                                         getMaxRadius( shape ) ) {
            protected void drawShape( Graphics g, int x, int y ) {
                if ( g instanceof Graphics2D ) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.translate( x, y );
                    g2.draw( shape );
                    g2.translate( -x, -y );
                }
                else {
                    g.drawRect( x, y, 3, 3 );
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
    public static MarkStyle filledShapeStyle( Color color, 
                                              final Shape shape ) {
        return new ConvenienceMarkStyle( color, shape,
                                         getMaxRadius( shape ) ) {
            protected void drawShape( Graphics g, int x, int y ) {
                if ( g instanceof Graphics2D ) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.translate( x, y );
                    g2.fill( shape );
                    g2.translate( -x, -y );
                }
                else {
                    g.fillRect( x, y, 3, 3 );
                }
            }
 
        };
    }

    /**
     * Returns a marker style which just plots a single point.
     * 
     * @param  color  colour
     * @return  marker style
     */
    public static MarkStyle pointStyle( Color color ) {
        return new ConvenienceMarkStyle( color, new Integer( 23 ), 1 ) {
            protected void drawShape( Graphics g, int x, int y ) {
                g.drawLine( x, y, x, y );
            }
        };
    }
}
