package uk.ac.starlink.topcat.plot2;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.Action;
import uk.ac.starlink.topcat.BasicAction;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.topcat.TopcatListener;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.plot.Styles;
import uk.ac.starlink.ttools.plot2.PlotType;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;

/**
 * Control manager that uses GangLayerControls to provide
 * panels that allow you to enter the position values once
 * for a given table and then go to other tabs in the control
 * to customise the layers generated.
 * 
 * @author   Mark Taylor
 * @since    15 Mar 2013
 */
public class GangControlManager implements ControlManager {

    private final ControlStack stack_;
    private final PlotType plotType_;
    private final PlotTypeGui plotTypeGui_;
    private final Configger baseConfigger_;
    private final TopcatListener tcListener_;
    private final NextSupplier nextSupplier_;
    private final Plotter[] posPlotters_;
    private final Action[] stackActs_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat.plot2" );

    /**
     * Constructor.
     */
    public GangControlManager( ControlStack stack, PlotType plotType,
                               PlotTypeGui plotTypeGui, Configger baseConfigger,
                               TopcatListener tcListener ) {
        stack_ = stack;
        plotType_ = plotType;
        plotTypeGui_ = plotTypeGui;
        baseConfigger_ = baseConfigger;
        tcListener_ = tcListener;
        nextSupplier_ = new NextSupplier();
        nextSupplier_.putValues( StyleKeys.COLOR, Styles.COLORS );

        /* Split the list of plotters into positional and dataless ones. */
        List<Plotter> posPlotterList = new ArrayList<Plotter>();
        List<Action> stackActList = new ArrayList<Action>();
        Plotter[] plotters = plotType_.getPlotters();
        for ( int i = 0; i < plotters.length; i++ ) {
            Plotter plotter = plotters[ i ];
            if ( plotter.getPositionCount() == 1 ) {
                posPlotterList.add( plotter );
            }
            else {
                Action stackAct =
                    PlotterStackAction.createAction( plotter, stack );
                if ( stackAct != null ) {
                    stackActList.add( stackAct );
                }
                else {
                    logger_.warning( "No GUI available for plotter "
                                   + plotter.getPlotterName() );
                }
            }
        }
        Action gangAct = new BasicAction( "Add Table Layer",
                                          ResourceIcon.PLOT_DATA,
                                          "Add a new table plot layer "
                                          + "control to the stack" ) {
            public void actionPerformed( ActionEvent evt ) {
                stack_.addControl( createGangControl() );
            }
        };
        stackActList.add( 0, gangAct );
        stackActs_ = stackActList.toArray( new Action[ 0 ] );
        posPlotters_ = posPlotterList.toArray( new Plotter[ 0 ] );
    }

    public Action[] getStackActions() {
        return stackActs_;
    }

    public Control createDefaultControl( TopcatModel tcModel ) {
        GangLayerControl control = createGangControl();
        control.setTopcatModel( tcModel );
        return control;
    }

    /**
     * Creates a new empty gang layer control.
     *
     * @return   gang control
     */
    private GangLayerControl createGangControl() {
        return new GangLayerControl( plotTypeGui_.createPositionCoordPanel(),
                                     posPlotters_, baseConfigger_,
                                     nextSupplier_, tcListener_ );
    }
}
