package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;
import java.util.Map;
import javax.swing.Icon;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.Captioner;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Decal;
import uk.ac.starlink.ttools.plot2.Drawing;
import uk.ac.starlink.ttools.plot2.LayerOpt;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Span;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.geom.Rotation;
import uk.ac.starlink.ttools.plot2.geom.SkySurface;
import uk.ac.starlink.ttools.plot2.geom.SkySurfaceFactory;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.OptionConfigKey;
import uk.ac.starlink.ttools.plot2.config.SkySysConfigKey;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.geom.GridLiner;
import uk.ac.starlink.ttools.plot2.geom.SkyAxisLabeller;
import uk.ac.starlink.ttools.plot2.geom.SkyAxisLabellers;
import uk.ac.starlink.ttools.plot2.geom.SkySurfaceFactory;
import uk.ac.starlink.ttools.plot2.geom.SkySys;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType;

/**
 * Plotter that can draw a sky axis grid on a sky surface.
 * This can be for a different sky system than that defined by
 * the sky surface itself.
 *
 * @author   Mark Taylor
 * @since    10 Apr 2017
 */
public class SkyGridPlotter extends AbstractPlotter<SkyGridPlotter.GridStyle> {

    private static final String VIEWSYS_NAME =
        SkySurfaceFactory.VIEWSYS_KEY.getMeta().getShortName();

    /** Config key for grid line colour. */
    public static final ConfigKey<Color> COLOR_KEY =
        StyleKeys.GRID_COLOR;

    /** Config key for grid line transparency. */
    public static final ConfigKey<Double> TRANSPARENCY_KEY =
        StyleKeys.TRANSPARENCY;

    /** Config key for longitude grid line crowding. */
    public static final ConfigKey<Double> LONCROWD_KEY =
        StyleKeys.createCrowdKey(
            new ConfigMeta( "loncrowd", "Longitude Grid Crowding" )
           .setShortDescription( "Longitude grid line crowding" )
           .setXmlDescription( new String[] {
                "<p>Determines how closely sky longitude grid lines",
                "(meridians) are spaced.",
                "The default value is 1, meaning normal crowding.",
                "Larger values result in more grid lines,",
                "and smaller values in fewer grid lines.",
                "</p>",
            } ) );

    /** Config key for latitude grid line crowding. */
    public static final ConfigKey<Double> LATCROWD_KEY =
        StyleKeys.createCrowdKey(
            new ConfigMeta( "latcrowd", "Latitude Grid Crowding" )
           .setShortDescription( "Latitude grid line crowding" )
           .setXmlDescription( new String[] {
                "<p>Determines how closely sky latitude grid lines",
                "(parallels) are spaced.",
                "The default value is 1, meaning normal crowding.",
                "Larger values result in more grid lines,",
                "and smaller values in fewer grid lines.",
                "</p>",
            } ) );

    /** Config key for grid sky system. */
    public static final ConfigKey<SkySys> GRIDSYS_KEY =
        new SkySysConfigKey(
            new ConfigMeta( "gridsys", "Sky System" )
           .setShortDescription( "Sky coordinate system for grid layer" )
           .setXmlDescription( new String[] {
                "<p>The sky coordinate system used for the additional",
                "grid axes.",
                "This is used in conunction with the",
                "<code>" + VIEWSYS_NAME + "</code> parameter",
                "defined for the plot as a whole",
                "to determin what grid lines to plot.",
                "</p>",
            } )
        , false )
       .setOptionUsage()
       .addOptionsXml();

    /** Config key for grid label positioning. */
    public static final ConfigKey<SkyAxisLabeller> LABELLER_KEY =
        new OptionConfigKey<SkyAxisLabeller>(
            new ConfigMeta( "labelpos", "Label Position" )
           .setShortDescription( "Position of grid axis labels" )
           .setXmlDescription( new String[] {
                "<p>Controls how and whether the numeric annotations",
                "of the lon/lat axes are displayed.",
                "</p>",
            } )
        , SkyAxisLabeller.class,
          new SkyAxisLabeller[] {
              SkyAxisLabellers.INTERNAL,
              SkyAxisLabellers.NONE,
          }
        , SkyAxisLabellers.INTERNAL, true ) {
            public String valueToString( SkyAxisLabeller labeller ) {
                return labeller.getLabellerName();
            }
            public String getXmlDescription( SkyAxisLabeller labeller ) {
                return labeller.getLabellerDescription();
            }
        }
       .setOptionUsage()
       .addOptionsXml();

    /**
     * Constructor.
     */
    public SkyGridPlotter() {
        super( "SkyGrid", ResourceIcon.PLOT_SKYGRID );
    }

    public String getPlotterDescription() {
        return PlotUtil.concatLines( new String[] {
            "<p>Plots an additional axis grid on the celestial sphere.",
            "This can be overlaid on the default sky axis grid",
            "so that axes for multiple sky coordinate systems",
            "are simultaneously visible.",
            "The plots are done relative to the View sky system",
            "(<code>" + VIEWSYS_NAME + "</code> parameter)",
            "defined for the plot as a whole.",
            "</p>",
            "<p>Note that some of the configuration items for this plotter,",
            "such as grid line antialiasing and the decimal/sexagesimal flag,",
            "are inherited from the values set for the main sky plot grid.",
            "</p>",
        } );
    }

    public ConfigKey<?>[] getStyleKeys() {
        return new ConfigKey<?>[] {
            GRIDSYS_KEY,
            COLOR_KEY,
            TRANSPARENCY_KEY,
            LABELLER_KEY,
            LONCROWD_KEY,
            LATCROWD_KEY,
        };
    }

    public GridStyle createStyle( ConfigMap config ) {

        /* Acquire config items from global config.
         * Note that the plotting UI (STILTS or TOPCAT) has to make
         * special arrangements to ensure that VIEWSYS_KEY is included
         * in the config map, since it is a global value for the plot,
         * not specified per layer, and therefore is not declared
         * as one of this plotter's style keys. */
        SkySys viewsys = config.get( SkySurfaceFactory.VIEWSYS_KEY );
        Captioner captioner = StyleKeys.CAPTIONER.createValue( config );
        boolean antialias =
            config.get( StyleKeys.GRID_ANTIALIAS ).booleanValue();
        boolean sexagesimal =
            config.get( SkySurfaceFactory.SEX_KEY ).booleanValue();

        /* Acquire config items from layer config. */
        SkySys gridsys = config.get( GRIDSYS_KEY );
        Color color =
            StyleKeys.getAlphaColor( config, COLOR_KEY, TRANSPARENCY_KEY );
        SkyAxisLabeller labeller = config.get( LABELLER_KEY );
        double loncrowd = config.get( LONCROWD_KEY ).doubleValue();
        double latcrowd = config.get( LATCROWD_KEY ).doubleValue();

        /* Turn config items into a GridStyle instance.
         * Plotting a sky grid only makes sense if both the grid system
         * and the view system are known, so make sure that the thing
         * fails if either is undefined (which shouldn't happen). */
        Rotation rotation = gridsys == null || viewsys == null
                          ? null
                          : Rotation.createRotation( gridsys, viewsys );
        return new GridStyle( rotation, color, labeller, captioner,
                              sexagesimal, loncrowd, latcrowd, antialias );
    }

    public PlotLayer createLayer( DataGeom geom, DataSpec dataSpec,
                                  final GridStyle style ) {
        final LayerOpt layerOpt = style.getLayerOpt();
        return new AbstractPlotLayer( this, geom, dataSpec, style, layerOpt ) {
            public Drawing createDrawing( Surface surf,
                                          Map<AuxScale,Span> auxSpans,
                                          final PaperType paperType ) {
                final SkySurface skySurf = (SkySurface) surf;
                return new UnplannedDrawing() {
                    protected void paintData( Paper paper,
                                              DataStore dataStore ) {
                        paperType.placeDecal( paper, new Decal() {
                            public void paintDecal( Graphics g ) {
                                paintGrid( skySurf, style, g );
                            }
                            public boolean isOpaque() {
                                return layerOpt.isOpaque();
                            }
                        } );
                    }
                };
            }
        };
    }

    /**
     * Perform grid painting.
     *
     * @param  surf   sky surface
     * @param  style  style
     * @param  g      target graphics context
     */
    private void paintGrid( SkySurface surf, GridStyle style, Graphics g ) {
        GridLiner gl = surf.createGridder( style.rotation_, style.sexagesimal_,
                                           style.loncrowd_, style.latcrowd_ );
        if ( gl == null ) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor( style.color_ );
        g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
                             style.antialias_
                                   ? RenderingHints.VALUE_ANTIALIAS_ON
                                   : RenderingHints.VALUE_ANTIALIAS_OFF );
        for ( double[][] line : gl.getLines() ) { 
            int nseg = line.length;
            GeneralPath path =
                new GeneralPath( GeneralPath.WIND_NON_ZERO, nseg );
            double[] seg0 = line[ 0 ];
            path.moveTo( (float) seg0[ 0 ], (float) seg0[ 1 ] );
            for ( double[] seg : line ) {
                path.lineTo( (float) seg[ 0 ], (float) seg[ 1 ] );
            }
            g2.draw( path );
        }
        style.labeller_.createAxisAnnotation( gl, style.captioner_ )
                       .drawLabels( g2 );
    }

    /**
     * Style for configuring the grid plot.
     */
    public static class GridStyle implements Style {
        private final Rotation rotation_;
        private final Color color_;
        private final SkyAxisLabeller labeller_;
        private final Captioner captioner_;
        private final boolean sexagesimal_;
        private final double loncrowd_;
        private final double latcrowd_;
        private final boolean antialias_;

        /**
         * Constructor.
         *
         * @param  rotation  rotation from plot basic orientation
         * @param  color     grid line colour
         * @param  labeller  grid line label positioner
         * @param  captioner grid line label captioner
         * @param  sexagesimal   true for sexagesimal, false for decimal
         * @param  loncrowd     longitude grid line crowding, 1 is normal
         * @param  latcrowd     latitude grid line crowding, 1 is normal
         * @param  antialias   true for antialiased lines
         */
        public GridStyle( Rotation rotation, Color color,
                          SkyAxisLabeller labeller, Captioner captioner,
                          boolean sexagesimal, double loncrowd, double latcrowd,
                          boolean antialias ) {
            rotation_ = rotation;
            color_ = color;
            labeller_ = labeller;
            captioner_ = captioner;
            sexagesimal_ = sexagesimal;
            loncrowd_ = loncrowd;
            latcrowd_ = latcrowd;
            antialias_ = antialias;
        }

        public Icon getLegendIcon() {
            return ResourceIcon.PLOT_SKYGRID;
        }

        /**
         * Reports layer option for this style.
         *
         * @return  layeropt
         */
        private LayerOpt getLayerOpt() {
            boolean isOpaque = (!antialias_) && color_.getAlpha() == 255;
            return new LayerOpt( color_, isOpaque );
        }

        @Override
        public int hashCode() {
            int code = 23225289;
            code = 23 * code + PlotUtil.hashCode( rotation_ );
            code = 23 * code + color_.hashCode();
            code = 23 * code + labeller_.hashCode();
            code = 23 * code + captioner_.hashCode();
            code = 23 * code + ( sexagesimal_ ? 11 : 13 );
            code = 23 * code + Float.floatToIntBits( (float) loncrowd_ );
            code = 23 * code + Float.floatToIntBits( (float) latcrowd_ );
            code = 23 * code + ( antialias_ ? 17 : 29 );
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof GridStyle ) {
                GridStyle other = (GridStyle) o;
                return PlotUtil.equals( this.rotation_, other.rotation_ )
                    && this.color_.equals( other.color_ )
                    && this.labeller_.equals( other.labeller_ )
                    && this.captioner_.equals( other.captioner_ )
                    && this.sexagesimal_ == other.sexagesimal_
                    && this.loncrowd_ == other.loncrowd_
                    && this.latcrowd_ == other.latcrowd_
                    && this.antialias_ == other.antialias_;
            }
            else {
                return false;
            }
        }
    }
}
