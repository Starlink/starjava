package uk.ac.starlink.ttools.plot2.geom;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot2.Captioner;
import uk.ac.starlink.ttools.plot2.Navigator;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotMetric;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Scale;
import uk.ac.starlink.ttools.plot2.Subrange;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.SurfaceFactory;
import uk.ac.starlink.ttools.plot2.config.BooleanConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigException;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.DoubleConfigKey;
import uk.ac.starlink.ttools.plot2.config.OptionConfigKey;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.DataStore;

/**
 * Surface factory for 3-d plotting.
 *
 * <p>This can be used in one of two modes (determined at construction time),
 * isotropic and non-isotropic.
 * In isotropic mode, the scaling on each of the 3 axes is the same,
 * and in non-isotropic mode they can vary independently of each other.
 * The profile and aspect configuration keys (that is, the user interface)
 * are different according to which mode is in effect, but the actual
 * surfaces generated are the same either way, undistinguished instances
 * of {@link CubeSurface}.
 *
 * @author   Mark Taylor
 * @since    20 Feb 2013
 */
public class CubeSurfaceFactory
             implements SurfaceFactory<CubeSurfaceFactory.Profile,CubeAspect> {

    private final boolean isIso_;

    /** Config key for X axis scale flag. */
    public static final ConfigKey<Scale> XSCALE_KEY =
        PlaneSurfaceFactory.createAxisScaleKey( "X" );

    /** Config key for Y axis scale flag. */
    public static final ConfigKey<Scale> YSCALE_KEY =
        PlaneSurfaceFactory.createAxisScaleKey( "Y" );

    /** Config key for Z axis scale flag. */
    public static final ConfigKey<Scale> ZSCALE_KEY =
        PlaneSurfaceFactory.createAxisScaleKey( "Z" );

    /** Config key for X axis deprecated log scale flag. */
    public static final ConfigKey<Boolean> XLOG_KEY =
        PlaneSurfaceFactory.createAxisLogKey( "X" );

    /** Config key for Y axis deprecated log scale flag. */
    public static final ConfigKey<Boolean> YLOG_KEY =
        PlaneSurfaceFactory.createAxisLogKey( "Y" );

    /** Config key for Z axis deprecated log scale flag. */
    public static final ConfigKey<Boolean> ZLOG_KEY =
        PlaneSurfaceFactory.createAxisLogKey( "Z" );

    /** Config key for X axis flip flag. */
    public static final ConfigKey<Boolean> XFLIP_KEY =
        PlaneSurfaceFactory.createAxisFlipKey( "X" );

    /** Config key for Y axis flip flag. */
    public static final ConfigKey<Boolean> YFLIP_KEY =
        PlaneSurfaceFactory.createAxisFlipKey( "Y" );

    /** Config key for Z axis flip flag. */
    public static final ConfigKey<Boolean> ZFLIP_KEY =
        PlaneSurfaceFactory.createAxisFlipKey( "Z" );

    /** Config key for X axis text label. */
    public static final ConfigKey<String> XLABEL_KEY =
        StyleKeys.createAxisLabelKey( "X" );

    /** Config key for Y axis text label. */
    public static final ConfigKey<String> YLABEL_KEY =
        StyleKeys.createAxisLabelKey( "Y" );

    /** Config key for Z axis text label. */
    public static final ConfigKey<String> ZLABEL_KEY =
        StyleKeys.createAxisLabelKey( "Z" );

    /** Config key for whether to draw axis wire frame. */
    public static final ConfigKey<Boolean> FRAME_KEY =
        new BooleanConfigKey(
            new ConfigMeta( "frame", "Draw wire frame" )
           .setShortDescription( "Draw wire frame?" )
           .setXmlDescription( new String[] {
                "<p>If true, a cube wire frame with labelled axes",
                "is drawn to indicate the limits of the plotted 3D region.",
                "If false, no wire frame and no axes are drawn.",
                "</p>",
            } )
        , true );

    /** Config key for X axis tick mark crowding. */
    public static final ConfigKey<Double> XCROWD_KEY =
        PlaneSurfaceFactory.createAxisCrowdKey( "X" );

    /** Config key for Y axis tick mark crowding. */
    public static final ConfigKey<Double> YCROWD_KEY =
        PlaneSurfaceFactory.createAxisCrowdKey( "Y" );

    /** Config key for Z axis tick mark crowding. */
    public static final ConfigKey<Double> ZCROWD_KEY =
        PlaneSurfaceFactory.createAxisCrowdKey( "Z" );

    /** Config key for isotropic tick mark crowding. */
    public static final ConfigKey<Double> ISOCROWD_KEY =
        StyleKeys.createCrowdKey(
            new ConfigMeta( "crowd", "Tick Crowding" )
           .setShortDescription( "Crowding of axis ticks" )
           .setXmlDescription( new String[] {
                "<p>Determines how closely tick marks are spaced",
                "on the wire frame axes.",
                "The default value is 1, meaning normal crowding.",
                "Larger values result in more grid lines,",
                "and smaller values in fewer grid lines.",
                "</p>",
            } )
        );

    /** Config key for X axis lower bound, before subranging. */
    public static final ConfigKey<Double> XMIN_KEY =
        PlaneSurfaceFactory.createAxisLimitKey( "X", false );

    /** Config key for X axis upper bound, before subranging. */
    public static final ConfigKey<Double> XMAX_KEY =
        PlaneSurfaceFactory.createAxisLimitKey( "X", true );

    /** Config key for X axis subrange. */
    public static final ConfigKey<Subrange> XSUBRANGE_KEY =
        PlaneSurfaceFactory.createAxisSubrangeKey( "X" );

    /** Config key for Y axis lower bound, before subranging. */
    public static final ConfigKey<Double> YMIN_KEY =
        PlaneSurfaceFactory.createAxisLimitKey( "Y", false );

    /** Config key for Y axis upper bound, before subranging. */
    public static final ConfigKey<Double> YMAX_KEY =
        PlaneSurfaceFactory.createAxisLimitKey( "Y", true );

    /** Config key for Y axis subrange. */
    public static final ConfigKey<Subrange> YSUBRANGE_KEY =
        PlaneSurfaceFactory.createAxisSubrangeKey( "Y" );

    /** Config key for Z axis lower bound, before subranging. */
    public static final ConfigKey<Double> ZMIN_KEY =
        PlaneSurfaceFactory.createAxisLimitKey( "Z", false );

    /** Config key for Z axis upper bound, before subranging. */
    public static final ConfigKey<Double> ZMAX_KEY =
        PlaneSurfaceFactory.createAxisLimitKey( "Z", true );

    /** Config key for Z axis subrange. */
    public static final ConfigKey<Subrange> ZSUBRANGE_KEY =
        PlaneSurfaceFactory.createAxisSubrangeKey( "Z" );

    /** Config key for X axis central position key (isotropic only). */
    public static final ConfigKey<Double> XC_KEY =
        createIsoCenterKey( "X" );

    /** Config key for Y axis central position key (isotropic only). */
    public static final ConfigKey<Double> YC_KEY =
        createIsoCenterKey( "Y" );

    /** Config key for Z axis central position key (isotropic only). */
    public static final ConfigKey<Double> ZC_KEY =
        createIsoCenterKey( "Z" );

    /** Config key for forcing isometric view in non-isotropic mode. */
    public static final ConfigKey<Boolean> FORCEISO_KEY =
        new BooleanConfigKey(
            new ConfigMeta( "isometric", "Isometric" )
           .setShortDescription( "Fix equal X/Y/Z scales" )
           .setXmlDescription( new String[] {
                "<p>If set true, the scaling will be the same on the",
                "X, Y and Z axes, so that positions retain their",
                "natural position in 3-d Cartesian space.",
                "If false, the three axes will be scaled independently,",
                "so that the positions may be squashed in some directions.",
                "This option is ignored if there is a mix of linear",
                "and logarithmic axes.",
                "</p>",
            } )
        , false );

    /** Config key for cube edge length (isotropic only). */
    public static final ConfigKey<Double> SCALE_KEY =
        DoubleConfigKey.createTextKey(
            new ConfigMeta( "scale", "Cube Edge Length" )
           .setShortDescription( "Cube edge length" )
           .setXmlDescription( new String[] {
                "<p>The length of the cube sides in data coordinates.",
                "This will be determined from the data range if not supplied.",
                "</p>",
            } )
        );

    /** Config key for first Euler angle of rotation, units of degrees. */
    public static final ConfigKey<Double> PHI_KEY =
        DoubleConfigKey.createSliderKey(
            new ConfigMeta( "phi", "Rotation \u03c6" )
           .setShortDescription( "First ZXZ Euler angle of view" )
           .setXmlDescription( new String[] {
                "<p>First of the Euler angles, in the ZXZ sequence,",
                "defining the rotation of the plotted 3d space.",
                "Units are degrees.",
                "This is the rotation around the initial Z axis applied before",
                "the plot is viewed.",
                "</p>",
            } )
           .setStringUsage( "<degrees>" )
        , 30, -180, 180, false );

    /** Config key for second Euler angle of rotation, units of degrees. */
    public static final ConfigKey<Double> THETA_KEY =
        DoubleConfigKey.createSliderKey(
            new ConfigMeta( "theta", "Rotation \u03b8" )
           .setShortDescription( "Second ZXZ Euler angle of view" )
           .setXmlDescription( new String[] {
                "<p>Second of the Euler angles, in the ZXZ sequence,",
                "defining the rotation of the plotted 3d space.",
                "Units are degrees.",
                "This is the rotation towards the viewer.",
                "</p>",
            } )
           .setStringUsage( "<degrees>" )
        , -15, -180, 180, false );

    /** Config key for third Euler angle of rotation, units of degrees. */
    public static final ConfigKey<Double> PSI_KEY =
         DoubleConfigKey.createSliderKey(
             new ConfigMeta( "psi", "Rotation \u03c8" )
            .setShortDescription( "Third ZXZ Euler angle of view" )
            .setXmlDescription( new String[] {
                "<p>Second of the Euler angles, in the ZXZ sequence,",
                "defining the rotation of the plotted 3d space.",
                "Units are degrees.",
                "</p>",
             } )
            .setStringUsage( "<degrees>" )
         , 0, -180, 180, false );
 
    /** Config key for zoom factor. */
    public static final ConfigKey<Double> ZOOM_KEY =
        DoubleConfigKey.createSliderKey(
            new ConfigMeta( "zoom", "Zoom factor" )
           .setShortDescription( "Magnification factor" )
           .setXmlDescription( new String[] {
                "<p>Sets the magnification factor at which the the",
                "plotted 3D region itself is viewed,",
                "without affecting its contents.",
                "The default value is 1, which means the cube",
                "fits into the plotting space however it is rotated.",
                "Much higher zoom factors will result in parts of the",
                "plotting region and axes being drawn outside of",
                "the plotting region (so invisible).",
                "</p>",
            } )
           .setStringUsage( "<factor>" )
        , 1.0, 0.1, 10.0, true );

    /** Config key for graphics X offset, units of 1/2 screen size. */
    public static final ConfigKey<Double> XOFF_KEY =
        DoubleConfigKey.createSliderKey(
            new ConfigMeta( "xoff", "X offset of centre" )
           .setShortDescription( "Horizontal offset in pixels" )
           .setXmlDescription( new String[] {
                "<p>Shifts the whole plot within the plotting region",
                "by the given number of pixels in the horizontal direction.",
                "</p>",
            } )
           .setStringUsage( "<pixels>" )
        , 0, -2, +2, false );

    /** Config key for graphics Y offset, units of 1/2 screen size. */
    public static final ConfigKey<Double> YOFF_KEY =
        DoubleConfigKey.createSliderKey(
            new ConfigMeta( "yoff", "Y offset of centre" )
           .setShortDescription( "Vertical offset in pixels" )
           .setXmlDescription( new String[] {
                "<p>Shifts the whole plot within the plotting region",
                "by the given number of pixels in the vertical direction.",
                "</p>",
            } )
           .setStringUsage( "<pixels>" )
        , 0, -2, +2, false );

    /** Config key for axis label orientation. */
    public static final ConfigKey<OrientationPolicy> ORIENTATIONS_KEY =
        new OptionConfigKey<OrientationPolicy>(
                new ConfigMeta( "labelangle", "Tick Label Angles" )
               .setShortDescription( "Tick label orientations" )
               .setXmlDescription( new String[] {
                    "<p>Controls the orientation of the numeric labels",
                    "on the axes.",
                    "By default the labels are written parallel to the axes",
                    "as long as they fit, but if they become too crowded",
                    "they can be angled so they don't overlap.",
                    "This option controls the choice of parallel or angled",
                    "labelling.",
                    "</p>",
                } ), 
                OrientationPolicy.class, OrientationPolicy.getOptions(),
                OrientationPolicy.ADAPTIVE ) {
            public String getXmlDescription( OrientationPolicy orient ) {
                return orient.getDescription();
            }
        }.setOptionUsage()
         .addOptionsXml();

    /** Proportional auto-ranging isotropic snap-to-origin threshold. */
    public static final double ISO_CENTER_TOLERANCE = 0.1;

    /**
     * Constructs an isotropic or non-isotropic cube surface factory.
     *
     * @param   isIso  whether to operate in isotropic mode
     */
    public CubeSurfaceFactory( boolean isIso ) {
        isIso_ = isIso;
    }

    public Surface createSurface( Rectangle plotBounds, Profile profile,
                                  CubeAspect aspect ) {
        Profile p = profile;
        return CubeSurface
              .createSurface( plotBounds, aspect, p.forceiso_,
                              new Scale[] { p.xscale_, p.yscale_, p.zscale_ },
                              new boolean[] { p.xflip_, p.yflip_, p.zflip_ },
                              new String[] { p.xlabel_, p.ylabel_, p.zlabel_ },
                              new double[] { p.xcrowd_, p.ycrowd_, p.zcrowd_ },
                              p.orientpolicy_, p.captioner_, p.frame_, p.minor_,
                              p.antialias_ );
    }

    public ConfigKey<?>[] getProfileKeys() {
        List<ConfigKey<?>> list = new ArrayList<ConfigKey<?>>();
        if ( ! isIso_ ) {
            list.addAll( Arrays.asList( new ConfigKey<?>[] {
                XSCALE_KEY,
                YSCALE_KEY,
                ZSCALE_KEY,
                XLOG_KEY,
                YLOG_KEY,
                ZLOG_KEY,
                XFLIP_KEY,
                YFLIP_KEY,
                ZFLIP_KEY,
                FORCEISO_KEY,
                XLABEL_KEY,
                YLABEL_KEY,
                ZLABEL_KEY,
                XCROWD_KEY,
                YCROWD_KEY,
                ZCROWD_KEY,
                ORIENTATIONS_KEY,
            } ) );
        }
        else {
            list.addAll( Arrays.asList( new ConfigKey<?>[] {
                ISOCROWD_KEY,
                ORIENTATIONS_KEY,
            } ) );
        }
        list.addAll( Arrays.asList( new ConfigKey<?>[] {
            FRAME_KEY, 
            StyleKeys.MINOR_TICKS,
            StyleKeys.GRID_ANTIALIAS,
        } ) );
        list.addAll( Arrays.asList( StyleKeys.CAPTIONER.getKeys() ) );
        return list.toArray( new ConfigKey<?>[ 0 ] );
    }

    public Profile createProfile( ConfigMap config ) {
        Scale xscale =
              isIso_
            ? Scale.LINEAR
            : PlaneSurfaceFactory.getScale( XSCALE_KEY, XLOG_KEY, config );
        Scale yscale =
              isIso_
            ? Scale.LINEAR
            : PlaneSurfaceFactory.getScale( YSCALE_KEY, YLOG_KEY, config );
        Scale zscale =
              isIso_
            ? Scale.LINEAR
            : PlaneSurfaceFactory.getScale( ZSCALE_KEY, ZLOG_KEY, config );
        boolean xflip = isIso_ ? false : config.get( XFLIP_KEY );
        boolean yflip = isIso_ ? false : config.get( YFLIP_KEY );
        boolean zflip = isIso_ ? false : config.get( ZFLIP_KEY );
        String xlabel = isIso_ ? "X" : config.get( XLABEL_KEY );
        String ylabel = isIso_ ? "Y" : config.get( YLABEL_KEY );
        String zlabel = isIso_ ? "Z" : config.get( ZLABEL_KEY );
        double xcrowd = config.get( isIso_ ? ISOCROWD_KEY : XCROWD_KEY );
        double ycrowd = config.get( isIso_ ? ISOCROWD_KEY : YCROWD_KEY );
        double zcrowd = config.get( isIso_ ? ISOCROWD_KEY : ZCROWD_KEY );
        OrientationPolicy orientpolicy = config.get( ORIENTATIONS_KEY );
        boolean forceiso = config.get( FORCEISO_KEY );
        Captioner captioner = StyleKeys.CAPTIONER.createValue( config );
        boolean frame = config.get( FRAME_KEY );
        boolean minor = config.get( StyleKeys.MINOR_TICKS );
        boolean antialias = config.get( StyleKeys.GRID_ANTIALIAS );
        return new Profile( xscale, yscale, zscale,
                            xflip, yflip, zflip,
                            xlabel, ylabel, zlabel,
                            forceiso, captioner, frame, xcrowd, ycrowd, zcrowd,
                            orientpolicy, minor, antialias );
    }

    public ConfigKey<?>[] getAspectKeys() {
        List<ConfigKey<?>> list = new ArrayList<ConfigKey<?>>();
        if ( isIso_ ) {
            list.addAll( Arrays.asList( new ConfigKey<?>[] {
                XC_KEY, YC_KEY, ZC_KEY,
                SCALE_KEY,
            } ) );
        }
        else {
            list.addAll( Arrays.asList( new ConfigKey<?>[] {
                XMIN_KEY, XMAX_KEY, XSUBRANGE_KEY,
                YMIN_KEY, YMAX_KEY, YSUBRANGE_KEY,
                ZMIN_KEY, ZMAX_KEY, ZSUBRANGE_KEY,
            } ) );
        }
        list.addAll( Arrays.asList( new ConfigKey<?>[] {
            PHI_KEY,
            THETA_KEY,
            PSI_KEY,
            ZOOM_KEY,
            XOFF_KEY, YOFF_KEY,
        } ) );
        return list.toArray( new ConfigKey<?>[ 0 ] );
    }

    public boolean useRanges( Profile profile, ConfigMap config ) {
        return getUnrangedXyzLimits( profile, config ) == null;
    }

    public CubeAspect createAspect( Profile profile, ConfigMap config,
                                    Range[] ranges ) {
        double[][] limits = getUnrangedXyzLimits( profile, config );
        if ( limits == null ) {
            if ( ranges == null ) {
                ranges = new Range[] { new Range(), new Range(), new Range() };
            }
            limits = getRangedXyzLimits( profile, config, ranges );
        }
        double[] rotmat = getRotation( config );
        double zoom = config.get( ZOOM_KEY ); 
        double xoff = config.get( XOFF_KEY );
        double yoff = config.get( YOFF_KEY );
        return new CubeAspect( limits[ 0 ], limits[ 1 ], limits[ 2 ],
                               rotmat, zoom, xoff, yoff );
    }

    public ConfigMap getAspectConfig( Surface surface ) {
        return surface instanceof CubeSurface
             ? ((CubeSurface) surface).getAspectConfig( isIso_ )
             : new ConfigMap();
    }

    public Range[] readRanges( Profile profile, PlotLayer[] layers,
                               DataStore dataStore ) {
        Range[] ranges = new Range[] { new Range(), new Range(), new Range() };
        PlotUtil.extendCoordinateRanges( layers, ranges, profile.getScales(),
                                         true, dataStore );
        return ranges;
    }

    public ConfigKey<?>[] getNavigatorKeys() {
        return CubeNavigator.getConfigKeys( isIso_ );
    }

    public Navigator<CubeAspect> createNavigator( ConfigMap navConfig ) {
        return CubeNavigator.createNavigator( isIso_, navConfig );
    }

    /**
     * Returns null.
     */
    public PlotMetric getPlotMetric() {
        return null;
    }

    /**
     * Attempts to determine axis data limits from profile and configuration,
     * but not ranging, information.  If not enough information is supplied,
     * null will be returned.
     *
     * @param  profile  config profile
     * @param  config  config map which may contain additional range info
     * @return  [3][2]-element array giving definite values for all
     *          (X,Y,Z) (min,max) data bounds, or null
     */
    private double[][] getUnrangedXyzLimits( Profile profile,
                                             ConfigMap config ) {
        if ( isIso_ ) {
            double scale = config.get( SCALE_KEY );
            double xc = config.get( XC_KEY );
            double yc = config.get( YC_KEY );
            double zc = config.get( ZC_KEY );
            double s2 = scale * 0.5;
            return ( Double.isNaN( scale ) ||
                     Double.isNaN( xc ) ||
                     Double.isNaN( yc ) ||
                     Double.isNaN( zc ) )
                  ? null
                  : new double[][] {
                        { xc - s2, xc + s2 },
                        { yc - s2, yc + s2 },
                        { zc - s2, zc + s2 },
                    };
        }
        else {
            double[] xlimits =
                PlaneSurfaceFactory
               .getLimits( config, XMIN_KEY, XMAX_KEY, XSUBRANGE_KEY,
                           profile.xscale_, null );
            double[] ylimits =
                PlaneSurfaceFactory
               .getLimits( config, YMIN_KEY, YMAX_KEY, YSUBRANGE_KEY,
                           profile.yscale_, null );
            double[] zlimits =
                PlaneSurfaceFactory
               .getLimits( config, ZMIN_KEY, ZMAX_KEY, ZSUBRANGE_KEY,
                           profile.zscale_, null );
            return xlimits == null || ylimits == null || zlimits == null
                 ? null
                 : new double[][] { xlimits, ylimits, zlimits };
        }
    }

    /**
     * Determines axis data limits from profile, configuration and ranging
     * information.  Config takes precedence over range where both are present.
     *
     * @param  profile  config profile
     * @param  config  config map which may contain additional range info
     * @param  ranges   3-element range array for X, Y, Z data ranges
     * @return  [3][2]-element array giving definite values for all
     *          (X,Y,Z) (min,max) data bounds, or null
     */
    private double[][] getRangedXyzLimits( Profile profile, ConfigMap config,
                                           Range[] ranges ) {
        if ( isIso_ ) {
            double scale = config.get( SCALE_KEY );
            double xc0 = config.get( XC_KEY );
            double yc0 = config.get( YC_KEY );
            double zc0 = config.get( ZC_KEY );
            double[] xlimits = ranges[ 0 ].getFiniteBounds( false );
            double[] ylimits = ranges[ 1 ].getFiniteBounds( false );
            double[] zlimits = ranges[ 2 ].getFiniteBounds( false );
            double xlo = xlimits[ 0 ];
            double xhi = xlimits[ 1 ];
            double ylo = ylimits[ 0 ];
            double yhi = ylimits[ 1 ];
            double zlo = zlimits[ 0 ];
            double zhi = zlimits[ 1 ];
            double ctol = ISO_CENTER_TOLERANCE;
            double xc = Double.isNaN( xc0 ) ? getCenter( xlo, xhi, ctol ) : xc0;
            double yc = Double.isNaN( yc0 ) ? getCenter( ylo, yhi, ctol ) : yc0;
            double zc = Double.isNaN( zc0 ) ? getCenter( zlo, zhi, ctol ) : zc0;
            assert ! Double.isNaN( xc + yc + zc );
            if ( Double.isNaN( scale ) ) {
                scale = 2 * max3( Math.max( xhi - xc, xc - xlo ),
                                  Math.max( yhi - yc, yc - ylo ),
                                  Math.max( zhi - zc, zc - zlo ) );
            }
            assert ! Double.isNaN( scale );
            return new double[][] {
                centerLimits( xlo, xhi, xc, scale ),
                centerLimits( ylo, yhi, yc, scale ),
                centerLimits( zlo, zhi, zc, scale ),
            };
        }
        else {
            return new double[][] {
                PlaneSurfaceFactory
               .getLimits( config, XMIN_KEY, XMAX_KEY, XSUBRANGE_KEY,
                           profile.xscale_, ranges[ 0 ] ),
                PlaneSurfaceFactory
               .getLimits( config, YMIN_KEY, YMAX_KEY, YSUBRANGE_KEY,
                           profile.yscale_, ranges[ 1 ] ),
                PlaneSurfaceFactory
               .getLimits( config, ZMIN_KEY, ZMAX_KEY, ZSUBRANGE_KEY,
                           profile.zscale_, ranges[ 2 ] ),
            };
        }
    }

    /**
     * Determines the axis center from an upper and lower limit.
     * This is generally just the mean of the two supplied values,
     * but if the answer comes out near to zero, zero is returned instead.
     *
     * @param   lo   lower limit
     * @param   hi   upper limit
     * @param   tolerance   proportional proximity to zero of basic center
     *                      which will trigger a zero result
     * @return   central value
     */
    private static double getCenter( double lo, double hi, double tolerance ) {
        double c0 = 0.5 * ( lo + hi );
        return Math.abs( c0 ) / ( hi - lo ) <= tolerance ? 0 : c0;
    }

    /**
     * Returns actual upper and lower data bounds for an axis given
     * suggested range and constraints on size and central position.
     *
     * @param   lo      suggested lower limit
     * @param   hi      suggested upper limit
     * @param   center  suggested central position, or NaN
     * @param   scale   fixed size of output range
     * @return  2-element array giving lower,upper limits;
     *          <code>upper=lower+scale</code>
     */
    private static double[] centerLimits( double lo, double hi, double center,
                                          double scale ) {
        if ( Double.isNaN( center ) ) {
            center = ( lo + hi ) * 0.5;
        }
        double s2 = scale * 0.5;
        return new double[] { center - s2, center + s2 };
    }

    /**
     * 3-argument maximum function.
     *
     * @param  a  value
     * @param  b  value
     * @param  c  value
     * @return  largest of input values, or NaN if any input was NaN
     */
    private static double max3( double a, double b, double c ) {
        return Math.max( a, Math.max( b, c ) );
    }

    /**
     * Reads the intended rotation matrix from a configuration map.
     *
     * @param  config  config map
     * @return  9-element rotation matrix
     */
    public static double[] getRotation( ConfigMap config ) {
        return eulerToRotationDegrees( new double[] {
            config.get( PHI_KEY ),
            config.get( THETA_KEY ),
            config.get( PSI_KEY ),
        } );
    }

    /**
     * Converts three ZXZ Euler angles to a rotation matrix.
     *
     * @param  eulers  1, 2 or 3-element array giving Euler angles
     *                 phi, theta, psi in degrees; if fewer elements than 3,
     *                 later angles are assumed zero
     * @return  9-element rotation matrix
     */
    public static double[] eulerToRotationDegrees( double[] eulers ) {
        int ne = eulers.length;
        double radFact = Math.PI / 180.;
        double phiRad = radFact * ( ne > 0 ? eulers[ 0 ] : 0 );
        double thetaRad = radFact * ( ne > 1 ? eulers[ 1 ] : 0 );
        double psiRad = radFact * ( ne > 2 ? eulers[ 2 ] : 0 );
        double s1 = Math.sin( phiRad );
        double c1 = Math.cos( phiRad );
        double s2 = Math.sin( thetaRad );
        double c2 = Math.cos( thetaRad );
        double s3 = Math.sin( psiRad );
        double c3 = Math.cos( psiRad );
        return new double[] {
            c1*c3-c2*s1*s3,  c3*s1+c1*c2*s3,    s2*s3,
           -c1*s3-c2*c3*s1,  c1*c2*c3-s1*s3,    c3*s2,
            s1*s2,          -c1*s2,                c2,
        };
    }

    /**
     * Converts rotation matrix to three ZXZ Euler angles in degrees.
     * May lose some accuracy near theta=0.
     * Note the result is not unique.
     *
     * @param  rotmat  9-element rotation matrix
     * @return   3-element array giving phi, theta, psi in degrees
     */
    public static double[] rotationToEulerDegrees( double[] rotmat ) {
        double thetaRad = Math.acos( rotmat[ 8 ] );
        final double phiRad;
        final double psiRad;
        if ( Math.abs( thetaRad ) < 0.1 * Math.PI / 180. ) {
            phiRad = Math.acos( rotmat[ 0 ] );
            psiRad = 0;
        }
        else {
            phiRad = Math.atan2( rotmat[ 6 ], - rotmat[ 7 ] );
            psiRad = Math.atan2( rotmat[ 2 ], rotmat[ 5 ] );
        }
        double degFact = 180. / Math.PI;
        return new double[] {
            degFact * phiRad,
            degFact * thetaRad,
            degFact * psiRad,
        };
    }

    /**
     * Returns a config key for setting one of the axis coordinate center
     * values.  Used only in the isotropic case.
     *
     * @param  axName  axis name
     * @return   config key
     */
    private static ConfigKey<Double> createIsoCenterKey( String axName ) {
        ConfigMeta meta =
            new ConfigMeta( "c" + axName.toLowerCase(), axName + " Center" );
        meta.setShortDescription( "Central " + axName + " coordinate" );
        meta.setXmlDescription( new String[] {
            "<p>Gives the central coordinate in the " + axName + " dimension.",
            "This will be determined from the data range if not supplied.",
            "</p>",
        } );
        return DoubleConfigKey.createTextKey( meta );
    }

    /**
     * Profile class which defines fixed configuration items for
     * an isotropic or non-isotropic CubeSurface.
     * Instances of this class are normally obtained from the
     * {@link #createProfile createProfile} method.
     */
    public static class Profile {
        private final Scale xscale_;
        private final Scale yscale_;
        private final Scale zscale_;
        private final boolean xflip_;
        private final boolean yflip_;
        private final boolean zflip_;
        private final String xlabel_;
        private final String ylabel_;
        private final String zlabel_;
        private final boolean forceiso_;
        private final Captioner captioner_;
        private final boolean frame_;
        private final double xcrowd_;
        private final double ycrowd_;
        private final double zcrowd_;
        private final OrientationPolicy orientpolicy_;
        private final boolean minor_;
        private final boolean antialias_;

        /**
         * Constructor.
         *
         * @param  xscale   scaling on X axis
         * @param  yscale   scaling on Y axis
         * @param  zscale   scaling on X axis
         * @param  xflip  whether to invert direction of X axis
         * @param  yflip  whether to invert direction of Y axis
         * @param  zflip  whether to invert direction of Z axis
         * @param  xlabel  text for labelling X axis
         * @param  ylabel  text for labelling Y axis
         * @param  zlabel  text for labelling Z axis
         * @param  forceiso  if true, scaling is forced the same on all axes;
         *                   only useful in non-isotropic mode
         * @param  captioner  text renderer for axis labels etc
         * @param  frame   whether to draw axis wire frame
         * @param  xcrowd  crowding factor for tick marks on X axis;
         *                 1 is normal
         * @param  ycrowd  crowding factor for tick marks on Y axis;
         *                 1 is normal
         * @param  zcrowd  crowding factor for tick marks on Z axis;
         *                 1 is normal
         * @param  orientpolicy  axis label orientation policy
         * @param  minor   whether to paint minor tick marks on axes
         * @param  antialias  whether to antialias grid lines and text
         */
        public Profile( Scale xscale, Scale yscale, Scale zscale,
                        boolean xflip, boolean yflip, boolean zflip,
                        String xlabel, String ylabel, String zlabel,
                        boolean forceiso, Captioner captioner, boolean frame,
                        double xcrowd, double ycrowd, double zcrowd,
                        OrientationPolicy orientpolicy, boolean minor,
                        boolean antialias ) {
            xscale_ = xscale;
            yscale_ = yscale;
            zscale_ = zscale;
            xflip_ = xflip;
            yflip_ = yflip;
            zflip_ = zflip;
            xlabel_ = xlabel;
            ylabel_ = ylabel;
            zlabel_ = zlabel;
            forceiso_ = forceiso;
            captioner_ = captioner;
            frame_ = frame;
            xcrowd_ = xcrowd;
            ycrowd_ = ycrowd;
            zcrowd_ = zcrowd;
            orientpolicy_ = orientpolicy;
            minor_ = minor;
            antialias_ = antialias;
        }

        /**
         * Returns a 3-element array giving X, Y and Z log flags.
         *
         * @return   (xscale, yscale, zscale) array
         */
        public Scale[] getScales() {
            return new Scale[] { xscale_, yscale_, zscale_ };
        }

        /**
         * Indicates whether isometric axis scaling is in force.
         *
         * @return  true if scaling is forced the same on all axes;
         *          only useful in non-isotropic mode
         */
        public boolean isForceIso() {
            return forceiso_;
        }
    }
}
