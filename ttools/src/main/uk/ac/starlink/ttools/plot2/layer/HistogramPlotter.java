package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.Icon;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.IteratorRowSequence;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
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
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.ReportKey;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.ReportMeta;
import uk.ac.starlink.ttools.plot2.Span;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.BooleanConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.DoubleConfigKey;
import uk.ac.starlink.ttools.plot2.config.OptionConfigKey;
import uk.ac.starlink.ttools.plot2.config.PerUnitConfigKey;
import uk.ac.starlink.ttools.plot2.config.SliderSpecifier;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.CoordGroup;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;
import uk.ac.starlink.ttools.plot2.geom.PlanarSurface;
import uk.ac.starlink.ttools.plot2.geom.SliceDataGeom;
import uk.ac.starlink.ttools.plot2.geom.TimeDataGeom;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType;
import uk.ac.starlink.util.SplitCollector;

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
    private final ConfigKey<Unit> unitKey_;
    private final ConfigKey<Combiner> combinerKey_;
    private final ReportKey<Combiner.Type> ctypeRepkey_;
    private final SliceDataGeom histoDataGeom_;
    private final CoordGroup histoCoordGrp_;
    private final int icX_;
    private final int icWeight_;
    private final boolean isTimeX_;

    /** ReportKey for histogram bins. */
    public static final ReportKey<BinBag> BINS_KEY =
        ReportKey.createUnprintableKey( new ReportMeta( "bins", "Bins" ),
                                        BinBag.class );

    /** ReportKey for actual bin width. */
    public static final ReportKey<Double> BINWIDTH_KEY =
        ReportKey.createDoubleKey( new ReportMeta( "binwidth", "Bin Width" ),
                                   false );

    /** ReportKey for tabular result of plot. */
    public static final ReportKey<StarTable> BINTABLE_KEY =
        ReportKey.createTableKey( new ReportMeta( "bins", "Bin data" ), true );

    /** Config key for bin size configuration. */
    public static final ConfigKey<BinSizer> BINSIZER_KEY =
        BinSizer.createSizerConfigKey(
            new ConfigMeta( "binsize", "Bin Size" )
           .setStringUsage( "+<width>|-<count>" )
           .setShortDescription( "Bin size specification" )
           .setXmlDescription( new String[] {
                "<p>Configures the width of histogram bins.",
                "If the supplied string is a positive number,",
                "it is interpreted as a fixed width in the data coordinates",
                "of the X axis",
                "(if the X axis is logarithmic, the value is a fixed factor).",
                "If it is a negative number, then it will be interpreted",
                "as the approximate number of bins to display across",
                "the width of the plot",
                "(though an attempt is made to use only round numbers",
                "for bin widths).",
                "</p>",
                "<p>When setting this value graphically,",
                "you can use either the slider to adjust the bin count",
                "or the numeric entry field to fix the bin width.",
                "</p>",
            } )
        , BINWIDTH_KEY, 30, false );

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
        , 0, 0, 1, false, false, SliderSpecifier.TextOption.ENTER_ECHO );

    /**
     * Constructor.
     *
     * @param   xCoord  X axis coordinate
     * @param   hasWeight   true to permit histogram weighting
     * @param   unitKey  config key to select X axis physical units,
     *                   or null if no unit selection required
     */
    public HistogramPlotter( FloatingCoord xCoord, boolean hasWeight,
                             PerUnitConfigKey<Unit> unitKey ) {
        xCoord_ = xCoord;
        unitKey_ = unitKey;
        ctypeRepkey_ = unitKey == null ? null
                                       : unitKey.getCombinerTypeReportKey();
        if ( hasWeight ) {
            weightCoord_ = FloatingCoord.WEIGHT_COORD;
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
        combinerKey_ = createCombinerKey( weightCoord_, unitKey );
        histoDataGeom_ =
            new SliceDataGeom( new FloatingCoord[] { xCoord_, null }, "X" );

        /* For this plot type, coordinate indices are not sensitive to
         * plot-time geom (the CoordGroup has no point positions),
         * so we can calculate them here. */
        icX_ = histoCoordGrp_.getExtraCoordIndex( 0, null );
        icWeight_ = hasWeight
                  ? histoCoordGrp_.getExtraCoordIndex( 1, null )
                  : -1;

        /* This is not nice, and will come back to bite me if I want to
         * allow for configurable axis alignment of the histogram.
         * But this information is required during
         * PlotLayer.extendCoordinateRange, and in the current framework
         * there's no better way to get it.
         * The problem is that PlotLayer.extendCoordinateRange is defined
         * with rather arbitrary/adhoc arguments. */
        isTimeX_ = xCoord == TimeDataGeom.T_COORD;
    }

    public String getPlotterName() {
        return "Histogram";
    }

    public Icon getPlotterIcon() {
        return ResourceIcon.FORM_HISTOGRAM;
    }

    public String getPlotterDescription() {
        final String weightPara;
        if ( weightCoord_ != null ) {
            weightPara = PlotUtil.concatLines( new String[] {
                "<p>Bin heights may optionally be weighted by the",
                "values of some additional coordinate,",
                "supplied using the",
                "<code>" + weightCoord_.getInput().getMeta().getShortName()
                         + "</code>",
                "parameter.",
                "In this case you can choose how these weights are combined",
                "in each bin using the",
                "<code>" + combinerKey_.getMeta().getShortName() + "</code>",
                "parameter.",
                "</p>",
            } );
        }
        else {
            weightPara = "";
        }
        return PlotUtil.concatLines( new String[] {
            "<p>Plots a histogram.",
            "</p>",
            weightPara,
            "<p>Various options are provided for configuring how the",
            "bar heights are calculated,",
            "but note that not all combinations of the available parameters",
            "will necessarily lead to meaningful visualisations.",
            "</p>",
            // for instance combine=mean and cumulative=true.
        } );
    }

    public CoordGroup getCoordGroup() {
        return histoCoordGrp_;
    }

    public ConfigKey<?>[] getStyleKeys() {
        List<ConfigKey<?>> list = new ArrayList<ConfigKey<?>>();
        list.addAll( Arrays.asList( new ConfigKey<?>[] {
            StyleKeys.COLOR,
            StyleKeys.TRANSPARENCY,
            BINSIZER_KEY,
            PHASE_KEY,
        } ) );
        list.add( combinerKey_ );
        if ( unitKey_ != null ) {
            list.add( unitKey_ );
        }
        list.addAll( Arrays.asList( new ConfigKey<?>[] {
            StyleKeys.CUMULATIVE,
            StyleKeys.NORMALISE,
            StyleKeys.BAR_FORM,
            THICK_KEY,
            StyleKeys.DASH,
        } ) );
        return list.toArray( new ConfigKey<?>[ 0 ] );
    }

    public HistoStyle createStyle( ConfigMap config ) {
        Color color = StyleKeys.getAlphaColor( config, StyleKeys.COLOR,
                                               StyleKeys.TRANSPARENCY );
        BarStyle.Form barForm = config.get( StyleKeys.BAR_FORM );
        BarStyle.Placement placement = BarStyle.PLACE_OVER;
        Cumulation cumulative = config.get( StyleKeys.CUMULATIVE );
        Normalisation norm = config.get( StyleKeys.NORMALISE );
        Unit unit = unitKey_ == null ? Unit.UNIT : config.get( unitKey_ );
        int thick = config.get( THICK_KEY );
        float[] dash = config.get( StyleKeys.DASH );
        BinSizer sizer = config.get( BINSIZER_KEY );
        double binPhase = config.get( PHASE_KEY );
        Combiner combiner = config.get( combinerKey_ );
        return new HistoStyle( color, barForm, placement, cumulative, norm,
                               unit, thick, dash, sizer, binPhase, combiner );
    }

    @Override
    public Object getRangeStyleKey( HistoStyle style ) {
        return Arrays.asList( style.combiner_, style.sizer_,
                              style.cumulative_, style.norm_ );
    }

    public boolean hasReports() {
        return true;
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
            final Combiner combiner = style.combiner_;
            final Cumulation cumul = style.cumulative_;
            final Normalisation norm = style.norm_;
            final Unit unit = style.unit_;
            final boolean hasWeight = weightCoord_ != null
                                   && ! dataSpec.isCoordBlank( icWeight_ );
            Color color = style.color_;
            final boolean isOpaque = color.getAlpha() == 255
                                 && style.barForm_.isOpaque();
            final Rounding xround = Rounding.getRounding( isTimeX_ );
            LayerOpt layerOpt = new LayerOpt( color, isOpaque );
            return new AbstractPlotLayer( this, histoDataGeom_, dataSpec,
                                          style, layerOpt ) {
                public Drawing createDrawing( Surface surface,
                                              Map<AuxScale,Span> auxSpans,
                                              final PaperType paperType ) {
                    if ( ! ( surface instanceof PlanarSurface ) ) {
                        throw new IllegalArgumentException( "Not planar surface"
                                                          + " " + surface );
                    }
                    final PlanarSurface pSurf = (PlanarSurface) surface;
                    final boolean xlog = pSurf.getLogFlags()[ 0 ];
                    double[] xlimits = pSurf.getDataLimits()[ 0 ];
                    final double xlo = xlimits[ 0 ];
                    final double xhi = xlimits[ 1 ];
                    final double binWidth =
                        sizer.getWidth( xlog, xlo, xhi, xround );

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
                                    if ( plan.matches( xlog, binWidth, binPhase,
                                                       combiner, dataSpec ) ) {
                                        return plan;
                                    }
                                }
                            }
                            BinBag binBag =
                                readBins( xlog, binWidth, binPhase, combiner,
                                          xlo, xhi, dataSpec, dataStore );
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
                                HistoPlan hplan = (HistoPlan) plan;
                                BinBag bbag = hplan.binBag_;
                                report.put( BINS_KEY, bbag );
                                report.put( BINWIDTH_KEY, bbag.getBinWidth() );
                                report.put( BINTABLE_KEY,
                                            new BinBagTable( hplan, style,
                                                             hasWeight,
                                                             xlog, xlo, xhi ) );
                                if ( ctypeRepkey_ != null ) {
                                    report.put( ctypeRepkey_,
                                                bbag.getCombiner().getType() );
                                }
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
                    double binWidth = sizer.getWidth( xlog, xlo, xhi, xround );
                    BinBag binBag =
                        readBins( xlog, binWidth, binPhase, combiner,
                                  xlo, xhi, dataSpec, dataStore );

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
                              binBag.binIterator( cumul, norm, unit, xlimits );
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
     * @param   combiner   bin aggregation mode
     * @param   xlo       representative lower value along axis
     * @param   xhi       representative upper value along axis
     * @param   dataSpec  specification for histogram data values
     * @param   dataStore  data storage
     */
    private BinBag readBins( final boolean xlog, final double binWidth,
                             final double binPhase, final Combiner combiner,
                             double xlo, double xhi,
                             DataSpec dataSpec, DataStore dataStore ) {
        final double point = PlotUtil.scaleValue( xlo, xhi, 0.5, xlog );
        final boolean isUnweighted =
            weightCoord_ == null || dataSpec.isCoordBlank( icWeight_ );
        SplitCollector<TupleSequence,BinBag> collector =
                new SplitCollector<TupleSequence,BinBag>() {
            public BinBag createAccumulator() {
                return new BinBag( xlog, binWidth, binPhase, combiner, point );
            }
            public void accumulate( TupleSequence tseq, BinBag binBag ) {
                if ( isUnweighted ) {
                    while ( tseq.next() ) {
                        double x = xCoord_.readDoubleCoord( tseq, icX_ );
                        binBag.submitToBin( x, 1 );
                    }
                }
                else {
                    while ( tseq.next() ) {
                        double x = xCoord_.readDoubleCoord( tseq, icX_ );
                        double w =
                            weightCoord_.readDoubleCoord( tseq, icWeight_ );
                        if ( ! Double.isNaN( w ) ) {
                            binBag.submitToBin( x, w );
                        }
                    }
                }
            }
            public BinBag combine( BinBag bag1, BinBag bag2 ) {
                if ( bag1.getBinCount() > bag2.getBinCount() ) {
                    bag1.add( bag2 );
                    return bag1;
                }
                else {
                    bag2.add( bag1 ); 
                    return bag2;
                }
            }
        };
        return PlotUtil.tupleCollect( collector, dataSpec, dataStore );
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
    private void paintBins( PlanarSurface surface, BinBag binBag,
                            HistoStyle style, int iseq, int nseq, Graphics g ) {
        Color color0 = g.getColor();
        g.setColor( style.color_ );
        BarStyle barStyle = style.barStyle_;
        Cumulation cumul = style.cumulative_;
        Normalisation norm = style.norm_;
        Unit unit = style.unit_;
        Rectangle clip = surface.getPlotBounds();
        int xClipMin = clip.x - 64;
        int xClipMax = clip.x + clip.width + 64;
        int yClipMin = clip.y - 64;
        int yClipMax = clip.y + clip.height + 64;
        double[][] dataLimits = surface.getDataLimits();
        double dxMin = dataLimits[ 0 ][ 0 ];
        double dxMax = dataLimits[ 0 ][ 1 ];
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
        for ( Iterator<BinBag.Bin> binIt =
                  binBag.binIterator( cumul, norm, unit,
                                      new double[] { dxMin, dxMax } );
              binIt.hasNext(); ) {
            BinBag.Bin bin = binIt.next();

            /* Get bin data. */
            double dxlo = bin.getXMin();
            double dxhi = bin.getXMax();
            double dy = bin.getY();

            /* Only plot those bins that fall at least partly in the X range. */
            if ( dxlo <= dxMax && dxhi >= dxMin &&
                 ( cumul.isCumulative() || dy != 0 ) ) {

                 /* Transform the corners of each bar to graphics coords. */
                 dpos0[ 0 ] = dxlo;
                 dpos0[ 1 ] = ylog ? Double.MIN_VALUE : 0;
                 dpos1[ 0 ] = dxhi;
                 dpos1[ 1 ] = dy;
                 if ( surface.dataToGraphics( dpos0, false, p0 ) &&
                      PlotUtil.isPointReal( p0 ) &&
                      surface.dataToGraphics( dpos1, false, p1 ) &&
                      PlotUtil.isPointReal( p1 ) ) {

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

        /* Draw the trailing edge of the final bar unless we have already
         * filled the whole X range. */
        if ( ! cumul.isCumulative() ) {
            barStyle.drawEdge( g, lastGx1, lastGy1, commonGy0, iseq, nseq );
        }
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
     * Creates a config key for selecting combination modes for a
     * histogram-like plotter.
     *
     * @param  weightCoord   weight coordinate, or null for no weighting
     * @param  unitKey     X-axis unit scaling key, or null for
     *                     no unit selection
     * @return   new config key
     */
    private static ConfigKey<Combiner>
            createCombinerKey( FloatingCoord weightCoord,
                               ConfigKey<Unit> unitKey ) {
        ConfigMeta meta = new ConfigMeta( "combine", "Combine" );
        boolean hasWeight = weightCoord != null;
        boolean hasUnit = unitKey != null;
        meta.setShortDescription( ( hasWeight ? "Weight" : "Value" )
                                + " combination mode" );
        StringBuffer dbuf = new StringBuffer();
        dbuf.append( PlotUtil.concatLines( new String[] {
            "<p>Defines how values contributing to the same bin",
            "are combined together to produce the value assigned to that bin,",
            "and hence its height.",
        } ) );
        if ( hasWeight ) {
            dbuf.append( PlotUtil.concatLines( new String[] {
                "The combined values are those given by the",
                "<code>" + weightCoord.getInput().getMeta().getShortName()
                         + "</code> coordinate,",
                "but if no weight is supplied,",
                "a weighting of unity is assumed.",
            } ) );
        }
        dbuf.append( "</p>\n" );
        if ( hasUnit ) {
            dbuf.append( PlotUtil.concatLines( new String[] {
                "<p>For density-like values",
                "(<code>" + Combiner.DENSITY + "</code>,",
                "<code>" + Combiner.WEIGHTED_DENSITY + "</code>)",
                "the scaling is additionally influenced by the",
                "<code>" + unitKey.getMeta().getShortName() + "</code>",
                "parameter.",
                "</p>",
            } ) );
        }
        meta.setXmlDescription( dbuf.toString() );
        List<Combiner> optionList = new ArrayList<Combiner>();
        for ( Combiner c : Combiner.getKnownCombiners() ) {
            if ( hasWeight || !Combiner.Type.INTENSIVE.equals( c.getType() ) ) {
                optionList.add( c );
            }
        }
        Combiner[] options = optionList.toArray( new Combiner[ 0 ] );
        OptionConfigKey<Combiner> key =
                new OptionConfigKey<Combiner>( meta, Combiner.class, options,
                                               Combiner.SUM ) {
            public String getXmlDescription( Combiner combiner ) {
                return combiner.getDescription();
            }
        };
        key.setOptionUsage();
        key.addOptionsXml();
        return key;
    }

    /**
     * Adapts a BinBag to a StarTable for presentation as a reported
     * output of a histogram plot.
     */
    private static class BinBagTable extends AbstractStarTable {
        private final BinBag binBag_;
        private final HistoStyle hstyle_;
        private final boolean xlog_;
        private final double xlo_;
        private final double xhi_;
        private final ColumnInfo xmidInfo_;
        private final ColumnInfo xminInfo_;
        private final ColumnInfo xmaxInfo_;
        private final ColumnInfo yInfo_;
        private final ColumnInfo[] colInfos_;

        /**
         * Constructor.
         *
         * @param  hplan   plan containing bin data
         * @param  hstyle  plot style
         * @param  hasWeight  true if the weight coordinate may be non-blank
         * @param  xlog   true for horizontal coordinate is logarithmic,
         *                false for linear
         * @param  xlo    lower bound on X axis
         * @param  xhi    upper bound on X axis
         */
        BinBagTable( HistoPlan hplan, HistoStyle hstyle, boolean hasWeight,
                     boolean xlog, double xlo, double xhi ) {
            binBag_ = hplan.binBag_;
            hstyle_ = hstyle;
            xlog_ = xlog;
            xlo_ = xlo;
            xhi_ = xhi;
            Combiner combiner = hstyle.getCombiner();
            String yName = "Y_" + ( hasWeight ? combiner.getName() : "COUNT" );
            xmidInfo_ = new ColumnInfo( "XMID", Double.class, "Bin midpoint" );
            xminInfo_ = new ColumnInfo( "XLOW", Double.class,
                                        "Bin lower bound" );
            xmaxInfo_ = new ColumnInfo( "XHIGH", Double.class,
                                        "Bin upper bound" );
            yInfo_ = new ColumnInfo( yName, Double.class, "Bin height" );
            colInfos_ =
                new ColumnInfo[] { xmidInfo_, yInfo_, xminInfo_, xmaxInfo_, };
        }

        public int getColumnCount() {
            return colInfos_.length;
        }

        public ColumnInfo getColumnInfo( int icol ) {
            return colInfos_[ icol ];
        }

        public long getRowCount() {
            return -1;
        }

        public RowSequence getRowSequence() {
            final Iterator<BinBag.Bin> binIt =
                binBag_.binIterator( hstyle_.getCumulative(),
                                     hstyle_.getNormalisation(),
                                     hstyle_.getUnit(),
                                     new double[] { xlo_, xhi_ } );
            final Iterator<Object[]> rowIt = new Iterator<Object[]>() {
                public boolean hasNext() {
                    return binIt.hasNext();
                }
                public Object[] next() {
                    BinBag.Bin bin = binIt.next();
                    double xmin = bin.getXMin();
                    double xmax = bin.getXMax();
                    double xmid = PlotUtil.scaleValue( xmin, xmax, 0.5, xlog_ );
                    return new Object[] {
                        Double.valueOf( xmid ),
                        Double.valueOf( bin.getY() ),
                        Double.valueOf( xmin ),
                        Double.valueOf( xmax ),
                    };
                }
                public void remove() {
                    binIt.remove();
                }
            };
            return new IteratorRowSequence( rowIt );
        }
    }

    /**
     * Style subclass for histogram plots.
     */
    public static class HistoStyle implements Style {
        private final Color color_;
        private final BarStyle.Form barForm_;
        private final BarStyle.Placement placement_;
        private final Cumulation cumulative_;
        private final Normalisation norm_;
        private final Unit unit_;
        private final int thick_;
        private final float[] dash_;
        private final BinSizer sizer_;
        private final double phase_;
        private final Combiner combiner_;

        private final BarStyle barStyle_;

        /**
         * Constructor.
         *
         * @param  color   bar colour
         * @param  barForm  bar form
         * @param  placement  bar placement
         * @param  cumulative  whether to plot cumulative bars
         * @param  norm    normalisation mode for the vertical scale
         * @param  unit    bin scaling unit
         * @param  thick   line thickness (only relevant for some forms)
         * @param  dash    line dash pattern (only relevant for some forms)
         * @param  sizer   determines bin widths
         * @param  phase   bin reference point, 0..1
         * @param  combiner  bin aggregation mode
         */
        public HistoStyle( Color color, BarStyle.Form barForm,
                           BarStyle.Placement placement,
                           Cumulation cumulative, Normalisation norm, Unit unit,
                           int thick, float[] dash,
                           BinSizer sizer, double phase, Combiner combiner ) {
            color_ = color;
            barForm_ = barForm;
            placement_ = placement;
            cumulative_ = cumulative;
            norm_ = norm;
            unit_ = unit;
            thick_ = thick;
            dash_ = dash;
            sizer_ = sizer;
            phase_ = phase;
            combiner_ = combiner;
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
         * Returns cumulative plot mode.
         *
         * @return  cumulative mode
         */
        public Cumulation getCumulative() {
            return cumulative_;
        }

        /**
         * Returns normalisation mode.
         *
         * @return  normalisation mode for count axis
         */
        public Normalisation getNormalisation() {
            return norm_;
        }

        /**
         * Returns the axis unit for density scaling.
         *
         * @return   x axis unit
         */
        public Unit getUnit() {
            return unit_;
        }

        /**
         * Returns the combination mode used for aggregating values into bins.
         *
         * @return  combiner
         */
        public Combiner getCombiner() {
            return combiner_;
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
            code = 23 * code + cumulative_.hashCode();
            code = 23 * code + PlotUtil.hashCode( norm_ );
            code = 23 * code + unit_.hashCode();
            code = 23 * code + thick_;
            code = 23 * code + Arrays.hashCode( dash_ );
            code = 23 * code + sizer_.hashCode();
            code = 23 * code + Float.floatToIntBits( (float) phase_ );
            code = 23 * code + combiner_.hashCode();
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
                    && PlotUtil.equals( this.norm_, other.norm_ )
                    && this.unit_.equals( other.unit_ )
                    && this.thick_ == other.thick_
                    && Arrays.equals( this.dash_, other.dash_ )
                    && this.sizer_.equals( other.sizer_ )
                    && this.phase_ == other.phase_
                    && this.combiner_.equals( other.combiner_ );
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
         * @param   combiner    bin aggregation mode
         * @param   dataSpec   source of coordinate data
         */
        boolean matches( boolean xlog, double binWidth, double binPhase,
                         Combiner combiner, DataSpec dataSpec ) {
            return binBag_.matches( xlog, binWidth, binPhase, combiner )
                && dataSpec_.equals( dataSpec );
        }
    }
}
