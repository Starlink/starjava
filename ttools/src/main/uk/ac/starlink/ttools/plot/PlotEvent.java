package uk.ac.starlink.ttools.plot;

import java.awt.Component;

/**
 * Event sent to a {@link PlotListener} when a plot has been completed.
 * This describes the state which led to the plot and any useful values
 * calculated as the plot was performed which would be difficult or
 * inefficient to determine without doing a plot.
 * Specialised subclasses may be defined to encapsulate additional such
 * values.
 *
 * @author   Mark Taylor
 * @since    3 Apr 2008
 */
public class PlotEvent {

    private final Component source_;
    private final PlotState plotState_;
    private final int nPotential_;
    private final int nIncluded_;
    private final int nVisible_;

    /**
     * Constructor.
     *
     * @param   source   source of this event
     * @param   plotState    plot state reflected by this change event
     * @param   nPotential  total number of points available
     * @param   nIncluded  number of points included in marked subsets
     * @param   nVisible  number of points actually plotted
     *                    (may be less than nIncluded if some are out of bounds)
     */
    public PlotEvent( Component source, PlotState plotState,
                      int nPotential, int nIncluded, int nVisible ) {
        source_ = source;
        plotState_ = plotState;
        nPotential_ = nPotential;
        nIncluded_ = nIncluded;
        nVisible_ = nVisible;
    }

    /**
     * Returns the component in which the plot was done.
     *
     * @return  event source
     */
    public Component getSource() {
        return source_;
    }

    /**
     * Returns the plot state defining the characteristics of the plot.
     *
     * @return  plot state
     */
    public PlotState getPlotState() {
        return plotState_;
    }

    /**
     * Returns the number of points in the point set which were available
     * for plotting.
     *
     * @return   maximum potential point count
     */
    public int getPotentialPointCount() {
        return nPotential_;
    }

    /**
     * Returns the number of points included in subsets which were
     * selected for plotting.
     *
     * @return  number of non-excluded points
     */
    public int getIncludedPointCount() {
        return nIncluded_;
    }

    /**
     * Returns the number of points which were actually plotted.
     * This may be fewer than the the value given by 
     * {@link #getIncludedPointCount} if some have blank values or 
     * fall outside the bounds of the chosen plotting surface.
     *
     * @return   number of points plotted
     */
    public int getVisiblePointCount() {
        return nVisible_;
    }
}
