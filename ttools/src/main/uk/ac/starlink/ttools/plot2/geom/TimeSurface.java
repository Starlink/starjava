package uk.ac.starlink.ttools.plot2.geom;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Supplier;
import uk.ac.starlink.ttools.plot2.Axis;
import uk.ac.starlink.ttools.plot2.BasicTicker;
import uk.ac.starlink.ttools.plot2.Captioner;
import uk.ac.starlink.ttools.plot2.CoordSequence;
import uk.ac.starlink.ttools.plot2.Orientation;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.Surround;
import uk.ac.starlink.ttools.plot2.Tick;
import uk.ac.starlink.ttools.plot2.TickRun;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;

/**
 * Surface implementation for time-series plotting.
 *
 * @author   Mark Taylor
 * @since    17 Jul 2013
 */
public class TimeSurface implements Surface, PlanarSurface {

    private final int gxlo_;
    private final int gxhi_;
    private final int gylo_;
    private final int gyhi_;
    private final double dtlo_;
    private final double dthi_;
    private final double dylo_;
    private final double dyhi_;
    private final boolean ylog_;
    private final boolean yflip_;
    private final Tick[] tticks_;
    private final Tick[] yticks_;
    private final Orientation torient_;
    private final Orientation yorient_;
    private final String tlabel_;
    private final String ylabel_;
    private final Tick[] t2ticks_;
    private final Tick[] y2ticks_;
    private final Orientation t2orient_;
    private final Orientation y2orient_;
    private final String t2label_;
    private final String y2label_;
    private final Captioner captioner_;
    private final Color gridcolor_;
    private final TimeFormat tformat_;
    private final SideFlags annotateflags_;
    private final Axis tAxis_;
    private final Axis yAxis_;

    private static final boolean INVERT_Y = true;

    /**
     * Constructor.
     *
     * @param  gxlo   graphics X coordinate lower bound
     * @param  gxhi   graphics X coordinate upper bound
     * @param  gylo   graphics Y coordinate lower bound
     * @param  gyhi   graphics Y coordinate upper bound
     * @param  dtlo   data time coordinate lower bound in unix seconds
     * @param  dthi   data time coordinate upper bound in unix seconds
     * @param  dylo   data Y coordinate lower bound
     * @param  dyhi   data Y coordinate upper bound
     * @param  ylog   whether to use logarithmic scaling on Y axis
     * @param  yflip  whether to invert direction of Y axis
     * @param  tticks  array of tickmark objects for time axis
     * @param  yticks  array of tickmark objects for Y axis
     * @param  torient  tick label orientation on time axis
     * @param  yorient  tick label orientation on Y axis
     * @param  tlabel  text for labelling time axis
     * @param  ylabel  text for labelling Y axis
     * @param  t2ticks  array of tickmark objects for secondary time axis
     * @param  y2ticks  array of tickmark objects for secondary Y axis
     * @param  t2orient  tick label orientation on secondary time axis
     * @param  y2orient  tick label orientation on secondary Y axis
     * @param  t2label  text for labelling secondary time axis
     * @param  y2label  text for labelling secondary Y axis
     * @param  captioner  text renderer for axis labels etc
     * @param  gridcolor   grid line colour, or null for none
     * @param  tformat  time labelling format
     * @param  annotateflags  which sides to annotate
     */
    @SuppressWarnings("this-escape")
    public TimeSurface( int gxlo, int gxhi, int gylo, int gyhi,
                        double dtlo, double dthi, double dylo, double dyhi,
                        boolean ylog, boolean yflip,
                        Tick[] tticks, Tick[] yticks,
                        Orientation torient, Orientation yorient,
                        String tlabel, String ylabel,
                        Tick[] t2ticks, Tick[] y2ticks,
                        Orientation t2orient, Orientation y2orient,
                        String t2label, String y2label,
                        Captioner captioner, Color gridcolor,
                        TimeFormat tformat, SideFlags annotateflags ) {
        gxlo_ = gxlo;
        gxhi_ = gxhi;
        gylo_ = gylo;
        gyhi_ = gyhi;
        dtlo_ = dtlo;
        dthi_ = dthi;
        dylo_ = dylo;
        dyhi_ = dyhi;
        ylog_ = ylog;
        yflip_ = yflip;
        tticks_ = tticks;
        yticks_ = yticks;
        torient_ = torient;
        yorient_ = yorient;
        tlabel_ = tlabel;
        ylabel_ = ylabel;
        t2ticks_ = t2ticks;
        y2ticks_ = y2ticks;
        t2orient_ = t2orient;
        y2orient_ = y2orient;
        t2label_ = t2label;
        y2label_ = y2label;
        captioner_ = captioner;
        gridcolor_ = gridcolor;
        tformat_ = tformat;
        annotateflags_ = annotateflags;
        tAxis_ = Axis.createAxis( gxlo_, gxhi_, dtlo_, dthi_, false, false );
        yAxis_ = Axis.createAxis( gylo_, gyhi_, dylo_, dyhi_, ylog_,
                                  yflip_ ^ INVERT_Y );
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
        double gx = tAxis_.dataToGraphics( dpos[ 0 ] );
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
        return new double[] { tAxis_.graphicsToData( gp.getX() ),
                              yAxis_.graphicsToData( gp.getY() ) };
    }

    public boolean isContinuousLine( double[] dpos0, double[] dpos1 ) {
        return true;
    }

    public String formatPosition( double[] dpos ) {
        double timePixelSize = ( dthi_ - dtlo_ ) / ( gxhi_ - gxlo_ );
        return new StringBuilder()
            .append( tformat_.formatTime( dpos[ 0 ], timePixelSize ) )
            .append( ", " )
            .append( PlaneSurface.formatPosition( yAxis_, dpos[ 1 ] ) )
            .toString();
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
            for ( Tick tick : tticks_ ) {
                if ( tick.getLabel() != null ) {
                    int gx = (int) tAxis_.dataToGraphics( tick.getValue() );
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
        g.setColor( Color.BLACK );
        createAxisAnnotation().drawLabels( g );

        /* Boundary. */
        g.drawRect( gxlo_, gylo_, gxhi_ - gxlo_, gyhi_ - gylo_ );

        /* Restore. */
        g.setColor( color0 );
    }

    public Captioner getCaptioner() {
        return captioner_;
    }

    public boolean[] getLogFlags() {
        return new boolean[] { false, ylog_ };
    }

    public boolean[] getFlipFlags() {
        return new boolean[] { false, yflip_ };
    }

    public boolean[] getTimeFlags() {
        return new boolean[] { true, false };
    }

    public double[][] getDataLimits() {
        return new double[][] { { dtlo_, dthi_ }, { dylo_, dyhi_ } };
    }

    public Axis[] getAxes() {
        return new Axis[] { tAxis_, yAxis_ };
    }

    /**
     * Returns approximate config to recreate this surface's aspect.
     *
     * @return   approximate aspect config
     */
    ConfigMap getAspectConfig() {
        ConfigMap config = new ConfigMap();
        double timeEpsilon = 0.1 * ( dthi_ - dtlo_ ) / ( gxhi_ - gxlo_ );
        TimeFormat tfmt = dthi_ - dtlo_ > 365 * 24 * 3600
                                        ? TimeFormat.DECIMAL_YEAR
                                        : TimeFormat.ISO8601;
        config.put( TimeSurfaceFactory.TMIN_KEY,
                    Double.valueOf( roundTime( dtlo_, tfmt, timeEpsilon ) ) );
        config.put( TimeSurfaceFactory.TMAX_KEY,
                    Double.valueOf( roundTime( dthi_, tfmt, timeEpsilon ) ) );
        config.putAll( PlotUtil.configLimits( TimeSurfaceFactory.YMIN_KEY,
                                              TimeSurfaceFactory.YMAX_KEY,
                                              dylo_, dyhi_, gyhi_ - gylo_ ) );
        return config;
    }

    /**
     * Returns a plot aspect representing a view of this surface zoomed
     * in some or all dimensions around the given central position.
     *
     * @param  pos  reference graphics position
     * @param  tZoom  horizontal axis zoom factor
     * @param  yZoom  vertical axis zoom factor
     * @return  new aspect
     */
    TimeAspect zoom( Point2D pos, double tZoom, double yZoom ) {
        return new TimeAspect( tAxis_
                              .dataZoom( tAxis_.graphicsToData( pos.getX() ),
                                         tZoom ),
                               yAxis_
                              .dataZoom( yAxis_.graphicsToData( pos.getY() ),
                                         yZoom ) );
    }

    /**
     * Returns a plot aspect representing a view of this surface panned
     * in some or all dimensions such that the data that used to appear
     * at one graphics coordinate now appears at another.
     *
     * @param   pos0  source graphics position
     * @param   pos1  destination graphics position
     * @param   tFlag  true to pan in horizontal direction
     * @param   yFlag  true to pan in vertical direction
     * @return  new aspect
     */
    TimeAspect pan( Point2D pos0, Point2D pos1, boolean tFlag, boolean yFlag ) {
        if ( tFlag || yFlag ) {
            return new TimeAspect(
                tFlag ? tAxis_.dataPan( tAxis_.graphicsToData( pos0.getX() ),
                                        tAxis_.graphicsToData( pos1.getX() ) )
                      : new double[] { dtlo_, dthi_ },
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
     * @param  tFlag  true to center in horizontal direction
     * @param  yFlag  true to center in vertical direction
     * @return  new aspect
     */
    TimeAspect center( double[] dpos, boolean tFlag, boolean yFlag ) {
        Point2D.Double gp = new Point2D.Double();
        return dataToGraphics( dpos, false, gp ) && PlotUtil.isPointFinite( gp )
             ? pan( gp, new Point2D.Double( ( gxlo_ + gxhi_ ) * 0.5,
                                            ( gylo_ + gyhi_ ) * 0.5 ),
                    tFlag, yFlag )
             : null;
    }

    /**
     * Returns a plot aspect covering the data region which is currently
     * covered by a given rectangle in graphics coordinates.
     *
     * @param   frame  rectangle in current graphics coordinates
     *                 giving data space region of interest
     */ 
    TimeAspect reframe( Rectangle frame ) {
        Point gp1 = new Point( frame.x, frame.y );
        Point gp2 = new Point( frame.x + frame.width, frame.y + frame.height );
        double[] dpos1 = graphicsToData( gp1, null );
        double[] dpos2 = graphicsToData( gp2, null );
        return new TimeAspect( PlotUtil.orderPair( dpos1[ 0 ], dpos2[ 0 ] ),
                               PlotUtil.orderPair( dpos1[ 1 ], dpos2[ 1 ] ) );
    }

    /**
     * Returns an axis annotation object for this surface.
     *
     * @return   axis annotation
     */
    private AxisAnnotation createAxisAnnotation() {
        return new PlaneAxisAnnotation( gxlo_, gxhi_, gylo_, gyhi_,
                                        tAxis_, yAxis_,
                                        tticks_, yticks_, torient_, yorient_,
                                        tlabel_, ylabel_,
                                        t2ticks_, y2ticks_, t2orient_,y2orient_,
                                        t2label_, y2label_,
                                        captioner_, annotateflags_ );
    }

    @Override
    public int hashCode() {
        int code = 943;
        code = 23 * code + gxlo_;
        code = 23 * code + gxhi_;
        code = 23 * code + gylo_;
        code = 23 * code + gyhi_;
        code = 23 * code + Float.floatToIntBits( (float) dtlo_ );
        code = 23 * code + Float.floatToIntBits( (float) dthi_ );
        code = 23 * code + Float.floatToIntBits( (float) dylo_ );
        code = 23 * code + Float.floatToIntBits( (float) dyhi_ );
        code = 23 * code + ( ylog_ ? 1 : 3 );
        code = 23 * code + ( yflip_ ? 5 : 7 );
        code = 23 * code + Arrays.hashCode( tticks_ );
        code = 23 * code + Arrays.hashCode( yticks_ );
        code = 23 * code + PlotUtil.hashCode( torient_ );
        code = 23 * code + PlotUtil.hashCode( yorient_ );
        code = 23 * code + PlotUtil.hashCode( tlabel_ );
        code = 23 * code + PlotUtil.hashCode( ylabel_ );
        code = 23 * code + Arrays.hashCode( t2ticks_ );
        code = 23 * code + Arrays.hashCode( y2ticks_ );
        code = 23 * code + PlotUtil.hashCode( t2orient_ );
        code = 23 * code + PlotUtil.hashCode( y2orient_ );
        code = 23 * code + PlotUtil.hashCode( t2label_ );
        code = 23 * code + PlotUtil.hashCode( y2label_ );
        code = 23 * code + captioner_.hashCode();
        code = 23 * code + PlotUtil.hashCode( gridcolor_ );
        code = 23 * code + tformat_.hashCode();
        code = 23 * code + annotateflags_.hashCode();
        return code;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof TimeSurface ) {
            TimeSurface other = (TimeSurface) o;
            return this.gxlo_ == other.gxlo_
                && this.gxhi_ == other.gxhi_
                && this.gylo_ == other.gylo_
                && this.gyhi_ == other.gyhi_
                && this.dtlo_ == other.dtlo_
                && this.dthi_ == other.dthi_
                && this.dylo_ == other.dylo_
                && this.dyhi_ == other.dyhi_
                && this.ylog_ == other.ylog_
                && this.yflip_ == other.yflip_
                && Arrays.equals( this.tticks_, other.tticks_ )
                && Arrays.equals( this.yticks_, other.yticks_ )
                && PlotUtil.equals( this.torient_, other.torient_ )
                && PlotUtil.equals( this.yorient_, other.yorient_ )
                && PlotUtil.equals( this.tlabel_, this.tlabel_ )
                && PlotUtil.equals( this.ylabel_, other.ylabel_ )
                && Arrays.equals( this.t2ticks_, other.t2ticks_ )
                && Arrays.equals( this.y2ticks_, other.y2ticks_ )
                && PlotUtil.equals( this.t2orient_, other.t2orient_ )
                && PlotUtil.equals( this.y2orient_, other.y2orient_ )
                && PlotUtil.equals( this.t2label_, other.t2label_ )
                && PlotUtil.equals( this.y2label_, other.y2label_ )
                && this.captioner_.equals( other.captioner_ )
                && PlotUtil.equals( this.gridcolor_, other.gridcolor_ )
                && this.tformat_.equals( other.tformat_ )
                && this.annotateflags_.equals( other.annotateflags_ );
        }
        else {
            return false;
        }
    }

    /**
     * Attempts format-sensitive rounding of a time value to a given level
     * of precision.
     *
     * @param  tval   time value in unix seconds
     * @param  tfmt   time format in which the value is to look rounded
     * @param  tepsilon   small value corresponding roughly to rounding amount
     * @return  rounded value near tval
     */
    private static double roundTime( double tval, TimeFormat tfmt,
                                     double tepsilon ) {
        try {
            return tfmt.parseTime( tfmt.formatTime( tval, tepsilon ) );
        }
        catch ( RuntimeException e ) {
            return tval;
        }
    }

    /**
     * Utility method to create a TimeSurface from available requirements.
     * It works out actual data coordinate bounds and tickmarks, and
     * then invokes the constructor.
     *
     * @param  plotBounds  rectangle which the plot data should occupy
     * @param  aspect  surface view configuration
     * @param  ylog   whether to use logarithmic scaling on Y axis
     * @param  yflip  whether to invert direction of Y axis
     * @param  tlabel  text for labelling time axis
     * @param  ylabel  text for labelling Y axis
     * @param  t2func  function mapping unix seconds to secondary time axis
     *                 data coord, null for no secondary time axis
     * @param  y2func  function mapping primary to secondary Y axis data coord,
     *                 null for no secondary Y axis
     * @param  t2label  secondary time axis label
     * @param  y2label  secondary Y axis label
     * @param  captioner  text renderer for axis labels etc
     * @param  gridcolor   colour of grid lines, or null for none
     * @param  tformat  time labelling format
     * @param  tcrowd   crowding factor for tick marks on time axis;
     *                  1 is normal
     * @param  ycrowd   crowding factor for tick marks on Y axis;
     *                  1 is normal
     * @param  orientpolicy   tick label orientation policy
     * @param  minor   whether to paint minor tick marks on axes
     * @param  shadow  whether to paint shadow ticks on opposite axes
     *                 if no secondary axis
     * @param  annotateflags  which axes to annotate
     * @return  new plot surface
     */
    public static TimeSurface createSurface( Rectangle plotBounds,
                                             TimeAspect aspect,
                                             boolean ylog, boolean yflip,
                                             String tlabel, String ylabel,
                                             DoubleUnaryOperator t2func,
                                             DoubleUnaryOperator y2func,
                                             String t2label, String y2label,
                                             Captioner captioner,
                                             Color gridcolor,
                                             TimeFormat tformat,
                                             double tcrowd, double ycrowd,
                                             OrientationPolicy orientpolicy,
                                             boolean minor, boolean shadow,
                                             SideFlags annotateflags ) {
        int gxlo = plotBounds.x;
        int gxhi = plotBounds.x + plotBounds.width;
        int gylo = plotBounds.y;
        int gyhi = plotBounds.y + plotBounds.height;
        double dtlo = aspect.getTMin();
        double dthi = aspect.getTMax();
        double dylo = aspect.getYMin();
        double dyhi = aspect.getYMax();
        TickRun ttickRun = tformat.getTicker()
                          .getTicks( aspect.getTMin(), aspect.getTMax(), minor,
                                     captioner, orientpolicy.getOrientationsX(),
                                     plotBounds.width, tcrowd );
        TickRun ytickRun = ( ylog ? BasicTicker.LOG : BasicTicker.LINEAR )
                          .getTicks( dylo, dyhi, minor,
                                     captioner, orientpolicy.getOrientationsY(),
                                     plotBounds.height, ycrowd );
        Tick[] tticks = ttickRun.getTicks();
        Tick[] yticks = ytickRun.getTicks();
        Orientation torient = ttickRun.getOrientation();
        Orientation yorient = ytickRun.getOrientation();
        Axis tAxis = Axis.createAxis( gxlo, gxhi, dtlo, dthi, false, false );
        Axis yAxis = Axis.createAxis( gylo, gyhi, dylo, dyhi, ylog,
                                      yflip ^ PlaneAxisAnnotation.INVERT_Y );
        TickRun t2tickRun =
              t2func == null
            ? ( shadow ? new TickRun( PlotUtil.getShadowTicks( tticks ),
                                      Orientation.ANTI_X )
                       : null )
            : new SlaveTicker( tAxis, t2func, BasicTicker.LINEAR )
                 .getTicks( aspect.getTMin(), aspect.getTMax(), minor,
                            captioner, orientpolicy.getOrientationsX2(),
                            plotBounds.width, tcrowd );
        TickRun y2tickRun =
              y2func == null
            ? ( shadow ? new TickRun( PlotUtil.getShadowTicks( yticks ),
                                      Orientation.ANTI_Y )
                       : null )
            : SlaveTicker.createTicker( yAxis, y2func )
                         .getTicks( dylo, dyhi, minor, captioner,
                                    orientpolicy.getOrientationsY2(),
                                    plotBounds.height, ycrowd );
        Tick[] t2ticks = t2tickRun == null ? null : t2tickRun.getTicks();
        Tick[] y2ticks = y2tickRun == null ? null : y2tickRun.getTicks();
        Orientation t2orient = t2tickRun == null
                             ? null
                             : t2tickRun.getOrientation();
        Orientation y2orient = y2tickRun == null
                             ? null
                             : y2tickRun.getOrientation();
        return new TimeSurface( gxlo, gxhi, gylo, gyhi, dtlo, dthi, dylo, dyhi,
                                ylog, yflip,
                                tticks, yticks, torient, yorient,
                                tlabel, ylabel,
                                t2ticks, y2ticks, t2orient, y2orient,
                                t2label, y2label,
                                captioner, gridcolor, tformat, annotateflags );
    }
}
