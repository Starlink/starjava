package uk.ac.starlink.topcat.plot2;

import java.util.Map;
import uk.ac.starlink.topcat.TablesListComboBox;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.LegendEntry;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.Specifier;
import uk.ac.starlink.ttools.plot2.data.DataSpec;

/**
 * Minimal LayerControl implementation.
 * Suitable for basic plotters with no table data, so no coordpanels required.
 * Currently, no legend entries are reported either.
 *
 * @author   Mark Taylor
 * @since    10 Apr 2017
 */
public class DatalessLayerControl extends ConfigControl
                                  implements LayerControl {

    private final Plotter<?> plotter_;
    private final Specifier<ZoneId> zsel_;
    private final Configger baseConfigger_;
    private final ConfigStyler styler_;

    /**
     * Constructor.
     *
     * @param  plotter  plotter
     * @param   zsel    zone id specifier, may be null for single-zone case
     * @param   baseConfigger   provides global configuration info
     */
    public DatalessLayerControl( Plotter<?> plotter, Specifier<ZoneId> zsel,
                                 Configger baseConfigger ) {
        super( plotter.getPlotterName(), plotter.getPlotterIcon() );
        plotter_ = plotter;
        zsel_ = zsel;
        baseConfigger_ = baseConfigger;
        styler_ = new ConfigStyler( getPanel() );
        addSpecifierTab( "Style",
                         new ConfigSpecifier( plotter.getStyleKeys() ) );
        if ( zsel != null ) {
            addZoneTab( zsel );
        }
    }

    public Plotter<?>[] getPlotters() {
        return new Plotter<?>[] { plotter_ };
    }

    public TopcatLayer[] getLayers() {
        DataGeom geom = null;
        DataSpec dataSpec = null;
        ConfigMap config = baseConfigger_.getConfig();
        config.putAll( getConfig() );
        PlotLayer plotLayer =
            styler_.createLayer( plotter_, geom, dataSpec, config );
        return plotLayer == null
             ? new TopcatLayer[ 0 ]
             : new TopcatLayer[] { new TopcatLayer( plotLayer, config, null ) };
    }

    public LegendEntry[] getLegendEntries() {
        return new LegendEntry[ 0 ];
    }

    public void submitReports( Map<LayerId,ReportMap> reports ) {
        TopcatLayer[] tcLayers = getLayers();
        PlotLayer layer = tcLayers.length == 1
                        ? tcLayers[ 0 ].getPlotLayer()
                        : null;
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

    public Specifier<ZoneId> getZoneSpecifier() {
        return zsel_;
    }

    public TablesListComboBox getTableSelector() {
        return null;
    }
}
