package uk.ac.starlink.topcat.plot2;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.topcat.TopcatListener;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.Specifier;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.layer.HandleArrayForm;
import uk.ac.starlink.ttools.plot2.layer.HistogramPlotter;
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
    private final ConfigKey<?>[] subsetKeys_;
    private final ControlStack formStack_;
    private final ControlStackModel formStackModel_;
    private final List<Plotter<?>> singlePlotterList_;
    private final Map<ModePlotter.Form,List<ModePlotter<?>>> modePlotterMap_;
    private final List<Action> dfltFormActs_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat.plot2" );
    private static final String[] HINT_LINES =
        { "Add layers", "using \"Forms\"", "button above", };

    /**
     * Constructor.
     *
     * @param  plotTypeGui   plot type
     * @param  posCoordPanel  panel for entering table and basic positional
     *                        coordinates
     * @param  tablesModel   list of available tables
     * @param  zsel    zone id specifier, may be null for single-zone plots
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
    public MultiFormLayerControl( PlotTypeGui<?,?> plotTypeGui,
                                  PositionCoordPanel posCoordPanel,
                                  ListModel<TopcatModel> tablesModel,
                                  Specifier<ZoneId> zsel, boolean autoPopulate,
                                  NextSupplier nextSupplier,
                                  TopcatListener tcListener, Icon controlIcon,
                                  Plotter<?>[] plotters,
                                  Configger baseConfigger ) {
        super( plotTypeGui, posCoordPanel, tablesModel, zsel, autoPopulate,
               nextSupplier, tcListener, controlIcon );
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
        final Action removeAction =
            formStack_
           .createRemoveAction( "Remove Selected Form",
                                "Delete the currently selected form"
                              + " from the stack" );
        formStack_.addListSelectionListener( new ListSelectionListener() {
            public void valueChanged( ListSelectionEvent evt ) {
                fcHolder.removeAll();
                Control fc = formStack_.getCurrentControl();
                if ( fc != null ) {
                    fcHolder.add( fc.getPanel(), BorderLayout.NORTH );
                    fcHolder.revalidate();
                    fcHolder.repaint();
                }
                String fcTrailer = fc == null
                                 ? ""
                                 : " (" + fc.getControlLabel() + ")";
                removeAction.putValue( Action.NAME,
                                       "Remove Selected Form" + fcTrailer );
                removeAction.putValue( Action.SHORT_DESCRIPTION,
                                       "Delete the currently selected form"
                                     + fcTrailer + " from the stack" );
                removeAction.setEnabled( fc != null );
            }
        } );
        JScrollPane stackScroller = new JScrollPane( formStack_ );
        stackScroller.setHorizontalScrollBarPolicy( JScrollPane
                                                  .HORIZONTAL_SCROLLBAR_NEVER );
        JScrollPane fcScroller = new JScrollPane( fcHolder );
        fcScroller.getVerticalScrollBar().setUnitIncrement( 16 );
        JComponent formPanel = new JPanel( new BorderLayout() );
        JComponent body =  new JPanel( new BorderLayout() );
        JComponent leftBox = new JPanel( new BorderLayout() );
        JComponent fBar = new JPanel( new BorderLayout() );
        fBar.setBorder( BorderFactory.createEmptyBorder( 0, 0, 2, 2 ) );
        leftBox.add( fBar, BorderLayout.NORTH );
        leftBox.add( stackScroller, BorderLayout.WEST );
        body.add( leftBox, BorderLayout.WEST );
        body.add( fcScroller, BorderLayout.CENTER );
        formPanel.add( body, BorderLayout.CENTER );
        formStackModel_.addPlotActionListener( getActionForwarder() );

        /* Fix it so that the hint message is visible at appropriate times,
         * and the form stack is resized according to the width of its
         * current contents. */
        formStackModel_.addListDataListener( new ListDataListener() {
            public void contentsChanged( ListDataEvent evt ) {
                rejig();
            }
            public void intervalRemoved( ListDataEvent evt ) {
                if ( formStackModel_.getSize() == 0 ) {
                    setHintVisible( true );
                }
                rejig();
            }
            public void intervalAdded( ListDataEvent evt ) {
                setHintVisible( false );
                rejig();
            }
            private void rejig() {
                JComponent panel = getPanel();
                panel.revalidate();
                panel.repaint();
            }
        } );
        setHintVisible( true );

        /* Divide up the supplied plotters into those which constitute
         * mode/form families, and standalone ones. */
        singlePlotterList_ = new ArrayList<Plotter<?>>();
        modePlotterMap_ =
            new LinkedHashMap<ModePlotter.Form,List<ModePlotter<?>>>();
        for ( int ip = 0; ip < plotters.length; ip++ ) {
            Plotter<?> plotter = plotters[ ip ];
            if ( plotter instanceof ModePlotter ) {
                ModePlotter<?> modePlotter = (ModePlotter<?>) plotter;
                ModePlotter.Form form = modePlotter.getForm();
                if ( ! modePlotterMap_.containsKey( form ) ) {
                    modePlotterMap_.put( form,
                                         new ArrayList<ModePlotter<?>>() );
                }
                modePlotterMap_.get( form ).add( modePlotter );
            }
            else {
                singlePlotterList_.add( plotter );
            }
        }

        /* Set up an action for each form-family of plotters, and each
         * standalone plotter, and record the first available one
         * to use as the default layer. */
        Action act0 = null;
        List<Action> formActionList = new ArrayList<>();
        for ( ModePlotter.Form form : modePlotterMap_.keySet() ) {
            List<ModePlotter<?>> modePlotters = modePlotterMap_.get( form );
            ModeFormAction act = new ModeFormAction( modePlotters, form );
            formActionList.add( act );
            if ( act0 == null ) {
                act0 = act;
            } 
        }
        for ( Plotter<?> plotter : singlePlotterList_ ) {
            SingleFormAction act = new SingleFormAction( plotter );
            formActionList.add( act );
            if ( act0 == null ) {
                act0 = act;
            }
        }

        /* Special case hacks for additional default forms.
         * Currently there are only two, but if these become more common
         * think about generalising this mechanism. */

        /* Make HandleForm inactive in the case of the XYArray control. */
        List<Action> extraDfltActs = new ArrayList<>();
        for ( ModePlotter.Form form : modePlotterMap_.keySet() ) {
            if ( form instanceof HandleArrayForm ) {
                List<ModePlotter<?>> modePlotters = modePlotterMap_.get( form );
                extraDfltActs.add( new ModeFormAction( modePlotters, form ) {
                    @Override
                    public void actionPerformed( ActionEvent evt ) {
                        Control control = createControl();
                        formStack_.addControl( control );
                        formStack_.setChecked( control, false );
                    }
                } );
            }
        }

        /* Add an additional histogram plot to the list if required. */
        if ( plotTypeGui.hasExtraHistogram() ) {
            for ( Plotter<?> plotter : singlePlotterList_ ) {
                if ( plotter instanceof HistogramPlotter ) {
                    extraDfltActs.add( new SingleFormAction( plotter ) );
                }
            }
        }

        /* Prepare plotters added to control by default. */
        dfltFormActs_ = new ArrayList<>();
        dfltFormActs_.addAll( extraDfltActs );
        dfltFormActs_.add( act0 );

        /* Populate a menu with these actions. */
        final JPopupMenu formMenu = new JPopupMenu( "Forms" );
        for ( Action formAct : formActionList ) {
            formMenu.add( formAct );
        }
        formMenu.addSeparator();
        formMenu.add( removeAction );
        Action fmenuAct = new AbstractAction( "Forms", ResourceIcon.ADD ) {
            public void actionPerformed( ActionEvent evt ) {
                Object src = evt.getSource();
                if ( src instanceof Component ) {
                    Component comp = (Component) src;
                    formMenu.show( comp, 0, 0 );
                }
            }
        };
        JButton fmenuButt = new JButton( fmenuAct );
        fmenuButt.setMargin( new Insets( 2, 2, 2, 2 ) );
        fBar.add( fmenuButt, BorderLayout.NORTH );

        addControlTab( "Form", formPanel, false );
        if ( zsel != null ) {
            addZoneTab( zsel );
        }
    }

    protected FormControl[] getFormControls() {
        int nf = formStackModel_.getSize();
        FormControl[] fcs = new FormControl[ nf ];
        for ( int ifm = 0; ifm < nf; ifm++ ) {
            fcs[ ifm ] = (FormControl) formStackModel_.getControlAt( ifm );
        }
        return fcs;
    }

    protected boolean isControlActive( FormControl fc ) {
        return formStackModel_.isControlActive( fc );
    }

    /**
     * Adds one or more layers that will give some default plot or other.
     */
    public void addDefaultLayers() {
        ActionEvent evt = new ActionEvent( this, 1, "Init" );
        for ( Action act : dfltFormActs_ ) {
            act.actionPerformed( evt );
        }
        setHintVisible( true );
    }

    /**
     * Attempts to add a specified layer to this control.
     *
     * @param   lcmd  layer specification
     */
    public void addLayer( LayerCommand<?> lcmd ) {
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
     * Determines whether the hint about using the Forms button is
     * painted or not.
     *
     * @param  isVisible  true iff hint is to be displayed
     */
    private void setHintVisible( boolean isVisible ) {
        formStack_.setListMessage( isVisible ? HINT_LINES : null );
    }

    /**
     * Creates a new form control for controlling a given plotter type.
     *
     * @param  plotter  plotter
     * @return  new control
     */
    private FormControl createFormControl( Plotter<?> plotter ) {

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
            ModePlotter<?> mPlotter = (ModePlotter<?>) plotter;
            ModePlotter.Mode mode = mPlotter.getMode();
            ModePlotter.Form form = mPlotter.getForm();
            List<ModePlotter<?>> mPlotterList = modePlotterMap_.get( form );
            if ( mPlotterList != null ) {
                for ( ModePlotter<?> mp1 : mPlotterList ) {
                    if ( mp1.getMode().equals( mode ) ) {
                        ModePlotter<?>[] mPlotters =
                            mPlotterList.toArray( new ModePlotter<?>[ 0 ] );
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
    private FormControl createSimpleFormControl( Plotter<?> plotter ) {

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
        PositionCoordPanel posCoordPanel = getPositionCoordPanel();
        Coord[] excludeCoords = PlotUtil.arrayConcat(
            posCoordPanel.getCoords(),
            posCoordPanel.getAdditionalManagedCoords()
        );
        extraCoords.removeAll( Arrays.asList( excludeCoords ) );
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
    private ModeFormControl createModeFormControl( ModePlotter<?>[] plotters ) {
        PositionCoordPanel posCoordPanel = getPositionCoordPanel();
        Coord[] excludeCoords = PlotUtil.arrayConcat(
            posCoordPanel.getCoords(),
            posCoordPanel.getAdditionalManagedCoords()
        );
        ModeFormControl fc =
            new ModeFormControl( baseConfigger_, plotters, subsetKeys_,
                                 excludeCoords );
        fc.setTable( getTopcatModel(), getSubsetManager(), getSubsetStack() );
        return fc;
    }

    /**
     * Action to add a form control for a non-modal plotter.
     */
    private class SingleFormAction extends AbstractAction {
        private final Plotter<?> plotter_;

        /**
         * Constructor.
         *
         * @param  plotter   object that generates plot layers
         */
        public SingleFormAction( Plotter<?> plotter ) {
            super( "Add " + plotter.getPlotterName(),
                   ResourceIcon.toAddIcon( plotter.getPlotterIcon() ) );
            putValue( SHORT_DESCRIPTION,
                      "Add a new " + plotter.getPlotterName() + " plot layer" );
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
        private final ModePlotter<?>[] plotters_;

        /**
         * Constructor.
         *
         * @param  plotters   family of plotters which all have the same form
         *                    but different modes
         * @param  form   common form
         */
        public ModeFormAction( List<ModePlotter<?>> plotters,
                               ModePlotter.Form form ) {
            super( "Add " + form.getFormName(),
                   ResourceIcon.toAddIcon( form.getFormIcon() ) );
            putValue( SHORT_DESCRIPTION,
                      "Add a new " + form.getFormName() + " plot layer" );
            plotters_ = plotters.toArray( new ModePlotter<?>[ 0 ] );
        }

        public void actionPerformed( ActionEvent evt ) {
            formStack_.addControl( createControl() );
        }

        /**
         * Returns the control associated with this action.
         *
         * @return  control
         */
        Control createControl() {
            return createModeFormControl( plotters_ );
        }
    }
}
