package uk.ac.starlink.ttools.plot2.geom;

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

    /** Config key for sky projection type. */
    public static final ConfigKey<Projection> PROJECTION_KEY =
            new OptionConfigKey<Projection>( new ConfigMeta( "projection",
                                                             "Projection" ),
                                             Projection.class,
                                             SkyAspect.getProjections() ) {
        public String valueToString( Projection proj ) {
            return proj.getName();
        }
    };
 
    /** Config key to determine whether longitude runs right to left. */
    public static final ConfigKey<Boolean> REFLECT_KEY =
        new BooleanConfigKey( new ConfigMeta( "reflectlon",
                                              "Reflect longitude axis" ),
                              true );

    /** Config key for the sky system used for projecting the data. */
    public static final ConfigKey<SkySys> VIEWSYS_KEY =
        SkySys.createConfigKey( new ConfigMeta( "viewsys", "View Sky System" ),
                                false );

    /** Config key to determine whether grid lines are drawn. */
    public static final ConfigKey<Boolean> GRID_KEY =
        new BooleanConfigKey( new ConfigMeta( "grid", "Draw Grid" ), true );

    /** Config key to control tick mark crowding. */
    public static final ConfigKey<Double> CROWD_KEY =
        StyleKeys.createCrowdKey( new ConfigMeta( "crowd", "Grid Crowding" ) );

    /** Config key to control axis label positioning. */
    public static final ConfigKey<SkyAxisLabeller> AXISLABELLER_KEY =
            new OptionConfigKey<SkyAxisLabeller>(
                    new ConfigMeta( "labelpos", "Label Positioning" ),
                    SkyAxisLabeller.class,
                    SkyAxisLabellers.getKnownLabellers() ) {
        public String valueToString( SkyAxisLabeller labeller ) {
            return labeller == null ? "Auto" : labeller.getName();
        }
    };
 
    /** Config key to determine whether sexagesimal coordinate s are used. */
    public static final ConfigKey<Boolean> SEX_KEY =
        new BooleanConfigKey( new ConfigMeta( "sex", "Sexagesimal" ), true );

    /** Config key for specifying aspect central longitude, in degrees. */
    public static final ConfigKey<Double> LON_KEY =
        DoubleConfigKey
       .createTextKey( new ConfigMeta( "clon", "Central Longitude" ) );

    /** Config key for specifying aspect central latitude, in degrees. */
    public static final ConfigKey<Double> LAT_KEY =
        DoubleConfigKey
       .createTextKey( new ConfigMeta( "clat", "Central Latitude" ) );

    /** Config key for specifying aspect field of view, in degrees. */
    public static final ConfigKey<Double> FOV_RADIUS_KEY =
        DoubleConfigKey
       .createTextKey( new ConfigMeta( "radius", "Radius" ), 1 );

    public Surface createSurface( Rectangle plotBounds, Profile p,
                                  SkyAspect aspect ) {
        return new SkySurface( plotBounds, aspect.getProjection(),
                               aspect.getRotation(), aspect.getZoom(),
                               aspect.getOffsetX(), aspect.getOffsetY(),
                               p.viewSystem_, p.grid_,
                               p.axisLabeller_, p.sex_, p.crowd_,
                               p.captioner_ );
    }

    public ConfigKey[] getProfileKeys() {
        List<ConfigKey> list = new ArrayList<ConfigKey>();
        list.addAll( Arrays.asList( new ConfigKey[] {
            PROJECTION_KEY,
            REFLECT_KEY,
            GRID_KEY,
            AXISLABELLER_KEY,
            SEX_KEY,
            CROWD_KEY,
        } ) );
        list.addAll( Arrays.asList( StyleKeys.getCaptionerKeys() ) );
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
        Captioner captioner = StyleKeys.createCaptioner( config );
        return new Profile( proj, reflect, viewSystem, grid, axLabeller,
                            sex, crowd, captioner );
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

    public Range[] readRanges( PlotLayer[] layers, DataStore dataStore ) {
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
        private final boolean sex_;
        private final double crowd_;
        private final Captioner captioner_;

        /**
         * Constructor.
         *
         * @param  projection  sky projection
         * @param  reflect  whether to run lon axis right to left
         * @param  viewSystem  sky system into which coordinates are projected
         * @param  grid   whether to draw coordinate grid
         * @param  axisLabeller  sky axis labelling object
         * @param  sex  whether to use sexagesimal coordinates
         * @param  crowd   tick mark crowding factor, 1 is normal
         * @param  captioner  text rendering object
         */
        public Profile( Projection projection, boolean reflect,
                        SkySys viewSystem, boolean grid, 
                        SkyAxisLabeller axisLabeller, boolean sex,
                        double crowd, Captioner captioner ) {
            projection_ = projection;
            reflect_ = reflect;
            viewSystem_ = viewSystem;
            grid_ = grid;
            axisLabeller_ = axisLabeller;
            sex_ = sex;
            crowd_ = crowd;
            captioner_ = captioner;
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
