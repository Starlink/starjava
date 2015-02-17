package uk.ac.starlink.topcat.plot2;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.topcat.TopcatListener;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.layer.ModePlotter;

/**
 * FormLayerControl in which forms can be added to and removed from
 * a form stack under user control.
 *
 * @author   Mark Taylor
 * @since    8 Jan 2014
 */
public class MultiFormLayerControl extends FormLayerControl {

    private final Configger baseConfigger_;
    private final ConfigKey[] subsetKeys_;
    private final ControlStack formStack_;
    private final ControlStackModel formStackModel_;
    private final List<Plotter> singlePlotterList_;
    private final Map<ModePlotter.Form,List<ModePlotter>> modePlotterMap_;
    private final Action dfltFormAct_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat.plot2" );

    /**
     * Constructor.
     *
     * @param  posCoordPanel  panel for entering table and basic positional
     *                        coordinates
     * @param  autoPopulate  if true, when the table is changed an attempt
     *                       will be made to initialise the coordinate fields
     *                       with some suitable values
     * @param  nextSupplier  manages global dispensing for some style options
     * @param  tcListener  listener for TopcatEvents
     * @param  controlIcon  icon for control stack
     * @param  plotters    plotter objects providing different plot layer
     *                     type options
     * @param  baseConfigger  configuration source for some global config
     *                        options
     */
    public MultiFormLayerControl( PositionCoordPanel posCoordPanel,
                                  boolean autoPopulate,
                                  NextSupplier nextSupplier,
                                  TopcatListener tcListener, Icon controlIcon,
                                  Plotter[] plotters,
                                  Configger baseConfigger ) {
        super( posCoordPanel, autoPopulate, nextSupplier, tcListener,
               controlIcon );
        baseConfigger_ = baseConfigger;
        subsetKeys_ = nextSupplier.getKeys();

        /* Set up the panel holding the form selector.
         * This is a (visually) complicated component which allows the
         * user to configure many different plot layers based on the
         * selected table and positional coordinates.
         * It has a list of controls and a panel; when an item is
         * selected from the list, the panel is filled in with the
         * corresponding component. */
        final JComponent fcHolder = new JPanel( new BorderLayout() );
        formStackModel_ = new ControlStackModel();
        formStack_ = new ControlStack( formStackModel_ );
        formStack_.addListSelectionListener( new ListSelectionListener() {
            public void valueChanged( ListSelectionEvent evt ) {
                fcHolder.removeAll();
                Object item = formStack_.getSelectedValue();
                if ( item instanceof FormControl ) {
                    FormControl fc = (FormControl) item;
                    fcHolder.add( fc.getPanel(), BorderLayout.NORTH );
                    fcHolder.revalidate();
                    fcHolder.repaint();
                }
            }
        } );
        JScrollPane stackScroller = new JScrollPane( formStack_ );
        stackScroller.setHorizontalScrollBarPolicy( JScrollPane
                                                  .HORIZONTAL_SCROLLBAR_NEVER );
        JScrollPane fcScroller = new JScrollPane( fcHolder );
        fcScroller.getVerticalScrollBar().setUnitIncrement( 16 );
        JComponent formPanel = new JPanel( new BorderLayout() );
        JComponent body =  new JPanel( new BorderLayout() );
        body.add( stackScroller, BorderLayout.WEST );
        body.add( fcScroller, BorderLayout.CENTER );
        formPanel.add( body, BorderLayout.CENTER );
        formStackModel_.addPlotActionListener( getActionForwarder() );

        /* Divide up the supplied plotters into those which constitute
         * mode/form families, and standalone ones. */
        singlePlotterList_ = new ArrayList<Plotter>();
        modePlotterMap_ =
            new LinkedHashMap<ModePlotter.Form,List<ModePlotter>>();
        for ( int ip = 0; ip < plotters.length; ip++ ) {
            Plotter plotter = plotters[ ip ];
            if ( plotter instanceof ModePlotter ) {
                ModePlotter modePlotter = (ModePlotter) plotter;
                ModePlotter.Form form = modePlotter.getForm();
                if ( ! modePlotterMap_.containsKey( form ) ) {
                    modePlotterMap_.put( form, new ArrayList<ModePlotter>() );
                }
                modePlotterMap_.get( form ).add( modePlotter );
            }
            else {
                singlePlotterList_.add( plotter );
            }
        }

        /* Set up an action for each form-family of plotters, and each
         * standalone plotter. */
        List<Action> formActionList = new ArrayList<Action>();
        for ( ModePlotter.Form form : modePlotterMap_.keySet() ) {
            ModePlotter[] modePlotters =
                modePlotterMap_.get( form ).toArray( new ModePlotter[ 0 ] );
            formActionList.add( new ModeFormAction( modePlotters, form ) );
        }
        for ( Plotter plotter : singlePlotterList_ ) {
            formActionList.add( new SingleFormAction( plotter ) );
        }

        /* Action to remove the current form from the stack. */
        Action removeAction =
            formStack_.createRemoveAction( "Remove",
                                           "Delete the current form" );

        /* Populate a toolbar with these actions. */
        JToolBar formToolbar = new JToolBar();
        formToolbar.setFloatable( false );
        for ( Action formAct : formActionList ) {
            formToolbar.add( formAct );
        }
        formToolbar.addSeparator();
        formToolbar.add( removeAction );
        formPanel.add( formToolbar, BorderLayout.NORTH );
        addControlTab( "Form", formPanel, false );
        dfltFormAct_ = formActionList.get( 0 );
    }

    /**
     * Returns the controls in the form control list which are contributing
     * to the plot.  Controls that the user has deactivated (unchecked)
     * are ignored.
     *
     * @return  list of active form controls
     */
    protected FormControl[] getActiveFormControls() {
        List<FormControl> fcList = new ArrayList<FormControl>();
        int nf = formStackModel_.getSize();
        for ( int ifm = 0; ifm < nf; ifm++ ) {
            FormControl fc = (FormControl) formStackModel_.getControlAt( ifm );
            if ( formStackModel_.isControlActive( fc ) ) {
                fcList.add( fc );
            }
        }
        return fcList.toArray( new FormControl[ 0 ] );
    }

    /**
     * Adds a layer that will give some default plot or other.
     */
    public void addDefaultLayer() {
        dfltFormAct_.actionPerformed( null );
    }

    /**
     * Attempts to add a specified layer to this control.
     *
     * @param   lcmd  layer specification
     */
    public void addLayer( LayerCommand lcmd ) {
        FormControl fc = createFormControl( lcmd.getPlotter() );
        if ( fc != null ) {
            FormStylePanel stylePanel = fc.getStylePanel();
            if ( stylePanel != null ) {
                getSubsetStack().setSelected( lcmd.getRowSubset(), true );
                stylePanel.setGlobalConfig( lcmd.getConfig() );
                formStack_.addControl( fc );
            }
        }
        else {
            logger_.warning( "Failed to add layer " + lcmd );
        }
    }

    /**
     * Creates a new form control for controlling a given plotter type.
     *
     * @param  plotter  plotter
     * @return  new control
     */
    private FormControl createFormControl( Plotter plotter ) {

        /* If it's a mode plotter, try to set up a ModeFormControl with
         * all the other associated modes present, but the relevant one
         * currently selected.
         * Note this currently only works if the supplied plotter is one of
         * the ones supplied at construction time.  This can be problematic,
         * since ModePlotter is not currently declared with @Equality,
         * so you could have a plotter which is equivalent but not actually
         * equal.  In that case you'll end up falling through this part,
         * and get a SimpleFormControl instead. */
        if ( plotter instanceof ModePlotter ) {
            ModePlotter mPlotter = (ModePlotter) plotter;
            ModePlotter.Mode mode = mPlotter.getMode();
            ModePlotter.Form form = mPlotter.getForm();
            List<ModePlotter> mPlotterList = modePlotterMap_.get( form );
            if ( mPlotterList != null ) {
                for ( ModePlotter mp1 : mPlotterList ) {
                    if ( mp1.getMode().equals( mode ) ) {
                        ModePlotter[] mPlotters =
                            mPlotterList.toArray( new ModePlotter[ 0 ] );
                        ModeFormControl fc = createModeFormControl( mPlotters );
                        fc.setMode( mp1.getMode() );
                        return fc;
                    }
                }
            }
        }

        /* If that didn't work (not a mode plotter, or couldn't assemble
         * an appropriate ModeFormControl for some other reason) return a
         * simple form control instead. */
        return createSimpleFormControl( plotter );
    }

    /**
     * Creates a simple form control for a given plotter.
     *
     * @param   plotter  plotter
     * @return   new form control configured for the current table
     */
    private FormControl createSimpleFormControl( Plotter plotter ) {

        /* The coordinate entry fields in the form control should be
         * those which are not requested by the (common to several forms)
         * PositionCoordPanel.  What we do below is to get a list of
         * the Extra coords required by the current plotter and remove
         * from that list those appearing in the position coord panel.
         * It would be more correct to assemble a list of all the
         * coordinates required by this plotter (positional as well as
         * extra ones) and use that minus the common ones - however in
         * all current cases the positional ones are always common,
         * so currently this works. */
        List<Coord> extraCoords =
            new ArrayList<Coord>( Arrays.asList( plotter.getCoordGroup()
                                                        .getExtraCoords() ) );
        extraCoords.removeAll( Arrays.asList( getPositionCoordPanel()
                                             .getCoords() ) );
        FormControl fc =
            new SimpleFormControl( baseConfigger_, plotter,
                                   extraCoords.toArray( new Coord[ 0 ] ) );
        fc.setTable( getTopcatModel(), getSubsetManager(), getSubsetStack() );
        return fc;
    }

    /**
     * Creates a mode form control for a number of plotters.
     *
     * @param  plotters  list of mode plotters with the same form
     * @return   new form control configured for the current table
     */
    private ModeFormControl createModeFormControl( ModePlotter[] plotters ) {
        ModeFormControl fc =
            new ModeFormControl( baseConfigger_, plotters, subsetKeys_ );
        fc.setTable( getTopcatModel(), getSubsetManager(), getSubsetStack() );
        return fc;
    }

    /**
     * Action to add a form control for a non-modal plotter.
     */
    private class SingleFormAction extends AbstractAction {
        private final Plotter plotter_;

        /**
         * Constructor.
         *
         * @param  plotter   object that generates plot layers
         */
        public SingleFormAction( Plotter plotter ) {
            super( plotter.getPlotterName(),
                   ResourceIcon.toAddIcon( plotter.getPlotterIcon() ) );
            putValue( SHORT_DESCRIPTION,
                      "Add new " + plotter.getPlotterName() + " form" );
            plotter_ = plotter;
        }

        public void actionPerformed( ActionEvent evt ) {
            formStack_.addControl( createSimpleFormControl( plotter_ ) );
        }
    }

    /**
     * Action to add a form control for a family of plotters with a common
     * form and a selection of modes.
     */
    private class ModeFormAction extends AbstractAction {
        private final ModePlotter[] plotters_;

        /**
         * Constructor.
         *
         * @param  plotters   family of plotters which all have the same form
         *                    but different modes
         * @param  form   common form
         */
        public ModeFormAction( ModePlotter[] plotters, ModePlotter.Form form ) {
            super( form.getFormName(),
                   ResourceIcon.toAddIcon( form.getFormIcon() ) );
            putValue( SHORT_DESCRIPTION,
                      "Add new " + form.getFormName() + " form" );
            plotters_ = plotters;
        }

        public void actionPerformed( ActionEvent evt ) {
            formStack_.addControl( createModeFormControl( plotters_ ) );
        }
    }
}
