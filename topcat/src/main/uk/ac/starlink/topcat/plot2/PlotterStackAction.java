package uk.ac.starlink.topcat.plot2;

import java.awt.event.ActionEvent;
import javax.swing.Action;
import uk.ac.starlink.topcat.BasicAction;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.layer.FunctionPlotter;
import uk.ac.starlink.ttools.plot2.layer.SpectrogramPlotter;

/**
 * Action for adding a single-plotter layer control to the plot stack.
 *
 * @author   Mark Taylor
 * @since    25 Jul 2013
 */
public abstract class PlotterStackAction extends BasicAction {

    private final Plotter plotter_;
    private final ControlStack stack_;

    /**
     * Constructor.
     *
     * @param   plotter  plotter this action is based on
     * @param   stack   plot stack
     */
    public PlotterStackAction( Plotter plotter, ControlStack stack ) {
        super( "Add " + plotter.getPlotterName() + " Layer",
               plotter.getPlotterIcon(),
               "Add a new " + plotter.getPlotterName().toLowerCase()
                            + " layer control to the stack" );
        plotter_ = plotter;
        stack_ = stack;
    }

    /**
     * Creates a layer control for this action's plotter.
     *
     * @return   new layer control
     */
    protected abstract LayerControl createLayerControl();

    public void actionPerformed( ActionEvent evt ) {
        stack_.addControl( createLayerControl() );
    }

    /**
     * Attempts to return an instance of this class corresponding to
     * a given plotter.  If no suitable implementation is available,
     * null is returned.
     *
     * @param  plotter   plotter to provide an action for
     * @param  stack    stack to which controls are to be added
     * @return  new action to add plotter control to stack, or null
     */
    public static Action createAction( final Plotter plotter,
                                       ControlStack stack ) {
        if ( plotter instanceof FunctionPlotter ) {
            final FunctionPlotter fPlotter = (FunctionPlotter) plotter;
            return new PlotterStackAction( plotter, stack ) {
                protected LayerControl createLayerControl() {
                    return new FunctionLayerControl( fPlotter );
                }
            };
        }
        else if ( plotter instanceof SpectrogramPlotter ) {
            final SpectrogramPlotter sPlotter = (SpectrogramPlotter) plotter;
            return new PlotterStackAction( plotter, stack ) {
                protected LayerControl createLayerControl() {
                    return new SpectrogramLayerControl( sPlotter );
                }
            };
        }

        /* Not great - no options for miscellaneous plotters with both
         * positional and non-positional coordinates.  There's no reason
         * it can't be done, but the (probably unnecessarily messy)
         * way that CoordPanel and PositionCoordPanel are currently
         * defined makes it fiddly to do. */
        else if ( plotter.getPositionCount() == 0 ) {
            return new PlotterStackAction( plotter, stack ) {
                protected LayerControl createLayerControl() {
                    PositionCoordPanel coordPanel =
                        new SimplePositionCoordPanel(
                            new CoordPanel( plotter.getExtraCoords(), false ),
                            null );
                    return new BasicCoordLayerControl( plotter, coordPanel );
                }
            };
        }
        else {
            return null;
        }
    }
}
