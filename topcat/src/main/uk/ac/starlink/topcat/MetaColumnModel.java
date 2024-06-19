package uk.ac.starlink.topcat;

import java.util.BitSet;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JMenu;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

/**
 * This ColumnModel provides enhanced functionality over a normal
 * ColumnModel, aimed at making it easy to select which columns
 * in a related TableModel are shown (appear in this ColumnModel)
 * or not shown (don't appear in this ColumnModel).
 * It does this by using an associated ListModel and ListSelectionModel.
 */
public class MetaColumnModel extends DefaultTableColumnModel {

    private final DefaultListModel<TableColumn> listModel_;
    private final ListSelectionModel visibleModel_;
    private final TableModel tableModel_;
    private final BitSet purgedColumns_;

    /**
     * Constructs a new MetaColumnModel from a base ColumnModel and a
     * TableModel.  The base ColumnModel is taken as the initial list
     * of columns that can appear in this ColumnModel, but they can
     * be deleted from and reinstated to this model without affecting
     * the base.  Subsequent changes to the base don't change this either.
     *
     * @param  baseColumnModel  the base ColumnModel
     * @param  tableModel       the TableModel which supplies the data
     *         for the columns this object describes; must refer to the
     *         same data as baseColumnModel
     */
    public MetaColumnModel( TableColumnModel baseColumnModel,
                            TableModel tableModel ) {
        tableModel_ = tableModel;
        purgedColumns_ = new BitSet();

        /* Initialise state of this ColumnModel from the base column model. */
        for ( int ipos = 0; ipos < baseColumnModel.getColumnCount(); ipos++ ) {
            super.addColumn( baseColumnModel.getColumn( ipos ) );
        }
        setColumnMargin( baseColumnModel.getColumnMargin() );
        setSelectionModel( baseColumnModel.getSelectionModel() );
        setColumnSelectionAllowed( baseColumnModel
                                  .getColumnSelectionAllowed() );

        /* Create and initialise a ListModel representing all the columns
         * in the original column model. */
        listModel_ = new DefaultListModel<TableColumn>();
        for ( int ipos = 0; ipos < baseColumnModel.getColumnCount(); ipos++ ) {
            listModel_.addElement( baseColumnModel.getColumn( ipos ) );
        }

        /* Create and initialise a ListSelectionModel representing 
         * which columns appear in this ColumnModel. */
        visibleModel_ = new DefaultListSelectionModel();
        for ( int i = 0; i < listModel_.getSize(); i++ ) {
            if ( isVisible( i ) ) {
                visibleModel_.addSelectionInterval( i, i );
            }
        }

        /* Make sure that this column model reflects any changes to the
         * visible model. */
        visibleModel_.addListSelectionListener( new ListSelectionListener() {
            public void valueChanged( ListSelectionEvent evt ) {
                handleSelectionChange( evt );
            }
        } );

        /* Make sure that if the table model changes so that something
         * interesting comes up, the relevant column is displayed. */
        tableModel_.addTableModelListener( new TableModelListener() {
            public void tableChanged( TableModelEvent evt ) {
                handleTableChange( evt );
            }
        } );
    }

    /**
     * Returns the ListModel representing all the columns in the 
     * original TableColumnModel.
     * 
     * @return  the list model 
     */
    public ListModel<TableColumn> getListModel() {
        return listModel_;
    }

    /**
     * Returns the ListSelectionModel representing which columns in the 
     * original TableColumnModel are currently visible (are represented in 
     * this ColumnModel) and which are invisible (are not represented in 
     * this ColumnModel).
     *
     * @return  the selection model
     */
    public ListSelectionModel getVisibleModel() {
        return visibleModel_;
    }

    /**
     * Indicates whether this TableColumnModel currently contains the
     * column at a given index in the list model.
     *
     * @param  i  the column we want to know about
     * @return  true iff this column model contains the column at index
     *          <code>i</code> in the listModel
     */
    private boolean isVisible( int i ) {

        /* Use DefaultTableColumnModel's protected tableColumns Vector to
         * answer the question. */
        return tableColumns.contains( listModel_.get( i ) );
    }

    /**
     * When a column is added to this TableColumnModel, add it to the
     * listModel and set it visible in the visibleModel
     */
    public void addColumn( TableColumn tcol ) {
        super.addColumn( tcol );
        if ( ! listModel_.contains( tcol ) ) {
            int insertPos = listModel_.getSize();
            listModel_.addElement( tcol );
            visibleModel_.addSelectionInterval( insertPos, insertPos );
            purgedColumns_.clear( insertPos );
        }
    }

    /**
     * When a column is removed from this TableColumnModel, set it 
     * invisible in the visibleModel.
     */
    public void removeColumn( TableColumn tcol ) {
        super.removeColumn( tcol );
        int ipos = listModel_.indexOf( tcol );
        if ( ipos >= 0 ) {
            visibleModel_.removeSelectionInterval( ipos, ipos );
            purgedColumns_.clear( ipos );
        }
    }

    /**
     * Sets a column invisible.  The column is identified by its position
     * in the list model, equal to its position in the visible model,
     * equal to its position in the base column model from which this
     * model was initialised.
     *
     * @param  ipos  position of the column to remove
     */
    public void removeColumn( int ipos ) {
        removeColumn( listModel_.get( ipos ) );
    }

    /**
     * Purges this column model of any column which contains nothing but
     * blank entries.  Columns purged in this way can be reinstated 
     * later by modifying the visibleModel.  Such a column will also
     * become visible automatically if at a later date it acquires a
     * non-blank entry.
     */
    public void purgeEmptyColumns() {
        int nrow = tableModel_.getRowCount();
        for ( int ipos = 0; ipos < listModel_.getSize(); ipos++ ) {
            TableColumn tcol = listModel_.get( ipos );
            int modelIndex = tcol.getModelIndex();
            boolean stillBlank = true;
            for ( int irow = 0; irow < nrow && stillBlank; irow++ ) {
                Object val = tableModel_.getValueAt( irow, modelIndex );
                stillBlank = stillBlank
                          && ( val == null || 
                               val.toString().trim().length() == 0 );
            }
            if ( stillBlank ) {
                removeColumn( tcol );
                purgedColumns_.set( ipos );
            }
        }
    }

    /**
     * Returns a menu component which can be used to control the visibility
     * of columns in the model.
     *
     * @param   name  the name of the menu
     * @return  a new JMenu with checkboxes for each column
     */
    public JMenu makeCheckBoxMenu( String name ) {
        CheckBoxMenu menu = new CheckBoxMenu( name );
        for ( int i = 0; i < listModel_.getSize(); i++ ) {
            TableColumn tcol = listModel_.getElementAt( i );
            Object title = tcol.getHeaderValue();
            menu.addMenuItem( title == null ? null : title.toString() );
        }
        menu.setSelectionModel( visibleModel_ );
        return menu;
    }

    /**
     * This method is called when this object's visibleModel's selection
     * status changes.  It's not a direct implementation of the 
     * ListSelectionListener.valueChanged(ListSelectionEvent) method,
     * since TableColumnModel already implements ListSelectionListener
     * so it would get called for the wrong events.
     *
     * @param   evt  the event to handle
     */
    private void handleSelectionChange( ListSelectionEvent evt ) {

        /* Look at each item in the visible list which may have
         * changed. */
        for ( int i = evt.getFirstIndex(); i <= evt.getLastIndex(); i++ ) {

            /* If a column has turned invisible, remove it from this
             * column model. */
            if ( ! visibleModel_.isSelectedIndex( i ) && 
                 isVisible( i ) ) {
                TableColumn tcol = listModel_.get( i );
                removeColumn( tcol );
            }

            /* If a column has turned visible, add it to this 
             * column model. */
            else if ( visibleModel_.isSelectedIndex( i ) && ! isVisible( i ) ) {

                /* Add the column in question. */
                TableColumn tcol = listModel_.get( i );
                addColumn( tcol );

                /* Try to place it in the order of the original
                 * column model, though if the columns have been
                 * moved around by the user it might end up somewhere
                 * funny. */
                int colPos = listModel_.indexOf( tcol );
                int ipos = 0;
                while ( ipos < getColumnCount() && 
                        listModel_.indexOf( getColumn( ipos ) ) < colPos ) {
                    ipos++;
                }
                int from = getColumnCount() - 1;
                if ( ipos < from && ipos >= 0 ) {
                    moveColumn( from, ipos );
                }
            }
        }
    }

    /**
     * Called when this object's associated TableModel changes - if an
     * invisible column starts to look interesting, expose it.
     *
     * @param   evt  the event to handle
     */
    private void handleTableChange( TableModelEvent evt ) {
        if ( evt.getType() == TableModelEvent.DELETE ) {
            return;
        }
        TableModel tableModel_ = (TableModel) evt.getSource();
        int first = Math.max( evt.getFirstRow(), 0 );
        int last = Math.min( evt.getLastRow(), tableModel_.getRowCount() - 1 );
        for ( int ipos = 0; ipos < listModel_.size(); ipos++ ) {
            if ( purgedColumns_.get( ipos ) ) {
                for ( int irow = first; irow <= last; irow++ ) {
                    TableColumn tcol = listModel_.get( ipos );
                    int modelIndex = tcol.getModelIndex();
                    Object val = tableModel_.getValueAt( irow, modelIndex );
                    if ( ! isVisible( ipos ) &&
                         ( val != null && 
                           val.toString().trim().length() > 0 ) ) {
                        visibleModel_.addSelectionInterval( ipos, ipos );
                    }
                }
            }
        }
    }

}
