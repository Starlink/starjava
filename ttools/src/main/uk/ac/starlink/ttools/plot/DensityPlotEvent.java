package uk.ac.starlink.ttools.plot;

import java.awt.Component;

/**
 * PlotEvent for density plots.
 *
 * @author   Mark Taylor
 * @since    3 Apr 2008
 */
public class DensityPlotEvent extends PlotEvent {

    private final double[] loCuts_;
    private final double[] hiCuts_;

    /**
     * Constructor.
     *
     * @param   source   source of this event
     * @param   plotState    plot state reflected by this change event
     * @param   nPotential  total number of points available
     * @param   nIncluded  number of points included in marked subsets
     * @param   nVisible  number of points actually plotted 
     * @param   loCuts    nchannel-element array of lower bin cut levels
     * @param   hiCuts    nchannel-element array of upper bin cut levels
     */
    public DensityPlotEvent( Component source, DensityPlotState plotState,
                             int nPotential, int nIncluded, int nVisible,
                             double[] loCuts, double[] hiCuts ) {
        super( source, plotState, nPotential, nIncluded, nVisible );
        loCuts_ = loCuts;
        hiCuts_ = hiCuts;
    }

    /**
     * Returns lower cut values for the channels plotted.
     * These are the bin counts below which pixels will be coloured
     * at the low-end colour.  For an unweighted plot these values will
     * be integers.
     *
     * @return   array giving absolute lower cut levels for each channel
     */
    public double[] getLoCuts() {
        return loCuts_;
    }

    /**
     * Returns upper cut values for the channels plotted.
     * These are the bin counts above which pixels will be coloured
     * as the high-end colour.  For an unweighted plot these values will
     * be integers.
     *
     * @return  array giving absolute upper cut levels for each channel
     */
    public double[] getHiCuts() {
        return hiCuts_;
    }
}
