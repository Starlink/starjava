package uk.ac.starlink.topcat.plot2;

import uk.ac.starlink.ttools.plot2.GangerFactory;

/**
 * Provides PlotType-specific aspects of the GUI, used by the 
 * generic PlotWindow GUI.
 *
 * @author   Mark Taylor
 * @since    12 Mar 2013
 */
public interface PlotTypeGui<P,A> {

    /**
     * Returns a user control for axis configuration.
     *
     * @return  new axis control for this plot type
     */
    AxisController<P,A> createAxisController();

    /**
     * Returns a user panel for entering basic standard data positions.
     *
     * @param   npos  number of groups of positional coordinates for entry
     * @return   new position entry panel for this plot type
     */
    PositionCoordPanel createPositionCoordPanel( int npos );

    /**
     * Indicates whether this plot type supports selectable point positions.
     * Normally the return is true, but if this plot type never plots
     * points that can be identified by a screen X,Y position, return false.
     *
     * @return  false iff this plot type never supports selectable points
     */
    boolean hasPositions();

    /**
     * Returns the GangerFactory used by this plot.
     * It controls how multi-zone plots are arranged.
     *
     * @return   ganger factory
     */
    GangerFactory getGangerFactory();

    /**
     * Returns a new zone ID factory for use with this plot.
     * This determines how zone selection for multi-zone plots is done.
     * A new instance should be acquired for each plot window.
     *
     * @return   zone id factory
     */
    ZoneFactory createZoneFactory();

    /**
     * Returns the help ID describing the navigation actions for this plot.
     *
     * @return  navigator help id
     */
    public String getNavigatorHelpId();
}
