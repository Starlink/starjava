package uk.ac.starlink.ttools.plot2.layer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.swing.Icon;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot.Range;
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
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.CoordGroup;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.InputMeta;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;
import uk.ac.starlink.ttools.plot2.geom.SliceDataGeom;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType;

/**
 * Plotter to calculate and display univariate statistics
 * of histogram-like data.
 *
 * @author   Mark Taylor
 * @since    30 Sep 2016
 */
public class Stats1Plotter implements Plotter<LineStyle> {

    private final FloatingCoord xCoord_;
    private final FloatingCoord weightCoord_;
    private final SliceDataGeom fitDataGeom_;
    private final CoordGroup fitCoordGrp_;
    private final int icX_;
    private final int icWeight_;

    /** Report key for fitted mean. */
    private static final ReportKey<Double> MEAN_KEY =
        ReportKey.createDoubleKey( new ReportMeta( "mu", "Mean" ), true );

    /** Report key for fitted standard deviation. */
    private static final ReportKey<Double> STDEV_KEY =
        ReportKey.createDoubleKey( new ReportMeta( "sigma",
                                                   "Standard Deviation" ),
                                   true );

    /**
     * Constructor.
     *
     * @param  xCoord   X axis coordinate
     * @param  hasWeight  true if weights may be used
     */
    public Stats1Plotter( FloatingCoord xCoord, boolean hasWeight ) {
        xCoord_ = xCoord;
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
        fitDataGeom_ =
            new SliceDataGeom( new FloatingCoord[] { xCoord_, null }, "X" );

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
        return new StringBuffer()
              .append( "<p>Presents a graphical representation of\n" )
              .append( "the mean and standard deviation\n" )
              .append( "of a sample of, possibly weighted, values.\n" )
              .append( "By default this representation takes the form of\n" )
              .append( "a Gaussian fit to the histogram of the data.\n" )
              .append( "</p>" )
              .toString();
    }

    public CoordGroup getCoordGroup() {
        return fitCoordGrp_;
    }

    public ConfigKey[] getStyleKeys() {
        List<ConfigKey> list = new ArrayList<ConfigKey>();
        list.add( StyleKeys.COLOR );
        list.addAll( Arrays.asList( StyleKeys.getStrokeKeys() ) );
        list.add( StyleKeys.ANTIALIAS );
        return list.toArray( new ConfigKey[ 0 ] );
    }

    public LineStyle createStyle( ConfigMap config ) {
        Color color = config.get( StyleKeys.COLOR );
        Stroke stroke = StyleKeys.createStroke( config, BasicStroke.CAP_ROUND,
                                                BasicStroke.JOIN_ROUND );
        boolean antialias = config.get( StyleKeys.ANTIALIAS );
        return new LineStyle( color, stroke, antialias );
    }

    public PlotLayer createLayer( final DataGeom geom, final DataSpec dataSpec,
                                  final LineStyle style ) {
        LayerOpt layerOpt = new LayerOpt( style.getColor(), true );
        return new AbstractPlotLayer( this, fitDataGeom_, dataSpec,
                                      style, layerOpt ) {
            public Drawing createDrawing( Surface surface,
                                          Map<AuxScale,Range> auxRanges,
                                          PaperType paperType ) {
                return new StatsDrawing( surface, geom, dataSpec, style,
                                         paperType );
            }
        };
    }

    /**
     * Drawing for stats plot.
     */
    private class StatsDrawing implements Drawing {

        private final Surface surface_;
        private final DataGeom geom_;
        private final DataSpec dataSpec_;
        private final LineStyle style_;
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
        StatsDrawing( Surface surface, DataGeom geom, DataSpec dataSpec,
                      LineStyle style, PaperType paperType ) {
            surface_ = surface;
            geom_ = geom;
            dataSpec_ = dataSpec;
            style_ = style;
            paperType_ = paperType;
        }

        public Object calculatePlan( Object[] knownPlans,
                                     DataStore dataStore ) {

            /* If one of the known plans matches the one we're about
             * to calculate, just return that. */
            for ( Object plan : knownPlans ) {
                if ( plan instanceof StatsPlan &&
                     ((StatsPlan) plan).matches( dataSpec_ ) ) {
                    return plan;
                }
            }

            /* Otherwise, accumulate statistics and return the result. */
            WStats stats = new WStats();
            TupleSequence tseq = dataStore.getTupleSequence( dataSpec_ );
            if ( weightCoord_ == null || dataSpec_.isCoordBlank( icWeight_ ) ) {
                while ( tseq.next() ) {
                    double x = xCoord_.readDoubleCoord( tseq, icX_ );
                    stats.addPoint( x );
                }
            }
            else {
                while ( tseq.next() ) {
                    double x = xCoord_.readDoubleCoord( tseq, icX_ );
                    double w = weightCoord_.readDoubleCoord( tseq, icWeight_ );
                    stats.addPoint( x, w );
                } 
            }
            return new StatsPlan( stats, dataSpec_ );
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
            return ((StatsPlan) plan).getReport();
        }
    }

    /**
     * Plan object encapsulating the inputs and results of a stats plot.
     */
    private static class StatsPlan {
        final double mean_;
        final double sigma_;
        final double c_;
        final DataSpec dataSpec_;

        /**
         * Constructor.
         *
         * @param  stats   univariate statistics giving fit results
         * @param  dataSpec   characterisation of input data points
         */
        StatsPlan( WStats stats, DataSpec dataSpec ) {
            mean_ = stats.getMean();
            sigma_ = stats.getSigma();
            c_ = stats.getSum() / ( sigma_ * Math.sqrt( 2.0 * Math.PI ) );
            dataSpec_ = dataSpec;
        }

        /**
         * Indicates whether this object's state will be the same as
         * a plan calculated for the given input values.
         *
         * @param  dataSpec  characterisation of input data points
         */
        boolean matches( DataSpec dataSpec ) {
            return dataSpec.equals( dataSpec_ );
        }

        /**
         * Plots the fit line for this fitting result.
         *
         * @param  g  graphics context
         * @param  surface  plot surface
         * @param  style   line style
         */
        void paintLine( Graphics g, Surface surface, LineStyle style,
                        boolean isBitmap ) {
            Graphics2D g2 = (Graphics2D) g;
            Rectangle box = surface.getPlotBounds();
            int gxlo = box.x - 2;
            int gxhi = box.x + box.width + 2;
            int np = gxhi - gxlo;
            LineTracer tracer = style.createLineTracer( g2, box, np, isBitmap );
            Point2D.Double gpos = new Point2D.Double();
            double[] dpos = new double[ surface.getDataDimCount() ];
            for ( int ip = 0; ip < np; ip++ ) {
                double dx =
                    surface
                   .graphicsToData( new Point( gxlo + ip, box.y ), null )[ 0 ];
                if ( ! Double.isNaN( dx ) ) {
                    dpos[ 0 ] = dx;
                    dpos[ 1 ] = gaussian( dx );
                    if ( surface.dataToGraphics( dpos, false, gpos ) &&
                         PlotUtil.isPointReal( gpos ) ) {
                        tracer.addVertex( gpos.x, gpos.y );
                    }
                }
            }
            tracer.flush();
        }

        /**
         * Returns the value of the Gaussian function for this plan,
         * using data coordinates.
         *
         * @param  x  input value
         * @return  Gaussian function evaluated at <code>x</code>
         */
        double gaussian( double x ) {
            double p = ( x - mean_ ) / sigma_;
            return c_ * Math.exp( - 0.5 * p * p );
        }

        /**
         * Returns a plot report based on the state of this plan.
         *
         * @return  report
         */
        public ReportMap getReport() {
            ReportMap report = new ReportMap();
            report.put( MEAN_KEY, mean_ );
            report.put( STDEV_KEY, sigma_ );
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
