package uk.ac.starlink.ttools.plot2.geom;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;
import uk.ac.starlink.ttools.plot2.Axis;
import uk.ac.starlink.ttools.plot2.Captioner;
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
    private final int xoff_;
    private final int yoff_;

    public static final boolean INVERT_Y = true;
    private static final Orientation X_ORIENT = Orientation.X;
    private static final Orientation Y_ORIENT = Orientation.Y;

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
     */
    public PlaneAxisAnnotation( int gxlo, int gxhi, int gylo, int gyhi,
                                Axis xaxis, Axis yaxis,
                                Tick[] xticks, Tick[] yticks,
                                String xlabel, String ylabel,
                                Captioner captioner ) {
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
        xaxis_.drawLabels( xticks_, xlabel_, captioner_, X_ORIENT,
                           false, g2 );
        g2.setTransform( transY );
        yaxis_.drawLabels( yticks_, ylabel_, captioner_, Y_ORIENT,
                           INVERT_Y, g2 );
        g2.setTransform( trans0 );
    }

    public Insets getPadding( boolean withScroll ) {
        Insets insets = withScroll ? getScrollTickPadding()
                                   : getNoScrollTickPadding();
        insets.left += 2;
        insets.bottom += 2;
        if ( xlabel_ != null ) {
            Rectangle cxbounds = captioner_.getCaptionBounds( xlabel_ );
            insets.bottom += -cxbounds.y + captioner_.getPad();
        }
        if ( ylabel_ != null ) {
            Rectangle cybounds = captioner_.getCaptionBounds( ylabel_ );
            insets.left += cybounds.height + captioner_.getPad();
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
        tickPad.add( getMaxTickSizeBounds( xticks_, false ) );
        tickPad.add( getMaxTickSizeBounds( yticks_, true ) );

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
     * Returns padding insets for tick mark text
     * if scrolling adjustments are not required.
     *
     * @return  insets
     */
    private Insets getNoScrollTickPadding() {

        // This method seems to be broken - probably the problem is in
        // getTickBoxes.  Defer to getScrollTickPadding instead for now.
        if ( true ) return getScrollTickPadding();

        /* Make a rectangle big enough to hold every ticmark painted
         * in its actual position. */
        Rectangle bounds = new Rectangle( xoff_, yoff_, 0, 0 );
        Rectangle[] boxes =
            PlotUtil.arrayConcat( getTickBoxes( xticks_, false ),
                                  getTickBoxes( yticks_, true ) );
        for ( int ib = 0; ib < boxes.length; ib++ ) {
            bounds.add( boxes[ ib ] );
        }

        /* Turn that into an insets object relative to the plot bounds,
         * and return. */
        int left = Math.max( 0, gxlo_ - bounds.x );
        int right = Math.max( 0, gxhi_ - bounds.x - bounds.width );
        int top = Math.max( 0, gylo_ - bounds.y );
        int bottom = Math.max( 0, gyhi_ - bounds.y - bounds.height );
        return new Insets( top, left, bottom, right );
    }

    /**
     * Returns an array of bounding boxes for the tick labels on an axis.
     *
     * @param  ticks  tick array
     * @param  isY  true for Y axis, false for X axis
     * @return  bounding box array
     */
    private Rectangle[] getTickBoxes( Tick[] ticks, boolean isY ) {
        Orientation orient = isY ? Y_ORIENT : X_ORIENT;
        AffineTransform axisTrans = axisTransform( 0, 0, isY );
        int cpad = captioner_.getPad();
        List<Rectangle> list = new ArrayList<Rectangle>();
        for ( int it = 0; it < ticks.length; it++ ) {
            Tick tick = ticks[ it ];
            String label = tick.getLabel();
            if ( label != null ) {
                Rectangle b0 = captioner_.getCaptionBounds( label );
                AffineTransform trans = new AffineTransform( axisTrans );
                double gx = ( isY ? yaxis_ : xaxis_ )
                           .dataToGraphics( tick.getValue() );
                double tx = isY ? ( yoff_ - gx ) : ( gx - xoff_ );
                trans.concatenate( AffineTransform
                                  .getTranslateInstance( tx, 0 ) );
                trans.concatenate( orient.captionTransform( b0, cpad ) );
                list.add( trans.createTransformedShape( b0 ).getBounds() );
            }
        }
        return list.toArray( new Rectangle[ 0 ] );
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
