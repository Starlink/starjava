package uk.ac.starlink.ttools.plot2.layer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.swing.Icon;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.Axis;
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
import uk.ac.starlink.ttools.plot2.Scale;
import uk.ac.starlink.ttools.plot2.Span;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.BooleanConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.DoubleArrayConfigKey;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.CoordGroup;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.InputMeta;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;
import uk.ac.starlink.ttools.plot2.geom.PlanarSurface;
import uk.ac.starlink.ttools.plot2.geom.SliceDataGeom;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType;
import uk.ac.starlink.util.SplitCollector;

/**
 * Plotter to calculate and display univariate statistics
 * of histogram-like data.
 *
 * @author   Mark Taylor
 * @since    30 Sep 2016
 */
public class Stats1Plotter implements Plotter<Stats1Plotter.StatsStyle> {

    private final FloatingCoord xCoord_;
    private final FloatingCoord weightCoord_;
    private final ConfigKey<Unit> unitKey_;
    private final CoordGroup fitCoordGrp_;
    private final int icX_;
    private final int icWeight_;

    /** Report key for fitted multiplicative constant. */
    public static final ReportKey<Double> CONST_KEY =
        ReportKey.createDoubleKey( new ReportMeta( "c", "Factor" ), true );

    /** Report key for fitted mean. */
    public static final ReportKey<Double> MEAN_KEY =
        ReportKey.createDoubleKey( new ReportMeta( "mu", "Mean" ), true );

    /** Report key for fitted standard deviation. */
    public static final ReportKey<Double> STDEV_KEY =
        ReportKey.createDoubleKey( new ReportMeta( "sigma",
                                                   "Standard Deviation" ),
                                   true );

    /** Report key for gaussian fit function. */
    public static final ReportKey<String> FUNCTION_KEY =
        ReportKey.createStringKey( new ReportMeta( "function", "Function" ),
                                   true );

    /** Config key for equivalent histogram bar width. */
    public static final ConfigKey<BinSizer> BINSIZER_KEY =
        HistogramPlotter.BINSIZER_KEY;

    /** Config key for normalisation. */
    public static final ConfigKey<Normalisation> NORMALISE_KEY =
        StyleKeys.NORMALISE;

    /** Config key to display a line at the mean value. */
    public static final ConfigKey<Boolean> SHOWMEAN_KEY =
        new BooleanConfigKey(
            new ConfigMeta( "showmean", "Show Mean" )
           .setShortDescription( "Display a line at the mean" )
           .setXmlDescription( new String[] {
                "<p>If true, a line is drawn at the position of",
                "the calculated mean.",
                "</p>",
            } )
        , true );

    /**
     * Constructor.
     *
     * @param  xCoord   X axis coordinate
     * @param  hasWeight  true if weights may be used
     * @param   unitKey  config key to select X axis physical units,
     *                   or null if no unit selection required
     */
    public Stats1Plotter( FloatingCoord xCoord, boolean hasWeight,
                          ConfigKey<Unit> unitKey ) {
        xCoord_ = xCoord;
        unitKey_ = unitKey;
        if ( hasWeight ) {
            weightCoord_ = FloatingCoord.WEIGHT_COORD;
            fitCoordGrp_ =
                CoordGroup
               .createPartialCoordGroup( new Coord[] { xCoord, weightCoord_ },
                                         new boolean[] { false, false } );
        }
        else {
            weightCoord_ = null;
            fitCoordGrp_ =
                CoordGroup
               .createPartialCoordGroup( new Coord[] { xCoord },
                                         new boolean[] { false } );
        }

        /* For this plot type, coordinate indices are not sensitive to
         * plot-time geom (the CoordGroup has no point positions),
         * so we can calculate them here. */
        icX_ = fitCoordGrp_.getExtraCoordIndex( 0, null );
        icWeight_ = hasWeight
                  ? fitCoordGrp_.getExtraCoordIndex( 1, null )
                  : -1;
    }

    public String getPlotterName() {
        return "Gaussian";
    }

    public Icon getPlotterIcon() {
        return ResourceIcon.FORM_GAUSSIAN;
    }

    public boolean hasReports() {
        return true;
    }

    public String getPlotterDescription() {
        return PlotUtil.concatLines( new String[] {
            "<p>Plots a best fit Gaussian to the histogram of",
            "a sample of data.",
            "In fact, all this plotter does is to calculate the mean",
            "and standard deviation of the sample,",
            "and plot the corresponding Gaussian curve.",
            "The mean and standard deviation values are reported by the plot.",
            "</p>",
            "<p>The <code>" + NORMALISE_KEY + "</code> config option,",
            "perhaps in conjunction with <code>" + BINSIZER_KEY + "</code>,",
            "can be used to scale the height of the plotted curve",
            "in data units.",
            "In this case, <code>" + BINSIZER_KEY + "</code>",
            "just describes the bar width of a notional histogram",
            "whose outline the plotted Gaussian should try to fit,",
            "and is only relevant for some of the normalisation options.",
            "</p>",
        } );
    }

    public CoordGroup getCoordGroup() {
        return fitCoordGrp_;
    }

    public ConfigKey<?>[] getStyleKeys() {
        List<ConfigKey<?>> list = new ArrayList<ConfigKey<?>>();
        list.add( StyleKeys.COLOR );
        list.add( SHOWMEAN_KEY );
        list.add( StyleKeys.SIDEWAYS );
        list.addAll( Arrays.asList( StyleKeys.getStrokeKeys() ) );
        list.add( StyleKeys.ANTIALIAS );
        if ( unitKey_ != null ) {
            list.add( unitKey_ );
        }
        list.add( NORMALISE_KEY );
        list.add( BINSIZER_KEY );
        return list.toArray( new ConfigKey<?>[ 0 ] );
    }

    public StatsStyle createStyle( ConfigMap config ) {
        Color color = config.get( StyleKeys.COLOR );
        boolean showmean = Boolean.TRUE.equals( config.get( SHOWMEAN_KEY ) );
        boolean isY = config.get( StyleKeys.SIDEWAYS ).booleanValue();
        Stroke stroke = StyleKeys.createStroke( config, BasicStroke.CAP_ROUND,
                                                BasicStroke.JOIN_ROUND );
        boolean antialias = config.get( StyleKeys.ANTIALIAS );
        Normalisation norm = config.get( NORMALISE_KEY );
        Unit unit = unitKey_ == null ? Unit.UNIT : config.get( unitKey_ );
        BinSizer sizer = config.get( BINSIZER_KEY );
        return new StatsStyle( color, stroke, antialias, showmean,
                               norm, unit, sizer, isY );
    }

    @Override
    public Object getRangeStyleKey( StatsStyle style ) {
        return style.norm_;
    }

    public PlotLayer createLayer( final DataGeom geom, final DataSpec dataSpec,
                                  final StatsStyle style ) {
        LayerOpt layerOpt = new LayerOpt( style.getColor(), true );
        boolean isY = style.isY_;
        DataGeom fitDataGeom =
              isY
            ? new SliceDataGeom( new FloatingCoord[] { null, xCoord_ }, "Y" )
            : new SliceDataGeom( new FloatingCoord[] { xCoord_, null }, "X" );
        return new AbstractPlotLayer( this, fitDataGeom, dataSpec,
                                      style, layerOpt ) {
            public Drawing createDrawing( Surface surface,
                                          Map<AuxScale,Span> auxSpans,
                                          PaperType paperType ) {
                return new StatsDrawing( (PlanarSurface) surface, geom,
                                         dataSpec, style, paperType );
            }
            @Override
            public void extendCoordinateRanges( Range[] ranges,
                                                Scale[] scales,
                                                DataStore dataStore ) {
                Scale scale = scales[ isY ? 1 : 0 ];
                WStats stats = collectStats( scale, dataSpec, dataStore );
                StatsPlan plan = new StatsPlan( scale, stats, dataSpec );
                double mean = stats.getMean();
                double sd = stats.getSigma();
                Range xRange = ranges[ isY ? 1 : 0 ];
                xRange.submit( mean - sd * 2 );
                xRange.submit( mean + sd * 2 );
                if ( xRange.isFinite() ) {
                    double[] xlims =
                        xRange.getFiniteBounds( scale.isPositiveDefinite() );
                    double yhi =
                        plan.getFactor( xlims[ 0 ], xlims[ 1 ], style );
                    Range yRange = ranges[ isY ? 0 : 1 ];
                    yRange.submit( 0 );
                    yRange.submit( yhi );
                }
            }
        };
    }

    /**
     * Determines the stats by going through the data.
     *
     * @param  scale      axis scale on X axis
     * @param  dataSpec   data spec
     * @param  dataStore  data store
     * @return  calculated statistics
     */
    private WStats collectStats( Scale scale, DataSpec dataSpec,
                                 DataStore dataStore ) {
        final boolean isUnweighted = weightCoord_ == null
                                  || dataSpec.isCoordBlank( icWeight_ );
        SplitCollector<TupleSequence,WStats> collector =
                new SplitCollector<TupleSequence,WStats>() {
            public WStats createAccumulator() {
                return new WStats();
            }
            public void accumulate( TupleSequence tseq, WStats stats ) {
                if ( isUnweighted ) {
                    while ( tseq.next() ) {
                        double x = xCoord_.readDoubleCoord( tseq, icX_ );
                        double s = scale.dataToScale( x );
                        if ( PlotUtil.isFinite( s ) ) {
                            stats.addPoint( s );
                        }
                    }
                }
                else {
                    while ( tseq.next() ) {
                        double x = xCoord_.readDoubleCoord( tseq, icX_ );
                        double s = scale.dataToScale( x );
                        if ( PlotUtil.isFinite( s ) ) {
                            double w = weightCoord_
                                      .readDoubleCoord( tseq, icWeight_ );
                            stats.addPoint( s, w );
                        }
                    } 
                }
            }
            public WStats combine( WStats stats1, WStats stats2 ) {
                stats1.add( stats2 );
                return stats1;
            }
        };
        return PlotUtil.tupleCollect( collector, dataSpec, dataStore );
    }

    /**
     * Style class associated with this plotter.
     */
    public static class StatsStyle extends LineStyle {

        final boolean showmean_;
        final Normalisation norm_;
        final Unit unit_;
        final BinSizer sizer_;
        final boolean isY_;

        /**
         * Constructor.
         *
         * @param  color   line colour
         * @param  stroke  line stroke
         * @param  antialias  true to draw line antialiased
         * @param  showmean   true to display a line showing the mean
         * @param  norm  normalisation
         * @param  unit   axis scaling unit
         * @param  sizer   histogram equivalent bin sizer,
         *                 may be used in conjunction with norm
         * @param  isY    if true, plotted sideways
         */
        public StatsStyle( Color color, Stroke stroke, boolean antialias,
                           boolean showmean, Normalisation norm, Unit unit,
                           BinSizer sizer, boolean isY ) {
            super( color, stroke, antialias );
            showmean_ = showmean;
            norm_ = norm;
            unit_ = unit;
            sizer_ = sizer;
            isY_ = isY;
        }

        @Override
        public int hashCode() {
            int code = super.hashCode();
            code = 23 * code + ( showmean_ ? 11 : 17 );
            code = 23 * code + PlotUtil.hashCode( norm_ );
            code = 23 * code + unit_.hashCode();
            code = 23 * code + PlotUtil.hashCode( sizer_ );
            code = 23 * code + ( isY_ ? 71 : 41 );
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof StatsStyle ) {
                StatsStyle other = (StatsStyle) o;
                return super.equals( other )
                    && this.showmean_ == other.showmean_
                    && PlotUtil.equals( this.norm_, other.norm_ )
                    && this.unit_.equals( other.unit_ )
                    && PlotUtil.equals( this.sizer_, other.sizer_ )
                    && this.isY_ == other.isY_;
            }
            else {
                return false;
            }
        }
    }

    /**
     * Drawing for stats plot.
     */
    private class StatsDrawing implements Drawing {

        private final PlanarSurface surface_;
        private final DataGeom geom_;
        private final DataSpec dataSpec_;
        private final StatsStyle style_;
        private final PaperType paperType_;

        /**
         * Constructor.
         *
         * @param  surface   plotting surface
         * @param  geom      maps position coordinates to graphics positions
         * @param  dataSpec  data points to fit
         * @param  style     line plotting style
         * @param  paperType  paper type
         */
        StatsDrawing( PlanarSurface surface, DataGeom geom, DataSpec dataSpec,
                      StatsStyle style, PaperType paperType ) {
            surface_ = surface;
            geom_ = geom;
            dataSpec_ = dataSpec;
            style_ = style;
            paperType_ = paperType;
        }

        public Object calculatePlan( Object[] knownPlans,
                                     DataStore dataStore ) {
            Scale scale = surface_.getAxes()[ style_.isY_ ? 1 : 0 ].getScale();

            /* If one of the known plans matches the one we're about
             * to calculate, just return that. */
            for ( Object plan : knownPlans ) {
                if ( plan instanceof StatsPlan &&
                     ((StatsPlan) plan).matches( scale, dataSpec_ ) ) {
                    return plan;
                }
            }

            /* Otherwise, accumulate statistics and return the result. */
            WStats stats = collectStats( scale, dataSpec_, dataStore );
            return new StatsPlan( scale, stats, dataSpec_ );
        }

        public void paintData( Object plan, Paper paper, DataStore dataStore ) {
            final StatsPlan splan = (StatsPlan) plan;
            paperType_.placeDecal( paper, new Decal() {
                public void paintDecal( Graphics g ) {
                    splan.paintLine( g, surface_, style_,
                                     paperType_.isBitmap() );
                }
                public boolean isOpaque() {
                    return ! style_.getAntialias();
                }
            } );
        }

        public ReportMap getReport( Object plan ) {
            return ((StatsPlan) plan).getReport( surface_, style_ );
        }
    }

    /**
     * Plan object encapsulating the inputs and results of a stats plot.
     */
    private static class StatsPlan {
        final Scale scale_;
        final double mean_;
        final double sigma_;
        final double sum_;
        final DataSpec dataSpec_;

        /**
         * Constructor.
         *
         * @param  scale   axis scaling
         * @param  stats   univariate statistics giving fit results
         * @param  dataSpec   characterisation of input data points
         */
        StatsPlan( Scale scale, WStats stats, DataSpec dataSpec ) {
            scale_ = scale;
            mean_ = stats.getMean();
            sigma_ = stats.getSigma();
            sum_ = stats.getSum();
            dataSpec_ = dataSpec;
        }

        /**
         * Indicates whether this object's state will be the same as
         * a plan calculated for the given input values.
         *
         * @param  scale     axis scaling
         * @param  dataSpec  characterisation of input data points
         */
        boolean matches( Scale scale, DataSpec dataSpec ) {
            return scale.equals( scale_ )
                && dataSpec.equals( dataSpec_ );
        }

        /**
         * Plots the fit line for this fitting result.
         *
         * @param  g  graphics context
         * @param  surface  plot surface
         * @param  style   style
         */
        void paintLine( Graphics g, PlanarSurface surface, StatsStyle style,
                        boolean isBitmap ) {
            double factor = getFactor( surface, style );
            boolean isY = style.isY_;
            Axis xAxis = surface.getAxes()[ isY ? 1 : 0 ];
            Axis yAxis = surface.getAxes()[ isY ? 0 : 1 ];
            Graphics2D g2 = (Graphics2D) g;
            Rectangle box = surface.getPlotBounds();
            int gxlo = xAxis.getGraphicsLimits()[ 0 ] - 2;
            int gxhi = xAxis.getGraphicsLimits()[ 1 ] + 2;
            int np = gxhi - gxlo;
            LineTracer tracer = style.createLineTracer( g2, box, np, isBitmap );
            Color color = style.getColor();
            for ( int ip = 0; ip < np; ip++ ) {
                double gx = gxlo + ip;
                double dx = xAxis.graphicsToData( gx );
                if ( ! Double.isNaN( dx ) ) {
                    double dy = factor * gaussian( dx );
                    double gy = yAxis.dataToGraphics( dy );
                    if ( ! Double.isNaN( gy ) ) {
                        tracer.addVertex( isY ? gy : gx, isY ? gx : gy, color );
                    }
                }
            }
            tracer.flush();
            if ( style.showmean_ ) {
                double dx = mean_;
                double gx = xAxis.dataToGraphics( dx );
                double gylo = yAxis.dataToGraphics( 0 );
                double gyhi = yAxis.dataToGraphics( factor );
                LineTracer meanTracer =
                    style.createLineTracer( g2, box, 3, isBitmap );
                if ( isY ) {
                    meanTracer.addVertex( gylo, gx, color );
                    meanTracer.addVertex( gyhi, gx, color );
                }
                else {
                    meanTracer.addVertex( gx, gylo, color );
                    meanTracer.addVertex( gx, gyhi, color );
                }
                meanTracer.flush();
            }
        }

        /**
         * Returns the multiplicative factor by which the <code>gaussian</code>
         * method should be multiplied to give the plotted value.
         *
         * @param  surface   target plotting surface
         * @param  style     stats style
         */
        private double getFactor( PlanarSurface surface, StatsStyle style ) {
            boolean isY = style.isY_;
            double[] xlims = surface.getDataLimits()[ isY ? 1 : 0 ];
            return getFactor( xlims[ 0 ], xlims[ 1 ], style );
        }

        /**
         * Returns the multiplicative factor by which the <code>gaussian</code>
         * method should be multiplied to give the plotted value.
         *
         * @param  xlo     approx X coordinate lower limit of surface
         * @param  xhi     approx X coordinate upper limit of surface
         * @param  style     stats style
         */
        private double getFactor( double xlo, double xhi, StatsStyle style ) {
            BinSizer sizer = style.sizer_;
            double binWidth = sizer.getScaleWidth( scale_, xlo, xhi, true )
                            / style.unit_.getExtent();
            double c = 1.0 / ( sigma_ * Math.sqrt( 2.0 * Math.PI ) );
            double sum = sum_;
            double max = c * sum * binWidth;
            boolean isCumulative = false;
            Combiner.Type ctype = Combiner.Type.EXTENSIVE;
            double normFactor =
                style.norm_.getScaleFactor( sum, max, binWidth, ctype,
                                            isCumulative );
            return normFactor * c * sum_ * binWidth;
        }

        /**
         * Returns the value of the Gaussian function for this plan,
         * using data coordinates.
         * The result is lacking a scale factor;
         * its value is unity at the mean.
         *
         * @param  x  input value in data coordinates
         * @return  unscaled Gaussian function evaluated at <code>x</code>
         */
        double gaussian( double x ) {
            double s = scale_.dataToScale( x );
            double p = ( s - mean_ ) / sigma_;
            return Math.exp( - 0.5 * p * p );
        }

        /**
         * Returns a plot report based on the state of this plan.
         *
         * @param   surface  target plotting surface
         * @param   style    plot style
         * @return  report
         */
        public ReportMap getReport( PlanarSurface surface, StatsStyle style ) {
            ReportMap report = new ReportMap();
            double factor = getFactor( surface, style );
            report.put( MEAN_KEY, mean_ );
            report.put( STDEV_KEY, sigma_ );
            report.put( CONST_KEY, factor );
            String function = new StringBuffer()
                .append( CONST_KEY.toText( factor ) )
                .append( " * " )
                .append( "exp(-0.5 * square((" )
                .append( scale_.dataToScaleExpression( "x" ) )
                .append( "-" )
                .append( MEAN_KEY.toText( mean_ ) )
                .append( ")/" )
                .append( STDEV_KEY.toText( sigma_ ) )
                .append( "))" )
                .toString();
            report.put( FUNCTION_KEY, function );
            return report;
        }
    }

    /**
     * Accumulates and calculates statistics for an optionally
     * weighted single variable.
     */
    private static class WStats {
        private double sw_;
        private double swX_;
        private double swXX_;

        /**
         * Submits a weighted data value.
         *
         * @param   x  data value
         * @param   w  weighting
         */
        public void addPoint( double x, double w ) {
            if ( w > 0 && ! Double.isInfinite( w ) ) {
                sw_ += w;
                swX_ += w * x;
                swXX_ += w * x * x;
            }
        }

        /**
         * Submits a data value with unit weighting.
         *
         * @param  x  data value
         */
        public void addPoint( double x ) { 
            sw_ += 1;
            swX_ += x;
            swXX_ += x * x;
        }

        /**
         * Merges the contents of a second instance into this one.
         * The effect is as if all the points that have been added to the
         * other one are added to this.
         *
         * @param  other  other stats
         */
        public void add( WStats other ) {
            sw_ += other.sw_;
            swX_ += other.swX_;
            swXX_ += other.swXX_;
        }

        /**
         * Returns the mean of the values submitted so far.
         *
         * @return  mean
         */
        public double getMean() {
            return swX_ / sw_;
        }

        /**
         * Returns the standard deviation of the values submitted so far.
         *
         * @return  standard deviation
         */
        public double getSigma() {
            return Math.sqrt( ( swXX_ - swX_ * swX_ / sw_ ) / sw_ );
        }

        /**
         * Returns the sum of the values submitted so far.
         *
         * @return  weighted sum
         */
        public double getSum() {
            return sw_;
        }
    }
}
