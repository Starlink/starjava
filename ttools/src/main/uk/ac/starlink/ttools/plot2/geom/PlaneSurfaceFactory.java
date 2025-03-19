package uk.ac.starlink.ttools.plot2.geom;

import gnu.jel.CompilationException;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.DoubleUnaryOperator;
import uk.ac.starlink.ttools.jel.JELFunction;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot2.Axis;
import uk.ac.starlink.ttools.plot2.Captioner;
import uk.ac.starlink.ttools.plot2.LabelledLine;
import uk.ac.starlink.ttools.plot2.Navigator;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotMetric;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Subrange;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.SurfaceFactory;
import uk.ac.starlink.ttools.plot2.config.BooleanConfigKey;
import uk.ac.starlink.ttools.plot2.config.CombinationConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigException;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.DoubleConfigKey;
import uk.ac.starlink.ttools.plot2.config.OptionConfigKey;
import uk.ac.starlink.ttools.plot2.config.Specifier;
import uk.ac.starlink.ttools.plot2.config.StringConfigKey;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.config.SubrangeConfigKey;
import uk.ac.starlink.ttools.plot2.config.TextFieldSpecifier;
import uk.ac.starlink.ttools.plot2.data.DataStore;

/**
 * Surface factory for flat 2-d plotting.
 *
 * @author   Mark Taylor
 * @since    19 Feb 2013
 */
public class PlaneSurfaceFactory
            implements SurfaceFactory<PlaneSurfaceFactory.Profile,PlaneAspect> {

    private static final XyKeyPair<Double> MIN_XYKEY =
        new XyKeyPair<Double>( a -> createAxisLimitKey( a, false ) );
    private static final XyKeyPair<Double> MAX_XYKEY =
        new XyKeyPair<Double>( a -> createAxisLimitKey( a, true ) );
    private static final XyKeyPair<Subrange> SUBRANGE_XYKEY =
        new XyKeyPair<Subrange>( a -> createAxisSubrangeKey( a ) );
    private static final XyKeyPair<Boolean> LOG_XYKEY =
        new XyKeyPair<Boolean>( a -> createAxisLogKey( a ) );
    private static final XyKeyPair<Boolean> FLIP_XYKEY =
        new XyKeyPair<Boolean>( a -> createAxisFlipKey( a ) );
    private static final XyKeyPair<String> LABEL_XYKEY =
        new XyKeyPair<String>( a -> StyleKeys.createAxisLabelKey( a ) );
    private static final XyKeyPair<JELFunction> FUNC2_XYKEY =
        new XyKeyPair<JELFunction>( a -> createSecondaryAxisFunctionKey( a ) );
    private static final XyKeyPair<String> LABEL2_XYKEY =
        new XyKeyPair<String>( a -> createSecondaryAxisLabelKey( a ) );
    private static final XyKeyPair<Double> CROWD_XYKEY =
        new XyKeyPair<Double>( a -> createAxisCrowdKey( a ) );
    private static final XyKeyPair<Boolean> ANCHOR_XYKEY =
        new XyKeyPair<Boolean>( a -> createAxisAnchorKey( a, false ) );

    /** Config key for X axis lower bound, before subranging. */
    public static final ConfigKey<Double> XMIN_KEY = MIN_XYKEY.getKeyX();

    /** Config key for X axis upper bound, before subranging. */
    public static final ConfigKey<Double> XMAX_KEY = MAX_XYKEY.getKeyX();

    /** Config key for X axis subrange. */
    public static final ConfigKey<Subrange> XSUBRANGE_KEY =
        SUBRANGE_XYKEY.getKeyX();

    /** Config key for Y axis lower bound, before subranging. */
    public static final ConfigKey<Double> YMIN_KEY = MIN_XYKEY.getKeyY();

    /** Config key for Y axis upper bound, before subranging. */
    public static final ConfigKey<Double> YMAX_KEY = MAX_XYKEY.getKeyY();

    /** Config key for Y axis subrange. */
    public static final ConfigKey<Subrange> YSUBRANGE_KEY =
        SUBRANGE_XYKEY.getKeyY();

    /** Config key for X axis log scale flag. */
    public static final ConfigKey<Boolean> XLOG_KEY = LOG_XYKEY.getKeyX();

    /** Config key for Y axis log scale flag. */
    public static final ConfigKey<Boolean> YLOG_KEY = LOG_XYKEY.getKeyY();

    /** Config key for X axis flip flag. */
    public static final ConfigKey<Boolean> XFLIP_KEY = FLIP_XYKEY.getKeyX();

    /** Config key for Y axis flip flag. */
    public static final ConfigKey<Boolean> YFLIP_KEY = FLIP_XYKEY.getKeyY();

    /** Config key for X axis text label. */
    public static final ConfigKey<String> XLABEL_KEY = LABEL_XYKEY.getKeyX();

    /** Config key for Y axis text label.*/
    public static final ConfigKey<String> YLABEL_KEY = LABEL_XYKEY.getKeyY();

    /** Config key for secondary X axis function. */
    public static final ConfigKey<JELFunction> X2FUNC_KEY =
        FUNC2_XYKEY.getKeyX();

    /** Config key for secondary Y axis function. */
    public static final ConfigKey<JELFunction> Y2FUNC_KEY =
        FUNC2_XYKEY.getKeyY();

    /** Config key for secondary X axis text label. */
    public static final ConfigKey<String> X2LABEL_KEY = LABEL2_XYKEY.getKeyX();

    /** Config key for secondary Y axis text label. */
    public static final ConfigKey<String> Y2LABEL_KEY = LABEL2_XYKEY.getKeyY();

    /** Config key for axis aspect ratio fix. */
    public static final ConfigKey<Double> XYFACTOR_KEY =
        DoubleConfigKey.createToggleKey(
            new ConfigMeta( "aspect", "Aspect Lock" )
           .setShortDescription( "X/Y axis unit ratio" )
           .setXmlDescription( new String[] {
                "<p>Ratio of the unit length on the X axis to the unit",
                "length on the Y axis.",
                "If set to 1, the space will be isotropic.",
                "If not set (the default)",
                "the ratio will be determined by the given or calculated",
                "data bounds on both axes and the shape of the plotting",
                "region.",
                "</p>",
            } )
        , Double.NaN, 1.0 );

    /** Config key to determine if grid lines are drawn. */
    public static final ConfigKey<Boolean> GRID_KEY =
        new BooleanConfigKey(
            new ConfigMeta( "grid", "Draw Grid" )
           .setShortDescription( "Draw grid lines?" )
           .setXmlDescription( new String[] {
                "<p>If true, grid lines are drawn on the plot",
                "at positions determined by the major tick marks.",
                "If false, they are absent.",
                "</p>",
            } )
        , false );

    /** Config key to control tick mark crowding on X axis. */
    public static final ConfigKey<Double> XCROWD_KEY = CROWD_XYKEY.getKeyX();

    /** Config key to control tick mark crowding on Y axis. */
    public static final ConfigKey<Double> YCROWD_KEY = CROWD_XYKEY.getKeyY();

    /** Config key to select which axes navigation actions will operate on. */
    public static final ConfigKey<boolean[]> NAVAXES_KEY =
        new CombinationConfigKey(
             new ConfigMeta( "navaxes", "Pan/Zoom Axes" )
            .setStringUsage( "xy|x|y" )
            .setShortDescription( "Axes affected by pan/zoom" )
            .setXmlDescription( new String[] {
                "<p>Determines the axes which are affected by",
                "the interactive navigation actions (pan and zoom).",
                "The default is <code>xy</code>, which means that",
                "the various mouse gestures will provide panning and zooming",
                "in both X and Y directions.",
                "However, if it is set to (for instance) <code>x</code>",
                "then the mouse will only allow panning and",
                "zooming in the horizontal direction,",
                "with the vertical extent fixed.",
                "</p>",
            } )
        , new String[] { "X", "Y" } );

    /** Config key to anchor X axis during zooms. */
    public static final ConfigKey<Boolean> XANCHOR_KEY = ANCHOR_XYKEY.getKeyX();

    /** Config key to anchor Y axis during zooms. */
    public static final ConfigKey<Boolean> YANCHOR_KEY = ANCHOR_XYKEY.getKeyY();

    /** OrientationPolicy key for use with Plane plot. */
    public static final ConfigKey<OrientationPolicy> ORIENTATIONS_KEY_PLANE =
        createOrientationsKey( OrientationPolicy.HORIZONTAL );

    /** OrientationPolicy key for use with Matrix plot. */
    public static final ConfigKey<OrientationPolicy> ORIENTATIONS_KEY_MATRIX =
        createOrientationsKey( OrientationPolicy.ANGLED );

    /** Default configuration for plane surface factory. */
    public static final Config DFLT_CONFIG = new Config() {
        public boolean has2dMetric() {
            return true;
        }
        public boolean hasSecondaryAxes() {
            return true;
        }
        public boolean labelFormattedPosition() {
            return false;
        }
        public ConfigKey<OrientationPolicy> getOrientationsKey() {
            return ORIENTATIONS_KEY_PLANE;
        }
    };

    private final PlotMetric plotMetric_;
    private final boolean hasSecondaryAxes_;
    private final boolean labelFormattedPosition_;
    private final ConfigKey<OrientationPolicy> orientationsKey_;

    /**
     * Constructs a PlaneSurfaceFactory with default characteristics.
     */
    public PlaneSurfaceFactory() {
        this( DFLT_CONFIG );
    }

    /**
     * Constructs a PlaneSurfaceFactory with configurable characteristics.
     *
     * @param  config  configuration options
     */
    public PlaneSurfaceFactory( Config config ) {
        plotMetric_ = new PlanePlotMetric( config.has2dMetric() );
        hasSecondaryAxes_ = config.hasSecondaryAxes();
        labelFormattedPosition_ = config.labelFormattedPosition();
        orientationsKey_ = config.getOrientationsKey();
    }

    public Surface createSurface( Rectangle plotBounds, Profile profile,
                                  PlaneAspect aspect ) {
        Profile p = profile;
        return PlaneSurface
              .createSurface( plotBounds, aspect,
                              p.xlog_, p.ylog_, p.xflip_, p.yflip_,
                              p.xlabel_, p.ylabel_, p.x2func_, p.y2func_,
                              p.x2label_, p.y2label_, p.captioner_,
                              p.annotateflags_, p.xyfactor_,
                              p.xcrowd_, p.ycrowd_, p.orientpolicy_,
                              p.minor_, p.shadow_,
                              p.gridcolor_, p.axlabelcolor_,
                              labelFormattedPosition_ );
    }

    public ConfigKey<?>[] getProfileKeys() {
        List<ConfigKey<?>> list = new ArrayList<ConfigKey<?>>();
        list.addAll( Arrays.asList( new ConfigKey<?>[] {
            XLOG_KEY,
            YLOG_KEY,
            XFLIP_KEY,
            YFLIP_KEY,
            XLABEL_KEY,
            YLABEL_KEY,
        } ) );
        if ( hasSecondaryAxes_ ) {
            list.addAll( Arrays.asList( new ConfigKey<?>[] {
                X2FUNC_KEY,
                Y2FUNC_KEY,
                X2LABEL_KEY,
                Y2LABEL_KEY,
            } ) );
        }
        list.addAll( Arrays.asList( new ConfigKey<?>[] {
            XYFACTOR_KEY,
            GRID_KEY,
            XCROWD_KEY,
            YCROWD_KEY,
            orientationsKey_,
            StyleKeys.MINOR_TICKS,
            StyleKeys.SHADOW_TICKS,
        } ) );
        list.addAll( Arrays.asList( StyleKeys.GRIDCOLOR_KEYSET.getKeys() ) );
        list.add( StyleKeys.AXLABEL_COLOR );
        list.addAll( Arrays.asList( StyleKeys.CAPTIONER.getKeys() ) );
        return list.toArray( new ConfigKey<?>[ 0 ] );
    }

    public Profile createProfile( ConfigMap config ) {
        boolean xlog = config.get( XLOG_KEY );
        boolean ylog = config.get( YLOG_KEY );
        boolean xflip = config.get( XFLIP_KEY );
        boolean yflip = config.get( YFLIP_KEY );
        String xlabel = config.get( XLABEL_KEY );
        String ylabel = config.get( YLABEL_KEY );
        DoubleUnaryOperator x2func = config.get( X2FUNC_KEY );
        DoubleUnaryOperator y2func = config.get( Y2FUNC_KEY );
        String x2label = config.get( X2LABEL_KEY );
        String y2label = config.get( Y2LABEL_KEY );
        double xyfactor = config.get( XYFACTOR_KEY );
        boolean grid = config.get( GRID_KEY );
        double xcrowd = config.get( XCROWD_KEY );
        double ycrowd = config.get( YCROWD_KEY );
        OrientationPolicy orientpolicy = config.get( orientationsKey_ );
        boolean minor = config.get( StyleKeys.MINOR_TICKS );
        boolean shadow = config.get( StyleKeys.SHADOW_TICKS );
        Color gridcolor = StyleKeys.GRIDCOLOR_KEYSET.createValue( config );
        if ( ! grid ) {
            gridcolor = null;
        }
        Color axlabelcolor = config.get( StyleKeys.AXLABEL_COLOR );
        Captioner captioner = StyleKeys.CAPTIONER.createValue( config );
        SideFlags annotateflags = SideFlags.ALL;
        return new Profile( xlog, ylog, xflip, yflip, xlabel, ylabel,
                            x2func, y2func, x2label, y2label,
                            captioner, annotateflags, xyfactor, xcrowd, ycrowd,
                            orientpolicy, minor, shadow,
                            gridcolor, axlabelcolor );
    }

    public ConfigKey<?>[] getAspectKeys() {
        return new ConfigKey<?>[] {
            XMIN_KEY, XMAX_KEY, XSUBRANGE_KEY,
            YMIN_KEY, YMAX_KEY, YSUBRANGE_KEY,
        };
    }

    public boolean useRanges( Profile profile, ConfigMap config ) {
        return createUnrangedAspect( profile, config ) == null;
    }

    public PlaneAspect createAspect( Profile profile, ConfigMap config,
                                     Range[] ranges ) {
        PlaneAspect unrangedAspect = createUnrangedAspect( profile, config );
        if ( unrangedAspect != null ) {
            return unrangedAspect;
        }
        else {
            Range xrange = ranges == null ? new Range() : ranges[ 0 ];
            Range yrange = ranges == null ? new Range() : ranges[ 1 ];
            double[] xlimits =
                getLimits( config, XMIN_KEY, XMAX_KEY, XSUBRANGE_KEY,
                           profile.xlog_, xrange );
            double[] ylimits =
                getLimits( config, YMIN_KEY, YMAX_KEY, YSUBRANGE_KEY,
                           profile.ylog_, yrange );
            return new PlaneAspect( xlimits, ylimits );
        }
    }

    public ConfigMap getAspectConfig( Surface surf ) {
        return surf instanceof PlaneSurface
             ? ((PlaneSurface) surf).getAspectConfig()
             : new ConfigMap();
    }

    public Range[] readRanges( Profile profile, PlotLayer[] layers,
                               DataStore dataStore ) {
        boolean[] logFlags = profile.getLogFlags();
        assert logFlags.length == 2;
        Range[] ranges = new Range[] { new Range(), new Range() };
        PlotUtil.extendCoordinateRanges( layers, ranges, logFlags, true,
                                         dataStore );
        return ranges;
    }

    public ConfigKey<?>[] getNavigatorKeys() {
        return new ConfigKey<?>[] {
            NAVAXES_KEY,
            XANCHOR_KEY,
            YANCHOR_KEY,
            StyleKeys.ZOOM_FACTOR,
        };
    }

    public Navigator<PlaneAspect> createNavigator( ConfigMap navConfig ) {
        double zoom = navConfig.get( StyleKeys.ZOOM_FACTOR );
        boolean[] navFlags = navConfig.get( NAVAXES_KEY );
        boolean xnav = navFlags[ 0 ];
        boolean ynav = navFlags[ 1 ];
        double xAnchor = navConfig.get( YANCHOR_KEY ) ? 0.0 : Double.NaN;
        double yAnchor = navConfig.get( XANCHOR_KEY ) ? 0.0 : Double.NaN;
        return new PlaneNavigator( zoom, xnav, ynav, xnav, ynav,
                                   xAnchor, yAnchor );
    }

    public PlotMetric getPlotMetric() {
        return plotMetric_;
    }

    /**
     * Returns a list of those keys which apply equally to the X and Y axes.
     *
     * @return   paired X/Y key objects
     */
    public XyKeyPair<?>[] getXyKeyPairs() {
        List<XyKeyPair<?>> list = new ArrayList<>();
        list.addAll( Arrays.asList( new XyKeyPair<?>[] {
            MIN_XYKEY,
            MAX_XYKEY,
            SUBRANGE_XYKEY,
            LOG_XYKEY,
            FLIP_XYKEY,
            LABEL_XYKEY,
            CROWD_XYKEY,
            ANCHOR_XYKEY,
        } ) );
        if ( hasSecondaryAxes_ ) {
            list.add( FUNC2_XYKEY );
            list.add( LABEL2_XYKEY );
        }
        return list.toArray( new XyKeyPair<?>[ 0 ] );
    }

    /**
     * Returns the OrientationPolicy config key used by this factory.
     *
     * @return  orientations key
     */
    public ConfigKey<OrientationPolicy> getOrientationsKey() {
        return orientationsKey_;
    }

    /**
     * Creates a config key for determining whether a named axis is
     * to be anchored at a data value of zero.
     * 
     * @param  axname  axis name
     * @param  dflt   anchor default value
     * @return  config key
     */
    public static ConfigKey<Boolean> createAxisAnchorKey( String axname,
                                                          boolean dflt ) {
        String axl = axname.toLowerCase();
        String axL = ConfigMeta.capitalise( axname );
        ConfigMeta meta =
            new ConfigMeta( axl + "anchor", "Anchor " + axL + " axis" );
        meta.setShortDescription( "Fix " + axL + " zero point?" );
        meta.setXmlDescription( new String[] {
            "<p>If true, then zoom actions",
            "will work in such a way that the zero point",
            "on the " + axL + " axis stays in the same position on the plot.",
            "</p>",
        } );
        return new BooleanConfigKey( meta, dflt );
    }

    /**
     * Creates a config key for fixing a minimum or maximum limit
     * for a named axis.
     *
     * @param   axname  axis name
     * @param   isMax   true for upper limit, false for lower limit
     * @return  new config key
     */
    public static ConfigKey<Double> createAxisLimitKey( String axname,
                                                        boolean isMax ) {
        String axl = axname.toLowerCase();
        String axL = ConfigMeta.capitalise( axname );
        String lim = isMax ? "max" : "min";
        String limit = isMax ? "Maximum" : "Minimum";
        String shortName = axl + lim;
        String longName = limit + " " + axL;
        ConfigMeta meta = new ConfigMeta( shortName, longName );
        meta.setShortDescription( limit + " " + axL + " data value" );
        meta.setXmlDescription( new String[] {
            "<p>" + limit + " value of the data coordinate",
            "on the " + axL + " axis.",
            "This sets the value before any subranging is applied.",
            "If not supplied, the value is determined from the plotted data.",
            "</p>",
        } );
        return DoubleConfigKey.createTextKey( meta );
    }

    /**
     * Creates a config key for determining whether a named Cartesian axis
     * is logarithmic or linear.
     *
     * @param  axname  axis name
     * @return  new config key, true for log scaling
     */
    public static ConfigKey<Boolean> createAxisLogKey( String axname ) {
        String axl = axname.toLowerCase();
        String axL = ConfigMeta.capitalise( axname );
        ConfigMeta meta = new ConfigMeta( axl + "log", axL + " Log" );
        meta.setShortDescription( "Logarithmic scale on " + axL + " axis?" );
        meta.setXmlDescription( new String[] {
            "<p>If false (the default), the scale on the " + axL + " axis",
            "is linear,",
            "if true it is logarithmic.",
            "</p>",
        } );
        return new BooleanConfigKey( meta );
    }

    /**
     * Creates a config key for determining whether a named Cartesian axis
     * is to be reversed.
     *
     * @param  axname  axis name
     * @return   new config key, true if sense is reversed
     */
    public static ConfigKey<Boolean> createAxisFlipKey( String axname ) {
        String axl = axname.toLowerCase();
        String axL = ConfigMeta.capitalise( axname );
        ConfigMeta meta = new ConfigMeta( axl + "flip", axL + " Flip" );
        meta.setShortDescription( "Flip scale on " + axL + " axis?" );
        meta.setXmlDescription( new String[] {
            "<p>If true, the scale on the " + axL + " axis",
            "will increase in the opposite sense from usual",
            "(e.g. right to left rather than left to right).",
            "</p>",
        } );
        return new BooleanConfigKey( meta );
    }

    /**
     * Creates a config key for selecting a subrange on a named Cartesian axis.
     *
     * @param  axname  axis name
     * @return  new config key
     */
    public static ConfigKey<Subrange> createAxisSubrangeKey( String axname ) {
        ConfigMeta meta = 
            SubrangeConfigKey
           .createAxisSubMeta( axname.toLowerCase(),
                               ConfigMeta.capitalise( axname ) );
        return new SubrangeConfigKey( meta, new Subrange(), -0.1, 1.1 );
    }

    /**
     * Creates a config key for determining tickmark crowding on a named axis.
     *
     * @param  axname  axis name
     * @return   new config key for dimensionless crowding figure
     */
    public static ConfigKey<Double> createAxisCrowdKey( String axname ) {
        String axl = axname.substring( 0, 1 ).toLowerCase();
        String axL = ConfigMeta.capitalise( axname );
        ConfigMeta meta =
            new ConfigMeta( axl + "crowd", axL + " Tick Crowding" );
        meta.setShortDescription( "Tick crowding on " + axL + " axis" );
        meta.setXmlDescription( new String[] {
            "<p>Determines how closely the tick marks are spaced",
            "on the " + axL + " axis.",
            "The default value is 1, meaning normal crowding.",
            "Larger values result in more ticks,",
            "and smaller values fewer ticks.",
            "Tick marks will not however be spaced so closely that",
            "the labels overlap each other,",
            "so to get very closely spaced marks you may need to",
            "reduce the font size as well.",
            "</p>",
        } );
        return StyleKeys.createCrowdKey( meta );
    }

    /**
     * Creates a config key for a secondary axis function.
     *
     * @param   primaryAxisName   name of primary axis, for instance "X"
     * @return  new config key
     */
    public static ConfigKey<JELFunction>
            createSecondaryAxisFunctionKey( String primaryAxisName ) {
        String axname = primaryAxisName.toLowerCase();
        final String varName = axname;
        final String secondaryEdge;
        if ( "X".equals( primaryAxisName ) ) {
            secondaryEdge = "top";
        }
        else if ( "Y".equals( primaryAxisName ) ) {
            secondaryEdge = "right";
        }
        else {
            secondaryEdge = null;
        }
        ConfigMeta meta =
            new ConfigMeta( axname + "2func",
                            "Secondary " + primaryAxisName + " Axis"
                          + " f(" + axname + ")" );
        meta.setStringUsage( "<function-of-" + axname + ">" );
        meta.setShortDescription( "Function of " + axname
                                + " mapping primary to secondary axis" );
        meta.setXmlDescription( new String[] {
            "<p>Defines a secondary " + primaryAxisName + " axis",
            "in terms of the primary one.",
            "If a secondary axis is defined in this way,",
            "then the axis opposite the primary one",
            ( secondaryEdge == null ? ""
                                    : "(i.e. on the " + secondaryEdge
                                                      + " side of the plot)" ),
            "will be annotated with the appropriate tickmarks.",
            "</p>",
            "<p>The value of this parameter is an",
            "<ref id='jel'>algebraic expression</ref> in terms of the variable",
            "<code>" + varName + "</code> giving the value",
            "on the secondary " + primaryAxisName + " axis",
            "corresponding to a given value",
            "on the primary " + primaryAxisName + " axis.",
            "</p>",
            "<p>For instance, if the primary " + primaryAxisName + " axis",
            "represents flux in Jansky,",
            "then supplying the expression",
            "\"<code>2.5*(23-log10(" + varName + "))-48.6</code>\"",
            "(or \"<code>janskyToAb(" + varName + ")</code>\")",
            "would annotate the secondary " + primaryAxisName + " axis",
            "as AB magnitudes.",
            "</p>",
            "<p>The function supplied should be monotonic",
            "and reasonably well-behaved,",
            "otherwise the secondary axis annotation may not work well.",
            "The application will attempt to make a sensible decision",
            "about whether to use linear or logarithmic tick marks.",
            "</p>",
        } );
        return new ConfigKey<JELFunction>( meta, JELFunction.class, null ) {
            public JELFunction stringToValue( String fexpr )
                    throws ConfigException {
                if ( fexpr == null || fexpr.trim().length() == 0 ) {
                    return null;
                }
                try {
                    return new JELFunction( varName, fexpr );
                }
                catch ( CompilationException e ) {
                    throw new ConfigException( this,
                                               "Expression \"" + fexpr + "\""
                                             + " not a function of " + varName
                                             + ": " + e.getMessage(), e );
                }
            }
            public String valueToString( JELFunction func ) {
                return func == null ? null : func.getExpression();
            }
            public Specifier<JELFunction> createSpecifier() {
                return new TextFieldSpecifier<JELFunction>( this, null );
            }
        };
    }

    /**
     * Returns a labelling config key for a secondary axis.
     *
     * @param  primaryAxisName  name of primary axis to which the
     *                          secondary axis relates
     * @return  new key
     */
    public static ConfigKey<String>
            createSecondaryAxisLabelKey( String primaryAxisName ) {
        String axName = primaryAxisName;
        ConfigMeta meta =
            new ConfigMeta( primaryAxisName.substring( 0, 1 ).toLowerCase()
                          + "2label",
                            "Secondary " + primaryAxisName + " Axis Label" );
        meta.setStringUsage( "<text>" );
        meta.setShortDescription( "Label for secondary " + primaryAxisName
                                + " axis" );
        meta.setXmlDescription( new String[] {
            "<p>Provides a string that will label the secondary",
            primaryAxisName,
            "axis.",
            "This appears on the opposite side of the plot to the",
            primaryAxisName,
            "axis itself.",
            "</p>",
        } );
        return new StringConfigKey( meta, null );
    }

    /**
     * Returns an OrientationPolicy config key suitable for use with this
     * factory, but with a configurable default value.
     *
     * @return  config key
     */
    public static ConfigKey<OrientationPolicy>
            createOrientationsKey( OrientationPolicy dflt ) {
        ConfigMeta meta = new ConfigMeta( "labelangle", "Tick Label Angles" );
        meta.setShortDescription( "Tick label orientations" );
        meta.setXmlDescription( new String[] {
            "<p>Controls the orientation of numeric labels on the axes.",
            "In most cases labels are written horizontally on both horizontal",
            "and vertical axes, but this option provides the possibility",
            "to write them at an angle which may be able to accommodate",
            "more labels on the horizontal axis,",
            "especially if the labels are long or a high crowding factor",
            "is requested.",
            "</p>",
            "<p>Note that the <code>" + OrientationPolicy.ADAPTIVE + "</code>",
            "option is currently not perfect, and can sometimes lead to",
            "suboptimal border placement.",
            "</p>",
        } );
        return new OptionConfigKey<OrientationPolicy>
                                  ( meta, OrientationPolicy.class,
                                    OrientationPolicy.getOptions(), dflt ) {
            public String getXmlDescription( OrientationPolicy orient ) {
                return orient.getDescription();
            }
        }.setOptionUsage()
         .addOptionsXml();
    }

    /**
     * Utility method to interrogate axis range configuration variables
     * and work out the actual range to use on a given Cartesian axis.
     * If not enough information is supplied to determine the definite range,
     * null is returned.
     *
     * @param  config  config map containing config values
     * @param  minKey  config key giving axis lower bound before subranging
     * @param  maxKey  config key giving axis upper bound before subranging
     * @param  subrangeKey  config key giving subrange value
     * @param  isLog  true for logarithmic axis, false for linear
     * @param  range   data range on axis; may be partially populated or null
     * @return  2-element array giving definite axis (lower,upper) bounds,
     *          or null
     */
    public static double[] getLimits( ConfigMap config,
                                      ConfigKey<Double> minKey,
                                      ConfigKey<Double> maxKey,
                                      ConfigKey<Subrange> subrangeKey,
                                      boolean isLog, Range range ) {
        return getLimits( config.get( minKey ), config.get( maxKey ),
                          config.get( subrangeKey ), isLog, range );
    }

    /**
     * Utility method to determine actual axis limits based on
     * requested high/low values and a subrange.
     * If not enough information is supplied to determine the definite range,
     * null is returned.
     *
     * @param  lo   requested lower bound before subranging, may be NaN
     * @param  hi   requested upper bound before subranging, may be NaN
     * @param  subrange  requested subrange
     * @param  isLog  true for logarithmic axis, false for linear
     * @param  range   actual data range on axis;
     *                 may be partially populated or null
     * @return  2-element array giving definite axis (lower,upper) bounds,
     *          or null
     */
    public static double[] getLimits( double lo, double hi, Subrange subrange,
                                      boolean isLog, Range range ) {
        boolean isFinite = lo < hi                 // entails neither is NaN
                        && ! Double.isInfinite( lo )
                        && ! Double.isInfinite( hi )
                        && ! ( isLog && lo <= 0 );
        if ( isFinite ) {
            return PlotUtil.scaleRange( lo, hi, subrange, isLog );
        }
        else if ( range != null ) {
            Range r1 = new Range( range );
            r1.limit( lo, hi );
            double[] b1 = r1.getFiniteBounds( isLog );
            return PlotUtil.scaleRange( b1[ 0 ], b1[ 1 ], subrange, isLog );
        }
        else {
            return null;
        }
    }

    /**
     * Attempts to determine an aspect value from profile and configuration,
     * but not ranging, information.  If not enough information is supplied,
     * null will be returned.
     *
     * @param  profile   config profile
     * @param  config  map which may contain additional range config info
     * @return  aspect, or null
     */
    private static PlaneAspect createUnrangedAspect( Profile profile,
                                                     ConfigMap config ) {
        double[] xlimits =
            getLimits( config, XMIN_KEY, XMAX_KEY, XSUBRANGE_KEY,
                       profile.xlog_, null );
        double[] ylimits =
            getLimits( config, YMIN_KEY, YMAX_KEY, YSUBRANGE_KEY,
                       profile.ylog_, null );
        return xlimits == null || ylimits == null
             ? null
             : new PlaneAspect( xlimits, ylimits );
    }

    /**
     * Specifies configuration options for the PlaneSurfaceFactory.
     * An instance of this interface is fed to the constructor.
     */
    public static interface Config {

        /**
         * Returns true if it may make sense to measure distances on
         * the plane surfaces constructed.
         *
         * @return   true to allow 2d measurement
         */
        boolean has2dMetric();

        /**
         * Returns true if secondary axis labelling can be configured
         * for the surface factory.
         *
         * @return  true to allow secondary axes
         */
        boolean hasSecondaryAxes();

        /**
         * Returns true if formatted position strings should be accompanied
         * by the axis labels.
         *
         * @return  true to add axis labels to formatted position strings
         * @see  uk.ac.starlink.ttools.plot2.Surface#formatPosition
         */
        boolean labelFormattedPosition();

        /**
         * Returns a suitable OrientationPolicy config key.
         *
         * @return  config key
         */
        ConfigKey<OrientationPolicy> getOrientationsKey();
    }

    /**
     * PlotMetric implementation for plane surface.
     */
    private static class PlanePlotMetric implements PlotMetric {
        private final boolean has2dMetric_;

        /**
         * Constructor.
         *
         * @param  has2dMetric  true if it may make sense to measure distances
         *                      that are not parallel to either axis
         */
        PlanePlotMetric( boolean has2dMetric ) {
            has2dMetric_ = has2dMetric;
        }

        public LabelledLine[] getMeasures( Surface surf,
                                           Point2D gp0, Point2D gp1 ) {
            final Axis[] axes;
            if ( surf instanceof PlaneSurface ) {
                axes = ((PlaneSurface) surf).getAxes();
            }
            else {
                return new LabelledLine[ 0 ];
            }
            List<LabelledLine> lineList = new ArrayList<LabelledLine>();
            Axis xAxis = axes[ 0 ];
            Axis yAxis = axes[ 1 ];
            double gx0 = gp0.getX();
            double gy0 = gp0.getY();
            double gx1 = gp1.getX();
            double gy1 = gp1.getY();
            double dx0 = xAxis.graphicsToData( gx0 );
            double dy0 = yAxis.graphicsToData( gy0 );
            double dx1 = xAxis.graphicsToData( gx1 );
            double dy1 = yAxis.graphicsToData( gy1 );

            /* Add X and Y vector component lines. */
            Point2D gp10 = new Point2D.Double( gx1, gy0 );
            double ex =
                Math.max( Math.abs( xAxis.graphicsToData( gx0 + 1 ) - dx0 ),
                          Math.abs( xAxis.graphicsToData( gx1 + 1 ) - dx1 ) );
            double ey =
                Math.max( Math.abs( yAxis.graphicsToData( gy0 + 1 ) - dy0 ),
                          Math.abs( yAxis.graphicsToData( gy1 + 1 ) - dy1 ) );
            String xLabel = PlotUtil.formatNumber( Math.abs( dx1 - dx0 ), ex );
            String yLabel = PlotUtil.formatNumber( Math.abs( dy1 - dy0 ), ey );
            lineList.add( new LabelledLine( gp0, gp10, xLabel ) );
            lineList.add( new LabelledLine( gp10, gp1, yLabel ) );

            /* If the plane is linear in both directions, add a vector line.
             * If either axis is nonlinear, there isn't really a sensible
             * metric on the space, so in that case don't. */
            if ( has2dMetric_ && xAxis.getScale().isLinear()
                              && yAxis.getScale().isLinear() ) {
                double gx01 = gx1 - gx0;
                double gy01 = gy1 - gy0;
                double g01 = Math.hypot( gx01, gy01 );
                double fact2 = ( g01 + 1.0 ) / g01;
                double gx2 = gx0 + fact2 * gx01;
                double gy2 = gy0 + fact2 * gy01;
                double dx2 = xAxis.graphicsToData( gx2 );
                double dy2 = yAxis.graphicsToData( gy2 );
                double d01 = Math.hypot( dx1 - dx0, dy1 - dy0 );
                double d12 = Math.hypot( dx2 - dx1, dy2 - dy1 );
                String hLabel = PlotUtil.formatNumber( d01, d12 );
                lineList.add( new LabelledLine( gp0, gp1, hLabel ) );
            }
            return lineList.toArray( new LabelledLine[ 0 ] );
        }
    }

    /**
     * Profile class which defines fixed configuration items for
     * a PlaneSurface.
     * Instances of this class are normally obtained from the
     * {@link #createProfile createProfile} method.
     */
    public static class Profile {
        private final boolean xlog_;
        private final boolean ylog_;
        private final boolean xflip_;
        private final boolean yflip_;
        private final String xlabel_;
        private final String ylabel_;
        private final DoubleUnaryOperator x2func_;
        private final DoubleUnaryOperator y2func_;
        private final String x2label_;
        private final String y2label_;
        private final Captioner captioner_;
        private final SideFlags annotateflags_;
        private final double xyfactor_;
        private final double xcrowd_;
        private final double ycrowd_;
        private final OrientationPolicy orientpolicy_;
        private final boolean minor_;
        private final boolean shadow_;
        private final Color gridcolor_;
        private final Color axlabelcolor_;

        /**
         * Constructor.
         *
         * @param  xlog   whether to use logarithmic scaling on X axis
         * @param  ylog   whether to use logarithmic scaling on Y axis
         * @param  xflip  whether to invert direction of X axis
         * @param  yflip  whether to invert direction of Y axis
         * @param  xlabel  text for labelling X axis
         * @param  ylabel  text for labelling Y axis
         * @param  x2func  function mapping primary to secondary X data coords,
         *                 or null for no secondary X axis
         * @param  y2func  function mapping primary to secondary Y data coords,
         *                 or null for no secondary Y axis
         * @param  x2label  text for labelling secondary X axis
         * @param  y2label  text for labelling secondary Y axis
         * @param  captioner  text renderer for axis labels etc
         * @param  annotateflags  which sides to annotate
         * @param  xyfactor   ratio (X axis unit length)/(Y axis unit length),
         *                    or NaN to use whatever bounds shape and
         *                    axis limits give you
         * @param  xcrowd  crowding factor for tick marks on X axis;
         *                 1 is normal
         * @param  ycrowd  crowding factor for tick marks on Y axis;
         *                 1 is normal
         * @param  orientpolicy  tick label orientation policy
         * @param  minor   whether to paint minor tick marks on axes
         * @param  shadow  whether to paint shadow ticks on opposite axes
         *                 if no secondary axis
         * @param  gridcolor  colour of grid lines, or null for none
         * @param  axlabelcolor  colour of axis labels
         */
        public Profile( boolean xlog, boolean ylog,
                        boolean xflip, boolean yflip,
                        String xlabel, String ylabel,
                        DoubleUnaryOperator x2func, DoubleUnaryOperator y2func,
                        String x2label, String y2label,
                        Captioner captioner, SideFlags annotateflags,
                        double xyfactor, double xcrowd, double ycrowd,
                        OrientationPolicy orientpolicy,
                        boolean minor, boolean shadow,
                        Color gridcolor, Color axlabelcolor ) {
            xlog_ = xlog;
            ylog_ = ylog;
            xflip_ = xflip;
            yflip_ = yflip;
            xlabel_ = xlabel;
            ylabel_ = ylabel;
            x2func_ = x2func;
            y2func_ = y2func;
            x2label_ = x2label;
            y2label_ = y2label;
            captioner_ = captioner;
            annotateflags_ = annotateflags;
            xyfactor_ = xyfactor;
            xcrowd_ = xcrowd;
            ycrowd_ = ycrowd;
            orientpolicy_ = orientpolicy;
            minor_ = minor;
            shadow_ = shadow;
            gridcolor_ = gridcolor;
            axlabelcolor_ = axlabelcolor;
        }

        /**
         * Returns a 2-element array giving X and Y log flags.
         *
         * @return  (xlog, ylog) array
         */
        public boolean[] getLogFlags() {
            return new boolean[] { xlog_, ylog_ };
        }

        /**
         * Returns a new profile instance the same as this one,
         * except that the flags for which sides to annotate are replaced.
         *
         * @param  annotateFlags  which sides to annotate
         * @param  addSecondary   if true, secondary axis annotations
         *                        duplicating the primary axis ones
         *                        will be added
         * @return   adjusted profile
         */
        public Profile fixAnnotation( SideFlags annotateFlags,
                                      boolean addSecondary ) {
            return new Profile( xlog_, ylog_, xflip_, yflip_, xlabel_, ylabel_,
                                addSecondary ? x -> x : x2func_,
                                addSecondary ? y -> y : y2func_,
                                addSecondary ? xlabel_ : x2label_,
                                addSecondary ? ylabel_ : y2label_,
                                captioner_, annotateFlags,
                                xyfactor_, xcrowd_, ycrowd_, orientpolicy_,
                                minor_, shadow_, gridcolor_, axlabelcolor_ );
        }
    }
}
