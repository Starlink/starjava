package uk.ac.starlink.topcat;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ComboBoxModel;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.gui.StarTableColumn;
import uk.ac.starlink.table.gui.StarTableSaver;
import uk.ac.starlink.table.gui.TableRowHeader;
import uk.ac.starlink.table.gui.StarJTable;
import uk.ac.starlink.table.jdbc.JDBCHandler;
import uk.ac.starlink.table.jdbc.SwingAuthenticator;
import uk.ac.starlink.util.ErrorDialog;
import uk.ac.starlink.util.Loader;

/**
 * Class defining the table viewer application.  Multiple viewers can
 * exist at once, but there is certain state (such as current directory
 * in file chooser dialogs) shared between them in class static members.
 * 
 * @author   Mark Taylor (Starlink)
 */
public class TableViewer extends AuxWindow
                         implements TableModelListener, 
                                    TableColumnModelListener {

    private PlasticStarTable dataModel;
    private ViewerTableModel viewModel;
    private TableColumnModel columnModel;
    private OptionsListModel subsets;

    private JTable jtab;
    private TableRowHeader rowHead;
    private JScrollPane scrollpane;
    private JProgressBar progBar;
    private JComboBox subSelector;
    private JComboBox sortSelector;
    private ColumnInfoWindow colinfoWindow;
    private ParameterWindow paramWindow;
    private StatsWindow statsWindow;
    private SubsetWindow subsetWindow;
    private Action newAct;
    private Action saveAct;
    private Action dupAct;
    private Action mirageAct;
    private Action plotAct;
    private Action paramAct;
    private Action colinfoAct;
    private Action statsAct;
    private Action subsetsAct;
    private Action unsortAct;
    private Action newsubsetAct;
    private Action nosubsetAct;
    private Action includeAct;
    private Action excludeAct;

    private static boolean standalone = false;
    private static StarTableFactory tabfact = new StarTableFactory();
    private static StarTableOutput taboutput = new StarTableOutput();
    private static LoadQueryWindow loadWindow;
    private static StarTableSaver saver;
    private static ListCellRenderer columnRenderer;

    private static final String DEFAULT_TITLE = "TOPCAT";
    private static int MAX_COLUMN_WIDTH = 300;
    private static int MAX_SAMPLE_ROWS = 20;

    /**
     * Constructs a new TableViewer not initially viewing any table.
     *
     * @param  sibling   a window for positioning relative to; the new
     *         one will generally come out a bit lower and to the right 
     *         of <tt>sibling</tt>.  May be <tt>null</tt>
     */
    public TableViewer( Window sibling ) {
        this( null, sibling );
    }
 
    /**
     * Constructs a new TableViewer to view a given table.
     * The given table must provide random access.
     *
     * @param  startab   a star table, or <tt>null</tt>
     * @param  sibling   a window for positioning relative to; the new
     *         one will generally come out a bit lower and to the right 
     *         of <tt>sibling</tt>.  May be <tt>null</tt>
     * @throws  IllegalArgumentException  if <tt>!startab.isRandom()</tt>
     */
    public TableViewer( StarTable startab, Window sibling ) {

        /* Do basic setup. */
        super( DEFAULT_TITLE, startab, null );
        AuxWindow.positionAfter( sibling, this );
        jtab = new JTable();
        jtab.setCellSelectionEnabled( false );
        jtab.setColumnSelectionAllowed( false );
        jtab.setRowSelectionAllowed( true );
        scrollpane = new SizingScrollPane( jtab );
        getMainArea().add( scrollpane, BorderLayout.CENTER );
        progBar = placeProgressBar();

        /* Set up row header panel. */
        rowHead = new TableRowHeader( jtab ) {
            public int rowNumber( int irow ) {
                int[] rowMap = viewModel.getRowMap();
                return ( ( rowMap == null ) ? irow : rowMap[ irow ] ) + 1;
            }
        };
        scrollpane.setRowHeaderView( rowHead );

        /* Initialise subsets list. */
        subsets = new OptionsListModel();
        subsets.add( RowSubset.ALL );

        /* Create and configure actions. */
        newAct = new ViewerAction( "Open", ResourceIcon.LOAD, 
                                   "Open a new viewer window" );
        saveAct = new ViewerAction( "Save", ResourceIcon.SAVE,
                                    "Write out this table" );
        dupAct = new ViewerAction( "Duplicate", ResourceIcon.COPY,
                                   "Display another copy of this table " +
                                   "in a new viewer" );

        mirageAct = new ViewerAction( "Mirage", 
                                      "Launch Mirage to display this table" );
        plotAct = new ViewerAction( "Plot", ResourceIcon.PLOT,
                                    "Plot columns from this table" );
        paramAct = new ViewerAction( "Table parameters", ResourceIcon.PARAMS,
                                     "Display table metadata" );
        colinfoAct = new ViewerAction( "Column metadata", ResourceIcon.COLUMNS,
                                       "Display metadata for each column" );
        statsAct = new ViewerAction( "Column statistics", ResourceIcon.STATS,
                                     "Display statistics for each column" );
        subsetsAct = new ViewerAction( "Row subsets", ResourceIcon.SUBSETS,
                                       "Display row subsets" );

        unsortAct = new ViewerAction( "Unsort", ResourceIcon.UNSORT,
                                      "Display in original order" );

        newsubsetAct = new ViewerAction( "New subset expression",
                                         "Define a new row subset" );
        includeAct = new ViewerAction( "Subset from selected rows",
                                       "Define a new row subset containing " +
                                       "all currently selected rows" );
        excludeAct = new ViewerAction( "Subset from unselected rows",
                                       "Define a new row subset containing " +
                                       "all rows not currently selected" );
        nosubsetAct = new ViewerAction( "View all rows",
                                        "Don't use any row subsetting" );

        /* Configure the table. */
        if ( startab != null ) {
            setStarTable( startab );
        }

        /* Set up menus. */
        JMenuBar mb = getJMenuBar();

        /* File menu. */
        JMenu fileMenu = getFileMenu();
        int fileMenuPos = 0;
        fileMenu.insert( newAct, fileMenuPos++ ).setIcon( null );
        fileMenu.insert( dupAct, fileMenuPos++ ).setIcon( null );
        fileMenu.insert( saveAct, fileMenuPos++ ).setIcon( null );
        fileMenu.insertSeparator( fileMenuPos++ );

        /* Windows menu. */
        JMenu winMenu = new JMenu( "Windows" );
        mb.add( winMenu );
        winMenu.add( paramAct ).setIcon( null );
        winMenu.add( colinfoAct ).setIcon( null );
        winMenu.add( statsAct ).setIcon( null );
        winMenu.add( subsetsAct ).setIcon( null );
        winMenu.add( plotAct ).setIcon( null );

        /* Subset menu. */
        JMenu subsetMenu = new JMenu( "Subsets" );
        mb.add( subsetMenu );
        subsetMenu.add( nosubsetAct ).setIcon( null );
        subsetMenu.add( newsubsetAct ).setIcon( null );
        subsetMenu.add( includeAct ).setIcon( null );
        subsetMenu.add( excludeAct ).setIcon( null );
        Action applysubsetAct = new AbstractAction() {
            public void actionPerformed( ActionEvent evt ) {
                int index = evt.getID();
                applySubset( (RowSubset) subsets.get( index ) );
            }
        };
        JMenu applysubsetMenu = 
            subsets.makeJMenu( "Apply subset", applysubsetAct );
        subsetMenu.add( applysubsetMenu );

        /* Launch menu. */
        if ( MirageHandler.isMirageAvailable() ) {
            JMenu launchMenu = new JMenu( "Launch" );
            mb.add( launchMenu );
            launchMenu.add( mirageAct ).setIcon( null );
        }

        /* Controls. */
        JPanel controlPanel = getControlPanel();

        /* Row subset selector. */
        subSelector = subsets.makeComboBox();
        subSelector.addItemListener( new ItemListener() {
            public void itemStateChanged( ItemEvent evt ) {
                if ( evt.getStateChange() == ItemEvent.SELECTED ) {
                    applySubset( (RowSubset) evt.getItem() );
                }
            }
        } );
        controlPanel.add( new JLabel( "Row Subset: " ) );
        controlPanel.add( subSelector );

        /* Sort order selector. */
        ComboBoxModel sortselModel =
            new RestrictedColumnComboBoxModel( columnModel, true ) {
                protected boolean acceptColumn( TableColumn tcol ) {
                    StarTableColumn stcol = (StarTableColumn) tcol;
                    Class clazz = stcol.getColumnInfo().getContentClass();
                    return Comparable.class.isAssignableFrom( clazz );
                }
            };
        sortSelector = new JComboBox( sortselModel );
        sortSelector.setSelectedItem( ColumnComboBoxModel.NO_COLUMN );
        sortSelector.setRenderer( getColumnRenderer() );
        sortSelector.addItemListener( new ItemListener() {
            public void itemStateChanged( ItemEvent evt ) {
                boolean up = true;
                if ( evt.getStateChange() == ItemEvent.SELECTED ) {
                    TableColumn tcol = (TableColumn) evt.getItem();
                    sortBy( tcol == ColumnComboBoxModel.NO_COLUMN ? null : tcol,
                            up );
                }
            }
        } );
        controlPanel.add( new JLabel( "     " ) );
        controlPanel.add( new JLabel( "Sort Column: " ) );
        controlPanel.add( sortSelector );

        /* Toolbar. */
        JToolBar toolBar = getToolBar();
        toolBar.add( newAct );
        toolBar.add( dupAct );
        toolBar.add( saveAct );
        toolBar.addSeparator();
        toolBar.add( paramAct );
        toolBar.add( colinfoAct );
        toolBar.add( statsAct );
        toolBar.add( subsetsAct );
        toolBar.add( plotAct );
        toolBar.addSeparator();

        /* Configure a listener for column popup menus. */
        MouseListener mousey = new MouseAdapter() {
            public void mousePressed( MouseEvent evt ) {
                maybeShowPopup( evt );
            }
            public void mouseReleased( MouseEvent evt ) {
                maybeShowPopup( evt );
            }
            private void maybeShowPopup( MouseEvent evt ) {
                if ( evt.isPopupTrigger() ) {
                    int jcol = jtab.columnAtPoint( evt.getPoint() );
                    if ( evt.getComponent() == rowHead ) {
                        jcol = -1;
                    }
                    if ( jcol >= -1 ) {
                        JPopupMenu popper = columnPopup( jcol );
                        if ( popper != null ) {
                            popper.show( evt.getComponent(),
                                         evt.getX(), evt.getY() );
                        }
                    }
                }
            }
        };
        jtab.addMouseListener( mousey );
        jtab.getTableHeader().addMouseListener( mousey );
        rowHead.addMouseListener( mousey );

        /* Configure a listener for row selection events. */
        final ListSelectionModel selectionModel = jtab.getSelectionModel();
        ListSelectionListener selList = new ListSelectionListener() {
            public void valueChanged( ListSelectionEvent evt ) {
                boolean hasSelection = ! selectionModel.isSelectionEmpty();
                includeAct.setEnabled( hasSelection );
                excludeAct.setEnabled( hasSelection );
            }
        };
        selectionModel.addListSelectionListener( selList );
        selList.valueChanged( null );

        /* Add help information. */
        addHelp( "TableViewer" );

        /* Apply the default row subset. */
        applySubset( RowSubset.ALL );
        updateHeading();

        /* Display. */
        pack();
        setVisible( true );
    }
 
    /**
     * Determines whether TableViewer generated from this class should
     * act as a standalone application.  If <tt>standalone</tt> is set
     * true, then it will be possible to exit the JVM using menu items
     * etc in the viewer.  Otherwise, no normal activity within the 
     * TableViewer GUI will cause a JVM exit.
     * 
     * @param  standalone  whether this class should act as a standalone
     *         application
     */
    public static void setStandalone( boolean standalone ) {
        TableViewer.standalone = standalone;
    }

    /**
     * Indicates whether the TableViewer application is standalone or not.
     *
     * @return  whether this should act as a standalone application.
     */
    public static boolean isStandalone() {
        return standalone;
    }

    /**
     * Returns the container for the data held by this viewer.
     * This model, which is a <tt>StarTable</tt> object, is not 
     * affected by changes to the data view such as the order of the results 
     * presented in the viewer.  It can have columns added to it but
     * not removed.  
     *
     * @return  the data model
     */
    public PlasticStarTable getDataModel() {
        return dataModel;
    }

    /**
     * Returns the table model used by this viewer's 
     * <tt>JTable</tt> for table display. 
     * This is based on the <tt>dataModel</tt>, 
     * but can be reordered and configured
     * to display only a subset of the rows and so on.
     *
     * @return  the table model
     */
    public ViewerTableModel getViewModel() {
        return viewModel;
    }

    /**
     * Returns the table column model used by this viewer's <tt>JTable</tt>
     * for table display.
     * This can be manipulated either programmatically or as a consequence
     * of user interaction with the JTable (dragging columns around)
     * to modify the mapping of columns visible in this viewer to 
     * columns in the dataModel.
     *
     * @return  the column model
     */
    public TableColumnModel getColumnModel() {
        return columnModel;
    }

    /**
     * Returns the <tt>ListModel</tt> which keeps track of which 
     * <tt>RowSubset</tt> objects are available.
     * 
     * @return   the RowSubset list model
     */
    public OptionsListModel getSubsets() {
        return subsets;
    }

    /**
     * Sets the viewer to view a given StarTable.
     * The given table must provide random access.
     *
     * @param  startab   a star table, or <tt>null</tt> to clear the
     *          viewer
     * @throws  IllegalArgumentException  if <tt>!startab.isRandom()</tt>
     */
    public void setStarTable( StarTable startab ) {
        if ( startab != null ) {

            /* Ensure that we have random access. */
            if ( ! startab.isRandom() ) {
                throw new IllegalArgumentException( 
                    "Can't use non-random table" );
            }

            /* Construct a data model based on the StarTable which will
             * and also allow some additional functionality such as 
             * column addition. */
            dataModel = new PlasticStarTable( startab );

            /* Configure the JTable with a new TableModel. */
            viewModel = new ViewerTableModel( dataModel );
            jtab.setModel( viewModel );
            rowHead.modelChanged();

            /* Configure the JTable with a new TableColumnModel */
            columnModel = new DefaultTableColumnModel();
            for ( int icol = 0; icol < dataModel.getColumnCount(); icol++ ) {
                ColumnInfo cinfo = dataModel.getColumnInfo( icol );
                TableColumn tcol = new StarTableColumn( cinfo, icol );
                columnModel.addColumn( tcol );
            }
            jtab.setColumnModel( columnModel );

            /* Make sure the viewer window is updated when the TableModel
             * or the TableColumnModel changes changes (for instance 
             * change of table shape). */
            viewModel.addTableModelListener( this );
            columnModel.addColumnModelListener( this );

            /* Set the view up right. */
            scrollpane.getViewport().setViewPosition( new Point( 0, 0 ) );
            StarJTable.configureColumnWidths( jtab, MAX_COLUMN_WIDTH,
                                              MAX_SAMPLE_ROWS );

            /* Add subsets for any boolean type columns. */
            int ncol = dataModel.getColumnCount();
            for ( int icol = 0; icol < ncol; icol++ ) {
                final ColumnInfo cinfo = dataModel.getColumnInfo( icol );
                if ( cinfo.getContentClass() == Boolean.class ) {
                    final int jcol = icol;
                    RowSubset yes = 
                        new BooleanColumnRowSubset( dataModel, icol);
                    subsets.add( yes );
                }
            }
        }
        else {
            jtab.setColumnModel( null );
            jtab.setModel( null );
            dataModel = null;
            viewModel = null;
            columnModel = null;
        }
        setTitle( AuxWindow.makeTitle( DEFAULT_TITLE, dataModel ) );
        configureActions();
    }

    /**
     * Appends a new column to the existing table at a given column index.
     * This method appends a column to the dataModel, fixes the 
     * TableColumnModel to put it in at the right place, and 
     * ensures that everybody is notified about what has gone on.
     *
     * @param  col  the new column
     * @param  colIndex  the column index at which the new column is
     *         to be appended, or -1 for at the end
     */
    public void appendColumn( ColumnData col, int colIndex ) {

        /* Check that we are not trying to add the column beyond the end of
         * the table. */
        if ( colIndex > dataModel.getColumnCount() ) {
            throw new IllegalArgumentException();
        }

        /* Add the column to the table model itself. */
        dataModel.addColumn( col );

        /* Add the new column to the column model. */
        int modelIndex = dataModel.getColumnCount() - 1;
        TableColumn tc = new StarTableColumn( col.getColumnInfo(), modelIndex );
        columnModel.addColumn( tc );

        /* Move the new column to the requested position. */
        if ( colIndex >= 0 ) {
            columnModel.moveColumn( columnModel.getColumnCount() - 1,
                                    colIndex );
        }
        else {
            colIndex = columnModel.getColumnCount() - 1;
        }

        /* Set its width. */
        StarJTable.configureColumnWidth( jtab, MAX_COLUMN_WIDTH,
                                         MAX_SAMPLE_ROWS, colIndex );
    }

    /**
     * Appends a new column to the existing table as the last column.
     *
     * @param  col  the new column
     */
    public void appendColumn( ColumnData col ) {
        appendColumn( col, -1 );
    }

    /**
     * Returns a label for a given column.  This will normally be the
     * column's name, but if it doesn't have one, it may be assigned a
     * number or something.
     *
     * @param  icol  the index of the column in this viewer's dataModel 
     */
    public String getColumnLabel( int icol ) {
        String name = dataModel.getColumnInfo( icol ).getName();
        if ( name == null || name.toString().trim().length() == 0 ) {
            name = "$" + icol;
        }
        return name;
    }

    /**
     * Adds a new row subset to the list which this viewer knows about.
     *
     * @param  rset  the new row subset
     */
    public void addSubset( RowSubset rset ) {
        subsets.add( rset );
        applySubset( rset );
    }

    /**
     * Applies a given RowSubset to the current viewer, so that only 
     * rows in this set will be visible in the table viewer window.
     *
     * @param  rset  the row subset to use
     */
    public void applySubset( RowSubset rset ) {
        if ( rset != viewModel.getSubset() ) {
            viewModel.setSubset( rset );
        }
        if ( rset != subSelector.getSelectedItem() ) {
            subSelector.setSelectedItem( rset );
        }
    }

    /**
     * Returns a StarTable representing the table data as displayed by
     * this viewer.  This may differ from the original StarTable object
     * held by it in a number of ways; it may have a different row order,
     * different column orderings, and added or removed columns.
     *
     * @return  a StarTable object representing what this viewer appears
     *          to be showing
     */
    public StarTable getApparentStarTable() {
        int ncol = columnModel.getColumnCount();
        final int nrow = viewModel.getRowCount();
        ColumnStarTable appTable = ColumnStarTable.makeTableWithRows( nrow );
        for ( int icol = 0; icol < ncol; icol++ ) {
            final int modelIndex = columnModel.getColumn( icol )
                                              .getModelIndex();
            ColumnInfo colinfo = dataModel.getColumnInfo( modelIndex );
            ColumnData coldata = new ColumnData( colinfo ) {
                public Object readValue( long lrow ) {
                    return viewModel.getValueAt( (int) lrow, modelIndex );
                }
            };
            appTable.addColumn( coldata );
        }
        return appTable;
    }

    /**
     * Returns a dialog used for loading new tables.
     *
     * @return  a table load window
     */
    private static LoadQueryWindow getLoader() {
        if ( loadWindow == null ) {
            loadWindow = new LoadQueryWindow( tabfact );
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
    private static StarTableSaver getSaver() {
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
     * Configures the enabledness of various actions for which this needs
     * doing based on the current state of this viewer.  To be called
     * both during viewer initialisation and when some relevant aspect
     * of the viewer changes.
     */
    private void configureActions() {
        boolean hasTable = dataModel != null;
        saveAct.setEnabled( hasTable );
        dupAct.setEnabled( hasTable );
    }

    /**
     * Returns a popup menu for a given column.  If the dummy column 
     * index -1 is used, a popup suitable for the row header column
     * will be returned.
     *
     * @param  jcol the data model column to which the menu applies
     */
    private JPopupMenu columnPopup( final int jcol ) {
        JPopupMenu popper = new JPopupMenu();

        if ( jcol >= 0 ) {
            Action deleteAct = new AbstractAction( "Delete" ) {
                public void actionPerformed( ActionEvent evt ) {
                    columnModel.removeColumn( columnModel.getColumn( jcol ) );
                }
            };
            popper.add( deleteAct );
        }

        Action addcolAct = new AbstractAction( "Synthetic column" ) {
            public void actionPerformed( ActionEvent evt ) {
                Component parent = TableViewer.this;
                TableViewer tv = TableViewer.this;
                new SyntheticColumnQueryWindow( tv, jcol + 1, parent );
            }
        };
        popper.add( addcolAct );

        if ( jcol >= 0 ) {
            StarTableColumn stcol = (StarTableColumn) 
                                    columnModel.getColumn( jcol );
            ColumnInfo colinfo = stcol.getColumnInfo();
            if ( Comparable.class
                           .isAssignableFrom( colinfo.getContentClass() ) ) {
                popper.add( new SortAction( stcol, true ) );
                popper.add( new SortAction( stcol, false ) );
            }
        }
        else {
            popper.add( unsortAct );
        }

        return popper;
    }

    /**
     * All actions are performed by instances of the one class defined here.  
     * This is a bit less fiddly and probably faster (fewer 
     * classes loaded) than defining a new anonymous class for 
     * each action.
     */
    private class ViewerAction extends BasicAction {

        private final Window parent = TableViewer.this;
        private final TableViewer tv = TableViewer.this;

        ViewerAction( String name, Icon icon, String shortdesc ) {
            super( name, icon, shortdesc );
        }

        ViewerAction( String name, String shortdesc ) {
            this( name, null, shortdesc );
        }

        public void actionPerformed( ActionEvent evt ) {

            /* Open a new table viewer window. */
            if ( this == newAct ) {
                getLoader().loadRandomStarTable( parent );
            }

            /* Open the same table in a new viewer. */
            else if ( this == dupAct ) {
                assert dataModel != null;  // action would be disabled 
                new TableViewer( getApparentStarTable(), parent );
            }

            /* Save the table to a file. */
            else if ( this == saveAct ) {
                assert dataModel != null;  // action would be disabled 
                StarTable saveTable = getApparentStarTable();
                getSaver().saveTable( saveTable, parent );
            }

            /* Launch Mirage. */
            else if ( this == mirageAct ) {
                assert MirageHandler.isMirageAvailable();
                try {
                    StarTable mirageTable = getApparentStarTable();
                    MirageHandler.invokeMirage( mirageTable, null );
                }
                catch ( Exception e ) {
                    ErrorDialog.showError( e, "Error launching Mirage",
                                           parent );
                }
            }

            /* Open a plot window. */
            else if ( this == plotAct ) {
                new PlotWindow( tv );
            }

            /* Display table parameters. */
            else if ( this == paramAct ) {
                if ( paramWindow == null ) {
                    paramWindow = new ParameterWindow( dataModel, columnModel,
                                                       parent );
                }
                else {
                    paramWindow.makeVisible();
                }
            }

            /* Display column parameters. */
            else if ( this == colinfoAct ) {
                if ( colinfoWindow == null ) {
                    colinfoWindow = new ColumnInfoWindow( tv );
                }
                else {
                    colinfoWindow.makeVisible();
                }
            }

            /* Display column statistics. */
            else if ( this == statsAct ) {
                if ( statsWindow == null ) {
                    statsWindow = new StatsWindow( tv );
                    statsWindow.setSubset( viewModel.getSubset() );
                }
                else {
                    statsWindow.setSubset( viewModel.getSubset() );
                    statsWindow.makeVisible();
                }
            }

            /* Display row subsets. */
            else if ( this == subsetsAct ) {
                if ( subsetWindow == null ) {
                    subsetWindow = new SubsetWindow( tv );
                }
                else {
                    subsetWindow.makeVisible();
                }
            }

            /* Set the row order back to normal. */
            else if ( this == unsortAct ) {
                sortBy( null, false );
            }

            /* Define a new subset by dialog. */
            else if ( this == newsubsetAct ) {
                new SyntheticSubsetQueryWindow( tv, parent );
            }

            /* Define a new subset using selected rows. */
            else if ( this == includeAct ) {
                String name = enquireSubsetName( parent );
                if ( name != null ) {
                    BitSet bits = getSelectedRowFlags();
                    addSubset( new BitsRowSubset( name, bits ) );
                }
            }

            /* Define a new subset using unselected rows. */
            else if ( this == excludeAct ) {
                String name = enquireSubsetName( parent );
                if ( name != null ) {
                    BitSet bits = getSelectedRowFlags();
                    int nrow = (int) dataModel.getRowCount();
                    bits.flip( 0, nrow );
                    addSubset( new BitsRowSubset( name, bits ) );
                }
            }

            /* Use null subset. */
            else if ( this == nosubsetAct ) {
                applySubset( RowSubset.ALL );
            }

            /* Shouldn't happen. */
            else {
                throw new AssertionError( 
                    "Unhandled action (programming error)" );
            }
        }
    }

    /**
     * Class defining an Action which does table sorting.  The sort is
     * effected by creating a mapping between model rows and (sorted)
     * view rows, and installing this into this viewer's data model.
     */
    private class SortAction extends AbstractAction {
        private TableColumn tcol;
        private boolean ascending;

        /**
         * Constructs a new SortAction which will sort in a given direction
         * based on a given column.
         *
         * @param  tcol  the column to be sorted on (must be a column in
         *               this viewer's model)
         * @param  ascending  true for ascending sort, false for descending
         */
        public SortAction( TableColumn tcol, boolean ascending ) {
            super( "Sort " + ( ascending ? "up" : "down" ) );
            this.tcol = tcol;
            this.ascending = ascending;
            String name = tcol.getIdentifier().toString();
            putValue( SHORT_DESCRIPTION,
                      "Sort rows by " +
                      ( ascending ? "ascending" : "descending" ) +
                      " value of " + name );
        }

        public void actionPerformed( ActionEvent evt ) {
            sortBy( tcol, ascending );
        }
    }

    /**
     * Sort the displayed table according to the order of values in a 
     * given column.
     *
     * @param  icol  the index of the column to be sorted on in 
     *               this viewer's model; if < 0, natural sorting will
     *               be applied
     * @param  ascending  true for ascending sort, false for descending
     */
    public void sortBy( TableColumn tcol, boolean ascending ) {

        /* Check that the selection box selected item is consistent with 
         * the sort we have been asked to do.  If not, set the box 
         * correctly and return; this will cause the reinvocation of this
         * method. */
        TableColumn nocol = ColumnComboBoxModel.NO_COLUMN;
        TableColumn selectorTcol = (TableColumn) sortSelector.getSelectedItem();
        TableColumn selectedTcol = selectorTcol == nocol ? null : selectorTcol;
        if ( tcol != selectedTcol ) {
            sortSelector.setSelectedItem( tcol == null ? nocol : tcol );
            sortSelector.repaint();
            return;
        }

        /* Do the sort. */
        int[] order;
        if ( tcol == null ) {
            order = null;
        }
        else {
            int modelIndex = tcol.getModelIndex();
            try {
                order = getSortOrder( modelIndex, ascending );
            }
            catch ( IOException e ) {
                e.printStackTrace();
                beep();
                return;
            }
        }
        viewModel.setOrder( order );
    }

    /**
     * Returns a row mapping array which gives the sort order corresponding
     * to a sort on values in a given column.
     *
     * @param  icol  the index of the column to be sorted on in 
     *               this viewer's model 
     * @param  ascending  true for ascending sort, false for descending
     */
    private int[] getSortOrder( int icol, final boolean ascending )
            throws IOException {

        /* Define a little class for objects being sorted. */
        class Item implements Comparable { 
            int rank;
            Comparable value;
            int sense = ascending ? 1 : -1;
            public int compareTo( Object o ) {
                if ( value != null && o != null ) {
                    return sense * 
                           value.compareTo( (Comparable) ((Item) o).value );
                }
                else if ( value == null && o == null ) {
                    return 0;
                }
                else {
                    return sense * ( ( value == null ) ? 1 : -1 );
                }
            }
        }

        /* Construct a list of all the elements in the given column. */
        int nrow = AbstractStarTable
                  .checkedLongToInt( dataModel.getRowCount() );
        ColumnData coldata = dataModel.getColumnData( icol );
        Item[] items = new Item[ nrow ];
        for ( int i = 0; i < nrow; i++ ) {
            Item item = new Item();
            item.rank = i;
            item.value = (Comparable) coldata.readValue( (long) i );
            items[ i ] = item;
        }

        /* Sort the list on the ordering of the items. */
        Arrays.sort( items );

        /* Construct and return a list of reordered ranks from the 
         * sorted array. */
        int[] rowMap = new int[ nrow ];
        for ( int i = 0; i < nrow; i++ ) {
            rowMap[ i ] = items[ i ].rank;
        }
        return rowMap;
    }

    /**
     * Returns a BitSet in which bit <i>i</i> is set if a table view row 
     * corresponding to row <i>i</i> of this viewer's data model has 
     * been selected in the GUI.  The BitSet has the same number of bits
     * as the data model has rows.
     *
     * @return  new bit vector
     */
    private BitSet getSelectedRowFlags() {
        int nrow = (int) dataModel.getRowCount();
        BitSet bits = new BitSet( nrow );
        int[] selected = jtab.getSelectedRows();
        int nsel = selected.length;
        int[] rowMap = viewModel.getRowMap();
        if ( rowMap == null ) {
            for ( int i = 0; i < nsel; i++ ) {
                bits.set( selected[ i ] );
            }
        }
        else {
            for ( int i = 0; i < nsel; i++ ) {
                bits.set( rowMap[ selected[ i ] ] );
            }
        }
        return bits;
    }

    /**
     * Pops up a modal dialog to ask the user the name for a new RowSubset.
     *
     * @param  parent component, used for positioning
     * @return  a new subset name entered by the user, or <tt>null</tt> if 
     *          he bailed out
     */
    public static String enquireSubsetName( Component parent ) {
        String name = JOptionPane.showInputDialog( parent, "New subset name" );
        if ( name == null || name.trim().length() == 0 ) {
            return null;
        }
        else {
            return name;
        }
    }

    /**
     * Returns a unique ID string for the given table column, which
     * should be one of the columns in this viewer's dataModdel 
     * (though not necessarily its columnModel).  The id will consist
     * of a '$' sign followed by an integer.
     *
     * @param   cinfo column metadata 
     * @return  ID string
     */
    public String getColumnID( ColumnInfo cinfo ) {
        return cinfo.getAuxDatumByName( PlasticStarTable.COLID_INFO.getName() )
                    .getValue()
                    .toString();
    }

    /**
     * Returns a new JComboBox for selecting columns in the columnModel
     * associated with this viewer.  Any item returned by the returned
     * component's {@link javax.swing.JComboBox#getSelectedItem} method
     * will be a {@link uk.ac.starlink.table.gui.StarTableColumn}
     * (or <tt>null</tt>).
     * <p>
     * If the <tt>hasNone</tt> parameter is set true, then the combobox
     * will contain an item additional to all the columns with the 
     * value <tt>null</tt>.
     *
     * @return   column selector
     */
    public JComboBox makeColumnComboBox( boolean hasNone ) {
        JComboBox colBox = 
            new JComboBox( new ColumnComboBoxModel( columnModel, hasNone ) );
        colBox.setRenderer( getColumnRenderer() );
        return colBox;
    }

    /**
     * Returns a renderer suitable for rendering StarTableColumn objects
     * into a JComboBox.  It can render other things too if necesary.
     *
     * @return   a combobox cell renderer
     */
    public static ListCellRenderer getColumnRenderer() {
        if ( columnRenderer == null ) {
            columnRenderer = new CustomComboBoxRenderer() {
                public Object mapValue( Object value ) {
                    if ( value instanceof StarTableColumn ) {
                        return ((StarTableColumn) value)
                              .getColumnInfo()
                              .getName();
                    }
                    else {
                        return value;
                    }
                }
            };
        }
        return columnRenderer;
    }

    /**
     * Updates the short heading which describes the table.
     * Since this contains information about the table shape, this method
     * should be invoked whenever the number of rows or columns might
     * have changed.
     */
    private void updateHeading() {
        int nrow = jtab.getRowCount();
        int ncol = jtab.getColumnCount();
        RowSubset rset = viewModel.getSubset();
        String head = new StringBuffer()
            .append( "Data for " )
            .append( rset == RowSubset.ALL 
                          ? "all rows" : ( "row subset: " + rset.getName() ) )
            .append( " (" ) 
            .append( nrow ) 
            .append( ' ' )
            .append( nrow == 1 ? "row" : "rows" )
            .append( " x " )
            .append( ncol )
            .append( ' ' )
            .append( nrow == 1 ? "column" : "columns" )
            .append( ')' )
            .toString();
        setMainHeading( head );
    }

    /*
     * Implementation of TableModelListener interface.
     */
    public void tableChanged( TableModelEvent evt ) {
        if ( evt.getSource() == viewModel ) {
            if ( evt.getLastRow() > viewModel.getRowCount() ||
                 evt.getType() != TableModelEvent.UPDATE ) {
                updateHeading();
            }
        }
    }

    /*
     * Implementation of TableColumnModelListener interface.
     */
    public void columnAdded( TableColumnModelEvent evt ) {
        if ( evt.getSource() == columnModel ) {
            updateHeading();
        }
    }
    public void columnRemoved( TableColumnModelEvent evt ) {
        if ( evt.getSource() == columnModel ) {
            updateHeading();
        }
    }
    public void columnMarginChanged( ChangeEvent evt ) {}
    public void columnMoved( TableColumnModelEvent evt ) {}
    public void columnSelectionChanged( ListSelectionEvent evt ) {}
    
   
    /**
     * Main method for the table viewer application.
     */
    public static void main( String[] args ) {
        Loader.loadProperties();
        String cmdname = 
            System.getProperty( "uk.ac.starlink.topcat.cmdname" );
        if ( cmdname == null ) {
            cmdname = "TableViewer";
        }
        String usage = new StringBuffer()
              .append( "Usage:\n" ) 
              .append( "   " + cmdname + " [table ...]\n" )
              .append( "   " + cmdname + " -guihelp" )
              .toString();
        setStandalone( true );
        if ( args.length > 0 ) {
            int nok = 0;
            boolean help = false;
            TableViewer lastViewer = null;
            for ( int i = 0; i < args.length; i++ ) {

                /* Deal with any known flags. */
                if ( args[ i ].equalsIgnoreCase( "-guihelp" ) ) {
                    System.out.println( "Displaying help browser" );
                    help = true;
                    HelpWindow.getInstance( null );
                    break;
                }
                else if ( args[ i ].startsWith( "-h" ) ) {
                    System.out.println( usage );
                    System.exit( 0 );
                }

                /* Try to interpret each command line argument as a 
                 * table specification. */
                boolean ok = false;
                try {
                    StarTable startab = tabfact.makeStarTable( args[ i ] );
                    if ( startab == null ) {
                        System.err.println( "No table \"" + args[ i ] + "\"" );
                    }
                    else {
                        startab = Tables.randomTable( startab );
                        lastViewer = new TableViewer( startab, lastViewer );
                        ok = true;
                    }
                }
                catch ( Exception e ) {
                    System.err.println( "Can't view table \""
                                      + args[ i ] + "\"" );
                    e.printStackTrace( System.err );
                }
                if ( ok ) {
                    nok++;
                }

                /* Bail out if there was an error reading the table. 
                 * This clause could be removed so that the viewer would
                 * still start up when multiple tables were specified on
                 * the command line with some broken and some OK. */
                else {
                    System.exit( 1 );
                }
            }

            /* Bail out in any case if we have no working tables. */
            if ( nok == 0 && ! help ) {
                System.exit( 1 );
            }
        }

        /* No tables named on the command line - pop up a dialog asking
         * for one. */
        else {
            new LoadQueryWindow( tabfact ).loadRandomStarTable( null );
        }
    }

}
