package uk.ac.starlink.topcat.plot2;

import java.util.ArrayList;
import java.util.List;
import uk.ac.starlink.ttools.plot2.SurfaceFactory;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.geom.TimeAspect;
import uk.ac.starlink.ttools.plot2.geom.TimeSurfaceFactory;

/**
 * Axis control for plot with a horizontal time axis.
 *
 * @author   Mark Taylor
 * @since    24 Jul 2013
 */
public class TimeAxisControl
        extends CartesianAxisControl<TimeSurfaceFactory.Profile,TimeAspect> {

    /**
     * Constructor.
     *
     * @param  stack  control stack
     */
    public TimeAxisControl( ControlStack stack ) {
        super( new TimeSurfaceFactory(), createAxisLabelKeys(), stack );
        SurfaceFactory surfFact = getSurfaceFactory();

        /* Log/flip tab. */
        addSpecifierTab( "Coords", new ConfigSpecifier( new ConfigKey[] {
            TimeSurfaceFactory.YLOG_KEY,
            TimeSurfaceFactory.YFLIP_KEY,
        } ) );

        /* Navigator tab. */
        addNavigatorTab();

        /* Range tab. */
        addAspectConfigTab( "Range",
                            new ConfigSpecifier( surfFact.getAspectKeys() ) );

        /* Grid tab. */
        addSpecifierTab( "Grid", new ConfigSpecifier( new ConfigKey[] {
            TimeSurfaceFactory.TFORMAT_KEY,
            TimeSurfaceFactory.GRID_KEY,
            StyleKeys.MINOR_TICKS,
            TimeSurfaceFactory.TCROWD_KEY,
            TimeSurfaceFactory.YCROWD_KEY,
        } ) );

        /* Labels tab. */
        addLabelsTab();

        /* Font tab. */
        addSpecifierTab( "Font",
                         new ConfigSpecifier( StyleKeys.getCaptionerKeys() ) );

        assert assertHasKeys( surfFact.getProfileKeys() );
    }

    @Override
    protected boolean logChanged( TimeSurfaceFactory.Profile prof1,
                                  TimeSurfaceFactory.Profile prof2 ) {
        return prof1.getYLog() != prof2.getYLog();
    }

    /**
     * Returns the config keys for axis labelling.
     *
     * @return  T, Y axis label config keys
     */
    private static ConfigKey<String>[] createAxisLabelKeys() {
        List<ConfigKey<String>> list = new ArrayList<ConfigKey<String>>();
        list.add( TimeSurfaceFactory.TLABEL_KEY );
        list.add( TimeSurfaceFactory.YLABEL_KEY );
        @SuppressWarnings("unchecked")
        ConfigKey<String>[] keys = list.toArray( new ConfigKey[ 0 ] );
        return keys;
    }
}
