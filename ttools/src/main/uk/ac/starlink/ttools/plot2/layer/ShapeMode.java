package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.Icon;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot.Shader;
import uk.ac.starlink.ttools.plot.Shaders;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.AuxReader;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Decal;
import uk.ac.starlink.ttools.plot2.Drawing;
import uk.ac.starlink.ttools.plot2.Equality;
import uk.ac.starlink.ttools.plot2.LayerOpt;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Subrange;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;
import uk.ac.starlink.ttools.plot2.geom.CubeSurface;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType;
import uk.ac.starlink.ttools.plot2.paper.PaperType2D;
import uk.ac.starlink.ttools.plot2.paper.PaperType3D;
import uk.ac.starlink.util.IconUtils;

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

    /**
     * List of modes suitable for use with 2D plotting.
     */
    public static final ShapeMode[] MODES_2D = new ShapeMode[] {
        new AutoDensityMode(),
        new FlatMode( false, BIN_THRESH_2D ),
        new FlatMode( true, BIN_THRESH_2D ),
        new CustomDensityMode(),
        new AuxShadingMode( true ),
    };

    /**
     * List of modes suitable for use with 3D plotting.
     */
    public static final ShapeMode[] MODES_3D = new ShapeMode[] {
        new FlatMode( false, NO_BINS ),
        new FlatMode( true, NO_BINS ),
        new CustomDensityMode(),
        new AuxShadingMode( true ),
    };

    /**
     * Simple flat mode.
     */
    public static final ShapeMode FLAT = new FlatMode( false, NO_BINS );

    /**
     * Constructor.
     *
     * @param  name  mode name
     * @param  icon  mode icon
     * @param  extraCoords  data coordinates associated with this mode
     *                      (not positional ones)
     */
    public ShapeMode( String name, Icon icon, Coord[] extraCoords ) {
        name_ = name;
        icon_ = icon;
        extraCoords_ = extraCoords;
    }

    public String getModeName() {
        return name_;
    }

    public Icon getModeIcon() {
        return icon_;
    }

    /**
     * Returns the additional coordinates associated with this mode.
     *
     * @return   array of non-positional coordinates associated with colouring
     */
    public Coord[] getExtraCoords() {
        return extraCoords_;
    }

    /**
     * Returns style configuration keys assocaited with this mode.
     * These keys will be used in the config map supplied to
     * {@link #createStamper}.
     *
     * @return  array of config keys for mode
     */
    public abstract ConfigKey[] getConfigKeys();

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
     * Mode for painting shapes in a single flat colour.
     */
    private static class FlatMode extends ShapeMode {
        private final int binThresh_;
        private final boolean transparent_;

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
                   transparent ? ResourceIcon.MODE_TRANSPARENT
                               : ResourceIcon.MODE_FLAT,
                   new Coord[ 0 ] );
            transparent_ = transparent;
            binThresh_ = binThresh;
        }

        public ConfigKey[] getConfigKeys() {
            List<ConfigKey> keyList = new ArrayList<ConfigKey>();
            keyList.add( StyleKeys.COLOR );
            if ( transparent_ ) {
                keyList.add( StyleKeys.OPAQUE );
            }
            return keyList.toArray( new ConfigKey[ 0 ] );
        }

        public Stamper createStamper( ConfigMap config ) {
            final Color color;
            Color baseColor = config.get( StyleKeys.COLOR );
            if ( transparent_ ) {
                int opaque = config.get( StyleKeys.OPAQUE ).intValue();
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
                Outliner.ShapePainter painter = drawSpec_.painter_;
                TupleSequence tseq =
                    dataStore.getTupleSequence( drawSpec_.dataSpec_ );
                while ( tseq.next() ) {
                    painter.paintPoint( tseq, color_, paper );
                }
            };
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
                IndexColorModel colorModel = createMaskColorModel( color_ );
                return new PixelImage( counts, colorModel );
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
        }

        /**
         * Returns a 2-colour indexed colour model.
         *
         * @param  color  non-blank colour
         * @return  colour map with two entries,
         *          <code>color</code> and transparent
         */
        private static IndexColorModel createMaskColorModel( Color color ) {
            IndexColorModel model =
                new IndexColorModel( 1, 2,
                                     new byte[] { 0, (byte) color.getRed() },
                                     new byte[] { 0, (byte) color.getGreen() },
                                     new byte[] { 0, (byte) color.getBlue() },
                                     0 );
            assert model.getTransparency() == Transparency.BITMASK;
            assert model.getTransparentPixel() == 0;
            return model;
        }

        /**
         * Stamper implementation for flat colouring.
         */
        private static class FlatStamper implements Stamper {
            final Color color_;
 
            /**
             * Constructor.
             *
             * @param   color   fixed colour
             */
            FlatStamper( Color color ) {
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
        static final int COLOR_MAP_SIZE = 128;

        /**
         * Constructor.
         *
         * @param  name  mode name
         * @param  icon  mode icon
         */
        AbstractDensityMode( String name, Icon icon ) {
            super( name, icon, new Coord[ 0 ] );
        }

        public PlotLayer createLayer( ShapePlotter plotter, ShapeForm form,
                                      DataGeom geom, DataSpec dataSpec,
                                      Outliner outliner, Stamper stamper ) {
            final Shader shader = ((DensityStamper) stamper).shader_;
            final Scaling scaling = ((DensityStamper) stamper).scaling_;
            ShapeStyle style = new ShapeStyle( outliner, stamper );
            LayerOpt opt = LayerOpt.OPAQUE;
            return new ShapePlotLayer( plotter, geom, dataSpec, style, opt,
                                       outliner ) {
                public Drawing createDrawing( DrawSpec drawSpec ) {
                    return new DensityDrawing( drawSpec, shader, scaling );
                }
            };
        }

        /**
         * Density mode Drawing implementation.
         */
        private static class DensityDrawing extends BinShapeDrawing {
            private final Shader shader_;
            private final Scaling scaling_;

            /**
             * Constructor.
             * 
             * @param  drawSpec  common drawing attributes
             * @param  shader  determines colours in colour map
             * @param  scaling  quantitative mapping from counts to colour
             */
            DensityDrawing( DrawSpec drawSpec, Shader shader,
                            Scaling scaling ) {
                super( drawSpec );
                shader_ = shader;
                scaling_ = scaling;
            }

            PixelImage createPixelImage( Object plan ) {

                /* Clone this array before modifying it in-place.
                 * Trashing the data wouldn't hurt this time,
                 * the plan may get re-used later. */
                int[] counts = getBinCounts( plan ).clone();
                IndexColorModel colorModel = createColorModel( shader_ );
                scaleLevels( counts, colorModel.getMapSize() - 1 );
                return new PixelImage( counts, colorModel );
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
                int n = buf.length;
                int max = 0;
                for ( int i = 0; i < n; i++ ) {
                    max = Math.max( max, buf[ i ] );
                }
                CountScaler scaler = scaling_.createScaler( max, nlevel );

                /* Leave 0 as 0 - this is transparent (no plot).
                 * Values >= 1 map to range 1..nlevel. */
                double nl1 = nlevel - 1;
                for ( int i = 0; i < n; i++ ) {
                    int b = buf[ i ];
                    if ( b > 0 ) {
                        buf[ i ] =
                            1 + (int) Math.round( nl1 * scaler.scale( b - 1 ) );
                    }
                }
            }
        }

        /**
         * Returns an indexed colour model whose entries range from one end
         * to the other of a given shader object.
         *
         * @param  shader   absolute shader
         * @return  colour model
         */
        private static IndexColorModel createColorModel( Shader shader ) {
            assert shader.isAbsolute();
            byte[] red = new byte[ COLOR_MAP_SIZE ];
            byte[] green = new byte[ COLOR_MAP_SIZE ];
            byte[] blue = new byte[ COLOR_MAP_SIZE ];
            float[] rgb = new float[ 4 ];
            float scale = 1f / ( COLOR_MAP_SIZE - 1 );
            int iTransparent = 0;
            for ( int i = 1; i < COLOR_MAP_SIZE; i++ ) {
                assert i != iTransparent;
                rgb[ 3 ] = 1f;
                double level = ( i - 1 ) * scale;
                shader.adjustRgba( rgb, (float) level );
                red[ i ] = (byte) ( rgb[ 0 ] * 255 );
                green[ i ] = (byte) ( rgb[ 1 ] * 255 );
                blue[ i ] = (byte) ( rgb[ 2 ] * 255 );
            }

            /* Set the transparent colour to transparent white
             * not transparent black.
             * In most cases this makes no difference, but for rendering
             * targets which ignore transparency (PostScript) it can
             * help a bit, though such renderers are not going to work
             * well for multi-layer plots. */
            red[ iTransparent ] = (byte) 0xff;
            green[ iTransparent ] = (byte) 0xff;
            blue[ iTransparent ] = (byte) 0xff;
            IndexColorModel model =
                new IndexColorModel( 8, COLOR_MAP_SIZE, red, green, blue,
                                     iTransparent );
            assert model.getTransparency() == Transparency.BITMASK;
            assert model.getMapSize() == COLOR_MAP_SIZE;
            assert model.getTransparentPixel() == 0;
            assert model.getPixelSize() == 8;
            return model;
        }
  
        /**
         * Defines a scaling stragy.
         */
        @Equality
        static interface Scaling {

            /**
             * Returns a count scaler to use for a given maximum input count
             * value and number of levels.
             *
             * @param   max  maximum value in input count data
             *               (minimum is implicitly zero)
             * @param  nlevel  number of levels in output array
             * @return  new scaler
             */
            CountScaler createScaler( int max, int nlevel );
        }

        /**
         * Stamper implementation for density mode.
         */
        static class DensityStamper implements Stamper {
            final Shader shader_;
            final Scaling scaling_;

            /**
             * Constructor.
             *
             * @param   shader  colour shader
             * @param  scaling  count scaling strategy
             */
            DensityStamper( Shader shader, Scaling scaling ) {
                shader_ = shader;
                scaling_ = scaling;
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
                        && this.scaling_.equals( other.scaling_ );
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
                return code;
            }
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

        /** Auto scaling - asinh-like with a small but visible delta. */
        private static final Scaling AUTO_SCALING = new Scaling() {
            public CountScaler createScaler( int max, int nlevel ) {
                return CountScaler.createScaler( max, 0.0625 );
            }
        };

        /**
         * Constructor.
         */
        AutoDensityMode() {
            super( "auto", ResourceIcon.MODE_AUTO );
        }

        public ConfigKey[] getConfigKeys() {
            return new ConfigKey[] {
                StyleKeys.COLOR,
            };
        }

        public Stamper createStamper( ConfigMap config ) {
            Color baseColor = config.get( StyleKeys.COLOR );
            Shader baseShader = Shaders.stretch( Shaders.SCALE_V, 1f, 0.2f );
            Shader densityShader =
                Shaders.applyShader( baseShader, baseColor, COLOR_MAP_SIZE );
            return new DensityStamper( densityShader, AUTO_SCALING );
        }
    }

    /**
     * Density mode variant which allows user selection of various parameters
     * affecting the appearance.
     */
    private static class CustomDensityMode extends AbstractDensityMode {

        /**
         * Constructor.
         */
        CustomDensityMode() {
            super( "density", ResourceIcon.MODE_DENSITY );
        }

        public ConfigKey[] getConfigKeys() {
            return new ConfigKey[] {
                StyleKeys.COLOR,
                StyleKeys.DENSITY_SHADER,
                StyleKeys.DENSITY_SHADER_CLIP,
                StyleKeys.DENSITY_LOG,
                StyleKeys.DENSITY_FLIP,
                StyleKeys.DENSITY_SUBRANGE,
            };
        }

        public Stamper createStamper( ConfigMap config ) {
            Color baseColor = config.get( StyleKeys.COLOR );
            Shader baseShader =
                StyleKeys.createShader( config, StyleKeys.DENSITY_SHADER,
                                                StyleKeys.DENSITY_SHADER_CLIP );
            boolean logFlag = config.get( StyleKeys.DENSITY_LOG );
            boolean flipFlag = config.get( StyleKeys.DENSITY_FLIP );
            Subrange subrange = config.get( StyleKeys.DENSITY_SUBRANGE );
            Shader densityShader =
                Shaders.applyShader( baseShader, baseColor, COLOR_MAP_SIZE );
            if ( flipFlag ) {
                densityShader = Shaders.invert( densityShader );
            }
            Scaling scaling = new CustomScaling( subrange, logFlag, false );
            return new DensityStamper( densityShader, scaling );
        }

        /**
         * Scaling strategy that can be adjusted by user preference.
         */
        private static class CustomScaling implements Scaling {
            final Subrange subrange_;
            final boolean logFlag_;
            final boolean showAll_;

            /**
             * Constructor.
             *
             * @param  subrange  defines a region of the full data range
             *                   over which the colour scale should run
             * @param  logFlag  true for log scaling, false for linear
             * @param  showAll  I'm not really sure what this does
             */
            CustomScaling( Subrange subrange, boolean logFlag,
                           boolean showAll ) {
                subrange_ = subrange;
                logFlag_ = logFlag;
                showAll_ = showAll;
            }

            public CountScaler createScaler( int max, int nlevel ) {
                double[] range =
                    PlotUtil.scaleRange( 1, max, subrange_, logFlag_ );
                final int lo = (int) Math.round( range[ 0 ] );
                final int hi = (int) Math.round( range[ 1 ] );
                final double dlo = lo;
                final double dhi = hi;
                final double min = ( showAll_ || lo == 1 )
                                 ? 1.0 / (double) nlevel
                                 : 0;
                final double scale = logFlag_
                                   ? ( 1.0 - min ) / Math.log( dhi / dlo )
                                   : ( 1.0 - min ) / ( dhi - dlo );
                return new CountScaler() {
                    public double scale( int c ) {
                        if ( c == 0 ) {
                            return 0;
                        }
                        else if ( c <= lo ) {
                            return min;
                        }
                        else if ( c >= hi ) {
                            return 1;
                        }
                        else {
                            double val =
                                 min + scale * ( logFlag_ ? Math.log( c / dlo )
                                                          : ( c - dlo ) );
                            assert val >= 0 && val <= 1 : val;
                            return val;
                        }
                    }
                };
            }

            @Override
            public boolean equals( Object o ) {
                if ( o instanceof CustomScaling ) {
                    CustomScaling other = (CustomScaling) o;
                    return this.subrange_.equals( other.subrange_ )
                        && this.logFlag_ == other.logFlag_
                        && this.showAll_ == other.showAll_;
                }
                else {
                    return false;
                }
            }

            @Override
            public int hashCode() {
                int code = 99;
                code = 23 * code + subrange_.hashCode();
                code = 23 * code + ( logFlag_ ? 5 : 7 );
                code = 23 * code + ( showAll_ ? 13 : 19 );
                return code;
            }
        }
    }

    /**
     * Mode for colouring shapes according to an additional data coordinate.
     */
    private static class AuxShadingMode extends ShapeMode {
        private final boolean transparent_;

        private static final AuxScale SCALE = AuxScale.COLOR;
        private static final FloatingCoord SHADE_COORD =
            FloatingCoord.createCoord( SCALE.getName(), "Colour coordinate",
                                       false );

        /**
         * Constructor.
         *
         * @param   transparent   if true, allow variable transparency
         */
        AuxShadingMode( boolean transparent ) {
            super( "aux", ResourceIcon.MODE_AUX, new Coord[] { SHADE_COORD } );
            transparent_ = transparent;
        }

        public ConfigKey[] getConfigKeys() {
            List<ConfigKey> list = new ArrayList<ConfigKey>();
            list.add( StyleKeys.AUX_SHADER );
            list.add( StyleKeys.AUX_SHADER_CLIP );
            if ( transparent_ ) {
                list.add( StyleKeys.AUX_OPAQUE );
            }
            list.add( StyleKeys.SHADE_LOG );
            list.add( StyleKeys.SHADE_FLIP );
            list.add( StyleKeys.SHADE_NULL_COLOR );
            return list.toArray( new ConfigKey[ 0 ] );
        }

        public Stamper createStamper( ConfigMap config ) {
            Shader shader =
                StyleKeys.createShader( config, StyleKeys.AUX_SHADER,
                                                StyleKeys.AUX_SHADER_CLIP );
            double opaque = transparent_
                          ? config.get( StyleKeys.AUX_OPAQUE )
                          : 1;
            float scaleAlpha = 1f / (float) opaque;
            boolean shadeLog = config.get( StyleKeys.SHADE_LOG );
            boolean shadeFlip = config.get( StyleKeys.SHADE_FLIP );
            Color baseColor = config.get( StyleKeys.COLOR );
            Color nullColor = config.get( StyleKeys.SHADE_NULL_COLOR );
            return new ShadeStamper( shader, shadeLog, shadeFlip, baseColor,
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
            final boolean shadeLog = shStamper.shadeLog_;
            final boolean shadeFlip = shStamper.shadeFlip_;
            final Color baseColor = shStamper.baseColor_;
            final Color nullColor = shStamper.nullColor_;
            final float scaleAlpha = shStamper.scaleAlpha_;
            Style style = new ShapeStyle( outliner, stamper );
            LayerOpt opt = scaleAlpha == 1f ? LayerOpt.OPAQUE
                                            : LayerOpt.NO_SPECIAL;
            return new AbstractPlotLayer( plotter, geom, dataSpec,
                                          style, opt ) {
                @Override
                public Map<AuxScale,AuxReader> getAuxRangers() {
                    Map<AuxScale,AuxReader> map = super.getAuxRangers();
                    final double[] dpos = new double[ geom.getDataDimCount() ];
                    final Point gpos = new Point();
                    map.put( SCALE, new AuxReader() {
                        public void updateAuxRange( Surface surface,
                                                    TupleSequence tseq,
                                                    Range range ) {
                            if ( geom.readDataPos( tseq, 0, dpos ) &&
                                 surface.dataToGraphics( dpos, true, gpos ) ) {
                                range.submit( SHADE_COORD
                                             .readDoubleCoord( tseq,
                                                               iShadeCoord ) );
                            }
                        }
                    } );
                    map.putAll( outliner.getAuxRangers( geom ) );
                    return map;
                }
                public Drawing createDrawing( Surface surface,
                                              Map<AuxScale,Range> auxRanges,
                                              PaperType paperType ) {
                    Range shadeRange = auxRanges.get( SCALE );
                    RangeScaler scaler =
                        RangeScaler.createScaler( shadeLog, shadeFlip,
                                                  shadeRange );
                    ColorKit kit = new ColorKit( iShadeCoord, shader, scaler,
                                                 baseColor, nullColor,
                                                 scaleAlpha );
                    DrawSpec drawSpec =
                        new DrawSpec( surface, geom, dataSpec, outliner,
                                      auxRanges, paperType );
                    return new AuxDrawing( drawSpec, kit );
                }
            };
        }

        /**
         * Encapsulates information about how to colour data points for
         * AuxShadingMode.
         */
        private static class ColorKit {

            private final int icShade_;
            private final Shader shader_;
            private final RangeScaler scaler_;
            private final Color scaledNullColor_;
            private final float scaleAlpha_;
            private final float[] baseRgba_;
            private final float[] rgba_;
            private Color lastColor_;
            private float lastScale_;

            /**
             * Constructor.
             *
             * @param  icShade  column index in tuple sequence at which
             *                  shade values are found
             * @param  shader   colour shader
             * @param  scaler   scales data values to normalised shader range
             * @param  baseColor  colour to adjust for non-absolute shaders
             * @param  nullColor  colour to use in case of null
             *                    aux coordinate; if null, such points are
             *                    not plotted
             * @param  scaleAlpha  alpha scaling for output colours;
             *                     1 means opaque
             */
            ColorKit( int icShade, Shader shader, RangeScaler scaler,
                      Color baseColor, Color nullColor, float scaleAlpha ) {
                icShade_ = icShade;
                shader_ = shader;
                scaler_ = scaler;
                scaleAlpha_ = scaleAlpha;
                scaledNullColor_ = nullColor == null
                                 ? null
                                 : toOutputColor( nullColor
                                                 .getRGBComponents( null ) );
                baseRgba_ = baseColor.getRGBComponents( null );
                rgba_ = new float[ 4 ];
                lastColor_ = scaledNullColor_;
                lastScale_ = Float.NaN;
            }

            /**
             * Returns the colour to use for plotting the current row of
             * a supplied tuple sequence.
             *
             * @param  tseq  tuple sequence positioned at the row of interest
             * @return  plotting colour, or null to omit point
             */
            Color readColor( TupleSequence tseq ) {
                double auxVal = SHADE_COORD.readDoubleCoord( tseq, icShade_ );
                float scaleVal = (float) scaler_.scale( auxVal );

                /* If null input return special null output value. */
                if ( Float.isNaN( scaleVal ) ) {
                    return scaledNullColor_;
                }

                /* If no change in input return the last output for
                 * efficiency (may get many the same in sequence). */
                else if ( lastScale_ == scaleVal ) {
                    return lastColor_;
                }

                /* Otherwise calculate, store and return a colour based on
                 * scaled input aux coordinate. */
                else {
                    System.arraycopy( baseRgba_, 0, rgba_, 0, 4 );
                    shader_.adjustRgba( rgba_, scaleVal );
                    Color color = toOutputColor( rgba_ );
                    lastScale_ = scaleVal;
                    lastColor_ = color;
                    return color;
                }
            }

            /**
             * Returns a colour for output based on RGBA values.
             * Alpha scaling is applied.  Null is returned for a completely
             * transparent colour.
             *
             * @param  4-element R,G,B,A array (elements in range 0..1)
             * @return  colour for output, or null
             */
            private Color toOutputColor( float[] rgba ) {
                float alpha = rgba[ 3 ];
                return alpha > 0 ? new Color( rgba[ 0 ], rgba[ 1 ], rgba[ 2 ],
                                              alpha * scaleAlpha_ )
                                 : null;
            }
        }

        /**
         * Drawing implementation used by AuxShadingMode.
         */
        private static class AuxDrawing extends UnplannedDrawing {
            private final DrawSpec drawSpec_;
            private final ColorKit kit_;

            /**
             * Constructor.
             *
             * @param  drawSpec  common drawing attributes
             * @param  kit  object that knows how to colour points per row
             */
            AuxDrawing( DrawSpec drawSpec, ColorKit kit ) {
                drawSpec_ = drawSpec;
                kit_ = kit;
            }

            public void paintData( Paper paper, DataStore dataStore ) { 
                Outliner.ShapePainter painter = drawSpec_.painter_;
                TupleSequence tseq =
                    dataStore.getTupleSequence( drawSpec_.dataSpec_ );
                while ( tseq.next() ) {
                    Color color = kit_.readColor( tseq );
                    if ( color != null ) {
                        painter.paintPoint( tseq, color, paper );
                    }
                }
            }
        }

        /**
         * Stamper implementation for use with AuxShadingMode.
         */
        private static class ShadeStamper implements Stamper {
            final Shader shader_;
            final boolean shadeLog_;
            final boolean shadeFlip_;
            final Color baseColor_;
            final Color nullColor_;
            final float scaleAlpha_;

            /**
             * Constructor.
             *
             * @param  shader  colour shader 
             * @param  shadeLog  true for logarithmic shading scale,
             *                   false for linear
             * @param  shadeFlip  true to invert direction of shading scale
             * @param  baseColor  colour to use for adjustments in case of
             *                    non-absolute shader
             * @param  nullColor  colour to use for null aux coordinate,
             *                    if null omit such points
             * @param  scaleAlpha  factor to scale output colour alpha by;
             *                     1 means opaque
             */
            public ShadeStamper( Shader shader, boolean shadeLog,
                                 boolean shadeFlip, Color baseColor,
                                 Color nullColor, float scaleAlpha ) {
                shader_ = shader;
                shadeLog_ = shadeLog;
                shadeFlip_ = shadeFlip;
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
                        && this.shadeLog_ == other.shadeLog_
                        && this.shadeFlip_ == other.shadeFlip_
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
                code = 23 * code + ( shadeLog_ ? 5 : 7 );
                code = 23 * code + ( shadeFlip_ ? 11 : 13 );
                code = 23 * code + PlotUtil.hashCode( baseColor_ );
                code = 23 * code + PlotUtil.hashCode( nullColor_ );
                code = 23 * code + Float.floatToIntBits( scaleAlpha_ );
                return code;
            }
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
     * Aggregates the information required to generate a ShapePainter.
     */
    private static class DrawSpec {
        final Surface surface_;
        final DataGeom geom_;
        final DataSpec dataSpec_;
        final Outliner outliner_;
        final Map<AuxScale,Range> auxRanges_;
        final PaperType paperType_;
        final Outliner.ShapePainter painter_;

        /**
         * Constructor.
         *
         * @param  surface  plot surface
         * @param  geom   data position coordinate definition
         * @param  dataSpec  data specification
         * @param  outliner  outline shape
         * @param  auxRanges   data ranges calculated by request
         * @param  paperType  graphics destination type
         */
        DrawSpec( Surface surface, DataGeom geom, DataSpec dataSpec,
                  Outliner outliner, Map<AuxScale,Range> auxRanges,
                  PaperType paperType ) {
            surface_ = surface;
            geom_ = geom;
            dataSpec_ = dataSpec;
            outliner_ = outliner;
            auxRanges_ = auxRanges;
            paperType_ = paperType;
            if ( paperType instanceof PaperType2D ) {
                painter_ = outliner.create2DPainter( surface, geom, auxRanges,
                                                     (PaperType2D) paperType );
            }
            else if ( paperType instanceof PaperType3D ) {
                painter_ = outliner.create3DPainter( (CubeSurface) surface,
                                                     geom, auxRanges,
                                                     (PaperType3D) paperType );
            }
            else {
                throw new IllegalArgumentException( "paper type" );
            }
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
                                      Map<AuxScale,Range> auxRanges,
                                      PaperType paperType ) {
            DrawSpec drawSpec =
                new DrawSpec( surface, getDataGeom(), getDataSpec(), outliner_,
                              auxRanges, paperType );
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
            return drawSpec_.outliner_
                  .calculateBinPlan( drawSpec_.surface_, drawSpec_.geom_,
                                     drawSpec_.auxRanges_, dataStore,
                                     drawSpec_.dataSpec_, knownPlans );
        }

        int[] getBinCounts( Object plan ) {
            return drawSpec_.outliner_.getBinCounts( plan );
        }

        public void paintData( Object plan, Paper paper, DataStore dataStore ) {
            final PixelImage pim = createPixelImage( plan );
            drawSpec_.paperType_.placeDecal( paper, new Decal() {
                public void paintDecal( Graphics g ) {
                    paintPixels( g, pim );
                }
                public boolean isOpaque() {
                    return true;
                }
            } );
        }

        /**
         * Does the work for painting the image for this drawing (decal).
         *
         * @param  g  graphics context
         * @param  pim  coloured pixel image
         */
        private void paintPixels( Graphics g, PixelImage pim ) {
            int[] pixels = pim.pixels_;
            IndexColorModel colorModel = pim.colorModel_;
            final Rectangle bounds = drawSpec_.surface_.getPlotBounds();
            int width = bounds.width;
            int height = bounds.height;
            final BufferedImage image =
                new BufferedImage( width, height,
                                   BufferedImage.TYPE_BYTE_INDEXED,
                                   colorModel );
            WritableRaster raster = image.getRaster();
            assert raster.getNumBands() == 1;
            raster.setSamples( 0, 0, width, height, 0, pixels );
            assert raster.getWidth() == width;
            assert raster.getHeight() == height;
            g.drawImage( image, bounds.x, bounds.y, null );
        }
    }

    /**
     * Information for generating a colour-mapped image.
     */
    private static class PixelImage {
        final int[] pixels_;
        final IndexColorModel colorModel_;

        /**
         * Constructor.
         *
         * @param  pixels  pixel array,
         *                 all values to fall in range of colour model
         * @param  colorModel  indexed colour model
         */
        PixelImage( int[] pixels, IndexColorModel colorModel ) {
            pixels_ = pixels;
            colorModel_ = colorModel;
        }
    }
}
