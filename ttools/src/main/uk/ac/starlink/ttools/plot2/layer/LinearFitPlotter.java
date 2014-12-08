package uk.ac.starlink.ttools.plot2.layer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot.XYStats;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Decal;
import uk.ac.starlink.ttools.plot2.Drawing;
import uk.ac.starlink.ttools.plot2.LayerOpt;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.PointCloud;
import uk.ac.starlink.ttools.plot2.SubCloud;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
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

    /**
     * Constructor.
     */
    public LinearFitPlotter() {
        super( "LinearFit", ResourceIcon.FORM_LINEARFIT, 1, new Coord[ 0 ] );
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

    public PlotLayer createLayer( DataGeom geom, DataSpec dataSpec,
                                  final LineStyle style ) {
        final PointCloud pointCloud =
            new PointCloud( new SubCloud( geom, dataSpec, 0 ) );
        LayerOpt layerOpt = new LayerOpt( style.getColor(), true );
        return new AbstractPlotLayer( this, geom, dataSpec, style, layerOpt ) {
            public Drawing createDrawing( Surface surface,
                                          Map<AuxScale,Range> auxRanges,
                                          PaperType paperType ) {
                return new LinearFitDrawing( (PlaneSurface) surface,
                                             pointCloud, style, paperType );
            }
        };
    }

    /**
     * Drawing for linear fit.
     */
    private static class LinearFitDrawing implements Drawing {

        private final PlaneSurface surface_;
        private final PointCloud cloud_;
        private final LineStyle style_;
        private final PaperType paperType_;

        /**
         * Constructor.
         *
         * @param  surface   plotting surface
         * @param  cloud     data points to fit
         * @param  style     line plotting style
         * @param  paperType  paper type
         */
        LinearFitDrawing( PlaneSurface surface, PointCloud cloud,
                          LineStyle style, PaperType paperType ) {
            surface_ = surface;
            cloud_ = cloud;
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
                     ((LinearFitPlan) knownPlan).matches( cloud_, logFlags ) ) {
                    return knownPlan;
                }
            }

            /* Otherwise, accumulate statistics and return the result. */
            XYStats stats = new XYStats( logFlags[ 0 ], logFlags[ 1 ] );
            Point gp = new Point();
            boolean visibleOnly = false;
            for ( double[] dpos : cloud_.createDataPosIterable( dataStore ) ) {
                if ( surface_.dataToGraphics( dpos, visibleOnly, gp ) ) {
                    stats.addPoint( dpos[ 0 ], dpos[ 1 ] );
                }
            }
            return new LinearFitPlan( stats, cloud_, logFlags );
        }

        public void paintData( final Object plan, Paper paper,
                               final DataStore dataStore ) {
            paperType_.placeDecal( paper, new Decal() {
                public void paintDecal( Graphics g ) {
                    ((LinearFitPlan) plan).paintLine( g, surface_, style_ );
                }
                public boolean isOpaque() {
                    return true;
                }
            } );
        }
    }

    /**
     * Plan object encapsulating the inputs and results of a linear fit.
     */
    private static class LinearFitPlan {
        final XYStats stats_;
        final PointCloud cloud_;
        final boolean[] logFlags_;

        /**
         * Constructor.
         *
         * @param  stats   bivariate statistics giving fit results
         * @param  cloud   characterisation of input data points 
         * @param  logFlags  2-element array giving true/false for X and Y
         *                   axis logarithmic/linear scaling
         */
        LinearFitPlan( XYStats stats, PointCloud cloud, boolean[] logFlags ) {
            stats_ = stats;
            cloud_ = cloud;
            logFlags_ = logFlags;
        }

        /**
         * Indicates whether this object's state will be the same as
         * a plan calculated for the given input values.
         *
         * @param  cloud   characterisation of input data points 
         * @param  logFlags  2-element array giving true/false for X and Y
         *                   axis logarithmic/linear scaling
         */
        boolean matches( PointCloud cloud, boolean[] logFlags ) {
            return cloud.equals( cloud_ )
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
            Point gp1 = new Point();
            Point gp2 = new Point();
            if ( surface.dataToGraphics( new double[] { dx1, dy1 },
                                         false, gp1 ) &&
                 surface.dataToGraphics( new double[] { dx2, dy2 },
                                         false, gp2 ) ) {
                LineTracer tracer = style.createLineTracer( g, bounds, 2 ); 
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
         * Log function.  This matches the one used by XYStats for 
         * calculating fit coefficients when axes are logarithmic
         * (that is it uses base 10).
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

        @Override
        public String toString() {
            double[] coeffs = stats_.getLinearCoefficients();
            return new StringBuffer()
                .append( logFlags_[ 1 ] ? "log10(y)" : "y" )
                .append( " = " )
                .append( coeffs[ 1 ] )
                .append( " * " )
                .append( logFlags_[ 0 ] ? "log10(x)" : "x" )
                .append( " + " )
                .append( coeffs[ 0 ] )
                .append( "; " )
                .append( "correlation = " )
                .append( stats_.getCorrelation() )
                .toString();
        }
    }
}
