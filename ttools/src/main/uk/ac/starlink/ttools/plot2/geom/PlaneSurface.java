package uk.ac.starlink.ttools.plot2.geom;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Supplier;
import javax.swing.Icon;
import uk.ac.starlink.ttools.plot2.Axis;
import uk.ac.starlink.ttools.plot2.Captioner;
import uk.ac.starlink.ttools.plot2.CoordSequence;
import uk.ac.starlink.ttools.plot2.Orientation;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Scale;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.Surround;
import uk.ac.starlink.ttools.plot2.Tick;
import uk.ac.starlink.ttools.plot2.TickRun;
import uk.ac.starlink.ttools.plot2.Ticker;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;

/**
 * Surface implementation for flat 2-d plotting.
 *
 * @author   Mark Taylor
 * @since    19 Feb 2013
 */
public class PlaneSurface implements Surface, PlanarSurface {

    private final int gxlo_;
    private final int gxhi_;
    private final int gylo_;
    private final int gyhi_;
    private final double dxlo_;
    private final double dxhi_;
    private final double dylo_;
    private final double dyhi_;
    private final Scale xscale_;
    private final Scale yscale_;
    private final boolean xflip_;
    private final boolean yflip_;
    private final Tick[] xticks_;
    private final Tick[] yticks_;
    private final Orientation xorient_;
    private final Orientation yorient_;
    private final String xlabel_;
    private final String ylabel_;
    private final Tick[] x2ticks_;
    private final Tick[] y2ticks_;
    private final Orientation x2orient_;
    private final Orientation y2orient_;
    private final String x2label_;
    private final String y2label_;
    private final Captioner captioner_;
    private final SideFlags annotateflags_;
    private final Color gridcolor_;
    private final Color axlabelcolor_;
    private final boolean islabelposition_;
    private final Axis xAxis_;
    private final Axis yAxis_;

    /**
     * Constructor.
     *
     * @param  gxlo   graphics X coordinate lower bound
     * @param  gxhi   graphics X coordinate upper bound
     * @param  gylo   graphics Y coordinate lower bound
     * @param  gyhi   graphics Y coordinate upper bound
     * @param  dxlo   data X coordinate lower bound
     * @param  dxhi   data X coordinate upper bound
     * @param  dylo   data Y coordinate lower bound
     * @param  dyhi   data Y coordinate upper bound
     * @param  xscale   scaling on X axis
     * @param  yscale   scaling on Y axis
     * @param  xflip  whether to invert direction of X axis
     * @param  yflip  whether to invert direction of Y axis
     * @param  xticks  array of tickmark objects for X axis
     * @param  yticks  array of tickmark objects for Y axis
     * @param  xorient  tick label orientation on X axis
     * @param  yorient  tick label orientation on Y axis
     * @param  xlabel  text for labelling X axis
     * @param  ylabel  text for labelling Y axis
     * @param  x2ticks  array of tickmark objects for secondary X axis
     * @param  y2ticks  array of tickmark objects for secondary Y axis
     * @param  x2orient  tick label orientation on secondary X axis
     * @param  y2orient  tick label orientation on secondary Y axis
     * @param  x2label  text for labelling secondary X axis
     * @param  y2label  text for labelling secondary Y axis
     * @param  captioner  text renderer for axis labels etc, or null if absent
     * @param  annotateflags  indicates which sides to annotate
     * @param  gridcolor  colour of grid lines, or null if not plotted
     * @param  axlabelcolor  colour of axis labels
     * @param  islabelposition  whether formatted positions should include
     *                          axis labels
     */
    @SuppressWarnings("this-escape")
    public PlaneSurface( int gxlo, int gxhi, int gylo, int gyhi,
                         double dxlo, double dxhi, double dylo, double dyhi,
                         Scale xscale, Scale yscale,
                         boolean xflip, boolean yflip,
                         Tick[] xticks, Tick[] yticks,
                         Orientation xorient, Orientation yorient,
                         String xlabel, String ylabel,
                         Tick[] x2ticks, Tick[] y2ticks,
                         Orientation x2orient, Orientation y2orient,
                         String x2label, String y2label,
                         Captioner captioner, SideFlags annotateflags,
                         Color gridcolor, Color axlabelcolor,
                         boolean islabelposition ) {
        gxlo_ = gxlo;
        gxhi_ = gxhi;
        gylo_ = gylo;
        gyhi_ = gyhi;
        dxlo_ = dxlo;
        dxhi_ = dxhi;
        dylo_ = dylo;
        dyhi_ = dyhi;
        xscale_ = xscale;
        yscale_ = yscale;
        xflip_ = xflip;
        yflip_ = yflip;
        xticks_ = xticks;
        yticks_ = yticks;
        xorient_ = xorient;
        yorient_ = yorient;
        xlabel_ = xlabel;
        ylabel_ = ylabel;
        x2ticks_ = x2ticks;
        y2ticks_ = y2ticks;
        x2orient_ = x2orient;
        y2orient_ = y2orient;
        x2label_ = x2label;
        y2label_ = y2label;
        captioner_ = captioner;
        annotateflags_ = annotateflags;
        gridcolor_ = gridcolor;
        axlabelcolor_ = axlabelcolor;
        islabelposition_ = islabelposition;
        xAxis_ = new Axis( gxlo_, gxhi_, dxlo_, dxhi_, xscale_, xflip_ );
        yAxis_ = new Axis( gylo_, gyhi_, dylo_, dyhi_, yscale_,
                           yflip_ ^ PlaneAxisAnnotation.INVERT_Y );
        assert this.equals( this );
    }

    public Rectangle getPlotBounds() {
        return new Rectangle( gxlo_, gylo_, gxhi_ - gxlo_, gyhi_ - gylo_ );
    }

    public Surround getSurround( boolean withScroll ) {
        return createAxisAnnotation().getSurround( withScroll );
    }

    /**
     * Returns 2.
     */
    public int getDataDimCount() {
        return 2;
    }

    public boolean dataToGraphics( double[] dpos, boolean visibleOnly,
                                   Point2D.Double gp ) {
        double gx = xAxis_.dataToGraphics( dpos[ 0 ] );
        double gy = yAxis_.dataToGraphics( dpos[ 1 ] );
        if ( ! visibleOnly ||
             ( gx >= gxlo_ && gx < gxhi_ && gy >= gylo_ && gy < gyhi_ ) ) {
            gp.x = gx;
            gp.y = gy;
            return true;
        }
        else {
            return false;
        }
    }

    public boolean dataToGraphicsOffset( double[] dpos0, Point2D.Double gpos0,
                                         double[] dpos1, boolean visibleOnly,
                                         Point2D.Double gpos1 ) {
        return dataToGraphics( dpos1, visibleOnly, gpos1 );
    }

    public double[] graphicsToData( Point2D gp,
                                    Supplier<CoordSequence> dposSupplier ) {
        return new double[] { xAxis_.graphicsToData( gp.getX() ),
                              yAxis_.graphicsToData( gp.getY() ) };
    }

    public boolean isContinuousLine( double[] dpos0, double[] dpos1 ) {
        return true;
    }

    public String formatPosition( double[] dpos ) {
        StringBuilder sbuf = new StringBuilder();
        sbuf.append( formatPosition( xAxis_, dpos[ 0 ] ) )
            .append( ", " )
            .append( formatPosition( yAxis_, dpos[ 1 ] ) );
        if ( islabelposition_ ) {
            sbuf.append( " (" )
                .append( xlabel_ )
                .append( ", " )
                .append( ylabel_ )
                .append( ")" );
        }
        return sbuf.toString();
    }

    public Captioner getCaptioner() {
        return captioner_;
    }

    public void paintBackground( Graphics g ) {
        Color color0 = g.getColor();
        g.setColor( Color.WHITE );
        g.fillRect( gxlo_, gylo_, gxhi_ - gxlo_, gyhi_ - gylo_ );
        g.setColor( color0 );
    }

    public void paintForeground( Graphics g ) {
        Color color0 = g.getColor();

        /* Grid lines. */
        if ( gridcolor_ != null ) {
            Graphics gGrid = PlotUtil.createLineGraphics( g, gridcolor_ );
            for ( Tick tick : xticks_ ) {
                if ( tick.getLabel() != null ) {
                    int gx = (int) xAxis_.dataToGraphics( tick.getValue() );
                    gGrid.drawLine( gx, gylo_, gx, gyhi_ );
                }
            }
            for ( Tick tick : yticks_ ) {
                if ( tick.getLabel() != null ) {
                    int gy = (int) yAxis_.dataToGraphics( tick.getValue() );
                    gGrid.drawLine( gxlo_, gy, gxhi_, gy );
                }
            }
        }

        /* Axis labels. */
        if ( axlabelcolor_ != null ) {
            g.setColor( axlabelcolor_ );
            createAxisAnnotation().drawLabels( g );

            /* Boundary. */
            g.drawRect( gxlo_, gylo_, gxhi_ - gxlo_, gyhi_ - gylo_ );
        }

        /* Restore. */
        g.setColor( color0 );
    }

    /**
     * Returns approximate config to recreate this surface's aspect.
     *
     * @return  approximate aspect config
     */
    ConfigMap getAspectConfig() {
        ConfigMap config = new ConfigMap();
        config.putAll( PlotUtil.configLimits( PlaneSurfaceFactory.XMIN_KEY,
                                              PlaneSurfaceFactory.XMAX_KEY,
                                              dxlo_, dxhi_, gxhi_ - gxlo_ ) );
        config.putAll( PlotUtil.configLimits( PlaneSurfaceFactory.YMIN_KEY,
                                              PlaneSurfaceFactory.YMAX_KEY,
                                              dylo_, dyhi_, gyhi_ - gylo_ ) );
        return config;
    }

    public double[][] getDataLimits() {
        return new double[][] { { dxlo_, dxhi_ }, { dylo_, dyhi_ } };
    }

    public boolean[] getLogFlags() {
        return new boolean[] { xscale_.isPositiveDefinite(),
                               yscale_.isPositiveDefinite() };
    }

    public boolean[] getFlipFlags() {
        return new boolean[] { xflip_, yflip_ };
    }

    public boolean[] getTimeFlags() {
        return new boolean[] { false, false };
    }

    public Axis[] getAxes() {
        return new Axis[] { xAxis_, yAxis_ };
    }

    /**
     * Returns a plot aspect representing a view of this surface zoomed
     * in some or all dimensions around the given central position.
     *
     * @param  pos  reference graphics position
     * @param  xZoom  X axis zoom factor
     * @param  yZoom  Y axis zoom factor
     * @return  new aspect
     */
    PlaneAspect zoom( Point2D pos, double xZoom, double yZoom ) {
        return new PlaneAspect(
            xAxis_.dataZoom( xAxis_.graphicsToData( pos.getX() ), xZoom ),
            yAxis_.dataZoom( yAxis_.graphicsToData( pos.getY() ), yZoom ) );
    }

    /**
     * Returns a plot aspect representing a view of this surface panned
     * such that the data that used to appear at one graphics coordinate
     * now appears at another.
     *
     * @param   pos0  source graphics position
     * @param   pos1  destination graphics position
     * @param   xFlag  true iff panning will operate in X direction
     * @param   yFlag  true iff panning will operate in Y direction
     * @return  new aspect, or null
     */
    PlaneAspect pan( Point2D pos0, Point2D pos1,
                     boolean xFlag, boolean yFlag ) {
        if ( xFlag || yFlag ) {
            return new PlaneAspect(
                xFlag ? xAxis_.dataPan( xAxis_.graphicsToData( pos0.getX() ),
                                        xAxis_.graphicsToData( pos1.getX() ) )
                      : new double[] { dxlo_, dxhi_ },
                yFlag ? yAxis_.dataPan( yAxis_.graphicsToData( pos0.getY() ),
                                        yAxis_.graphicsToData( pos1.getY() ) )
                      : new double[] { dylo_, dyhi_ } );
        }
        else {
            return null;
        }
    }

    /**
     * Returns a plot aspect in which the given data position is centred.
     *
     * @param  dpos  data position to end up central
     * @param  xFlag  true to center in X direction
     * @param  yFlag  true to center in Y direction
     * @return  new aspect
     */
    PlaneAspect center( double[] dpos, boolean xFlag, boolean yFlag ) {
        Point2D.Double gp = new Point2D.Double();
        return dataToGraphics( dpos, false, gp ) && PlotUtil.isPointFinite( gp )
             ? pan( gp, new Point2D.Double( ( gxlo_ + gxhi_ ) * 0.5,
                                            ( gylo_ + gyhi_ ) * 0.5 ),
                    xFlag, yFlag )
             : null;
    }

    /**
     * Returns a plot aspect covering the data region which is currently
     * covered by a given rectangle in graphics coordinates.
     *
     * @param   frame  rectangle in current graphics coordinates
     *                 giving data space region of interest
     */
    PlaneAspect reframe( Rectangle frame ) {
        Point gp1 = new Point( frame.x, frame.y );
        Point gp2 = new Point( frame.x + frame.width, frame.y + frame.height );
        double[] dpos1 = graphicsToData( gp1, null );
        double[] dpos2 = graphicsToData( gp2, null );
        return new PlaneAspect( PlotUtil.orderPair( dpos1[ 0 ], dpos2[ 0 ] ),
                                PlotUtil.orderPair( dpos1[ 1 ], dpos2[ 1 ] ) );
    }

    /**
     * Returns an axis annotation object for this surface.
     *
     * @return   axis annotation
     */
    private AxisAnnotation createAxisAnnotation() {
        return new PlaneAxisAnnotation( gxlo_, gxhi_, gylo_, gyhi_,
                                        xAxis_, yAxis_,
                                        xticks_, yticks_, xorient_, yorient_,
                                        xlabel_, ylabel_,
                                        x2ticks_, y2ticks_, x2orient_,y2orient_,
                                        x2label_, y2label_,
                                        captioner_, annotateflags_ );
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof PlaneSurface ) {
            PlaneSurface other = (PlaneSurface) o;
            return this.gxlo_ == other.gxlo_
                && this.gxhi_ == other.gxhi_
                && this.gylo_ == other.gylo_
                && this.gyhi_ == other.gyhi_
                && this.dxlo_ == other.dxlo_
                && this.dxhi_ == other.dxhi_
                && this.dylo_ == other.dylo_
                && this.dyhi_ == other.dyhi_
                && this.xscale_.equals( other.xscale_ )
                && this.yscale_.equals( other.yscale_ )
                && this.xflip_ == other.xflip_
                && this.yflip_ == other.yflip_
                && Arrays.equals( this.xticks_, other.xticks_ )
                && Arrays.equals( this.yticks_, other.yticks_ )
                && PlotUtil.equals( this.xorient_, other.xorient_ )
                && PlotUtil.equals( this.yorient_, other.yorient_ )
                && PlotUtil.equals( this.xlabel_, other.xlabel_ )
                && PlotUtil.equals( this.ylabel_, other.ylabel_ )
                && Arrays.equals( this.x2ticks_, other.x2ticks_ )
                && Arrays.equals( this.y2ticks_, other.y2ticks_ )
                && PlotUtil.equals( this.x2orient_, other.x2orient_ )
                && PlotUtil.equals( this.y2orient_, other.y2orient_ )
                && PlotUtil.equals( this.x2label_, other.x2label_ )
                && PlotUtil.equals( this.y2label_, other.y2label_ )
                && this.captioner_.equals( other.captioner_ )
                && this.annotateflags_.equals( other.annotateflags_ )
                && PlotUtil.equals( this.gridcolor_, other.gridcolor_ )
                && PlotUtil.equals( this.axlabelcolor_, other.axlabelcolor_ )
                && this.islabelposition_ == other.islabelposition_;
        }
        else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int code = 23023;
        code = 23 * code + gxlo_;
        code = 23 * code + gxhi_;
        code = 23 * code + gylo_;
        code = 23 * code + gyhi_;
        code = 23 * code + Float.floatToIntBits( (float) dxlo_ );
        code = 23 * code + Float.floatToIntBits( (float) dxhi_ );
        code = 23 * code + Float.floatToIntBits( (float) dylo_ );
        code = 23 * code + Float.floatToIntBits( (float) dyhi_ );
        code = 23 * code + xscale_.hashCode();
        code = 23 * code + yscale_.hashCode();
        code = 23 * code + ( xflip_ ? 4 : 0 )
                         + ( yflip_ ? 8 : 0 );
        code = 23 * code + Arrays.hashCode( xticks_ );
        code = 23 * code + Arrays.hashCode( yticks_ );
        code = 23 * code + PlotUtil.hashCode( xorient_ );
        code = 23 * code + PlotUtil.hashCode( yorient_ );
        code = 23 * code + PlotUtil.hashCode( xlabel_ );
        code = 23 * code + PlotUtil.hashCode( ylabel_ );
        code = 23 * code + Arrays.hashCode( x2ticks_ );
        code = 23 * code + Arrays.hashCode( y2ticks_ );
        code = 23 * code + PlotUtil.hashCode( x2orient_ );
        code = 23 * code + PlotUtil.hashCode( y2orient_ );
        code = 23 * code + PlotUtil.hashCode( x2label_ );
        code = 23 * code + PlotUtil.hashCode( y2label_ );
        code = 23 * code + captioner_.hashCode();
        code = 23 * code + annotateflags_.hashCode();
        code = 23 * code + PlotUtil.hashCode( gridcolor_ );
        code = 23 * code + PlotUtil.hashCode( axlabelcolor_ );
        code = 23 * code + ( islabelposition_ ? 5 : 13 );
        return code;
    }

    /**
     * Utility method to create a PlaneSurface from available requirements.
     * It works out actual data coordinate bounds and tickmarks, and
     * then invokes the constructor.
     *
     * @param  plotBounds  rectangle which the plot data should occupy
     * @param  aspect  surface view configuration
     * @param  xscale  scaling on X axis
     * @param  yscale  scaling on Y axis
     * @param  xflip  whether to invert direction of X axis
     * @param  yflip  whether to invert direction of Y axis
     * @param  xlabel  text for labelling X axis
     * @param  ylabel  text for labelling Y axis
     * @param  x2func  function mapping primary to secondary X axis data coord,
     *                 null for no secondary X axis
     * @param  y2func  function mapping primary to secondary Y axis data coord,
     *                 null for no secondary Y axis
     * @param  x2label  secondary X axis label
     * @param  y2label  secondary Y axis label
     * @param  captioner  text renderer for axis labels etc
     * @param  annotateflags  which sides to annotate
     * @param  xyfactor   ratio (X axis unit length)/(Y axis unit length),
     *                    or NaN to use whatever bounds shape and
     *                    axis limits give you
     * @param  xcrowd  crowding factor for tick marks on X axis;
     *                 1 is normal
     * @param  ycrowd  crowding factor for tick marks on Y axis;
     *                 1 is normal
     * @param  orientpolicy  tick label orientation policy
     * @param  minor   whether to paint minor tick marks on axes
     * @param  shadow  whether to paint shadow ticks on opposite axes
     *                 if no secondary axis
     * @param  gridcolor  colour of grid lines, or null for none
     * @param  axlabelcolor  colour of axis labels
     * @param  islabelposition  whether to include axis labels in
     *                          formatted position text
     * @return  new plot surface
     */
    public static PlaneSurface createSurface( Rectangle plotBounds,
                                              PlaneAspect aspect,
                                              Scale xscale, Scale yscale,
                                              boolean xflip, boolean yflip,
                                              String xlabel, String ylabel,
                                              DoubleUnaryOperator x2func,
                                              DoubleUnaryOperator y2func,
                                              String x2label, String y2label,
                                              Captioner captioner,
                                              SideFlags annotateflags,
                                              double xyfactor,
                                              double xcrowd, double ycrowd,
                                              OrientationPolicy orientpolicy,
                                              boolean minor, boolean shadow,
                                              Color gridcolor,
                                              Color axlabelcolor,
                                              boolean islabelposition ) {
        int gxlo = plotBounds.x;
        int gxhi = plotBounds.x + plotBounds.width;
        int gylo = plotBounds.y;
        int gyhi = plotBounds.y + plotBounds.height;
        double dxlo = aspect.getXMin();
        double dxhi = aspect.getXMax();
        double dylo = aspect.getYMin();
        double dyhi = aspect.getYMax();
        Ticker xTicker = xscale.getTicker();
        Ticker yTicker = yscale.getTicker();

        /* Fixed ratio of X/Y data scales.  Interpret this by ensuring that
         * all of both requested data ranges is included, and one of them is
         * extended if necessary to accommodate the extra graphics space.
         * Only makes much sense if both have the same scaling. */
        final TickRun xtickRun;
        final TickRun ytickRun;
        if ( xyfactor > 0 && xscale.equals( yscale ) ) {
            double gx = gxhi - gxlo;
            double gy = gyhi - gylo;
            double sxlo = xscale.dataToScale( dxlo );
            double sxhi = xscale.dataToScale( dxhi );
            double sylo = yscale.dataToScale( dylo );
            double syhi = yscale.dataToScale( dyhi );
            double sx = sxhi - sxlo;
            double sy = syhi - sylo;
            double fadj = xyfactor * ( gy / sy ) / ( gx / sx );
            if ( fadj > 1 ) {
                double dyadj = sy * ( 1 * fadj - 1 );
                sylo += -0.5 * dyadj;
                syhi += +0.5 * dyadj;
                dylo = yscale.scaleToData( sylo );
                dyhi = yscale.scaleToData( syhi );
            }
            else {
                double dxadj = sx * ( 1 / fadj - 1 );
                sxlo += -0.5 * dxadj;
                sxhi += +0.5 * dxadj;
                dxlo = xscale.scaleToData( sxlo );
                dxhi = xscale.scaleToData( sxhi );
            }
            assert Math.abs( xyfactor * ( gy / ( syhi - sylo ) )
                                      / ( gx / ( sxhi - sxlo ) )
                             - 1 ) < 1e-6;
            xtickRun = xTicker.getTicks( dxlo, dxhi, minor, captioner,
                                         orientpolicy.getOrientationsX(),
                                         plotBounds.width, 1 );
            ytickRun = yTicker.getTicks( dylo, dyhi, minor, captioner,
                                         orientpolicy.getOrientationsY(),
                                         plotBounds.height, 1 );
        }

        /* Otherwise generate standard axis ticks. */
        else {
            xtickRun = xTicker.getTicks( dxlo, dxhi, minor, captioner,
                                         orientpolicy.getOrientationsX(),
                                         plotBounds.width, xcrowd );
            ytickRun = yTicker.getTicks( dylo, dyhi, minor, captioner,
                                         orientpolicy.getOrientationsY(),
                                         plotBounds.height, ycrowd );
        }
        Tick[] xticks = xtickRun.getTicks();
        Tick[] yticks = ytickRun.getTicks();
        Orientation xorient = xtickRun.getOrientation();
        Orientation yorient = ytickRun.getOrientation();
        Axis xAxis = new Axis( gxlo, gxhi, dxlo, dxhi, xscale, xflip );
        Axis yAxis = new Axis( gylo, gyhi, dylo, dyhi, yscale,
                               yflip ^ PlaneAxisAnnotation.INVERT_Y );
        TickRun x2tickRun =
              x2func == null
            ? ( shadow ? new TickRun( PlotUtil.getShadowTicks( xticks ),
                                      Orientation.ANTI_X )
                       : null )
            : SlaveTicker.createTicker( xAxis, x2func )
                         .getTicks( dxlo, dxhi, minor, captioner,
                                    orientpolicy.getOrientationsX2(),
                                    plotBounds.width, xcrowd );
        TickRun y2tickRun =
              y2func == null
            ? ( shadow ? new TickRun( PlotUtil.getShadowTicks( yticks ),
                                      Orientation.ANTI_Y )
                       : null )
            : SlaveTicker.createTicker( yAxis, y2func )
                         .getTicks( dylo, dyhi, minor, captioner,
                                    orientpolicy.getOrientationsY2(),
                                    plotBounds.height, ycrowd );
        Tick[] x2ticks = x2tickRun == null ? null : x2tickRun.getTicks();
        Tick[] y2ticks = y2tickRun == null ? null : y2tickRun.getTicks();
        Orientation x2orient = x2tickRun == null
                             ? null
                             : x2tickRun.getOrientation();
        Orientation y2orient = y2tickRun == null
                             ? null
                             : y2tickRun.getOrientation();
        return new PlaneSurface( gxlo, gxhi, gylo, gyhi, dxlo, dxhi, dylo, dyhi,
                                 xscale, yscale, xflip, yflip,
                                 xticks, yticks, xorient, yorient,
                                 xlabel, ylabel,
                                 x2ticks, y2ticks, x2orient, y2orient,
                                 x2label, y2label,
                                 captioner, annotateflags,
                                 gridcolor, axlabelcolor, islabelposition );
    }

    /**
     * Formats a coordinate value for presentation to the user.
     *
     * @param   axis  axis on which value appears
     * @param   dpos  data coordinate value
     * @return   formatted data coordinate string
     */
    public static String formatPosition( Axis axis, double dpos ) {

        /* This could be implemented better.
         * It would be better if the precision determination, and hence
         * the number of digits before and after the decimal point and
         * in the exponent, were determined by the axis (bounds and
         * scaling) rather than by the value.  That would be fairly
         * easy for linear axes, a bit harder for logarithmic. */

        /* Work out pixel size in data coordinates by looking at the
         * data position of a point two pixels away. */
        double dp2 = axis.graphicsToData( axis.dataToGraphics( dpos ) + 2 );
        return PlotUtil.formatNumber( dpos, Math.abs( dp2 - dpos ) / 2. );
    }
}
