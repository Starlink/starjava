package uk.ac.starlink.ttools.plot;

import java.awt.Component;

/**
 * PlotEvent for scatter plots.
 *
 * @author   Mark Taylor
 * @since    3 Apr 2008
 */
public class ScatterPlotEvent extends PlotEvent {

    private final XYStats[] xyStats_;

    /**
     * Constructor.
     *
     * @param   source   source of this event
     * @param   plotState    plot state reflected by this change event
     * @param   nPotential  total number of points available
     * @param   nIncluded  number of points included in marked subsets
     * @param   nVisible  number of points actually plotted 
     * @param   xyStats   calculated correlation statistics for each set
     *                    if applicable
     */
    public ScatterPlotEvent( Component source, PlotState plotState,
                             int nPotential, int nIncluded, int nVisible,
                             XYStats[] xyStats ) {
        super( source, plotState, nPotential, nIncluded, nVisible );
        xyStats_ = xyStats;
    }

    /**
     * Returns an array of the calculated linear correlation statistics 
     * for each set, if correlation calculations were requested.
     *
     * @return   nset-element array of correlation statistics if required
     */
    public XYStats[] getXYStats() {
        return xyStats_;
    }
}
