package uk.ac.starlink.topcat;

import java.util.Arrays;
import java.util.List;
import java.util.Comparator;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

/**
 * Makes a TableModel out of a list of MetaColumn objects.
 */
public abstract class MetaColumnTableModel extends AbstractTableModel {

    private final List<MetaColumn> metaList_;
    private int[] rowMap_;

    /**
     * Constructs a new MetaColumnTableModel.
     *
     * @param   metaList  a list of {@link MetaColumn} objects
     */
    public MetaColumnTableModel( List<MetaColumn> metaList ) {
        metaList_ = metaList;
    }

    public int getColumnCount() {
        return metaList_.size();
    }

    abstract public int getRowCount();

    public Object getValueAt( int irow, int icol ) {
        return metaList_.get( icol ).getValue( getListIndex( irow ) );
    }

    public void setValueAt( Object value, int irow, int icol ) {
        metaList_.get( icol ).setValue( getListIndex( irow ), value );
        fireTableCellUpdated( irow, icol );
    }

    public Class getColumnClass( int icol ) {
        return metaList_.get( icol ).getContentClass();
    }

    public String getColumnName( int icol ) {
        return metaList_.get( icol ).getName();
    }

    public boolean isCellEditable( int irow, int icol ) {
        return metaList_.get( icol ).isEditable( getListIndex( irow ) );
    }

    /**
     * Returns the list of columns which provide the data for this model.
     * The list may be altered (but fire appropriate events if you do it
     * on a live instance).
     *
     * @return   column list
     */
    public List<MetaColumn> getColumnList() {
        return metaList_;
    }

    /**
     * Indicates whether the supplied column has a defined sort order.
     *
     * @param  sortCol  column, not null
     * @return  true iff sortCol can be sorted on
     */
    public boolean canSort( MetaColumn sortCol ) {
        return getComparator( sortCol, false ) != null;
    }

    /**
     * Reorders the rows of this table model based on the contents of
     * one of its columns.  This method does not inform listeners that
     * the table data may have changed, so calling code should do that
     * where appropriate.  It only needs to do so if the return value
     * of this method is true.
     *
     * @param  sortCol  column to sort on, or null for natural ordering
     * @param  isDescending  false to sort up, true to sort down
     * @return   true if changes may have been made to the rows,
     *           false if no changes were made
     */
    public boolean sortRows( MetaColumn sortCol, boolean isDescending ) {

        /* Determine the comparison order. */
        final Comparator<Comparable> vcomp =
            sortCol == null ? null : getComparator( sortCol, isDescending );

        /* Natural sort order is required. */
        if ( vcomp == null ) {
            if ( rowMap_ == null ) {
                return false;
            }
            else {
                rowMap_ = null;
                return true;
            }
        }

        /* A non-trivial sort order is defined. */
        else {
            int nrow = getRowCount();

            /* If the rows are already ordered correctly, return false
             * without any further action. */
            if ( rowMap_ != null && rowMap_.length == nrow ) {
                try {
                    if ( isSorted( rowMap_, sortCol, vcomp ) ) {
                        return false;
                    }
                }
                catch ( ClassCastException e ) {
                    if ( rowMap_ == null ) {
                        return false;
                    }
                    else {
                        rowMap_ = null;
                        return true;
                    }
                }
            }

            /* Otherwise, perform the sort. */
            rowMap_ = calculateSortMap( nrow, sortCol, vcomp );
            return true;
        }
    }

    /**
     * Returns a comparator for the given column.
     * If null is returned, it means sorting cannot be performed on
     * that column.
     *
     * @param  sortCol    column to sort on, not null
     * @param  isDescending  false ascending order, true for descending
     * @return  comparator, or null
     */
    private Comparator<Comparable> getComparator( MetaColumn sortCol,
                                                  boolean isDescending ) {
        return Comparable.class.isAssignableFrom( sortCol.getContentClass() )
             ? new NormalComparator( isDescending, false )
             : null;
    }

    /**
     * Determines whether the table data is currently sorted according to 
     * a given criterion.
     *
     * @param  rowMap  row index indirection map
     * @param  sortCol   column on whose values to sort
     * @param  vcomp    comparator for column values
     * @return   true iff the rowMap is already arranged according to
     *           the requested sort order
     */
    private static boolean isSorted( int[] rowMap, MetaColumn sortCol,
                                     Comparator<Comparable> vcomp ) {
        for ( int i = 0; i < rowMap.length - 1; i++ ) {
            Comparable c1 = (Comparable) sortCol.getValue( rowMap[ i ] );
            Comparable c2 = (Comparable) sortCol.getValue( rowMap[ i + 1 ] );
            if ( vcomp.compare( c1, c2 ) > 0 ) {
                return false;
            }
        }
        return true;
    }

    /**
     * Calculates the array mapping the natural order to sorted order of rows.
     *
     * @param   nrow   row count
     * @param   sortCol   column whose values are to be sortd by
     * @param  descending  false to sort up, true to sort down
     * @return   nrow-element array mapping row index from natural
     *           to sorted order, or null for natural order 
     */
    private static int[]
            calculateSortMap( int nrow, MetaColumn sortCol,
                              final Comparator<Comparable> vcomp ) {
        IndexedValue[] items = new IndexedValue[ nrow ];
        for ( int i = 0; i < nrow; i++ ) {
            items[ i ] =
                new IndexedValue( i, (Comparable) sortCol.getValue( i ) );
        }
        try {
            Arrays.sort( items, new Comparator<IndexedValue>() {
                public int compare( IndexedValue o1, IndexedValue o2 ) {
                    int c = vcomp.compare( o1.value_, o2.value_ );

                    /* This tie-breaker isn't really necessary,
                     * but it makes the sort deterministic. */
                    return c == 0 ? o1.index_ - o2.index_ : c;
                }
            } );
        }
        catch ( ClassCastException e ) {
            return null;
        }
        int[] rowMap = new int[ nrow ];
        for ( int i = 0; i < nrow; i++ ) {
            rowMap[ i ] = items[ i ].index_;
        }
        return rowMap;
    }

    /**
     * Returns the index in the natural row sequence for a row index in
     * the table model.
     *
     * @param   irow  table row index
     * @return   list row index
     */
    public int getListIndex( int irow ) {

        /* During normal operation, the supplied row index should not
         * exceed the length of the row map.  However, if the table
         * length has just changed, and the re-sort triggered by the
         * corresponding event has not yet happened, the supplied index
         * may be out of range, so make sure some value is returned
         * rather than throwing an exception here. */
        return rowMap_ == null || irow >= rowMap_.length
             ? irow
             : rowMap_[ irow ];
    }

    /**
     * Utility class aggregating a value object and a numeric index.
     */
    private static class IndexedValue {
        final int index_;
        final Comparable value_;

        /**
         * Constructor.
         *
         * @param  index  index
         * @param  value  value
         */
        IndexedValue( int index, Comparable value ) {
            index_ = index;
            value_ = value;
        }
    }

    /**
     * Comparator for objects which are expected to be assignment-compatible
     * with Comparable.  Nulls are toleratred.
     */
    private static class NormalComparator implements Comparator<Comparable> {
        private final int sense_;
        private final boolean nullsFirst_;

        /**
         * Constructor.
         *
         * @param  descending  false to sort up, true to sort down
         * @param  nullsFirst  false for nulls at the top of the list,
         *                     true for the top
         */
        public NormalComparator( boolean descending, boolean nullsFirst ) {
            sense_ = descending ? -1 : +1;
            nullsFirst_ = nullsFirst;
        }

        public int compare( Comparable o1, Comparable o2 ) {
            boolean null1 = o1 == null;
            boolean null2 = o2 == null;
            if ( null1 && null2 ) {
                return 0;
            }
            else if ( null1 ) {
                return nullsFirst_ ? -1 : +1;
            }
            else if ( null2 ) {
                return nullsFirst_ ? +1 : -1;
            }
            else {
                return sense_ * o1.compareTo( o2 );
            }
        }
    }
}
