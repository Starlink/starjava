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
import uk.ac.starlink.ttools.gui.MarkStyleSelectors;
import uk.ac.starlink.ttools.gui.ThicknessComboBox;
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
import uk.ac.starlink.ttools.plot2.Scaling;
import uk.ac.starlink.ttools.plot2.Subrange;
import uk.ac.starlink.ttools.plot2.geom.PlaneSurfaceFactory;
import uk.ac.starlink.ttools.plot2.layer.LevelMode;
import uk.ac.starlink.ttools.plot2.layer.XYShape;
import uk.ac.starlink.ttools.plot2.layer.XYShapes;
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
        new OptionConfigKey<MarkShape>(
            new ConfigMeta( "shape", "Shape" )
           .setShortDescription( "Marker shape" )
           .setXmlDescription( new String[] {
                "<p>Sets the shape of markers that are plotted at each",
                "position of the scatter plot.",
                "</p>",
            } )
        , MarkShape.class, SHAPES, MarkShape.FILLED_CIRCLE ) {
        public Specifier<MarkShape> createSpecifier() {
            return new ComboBoxSpecifier<MarkShape>( MarkStyleSelectors
                                                    .createShapeSelector() );
        }
    }.setOptionUsage()
     .addOptionsXml();

    /** Config key for marker size. */
    public static final ConfigKey<Integer> SIZE =
        new IntegerConfigKey(
            new ConfigMeta( "size", "Size" )
           .setStringUsage( "<pixels>" )
           .setShortDescription( "Marker size in pixels" )
           .setXmlDescription( new String[] {
                "<p>Size of the scatter plot markers.",
                "The unit is pixels, in most cases the marker is approximately",
                "twice the size of the supplied value.",
                "</p>",
            } )
        , 1 ) {
        public Specifier<Integer> createSpecifier() {
            return new ComboBoxSpecifier<Integer>( MarkStyleSelectors
                                                  .createSizeSelector() );
        }
    };

    private static final XYShape[] XYSHAPES = XYShapes.getXYShapes();

    /** Config key for XY shape. */
    public static final ConfigKey<XYShape> XYSHAPE =
        new OptionConfigKey<XYShape>(
            new ConfigMeta( "shape", "Shape" )
           .setShortDescription( "Marker shape" )
           .setXmlDescription( new String[] {
            } )
        , XYShape.class, XYSHAPES ) {
        public Specifier<XYShape> createSpecifier() {
            JComboBox shapeSelector = new RenderingComboBox( XYSHAPES ) {
                @Override
                protected Icon getRendererIcon( Object shape ) {
                    return XYShape.createIcon( (XYShape) shape, 20, 12, true );
                }
                protected String getRendererText( Object shape ) {
                    return null;
                }
            };
            return new ComboBoxSpecifier<XYShape>( shapeSelector );
        }
    }.setOptionUsage()
     .addOptionsXml();

    /** Config key for style colour. */
    public static final ConfigKey<Color> COLOR =
        new ColorConfigKey( ColorConfigKey
                           .createColorMeta( "color", "Color", "plotted data" ),
                            Color.RED, false );

    /** Config key for the opacity limit of transparent plots.
     *  This is the number of times a point has to be hit to result in
     *  a saturated (opaque) pixel. */
    public static final ConfigKey<Double> OPAQUE =
        DoubleConfigKey.createSliderKey(
            new ConfigMeta( "opaque", "Opaque limit" )
           .setShortDescription( "Fraction of fully opaque" )
           .setXmlDescription( new String[] {
                "<p>The opacity of plotted points.",
                "The value is the number of points which have to be",
                "overplotted before the background is fully obscured.",
                "</p>",
            } )
        , 4, 1, 10000, true );

    /** Config key for the opacity limit of auxiliary shaded plots. */
    public static final ConfigKey<Double> AUX_OPAQUE =
        DoubleConfigKey.createSliderKey(
            new ConfigMeta( "opaque", "Opaque limit" )
           .setShortDescription( "Aux fraction of fully opaque" )
           .setXmlDescription( new String[] {
                "<p>The opacity of points plotted in the Aux colour.",
                "The value is the number of points which have to be",
                "overplotted before the background is fully obscured.",
                "</p>",
            } )
        , 1, 1, 1000, true );

    /** Config key for transparency level of adaptive transparent plots. */
    public static final ConfigKey<Double> TRANSPARENT_LEVEL =
        DoubleConfigKey.createSliderKey(
            new ConfigMeta( "translevel", "Transparency Level" )
           .setShortDescription( "Transparency" )
           .setXmlDescription( new String[] {
                "<p>Sets the level of automatically controlled transparency. ",
                "The higher this value the more transparent points are.",
                "Exactly how transparent points are depends on how many",
                "are currently being plotted on top of each other and",
                "the value of this parameter.",
                "The idea is that you can set it to some fixed value,",
                "and then get something which looks similarly",
                "transparent while you zoom in and out.",
                "</p>",
            } )
        , 0.1, 0.001, 2, true );

    /** Config key for "normal" transparency - it's just 1-alpha. */
    public static final ConfigKey<Double> TRANSPARENCY =
        DoubleConfigKey.createSliderKey(
            new ConfigMeta( "transparency", "Transparency" )
           .setShortDescription( "Fractional transparency" )
           .setXmlDescription( new String[] {
                "<p>Transparency with which compoents are plotted,",
                "in the range 0 (opaque) to 1 (invisible).",
                "The value is 1-alpha.",
                "</p>",
            } )
           .setStringUsage( "0..1" )
        , 0, 0, 1, false );

    /** Config key for line thickness. */
    private static final ConfigKey<Integer> THICKNESS = createThicknessKey( 1 );

    /** Config key for line dash style. */
    public static final ConfigKey<float[]> DASH =
        new DashConfigKey( DashConfigKey.createDashMeta( "dash", "Dash" ) );

    /** Config key for axis grid colour. */
    public static final ConfigKey<Color> GRID_COLOR =
        new ColorConfigKey( ColorConfigKey
                           .createColorMeta( "gridcolor", "Grid Color",
                                             "the plot grid" ),
                            Color.LIGHT_GRAY, false );

    /** Config key for axis label colour. */
    public static final ConfigKey<Color> AXLABEL_COLOR =
        new ColorConfigKey(
            ColorConfigKey 
           .createColorMeta( "labelcolor", "Label Color",
                             "axis labels and other plot annotations" )
            , Color.BLACK, false );

    private static final BarStyle.Form[] BARFORMS = new BarStyle.Form[] {
        BarStyle.FORM_FILLED,
        BarStyle.FORM_OPEN,
        BarStyle.FORM_TOP,
        BarStyle.FORM_SPIKE,
    };

    /** Config key for histogram bar style. */
    public static final ConfigKey<BarStyle.Form> BAR_FORM =
        new OptionConfigKey<BarStyle.Form>(
            new ConfigMeta( "barform", "Bar Form" )
           .setShortDescription( "Histogram bar shape" )
           .setXmlDescription( new String[] {
                "<p>How histogram bars are represented.",
                "</p>",
            } )
        , BarStyle.Form.class, BARFORMS ) {
            public Specifier<BarStyle.Form> createSpecifier() {
                JComboBox formSelector = new RenderingComboBox( BARFORMS ) {
                    protected Icon getRendererIcon( Object form ) {
                        return BarStyles.getIcon( (BarStyle.Form) form );
                    }
                };
                return new ComboBoxSpecifier<BarStyle.Form>( formSelector );
            }
        }.setOptionUsage()
         .addOptionsXml();

    /** Config key for line antialiasing. */
    public static final ConfigKey<Boolean> ANTIALIAS =
        new BooleanConfigKey(
            new ConfigMeta( "antialias", "Antialiasing" )
           .setShortDescription( "Antialias lines?" )
           .setXmlDescription( new String[] {
                "<p>If true, plotted lines are drawn with antialising.",
                "Antialised lines look smoother, but may take",
                "perceptibly longer to draw.",
                "Only has any effect for bitmapped output formats.",
                "</p>",
            } )
        , false );

    /** Config key for axis grid antialiasing. */
    public static final ConfigKey<Boolean> GRID_ANTIALIAS =
        new BooleanConfigKey(
            new ConfigMeta( "gridaa", "Antialiasing" )
           .setShortDescription( "Use antialiasing for grid lines?" )
           .setXmlDescription( new String[] {
                "<p>If true, grid lines are drawn with antialiasing.",
                "Antialiased lines look smoother, but may take",
                "perceptibly longer to draw.",
                "Only has any effect for bitmapped output formats.",
                "</p>",
            } )
        , false );

    /** Config key for text anchor positioning. */
    public static final ConfigKey<Anchor> ANCHOR =
        new OptionConfigKey<Anchor>(
            new ConfigMeta( "anchor", "Anchor" )
           .setShortDescription( "Text label anchor position" )
           .setXmlDescription( new String[] {
                "<p>Determines where the text appears",
                "in relation to the plotted points.",
                "Values are points of the compass.",
                "</p>",
            } )
        , Anchor.class, new Anchor[] { Anchor.W, Anchor.E, Anchor.N, Anchor.S, }
        ).setOptionUsage()
         .addOptionsXml();

    /** Config key for scaling level mode. */ 
    public static final ConfigKey<LevelMode> LEVEL_MODE =
        new OptionConfigKey<LevelMode>(
            new ConfigMeta( "scaling", "Scaling" )
           .setShortDescription( "Level scaling" )
           .setXmlDescription( new String[] {
                "<p>How the smoothed density is treated before",
                "contour levels are determined.",
                "</p>",
            } )
            , LevelMode.class, LevelMode.MODES, LevelMode.LINEAR
        ).setOptionUsage()
         .addOptionsXml();

    /** Config key for vector marker style. */
    public static final MultiPointConfigKey VECTOR_SHAPE =
        createMultiPointKey( "arrow", "Arrow", ErrorRenderer.getOptionsVector(),
                             new ErrorMode[] { ErrorMode.UPPER } );

    /** Config key for ellipse marker style. */
    public static final MultiPointConfigKey ELLIPSE_SHAPE =
        createMultiPointKey( "ellipse", "Ellipse",
                             ErrorRenderer.getOptionsEllipse(),
                             new ErrorMode[] { ErrorMode.SYMMETRIC,
                                               ErrorMode.SYMMETRIC } );

    /** Config key for 1d (vertical) error marker style. */
    public static final MultiPointConfigKey ERROR_SHAPE_1D =
        createMultiPointKey( "errorbar", "Error Bar",
                             ErrorRenderer.getOptions1d(),
                             new ErrorMode[] { ErrorMode.SYMMETRIC } );

    /** Config key for 2d error marker style. */
    public static final MultiPointConfigKey ERROR_SHAPE_2D =
        createMultiPointKey( "errorbar", "Error Bar",
                             ErrorRenderer.getOptions2d(),
                             new ErrorMode[] { ErrorMode.SYMMETRIC,
                                               ErrorMode.SYMMETRIC } );

    /** Config key for 3d error marker style. */
    public static final MultiPointConfigKey ERROR_SHAPE_3D =
        createMultiPointKey( "errorbar", "Error Bar",
                             ErrorRenderer.getOptions3d(),
                             new ErrorMode[] { ErrorMode.SYMMETRIC,
                                               ErrorMode.SYMMETRIC,
                                               ErrorMode.SYMMETRIC } );

    /** Config key for aux axis tick crowding. */
    public static final ConfigKey<Double> AUX_CROWD =
        PlaneSurfaceFactory.createAxisCrowdKey( "Aux" );

    /** Config key for aux shader lower limit. */
    public static final ConfigKey<Double> SHADE_LOW =
        PlaneSurfaceFactory.createAxisLimitKey( "Aux", false );

    /** Config key for aux shader upper limit. */
    public static final ConfigKey<Double> SHADE_HIGH =
        PlaneSurfaceFactory.createAxisLimitKey( "Aux", true );

    /** Config key for aux shader subrange. */
    public static final ConfigKey<Subrange> SHADE_SUBRANGE =
        new SubrangeConfigKey( SubrangeConfigKey
                              .createAxisSubMeta( "aux", "Aux" ) );

    /** Config key for aux null colour. */
    public static final ConfigKey<Color> AUX_NULLCOLOR =
        createNullColorKey( "aux", "Aux" );

    private static final String SCALE_NAME = "scale";
    private static final String AUTOSCALE_NAME = "autoscale";

    /** Config key for scaling of markers in data space. */
    public static final ConfigKey<Double> SCALE =
        DoubleConfigKey
       .createSliderKey(
            new ConfigMeta( SCALE_NAME, "Scale" )
           .setStringUsage( "<factor>" )
           .setShortDescription( "Marker size multiplier" )
           .setXmlDescription( new String[] {
                "<p>Affects the size of variable-sized markers",
                "like vectors and ellipses.",
                "The default value is 1, smaller or larger values",
                "multiply the visible sizes accordingly.",
                "</p>",
            } )
        , 1, 1e-6, 1e6, true );

    /** Config key for scaling of markers in pixel space. */
    public static final ConfigKey<Double> SCALE_PIX =
        DoubleConfigKey
       .createSliderKey(
            new ConfigMeta( SCALE_NAME, "Scale" )
           .setStringUsage( "<factor>" )
           .setShortDescription( "Marker size multiplier" )
           .setXmlDescription( new String[] {
                "<p>Scales the size of variable-sized markers.",
                "The default is 1, smaller or larger values",
                "multiply the visible sizes accordingly.",
                "</p>",
            } )
        , 1, 1e-2, 1e2, true );

    /** Config key for autoscale flag for markers in data space. */
    public static final ConfigKey<Boolean> AUTOSCALE =
        new BooleanConfigKey(
            new ConfigMeta( AUTOSCALE_NAME, "Auto Scale" )
           .setShortDescription( "Scale marker sizes automatically?" )
           .setXmlDescription( new String[] {
                "<p>Determines whether the default size of variable-sized",
                "markers like vectors and ellipses are automatically",
                "scaled to have a sensible size.",
                "If true, then the sizes of all the plotted markers",
                "are examined, and some dynamically calculated factor is",
                "applied to them all to make them a sensible size",
                "(by default, the largest ones will be a few tens of pixels).",
                "If false, the sizes will be the actual input values",
                "interpreted in data coordinates.",
                "</p>",
                "<p>If auto-scaling is on, then markers will keep",
                "approximately the same screen size during zoom operations;",
                "if it's off, they will keep the same size",
                "in data coordinates.",
                "</p>",
                "<p>Marker size is also affected by the",
                "<code>" + SCALE_NAME + "</code> parameter.",
                "</p>",
            } )
        , Boolean.TRUE );

    /** Config key for autoscale flag for markers in pixel space. */
    public static final ConfigKey<Boolean> AUTOSCALE_PIX =
        new BooleanConfigKey(
            new ConfigMeta( AUTOSCALE_NAME, "Auto Scale" )
           .setShortDescription( "Scale marker sizes automatically?" )
           .setXmlDescription( new String[] {
                "<p>Determines whether the basic size",
                "of variable sized markers is automatically",
                "scaled to have a sensible size.",
                "If true, then the sizes of all the plotted markers",
                "are examined, and some dynamically calculated factor is",
                "applied to them all to make them a sensible size",
                "(by default, the largest ones will be a few tens of pixels).",
                "If false, the sizes will be the actual input values",
                "in units of pixels.",
                "</p>",
                "<p>If auto-scaling is off, then markers will keep",
                "exactly the same screen size during pan and zoom operations;",
                "if it's on, then the visible sizes will change according",
                "to what other points are currently plotted.",
                "</p>",
                "<p>Marker size is also affected by the",
                "<code>" + SCALE_NAME + "</code> parameter.",
                "</p>",
            } )
        , Boolean.TRUE );

    /** Config key for a layer label string. */
    public static final ConfigKey<String> LABEL =
        new StringConfigKey(
            new ConfigMeta( "label", "Label" )
           .setStringUsage( "<txt>" )
           .setShortDescription( "Plot layer label" )
           .setXmlDescription( new String[] {
                "<p>Supplies a text label for a plot layer.",
                "This may be used for identifying it in the legend.",
                "If not supplied, a label will be generated automatically",
                "where required.",
                "</p>",
            } )
        , null );

    /** Config key for legend inclusion flag. */
    public static final ConfigKey<Boolean> SHOW_LABEL =
        new BooleanConfigKey(
            new ConfigMeta( "inlegend", "In Legend" )
           .setShortDescription( "Show layer in legend?" )
           .setXmlDescription( new String[] {
                "<p>Determines whether the layer has an entry in the",
                "plot legend, if shown.",
                "</p>",
            } )
        , true );

    /** Config key for minor tick drawing key. */
    public static final ConfigKey<Boolean> MINOR_TICKS =
        new BooleanConfigKey(
            new ConfigMeta( "minor", "Minor Ticks" )
           .setShortDescription( "Display minor tick marks?" )
           .setXmlDescription( new String[] {
                "<p>If true, minor tick marks are painted along the axes",
                "as well as the major tick marks.",
                "Minor tick marks do not have associated grid lines.",
                "</p>",
            } )
        , true );

    /** Config key for zoom factor. */
    public static final ConfigKey<Double> ZOOM_FACTOR =
        DoubleConfigKey.createSliderKey(
            new ConfigMeta( "zoomfactor", "Zoom Factor" )
           .setShortDescription( "Amount of zoom per mouse wheel unit" )
           .setXmlDescription( new String[] {
                "<p>Sets the amount by which the plot view zooms in or out",
                "for each unit of mouse wheel movement.",
                "A value of 1 means that mouse wheel zooming has no effect.",
                "A higher value means that the mouse wheel zooms faster",
                "and a value nearer 1 means it zooms slower.",
                "Values below 1 are not permitted.",
                "</p>",
            } )
        , 1.2, 1, 2, true );

    /** Config key set for axis and general captioner. */
    public static final CaptionerKeySet CAPTIONER = new CaptionerKeySet();

    /** Config key set for global Aux axis colour ramp. */
    public static final RampKeySet AUX_RAMP =
        new RampKeySet( "aux", "Aux",
                        createAuxShaders(), Scaling.LINEAR, false );

    /** Config key set for density shading. */
    public static final RampKeySet DENSITY_RAMP =
        new RampKeySet( "dense", "Density",
                        createDensityShaders(), Scaling.LOG, true );

    /** Config key set for spectrogram shading. */
    public static final RampKeySet SPECTRO_RAMP =
        new RampKeySet( "spectro", "Spectral",
                        createAuxShaders(), Scaling.LINEAR, true );

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
        ConfigMeta meta = new ConfigMeta( axName.toLowerCase() + "label",
                                          axName + " Label" );
        meta.setStringUsage( "<text>" );
        meta.setShortDescription( "Label for axis " + axName );
        meta.setXmlDescription( new String[] {
            "<p>Gives a label to be used for annotating axis " + axName,
            "A default value based on the plotted data will be used",
            "if no value is supplied.",
            "</p>",
        } );
        return new StringConfigKey( meta, axName );
    }

    /**
     * Returns a key for acquiring a colour used in place of a shading ramp
     * colour in case that the input data is null.
     *
     * @param  axname  short form of axis name, used in text parameter names
     * @param  axName  long form of axis name, used in descriptions
     * @return  new key
     */
    public static ConfigKey<Color> createNullColorKey( String axname,
                                                       String axName ) {
        return new ColorConfigKey(
            ColorConfigKey.createColorMeta( axname.toLowerCase() + "nullcolor",
                                            "Null Color",
                                            "points with a null value of the "
                                          + axName + " coordinate" )
           .appendXmlDescription( new String[] {
                "<p>If the value is null, then points with a null",
                axName,
                "value will not be plotted at all.",
                "</p>",
            } )
            , Color.GRAY, true );
    }

    /**
     * Returns a config key for line thickness with a given default value.
     *
     * @param  dfltThick  default value for line width in pixels
     * @return   new config key
     */
    public static ConfigKey<Integer> createThicknessKey( int dfltThick ) {
        ConfigMeta meta = new ConfigMeta( "thick", "Thickness" );
        meta.setStringUsage( "<pixels>" );
        meta.setShortDescription( "Line thickness in pixels" );
        meta.setXmlDescription( new String[] {
            "<p>Thickness of plotted line in pixels.",
            "</p>",
        } );
        return new IntegerConfigKey( meta, dfltThick ) {
            public Specifier<Integer> createSpecifier() {
                return new ComboBoxSpecifier<Integer>(
                               new ThicknessComboBox( 5 ) );
            }
        };
    }

    /**
     * Creates a config key for a multipoint shape.
     *
     * @param   shortName   one-word name
     * @param   longName   GUI name
     * @param   renderers   renderer options
     * @param   modes   error mode objects, used with renderers to draw icon
     * @return  new key
     */
    private static MultiPointConfigKey
            createMultiPointKey( String shortName, String longName,
                                 ErrorRenderer[] renderers,
                                 ErrorMode[] modes ) {
        ConfigMeta meta = new ConfigMeta( shortName, longName );
        meta.setShortDescription( longName + " shape" );
        meta.setXmlDescription( new String[] {
            "<p>How " + shortName + "s are represented.",
            "</p>",
        } );
        MultiPointConfigKey key =
            new MultiPointConfigKey( meta, renderers, modes );
        key.setOptionUsage();
        key.addOptionsXml();
        return key;
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
            Shaders.invert( Shaders.LUT_RAINBOW3 ),
            Shaders.invert( Shaders.LUT_ACCENT ),
            Shaders.invert( Shaders.LUT_GNUPLOT ),
            Shaders.invert( Shaders.LUT_GNUPLOT2 ),
            Shaders.invert( Shaders.LUT_CUBEHELIX ),
            Shaders.invert( Shaders.LUT_SPECXB2Y ),
            Shaders.LUT_SET1,
            Shaders.LUT_PAIRED,
            Shaders.CYAN_MAGENTA,
            Shaders.RED_BLUE,
            Shaders.LUT_BRG,
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
     * Returns a list of shaders suitable for aux axis shading.
     *
     * @return  shaders
     */
    private static Shader[] createAuxShaders() {
        return new Shader[] {
            Shaders.LUT_RAINBOW,
            Shaders.LUT_GLNEMO2,
            Shaders.LUT_PASTEL,
            Shaders.LUT_ACCENT,
            Shaders.LUT_GNUPLOT,
            Shaders.LUT_GNUPLOT2,
            Shaders.LUT_CUBEHELIX,
            Shaders.LUT_SPECXB2Y,
            Shaders.LUT_SET1,
            Shaders.LUT_PAIRED,
            Shaders.CYAN_MAGENTA,
            Shaders.RED_BLUE,
            Shaders.LUT_BRG,
            Shaders.LUT_HEAT,
            Shaders.LUT_COLD,
            Shaders.LUT_LIGHT,
            Shaders.LUT_COLOR,
            Shaders.WHITE_BLACK,
            Shaders.LUT_STANDARD,
            Shaders.LUT_RAINBOW3,
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
        };
    }
}
