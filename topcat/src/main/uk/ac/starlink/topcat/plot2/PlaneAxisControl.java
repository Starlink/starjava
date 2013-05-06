package uk.ac.starlink.topcat.plot2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot2.SurfaceFactory;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.geom.PlaneAspect;
import uk.ac.starlink.ttools.plot2.geom.PlaneSurfaceFactory;

/**
 * Axis control for 2d Cartesian plot.
 * 
 * @author   Mark Taylor
 * @since    14 Mar 2013
 */
public class PlaneAxisControl
        extends CartesianAxisControl<PlaneSurfaceFactory.Profile,PlaneAspect> {

    /**
     * Constructor.
     *
     * @param  stack   control stack
     */
    public PlaneAxisControl( ControlStack stack ) {
        super( new PlaneSurfaceFactory(), createAxisLabelKeys(), stack );
        SurfaceFactory surfFact = getSurfaceFactory();
   
        /* Log/flip tab. */
        addSpecifierTab( "Coords", new ConfigSpecifier( new ConfigKey[] {
            PlaneSurfaceFactory.XLOG_KEY,
            PlaneSurfaceFactory.YLOG_KEY,
            PlaneSurfaceFactory.XFLIP_KEY,
            PlaneSurfaceFactory.YFLIP_KEY,
            PlaneSurfaceFactory.XYFACTOR_KEY,
        } ) );

        /* Range tab. */
        addAspectConfigTab( "Range",
                            new ConfigSpecifier( surfFact.getAspectKeys() ) );

        /* Grid tab. */
        addSpecifierTab( "Grid", new ConfigSpecifier( new ConfigKey[] {
            PlaneSurfaceFactory.GRID_KEY,
            StyleKeys.MINOR_TICKS,
            PlaneSurfaceFactory.XCROWD_KEY,
            PlaneSurfaceFactory.YCROWD_KEY,
        } ) );

        /* Labels tab. */
        addLabelsTab();

        /* Font tab. */
        addSpecifierTab( "Font",
                         new ConfigSpecifier( StyleKeys.getCaptionerKeys() ) );

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
    private static ConfigKey<String>[] createAxisLabelKeys() {
        List<ConfigKey<String>> list = new ArrayList<ConfigKey<String>>();
        list.add( PlaneSurfaceFactory.XLABEL_KEY );
        list.add( PlaneSurfaceFactory.YLABEL_KEY );
        @SuppressWarnings("unchecked")
        ConfigKey<String>[] keys = list.toArray( new ConfigKey[ 0 ] );
        return keys;
    }
}
