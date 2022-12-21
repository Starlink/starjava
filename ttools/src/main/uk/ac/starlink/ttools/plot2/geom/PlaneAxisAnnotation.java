package uk.ac.starlink.ttools.plot2.geom;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;
import uk.ac.starlink.ttools.plot2.Axis;
import uk.ac.starlink.ttools.plot2.Caption;
import uk.ac.starlink.ttools.plot2.Captioner;
import uk.ac.starlink.ttools.plot2.NullCaptioner;
import uk.ac.starlink.ttools.plot2.Orientation;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Surround;
import uk.ac.starlink.ttools.plot2.Tick;
import uk.ac.starlink.ttools.plot2.TickLook;

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
    private final Tick[] x2ticks_;
    private final Tick[] y2ticks_;
    private final String x2label_;
    private final String y2label_;
    private final Captioner captioner_;
    private final boolean xAnnotate_;
    private final boolean yAnnotate_;
    private final int xoff_;
    private final int yoff_;
    private final int x2off_;
    private final int y2off_;

    public static final boolean INVERT_Y = true;
    public static final Orientation X_ORIENT = Orientation.X;
    public static final Orientation Y_ORIENT = Orientation.Y;
    public static final Orientation X2_ORIENT = Orientation.ANTI_X;
    public static final Orientation Y2_ORIENT = Orientation.ANTI_Y;

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
     * @param  x2ticks  array of ticks along secondary X axis, may be null
     * @param  y2ticks  array of ticks along secondary Y axis, may be null
     * @param  x2label  text label on secondary X axis
     * @param  y2label  text label on secondary Y axis
     * @param  captioner   text renderer for axis labels etc
     * @param  xAnnotate   true iff annotations are required on X axis
     * @param  yAnnotate   true iff annotations are required on Y axis
     */
    public PlaneAxisAnnotation( int gxlo, int gxhi, int gylo, int gyhi,
                                Axis xaxis, Axis yaxis,
                                Tick[] xticks, Tick[] yticks,
                                String xlabel, String ylabel,
                                Tick[] x2ticks, Tick[] y2ticks,
                                String x2label, String y2label,
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
        x2ticks_ = x2ticks == null ? new Tick[ 0 ] : x2ticks;
        y2ticks_ = y2ticks == null ? new Tick[ 0 ] : y2ticks;
        x2label_ = x2label;
        y2label_ = y2label;
        captioner_ = captioner;
        xAnnotate_ = xAnnotate;
        yAnnotate_ = yAnnotate;
        xoff_ = gxlo;
        yoff_ = gyhi;
        x2off_ = gxhi;
        y2off_ = gylo;
    }

    public void drawLabels( Graphics g ) {
        Graphics2D g2 = (Graphics2D) g;
        Captioner xCaptioner = xAnnotate_ ? captioner_ : NullCaptioner.INSTANCE;
        Captioner yCaptioner = yAnnotate_ ? captioner_ : NullCaptioner.INSTANCE;
        TickLook tickLook = TickLook.STANDARD;
        AffineTransform trans0 = g2.getTransform();
        AffineTransform transX = new AffineTransform( trans0 );
        transX.concatenate( axisTransform( xoff_, yoff_, false ) );
        AffineTransform transY = new AffineTransform( trans0 );
        transY.concatenate( axisTransform( xoff_, yoff_, true ) );
        g2.setTransform( transX );
        xaxis_.drawLabels( xticks_, xlabel_, xCaptioner, tickLook,
                           X_ORIENT, false, g2 );
        g2.setTransform( transY );
        yaxis_.drawLabels( yticks_, ylabel_, yCaptioner, tickLook,
                           Y_ORIENT, INVERT_Y, g2 );
        if ( x2ticks_.length > 0 || x2label_ != null ) {
            AffineTransform transX2 = new AffineTransform( trans0 );
            transX2.concatenate( axisTransform( xoff_, y2off_, false ) );
            g2.setTransform( transX2 );
            xaxis_.drawLabels( x2ticks_, x2label_, xCaptioner, tickLook,
                               X2_ORIENT, false, g2 );
        }
        if ( y2ticks_.length > 0 || y2label_ != null ) {
            AffineTransform transY2 = new AffineTransform( trans0 );
            transY2.concatenate( axisTransform( x2off_, yoff_, true ) );
            g2.setTransform( transY2 );
            yaxis_.drawLabels( y2ticks_, y2label_, yCaptioner, tickLook,
                               Y2_ORIENT, INVERT_Y, g2 );
        }
        g2.setTransform( trans0 );
    }

    public Surround getSurround( boolean withScroll ) {
        Rectangle bounds =
            new Rectangle( gxlo_, gylo_, gxhi_ - gxlo_, gyhi_ - gylo_ );
        Surround surround = new Surround();
        Captioner xCaptioner = xAnnotate_ ? captioner_ : NullCaptioner.INSTANCE;
        Captioner yCaptioner = yAnnotate_ ? captioner_ : NullCaptioner.INSTANCE;

        /* Extend the surround rectangles by the rectangles bounding
         * the annotations on the X and Y axes.
         * I feel there may be a more straightforward way to do this than
         * the following, since we are effectively rotating the graphics
         * space twice, but this does do the right thing, so leave it be. */
        Rectangle xrect =
            getLabelBounds( xaxis_, xticks_, xlabel_, xCaptioner,
                            X_ORIENT, false, withScroll );
        Rectangle bottomRect = axisTransform( xoff_, yoff_, false )
                              .createTransformedShape( xrect )
                              .getBounds();
        int bottomExtent = bottomRect.y + bottomRect.height - gyhi_;
        int bottomUnder = Math.max( 0, gxlo_ - bottomRect.x );
        int bottomOver = Math.max( 0, bottomRect.x + bottomRect.width - gxhi_ );
        surround.bottom =
            new Surround.Block( bottomExtent, bottomUnder, bottomOver );
        Rectangle yrect =
            getLabelBounds( yaxis_, yticks_, ylabel_, yCaptioner,
                            Y_ORIENT, INVERT_Y, withScroll );
        Rectangle leftRect = axisTransform( xoff_, yoff_, true )
                            .createTransformedShape( yrect )
                            .getBounds();
        int leftExtent = gxlo_ - leftRect.x;
        int leftUnder = Math.max( 0, gylo_ - leftRect.y );
        int leftOver = Math.max( 0, leftRect.y + leftRect.height - gyhi_ );
        surround.left =
            new Surround.Block( leftExtent, leftUnder, leftOver );
        if ( x2ticks_.length > 0 || x2label_ != null ) {
            Rectangle x2rect =
                getLabelBounds( xaxis_, x2ticks_, x2label_, xCaptioner,
                                X2_ORIENT, false, withScroll );
            Rectangle topRect = axisTransform( xoff_, y2off_, false )
                               .createTransformedShape( x2rect )
                               .getBounds();
            int topExtent = gylo_ - topRect.y;
            int topUnder = Math.max( 0, gxlo_ - topRect.x );
            int topOver = Math.max( 0, topRect.x + topRect.width - gxhi_ );
            surround.top =
                new Surround.Block( topExtent, topUnder, topOver );
        }
        if ( y2ticks_.length > 0 || y2label_ != null ) {
            Rectangle y2rect =
                getLabelBounds( yaxis_, y2ticks_, y2label_, yCaptioner,
                                Y2_ORIENT, INVERT_Y, withScroll );
            Rectangle rightRect = axisTransform( x2off_, yoff_, true )
                                 .createTransformedShape( y2rect )
                                 .getBounds();
            int rightExtent = rightRect.x + rightRect.width - gxhi_;
            int rightUnder = Math.max( 0, gylo_ - rightRect.y );
            int rightOver = Math.max( 0, rightRect.y + rightRect.height -gyhi_);
            surround.right =
                new Surround.Block( rightExtent, rightUnder, rightOver );
        }
        return surround;
    }

    /**
     * Returns the rectangle required to accommodate the ticks and text
     * decorating an axis.
     *
     * @param  axis  axis
     * @param  ticks  tickmark array
     * @param  label  axis label text, may be null
     * @param  captioner  text positioning object
     * @param  orient  axis orientation code
     * @param  invert  whether to reverse sense of axis
     * @param   withScroll  true to reserve space for nicer scrolling
     * @return   bounding box for all annotations
     */
    private static Rectangle getLabelBounds( Axis axis, Tick[] ticks,
                                             String label, Captioner captioner,
                                             Orientation orient, boolean invert,
                                             boolean withScroll ) {
        final Tick[] ticks1;

        /* If withScroll is set, add fake tickmarks containing all of the
         * tickmark text at both the lower and upper extents of the axes.
         * Then scrolling the axes so that tickmarks with these labels
         * are nearer the end of the axis won't increase the amount of
         * space required. */
        if ( withScroll ) {

            /* Work out the data space positions of both ends of the axis.
             * The +/-0.5 is required in practice to avoid numerical
             * instability (int conversion boundaries) when this data
             * space position gets converted back to graphics coordinates. */
            int[] glims = axis.getGraphicsLimits();
            double d1 = axis.graphicsToData( glims[ 0 ] - 0.5 );
            double d2 = axis.graphicsToData( glims[ 1 ] + 0.5 );
            List<Tick> tickList = new ArrayList<>( 3 * ticks.length );
            for ( Tick tick : ticks ) {
                Caption caption = tick.getLabel();
                tickList.add( tick );
                tickList.add( new Tick( d1, caption ) );
                tickList.add( new Tick( d2, caption ) );
            }
            ticks1 = tickList.toArray( new Tick[ 0 ] );
        }
        else {
            ticks1 = ticks;
        }

        /* Get the axis itself to determine the required space given the
         * ticks we are supplying. */
        return axis.getLabelBounds( ticks1, label, captioner, orient, invert );
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
    private static AffineTransform axisTransform( int x0, int y0, boolean isY ){
        AffineTransform trans = new AffineTransform();
        trans.translate( x0, y0 );
        if ( isY ) {
            trans.rotate( -0.5 * Math.PI );
        }
        return trans;
    }
}
