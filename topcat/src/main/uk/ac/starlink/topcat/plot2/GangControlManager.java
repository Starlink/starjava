package uk.ac.starlink.topcat.plot2;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.Icon;
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
    private final SortedMap<Integer,List<Plotter>> plotterMap_;
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
        List<Action> stackActList = new ArrayList<Action>();

        /* Split the list up by the number of positional coordinates
         * they have. */
        plotterMap_ = new TreeMap<Integer,List<Plotter>>();
        plotterMap_.put( 0, new ArrayList<Plotter>() );
        plotterMap_.put( 1, new ArrayList<Plotter>() );
        plotterMap_.put( 2, new ArrayList<Plotter>() );
        Plotter[] plotters = plotType_.getPlotters();
        for ( int i = 0; i < plotters.length; i++ ) {
            Plotter plotter = plotters[ i ];
            int npos = plotter.getPositionCount();
            if ( ! plotterMap_.containsKey( npos ) ) {
                plotterMap_.put( npos, new ArrayList<Plotter>() );
            }
            plotterMap_.get( npos ).add( plotter );
        }

        /* Add an action for single-position plotters. */
        final Icon icon1 = ResourceIcon.PLOT_DATA;
        stackActList.add( new BasicAction( "Add Position Plot", icon1,
                                           "Add a new positional "
                                         + "plot control to the stack" ) {
            public void actionPerformed( ActionEvent evt ) {
                stack_.addControl( createGangControl( 1, icon1 ) );
            }
        } );

        /* Add an action for double-position plotters. */
        final Icon icon2 = ResourceIcon.PLOT_PAIR;
        stackActList.add( new BasicAction( "Add Pair Plot", icon2,
                                           "Add a new pair position "
                                         + "plot control to the stack" ) {
            public void actionPerformed( ActionEvent evt ) {
                stack_.addControl( createGangControl( 2, icon2 ) );
            }
        } );

        /* Add actions for non-positional plotters. */
        for ( Plotter plotter : plotterMap_.get( 0 ) ) {
            Action stackAct = PlotterStackAction.createAction( plotter, stack );
            if ( stackAct != null ) {
                stackActList.add( stackAct );
            }
            else {
                logger_.warning( "No GUI available for plotter "
                               + plotter.getPlotterName() );
            }
        }

        /* For now, we don't take steps to present triple-positional plotters
         * and beyond, because there aren't any.  But warn if some arise. */
        stackActs_ = stackActList.toArray( new Action[ 0 ] );
        int unused = plotterMap_.tailMap( new Integer( 3 ) ).size();
        if ( unused > 0 ) {
            logger_.warning( unused + " plotters not presented in GUI" );
        }
    }

    public Action[] getStackActions() {
        return stackActs_;
    }

    public Control createDefaultControl( TopcatModel tcModel ) {
        GangLayerControl control =
            createGangControl( 1, ResourceIcon.PLOT_DATA );
        control.setTopcatModel( tcModel );
        return control;
    }

    /**
     * Creates a new empty gang layer control.
     *
     * @param   npos  number of groups of positional coordinates for entry
     * @return   gang control
     */
    private GangLayerControl createGangControl( int npos, Icon icon ) {
        List<Plotter> plotterList = plotterMap_.get( npos );
        return plotterList != null && plotterList.size() > 0
             ? new GangLayerControl( plotTypeGui_
                                    .createPositionCoordPanel( npos ),
                                     npos == 1,
                                     plotterList.toArray( new Plotter[ 0 ] ),
                                     baseConfigger_, nextSupplier_,
                                     tcListener_, icon )
             : null;
    }
}
