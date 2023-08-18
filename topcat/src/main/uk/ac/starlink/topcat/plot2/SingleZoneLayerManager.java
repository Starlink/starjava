package uk.ac.starlink.topcat.plot2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Ganger;
import uk.ac.starlink.ttools.plot2.LegendEntry;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.Specifier;
import uk.ac.starlink.ttools.plot2.data.DataSpec;

/**
 * ZoneLayerManager implementation for a single FormLayerControl
 * working with a single plot zone.
 *
 * @author   Mark Taylor
 * @since    18 Aug 2023
 */
public class SingleZoneLayerManager implements ZoneLayerManager {

    private final FormLayerControl flc_;

    /**
     * Constructor.
     *
     * @param   flc   layer control
     */
    public SingleZoneLayerManager( FormLayerControl flc ) {
        flc_ = flc;
    }

    public boolean hasLayers() {
        return getSingleZoneLayers().length > 0;
    }

    public Map<RowSubset,List<Style>> getStylesBySubset() {
        Map<RowSubset,List<Style>> map = new LinkedHashMap<>();
        for ( SingleZoneLayer szLayer : getSingleZoneLayers() ) {
            PlotLayer layer = szLayer.getPlotLayer();
            DataSpec dspec = layer.getDataSpec();
            if ( dspec != null ) {
                assert dspec instanceof GuiDataSpec;
                if ( dspec instanceof GuiDataSpec ) {
                    map.computeIfAbsent( ((GuiDataSpec) dspec).getRowSubset(),
                                         m -> new ArrayList<Style>() )
                       .add( layer.getStyle() );
                }
            }
        }
        return map;
    }

    public Map<FormControl,List<PlotLayer>>
            getLayersByControl( Ganger<?,?> ganger ) {
        Map<FormControl,List<PlotLayer>> map = new LinkedHashMap<>();
        for ( ManagedLayer szLayer : getSingleZoneLayers() ) {
            map.computeIfAbsent( szLayer.fc_,
                                 fc -> new ArrayList<PlotLayer>() )
               .add( szLayer.getPlotLayer() );
        }
        return map;
    }

    public TopcatLayer[] getLayers( Ganger<?,?> ganger ) {
        Specifier<ZoneId> zsel = flc_.getZoneSpecifier();
        return Arrays.stream( getSingleZoneLayers() )
                     .map( szl -> szl.toGangLayer( ganger, zsel ) )
                     .toArray( n -> new TopcatLayer[ n ] );
    }

    /**
     * Returns a list of SingleZoneLayers managed by this component.
     *
     * @return  single zone layers
     */
    private ManagedLayer[] getSingleZoneLayers() {
        RowSubset[] subsets = flc_.getSubsetStack().getSelectedSubsets();
        PositionCoordPanel posCoordPanel = flc_.getPositionCoordPanel();
        GuiCoordContent[] posContents = posCoordPanel.getContents();
        TopcatModel tcModel = flc_.getTopcatModel();
        if ( tcModel == null || posContents == null || subsets == null ) {
            return new ManagedLayer[ 0 ];
        }
        DataGeom geom = posCoordPanel.getDataGeom();
        ConfigMap coordConfig = posCoordPanel.getConfig();
        FormControl[] fcs = flc_.getActiveFormControls();
        List<ManagedLayer> layerList = new ArrayList<>();
        for ( RowSubset subset : subsets ) {
            String leglabel = flc_.getLegendLabel( subset );
            for ( FormControl fc : fcs ) {
                GuiCoordContent[] extraContents = fc.getExtraCoordContents();
                if ( extraContents != null ) {
                    GuiCoordContent[] contents =
                        PlotUtil.arrayConcat( posContents, extraContents );
                    DataSpec dspec =
                        new GuiDataSpec( tcModel, subset, contents );
                    PlotLayer plotLayer = fc.createLayer( geom, dspec, subset );
                    if ( plotLayer != null ) { 
                        ConfigMap config = new ConfigMap();
                        config.putAll( coordConfig );
                        config.putAll( fc.getExtraConfig() );
                        config.putAll( fc.getStylePanel()
                                         .getConfig( subset.getKey() ) );
                        ManagedLayer layer =
                            new ManagedLayer( plotLayer, config, leglabel,
                                              tcModel, contents, subset, fc );
                        layerList.add( layer );
                    }
                }
            }
        }
        return layerList.toArray( new ManagedLayer[ 0 ] );
    }

    /**
     * Utility class aggregating a SingleZoneLayer with the FormControl
     * that produced it.
     */
    private static class ManagedLayer extends SingleZoneLayer {
        final FormControl fc_;
        ManagedLayer( PlotLayer plotLayer, ConfigMap config, String leglabel,
                      TopcatModel tcModel, GuiCoordContent[] contents,
                      RowSubset rset, FormControl fc ) {
            super( plotLayer, config, leglabel, tcModel, contents, rset );
            fc_ = fc;
        }
    }
}
