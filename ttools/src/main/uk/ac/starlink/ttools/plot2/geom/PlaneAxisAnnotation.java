package uk.ac.starlink.ttools.plot2.geom;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import uk.ac.starlink.ttools.plot2.Axis;
import uk.ac.starlink.ttools.plot2.Captioner;
import uk.ac.starlink.ttools.plot2.NullCaptioner;
import uk.ac.starlink.ttools.plot2.Orientation;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Tick;

/**
 * AxisAnnotation implementation for 2D surfaces.
 *
 * @author   Mark Taylor
 * @since    26 Jul 2013
 */
public class PlaneAxisAnnotation implements AxisAnnotation {

    private final int gxlo_;
    private final int gxhi_;
    private final int gylo_;
    private final int gyhi_;
    private final Axis xaxis_;
    private final Axis yaxis_;
    private final Tick[] xticks_;
    private final Tick[] yticks_;
    private final String xlabel_;
    private final String ylabel_;
    private final Captioner captioner_;
    private final boolean xAnnotate_;
    private final boolean yAnnotate_;
    private final int xoff_;
    private final int yoff_;

    public static final boolean INVERT_Y = true;
    public static final Orientation X_ORIENT = Orientation.X;
    public static final Orientation Y_ORIENT = Orientation.Y;

    /**
     * Constructor.
     *
     * @param  gxlo    graphics X coordinate lower bound
     * @param  gxhi    graphics X coordinate upper bound
     * @param  gylo    graphics Y coordinate lower bound
     * @param  gyhi    graphics Y coordinate upper bound
     * @param  xaxis   X axis object
     * @param  yaxis   Y axis object
     * @param  xticks  array of ticks along the X axis
     * @param  yticks  array of ticks along the Y axis
     * @param  xlabel  text label on X axis
     * @param  ylabel  text label on Y axis
     * @param  captioner   text renderer for axis labels etc
     * @param  xAnnotate   true iff annotations are required on X axis
     * @param  yAnnotate   true iff annotations are required on Y axis
     */
    public PlaneAxisAnnotation( int gxlo, int gxhi, int gylo, int gyhi,
                                Axis xaxis, Axis yaxis,
                                Tick[] xticks, Tick[] yticks,
                                String xlabel, String ylabel,
                                Captioner captioner,
                                boolean xAnnotate, boolean yAnnotate ) {
        gxlo_ = gxlo;
        gxhi_ = gxhi;
        gylo_ = gylo;
        gyhi_ = gyhi;
        xaxis_ = xaxis;
        yaxis_ = yaxis;
        xticks_ = xticks;
        yticks_ = yticks; 
        xlabel_ = xlabel;
        ylabel_ = ylabel;
        captioner_ = captioner;
        xAnnotate_ = xAnnotate;
        yAnnotate_ = yAnnotate;
        xoff_ = gxlo;
        yoff_ = gyhi;
    }

    public void drawLabels( Graphics g ) {
        Graphics2D g2 = (Graphics2D) g;
        AffineTransform trans0 = g2.getTransform();
        AffineTransform transX = new AffineTransform( trans0 );
        transX.concatenate( axisTransform( xoff_, yoff_, false ) );
        AffineTransform transY = new AffineTransform( trans0 );
        transY.concatenate( axisTransform( xoff_, yoff_, true ) );
        g2.setTransform( transX );
        xaxis_.drawLabels( xticks_, xlabel_,
                           xAnnotate_ ? captioner_ : NullCaptioner.INSTANCE,
                           X_ORIENT, false, g2 );
        g2.setTransform( transY );
        yaxis_.drawLabels( yticks_, ylabel_,
                           yAnnotate_ ? captioner_ : NullCaptioner.INSTANCE,
                           Y_ORIENT, INVERT_Y, g2 );
        g2.setTransform( trans0 );
    }

    public Insets getPadding( boolean withScroll ) {
        Rectangle bounds =
            new Rectangle( gxlo_, gylo_, gxhi_ - gxlo_, gyhi_ - gylo_ );

        /* Get the bounds of the plot region. */
        Rectangle labelRect = new Rectangle( bounds );

        /* Extend this by the the rectangles bounding the annotations
         * on the X and Y axes. */
        Rectangle xrect =
            xaxis_.getLabelBounds( xticks_, xlabel_,
                                   xAnnotate_ ? captioner_
                                              : NullCaptioner.INSTANCE,
                                   X_ORIENT, false );
        labelRect.add( axisTransform( xoff_, yoff_, false )
                      .createTransformedShape( xrect )
                      .getBounds() );
        Rectangle yrect =
            yaxis_.getLabelBounds( yticks_, ylabel_,
                                   yAnnotate_ ? captioner_
                                              : NullCaptioner.INSTANCE,
                                   Y_ORIENT, INVERT_Y );
        labelRect.add( axisTransform( xoff_, yoff_, true )
                      .createTransformedShape( yrect )
                      .getBounds() );

        /* Work out what that means in terms of insets. */
        int top = bounds.y - labelRect.y;
        int left = bounds.x - labelRect.x;
        int bottom = labelRect.y + labelRect.height - bounds.y - bounds.height;
        int right = labelRect.x + labelRect.width - bounds.x - bounds.width;
        Insets insets = new Insets( top, left, bottom, right );

        /* If scrolling is required, extend the insets by the maximum
         * amount that displaced axis labels might require. */
        if ( withScroll ) {
            Insets scrollInsets = getScrollTickPadding();
            insets.top = Math.max( insets.top, scrollInsets.top );
            insets.left = Math.max( insets.left, scrollInsets.left );
            insets.bottom = Math.max( insets.bottom, scrollInsets.bottom );
            insets.right = Math.max( insets.right, scrollInsets.right );
        }
        return insets;
    }

    /**
     * Returns padding insets for tick mark text
     * if scrolling adjustments are required.
     *
     * @return  insets
     */
    private Insets getScrollTickPadding() {
        Rectangle tickPad = new Rectangle( 0, 0, 0, 0 );

        /* Get bounding boxes for largest ticks on each axis. */
        if ( xAnnotate_ ) {
            tickPad.add( getMaxTickSizeBounds( xticks_, false ) );
        }
        if ( yAnnotate_ ) {
            tickPad.add( getMaxTickSizeBounds( yticks_, true ) );
        }

        /* Extend the insets enough to accommodate the largest tick
         * positioned at the extreme ends of each axis. */
        int left = -tickPad.x;
        int right = tickPad.width + tickPad.x;
        int top = -tickPad.y;
        int bottom = tickPad.height + tickPad.y;

        /* Return the resulting box. */
        return new Insets( top, left, bottom, right );
    }

    /**
     * Returns a rectangle large enougn to bound the text associated
     * with any one of a supplied list of ticks on an axis.
     * The returned rectangle is in unrotated graphics coordinates,
     * so that -Y is up, not (necessarily) perpendicular to the axis.
     *
     * @param  ticks  ticks to assess bounds of
     * @param  isY  true for Y axis, false for X axis
     * @param  largest necessary bounding box for one tick
     */
    private Rectangle getMaxTickSizeBounds( Tick[] ticks, boolean isY ) {
        Orientation orient = isY ? Y_ORIENT : X_ORIENT;
        AffineTransform axisTrans = axisTransform( 0, 0, isY );
        int cpad = captioner_.getPad();
        Rectangle bounds = new Rectangle( 0, 0, 0, 0 );
        for ( int it = 0; it < ticks.length; it++ ) {
            Tick tick = ticks[ it ];
            String label = tick.getLabel();
            if ( label != null ) {
                Rectangle b0 = captioner_.getCaptionBounds( tick.getLabel() );
                AffineTransform trans = new AffineTransform( axisTrans );
                trans.concatenate( orient.captionTransform( b0, cpad ) );
                bounds.add( trans.createTransformedShape( b0 ).getBounds() );
            }
        }
        return bounds;
    }

    /**
     * Returns the AffineTransform which transforms an indicated axis
     * to run from the origin in the positive X direction.
     *
     * @param   x0  origin X coordinate in starting coords
     * @param   y0  origin Y coordinate in starting coords
     * @param   isY  true for Y axis, false for X axis
     * @return   transform that transforms starting coords to axis coords
     */
    private AffineTransform axisTransform( int x0, int y0, boolean isY ) {
        AffineTransform trans = new AffineTransform();
        trans.translate( x0, y0 );
        if ( isY ) {
            trans.rotate( -0.5 * Math.PI );
        }
        return trans;
    }
}
