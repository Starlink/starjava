package uk.ac.starlink.topcat;

import java.util.ArrayList;
import java.util.List;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.gui.StarTableColumn;

/**
 * Does a similar job as for ColumnComboBoxModel, but when only a subset
 * of the available columns should show up in the combobox.
 * Use it in the same way as ColumnComboBoxModel, but implement the
 * {@link #acceptColumn} method appropriately.  In the case that
 * <code>acceptColumn</code> accepts everything, this will behave just the
 * same as a ColumnComboBoxModel (though perhaps less efficiently).
 */
public abstract class RestrictedColumnComboBoxModel 
              extends ColumnComboBoxModel {

    private TableColumnModel colModel;
    private boolean hasNone;
    private List<TableColumn> activeColumns;
    private List<TableColumn> modelColumns;

    public RestrictedColumnComboBoxModel( TableColumnModel colModel,
                                          boolean hasNone ) {
        super( colModel, hasNone );
        this.colModel = colModel;
        this.hasNone = hasNone;
        activeColumns = new ArrayList<TableColumn>();
        modelColumns = new ArrayList<TableColumn>();
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
     * Determines whether a column with given metadata in the column 
     * model should show up in the combobox.
     *
     * @param   cinfo  column metadata to assess
     * @return  <code>true</code> iff the column is to be used
     */
    public abstract boolean acceptColumn( ColumnInfo cinfo );

    /**
     * Determines whether a given TableColumn should show up in the combobox.
     *
     * @param  tcol  table column to assess
     * @return  <code>true</code> iff the column is to be used
     */
    private boolean acceptColumn( TableColumn tcol ) {
        return tcol instanceof StarTableColumn 
            && acceptColumn( ((StarTableColumn) tcol).getColumnInfo() );
    }

    public TableColumn getElementAt( int index ) {
        return activeColumns.get( index );
    }

    public int getSize() {
        return activeColumns.size();
    }

    public void columnAdded( TableColumnModelEvent evt ) {
        int index = evt.getToIndex();
        TableColumn tcol = colModel.getColumn( index );
        modelColumns.add( tcol );
        if ( acceptColumn( tcol ) ) {
            int pos = activeColumns.size();
            activeColumns.add( tcol );
            fireIntervalAdded( this, pos, pos );
        }
    }

    public void columnRemoved( TableColumnModelEvent evt ) {
        int index = evt.getFromIndex();
        TableColumn tcol = modelColumns.get( index );
        modelColumns.remove( tcol );
        int pos = activeColumns.indexOf( tcol );
        if ( pos >= 0 ) {
            activeColumns.remove( pos );
            fireIntervalRemoved( this, pos, pos );
        }
    }

    public void columnMoved( TableColumnModelEvent evt ) {
        int from = evt.getFromIndex();
        TableColumn tcol = modelColumns.get( from );
        if ( activeColumns.contains( tcol ) ) {
            List<TableColumn> oldActive = activeColumns;
            activeColumns = new ArrayList<TableColumn>();
            modelColumns = new ArrayList<TableColumn>();
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

    /**
     * Returns a column combo box model which checks for compatibility
     * with a given class.  Class matching is not strict in the following
     * sense: any numeric class ({@link java.lang.Number}) is considered 
     * assignable to any other numeric class.
     *
     * @param  colModel   the column model
     * @param  hasNone    true if you want a NO_COLUMN entry
     * @param  clazz      the class that available columns have to have
     *                    data assignable to
     */
    public static RestrictedColumnComboBoxModel makeClassColumnComboBoxModel(
            TableColumnModel colModel, boolean hasNone, Class<?> clazz ) {
        if ( Number.class.isAssignableFrom( clazz ) ) {
            clazz = Number.class;
        }
        final Class<?> effClazz = clazz;
        return new RestrictedColumnComboBoxModel( colModel, hasNone ) {
            public boolean acceptColumn( ColumnInfo colInfo ) {
                return effClazz.isAssignableFrom( colInfo.getContentClass() );
            }
        };
    }
}
