package uk.ac.starlink.topcat.plot2;

import javax.swing.Action;
import uk.ac.starlink.topcat.TopcatModel;

/**
 * Abstracts the provision of plotting controls for the plotting GUI.
 *
 * @author   Mark Taylor
 * @since    15 Mar 2013
 */
public interface ControlManager {

    /**
     * Returns a list of actions which can be performed to add controls
     * to the stack.
     *
     * @return   list of stack actions
     */
    Action[] getStackActions();

    /**
     * Returns a suitable control to add to the stack for a given table.
     * It's useful to have something like this so that when the plot
     * window is first shown, it displays some data rather than none.
     * Even if the actual plot is not very meaningful, it gives the
     * user a chance to get started with the GUI with a minimum of thought.
     *
     * @param  tcModel  initial table
     * @return  some control that will generate an example plot
     *          using table data
     */
    Control createDefaultControl( TopcatModel tcModel );

    /**
     * Adds a layer to the plot as specified by the given layer command.
     * Ideally, appropriate changes should be made to the GUI as well,
     * so the effect is just as if the user had added the layer by hand.
     *
     * @param   lcmd  specifies the layer to add
     */
    void addLayer( LayerCommand lcmd );
}
