package uk.ac.starlink.topcat;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
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
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
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
import uk.ac.starlink.table.gui.StarTableSaver;
import uk.ac.starlink.util.gui.DragListener;
import uk.ac.starlink.util.ErrorDialog;

/**
 * Main window providing user control of the TOPCAT application.
 * This is a singleton class.
 *
 * @author   Mark Taylor (Starlink)
 * @since    9 Mar 2004
 */
public class ControlWindow extends AuxWindow
                           implements ListSelectionListener, 
                                      TableModelListener,
                                      TableColumnModelListener,
                                      TopcatListener {

    private static ControlWindow instance;

    private final JList tablesList;
    private final DefaultListModel tablesModel;
    private final TableModelListener tableWatcher = this;
    private final TopcatListener topcatWatcher = this;
    private final ListSelectionListener selectionWatcher = this;
    private final TableColumnModelListener columnWatcher = this;
    private final WindowListener windowWatcher = new ControlWindowListener();
    private final StarTableFactory tabfact = new StarTableFactory( true );
    private final StarTableOutput taboutput = new StarTableOutput();
    private final boolean canWrite = Driver.canWrite();
    private final boolean canRead = Driver.canRead();
    private final TransferHandler importTransferHandler = 
        new ControlTransferHandler( true, false );
    private final TransferHandler exportTransferHandler =
        new ControlTransferHandler( false, true );
    private final TransferHandler bothTransferHandler =
        new ControlTransferHandler( true, true );
    private final Window window = this;
    private final ComboBoxModel dummyComboBoxModel = new DefaultComboBoxModel();
    private final ButtonModel dummyButtonModel = new DefaultButtonModel();
    private LoadQueryWindow loadWindow;
    private StarTableSaver saver;

    private final JTextField idField = new JTextField();
    private final JLabel locLabel = new JLabel();
    private final JLabel nameLabel = new JLabel();
    private final JLabel rowsLabel = new JLabel();
    private final JLabel colsLabel = new JLabel();
    private final JComboBox subsetSelector = new JComboBox();
    private final JComboBox sortSelector = new JComboBox();
    private final JToggleButton sortSenseButton = new UpDownButton();

    private final Action viewerAct;
    private final Action paramAct;
    private final Action colinfoAct;
    private final Action statsAct;
    private final Action subsetAct;
    private final Action plotAct;
    private final Action hideAct;
    private final Action readAct;
    private final Action writeAct;
    private final Action dupAct;
    private final Action mirageAct;
    private final Action removeAct;

    /**
     * Constructs a new window.
     */
    private ControlWindow() {
        super( "Starlink TOPCAT", null );

        /* Set up a list of the known tables. */
        tablesModel = new DefaultListModel();
        tablesList = new JList( tablesModel );

        /* Watch the list. */
        tablesList.addListSelectionListener( selectionWatcher );

        /* Watch the label field. */
        idField.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                getCurrentModel().setLabel( idField.getText() );
            }
        } );

        /* Set up a panel displaying table information. */
        InfoStack info = new InfoStack();
        info.setBorder( BorderFactory.createEmptyBorder( 4, 4, 4, 4 ) );
        info.addLine( "Label", idField );
        info.addLine( "Location", locLabel );
        info.addLine( "Name", nameLabel );
        info.addLine( "Rows", rowsLabel );
        info.addLine( "Columns", colsLabel );
        info.addLine( "Sort Order", new Component[] { sortSenseButton,
                                                      sortSelector } );
        info.addLine( "Row Subset", subsetSelector );
        info.fillIn();

        /* Set up a split pane in the main panel. */
        JSplitPane splitter = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT );
        JScrollPane listScroller = new JScrollPane( tablesList );
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
        tablesList.setDragEnabled( true );
        tablesList.setTransferHandler( bothTransferHandler );

        /* Set up actions. */
        removeAct = new ControlAction( "Remove Table", ResourceIcon.REMOVE,
                                       "Forget about the current table" );
        readAct = new ControlAction( "Load Table", ResourceIcon.LOAD,
                                     "Open a new table" );
        readAct.setEnabled( canRead );

        writeAct = new ExportAction( "Save Table", ResourceIcon.SAVE,
                                     "Write out the current table" );
        dupAct = new ExportAction( "Duplicate Table", ResourceIcon.COPY,
                                   "Create a duplicate of the current table" );
        mirageAct = new ExportAction( "Export To Mirage", null,
                               "Launch Mirage to display the current table" );
        mirageAct.setEnabled( MirageHandler.isMirageAvailable() );

        hideAct = new ModelAction( "Hide Windows", ResourceIcon.HIDE,
                                   "Hide viewer windows" );
        viewerAct = new ModelAction( "Table Browser", ResourceIcon.VIEWER,
                                     "Display table cell data" );
        paramAct = new ModelAction( "Table Parameters", ResourceIcon.PARAMS,
                                    "Display table metadata" );
        colinfoAct = new ModelAction( "Column Info", ResourceIcon.COLUMNS,
                                      "Display column metadata" );
        subsetAct = new ModelAction( "Row Subsets", ResourceIcon.SUBSETS,
                                     "Display row subsets" );
        statsAct = new ModelAction( "Column Statistics", ResourceIcon.STATS,
                                    "Display statistics for each column" );
        plotAct = new ModelAction( "Plot", ResourceIcon.PLOT,
                                   "Plot table columns" );
       
        /* Add general control buttons to the toolbar. */
        JToolBar toolBar = getToolBar();
        toolBar.add( readAct ).setTransferHandler( importTransferHandler );
        toolBar.addSeparator();

        /* Add export buttons to the toolbar. */
        configureExportSource( toolBar.add( writeAct ) );
        configureExportSource( toolBar.add( dupAct ) );
        configureExportSource( toolBar.add( removeAct ) );
        toolBar.addSeparator();

        /* Add table view buttons to the toolbar. */
        toolBar.add( viewerAct );
        toolBar.add( paramAct );
        toolBar.add( colinfoAct );
        toolBar.add( subsetAct );
        toolBar.add( statsAct );
        toolBar.add( plotAct );
        toolBar.addSeparator();
        toolBar.add( hideAct );
        toolBar.addSeparator();

        /* Add help information. */
        addHelp( "ControlWindow" );

        /* Make closing this window equivalent to closing the application,
         * since without it the application can't be controlled. */
        setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE );
        addWindowListener( windowWatcher );

        /* Display the window. */
        updateInfo();
        pack();
        setVisible( true );
    }

    /**
     * Returns the sole instance of this window.
     * 
     * @return  instance of control window
     */
    public static ControlWindow getInstance() {
        if ( instance == null ) {
            instance = new ControlWindow();
        }
        return instance;
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
     */
    public void addTable( StarTable table, String location, boolean select ) {
        TopcatModel tcModel = new TopcatModel( table, location );
        tcModel.setLabel( location );
        tablesModel.addElement( tcModel );
        if ( select || tablesList.getSelectedValue() == null ) {
            tablesList.setSelectedValue( tcModel, true );
        }
        if ( select ) {
            makeVisible();
        }
    }

    /**
     * Removes an entry from the table list.
     *
     * @param  model  the table entry to remove
     */
    public void removeTable( TopcatModel model ) {
        if ( tablesModel.contains( model ) ) {
            model.hideWindows();
            tablesList.clearSelection();
            tablesModel.removeElement( model );
        }
    }

    /**
     * Returns the TopcatModel corresponding to the currently selected table.
     *
     * @return  selected model
     */
    private TopcatModel getCurrentModel() {
        return (TopcatModel) tablesList.getSelectedValue();
    }

    /**
     * Returns a dialog used for loading new tables.
     *
     * @return  a table load window
     */
    public LoadQueryWindow getLoader() {
        if ( loadWindow == null ) {
            loadWindow = new LoadQueryWindow( tabfact, this ) {
                protected void performLoading( StarTable st, String loc ) {
                    addTable( st, loc, true );
                }
            };
            if ( saver != null ) {
                File dir = saver.getFileChooser().getCurrentDirectory();
                loadWindow.getFileChooser().setCurrentDirectory( dir );
            }
        }
        return loadWindow;
    }

    /**
     * Returns a dialog used for saving tables.
     *
     * @return  a table saver
     */
    public StarTableSaver getSaver() {
        if ( saver == null ) {
            File dir = ( loadWindow == null )
                           ? new File( "." )
                           : loadWindow.getFileChooser().getCurrentDirectory();
            saver = new StarTableSaver();
            saver.getFileChooser().setCurrentDirectory( dir );
            saver.setStarTableOutput( taboutput );
        }
        return saver;
    }

    /**
     * Returns the table factory used by this window.
     *
     * @return  table factory
     */
    public StarTableFactory getTableFactory() {
        return tabfact;
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
        if ( ( ! confirm ) || confirm( "Shut down TOPCAT", "Confirm Exit" ) ) {
            removeWindowListener( windowWatcher );
            if ( Driver.isStandalone() ) {
                System.exit( 0 );
            }
            else {
                for ( Enumeration en = tablesModel.elements();
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

            idField.setText( tcModel.getLabel() );
            locLabel.setText( loc );
            nameLabel.setText( loc.equals( name ) ? null : name );
            rowsLabel.setText( totRows + 
                               ( ( visRows == totRows ) 
                                           ? ""
                                           : " (" + visRows + " apparent)" ) );
            colsLabel.setText( totCols +
                               ( ( visCols == totCols )
                                           ? ""
                                           : " (" + visCols + " apparent)" ) );

            sortSelector.setModel( tcModel.getSortSelectionModel() );
            subsetSelector.setModel( tcModel.getSubsetSelectionModel() );
            sortSenseButton.setModel( tcModel.getSortSenseModel() );
        }
        else {
            idField.setText( null );
            locLabel.setText( null );
            nameLabel.setText( null );
            rowsLabel.setText( null );
            colsLabel.setText( null );

            sortSelector.setModel( dummyComboBoxModel );
            subsetSelector.setModel( dummyComboBoxModel );
            sortSenseButton.setModel( dummyButtonModel );
        }

        /* Make sure that the actions which relate to a particular table model
         * are up to date. */
        writeAct.setEnabled( hasModel && canWrite );
        dupAct.setEnabled( hasModel );
        mirageAct.setEnabled( hasModel );
        removeAct.setEnabled( hasModel );
        subsetSelector.setEnabled( hasModel );
        sortSelector.setEnabled( hasModel );
        sortSenseButton.setEnabled( hasModel );
        viewerAct.setEnabled( hasModel );
        paramAct.setEnabled( hasModel );
        colinfoAct.setEnabled( hasModel );
        statsAct.setEnabled( hasModel );
        subsetAct.setEnabled( hasModel );
        plotAct.setEnabled( hasModel );
        hideAct.setEnabled( hasModel );
        idField.setEnabled( hasModel );
        idField.setEditable( hasModel );
    }

    /*
     * Listener implementations.
     */

    public void valueChanged( ListSelectionEvent evt ) {
        int watchCount = 0;
        for ( int i = evt.getFirstIndex(); i <= evt.getLastIndex(); i++ ) {
            TopcatModel tcModel = (TopcatModel) tablesModel.getElementAt( i );
            ViewerTableModel viewModel = tcModel.getViewModel();
            TableColumnModel columnModel = tcModel.getColumnModel();
            if ( tablesList.isSelectedIndex( i ) ) {
                watchCount++;
                tcModel.addTopcatListener( topcatWatcher );
                viewModel.addTableModelListener( tableWatcher );
                columnModel.addColumnModelListener( columnWatcher );
            }
            else {
                tcModel.removeTopcatListener( topcatWatcher );
                viewModel.removeTableModelListener( tableWatcher );
                columnModel.removeColumnModelListener( columnWatcher );
            }
        }
        assert watchCount <= 1;
        updateInfo();
    }

    public void tableChanged( TableModelEvent evt ) {
        updateInfo();
    }

    public void modelChanged( TopcatModel tcModel, int code ) {
        switch ( code ) {

            /* Model label has changed. */
            case TopcatListener.LABEL:
                updateInfo();
                int index = tablesModel.indexOf( tcModel );

                /* If the model is represented in the list panel (presumably
                 * it is), update the list panel by firing events on the 
                 * list model's listeners. */
                if ( index >= 0 ) {
                    ListDataEvent event = 
                        new ListDataEvent( this, ListDataEvent.CONTENTS_CHANGED,
                                           index, index );
                    ListDataListener[] listWatchers = 
                        tablesModel.getListDataListeners();
                    for ( int i = 0; i < listWatchers.length; i++ ) {
                        listWatchers[ i ].contentsChanged( event );
                    }
                }
                break;

            /* Unknown model event?? */
            default:
                assert false;
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


    /**
     * General control actions.
     */
    private class ControlAction extends BasicAction {
        
        ControlAction( String name, Icon icon, String shortdesc ) {
            super( name, icon, shortdesc );
        }

        public void actionPerformed( ActionEvent evt ) {
            if ( this == readAct ) {
                getLoader().loadRandomStarTable( window );
            }
            else if ( this == removeAct ) {
                TopcatModel tcModel = getCurrentModel();
                if ( confirm( "Remove table \"" + tcModel + "\" from list?",
                              "Confirm Remove" ) ) {
                    removeTable( tcModel );
                }
            }
        }
    }

    /**
     * Actions which correspond to actions provided by the TopcatModel objects.
     */
    private class ModelAction extends BasicAction {

        ModelAction( String name, Icon icon, String shortdesc ) {
            super( name, icon, shortdesc );
        }

        public void actionPerformed( ActionEvent evt ) {
            TopcatModel tcModel = getCurrentModel();
            if ( tcModel == null ) {
                assert false;
            }
            Action act;
            if ( this == viewerAct ) {
                act = tcModel.getViewerAction();
            }
            else if ( this == paramAct ) {
                act = tcModel.getParameterAction();
            }
            else if ( this == colinfoAct ) {
                act = tcModel.getColumnInfoAction();
            }
            else if ( this == statsAct ) {
                act = tcModel.getStatsAction();
            }
            else if ( this == subsetAct ) {
                act = tcModel.getSubsetAction();
            }
            else if ( this == plotAct ) {
                act = tcModel.getPlotAction();
            }
            else if ( this == hideAct ) {
                act = tcModel.getHideAction();
            }
            else {
                throw new AssertionError();
            }
            act.actionPerformed( evt );
        }
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
        comp.setTransferHandler( exportTransferHandler );
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
            if ( this == writeAct ) {
                getSaver().saveTable( table, window );
            }
            else if ( this == dupAct ) {
                addTable( table, "Copy of " + tcModel, true );
            }
            else if ( this == mirageAct ) {
                assert MirageHandler.isMirageAvailable();
                try {
                    MirageHandler.invokeMirage( table, null );
                }
                catch ( Exception e ) {
                    ErrorDialog.showError( e, "Error launching Mirage", 
                                           window );
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
            return imprt && tabfact.canImport( flavs );
        }
        public Icon getVisualRepresentation() {
            return ResourceIcon.TABLE;
        }
        protected Transferable createTransferable( JComponent comp ) {
            return taboutput.transferStarTable( getCurrentModel()
                                               .getApparentStarTable() );
        }
        public boolean importData( JComponent comp, Transferable trans ) {
            StarTable table;
            try {
                table = tabfact.makeStarTable( trans );
                if ( table == null ) {
                    return false;
                }
            }
            catch ( IOException e ) {
                ErrorDialog.showError( e, "Drop operation failed", window );
                return false;
            }
            try {
                table = Tables.randomTable( table );
                addTable( Tables.randomTable( table ), "dropped", true );
                return true;
            }
            catch ( IOException e ) {
                ErrorDialog.showError( e, "Can't randomise table", window );
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
