package uk.ac.starlink.topcat.activate;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoundedRangeModel;
import javax.swing.Box;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import uk.ac.starlink.topcat.AuxWindow;
import uk.ac.starlink.topcat.BasicAction;
import uk.ac.starlink.topcat.BasicCheckBoxList;
import uk.ac.starlink.topcat.CheckBoxList;
import uk.ac.starlink.topcat.LineBox;
import uk.ac.starlink.topcat.MethodWindow;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.topcat.Safety;
import uk.ac.starlink.topcat.ToggleButtonModel;
import uk.ac.starlink.topcat.TopcatEvent;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.topcat.ViewerTableModel;
import uk.ac.starlink.util.Loader;

/**
 * A window which allows configuration of activation actions.
 *
 * @author   Mark Taylor
 * @since    21 Dec 2017
 */
public class ActivationWindow extends AuxWindow {

    private final TopcatModel tcModel_;
    private final DefaultListModel<ActivationEntry> listModel_;
    private final BasicCheckBoxList<ActivationEntry> list_;
    private final JComponent configContainer_;
    private final JComponent outputContainer_;
    private final JTextArea descriptionLabel_;
    private final JComponent securityContainer_;
    private final JComponent securityPanel_;
    private final JLabel statusLabel_;
    private final InvokeAllAction invokeAllAct_;
    private final InvokeSingleAction invokeSingleAct_;
    private final Action removeAct_;
    private final SingleSequenceAction singleSeqAct_;
    private final AllSequenceAction allSeqAct_;
    private final CancelSequenceAction cancelSeqAct_;
    private final ApproveAllAction approveAllAct_;
    private final SequencePauser seqPauser_;
    private final ActionListener statusListener_;
    private final JProgressBar progBar_;
    private final ThreadFactory seqThreadFact_;
    private ExecutorService sequenceQueue_;
    private long currentRow_;
    private ActivationEntry selectedEntry_;
    private String summary_;
    private static final Color WARNING_FG = Color.RED;
    private static final Color WARNING_BG = new Color( 0xffeeee );
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat.activate" );

    /**
     * Name of a system property ({@value}) that may supply
     * ActivationType implementations to be made available at run-time.
     * The value is a colon-separated list, each item being the name of
     * a class that implements {@link ActivationType} and has a no-arg
     * constructor.
     */
    public static final String ACTIVATORS_PROP = "topcat.activators";

    private static final String ATYPECLAZZ_KEY = "_ActivationType";
    private static final String ISACTIVE_KEY = "_isActive";
    private static final String ISSELECTED_KEY = "_isSelected";
    private static final String TRUE = "true";

    /**
     * Constructs a new window.
     *
     * @param  tcModel  topcat model
     * @param  parent  parent window, may be used for positioning
     */
    @SuppressWarnings({"fallthrough","this-escape"})
    public ActivationWindow( final TopcatModel tcModel, Component parent ) {
        super( tcModel, "Activation Actions", parent );
        tcModel_ = tcModel;
        currentRow_ = 0;
        seqThreadFact_ = new ThreadFactory() {
            public Thread newThread( Runnable r ) {
                Thread thread = new Thread( r, "Activation Sequence" );
                thread.setDaemon( true );
                return thread;
            }
        };
        final Color enabledFg = UIManager.getColor( "Label.foreground" );
        final Color disabledFg =
            UIManager.getColor( "Label.disabledForeground" );
        CheckBoxList.Rendering<ActivationEntry,JLabel> rendering =
                new CheckBoxList.Rendering<ActivationEntry,JLabel>() {
            public JLabel createRendererComponent() {
                return new JLabel();
            }
            public void configureRendererComponent( JLabel label,
                                                    ActivationEntry item,
                                                    int index ) {
                ActivationType atype = item.getType();
                ActivatorConfigurator configurator = item.getConfigurator();
                boolean isEnabled = configurator.getActivator() != null;
                boolean isChecked = list_.isChecked( item );
                label.setText( atype.getName() );
                label.setEnabled( isEnabled );
                final Color fg;
                if ( isEnabled ) {
                    fg = item.isBlocked() && isChecked ? WARNING_FG
                                                       : enabledFg;
                }
                else {
                    fg = disabledFg;
                }
                label.setForeground( fg );
            }
        };
        list_ = new BasicCheckBoxList<ActivationEntry>( true, rendering ) {
            @Override
            public void setChecked( ActivationEntry entry, boolean isChecked ) {
                super.setChecked( entry, isChecked );
                updateActivations();
            }
        };
        listModel_ = list_.getModel();

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

        /* The add item action is really only suitable for a menu item,
         * but it's important to have it on display, since otherwise
         * users might not realise that you can add to this list.
         * So provide a toolbar action with a popup menu instead.
         * It's slightly clunky to do that, but too bad. */
        String addName = "Add Action";
        String addDescrip = "Add a new Activation Action to the displayed list";
        JMenu addMenu = new JMenu( addName );
        addMenu.setIcon( ResourceIcon.ADD );
        addMenu.setToolTipText( addDescrip );
        actMenu.add( addMenu );
        final JPopupMenu addPopupMenu = new JPopupMenu( addName );
        Action addPopupAct =
                new BasicAction( addName, ResourceIcon.ADD, addDescrip ) {
            public void actionPerformed( ActionEvent evt ) {
                Object src = evt.getSource();
                if ( src instanceof Component ) {
                    Component comp = (Component) src;
                    addPopupMenu.show( comp, 0, comp.getHeight() );
                }
            }
        };

        /* Don't delare this final - it may be out of date after this
         * constructor finishes executing. */
        TopcatModelInfo tinfo0 = TopcatModelInfo.createInfo( tcModel );
        SortedMap<Suitability,List<ActivationType>> atypeMap =
            new TreeMap<Suitability,List<ActivationType>>();
        List<Action> addMenuActions = new ArrayList<Action>();
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
                    addMenuActions.add( addAct );
                case NONE:
                    break;
                default:
                    assert false : "Unknown Suitability: " + suitability;
            }
        }

        /* Add the relevant actions to the displayed list.  These go in
         * rough order of interest - the ones most likely to be useful
         * are near the top. */
        for ( Suitability s : atypeMap.keySet() ) {
            for ( ActivationType atype : atypeMap.get( s ) ) {
                addActivationEntry( atype, tinfo0, s == Suitability.ACTIVE );
            }
        }

        /* Add insertion actions for all the actions to the Add menus.
         * Put them in alphabetical order. */
        Collections.sort( addMenuActions, new Comparator<Action>() {
            public int compare( Action a1, Action a2 ) {
                return ((String) a1.getValue( Action.NAME ))
                      .compareTo( (String) a2.getValue( Action.NAME ) );
            }
        } );
        for ( Action addAct : addMenuActions ) {
            addMenu.add( addAct );
            addPopupMenu.add( addAct );
        }

        /* Add some more actions. */
        tinfo0 = null;  // do not keep around for re-use, may go out of date
        removeAct_ = new BasicAction( "Remove Selected Action",
                                      ResourceIcon.DELETE,
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
        final Action removeInactiveAct =
                new BasicAction( "Remove Inactive Actions",
                                 ResourceIcon.DELETE_INACTIVE,
                                 "Remove all inactive (unchecked) actions"
                               + " from the list" ) {
            public void actionPerformed( ActionEvent evt ) {
                for ( ActivationEntry entry : list_.getItems() ) {
                    if ( ! list_.isChecked( entry ) ) {
                        listModel_.removeElement( entry );
                    }
                }
            }
        };
        invokeAllAct_ = new InvokeAllAction();
        invokeSingleAct_ = new InvokeSingleAction();
        singleSeqAct_ = new SingleSequenceAction();
        allSeqAct_ = new AllSequenceAction();
        cancelSeqAct_ = new CancelSequenceAction();
        approveAllAct_ = new ApproveAllAction();
        seqPauser_ = new SequencePauser();
        ToggleButtonModel pauseModel = seqPauser_.model_;

        list_.addListSelectionListener( new ListSelectionListener() {
            public void valueChanged( ListSelectionEvent evt ) {
                updateSelection();
            }
        } );

        list_.addListDataListener( new ListDataListener() {
             public void contentsChanged( ListDataEvent evt ) {
                 listChanged();
             }
             public void intervalAdded( ListDataEvent evt ) {
                 listChanged();
             }
             public void intervalRemoved( ListDataEvent evt ) {
                 listChanged();
             }
             private void listChanged() {
                 updateActivations();
                 invokeAllAct_.configure();
                 allSeqAct_.configure();
             }
        } );

        configContainer_ = new JPanel( new BorderLayout() );
        outputContainer_ = new JPanel( new BorderLayout() );
        JScrollPane configScroller = new JScrollPane( configContainer_ );
        configScroller.setBorder( makeTitledBorder( "Configuration" ) );
        JComponent resultsPanel = new JPanel( new BorderLayout() );
        resultsPanel.setBorder( makeTitledBorder( "Results" ) );
        resultsPanel.add( outputContainer_, BorderLayout.CENTER );

        securityContainer_ = new JPanel( new BorderLayout() );
        Action approveAct = new AbstractAction( "Approve" ) {
            public void actionPerformed( ActionEvent evt ) {
                unblockSelected();
            }
        };
        Action deleteAct = new AbstractAction( "Delete" ) {
            public void actionPerformed( ActionEvent evt ) {
                removeAct_.actionPerformed( evt );
            }
        };
        securityPanel_ = Box.createVerticalBox();
        Border securityBorder =
            BorderFactory
           .createTitledBorder( BorderFactory.createLineBorder( WARNING_FG ),
                                "Security", TitledBorder.LEADING,
                                TitledBorder.TOP, null, WARNING_FG );
        securityPanel_.setBorder( securityBorder );
        securityPanel_.setBackground( WARNING_BG );
        securityPanel_.setOpaque( true );
        JTextArea securityLabel = new JTextArea();
        securityLabel.setEditable( false );
        securityLabel.setOpaque( false );
        securityLabel.setLineWrap( true );
        securityLabel.setWrapStyleWord( true );
        securityLabel.setForeground( WARNING_FG );
        securityLabel.setText( new StringBuffer()
            .append( "This action, loaded from external configuration, " )
            .append( "may potentially contain insecure code. " )
            .append( "Please review the configuration and approve " )
            .append( "if you are happy with it." )
            .toString() );
        JComponent sbuttLine = Box.createHorizontalBox();
        sbuttLine.add( Box.createHorizontalGlue() );
        sbuttLine.add( new JButton( approveAct ) );
        sbuttLine.add( Box.createHorizontalStrut( 10 ) );
        sbuttLine.add( new JButton( deleteAct ) );
        securityPanel_.add( securityLabel );
        securityPanel_.add( sbuttLine );

        JComponent descriptionPanel = new JPanel( new BorderLayout() );
        descriptionPanel.setBorder( makeTitledBorder( "Description" ) );
        descriptionLabel_ = new JTextArea();
        descriptionLabel_.setEditable( false );
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
        entryPanel.add( securityContainer_, BorderLayout.NORTH );
        JComponent normalPanel = new JPanel( new BorderLayout() );
        normalPanel.add( descriptionPanel, BorderLayout.NORTH );
        normalPanel.add( configScroller, BorderLayout.CENTER );
        normalPanel.add( statusPanel, BorderLayout.SOUTH );
        entryPanel.add( normalPanel, BorderLayout.CENTER );

        /* Size and place components. */
        listScroller.setPreferredSize( new Dimension( 200, 300 ) );
        entryPanel.setPreferredSize( new Dimension( 400, 300 ) );
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
        progBar_ = placeProgressBar();
        getMainArea().add( mainPanel );

        /* Add menus. */
        getJMenuBar().add( actMenu );
        actMenu.add( removeAct_ );
        actMenu.add( removeInactiveAct );
        actMenu.add( approveAllAct_ );
        actMenu.addSeparator();
        actMenu.add( invokeSingleAct_ );
        actMenu.add( invokeAllAct_ );
        actMenu.addSeparator();
        actMenu.add( singleSeqAct_ );
        actMenu.add( allSeqAct_ );
        actMenu.add( pauseModel.createMenuItem() );
        actMenu.add( cancelSeqAct_ );

        /* Add tools. */
        JToolBar toolbar = getToolBar();
        toolbar.add( addPopupAct );
        toolbar.add( removeAct_ );
        toolbar.add( removeInactiveAct );
        toolbar.addSeparator();
        toolbar.add( invokeSingleAct_ );
        toolbar.add( invokeAllAct_ );
        toolbar.addSeparator();
        toolbar.add( singleSeqAct_ );
        toolbar.add( allSeqAct_ );
        toolbar.add( pauseModel.createToolbarButton() );
        toolbar.add( cancelSeqAct_ );
        toolbar.addSeparator();
        toolbar.add( MethodWindow.getWindowAction( this, true ) );

        /* Initialise the state. */
        list_.setSelectedIndex( 0 );

        /* Add help information. */
        addHelp( "ActivationWindow" );
    }

    /**
     * Returns the currently selected ActivationEntry.
     * May be null.
     *
     * @return selected entry
     */
    public ActivationEntry getSelectedEntry() {
        return list_.getSelectedValue();
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
        if ( checkAllUnblocked( list_.getCheckedItems() ) ) {
            for ( ActivationEntry entry : list_.getCheckedItems() ) {
                entry.activateRowAsync( lrow, meta );
            }
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
            ActivationEntry entry = listModel_.getElementAt( i );
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
     * Returns the state of this window in a form that is easily
     * serialized but can be fed back to another instance of this class
     * with the same table to restore the interesting parts of the state.
     *
     * @return  activation state object
     */
    public List<Map<String,String>> getActivationState() {
        List<Map<String,String>> list = new ArrayList<Map<String,String>>();
        ActivationEntry selected = getSelectedEntry();
        for ( ActivationEntry entry : list_.getItems() ) {
            Map<String,String> map = new LinkedHashMap<String,String>();
            ActivationType type = entry.getType();
            boolean isActive = list_.isChecked( entry );
            boolean isSelected = entry.equals( selected );
            ConfigState astate = entry.getConfigurator().getState();
            map.put( ATYPECLAZZ_KEY, type.getClass().getName() );
            if ( isActive ) {
                map.put( ISACTIVE_KEY, TRUE );
            }
            map.putAll( astate.getMap() );
            if ( isSelected ) {
                map.put( ISSELECTED_KEY, TRUE );
            }
            list.add( map );
        }
        return list;
    }

    /**
     * Updates the state of this window to match state stored from a
     * previous instance.  The supplied state is expected to apply to
     * a table (TopcatModel) that matches the one owned by this window
     * in relevant respects (for instance column list).
     *
     * @param  stateList  activation state object
     */
    public void setActivationState( List<Map<String,String>> stateList ) {
        if ( stateList == null || stateList.size() == 0 ) {
            return;
        }
        listModel_.removeAllElements();
        TopcatModelInfo tinfo = TopcatModelInfo.createInfo( tcModel_ );
        TypeFinder typeFinder = new TypeFinder();
        ActivationEntry selected = null;
        for ( Map<String,String> map : stateList ) {
            String clazzName = map.get( ATYPECLAZZ_KEY );
            ActivationType atype = typeFinder.findType( clazzName );
            if ( atype != null ) {
                boolean isActive = TRUE.equals( map.get( ISACTIVE_KEY ) );
                boolean isSelected = TRUE.equals( map.get( ISSELECTED_KEY ) );
                ActivationEntry entry =
                    addActivationEntry( atype, tinfo, isActive );
                ActivatorConfigurator config = entry.getConfigurator();
                config.setState( new ConfigState( map ) );

                /* Any potentially unsafe actions which come configured ready
                 * to work from this external source are marked blocked,
                 * which means the user must explicitly approve them before
                 * they will be invoked.  The thinking is that
                 * a bad person may have prepared a session file such that
                 * when a third party loads it, it executes harmful code.
                 * Actions which the user has prepared from scratch are
                 * not considered to be risky in this way. */
                if ( config.getSafety() != Safety.SAFE ) {
                    entry.setBlocked( true );
                }
                list_.setChecked( entry, isActive );
                if ( isSelected ) {
                    selected = entry;
                }
                else if ( selected == null && isActive ) {
                    selected = entry;
                }
            }
        }
        if ( selected == null && listModel_.getSize() == 0 ) {
            selected = listModel_.getElementAt( 0 );
        }
        list_.setSelectedValue( selected, true );
        updateSelection();
        approveAllAct_.configure();
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
     * Updates the currently active row in the topcat model served
     * by this activation window.  In most cases, it's not necessary
     * to call this, since the activation actions defined here will
     * have been invoked by a row being activated (made active)
     * elsewhere in the application.  But for the sequences initiated
     * here, the rest of the application doesn't automatically know the
     * active row has changed, so this method should be called to
     * pass back that information during sequence operation.
     *
     * <p>Note this method does not (and must not, on pain of infinite
     * recursion) trigger the results of an activation action as defined
     * by this window.
     *
     * @param  lrow  new active row
     */
    private void updateActiveRow( long lrow ) {
        tcModel_.fireModelChanged( TopcatEvent.ROW, Long.valueOf( lrow ) );
    }

    /**
     * Adds a new entry to the list of currently configured/configurable
     * activation actions.
     *
     * @param  atype  activation type
     * @param  tinfo   topcat model information, current at time of call
     * @param  isActive   whether the action should initially be configured
     *                    active
     * @return  added entry
     */
    private ActivationEntry addActivationEntry( ActivationType atype,
                                                TopcatModelInfo tinfo,
                                                boolean isActive ) {

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
                if ( entry == getSelectedEntry() ) {
                    invokeSingleAct_.configure();
                    singleSeqAct_.configure();
                }
            }
        } );

        /* Add it to the list. */
        listModel_.addElement( entry );
        list_.setSelectedIndex( listModel_.getSize() - 1 );
        list_.setChecked( entry, isActive );
        return entry;
    }

    /**
     * Returns the index at which a given entry can be found in a ListModel.
     *
     * @param   model  list model
     * @param   entry  entry to locate
     * @return   index of <code>entry</code> in <code>model</code>,
     *           or -1 if not found
     */
    private int getEntryIndex( ListModel<ActivationEntry> model,
                               ActivationEntry entry ) {
        int n = model.getSize();
        for ( int i = 0; i < n; i++ ) {
            if ( model.getElementAt( i ) == entry ) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Updates the status panel when the selected entry or significant
     * characteristics of it may have changed.
     */
    private void updateSelection() {
        if ( selectedEntry_ != null ) {
            selectedEntry_.getConfigurator()
                          .removeActionListener( statusListener_ );
        }
        selectedEntry_ = getSelectedEntry();
        if ( selectedEntry_ != null ) {
            selectedEntry_.getConfigurator()
                          .addActionListener( statusListener_ );
        }
        removeAct_.setEnabled( ! list_.isSelectionEmpty() );
        configContainer_.removeAll();
        outputContainer_.removeAll();
        securityContainer_.removeAll();
        if ( selectedEntry_ != null ) {
            configContainer_.add( selectedEntry_.getConfigurator().getPanel() );
            outputContainer_.add( selectedEntry_.getLogPanel() );
            descriptionLabel_.setText( selectedEntry_.getType()
                                      .getDescription() );
            if ( selectedEntry_.isBlocked() ) {
                securityContainer_.add( securityPanel_ );
            }
        }
        else {
            descriptionLabel_.setText( null );
        }
        invokeSingleAct_.entry_ = selectedEntry_;
        invokeSingleAct_.configure();
        singleSeqAct_.configure();
        updateStatus();
        configContainer_.revalidate();
        outputContainer_.revalidate();
        descriptionLabel_.revalidate();
        securityContainer_.revalidate();
        configContainer_.repaint();
        outputContainer_.repaint();
        descriptionLabel_.repaint();
        securityContainer_.repaint();
    }

    /**
     * Updates the status panel when the activator or config message
     * it generates may have changed.
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
     * Sets the ExecutorService which is used to invoke sequences of
     * actions (one for each table row).  To keep things sane,
     * and in view of the fact we only have one progress bar,
     * only one of these queues is allowed to be present and active at
     * any one time (the invocation actions which would cause
     * creation of a new one are disabled while any other is running).
     * To install a new queue, or to signal that an existing one is
     * finished with, this method should be called.
     * Calling it with a null value will ensure that any existing queue
     * is shut down.
     *
     * <p>This method must be invoked from the EDT.
     *
     * @param   queue  new sequence activation queue;
     *          use null to indicate that an existing queue is finished with
     */
    private void setSequenceQueue( ExecutorService queue ) {
        progBar_.setModel( new DefaultBoundedRangeModel() );
        if ( sequenceQueue_ != null ) {
            sequenceQueue_.shutdownNow();
        }
        sequenceQueue_ = queue;
        singleSeqAct_.configure();
        allSeqAct_.configure();
        cancelSeqAct_.configure();
        seqPauser_.configure();
    }

    /**
     * Sets the currently selected ActivationEntry to unblocked.
     */
    private void unblockSelected() {
        ActivationEntry entry = selectedEntry_;
        if ( entry != null && entry.isBlocked() ) {
            entry.setBlocked( false );
            securityContainer_.removeAll();
            securityContainer_.revalidate();
            securityContainer_.repaint();
        }
        list_.repaint();
    }

    /**
     * Indicates whether a given ActivationEntry is blocked from use.
     * If it unblocked, true is returned.
     * If it is blocked, then this window is made visible in such a way that
     * the user can see the issue, and false is returned.
     *
     * @param  entry   entry to test
     * @return   true iff activation is unblocked
     */
    private boolean checkUnblocked( ActivationEntry entry ) {
        if ( entry == null || ! entry.isBlocked() ) {
            return true;
        }
        else {
            list_.setSelectedValue( entry, true );
            makeVisible();
            toFront();
            return false;
        }
    }

    /**
     * Indicates whether all the given ActivationEntries are unblocked for use.
     * If they are all unblocked, true is returned.
     * If any is blocked, then this window is made visible in such a way that
     * the user can see the issue, and false is returned.
     * 
     * @param  entries  entries to test
     * @return   true iff activation is unblocked for all
     */
    private boolean checkAllUnblocked( List<ActivationEntry> entries ) {
        for ( ActivationEntry entry : entries ) {
            if ( ! checkUnblocked( entry ) ) {
                return false;
            }
        }
        return true;
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

        /* These should generally have no-arg constructors,
         * since instances might need to be created dynamically
         * when restoring state from its serialized form.
         * Failing that, there should only be one instance of each
         * distinct class in the list.
         * Otherwise, some state restoration might not work well. */
        list.addAll( Arrays.asList( new ActivationType[] {
            NopActivationType.INSTANCE,
            new TopcatSkyPosActivationType(),
            new SendSkyPosActivationType(),
            new ViewHips2fitsActivationType(),
            new SendHips2fitsActivationType(),
            new InvokeDatalinkActivationType(),
            new ViewDatalinkActivationType(),
            new ViewImageActivationType(),
            new RegionViewImageActivationType(),
            new ServiceActivationType(),
            new LoadTableActivationType(),
            new PlotTableActivationType(),
            new SendTableActivationType(),
            new SendImageActivationType(),
            new SendSpectrumActivationType(),
            new CutoutActivationType(),
            new DownloadActivationType(),
            new BrowserActivationType(),
            new DelayActivationType(),
            new JelActivationType(),
            new ShellActivationType(),
            new SendCustomActivationType(),
            new SendIndexActivationType( true ),
        } ) );
        return list.toArray( new ActivationType[ 0 ] );
    }

    /**
     * Manages pausing of a programmed sequence of activation actions.
     * The main job of this class is to mediate between activities on
     * the Event Dispatch Thread whose sequence is ensured by the
     * single-threaded nature of the GUI and a lock object that may
     * be accessed from different threads.
     */
    private class SequencePauser {
        private final Object lock_;
        private final ToggleButtonModel model_;
        private boolean isPaused_;
        
        SequencePauser() {
            lock_ = this;
            model_ = new ToggleButtonModel( "Pause Sequence",
                                            ResourceIcon.PAUSE_SEQ,
                                            "Pause (or unpause) a running "
                                          + "sequence of actions" );
            model_.setSelected( false );
            model_.addChangeListener( new ChangeListener() {
                public void stateChanged( ChangeEvent evt ) {
                    boolean isSelected = model_.isSelected();
                    synchronized ( lock_ ) {
                        if ( isPaused_ != isSelected ) {
                            isPaused_ = isSelected;
                            lock_.notifyAll();
                        }
                    }
                }
            } );
            configure();
        }

        /**
         * Will block until this object's button model is no longer
         * in the paused state.
         */
        public void waitIfPaused() throws InterruptedException {
            synchronized ( lock_ ) {
                while ( isPaused_ ) {
                    lock_.wait();
                }
            }
        }

        /** 
         * Configure for current state.  Should be called if anything,
         * especially anything that may affect the enabledness, changes.
         */
        public void configure() {
            model_.setEnabled( sequenceQueue_ != null );
            if ( sequenceQueue_ == null ) {
                model_.setSelected( false );
            }
        }
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
            if ( entry_ != null && currentRow_ >= 0 &&
                 checkUnblocked( entry_ ) ) {
                entry_.activateRowAsync( currentRow_, meta_ );
            }
        }

        /**
         * Configure for current state.  Should be called if anything,
         * especially anything may affect the enabledness, changes.
         */
        public void configure() {
            boolean hasRow = currentRow_ >= 0;
            String rowTxt = hasRow ? " (" + ( currentRow_ + 1 ) + ")" : "";
            putValue( NAME, "Invoke selected action on current row" + rowTxt );
            putValue( SHORT_DESCRIPTION,
                      "Perform the currently selected action"
                    + " on the most recently selected row" + rowTxt );
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
                List<ActivationEntry> entries = list_.getCheckedItems();
                if ( checkAllUnblocked( entries ) ) {
                    for ( ActivationEntry entry : entries ) {
                        entry.activateRowAsync( currentRow_, meta_ );
                    }
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
            putValue( NAME, "Activate current row" + rowTxt );
            putValue( SHORT_DESCRIPTION,
                      "Perform all active (checked) actions"
                    + " on the most recently selected row" + rowTxt );
            setEnabled( hasRow && getActiveActivators().length > 0 );
        }
    }

    /**
     * Action that cancels the operation of any multi-row activation
     * sequence that is under way.
     */
    private class CancelSequenceAction extends BasicAction {
        CancelSequenceAction() {
            super( "Cancel sequence", ResourceIcon.CANCEL_SEQ,
                   "Stop a running sequence of actions" );
            configure();
        }
        public void actionPerformed( ActionEvent evt ) {
            setSequenceQueue( null );
        }

        /**
         * Should be called if anything affecting enabledness changes.
         */
        public void configure() {
            setEnabled( sequenceQueue_ != null );
        }
    }

    /**
     * Action that ensures all entries are marked unblocked
     * without further user interaction.
     */
    private class ApproveAllAction extends BasicAction {
        ApproveAllAction() {
            super( "Approve All Actions", ResourceIcon.APPROVE_ALL,
                   "Approve all potentially unsafe actions; use with care" );
            configure();
        }
        public void actionPerformed( ActionEvent evt ) {
            if ( selectedEntry_ != null && selectedEntry_.isBlocked() ) {
                unblockSelected();
            }
            for ( ActivationEntry entry : getBlockedEntries() ) {
                entry.setBlocked( false );
            }
            list_.repaint();
            configure();
        }

        /**
         * Should be called if anything affecting enabledness changes.
         */
        public void configure() {
            setEnabled( ! getBlockedEntries().isEmpty() );
        }

        /**
         * Returns a list of all the activation entries currently in the list
         * that are marked blocked.
         *
         * @return  blocked list
         */
        private List<ActivationEntry> getBlockedEntries() {
            List<ActivationEntry> list = new ArrayList<ActivationEntry>();
            for ( ActivationEntry entry : list_.getItems() ) {
                if ( entry.isBlocked() ) {
                    list.add( entry );
                }
            }
            return list;
        }
    }

    /**
     * Action that invokes the currently selected activation action
     * on every row of the current apparent table.
     */
    private class SingleSequenceAction extends BasicAction {
        private final ActivationMeta meta_;

        SingleSequenceAction() {
            super( "Invoke selected action on all rows",
                   ResourceIcon.ACTIVATE_SEQ,
                   "Perform the currently selected action"
                 + " on every row in the current subset in turn" );
            meta_ = ActivationMeta.NORMAL;
            configure();
        }

        public void actionPerformed( ActionEvent evt ) {
            final ActivationEntry entry = getSelectedEntry();
            if ( entry != null && checkUnblocked( entry ) ) {
                final Activator activator =
                    entry.getConfigurator().getActivator();
                if ( activator != null ) {
                    final ExecutorService queue =
                        Executors.newSingleThreadExecutor( seqThreadFact_ );
                    setSequenceQueue( queue );
                    seqThreadFact_.newThread( new Runnable() {
                        public void run() {
                            try {
                                runSequence( queue, entry, activator );
                            }
                            finally {
                                SwingUtilities.invokeLater( new Runnable() {
                                    public void run() {
                                        if ( queue == sequenceQueue_ ) {
                                            setSequenceQueue( null );
                                        }
                                    }
                                } );
                            }
                        }
                    } ).start();
                }
            }
        }

        /**
         * Does the work of invoking a given activator for each row
         * of this window's table.
         *
         * @param  queue  execution queue
         * @param  entry  activation entry
         * @param  activator  activator
         */
        private void runSequence( final ExecutorService queue,
                                  final ActivationEntry entry,
                                  final Activator activator ) {
            ViewerTableModel viewModel = tcModel_.getViewModel();
            final BoundedRangeModel progModel =
                new DefaultBoundedRangeModel( 0, 0, 0,
                                              viewModel.getRowCount() );
            SwingUtilities.invokeLater( new Runnable() {
                public void run() {
                    progBar_.setModel( progModel );
                }
            } );
            try {
                int ir = 0;
                for ( Iterator<Long> it = viewModel.getRowIndexIterator();
                      it.hasNext() && ! queue.isShutdown(); ) {
                    seqPauser_.waitIfPaused();
                    final long lrow = it.next().longValue();
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            updateActiveRow( lrow );
                        }
                    } );
                    queue.submit( new Runnable() {
                        public void run() {
                            entry.activateRowSync( activator, lrow, meta_ );
                        }
                    } ).get();  // wait for completion
                    final int ir0 = ++ir;
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            progModel.setValue( ir0 );
                        }
                    } );
                }
            }
            catch ( InterruptedException e ) {
                // job was cancelled by user (probably)
            }
            catch ( RejectedExecutionException e ) {
                // job was cancelled by user (probably)
            }
            catch ( ExecutionException e ) {
                // Shouldn't really happen.  Could catch this per iteration,
                // but given that there is plenty of error handling already
                // there, a failure here may mean something serious has gone
                // wrong, and is likely to go wrong for every iteration.
                logger_.log( Level.WARNING, "Activation sequence failed: " + e,
                             e );
            }
        }

        /**
         * Should be called if anything affecting enabledness changes.
         */
        public void configure() {
            setEnabled( sequenceQueue_ == null &&
                        selectedEntry_ != null &&
                        selectedEntry_.getConfigurator()
                                      .getActivator() != null );
        }
    }

    /**
     * Action that invokes all the currently active activation actions
     * on every row of the current apparent table.
     */ 
    private class AllSequenceAction extends BasicAction {
        private final ActivationMeta meta_;

        AllSequenceAction() {
            super( "Activate all rows",
                   ResourceIcon.ACTIVATE_SEQ_ALL,
                   "Perform all active (checked) actions"
                 + " on every row in the current subset in turn" );
            meta_ = ActivationMeta.NORMAL;
            configure();
        }

        public void actionPerformed( ActionEvent evt ) {
            List<ActivationEntry> entries = list_.getCheckedItems();
            if ( ! checkAllUnblocked( entries ) ) {
                return;
            }

            /* Prepare a list of the activators to invoke at each row. */
            final Map<ActivationEntry,Activator> activators =
                new LinkedHashMap<ActivationEntry,Activator>();
            for ( ActivationEntry entry : entries ) {
                Activator activator = entry.getConfigurator().getActivator();
                if ( activator != null ) {
                    activators.put( entry, activator );
                }
            }

            /* Call on all rows, and dispose of the queue when done. */
            if ( activators.size() > 0 ) {
                final ExecutorService queue =
                    Executors.newCachedThreadPool( seqThreadFact_ );
                setSequenceQueue( queue );
                seqThreadFact_.newThread( new Runnable() {
                    public void run() {
                        try {
                            runSequence( queue, activators );
                        }
                        finally {
                            SwingUtilities.invokeLater( new Runnable() {
                                public void run() {
                                    if ( queue == sequenceQueue_ ) {
                                        setSequenceQueue( null );
                                    }
                                }
                            } );
                        }
                    }
                } ).start();
            }
        }

        /**
         * Does the work of invoking the supplied activators for each row
         * of this window's table.
         *
         * @param  queue  execution queue
         * @param  activators  list of ActivationEntry,Activator pairs
         */
        private void
                runSequence( final ExecutorService queue,
                             final Map<ActivationEntry,Activator> activators ) {
            ViewerTableModel viewModel = tcModel_.getViewModel();
            final BoundedRangeModel progModel =
                new DefaultBoundedRangeModel( 0, 0, 0,
                                              viewModel.getRowCount() );
            SwingUtilities.invokeLater( new Runnable() {
                public void run() {
                    progBar_.setModel( progModel );
                }
            } );
            try {

                /* Step through each row of the table. */
                int ir = 0;
                for ( Iterator<Long> it = viewModel.getRowIndexIterator();
                      it.hasNext() && ! queue.isShutdown(); ) {
                    seqPauser_.waitIfPaused();
                    final long lrow = it.next().longValue();
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            updateActiveRow( lrow );
                        }
                    } );

                    /* Prepare a list of all the actions to be invoked. */
                    Collection<Callable<Void>> jobs =
                        new ArrayList<Callable<Void>>();
                    for ( Map.Entry<ActivationEntry,Activator> e :
                          activators.entrySet() ) {
                        final ActivationEntry entry = e.getKey();
                        final Activator activator = e.getValue();
                        jobs.add( new Callable<Void>() {
                            public Void call() {
                                entry.activateRowSync( activator, lrow, meta_ );
                                return null;
                            }
                        } );
                    }

                    /* Invoke them all concurrently, waiting until
                     * all are complete before moving on to the next row. */
                    queue.invokeAll( jobs );
                    final int ir0 = ++ir;
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            progModel.setValue( ir0 );
                        }
                    } );
                }
            }
            catch ( InterruptedException e ) {
                // job was cancelled by user (probably)
            }
            catch ( RejectedExecutionException e ) {
                // job was cancelled by user (probably)
            }
            catch ( Throwable e ) {
                logger_.log( Level.WARNING, "Activation sequence failed: " + e,
                             e );
            }
        }

        /**
         * Should be called if enabledness changes.
         */
        public void configure() {
            setEnabled( sequenceQueue_ == null &&
                        getActiveActivators().length > 0 );
        }
    }

    /**
     * Helper class that comes up with an ActivationType instance given
     * its class name.
     */
    private static class TypeFinder {
        final Map<String,ActivationType> typeMap_;

        /**
         * Constructor.
         */
        TypeFinder() {
            typeMap_ = new HashMap<String,ActivationType>();
            for ( ActivationType type : getOptionActivationTypes() ) {
                typeMap_.put( type.getClass().getName(), type );
            }
        }

        /**
         * Obtains an ActivationType with a given class name.
         * This may be one of the ones provided by default by this window,
         * or if not, an attempt is made to load the class and create an
         * instance dynamically (using a no-arg constructor).
         *
         * @param   clazzName  ActivateType class name;
         *                     preferably should have a no-arg constructor
         * @return  activation type instance, or null if it can't be found
         */
        ActivationType findType( String clazzName ) {
            if ( typeMap_.containsKey( clazzName ) ) {
                return typeMap_.get( clazzName );
            }
            try {
                ActivationType atype =
                    (ActivationType) Class.forName( clazzName )
                                          .getDeclaredConstructor()
                                          .newInstance();
                typeMap_.put( atype.getClass().getName(), atype );
                return atype;
            }
            catch ( Throwable e ) {
                logger_.log( Level.WARNING,
                             "Can't restore activation type " + clazzName
                           + " (" + e + ")", e );
                return null;
            }
        }
    }
}
