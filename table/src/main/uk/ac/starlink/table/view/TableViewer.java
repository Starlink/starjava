package uk.ac.starlink.table.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
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
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
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
import uk.ac.starlink.table.gui.StarTableChooser;
import uk.ac.starlink.table.gui.StarTableColumn;
import uk.ac.starlink.table.gui.StarTableSaver;
import uk.ac.starlink.table.gui.TableRowHeader;
import uk.ac.starlink.table.gui.StarJTable;
import uk.ac.starlink.table.jdbc.JDBCHandler;
import uk.ac.starlink.table.jdbc.SwingAuthenticator;
import uk.ac.starlink.util.Loader;

/**
 * Class defining the table viewer application.  Multiple viewers can
 * exist at once, but there is certain state (such as current directory
 * in file chooser dialogs) shared between them in class static members.
 * 
 * @author   Mark Taylor (Starlink)
 */
public class TableViewer extends JFrame {

    /**
     * Container for the data held by this viewer.  This model is not 
     * affected by changes to the data view such as the order of the results 
     * presented in the viewer.  It can have columns added to it but
     * not removed.
     */
    private PlasticStarTable dataModel;

    /**
     * The table model used by this viewer's JTable for table display. 
     * This is based on dataModel, but can be reordered and configured
     * to display only a subset of the rows and so on.
     */
    private ViewerTableModel viewModel;

    /**
     * The table column model used by this viewer's JTable for table display.
     * This can be manipulated either programmatically or as a consequence
     * of user interaction with the JTable (dragging columns around)
     * to modify the mapping of columns visible in this viewer to 
     * columns in the dataModel.
     */
    private TableColumnModel columnModel;
  
    private OptionsListModel subsets;
    private JTable jtab;
    private TableRowHeader rowHead;
    private JScrollPane scrollpane;
    private Action exitAct;
    private Action closeAct;
    private Action newAct;
    private Action saveAct;
    private Action dupAct;
    private Action mirageAct;
    private Action plotAct;
    private Action paramAct;
    private Action colinfoAct;
    private Action unsortAct;
    private Action newsubsetAct;
    private Action nosubsetAct;
    private Action includeAct;
    private Action excludeAct;

    private static StarTableFactory tabfact = new StarTableFactory();
    private static StarTableOutput taboutput = new StarTableOutput();
    private static WindowTracker wtracker = new WindowTracker();
    private static StarTableChooser chooser;
    private static StarTableSaver saver;

    private static final String DEFAULT_TITLE = "Table Viewer";
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
        super();
        AuxWindow.positionAfter( sibling, this );
        jtab = new JTable();
        jtab.setCellSelectionEnabled( false );
        jtab.setColumnSelectionAllowed( false );
        jtab.setRowSelectionAllowed( true );
        scrollpane = new SizingScrollPane( jtab );
        getContentPane().add( scrollpane, BorderLayout.CENTER );

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
        exitAct = new ViewerAction( "Exit", 0,
                                    "Exit the application" );
        closeAct = new ViewerAction( "Close", 0,
                                     "Close this viewer" );
        newAct = new ViewerAction( "New", 0,
                                   "Open a new viewer window" );
        saveAct = new ViewerAction( "Save", 0,
                                    "Write out this table" );
        dupAct = new ViewerAction( "Duplicate", 0,
                       "Display another copy of this table in a new viewer" );

        mirageAct = new ViewerAction( "Mirage", 0,
                                      "Launch Mirage to display this table" );
        plotAct = new ViewerAction( "Plot", 0,
                                    "Plot columns from this table" );
        paramAct = new ViewerAction( "Table parameters", 0,
                                     "Display table metadata" );
        colinfoAct = new ViewerAction( "Column metadata", 0,
                                       "Display column metadata" );
        unsortAct = new ViewerAction( "Unsort", 0,
                                      "Display in original order" );

        newsubsetAct = new ViewerAction( "New subset expression", 0,
                                         "Define a new row subset" );
        includeAct = new ViewerAction( "Subset from selected rows", 0,
            "Define a new row subset containing all currently selected rows" );
        excludeAct = new ViewerAction( "Subset from unselected rows", 0,
            "Define a new row subset containing all " +
            "rows not currently selected" );
        nosubsetAct = new ViewerAction( "View all rows", 0,
                                        "Don't use any row subsetting" );

        /* Configure the table. */
        if ( startab != null ) {
            setStarTable( startab );
        }

        /* Keep track of instances. */
        setDefaultCloseOperation( DISPOSE_ON_CLOSE );
        wtracker.register( this );

        /* Set up menus. */
        JMenuBar mb = new JMenuBar();
        setJMenuBar( mb );

        /* File menu. */
        JMenu fileMenu = new JMenu( "File" );
        mb.add( fileMenu );
        fileMenu.add( newAct ).setIcon( null );
        fileMenu.add( dupAct ).setIcon( null );
        fileMenu.add( saveAct ).setIcon( null );
        fileMenu.add( closeAct ).setIcon( null );
        fileMenu.add( exitAct ).setIcon( null );

        /* Launch menu. */
        if ( MirageHandler.isMirageAvailable() ) {
            JMenu launchMenu = new JMenu( "Launch" );
            mb.add( launchMenu );
            launchMenu.add( mirageAct ).setIcon( null );
        }

        /* Plot menu. */
        JMenu plotMenu = new JMenu( "Plot" );
        mb.add( plotMenu );
        plotMenu.add( plotAct ).setIcon( null );

        /* Metadata menu. */
        JMenu metaMenu = new JMenu( "Metadata" );
        mb.add( metaMenu );
        metaMenu.add( paramAct ).setIcon( null );
        metaMenu.add( colinfoAct ).setIcon( null );

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
                    if ( jcol >= 0 ) {
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

        /* Display. */
        pack();
        setVisible( true );
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

            /* Set the view up right. */
            scrollpane.getViewport().setViewPosition( new Point( 0, 0 ) );
            StarJTable.configureColumnWidths( jtab, MAX_COLUMN_WIDTH,
                                              MAX_SAMPLE_ROWS );
        }
        else {
            jtab.setColumnModel( null );
            jtab.setModel( null );
            dataModel = null;
            viewModel = null;
            columnModel = null;
        }
        setTitle( AuxWindow.makeTitle( DEFAULT_TITLE, startab ) );
        configureActions();
    }

    /**
     * Appends a new column to the existing table at a given column index.
     * This method appends a column to the dataModel, fixes the 
     * TableColumnModel to put it in at the right place, and 
     * ensures that everybody is notified about what has gone on.
     *
     * @param  colIndex  the column index at which the new column is
     *         to be appended
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
        columnModel.moveColumn( columnModel.getColumnCount() - 1, colIndex );

        /* Set its width. */
        StarJTable.configureColumnWidth( jtab, MAX_COLUMN_WIDTH,
                                         MAX_SAMPLE_ROWS, colIndex );
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
        viewModel.setSubset( rset );
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
        ColumnStarTable appTable = new ColumnStarTable() {
            public long getRowCount() {
                return nrow;
            }
        };
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
     * @return  a table chooser
     */
    private static StarTableChooser getChooser() {
        if ( chooser == null ) {
            File dir = ( saver == null ) 
                           ? new File( "." )
                           : saver.getFileChooser().getCurrentDirectory();
            chooser = new StarTableChooser();
            chooser.getFileChooser().setCurrentDirectory( dir );
            chooser.setStarTableFactory( tabfact );
        }
        return chooser;
    }

    /**
     * Returns a dialog used for saving tables.
     *
     * @return  a table saver
     */
    private static StarTableSaver getSaver() {
        if ( saver == null ) {
            File dir = ( chooser == null )
                           ? new File( "." )
                           : chooser.getFileChooser().getCurrentDirectory();
            saver = new StarTableSaver();
            saver.getFileChooser().setCurrentDirectory( dir );
            saver.setStarTableOutput( taboutput );
        }
        return saver;
    }

    /**
     * Change the order that the table rows are displayed in.
     *
     * @param  order  new order in which visible rows should be seeen;
     *         may be null to indicate natural order.
     */
    private void setOrder( int[] order ) {
        viewModel.setOrder( order );
    }

    /**
     * Call this method to indicate that where there is a choice between
     * a command line and a graphical user interface, the graphical one
     * should be used.  The idea is that if the application is started
     * up from the command line, it might as well use a command line
     * interface until such time as it has posted any windows since
     * (a) that is where the user will be looking and (b) if there is
     * an error near startup it may never be necessary to to the 
     * expensive load of Swing classes.  But once the GUI has got going,
     * then the user will be thinking GUI and a few more dialog boxes
     * won't make any odds.
     */
    private static void setUseGUI() {
        JDBCHandler jh = tabfact.getJDBCHandler();
        if ( ! ( jh.getAuthenticator() instanceof SwingAuthenticator ) ) {
            jh.setAuthenticator( new SwingAuthenticator() );
        }
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
     * Returns a popup menu for a given column.
     *
     * @param  jcol the data model column to which the menu applies
     */
    private JPopupMenu columnPopup( final int jcol ) {
        JPopupMenu popper = new JPopupMenu();

        Action deleteAct = new AbstractAction( "Delete" ) {
            public void actionPerformed( ActionEvent evt ) {
                columnModel.removeColumn( columnModel.getColumn( jcol ) );
            }
        };
        popper.add( deleteAct );

        Action addcolAct = new AbstractAction( "New column" ) {
            public void actionPerformed( ActionEvent evt ) {
                Component parent = TableViewer.this;
                ColumnData coldata = new ColumnDialog( dataModel, subsets )
                                    .obtainColumn( parent );
                if ( coldata != null ) {
                    appendColumn( coldata, jcol + 1 );
                }
            }
        };
        popper.add( addcolAct );

        int icol = columnModel.getColumn( jcol ).getModelIndex();
        if ( Comparable.class.isAssignableFrom( dataModel.getColumnInfo( icol )
                                               .getContentClass() ) ) {
            popper.add( new SortAction( icol, true ) );
            popper.add( new SortAction( icol, false ) );
        }

        return popper;
    }

    /**
     * All actions are performed by instances of the one class defined here.  
     * This is a bit less fiddly and probably faster (fewer 
     * classes loaded) than defining a new anonymous class for 
     * each action.
     */
    private class ViewerAction extends AbstractAction {

        private final Window parent = TableViewer.this;

        ViewerAction( String name, int iconId, String shortdesc ) {
            super( name, null );
            putValue( SHORT_DESCRIPTION, shortdesc );
        }

        public void actionPerformed( ActionEvent evt ) {

            /* Exit the application. */
            if ( this == exitAct ) {
                System.exit( 0 );
            }

            /* Close this viewer window. */
            else if ( this == closeAct ) {
                dispose();
            }

            /* Open a new table viewer window. */
            else if ( this == newAct ) {
                StarTable st = getChooser().getRandomTable( parent );
                if ( st != null ) {
                    new TableViewer( st, parent );
                }
            }

            /* Open the same table in a new viewer. */
            else if ( this == dupAct ) {
                assert dataModel != null;  // action would be disabled 
                new TableViewer( getApparentStarTable(), parent );
            }

            /* Save the table to a file. */
            else if ( this == saveAct ) {
                assert dataModel != null;  // action would be disabled 
                getSaver().saveTable( getApparentStarTable(), parent );
            }

            /* Launch Mirage. */
            else if ( this == mirageAct ) {
                assert MirageHandler.isMirageAvailable();
                try {
                    MirageHandler.invokeMirage( getApparentStarTable(), null );
                }
                catch ( Exception e ) {
                    JOptionPane.showMessageDialog( parent, e.toString(),
                                                   "Error launching Mirage",
                                                   JOptionPane.ERROR_MESSAGE );
                }
            }

            /* Open a plot window. */
            else if ( this == plotAct ) {
                wtracker
               .register( new PlotWindow( dataModel, columnModel, subsets, 
                                          parent ) );
            }

            /* Display table parameters. */
            else if ( this == paramAct ) {
                wtracker
               .register( new ParameterWindow( dataModel, columnModel, 
                                               parent ) );
            }

            /* Display column parameters. */
            else if ( this == colinfoAct ) {
                wtracker
               .register( new ColumnInfoWindow( dataModel, columnModel, 
                                                parent ) );
            }

            /* Set the row order back to normal. */
            else if ( this == unsortAct ) {
                setOrder( null );
            }

            /* Define a new subset by dialog. */
            else if ( this == newsubsetAct ) {
                RowSubset rset = new SubsetDialog( dataModel, subsets )
                                .obtainSubset( parent );
                if ( rset != null ) {
                    addSubset( rset );
                }
            }

            /* Define a new subset using selected rows. */
            else if ( this == includeAct ) {
                String name = enquireSubsetName();
                if ( name != null ) {
                    BitSet bits = getSelectedRowFlags();
  System.out.println( bits.cardinality() );
                    addSubset( new BitsRowSubset( name, bits ) );
                }
            }

            /* Define a new subset using unselected rows. */
            else if ( this == excludeAct ) {
                String name = enquireSubsetName();
                if ( name != null ) {
                    BitSet bits = getSelectedRowFlags();
  System.out.print( bits.cardinality() + "  " );
                    int nrow = (int) dataModel.getRowCount();
                    bits.flip( 0, nrow );
  System.out.println( bits.cardinality() );
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
        private int icol;
        private boolean ascending;

        /**
         * Constructs a new SortAction which will sort in a given direction
         * based on a given column.
         *
         * @param  icol  the index of the column to be sorted on in 
         *               this viewer's model 
         * @param  ascending  true for ascending sort, false for descending
         */
        public SortAction( int icol, boolean ascending ) {
            super( "Sort " + ( ascending ? "up" : "down" ) );
            this.icol = icol;
            this.ascending = ascending;
            putValue( SHORT_DESCRIPTION, "Sort rows by ascending value of " 
                                       + getColumnLabel( icol ) );
        }

        public void actionPerformed( ActionEvent evt ) {
            try {
                setOrder( getSortOrder( icol, ascending ) );
            }
            catch ( IOException e ) {
                e.printStackTrace();
            }
        }
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
     * @return  a new subset name entered by the user, or <tt>null</tt> if 
     *          he bailed out
     */
    private String enquireSubsetName() {
        String name = JOptionPane.showInputDialog( this, "New subset name" );
        if ( name == null || name.trim().length() == 0 ) {
            return null;
        }
        else {
            return name;
        }
    }

    /**
     * Main method for the table viewer application.
     */
    public static void main( String[] args ) {
        Loader.loadProperties();
        String cmdname = 
            System.getProperty( "uk.ac.starlink.table.view.cmdname" );
        if ( cmdname == null ) {
            cmdname = "TableViewer";
        }
        String usage = "Usage:\n" 
                     + "   " + cmdname + " [table ...]\n";
        if ( args.length > 0 ) {
            int nok = 0;
            TableViewer lastViewer = null;
            for ( int i = 0; i < args.length; i++ ) {

                /* Deal with any known flags. */
                if ( args[ i ].startsWith( "-h" ) ) {
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
                        setUseGUI();
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
            if ( nok == 0 ) {
                System.exit( 1 );
            }
        }

        /* No tables named on the command line - pop up a dialog asking
         * for one. */
        else {
            StarTable st = getChooser().getRandomTable( null );
            if ( st != null ) {
                new TableViewer( st, null );
            }
            else {
                System.exit( 1 );
            }
        }
    }

}
