package uk.ac.starlink.topcat.plot2;

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
     * Returns a user panel for entering Area coordinates.
     * If this plot type does not support Area coordinates,
     * an UnsupportedOperationException may be thrown.
     *
     * @return  new area position entry panel for this plot type
     * @throws  UnsupportedOperationException if plot type doesn't support
     *                                        area coordinates
     */
    PositionCoordPanel createAreaCoordPanel();

    /**
     * Indicates whether this plot type supports selectable point positions.
     * Normally the return is true, but if this plot type never plots
     * points that can be identified by a screen X,Y position, return false.
     *
     * @return  false iff this plot type never supports selectable points
     */
    boolean hasPositions();

    /**
     * Returns a list of figure drawing modes that can be used for graphically
     * marking out shapes on the plot surface.
     *
     * @return  available figure modes; may be empty
     */
    public FigureMode[] getFigureModes();

    /**
     * Returns a new zone ID factory for use with this plot.
     * This determines how zone selection for multi-zone plots is done.
     * A new instance should be acquired for each plot window.
     *
     * @return   zone id factory
     */
    ZoneFactory createZoneFactory();

    /**
     * Returns an object that can characterise surfaces used by this plot type
     * as hypercubes in data coordinate space.
     * If it can't be done, null is returned.
     *
     * @return  Cartesian ranger for this plot type, or null
     */
    CartesianRanger getCartesianRanger();

    /**
     * Returns the help ID describing the navigation actions for this plot.
     *
     * @return  navigator help id
     */
    String getNavigatorHelpId();
}
