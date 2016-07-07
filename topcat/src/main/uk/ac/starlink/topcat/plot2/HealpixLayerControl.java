package uk.ac.starlink.topcat.plot2;

import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.LegendEntry;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.Specifier;
import uk.ac.starlink.ttools.plot2.layer.HealpixPlotter;

/**
 * LayerControl for plotting Healpix tile sets.
 *
 * @author   Mark Taylor
 * @since    20 Apr 2016
 */
public class HealpixLayerControl extends BasicCoordLayerControl {

    /**
     * Constructor.
     *
     * @param   plotter  healpix plotter
     * @param   zsel    zone id specifier, may be null for single-zone case
     * @param   baseConfigger   provides global configuration info
     */
    public HealpixLayerControl( HealpixPlotter plotter, Specifier<ZoneId> zsel,
                                Configger baseConfigger ) {
        super( plotter, zsel, new HealpixCoordPanel( plotter ),
               baseConfigger, true );
        assert plotter.getCoordGroup().getPositionCount() == 0;
    }

    public LegendEntry[] getLegendEntries() {
        String label = getLegendLabel();
        Style style = getLegendStyle();
        return label != null && style != null
             ? new LegendEntry[] { new LegendEntry( label, style ) }
             : new LegendEntry[ 0 ];
    }

    /**
     * Returns the plot style chosen for the currently configured plot,
     * if any.
     *
     * @return  style or null
     */
    private Style getLegendStyle() {
        PlotLayer[] layers = getPlotLayers();
        if ( layers.length == 1 ) {
            return layers[ 0 ].getStyle();
        }
        return null;
    }

    /**
     * Returns a label suitable for the legend of the currently configured
     * plot, if any.
     *
     * @return  data label or null
     */
    private String getLegendLabel() {
        for ( GuiCoordContent content : getCoordPanel().getContents() ) {
            if ( HealpixPlotter.VALUE_COORD.equals( content.getCoord() ) ) {
                String[] labels = content.getDataLabels();
                if ( labels.length == 1 ) {
                    return labels[ 0 ];
                }
            }
        }
        return null;
    }

    /**
     * CoordPanel implementation for HealpixLayerControl.
     */
    private static class HealpixCoordPanel extends SimplePositionCoordPanel {
        HealpixCoordPanel( HealpixPlotter plotter ) {
            super( plotter.getCoordGroup().getExtraCoords(),
                   new ConfigKey[] {
                        HealpixPlotter.DATASYS_KEY,
                        HealpixPlotter.DATALEVEL_KEY,
                   }, null );
        }
        public void autoPopulate() {
            // cf. SkyPlotWindow.SkyPositionCoordPanel
            // I need to get the table, look at the parameters,
            // identify the right column for healpix index,
            // try to work out level, etc.
        }
    }
}
