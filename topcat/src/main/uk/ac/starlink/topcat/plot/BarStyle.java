package uk.ac.starlink.topcat.plot;

import java.awt.Color;
import java.awt.Graphics;

/**
 * Defines a style for plotting a bar in a histogram.
 *
 * @author   Mark Taylor
 * @since    16 Nov 2005
 */
public abstract class BarStyle extends DefaultStyle {

    /**
     * Constructor giving colour and additional attributes.
     *
     * @param   color  initial colour
     * @param   otherAtts   other distinguishing features of this instance
     */
    protected BarStyle( Color color, Object otherAtts ) {
        super( color, otherAtts );
    }

    /**
     * Constructor giving colour.
     *
     * @param  color  initial colour
     */
    protected BarStyle( Color color ) {
        super( color, null );
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
     * Draws a bar for inclusion in a histogram.
     * Parameters are the same as for {@link #drawBarShape},
     * but this style's current colour is applied as well.
     */
    public void drawBar( Graphics g, int xlo, int xhi, int ylo, int yhi,
                         int iseq, int nseq ) {
        Color col = g.getColor();
        g.setColor( getColor() );
        drawBarShape( g, xlo, ylo, xhi - xlo, yhi - ylo, iseq, nseq );
        g.setColor( col );
    }

    public void drawLegend( Graphics g, int x, int y ) {
        drawBar( g, x - 4, x + 4, y - 6, y + 6, 0, 3 );
    }
}
