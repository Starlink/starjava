package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.util.Map;
import javax.swing.Icon;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot.Shaders;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Drawing;
import uk.ac.starlink.ttools.plot2.LayerOpt;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.ReportKey;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.ReportMeta;
import uk.ac.starlink.ttools.plot2.Span;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.ColorConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.DoubleConfigKey;
import uk.ac.starlink.ttools.plot2.config.IntegerConfigKey;
import uk.ac.starlink.ttools.plot2.config.SliderSpecifier;
import uk.ac.starlink.ttools.plot2.config.Specifier;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.geom.CubeSurface;
import uk.ac.starlink.ttools.plot2.geom.GPoint3D;
import uk.ac.starlink.ttools.plot2.geom.SphereNet;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType;
import uk.ac.starlink.ttools.plot2.paper.PaperType3D;

/**
 * Plotter that can draw a spherical grid around the origin of
 * a CubeSurface.
 *
 * @author   Mark Taylor
 * @since    15 Aug 2018
 */
public class SphereGridPlotter
             extends AbstractPlotter<SphereGridPlotter.GridStyle> {

    /** Config key for grid line colour. */
    public static final ConfigKey<Color> COLOR_KEY =
        new ColorConfigKey( ColorConfigKey
                           .createColorMeta( "gridcolor", "Grid Color",
                                             "the spherical grid" ),
                            ColorConfigKey.COLORNAME_GREY, false );

    /** Config key for grid line thickness. */
    public static final ConfigKey<Integer> THICK_KEY =
       StyleKeys.createThicknessKey( 1 );

    /** Config key for sphere radius. */
    public static final ConfigKey<Double> RADIUS_KEY = createRadiusKey();

    /** Config key for number of longitude lines drawn. */
    public static final ConfigKey<Integer> NLON_KEY =
        IntegerConfigKey.createSliderKey(
            new ConfigMeta( "nlon", "Lon Count" )
           .setShortDescription( "Number of longitude lines" )
           .setXmlDescription( new String[] {
                "<p>Number of longitude great circles to plot",
                "in the spherical grid.",
                "Since each great circle joins the poles in two hemispheres,",
                "this value is half the number of meridians to be drawn.",
                "</p>",
            } )
        , 3, 0, 24, false );

    /** Config key determining number of latitude lines drawn. */
    public static final ConfigKey<Integer> NLAT_KEY =
        IntegerConfigKey.createSliderKey(
            new ConfigMeta( "nlat", "Lat Count" )
           .setShortDescription( "Number of latitude lines above equator" )
           .setXmlDescription( new String[] {
                "<p>Number of latitude lines to plot both above and below",
                "the equator in the spherical grid.",
                "A value of zero plots just the equator,",
                "and negative values plot no parallels at all.",
                "</p>",
            } )
        , 2, -1, 16, false );

    /** Report key for actual radius value. */
    private static final ReportKey<Double> RADIUS_REPKEY =
        ReportKey.createDoubleKey( new ReportMeta( "radius", "Radius" ),
                                   false );

    /**
     * Constructor.
     */
    public SphereGridPlotter() {
        super( "SphereGrid", ResourceIcon.PLOT_SKYGRID );
    }

    public String getPlotterDescription() {
        return PlotUtil.concatLines( new String[] {
            "<p>Plots a spherical grid around the origin of a 3-d plot.",
            "The radius of the sphere can be configured explicitly,",
            "otherwise a suitable default value",
            "(that should make at least some of the grid visible)",
            "will be chosen.",
            "</p>",
        } );
    }

    public ConfigKey<?>[] getStyleKeys() {
        return new ConfigKey<?>[] {
            RADIUS_KEY,
            COLOR_KEY,
            THICK_KEY,
            NLON_KEY,
            NLAT_KEY,
        };
    }

    public GridStyle createStyle( ConfigMap config ) {
        Color color = config.get( COLOR_KEY );
        double radius = config.get( RADIUS_KEY ).doubleValue();
        int thick = config.get( THICK_KEY );
        int pixgap = 0;
        int nlon = config.get( NLON_KEY );
        int nlat = config.get( NLAT_KEY );
        double bgFade = 0.7;
        return new GridStyle( color, radius, thick, pixgap,
                              nlon, nlat, bgFade );
    }

    public PlotLayer createLayer( DataGeom geom, DataSpec dataSpec,
                                  final GridStyle style ) {
        final LayerOpt layerOpt = new LayerOpt( style.color_, true );
        return new AbstractPlotLayer( this, geom, dataSpec, style, layerOpt ) {
            public Drawing createDrawing( Surface surf,
                                          Map<AuxScale,Span> auxSpans,
                                          PaperType paperType ) {
                final CubeSurface csurf = (CubeSurface) surf;
                final PaperType3D ptype = (PaperType3D) paperType;
                final int thick = style.thick_;
                final int pixgap = style.pixgap_;
                final Color fg = style.color_;
                final Color bg = fade( fg, style.bgFade_ );
                final double radius = style.radius_;
                final int nlon = style.nlon_;
                final int nlat = style.nlat_;
                GPoint3D gpos0 = new GPoint3D();
                csurf.dataToGraphicZ( new double[ 3 ], false, gpos0 );
                final double z0 = gpos0.z;
                final SphereNet net = new SphereNet( csurf );
                final double r = radius > 0 ? radius
                                            : -radius * net.getDefaultRadius();
                final ReportMap report = new ReportMap();
                report.put( RADIUS_REPKEY, Double.valueOf( r ) );
                return new UnplannedDrawing() {
                    protected void paintData( final Paper paper,
                                              DataStore dataStore ) {
                        if ( nlat >= 0 ) {
                            paintLines( paper,
                                        net.getLatitudeLines( r, nlat ) );
                        }
                        paintLines( paper, net.getLongitudeLines( r, nlon ) );
                    }
                    void paintLines( Paper paper, SphereNet.Line3d[] lines ) {
                        GPoint3D gpos = new GPoint3D();
                        boolean sameColor = fg.equals( bg );
                        for ( Iterable<double[]> line : lines ) {
                            LineTracer3D tracer =
                                LineTracer3D
                               .createTracer( ptype, paper, csurf, thick,
                                              pixgap );
                            for ( double[] dpos : line ) {

                                /* This dataToGraphicZ call is made twice
                                 * unfortunately, once here and once during
                                 * the line painting. */
                                boolean isFront =
                                      sameColor
                                   || ( csurf.dataToGraphicZ( dpos, true, gpos )
                                        && gpos.z < z0 );
                                Color color = isFront ? fg : bg;
                                tracer.addPoint( dpos, color );
                            }
                        }
                    }
                    @Override
                    public ReportMap getReport( Object plan ) {
                        return report;
                    }
                };
            }
        };
    }

    /**
     * Fades a colour to white by a given factor.
     *
     * @param  color   input colour
     * @param  factor   fadedness in range 0..1
     * @return  faded colour
     */
    private static Color fade( Color color, double factor ) {
        float[] rgba = color.getColorComponents( new float[ 4 ] );
        Shaders.FADE_WHITE.adjustRgba( rgba, (float) factor );
        return new Color( rgba[ 0 ], rgba[ 1 ], rgba[ 2 ] );
    }

    /**
     * Creates the configuration key for sphere radius.
     *
     * @return  new config key
     */
    private static ConfigKey<Double> createRadiusKey() {
        ConfigMeta cmeta = new ConfigMeta( "radius", "Radius" );
        cmeta.setShortDescription( "Sphere grid radius" );
        cmeta.setXmlDescription( new String[] {
            "<p>Defines the radius of the spherical grid",
            "that is drawn around the origin.",
            "Positive values give the radius in data units,",
            "negative values provide a multiplier for the default radius",
            "which is chosen on the basis of the currently visible",
            "volume.",
            "</p>",
        } );
        return new DoubleConfigKey( cmeta, -1 ) {
            public Specifier<Double> createSpecifier() {
                return new SliderSpecifier( -5, -0.1, false, -1, true,
                                            SliderSpecifier.TextOption.ENTER ) {
                    @Override
                    public void submitReport( ReportMap report ) {
                        super.submitReport( report );
                        if ( isSliderActive() ) {
                            Double objval = report == null
                                          ? null
                                          : report.get( RADIUS_REPKEY );
                            double dval = objval == null
                                        ? Double.NaN
                                        : objval.doubleValue();
                            getTextField().setText( formatRadius( dval ) );
                            getTextField().setCaretPosition( 0 );
                        }
                    }
                };
            }
        };
    }

    /**
     * Formats a radius value for presentation in the text entry part
     * of a slider specifier.  The idea is not to provide too much precision,
     * since it only comes from slider position.  This is not a very
     * careful implementation, but it's better than nothing.
     *
     * @param  r  radius
     * @return   text representation for presentation in a small-width field
     */
    private static String formatRadius( double r ) {
        if ( Double.isNaN( r ) ) {
            return "";
        }
        else if ( r > 100 ) {
            return Long.toString( (long) r );
        }
        else {
            return Float.toString( (float) r );
        }
    }

    /**
     * Style class for sphere grid plotter.
     */
    public static class GridStyle implements Style {

        private final Color color_;
        private final double radius_;
        private final int thick_;
        private final int pixgap_;
        private final int nlon_;
        private final int nlat_;
        private final double bgFade_;

        /**
         * Constructor.
         *
         * @param  color  basic grid colour
         * @param  radius  radius of sphere in data units
         * @param  thick   line thickness
         * @param  pixgap  pixel gap for dotted lines
         * @param  nlon    number of longitude lines
         * @param  nlat    number of latitude lines per hemisphere
         * @param  bgFade  fading amount to generate background colour
         */
        public GridStyle( Color color, double radius, int thick, int pixgap,
                          int nlon, int nlat, double bgFade ) {
            color_ = color;
            radius_ = radius;
            thick_ = thick;
            pixgap_ = pixgap;
            nlon_ = nlon;
            nlat_ = nlat;
            bgFade_ = bgFade;
        }

        public Icon getLegendIcon() {
            return ResourceIcon.PLOT_SKYGRID;
        }

        @Override
        public int hashCode() {
            int code = 55401;
            code = 23 * code + color_.hashCode();
            code = 23 * code + Float.floatToIntBits( (float) radius_ );
            code = 23 * code + thick_;
            code = 23 * code + pixgap_;
            code = 23 * code + nlon_;
            code = 23 * code + nlat_;
            code = 23 * code + Float.floatToIntBits( (float) bgFade_ );
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof GridStyle ) {
                GridStyle other = (GridStyle) o;
                return this.color_.equals( other.color_ )
                    && this.radius_ == other.radius_
                    && this.thick_ == other.thick_
                    && this.pixgap_ == other.pixgap_
                    && this.nlon_ == other.nlon_
                    && this.nlat_ == other.nlat_
                    && this.bgFade_ == other.bgFade_;
            }
            else {
                return false;
            }
        }
    }
}
