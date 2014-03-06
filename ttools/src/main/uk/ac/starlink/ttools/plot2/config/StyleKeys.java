package uk.ac.starlink.ttools.plot2.config;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.ListModel;
import uk.ac.starlink.ttools.gui.ColorComboBox;
import uk.ac.starlink.ttools.gui.DashComboBox;
import uk.ac.starlink.ttools.gui.MarkStyleSelectors;
import uk.ac.starlink.ttools.gui.ThicknessComboBox;
import uk.ac.starlink.ttools.gui.ShaderListCellRenderer;
import uk.ac.starlink.ttools.plot.BarStyle;
import uk.ac.starlink.ttools.plot.BarStyles;
import uk.ac.starlink.ttools.plot.ErrorMode;
import uk.ac.starlink.ttools.plot.ErrorRenderer;
import uk.ac.starlink.ttools.plot.MarkShape;
import uk.ac.starlink.ttools.plot.MarkStyle;
import uk.ac.starlink.ttools.plot.Shader;
import uk.ac.starlink.ttools.plot.Shaders;
import uk.ac.starlink.ttools.plot.Styles;
import uk.ac.starlink.ttools.plot2.Anchor;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Subrange;
import uk.ac.starlink.ttools.plot2.layer.LevelMode;
import uk.ac.starlink.ttools.plottask.ColorParameter;
import uk.ac.starlink.ttools.plottask.DashParameter;
import uk.ac.starlink.ttools.plottask.NamedObjectParameter;
import uk.ac.starlink.util.gui.RenderingComboBox;

/**
 * Contains many common config keys and associated utility methods.
 *
 * @author   Mark Taylor
 * @since    25 Feb 2013
 */
public class StyleKeys {

    private static final MarkShape[] SHAPES = createShapes();

    /** Config key for marker shape. */
    public static final ConfigKey<MarkShape> MARK_SHAPE =
            new OptionConfigKey<MarkShape>( new ConfigMeta( "shape", "Shape" ),
                                            MarkShape.class, SHAPES,
                                            MarkShape.FILLED_CIRCLE ) {
        public Specifier<MarkShape> createSpecifier() {
            return new ComboBoxSpecifier<MarkShape>( MarkStyleSelectors
                                                    .createShapeSelector() );
        }
    };

    /** Config key for marker size. */
    public static final ConfigKey<Integer> SIZE =
            new IntegerConfigKey( new ConfigMeta( "size", "Size" ), 1 ) {
        public Specifier<Integer> createSpecifier() {
            return new ComboBoxSpecifier<Integer>( MarkStyleSelectors
                                                  .createSizeSelector() );
        }
    };

    /** Config key for style colour. */
    public static final ConfigKey<Color> COLOR =
        new ColorConfigKey( new ConfigMeta( "color", "Colour" ),
                            Color.RED, false );

    /** Config key for the opacity limit of transparent plots.
     *  This is the number of times a point has to be hit to result in
     *  a saturated (opaque) pixel. */
    public static final ConfigKey<Double> OPAQUE =
        DoubleConfigKey
       .createSliderKey( new ConfigMeta( "opaque", "Opaque limit" ),
                         4, 1, 10000, true );

    /** Config key for the opacity limit of auxiliary shaded plots. */
    public static final ConfigKey<Double> AUX_OPAQUE =
        DoubleConfigKey
       .createSliderKey( new ConfigMeta( "opaque", "Opaque limit" ),
                         1, 1, 1000, true );

    /** Config key for transparency level of adaptive transparent plots. */
    public static final ConfigKey<Double> TRANSPARENT_LEVEL =
        DoubleConfigKey
       .createSliderKey( new ConfigMeta( "translevel", "Transparency Level" ),
                         0.1, 0.001, 2, true );

    /** Config key for "normal" transparency - it's just 1-alpha. */
    public static final ConfigKey<Double> TRANSPARENCY =
        DoubleConfigKey.createSliderKey( new ConfigMeta( "transparency",
                                                         "Transparency" ),
                                         0, 0, 1, false );

    /** Config key for line thickness. */
    private static final ConfigKey<Integer> THICKNESS = createThicknessKey( 1 );

    /** Config key for line dash style. */
    public static final ConfigKey<float[]> DASH =
            new NamedObjectKey<float[]>( new ConfigMeta( "dash", "Dash" ),
                                         float[].class, null,
                                         new DashParameter( "dash" ) ) {
        public Specifier<float[]> createSpecifier() {
            return new ComboBoxSpecifier<float[]>( new DashComboBox() );
        }
    };

    /** Config key for axis grid colour. */
    public static final ConfigKey<Color> GRID_COLOR =
        new ColorConfigKey( new ConfigMeta( "gridcolor", "Grid Colour" ),
                            Color.LIGHT_GRAY, false );

    /** Config key for axis label colour. */
    public static final ConfigKey<Color> AXLABEL_COLOR =
        new ColorConfigKey( new ConfigMeta( "labelcolor", "Label Colour" ),
                            Color.BLACK, false );

    private static final BarStyle.Form[] BARFORMS = new BarStyle.Form[] {
        BarStyle.FORM_FILLED,
        BarStyle.FORM_OPEN,
        BarStyle.FORM_TOP,
        BarStyle.FORM_SPIKE,
    };

    /** Config key for histogram bar style. */
    public static final ConfigKey<BarStyle.Form> BAR_FORM =
            new OptionConfigKey<BarStyle.Form>( new ConfigMeta( "barform",
                                                                "Bar Form" ),
                                                BarStyle.Form.class,
                                                BARFORMS ) {
        public Specifier<BarStyle.Form> createSpecifier() {
            JComboBox formSelector = new RenderingComboBox( BARFORMS ) {
                protected Icon getRendererIcon( Object form ) {
                    return BarStyles.getIcon( (BarStyle.Form) form );
                }
            };
            return new ComboBoxSpecifier<BarStyle.Form>( formSelector );
        }
    };

    /** Config key for line antialiasing. */
    public static final ConfigKey<Boolean> ANTIALIAS =
        new BooleanConfigKey( new ConfigMeta( "antialias", "Antialiasing" ),
                              false );

    /** Config key for axis grid antialiasing. */
    public static final ConfigKey<Boolean> GRID_ANTIALIAS =
        new BooleanConfigKey( new ConfigMeta( "gridaa", "Antialiasing" ),
                              PlotUtil.getDefaultTextAntialiasing() );

    /** Config key for text anchor positioning. */
    public static final ConfigKey<Anchor> ANCHOR =
        new OptionConfigKey<Anchor>( new ConfigMeta( "anchor", "Anchor" ),
                                     Anchor.class,
                                     new Anchor[] {
                                         Anchor.W, Anchor.E, Anchor.N, Anchor.S,
                                     } );

    /** Config key for scaling level mode. */ 
    public static final ConfigKey<LevelMode> LEVEL_MODE =
        new OptionConfigKey<LevelMode>( new ConfigMeta( "scaling", "Scaling" ),
                                        LevelMode.class, LevelMode.MODES,
                                        LevelMode.LINEAR );

    /** Config key for vector marker style. */
    public static final MultiPointConfigKey VECTOR_SHAPE =
        new MultiPointConfigKey( new ConfigMeta( "arrow", "Arrow" ),
                                 ErrorRenderer.getOptionsVector(),
                                 new ErrorMode[] { ErrorMode.UPPER } );

    /** Config key for ellipse marker style. */
    public static final MultiPointConfigKey ELLIPSE_SHAPE =
        new MultiPointConfigKey( new ConfigMeta( "ellipse", "Ellipse" ),
                                 ErrorRenderer.getOptionsEllipse(),
                                 new ErrorMode[] { ErrorMode.SYMMETRIC,
                                                   ErrorMode.SYMMETRIC } );

    /** Config key for 1d (vertical) error marker style. */
    public static final MultiPointConfigKey ERROR_SHAPE_1D =
        new MultiPointConfigKey( new ConfigMeta( "errorbar", "Error Bar" ),
                                 ErrorRenderer.getOptions1d(),
                                 new ErrorMode[] { ErrorMode.SYMMETRIC } );

    /** Config key for 2d error marker style. */
    public static final MultiPointConfigKey ERROR_SHAPE_2D =
        new MultiPointConfigKey( new ConfigMeta( "errorbar", "Error Bar" ),
                                 ErrorRenderer.getOptions2d(),
                                 new ErrorMode[] { ErrorMode.SYMMETRIC,
                                                   ErrorMode.SYMMETRIC } );

    /** Config key for 3d error marker style. */
    public static final MultiPointConfigKey ERROR_SHAPE_3D =
        new MultiPointConfigKey( new ConfigMeta( "errorbar", "Error Bar" ),
                                 ErrorRenderer.getOptions3d(),
                                 new ErrorMode[] { ErrorMode.SYMMETRIC,
                                                   ErrorMode.SYMMETRIC,
                                                   ErrorMode.SYMMETRIC } );

    /** Config key for aux shader colour ramp. */
    public static final ConfigKey<Shader> AUX_SHADER =
        new ShaderConfigKey( new ConfigMeta( "shader", "Shader" ),
                             createAuxShaders(), Shaders.LUT_RAINBOW );

    /** Config key for restricting the range of an aux shader colour map. */
    public static final ConfigKey<Subrange> AUX_SHADER_CLIP =
        new SubrangeConfigKey( new ConfigMeta( "shadeclip", "Shade Clip" ) );

    /** Config key for aux shader logarithmic flag. */
    public static final ConfigKey<Boolean> SHADE_LOG =
        new BooleanConfigKey( new ConfigMeta( "shadelog", "Shade Log" ) );

    /** Config key for aux shader axis inversion flag. */
    public static final ConfigKey<Boolean> SHADE_FLIP =
        new BooleanConfigKey( new ConfigMeta( "shadeflip", "Shade Flip" ) );

    /** Config key for aux shader null colour. */
    public static final ConfigKey<Color> SHADE_NULL_COLOR =
        new ColorConfigKey( new ConfigMeta( "nullcolor", "Null Colour" ),
                            Color.GRAY, true );

    /** Config key for aux shader lower limit. */
    public static final ConfigKey<Double> SHADE_LOW =
        DoubleConfigKey
       .createTextKey( new ConfigMeta( "auxmin", "Aux Lower Limit" ) );

    /** Config key for aux shader upper limit. */
    public static final ConfigKey<Double> SHADE_HIGH =
        DoubleConfigKey
       .createTextKey( new ConfigMeta( "auxmax", "Aux Upper Limit" ) );

    /** Config key for aux shader subrange. */
    public static final ConfigKey<Subrange> SHADE_SUBRANGE =
        new SubrangeConfigKey( new ConfigMeta( "auxscale", "Aux Sub-range" ) );

    /** Config key for density shader colour map. */
    public static final ConfigKey<Shader> DENSITY_SHADER =
        new ShaderConfigKey( new ConfigMeta( "densemap", "Map" ),
                             createDensityShaders(),
                             createDensityShaders()[ 0 ] );

    /** Config key for restricting the range of a density shader colour map. */
    public static final ConfigKey<Subrange> DENSITY_SHADER_CLIP =
        new SubrangeConfigKey( new ConfigMeta( "denseclip", "Map clip" ) );
                             
    /** Config key for density shader subrange. */
    public static final ConfigKey<Subrange> DENSITY_SUBRANGE =
        new SubrangeConfigKey( new ConfigMeta( "densescale",
                                               "Sub-range" ) );

    /** Config key for density shader log flag. */
    public static final ConfigKey<Boolean> DENSITY_LOG =
        new BooleanConfigKey( new ConfigMeta( "denselog", "Log" ),
                              Boolean.TRUE );

    /** Config key for density shader flip flag. */
    public static final ConfigKey<Boolean> DENSITY_FLIP =
        new BooleanConfigKey( new ConfigMeta( "denseflip", "Flip" ),
                              Boolean.FALSE );

    /** Config key for sized marker scaling. */
    public static final ConfigKey<Double> SCALE =
        DoubleConfigKey.createSliderKey( new ConfigMeta( "scale", "Scale" ),
                                         1, 1e-7, 1e7, true );

    /** Config key for sized marker autoscale flag. */
    public static final ConfigKey<Boolean> AUTOSCALE =
        new BooleanConfigKey( new ConfigMeta( "autoscale", "Auto Scale" ),
                              Boolean.TRUE );

    /** Config key for a label string. */
    public static final ConfigKey<String> LABEL =
        new StringConfigKey( new ConfigMeta( "label", "Label" ), null );

    /** Config key for legend inclusion flag. */
    public static final ConfigKey<Boolean> SHOW_LABEL =
        new BooleanConfigKey( new ConfigMeta( "inlegend", "In Legend" ),
                              true );

    /** Config key for minor tick drawing key. */
    public static final ConfigKey<Boolean> MINOR_TICKS =
        new BooleanConfigKey( new ConfigMeta( "minor", "Minor Ticks" ), true );

    /** Config key for zoom factor. */
    public static final ConfigKey<Double> ZOOM_FACTOR =
        DoubleConfigKey
       .createSliderKey( new ConfigMeta( "zoomfactor", "Zoom Factor" ),
                         1.2, 1, 2, true );

    /** Config key set for axis and general captioner. */
    public static final CaptionerKeySet CAPTIONER = new CaptionerKeySet();

    /**
     * Private constructor prevents instantiation.
     */
    private StyleKeys() {
    }

    /**
     * Returns a list of config keys for configuring a line-drawing stroke.
     * Pass a map with values for these to the <code>createStroke</code>
     * method.
     *
     * @return  stroke key list
     * @see  #createStroke
     */
    public static ConfigKey[] getStrokeKeys() {
        return new ConfigKey[] {
            THICKNESS,
            DASH,
        };
    }

    /**
     * Obtains a line drawing stroke based on a config map.
     * The keys used are those returned by <code>getStrokeKeys</code>.
     * The line join and cap policy must be provided.
     *
     * @param  config  config map
     * @param  cap     one of {@link java.awt.BasicStroke}'s CAP_* constants
     * @param  join    one of {@link java.awt.BasicStroke}'s JOIN_* constants
     * @return  stroke
     */
    public static Stroke createStroke( ConfigMap config, int cap, int join ) {
        int thick = config.get( THICKNESS );
        float[] dash = config.get( DASH );
        if ( dash != null && thick != 1 ) {
            dash = (float[]) dash.clone();
            for ( int i = 0; i < dash.length; i++ ) {
                dash[ i ] *= thick;
            }
        }
        return new BasicStroke( thick, cap, join, 10f, dash, 0f );
    }

    /**
     * Returns an axis tick mark crowding config key.
     *
     * @param  meta  metadata
     * @return   new key
     */
    public static ConfigKey<Double> createCrowdKey( ConfigMeta meta ) {
        return DoubleConfigKey.createSliderKey( meta, 1, 0.125, 10, true );
    }

    /**
     * Returns an axis labelling config key.
     *
     * @param  axName  axis name
     * @return  new key
     */
    public static ConfigKey<String> createAxisLabelKey( String axName ) {
        return new StringConfigKey( new ConfigMeta( axName.toLowerCase()
                                                    + "label",
                                                    axName + " Label" ),
                                                    axName );
    }

    /**
     * Returns a config key for line thickness with a given default value.
     *
     * @param  dfltThick  default value for line width in pixels
     * @return   new config key
     */
    public static ConfigKey<Integer> createThicknessKey( int dfltThick ) {
        return new IntegerConfigKey( new ConfigMeta( "thick", "Thickness" ),
                                     dfltThick ) {
            public Specifier<Integer> createSpecifier() {
                return new ComboBoxSpecifier<Integer>(
                               new ThicknessComboBox( 5 ) );
            }
        };
    }

    /**
     * Obtains a shader from a config map given appropriate keys.
     *
     * @param  config  config map
     * @param  baseShaderKey   key for extracting a shader
     * @param  clipKey   key for extracting a clip range of a shader
     * @return  shader with clip applied if appropriate
     */
    public static Shader createShader( ConfigMap config,
                                       ConfigKey<Shader> baseShaderKey,
                                       ConfigKey<Subrange> clipKey ) {
        Shader shader = config.get( baseShaderKey );
        if ( shader == null ) {
            return null;
        }
        Subrange clip = config.get( clipKey );
        return Subrange.isIdentity( clip )
             ? shader
             : Shaders.stretch( shader,
                                (float) clip.getLow(), (float) clip.getHigh() );
    }

    /**
     * Returns an array of marker shapes.
     *
     * @return  marker shapes
     */
    private static MarkShape[] createShapes() {
        return new MarkShape[] {
            MarkShape.FILLED_CIRCLE,
            MarkShape.OPEN_CIRCLE,
            MarkShape.CROSS,
            MarkShape.CROXX,
            MarkShape.OPEN_SQUARE,
            MarkShape.OPEN_DIAMOND,
            MarkShape.OPEN_TRIANGLE_UP,
            MarkShape.OPEN_TRIANGLE_DOWN,
            MarkShape.FILLED_SQUARE,
            MarkShape.FILLED_DIAMOND,
            MarkShape.FILLED_TRIANGLE_UP,
            MarkShape.FILLED_TRIANGLE_DOWN,
        };
    }

    /**
     * Returns a list of shaders suitable for aux axis shading.
     *
     * @return  shaders
     */
    private static Shader[] createAuxShaders() {
        List<Shader> shaderList = new ArrayList<Shader>();
        shaderList.addAll( Arrays.asList( new Shader[] {
            Shaders.LUT_RAINBOW,
            Shaders.LUT_GLNEMO2,
            Shaders.LUT_PASTEL,
            Shaders.LUT_ACCENT,
            Shaders.LUT_GNUPLOT,
            Shaders.LUT_GNUPLOT2,
            Shaders.LUT_CUBEHELIX,
            Shaders.LUT_SPECXB2Y,
            Shaders.CYAN_MAGENTA,
            Shaders.RED_BLUE,
            Shaders.LUT_HEAT,
            Shaders.LUT_COLD,
            Shaders.LUT_LIGHT,
            Shaders.LUT_COLOR,
            Shaders.WHITE_BLACK,
            Shaders.LUT_STANDARD,
            Shaders.createMaskShader( "Mask", 0f, 1f, true ),
            Shaders.FIX_HUE,
            Shaders.TRANSPARENCY,
            Shaders.FIX_INTENSITY,
            Shaders.FIX_RED,
            Shaders.FIX_GREEN,
            Shaders.FIX_BLUE,
            Shaders.HSV_H,
            Shaders.HSV_S,
            Shaders.HSV_V,
            Shaders.FIX_Y,
            Shaders.FIX_U,
            Shaders.FIX_V,
            Shaders.BREWER_BUGN,
            Shaders.BREWER_BUPU,
            Shaders.BREWER_ORRD,
            Shaders.BREWER_PUBU,
            Shaders.BREWER_PURD,
        } ) );
        shaderList.addAll( Arrays.asList( Shaders.getCustomShaders() ) );
        return shaderList.toArray( new Shader[ 0 ] );
    }

    /**
     * Returns a list of shaders suitable for density shading.
     *
     * @return  shaders
     */
    private static Shader[] createDensityShaders() {
        return new Shader[] {
            Shaders.invert( Shaders.SCALE_V ),
            Shaders.invert( Shaders.LUT_PASTEL ),
            Shaders.invert( Shaders.LUT_RAINBOW ),
            Shaders.invert( Shaders.LUT_GLNEMO2 ),
            Shaders.invert( Shaders.LUT_ACCENT ),
            Shaders.invert( Shaders.LUT_GNUPLOT ),
            Shaders.invert( Shaders.LUT_GNUPLOT2 ),
            Shaders.invert( Shaders.LUT_CUBEHELIX ),
            Shaders.invert( Shaders.LUT_SPECXB2Y ),
            Shaders.CYAN_MAGENTA,
            Shaders.RED_BLUE,
            Shaders.invert( Shaders.LUT_HEAT ),
            Shaders.invert( Shaders.LUT_COLD ),
            Shaders.invert( Shaders.LUT_LIGHT ),
            Shaders.WHITE_BLACK,
            Shaders.SCALE_V,
            Shaders.SCALE_S,
            Shaders.LUT_COLOR,
            Shaders.LUT_STANDARD,
            Shaders.BREWER_BUGN,
            Shaders.BREWER_BUPU,
            Shaders.BREWER_ORRD,
            Shaders.BREWER_PUBU,
            Shaders.BREWER_PURD,
        };
    }

    /**
     * Config key implementation based on a NamedObject.
     * This gives you a list of options but also lets you specify
     * values by string using some potentially constructive syntax.
     */
    private static abstract class NamedObjectKey<T> extends ConfigKey<T> {
        final NamedObjectParameter param_;
        final String[] names_;
        final Object[] options_;

        /**
         * Constructor.
         *
         * @param   meta  metadata
         * @param   clazz  value class
         * @param   dflt  default value
         * @param   param  parameter object contaning encode/decode logic
         */
        NamedObjectKey( ConfigMeta meta, Class<T> clazz, T dflt,
                        NamedObjectParameter param ) {
            super( meta, clazz, dflt );
            param_ = param;
            names_ = param.getNames();
            options_ = param.getOptions();
        }
        public T stringToValue( String txt ) {
            if ( txt == null || txt.length() == 0 ) {
                return null;
            }
            for ( int i = 0; i < names_.length; i++ ) {
                if ( txt.equals( names_[ i ] ) ) {
                    return cast( options_[ i ] );
                }
            }
            try {
                return cast( param_.fromString( txt ) );
            }
            catch ( RuntimeException e ) {
                throw new ConfigException( this, e.getMessage(), e );
            }
        }
        public String valueToString( T value ) {
            if ( value == null ) {
                return null;
            }
            for ( int i = 0; i < options_.length; i++ ) {
                if ( value.equals( options_[ i ] ) ) {
                    return names_[ i ];
                }
            }
            return param_.toString( value );
        }
    }

    /**
     * Config key implementation for selecting colours.
     * A null colour is optionally available, controlled by a toggle switch.
     */
    private static class ColorConfigKey extends NamedObjectKey<Color> {
        private final boolean nullPermitted_;
        private final Color[] colors_;

        /**
         * Constructor.
         *
         * @param  meta  metadata
         * @param  dflt  default value
         * @param  nullPermitted  true if null is a legal option
         */
        ColorConfigKey( ConfigMeta meta, Color dflt, boolean nullPermitted ) {
            super( meta, Color.class, dflt,
                   new ColorParameter( meta.getShortName() ) );
            nullPermitted_ = nullPermitted;
            param_.setNullPermitted( nullPermitted );
            List<Color> colorList =
                new ArrayList<Color>( Arrays.asList( Styles.COLORS ) );
            if ( ! colorList.contains( dflt ) ) {
                colorList.add( 0, dflt );
            }
            colors_ = colorList.toArray( new Color[ 0 ] );
        }
        public Specifier<Color> createSpecifier() {
            Specifier<Color> basic =
                new ComboBoxSpecifier<Color>( new ColorComboBox( colors_ ) );
            return nullPermitted_
                 ? new ToggleSpecifier<Color>( basic, null, "Hide" )
                 : basic;
        }
    };

    /**
     * Config key implementation for selecting shader objects.
     */
    private static class ShaderConfigKey extends OptionConfigKey<Shader> {

        /**
         * Constructor.
         *
         * @param  meta  metadata
         * @param  shaders  list of options
         * @param  dflt  default value
         */
        ShaderConfigKey( ConfigMeta meta, Shader[] shaders, Shader dflt ) {
            super( meta, Shader.class, shaders, dflt );
        }
        public String valueToString( Shader shader ) {
            return shader.getName();
        }
        public Specifier<Shader> createSpecifier() {
            JComboBox comboBox = new JComboBox( getOptions() );
            comboBox.setSelectedItem( getDefaultValue() );
            comboBox.setRenderer( new ShaderListCellRenderer( comboBox ) );
            return new ComboBoxSpecifier<Shader>( comboBox );
        }
    };
}
