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
     * @param  stack   control stack into which control will be integrated
     * @return  new axis control for this plot type
     */
    AxisControl<P,A> createAxisControl( ControlStack stack );

    /**
     * Returns a user panel for entering basic standard data positions.
     *
     * @param   npos  number of groups of positional coordinates for entry
     * @return   new position entry panel for this plot type
     */
    PositionCoordPanel createPositionCoordPanel( int npos );
}
