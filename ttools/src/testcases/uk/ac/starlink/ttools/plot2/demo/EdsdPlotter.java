package uk.ac.starlink.ttools.plot2.demo;

import gaia.cu9.tools.parallax.PDF.ExpDecrVolumeDensityDEM;
import gaia.cu9.tools.parallax.PDF.PDF;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.Map;
import javax.swing.Icon;
import uk.ac.starlink.dpac.math.Edsd;
import uk.ac.starlink.dpac.math.FuncUtils;
import uk.ac.starlink.dpac.math.Function;
import uk.ac.starlink.dpac.math.NumericFunction;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.Axis;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Decal;
import uk.ac.starlink.ttools.plot2.Drawing;
import uk.ac.starlink.ttools.plot2.LayerOpt;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.Span;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.BooleanConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.DoubleConfigKey;
import uk.ac.starlink.ttools.plot2.config.MinimalConfigMeta;
import uk.ac.starlink.ttools.plot2.config.SliderSpecifier;
import uk.ac.starlink.ttools.plot2.config.Specifier;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.geom.PlanarSurface;
import uk.ac.starlink.ttools.plot2.layer.AbstractPlotLayer;
import uk.ac.starlink.ttools.plot2.layer.AbstractPlotter;
import uk.ac.starlink.ttools.plot2.layer.LineTracer;
import uk.ac.starlink.ttools.plot2.layer.UnplannedDrawing;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType;
import uk.ac.starlink.ttools.plot2.task.SimpleLayerType;

/**
 * Plotter implementation for playing with the Exponentially Decreasing
 * Space Density PDF.  Config options are provided for changing the
 * various parameters.
 *
 * <p>Topcat and stilts don't know about this, and it's not properly
 * auto-documented or fully configurable, it's just investigative
 * code not intended for public use.  You can invoke it, assuming
 * this class is on the class path, like:
 * <pre>
 *    stilts layer='uk.ac.starlink.ttools.plot2.demo.EdsdPlotter$EdsdLayerType'
 * </pre>
 * or
 * <pre>
 *    topcat -Dplot2.plotters=uk.ac.starlink.ttools.plot2.demo.EdsdPlotter
 * </pre>
 *
 * @author   Mark Taylor
 * @since    16 Mar 2018
 */
public class EdsdPlotter extends AbstractPlotter<EdsdPlotter.EdsdStyle> {

    public static final ConfigKey<Double> L_KEY =
        createSliderKey( new MinimalConfigMeta( "L" ), 1.350, .010, 10, true );
    public static final ConfigKey<Double> PLX_KEY =
        createSliderKey( new MinimalConfigMeta( "parallax" ),
                         40, 0.0001, 50, true );
    public static final ConfigKey<Boolean> NEGPLX_KEY =
        new BooleanConfigKey( new MinimalConfigMeta( "negplx" ), false );
    public static final ConfigKey<Double> EPLX_KEY =
        createSliderKey( new MinimalConfigMeta( "e_parallax" ),
                         3.3, 0.00001, 6, true );

    public EdsdPlotter() {
        super( "EDSD", ResourceIcon.TTOOLS_DOWHAT );
    }

    public String getPlotterDescription() {
        return "<p>Exponentionally Decreasing Space Density.</p>";
    }

    public ConfigKey[] getStyleKeys() {
        return new ConfigKey[] {
            L_KEY,
            PLX_KEY,
            NEGPLX_KEY,
            EPLX_KEY,
        };
    }

    public EdsdStyle createStyle( ConfigMap config ) {
        double l = config.get( L_KEY ).doubleValue();
        double plx = ( config.get( NEGPLX_KEY ).booleanValue() ? -1 : +1 )
                   * config.get( PLX_KEY ).doubleValue();
        double eplx = config.get( EPLX_KEY ).doubleValue();
        return new EdsdStyle( l, plx, eplx );
    }

    public PlotLayer createLayer( final DataGeom geom, final DataSpec dataSpec,
                                  final EdsdStyle style ) {
        LayerOpt opt = LayerOpt.NO_SPECIAL;
        return new AbstractPlotLayer( this, geom, dataSpec, style, opt ) {
            public void extendCoordinateRanges( Range[] ranges,
                                                boolean[] logFlags,
                                                DataStore dataStore ) {
            }
            public Drawing createDrawing( final Surface surface,
                                          Map<AuxScale,Span> auxSpans,
                                          final PaperType paperType ) {
                return new UnplannedDrawing() {
                    protected void paintData( Paper paper,
                                              DataStore dataStore ) {
                        paperType.placeDecal( paper, new Decal() {
                            public void paintDecal( Graphics g ) {
                                paintFunction( (Graphics2D) g,
                                               (PlanarSurface) surface, style );
                            }
                            public boolean isOpaque() {
                                return false;
                            }
                        } );
                    }
                };
            }
        };
    }

    private void paintFunction( Graphics2D g2, PlanarSurface surface,
                                EdsdStyle style ) {
        Rectangle bounds = surface.getPlotBounds();
        Axis xAxis = surface.getAxes()[ 0 ];
        Axis yAxis = surface.getAxes()[ 1 ];
        Color[] colors = {
            Color.CYAN,
            Color.BLUE,
            Color.RED,
        };
        Function[] functions = {
            // PDF from DPAC code.
            new NormPdf( style.dPdf_ ),

            // PDF from Edsd code - should be exactly the same.
            style.dEdsd_.getPdf(),

            // Reconstructed PDF, interpolated from the adaptively located
            // values used to characterise the CDF.  If the CDF quadrature
            // has followed the shape of the PDF carefully, this should be
            // close to the true PDFs.
            FuncUtils
           .interpolateQuadratic( style.dEdsd_
                                 .getSampledPdf( style.dEdsd_
                                                .calculateCdf( 1e-5 ) ) ),
        };
        int[] gxLimits = xAxis.getGraphicsLimits();
        for ( int i = 0; i < 3; i++ ) {
            Color color = colors[ i ];
            Function function = functions[ i ];
            LineTracer tracer =
                new LineTracer( g2, bounds, new BasicStroke( i == 0 ? 3 : 1 ),
                                true, 2000, true );
            for ( int gx = gxLimits[ 0 ]; gx < gxLimits[ 1 ]; gx++ ) {
                double dx = xAxis.graphicsToData( gx );
                double dy = function.f( dx );
                double gy = yAxis.dataToGraphics( dy );
                tracer.addVertex( gx, gy, color );
                if ( function instanceof NormPdf ) {
                    double rMode = ((NormPdf) function).rMode_;
                    Color color0 = g2.getColor();
                    Stroke stroke0 = g2.getStroke();
                    g2.setColor( Color.GRAY );
                    g2.setStroke( new BasicStroke( 1, BasicStroke.CAP_ROUND,
                                                   BasicStroke.JOIN_ROUND,
                                                   5f, new float[] { 4, 4 },
                                                   2f ) );
                    int gxMode = (int) xAxis.dataToGraphics( rMode );
                    int gyMode = (int) yAxis.dataToGraphics( 1.0 );
                    int gy0 = (int) yAxis.dataToGraphics( 0.0 );
                    g2.drawLine( gxMode, gy0, gxMode, gyMode );
                    g2.setColor( color0 );
                    g2.setStroke( stroke0 );
                }
            }
            tracer.flush();
        }
    }

    private static class NormPdf implements Function {
        final PDF pdf_;
        final double scale_;
        final double rMode_;
        NormPdf( PDF pdf ) {
            pdf_ = pdf;
            rMode_ = pdf.getBestEstimation();
            scale_ = 1.0 / pdf.getUnnormalizedProbabilityAt( rMode_ );
        }
        public double f( double x ) {
            return scale_ * pdf_.getUnnormalizedProbabilityAt( x );
        }
    }

    private static ConfigKey<Double> createSliderKey( ConfigMeta meta,
                                                      final double dflt,
                                                      final double lo,
                                                      final double hi,
                                                      final boolean log ) {
        return new DoubleConfigKey( meta, dflt ) {
            public Specifier<Double> createSpecifier() {
                return new SliderSpecifier( lo, hi, log, dflt, false,
                                            SliderSpecifier.TextOption
                                                           .ENTER_ECHO );
            }
        };
    }

    public static class EdsdStyle implements Style {
        private final double l_;
        private final double plx_;
        private final double eplx_;
        private final PDF dPdf_;
        private final PDF mPdf_;
        private final Edsd dEdsd_;
        EdsdStyle( double l, double plx, double eplx ) {
            l_ = l;
            plx_ = plx;
            eplx_ = eplx;
            ExpDecrVolumeDensityDEM dem =
                new ExpDecrVolumeDensityDEM( plx, eplx, l );
            dPdf_ = dem.getDistancePDF();
            mPdf_ = dem.getDistanceModulusPDF();
            dEdsd_ = new Edsd( plx, eplx, l );
        }
        public Icon getLegendIcon() {
            return ResourceIcon.TTOOLS_DOWHAT;
        }
        @Override
        public int hashCode() {
            int code = 88921;
            code = 23 * code + Float.floatToIntBits( (float) l_ );
            code = 23 * code + Float.floatToIntBits( (float) plx_ );
            code = 23 * code + Float.floatToIntBits( (float) eplx_ );
            return code;
        }
        @Override
        public boolean equals( Object o ) {
            if ( o instanceof EdsdStyle ) {
                EdsdStyle other = (EdsdStyle) o;
                return this.l_ == other.l_
                    && this.plx_ == other.plx_
                    && this.eplx_ == other.eplx_;
            }
            else {
                return false;
            }
        }
        @Override
        public String toString() {
            return "(" + l_ + ", " + plx_ + ", " + eplx_ + ")";
        }
    }

    public static class EdsdLayerType extends SimpleLayerType {
        public EdsdLayerType() {
            super( new EdsdPlotter() );
        }
    }
}
