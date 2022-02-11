package uk.ac.starlink.topcat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/**
 * Supplies a TableModel which can be displayed and interacted with by
 * a user to select one or more TopcatModels from the currently loaded list.
 * The table entries are automatically updated in sync with changes to
 * the loaded list.  The entries of the table may be extended by subclassing.
 *
 * @author    Mark Taylor
 * @since     3 Aug 2010
 */
public class TopcatModelSelectionTable {

    private final boolean defaultSelected_;
    private final ListModel<TopcatModel> tableList_;
    private final Set<TopcatModel> selectedSet_;
    private final TopcatListener tcListener_;
    private final MetaColumnTableModel tModel_;
    private final int icolFlag_;
    private final int icolName_;
    private Set<TopcatModel> allSet_;
    
    /**
     * Constructor.
     *
     * @param  selectLabel   label for the selection column
     * @param  defaultSelected  wether entries will be selected by default
     */
    public TopcatModelSelectionTable( String selectLabel,
                                      boolean defaultSelected ) {
        defaultSelected_ = defaultSelected;
        tableList_ = ControlWindow.getInstance().getTablesListModel();
        allSet_ = new HashSet<TopcatModel>();
        selectedSet_ = new HashSet<TopcatModel>();

        /* Listener to ensure that table characteristics are kept up to date. */
        tcListener_ = new TopcatListener() {
            public void modelChanged( TopcatEvent evt ) {
                int[] icols = getEventColumnIndices( evt.getCode() );
                if ( icols.length > 0 ) {
                    int irow = getRowIndex( evt.getModel() );
                    for ( int ix = 0; ix < icols.length; ix++ ) {
                        tModel_.fireTableCellUpdated( irow, icols[ ix ] );
                    }
                }
            }
        };

        /* Listener to ensure that the table list is kept up to date. */
        tableList_.addListDataListener( new ListDataListener() {
            public void intervalAdded( ListDataEvent evt ) {
                tModel_.fireTableRowsInserted( evt.getIndex0(),
                                               evt.getIndex1() );
                updateTopcatListeners();
            }
            public void intervalRemoved( ListDataEvent evt ) {
                tModel_.fireTableRowsDeleted( evt.getIndex0(),
                                              evt.getIndex1() );
                updateTopcatListeners();
            }
            public void contentsChanged( ListDataEvent evt ) {
                int ir0 = evt.getIndex0();
                int ir1 = evt.getIndex1();
                if ( ir0 >= 0 && ir1 < tModel_.getRowCount() ) {
                    for ( int irow = ir0; irow <= ir1; irow++ ) {
                        tModel_.fireTableCellUpdated( irow, icolName_ );
                    }
                }
                else {
                    /* Try to avoid a fireTableStructureChanged - see 
                     * dreadful java bug 4243683. */
                    tModel_.fireTableStructureChanged();
                }
                updateTopcatListeners();
            }
        } );

        /* Prepare the columns which will constitute the displayed table. */
        List<MetaColumn> metaList = new ArrayList<MetaColumn>();

        /* Define column with a selector checkbox. */
        icolFlag_ = metaList.size();
        metaList.add( new MetaColumn( selectLabel, Boolean.class,
                                      "Select this table for "
                                    + selectLabel + "?" ) {
            public Object getValue( int irow ) {
                return Boolean
                      .valueOf( selectedSet_.contains( getTable( irow ) ) );
            }
            public boolean isEditable( int irow ) {
                return true;
            }
            public void setValue( int irow, Object value ) {
                TopcatModel tcModel = getTable( irow );
                if ( Boolean.TRUE.equals( value ) ) {
                    selectedSet_.add( tcModel );
                }
                else if ( Boolean.FALSE.equals( value ) ) {
                    selectedSet_.remove( tcModel );
                }
                else {
                    assert false;
                }
            }
        } );

        /* Define column with table name. */
        icolName_ = metaList.size();
        metaList.add( new MetaColumn( "Table", String.class, "Table ID" ) {
            public Object getValue( int irow ) {
                return getTable( irow ).toString();
            }
        } );

        /* Construct the table model. */
        tModel_ = new MetaColumnTableModel( metaList ) {
            public int getRowCount() {
                return tableList_.getSize();
            }
        };
        updateTopcatListeners();
    }

    /**
     * Returns zero or more column indices which may be affected by a
     * TopcatEvent of with a given code.
     *
     * @param   evtCode  code from a TopcatEvent
     * @return   array of table column indices whose values in the row
     *           pertaining to the relevant TopcatModel may be changed
     */
    protected int[] getEventColumnIndices( int evtCode ) {
        if ( evtCode == TopcatEvent.LABEL ) {
            return new int[] { icolName_ }; 
        }
        else {
            return new int[ 0 ];
        }
    }

    /**
     * Returns the table model used for containing information about each
     * TopcatModel.  This can be displayed within a JTable, and it can
     * have additional columns added as required.
     *
     * @return  table model for display
     */
    public MetaColumnTableModel getTableModel() {
        return tModel_;
    }

    /**
     * Returns the TopcatModels currently selected in this component.
     *
     * @return   array of currently selected tables
     */
    public TopcatModel[] getSelectedTables() {
        List<TopcatModel> selectedList = new ArrayList<TopcatModel>();
        for ( int irow = 0; irow < tableList_.getSize(); irow++ ) {
            if ( Boolean.TRUE
                .equals( tModel_.getValueAt( irow, icolFlag_ ) ) ) {
                selectedList.add( tableList_.getElementAt( irow ) );
            }
        }
        return selectedList.toArray( new TopcatModel[ 0 ] );
    }

    /**
     * Returns the table at a given row in the displayed JTable.
     *
     * @param   irow  row index
     * @return  table
     */
    public TopcatModel getTable( int irow ) {
        return tableList_.getElementAt( irow );
    }

    /**
     * Determines the row index for a given table.
     *
     * @param  tcModel  table to locate
     * @return   row index
     */
    public int getRowIndex( TopcatModel tcModel ) {
        for ( int i = 0; i < tableList_.getSize(); i++ ) {
            if ( tableList_.getElementAt( i ) == tcModel ) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Performs wholesale selection or deselection of all tables in the list.
     *
     * @param  isSelect  true to select, false to deselect
     */
    public void setAllSelected( boolean isSelect ) {
        Boolean isSel = Boolean.valueOf( isSelect );
        int nt = tableList_.getSize();
        for ( int it = 0; it < nt; it++ ) {
            tModel_.setValueAt( isSel, it, icolFlag_ );
        }
    }

    /**
     * Makes sure that we're listening to all the right TopcatModels.
     * This should be called whenever the content of the set of tables
     * currently being displayed changes.
     */
    private void updateTopcatListeners() {

        /* Find out which tables are currently displayed. */
        Set<TopcatModel> allSet = new HashSet<TopcatModel>();
        for ( int i = 0; i < tableList_.getSize(); i++ ) {
            allSet.add( tableList_.getElementAt( i ) );
        }

        /* Compare this with the set we had from last time to find out which
         * ones are new, and which ones are no longer required. */
        Set<TopcatModel> added = new HashSet<TopcatModel>( allSet );
        added.removeAll( allSet_ );
        Set<TopcatModel> removed = new HashSet<TopcatModel>( allSet_ );
        removed.removeAll( allSet );
        allSet_ = allSet;

        /* Remove and add listeners as required. */
        for ( TopcatModel m : removed ) {
            m.removeTopcatListener( tcListener_ );
            selectedSet_.remove( m );
        }
        for ( TopcatModel m : added ) {
            m.addTopcatListener( tcListener_ );
            if ( defaultSelected_ ) {
                selectedSet_.add( m );
            }
        }
    }
}
