package uk.ac.starlink.topcat.plot2;

import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.plot2.Ganger;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.Specifier;

/**
 * Represents a PlotLayer and associated information when it is to be
 * used within a single plot zone.
 *
 * @author   Mark Taylor
 * @since    18 Aug 2023
 */
public class SingleZoneLayer {

    private final PlotLayer plotLayer_;
    private final ConfigMap config_;
    private final String leglabel_;
    private final TopcatModel tcModel_;
    private final GuiCoordContent[] contents_;
    private final RowSubset rset_;

    /**
     * General constructor.
     *
     * @param  plotLayer  plot layer
     * @param  config    configuration items that generated this layer
     *                   (superset is permitted)
     * @param  leglabel   label used in the legend;
     *                    if null, excluded from the legend
     * @param  tcModel    topcat model producing layer, may be null
     * @param  contents  information about data columns used to construct plot
     *                   (superset is not permitted)
     * @param  rset    row subset for which layer is plotted
     */
    public SingleZoneLayer( PlotLayer plotLayer, ConfigMap config,
                            String leglabel, TopcatModel tcModel,
                            GuiCoordContent[] contents, RowSubset rset ) {
        plotLayer_ = plotLayer;
        config_ = config;
        leglabel_ = leglabel;
        tcModel_ = tcModel;
        contents_ = contents;
        rset_ = rset;
        assert plotLayer_ != null;
    }

    /**
     * Constructor for dataless layer.
     *
     * @param  plotLayer  plot layer
     * @param  config    configuration items that generated this layer
     *                   (superset is permitted)
     * @param  leglabel   label used in the legend;
     *                    if null, excluded from the legend
     */
    public SingleZoneLayer( PlotLayer plotLayer, ConfigMap config,
                            String leglabel ) {
        this( plotLayer, config, leglabel, null, null, null );
    }

    /**
     * Returns the plot layer.
     *
     * @return  plot layer
     */
    public PlotLayer getPlotLayer() {
        return plotLayer_;
    }

    /**
     * Returns the layer identifier.
     *
     * @return  layer ID
     */
    public LayerId getLayerId() {
        return LayerId.createLayerId( plotLayer_ );
    }

    /**
     * Converts this single-zone layer to a TopcatLayer.
     * If the supplied zone selector is non-null it will determine
     * which zone the PlotLayer appears in, otherwise it will appear
     * in the first (and presumably only) zone.
     *
     * @param   ganger  ganger
     * @param   zsel   zone selector, may be null
     * @return  topcat layer containing this layer
     */
    public TopcatLayer toGangLayer( Ganger<?,?> ganger,
                                    Specifier<ZoneId> zsel ) {
        int nz = ganger.getZoneCount();
        int izone = zsel == null
                  ? 0
                  : zsel.getSpecifiedValue().getZoneIndex( ganger );
        PlotLayer[] gangLayers = new PlotLayer[ nz ];
        gangLayers[ izone ] = plotLayer_;
        return new TopcatLayer( gangLayers, config_, leglabel_, tcModel_,
                                contents_, rset_ );
    }
}
