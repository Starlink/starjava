package uk.ac.starlink.ttools.plot2.geom;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot2.Captioner;
import uk.ac.starlink.ttools.plot2.Navigator;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PointCloud;
import uk.ac.starlink.ttools.plot2.SubCloud;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.SurfaceFactory;
import uk.ac.starlink.ttools.plot2.config.BooleanConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigException;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.DoubleConfigKey;
import uk.ac.starlink.ttools.plot2.config.OptionConfigKey;
import uk.ac.starlink.ttools.plot2.config.SkySysConfigKey;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.SkyCoord;

/**
 * Surface factory for plotting on the surface of the celestial sphere.
 *
 * @author   Mark Taylor
 * @since    20 Feb 2013
 */
public class SkySurfaceFactory
             implements SurfaceFactory<SkySurfaceFactory.Profile,SkyAspect> {

    private static final String LON_NAME = "clon";
    private static final String LAT_NAME = "clat";
    private static final String FOV_RADIUS_NAME = "radius";

    /** Config key for sky projection type. */
    public static final ConfigKey<Projection> PROJECTION_KEY =
        createProjectionKey();
 
    /** Config key to determine whether longitude runs right to left. */
    public static final ConfigKey<Boolean> REFLECT_KEY =
        new BooleanConfigKey(
            new ConfigMeta( "reflectlon", "Reflect longitude axis" )
           .setShortDescription( "Reflect longitude axis?" )
           .setXmlDescription( new String[] {
                "<p>Whether to invert the celestial sphere by displaying",
                "the longitude axis increasing right-to-left",
                "rather than left-to-right.",
                "It is conventional to display the celestial sphere",
                "in this way because that's what it looks like",
                "from the earth, so the default is <code>true</code>.",
                "Set it false to see the sphere from the outside.",
                "</p>",
            } )
            , true );

    /** Config key for the sky system used for projecting the data. */
    public static final ConfigKey<SkySys> VIEWSYS_KEY =
        new SkySysConfigKey(
            new ConfigMeta( "viewsys", "View Sky System" )
           .setShortDescription( "Sky coordinate system for plot display" )
           .setXmlDescription( new String[] {
                "<p>The sky coordinate system used for the generated plot.",
                "</p>",
                "<p>Choice of this value goes along with the data coordinate",
                "system that may be specified for plot layers.",
                "If unspecified, a generic longitude/latitude system is used,",
                "and all lon/lat coordinates in the plotted data layers",
                "are assumed to be in the same system.",
                "If a value is supplied for this parameter,",
                "then a sky system must (implicitly or explicitly)",
                "be supplied for each data layer,",
                "and the coordinates are converted from data to view system",
                "before being plotted.",
                "</p>",
                SkySysConfigKey.getDescribedOptionsXml(),
            } )
            , false ).setOptionUsage();

    /** Config key to determine whether grid lines are drawn. */
    public static final ConfigKey<Boolean> GRID_KEY =
        new BooleanConfigKey(
            new ConfigMeta( "grid", "Draw Grid" )
           .setShortDescription( "Draw sky grid?" )
           .setXmlDescription( new String[] {
                "<p>If true, sky coordinate grid lines are drawn",
                "on the plot.",
                "If false, they are absent.",
                "</p>",
            } )
        , true );

    /** Config key to control tick mark crowding. */
    public static final ConfigKey<Double> CROWD_KEY =
        StyleKeys.createCrowdKey(
            new ConfigMeta( "crowd", "Grid Crowding" )
           .setShortDescription( "Grid line crowding" )
           .setXmlDescription( new String[] {
                "<p>Determines how closely sky grid lines are spaced.",
                "The default value is 1, meaning normal crowding.",
                "Larger values result in more grid lines,",
                "and smaller values in fewer grid lines.",
                "</p>",
            } )
        );

    /** Config key to control axis label positioning. */
    public static final ConfigKey<SkyAxisLabeller> AXISLABELLER_KEY =
        createAxisLabellerKey();
 
    /** Config key to determine whether sexagesimal coordinates are used. */
    public static final ConfigKey<Boolean> SEX_KEY =
        new BooleanConfigKey(
            new ConfigMeta( "sex", "Sexagesimal" )
           .setShortDescription( "Sexagesimal labels?" )
           .setXmlDescription( new String[] {
                "<p>If true, grid line labels are written in",
                "sexagesimal notation, if false in decimal degrees.",
                "</p>",
            } )
        , true );

    /** Config key for specifying aspect central longitude, in degrees. */
    public static final ConfigKey<Double> LON_KEY =
        DoubleConfigKey.createTextKey(
            new ConfigMeta( LON_NAME, "Central Longitude" )
           .setShortDescription( "Longitude of plot centre" )
           .setXmlDescription( new String[] {
                "<p>Longitude of the central position of the plot",
                "in decimal degrees.",
                "Use with <code>" + LAT_NAME + "</code>",
                "and <code>" + FOV_RADIUS_NAME + "</code>.",
                "If the center is not specified,",
                "the field of view is determined from the data.",
                "</p>",
            } )
        );

    /** Config key for specifying aspect central latitude, in degrees. */
    public static final ConfigKey<Double> LAT_KEY =
        DoubleConfigKey.createTextKey(
            new ConfigMeta( LAT_NAME, "Central Latitude" )
           .setShortDescription( "Latitude of plot centre" )
           .setXmlDescription( new String[] {
                "<p>Latitude of the central position of the plot",
                "in decimal degrees.",
                "Use with <code>" + LON_NAME + "</code>",
                "and <code>" + FOV_RADIUS_NAME + "</code>.",
                "If the center is not specified,",
                "the field of view is determined from the data.",
                "</p>",
            } )
        );

    /** Config key for specifying aspect field of view, in degrees. */
    public static final ConfigKey<Double> FOV_RADIUS_KEY =
        DoubleConfigKey.createTextKey(
            new ConfigMeta( FOV_RADIUS_NAME, "Radius" )
           .setShortDescription( "Field of view radius in degrees" )
           .setXmlDescription( new String[] {
                "<p>Approximate radius of the plot field of view in degrees.",
                "Only used if <code>" + LON_NAME + "</code>",
                "and <code>" + LAT_NAME + "</code> are also specified.",
                "</p>",
            } )
        , 1 );

    public Surface createSurface( Rectangle plotBounds, Profile p,
                                  SkyAspect aspect ) {
        return new SkySurface( plotBounds, aspect.getProjection(),
                               aspect.getRotation(), aspect.getZoom(),
                               aspect.getOffsetX(), aspect.getOffsetY(),
                               p.viewSystem_, p.axisLabeller_,
                               p.grid_ ? p.gridColor_ : null, p.axlabelColor_,
                               p.sex_, p.crowd_, p.captioner_, p.antialias_ );
    }

    public ConfigKey[] getProfileKeys() {
        List<ConfigKey> list = new ArrayList<ConfigKey>();
        list.addAll( Arrays.asList( new ConfigKey[] {
            PROJECTION_KEY,
            VIEWSYS_KEY,
            REFLECT_KEY,
            GRID_KEY,
            AXISLABELLER_KEY,
            SEX_KEY,
            CROWD_KEY,
            StyleKeys.GRID_COLOR,
            StyleKeys.AXLABEL_COLOR,
            StyleKeys.GRID_ANTIALIAS,
        } ) );
        list.addAll( Arrays.asList( StyleKeys.CAPTIONER.getKeys() ) );
        return list.toArray( new ConfigKey[ 0 ] );
    }

    public Profile createProfile( ConfigMap config ) throws ConfigException {
        Projection proj = config.get( PROJECTION_KEY );
        boolean reflect = config.get( REFLECT_KEY );
        SkySys viewSystem = config.get( VIEWSYS_KEY );
        boolean grid = config.get( GRID_KEY );   
        SkyAxisLabeller axLabeller = config.get( AXISLABELLER_KEY );
        boolean sex = config.get( SEX_KEY );
        double crowd = config.get( CROWD_KEY );
        Color gridColor = config.get( StyleKeys.GRID_COLOR );
        Color axlabelColor = config.get( StyleKeys.AXLABEL_COLOR );
        boolean antialias = config.get( StyleKeys.GRID_ANTIALIAS );
        Captioner captioner = StyleKeys.CAPTIONER.createValue( config );
        return new Profile( proj, reflect, viewSystem, grid, axLabeller,
                            gridColor, axlabelColor, sex, crowd, captioner,
                            antialias );
    }

    public ConfigKey[] getAspectKeys() {
        return new ConfigKey[] {
            LON_KEY, LAT_KEY, FOV_RADIUS_KEY,
        };
    }

    public boolean useRanges( Profile profile, ConfigMap config ) {
        Projection proj = profile.getProjection();
        boolean reflect = profile.isReflected();
        double lonDeg = config.get( LON_KEY );
        double latDeg = config.get( LAT_KEY );
        double radiusDeg = config.get( FOV_RADIUS_KEY );
        double[] r3 = SkyCoord.lonLatDegreesToDouble3( lonDeg, latDeg );
        return proj.useRanges( reflect, r3, Math.toRadians( radiusDeg ) );
    }

    public SkyAspect createAspect( Profile profile, ConfigMap config,
                                   Range[] ranges ) {
        Projection proj = profile.getProjection();
        boolean reflect = profile.isReflected();
        double lonDeg = config.get( LON_KEY );
        double latDeg = config.get( LAT_KEY );
        double radiusDeg = config.get( FOV_RADIUS_KEY );
        double[] r3 = SkyCoord.lonLatDegreesToDouble3( lonDeg, latDeg );
        return proj.createAspect( reflect, r3, Math.toRadians( radiusDeg ),
                                  ranges );
    }

    public Range[] readRanges( Profile profile, PlotLayer[] layers,
                               DataStore dataStore ) {
        PointCloud pointCloud =
            new PointCloud( SubCloud.createSubClouds( layers, true ) );
        Range[] ranges = new Range[] { new Range(), new Range(), new Range() };
        long ip = 0;
        for ( double[] dpos : pointCloud.createDataPosIterable( dataStore ) ) {
            for ( int idim = 0; idim < 3; idim++ ) {
                ranges[ idim ].submit( dpos[ idim ] );
            }

            /* Periodically check if the whole sky is covered.
             * If so, don't bother carrying on. */
            if ( ++ip % 10000 == 0 && isAllSky( ranges ) ) {
                return ranges;
            }
        }
        return ranges;
    }

    public ConfigKey[] getNavigatorKeys() {
        return SkyNavigator.getConfigKeys();
    }

    public Navigator<SkyAspect> createNavigator( ConfigMap navConfig ) {
        return SkyNavigator.createNavigator( navConfig );
    }

    /**
     * Determines whether a set of ranges of normalised data coordinates
     * cover enough of the full data cube (-1..+1 in each dimension)
     * to count as full sky coverage.  It's a bit arbitrary what this
     * means, but in case of full sky the view should not be centred on
     * any particular position.
     *
     * @param  vxyzRanges  3-element array giving data ranges for normalised
     *                     X,Y,Z coordinates
     * @return  true if they cover most of the sky
     */
    public static boolean isAllSky( Range[] vxyzRanges ) {
        for ( int id = 0; id < 3; id++ ) {
            double[] bounds = vxyzRanges[ id ].getBounds();
            if ( bounds[ 1 ] - bounds[ 0 ] < 0.9 ) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a config key for selecting sky projection.
     *
     * @return  Projection config key
     */
    private static ConfigKey<Projection> createProjectionKey() {
        ConfigMeta meta = new ConfigMeta( "projection", "Projection" );
        Projection[] projs = SkyAspect.getProjections();
        meta.setShortDescription( "Sky coordinate projection" );
        StringBuffer sbuf = new StringBuffer();
        for ( Projection proj : projs ) {
            sbuf.append( "<li>" )
                .append( "<code>" )
                .append( proj.getProjectionName() )
                .append( "</code>" )
                .append( ": " )
                .append( proj.getProjectionDescription() )
                .append( "</li>\n" );
        }
        meta.setXmlDescription( new String[] {
            "<p>Sky projection used to display the plot.",
            "The options are:",
            "<ul>",
            sbuf.toString(),
            "</ul>",
            "</p>",
        } );
        OptionConfigKey<Projection> key =
                new OptionConfigKey<Projection>( meta, Projection.class,
                                                 projs ) {
            public String valueToString( Projection proj ) {
                return proj.getProjectionName().toLowerCase();
            }
        };
        key.setOptionUsage();
        return key;
    }

    /**
     * Returns a config key for selecting sky grid line labeller policy.
     *
     * @return   SkyAxisLabeller config key
     */
    private static ConfigKey<SkyAxisLabeller> createAxisLabellerKey() {
        final String auto = "Auto";
        SkyAxisLabeller[] labellers = SkyAxisLabellers.getKnownLabellers();
        ConfigMeta meta =
            new ConfigMeta( "labelpos", "Grid Label Positioning" );
        meta.setShortDescription( "Position of sky grid labels" );
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( "<li><code>" )
            .append( auto )
            .append( "</code>: " )
            .append( "Uses " )
            .append( "<code>" )
            .append( SkyAxisLabellers.EXTERNAL.getLabellerName() )
            .append( "</code>" )
            .append( " or " )
            .append( "<code>" )
            .append( SkyAxisLabellers.INTERNAL.getLabellerName() )
            .append( "</code>" )
            .append( " policy according to whether " )
            .append( "the sky fills the plot bounds or not" )
            .append( "</li>\n" );
        for ( SkyAxisLabeller labeller : labellers ) {
            if ( labeller != null ) {
                sbuf.append( "<li>" )
                    .append( "<code>" )
                    .append( labeller.getLabellerName() )
                    .append( "</code>" )
                    .append( ": " )
                    .append( labeller.getLabellerDescription() )
                    .append( "</li>\n" );
            }
        }
        meta.setXmlDescription( new String[] {
            "<p>Controls whether and where the numeric annotations",
            "of the lon/lat axes are displayed.",
            "The default option <code>" + auto + "</code>",
            "usually does the sensible thing,",
            "but other options exist to force labelling internally",
            "or externally to the plot region,",
            "or to remove numeric labels altogether.",
            "</p>",
            "<p>Available options are:",
            "<ul>",
            sbuf.toString(),
            "</ul>",
            "</p>",
        } );
        OptionConfigKey<SkyAxisLabeller> key =
                new OptionConfigKey<SkyAxisLabeller>( meta,
                                                      SkyAxisLabeller.class,
                                                      labellers ) {
            @Override
            public String valueToString( SkyAxisLabeller labeller ) {
                return labeller == null ? auto : labeller.getLabellerName();
            }
        };
        key.setOptionUsage();
        return key;
    }

    /**
     * Profile class which defines fixed configuration items for a SkySurface.
     * Instances of this class are normally obtained from the
     * {@link #createProfile createProfile} method.
     */
    public static class Profile {
        private final Projection projection_;
        private final boolean reflect_;
        private final SkySys viewSystem_;
        private final boolean grid_;
        private final SkyAxisLabeller axisLabeller_;
        private final Color gridColor_;
        private final Color axlabelColor_;
        private final boolean sex_;
        private final double crowd_;
        private final Captioner captioner_;
        private final boolean antialias_;

        /**
         * Constructor.
         *
         * @param  projection  sky projection
         * @param  reflect  whether to run lon axis right to left
         * @param  viewSystem  sky system into which coordinates are projected
         * @param  grid   whether to draw coordinate grid
         * @param  axisLabeller  sky axis labelling object
         * @param  gridColor   colour of grid lines
         * @param  axlabelColor  colour of axis labels
         * @param  sex  whether to use sexagesimal coordinates
         * @param  crowd   tick mark crowding factor, 1 is normal
         * @param  captioner  text rendering object
         * @param  antialias  whether to antialias grid lines
         */
        public Profile( Projection projection, boolean reflect,
                        SkySys viewSystem, boolean grid, 
                        SkyAxisLabeller axisLabeller, Color gridColor,
                        Color axlabelColor, boolean sex,
                        double crowd, Captioner captioner, boolean antialias ) {
            projection_ = projection;
            reflect_ = reflect;
            viewSystem_ = viewSystem;
            grid_ = grid;
            axisLabeller_ = axisLabeller;
            gridColor_ = gridColor;
            axlabelColor_ = axlabelColor;
            sex_ = sex;
            crowd_ = crowd;
            captioner_ = captioner;
            antialias_ = antialias;
        }

        /**
         * Returns the sky projection used by this profile.
         *
         * @return  projection
         */
        public Projection getProjection() {
            return projection_;
        }

        /**
         * Indicates whether longitude runs right to left in this profile.
         *
         * @return   true for longitude right to left, false for left to right
         */
        public boolean isReflected() {
            return reflect_;
        }

        /**
         * Returns the sky system into which coordinates are projected.
         *
         * @return  sky view system
         */
        public SkySys getViewSystem() {
            return viewSystem_;
        }
    }
}
