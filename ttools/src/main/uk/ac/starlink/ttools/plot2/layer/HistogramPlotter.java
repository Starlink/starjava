package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.util.Map;
import javax.swing.Icon;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Decal;
import uk.ac.starlink.ttools.plot2.Drawing;
import uk.ac.starlink.ttools.plot2.LayerOpt;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;
import uk.ac.starlink.ttools.plot2.geom.PlaneSurface;
import uk.ac.starlink.ttools.plot2.geom.SliceDataGeom;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType;

/**
 * Plotter for 1-dimensional histograms.
 * This only works on plane plots.
 *
 * @author   Mark Taylor
 * @since    8 Jan 2014
 */
public class HistogramPlotter
        implements Plotter<HistogramPlotter.HistoStyle> {

    private final FloatingCoord xCoord_;
    private final SliceDataGeom histoDataGeom_;

    private static final AuxScale COUNT_SCALE = new AuxScale( "Count" );

    /**
     * Constructor.
     *
     * @param   xCoord  X axis coordinate
     */
    public HistogramPlotter( FloatingCoord xCoord ) {
        xCoord_ = xCoord;
        histoDataGeom_ =
            new SliceDataGeom( new FloatingCoord[] { xCoord_, null }, "X" );
    }

    public String getPlotterName() {
        return "Histogram";
    }

    public Icon getPlotterIcon() {
        return ResourceIcon.PLOT_HISTO;
    }

    /**
     * Returns false, since rows do not correspond to a point-like position.
     */
    public int getPositionCount() {
        return 0;
    }

    public Coord[] getExtraCoords() {
        return new Coord[] {
            xCoord_,
        };
    }

    public ConfigKey[] getStyleKeys() {
        return new ConfigKey[] {
            StyleKeys.COLOR,
        };
    }

    public HistoStyle createStyle( ConfigMap config ) {
        Color color = config.get( StyleKeys.COLOR );
        BinSizer sizer = BinSizer.createCountBinSizer( 20 );
        double binBase = 0;
        return new HistoStyle( color, sizer, binBase );
    }

    /**
     * The supplied <code>geom</code> is ignored.
     */
    public PlotLayer createLayer( DataGeom geom, final DataSpec dataSpec,
                                  final HistoStyle style ) {
        if ( dataSpec == null || style == null ) {
            return null;
        }
        else {
            final double binBase = style.base_;
            final BinSizer sizer = style.sizer_;
            Color color = style.color_;
            final boolean isOpaque = color.getAlpha() == 255;
            LayerOpt layerOpt = new LayerOpt( color, isOpaque );
            return new AbstractPlotLayer( this, histoDataGeom_, dataSpec,
                                          style, layerOpt ) {
                public Drawing createDrawing( Surface surface,
                                              Map<AuxScale,Range> auxRanges,
                                              final PaperType paperType ) {
                    if ( ! ( surface instanceof PlaneSurface ) ) {
                        throw new IllegalArgumentException( "Not plane surface"
                                                          + surface );
                    }
                    final PlaneSurface pSurf = (PlaneSurface) surface;
                    final boolean xlog = pSurf.getLogFlags()[ 0 ];
                    double[] xlimits = pSurf.getDataLimits()[ 0 ];
                    final double xlo = xlimits[ 0 ];
                    final double xhi = xlimits[ 1 ];
                    final double binWidth = sizer.getWidth( xlog, xlo, xhi );
                    return new Drawing() {
                        public Object calculatePlan( Object[] knownPlans,
                                                     DataStore dataStore ) {
                            for ( int ip = 0; ip < knownPlans.length; ip++ ) {
                                if ( knownPlans[ ip ] instanceof HistoPlan ) {
                                    HistoPlan plan =
                                        (HistoPlan) knownPlans[ ip ];
                                    if ( plan.matches( xlog, binWidth,
                                                       binBase, dataSpec ) ) {
                                        return plan;
                                    }
                                }
                            }
                            BinBag binBag =
                                readBins( xlog, binWidth, binBase, xlo,
                                          dataSpec, dataStore );
                            return new HistoPlan( binBag, dataSpec );
                        }
                        public void paintData( Object plan, Paper paper,
                                               DataStore dataStore ) {
                            HistoPlan hPlan = (HistoPlan) plan;
                            final BinBag binBag = hPlan.binBag_;
                            paperType.placeDecal( paper, new Decal() {
                                public void paintDecal( Graphics g ) {
                                    paintBins( pSurf, binBag, style, g );
                                }
                                public boolean isOpaque() {
                                    return isOpaque;
                                }
                            } );
                        }
                    };
                }

                /* Override this method so that we can indicate to the plot
                 * the height of the bars for auto-ranging purposes. */
                @Override
                public void extendCoordinateRanges( Range[] ranges,
                                                    boolean[] logFlags,
                                                    DataStore dataStore ) {
                    Range xRange = ranges[ 0 ];
                    Range yRange = ranges[ 1 ];
                    boolean xlog = logFlags[ 0 ];

                    /* The range in X will have been already calculated on
                     * the basis of the X values in this and any other layers
                     * by earlier stages of the auto-ranging process.
                     * We have to use this to get the bin sizes which will
                     * in turn determine the heights of the histogram bars. */
                    double[] xlimits = xRange.getFiniteBounds( xlog );
                    double xlo = xlimits[ 0 ];
                    double xhi = xlimits[ 1 ];
                    double binWidth = sizer.getWidth( xlog, xlo, xhi );
                    BinBag binBag = readBins( xlog, binWidth, binBase, xlo,
                                              dataSpec, dataStore );

                    /* Assume y=0 is always of interest for a histogram. */
                    yRange.submit( 0 );

                    /* For each non-empty bar, note its Y value
                     * (top of the bar).
                     * We also note the X min/max values - although the X
                     * range is mostly correct already, this ensures that
                     * each bin is visible in its entirety rather than
                     * cut off in the middle.  The fact that this resets the
                     * X range in turn means that the Y ranging may no longer
                     * be exactly right, but it won't be far off. */
                    for ( BinBag.Bin bin : binBag.getBins() ) {
                        double y = bin.getY();
                        if ( y != 0 ) {
                            yRange.submit( y );
                            xRange.submit( bin.getXMin() );
                            xRange.submit( bin.getXMax() );
                        }
                    }
                }
            };
        }
    }

    /**
     * Reads histogram data from a given data set.
     *
     * @param   xlog  false for linear scaling, true for logarithmic
     * @param   binWidth  additive/multiplicative bin width
     * @param   binBase   zero point for bin zero
     * @param   point     representative data value along axis
     * @param   dataSpec  specification for histogram data values
     * @param   dataStore  data storage
     */
    private BinBag readBins( boolean xlog, double binWidth, double binBase,
                             double point, DataSpec dataSpec,
                             DataStore dataStore ) {
        BinBag binBag = new BinBag( xlog, binWidth, binBase, point );
        TupleSequence tseq = dataStore.getTupleSequence( dataSpec );
        while ( tseq.next() ) {
            double x = xCoord_.readDoubleCoord( tseq, 0 );
            binBag.addToBin( x, 1 );
        }
        return binBag;
    }

    /**
     * Does the actual painting of the histogram to a graphics context
     * given the set of bins.
     *
     * @param  surface  plot surface
     * @param  binBag   calculated histogram data
     * @param  style    style
     * @param  g        graphics context
     */
    private void paintBins( PlaneSurface surface, BinBag binBag,
                            HistoStyle style, Graphics g ) {
        Color color0 = g.getColor();
        g.setColor( style.color_ );
        double[][] dataLimits = surface.getDataLimits();
        double dxlo = dataLimits[ 0 ][ 0 ];
        double dxhi = dataLimits[ 0 ][ 1 ];
        double dylo = dataLimits[ 1 ][ 0 ];
        double dyhi = dataLimits[ 1 ][ 1 ];
        Point p0 = new Point();
        Point p1 = new Point();
        for ( BinBag.Bin bin : binBag.getBins() ) {
            double xmin = bin.getXMin();
            double xmax = bin.getXMax();
            double y = bin.getY();
            if ( xmin <= dxhi && xmax >= dxlo ) {
                double ymin = 0;
                double ymax = 0;
                if ( y > 0 ) {
                    ymax = y;
                }
                else if ( y < 0 ) {
                    ymin = y;
                }
                ymin = Math.max( ymin, dylo );
                ymax = Math.min( ymax, dyhi );
                if ( ! ( ymin == ymax ) &&
                     surface.dataToGraphics( new double[] { xmin, ymin },
                                             false, p0 ) &&
                     surface.dataToGraphics( new double[] { xmax, ymax },
                                             false, p1 ) ) {
                    int rx = Math.min( p0.x, p1.x );
                    int ry = Math.min( p0.y, p1.y );
                    int rw = Math.abs( p1.x - p0.x );
                    int rh = Math.abs( p1.y - p0.y );

                    /* Ensure that there is at least one pixel for non-zero
                     * data extents, rather than rounding down to zero. */
                    if ( rw == 0 ) {
                        rw += 1;
                    }
                    if ( rh == 0 ) {
                        ry -= 1;
                        rh += 1;
                    }
                    g.fillRect( rx, ry, rw, rh );
                }
            }
        }
        g.setColor( color0 );
    }

    /**
     * Style subclass for histogram plots.
     */
    public static class HistoStyle implements Style {
        private final Color color_;
        private final BinSizer sizer_;
        private final double base_;

        /**
         * Constructor.
         *
         * @param  color   bar colour
         * @param  sizer   determines bin widths
         * @param  base    bin reference point
         */
        public HistoStyle( Color color, BinSizer sizer, double base ) {
            color_ = color;
            sizer_ = sizer;
            base_ = base;
        }

        public Icon getLegendIcon() {
            return ResourceIcon.PLOT_HISTO;
        }

        @Override
        public int hashCode() {
            int code = 55012;
            code = 23 * code + color_.hashCode();
            code = 23 * code + sizer_.hashCode();
            code = 23 * code + Float.floatToIntBits( (float) base_ );
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof HistoStyle ) {
                HistoStyle other = (HistoStyle) o;
                return this.color_.equals( other.color_ )
                    && this.sizer_.equals( other.sizer_ )
                    && this.base_ == other.base_;
            }
            else {
                return false;
            }
        }
    }

    /**
     * Plan object used for histogram plots.
     */
    private static class HistoPlan {
        final BinBag binBag_;
        final DataSpec dataSpec_;

        /**
         * Constructor.
         *
         * @param  binBag  calculated bin data
         * @param  dataSpec  defines source of data data
         */
        HistoPlan( BinBag binBag, DataSpec dataSpec ) {
            binBag_ = binBag;
            dataSpec_ = dataSpec;
        }

        /**
         * Indicates whether this plan will have the same content as one
         * constructed with given parameters.
         *
         * @param   xlog   axis scaling
         * @param   binWidth   bin width
         * @param   binBase    bin reference position
         * @param   dataSpec   source of coordinate data
         */
        boolean matches( boolean xlog, double binWidth, double binBase,
                         DataSpec dataSpec ) {
            return binBag_.matches( xlog, binWidth, binBase )
                && dataSpec_.equals( dataSpec );
        }
    }
}
