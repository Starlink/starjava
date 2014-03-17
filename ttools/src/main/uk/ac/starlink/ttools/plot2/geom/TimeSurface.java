package uk.ac.starlink.ttools.plot2.geom;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.Arrays;
import uk.ac.starlink.ttools.plot2.Axis;
import uk.ac.starlink.ttools.plot2.BasicTicker;
import uk.ac.starlink.ttools.plot2.Captioner;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.Tick;

/**
 * Surface implementation for time-series plotting.
 *
 * @author   Mark Taylor
 * @since    17 Jul 2013
 */
public class TimeSurface implements Surface {

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
    private final String tlabel_;
    private final String ylabel_;
    private final Captioner captioner_;
    private final boolean grid_;
    private final TimeFormat tformat_;
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
     * @param  tlabel  text for labelling time axis
     * @param  ylabel  text for labelling Y axis
     * @param  captioner  text renderer for axis labels etc
     * @param  grid   whether to draw grid lines
     * @param  tformat  time labelling format
     */
    public TimeSurface( int gxlo, int gxhi, int gylo, int gyhi,
                        double dtlo, double dthi, double dylo, double dyhi,
                        boolean ylog, boolean yflip,
                        Tick[] tticks, Tick[] yticks,
                        String tlabel, String ylabel,
                        Captioner captioner, boolean grid,
                        TimeFormat tformat ) {
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
        tlabel_ = tlabel;
        ylabel_ = ylabel;
        captioner_ = captioner;
        grid_ = grid;
        tformat_ = tformat;
        tAxis_ = Axis.createAxis( gxlo_, gxhi_, dtlo_, dthi_, false, false );
        yAxis_ = Axis.createAxis( gylo_, gyhi_, dylo_, dyhi_, ylog_,
                                  yflip_ ^ INVERT_Y );
        assert this.equals( this );
    }

    public Rectangle getPlotBounds() {
        return new Rectangle( gxlo_, gylo_, gxhi_ - gxlo_, gyhi_ - gylo_ );
    }

    public Insets getPlotInsets( boolean withScroll ) {
        return createAxisAnnotation().getPadding( withScroll );
    }

    /**
     * Returns 2.
     */
    public int getDataDimCount() {
        return 2;
    }

    public boolean dataToGraphics( double[] dpos, boolean visibleOnly,
                                   Point gp ) {
        if ( dpos == null ) {
            return false;
        }
        int gx = tAxis_.dataToGraphics( dpos[ 0 ] );
        int gy = yAxis_.dataToGraphics( dpos[ 1 ] );
        if ( visibleOnly &&
             ( gx < gxlo_ || gx >= gxhi_ || gy < gylo_ || gy >= gyhi_ ) ) {
            return false;
        }
        else {
            gp.x = gx;
            gp.y = gy;
            return true;
        }
    }

    public boolean dataToGraphicsOffset( double[] dpos0, Point gpos0,
                                         double[] dpos1, boolean visibleOnly,
                                         Point gpos1 ) {
        return dataToGraphics( dpos1, visibleOnly, gpos1 );
    }

    public double[] graphicsToData( Point gp, Iterable<double[]> dposIt ) {
        return new double[] { tAxis_.graphicsToData( gp.x ),
                              yAxis_.graphicsToData( gp.y ) };
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
        if ( grid_ ) {
            g.setColor( Color.LIGHT_GRAY );
            for ( int it = 0; it < tticks_.length; it++ ) {
                Tick tick = tticks_[ it ];
                if ( tick.getLabel() != null ) {
                    int gx = tAxis_.dataToGraphics( tick.getValue() );
                    g.drawLine( gx, gylo_, gx, gyhi_ );
                }
            }
            for ( int it = 0; it < yticks_.length; it++ ) {
                Tick tick = yticks_[ it ];
                if ( tick.getLabel() != null ) {
                    int gy = yAxis_.dataToGraphics( tick.getValue() );
                    g.drawLine( gxlo_, gy, gxhi_, gy );
                }
            }
        }
        g.setColor( color0 );
    }

    public void paintForeground( Graphics g ) {
        Color color0 = g.getColor();
        g.setColor( Color.BLACK );
        createAxisAnnotation().drawLabels( g );

        /* Boundary. */
        g.drawRect( gxlo_, gylo_, gxhi_ - gxlo_, gyhi_ - gylo_ );

        /* Restore. */
        g.setColor( color0 );
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
    TimeAspect zoom( Point pos, double tZoom, double yZoom ) {
        return new TimeAspect( tAxis_.dataZoom( tAxis_.graphicsToData( pos.x ),
                                                tZoom ),
                               yAxis_.dataZoom( yAxis_.graphicsToData( pos.y ),
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
    TimeAspect pan( Point pos0, Point pos1, boolean tFlag, boolean yFlag ) {
        if ( tFlag || yFlag ) {
            return new TimeAspect(
                tFlag ? tAxis_.dataPan( tAxis_.graphicsToData( pos0.x ),
                                        tAxis_.graphicsToData( pos1.x ) )
                      : new double[] { dtlo_, dthi_ },
                yFlag ? yAxis_.dataPan( yAxis_.graphicsToData( pos0.y ),
                                        yAxis_.graphicsToData( pos1.y ) )
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
        Point gp = new Point();
        return dataToGraphics( dpos, false, gp )
             ? pan( gp, new Point( ( gxlo_ + gxhi_ ) / 2,
                                   ( gylo_ + gyhi_ ) / 2 ),
                    tFlag, yFlag )
             : null;
    }

    /**
     * Returns an axis annotation object for this surface.
     *
     * @return   axis annotation
     */
    private AxisAnnotation createAxisAnnotation() {
        return new PlaneAxisAnnotation( gxlo_, gxhi_, gylo_, gyhi_,
                                        tAxis_, yAxis_, tticks_, yticks_,
                                        tlabel_, ylabel_, captioner_ );
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
        code = 23 * code + PlotUtil.hashCode( tlabel_ );
        code = 23 * code + PlotUtil.hashCode( ylabel_ );
        code = 23 * code + captioner_.hashCode();
        code = 23 * code + ( grid_ ? 11 : 13 );
        code = 23 * code + tformat_.hashCode();
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
                && PlotUtil.equals( this.tlabel_, this.tlabel_ )
                && PlotUtil.equals( this.ylabel_, other.ylabel_ )
                && this.captioner_.equals( other.captioner_ )
                && this.grid_ == other.grid_
                && this.tformat_.equals( other.tformat_ );
        }
        else {
            return false;
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
     * @param  captioner  text renderer for axis labels etc
     * @param  grid   whether to draw grid lines
     * @param  tformat  time labelling format
     * @param  tcrowd   crowding factor for tick marks on time axis;
     *                  1 is normal
     * @param  ycrowd   crowding factor for tick marks on Y axis;
     *                  1 is normal
     * @param  minor   whether to paint minor tick marks on axes
     * @return  new plot surface
     */
    public static TimeSurface createSurface( Rectangle plotBounds,
                                             TimeAspect aspect,
                                             boolean ylog, boolean yflip,
                                             String tlabel, String ylabel,
                                             Captioner captioner, boolean grid,
                                             TimeFormat tformat,
                                             double tcrowd, double ycrowd,
                                             boolean minor ) {
        int gxlo = plotBounds.x;
        int gxhi = plotBounds.x + plotBounds.width;
        int gylo = plotBounds.y;
        int gyhi = plotBounds.y + plotBounds.height;
        double dtlo = aspect.getTMin();
        double dthi = aspect.getTMax();
        double dylo = aspect.getYMin();
        double dyhi = aspect.getYMax();
        Tick[] tticks = tformat.getTicker()
                       .getTicks( aspect.getTMin(), aspect.getTMax(), minor,
                                  captioner, PlaneAxisAnnotation.X_ORIENT,
                                  plotBounds.width, tcrowd );
        Tick[] yticks = ( ylog ? BasicTicker.LOG : BasicTicker.LINEAR )
                       .getTicks( dylo, dyhi, minor, captioner,
                                  PlaneAxisAnnotation.Y_ORIENT,
                                  plotBounds.height, ycrowd );
        return new TimeSurface( gxlo, gxhi, gylo, gyhi, dtlo, dthi, dylo, dyhi,
                                ylog, yflip, tticks, yticks, tlabel, ylabel,
                                captioner, grid, tformat );
    }
}
