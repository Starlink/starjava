package uk.ac.starlink.topcat.plot2;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import uk.ac.starlink.ttools.plot2.LegendEntry;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.config.ConfigException;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.Specifier;
import uk.ac.starlink.ttools.plot2.config.StringConfigKey;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.layer.FunctionPlotter;

/**
 * Layer control for plotting functions.
 *
 * @author   Mark Taylor
 * @since    26 Mar 2013
 */
public class FunctionLayerControl extends ConfigControl
                                  implements LayerControl {

    private final FunctionPlotter plotter_;
    private final Specifier<ZoneId> zsel_;
    private static final ConfigKey<String> FUNCLABEL_KEY =
        new StringConfigKey( new ConfigMeta( "label", "Label" ), "Function" );

    /**
     * Constructor.
     *
     * @param   plotter  function plotter
     * @param   zsel    zone id specifier, may be null for single-zone plots
     */
    public FunctionLayerControl( FunctionPlotter plotter,
                                 Specifier<ZoneId> zsel ) {
        super( plotter.getPlotterName(), plotter.getPlotterIcon() );
        plotter_ = plotter;
        zsel_ = zsel;
        AutoConfigSpecifier legendSpecifier =
            new AutoConfigSpecifier( new ConfigKey[] { FUNCLABEL_KEY,
                                                       StyleKeys.SHOW_LABEL },
                                     new ConfigKey[] { FUNCLABEL_KEY } );
        final AutoSpecifier<String> labelSpecifier =
            legendSpecifier.getAutoSpecifier( FUNCLABEL_KEY );

        /* Split up style keys into two parts for more logical presentation
         * in the GUI. */
        final ConfigKey[] funcKeys = plotter.getFunctionStyleKeys();
        List<ConfigKey> otherKeyList =
            new ArrayList( Arrays.asList( plotter.getStyleKeys() ) );
        otherKeyList.removeAll( Arrays.asList( funcKeys ) );
        final ConfigKey[] otherKeys =
            otherKeyList.toArray( new ConfigKey[ 0 ] );
        final ConfigSpecifier funcSpecifier = new ConfigSpecifier( funcKeys );
        final ConfigSpecifier otherSpecifier = new ConfigSpecifier( otherKeys );

        /* Fix it so the default value of the legend label is the
         * text of the function. */
        labelSpecifier.setAutoValue( plotter.getPlotterName() );
        funcSpecifier.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                FunctionPlotter.FunctionStyle style;
                try {
                    style = plotter_.createStyle( getConfig() );
                }
                catch ( ConfigException e ) {
                    funcSpecifier.reportError( e );
                    style = null;
                }
                labelSpecifier.setAutoValue( style == null
                                           ? plotter_.getPlotterName()
                                           : style.toString() );
            }
        } );

        /* Add tabs. */
        addSpecifierTab( "Function", funcSpecifier );
        addSpecifierTab( "Style", otherSpecifier );
        addSpecifierTab( "Label", legendSpecifier );
        if ( zsel != null ) {
            addZoneTab( zsel );
        }
    }

    public PlotLayer[] getPlotLayers() {
        PlotLayer layer =
            plotter_.createLayer( null, null, getFunctionStyle( getConfig() ) );
        return layer == null ? new PlotLayer[ 0 ] : new PlotLayer[] { layer };
    }

    public LegendEntry[] getLegendEntries() {
        ConfigMap config = getConfig();
        FunctionPlotter.FunctionStyle style = getFunctionStyle( config );
        Boolean showLabel = config.get( StyleKeys.SHOW_LABEL );
        String label = config.get( FUNCLABEL_KEY );
        return showLabel && style != null && label != null
             ? new LegendEntry[] { new LegendEntry( label, style ) }
             : new LegendEntry[ 0 ];
    }

    public Specifier<ZoneId> getZoneSpecifier() {
        return zsel_;
    }

    public void submitReports( Map<LayerId,ReportMap> reports ) {
        PlotLayer[] layers = getPlotLayers();
        PlotLayer layer = layers.length == 1 ? layers[ 0 ] : null;
        if ( layer != null ) {
            ReportMap report = reports.get( LayerId.createLayerId( layer ) );
            if ( report != null ) {
                for ( Specifier<ConfigMap> cspec : getConfigSpecifiers() ) {
                    cspec.submitReport( report );
                }
            }
        }
    }

    public String getCoordLabel( String userCoordName ) {
        return null;
    }

    /**
     * Returns the style for a given config without error.
     * In case of ConfigException, null is returned.
     *
     * @param  config  config map
     * @return  style, or null
     */
    private FunctionPlotter.FunctionStyle getFunctionStyle( ConfigMap config ) {
        try {
            return plotter_.createStyle( config );
        }
        catch ( ConfigException e ) {
            return null;
        }
    }
}
