package uk.ac.starlink.topcat;

import java.util.ArrayList;
import java.util.List;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

/**
 * Does a similar job as for ColumnComboBoxModel, but when only a subset
 * of the available columns should show up in the combobox.
 * Use it in the same way as ColumnComboBoxModel, but implement the
 * {@link #acceptColumn} method appropriately.  In the case that
 * <tt>acceptColumn</tt> accepts everything, this will behave just the
 * same as a ColumnComboBoxModel (though perhaps less efficiently).
 */
public abstract class RestrictedColumnComboBoxModel 
              extends ColumnComboBoxModel {

    private TableColumnModel colModel;
    private boolean hasNone;
    private List activeColumns;
    private List modelColumns;

    public RestrictedColumnComboBoxModel( TableColumnModel colModel,
                                          boolean hasNone ) {
        super( colModel, hasNone );
        this.colModel = colModel;
        this.hasNone = hasNone;
        activeColumns = new ArrayList();
        modelColumns = new ArrayList();
        if ( hasNone ) {
            activeColumns.add( NO_COLUMN );
        }
        for ( int i = 0; i < colModel.getColumnCount(); i++ ) {
            TableColumn tcol = colModel.getColumn( i );
            modelColumns.add( tcol );
            if ( acceptColumn( tcol ) ) {
                activeColumns.add( tcol );
            }
        }
    }

    /**
     * Determines whether a given column in the column model should show
     * up in the combobox.
     *
     * @param   tcol  the column to assess
     * @return  <tt>true</tt> iff the column is to be used
     */
    protected abstract boolean acceptColumn( TableColumn tcol );

    public synchronized Object getElementAt( int index ) {
        return activeColumns.get( index );
    }

    public synchronized int getSize() {
        return activeColumns.size();
    }

    public synchronized void columnAdded( TableColumnModelEvent evt ) {
        int index = evt.getToIndex();
        TableColumn tcol = colModel.getColumn( index );
        modelColumns.add( tcol );
        if ( acceptColumn( tcol ) ) {
            int pos = activeColumns.size();
            activeColumns.add( tcol );
            fireIntervalAdded( this, pos, pos );
        }
    }

    public synchronized void columnRemoved( TableColumnModelEvent evt ) {
        int index = evt.getFromIndex();
        TableColumn tcol = (TableColumn) modelColumns.get( index );
        modelColumns.remove( tcol );
        int pos = activeColumns.indexOf( tcol );
        if ( pos >= 0 ) {
            activeColumns.remove( pos );
            fireIntervalRemoved( this, pos, pos );
        }
    }

    public synchronized void columnMoved( TableColumnModelEvent evt ) {
        int from = evt.getFromIndex();
        TableColumn tcol = (TableColumn) modelColumns.get( from );
        if ( activeColumns.contains( tcol ) ) {
            List oldActive = activeColumns;
            activeColumns = new ArrayList();
            modelColumns = new ArrayList();
            if ( hasNone ) {
                activeColumns.add( NO_COLUMN );
            }
            for ( int i = 0; i < colModel.getColumnCount(); i++ ) {
                TableColumn tc = colModel.getColumn( i );
                modelColumns.add( tc );
                if ( oldActive.contains( tc ) ) {
                    activeColumns.add( tc );
                }
            }
            int index0 = hasNone ? 1 : 0;
            int index1 = activeColumns.size() - 1;
            fireContentsChanged( this, index0, index1 );
        }
    }

}
