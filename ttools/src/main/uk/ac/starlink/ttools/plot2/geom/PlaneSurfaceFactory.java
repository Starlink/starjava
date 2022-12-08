package uk.ac.starlink.ttools.plot2.geom;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.DoubleUnaryOperator;
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
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.config.SubrangeConfigKey;
import uk.ac.starlink.ttools.plot2.data.DataStore;

/**
 * Surface factory for flat 2-d plotting.
 *
 * @author   Mark Taylor
 * @since    19 Feb 2013
 */
public class PlaneSurfaceFactory
            implements SurfaceFactory<PlaneSurfaceFactory.Profile,PlaneAspect> {

    /** Config key for X axis lower bound, before subranging. */
    public static final ConfigKey<Double> XMIN_KEY =
        createAxisLimitKey( "X", false );

    /** Config key for X axis upper bound, before subranging. */
    public static final ConfigKey<Double> XMAX_KEY =
        createAxisLimitKey( "X", true );

    /** Config key for X axis subrange. */
    public static final ConfigKey<Subrange> XSUBRANGE_KEY =
        createAxisSubrangeKey( "X" );

    /** Config key for Y axis lower bound, before subranging. */
    public static final ConfigKey<Double> YMIN_KEY =
        createAxisLimitKey( "Y", false );

    /** Config key for Y axis upper bound, before subranging. */
    public static final ConfigKey<Double> YMAX_KEY =
        createAxisLimitKey( "Y", true );

    /** Config key for Y axis subrange. */
    public static final ConfigKey<Subrange> YSUBRANGE_KEY =
        createAxisSubrangeKey( "Y" );

    /** Config key for X axis log scale flag. */
    public static final ConfigKey<Boolean> XLOG_KEY =
        createAxisLogKey( "X" );

    /** Config key for Y axis log scale flag. */
    public static final ConfigKey<Boolean> YLOG_KEY =
        createAxisLogKey( "Y" );

    /** Config key for X axis flip flag. */
    public static final ConfigKey<Boolean> XFLIP_KEY =
        createAxisFlipKey( "X" );

    /** Config key for Y axis flip flag. */
    public static final ConfigKey<Boolean> YFLIP_KEY =
        createAxisFlipKey( "Y" );

    /** Config key for X axis text label. */
    public static final ConfigKey<String> XLABEL_KEY =
        StyleKeys.createAxisLabelKey( "X" );

    /** Config key for Y axis text label.*/
    public static final ConfigKey<String> YLABEL_KEY =
        StyleKeys.createAxisLabelKey( "Y" );

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
    public static final ConfigKey<Double> XCROWD_KEY =
        createAxisCrowdKey( "X" );

    /** Config key to control tick mark crowding on Y axis. */
    public static final ConfigKey<Double> YCROWD_KEY =
        createAxisCrowdKey( "Y" );

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
    public static final ConfigKey<Boolean> XANCHOR_KEY =
        createAxisAnchorKey( "X", false );

    /** Config key to anchor Y axis during zooms. */
    public static final ConfigKey<Boolean> YANCHOR_KEY =
        createAxisAnchorKey( "Y", false );

    private final PlotMetric plotMetric_;

    /**
     * Constructs a PlaneSurfaceFactory with default characteristics.
     */
    public PlaneSurfaceFactory() {
        this( true );
    }

    /**
     * Constructs a PlaneSurfaceFactory with configurable characteristics.
     *
     * @param  has2dMetric  true if it may make sense to measure distances
     *                      that are not parallel to either axis
     */
    public PlaneSurfaceFactory( boolean has2dMetric ) {
        plotMetric_ = new PlanePlotMetric( has2dMetric );
    }

    public Surface createSurface( Rectangle plotBounds, Profile profile,
                                  PlaneAspect aspect ) {
        Profile p = profile;
        return PlaneSurface
              .createSurface( plotBounds, aspect,
                              p.xlog_, p.ylog_, p.xflip_, p.yflip_,
                              p.xlabel_, p.ylabel_, p.x2func_, p.y2func_,
                              p.x2label_, p.y2label_, p.captioner_,
                              p.xyfactor_, p.grid_, p.xcrowd_, p.ycrowd_,
                              p.minor_, p.gridcolor_, p.axlabelcolor_ );
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
            XYFACTOR_KEY,
            GRID_KEY,
            XCROWD_KEY,
            YCROWD_KEY,
            StyleKeys.MINOR_TICKS,
            StyleKeys.GRID_COLOR,
            StyleKeys.AXLABEL_COLOR,
        } ) );
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
        DoubleUnaryOperator x2func = null;
        DoubleUnaryOperator y2func = null;
        String x2label = null;
        String y2label = null;
        double xyfactor = config.get( XYFACTOR_KEY );
        boolean grid = config.get( GRID_KEY );
        double xcrowd = config.get( XCROWD_KEY );
        double ycrowd = config.get( YCROWD_KEY );
        boolean minor = config.get( StyleKeys.MINOR_TICKS );
        Color gridcolor = config.get( StyleKeys.GRID_COLOR );
        Color axlabelcolor = config.get( StyleKeys.AXLABEL_COLOR );
        Captioner captioner = StyleKeys.CAPTIONER.createValue( config );
        return new Profile( xlog, ylog, xflip, yflip, xlabel, ylabel,
                            x2func, y2func, x2label, y2label,
                            captioner, xyfactor, grid, xcrowd, ycrowd, minor,
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
             * If either axis is logarithmic, there isn't really a sensible
             * metric on the space, so in that case don't. */
            if ( has2dMetric_ && xAxis.isLinear() && yAxis.isLinear() ) {
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
        private final double xyfactor_;
        private final boolean grid_;
        private final double xcrowd_;
        private final double ycrowd_;
        private final boolean minor_;
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
         * @param  xyfactor   ratio (X axis unit length)/(Y axis unit length),
         *                    or NaN to use whatever bounds shape and
         *                    axis limits give you
         * @param  grid   whether to draw grid lines
         * @param  xcrowd  crowding factor for tick marks on X axis;
         *                 1 is normal
         * @param  ycrowd  crowding factor for tick marks on Y axis;
         *                 1 is normal
         * @param  minor   whether to paint minor tick marks on axes
         * @param  gridcolor  colour of grid lines, if plotted
         * @param  axlabelcolor  colour of axis labels
         */
        public Profile( boolean xlog, boolean ylog,
                        boolean xflip, boolean yflip,
                        String xlabel, String ylabel,
                        DoubleUnaryOperator x2func, DoubleUnaryOperator y2func,
                        String x2label, String y2label,
                        Captioner captioner, double xyfactor, boolean grid,
                        double xcrowd, double ycrowd, boolean minor,
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
            xyfactor_ = xyfactor;
            grid_ = grid;
            xcrowd_ = xcrowd;
            ycrowd_ = ycrowd;
            minor_ = minor;
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
    }
}
