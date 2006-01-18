package uk.ac.starlink.topcat.plot;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Icon;

/**
 * Defines a style of marker for plotting in a scatter plot.
 * The marker part of a MarkStyle is characterised visually by its
 * shapeId, colour and size.  If it represents a line to be drawn as well
 * it also has a stroke and a join type.  A matching instance of a
 * MarkStyle style can in general be produced by doing
 * <pre>
 *    style1 = style0.getShapeId()
 *                   .getStyle( style0.getColor(), style0.getSize() );
 *    style1.setLine( style0.getLine() );
 *    style1.setLineWidth( style0.getLineWidth() );
 *    style1.setDash( style0.getDash() );
 *    style1.setHidePoints( style0.getHidePoints() );
 *    style1.setOpaqueLimit( style0.getOpaqueLimit() );
 * </pre>
 * style0 and style1 should then match according to the <code>equals()</code>
 * method.  A style may however have a null <code>shapeId</code>, in
 * which case you can't generate a matching instance.
 *
 * @author   Mark Taylor (Starlink)
 * @since    16 Jun 2004
 */
public abstract class MarkStyle extends DefaultStyle {

    private final int size_;
    private final int maxr_;
    private final MarkShape shapeId_;
    private Line line_;
    private boolean hidePoints_;
    private int opaqueLimit_ = 1;
    private int[] pixoffs_;
    private static final RenderingHints pixHints_;

    /** Symbolic constant meaning join points by straight line segments. */
    public static final Line DOT_TO_DOT = new Line( "DotToDot" );

    /** Symbolic constant meaning draw a linear regression line. */
    public static final Line LINEAR = new Line( "LinearRegression" );

    private static final int LEGEND_ICON_WIDTH = 20;
    private static final int LEGEND_ICON_HEIGHT = 12;

    static {
        pixHints_ = new RenderingHints( null );
        pixHints_.put( RenderingHints.KEY_ANTIALIASING,
                       RenderingHints.VALUE_ANTIALIAS_OFF );
        pixHints_.put( RenderingHints.KEY_RENDERING,
                       RenderingHints.VALUE_RENDER_QUALITY );
        pixHints_.put( RenderingHints.KEY_DITHERING,
                       RenderingHints.VALUE_DITHER_DISABLE );
        pixHints_.put( RenderingHints.KEY_FRACTIONALMETRICS,
                       RenderingHints.VALUE_FRACTIONALMETRICS_ON );
    }

    /**
     * Constructor.
     *
     * @param   color  colour
     * @param   otherAtts  distinguisher for this instance (besides class
     *                     and colour)
     * @param   shapeId  style factory 
     * @param   size     nominal size
     * @param   maxr     maximum radius (furthest distance from centre that
     *                   this style may plot a pixel)
     */
    protected MarkStyle( Color color, Object otherAtts,
                         MarkShape shapeId, int size, int maxr ) {
        super( color, otherAtts );
        shapeId_ = shapeId;
        size_ = size;
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
     * Returns this style's shape id.  This is a factory capable of producing
     * match styles which resemble this one in point of shape (but may
     * differ in size or colour).
     *
     * @return   style factory
     */
    public MarkShape getShapeId() {
        return shapeId_;
    }

    /**
     * Returns the nominal size of this style.  In general a size of 1 
     * is the smallest, 2 is the next smallest etc.
     *
     * @return   style size
     */
    public int getSize() {
        return size_;
    }

    /**
     * Sets the line type for this style.
     *
     * @param  line  line type
     */
    public void setLine( Line line ) {
        line_ = line;
    }

    /**
     * Returns the line type for this style.
     *
     * @return  line type
     */
    public Line getLine() {
        return line_;
    }

    /**
     * Sets whether points should be hidden or visible.
     * This will usually only be honoured if the line style is non-null.
     *
     * @param  visible  true if you want points to be invisible
     */
    public void setHidePoints( boolean visible ) {
        hidePoints_ = visible;
    }

    /**
     * Indicates whether points are hidden or visible.
     * This should usually only be honoured if the line style is non-null.
     *
     * @return  true if points are to be invisible
     */
    public boolean getHidePoints() {
        return hidePoints_;
    }

    /**
     * Sets the opacity limit for this style.  The limit is the number
     * of pixels plotted on top of each other which will result in
     * complete opacity.  The default is one, which corresponds to 
     * fully opaque pixels.
     *
     * @param   lim  new opacity limit
     */
    public void setOpaqueLimit( int lim ) {
        if ( lim < 1 ) {
            throw new IllegalArgumentException( lim + " < 1" );
        }
        opaqueLimit_ = lim;
    }

    /**
     * Returns the opacity limit for this style.
     *
     * @return  opacity limit
     */
    public int getOpaqueLimit() {
        return opaqueLimit_;
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
     * <code>ColorTweaker</code> object.  This permits changes to
     * be made to the colour just before the marker is drawn.
     * In some cases this could be handled by modifying the graphics 
     * context before the call to <code>drawMarker</code>, but doing it
     * like this makes sure that the graphics context has been assigned
     * the right colour and position.
     *
     * @param  g  graphics context
     * @param  x  x position
     * @param  y  y position
     * @param  fixer  hook for modifying the colour (may be null)
     */
    public void drawMarker( Graphics g, int x, int y, ColorTweaker fixer ) {
        Color origColor = g.getColor();
        Color markColor = fixer == null ? getColor()
                                        : fixer.tweakColor( getColor() );
        g.setColor( markColor );
        g.translate( x, y );
        drawShape( g );
        g.translate( -x, -y );
        g.setColor( origColor );
    }

    /**
     * Configures the given graphics context ready to do line drawing with
     * a given stroke cap and join policy.
     *
     * @param   g  graphics context - will be altered
     * @param   cap  one of {@link java.awt.BasicStroke}'s CAP_* constants
     * @param   join one of {@link java.awt.BasicStroke}'s JOIN_* constants
     */
    public void configureForLine( Graphics g, int cap, int join ) {
        g.setColor( getColor() );
        if ( g instanceof Graphics2D ) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setStroke( getStroke( cap, join ) );
        }
    }

    /**
     * Draws a legend icon for this style.
     * This method sets the colour of the graphics context and then 
     * calls {@link #drawLegendShape}.
     *
     * @return  legend icon
     */
    public Icon getLegendIcon() {
        return new Icon() {
            public int getIconHeight() {
                return LEGEND_ICON_HEIGHT;
            }
            public int getIconWidth() {
                return LEGEND_ICON_WIDTH;
            }
            public void paintIcon( Component c, Graphics g, int x, int y ) {
                boolean hide;
                if ( getLine() != null ) {
                    Graphics g1 = g.create();
                    configureForLine( g1, BasicStroke.CAP_BUTT,
                                          BasicStroke.JOIN_MITER );
                    int ypos = y + LEGEND_ICON_HEIGHT / 2;
                    g1.drawLine( x, ypos, x + LEGEND_ICON_WIDTH, ypos );
                    hide = getHidePoints();
                }
                else {
                    hide = false;
                }
                if ( ! hide ) {
                    Graphics g1 = g.create();
                    g1.setColor( getColor() );
                    g1.translate( x + LEGEND_ICON_WIDTH / 2,
                                  y + LEGEND_ICON_HEIGHT / 2 );
                    drawLegendShape( g1 );
                }
            }
        };
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
     * Returns an array of pixel offsets which can be used to draw this
     * marker onto a raster.  This can be used as an alternative to 
     * rendering the marker using the <code>drawMarker()</code> methods
     * in situations where it might be more efficient.
     * The returned value is a 2N-element array describing N points
     * as offsets from (0,0); the format is (xoff0,yoff0, xoff1,yoff1, ...).
     * The assumption is that all the pixels are the same colour.
     *
     * @return   array of pixel offsets reprensenting this style as a bitmap
     */
    public int[] getPixelOffsets() {
        if ( pixoffs_ == null ) {

            /* Construct a BufferedImage big enough to hold all the pixels
             * in a rendering of the marker. */
            int xdim = 2 * maxr_ + 1;
            int ydim = 2 * maxr_ + 1;
            int xoff = maxr_;
            int yoff = maxr_;
            BufferedImage im =
                new BufferedImage( xdim, ydim, BufferedImage.TYPE_INT_ARGB );

            /* Draw this marker onto the graphics associated with it.
             * We use high quality rendering hints since we're only going to
             * do this once so we might as well get the best shape for it
             * (not that it seems to make much difference). */
            Graphics2D g = im.createGraphics();
            g.setRenderingHints( pixHints_ );
            drawMarker( g, xoff, yoff, null );

            /* Now examine the pixels in the image we've just drawn to, and
             * extract a list of the touched pixels. */
            Raster raster = im.getData();
            List pointList = new ArrayList( xdim * ydim );
            for ( int ix = 0; ix < xdim; ix++ ) {
                for ( int iy = 0; iy < ydim; iy++ ) {
                    int alpha = raster.getSample( ix, iy, 3 );
                    if ( alpha > 0 ) {
                        assert alpha == 255 : alpha;
                        pointList.add( new Point( ix - xoff, iy - yoff ) );
                    }
                }
            }

            /* Turn it into an xy array suitable for return. */
            int noff = pointList.size();
            int[] pixoffs = new int[ noff * 2 ];
            for ( int ioff = 0; ioff < noff; ioff++ ) {
                Point p = (Point) pointList.get( ioff );
                pixoffs[ ioff * 2 + 0 ] = p.x;
                pixoffs[ ioff * 2 + 1 ] = p.y;
            }
            pixoffs_ = pixoffs;
        }
        return pixoffs_;
    }

    public boolean equals( Object o ) {
        if ( super.equals( o ) ) {
            MarkStyle other = (MarkStyle) o;
            return this.line_ == other.line_
                && this.hidePoints_ == other.hidePoints_
                && this.opaqueLimit_ == other.opaqueLimit_;
        }
        else {
            return false;
        }
    }

    public int hashCode() {
        int code = super.hashCode();
        code = code * 23 + ( line_ == null ? 1 : line_.hashCode() );
        code = code * 23 + ( hidePoints_ ? 0 : 1 );
        code = code * 23 + opaqueLimit_;
        return code;
    }

    /**
     * Returns a style which looks like a target.  Suitable for use
     * as a cursor.
     */
    public static MarkStyle targetStyle() {
        return new MarkStyle( new Color( 0, 0, 0, 192 ), new Object(),
                              null, 1, 7 ) {
            final Stroke stroke_ = new BasicStroke( 2, BasicStroke.CAP_ROUND,
                                                    BasicStroke.JOIN_ROUND );
            protected void drawShape( Graphics g ) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setStroke( stroke_ );
                g2.drawOval( -6, -6, 13, 13 );
                g2.drawLine( 0, +4, 0, +8 );
                g2.drawLine( 0, -4, 0, -8 );
                g2.drawLine( +4, 0, +8, 0 );
                g2.drawLine( -4, 0, -8, 0 );
            }
        };
    }

    /**
     * Enumeration class describing the types of line which can be drawn
     * in association with markers.
     */
    public static class Line {
        private final String name_;
        private Line( String name ) {
            name_ = name;
        }
        public String toString() {
            return name_;
        }
    }
}
