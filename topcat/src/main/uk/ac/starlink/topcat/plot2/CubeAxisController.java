package uk.ac.starlink.topcat.plot2;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.ac.starlink.ttools.plot2.Scale;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.SurfaceFactory;
import uk.ac.starlink.ttools.plot2.config.ConfigException;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.geom.CubeAspect;
import uk.ac.starlink.ttools.plot2.geom.CubeSurface;
import uk.ac.starlink.ttools.plot2.geom.CubeSurfaceFactory;

/**
 * Axis control for cube plot.
 * This operates in two modes, one isotropic (with geometry specified
 * using spherical polar coordinates) and one at least potentially 
 * anisotropic (with geometry specified using Cartesian coordinates).
 * Which to use is specified at construction time.
 * 
 * @author   Mark Taylor
 * @since    14 Mar 2013
 */
public class CubeAxisController
       extends CartesianAxisController<CubeSurfaceFactory.Profile,CubeAspect> {

    private final boolean isIso_;
    private CubeSurface oldSurface_;

    /**
     * Constructor.
     *
     * @param  isIso   true for isotropic, false for anisotropic
     */
    @SuppressWarnings("this-escape")
    public CubeAxisController( boolean isIso ) {
        super( new CubeSurfaceFactory( isIso ), createAxisLabelKeys() );
        isIso_ = isIso;
        final SurfaceFactory<CubeSurfaceFactory.Profile,CubeAspect> surfFact =
            getSurfaceFactory();
        ConfigControl mainControl = getMainControl();

        /* Log/flip config tab - only makes sense for anisotropic mode. */
        if ( ! isIso ) {
            mainControl.addSpecifierTab( "Coords",
                                     new ConfigSpecifier( new ConfigKey<?>[] {
                CubeSurfaceFactory.FORCEISO_KEY,
                CubeSurfaceFactory.XSCALE_KEY,
                CubeSurfaceFactory.YSCALE_KEY,
                CubeSurfaceFactory.ZSCALE_KEY,
                CubeSurfaceFactory.XFLIP_KEY,
                CubeSurfaceFactory.YFLIP_KEY,
                CubeSurfaceFactory.ZFLIP_KEY,
            } ) );
        }

        /* Navigator tab. */
        addNavigatorTab();

        /* Provide the aspect configuration in two separate panels.
         * Either can reset the whole aspect, but each takes part of the
         * state from the existing aspect so that adjusting the controls
         * on one panel does not pull in the current values set on the other,
         * which might not reflect the current visible state. */
        final ConfigKey<?>[] rangeKeys = 
            isIso ? new ConfigKey<?>[] {
                        CubeSurfaceFactory.SCALE_KEY,
                        CubeSurfaceFactory.XC_KEY,
                        CubeSurfaceFactory.YC_KEY,
                        CubeSurfaceFactory.ZC_KEY,
                    }
                  : new ConfigKey<?>[] {
                        CubeSurfaceFactory.XMIN_KEY,
                        CubeSurfaceFactory.XMAX_KEY,
                        CubeSurfaceFactory.XSUBRANGE_KEY,
                        CubeSurfaceFactory.YMIN_KEY,
                        CubeSurfaceFactory.YMAX_KEY,
                        CubeSurfaceFactory.YSUBRANGE_KEY,
                        CubeSurfaceFactory.ZMIN_KEY,
                        CubeSurfaceFactory.ZMAX_KEY,
                        CubeSurfaceFactory.ZSUBRANGE_KEY,
                    };
        final ConfigKey<?>[] viewKeys = new ConfigKey<?>[] {
            CubeSurfaceFactory.ZOOM_KEY,
            CubeSurfaceFactory.XOFF_KEY,
            CubeSurfaceFactory.YOFF_KEY,
        };
        ConfigSpecifier rangeSpecifier = new ConfigSpecifier( rangeKeys ) {
            @Override
            protected void checkConfig( ConfigMap config )
                    throws ConfigException {
                if ( ! isIso_ ) {
                    checkRangeSense( config, "X",
                                     CubeSurfaceFactory.XMIN_KEY,
                                     CubeSurfaceFactory.XMAX_KEY );
                    checkRangeSense( config, "Y",
                                     CubeSurfaceFactory.YMIN_KEY,
                                     CubeSurfaceFactory.YMAX_KEY );
                    checkRangeSense( config, "Z",
                                     CubeSurfaceFactory.ZMIN_KEY,
                                     CubeSurfaceFactory.ZMAX_KEY );
                }
            }
        };
        addAspectConfigTab( "Range", rangeSpecifier );
        ConfigSpecifier viewSpecifier = new ConfigSpecifier( viewKeys ) {
            @Override
            public ConfigMap getSpecifiedValue() {
                ConfigMap config = new ConfigMap();
                CubeSurface surf = oldSurface_;
                if ( surf != null ) {
                    config.putAll( surfFact.getAspectConfig( surf ) );
                    config.keySet().removeAll( Arrays.asList( rangeKeys ) );
                }
                config.putAll( super.getSpecifiedValue() );
                return config;
            }
        };
        addAspectConfigTab( "View", viewSpecifier );

        /* Grid config tab. */
        List<ConfigKey<?>> gridKeyList = new ArrayList<ConfigKey<?>>();
        gridKeyList.add( CubeSurfaceFactory.FRAME_KEY );
        gridKeyList.add( StyleKeys.MINOR_TICKS );
        if ( isIso ) {
            gridKeyList.add( CubeSurfaceFactory.ISOCROWD_KEY );
        }
        else {
            gridKeyList.addAll( Arrays.asList( new ConfigKey<?>[] {
                CubeSurfaceFactory.XCROWD_KEY,
                CubeSurfaceFactory.YCROWD_KEY,
                CubeSurfaceFactory.ZCROWD_KEY,
            } ) );
        }
        gridKeyList.addAll( Arrays.asList( new ConfigKey<?>[] {
            CubeSurfaceFactory.ORIENTATIONS_KEY,
            StyleKeys.GRID_ANTIALIAS,
        } ) );
        mainControl.addSpecifierTab( "Grid",
                     new ConfigSpecifier( gridKeyList
                                         .toArray( new ConfigKey<?>[ 0 ] ) ) );

        /* Labels config tab. */
        if ( ! isIso ) {
            addLabelsTab();
        }

        /* Font config tab. */
        mainControl.addSpecifierTab( "Font",
                                     new ConfigSpecifier( StyleKeys.CAPTIONER
                                                         .getKeys() ) );

        /* Check we have the keys specified by the surface factory,
         * but exclude redundant/deprecated ones used for CLI
         * backward compatibility. */
        List<ConfigKey<?>> reqKeys =
            new ArrayList<ConfigKey<?>>( Arrays.asList( surfFact
                                                       .getProfileKeys() ) );
        reqKeys.remove( CubeSurfaceFactory.XLOG_KEY );
        reqKeys.remove( CubeSurfaceFactory.YLOG_KEY );
        reqKeys.remove( CubeSurfaceFactory.ZLOG_KEY );
        assert assertHasKeys( reqKeys.toArray( new ConfigKey<?>[ 0 ] ) );
    }

    @Override
    public void setLatestSurface( Surface surface ) {
        super.setLatestSurface( surface );
        oldSurface_ = surface instanceof CubeSurface
                    ? (CubeSurface) surface
                    : null;
    }

    @Override
    public ConfigMap getConfig() {
        ConfigMap config = super.getConfig();
        if ( isIso_ ) {
            config.put( CubeSurfaceFactory.XSCALE_KEY, Scale.LINEAR );
            config.put( CubeSurfaceFactory.YSCALE_KEY, Scale.LINEAR );
            config.put( CubeSurfaceFactory.ZSCALE_KEY, Scale.LINEAR );
            config.put( CubeSurfaceFactory.XFLIP_KEY, false );
            config.put( CubeSurfaceFactory.YFLIP_KEY, false );
            config.put( CubeSurfaceFactory.ZFLIP_KEY, false );
        }
        return config;
    }

    @Override
    protected boolean logChanged( CubeSurfaceFactory.Profile prof1,
                                  CubeSurfaceFactory.Profile prof2 ) {
        return logChanged( prof1.getScales(), prof2.getScales() );
    }

    @Override
    protected boolean forceClearRange( CubeSurfaceFactory.Profile prof1,
                                       CubeSurfaceFactory.Profile prof2 ) {
        return ( ( prof1.isForceIso() ^ prof2.isForceIso() ) &&
                 ! logChanged( prof1, prof2 ) &&
                 CubeSurface.isIsometricPossible( prof1.getScales() ) )
            || super.forceClearRange( prof1, prof2 );
    }

    private static ConfigKey<String>[] createAxisLabelKeys() {
        List<ConfigKey<String>> list = new ArrayList<ConfigKey<String>>();
        list.add( CubeSurfaceFactory.XLABEL_KEY );
        list.add( CubeSurfaceFactory.YLABEL_KEY );
        list.add( CubeSurfaceFactory.ZLABEL_KEY );
        @SuppressWarnings("unchecked")
        ConfigKey<String>[] keys =
            (ConfigKey<String>[]) list.toArray( new ConfigKey<?>[ 0 ] );
        return keys;
    }
}
