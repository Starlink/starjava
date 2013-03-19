package uk.ac.starlink.ttools.plot2.config;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.ListModel;
import org.scilab.forge.jlatexmath.TeXFormula;
import uk.ac.starlink.ttools.gui.ColorComboBox;
import uk.ac.starlink.ttools.gui.DashComboBox;
import uk.ac.starlink.ttools.gui.MarkStyleSelectors;
import uk.ac.starlink.ttools.gui.ThicknessComboBox;
import uk.ac.starlink.ttools.gui.ShaderListCellRenderer;
import uk.ac.starlink.ttools.plot.ErrorMode;
import uk.ac.starlink.ttools.plot.ErrorRenderer;
import uk.ac.starlink.ttools.plot.MarkShape;
import uk.ac.starlink.ttools.plot.MarkStyle;
import uk.ac.starlink.ttools.plot.Shader;
import uk.ac.starlink.ttools.plot.Shaders;
import uk.ac.starlink.ttools.plot2.Anchor;
import uk.ac.starlink.ttools.plot2.Captioner;
import uk.ac.starlink.ttools.plot2.BasicCaptioner;
import uk.ac.starlink.ttools.plot2.LatexCaptioner;
import uk.ac.starlink.ttools.plot2.Subrange;
import uk.ac.starlink.ttools.plot2.layer.LevelMode;
import uk.ac.starlink.ttools.plottask.ColorParameter;
import uk.ac.starlink.ttools.plottask.DashParameter;
import uk.ac.starlink.ttools.plottask.NamedObjectParameter;

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
            new IntegerConfigKey( new ConfigMeta( "size", "Size" ), 0, 5, 1 ) {
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

    /** Config key for line thickness. */
    public static final ConfigKey<Integer> THICKNESS =
            new IntegerConfigKey( new ConfigMeta( "thick", "Thickness" ),
                                  1, 5, 1 ) {
        public Specifier<Integer> createSpecifier() {
            return new ComboBoxSpecifier<Integer>( new ThicknessComboBox( 5 ) );
        }
    };

    /** Config key for line dash style. */
    public static final ConfigKey<float[]> DASH =
            new NamedObjectKey<float[]>( new ConfigMeta( "dash", "Dash" ),
                                         float[].class, null,
                                         new DashParameter( "dash" ) ) {
        public Specifier<float[]> createSpecifier() {
            return new ComboBoxSpecifier<float[]>( new DashComboBox() );
        }
    };

    /** Config key for text anchor positioning. */
    public static final ConfigKey<Anchor> ANCHOR =
        new OptionConfigKey<Anchor>( new ConfigMeta( "anchor", "Anchor" ),
                                     Anchor.class,
                                     new Anchor[] {
                                         Anchor.W, Anchor.E, Anchor.N, Anchor.S,
                                     } );

    /** Config key for defining how label text is to be understood.
     *  @see  #createCaptioner */
    private static final ConfigKey<TextSyntax> TEXT_SYNTAX =
        new OptionConfigKey<TextSyntax>( new ConfigMeta( "syntax",
                                                         "Text Syntax" ),
                                         TextSyntax.class,
                                         TextSyntax.values(),
                                         TextSyntax.values()[ 0 ], true );

    /** Config key for label font size.
     *  @see  #createCaptioner */
    private static final ConfigKey<Integer> FONT_SIZE =
        new IntegerConfigKey( new ConfigMeta( "fontsize", "Font Size" ),
                              2, 64, 12 );

    /** Config key for label font style.
     *  @see  #createCaptioner */
    private static final ConfigKey<FontType> FONT_TYPE =
        new OptionConfigKey<FontType>( new ConfigMeta( "fontstyle",
                                                       "Font Style" ),
                                       FontType.class, FontType.values() );

    /** Config key for label font weight.
     *  @see  #createCaptioner */
    private static final ConfigKey<FontWeight> FONT_WEIGHT =
        new OptionConfigKey<FontWeight>( new ConfigMeta( "fontweight",
                                                         "Font Weight" ),
                                         FontWeight.class,
                                         FontWeight.values() );

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

    /** Config key for aux shader colour ramp. */
    public static final ConfigKey<Shader> AUX_SHADER =
        new ShaderConfigKey( new ConfigMeta( "shader", "Shader" ),
                             createAuxShaders(), Shaders.LUT_RAINBOW );

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
        new ShaderConfigKey( new ConfigMeta( "densemap", "Density Map" ),
                             createDensityShaders(),
                             createDensityShaders()[ 0 ] );

    /** Config key for density shader subrange. */
    public static final ConfigKey<Subrange> DENSITY_SUBRANGE =
        new SubrangeConfigKey( new ConfigMeta( "densescale",
                                               "Density Sub-range" ) );

    /** Config key for density shader log flag. */
    public static final ConfigKey<Boolean> DENSITY_LOG =
        new BooleanConfigKey( new ConfigMeta( "denselog", "Density Log" ),
                              Boolean.TRUE );

    /** Config key for density shader flip flag. */
    public static final ConfigKey<Boolean> DENSITY_FLIP =
        new BooleanConfigKey( new ConfigMeta( "denseflip", "Density Flip" ),
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

    /**
     * Private constructor prevents instantiation.
     */
    private StyleKeys() {
    }

    /**
     * Returns a list of config keys to use for configuring a captioner.
     * Pass a map with values for these to the <code>createCaptioner</code>
     * method.
     *
     * @return   captioner key list
     * @see    #createCaptioner
     */
    public static ConfigKey[] getCaptionerKeys() {
        return new ConfigKey[] {
            TEXT_SYNTAX,
            FONT_SIZE,
            FONT_TYPE,
            FONT_WEIGHT,
        };
    }

    /**
     * Obtains a captioner object based on a config map.
     * The keys used are those returned by <code>getCaptionerKeys</code>.
     *
     * @param  config  config map
     * @return  captioner
     * @see    #getCaptionerKeys
     */
    public static Captioner createCaptioner( ConfigMap config ) {
        TextSyntax syntax = config.get( TEXT_SYNTAX );
        FontType type = config.get( FONT_TYPE );
        FontWeight weight = config.get( FONT_WEIGHT );
        int size = config.get( FONT_SIZE );
        return syntax.createCaptioner( type, weight, size );
    }

    /**
     * Returns an axis tick mark crowding config key.
     *
     * @param  meta  metadata
     * @return   new key
     */
    public static ConfigKey<Double> createCrowdKey( ConfigMeta meta ) {
        return DoubleConfigKey.createSliderKey( meta, 1, 0.0625, 8, true );
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
            Shaders.LUT_PASTEL,
            Shaders.LUT_HEAT,
            Shaders.LUT_LIGHT,
            Shaders.LUT_COLOR,
            Shaders.RED_BLUE,
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
            Shaders.RED_BLUE,
            Shaders.invert( Shaders.LUT_HEAT ),
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
     
        }
        public Specifier<Color> createSpecifier() {
            Specifier<Color> basic =
                new ComboBoxSpecifier<Color>( new ColorComboBox() );
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

    /**
     * Font type enum for use with captioner configuration.
     */
    private enum FontType {
        SANSSERIF( "Standard",  "Dialog", TeXFormula.SANSSERIF ),
        SERIF( "Serif", "Serif", TeXFormula.ROMAN ),
        MONO( "Mono", "Monospaced", TeXFormula.TYPEWRITER );

        private final String name_;
        private final String awtName_;
        private final int texType_;

        FontType( String name, String awtName, int texType ) {
            name_ = name;
            awtName_ = awtName;
            texType_ = texType;
        }

        public String toString() {
            return name_;
        }
    }

    /**
     * Font weight enum for use with captioner configuration.
     */
    private enum FontWeight {
        PLAIN( "Plain", Font.PLAIN, 0 ),
        BOLD( "Bold", Font.BOLD, TeXFormula.BOLD ),
        ITALIC( "Italic", Font.ITALIC, TeXFormula.ITALIC ),
        BOLDITALIC( "Bold Italic", Font.BOLD | Font.ITALIC,
                                   TeXFormula.BOLD | TeXFormula.ITALIC );

        private final String name_;
        private final int awtWeight_;
        private final int texWeight_;

        FontWeight( String name, int awtWeight, int texWeight ) {
            name_ = name;
            awtWeight_ = awtWeight;
            texWeight_ = texWeight;
        }

        public String toString() {
            return name_;
        }
    }

    /**
     * Text interpretation language enum for use with captioner configuration.
     */
    private enum TextSyntax {
        PLAIN( "Plain" ) {
            public Captioner createCaptioner( FontType type, FontWeight weight,
                                              int size ) {
                return new BasicCaptioner( new Font( type.awtName_,
                                                     weight.awtWeight_,
                                                     size ) );
            }
        },
        LATEX( "LaTeX" ) {
            public Captioner createCaptioner( FontType type, FontWeight weight,
                                              int size ) {
                return new LatexCaptioner( size,
                                           type.texType_ | weight.texWeight_ );
            }
        };
        private final String name_;
        TextSyntax( String name ) {
            name_ = name;
        }
        public abstract Captioner createCaptioner( FontType type,
                                                   FontWeight weight,
                                                   int size );
        public String toString() {
            return name_;
        }
    }
}
