package uk.ac.starlink.topcat.plot2;

import java.util.Map;
import uk.ac.starlink.topcat.TablesListComboBox;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.LegendEntry;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.Plotter;
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
public class DatalessLayerControl extends SingleZoneLayerControl {

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
    @SuppressWarnings("this-escape")
    public DatalessLayerControl( Plotter<?> plotter, Specifier<ZoneId> zsel,
                                 Configger baseConfigger ) {
        super( plotter.getPlotterName(), plotter.getPlotterIcon(), zsel );
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

    protected SingleZoneLayer getSingleZoneLayer() {
        DataGeom geom = null;
        DataSpec dataSpec = null;
        ConfigMap config = baseConfigger_.getConfig();
        config.putAll( getConfig() );
        PlotLayer plotLayer =
            styler_.createLayer( plotter_, geom, dataSpec, config );
        return plotLayer == null
             ? null
             : new SingleZoneLayer( plotLayer, config, null );
    }

    public LegendEntry[] getLegendEntries() {
        return new LegendEntry[ 0 ];
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
