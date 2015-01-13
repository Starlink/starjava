package uk.ac.starlink.ttools.plot2.layer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Decal;
import uk.ac.starlink.ttools.plot2.Drawing;
import uk.ac.starlink.ttools.plot2.LayerOpt;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotUtil;
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
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.ttools.plot2.data.InputMeta;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;
import uk.ac.starlink.ttools.plot2.geom.PlaneSurface;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType;

/**
 * Fits a set of 2-d points to a linear equation, and plots the line.
 *
 * @author   Mark Taylor
 * @since    8 Dec 2014
 */
public class LinearFitPlotter extends AbstractPlotter<LineStyle> {

    /** Report key for coefficients of linear fit (2 element array (c, m)). */
    public static final ReportKey<double[]> COEFFS_KEY =
        new ReportKey<double[]>( new ReportMeta( "coeffs", "Coefficients" ),
                                 double[].class, false );

    /** Report key for text of linear fit equation. */
    public static final ReportKey<String> EQUATION_KEY =
        new ReportKey<String>( new ReportMeta( "equation", "Equation" ),
                               String.class, true );

    /** Report key for product moment correlation coefficient. */
    public static final ReportKey<Double> CORRELATION_KEY =
        new ReportKey<Double>( new ReportMeta( "correlation", "Correlation" ),
                               Double.class, true );

    /** Report key for order zero polynomial coefficient. */
    private static final ReportKey<Double> C0_KEY =
        new ReportKey<Double>( new ReportMeta( "c", "c" ), Double.class, true );

    /** Report key for order one polynomial coefficient. */
    private static final ReportKey<Double> C1_KEY =
        new ReportKey<Double>( new ReportMeta( "m", "m" ), Double.class, true );

    private static final FloatingCoord WEIGHT_COORD =
        FloatingCoord.createCoord(
            new InputMeta( "weight", "Weight" )
           .setShortDescription( "Weight for line fitting" )
           .setXmlDescription( new String[] {
                "<p>The weight associated with each data point",
                "for fitting purposes.",
                "This is used for calculating the coefficients of",
                "the line of best fit, and the correlation coefficient.",
                "If no coordinate is supplied, all points are assumed to",
                "have equal weight (1).",
                "Otherwise, any point with a null weight value",
                "is assigned a weight of zero, i.e. ignored.",
                "</p>",
                "<p>Given certain assumptions about independence of samples,",
                "a suitable value for the weight may be",
                "<code>1/(err*err)</code>, if <code>err</code> is the",
                "measurement error for each Y value.",
                "</p>",
            } )
        , false );

    /**
     * Constructor.
     *
     * @param  hasWeights  true if points may be weighted
     */
    public LinearFitPlotter( boolean hasWeights ) {
        super( "LinearFit", ResourceIcon.FORM_LINEARFIT,
               CoordGroup
              .createCoordGroup( 1, hasWeights ? new Coord[] { WEIGHT_COORD }
                                               : new Coord[ 0 ] ),
               true );
    }

    public String getPlotterDescription() {
        return PlotUtil.concatLines( new String[] {
            "<p>Plots a line of best fit for the data points.",
            "</p>",
        } );
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
        final CoordGroup cgrp = getCoordGroup();
        return new AbstractPlotLayer( this, geom, dataSpec, style, layerOpt ) {
            public Drawing createDrawing( Surface surface,
                                          Map<AuxScale,Range> auxRanges,
                                          PaperType paperType ) {
                return new LinearFitDrawing( (PlaneSurface) surface, geom,
                                             dataSpec, cgrp, style, paperType );
            }
        };
    }

    /**
     * Log function, used for transforming X/Y values to values for fitting.
     *
     * @param  val  value
     * @return  log to base 10 of <code>val</code>
     */
    private static double log( double val ) {
        return Math.log10( val );
    }

    /**
     * Inverse of log function.
     *
     * @param  val   value
     * @return   ten to the power of <code>val</code>
     */
    private static double unlog( double val ) {
        return Math.pow( 10, val );
    }

    /**
     * Drawing for linear fit.
     */
    private static class LinearFitDrawing implements Drawing {

        private final PlaneSurface surface_;
        private final DataGeom geom_;
        private final DataSpec dataSpec_;
        private final CoordGroup cgrp_;
        private final LineStyle style_;
        private final PaperType paperType_;

        /**
         * Constructor.
         *
         * @param  surface   plotting surface
         * @param  geom      maps position coordinates to graphics positions
         * @param  dataSpec  data points to fit
         * @param  cgrp      plotter coord group
         * @param  style     line plotting style
         * @param  paperType  paper type
         */
        LinearFitDrawing( PlaneSurface surface, DataGeom geom,
                          DataSpec dataSpec, CoordGroup cgrp, LineStyle style,
                          PaperType paperType ) {
            surface_ = surface;
            geom_ = geom;
            dataSpec_ = dataSpec;
            cgrp_ = cgrp;
            style_ = style;
            paperType_ = paperType;
        }

        public Object calculatePlan( Object[] knownPlans,
                                     DataStore dataStore ) {
            boolean[] logFlags = surface_.getLogFlags();

            /* If one of the known plans matches the one we're about
             * to calculate, just return that. */
            for ( Object knownPlan : knownPlans ) {
                if ( knownPlan instanceof LinearFitPlan &&
                     ((LinearFitPlan) knownPlan)
                                     .matches( dataSpec_, logFlags ) ) {
                    return knownPlan;
                }
            }

            /* Otherwise, accumulate statistics and return the result. */
            WXYStats stats = new WXYStats();
            Point2D.Double gp = new Point2D.Double();
            boolean visibleOnly = false;
            boolean xlog = logFlags[ 0 ];
            boolean ylog = logFlags[ 1 ];
            TupleSequence tseq = dataStore.getTupleSequence( dataSpec_ );
            int icPos = cgrp_.getPosCoordIndex( 0, geom_ );
            final boolean hasWeight;
            final int icWeight;
            if ( cgrp_.getExtraCoords().length > 0 ) {
                icWeight = cgrp_.getExtraCoordIndex( 0, geom_ );
                hasWeight = ! dataSpec_.isCoordBlank( icWeight );
            }
            else {
                icWeight = -1;
                hasWeight = false;
            }
            double[] dpos = new double[ geom_.getDataDimCount() ];
            while ( tseq.next() ) {
                if ( geom_.readDataPos( tseq, icPos, dpos ) &&
                     surface_.dataToGraphics( dpos, visibleOnly, gp ) ) {
                    double x = xlog ? log( dpos[ 0 ] ) : dpos[ 0 ];
                    double y = ylog ? log( dpos[ 1 ] ) : dpos[ 1 ];
                    if ( hasWeight ) {
                        double w = tseq.getDoubleValue( icWeight );
                        stats.addPoint( x, y, w );
                    }
                    else {
                        stats.addPoint( x, y );
                    }
                }
            }
            return new LinearFitPlan( stats, dataSpec_, logFlags );
        }

        public void paintData( final Object plan, Paper paper,
                               DataStore dataStore ) {
            paperType_.placeDecal( paper, new Decal() {
                public void paintDecal( Graphics g ) {
                    ((LinearFitPlan) plan).paintLine( g, surface_, style_ );
                }
                public boolean isOpaque() {
                    return true;
                }
            } );
        }

        public ReportMap getReport( Object plan ) {
            return ((LinearFitPlan) plan).getReport();
        }
    }

    /**
     * Plan object encapsulating the inputs and results of a linear fit.
     */
    private static class LinearFitPlan {
        final WXYStats stats_;
        final DataSpec dataSpec_;
        final boolean[] logFlags_;

        /**
         * Constructor.
         *
         * @param  stats   bivariate statistics giving fit results
         * @param  dataSpec   characterisation of input data points 
         * @param  logFlags  2-element array giving true/false for X and Y
         *                   axis logarithmic/linear scaling
         */
        LinearFitPlan( WXYStats stats, DataSpec dataSpec, boolean[] logFlags ) {
            stats_ = stats;
            dataSpec_ = dataSpec;
            logFlags_ = logFlags;
        }

        /**
         * Indicates whether this object's state will be the same as
         * a plan calculated for the given input values.
         *
         * @param  dataSpec  characterisation of input data points 
         * @param  logFlags  2-element array giving true/false for X and Y
         *                   axis logarithmic/linear scaling
         */
        boolean matches( DataSpec dataSpec, boolean[] logFlags ) {
            return dataSpec.equals( dataSpec_ )
                && Arrays.equals( logFlags, logFlags_ );
        }

        /**
         * Plots the linear fit line for this fitting result.
         *
         * @param  g  graphics context
         * @param  surface  plot surface
         * @param  style   line style
         */
        void paintLine( Graphics g, PlaneSurface surface, LineStyle style ) {
            Rectangle bounds = surface.getPlotBounds();
            int gy0 = bounds.y;
            int gx1 = bounds.x - 10;
            int gx2 = bounds.x + bounds.width + 10;
            double dx1 =
                surface.graphicsToData( new Point( gx1, gy0 ), null )[ 0 ];
            double dx2 =
                surface.graphicsToData( new Point( gx2, gy0 ), null )[ 0 ];
            double[] coeffs = stats_.getLinearCoefficients();
            double dy1 = yFunction( dx1 );
            double dy2 = yFunction( dx2 );
            Point2D.Double gp1 = new Point2D.Double();
            Point2D.Double gp2 = new Point2D.Double();
            if ( surface.dataToGraphics( new double[] { dx1, dy1 },
                                         false, gp1 ) &&
                 surface.dataToGraphics( new double[] { dx2, dy2 },
                                         false, gp2 ) ) {
                LineTracer tracer =
                    style.createLineTracer( g, bounds, 2, false ); 
                tracer.addVertex( gp1.x, gp1.y );
                tracer.addVertex( gp2.x, gp2.y );
                tracer.flush();
            }
        }

        /**
         * Calculates the function y(x) defined by this plan's linear equation.
         *
         * @param   x  independent variable
         * @return  function evaluated at <code>x</code>
         */
        private double yFunction( double x ) {
            double[] coeffs = stats_.getLinearCoefficients();
            double y = coeffs[ 0 ]
                     + coeffs[ 1 ] * ( logFlags_[ 0 ] ? log( x ) : x );
            return logFlags_[ 1 ] ? unlog( y ) : y;
        }

        /**
         * Returns a plot report based on the state of this plan.
         *
         * @return  report
         */
        public ReportMap getReport() {
            double[] coeffs = stats_.getLinearCoefficients();
            String equation = new StringBuffer()
                .append( logFlags_[ 1 ] ? "log10(y)" : "y" )
                .append( " = " )
                .append( C1_KEY.getMeta().getShortName() )
                .append( " * " )
                .append( logFlags_[ 0 ] ? "log10(x)" : "x" )
                .append( " + " )
                .append( C0_KEY.getMeta().getShortName() )
                .toString();
            ReportMap report = new ReportMap();
            report.set( EQUATION_KEY, equation );
            report.set( C0_KEY, coeffs[ 0 ] );
            report.set( C1_KEY, coeffs[ 1 ] );
            report.set( CORRELATION_KEY, stats_.getCorrelation() );
            report.set( COEFFS_KEY, coeffs );
            return report; 
        }
    }

    /**
     * Accumulates bivariate statistics to calculate
     * weighted X-Y linear regression coefficients.
     */
    private static class WXYStats {
        private double sw_;
        private double swX_;
        private double swY_;
        private double swXX_;
        private double swYY_;
        private double swXY_;

        /**
         * Submits a data point with a given weight.
         *
         * @param  x  X coordinate
         * @param  y  Y coordinate
         * @param  w  weighting
         */
        public void addPoint( double x, double y, double w ) {
            if ( w > 0 && ! Double.isInfinite( w ) ) {
                sw_ += w;
                swX_ += w * x;
                swY_ += w * y;
                swXX_ += w * x * x;
                swYY_ += w * y * y;
                swXY_ += w * x * y;
            }
        }

        /**
         * Submits a data point with unit weight.
         *
         * @param  x  X coordinate
         * @param  y  Y coordinate
         */
        public void addPoint( double x, double y ) {
            sw_ += 1;
            swX_ += x;
            swY_ += y;
            swXX_ += x * x;
            swYY_ += y * y;
            swXY_ += x * y;
        }

        /**
         * Returns the polynomial coefficients of a linear regression line
         * for the submitted data.
         *
         * @return  2-element array: (intercept, gradient)
         */
        public double[] getLinearCoefficients() {
            double sw2x = sw_ * swXX_ - swX_ * swX_;
            double c = ( swXX_ * swY_ - swX_ * swXY_ ) / sw2x;
            double m = ( sw_ * swXY_ - swX_ * swY_ ) / sw2x;
            return new double[] { c, m };
        }

        /**
         * Returns the Pearson product moment correlation coefficient.
         *
         * @return  correlation coefficient
         */
        public double getCorrelation() {
            double sw2x = sw_ * swXX_ - swX_ * swX_;
            double sw2y = sw_ * swYY_ - swY_ * swY_;
            return ( sw_ * swXY_ - swX_ * swY_ ) / Math.sqrt( sw2x * sw2y );
        }
    }
}
