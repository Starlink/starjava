package uk.ac.starlink.topcat.plot;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;

/**
 * Defines a style for plotting a bar in a histogram.
 *
 * @author   Mark Taylor
 * @since    16 Nov 2005
 */
public abstract class BarStyle extends DefaultStyle {

    /**
     * Constructor giving colour, stroke and additional attributes.
     *
     * @param   color  initial colour
     * @param   stroke  initial stroke style
     * @param   otherAtts   other distinguishing features of this instance
     */
    protected BarStyle( Color color, Stroke stroke, Object otherAtts ) {
        super( color, stroke, otherAtts );
    }

    /**
     * Draws the shape of a bar for inclusion in a histogram.
     * The colour is taken care of, you only need to draw the shape.
     * The <code>iseq</code> and <code>nseq</code> values permit 
     * bars to take up less than the total width if there are several
     * to be plotted.
     *
     * @param  g   graphics context
     * @param  x   left X coordinate for the space allocated to current region
     *             (lowest X value permitted)
     * @param  y   lower Y coordinate for the space allocated to current region
     *             (lowest Y value permitted)
     * @param  width  width for the space allocated to current region
     *                (x+width is highest X value permitted)
     * @param  height height of the space allocated to current region
     *                (y+height is highest Y value permitted)
     * @param  iseq   index of the bar in the sequence of bars plotted on
     *                current region
     * @param  nseq   number of bars in total to be plotted on current region
     */
    protected abstract void drawBarShape( Graphics g, int x, int y,
                                          int width, int height,
                                          int iseq, int nseq );

    /**
     * Draws the edge of a bar.  This can be invoked to draw the boundary
     * between one bar and its immediate neighbour; the edge described
     * by the call's parameters is not the edge of the block representing
     * the bar's data, but the edge between the current bar and its
     * neighbour on one side or the other, so it may go up or down from
     * the Y value.
     *
     * <p>The default implementation does nothing, which is correct for
     * many bar styles.
     *
     * @param   g    graphics context
     * @param   x    x position of the edge
     * @param   y1   one y value of the edge
     * @param   y2   other y value of the edge
     * @param   iseq  index of the set being plotted
     * @param   nseq  number of sets being plotted for this bar
     */
    protected void drawEdgeShape( Graphics g, int x, int y1, int y2,
                                  int iseq, int nseq ) {
        // no action.
    }

    /**
     * Draws a bar for inclusion in a histogram.
     *
     * @param   g  graphics context
     * @param   xlo  lower bound in X direction
     * @param   xhi  upper gound in X direction
     * @param   ylo  lower bound in Y direction
     * @param   yhi  upper bound in Y direction
     * @param   iseq  index of the set being plotted
     * @param   nseq  number of sets being plotted for this bar
     */
    public void drawBar( Graphics g, int xlo, int xhi, int ylo, int yhi,
                         int iseq, int nseq ) {
        Graphics2D g2 = (Graphics2D) g;
        Color col = g.getColor();
        Stroke str = g2.getStroke();
        g.setColor( getColor() );
        g2.setStroke( getStroke() );
        drawBarShape( g, xlo, ylo, xhi - xlo, yhi - ylo, iseq, nseq );
        g.setColor( col );
        g2.setStroke( str );
    }

    /**
     * Draws the edge of a bar.  This can be invoked to draw the boundary
     * between one bar and its immediate neighbour; the edge described 
     * by the call's parameters is not the edge of the block representing
     * the bar's data, but the edge between the current bar and its
     * neighbour on one side or the other, so it may go up or down from
     * the Y value.  For many bar styles this will be a no-op.
     *
     * @param   g    graphics context
     * @param   x    x position of the edge
     * @param   y1   one y value for the edge
     * @param   y2   other y value for the edge
     * @param   iseq  index of the set being plotted
     * @param   nseq  number of sets being plotted for this bar
     */
    public void drawEdge( Graphics g, int x, int y1, int y2,
                          int iseq, int nseq ) {
        Graphics2D g2 = (Graphics2D) g;
        Color col = g.getColor();
        Stroke str = g2.getStroke();
        g.setColor( getColor() );
        g2.setStroke( getStroke() );
        drawEdgeShape( g, x, y1, y2, iseq, nseq );
        g.setColor( col );
        g2.setStroke( str );
    }

    public void drawLegend( Graphics g, int x, int y ) {
        drawEdge( g, x - 4, y - 6, y + 6, 0, 3 );
        drawBar( g, x - 4, x + 4, y - 6, y + 6, 0, 3 );
        drawEdge( g, x + 4, y - 6, y + 6, 0, 3 );
    }

}
