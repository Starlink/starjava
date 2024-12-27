package uk.ac.starlink.util.gui;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Comparator;
import javax.swing.table.AbstractTableModel;

/**
 * TableModel which contains its data as an array of objects, one per row.
 * The {@link ArrayTableSorter} class can be used in conjunction with 
 * this class to provide tables which can be sorted by clicking on 
 * column headers.
 * The model works by having an array of column objects which know how to 
 * interrogate the array of data objects in such a way as to return cell
 * values.  The implementation of the column and data item objects 
 * is therefore interlinked.
 *
 * @author   Mark Taylor
 * @since    14 Oct 2009
 */
public class ArrayTableModel<R> extends AbstractTableModel {

    private ArrayTableColumn<R,?>[] columns_;
    private R[] items_;

    /**
     * Constructor.
     *
     * @param  items  initial list of items
     */
    public ArrayTableModel( R[] items ) {
        items_ = items;
        @SuppressWarnings("unchecked")
        ArrayTableColumn<R,?>[] columns =
            (ArrayTableColumn<R,?>[]) new ArrayTableColumn<?,?>[ 0 ];
        columns_ = columns;
    }

    /**
     * Sets the column specifiers to be used by this table.
     *
     * @param   columns  column specifiers
     */
    public void setColumns( List<? extends ArrayTableColumn<? extends R,?>>
                                 columns ) {
        @SuppressWarnings("unchecked")
        ArrayTableColumn<R,?>[] tcols =
            (ArrayTableColumn<R,?>[])
            columns.toArray( new ArrayTableColumn<?,?>[ 0 ] );
        columns_ = tcols;
        fireTableStructureChanged();
    }

    /**
     * Returns the column specifiers used by this table.
     *
     * @return   column specifiers
     */
    public List<ArrayTableColumn<R,?>> getColumns() {
        return Collections.unmodifiableList( Arrays.asList( columns_ ) );
    }

    /**
     * Sets the row data items to be used by this table.
     *
     * @param  items  row data items, one per row
     */
    public void setItems( R[] items ) {
        items_ = items;
        fireTableDataChanged();
    }

    /**
     * Returns the row data items used by this model.
     *
     * @return  row data items, one per row
     */
    public R[] getItems() {
        return items_;
    }

    /**
     * Sorts the rows in this table according to the ordering of the 
     * data in one of the columns.
     * The ordering may not be maintained if the data changes.
     *
     * @param  icol  index of column to sort by
     * @param  descending  true to sort down, false to sort up
     */
    public void sortByColumn( int icol, boolean descending ) {
        ArrayTableColumn<R,?> col = columns_[ icol ];
        if ( Comparable.class.isAssignableFrom( col.getContentClass() ) ) {
            Comparator<R> comparator = new ColumnComparator( col, descending );
            if ( needsSort( items_, comparator ) ) {
                Arrays.sort( items_, comparator );
                assert ! needsSort( items_, comparator );
                fireTableDataChanged();
            }
        }
    }

    /**
     * Determines whether the array of items is currently sorted or not.
     *
     * @param  items  item array
     * @param  cmp   comparator
     * @return   true iff items is not in sorted order
     */
    private boolean needsSort( R[] items, Comparator<R> cmp ) {
        for ( int i = 1; i < items.length; i++ ) {
            if ( cmp.compare( items[ i - 1 ], items[ i ] ) > 0 ) {
                return true;
            }
        }
        return false;
    }

    public int getColumnCount() {
        return columns_.length;
    }

    public int getRowCount() {
        return items_.length;
    }

    public Object getValueAt( int irow, int icol ) {
        return columns_[ icol ].getValue( items_[ irow ] );
    }

    public String getColumnName( int icol ) {
        return columns_[ icol ].getName();
    }

    public Class<?> getColumnClass( int icol ) {
        return columns_[ icol ].getContentClass();
    }

    /**
     * Can compare two row data items according to the value of a column.
     */
    private class ColumnComparator implements Comparator<R> {
        private final ArrayTableColumn<R,?> col_;
        private final int sense_;

        /**
         * Constructor.
         *
         * @param   col   column whose contents determines sort order
         * @param   descending  false for ascending, true for descending;
         *                      nulls are always at the bottom
         */
        public ColumnComparator( ArrayTableColumn<R,?> col,
                                 boolean descending ) {
            col_ = col;
            sense_ = descending ? -1 : +1;
        }

        public int compare( R r1, R r2 ) {
            // Might throw a ClassCastException here, but that is a
            // documented possibility for this method anyway.
            // Enforcing this with generics is too horrible.
            @SuppressWarnings("unchecked")
            Comparable<Object> c1 = (Comparable<Object>) col_.getValue( r1 );
            Object c2 = col_.getValue( r2 );
            boolean blank1 = isBlank( c1 );
            boolean blank2 = isBlank( c2 );
            if ( blank1 && blank2 ) {
                return 0;
            }
            else if ( blank1 ) {
                return +1;
            }
            else if ( blank2 ) {
                return -1;
            }
            else {
                return sense_ * c1.compareTo( c2 );
            }
        }

        /**
         * Returns true if an object counts as empty for sort purposes.
         *
         * @param  o  object
         * @return   true if this comparator's column value is to be treated
         *           as blank
         */
        private boolean isBlank( Object o ) {
            return o == null
                || ( o instanceof String && ((String) o).trim().length() == 0 );
        }
    }
}
