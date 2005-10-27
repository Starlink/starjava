package uk.ac.starlink.topcat;

import gnu.jel.CompilationException;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.Action;
import javax.swing.ComboBoxModel;
import javax.swing.Icon;
import javax.swing.ListSelectionModel;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumnModel;
import uk.ac.starlink.table.ColumnInfo;

/**
 * A window which displays currently defined RowSubsets for the current
 * table, and offers various subset-related functions.
 *
 * @author   Mark Taylor (Starlink)
 */
public class SubsetWindow extends AuxWindow implements ListDataListener {

    private final TopcatModel tcModel;
    private final OptionsListModel subsets;
    private final Map subsetCounts;
    private final PlasticStarTable dataModel;
    private final MetaColumnTableModel subsetsTableModel;
    private final Action addAct;
    private final Action tocolAct;
    private final Action countAct;
    private final Action invertAct;
    private JTable jtab;
    private JProgressBar progBar;
    private SubsetCounter activeCounter;

    /**
     * Constructs a new SubsetWindow from a TableViewer;
     *
     * @param  tcModel  model containing the data for the table concerned
     * @param  parent   component used for window positioning
     */
    public SubsetWindow( final TopcatModel tcModel, Component parent ) {
        super( null, parent );
        labelView( tcModel, "Row Subsets" );

        this.tcModel = tcModel;
        this.subsets = tcModel.getSubsets();
        this.subsetCounts = tcModel.getSubsetCounts();
        this.dataModel = tcModel.getDataModel();

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

        /* Action for adding a new subset. */
        addAct = new SubsetAction( "New subset", ResourceIcon.ADD,
                                   "Define a new subset using " +
                                   "algebraic expression" );
        addAct.setEnabled( TopcatUtils.canJel() );

        /* Action for turning a subset into a column. */
        tocolAct = new SubsetAction( "To column", ResourceIcon.TO_COLUMN,
                                     "Create new boolean column from " +
                                     "selected subset" );

        /* Action for counting subset sizes. */
        countAct = new SubsetAction( "Count rows", ResourceIcon.COUNT,
                                     "Count the number of rows in each " +
                                     "subset" );

        /* Action for producing inverse subset. */
        invertAct = new SubsetAction( "Invert subset", ResourceIcon.INVERT,
                                      "Create new subset complementary to " +
                                      "selected subset" );

        /* Add a selection listener to ensure that the right actions 
         * are enabled/disabled. */
        ListSelectionListener selList = new ListSelectionListener() {
            public void valueChanged( ListSelectionEvent evt ) {
                int nsel = jtab.getSelectedRowCount();
                boolean hasUniqueSelection = nsel == 1;
                tocolAct.setEnabled( hasUniqueSelection );
                invertAct.setEnabled( hasUniqueSelection );
            }
        };
        final ListSelectionModel selectionModel = jtab.getSelectionModel();
        selectionModel.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
        selectionModel.addListSelectionListener( selList );
        selList.valueChanged( null );

        /* Add a listener to highlight a subset if it's selected as the
         * TopcatModel's current subset (this influence deliberately doesn't
         * work the other way round). */
        tcModel.addTopcatListener( new TopcatListener() {
            public void modelChanged( TopcatEvent evt ) {
                if ( evt.getCode() == TopcatEvent.SUBSET ) {
                    selectionModel.setValueIsAdjusting( true );
                    selectionModel.clearSelection();
                    RowSubset selected = tcModel.getSelectedSubset();
                    ComboBoxModel tcSelModel = 
                        tcModel.getSubsetSelectionModel();
                    for ( int i = 0; i < tcSelModel.getSize(); i++ ) {
                        if ( tcSelModel.getElementAt( i ) == selected ) {
                            selectionModel.addSelectionInterval( i, i );
                        }
                    }
                    selectionModel.setValueIsAdjusting( false );
                }
            }
        } );
 
        /* Toolbar. */
        getToolBar().add( addAct );
        getToolBar().add( invertAct );
        getToolBar().add( tocolAct );
        getToolBar().add( countAct );
        getToolBar().addSeparator();

        /* Subsets menu. */
        JMenu subsetsMenu = new JMenu( "Subsets" );
        subsetsMenu.setMnemonic( KeyEvent.VK_S );
        subsetsMenu.add( addAct );
        subsetsMenu.add( invertAct );
        subsetsMenu.add( tocolAct );
        subsetsMenu.add( countAct );
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
        MetaColumn idCol = new MetaColumn( TopcatJELRowReader.SUBSET_ID_CHAR 
                                           + "ID", String.class ) {
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
                RowSubset rset = getSubset( irow );
                if ( rset instanceof SyntheticRowSubset ) {
                    return ((SyntheticRowSubset) rset ).getExpression();
                }
                else {
                    return null;
                }
            }
            public boolean isEditable( int irow ) {
                return getSubset( irow ) instanceof SyntheticRowSubset;
            }
            public void setValue( int irow, Object value ) {
                RowSubset rset = getSubset( irow );
                try {
                    RowSubset newSet = 
                        new SyntheticRowSubset( dataModel, subsets,
                                                rset.getName(), 
                                                value.toString() );
                    subsets.set( irow, newSet );
                    tcModel.getViewModel().fireTableDataChanged();
                }
                catch ( CompilationException e ) {
                    String[] msg = new String[] {
                        "Syntax error in algebraic subset expression \""
                        + value + "\":",
                        e.getMessage(),
                    };
                    JOptionPane.showMessageDialog( SubsetWindow.this, msg,
                                                   "Expression Syntax Error",
                                                   JOptionPane.ERROR_MESSAGE );
                }
            }
        };

        /* Column ID column for column subsets. */
        String ccname = "Column " + TopcatJELRowReader.COLUMN_ID_CHAR + "ID";
        MetaColumn colCol = new MetaColumn( ccname, String.class ) {
            public Object getValue( int irow ) {
                RowSubset rset = getSubset( irow );
                if ( rset instanceof BooleanColumnRowSubset ) {
                    ColumnInfo cinfo = ((BooleanColumnRowSubset) rset)
                                      .getColumnInfo();
                    return " " + tcModel.getColumnID( cinfo );
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
     * Returns the subset at a given row of the displayed jtable.
     *
     * @param  irow  row index
     * @return  subset at <tt>irow</tt>
     */
    private RowSubset getSubset( int irow ) {
        return (RowSubset) subsets.get( irow );
    }

    /**
     * Returns the subset ID string for the subset at a given position
     * in the subsets list (row in the presentation table).
     *
     * @param   irow  index into subsets list
     * @return  subset ID string
     */
    private String getSubsetID( int irow ) {
        return TopcatJELRowReader.SUBSET_ID_CHAR + Integer.toString( irow + 1 );
    }

    /**
     * Returns the subset name string for the subset at a given position
     * in the subsets list (row in the presentation table).
     *
     * @param   irow  index into subsets list
     * @return  subset name
     */
    private String getSubsetName( int irow ) {
        return getSubset( irow ).getName();
    }

    /**
     * Returns the subset size string for the subset at a given position
     * in the subsets list (row in the presentation table).
     *
     * @param   irow  index into subsets list
     * @return  subset count object (probably a Number or null)
     */
    private Object getSubsetSize( int irow ) {
        RowSubset rset = getSubset( irow );
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
            setBusy( false );
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
     * Implementation of actions specific to this window.
     */
    private class SubsetAction extends BasicAction {
        SubsetAction( String name, Icon icon, String description ) {
            super( name, icon, description );
        }

        public void actionPerformed( ActionEvent evt ) {
            Component parent = SubsetWindow.this;
            if ( this == addAct ) {
                new SyntheticSubsetQueryWindow( tcModel, parent );
            }

            else if ( this == tocolAct ) {
                SyntheticColumnQueryWindow colwin =
                    new SyntheticColumnQueryWindow( tcModel, -1, parent );
                int irow = jtab.getSelectedRow();
                colwin.setExpression( getSubsetID( irow ) );
                colwin.setName( getSubsetName( irow ) );
            }

            else if ( this == countAct ) {
                if ( activeCounter != null ) {
                    activeCounter.interrupt();
                }
                SubsetCounter sc = new SubsetCounter();
                activeCounter = sc;
                sc.start();
            }

            else if ( this == invertAct ) {
                int irow = jtab.getSelectedRow();
                subsets.add( new InverseRowSubset( getSubset( irow ) ) );
            }

            else {
                throw new AssertionError();
            }
        }
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
            SwingUtilities.invokeLater( new Runnable() {
                public void run() {
                    if ( activeCounter == SubsetCounter.this ) {
                        setBusy( true );
                    }
                }
            } );
            count();
            SwingUtilities.invokeLater( new Runnable() {
                public void run() {
                    if ( activeCounter == SubsetCounter.this ) {
                        activeCounter = null;
                        setBusy( false );
                    }
                }
            } );
        }
   
        /**
         * Count the members of each known RowSubset.  When completed, 
         * the subsetCounts map is updated and the subsets list notified
         * that there are changes.
         */
        void count() {
   
            /* Prepare for the calculations. */
            final RowSubset[] rsets = subsets == null 
                                    ? new RowSubset[ 0 ]
                                    : (RowSubset[]) 
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
            long every = nrow / 200L;
            long counter = 0;
            for ( currentRow = 0; currentRow < nrow && ! interrupted();
                  currentRow++ ) {
                if ( --counter < 0 ) {
                    SwingUtilities.invokeLater( updater );
                    counter = every;
                }
                for ( int i = 0; i < nrset; i++ ) {
                    RowSubset rset = rsets[ i ];
                    if ( rset.isIncluded( currentRow ) ) {
                        counts[ i ]++;
                    }
                }
            }
            SwingUtilities.invokeLater( updater );

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
                            progBar.setValue( 0 );
                        }
                    }
                } );
            }
        }
    }

}
