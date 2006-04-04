package uk.ac.starlink.topcat.plot;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.FontMetrics;

/**
 * Assigns and draws axis labels.
 *
 * @author   Mark Taylor
 * @since    13 Mar 2006
 */
public class AxisLabeller {

    private double lo_;
    private double hi_;
    private int npix_;
    private boolean log_;
    private boolean flip_;
    private TickLabel[] tickLabels_;
    private String axisLabel_;
    private FontMetrics fm_;
    private TickStyle tickStyle_;
    private int maxHeight_;
    private boolean drawText_ = true;

    /**
     * Constructs a new labeller giving enough information to determine
     * where the tickmarks will appear.
     *
     * @param   axisLabel  text annotation for the axis
     * @param   lo     lower bound of the data range
     * @param   hi     upper bound of the data range
     * @param   npix   number of pixels along the length of the axis
     * @param   log    true iff the scale is to be logarithmic
     * @param   flip   true iff the scale is reversed (low to high is
     *                 right to left instead of left to right)
     * @param   fm     font metrics used for the text
     * @param   tickStyle  determines positioning of ticks
     * @param   reqTick  suggested number of tick marks on the axis;
     *          the actual number may be greater or smaller according to
     *          axis length, font size etc
     */
    public AxisLabeller( String axisLabel, double lo, double hi, int npix,
                         boolean log, boolean flip, FontMetrics fm,
                         TickStyle tickStyle, int reqTick ) {
        axisLabel_ = axisLabel;
        lo_ = lo;
        hi_ = hi;
        npix_ = npix;
        log_ = log;
        flip_ = flip;
        fm_ = fm;
        tickStyle_ = tickStyle;

        /* Set up actual tick marks and their positions.  Start out with
         * the requested number, but if this makes the axis too crowded
         * reduce the number and start again. */
        TickLabel[] labels = null;
        int maxh = 0;
        for ( int mTick = reqTick; labels == null && mTick > 0; mTick-- ) {
            AxisLabels axer = log ? AxisLabels.labelLogAxis( lo, hi, mTick )
                                  : AxisLabels.labelLinearAxis( lo, hi, mTick );
            int nTick = axer.getCount();
            labels = new TickLabel[ nTick ];
            Rectangle lastBounds = null;
            for ( int iTick = 0; iTick < nTick; iTick++ ) {
                double tick = axer.getTick( iTick );
                double frac = log ? Math.log( tick / lo ) / Math.log( hi / lo )
                                  : ( tick - lo ) / ( hi - lo );
                int pos = (int) Math.round( npix * ( flip ? ( 1. - frac )
                                                          : frac ) );
                String text = axer.getLabel( iTick );
                labels[ iTick ] = new TickLabel( pos, text );

                /* Check it's not too close to the last label. */
                Rectangle bounds = tickStyle.getBounds( fm, pos, 0, text );
                maxh = Math.max( maxh, bounds.height );
                if ( iTick > 0 ) {
                    Rectangle r1 =
                       new Rectangle( bounds.x, bounds.y, 
                                      (int) ( bounds.width * 1.4 ),
                                      (int) ( bounds.height * 1.4 ) );
                    Rectangle r2 = 
                       new Rectangle( lastBounds.x, lastBounds.y,
                                      (int) ( lastBounds.width * 1.4 ),
                                      (int) ( lastBounds.height * 1.4 ) );
                    if ( r1.intersects( r2 ) ) {
                        labels = null;
                        break;
                    }
                }
                lastBounds = bounds;
            }
        }
        assert labels != null;
        tickLabels_ = labels;
        maxHeight_ = maxh;
    }

    /**
     * Returns the height in pixels of the annotation text.  This is a
     * distance perpendicular to the axis itself.
     *
     * @return   size of annotation box
     */
    public int getAnnotationHeight() {
        int h = maxHeight_;
        if ( axisLabel_ != null && axisLabel_.trim().length() > 0 ) {
            h += fm_.getHeight() + fm_.getDescent();
        }
        return h;
    }

    /**
     * Draw the axis labels on a given graphics context.
     * The axis will be drawn along the horizontal direction of the context,
     * starting at the origin.
     *
     * @param   g  graphics context
     */
    public void annotateAxis( Graphics g ) {

        /* Draw the text label of the axis, if there is one. */
        if ( drawText_ &&
             axisLabel_ != null && axisLabel_.trim().length() > 0 ) {
            g.drawString( axisLabel_,
                         ( npix_ - fm_.stringWidth( axisLabel_ ) ) / 2,
                         tickStyle_.isDown() 
                                  ? maxHeight_ + fm_.getHeight()
                                  : - maxHeight_ - fm_.getDescent() );
        }

        /* Draw the tick marks. */
        for ( int i = 0; i < tickLabels_.length; i++ ) {
            int tpos = tickLabels_[ i ].pos_;
            String ttext = tickLabels_[ i ].text_;
            g.drawLine( tpos, -2, tpos, +2 );
            if ( drawText_ ) {
                Rectangle bounds = tickStyle_.getBounds( fm_, tpos, 0, ttext );
                if ( bounds.x > 0 && bounds.x + bounds.width < npix_ ) {
                    tickStyle_.drawString( g, fm_, tpos, 0, ttext );
                }
            }
        }
    }

    /**
     * Draws grid lines on a given graphics context.
     * The lines will be drawn vertically, the axis being considered 
     * horizontal and starting at the origin.  The vertical extent of the
     * grid lines is given by two values <code>y0</code> and <code>y1</code>.
     * It is the caller's responsibility to set colours and so on.
     *
     * @param   g   graphics context
     * @param   y0  y coordinate of one end of the lines
     * @parm    y1  y coordinate of the other end of the lines
     */
    public void drawGridLines( Graphics g, int y0, int y1 ) {
        for ( int i = 0; i < tickLabels_.length; i++ ) {
            int tpos = tickLabels_[ i ].pos_;
            g.drawLine( tpos, y0, tpos, y1 );
        }
    }

    /**
     * Sets the tick mark style to one of the predefined settings.
     * Currently the values {@link #X} and {@link #Y} are available.
     *
     * @param   tickStyle  style
     */
    public void setTickStyle( TickStyle tickStyle ) {
        tickStyle_ = tickStyle;
    }

    /**
     * Sets whether textual labels should be drawn on the axis.
     * If false, only tickmarks will be drawn.  True by default.
     *
     * @param   drawText  true iff you want textual labelling
     */
    public void setDrawText( boolean drawText ) {
        drawText_ = drawText;
    }

    /**
     * Defines tick mark annotation styles.
     */
    public static abstract class TickStyle {

        /**
         * Annotates a tickmark at a given point with a given text label.
         *
         * @param   g   graphics context
         * @param   fm  font metrics
         * @param   x   x position of tick mark
         * @param   y   y position of tick mark (normally zero)
         * @param   label   text of label
         */
        abstract void drawString( Graphics g, FontMetrics fm,
                                  int x, int y, String label );

        /**
         * Returns the bounding box for a label that would be drawn by
         * {@link #drawString}.
         *
         * @param   fm  font metrics
         * @param   x   x position of tick mark
         * @param   y   y position of tick mark (normally zero)
         * @param   label   text of label
         */
        abstract Rectangle getBounds( FontMetrics fm, int x, int y,
                                      String label );

        /**
         * Indicates the sense of the direction away from the horizontal axis
         * of labels.  If true, annotations are drawn in the positive Y
         * direction from the axis.  If false, the negative Y direction.
         *
         * @return   direction of annotation text
         */
        abstract boolean isDown();
    }

    /** Tick style suitable for X axis labels. */
    public static final TickStyle X = new TickStyle() {
        void drawString( Graphics g, FontMetrics fm, int x, int y,
                         String label ) {
            g.drawString( label, x - fm.stringWidth( label ) / 2,
                                 y + fm.getHeight() );
        }
        Rectangle getBounds( FontMetrics fm, int x, int y,
                                      String label ) {
            int w = fm.stringWidth( label );
            int h = fm.getHeight();
            return new Rectangle( x - w / 2, y + h, w, h );
        }
        boolean isDown() {
            return true;
        }
    };

    /** Tick style suitable for Y axis labels. */
    public static final TickStyle Y = new TickStyle() {
        void drawString( Graphics g, FontMetrics fm, int x, int y,
                         String label ) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.translate( x, y );
            g2.rotate( Math.PI * 0.5 );
            g2.drawString( label, - fm.stringWidth( label + "0" ),
                           fm.getAscent() / 2 );
        }
        Rectangle getBounds( FontMetrics fm, int x, int y, String label ) {
            int w = fm.stringWidth( label + "0" );
            int h = fm.getHeight();
            return new Rectangle( x - h, y + w, h, w );
        }
        boolean isDown() {
            return false;
        }
    };

    /**
     * Encapsulates the positions and text labels of tick marks.
     */
    private static class TickLabel {
        final int pos_;
        final String text_;
        TickLabel( int pos, String text ) {
            pos_ = pos;
            text_ = text;
        }
    }
}
