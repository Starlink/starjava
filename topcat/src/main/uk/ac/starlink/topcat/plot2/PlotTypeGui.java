package uk.ac.starlink.topcat.plot2;

import uk.ac.starlink.ttools.plot2.config.Specifier;

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
     * Returns a factory for zone selectors to be used by layer controls
     * for this plot.  This determines how zone selection for multi-zone
     * plots is done.  The return value of this method must not be null,
     * but in the case of a single-zone plot type, the specifiers it
     * dispenses may be null.
     *
     * @return   zone id specifier factory
     */
    Factory<Specifier<ZoneId>> createZoneSpecifierFactory();

    /**
     * Returns the help ID describing the navigation actions for this plot.
     *
     * @return  navigator help id
     */
    public String getNavigatorHelpId();
}
