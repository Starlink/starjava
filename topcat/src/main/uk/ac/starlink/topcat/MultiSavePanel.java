package uk.ac.starlink.topcat;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.ListModel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.gui.TableSaveChooser;

/**
 * Save panel for saving multiple tables to the same container file.
 *
 * @author   Mark Taylor
 * @since    15 Jul 2010
 */
public class MultiSavePanel extends SavePanel {

    private final ListModel tableList_;
    private final Set<TopcatModel> saveSet_;
    private final AbstractTableModel tModel_;
    private final TopcatListener tcListener_;
    private final int icolFlag_;
    private final int icolName_;
    private final int icolSubset_;
    private final int icolOrder_;
    private final boolean defaultSave_ = true;
    private Set<TopcatModel> allSet_;

    /**
     * Constructor.
     *
     * @param  saveChooser  chooser
     * @param  sto  output marshaller
     */
    public MultiSavePanel( TableSaveChooser saveChooser,
                           StarTableOutput sto ) {
        super( "Multiple Tables", saveChooser,
               TableSaveChooser.makeFormatBoxModel( sto, true ) );
        allSet_ = new HashSet<TopcatModel>();
        saveSet_ = new HashSet<TopcatModel>();
        tableList_ = ControlWindow.getInstance().getTablesListModel();

        /* Listener to ensure that table characteristics are kept up to date. */
        tcListener_ = new TopcatListener() {
            public void modelChanged( TopcatEvent evt ) {
                TopcatModel tcModel = evt.getModel();
                int code = evt.getCode();
                if ( code == TopcatEvent.LABEL ) {
                    tModel_.fireTableCellUpdated( getRowIndex( tcModel ),
                                                  icolName_ );
                }
                else if ( code == TopcatEvent.CURRENT_SUBSET ) {
                    tModel_.fireTableCellUpdated( getRowIndex( tcModel ),
                                                  icolSubset_ );
                }
                else if ( code == TopcatEvent.CURRENT_ORDER ) {
                    tModel_.fireTableCellUpdated( getRowIndex( tcModel ),
                                                  icolOrder_ );
                }
            }
        };

        /* Listener to ensure that table list is kept up to date. */
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
                tModel_.fireTableStructureChanged();
                updateTopcatListeners();
            }
        } );

        /* Prepare model for JTable displaying available tables. */
        List<MetaColumn> metaList = new ArrayList<MetaColumn>();

        icolFlag_ = metaList.size();
        metaList.add( new MetaColumn( "Save", Boolean.class,
                                      "Save this table?" ) {
            public Object getValue( int irow ) {
                return Boolean.valueOf( saveSet_.contains( getTable( irow ) ) );
            }
            public boolean isEditable( int irow ) {
                return true;
            }
            public void setValue( int irow, Object value ) {
                TopcatModel tcModel = getTable( irow );
                if ( Boolean.TRUE.equals( value ) ) {
                    saveSet_.add( tcModel );
                }
                else if ( Boolean.FALSE.equals( value ) ) {
                    saveSet_.remove( tcModel );
                }
                else {
                    assert false;
                }
            }
        } );

        icolName_ = metaList.size();
        metaList.add( new MetaColumn( "Table", String.class, "Table ID" ) {
            public Object getValue( int irow ) {
                return getTable( irow ).toString();
            }
        } );

        icolSubset_ = metaList.size();
        metaList.add( new MetaColumn( "Subset", String.class,
                                      "Current Subset" ) {
            public Object getValue( int irow ) {
                RowSubset subset = getTable( irow ).getSelectedSubset();
                return RowSubset.ALL.equals( subset ) ? null
                                                      : subset.toString();
            }
        } );

        icolOrder_ = metaList.size();
        metaList.add( new MetaColumn( "Order", String.class, "Sort Order" ) {
            public Object getValue( int irow ) {
                return getTable( irow ).getSelectedSort().toString();
            }
        } );

        tModel_ = new MetaColumnTableModel( metaList ) {
            public int getRowCount() {
                return tableList_.getSize();
            }
        };
        updateTopcatListeners();

        /* Place components. */
        setLayout( new BorderLayout() );
        JTable jtable = new JTable( tModel_ );
        jtable.setRowSelectionAllowed( false );
        jtable.setColumnSelectionAllowed( false );
        jtable.setCellSelectionEnabled( false );
        TableColumnModel colModel = jtable.getColumnModel();
        colModel.getColumn( icolFlag_ ).setPreferredWidth( 32 );
        colModel.getColumn( icolFlag_ ).setMaxWidth( 32 );
        colModel.getColumn( icolFlag_ ).setMinWidth( 32 );
        colModel.getColumn( icolSubset_ ).setPreferredWidth( 64 );
        colModel.getColumn( icolName_ ).setPreferredWidth( 300 );
        colModel.getColumn( icolOrder_ ).setPreferredWidth( 64 );
        JScrollPane scroller =
            new JScrollPane( jtable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                     JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
        add( scroller, BorderLayout.CENTER );
    }

    public StarTable[] getTables() {
        List<StarTable> saveList = new ArrayList<StarTable>();
        for ( int irow = 0; irow < tableList_.getSize(); irow++ ) {
            if ( Boolean.TRUE
                .equals( tModel_.getValueAt( irow, icolFlag_ ) ) ) {
                saveList.add( ((TopcatModel) tableList_.getElementAt( irow ))
                             .getApparentStarTable() );
            }
        }
        return saveList.toArray( new StarTable[ 0 ] );
    }

    /**
     * Returns the table at a given row in the displayed JTable.
     *
     * @param   irow  row index
     * @return  table
     */
    private TopcatModel getTable( int irow ) {
        return (TopcatModel) tableList_.getElementAt( irow );
    }

    /**
     * Determines the row index for a given table.
     *
     * @param  tcModel  table to locate
     * @return   row index
     */
    private int getRowIndex( TopcatModel tcModel ) {
        for ( int i = 0; i < tableList_.getSize(); i++ ) {
            if ( tableList_.getElementAt( i ) == tcModel ) {
                return i;
            }
        }
        return -1;
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
            allSet.add( (TopcatModel) tableList_.getElementAt( i ) );
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
            saveSet_.remove( m );
        }
        for ( TopcatModel m : added ) {
            m.addTopcatListener( tcListener_ );
            if ( defaultSave_ ) {
                saveSet_.add( m );
            }
        }
    }
}
