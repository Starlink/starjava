package uk.ac.starlink.topcat.activate;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import uk.ac.starlink.topcat.AuxWindow;
import uk.ac.starlink.topcat.BasicAction;
import uk.ac.starlink.topcat.BasicCheckBoxList;
import uk.ac.starlink.topcat.LineBox;
import uk.ac.starlink.topcat.MethodWindow;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.topcat.TopcatEvent;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.util.Loader;

/**
 * A window which allows configuration of activation actions.
 *
 * @author   Mark Taylor
 * @since    21 Dec 2017
 */
public class ActivationWindow extends AuxWindow {

    private final TopcatModel tcModel_;
    private final DefaultListModel listModel_;
    private final BasicCheckBoxList<ActivationEntry> list_;
    private final JComponent configContainer_;
    private final JComponent outputContainer_;
    private final JTextArea descriptionLabel_;
    private final JLabel statusLabel_;
    private final InvokeAllAction invokeAllAct_;
    private final InvokeSingleAction invokeSingleAct_;
    private final ActionListener statusListener_;
    private long currentRow_;
    private ActivationEntry selectedEntry_;
    private String summary_;

    /**
     * Name of a system property ({@value}) that may supply
     * ActivationType implementations to be made available at run-time.
     * The value is a colon-separated list, each item being the name of
     * a class that implements {@link ActivationType} and has a no-arg
     * constructor.
     */
    public static final String ACTIVATORS_PROP = "topcat.activators";

    /**
     * Constructs a new window.
     *
     * @param  tcModel  topcat model
     * @param  parent  parent window, may be used for positioning
     */
    public ActivationWindow( final TopcatModel tcModel, Component parent ) {
        super( tcModel, "Activation Actions", parent );
        tcModel_ = tcModel;
        currentRow_ = 0;
        list_ = new BasicCheckBoxList<ActivationEntry>( ActivationEntry.class,
                                                        true ) {
            @Override
            protected void configureEntryRenderer( JComponent entryRenderer,
                                                   ActivationEntry item,
                                                   int index ) {
                ActivationType atype = item.getType();
                ActivatorConfigurator configurator = item.getConfigurator();
                boolean isEnabled = configurator.getActivator() != null;
                if ( entryRenderer instanceof JLabel ) {
                    JLabel label = (JLabel) entryRenderer;
                    label.setText( atype.getName() );
                    label.setEnabled( isEnabled );
                }
                else {
                    assert false;
                }
            }
            @Override
            public void setChecked( ActivationEntry entry, boolean isChecked ) {
                super.setChecked( entry, isChecked );
                updateActivations();
            }
        };
        listModel_ = (DefaultListModel) list_.getModel();

        list_.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
        JComponent listContainer = new JPanel( new BorderLayout() );
        listContainer.setBorder( makeTitledBorder( "Actions" ) );
        JScrollPane listScroller =
            new JScrollPane( list_,
                             JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                             JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
        listContainer.add( listScroller, BorderLayout.CENTER );

        statusListener_ = new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                updateStatus();
            }
        };

        final JMenu actMenu = new JMenu( "Actions" );
        actMenu.setMnemonic( KeyEvent.VK_A );
        JMenu addMenu = new JMenu( "Add Action" );
        addMenu.setIcon( ResourceIcon.ADD );
        addMenu.setToolTipText( "Add a new Activation Action"
                              + " to the displayed list" );
        actMenu.add( addMenu );

        /* Don't delare this final - it may be out of date after this
         * constructor finishes executing. */
        TopcatModelInfo tinfo0 = TopcatModelInfo.createInfo( tcModel );
        SortedMap<Suitability,List<ActivationType>> atypeMap =
            new TreeMap<Suitability,List<ActivationType>>();
        for ( ActivationType atype : getOptionActivationTypes() ) {
            Suitability suitability = atype.getSuitability( tinfo0 );
            final ActivationType atype0 = atype;
            Action addAct = new AbstractAction( atype.getName() ) {
                public void actionPerformed( ActionEvent evt ) {

                    /* Note the TopcatModelInfo has to be recreated here
                     * rather than using the one created during
                     * ActivationWindow setup, since its state may
                     * become out of date. */
                    TopcatModelInfo tinfo1 =
                        TopcatModelInfo.createInfo( tcModel_ );
                    addActivationEntry( atype0, tinfo1, true );
                }
            };
            addAct.putValue( Action.SHORT_DESCRIPTION, atype.getDescription() );
            boolean isEnabled = false;
            switch ( suitability ) {
                case ACTIVE:
                case SUGGESTED:
                case PRESENT:
                    if ( ! atypeMap.containsKey( suitability ) ) {
                        atypeMap.put( suitability,
                                      new ArrayList<ActivationType>() );
                    }
                    atypeMap.get( suitability ).add( atype );
                    // fall through
                case AVAILABLE:
                    isEnabled = true;
                case DISABLED:
                    addAct.setEnabled( isEnabled );
                    addMenu.add( addAct );
                case NONE:
                    break;
                default:
                    assert false : "Unknown Suitability: " + suitability;
            }
        }
        for ( Suitability s : atypeMap.keySet() ) {
            for ( ActivationType atype : atypeMap.get( s ) ) {
                addActivationEntry( atype, tinfo0, s == Suitability.ACTIVE );
            }
        }
        tinfo0 = null;  // do not keep around for re-use, may go out of date
        final Action removeAct =
                new BasicAction( "Remove Selected Action", ResourceIcon.DELETE,
                                 "Remove currently selected "
                               + "activation action" ) {
            public void actionPerformed( ActionEvent evt ) {
                int isel = list_.getSelectedIndex();
                if ( isel >= 0 ) {
                    listModel_.removeElementAt( isel );
                    isel = Math.min( isel, listModel_.getSize() - 1 );
                    if ( isel >= 0 ) {
                        list_.setSelectedIndex( isel );
                    }
                }
            }
        };
        invokeAllAct_ = new InvokeAllAction();
        invokeSingleAct_ = new InvokeSingleAction();
        list_.addListSelectionListener( new ListSelectionListener() {
            public void valueChanged( ListSelectionEvent evt ) {
                if ( selectedEntry_ != null ) {
                    selectedEntry_.getConfigurator()
                                  .removeActionListener( statusListener_ );
                }
                selectedEntry_ = (ActivationEntry) list_.getSelectedValue();
                if ( selectedEntry_ != null ) {
                    selectedEntry_.getConfigurator()
                                  .addActionListener( statusListener_ );
                }
                removeAct.setEnabled( ! list_.isSelectionEmpty() );
                configContainer_.removeAll();
                outputContainer_.removeAll();
                if ( selectedEntry_ != null ) {
                    configContainer_.add( selectedEntry_.getConfigurator()
                                                        .getPanel() );
                    outputContainer_.add( selectedEntry_.getLogPanel() );
                    descriptionLabel_.setText( selectedEntry_.getType()
                                              .getDescription() );
                }
                else {
                    descriptionLabel_.setText( null );
                }
                invokeSingleAct_.entry_ = selectedEntry_;
                invokeSingleAct_.configure();
                updateStatus();
                configContainer_.revalidate();
                outputContainer_.revalidate();
                descriptionLabel_.revalidate();
                configContainer_.repaint();
                outputContainer_.repaint();
                descriptionLabel_.repaint();
            }
        } );

        listModel_.addListDataListener( new ListDataListener() {
            public void contentsChanged( ListDataEvent evt ) {
                updateActivations();
            }
            public void intervalAdded( ListDataEvent evt ) {
                updateActivations();
            }
            public void intervalRemoved( ListDataEvent evt ) {
                updateActivations();
            }
        } );

        configContainer_ = new JPanel( new BorderLayout() );
        outputContainer_ = new JPanel( new BorderLayout() );
        JScrollPane configScroller = new JScrollPane( configContainer_ );
        configScroller.setBorder( makeTitledBorder( "Configuration" ) );
        JComponent resultsPanel = new JPanel( new BorderLayout() );
        resultsPanel.setBorder( makeTitledBorder( "Results" ) );
        resultsPanel.add( outputContainer_, BorderLayout.CENTER );

        JComponent descriptionPanel = new JPanel( new BorderLayout() );
        descriptionPanel.setBorder( makeTitledBorder( "Description" ) );
        descriptionLabel_ = new JTextArea();
        descriptionLabel_.setOpaque( false );
        descriptionLabel_.setLineWrap( true );
        descriptionLabel_.setWrapStyleWord( true );
        // Necessary otherwise JTextArea never shrinks.
        descriptionLabel_.setMinimumSize( new Dimension( 100, 10 ) );
        descriptionPanel.add( descriptionLabel_, BorderLayout.NORTH );

        JComponent statusPanel = Box.createHorizontalBox();
        statusPanel.setBorder( makeTitledBorder( "Status" ) );
        statusLabel_ = new JLabel();
        JButton invokeButt = new JButton( invokeSingleAct_ );
        invokeButt.setHideActionText( true );
        statusPanel.add( invokeButt );
        statusPanel.add( Box.createHorizontalStrut( 10 ) );
        statusPanel.add( statusLabel_ );

        JComponent entryPanel = new JPanel( new BorderLayout() );
        entryPanel.add( descriptionPanel, BorderLayout.NORTH );
        entryPanel.add( configScroller, BorderLayout.CENTER );
        entryPanel.add( statusPanel, BorderLayout.SOUTH );

        /* Size and place components. */
        listScroller.setPreferredSize( new Dimension( 200, 250 ) );
        entryPanel.setPreferredSize( new Dimension( 400, 250 ) );
        resultsPanel.setPreferredSize( new Dimension( 600, 120 ) );
        JSplitPane hsplitter =
            new JSplitPane( JSplitPane.HORIZONTAL_SPLIT,
                            listContainer, entryPanel );
        JSplitPane vsplitter =
            new JSplitPane( JSplitPane.VERTICAL_SPLIT,
                            hsplitter, resultsPanel );
        vsplitter.setOneTouchExpandable( true );
        JComponent mainPanel = new JPanel( new BorderLayout() );
        mainPanel.add( vsplitter, BorderLayout.CENTER );
        getMainArea().add( mainPanel );

        /* Add menus. */
        getJMenuBar().add( actMenu );
        actMenu.addSeparator();
        actMenu.add( removeAct );
        actMenu.addSeparator();
        actMenu.add( invokeSingleAct_ );
        actMenu.add( invokeAllAct_ );

        /* Add tools. */
        getToolBar().add( invokeSingleAct_ );
        getToolBar().add( invokeAllAct_ );
        getToolBar().add( MethodWindow.getWindowAction( this, true ) );
        getToolBar().addSeparator();

        /* Initialise the state. */
        list_.setSelectedIndex( 0 );

        /* Add help information. */
        addHelp( "ActivationWindow" );
    }

    /**
     * Invokes all the currently configured activation actions for a
     * given table row index.  Calling this method is the correct way
     * to invoke activation actions, since it performs appropriate
     * scheduling and delivers result information to the right place.
     *
     * @param  lrow  row index
     * @param  meta   additional invocation information
     */
    public void activateRow( long lrow, ActivationMeta meta ) {
        currentRow_ = lrow;
        invokeAllAct_.configure();
        invokeSingleAct_.configure();
        updateStatus();
        for ( ActivationEntry entry : list_.getCheckedItems() ) {
            entry.activateRow( lrow, meta );
        }
    }

    /**
     * Returns a list of the currently active Activator objects.
     * Those whose checkboxes are unchecked or which are not configured
     * to perform any action are excluded.
     *
     * @return  list of active activators
     */
    public Activator[] getActiveActivators() {
        List<Activator> activList = new ArrayList<Activator>();
        for ( ActivationEntry entry : list_.getCheckedItems() ) {
            Activator activator = entry.getConfigurator().getActivator();
            if ( activator != null ) {
                activList.add( activator );
            }
        }
        return activList.toArray( new Activator[ 0 ] );
    }

    /**
     * Returns a short text summary of the current activation status.
     *
     * @return   activation summary text
     */
    public String getActivationSummary() {
        int nEnabled = 0;
        int nActive = 0;
        for ( int i = 0; i < listModel_.getSize(); i++ ) {
            ActivationEntry entry =
                (ActivationEntry) listModel_.getElementAt( i );
            if ( entry.getConfigurator().getActivator() != null ) {
                nEnabled++;
                if ( list_.isChecked( entry ) ) {
                    nActive++;
                }
            }
        }
        return "" + nActive + " / " + nEnabled;
    }

    /**
     * Called when the result of the <code>getActivationSummary</code> method
     * may have changed.  Messages the topcat model that this has happened.
     */
    private void updateActivations() {
        String summary = getActivationSummary();
        if ( ! summary.equals( summary_ ) ) {
            summary_ = summary;
            tcModel_.fireModelChanged( TopcatEvent.ACTIVATOR, summary );
        }
    }

    /**
     * Adds a new entry to the list of currently configured/configurable
     * activation actions.
     *
     * @param  atype  activation type
     * @param  tinfo   topcat model information, current at time of call
     * @param  isActive   whether the action should initially be configured
     *                    active
     */
    private void addActivationEntry( ActivationType atype,
                                     TopcatModelInfo tinfo, boolean isActive ) {

        /* Create a new entry. */
        final ActivationEntry entry = new ActivationEntry( atype, tinfo );

        /* Ensure that its appearance in the list is updated promptly
         * when the configuration status changes; the main point of this
         * is so that the enabled-ness (whether it's greyed out or not)
         * is updated in response to user configuration actions. */
        entry.getConfigurator().addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                int ix = getEntryIndex( listModel_, entry );
                if ( ix >= 0 ) {
                    ListDataEvent listEvt =
                        new ListDataEvent( evt.getSource(),
                                           ListDataEvent.CONTENTS_CHANGED, 
                                           ix, ix );
                    for ( ListDataListener l :
                          listModel_.getListDataListeners() ) {
                        l.contentsChanged( listEvt );
                    }
                }
                if ( entry == invokeSingleAct_.entry_ ) {
                    invokeSingleAct_.configure();
                }
            }
        } );

        /* Add it to the list. */
        listModel_.addElement( entry );
        list_.setSelectedIndex( listModel_.getSize() - 1 );
        list_.setChecked( entry, isActive );
    }

    /**
     * Returns the index at which a given entry can be found in a ListModel.
     *
     * @param   model  list model
     * @param   entry  entry to locate
     * @return   index of <code>entry</code> in <code>model</code>,
     *           or -1 if not found
     */
    private int getEntryIndex( ListModel model, ActivationEntry entry ) {
        int n = model.getSize();
        for ( int i = 0; i < n; i++ ) {
            if ( model.getElementAt( i ) == entry ) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Updates the status panel.  Must be called when the selected
     * entry, or the activator or config message it generates,
     * may have changed.
     */
    private void updateStatus() {
        final String message;
        if ( currentRow_ < 0 ) {
            message = "No current row";
        }
        else if ( invokeSingleAct_.isEnabled() ) {
            message = "Invoke now on row " + ( currentRow_ + 1 );
        }
        else if ( selectedEntry_ != null ) {
            message = selectedEntry_.getConfigurator().getConfigMessage();
        }
        else {
            message = null;
        }
        statusLabel_.setText( message );
        statusLabel_.setEnabled( invokeSingleAct_.isEnabled() );
    }

    /**
     * Returns a list of activation types that should be available
     * for selection for a given table.
     *
     * @return  available activation types
     */
    public static ActivationType[] getOptionActivationTypes() {
        List<ActivationType> list = new ArrayList<ActivationType>();
        list.addAll( Loader.getClassInstances( ACTIVATORS_PROP,
                                               ActivationType.class ) );
        list.addAll( Arrays.asList( new ActivationType[] {
            NopActivationType.INSTANCE,
            new TopcatSkyPosActivationType(),
            new SendSkyPosActivationType(),
            new CutoutActivationType(),
        } ) );
        return list.toArray( new ActivationType[ 0 ] );
    }

    /**
     * Invokes the currently selected action for the
     * most recently-activated row.
     */
    private class InvokeSingleAction extends AbstractAction {
        private final ActivationMeta meta_;
        private ActivationEntry entry_;

        InvokeSingleAction() {
            super( null, ResourceIcon.REACTIVATE );
            meta_ = ActivationMeta.NORMAL;
            configure();
        }

        public void actionPerformed( ActionEvent evt ) {
            if ( entry_ != null && currentRow_ >= 0 ) {
                entry_.activateRow( currentRow_, meta_ );
            }
        }

        /**
         * Configure for current state.  Should be called if anything,
         * especially anything may affect the enabledness, changes.
         */
        public void configure() {
            boolean hasRow = currentRow_ >= 0;
            String rowTxt = hasRow ? " (" + ( currentRow_ + 1 ) + ")" : "";
            putValue( NAME, "Invoke Selected Action on Current Row" + rowTxt );
            putValue( SHORT_DESCRIPTION,
                      "Invoke currently selected action for "
                    + "the most recently selected row" + rowTxt );
            setEnabled( hasRow &&
                        entry_ != null &&
                        entry_.getConfigurator().getActivator() != null );
        }
    }

    /**
     * Invokes all active actions for the most recently-activated row.
     */
    private class InvokeAllAction extends AbstractAction {
        private final ActivationMeta meta_;

        InvokeAllAction() {
            super( null, ResourceIcon.REACTIVATE_ALL );
            meta_ = ActivationMeta.NORMAL;
            configure();
        }

        public void actionPerformed( ActionEvent evt ) {
            if ( currentRow_ >= 0 ) {
                for ( ActivationEntry entry : list_.getCheckedItems() ) {
                    entry.activateRow( currentRow_, meta_ );
                }
            }
        }

        /**
         * Configure for current state.  Should be called if anything,
         * especially anything may affect the enabledness, changes.
         */
        public void configure() {
            boolean hasRow = currentRow_ >= 0;
            String rowTxt = hasRow ? " (" + ( currentRow_ + 1 ) + ")" : "";
            putValue( NAME, "Activate Current Row" + rowTxt );
            putValue( SHORT_DESCRIPTION,
                      "Invoke all configured actions for "
                    + "the most recently selected row" + rowTxt );
            setEnabled( hasRow );
        }
    }
}
