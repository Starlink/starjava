package uk.ac.starlink.topcat;

import gnu.jel.CompilationException;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.Action;
import javax.swing.ListSelectionModel;
import javax.swing.JMenu;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumnModel;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.util.ErrorDialog;

/**
 * A window which displays currently defined RowSubsets for the current
 * table, and offers various subset-related functions.
 *
 * @author   Mark Taylor (Starlink)
 */
public class SubsetWindow extends AuxWindow implements ListDataListener {

    private final TableViewer tv;
    private final OptionsListModel subsets;
    private final Map subsetCounts;
    private final PlasticStarTable dataModel;
    private final MetaColumnTableModel subsetsTableModel;
    private JTable jtab;
    private JProgressBar progBar;
    private SubsetCounter activeCounter;

    /**
     * Constructs a new SubsetWindow from a TableViewer;
     *
     * @param   tableviewer the viewer whose table is to be reflected here
     */
    public SubsetWindow( TableViewer tableviewer ) {
        super( "Row Subsets", tableviewer );
        this.tv = tableviewer;
        this.subsets = tv.getSubsets();
        this.subsetCounts = tv.getSubsetCounts();
        this.dataModel = tv.getDataModel();

        /* Get a model for the table containing the bulk of the data. */
        subsetsTableModel = makeTableModel();

        /* Prepare to update dynamically in response to changes to the 
         * subset list. */
        subsets.addListDataListener( this );

        /* Place a progress bar. */
        progBar = placeProgressBar();

        /* Construct and place a JTable to contain it. */
        jtab = new JTable( subsetsTableModel );
        jtab.setColumnSelectionAllowed( false );
        jtab.setRowSelectionAllowed( true );

        /* Configure column widths. */
        TableColumnModel tcm = jtab.getColumnModel();
        int icol = 0;
        tcm.getColumn( icol++ ).setPreferredWidth( 64 );
        tcm.getColumn( icol++ ).setPreferredWidth( 200 );
        tcm.getColumn( icol++ ).setPreferredWidth( 80 );
        tcm.getColumn( icol++ ).setPreferredWidth( 200 );
        tcm.getColumn( icol++ ).setPreferredWidth( 80 );

        /* Customise the JTable's column model to provide control over
         * which columns are displayed. */
        MetaColumnModel metaColumnModel =
            new MetaColumnModel( jtab.getColumnModel(), subsetsTableModel );
        metaColumnModel.purgeEmptyColumns();
        jtab.setColumnModel( metaColumnModel );

        /* Place the table into a scrollpane in this frame. */
        getMainArea().add( new SizingScrollPane( jtab ) );
        setMainHeading( "Row Subsets" );

        /* Action for adding a new subset. */
        Action addAction = new BasicAction( "New subset", ResourceIcon.ADD,
                                            "Define a new subset using " +
                                            "algebraic expression" ) {
            public void actionPerformed( ActionEvent evt ) {
                Component parent = SubsetWindow.this;
                new SyntheticSubsetQueryWindow( tv, parent );
            }
        };

        /* Action for turning a subset into a column. */
        final Action tocolAction =
            new BasicAction( "To column", ResourceIcon.TO_COLUMN,
                             "Create new boolean column from selected " +
                             "subset" ) {
                public void actionPerformed( ActionEvent evt ) {
                    Component parent = SubsetWindow.this;
                    SyntheticColumnQueryWindow colwin = 
                        new SyntheticColumnQueryWindow( tv, -1, parent );
                    int irow = jtab.getSelectedRow();
                    colwin.setExpression( getSubsetID( irow ) );
                    colwin.setName( getSubsetName( irow ) );
                }
           };

        /* Action for counting subset sizes. */
        final Action countAction = 
            new BasicAction( "Count rows", ResourceIcon.COUNT,
                             "Count the number of rows in each subset" ) {
                public void actionPerformed( ActionEvent evt ) {
                    if ( activeCounter != null ) {
                        activeCounter.interrupt();
                    }
                    SubsetCounter sc = new SubsetCounter();
                    activeCounter = sc;
                    sc.start();
                }
            };

        /* Add a selection listener to ensure that the right actions 
         * are enabled/disabled. */
        ListSelectionListener selList = new ListSelectionListener() {
            public void valueChanged( ListSelectionEvent evt ) {
                int nsel = jtab.getSelectedRowCount();
                boolean hasUniqueSelection = nsel == 1;
                tocolAction.setEnabled( hasUniqueSelection );
            }
        };
        ListSelectionModel selectionModel = jtab.getSelectionModel();
        selectionModel.addListSelectionListener( selList );
        selList.valueChanged( null );
 
        /* Toolbar. */
        getToolBar().add( addAction );
        getToolBar().add( tocolAction );
        getToolBar().add( countAction );
        getToolBar().addSeparator();

        /* Subsets menu. */
        JMenu subsetsMenu = new JMenu( "Subsets" );
        subsetsMenu.setMnemonic( KeyEvent.VK_S );
        subsetsMenu.add( addAction ).setIcon( null );
        subsetsMenu.add( tocolAction ).setIcon( null );
        subsetsMenu.add( countAction ).setIcon( null );
        getJMenuBar().add( subsetsMenu );

        /* Display menu. */
        JMenu displayMenu = metaColumnModel.makeCheckBoxMenu( "Display" );
        displayMenu.setMnemonic( KeyEvent.VK_D );
        getJMenuBar().add( displayMenu );

        /* Add standard help actions. */
        addHelp( "SubsetWindow" );

        /* Make the component visible. */
        pack();
        setVisible( true );
    }
   
    /**
     * Constructs a TableModel holding the useful data being presented
     * by this component.
     *
     * @return  a table model with subset details
     */
    public MetaColumnTableModel makeTableModel() {

        /* ID column. */
        MetaColumn idCol = new MetaColumn( "#ID", String.class ) {
            public Object getValue( int irow ) {
                return getSubsetID( irow );
            }
        };

        /* Name column. */
        MetaColumn nameCol = new MetaColumn( "Name", String.class ) {
            public Object getValue( int irow ) {
                return getSubsetName( irow );
            }
        };

        /* Size column. */
        MetaColumn sizeCol = new MetaColumn( "Size", Long.class ) {
            public Object getValue( int irow ) {
                return getSubsetSize( irow );
            }
        };

        /* Expression column for algebraic subsets. */
        MetaColumn exprCol = new MetaColumn( "Expression", String.class ) { 
            public Object getValue( int irow ) {
                RowSubset rset = (RowSubset) subsets.get( irow );
                if ( rset instanceof SyntheticRowSubset ) {
                    return ((SyntheticRowSubset) rset ).getExpression();
                }
                else {
                    return null;
                }
            }
            public boolean isEditable( int irow ) {
                return subsets.get( irow ) instanceof SyntheticRowSubset;
            }
            public void setValue( int irow, Object value ) {
                RowSubset rset = (RowSubset) subsets.get( irow );
                try {
                    RowSubset newSet = 
                        new SyntheticRowSubset( dataModel, subsets,
                                                rset.getName(), 
                                                value.toString() );
                    subsets.set( irow, newSet );
                }
                catch ( CompilationException e ) {
                    ErrorDialog.showError( e, "Error in expression " + value,
                                           SubsetWindow.this );
                }
            }
        };

        /* Column ID column for column subsets. */
        MetaColumn colCol = new MetaColumn( "Column $ID", String.class ) {
            public Object getValue( int irow ) {
                RowSubset rset = (RowSubset) subsets.get( irow );
                if ( rset instanceof BooleanColumnRowSubset ) {
                    ColumnInfo cinfo = ((BooleanColumnRowSubset) rset)
                                      .getColumnInfo();
                    return " " + tv.getColumnID( cinfo );
                }
                else {
                    return null;
                }
            }
        };

        /* Make a TableModel from these columns. */
        List cols = new ArrayList();
        cols.add( idCol );
        cols.add( nameCol );
        cols.add( sizeCol );
        cols.add( exprCol );
        cols.add( colCol );
        return new MetaColumnTableModel( cols ) {
            public int getRowCount() {
                return subsets.size();
            }
        };
    }

    /**
     * Returns the subset ID string for the subset at a given position
     * in the subsets list (row in the presentation table).
     *
     * @param   irow  index into subsets list
     * @return  subset ID string
     */
    private String getSubsetID( int irow ) {
        return "#" + ( irow + 1 );
    }

    /**
     * Returns the subset name string for the subset at a given position
     * in the subsets list (row in the presentation table).
     *
     * @param   irow  index into subsets list
     * @return  subset name
     */
    private String getSubsetName( int irow ) {
        return ((RowSubset) subsets.get( irow )).getName();
    }

    /**
     * Returns the subset size string for the subset at a given position
     * in the subsets list (row in the presentation table).
     *
     * @param   irow  index into subsets list
     * @return  subset count object (probably a Number or null)
     */
    private Object getSubsetSize( int irow ) {
        RowSubset rset = (RowSubset) subsets.get( irow );
        Number count = (Number) subsetCounts.get( rset );
        return ( count == null || count.longValue() < 0 ) ? null : count;
    }

    /**
     * Extend the dispose method to interrupt any pending calculations.
     */
    public void dispose() {
        super.dispose();
        if ( activeCounter != null ) {
            activeCounter.interrupt();
            activeCounter = null;
            progBar.setValue( 0 );
        }
    }

    /*
     * Implementation of ListDataListener.
     */
    public void contentsChanged( ListDataEvent evt ) {
        subsetsTableModel.fireTableDataChanged();
    }
    public void intervalAdded( ListDataEvent evt ) {
        subsetsTableModel.fireTableDataChanged();
    }
    public void intervalRemoved( ListDataEvent evt ) {
        subsetsTableModel.fireTableDataChanged();
    }


    /**
     * Helper class which performs the counting of how many rows are in
     * each subset.
     */
    private class SubsetCounter extends Thread {

        private long currentRow;

        public SubsetCounter() {
            super( "Subset counter" );
        }

        public void run() {
            count();
        }
   
        /**
         * Count the members of each known RowSubset.  When completed, 
         * the subsetCounts map is updated and the subsets list notified
         * that there are changes.
         */
        void count() {
   
            /* Prepare for the calculations. */
            final RowSubset[] rsets = (RowSubset[]) 
                                      subsets.toArray( new RowSubset[ 0 ] );
            final int nrset = rsets.length;
            final long[] counts = new long[ nrset ];
            long nrow = dataModel.getRowCount();

            /* Prepare the progress bar for use. */
            progBar.setMaximum( (int) Math.min( (long) Integer.MAX_VALUE,
                                                nrow ) );

            /* Prepare an object which can update the progress bar. */
            Runnable updater = new Runnable() {
                public void run() {
                    if ( activeCounter == SubsetCounter.this ) {
                        progBar.setValue( (int) currentRow );
                    }
                }
            };

            /* Iterate over all the rows in the table. */
            for ( currentRow = 0; currentRow < nrow && ! interrupted();
                  currentRow++ ) {
                SwingUtilities.invokeLater( updater );
                for ( int i = 0; i < nrset; i++ ) {
                    RowSubset rset = rsets[ i ];
                    if ( rset.isIncluded( currentRow ) ) {
                        counts[ i ]++;
                    }
                }
            }

            /* If we finished without being interrupted, act on the results
             * we calculated. */
            if ( currentRow == nrow ) {
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {

                        /* Update the subset counts. */
                        for ( int i = 0; i < nrset; i++ ) {
                            subsetCounts.put( rsets[ i ],
                                              new Long( counts[ i ] ) );
                        }

                        /* Notify listeners that the counts have changed. */
                        subsets.fireContentsChanged( 0, nrset - 1 );

                        /* Deactivate the progress bar. */
                        if ( activeCounter == SubsetCounter.this ) {
                            activeCounter = null;
                            progBar.setValue( 0 );
                        }
                    }
                } );
            }
        }
    }

}
