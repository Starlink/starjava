package uk.ac.starlink.ttools.plot;

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
    private int loPad_;
    private int hiPad_;
    private boolean log_;
    private boolean flip_;
    private TickLabel[] tickLabels_;
    private String axisLabel_;
    private FontMetrics fm_;
    private TickStyle tickStyle_;
    private int reqTick_;
    private Rectangle axisBounds_;
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
     * @param   loPad  number of pixels below 0 available for drawing on
     * @param   hiPad  number of pixels above npix available for drawing on
     */
    public AxisLabeller( String axisLabel, double lo, double hi, int npix,
                         boolean log, boolean flip, FontMetrics fm,
                         TickStyle tickStyle, int reqTick, 
                         int loPad, int hiPad ) {
        axisLabel_ = axisLabel;
        lo_ = lo;
        hi_ = hi;
        npix_ = npix;
        log_ = log;
        flip_ = flip;
        fm_ = fm;
        tickStyle_ = tickStyle;
        reqTick_ = reqTick;
        loPad_ = loPad;
        hiPad_ = hiPad;
        configure();
    }

    /**
     * Ensures that internal state is consistent.  Should be called after
     * some characteristics change.
     */
    private void configure() {

        /* Set up actual tick marks and their positions.  Start out with
         * the requested number, but if this makes the axis too crowded
         * reduce the number and start again. */
        TickLabel[] labels = null;
        int xmin = 0;
        int xmax = npix_;
        int hmax = 0;
        for ( int mTick = reqTick_; labels == null && mTick > 0; mTick-- ) {
            AxisLabels axer = log_
                            ? AxisLabels.labelLogAxis( lo_, hi_, mTick )
                            : AxisLabels.labelLinearAxis( lo_, hi_, mTick );
            int nTick = axer.getCount();
            labels = new TickLabel[ nTick ];
            Rectangle lastBounds = null;
            for ( int iTick = 0; iTick < nTick; iTick++ ) {
                int pos = getTickPosition( axer.getTick( iTick ) );
                String text = axer.getLabel( iTick );
                labels[ iTick ] = new TickLabel( pos, text );

                /* Check it's not too close to the last label. */
                Rectangle bounds = tickStyle_.getBounds( fm_, pos, 0, text );
                hmax = Math.max( hmax, bounds.height );
                xmin = Math.min( xmin, bounds.x );
                xmax = Math.max( xmax, bounds.x + bounds.width );
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
        maxHeight_ = hmax;
        int fullHeight = hmax;
        if ( axisLabel_ != null && axisLabel_.trim().length() > 0 ) {
            fullHeight += fm_.getHeight() + fm_.getDescent();
        }
        axisBounds_ =
            new Rectangle( xmin, tickStyle_.isDown() ? -fullHeight : 0,
                           xmax - xmin, fullHeight );
    }

    /**
     * Returns the bounding box that contains the axis and annotations
     * drawn that this labeller would like to draw.  
     * In the horizontal direction this covers
     * at least the range 0-npix, and may be larger if some numeric labels
     * extend beyond the axis itself.  In the vertical direction it
     * includes space for the height of numeric labels and possibly a
     * text label.
     *
     * @return  annotation bounding box
     */
    public Rectangle getAnnotationBounds() {
        return new Rectangle( axisBounds_ );
    }

    /**
     * Returns the number of pixels along this axis.
     *
     * @return   npix
     */
    public int getNpix() {
        return npix_;
    }

    /**
     * Sets the number of pixels along this axis.
     *
     * @param  npix  axis length in pixels
     */
    public void setNpix( int npix ) {
        if ( npix != npix_ ) {
            npix_ = npix;
            configure();
        }
    }

    /**
     * Returns the number of pixels below 0 available for drawing on.
     *
     * @return   left padding pixel count 
     */
    public int getLoPad() {
        return loPad_;
    }

    /**
     * Returns the number of pixels above npix available for drawing on.
     *
     * @return  right padding pixel count
     */
    public int getHiPad() {
        return hiPad_;
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
                         tickStyle_.isDown() ? maxHeight_ + fm_.getHeight()
                                             : -maxHeight_ - fm_.getDescent() );
        }

        /* Draw the tick marks. */
        for ( int i = 0; i < tickLabels_.length; i++ ) {
            int tpos = tickLabels_[ i ].pos_;
            String ttext = tickLabels_[ i ].text_;
            g.drawLine( tpos, -2, tpos, +2 );
            if ( drawText_ ) {
                Rectangle bounds = tickStyle_.getBounds( fm_, tpos, 0, ttext );
                if ( bounds.x >= 0 - loPad_ &&
                     bounds.x + bounds.width <= npix_ + hiPad_ ) {
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
     * @param   y1  y coordinate of the other end of the lines
     */
    public void drawGridLines( Graphics g, int y0, int y1 ) {
        for ( int i = 0; i < tickLabels_.length; i++ ) {
            int tpos = tickLabels_[ i ].pos_;
            g.drawLine( tpos, y0, tpos, y1 );
        }
    }

    /**
     * Draws a single grid line on a given graphics context.
     * The line will be drawn vertically, the axis being considered
     * horizontal and starting at the origin.  The vertical extent of the
     * grid line is given by two values <code>y0</code> and <code>y1</code>.
     * The horizontal position is given by the <code>value</code>.
     * If the line is out of range, no action is taken.
     * It is the caller's responsibility to set colours and so on.
     *
     * @param   g   graphics context
     * @param   y0  y coordinate of one end of the lines
     * @param   y1  y coordinate of the other end of the lines
     * @param   value  x position of the line in data coordinates
     */
    public void drawGridLine( Graphics g, int y0, int y1, double value ) {
        if ( value >= lo_ && value <= hi_ ) {
            int tpos = getTickPosition( value );
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
     * Returns the position on the axis in graphics coordinates offset from
     * the origin at which a tickmark with a given numerical value will
     * appear.
     *
     * @param  tick  numeric value associated with tickmark
     * @return   graphical coordinate for tickmark position
     */
    private int getTickPosition( double tick ) {
        double frac = log_ ? Math.log( tick / lo_ ) / Math.log( hi_ / lo_ )
                           : ( tick - lo_ ) / ( hi_ - lo_ );
        return (int) Math.round( npix_ * ( flip_ ? ( 1. - frac ) : frac ) );
    }

    /**
     * Defines tick mark annotation styles.
     */
    public static abstract class TickStyle {

        /**
         * Private sole constructor.
         */
        private TickStyle() {
        }

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
        Rectangle getBounds( FontMetrics fm, int x, int y, String label ) {
            int w = fm.stringWidth( label );
            int h = fm.getHeight();
            return new Rectangle( x - w / 2, y, w, h );
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
            return new Rectangle( x - h / 2, - y - w, h, w );
        }
        boolean isDown() {
            return false;
        }
    };

    /** Tick style suitable for right-hand-side Y axis labels. */
    public static final TickStyle ANTI_Y = new TickStyle() {
        void drawString( Graphics g, FontMetrics fm, int x, int y,
                         String label ) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.translate( x, y );
            g2.rotate( - Math.PI * 0.5 );
            g2.drawString( label, fm.charWidth( '0' ) / 2,
                           fm.getAscent() / 2 );
        }
        Rectangle getBounds( FontMetrics fm, int x, int y, String label ) {
            int w = fm.stringWidth( label ) + fm.charWidth( '0' ) / 2;
            int h = fm.getHeight();
            return new Rectangle( x - h / 2, y - w, h, w );
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
