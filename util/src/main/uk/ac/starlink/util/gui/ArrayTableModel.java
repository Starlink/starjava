package uk.ac.starlink.util.gui;

import java.util.Arrays;
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
public class ArrayTableModel extends AbstractTableModel {

    private ArrayTableColumn[] columns_;
    private Object[] items_;

    /**
     * Constructs a model with no columns or rows.
     */
    public ArrayTableModel() {
        this( new ArrayTableColumn[ 0 ], new Object[ 0 ] );
    }

    /**
     * Constructs a model with a given set of columns and rows.
     *
     * @param   columns   array of column specifiers
     * @param   items     array of row data objects
     */
    public ArrayTableModel( ArrayTableColumn[] columns, Object[] items ) {
        columns_ = columns;
        items_ = items;
    }

    /**
     * Sets the column specifiers to be used by this table.
     *
     * @param   columns  column specifiers
     */
    public void setColumns( ArrayTableColumn[] columns ) {
        columns_ = columns;
        fireTableStructureChanged();
    }

    /**
     * Returns the column specifiers used by this table.
     *
     * @return   column specifiers
     */
    public ArrayTableColumn[] getColumns() {
        return columns_;
    }

    /**
     * Sets the row data items to be used by this table.
     *
     * @param  items  row data items, one per row
     */
    public void setItems( Object[] items ) {
        items_ = items;
        fireTableDataChanged();
    }

    /**
     * Returns the row data items used by this model.
     *
     * @return  row data items, one per row
     */
    public Object[] getItems() {
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
        ArrayTableColumn col = columns_[ icol ];
        if ( Comparable.class.isAssignableFrom( col.getContentClass() ) ) {
            Comparator comparator = new ColumnComparator( col, descending );
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
    private static boolean needsSort( Object[] items, Comparator cmp ) {
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

    public Class getColumnClass( int icol ) {
        return columns_[ icol ].getContentClass();
    }

    /**
     * Can compare two row data items according to the value of a column.
     */
    private static class ColumnComparator implements Comparator {
        private final ArrayTableColumn col_;
        private final int sense_;

        /**
         * Constructor.
         *
         * @param   col   column whose contents determines sort order
         * @param   descending  false for ascending, true for descending;
         *                      nulls are always at the bottom
         */
        public ColumnComparator( ArrayTableColumn col, boolean descending ) {
            col_ = col;
            sense_ = descending ? -1 : +1;
        }

        public int compare( Object o1, Object o2 ) {
            Comparable c1 = (Comparable) col_.getValue( o1 );
            Comparable c2 = (Comparable) col_.getValue( o2 );
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
