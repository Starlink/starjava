package uk.ac.starlink.topcat;

import gnu.jel.CompilationException;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Action;
import javax.swing.ListSelectionModel;
import javax.swing.JMenu;
import javax.swing.JTable;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
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
    private final PlasticStarTable dataModel;
    private final AbstractTableModel subsetsTableModel;
    private JTable jtab;

    /**
     * Constructs a new SubsetWindow from a TableViewer;
     *
     * @param   tableviewer the viewer whose table is to be reflected here
     */
    public SubsetWindow( TableViewer tableviewer ) {
        super( "Row Subsets", tableviewer );
        this.tv = tableviewer;
        this.subsets = tv.getSubsets();
        this.dataModel = tv.getDataModel();

        /* Get a model for the table containing the bulk of the data. */
        subsetsTableModel = makeTableModel();

        /* Prepare to update dynamically in response to changes to the 
         * subset list. */
        subsets.addListDataListener( this );

        /* Construct and place a JTable to contain it. */
        jtab = new JTable( subsetsTableModel );
        jtab.setColumnSelectionAllowed( false );
        jtab.setRowSelectionAllowed( true );

        /* Configure column widths. */
        TableColumnModel tcm = jtab.getColumnModel();
        int icol = 0;
        tcm.getColumn( icol++ ).setPreferredWidth( 64 );
        tcm.getColumn( icol++ ).setPreferredWidth( 200 );
        tcm.getColumn( icol++ ).setPreferredWidth( 200 );
        tcm.getColumn( icol++ ).setPreferredWidth( 80 );

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
        getToolBar().addSeparator();

        /* Menu. */
        JMenu subsetsMenu = new JMenu( "Subsets" );
        subsetsMenu.add( addAction ).setIcon( null );
        subsetsMenu.add( tocolAction ).setIcon( null );
        getJMenuBar().add( subsetsMenu );

        /* Add standard help actions. */
        addHelp( "RowSubset" );

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
    public AbstractTableModel makeTableModel() {

        /* ID column. */
        MetaColumn idCol = new MetaColumn( "#ID", String.class, false  ) {
            public Object getValue( int irow ) {
                return getSubsetID( irow );
            }
        };

        /* Name column. */
        MetaColumn nameCol = new MetaColumn( "Name", String.class, false ) {
            public Object getValue( int irow ) {
                return getSubsetName( irow );
            }
        };

        /* Expression column for algebraic subsets. */
        MetaColumn exprCol = new MetaColumn( "Expression", String.class, 
                                              true ) {
            public Object getValue( int irow ) {
                RowSubset rset = (RowSubset) subsets.get( irow );
                if ( rset instanceof SyntheticRowSubset ) {
                    return ((SyntheticRowSubset) rset ).getExpression();
                }
                else {
                    return null;
                }
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
        MetaColumn colCol = new MetaColumn( "Column $ID", String.class,
                                            false ) {
            public Object getValue( int irow ) {
                RowSubset rset = (RowSubset) subsets.get( irow );
                if ( rset instanceof BooleanColumnRowSubset ) {
                    ColumnInfo cinfo = ((BooleanColumnRowSubset) rset)
                                      .getColumnInfo();
                    return tv.getColumnID( cinfo );
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

}
