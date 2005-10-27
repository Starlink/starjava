package uk.ac.starlink.topcat;

import cds.tools.ExtApp;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Enumeration;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonModel;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultButtonModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ListModel;
import javax.swing.TransferHandler;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumnModel;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.gui.PasteLoader;
import uk.ac.starlink.table.gui.TableLoadChooser;
import uk.ac.starlink.topcat.join.MatchWindow;
import uk.ac.starlink.topcat.plot.PlotWindow;
import uk.ac.starlink.util.gui.DragListener;
import uk.ac.starlink.util.gui.ErrorDialog;

/**
 * Main window providing user control of the TOPCAT application.
 * This is a singleton class.
 *
 * @author   Mark Taylor (Starlink)
 * @since    9 Mar 2004
 */
public class ControlWindow extends AuxWindow
                           implements ListSelectionListener, 
                                      ListDataListener,
                                      TableModelListener,
                                      TableColumnModelListener,
                                      TopcatListener {

    private static ControlWindow instance_;
    private static Logger logger_ = Logger.getLogger( "uk.ac.starlink.topcat" );

    private final JList tablesList_;
    private final DefaultListModel tablesModel_;
    private final TableModelListener tableWatcher_ = this;
    private final TopcatListener topcatWatcher_ = this;
    private final ListSelectionListener selectionWatcher_ = this;
    private final ListDataListener tablesWatcher_ = this;
    private final TableColumnModelListener columnWatcher_ = this;
    private final WindowListener windowWatcher_ = new ControlWindowListener();
    private final StarTableOutput taboutput_ = new StarTableOutput();
    private final boolean canWrite_ = Driver.canWrite();
    private final boolean canRead_ = Driver.canRead();
    private final TransferHandler importTransferHandler_ = 
        new ControlTransferHandler( true, false );
    private final TransferHandler exportTransferHandler_ =
        new ControlTransferHandler( false, true );
    private final TransferHandler bothTransferHandler_ =
        new ControlTransferHandler( true, true );
    private final Window window_ = this;
    private final ComboBoxModel dummyComboBoxModel_ =
        new DefaultComboBoxModel();
    private final ButtonModel dummyButtonModel_ = new DefaultButtonModel();
    private StarTableFactory tabfact_ = new StarTableFactory( true );
    private TableLoadChooser loadChooser_;
    private LoadQueryWindow loadWindow_;
    private ConcatWindow concatWindow_;
    private ExtApp extApp_;

    private final JTextField idField_ = new JTextField();
    private final JLabel indexLabel_ = new JLabel();
    private final JLabel locLabel_ = new JLabel();
    private final JLabel nameLabel_ = new JLabel();
    private final JLabel rowsLabel_ = new JLabel();
    private final JLabel colsLabel_ = new JLabel();
    private final JComboBox subsetSelector_ = new JComboBox();
    private final JComboBox sortSelector_ = new JComboBox();
    private final JToggleButton sortSenseButton_ = new UpDownButton();
    private final JButton activatorButton_ = new JButton();

    private final Action readAct_;
    private final Action writeAct_;
    private final Action dupAct_;
    private final Action mirageAct_;
    private final Action removeAct_;
    private final Action concatAct_;
    private final Action logAct_;
    private final Action[] matchActs_;
    private final ShowAction[] showActs_;
    private final ModelViewAction[] viewActs_;

    /**
     * Constructs a new window.
     */
    private ControlWindow() {
        super( "Starlink TOPCAT", null );

        /* Set up a list of the known tables. */
        tablesModel_ = new DefaultListModel();
        tablesList_ = new JList( tablesModel_ );

        /* Watch the list. */
        tablesList_.addListSelectionListener( selectionWatcher_ );
        tablesModel_.addListDataListener( tablesWatcher_ );

        /* Watch the label field. */
        idField_.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                getCurrentModel().setLabel( idField_.getText() );
            }
        } );

        /* Set up a panel displaying table information. */
        InfoStack info = new InfoStack();
        info.setBorder( BorderFactory.createEmptyBorder( 4, 4, 4, 4 ) );
        info.addLine( "Label", idField_ );
        info.addLine( "Location", locLabel_ );
        info.addLine( "Name", nameLabel_ );
        info.addLine( "Rows", rowsLabel_ );
        info.addLine( "Columns", colsLabel_ );
        info.addLine( "Sort Order", new Component[] { sortSenseButton_,
                                                      sortSelector_ } );
        info.addLine( "Row Subset", subsetSelector_ );
        info.addLine( "Activation Action", activatorButton_ );
        activatorButton_.setText( "           " );
        info.fillIn();

        /* Set up a split pane in the main panel. */
        JSplitPane splitter = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT );
        JScrollPane listScroller = new JScrollPane( tablesList_ );
        JScrollPane infoScroller = new JScrollPane( info );
        listScroller.setBorder( makeTitledBorder( "Table List" ) );
        infoScroller.setBorder( makeTitledBorder( "Current Table " +
                                                  "Properties" ) );
        splitter.setLeftComponent( listScroller );
        splitter.setRightComponent( infoScroller );
        splitter.setPreferredSize( new Dimension( 600, 250 ) );
        splitter.setDividerLocation( 192 );
        getMainArea().add( splitter );

        /* Configure drag and drop on the list panel. */
        tablesList_.setDragEnabled( true );
        tablesList_.setTransferHandler( bothTransferHandler_ );

        /* Set up actions. */
        removeAct_ = new ControlAction( "Discard Table", ResourceIcon.DELETE,
                                        "Forget about the current table" );
        readAct_ = new ControlAction( "Load Table", ResourceIcon.LOAD,
                                      "Open a new table" );
        concatAct_ = new ControlAction( "Concatenate Tables",
                                        ResourceIcon.CONCAT,
                                        "Join tables by concatenating them" );
        logAct_ = new ControlAction( "View Log", ResourceIcon.LOG,
                                     "Display the log of events" );
        readAct_.setEnabled( canRead_ );
        logAct_.setEnabled( LogHandler.getInstance() != null );

        dupAct_ = new ExportAction( "Duplicate Table", ResourceIcon.COPY,
                                    "Create a duplicate of the current table" );
        mirageAct_ = new ExportAction( "Export To Mirage", null,
                               "Launch Mirage to display the current table" );
        mirageAct_.setEnabled( MirageHandler.isMirageAvailable() );

        final ModelViewAction viewerAct = 
            new ModelViewWindowAction( "Table Data", ResourceIcon.VIEWER,
                                       "Display table cell data",
                                       TableViewerWindow.class );
        viewActs_ = new ModelViewAction[] {
            viewerAct,
            new ModelViewWindowAction( "Table Parameters", ResourceIcon.PARAMS,
                                       "Display table metadata",
                                       ParameterWindow.class ),
            new ModelViewWindowAction( "Column Info", ResourceIcon.COLUMNS,
                                       "Display column metadata",
                                       ColumnInfoWindow.class ),
            new ModelViewWindowAction( "Row Subsets", ResourceIcon.SUBSETS,
                                       "Display row subsets",
                                       SubsetWindow.class ),
            new ModelViewWindowAction( "Column Statistics", ResourceIcon.STATS,
                                       "Display statistics for each column",
                                       StatsWindow.class ),
            new ModelViewWindowAction( "Plot", ResourceIcon.PLOT,
                                       "Plot table columns",
                                       PlotWindow.class ),
        };
        writeAct_ = new ModelViewAction( "Save Table", ResourceIcon.SAVE,
                                         "Write out the current table" ) {
            public Window createWindow( TopcatModel tcModel ) {
                return new SaveQueryWindow( tcModel, getTableOutput(),
                                            getLoadChooser(),
                                            ControlWindow.this );
            }
        };

        matchActs_ = new Action[] {
            new MatchWindowAction( "Internal Match", ResourceIcon.MATCH1,
                                   "Perform row matching on a single table", 
                                   1 ),
            new MatchWindowAction( "Pair Match", ResourceIcon.MATCH2,
                                   "Create new table by matching rows in " +
                                   "two existing tables", 2 ),
            new MatchWindowAction( "Triple Match", ResourceIcon.MATCHN,
                                   "Create new table by matching rows in " +
                                   "three existing tables", 3 ),
            new MatchWindowAction( "Quadruple Match", ResourceIcon.MATCHN,
                                   "Create new table by matching rows in " +
                                   "four existing tables", 4 ),
        };

        /* Configure the list to try to load a table when you paste 
         * text location into it. */
        MouseListener pasteLoader = new PasteLoader( this ) {
            protected void tableLoaded( StarTable table, String loc ) {
                addTable( table, loc, true );
            }
            public StarTableFactory getTableFactory() {
                return tabfact_;
            }
        };
        tablesList_.addMouseListener( pasteLoader );

        /* Configure load button for mouse actions. */
        JButton readButton = new JButton( readAct_ );
        readButton.setText( null );
        readButton.setTransferHandler( importTransferHandler_ );
        readButton.addMouseListener( pasteLoader );

        /* Bind an action for double-click or Enter key on the list. */
        tablesList_.addMouseListener( new MouseAdapter() {
            public void mouseClicked( MouseEvent evt ) {
                if ( evt.getClickCount() >= 2 ) {
                    ActionEvent aevt = new ActionEvent( evt.getSource(),
                                                        evt.getID(),
                                                        "Display Table" );
                    viewerAct.actionPerformed( aevt );
                }
            }
        } );
        Object actkey = viewerAct.getValue( Action.NAME );
        tablesList_.getInputMap()
                  .put( KeyStroke.getKeyStroke( KeyEvent.VK_ENTER, 0 ), 
                        actkey );
        tablesList_.getActionMap().put( actkey, viewerAct );

        /* Add load/save control buttons to the toolbar. */
        JToolBar toolBar = getToolBar();
        toolBar.setFloatable( true );
        toolBar.add( readButton );
        configureExportSource( toolBar.add( writeAct_ ) );
        configureExportSource( toolBar.add( dupAct_ ) );
        toolBar.addSeparator();

        /* Add table view buttons to the toolbar. */
        for ( int i = 0; i < viewActs_.length; i++ ) {
            toolBar.add( viewActs_[ i ] );
        }
        toolBar.addSeparator();

        /* Add join/match control buttons to the toolbar. */
        toolBar.add( concatAct_ );
        toolBar.add( matchActs_[ 0 ] );
        toolBar.add( matchActs_[ 1 ] );
        toolBar.addSeparator();

        /* Add miscellaneous actions to the toolbar. */
        toolBar.add( MethodWindow.getWindowAction( this, true ) );
        toolBar.addSeparator();

        /* Add actions to the file menu. */
        JMenu fileMenu = getFileMenu();
        int fileMenuPos = 0;
        fileMenu.insert( readAct_, fileMenuPos++ );
        fileMenu.insert( removeAct_, fileMenuPos++ );
        fileMenu.insertSeparator( fileMenuPos++ );
        fileMenu.insert( writeAct_, fileMenuPos++ );
        fileMenu.insert( dupAct_, fileMenuPos++ );
        if ( MirageHandler.isMirageAvailable() ) {
            fileMenu.insert( mirageAct_, fileMenuPos++ );
        }
        fileMenu.insertSeparator( fileMenuPos++ );
        fileMenu.insert( logAct_, fileMenuPos++ );
        fileMenu.insertSeparator( fileMenuPos++ );

        /* Add a menu for the table views. */
        JMenu viewMenu = new JMenu( "TableViews" );
        viewMenu.setMnemonic( KeyEvent.VK_V );
        for ( int i = 0; i < viewActs_.length; i++ ) {
            viewMenu.add( viewActs_[ i ] );
        }
        getJMenuBar().add( viewMenu );

        /* Add a menu for window management. */
        JMenu winMenu = new JMenu( "Windows" );
        winMenu.setMnemonic( KeyEvent.VK_W );
        showActs_ = makeShowActions();
        for ( int i = 0; i < showActs_.length; i++ ) {
            winMenu.add( showActs_[ i ] );
        }
        getJMenuBar().add( winMenu );

        /* Add a menu for table joining. */
        JMenu joinMenu = new JMenu( "Joins" );
        joinMenu.setMnemonic( KeyEvent.VK_J );
        joinMenu.add( concatAct_ );
        for ( int i = 0; i < matchActs_.length; i++ ) {
            joinMenu.add( matchActs_[ i ] );
        }
        getJMenuBar().add( joinMenu );

        /* Mark this window as top-level. */
        setCloseIsExit();

        /* Add help information. */
        addHelp( "ControlWindow" );

        /* Make closing this window equivalent to closing the application,
         * since without it the application can't be controlled. */
        setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE );
        addWindowListener( windowWatcher_ );

        /* Display the window. */
        updateInfo();
        updateControls();
        pack();
        setVisible( true );
    }

    /**
     * Returns the sole instance of this window.
     * 
     * @return  instance of control window
     */
    public static ControlWindow getInstance() {
        if ( instance_ == null ) {
            instance_ = new ControlWindow();
        }
        return instance_;
    }

    /**
     * Returns a CDS-style ExtApp object which can be used for control
     * of this control window instance.
     *
     * @return   external application object for TOPCAT
     */
    public ExtApp getExtApp() {
        if ( extApp_ == null ) {
            extApp_ = new TopcatExtApp( this );
        }
        return extApp_;
    }

    /**
     * Adds a table to this windows list.
     * Following this, a user will be able to do TOPCATty things with
     * the table in question from this control window.
     *
     * @param  table  the table to add
     * @param  location  location string indicating the provenance of
     *         <tt>table</tt> - preferably a URL or filename or something
     * @param  select  true iff the newly-added table should become the
     *         currently selected table
     * @return the newly-created TopcatModel object corresponding to 
     *         <tt>table</tt>
     */
    public TopcatModel addTable( StarTable table, String location,
                                 boolean select ) {
        TopcatModel tcModel = new TopcatModel( table, location, this );
        tcModel.setLabel( shorten( location ) );
        tablesModel_.addElement( tcModel );
        logger_.info( "Load new table " + tcModel + " from " + location );
        if ( select || tablesList_.getSelectedValue() == null ) {
            tablesList_.setSelectedValue( tcModel, true );
        }
        if ( select ) {
            makeVisible();
        }
        return tcModel;
    }

    /**
     * Removes an entry from the table list.
     *
     * @param  model  the table entry to remove
     */
    public void removeTable( TopcatModel model ) {
        if ( tablesModel_.contains( model ) ) {
            setViewsVisible( model, false );
            tablesList_.clearSelection();
            tablesModel_.removeElement( model );
        }
    }

    /**
     * Returns the TopcatModel corresponding to the currently selected table.
     *
     * @return  selected model
     */
    private TopcatModel getCurrentModel() {
        return (TopcatModel) tablesList_.getSelectedValue();
    }

    /**
     * Returns the list model which keeps track of which tables are available
     * to the application.
     *
     * @return  list model of {@link TopcatModel} objects
     */
    public ListModel getTablesListModel() {
        return tablesModel_;
    }

    /**
     * Returns a dialog used for loading new tables.
     *
     * @return  a table load window
     */
    public LoadQueryWindow getLoader() {
        if ( loadWindow_ == null ) {
            loadWindow_ = new LoadQueryWindow( tabfact_, getLoadChooser(),
                                               this ) {
                protected void performLoading( StarTable st, String loc ) {
                    addTable( st, loc, true );
                }
            };
        }
        return loadWindow_;
    }

    /**
     * Returns a dialog used for doing table concatenation.
     *
     * @return  concatenation window
     */
    public ConcatWindow getConcatWindow() {
        if ( concatWindow_ == null ) {
            concatWindow_ = new ConcatWindow( this );
        }
        return concatWindow_;
    }

    /**
     * Returns the table factory used by this window.
     *
     * @return  table factory
     */
    public StarTableFactory getTableFactory() {
        return tabfact_;
    }

    /**
     * Returns the table output manager used by this window.
     *
     * @return  table outputter
     */
    public StarTableOutput getTableOutput() {
        return taboutput_;
    }

    /**
     * Sets the table factory used by this window.
     *
     * @param   tabfact   table factory
     */
    public void setTableFactory( StarTableFactory tabfact ) {
        tabfact_ = tabfact;
    }

    /**
     * Returns the dialogue used for loading tables used by this application.
     *
     * @return  load chooser dialogue
     */
    public TableLoadChooser getLoadChooser() {
        if ( loadChooser_ == null ) {
            loadChooser_ = new TableLoadChooser( getTableFactory() );
        }
        return loadChooser_;
    }

    /**
     * Sets the dialogue used for loading tables used by this application.
     *
     * @param  chooser  load chooser dialogue
     */
    public void setLoadChooser( TableLoadChooser chooser ) {
        loadChooser_ = chooser;
    }

    /**
     * Reveals or hides any existing view windows for a given table.
     *
     * @param  tcModel  table to affect
     * @param  visible   true to reveal, false to hide
     */
    public void setViewsVisible( TopcatModel tcModel, boolean visible ) {
        for ( int i = 0; i < viewActs_.length; i++ ) {
            viewActs_[ i ].setViewVisible( tcModel, visible );
        }
    }

    /**
     * Shuts down TOPCAT.  According to whether or not it is running 
     * standalone, this may invoke {@link java.lang.System#exit} itself,
     * or it may just attempt to get rid of all the windows associated
     * with the TOPCAT application.  In the latter case, the JVM should
     * survive.
     *
     * @param   confirm  whether to seek confirmation from the user
     * @return  whether shutdown took place.  If the user aborted the
     *          exit, then <tt>false</tt> will be returned.  If the exit
     *          did happen, then either <tt>true</tt> will be returned
     *          or (standalone case) there will be no return.
     */
    public boolean exit( boolean confirm ) {
        if ( ( ! confirm ) || 
             tablesModel_.getSize() == 0 ||
             confirm( "Shut down TOPCAT", "Confirm Exit" ) ) {
            removeWindowListener( windowWatcher_ );
            if ( Driver.isStandalone() ) {
                System.exit( 0 );
            }
            else {
                for ( Enumeration en = tablesModel_.elements();
                      en.hasMoreElements(); ) {
                    removeTable( (TopcatModel) en.nextElement() );
                }
                dispose();
            }
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Updates the information displayed in the info panel of this window.
     * This should be invoked at least when the selection is changed.
     */
    private void updateInfo() {
        TopcatModel tcModel = getCurrentModel();
        boolean hasModel = tcModel != null;

        /* Ensure the info panel is up to date. */
        if ( hasModel ) {
            StarTable dataModel = tcModel.getDataModel();
            ViewerTableModel viewModel = tcModel.getViewModel();
            TableColumnModel columnModel = tcModel.getColumnModel();
            long totCols = dataModel.getColumnCount();
            long totRows = dataModel.getRowCount();
            int visCols = columnModel.getColumnCount();
            int visRows = viewModel.getRowCount();
            String loc = tcModel.getLocation();
            String name = dataModel.getName();
            Activator activator = tcModel.getActivator();

            idField_.setText( tcModel.getLabel() );
            indexLabel_.setText( tcModel.getID() + ": " );
            locLabel_.setText( loc );
            nameLabel_.setText( loc.equals( name ) ? null : name );
            rowsLabel_.setText( totRows + 
                                ( ( visRows == totRows ) 
                                            ? ""
                                            : " (" + visRows + " apparent)" ) );
            colsLabel_.setText( totCols +
                                ( ( visCols == totCols )
                                            ? ""
                                            : " (" + visCols + " apparent)" ) );

            sortSelector_.setModel( tcModel.getSortSelectionModel() );
            subsetSelector_.setModel( tcModel.getSubsetSelectionModel() );
            sortSenseButton_.setModel( tcModel.getSortSenseModel() );
            activatorButton_.setAction( tcModel.getActivationAction() );
            activatorButton_.setText( activator.toString() );
        }
        else {
            idField_.setText( null );
            indexLabel_.setText( "0: " );
            locLabel_.setText( null );
            nameLabel_.setText( null );
            rowsLabel_.setText( null );
            colsLabel_.setText( null );

            sortSelector_.setModel( dummyComboBoxModel_ );
            subsetSelector_.setModel( dummyComboBoxModel_ );
            sortSenseButton_.setModel( dummyButtonModel_ );
            activatorButton_.setModel( dummyButtonModel_ );
        }

        /* Make sure that the actions which relate to a particular table model
         * are up to date. */
        writeAct_.setEnabled( hasModel && canWrite_ );
        dupAct_.setEnabled( hasModel );
        mirageAct_.setEnabled( hasModel );
        removeAct_.setEnabled( hasModel );
        subsetSelector_.setEnabled( hasModel );
        sortSelector_.setEnabled( hasModel );
        sortSenseButton_.setEnabled( hasModel );
        for ( int i = 0; i < viewActs_.length; i++ ) {
            viewActs_[ i ].setEnabled( hasModel );
        }
        for ( int i = 0; i < showActs_.length; i++ ) {
            ShowAction sact = showActs_[ i ];
            if ( sact.selEffect != sact.otherEffect ) {
                sact.setEnabled( hasModel );
            }
        }
        idField_.setEnabled( hasModel );
        idField_.setEditable( hasModel );
    }

    /**
     * Updates some window state.  This should be called at least when the
     * list of tables changes.
     */
    public void updateControls() {
        boolean hasTables = tablesModel_.getSize() > 0;
        concatAct_.setEnabled( hasTables );
        for ( int i = 0; i < matchActs_.length; i++ ) {
            matchActs_[ i ].setEnabled( hasTables );
        }
    }

    /*
     * Listener implementations.
     */

    public void valueChanged( ListSelectionEvent evt ) {
        int watchCount = 0;
        for ( int i = evt.getFirstIndex(); i <= evt.getLastIndex(); i++ ) {
            if ( i < tablesModel_.size() ) {
                TopcatModel tcModel = (TopcatModel) 
                                      tablesModel_.getElementAt( i );
                ViewerTableModel viewModel = tcModel.getViewModel();
                TableColumnModel columnModel = tcModel.getColumnModel();
                if ( tablesList_.isSelectedIndex( i ) ) {
                    watchCount++;
                    tcModel.addTopcatListener( topcatWatcher_ );
                    viewModel.addTableModelListener( tableWatcher_ );
                    columnModel.addColumnModelListener( columnWatcher_ );
                }
                else {
                    tcModel.removeTopcatListener( topcatWatcher_ );
                    viewModel.removeTableModelListener( tableWatcher_ );
                    columnModel.removeColumnModelListener( columnWatcher_ );
                }
            }
        }
        assert watchCount <= 1;
        updateInfo();
    }

    public void tableChanged( TableModelEvent evt ) {
        updateInfo();
    }

    public void modelChanged( TopcatEvent evt ) {
        switch ( evt.getCode() ) {

            /* Model label has changed. */
            case TopcatEvent.LABEL:
                updateInfo();
                int index = tablesModel_.indexOf( evt.getModel() );

                /* If the model is represented in the list panel (presumably
                 * it is), update the list panel by firing events on the 
                 * list model's listeners. */
                if ( index >= 0 ) {
                    ListDataEvent event = 
                        new ListDataEvent( this, ListDataEvent.CONTENTS_CHANGED,
                                           index, index );
                    ListDataListener[] listWatchers = 
                        tablesModel_.getListDataListeners();
                    for ( int i = 0; i < listWatchers.length; i++ ) {
                        listWatchers[ i ].contentsChanged( event );
                    }
                }
                break;

            /* Activator has changed. */
            case TopcatEvent.ACTIVATOR:
                updateInfo();
                break;
        }
    }

    public void columnAdded( TableColumnModelEvent evt ) {
        updateInfo();
    }
    public void columnRemoved( TableColumnModelEvent evt ) {
        updateInfo();
    }
    public void columnMarginChanged( ChangeEvent evt ) {}
    public void columnMoved( TableColumnModelEvent evt ) {}
    public void columnSelectionChanged( ListSelectionEvent evt ) {}

    public void contentsChanged( ListDataEvent evt ) {
        updateControls();
    }
    public void intervalAdded( ListDataEvent evt ) {
        updateControls();
    }
    public void intervalRemoved( ListDataEvent evt ) {
        updateControls();
    }


    /**
     * General control actions.
     */
    private class ControlAction extends BasicAction {
        
        ControlAction( String name, Icon icon, String shortdesc ) {
            super( name, icon, shortdesc );
        }

        public void actionPerformed( ActionEvent evt ) {
            if ( this == readAct_ ) {
                getLoader().makeVisible();
            }
            else if ( this == removeAct_ ) {
                TopcatModel tcModel = getCurrentModel();
                if ( confirm( "Remove table \"" + tcModel + "\" from list?",
                              "Confirm Remove" ) ) {
                    removeTable( tcModel );
                }
            }
            else if ( this == concatAct_ ) {
                getConcatWindow().makeVisible();
            }
            else if ( this == logAct_ ) {
                LogHandler.getInstance().showWindow( ControlWindow.this );
            }
            else {
                throw new AssertionError();
            }
        }
    }

    /**
     * Actions for popping up match windows.
     */
    private class MatchWindowAction extends BasicAction {

        private final int nTable;
        private MatchWindow matchWin;

        MatchWindowAction( String name, Icon icon, String shortdesc,
                           int nTable ) {
            super( name, icon, shortdesc );
            this.nTable = nTable;
        }

        public void actionPerformed( ActionEvent evt ) {
            if ( matchWin == null ) {
                matchWin = new MatchWindow( ControlWindow.this, nTable );
            }
            matchWin.makeVisible();
        }
    }

    /**
     * Class for showing a certain kind of window which applies to TopcatModels.
     */
    private abstract class ModelViewAction extends BasicAction {

        private final Map modelWindows_ = new WeakHashMap();

        /**
         * Constructor. 
         *
         * @param   name  action name
         * @param   icon  action icon
         * @param   shortdesc  action short description
         */
        ModelViewAction( String name, Icon icon, String shortdesc ) {
            super( name, icon, shortdesc );
        }

        /**
         * Displays this action's window type for the currently selected
         * table.
         */
        public void actionPerformed( ActionEvent evt ) {
            TopcatModel tcModel = getCurrentModel();
            if ( tcModel == null ) {
                return;
            }
            Window window = (Window) modelWindows_.get( tcModel );
            if ( window == null ) {
                window = createWindow( tcModel );
                modelWindows_.put( tcModel, window );
            }
            window.setVisible( true );
        }

        /**
         * Ensures that this action's window for a given table is visible
         * or invisible, if it exists.
         * The window will not be created specially however. 
         *
         * @param  tcModel  table to reveal for
         * @param  visible  true to reveal, false to hide
         */
        public void setViewVisible( TopcatModel tcModel, boolean visible ) {
            Window window = (Window) modelWindows_.get( tcModel );
            if ( window != null ) {
                if ( visible ) {
                    window.setVisible( true );
                }
                else {
                    window.dispose();
                }
            }
        }

        /**
         * Creates this action's window for a given table.
         *
         * @param  tcModel  table the window will apply to
         */
        protected abstract Window createWindow( TopcatModel tcModel );
    }

    /**
     * ModelViewAction class for actions which pop up a TopcatViewWindow.
     */
    private class ModelViewWindowAction extends ModelViewAction {

        TopcatViewWindow window_;
        final Constructor constructor_;

        /**
         * Constructor.
         *
         * @param  name  action name
         * @param  icon  action icon
         * @param  shortdesc  action short description
         * @param  winClass  TopcatViewWindow class - must have a
         *         constructor that takes (TopcatModel,Component).
         */
        ModelViewWindowAction( String name, Icon icon, String shortdesc,
                               Class winClass ) {
            super( name, icon, shortdesc );
            if ( ! TopcatViewWindow.class.isAssignableFrom( winClass ) ) {
                throw new IllegalArgumentException();
            }
            try {
                constructor_ = winClass.getConstructor( new Class[] {
                    TopcatModel.class, Component.class,
                } );
            }
            catch ( NoSuchMethodException e ) {
                throw (IllegalArgumentException)
                      new IllegalArgumentException( "No suitable constructor" )
                     .initCause( e );
            }
        }

        protected Window createWindow( TopcatModel tcModel ) {
            try {
                Object[] args = new Object[] { tcModel, ControlWindow.this };
                return (TopcatViewWindow) constructor_.newInstance( args );
            }
            catch ( Exception e ) {
                throw new RuntimeException( "Window creation failed???", e );
            }
        }
    }

    /**
     * Implementation of actions relating to hiding/revealing view windows
     * associated with some or all of the tables.
     */
    private class ShowAction extends BasicAction {

        final WindowEffect selEffect;
        final WindowEffect otherEffect;

        /**
         * Constructs a new action with particular effects for the selected
         * table and the others.
         *
         * @param  name  action name
         * @param  shortdesc  action short description
         * @param  selEffect  effect of action on the selected table
         *         (one of HIDE, REVEAL, NOOP)
         * @param  otherEffect  effect of action on the unselected tables
         *         (one of HIDE, REVEAL, NOOP)
         */
        ShowAction( String name, String shortdesc, WindowEffect selEffect, 
                    WindowEffect otherEffect ) {
            super( name, null, shortdesc );
            this.selEffect = selEffect;
            this.otherEffect = otherEffect;
        }

        public void actionPerformed( ActionEvent evt ) {
            int ntab = tablesModel_.getSize();
            for ( int i = 0; i < ntab; i++ ) {
                boolean isSelected = tablesList_.isSelectedIndex( i );
                TopcatModel tcModel = 
                    (TopcatModel) tablesModel_.getElementAt( i );
                Object effect = isSelected ? selEffect : otherEffect;
                if ( effect == WindowEffect.HIDE ) {
                    setViewsVisible( tcModel, false );
                }
                else if ( effect == WindowEffect.REVEAL ) {
                    setViewsVisible( tcModel, true );
                }
            }
        }
    }

    /**
     * Enumeration for use with ShowAction class.
     */
    private static class WindowEffect {
        final static WindowEffect HIDE = new WindowEffect();
        final static WindowEffect REVEAL = new WindowEffect();
        final static WindowEffect NOOP = null;
        private WindowEffect() {
        }
    }

    /**
     * Returns an array of actions concerned with hiding or revealing 
     * various table view windows.
     */
    private ShowAction[] makeShowActions() {
        return new ShowAction[] {
            new ShowAction( "Show Selected Views Only",
                            "Show viewer windows for selected table only",
                            WindowEffect.REVEAL, WindowEffect.HIDE ),
            new ShowAction( "Show Selected Views",
                            "Show viewer windows for selected table",
                            WindowEffect.REVEAL, WindowEffect.NOOP ),
            new ShowAction( "Show All Views",
                            "Show viewer windows of all tables",
                            WindowEffect.REVEAL, WindowEffect.REVEAL ),
            new ShowAction( "Hide Unselected Views",
                            "Hide viewer windows for tables except " +
                            "selected one",
                            WindowEffect.NOOP, WindowEffect.HIDE ),
            new ShowAction( "Hide Selected Views",
                            "Hide viewer windows for selected table",
                            WindowEffect.HIDE, WindowEffect.NOOP ),
            new ShowAction( "Hide All Views",
                            "Hide viewer windows for all tables",
                            WindowEffect.HIDE, WindowEffect.HIDE ),
        };
    }

    /**
     * Sets up a component so that it will act as a drag source for 
     * drag'n'drop actions, picking up the currently selected
     * table.
     *
     * @param  comp  the component to configure as an export source
     */
    private void configureExportSource( JComponent comp ) {
        MouseInputAdapter dragListener = new DragListener();
        comp.addMouseMotionListener( dragListener );
        comp.addMouseListener( dragListener );
        comp.setTransferHandler( exportTransferHandler_ );
    }

    /**
     * Utility method to turn a location string into a shorter version 
     * by stripping directory information etc.
     *
     * @param  label  original label
     * @return  possibly shortened version
     */
    private static String shorten( String label ) {
        int sindex = label.lastIndexOf( '/' );
        if ( sindex < 0 || sindex == label.length() - 1 ) {
            sindex = label.lastIndexOf( '\\' );
        }
        if ( sindex < 0 || sindex == label.length() - 1 ) {
            sindex = label.lastIndexOf( ':' );
        }
        if ( sindex > 0 && sindex < label.length() - 1 ) {
            label = label.substring( sindex + 1 );
        }
        return label;
    }

    /**
     * Actions which correspond to output of a table.
     */
    private class ExportAction extends BasicAction {

        ExportAction( String name, Icon icon, String shortdesc ) {
            super( name, icon, shortdesc );
        }

        public void actionPerformed( ActionEvent evt ) {
            TopcatModel tcModel = getCurrentModel();
            assert tcModel != null : "Action should be disabled!";
            StarTable table = tcModel.getApparentStarTable();
            if ( this == dupAct_ ) {
                addTable( table, "Copy of " + tcModel.getID(), true );
            }
            else if ( this == mirageAct_ ) {
                assert MirageHandler.isMirageAvailable();
                try {
                    MirageHandler.invokeMirage( table, null );
                }
                catch ( Exception e ) {
                    ErrorDialog.showError( window_, "Mirage Error", e );
                }
            }
        }
    }

    /**
     * Drag and drop handler class.
     */
    private class ControlTransferHandler extends TransferHandler {
        private boolean imprt;
        private boolean export;

        /**
         * Constructs a new TransferHandler.
         *
         * @param  imprt   whether handler should accept dropped tables
         * @param  export  whether handler should act as a drag source
         */
        ControlTransferHandler( boolean imprt, boolean export ) {
            this.imprt = imprt;
            this.export = export;
        }
        public int getSourceActions( JComponent comp ) {
            return ( export && getCurrentModel() != null ) ? COPY : NONE;
        }
        public boolean canImport( JComponent comp, DataFlavor[] flavs ) {
            return imprt && tabfact_.canImport( flavs );
        }
        public Icon getVisualRepresentation() {
            return ResourceIcon.TABLE;
        }
        protected Transferable createTransferable( JComponent comp ) {
            return taboutput_.transferStarTable( getCurrentModel()
                                                .getApparentStarTable() );
        }
        public boolean importData( JComponent comp, Transferable trans ) {
            StarTable table;
            try {
                table = tabfact_.makeStarTable( trans );
                if ( table == null ) {
                    return false;
                }
            }
            catch ( IOException e ) {
                ErrorDialog.showError( window_, "Drop Error", e,
                                       "Table drop operation failed" );
                return false;
            }
            try {
                table = tabfact_.randomTable( table );
                String loc = table.getName();
                loc = loc == null ? "dropped" : loc;
                addTable( table, loc, true );
                return true;
            }
            catch ( IOException e ) {
                ErrorDialog.showError( window_, "I/O Error", e,
                                       "Can't randomise table" );
                return false;
            }
        }
    }

    /**
     * Ensures that closing the control window is equivalent to shutting
     * down the application.
     */
    private class ControlWindowListener extends WindowAdapter {
        public void windowClosing( WindowEvent evt ) {
            exit( true );
        }
        public void windowClosed( WindowEvent evt ) {
            if ( ! exit( true ) ) {
                setVisible( true );
            }
        }
    }

    /**
     * Layout handler for info window.  GridBagLayouts are so horrible 
     * it's easiest to write this in its own class.
     */
    private static class InfoStack extends JPanel {
        GridBagLayout layer = new GridBagLayout();
        GridBagConstraints c1 = new GridBagConstraints();
        GridBagConstraints c2 = new GridBagConstraints();

        InfoStack() {
            setLayout( layer );
            c1.gridx = 0;
            c1.ipadx = 2;
            c1.ipady = 2;
            c1.anchor = GridBagConstraints.EAST;

            c2.gridx = 1;
            c2.ipadx = 2;
            c2.weightx = 1.0;
            c2.fill = GridBagConstraints.NONE;
            c2.gridwidth = GridBagConstraints.REMAINDER;
            c2.anchor = GridBagConstraints.WEST;
        }

        void addLine( String name, JComponent comp ) {
            c1.gridy++;
            c2.gridy++;

            addItem( new JLabel( name + ": " ), c1 );

            GridBagConstraints c2c = (GridBagConstraints) c2.clone();
            if ( comp instanceof JTextField ) {
                c2c.fill = GridBagConstraints.HORIZONTAL;
            }
            addItem( comp, c2c );
        }

        void addLine( String name, Component[] comps ) {
            c1.gridy++;
            c2.gridy++;

            addItem( new JLabel( name + ": " ), c1 );

            GridBagConstraints c2c = (GridBagConstraints) c2.clone();
            c2c.gridwidth = 1;
            c2c.weightx = 0.0;
            c2c.ipadx = 10;
            for ( int i = 0; i < comps.length; i++ ) {
                addItem( comps[ i ], c2c );
                c2c.gridx++;
            }
        }

        void addItem( Component comp, GridBagConstraints c ) {
            layer.setConstraints( comp, c );
            add( comp );
        }

        void fillIn() {
            c1.gridy++;
            Component filler = new JPanel();
            c1.weighty = 1.0;
            layer.setConstraints( filler, c1 );
            add( filler );
        }
    }

}
