package uk.ac.starlink.topcat.plot2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot2.SurfaceFactory;
import uk.ac.starlink.ttools.plot2.config.ConfigException;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.geom.PlaneAspect;
import uk.ac.starlink.ttools.plot2.geom.PlaneSurfaceFactory;

/**
 * Axis control for 2d Cartesian plot.
 * 
 * @author   Mark Taylor
 * @since    14 Mar 2013
 */
public class PlaneAxisController
        extends CartesianAxisController<PlaneSurfaceFactory.Profile,
                                        PlaneAspect> {

    /**
     * Constructor.
     */
    public PlaneAxisController() {
        super( new PlaneSurfaceFactory( false ), createAxisLabelKeys() );
        SurfaceFactory<PlaneSurfaceFactory.Profile,PlaneAspect> surfFact =
            getSurfaceFactory();
        ConfigControl mainControl = getMainControl();
   
        /* Log/flip tab. */
        mainControl.addSpecifierTab( "Coords",
                                     new ConfigSpecifier( new ConfigKey<?>[] {
            PlaneSurfaceFactory.XLOG_KEY,
            PlaneSurfaceFactory.YLOG_KEY,
            PlaneSurfaceFactory.XFLIP_KEY,
            PlaneSurfaceFactory.YFLIP_KEY,
            PlaneSurfaceFactory.XYFACTOR_KEY,
        } ) );

        /* Navigator tab. */
        addNavigatorTab();

        /* Range tab. */
        addAspectConfigTab( "Range",
                            new ConfigSpecifier( surfFact.getAspectKeys() ) {
            @Override
            protected void checkConfig( ConfigMap config )
                    throws ConfigException {
                checkRangeSense( config, "X",
                                 PlaneSurfaceFactory.XMIN_KEY,
                                 PlaneSurfaceFactory.XMAX_KEY );
                checkRangeSense( config, "Y",
                                 PlaneSurfaceFactory.YMIN_KEY,
                                 PlaneSurfaceFactory.YMAX_KEY );
            }
        } );

        /* Grid tab. */
        List<ConfigKey<?>> gridKeyList = new ArrayList<>();
        gridKeyList.add( PlaneSurfaceFactory.GRID_KEY );
        gridKeyList.addAll( Arrays
                           .asList( StyleKeys.GRIDCOLOR_KEYSET.getKeys() ) );
        gridKeyList.addAll( Arrays.asList( new ConfigKey<?>[] {
            StyleKeys.AXLABEL_COLOR,
            StyleKeys.MINOR_TICKS,
            StyleKeys.SHADOW_TICKS,
            PlaneSurfaceFactory.XCROWD_KEY,
            PlaneSurfaceFactory.YCROWD_KEY,
        } ) );
        ConfigKey<?>[] gridKeys = gridKeyList.toArray( new ConfigKey<?>[ 0 ] );
        mainControl.addSpecifierTab( "Grid", new ConfigSpecifier( gridKeys ) );

        /* Labels tab. */
        addLabelsTab();

        /* Secondary axes tab. */
        mainControl.addSpecifierTab( "Secondary",
                                     new ConfigSpecifier( new ConfigKey<?>[] {
            PlaneSurfaceFactory.X2FUNC_KEY,
            PlaneSurfaceFactory.X2LABEL_KEY,
            PlaneSurfaceFactory.Y2FUNC_KEY,
            PlaneSurfaceFactory.Y2LABEL_KEY,
        } ) );

        /* Font tab. */
        mainControl.addSpecifierTab( "Font",
                                     new ConfigSpecifier( StyleKeys.CAPTIONER
                                                         .getKeys() ) );

        assert assertHasKeys( surfFact.getProfileKeys() );
    }

    @Override
    protected boolean logChanged( PlaneSurfaceFactory.Profile prof1,
                                  PlaneSurfaceFactory.Profile prof2 ) {
        return ! Arrays.equals( prof1.getLogFlags(), prof2.getLogFlags() );
    }

    /**
     * Returns the config keys for axis labelling.
     *
     * @return  X, Y axis label config keys
     */
    static ConfigKey<String>[] createAxisLabelKeys() {
        List<ConfigKey<String>> list = new ArrayList<ConfigKey<String>>();
        list.add( PlaneSurfaceFactory.XLABEL_KEY );
        list.add( PlaneSurfaceFactory.YLABEL_KEY );
        @SuppressWarnings("unchecked")
        ConfigKey<String>[] keys =
            (ConfigKey<String>[]) list.toArray( new ConfigKey<?>[ 0 ] );
        return keys;
    }
}
