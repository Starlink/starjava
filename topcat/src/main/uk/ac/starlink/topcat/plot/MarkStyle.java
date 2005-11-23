package uk.ac.starlink.topcat.plot;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.Arrays;
import javax.swing.Icon;

/**
 * Defines a style of marker for plotting in a scatter plot.
 * A number of static factory methods generating useful MarkStyle 
 * instances are provided.
 *
 * @author   Mark Taylor (Starlink)
 * @since    16 Jun 2004
 */
public abstract class MarkStyle extends DefaultStyle {

    private final int maxr_;

    /**
     * Constructor.
     *
     * @param   color  colour
     * @param   otherAtts  distinguisher for this instance
     * @param   maxr  maximum radius
     */
    protected MarkStyle( Color color, Object otherAtts, int maxr ) {
        super( color, otherAtts );
        maxr_ = maxr;
    }

    /**
     * Draws this marker's shape centered at the origin in a graphics context.
     * Implementing classes don't need to worry about the colour.
     *
     * @param  g  graphics context
     */
    protected abstract void drawShape( Graphics g );

    /**
     * Draws this marker's shape centred at the origin suitable for display
     * as a legend.  The default implementation just invokes 
     * {@link #drawShape}, but it may be overridden if there are special
     * requirements, for instance if <tt>drawShape</tt> draws a miniscule
     * graphic.
     *
     * @param   g  graphics context
     */
    protected void drawLegendShape( Graphics g ) {
        drawShape( g );
    }

    /**
     * Returns the maximum radius of a marker drawn by this class.
     * It is permissible to return a (gross) overestimate if no sensible
     * maximum can be guaranteed.
     *
     * @return   maximum distance from the specified <tt>x</tt>,<tt>y</tt>
     *           point that <tt>drawMarker</tt> might draw
     */
    public int getMaximumRadius() {
        return maxr_;
    }

    /**
     * Draws this marker centered at a given position.  
     * This method sets the colour of the graphics context and 
     * then calls {@link #drawShape}.
     *
     * @param  g  graphics context
     * @param  x  x position
     * @param  y  y position
     */
    public void drawMarker( Graphics g, int x, int y ) {
         drawMarker( g, x, y, null );
    }

    /**
     * Draws this marker in a way which may be modified by a supplied
     * <code>GraphicsTweaker</code> object.  This permits changes to
     * be made to the graphics context just before the marker is drawn.
     * In some cases this could be handled by modifying the graphics 
     * context before the call to <code>drawMarker</code>, but doing it
     * like this makes sure that the graphics context has been assigned
     * the right colour and position.
     *
     * @param  g  graphics context
     * @param  x  x position
     * @param  y  y position
     * @param  fixer  hook for modifying the graphics context (may be null)
     */
    public void drawMarker( Graphics g, int x, int y, GraphicsTweaker fixer ) {
        Color col = g.getColor();
        g.setColor( getColor() );
        g.translate( x, y );
        drawShape( fixer == null ? g : fixer.tweak( g ) );
        g.translate( -x, -y );
        g.setColor( col );
    }

    /**
     * Draws a legend for this marker centered at a given position.
     * This method sets the colour of the graphics context and then 
     * calls {@link #drawLegendShape}.
     *
     * @param  g  graphics context
     * @param  x  x position
     * @param  y  y position
     */
    public void drawLegend( Graphics g, int x, int y ) {
        Color col = g.getColor();
        g.setColor( getColor() );
        g.translate( x, y );
        drawLegendShape( g );
        g.translate( -x, -y );
        g.setColor( col );
    }

    /**
     * Returns an icon that draws this MarkStyle.
     *
     * @param  width  icon width
     * @param  height icon height
     * @return icon
     */
    public Icon getIcon( final int width, final int height ) {
        return new Icon() {
            public int getIconHeight() {
                return height;
            }
            public int getIconWidth() {
                return width;
            }
            public void paintIcon( Component c, Graphics g, 
                                   int xoff, int yoff ) {
                int x = xoff + width / 2;
                int y = yoff + height / 2;
                drawMarker( g, x, y );
            }
        };
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
     * Returns an open circle marker style.
     *
     * @param  color  colour
     * @param  size   approximate circle radius
     * @return  marker style
     */
    public static MarkStyle openCircleStyle( Color color, final int size ) {
        final int off = -size;
        final int diam = size * 2;
        return new MarkStyle( color, new Integer( size ), size + 1 ) {
            protected void drawShape( Graphics g ) {
                g.drawOval( off, off, diam, diam );
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
        final int off = -size;
        final int diam = size * 2;
        return new MarkStyle( color, new Integer( size ), size + 1 ) {
            protected void drawShape( Graphics g ) {
                g.fillOval( off, off, diam, diam );

                /* In pixel-type graphics contexts, the filled circle is
                 * ugly (asymmetric) if the outline is not painted too. */
                g.drawOval( off, off, diam, diam );
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
        final int off = -size;
        final int height = size * 2;
        return new MarkStyle( color, new Integer( size ), size + 1 ) {
            protected void drawShape( Graphics g ) {
                g.drawRect( off, off, height, height );
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
        final int off = -size;
        final int height = size * 2 + 1;
        return new MarkStyle( color, new Integer( size ), size + 1 ) {
           protected void drawShape( Graphics g ) {
                g.fillRect( off, off, height, height );
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
        return new MarkStyle( color, new Integer( size ), size + 1 ) {
            protected void drawShape( Graphics g ) {
                g.drawLine( -off, 0, off, 0 );
                g.drawLine( 0, -off, 0, off );
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
        return new MarkStyle( color, new Integer( size ), size + 1 ) {
            protected void drawShape( Graphics g ) {
                g.drawLine( -off, -off, off, off );
                g.drawLine( off, -off, -off, off );
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
        return openShapeStyle( color, diamond( size ) );
    }

    /**
     * Returns a filled diamond marker style.
     *
     * @param  color  colour
     * @param  size   approximate diamond radius
     * @return  marker style
     */
    public static MarkStyle filledDiamondStyle( Color color, final int size ) {
        final int off = size;
        final Shape di = diamond( size );
        return new MarkStyle( color, new Integer( size ), size + 1 ) {
            protected void drawShape( Graphics g ) {
                if ( g instanceof Graphics2D ) {
                    Graphics2D g2 = (Graphics2D) g;

                    /* In pixel-like graphics contexts, the diamond is ugly
                     * if you just fill it. */
                    g2.fill( di );
                    g2.draw( di );
                }
                else {
                    g.drawRect( -off, -off, size * 2, size * 2 );
                }
            }
        };
    }

    /**
     * Returns an open triangle style.
     *
     * @param  color  colour
     * @param  size   approximate triangle radius
     * @param  up     true for pointing upwards, false for pointing down
     * @return  marker style
     */
    public static MarkStyle openTriangleStyle( Color color, int size, 
                                               boolean up ) {
        return openShapeStyle( color, triangle( size, up ) );
    }

    /**
     * Returns a filled triangle style.
     *
     * @param  color  colour
     * @param  size   approximate triangl radius
     * @param  up     true for pointing upwards, false for pointing down
     * @return  marker style
     */
    public static MarkStyle filledTriangleStyle( Color color, int size,
                                                 boolean up ) {
        return filledShapeStyle( color, triangle( size, up ) );
    }

    /**
     * Returns an open shape marker style.
     *
     * @param  color  colour
     * @param  shape  shape
     * @return  marker style
     */
    public static MarkStyle openShapeStyle( Color color, final Shape shape ) {
        return new MarkStyle( color, shape, getMaxRadius( shape ) ) {
            protected void drawShape( Graphics g ) {
                if ( g instanceof Graphics2D ) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.draw( shape );
                }
                else {
                    g.drawRect( -1, -1, 2, 2 );
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
        return new MarkStyle( color, shape, getMaxRadius( shape ) ) {
            protected void drawShape( Graphics g ) {
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
     * Returns a marker style which just plots a single point.
     * 
     * @param  color  colour
     * @return  marker style
     */
    public static MarkStyle pointStyle( Color color ) {
        return new MarkStyle( color, new Integer( 23 ), 1 ) {
            protected void drawShape( Graphics g ) {
                g.drawLine( 0, 0, 0, 0 );
            }
            protected void drawLegendShape( Graphics g ) {
                g.fillOval( -1, -1, 2, 2 );
                g.drawOval( -1, -1, 2, 2 );
            }
        };
    }

    /**
     * Returns a marker style which plots using different given styles
     * for normal points and legends.
     *
     * @param   normalStyle  style used for most things
     * @param   legendStyle  style used for {@link #drawLegendShape} method
     * @return  marker style
     */
    public static MarkStyle compositeMarkStyle( final MarkStyle normalStyle,
                                                final MarkStyle legendStyle ) {
        return new MarkStyle( normalStyle.getColor(),
                              normalStyle.getOtherAtts(),
                              normalStyle.getMaximumRadius() ) {
            protected void drawShape( Graphics g ) {
                normalStyle.drawShape( g );
            }
            protected void drawLegendShape( Graphics g ) {
                legendStyle.drawLegendShape( g );
            }
            public int getMaximumRadius() {
                return Math.max( normalStyle.getMaximumRadius(),
                                 legendStyle.getMaximumRadius() );
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
            return npoints + xpoints.hashCode() + ypoints.hashCode();
        }
    }
}
