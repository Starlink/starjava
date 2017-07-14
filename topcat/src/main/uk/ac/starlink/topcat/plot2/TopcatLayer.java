package uk.ac.starlink.topcat.plot2;

import uk.ac.starlink.ttools.plot2.PlotLayer;

/**
 * Permits aggregation of a PlotLayer with other application-specific
 * information.
 *
 * @author   Mark Taylor
 * @since    14 Jul 2017
 */
public class TopcatLayer {

    private final PlotLayer plotLayer_;

    /**
     * Constructor.
     *
     * @param  plotLayer  plot layer, not null
     */
    public TopcatLayer( PlotLayer plotLayer ) {
        plotLayer_ = plotLayer;
    }

    /**
     * Returns this object's plot layer.
     *
     * @return  plot layer, not null
     */
    public PlotLayer getPlotLayer() {
        return plotLayer_;
    }
}
