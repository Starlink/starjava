package uk.ac.starlink.topcat.plot2;

import java.util.Map;
import javax.swing.Icon;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.plot2.Ganger;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.Specifier;

/**
 * Partial LayerControl implementation for single-zone plots.
 *
 * @author   Mark Taylor
 * @since    18 Aug 2023
 */
public abstract class SingleZoneLayerControl extends ConfigControl
                                             implements LayerControl {

    private final Specifier<ZoneId> zsel_;

    /**
     * Constructor.
     *
     * @param  label  control label
     * @param  icon   control icon
     * @param   zsel   zone selector, may be null
     */
    protected SingleZoneLayerControl( String label, Icon icon,
                                      Specifier<ZoneId> zsel ) {
        super( label, icon );
        zsel_ = zsel;
    }

    /**
     * Returns the layer produced by this control.
     *
     * @return  single zone layer, or null if none is active
     */
    protected abstract SingleZoneLayer getSingleZoneLayer();

    public boolean hasLayers() {
        return getSingleZoneLayer() != null;
    }

    public TopcatLayer[] getLayers( Ganger<?,?> ganger ) {
        SingleZoneLayer szLayer = getSingleZoneLayer();
        return szLayer == null
             ? new TopcatLayer[ 0 ]
             : new TopcatLayer[] { szLayer.toGangLayer( ganger, zsel_ ) };
    }

    public void submitReports( Map<LayerId,ReportMap> reports,
                               Ganger<?,?>  ganger ) {
        SingleZoneLayer szLayer = getSingleZoneLayer();
        if ( szLayer != null ) {
            ReportMap report = reports.get( szLayer.getLayerId() );
            if ( report != null ) {
                for ( Specifier<ConfigMap> cspec : getConfigSpecifiers() ) {
                    cspec.submitReport( report );
                }
            }
        }
    }
}
