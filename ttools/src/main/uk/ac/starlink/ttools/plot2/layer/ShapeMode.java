package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.IndexColorModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import javax.swing.Icon;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.ttools.func.Tilings;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot.Shader;
import uk.ac.starlink.ttools.plot.Shaders;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.AuxReader;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.Axis;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Decal;
import uk.ac.starlink.ttools.plot2.Drawing;
import uk.ac.starlink.ttools.plot2.Equality;
import uk.ac.starlink.ttools.plot2.LayerOpt;
import uk.ac.starlink.ttools.plot2.Pixer;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Ranger;
import uk.ac.starlink.ttools.plot2.ReportKey;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.ReportMeta;
import uk.ac.starlink.ttools.plot2.Scaler;
import uk.ac.starlink.ttools.plot2.Scaling;
import uk.ac.starlink.ttools.plot2.Scalings;
import uk.ac.starlink.ttools.plot2.Span;
import uk.ac.starlink.ttools.plot2.Subrange;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.OptionConfigKey;
import uk.ac.starlink.ttools.plot2.config.RampKeySet;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.ttools.plot2.data.InputMeta;
import uk.ac.starlink.ttools.plot2.data.IntegerCoord;
import uk.ac.starlink.ttools.plot2.data.Tuple;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;
import uk.ac.starlink.ttools.plot2.geom.CubeSurface;
import uk.ac.starlink.ttools.plot2.geom.PlanarSurface;
import uk.ac.starlink.ttools.plot2.geom.SkySurface;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType;
import uk.ac.starlink.ttools.plot2.paper.PaperType2D;
import uk.ac.starlink.ttools.plot2.paper.PaperType3D;
import uk.ac.starlink.util.IconUtils;
import uk.ac.starlink.util.SplitCollector;

/**
 * Defines how outlines defined by a ShapeForm are coloured in a plot.
 * This abstract class contains several implementations (inner classes).
 * It's rather crowded, should possibly be split into several files.
 *
 * @author   Mark Taylor
 * @since    19 Feb 2013
 */
public abstract class ShapeMode implements ModePlotter.Mode {

    private final String name_;
    private final Icon icon_;
    private final Coord[] extraCoords_;
    private final boolean hasReports_;

    /**
     * Point count threshold above which some plots may be done by
     * preparing a pixel map rather than painting glyphs.
     */
    private static final int BIN_THRESH_2D = 50000;

    /**
     * Point count threshold resulting in always painting glyphs
     * and never using a pixel map.
     */
    private static final int NO_BINS = Integer.MAX_VALUE;

    /** Number of entries in a constructed colour map. */
    private static final int COLOR_MAP_SIZE = 128;

    /** Auto density mode, no user settings. */
    public static final ShapeMode AUTO = new AutoDensityMode();

    /** Simple flat mode for use with 2D plots.  */
    public static final ShapeMode FLAT2D = new FlatMode( false, BIN_THRESH_2D );

    /** Simple flat mode for use with 3D plots. */
    public static final ShapeMode FLAT3D = new FlatMode( false, NO_BINS );

    /** Transparency with automatic adjustment of opacity level. */
    public static final ShapeMode TRANSLUCENT = new AutoTransparentMode();

    /** Transparency with explicit opacity setting, suitable for 2D plots. */
    public static final ShapeMode TRANSPARENT2D = new FlatMode( true,
                                                                BIN_THRESH_2D );

    /** Transparency with explicit opacity setting, suitable for 3D plots. */
    public static final ShapeMode TRANSPARENT3D = new FlatMode( true, NO_BINS );

    /** Configurable density mode. */
    public static final ShapeMode DENSITY = new CustomDensityMode();

    /** Aux variable colouring mode with global colour map. */
    public static final ShapeMode AUX = new AuxShadingMode( true, false );

    /** Weighted density mode with global colour map. */
    public static final ShapeMode WEIGHTED = new WeightedDensityMode( false );

    /* Aux mode with private colour map. */
    public static final ShapeMode AUX_PRIVATE = new PrivateAuxMode( true );

    /* Weighted mode with private colour map. */
    public static final ShapeMode WEIGHTED_PRIVATE =
        new WeightedDensityMode( true );

    /** Flat RGB mode. */
    // For now this is not offered as one of the shading modes for 2D or 3D
    // plots.  I'm not sure if it's useful enough to include in the
    // visible list.  But it's available in principle.
    public static final ShapeMode IRGB = new RgbFlatMode( true );

    /** List of modes suitable for use with 2D plotting. */
    public static final ShapeMode[] MODES_2D = new ShapeMode[] {
        AUTO,
        FLAT2D,
        TRANSLUCENT,
        TRANSPARENT2D,
        DENSITY,
        AUX,
        WEIGHTED,
        AUX_PRIVATE,
        WEIGHTED_PRIVATE,
    };

    /** List of modes suitable for use with 3D plotting. */
    public static final ShapeMode[] MODES_3D = new ShapeMode[] {
        FLAT3D,
        TRANSLUCENT,
        TRANSPARENT3D,
        DENSITY,
        AUX,
        WEIGHTED,
        AUX_PRIVATE,
        WEIGHTED_PRIVATE,
    };

    /** Report key for pixel X dimension in data coordinates. */
    public static final ReportKey<Double> REPKEY_XPIX =
        ReportKey.createDoubleKey( new ReportMeta( "xpix_size",
                                                   "Pixel X dimension"
                                                + " in data coords" ),
                                   true );

    /** Report key for pixel Y dimension in data coordinates. */
    public static final ReportKey<Double> REPKEY_YPIX =
        ReportKey.createDoubleKey( new ReportMeta( "ypix_size",
                                                   "Pixel Y dimension"
                                                 + " in data coords" ),
                                   true );

    /** Report key for nominal pixel size in square degrees. */
    public static final ReportKey<Double> REPKEY_SKYPIX =
        ReportKey.createDoubleKey( new ReportMeta( "pixel_sqdeg",
                                                   "Pixel size in square "
                                                 + "degrees at proj center" ),
                                   true );

    /**
     * Constructor.
     *
     * @param  name  mode name
     * @param  icon  mode icon
     * @param  extraCoords  data coordinates associated with this mode
     *                      (not positional ones)
     * @param  hasReports  whether plot reports containing genera-interest
     *                     plot information are generated
     */
    public ShapeMode( String name, Icon icon, Coord[] extraCoords,
                      boolean hasReports ) {
        name_ = name;
        icon_ = icon;
        extraCoords_ = extraCoords;
        hasReports_ = hasReports;
    }

    public String getModeName() {
        return name_;
    }

    public Icon getModeIcon() {
        return icon_;
    }

    /**
     * Indicates whether the drawing produced by this mode willl return
     * general interest report information to upstream plotting code.
     *
     * @return  true if the plot report may contain interesting information
     */
    public boolean hasReports() {
        return hasReports_;
    }

    /**
     * Returns a description of this mode as an XML string.
     * The return value should be one or more &lt;p&gt; elements.
     *
     * @return  XML description of mode
     */
    public abstract String getModeDescription();

    /**
     * Returns the additional coordinates associated with this mode.
     *
     * @return   array of non-positional coordinates associated with colouring
     */
    public Coord[] getExtraCoords() {
        return extraCoords_;
    }

    /**
     * Returns style configuration keys associated with this mode.
     * These keys will be used in the config map supplied to
     * {@link #createStamper}.
     *
     * @return  array of config keys for mode
     */
    public abstract ConfigKey<?>[] getConfigKeys();

    /**
     * Returns an object which will do the work of colouring in shapes
     * when supplied with the appropriate style information and data.
     * The significant keys in the supplied config map are those
     * given by {@link #getConfigKeys}.
     *
     * @param   config  configuration map from which values for this mode's
     *                  config keys will be extracted
     * @return  new stamper object
     */
    public abstract Stamper createStamper( ConfigMap config );

    /**
     * Creates a plot layer.
     *
     * @param  plotter  plotter
     * @param  form   shape form
     * @param  geom   data coordinate specification
     * @param  dataSpec  data specification
     * @param  outliner  shape outliner
     * @param  stamper   shape stamper
     * @return   new layer
     */
    public abstract PlotLayer createLayer( ShapePlotter plotter, ShapeForm form,
                                           DataGeom geom, DataSpec dataSpec,
                                           Outliner outliner, Stamper stamper );

    /**
     * Returns a report describing pixel sizes for a given surface.
     * This information is required if you want to do quantitative
     * analysis on some of the weighted density modes.
     * What, if any, information is included is dependent on the surface type.
     *
     * @param  surface   plotting surface
     * @return  pixel size report 
     */
    private static ReportMap getPixelReport( Surface surface ) {
        ReportMap report = new ReportMap();
        if ( surface instanceof PlanarSurface ) {
            Axis[] axes = ((PlanarSurface) surface).getAxes();
            addPixelSize( report, REPKEY_XPIX, axes[ 0 ] );
            addPixelSize( report, REPKEY_YPIX, axes[ 1 ] );
        }
        else if ( surface instanceof SkySurface ) {
            SkySurface ssurf = (SkySurface) surface;
            Point p = ssurf.getSkyCenter();
            double[] p1 =
                ssurf.graphicsToData( new Point2D.Double( p.x - .5, p.y - .5 ),
                                      null );
            double[] p2 =
                ssurf.graphicsToData( new Point2D.Double( p.x + .5, p.y + .5 ),
                                      null );
            if ( p1 != null && p2 != null ) {
                double pixTheta = SkyDensityPlotter.vectorSeparation( p1, p2 )
                                / Math.sqrt( 2.0 );
                double pixSteradians = pixTheta * pixTheta;
                double pixSqdeg = pixSteradians / Tilings.SQDEG;
                report.put( REPKEY_SKYPIX, pixSqdeg );
            }
        }
        return report;
    }

    /**
     * Attempts to add a pixel dimension entry to a given report map
     * for a certain axis.
     *
     * @param  report  map to augment
     * @param  key   report key for new entry
     * @param  axis   axis along which dimension is to be reported
     */
    private static void addPixelSize( ReportMap report, ReportKey<Double> key,
                                      Axis axis ) {
        if ( axis.isLinear() ) {
            int g0 = axis.getGraphicsLimits()[ 0 ];
            double pixSize = Math.abs( axis.graphicsToData( g0 + 1 )
                                     - axis.graphicsToData( g0 ) );
            report.put( key, Double.valueOf( pixSize ) );
        }
    }

    /**
     * Mode for painting shapes in a single flat colour.
     */
    private static class FlatMode extends ShapeMode {
        private final int binThresh_;
        private final boolean transparent_;
        private static final ConfigKey<Double> OPAQUE_KEY =
            StyleKeys.createOpaqueKey( 4 );

        /**
         * Constructor.
         *
         * @param   transparent  true to use a transparent colour;
         *          in case of transparency, the appropriate config key
         *          will determine the amount of transparency
         * @param   binThresh  point count above which the output may
         *          be drawn as a single decal rather as multiple glyphs,
         *          if that looks sensible
         */
        FlatMode( boolean transparent, int binThresh ) {
            super( transparent ? "transparent" : "flat",
                   transparent ? ResourceIcon.MODE_ALPHA_FIX
                               : ResourceIcon.MODE_FLAT,
                   new Coord[ 0 ], false );
            transparent_ = transparent;
            binThresh_ = binThresh;
        }

        public String getModeDescription() {
            if ( transparent_ ) {
                return PlotUtil.concatLines( new String[] {
                    "<p>Paints markers in a transparent version of their",
                    "selected colour.",
                    "The degree of transparency is determined by",
                    "how many points are plotted on top of each other",
                    "and by the opaque limit.",
                    "The opaque limit fixes how many points must be",
                    "plotted on top of each other to completely obscure",
                    "the background.  This is set to a fixed value,",
                    "so a transparent level that works well for a crowded",
                    "region (or low magnification) may not work so well",
                    "for a sparse region (or when zoomed in).",
                    "</p>",
                } );
            }
            else {
                return "<p>Paints markers in a single fixed colour.</p>";
            }
        }

        public ConfigKey<?>[] getConfigKeys() {
            List<ConfigKey<?>> keyList = new ArrayList<ConfigKey<?>>();
            keyList.add( StyleKeys.COLOR );
            if ( transparent_ ) {
                keyList.add( OPAQUE_KEY );
            }
            return keyList.toArray( new ConfigKey<?>[ 0 ] );
        }

        public Stamper createStamper( ConfigMap config ) {
            final Color color;
            Color baseColor = config.get( StyleKeys.COLOR );
            if ( transparent_ ) {
                int opaque = config.get( OPAQUE_KEY ).intValue();
                float[] rgba = baseColor
                              .getRGBColorComponents( new float[ 4 ] );
                rgba[ 3 ] = 1f / opaque;
                color = new Color( rgba[ 0 ], rgba[ 1 ], rgba[ 2 ], rgba[ 3 ] );
            }
            else {
                color = baseColor;
            }
            return new FlatStamper( color );
        }

        public PlotLayer createLayer( ShapePlotter plotter, ShapeForm form,
                                      DataGeom geom, DataSpec dataSpec,
                                      Outliner outliner, Stamper stamper ) {
            final Color color = ((FlatStamper) stamper).color_;
            Style style = new ShapeStyle( outliner, stamper );
            LayerOpt opt = new LayerOpt( color, color.getAlpha() == 255 );
            return new ShapePlotLayer( plotter, geom, dataSpec, style, opt,
                                       outliner ) {
                public Drawing createDrawing( DrawSpec drawSpec ) {

                    /* For vector output you need to paint it.
                     * For bitmap output in 2D the output will be (roughly) the
                     * same either way, but painting will be faster for
                     * small point counts and binning will be faster for
                     * large ones.  So use an adaptive strategy. 
                     * Note that binning is (probably) done by the paper
                     * in any case, if appropriate.  The advantage of doing
                     * it here is that we know the shape of the markers,
                     * so we can fill the bins once and then move the resulting
                     * mask around, so that large points are plotted in
                     * (almost) the same time as small ones.  Probably only
                     * helps for rather large numbers.
                     * In the 3D case we don't do that binning here
                     * (enforced by high binThresh) since it interferes with
                     * the z-coordinate positioning (glyph placement has to
                     * be handled by the 3D paper).  */
                    return drawSpec.paperType_.isBitmap() && ! transparent_
                         ? new HybridFlatDrawing( drawSpec, color, binThresh_ )
                         : new PaintFlatDrawing( drawSpec, color );
                }
            };
        }

        /**
         * Flat mode Drawing implementation that paints a glyph at each point.
         */
        private static class PaintFlatDrawing extends UnplannedDrawing {
            private final DrawSpec drawSpec_;
            private final Color color_;

            /**
             * Constructor.
             *
             * @param  drawSpec  common drawing attributes
             * @param  color  fixed colour
             */
            PaintFlatDrawing( DrawSpec drawSpec, Color color ) {
                drawSpec_ = drawSpec;
                color_ = color;
            }

            public void paintData( Paper paper, DataStore dataStore ) {
                BiConsumer<TupleSequence,Paper> tuplePainter = (tseq, p) -> {
                    ShapePainter painter = drawSpec_.createPainter();
                    while ( tseq.next() ) {
                        painter.paintPoint( tseq, color_, p );
                    }
                };
                dataStore.getTupleRunner()
                         .paintData( tuplePainter, paper,
                                     drawSpec_.dataSpec_, dataStore );
            }

            @Override
            public ReportMap getReport( Object plan ) {
                return drawSpec_.outliner_.getReport( plan );
            }
        }

        /**
         * Flat mode drawing implementation that assembles a data point
         * density map for use in generating a decal.
         */
        private static class BinFlatDrawing extends BinShapeDrawing {
            private final Color color_;

            /**
             * Constructor.
             *
             * @param  drawSpec  common drawing attributes
             * @param  color  fixed colour
             */
            BinFlatDrawing( DrawSpec drawSpec, Color color ) {
                super( drawSpec );
                color_ = color;
            }

            PixelImage createPixelImage( Object plan ) {

                /* Assemble a counts map, but we only care if the values
                 * in it are zero or non-zero.  Don't trash the array
                 * since the plan may get re-used later. */
                int[] counts = getBinCounts( plan ).clone();
                int n = counts.length;
                for ( int i = 0; i < n; i++ ) {
                    counts[ i ] = Math.min( counts[ i ], 1 );
                }

                /* Colour it using a 2-colour colour model. */
                IndexColorModel colorModel =
                    PixelImage.createMaskColorModel( color_ );
                Dimension size = drawSpec_.surface_.getPlotBounds().getSize();
                return new PixelImage( size, counts, colorModel );
            }
        }

        /**
         * Flat mode drawing implementation that delegates to either
         * glyph or decal drawing methods depending on point count.
         */
        private static class HybridFlatDrawing implements Drawing {
            private final DrawSpec drawSpec_;
            private final int binThreshold_;
            private final PaintFlatDrawing paintDrawing_;
            private final BinFlatDrawing binDrawing_;
            private static final Object PAINT_PLAN = null;

            /**
             * Constructor.
             *
             * @param  drawSpec  common drawing attributes
             * @param  color  fixed colour
             * @param   binThresh  point count above which the output may
             *          be drawn as a single decal rather as multiple glyphs,
             *          if that looks sensible
             */
            HybridFlatDrawing( DrawSpec drawSpec, Color color,
                               int binThreshold ) {
                drawSpec_ = drawSpec;
                binThreshold_ = binThreshold;
                paintDrawing_ = new PaintFlatDrawing( drawSpec, color );
                binDrawing_ = new BinFlatDrawing( drawSpec, color );
            }

            public Object calculatePlan( Object[] knownPlans,
                                         DataStore dataStore ) {
                assert paintDrawing_.calculatePlan( knownPlans, dataStore )
                       == PAINT_PLAN;
                return binDrawing_.calculatePlan( knownPlans, dataStore );
            }

            public void paintData( Object binPlan, Paper paper,
                                   DataStore dataStore ) {
                long npoint = drawSpec_.outliner_.getPointCount( binPlan );
                if ( npoint < binThreshold_ ) {
                    paintDrawing_.paintData( PAINT_PLAN, paper, dataStore );
                }
                else {
                    binDrawing_.paintData( binPlan, paper, dataStore );
                }
            }

            public ReportMap getReport( Object plan ) {
                return drawSpec_.outliner_.getReport( plan );
            }
        }
    }

    /**
     * Mode for painting shapes according to a supplied RGB integer coordinate.
     */
    private static class RgbFlatMode extends ShapeMode {

        private final boolean transparent_;
        private static final ConfigKey<Color> RGB_NULLCOLOR_KEY =
            StyleKeys.createNullColorKey( "rgb", "RGB" );
        private static final ConfigKey<Double> RGB_OPAQUE_KEY =
            StyleKeys.createOpaqueKey( 1 );
        private static final IntegerCoord.IntType INT_TYPE =
            IntegerCoord.IntType.INT;
        private static final int BAD_RGB = INT_TYPE.getBadValue().intValue();
        private static final IntegerCoord RGB_COORD = new IntegerCoord(
            new InputMeta( "rgb", "RGB" )
           .setShortDescription( "RGB coordinate for shading; "
                               + "bits 0-7 give Red, 8-15 give Green, "
                               + "16-23 give Blue" ),
            false, INT_TYPE );

        /**
         * Constructor.
         *
         * @param  transparent  if true, there is the option to supply
         *                      a fixed opacity for the painted shapes
         */
        RgbFlatMode( boolean transparent ) {
            super( "RGB", ResourceIcon.MODE_RGB,
                   new Coord[] { RGB_COORD }, false );
            transparent_ = transparent;
        }

        public String getModeDescription() {
            StringBuffer sbuf = new StringBuffer()
               .append( "<p>Paints markers in the RGB colour\n" )
               .append( "explicitly given by a supplied integer coordinate.\n" )
               .append( "The RGB coordinate contains the Red, Green and Blue " )
               .append( "components of the plotted colour\n" )
               .append( "in bytes 2, 1 and 0, so for instance 0xff0040\n" )
               .append( "would represent a slighly bluish red.\n" )
               .append( "The highest order byte is ignored." );
            if ( transparent_ ) {
                sbuf.append( "You can also adjust the transparency\n" )
                    .append( "of the colours used.\n" );
            }
            sbuf.append( "</p>\n" );
            return sbuf.toString();
        }

        public ConfigKey<?>[] getConfigKeys() {
            List<ConfigKey<?>> list = new ArrayList<>();
            if ( transparent_ ) {
                list.add( RGB_OPAQUE_KEY );
            }
            list.add( RGB_NULLCOLOR_KEY );
            return list.toArray( new ConfigKey<?>[ 0 ] );
        }

        public Stamper createStamper( ConfigMap config ) {
            Color nullColor = config.get( RGB_NULLCOLOR_KEY );
            double opaque = transparent_ ? config.get( RGB_OPAQUE_KEY ) : 1;
            float alpha = 1f / (float) opaque;
            return new RgbStamper( alpha, nullColor );
        }

        public PlotLayer createLayer( ShapePlotter plotter,
                                      ShapeForm form,
                                      DataGeom geom,
                                      DataSpec dataSpec,
                                      Outliner outliner,
                                      Stamper stamper ) {
            int icRgb = plotter.getModeCoordsIndex( geom );
            RgbStamper rgbStamper = (RgbStamper) stamper;
            final float alpha = rgbStamper.alpha_;
            final Color nullColor = rgbStamper.nullColor_;
            Style style = new ShapeStyle( outliner, stamper );
            boolean isOpaque = alpha == 1f;
            LayerOpt opt = isOpaque ? LayerOpt.OPAQUE : LayerOpt.NO_SPECIAL;
            final Supplier<ColorKit> kitFact;
            if ( isOpaque ) {
                kitFact = () -> new ColorKit() {
                    public Color readColor( Tuple tuple ) {
                        int rgb = tuple.getIntValue( icRgb );
                        return rgb == BAD_RGB ? nullColor : new Color( rgb );
                    }
                };
            }
            else {
                final int alphaMask =
                   ( (int) Math.min( 255, Math.max( 0, alpha * 256 ) ) ) << 24;
                kitFact = () -> new ColorKit() {
                    public Color readColor( Tuple tuple ) {
                        int rgb = tuple.getIntValue( icRgb );
                        return rgb == BAD_RGB
                             ? nullColor
                             : new Color( rgb | alphaMask, true );
                    }
                };
            }
            return new ShapePlotLayer( plotter, geom, dataSpec, style, opt,
                                       outliner ) {
                Drawing createDrawing( DrawSpec drawSpec ) {
                    return drawSpec.createDrawing( kitFact );
                }
            };
        }

        /**
         * Stamper for use with RGB shading mode.
         */
        public static class RgbStamper implements Stamper {

            final float alpha_;
            final Color nullColor_;

            /**
             * Constructor.
             *
             * @param   alpha   alpha channel for painted colours
             * @parma   nullColor  colour to paint for blank RGB coordinate
             */
            public RgbStamper( float alpha, Color nullColor ) {
                alpha_ = alpha;
                nullColor_ = nullColor;
            }

            public Icon createLegendIcon( Outliner outliner ) {
                return outliner.getLegendIcon();
            }

            @Override
            public int hashCode() {
                int code = 813771;
                code = 23 * code + Float.floatToIntBits( alpha_ );
                code = 23 * code + PlotUtil.hashCode( nullColor_ );
                return code;
            }

            @Override
            public boolean equals( Object o ) {
                if ( o instanceof RgbStamper ) {
                    RgbStamper other = (RgbStamper) o;
                    return this.alpha_ == other.alpha_
                        && PlotUtil.equals( this.nullColor_, other.nullColor_ );
                }
                else {
                    return false;
                }
            }
        }
    }

    /**
     * Stamper implementation for flat colouring.
     */
    public static class FlatStamper implements Stamper {
        final Color color_;

        /**
         * Constructor.
         *
         * @param   color   fixed colour
         */
        public FlatStamper( Color color ) {
            color_ = color;
        }

        public Icon createLegendIcon( Outliner outliner ) {
            return IconUtils
                  .colorIcon( outliner.getLegendIcon(),
                              new Color( color_.getRGB(), false ) );
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof FlatStamper ) {
                FlatStamper other = (FlatStamper) o;
                return this.color_.equals( other.color_ );
            }
            else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return this.color_.hashCode();
        }
    }

    /**
     * Mode for painting shapes in a transparent colour where the transparency
     * level is adjusted automatically on the basis of how crowded the
     * plot is.
     */
    private static class AutoTransparentMode extends ShapeMode {

        /**
         * Constructor.
         */
        AutoTransparentMode() {
            super( "translucent", ResourceIcon.MODE_ALPHA,
                   new Coord[ 0 ], false );
        }

        public String getModeDescription() {
            return PlotUtil.concatLines( new String[] {
                "<p>Paints markers in a transparent version of their",
                "selected colour.",
                "The degree of transparency is determined by how many points",
                "are plotted on top of each other and by the transparency",
                "level.",
                "Unlike " + modeRef( TRANSPARENT2D ) + " mode,",
                "the transparency varies according to the average",
                "point density in the plot,",
                "so leaving the setting the same as you zoom in and out",
                "usually has a sensible effect.",
                "</p>",
            } );
        }

        public ConfigKey<?>[] getConfigKeys() {
            return new ConfigKey<?>[] {
                StyleKeys.COLOR,
                StyleKeys.TRANSPARENT_LEVEL,
            };
        }

        public Stamper createStamper( ConfigMap config ) {
            Color color = config.get( StyleKeys.COLOR );
            double level = config.get( StyleKeys.TRANSPARENT_LEVEL );
            return new AutoTransparentStamper( color, level );
        }

        public PlotLayer createLayer( ShapePlotter plotter,
                                      ShapeForm form,
                                      DataGeom geom,
                                      DataSpec dataSpec,
                                      Outliner outliner,
                                      Stamper stamper ) {
            final Color color = ((AutoTransparentStamper) stamper).color_;
            final double level = ((AutoTransparentStamper) stamper).level_;
            Style style = new ShapeStyle( outliner, stamper );
            LayerOpt opt = new LayerOpt( color, level == 0 );
            return new ShapePlotLayer( plotter, geom, dataSpec, style, opt,
                                       outliner ) {
                public Drawing createDrawing( DrawSpec drawSpec ) {
                    return new AutoTransparentDrawing( drawSpec, color, level );
                }
            };
        }

        /**
         * Auto transparent Drawing implementation.
         */
        private static class AutoTransparentDrawing implements Drawing {
            private final float[] rgb_;
            private final double level_;
            private final DrawSpec drawSpec_;

            /**
             * Constructor.
             *
             * @param  drawSpec  common drawing attributes
             * @param  base  (opaque) colour
             * @param  level  transparency modifier
             */
            AutoTransparentDrawing( DrawSpec drawSpec, Color color,
                                    double level ) {
                drawSpec_ = drawSpec;
                rgb_ = color.getRGBColorComponents( new float[ 3 ] );
                level_ = level;
            }

            public Object calculatePlan( Object[] knownPlans,
                                         DataStore dataStore ) {

                /* The plan could just be a histogram of point counts
                 * as far as this drawing is concerned.
                 * However, if we use a BinPlan we can reuse plans from
                 * other modes.  Not clear which is best. */
                return drawSpec_.calculateBinPlan( knownPlans, dataStore );
            }

            public void paintData( Object plan, Paper paper,
                                   DataStore dataStore ) {
                int[] counts = drawSpec_.outliner_.getBinCounts( plan );
                float alpha = (float) getAlpha( counts, level_ );
                final Color color =
                    new Color( rgb_[ 0 ], rgb_[ 1 ], rgb_[ 2 ], alpha );
                BiConsumer<TupleSequence,Paper> tuplePainter = ( tseq, p ) -> {
                    ShapePainter painter = drawSpec_.createPainter();
                    while ( tseq.next() ) {
                        painter.paintPoint( tseq, color, p );
                    }
                };
                dataStore.getTupleRunner()
                         .paintData( tuplePainter, paper,
                                     drawSpec_.dataSpec_, dataStore );
            }

            public ReportMap getReport( Object plan ) {
                return drawSpec_.outliner_.getReport( plan );
            }

            /**
             * Returns the alpha level to use given a specified transparency
             * level and a grid of pixel counts.
             *
             * @param  counts  pixel count array
             * @param  level  transparency level
             */
            private static double getAlpha( int[] counts, double level ) {
                int count = 0;
                int max = 0;
                int n = counts.length;
                for ( int i = 0; i < n; i++ ) {
                    int c = counts[ i ];
                    if ( c > 0 ) {
                        count++;
                        if ( c > max ) {
                            max = c;
                        }
                    }
                }
                double opaque = Math.max( 1, max * level );
                return 1f / opaque;
            }
        }
    }

    /**
     * Stamper implementation for auto transparency.
     */
    public static class AutoTransparentStamper implements Stamper {
        final Color color_;
        final double level_;

        /**
         * Constructor.
         *
         * @param   color   base (opaque) colour
         * @param   level   transparency level
         */
        public AutoTransparentStamper( Color color, double level ) {
            color_ = color;
            level_ = level;
        }

        public Icon createLegendIcon( Outliner outliner ) {
            return IconUtils.colorIcon( outliner.getLegendIcon(), color_ );
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof AutoTransparentStamper ) {
                AutoTransparentStamper other = (AutoTransparentStamper) o;
                return this.color_.equals( other.color_ )
                    && this.level_ == other.level_;
            }
            else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            int code = 5231;
            code = code * 23 + color_.hashCode();
            code = code * 23 + Float.floatToIntBits( (float) level_ );
            return code;
        }
    }

    /**
     * Mode for painting shapes with pixel colours dependent on the
     * number of times a given pixel is hit by a shape.
     * Colour determination is done by a Shader.
     * This has to be done by decal not glyph.
     *
     * <p>Concrete subclasses must implement the <code>createStamper</code>
     * method.
     */
    private static abstract class AbstractDensityMode extends ShapeMode {

        /**
         * Constructor.
         *
         * @param  name  mode name
         * @param  icon  mode icon
         */
        AbstractDensityMode( String name, Icon icon ) {
            super( name, icon, new Coord[ 0 ], false );
        }

        public PlotLayer createLayer( ShapePlotter plotter, ShapeForm form,
                                      DataGeom geom, DataSpec dataSpec,
                                      Outliner outliner, Stamper stamper ) {
            DensityStamper dstamper = (DensityStamper) stamper;
            final Shader shader = dstamper.shader_;
            final Scaling scaling = dstamper.scaling_;
            final Subrange dataclip = dstamper.dataclip_;
            ShapeStyle style = new ShapeStyle( outliner, stamper );
            LayerOpt opt = Shaders.isTransparent( shader ) ? LayerOpt.NO_SPECIAL
                                                           : LayerOpt.OPAQUE;
            return new ShapePlotLayer( plotter, geom, dataSpec, style, opt,
                                       outliner ) {
                public Drawing createDrawing( DrawSpec drawSpec ) {
                    return new DensityDrawing( drawSpec, shader, scaling,
                                               dataclip );
                }
            };
        }

        /**
         * Density mode Drawing implementation.
         */
        private static class DensityDrawing extends BinShapeDrawing {
            private final Shader shader_;
            private final Scaling scaling_;
            private final Subrange dataclip_;

            /**
             * Constructor.
             * 
             * @param  drawSpec  common drawing attributes
             * @param  shader  determines colours in colour map
             * @param  scaling  quantitative mapping from counts to colour
             * @param  dataclip  count mapping range adjustment
             */
            DensityDrawing( DrawSpec drawSpec, Shader shader,
                            Scaling scaling, Subrange dataclip ) {
                super( drawSpec );
                shader_ = shader;
                scaling_ = scaling;
                dataclip_ = dataclip;
            }

            PixelImage createPixelImage( Object plan ) {

                /* Clone this array before modifying it in-place.
                 * Trashing the data wouldn't hurt this time,
                 * but the plan may get re-used later. */
                int[] counts = getBinCounts( plan ).clone();
                IndexColorModel colorModel =
                    PixelImage.createColorModel( shader_, true );
                scaleLevels( counts, colorModel.getMapSize() - 1 );
                Dimension size = drawSpec_.surface_.getPlotBounds().getSize();
                return new PixelImage( size, counts, colorModel );
            }

            /**
             * Turns an array of count values into an array of level values.
             * The array is updated in place.  On input, values are between
             * zero and some maximum.  On output they are scaled so that
             * they are between zero and the given <code>nlevel</code> value.
             * Zero maps to zero, and the mapping function is monotonic.
             *
             * @param   buf  data values for input and output
             * @param   nlevel   number of levels represented in output
             */
            private void scaleLevels( int[] buf, int nlevel ) {
                Ranger ranger =
                    Scalings.createRanger( new Scaling[] { scaling_ } );
                int n = buf.length;
                int max = 0;
                for ( int i = 0; i < n; i++ ) {
                    int b = buf[ i ];
                    if ( b > 0 ) {
                        max = Math.max( b, max );
                        ranger.submitDatum( b );
                    }
                }

                /* Leave 0 as 0 - this is transparent (no plot).
                 * Values >= 1 map to range 1..nlevel. */
                if ( max > 0 ) {
                    ranger.submitDatum( 1 );
                    ranger.submitDatum( PlotUtil.MIN_RAMP_UNIT );
                    Scaler scaler = ranger.createSpan()
                                   .createScaler( scaling_, dataclip_ );
                    CountScaler countScaler = new CountScaler( scaler, nlevel );
                    for ( int i = 0; i < n; i++ ) {
                        buf[ i ] = countScaler.scaleCount( buf[ i ] );
                    }
                }
            }
        }
    }

    /**
     * Stamper implementation for density mode.
     */
    public static class DensityStamper implements Stamper {
        final Shader shader_;
        final Scaling scaling_;
        final Subrange dataclip_;

        /**
         * Constructor.
         *
         * @param   shader  colour shader
         * @param  scaling  count scaling strategy
         * @param  dataclip  scaling range adjustment
         */
        public DensityStamper( Shader shader, Scaling scaling,
                               Subrange dataclip ) {
            shader_ = shader;
            scaling_ = scaling;
            dataclip_ = dataclip;
        }

        public Icon createLegendIcon( Outliner outliner ) {
            return createColoredIcon( outliner.getLegendIcon(),
                                      shader_, 0f );
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof DensityStamper ) {
                DensityStamper other = (DensityStamper) o;
                return this.shader_.equals( other.shader_ )
                    && this.scaling_.equals( other.scaling_ )
                    && this.dataclip_.equals( other.dataclip_ );
            }
            else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            int code = 3311;
            code = 23 * code + shader_.hashCode();
            code = 23 * code + scaling_.hashCode();
            code = 23 * code + dataclip_.hashCode();
            return code;
        }
    }

    /**
     * Density mode variant which uses scaling with no free parameters.
     * It's intended to work reasonably well with any data set,
     * at least it should be good enough for a first look.
     * Colour scaling asinh-like and such that the the colours for counts
     * of 1 and 2 should be visually distinguishable.  The colour map
     * just darkens the colours a bit.
     */
    private static class AutoDensityMode extends AbstractDensityMode {

        /**
         * Constructor.
         */
        AutoDensityMode() {
            super( "auto", ResourceIcon.MODE_AUTO );
        }

        public String getModeDescription() {
            return PlotUtil.concatLines( new String[] {
                "<p>Paints isolated points in their selected colour",
                "but where multiple points",
                "<em>in the same layer</em>",
                "overlap it adjusts the clour by darkening it.",
                "This means that for isolated points",
                "(most or all points in a non-crowded plot,",
                "or outliers in a crowded plot)",
                "it behaves just like " + modeRef( FLAT2D ) + " mode,",
                "but it's easy to see where overdense regions lie.",
                "</p>",
                "<p>This is like " + modeRef( DENSITY ) + " mode,",
                "but with no user-configurable options.",
                "</p>",
            } );
        }

        public ConfigKey<?>[] getConfigKeys() {
            return new ConfigKey<?>[] {
                StyleKeys.COLOR,
            };
        }

        public Stamper createStamper( ConfigMap config ) {
            Color baseColor = config.get( StyleKeys.COLOR );
            Shader baseShader = Shaders.stretch( Shaders.SCALE_V, 1f, 0.2f );
            Shader densityShader =
                Shaders.applyShader( baseShader, baseColor, COLOR_MAP_SIZE );
            return new DensityStamper( densityShader, Scaling.AUTO,
                                       new Subrange() );
        }
    }

    /**
     * Density mode variant which allows user selection of various parameters
     * affecting the appearance.
     */
    private static class CustomDensityMode extends AbstractDensityMode {

        private static final RampKeySet RAMP_KEYS = StyleKeys.DENSITY_RAMP;

        /**
         * Constructor.
         */
        CustomDensityMode() {
            super( "density", ResourceIcon.MODE_DENSITY );
        }

        public String getModeDescription() {
            return PlotUtil.concatLines( new String[] {
                "<p>Paints markers using a configurable colour map",
                "to indicate how many points are plotted over each other.",
                "Specifically, it colours each pixel according to how many",
                "times that pixel has has been covered by one of the markers",
                "plotted by the layer in question.",
                "To put it another way,",
                "it generates a false-colour density map with pixel",
                "granularity using a smoothing kernel of the form of the",
                "markers plotted by the layer.",
                "The upshot is that you can see the plot density",
                "of points or other markers plotted.",
                "</p>",
                "<p>This is like " + modeRef( AUTO ) + " mode,",
                "but with more user-configurable options.",
                "</p>",
            } );
        }

        public ConfigKey<?>[] getConfigKeys() {
            List<ConfigKey<?>> keyList = new ArrayList<ConfigKey<?>>();
            keyList.add( StyleKeys.COLOR );
            keyList.addAll( Arrays.asList( RAMP_KEYS.getKeys() ) );
            return keyList.toArray( new ConfigKey<?>[ 0 ] );
        }

        public Stamper createStamper( ConfigMap config ) {
            Color baseColor = config.get( StyleKeys.COLOR );
            RampKeySet.Ramp ramp = RAMP_KEYS.createValue( config );
            Shader baseShader = ramp.getShader();
            Shader densityShader =
                Shaders.applyShader( baseShader, baseColor, COLOR_MAP_SIZE );
            Scaling scaling = ramp.getScaling();
            Subrange dataclip = ramp.getDataClip();
            return new DensityStamper( densityShader, scaling, dataclip );
        }
    }

    /**
     * Mode for colouring shapes according to an additional data coordinate,
     * using the shared AuxScale.COLOR map.
     */
    private static class AuxShadingMode extends ShapeMode {
        private final boolean transparent_;
        private final boolean reportAuxKeys_;

        private static final AuxScale SCALE = AuxScale.COLOR;
        private static final RampKeySet RAMP_KEYS = StyleKeys.AUX_RAMP;
        private static final String scaleName = SCALE.getName();
        private static final FloatingCoord SHADE_COORD =
            FloatingCoord.createCoord(
                new InputMeta( scaleName.toLowerCase(), scaleName )
               .setShortDescription( "Colour coordinate for " + scaleName
                                   + " shading" )
            , false );

        /**
         * Constructor.
         *
         * <p>The reportAuxKeys flag ought normally to be false, since
         * the same global aux colour ramp should be used for all layers,
         * as only one ramp will be drawn on the axes.
         * But in principle you could have different maps for different layers.
         *
         * @param   transparent   if true, allow variable transparency
         * @param   reportAuxKeys  if true, report global aux ramp config keys
         */
        AuxShadingMode( boolean transparent, boolean reportAuxKeys ) {
            super( "aux", ResourceIcon.MODE_AUX, new Coord[] { SHADE_COORD },
                   false );
            transparent_ = transparent;
            reportAuxKeys_ = reportAuxKeys;
        }

        public String getModeDescription() {
            StringBuffer sbuf = new StringBuffer()
                .append( "<p>Paints markers in a colour determined by\n" )
                .append( "the value of an additional data coordinate,\n" )
                .append( "using a colour map shared between\n" )
                .append( "all similar layers.\n" )
                .append( "The marker colours then represent an additional\n" )
                .append( "dimension of the plot.\n" );
            if ( transparent_ ) {
                sbuf.append( "You can also adjust the transparency\n" )
                    .append( "of the colours used.\n" );
            }
            if ( reportAuxKeys_ ) {
                sbuf.append( "There are additional options to adjust\n" )
                    .append( "the way data values are mapped to colours.\n" );
            }
            else {
                sbuf.append( "The way that data values are mapped\n" )
                    .append( "to colours is usually controlled by options\n" )
                    .append( "at the level of the plot itself,\n" )
                    .append( "rather than by per-layer configuration.\n" );
            }
            sbuf.append( "</p>\n" );
            return sbuf.toString();
        }

        public ConfigKey<?>[] getConfigKeys() {
            List<ConfigKey<?>> list = new ArrayList<ConfigKey<?>>();
            if ( reportAuxKeys_ ) {
                list.addAll( Arrays.asList( RAMP_KEYS.getKeys() ) );
            }
            list.add( StyleKeys.AUX_NULLCOLOR );
            if ( transparent_ ) {
                list.add( StyleKeys.AUX_OPAQUE );
            }
            return list.toArray( new ConfigKey<?>[ 0 ] );
        }

        public Stamper createStamper( ConfigMap config ) {
            RampKeySet.Ramp ramp = RAMP_KEYS.createValue( config );
            Shader shader = ramp.getShader();
            Scaling scaling = ramp.getScaling();
            Subrange dataclip = ramp.getDataClip();
            Color nullColor = config.get( StyleKeys.AUX_NULLCOLOR );
            double opaque = transparent_
                          ? config.get( StyleKeys.AUX_OPAQUE )
                          : 1;
            float scaleAlpha = 1f / (float) opaque;
            Color baseColor = config.get( StyleKeys.COLOR );
            return new ShadeStamper( shader, scaling, dataclip, baseColor,
                                     nullColor, scaleAlpha );
        }

        public PlotLayer createLayer( ShapePlotter plotter,
                                      final ShapeForm form,
                                      final DataGeom geom,
                                      final DataSpec dataSpec,
                                      final Outliner outliner,
                                      final Stamper stamper ) {
            final int iShadeCoord = plotter.getModeCoordsIndex( geom );
            assert dataSpec.getCoord( iShadeCoord ) == SHADE_COORD;
            ShadeStamper shStamper = (ShadeStamper) stamper;
            final Shader shader = shStamper.shader_;
            final Scaling scaling = shStamper.scaling_;
            final Subrange dataclip = shStamper.dataclip_;
            final Color baseColor = shStamper.baseColor_;
            final Color nullColor = shStamper.nullColor_;
            final float scaleAlpha = shStamper.scaleAlpha_;
            Style style = new ShapeStyle( outliner, stamper );
            LayerOpt opt = scaleAlpha < 1f || Shaders.isTransparent( shader )
                         ? LayerOpt.NO_SPECIAL
                         : LayerOpt.OPAQUE;
            return new AbstractPlotLayer( plotter, geom, dataSpec,
                                          style, opt ) {
                @Override
                public Map<AuxScale,AuxReader> getAuxRangers() {
                    Map<AuxScale,AuxReader> map = super.getAuxRangers();
                    AuxReader shadeReader =
                        new FloatingCoordAuxReader( SHADE_COORD, iShadeCoord,
                                                    geom, true, scaling );
                    map.put( SCALE, shadeReader );
                    map.putAll( outliner.getAuxRangers( geom ) );
                    return map;
                }
                public Drawing createDrawing( Surface surface,
                                              Map<AuxScale,Span> auxSpans,
                                              PaperType paperType ) {
                    final Span shadeSpan = auxSpans.get( SCALE );
                    DrawSpec drawSpec =
                        new DrawSpec( surface, geom, dataSpec, outliner,
                                      auxSpans, paperType );
                    Supplier<ColorKit> kitFact = new Supplier<ColorKit>() {
                        public ColorKit get() {
                            Scaler scaler =
                                shadeSpan.createScaler( scaling, dataclip );
                            return new AuxColorKit( iShadeCoord, shader, scaler,
                                                    baseColor, nullColor,
                                                    scaleAlpha );
                        }
                    };

                    // A possible optimisation would be in the case that we
                    // have an opaque 2D plot to return a planned drawing,
                    // where the plan is a double[] array of aux values.
                    // This would allow fast changes to the colour map,
                    // like for density plots.  Would take some code,
                    // but nothing that hasn't been done already.
                    // See classes PixOutliner, PixelImage, Gridder etc.
                    // if ( scaleAlpha == 1f &&
                    //      paperType.isBitmap() &&
                    //      paperType instanceof PaperType2D ) {
                    //     return new AuxGridDrawing2D( drawSpec, iShadeCoord,
                    //                                  shader, scaler,
                    //                                  baseColor, nullColor );
                    // }
                    return drawSpec.createDrawing( kitFact );
                }
            };
        }
    }

    /**
     * Mode for colouring shapes according to an additional data coordinate,
     * using a colour map private to the layer.
     */
    private static class PrivateAuxMode extends ShapeMode {
        private final boolean transparent_;

        private static final RampKeySet RAMP_KEYS = StyleKeys.AUXLOCAL_RAMP;
        private static final ConfigKey<Color> NULLCOLOR_KEY =
            StyleKeys.AUXLOCAL_NULLCOLOR;
        private static final ConfigKey<Double> OPAQUE_KEY =
            StyleKeys.AUX_OPAQUE;
        private static final AuxScale SCALE = new AuxScale( "PAux" );
        private static final String scaleName = SCALE.getName();
        private static final FloatingCoord SHADE_COORD =
            FloatingCoord.createCoord(
                new InputMeta( "aux", "Aux" )
               .setShortDescription( "Colour coordinate for " + scaleName
                                   + " shading" )
            , false );

        /**
         * Constructor.
         *
         * @param   transparent   if true, allow variable transparency
         */
        PrivateAuxMode( boolean transparent ) {
            super( "paux", ResourceIcon.MODE_PAUX,
                   new Coord[] { SHADE_COORD }, false );
            transparent_ = transparent;
        }

        public String getModeDescription() {
            StringBuffer sbuf = new StringBuffer()
                .append( "<p>Paints markers in a colour determined by\n" )
                .append( "the value of an additional data coordinate,\n" )
                .append( "using a colour map private to this layer.\n" )
                .append( "The marker colours then represent an additional\n" )
                .append( "dimension of the plot.\n" );
            if ( transparent_ ) {
                sbuf.append( "You can also adjust the transparency\n" )
                    .append( "of the colours used.\n" );
            }
            sbuf.append( "There are additional options to adjust\n" )
                .append( "the way data values are mapped to colours.\n" )
                .append( "</p>\n" )
                .append( "<p>This resembles\n" )
                .append( "<ref id='shading-aux'><code>aux</code></ref>\n" )
                .append( "mode, but the colour map is not shared with other\n" )
                .append( "layers, and the colour ramp is not displayed.\n" )
                .append( "So by using this mode alongside\n" )
                .append( "<code>aux</code> or\n" )
                .append( "<ref id='shading-weighted'><code>weighted</code>"
                               + "</ref>\n" )
                .append( "you can make a plot that uses multiple\n" )
                .append( "different colour maps,\n" )
                .append( "though only one can have\n" )
                .append( "an associated visible ramp.\n" )
                .append( "</p>\n" );
            return sbuf.toString();
        }

        public ConfigKey<?>[] getConfigKeys() {
            List<ConfigKey<?>> list = new ArrayList<>();
            list.addAll( Arrays.asList( RAMP_KEYS.getKeys() ) );
            list.add( NULLCOLOR_KEY );
            if ( transparent_ ) {
                list.add( OPAQUE_KEY );
            }
            return list.toArray( new ConfigKey<?>[ 0 ] );
        }

        public Stamper createStamper( ConfigMap config ) {
            RampKeySet.Ramp ramp = RAMP_KEYS.createValue( config );
            Shader shader = ramp.getShader();
            Scaling scaling = ramp.getScaling();
            Subrange dataclip = ramp.getDataClip();
            Color nullColor = config.get( NULLCOLOR_KEY );
            double opaque = transparent_ ? config.get( OPAQUE_KEY ) : 1.0;
            float scaleAlpha = 1f / (float) opaque;
            Color baseColor = config.get( StyleKeys.COLOR );
            return new ShadeStamper( shader, scaling, dataclip, baseColor,
                                     nullColor, scaleAlpha );
        }

        public PlotLayer createLayer( ShapePlotter plotter, ShapeForm form,
                                      DataGeom geom, DataSpec dataSpec,
                                      Outliner outliner, Stamper stamper ) {
            int iShadeCoord = plotter.getModeCoordsIndex( geom );
            assert dataSpec.getCoord( iShadeCoord ) == SHADE_COORD;
            ShadeStamper shStamper = (ShadeStamper) stamper;
            Shader shader = shStamper.shader_;
            Scaling scaling = shStamper.scaling_;
            Subrange dataclip = shStamper.dataclip_;
            Color baseColor = shStamper.baseColor_;
            Color nullColor = shStamper.nullColor_;
            float scaleAlpha = shStamper.scaleAlpha_;
            Style style = new ShapeStyle( outliner, stamper );
            AuxReader auxReader =
                new FloatingCoordAuxReader( SHADE_COORD, iShadeCoord, geom,
                                            true, scaling );
            LayerOpt opt = scaleAlpha < 1f || Shaders.isTransparent( shader )
                         ? LayerOpt.NO_SPECIAL
                         : LayerOpt.OPAQUE;
            return new AbstractPlotLayer( plotter, geom, dataSpec,
                                          style, opt ) {
                @Override
                public Map<AuxScale,AuxReader> getAuxRangers() {
                    Map<AuxScale,AuxReader> map = super.getAuxRangers();
                    map.putAll( outliner.getAuxRangers( geom ) );
                    return map;
                }
                public Drawing createDrawing( Surface surface,
                                              Map<AuxScale,Span> auxSpans,
                                              PaperType paperType ) {
                    DrawSpec drawSpec =
                        new DrawSpec( surface, geom, dataSpec, outliner,
                                      auxSpans, paperType );
                    return new Drawing() {
                        public Object calculatePlan( Object[] knownPlans,
                                                     DataStore dataStore ) {
                            for ( Object plan : knownPlans ) {
                                if ( plan instanceof SpanPlan ) {
                                    SpanPlan splan = (SpanPlan) plan;
                                    if ( splan.matches( scaling, surface,
                                                        geom, dataSpec ) ) {
                                        return splan;
                                     }
                                }
                            }
                            Ranger ranger =
                                Scalings
                               .createRanger( new Scaling[] { scaling } );
                            auxReader.adjustAuxRange( surface, dataSpec,
                                                      dataStore, knownPlans,
                                                      ranger );
                            Span shadeSpan = ranger.createSpan();
                            return new SpanPlan( shadeSpan, scaling, surface,
                                                 geom, dataSpec );
                        }
                        public void paintData( Object plan, Paper paper,
                                               DataStore dataStore ) {
                            Span shadeSpan = ((SpanPlan) plan).span_;
                            Scaler scaler =
                                shadeSpan.createScaler( scaling, dataclip );
                            BiConsumer<TupleSequence,Paper> tuplePainter =
                                    (tseq, p) -> {
                                ShapePainter painter = drawSpec.createPainter();
                                ColorKit colorKit =
                                    new AuxColorKit( iShadeCoord, shader,
                                                     scaler, baseColor,
                                                     nullColor, scaleAlpha );
                                while ( tseq.next() ) {
                                    Color color = colorKit.readColor( tseq );
                                    if ( color != null ) {
                                        painter.paintPoint( tseq, color, p );
                                    }
                                }
                            };
                            dataStore.getTupleRunner()
                                     .paintData( tuplePainter, paper, dataSpec,
                                                 dataStore );
                        }
                        public ReportMap getReport( Object plan ) {
                            return outliner.getReport( plan );
                        }
                    };
                }
            };
        }

        /**
         * Aggregates a Span with information that characterises its
         * scope of applicability.
         */
        private static class SpanPlan {
            final Span span_;
            final Scaling scaling_;
            final Surface surface_;
            final DataGeom geom_;
            final DataSpec dataSpec_;

            /**
             * Constructor.
             *
             * @param  span  calculated span
             * @param  scaling  scaling type
             * @param  surface  plot surface
             * @param  geom  data geom
             * @param  dataSpec   data spec
             */
            SpanPlan( Span span, Scaling scaling, Surface surface,
                      DataGeom geom, DataSpec dataSpec ) {
                span_ = span;
                scaling_ = scaling;
                surface_ = surface;
                geom_ = geom;
                dataSpec_ = dataSpec;
            }

            /**
             * Indicates whether this plan can be used for a given set
             * of drawing requirements.
             *
             * @param  scaling  scaling type
             * @param  surface  plot surface
             * @param  geom  data geom
             * @param  dataSpec   data spec
             * @return true iff this plan's data matches the requirements
             */
            public boolean matches( Scaling scaling, Surface surface,
                                    DataGeom geom, DataSpec dataSpec ) {
                return scaling.equals( scaling_ )
                    && surface.equals( surface_ )
                    && geom.equals( geom_ )
                    && dataSpec.equals( dataSpec_ );
            }
        }
    }

    /**
     * Mode for displaying density weighted by an additional data coordinate.
     */
    private static class WeightedDensityMode extends ShapeMode {

        private final boolean isPrivate_;

        private final AuxScale scale_;
        private final RampKeySet rampKeys_;
        private final ConfigKey<Combiner> COMBINER_KEY =
            createWeightCombinerKey();
        private static final FloatingCoord WEIGHT_COORD =
            FloatingCoord.createCoord(
                new InputMeta( "weight", "Weight" )
               .setShortDescription( "Weight coordinate "
                                   + "for weighted density shading" )
            , false );

        /**
         * Constructor.
         *
         * @param  isPrivate  if true, colour map is private to this layer;
         *                    if false, colour map is AuxScale.COLOR shared
         *                    between layers
         */
        WeightedDensityMode( boolean isPrivate ) {
            super( isPrivate ? "pweighted" : "weighted",
                   isPrivate ? ResourceIcon.MODE_PWEIGHT
                             : ResourceIcon.MODE_WEIGHT,
                   new Coord[] { WEIGHT_COORD }, true );
            isPrivate_ = isPrivate;
            scale_ = isPrivate ? new AuxScale( "PWeight" ) : AuxScale.COLOR;
            rampKeys_ = isPrivate ? StyleKeys.AUXLOCAL_RAMP
                                  : StyleKeys.AUX_RAMP;
        }

        public String getModeDescription() {
            StringBuffer sbuf = new StringBuffer()
                .append( "<p>Paints markers like the Density mode,\n" )
                .append( "but with optional weighting by an additional\n" )
                .append( "coordinate;\n" )
                .append( "the colour map is\n" )
                .append( isPrivate_ ? "private to this layer"
                                    : "shared between layers" )
                .append( ".\n" )
                .append( "You can configure how the weighted coordinates\n" )
                .append( "are combined to give the final weighted result.\n" );
            if ( isPrivate_ ) {
                sbuf.append( "There are additional options to adjust\n" )
                    .append( "the way data values are mapped to colours.\n" );
            }
            else {
                sbuf.append( "The way that data values are mapped\n" )
                    .append( "to colours is usually controlled by options\n" )
                    .append( "at the level of the plot itself,\n" )
                    .append( "rather than by per-layer configuration.\n" );
            }
            sbuf.append( "</p>\n" );
            if ( isPrivate_ ) {
                sbuf.append( "<p>This resembles\n" )
                    .append( "<ref id='shading-weighted'><code>weighted</code>"
                                 + "</ref> mode,\n" )
                    .append( "but the colour map is not shared with other\n" )
                    .append( "layers, and the colour ramp is not displayed.\n" )
                    .append( "So by using this mode alongside\n" )
                    .append( "<code>weighted</code> or\n" )
                    .append( "<ref id='shading-aux'><code>aux</code></ref>\n" )
                    .append( "you can make a plot that uses multiple\n" )
                    .append( "different colour maps,\n" )
                    .append( "though only one can have\n" )
                    .append( "an associated visible ramp.\n" )
                    .append( "</p>\n" );
            }
            return sbuf.toString();
        }

        public ConfigKey<?>[] getConfigKeys() {
            List<ConfigKey<?>> keyList = new ArrayList<>();
            keyList.add( StyleKeys.COLOR );
            keyList.add( COMBINER_KEY );
            if ( isPrivate_ ) {
                keyList.addAll( Arrays.asList( rampKeys_.getKeys() ) );
            }
            return keyList.toArray( new ConfigKey<?>[ 0 ] );
        }

        public Stamper createStamper( ConfigMap config ) {
            Color baseColor = config.get( StyleKeys.COLOR );
            RampKeySet.Ramp ramp = rampKeys_.createValue( config );
            Shader baseShader = ramp.getShader();
            Shader shader =
                Shaders.applyShader( baseShader, baseColor, COLOR_MAP_SIZE );
            Scaling scaling = ramp.getScaling();
            Subrange dataclip = ramp.getDataClip();
            Combiner combiner = config.get( COMBINER_KEY );
            return new WeightStamper( shader, scaling, dataclip, combiner );
        }

        public PlotLayer createLayer( ShapePlotter plotter, ShapeForm form,
                                      DataGeom geom, DataSpec dataSpec,
                                      Outliner outliner, Stamper stamper ) {
            return new WeightLayer( plotter, geom, dataSpec, outliner,
                                    (WeightStamper) stamper, isPrivate_,
                                    scale_ );
        }

        /**
         * Constructs the config key used to solicit a Combiner value
         * from the user.
         *
         * @return  combiner key
         */
        private static ConfigKey<Combiner> createWeightCombinerKey() {
            ConfigMeta meta = new ConfigMeta( "combine", "Combine" );
            meta.setShortDescription( "Value combination mode" );
            meta.setXmlDescription( new String[] {
                "<p>Defines how values contributing to the same",
                "pixel are combined together to produce",
                "the value assigned to that pixel (and hence its colour).",
                "</p>",
                "<p>When a weight is in use,",
                "<code>" + Combiner.MEAN + "</code> or",
                "<code>" + Combiner.SUM + "</code>",
                "are typically sensible choices.",
                "If there is no weight (a pure density map)",
                "then <code>" + Combiner.COUNT + "</code> is usually better,",
                "but in that case it may make more sense",
                "(it is more efficient)",
                "to use one of the other shading modes instead.",
                "</p>",
            } );

            /* Prepare a list of suitable options.  We exclude density-like
             * options, since they require scaling by bin size.
             * Although we could come up with a physical bin size in some
             * cases (2d, linear axes), it can't really be done in others
             * (logarithmic axes, and especially 3d). */
            List<Combiner> optionList = new ArrayList<>();
            for ( Combiner c : Combiner.getKnownCombiners() ) {
                if ( ! Combiner.Type.DENSITY.equals( c.getType() ) ) {
                    optionList.add( c );
                }
            }
            Combiner[] options = optionList.toArray( new Combiner[ 0 ] );
            Combiner dflt = Combiner.MEAN;
            OptionConfigKey<Combiner> key =
                    new OptionConfigKey<Combiner>( meta, Combiner.class,
                                                   options, dflt ) {
                public String getXmlDescription( Combiner combiner ) {
                    return combiner.getDescription();
                }
            };
            key.setOptionUsage();
            key.addOptionsXml();
            return key;
        }

        /**
         * PlotLayer implementation for use with the weighted density plot.
         */
        private static class WeightLayer extends AbstractPlotLayer {

            private final Outliner outliner_;
            private final WeightStamper wstamper_;
            private final int icWeight_;
            private final boolean isPrivate_;
            private final AuxScale scale_;
            private final AuxReader scaleReader_;

            /**
             * Constructor.
             *
             * @param  plotter  plotter
             * @param  geom   data geom
             * @param  dataSpec  data specification
             * @param  outliner  outliner
             * @param  wstamper  stamper
             * @param  isPrivate  true for private colour map, false for shared
             * @param  scale   aux scale used for colour values
             */
            WeightLayer( ShapePlotter plotter, DataGeom geom, DataSpec dataSpec,
                         Outliner outliner, WeightStamper wstamper,
                         boolean isPrivate, AuxScale scale ) {
                super( plotter, geom, dataSpec,
                       new ShapeStyle( outliner, wstamper ),
                       isTransparent( wstamper ) ? LayerOpt.NO_SPECIAL
                                                 : LayerOpt.OPAQUE );
                outliner_ = outliner;
                wstamper_ = wstamper;
                isPrivate_ = isPrivate;
                scale_ = scale;
                icWeight_ = plotter.getModeCoordsIndex( geom );
                assert dataSpec.getCoord( icWeight_ ) == WEIGHT_COORD;
                scaleReader_ = new AuxReader() {
                    public int getCoordIndex() {
                        return icWeight_;
                    }
                    public Scaling getScaling() {
                        return wstamper_.scaling_;
                    }
                    public ValueInfo getAxisInfo( DataSpec dataSpec ) {
                        Combiner combiner = wstamper_.combiner_;
                        final ValueInfo weightInfo;
                        if ( icWeight_ < 0 ||
                             dataSpec.isCoordBlank( icWeight_ ) ) {
                            weightInfo =
                                new DefaultValueInfo( "1", Double.class,
                                                      "Weight unspecified"
                                                    + ", taken as unity" );
                        }
                        else {
                            ValueInfo[] winfos =
                                dataSpec.getUserCoordInfos( icWeight_ );
                            weightInfo = winfos != null && winfos.length == 1
                                       ? winfos[ 0 ]
                                       : new DefaultValueInfo( "Weight",
                                                               Double.class );
                        }
                        return wstamper_.combiner_
                              .createCombinedInfo( weightInfo, Unit.UNIT );
                    }
                    public void adjustAuxRange( Surface surface,
                                                DataSpec dataSpec,
                                                DataStore dataStore,
                                                Object[] knownPlans,
                                                Ranger ranger ) {
                        WeightPlan wplan =
                            getWeightPlan( knownPlans, surface, dataSpec );
                        final BinList.Result binResult;
                        if ( wplan == null ) {
                            // no auxSpans - have to fake it.
                            Map<AuxScale,Span> auxSpans = new HashMap<>();
                            binResult = readBinList( surface, dataSpec,
                                                     dataStore, auxSpans )
                                       .getResult();
                        }
                        else {
                            binResult = wplan.binResult_;
                        }
                        assert ! isBinFactorRequired();
                        for ( Iterator<Long> it = binResult.indexIterator();
                              it.hasNext(); ) {
                            long ibin = it.next().longValue();
                            ranger.submitDatum( binResult
                                               .getBinValue( ibin ) );
                        }
                    }
                };
            }

            /**
             * Indicates whether BinList.Result scaling is required.
             * This is used in assertions, and should always return false,
             * since no density-like combiners are in use by this mode.
             * The idea is to provide evidence in the source code that
             * bin scaling has not been overlooked.
             *
             * @return  whether BinList.Result values need to be scaled;
             *          always false
             */
            boolean isBinFactorRequired() {
                return wstamper_.combiner_.getType().getBinFactor( 0.01 ) != 1.;
            }

            @Override
            public Map<AuxScale,AuxReader> getAuxRangers() {
                Map<AuxScale,AuxReader> map = super.getAuxRangers();
                map.putAll( outliner_.getAuxRangers( getDataGeom() ) );
                if ( ! isPrivate_ ) {
                    map.put( scale_, scaleReader_ );
                }
                return map;
            }

            public Drawing createDrawing( Surface surface,
                                          Map<AuxScale,Span> auxSpans,
                                          PaperType paperType ) {
                return new WeightDrawing( surface, auxSpans, paperType );
            }

            /**
             * Constructs the binlist data structure containing the information
             * about how to do the plot.  There is no need to scale the
             * result of this call using a bin factor.
             *
             * @param  surface  plot surface
             * @param  dataSpec  data specification
             * @param  dataStore  data storage
             * @param  auxSpans   aux data range map
             * @return  bin list that does not require scaling
             */
            private BinList readBinList( Surface surface, DataSpec dataSpec,
                                         DataStore dataStore,
                                         Map<AuxScale,Span> auxSpans ) {
                DataGeom geom = getDataGeom();
                int icw = dataSpec.isCoordBlank( icWeight_ ) ? -1 : icWeight_;
                WeightCollector collector =
                    new WeightCollector( surface, icw, wstamper_.combiner_,
                                         outliner_, geom, dataSpec, auxSpans );
                WeightPaper wpaper =
                    dataStore.getTupleRunner()
                   .collectPool( collector,
                                 () -> dataStore.getTupleSequence( dataSpec ) );

                /* We have excluded density-like combiners because it's
                 * hard to know what to use as a bin size, so no scaling
                 * of bin list results is required. */
                assert ! isBinFactorRequired();
                return wpaper.binList_;
            }

            /**
             * Determines whether a given stamper has any transparency.
             *
             * @param  wstamper  stamper
             * @return   true iff any pixels may be painted with alpha&lt;1
             */
            private static boolean isTransparent( WeightStamper wstamper ) {
                return Shaders.isTransparent( wstamper.shader_ );
            }

            /**
             * Drawing implementation for use with weighted density layer.
             */
            private class WeightDrawing implements Drawing {
                final Surface surface_;
                final Map<AuxScale,Span> auxSpans_;
                final PaperType paperType_;

                /**
                 * Constructor.
                 *
                 * @param  surface  plot surface
                 * @param  auxSpans   aux data ranges
                 * @param  paperType   paper type
                 */
                WeightDrawing( Surface surface, Map<AuxScale,Span> auxSpans,
                               PaperType paperType ) {
                    surface_ = surface;
                    auxSpans_ = auxSpans;
                    paperType_ = paperType;
                }

                public Object calculatePlan( Object[] knownPlans,
                                             DataStore dataStore ) {
                    DataSpec dataSpec = getDataSpec();
                    WeightPlan knownPlan =
                        getWeightPlan( knownPlans, surface_,  dataSpec );
                    if ( knownPlan != null ) {
                        return knownPlan;
                    }
                    else {
                        BinList binList = readBinList( surface_, dataSpec,
                                                       dataStore, auxSpans_ );
                        int nbin = (int) binList.getSize();  // pixel count
                        Combiner combiner = binList.getCombiner();
                        BinList.Result binResult =
                            binList.getResult().compact();
                        DataGeom geom = getDataGeom();
                        Scaling scaling = wstamper_.scaling_;
                        final Span auxSpan;
                        if ( isPrivate_ ) {
                            Ranger ranger =
                                Scalings
                               .createRanger( new Scaling[] { scaling } );
                            scaleReader_.adjustAuxRange( surface_, dataSpec,
                                                         dataStore, knownPlans,
                                                         ranger );
                            auxSpan = ranger.createSpan();
                        }
                        else {
                            auxSpan = null;
                        }
                        return new WeightPlan( nbin, combiner, binResult,
                                               surface_, geom, dataSpec,
                                               outliner_, scaling, auxSpan );
                    }
                }

                public void paintData( Object plan, Paper paper,
                                       DataStore dataStore ) {
                    WeightPlan wplan = (WeightPlan) plan;
                    final int nbin = wplan.nbin_;
                    final BinList.Result binResult = wplan.binResult_;
                    final Span auxSpan = isPrivate_ ? wplan.span_
                                                    : auxSpans_.get( scale_ );
                    paperType_.placeDecal( paper, new Decal() {
                        public void paintDecal( Graphics g ) {
                            paintBins( g, nbin, binResult, auxSpan );
                        }
                        public boolean isOpaque() {
                            return ! isTransparent( wstamper_ );
                        }
                    } );
                }

                public ReportMap getReport( Object plan ) {
                    ReportMap map = new ReportMap();
                    ReportMap pixelReport = getPixelReport( surface_ );
                    if ( pixelReport != null ) {
                        map.putAll( pixelReport );
                    }
                    ReportMap outlineReport = outliner_.getReport( plan );
                    if ( outlineReport != null ) {
                        map.putAll( outlineReport );
                    }
                    return map;
                }

                /**
                 * Paints a given bin list onto a graphics context for this
                 * drawing.
                 *
                 * @param   g  graphics context
                 * @param   nbin  number of (potential) bins
                 * @param   binResult  bin values
                 * @param   auxSpan   span of shading values
                 */
                private void paintBins( Graphics g, int nbin,
                                        BinList.Result binResult,
                                        Span auxSpan ) {
                    if ( auxSpan == null ) {
                        auxSpan = PlotUtil.EMPTY_SPAN;
                    }
                    Rectangle plotBounds = surface_.getPlotBounds();
                    IndexColorModel colorModel =
                        PixelImage.createColorModel( wstamper_.shader_, true );
                    Scaler scaler = auxSpan.createScaler( wstamper_.scaling_,
                                                          wstamper_.dataclip_ );
                    int[] pixels =
                        scaleLevels( nbin, binResult, scaler,
                                     colorModel.getMapSize() - 1 );
                    PixelImage image =
                        new PixelImage( plotBounds.getSize(), pixels,
                                        colorModel );
                    image.paintPixels( g, plotBounds.getLocation() );
                }

                /**
                 * Turns a populated bin list into an integer array suitable
                 * for use with an indexed colour map.
                 * Entry zero of the colour map is transparent,
                 * and only untouched entries should translate to zero values
                 * in the output array.
                 *
                 * @param  nbin     number of bins
                 * @param  binResult  contains input data per pixel
                 * @param  scaler   normalises bin values
                 * @param  nlevel   number of entries in the colour map;
                 *                  maximum value in output pixel array
                 * @return  pixel array for use with an nlevel-entry colour map;
                 *          zero values are transparent
                 */
                private int[] scaleLevels( int nbin, BinList.Result binResult,
                                           Scaler scaler, int nlevel ) {
                    int[] pixels = new int[ nbin ];
                    assert ! isBinFactorRequired();
                    for ( int i = 0; i < nbin; i++ ) {
                        double val = binResult.getBinValue( i );
                        if ( ! Double.isNaN( val ) ) {
                            int p = (int) ( scaler.scaleValue( val ) * nlevel );
                            pixels[ i ] = Math.min( 1 + p, nlevel - 1 );
                        }
                    }
                    return pixels;
                }
            }

            /**
             * Identifies and retrieves a weight plan that can be used for
             * this layer from a list of precalculated plans.
             * If none of the supplied plans is suitable, null is returned.
             *
             * @param  knownPlans  available pre-calculated plans
             * @param  surface   target plotting surface
             * @param  dataSpec   weight data specification
             * @return  suitable weight plan from supplied list, or null
             */
            private WeightPlan getWeightPlan( Object[] knownPlans,
                                              Surface surface,
                                              DataSpec dataSpec ) {
                DataGeom geom = getDataGeom();
                for ( Object plan : knownPlans ) {
                    if ( plan instanceof WeightPlan ) {
                        WeightPlan wplan = (WeightPlan) plan;
                        if ( wplan.matches( wstamper_.combiner_, surface,
                                            geom, dataSpec, outliner_,
                                            wstamper_.scaling_, isPrivate_ ) ) {
                            return wplan;
                        }
                    }
                }
                return null;
            }
        }

        /**
         * Aggregates a BinList with information that characterises its
         * scope of applicability.
         */
        private static class WeightPlan {
            final int nbin_;
            final Combiner combiner_;
            final BinList.Result binResult_;
            final Surface surface_;
            final DataGeom geom_;
            final DataSpec dataSpec_;
            final Outliner outliner_;
            final Scaling scaling_;
            final Span span_;

            /**
             * Constructor.
             *
             * @param  nbin      size of bin list
             * @param  combiner  combination method for values
             * @param  binResult  contains accumulated weight data
             * @param  surface   plot surface
             * @param  geom     geom
             * @param  dataSpec   data specification
             * @param  outliner   defines shape of plotted points
             * @param  scaling   scaling
             * @param  span   aux span, non-null only for private colour map
             */
            WeightPlan( int nbin, Combiner combiner, BinList.Result binResult,
                        Surface surface, DataGeom geom, DataSpec dataSpec,
                        Outliner outliner, Scaling scaling, Span span ) {
                nbin_ = nbin;
                combiner_ = combiner;
                binResult_ = binResult;
                surface_ = surface;
                geom_ = geom;
                dataSpec_ = dataSpec;
                outliner_ = outliner;
                scaling_ = scaling;
                span_ = span;
            }

            /**
             * Indicates whether this plan can be used for a given set
             * of drawing requirements.
             *
             * @param  combiner   target weight combiner
             * @param  surface   plot surface
             * @param  geom     geom
             * @param  dataSpec   data specification
             * @param  outliner   defines shape of plotted points
             * @param  scaling   scaling
             * @param  span   aux span, only relevant for private colour map
             * @return  true iff this plan's data matches the requirements
             */
            public boolean matches( Combiner combiner, Surface surface,
                                    DataGeom geom, DataSpec dataSpec,
                                    Outliner outliner, Scaling scaling,
                                    boolean isPrivate ) {
                return combiner.equals( combiner_ )
                    && surface.equals( surface_ )
                    && PlotUtil.equals( geom, geom_ )
                    && dataSpec.equals( dataSpec_ )
                    && outliner.equals( outliner_ )
                    && scaling.equals( scaling_ )
                    && ( span_ != null || !isPrivate );
            }
        }

        /**
         * Accepts glyphs and turns them into a BinList using weights.
         */
        private static class WeightPaper extends GlyphPaper {
            private final Rectangle bounds_;
            private final Gridder gridder_;
            private final BinList binList_;
            private final int xoff_;
            private final int yoff_;
            private double weight_;
   
            /**
             * Constructor.
             *
             * @param  bounds  plot bounds
             * @param  combiner  how to combine value weights
             */
            WeightPaper( Rectangle bounds, Combiner combiner ) {
                super( bounds );
                bounds_ = new Rectangle( bounds );
                gridder_ = new Gridder( bounds.width, bounds.height );
                int nbin = gridder_.getLength();
                BinList binlist = combiner.createArrayBinList( nbin );
                binList_ = binlist == null
                         ? new HashBinList( nbin, combiner )
                         : binlist;
                xoff_ = bounds_.x;
                yoff_ = bounds_.y;
            }

            /**
             * Sets the weight value to use for subseqent glyphs when
             * accumulating values into the bin list.
             */
            public void setWeight( double weight ) {
                weight_ = weight;
            }

            public void glyphPixels( Pixer pixer ) {
                while ( pixer.next() ) {
                    int px = pixer.getX();
                    int py = pixer.getY();
                    assert bounds_.contains( px, py );
                    int ix = px - xoff_;
                    int iy = py - yoff_;
                    binList_.submitToBin( gridder_.getIndex( ix, iy ),
                                          weight_ );
                }
            }
        }

        /**
         * SplitCollector implementation for use with WeightedDensityMode.
         */
        private static class WeightCollector
                implements SplitCollector<TupleSequence,WeightPaper> {

            private final Surface surface_;
            private final int icWeight_;
            private final Combiner combiner_;
            private final Outliner outliner_;
            private final DataGeom geom_;
            private final DataSpec dataSpec_;
            private final Map<AuxScale,Span> auxSpans_;

            /**
             * Constructor.
             *
             * @param  surface  plot surface
             * @param  icWeight   weight coordinate index, or -1 for unit weight
             * @param  combiner   combination mode
             * @param  outliner   outliner
             * @param  geom    data geom
             * @param  dataSpec  data specification
             * @param  auxSpans   aux data range map
             */
            WeightCollector( Surface surface, int icWeight, Combiner combiner,
                             Outliner outliner, DataGeom geom,
                             DataSpec dataSpec, Map<AuxScale,Span> auxSpans ) {
                surface_ = surface;
                icWeight_ = icWeight;
                combiner_ = combiner;
                outliner_ = outliner;
                geom_ = geom;
                dataSpec_ = dataSpec;
                auxSpans_ = auxSpans;
            }

            public WeightPaper createAccumulator() {
                return new WeightPaper( surface_.getPlotBounds(), combiner_ );
            }

            public void accumulate( TupleSequence tseq, WeightPaper wpaper ) {
                GlyphPaper.GlyphPaperType ptype = wpaper.getPaperType();
                ShapePainter painter =
                      surface_ instanceof CubeSurface
                    ? outliner_.create3DPainter( (CubeSurface) surface_, geom_,
                                                 dataSpec_, auxSpans_, ptype )
                    : outliner_.create2DPainter( surface_, geom_,
                                                 dataSpec_, auxSpans_, ptype );
                if ( icWeight_ >= 0 ) {
                    while ( tseq.next() ) {
                        double w =
                            WEIGHT_COORD.readDoubleCoord( tseq, icWeight_ );
                        if ( ! Double.isNaN( w ) ) {
                            wpaper.setWeight( w );
                            painter.paintPoint( tseq, null, wpaper );
                        }
                    }
                }
                else {
                    wpaper.setWeight( 1 );
                    while ( tseq.next() ) {
                        painter.paintPoint( tseq, null, wpaper );
                    }
                }
            }

            public WeightPaper combine( WeightPaper wpaper1,
                                        WeightPaper wpaper2 ) {
                BinList bl1 = wpaper1.binList_;
                BinList bl2 = wpaper2.binList_;
                if ( bl1 instanceof ArrayBinList ) {
                    assert bl2 instanceof ArrayBinList;
                    ((ArrayBinList) bl1).addBins( (ArrayBinList) bl2 );
                }
                else if ( bl1 instanceof HashBinList ) {
                    assert bl2 instanceof HashBinList;
                    ((HashBinList) bl1).addBins( bl2 );
                }
                else {
                    throw new AssertionError();
                }
                return wpaper1;
            }
        }
    }

    /**
     * Stamper implementation for use with WeightedShadingMode.
     */
    public static class WeightStamper implements Stamper {
        final Shader shader_;
        final Scaling scaling_;
        final Subrange dataclip_;
        final Combiner combiner_;

        /**
         * Constructor.
         *
         * @param   shader  colour shader
         * @param  scaling  count scaling strategy
         * @param  dataclip  count scaling range adjustment
         * @param  combiner   combiner
         */
        public WeightStamper( Shader shader, Scaling scaling, Subrange dataclip,
                              Combiner combiner ) {
            shader_ = shader;
            scaling_ = scaling;
            dataclip_ = dataclip;
            combiner_ = combiner;
        }

        public Icon createLegendIcon( Outliner outliner ) {
            return createColoredIcon( outliner.getLegendIcon(),
                                      shader_, 0f );
        }       
            
        @Override
        public boolean equals( Object o ) {
            if ( o instanceof WeightStamper ) {
                WeightStamper other = (WeightStamper) o;
                return this.shader_.equals( other.shader_ )
                    && this.scaling_.equals( other.scaling_ )
                    && this.dataclip_.equals( other.dataclip_ )
                    && this.combiner_.equals( other.combiner_ );
            }
            else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            int code = 3311;
            code = 23 * code + shader_.hashCode();
            code = 23 * code + scaling_.hashCode();
            code = 23 * code + dataclip_.hashCode();
            code = 23 * code + combiner_.hashCode();
            return code;
        }
    }

    /**
     * Stamper implementation for use with AuxShadingMode.
     */
    public static class ShadeStamper implements Stamper {
        final Shader shader_;
        final Scaling scaling_;
        final Subrange dataclip_;
        final Color baseColor_;
        final Color nullColor_;
        final float scaleAlpha_;

        /**
         * Constructor.
         *
         * @param  shader  colour shader 
         * @param  scaling   scaling function from data to shade value
         * @param  dataclip  scaling function range adjustment
         * @param  baseColor  colour to use for adjustments in case of
         *                    non-absolute shader
         * @param  nullColor  colour to use for null aux coordinate,
         *                    if null omit such points
         * @param  scaleAlpha  factor to scale output colour alpha by;
         *                     1 means opaque
         */
        public ShadeStamper( Shader shader, Scaling scaling, Subrange dataclip,
                             Color baseColor, Color nullColor,
                             float scaleAlpha ) {
            shader_ = shader;
            scaling_ = scaling;
            dataclip_ = dataclip;
            baseColor_ = baseColor;
            nullColor_ = nullColor;
            scaleAlpha_ = scaleAlpha;
        }

        public Icon createLegendIcon( Outliner outliner ) {
            return createColoredIcon( outliner.getLegendIcon(),
                                      shader_, 0.5f );
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof ShadeStamper ) {
                ShadeStamper other = (ShadeStamper) o;
                return this.shader_.equals( other.shader_ )
                    && this.scaling_.equals( other.scaling_ )
                    && this.dataclip_.equals( other.dataclip_ )
                    && PlotUtil.equals( this.baseColor_, other.baseColor_ )
                    && PlotUtil.equals( this.nullColor_, other.nullColor_ )
                    && this.scaleAlpha_ == other.scaleAlpha_;
            }
            else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            int code = 7301;
            code = 23 * code + shader_.hashCode();
            code = 23 * code + scaling_.hashCode();
            code = 23 * code + dataclip_.hashCode();
            code = 23 * code + PlotUtil.hashCode( baseColor_ );
            code = 23 * code + PlotUtil.hashCode( nullColor_ );
            code = 23 * code + Float.floatToIntBits( scaleAlpha_ );
            return code;
        }
    }

    /**
     * Returns a wrapped icon whose painting is done in the context of
     * a forground colour got by applying a given shader and shading value.
     *
     * @param  base  base icon
     * @param  shader   colour shader
     * @param  value  value in range 0-1 at which shader should be applied
     * @return  coloured icon
     */
    public static Icon createColoredIcon( Icon base, Shader shader,
                                          float value ) {
        float[] rgba = new float[] { 0, 0, 0, 1 };
        shader.adjustRgba( rgba, value );
        Color color = new Color( rgba[ 0 ], rgba[ 1 ], rgba[ 2 ], rgba[ 3 ] );
        return IconUtils.colorIcon( base, color );
    }

    /**
     * Returns an XML reference to the given mode.
     *
     * @param  mode  shape mode
     * @return  &lt;ref&gt; element with content <code>mode.getModeName()</code>
     */
    public static String modeRef( ShapeMode mode ) {
        String mname = mode.getModeName().toString();
        return "<ref id='shading-" + mname + "'>" + mname + "</ref>";
    }

    /**
     * Aggregates the information required to generate a ShapePainter.
     */
    private static class DrawSpec {
        final Surface surface_;
        final DataGeom geom_;
        final DataSpec dataSpec_;
        final Outliner outliner_;
        final Map<AuxScale,Span> auxSpans_;
        final PaperType paperType_;

        /**
         * Constructor.
         *
         * @param  surface  plot surface
         * @param  geom   data position coordinate definition
         * @param  dataSpec  data specification
         * @param  outliner  outline shape
         * @param  auxSpans   data ranges calculated by request
         * @param  paperType  graphics destination type
         */
        DrawSpec( Surface surface, DataGeom geom, DataSpec dataSpec,
                  Outliner outliner, Map<AuxScale,Span> auxSpans,
                  PaperType paperType ) {
            surface_ = surface;
            geom_ = geom;
            dataSpec_ = dataSpec;
            outliner_ = outliner;
            auxSpans_ = auxSpans;
            paperType_ = paperType;
        }

        /**
         * Returns a ShapePainter for use with this DrawSpec.
         * ShapePainter instances are not thread-safe.
         *
         * @return  new shape painter
         */
        public ShapePainter createPainter() {
            if ( paperType_ instanceof PaperType2D ) {
                return outliner_.create2DPainter( surface_, geom_, dataSpec_,
                                                  auxSpans_,
                                                  (PaperType2D) paperType_ );
            }
            else if ( paperType_ instanceof PaperType3D ) {
                return outliner_.create3DPainter( (CubeSurface) surface_,
                                                  geom_, dataSpec_, auxSpans_,
                                                  (PaperType3D) paperType_ );
            }
            else {
                throw new IllegalArgumentException( "paper type" );
            }
        }

        /**
         * Utiliity method that generates a bin plan for this draw spec.
         * 
         * @param  knownPlans  known plans
         * @param  dataStore  data storage
         * @param   new plan
         */
        public Object calculateBinPlan( Object[] knownPlans,
                                        DataStore dataStore ) {
            return outliner_
                  .calculateBinPlan( surface_, geom_, auxSpans_, dataStore,
                                     dataSpec_, knownPlans );
        }

        /**
         * Utility method that creates an unplanned drawing
         * using this spec's state where points are coloured
         * from the data in accordance with a supplied ColorKit.
         *
         * @param  kitFact  factory for object that maps
         *                  data points to data colour
         * @return new drawing
         */
        public Drawing createDrawing( final Supplier<ColorKit> kitFact ) {
            final BiConsumer<TupleSequence,Paper> tuplePainter = (tseq, p) -> {
                ShapePainter painter = createPainter();
                ColorKit kit = kitFact.get();
                while ( tseq.next() ) {
                    Color color = kit.readColor( tseq );
                    if ( color != null ) {
                        painter.paintPoint( tseq, color, p );
                    }
                }
            };
            return new UnplannedDrawing() {
                public void paintData( Paper paper, DataStore dataStore ) {
                    dataStore.getTupleRunner()
                   .paintData( tuplePainter, paper, dataSpec_, dataStore );
                }
                @Override
                public ReportMap getReport( Object plan ) {
                    return outliner_.getReport( plan );
                }
            };
        }
    }

    /**
     * Abstract PlotLayer implementation based on a DrawSpec.
     * Useful for some of the simpler modes in this class.
     */
    private static abstract class ShapePlotLayer extends AbstractPlotLayer {
        private final Outliner outliner_;

        /**
         * Constructor.
         *
         * @param  plotter  plotter 
         * @param  geom   data coordinate specification
         * @param  dataSpec  data specification
         * @param  style   plot style
         * @param  opt   describes layer options
         * @param  outliner  shape outliner
         */
        ShapePlotLayer( ShapePlotter plotter, DataGeom geom, DataSpec dataSpec,
                        Style style, LayerOpt opt, Outliner outliner ) {
            super( plotter, geom, dataSpec, style, opt );
            outliner_ = outliner;
        }

        /**
         * Turns a DrawSpec into a Drawing.
         *
         * @param  drawSpec  drawing specification
         * @return  drawing
         */
        abstract Drawing createDrawing( DrawSpec drawSpec );

        @Override
        public Map<AuxScale,AuxReader> getAuxRangers() {
            Map<AuxScale,AuxReader> map = super.getAuxRangers();
            map.putAll( outliner_.getAuxRangers( getDataGeom() ) );
            return map;
        }

        public Drawing createDrawing( Surface surface,
                                      Map<AuxScale,Span> auxSpans,
                                      PaperType paperType ) {
            DrawSpec drawSpec =
                new DrawSpec( surface, getDataGeom(), getDataSpec(), outliner_,
                              auxSpans, paperType );
            return createDrawing( drawSpec );
        }
    }

    /**
     * Partial drawing implementation for use with BinPlan type plotting modes.
     * Concrete subclasses have to turn a bin plan into a coloured pixel map.
     */
    private static abstract class BinShapeDrawing implements Drawing {
        final DrawSpec drawSpec_;

        /**
         * Constructor.
         *
         * @param  drawSpec  common drawing attributes
         */
        BinShapeDrawing( DrawSpec drawSpec ) {
            drawSpec_ = drawSpec;
        }

        /**
         * Turns a bin plan into a pixel array with associated colour map.
         *
         * @param  plan  bin plan got from <code>calculatePlan</code> method
         * @return  pixel image
         */
        abstract PixelImage createPixelImage( Object plan );

        public Object calculatePlan( Object[] knownPlans,
                                     DataStore dataStore ) {
            return drawSpec_.calculateBinPlan( knownPlans, dataStore );
        }

        int[] getBinCounts( Object plan ) {
            return drawSpec_.outliner_.getBinCounts( plan );
        }

        public void paintData( Object plan, Paper paper, DataStore dataStore ) {
            final PixelImage pim = createPixelImage( plan );
            final Rectangle bounds = drawSpec_.surface_.getPlotBounds();
            drawSpec_.paperType_.placeDecal( paper, new Decal() {
                public void paintDecal( Graphics g ) {
                    pim.paintPixels( g, bounds.getLocation() );
                }
                public boolean isOpaque() {
                    return true;
                }
            } );
        }

        public ReportMap getReport( Object plan ) {
            return drawSpec_.outliner_.getReport( plan );
        }
    }
}
