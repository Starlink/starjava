package uk.ac.starlink.ttools.plot2;

/**
 * Supplies additional plot-time information about multi-zone requirements.
 *
 * @author   Mark Taylor
 * @since    16 Aug 2023
 */
public interface GangContext {

    /**
     * Returns the list of plotters that will contribute to the plot.
     *
     * @return   array of plotters, one per plotted layer
     */
    Plotter<?>[] getPlotters();

    /**
     * Returns the list of zone names associated with the zones being plotted.
     * This may influence gang creation for GangerFactories with
     * {@link GangerFactory#hasIndependentZones}=true.
     *
     * @return  array of requested zone names
     */
    String[] getRequestedZoneNames();
}
