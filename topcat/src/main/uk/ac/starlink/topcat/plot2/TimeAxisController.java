package uk.ac.starlink.topcat.plot2;

import java.util.ArrayList;
import java.util.List;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.SurfaceFactory;
import uk.ac.starlink.ttools.plot2.config.ConfigException;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.geom.TimeAspect;
import uk.ac.starlink.ttools.plot2.geom.TimeSurfaceFactory;

/**
 * Axis control for plot with a horizontal time axis.
 *
 * @author   Mark Taylor
 * @since    24 Jul 2013
 */
public class TimeAxisController
        extends CartesianAxisController<TimeSurfaceFactory.Profile,TimeAspect> {

    /**
     * Constructor.
     */
    public TimeAxisController() {
        super( new TimeSurfaceFactory(), createAxisLabelKeys() );
        SurfaceFactory<TimeSurfaceFactory.Profile,TimeAspect> surfFact =
            getSurfaceFactory();
        ConfigControl mainControl = getMainControl();

        /* Log/flip tab. */
        mainControl.addSpecifierTab( "Coords",
                                     new ConfigSpecifier( new ConfigKey<?>[] {
            TimeSurfaceFactory.YLOG_KEY,
            TimeSurfaceFactory.YFLIP_KEY,
        } ) );

        /* Navigator tab. */
        addNavigatorTab();

        /* Range tab. */
        addAspectConfigTab( "Range",
                            new ConfigSpecifier( surfFact.getAspectKeys() ) {
            @Override
            protected void checkConfig( ConfigMap config )
                    throws ConfigException {
                checkRangeSense( config, "Time",
                                 TimeSurfaceFactory.TMIN_KEY,
                                 TimeSurfaceFactory.TMAX_KEY );
                checkRangeSense( config, "Y",
                                 TimeSurfaceFactory.YMIN_KEY,
                                 TimeSurfaceFactory.YMAX_KEY );
            }
        } );

        /* Grid tab. */
        mainControl.addSpecifierTab( "Grid",
                                     new ConfigSpecifier( new ConfigKey<?>[] {
            TimeSurfaceFactory.TFORMAT_KEY,
            TimeSurfaceFactory.GRID_KEY,
            StyleKeys.MINOR_TICKS,
            TimeSurfaceFactory.TCROWD_KEY,
            TimeSurfaceFactory.YCROWD_KEY,
        } ) );

        /* Labels tab. */
        addLabelsTab();

        /* Secondary axes tab. */
        mainControl.addSpecifierTab( "Secondary",
                                     new ConfigSpecifier( new ConfigKey<?>[] {
            TimeSurfaceFactory.T2FUNC_KEY,
            TimeSurfaceFactory.T2LABEL_KEY,
            TimeSurfaceFactory.Y2FUNC_KEY,
            TimeSurfaceFactory.Y2LABEL_KEY,
        } ) );

        /* Font tab. */
        mainControl.addSpecifierTab( "Font",
                                     new ConfigSpecifier( StyleKeys.CAPTIONER
                                                         .getKeys() ) );

        assert assertHasKeys( surfFact.getProfileKeys() );
    }

    @Override
    protected boolean logChanged( TimeSurfaceFactory.Profile prof1,
                                  TimeSurfaceFactory.Profile prof2 ) {
        return prof1.getYLog() != prof2.getYLog();
    }

    @Override
    public ConfigMap getConfig() {
        ConfigMap config = super.getConfig();
        assert ! config.keySet().contains( TimeSurfaceFactory.TLABEL_KEY );
        config.put( TimeSurfaceFactory.TLABEL_KEY,
                    TimeSurfaceFactory.TLABEL_KEY.getDefaultValue() );
        return config;
    }

    /**
     * Returns the config keys for axis labelling.
     *
     * @return  T, Y axis label config keys
     */
    private static ConfigKey<String>[] createAxisLabelKeys() {
        List<ConfigKey<String>> list = new ArrayList<ConfigKey<String>>();
        list.add( TimeSurfaceFactory.YLABEL_KEY );
        @SuppressWarnings("unchecked")
        ConfigKey<String>[] keys =
            (ConfigKey<String>[]) list.toArray( new ConfigKey<?>[ 0 ] );
        return keys;
    }
}
