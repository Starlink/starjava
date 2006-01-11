package uk.ac.starlink.topcat.plot;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
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
 *    style1.setStroke( style0.getStroke() );
 *    style1.setLine( style0.getLine() );
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

    /** Symbolic constant meaning join points by straight line segments. */
    public static final Line DOT_TO_DOT = new Line( "DotToDot" );

    /** Symbolic constant meaning draw a linear regression line. */
    public static final Line LINEAR = new Line( "LinearRegression" );

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
     * Configures the given graphics context ready to do line drawing
     * in accordance with the current state of this style.
     * Only call this if {@link #getLine} returns true.
     *
     * @param  g  graphics context - will be altered
     */
    public void configureForLine( Graphics g ) {
        g.setColor( getColor() );
        if ( g instanceof Graphics2D ) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setStroke( getStroke() );
        }
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
        if ( getLine() != null ) {
            Graphics g1 = g.create();
            configureForLine( g1 );
            g1.drawLine( x - 8, y, x + 8, y );
        }
        Graphics g1 = g.create();
        g1.setColor( getColor() );
        g1.translate( x, y );
        drawLegendShape( g1 );
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

    public boolean equals( Object o ) {
        if ( super.equals( o ) ) {
            MarkStyle other = (MarkStyle) o;
            return this.line_ == other.line_;
        }
        else {
            return false;
        }
    }

    public int hashCode() {
        int code = super.hashCode();
        code = code * 23 + ( line_ == null ? 1 : line_.hashCode() );
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
