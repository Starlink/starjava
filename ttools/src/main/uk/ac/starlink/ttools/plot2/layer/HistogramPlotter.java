package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import javax.swing.Icon;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot.BarStyle;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Decal;
import uk.ac.starlink.ttools.plot2.Drawing;
import uk.ac.starlink.ttools.plot2.LayerOpt;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.ReportKey;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.ReportMeta;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.BooleanConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.DoubleConfigKey;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.CoordGroup;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.ttools.plot2.data.InputMeta;
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
    private final FloatingCoord weightCoord_;
    private final SliceDataGeom histoDataGeom_;
    private final CoordGroup histoCoordGrp_;
    private final int icX_;
    private final int icWeight_;

    /** ReportKey for histogram bins. */
    public static final ReportKey<BinBag> BINS_KEY =
        new ReportKey<BinBag>( new ReportMeta( "bins", "Bins" ), BinBag.class,
                               false );

    /** Config key for bar line thickness. */
    public static final ConfigKey<Integer> THICK_KEY =
        StyleKeys.createThicknessKey( 2 );

    /** Config key for bar phase. */
    public static final ConfigKey<Double> PHASE_KEY =
        DoubleConfigKey.createSliderKey(
            new ConfigMeta( "phase", "Bin Phase" )
           .setShortDescription( "Horizontal zero point" )
           .setXmlDescription( new String[] {
                "<p>Controls where the horizontal zero point for binning",
                "is set.",
                "For instance if your bin size is 1,",
                "this value controls whether bin boundaries are at",
                "0, 1, 2, .. or 0.5, 1.5, 2.5, ... etc.",
                "</p>",
                "<p>A value of 0 (or any integer) will result in",
                "a bin boundary at X=0 (linear X axis)",
                "or X=1 (logarithmic X axis).",
                "A fractional value will give a bin boundary at",
                "that value multiplied by the bin width.",
                "</p>",
            } )
        , 0, 0, 1, false );

    /** Config key for cumulative histogram flag. */
    public static final ConfigKey<Boolean> CUMULATIVE_KEY =
        new BooleanConfigKey(
            new ConfigMeta( "cumulative", "Cumulative" )
           .setShortDescription( "Cumulative histogram?" )
           .setXmlDescription( new String[] {
                "<p>If true, the histogram bars plotted are calculated",
                "cumulatively;",
                "each bin includes the counts from all previous bins.",
                "</p>",
            } )
        );

    /** Config key for normalised histogram flag. */
    public static final ConfigKey<Boolean> NORM_KEY =
        new BooleanConfigKey(
            new ConfigMeta( "norm", "Normalised" )
           .setShortDescription( "Normalised histogram?" )
           .setXmlDescription( new String[] {
                "<p>If true, the counts in each plotted histogram are",
                "normalised so that the sum of all bars is 1.",
                "</p>",
            } )
        );

    /**
     * Constructor.
     *
     * @param   xCoord  X axis coordinate
     * @param   hasWeight   true to permit histogram weighting
     */
    public HistogramPlotter( FloatingCoord xCoord, boolean hasWeight ) {
        xCoord_ = xCoord;
        if ( hasWeight ) {
            weightCoord_ =
                FloatingCoord.createCoord(
                    new InputMeta( "weight", "Weight" )
                   .setShortDescription( "Non-unit weighting of data points" )
                   .setXmlDescription( new String[] {
                        "<p>Weighting of data points.",
                        "If supplied, each point contributes a value",
                        "to the histogram equal to the data value",
                        "multiplied by this coordinate.",
                        "If not supplied, the effect is the same as",
                        "supplying a fixed value of one.",
                        "</p>",
                    } )
                , false );
            histoCoordGrp_ =
                CoordGroup
               .createPartialCoordGroup( new Coord[] { xCoord, weightCoord_ },
                                         new boolean[] { true, true } );
        }
        else {
            weightCoord_ = null;
            histoCoordGrp_ =
               CoordGroup
              .createPartialCoordGroup( new Coord[] { xCoord },
                                        new boolean[] { true } );
        }
        histoDataGeom_ =
            new SliceDataGeom( new FloatingCoord[] { xCoord_, null }, "X" );

        /* For this plot type, coordinate indices are not sensitive to
         * plot-time geom (the CoordGroup has no point positions),
         * so we can calculate them here. */
        icX_ = histoCoordGrp_.getExtraCoordIndex( 0, null );
        icWeight_ = hasWeight
                  ? histoCoordGrp_.getExtraCoordIndex( 1, null )
                  : -1;
    }

    public String getPlotterName() {
        return "Histogram";
    }

    public Icon getPlotterIcon() {
        return ResourceIcon.PLOT_HISTO;
    }

    public String getPlotterDescription() {
        return "<p>Plots a histogram.</p>";
    }

    public CoordGroup getCoordGroup() {
        return histoCoordGrp_;
    }

    public ConfigKey[] getStyleKeys() {
        return new ConfigKey[] {
            StyleKeys.COLOR,
            StyleKeys.TRANSPARENCY,
            BinSizer.BINSIZER_KEY,
            CUMULATIVE_KEY,
            NORM_KEY,
            PHASE_KEY,
            StyleKeys.BAR_FORM,
            THICK_KEY,
            StyleKeys.DASH,
        };
    }

    public HistoStyle createStyle( ConfigMap config ) {
        Color baseColor = config.get( StyleKeys.COLOR );
        double alpha = 1 - config.get( StyleKeys.TRANSPARENCY );
        float[] rgba = baseColor.getRGBComponents( new float[ 4 ] );
        rgba[ 3 ] *= alpha;
        Color color = new Color( rgba[ 0 ], rgba[ 1 ], rgba[ 2 ], rgba[ 3 ] );
        BarStyle.Form barForm = config.get( StyleKeys.BAR_FORM );
        BarStyle.Placement placement = BarStyle.PLACE_OVER;
        boolean cumulative = config.get( CUMULATIVE_KEY );
        boolean norm = config.get( NORM_KEY );
        int thick = config.get( THICK_KEY );
        float[] dash = config.get( StyleKeys.DASH );
        BinSizer sizer = config.get( BinSizer.BINSIZER_KEY );
        double binPhase = config.get( PHASE_KEY );
        return new HistoStyle( color, barForm, placement, cumulative, norm,
                               thick, dash, sizer, binPhase );
    }

    public boolean hasReports() {
        return false;
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
            final double binPhase = style.phase_;
            final BinSizer sizer = style.sizer_;
            final boolean cumul = style.cumulative_;
            final boolean norm = style.norm_;
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

                    /* We can't work out what other histogram data sets are
                     * being plotted or where this one fits in - that level
                     * of inter-layer communication is not provided by the
                     * plotting framework.  Would take some effort to add it.
                     * For now, just treat each histogram layer as if it's
                     * the only one. */
                    final int iseq = 0;
                    final int nseq = 1;
                    return new Drawing() {
                        public Object calculatePlan( Object[] knownPlans,
                                                     DataStore dataStore ) {
                            for ( int ip = 0; ip < knownPlans.length; ip++ ) {
                                if ( knownPlans[ ip ] instanceof HistoPlan ) {
                                    HistoPlan plan =
                                        (HistoPlan) knownPlans[ ip ];
                                    if ( plan.matches( xlog, binWidth,
                                                       binPhase, dataSpec ) ) {
                                        return plan;
                                    }
                                }
                            }
                            BinBag binBag =
                                readBins( xlog, binWidth, binPhase, xlo,
                                          dataSpec, dataStore );
                            return new HistoPlan( binBag, dataSpec );
                        }
                        public void paintData( Object plan, Paper paper,
                                               DataStore dataStore ) {
                            HistoPlan hPlan = (HistoPlan) plan;
                            final BinBag binBag = hPlan.binBag_;
                            paperType.placeDecal( paper, new Decal() {
                                public void paintDecal( Graphics g ) {
                                    paintBins( pSurf, binBag, style,
                                               iseq, nseq, g );
                                }
                                public boolean isOpaque() {
                                    return isOpaque;
                                }
                            } );
                        }
                        public ReportMap getReport( Object plan ) {
                            ReportMap report = new ReportMap();
                            if ( plan instanceof HistoPlan ) {
                                report.set( BINS_KEY,
                                            ((HistoPlan) plan).binBag_ );
                            }
                            return report;
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
                    BinBag binBag = readBins( xlog, binWidth, binPhase, xlo,
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
                    for ( Iterator<BinBag.Bin> it =
                              binBag.binIterator( cumul, norm );
                          it.hasNext(); ) {
                        BinBag.Bin bin = it.next();
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
     * Returns the DataSpec coord index used for the weighting data
     * for this plotter.  If weighting is not supported, a negative
     * value is returned.
     *
     * @return   weight coord index, or -1
     */
    public int getWeightCoordIndex() {
        return icWeight_;
    }

    /**
     * Reads histogram data from a given data set.
     *
     * @param   xlog  false for linear scaling, true for logarithmic
     * @param   binWidth  additive/multiplicative bin width
     * @param   binPhase   bin reference point, 0..1
     * @param   point     representative data value along axis
     * @param   dataSpec  specification for histogram data values
     * @param   dataStore  data storage
     */
    private BinBag readBins( boolean xlog, double binWidth, double binPhase,
                             double point, DataSpec dataSpec,
                             DataStore dataStore ) {
        BinBag binBag = new BinBag( xlog, binWidth, binPhase, point );
        TupleSequence tseq = dataStore.getTupleSequence( dataSpec );
        if ( weightCoord_ == null || dataSpec.isCoordBlank( icWeight_ ) ) {
            while ( tseq.next() ) {
                double x = xCoord_.readDoubleCoord( tseq, icX_ );
                binBag.addToBin( x, 1 );
            }
        }
        else {
            while ( tseq.next() ) {
                double x = xCoord_.readDoubleCoord( tseq, icX_ );
                double w = weightCoord_.readDoubleCoord( tseq, icWeight_ );
                double weight = Double.isNaN( w ) ? 0 : w;
                binBag.addToBin( x, weight );
            }
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
     * @param  iseq     index of this histogram in the plot
     * @param  nseq     total number of histograms in the plot
     * @param  g        graphics context
     */
    private void paintBins( PlaneSurface surface, BinBag binBag,
                            HistoStyle style, int iseq, int nseq, Graphics g ) {
        Color color0 = g.getColor();
        g.setColor( style.color_ );
        BarStyle barStyle = style.barStyle_;
        boolean cumul = style.cumulative_;
        boolean norm = style.norm_;
        Rectangle clip = surface.getPlotBounds();
        int xClipMin = clip.x - 64;
        int xClipMax = clip.x + clip.width + 64;
        int yClipMin = clip.y - 64;
        int yClipMax = clip.y + clip.height + 64;
        double[][] dataLimits = surface.getDataLimits();
        double dxMin = dataLimits[ 0 ][ 0 ];
        double dxMax = dataLimits[ 0 ][ 1 ];
        double dyMin = dataLimits[ 1 ][ 0 ];
        double dyMax = dataLimits[ 1 ][ 1 ];
        boolean[] flipFlags = surface.getFlipFlags();
        final boolean xflip = flipFlags[ 0 ];
        final boolean yflip = flipFlags[ 1 ];
        boolean ylog = surface.getLogFlags()[ 1 ];
       
        Point2D.Double p0 = new Point2D.Double();
        Point2D.Double p1 = new Point2D.Double();
        double[] dpos0 = new double[ 2 ];
        double[] dpos1 = new double[ 2 ];
        int lastGx1 = xflip ? Integer.MAX_VALUE : Integer.MIN_VALUE;
        int lastGy1 = 0;
        int commonGy0 = 0;

        /* Iterate over bins, plotting each one individually. */
        for ( Iterator<BinBag.Bin> binIt = binBag.binIterator( cumul, norm );
              binIt.hasNext(); ) {
            BinBag.Bin bin = binIt.next();

            /* Get bin data. */
            double dxlo = bin.getXMin();
            double dxhi = bin.getXMax();
            double dy = bin.getY();

            /* Only plot those bins that fall at least partly in the X range. */
            if ( dxlo <= dxMax && dxhi >= dxMin && dy != 0 ) {

                 /* Transform the corners of each bar to graphics coords. */
                 dpos0[ 0 ] = dxlo;
                 dpos0[ 1 ] = ylog ? Double.MIN_VALUE : 0;
                 dpos1[ 0 ] = dxhi;
                 dpos1[ 1 ] = dy;
                 if ( surface.dataToGraphics( dpos0, false, p0 ) &&
                      surface.dataToGraphics( dpos1, false, p1 ) ) {

                    /* Clip them so they are not too far off the plot region;
                     * attempting to draw ridiculously large rectangles can
                     * give AWT a headache. */
                    int gx0 = clip( (int) p0.x, xClipMin, xClipMax );
                    int gx1 = clip( (int) p1.x, xClipMin, xClipMax );
                    int gy0 = clip( (int) p0.y, yClipMin, yClipMax );
                    int gy1 = clip( (int) p1.y, yClipMin, yClipMax );

                    /* Draw the trailing edge of the previous bar if
                     * necessary. */
                    if ( lastGx1 != gx0 ) {
                        barStyle.drawEdge( g, lastGx1, lastGy1, gy0,
                                           iseq, nseq );
                        lastGy1 = gy0;
                    }

                    /* Draw the leading edge of the current bar. */
                    barStyle.drawEdge( g, gx0, lastGy1, gy1, iseq, nseq );
                    lastGx1 = gx1;
                    lastGy1 = gy1;
                    commonGy0 = gy0;

                    /* Draw the bar. */
                    int gxlo = xflip ? gx1 : gx0;
                    int gxhi = xflip ? gx0 : gx1;
                    drawGeneralBar( barStyle, g, gxlo, gxhi, gy0, gy1,
                                    iseq, nseq );
                }
            }
        }

        /* Draw the trailing edge of the final bar. */
        barStyle.drawEdge( g, lastGx1, lastGy1, commonGy0, iseq, nseq );
        g.setColor( color0 );
    }

    /**
     * Clips a value to a given range.
     *
     * @param  p  input value
     * @param  lo  minimum acceptable value
     * @param  hi  maximum acceptable value
     * @return   input value clipped to given limits
     */
    private static int clip( int p, int lo, int hi ) {
        return Math.max( Math.min( p, hi ), lo );
    }

    /**
     * Draws a bar which may point upwards or downwards in a histogram.
     *
     * @param   g  graphics context
     * @param   xlo  lower bound in X direction
     * @param   xhi  upper bound in X direction
     * @param   y0   Y coordinate of the base of the bar
     * @param   y1   Y coordinate of the data level of the bar
     * @param   iseq  index of the set being plotted
     * @param   nseq  number of sets being plotted for this bar
     */
    private void drawGeneralBar( BarStyle barStyle, Graphics g,
                                 int xlo, int xhi, int y0, int y1,
                                 int iseq, int nseq ) {

        /* The work is done by the BarStyle.drawBar method.
         * However, that method can only draw bars with their base
         * facing down and the data level facing up.  If we need to
         * do it the other way round (Y axis inverted) do it by
         * applying a coordinate transformation to the graphics context. */
        if ( y0 >= y1 ) {
            barStyle.drawBar( g, xlo, xhi, y1, y0, iseq, nseq );
        }
        else {
            Graphics2D g2 = (Graphics2D) g;
            AffineTransform trans0 = g2.getTransform();
            g2.translate( 0, y0 + y1 );
            g2.scale( 1, -1 );
            barStyle.drawBar( g, xlo, xhi, y0, y1, iseq, nseq );
            g2.setTransform( trans0 );
        }
    }

    /**
     * Style subclass for histogram plots.
     */
    public static class HistoStyle implements Style {
        private final Color color_;
        private final BarStyle.Form barForm_;
        private final BarStyle.Placement placement_;
        private final boolean cumulative_;
        private final boolean norm_;
        private final int thick_;
        private final float[] dash_;
        private final BinSizer sizer_;
        private final double phase_;

        private final BarStyle barStyle_;

        /**
         * Constructor.
         *
         * @param  color   bar colour
         * @param  barForm  bar form
         * @param  placement  bar placement
         * @param  cumulative  whether to plot cumulative bars
         * @param  norm    whether to normalise vertical scale
         * @param  thick   line thickness (only relevant for some forms)
         * @param  dash    line dash pattern (only relevant for some forms)
         * @param  sizer   determines bin widths
         * @param  phase   bin reference point, 0..1
         */
        public HistoStyle( Color color, BarStyle.Form barForm,
                           BarStyle.Placement placement,
                           boolean cumulative, boolean norm,
                           int thick, float[] dash,
                           BinSizer sizer, double phase ) {
            color_ = color;
            barForm_ = barForm;
            placement_ = placement;
            cumulative_ = cumulative;
            norm_ = norm;
            thick_ = thick;
            dash_ = dash;
            sizer_ = sizer;
            phase_ = phase;
            barStyle_ = new BarStyle( color, barForm, placement );
            barStyle_.setLineWidth( thick );
            barStyle_.setDash( dash );
        }

        /**
         * Returns the bin sizer.
         *
         * @return   bin sizer
         */
        public BinSizer getBinSizer() {
            return sizer_;
        }

        /**
         * Returns cumulative flag.
         *
         * @return  true iff counts are cumulative
         */
        public boolean isCumulative() {
            return cumulative_;
        }

        /**
         * Returns normalised flag.
         *
         * @return  true iff counts are normalised
         */
        public boolean isNormalised() {
            return norm_;
        }

        public Icon getLegendIcon() {
            return barStyle_;
        }

        @Override
        public int hashCode() {
            int code = 55012;
            code = 23 * code + color_.hashCode();
            code = 23 * code + barForm_.hashCode();
            code = 23 * code + placement_.hashCode();
            code = 23 * code + ( cumulative_ ? 11 : 13 );
            code = 23 * code + ( norm_ ? 17 : 19 );
            code = 23 * code + thick_;
            code = 23 * code + Arrays.hashCode( dash_ );
            code = 23 * code + sizer_.hashCode();
            code = 23 * code + Float.floatToIntBits( (float) phase_ );
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof HistoStyle ) {
                HistoStyle other = (HistoStyle) o;
                return this.color_.equals( other.color_ )
                    && this.barForm_.equals( other.barForm_ )
                    && this.placement_.equals( other.placement_ )
                    && this.cumulative_ == other.cumulative_
                    && this.norm_ == other.norm_
                    && this.thick_ == other.thick_
                    && Arrays.equals( this.dash_, other.dash_ )
                    && this.sizer_.equals( other.sizer_ )
                    && this.phase_ == other.phase_;
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
         * @param   binPhase    bin reference point, 0..1
         * @param   dataSpec   source of coordinate data
         */
        boolean matches( boolean xlog, double binWidth, double binPhase,
                         DataSpec dataSpec ) {
            return binBag_.matches( xlog, binWidth, binPhase )
                && dataSpec_.equals( dataSpec );
        }
    }
}
