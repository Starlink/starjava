package uk.ac.starlink.ttools.plot;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.Arrays;
import javax.swing.Icon;

/**
 * Defines a style for plotting a bar in a histogram.
 *
 * @author   Mark Taylor
 * @since    16 Nov 2005
 */
public class BarStyle extends DefaultStyle implements Icon {

    private final Form form_;
    private final Placement placement_;

    private static final int ICON_HEIGHT = 12;
    private static final int ICON_WIDTH = 8;
    private static final float BODY_FADE = 0.25f;
    private static final float LINE_FADE = 0.50f;

    /** Bar form using open rectangles. */
    public static final Form FORM_OPEN = new Form( "Open", true ) {
        public void drawBar( Graphics g, int x, int y, int width, int height ) {
            Graphics2D g2 = (Graphics2D) g;
            int thickness = ( g2.getStroke() instanceof BasicStroke )
                          ? (int) ((BasicStroke) g2.getStroke()).getLineWidth()
                          : 1;
            int x0 = x + thickness / 2;
            int y0 = y + height;
            int x1 = x + width - 1 - ( ( thickness + 1 ) / 2 );
            int y1 = y;
            int[] xp = new int[] { x0, x0, x1, x1, };
            int[] yp = new int[] { y0, y1, y1, y0, };

            // This doesn't seem to be honouring the JOIN policy.  Why????
            g2.drawPolyline( xp, yp, 4 );
        }
    };

    /** Bar form using filled rectangles. */
    public static final Form FORM_FILLED = new Form( "Filled", true ) {
        public void drawBar( Graphics g, int x, int y, int width, int height ) {
            g.fillRect( x, y, Math.max( width - 1, 1 ), height );
        }
    };

    /** Bar form using filled 3d rectangles. */
    public static final Form FORM_FILLED3D = new Form( "Filled 3D", true ) {
        public void drawBar( Graphics g, int x, int y, int width, int height ) {
            g.fill3DRect( x, y, Math.max( width - 1, 1 ), height, true );
        }
    };

    /** Bar form drawing only the tops of the bars. */
    public static final Form FORM_TOP = new Form( "Steps", true ) {
        public void drawBar( Graphics g, int x, int y, int width, int height ) {
            g.drawLine( x, y, x + width, y );
        }
        public void drawEdge( Graphics g, int x, int y1, int y2 ) {
            g.drawLine( x, y1, x, y2 );
        }
    };

    /** Bar form using 1-d spikes. */
    public static final Form FORM_SPIKE = new Form( "Spikes", true ) {
        public void drawBar( Graphics g, int x, int y, int width, int height ) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setStroke( getStroke( g2.getStroke(), BasicStroke.CAP_ROUND,
                                     BasicStroke.JOIN_MITER ) );
            int xpos = x + width / 2;
            g2.drawLine( xpos, y + height, xpos, y );
        }
    };

    /** Bar form with an outline and a transparent inside. */
    public static final Form FORM_SEMIFILLED = new Form( "Semi Filled",
                                                         false ) {
        public void drawBar( Graphics g, int x, int y, int width, int height ) {
            Graphics2D g2 = (Graphics2D) g;
            Color color0 = g2.getColor();
            g2.setColor( fade( color0, BODY_FADE ) );
            g2.fillRect( x, y, width, height + 1 );
            int thickness = ( g2.getStroke() instanceof BasicStroke )
                          ? (int) ((BasicStroke) g2.getStroke()).getLineWidth()
                          : 1;
            int x0 = x;
            int y0 = y + height;
            int x1 = x0 + width;
            int y1 = y;
            int[] xp = new int[] { x0, x0, x1, x1, };
            int[] yp = new int[] { y0, y1, y1, y0, };
            g2.setColor( fade( color0, LINE_FADE ) );
            g2.drawPolyline( xp, yp, 4 );
            g2.setColor( color0 );
        }
    };

    /** Bar form with steps and a transparent inside. */
    public static final Form FORM_SEMITOP = new Form( "Semi Steps", false ) {
        public void drawBar( Graphics g, int x, int y, int width, int height ) {
            Color color0 = g.getColor();
            g.setColor( fade( color0, BODY_FADE ) );
            g.fillRect( x, y, width, height + 1 );
            g.setColor( fade( color0, 0.50f ) );
            g.drawLine( x, y, x + width, y );
            g.setColor( color0 );
        }
        public void drawEdge( Graphics g, int x, int y1, int y2 ) {
            Color color0 = g.getColor();
            g.setColor( fade( color0, LINE_FADE ) );
            g.drawLine( x, y1, x, y2 );
            g.setColor( color0 );
        }
    };

    /** Placement which puts bars next to each other. */
    public static final Placement PLACE_ADJACENT = new Placement( "adjacent" ) {
        public int[] getXRange( int lo, int hi, int iseq, int nseq ) {
            int gap = ( hi - lo - 2 ) / nseq;
            int rlo = lo + 1 + gap * iseq;
            int rhi = rlo + gap + 1;
            return new int[] { rlo, rhi };
        }
    };

    /** Placement which puts bars in the same X region. */
    public static final Placement PLACE_OVER = new Placement( "over" ) {
        public int[] getXRange( int lo, int hi, int iseq, int nseq ) {
            return new int[] { lo, hi };
        }
    };

    /**
     * Constructor.
     *
     * @param   color  initial colour
     * @param   form    bar form
     * @param   placement  bar placement
     */
    @SuppressWarnings("this-escape")
    public BarStyle( Color color, Form form, Placement placement ) {
        super( color, Arrays.asList( new Object[] { form, placement } ) );
        setLineWidth( 2 );
        form_ = form;
        placement_ = placement;
    }

    /**
     * Draws a bar for inclusion in a histogram.
     *
     * @param   g  graphics context
     * @param   xlo  lower bound in X direction
     * @param   xhi  upper bound in X direction
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
        g2.setStroke( getStroke( BasicStroke.CAP_SQUARE,
                                 BasicStroke.JOIN_MITER) );
        int[] xRange = placement_.getXRange( xlo, xhi, iseq, nseq );
        int x = xRange[ 0 ];
        int width = xRange[ 1 ] - xRange[ 0 ];
        form_.drawBar( g, xRange[ 0 ], ylo,
                          xRange[ 1 ] - xRange[ 0 ], yhi - ylo );
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
        g2.setStroke( getStroke( BasicStroke.CAP_SQUARE,
                                 BasicStroke.JOIN_MITER ) );
        form_.drawEdge( g, x, y1, y2 );
        g.setColor( col );
        g2.setStroke( str );
    }

    /**
     * Returns the form of this style.
     *
     * @return  bar form
     */
    public Form getForm() {
        return form_;
    }

    /**
     * Returns the placement of this style.
     *
     * @return  bar placement
     */
    public Placement getPlacement() {
        return placement_;
    }

    public Icon getLegendIcon() {
        return this;
    }

    public int getIconHeight() {
        return ICON_HEIGHT;
    }

    public int getIconWidth() {
        return ICON_WIDTH;
    }

    public void paintIcon( Component c, Graphics g, int x, int y ) {
        int x0 = x;
        int x1 = x + ICON_WIDTH;
        int y0 = y;
        int y1 = y + ICON_HEIGHT;
        int iseq = 1;
        int nseq = 3;
        drawEdge( g, x0, y0, y1, iseq, nseq );
        drawBar( g, x0, x1, y0, y1, iseq, nseq );
        drawEdge( g, x1, y0, y1, iseq, nseq );
    }

    /**
     * Scales the alpha component of a given colour.
     *
     * @param  color  basic colour
     * @param   alpha  scaling to apply to alpha
     * @return  faded colour
     */
    private static Color fade( Color color, float alpha ) {
        float[] rgba = color.getComponents( null );
        return new Color( rgba[ 0 ], rgba[ 1 ], rgba[ 2 ], rgba[ 3 ] * alpha );
    }

    /**
     * Describes the form of a bar style, that is what each bar looks like.
     */
    public static abstract class Form {
        private final String name_;
        private final boolean isOpaque_;

        protected Form( String name, boolean isOpaque ) {
            name_ = name;
            isOpaque_ = isOpaque;
        }

        /**
         * Draws a bar.  The whole region described by 
         * <code>x</code>, <code>y</code>,
         * <code>width</code> and <code>height</code> is available for 
         * drawing in.
         *
         * @param  g   graphics context
         * @param  x   left X coordinate of region (lowest X value permitted)
         * @param  y   lower Y coordinate of region (lowest Y value permitted)
         * @param  width  width of region
         *                (x+width is highest X value permitted)
         * @param  height height of region
         *                (y+height is highest Y value permitted)
         */
        public abstract void drawBar( Graphics g, int x, int y,
                                                  int width, int height );

        /**
         * Draws the edge of a bar.  This can be invoked to draw the boundary
         * between one bar and its immediate neighbour; the edge described
         * by the call's parameters is not the edge of the block representing
         * the bar's data, but the edge between the current bar and its
         * neighbour on one side or the other, so it may go up or down from
         * the Y value.
         *
         * <p>The default implementation does nothing, which is correct for
         * many forms.
         *
         * @param   g    graphics context
         * @param   x    x position of the edge
         * @param   y1   one y value of the edge
         * @param   y2   other y value of the edge
         */
        public void drawEdge( Graphics g, int x, int y1, int y2 ) {
            // no action
        }

        /**
         * Indicates whether this bar form is as opaque as the colour of
         * the supplied graphics context.  If it adjusts the alpha of the
         * supplied colour, it must return false.
         *
         * @return  true iff alpha is not adjusted
         */
        public boolean isOpaque() {
            return isOpaque_;
        }

        public String toString() {
            return name_;
        }
    }

    /**
     * Describes bar placement, that is how multiple bars covering the same
     * data range are to be arranged.
     */
    public static abstract class Placement {
        private final String name_;

        protected Placement( String name ) {
            name_ = name;
        }

        /**
         * Returns the range of X coordinates to be used for plotting a bar.
         *
         * @param  lo  lower bound of total range for data region
         * @param  hi  upper bound of total range for data region + 1
         * @param  iseq  index of the bar to be plotted in the returned range
         * @param  nseq  total number of bars which will be plotted in 
         *               the data region
         * @return  2-element arrage giving (lower bound, upper bound+1) of
         *          the region the plotted bar should occupy
         */
        public abstract int[] getXRange( int lo, int hi, int iseq, int nseq );

        public String toString() {
            return name_;
        }
    }
}
