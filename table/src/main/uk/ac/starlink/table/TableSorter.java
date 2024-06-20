package uk.ac.starlink.table;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Provides table sorting functionality.
 *
 * @author   Mark Taylor (Starlink)
 * @since    8 Mar 2005
 * @see      Tables#sortTable
 */
class TableSorter {

    /**
     * Returns an array representing the sorted row sequence for a table.
     *
     * @param   table  table to sort
     * @param   colIndices  indices of the columns which are to act as sort
     *          keys; first element is primary key etc
     * @param   up  true for sorting into ascending order, false for 
     *          descending order
     * @param   nullsLast  true if blank values should be considered 
     *          last in the collation order, false if they should
     *          be considered first
     * @return  an array mapping the rows in a table to their sorted order
     * @throws  IOException  if <code>table.isRandom()</code> returns false
     */ 
    public static long[] getSortedOrder( StarTable table, int[] colIndices,
                                         boolean up, boolean nullsLast )
            throws IOException {
        if ( ! table.isRandom() ) {
            throw new IOException( "Table does not have random access" );
        }
        int nrow = Tables.checkedLongToInt( table.getRowCount() );
        Number[] rowMap = new Number[ nrow ];
        for ( int i = 0; i < nrow; i++ ) {
            rowMap[ i ] = Integer.valueOf( i );
        }
        Comparator<Number> comp =
            new RowComparator( table, colIndices, up, nullsLast );
        try {
            Arrays.sort( rowMap, comp );
        }
        catch ( SortException e ) {
            throw e.asIOException();
        }
        long[] order = new long[ nrow ];
        for ( int i = 0; i < nrow; i++ ) {
            order[ i ] = rowMap[ i ].longValue();
        }
        return order;
    }

    /**
     * Helper class which can compare objects representing the rows of
     * a table.  The objects compared using this comparator must be
     * <code>Number</code>s containing the row index of a table.
     */
    private static class RowComparator implements Comparator<Number> {

        final StarTable table_;
        final int[] colIndices_;
        final boolean up_;
        final boolean nullsLast_;
        final int ncol_;

        /**
         * Constructor.
         *
         * @param   table  table whose rows will be compared
         * @param   colIndices  indices of the columns which are to act as sort
         *          keys; first element is primary key etc
         * @param   up  true for sorting into ascending order, false for
         *          descending order
         * @param   nullsLast  true if blank values should be considered
         *          last in the collation order, false if they should
         *          be considered first
         */
        RowComparator( StarTable table, int[] colIndices, boolean up,
                       boolean nullsLast ) throws IOException {
            table_ = table;
            colIndices_ = colIndices;
            up_ = up;
            nullsLast_ = nullsLast;
            ncol_ = colIndices_.length;

            for ( int icol = 0; icol < ncol_; icol++ ) {
                ColumnInfo info = table.getColumnInfo( icol );
                if ( ! Comparable.class
                      .isAssignableFrom( info.getContentClass() ) ) {
                    throw new IOException( "Column " + info + 
                                           " has no defined sort order" );
                }
            }
        }

        /**
         * Compares two Numbers.
         */
        public int compare( Number num1, Number num2 ) {
            long irow1 = num1.longValue();
            long irow2 = num2.longValue();
            int c = 0;
            for ( int i = 0; i < ncol_ && c == 0; i++ ) {
                int icol = colIndices_[ i ];
                Object val1;
                Object val2;
                try {
                    val1 = table_.getCell( irow1, icol );
                    val2 = table_.getCell( irow2, icol );
                }
                catch ( IOException e ) {
                    throw new SortException( "Sort Error", e );
                }
                try {
                    c = compareValues( (Comparable<?>) val1,
                                       (Comparable<?>) val2 );
                }
                catch ( ClassCastException e ) {
                    throw new SortException(
                        "Expression comparison error during sorting", e );
                }
            }
            return c;
        }

        /**
         * Compares two values.  Each value is the content of a cell in 
         * a table - <code>o1</code> and <code>o2</code> should be from
         * the same column.  Either or both may be null.
         *
         * @param  o1  first value
         * @param  o2  second value
         * @return  +1, 0, or -1 according to o1 greater than, equal to, or
         *          less than o2
         */
        @SuppressWarnings({"rawtypes","unchecked"})
        private int compareValues( Comparable o1, Comparable o2 ) {
            boolean null1 = Tables.isBlank( o1 );
            boolean null2 = Tables.isBlank( o2 );
            if ( null1 && null2 ) {
                return 0;
            }
            else if ( null1 ) {
                return ( up_ ^ nullsLast_ ) ? -1 : +1;
            }
            else if ( null2 ) {
                return ( up_ ^ nullsLast_ ) ? +1 : -1;
            }
            else {
                return up_ ? o1.compareTo( o2 )
                           : o2.compareTo( o1 );
            }
        }
    }

    /**
     * Runtime exception which can be thrown from methods invoked by 
     * Arrays.sort.
     */
    private static class SortException extends RuntimeException {

        /**
         * Constructor.
         *
         * @param  msg  message
         * @param  e    cause
         */
        SortException( String msg, Throwable e ) {
            super( msg, e );
        }

        /**
         * Returns an IOException corresponding to this exception.
         *
         * @return  IOException
         */
        IOException asIOException() {
            Throwable error = getCause();
            return error instanceof IOException
                 ? (IOException) error
                 : (IOException) new IOException( getMessage() )
                                .initCause( error );
        }
    }

}
