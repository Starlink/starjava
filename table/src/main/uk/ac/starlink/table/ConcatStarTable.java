package uk.ac.starlink.table;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

/**
 * StarTable implementation which concatenates several tables together
 * top-to-bottom.  A (possibly blank) metadata table is supplied to 
 * define the column and table metadata for the result, and other data
 * tables are added on one way or another, depending which constructor
 * is used.  The columns of each data table must be compatible with the
 * columns of the metadata table, or the the data will not be included.
 *
 * @author   Mark Taylor
 * @since    29 Mar 2004
 */
public class ConcatStarTable extends WrapperStarTable {

    private final ColumnInfo[] colInfos_;
    private final List tableList_;
    private Iterator tableIt_;
    private Boolean isRandom_;
    private long nrow_ = -1L;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.table" );

    /**
     * Constructs a concatenated table from a metadata table and an iterator
     * over data-bearing tables.
     * If any of the tables supplied by the iterator are incompatible with
     * the metadata in <code>meta</code> a warning message is 
     * issued through the logging system and its rows are ignored.
     *
     * @param   meta   table supplying column and table metadata for this one;
     *                 its row count is ignored and its data is never read
     * @param  tableIt iterator over constituent {@link StarTable}s which
     *                 taken in sequence supply the row data for this one
     */
    public ConcatStarTable( StarTable meta, Iterator tableIt ) {
        super( meta );
        colInfos_ = Tables.getColumnInfos( meta );
        tableIt_ = tableIt;
        tableList_ = new ArrayList();
    }

    /**
     * Constructs a concatenated table from a metadata table and an array
     * of data-bearing tables.
     * If any of <code>tables</code> are incompatible with the metadata
     * in <code>meta</code> an IOException is thrown.
     *
     * @param   meta   table supplying column and table metadata for this one;
     *                 its row count is ignored and its data is never read
     * @param   tables array of tables which taken in sequence supply the
     *                 row data for this one
     * @throws  IOException  if any of <code>tables</code> are not compatible
     *          with <code>meta</code>
     */
    public ConcatStarTable( StarTable meta, StarTable[] tables ) 
            throws IOException {
        super( meta );
        colInfos_ = Tables.getColumnInfos( meta );
        for ( int itab = 0; itab < tables.length; itab++ ) {
            checkCompatible( tables[ itab ] );
        }
        tableIt_ = null;
        tableList_ = Arrays.asList( tables );
        gotTables();
    }

    public long getRowCount() {
        return nrow_;
    }

    public Object getCell( long irow, int icol ) throws IOException {
        if ( isRandom() ) {
            for ( Iterator it = tableList_.iterator(); it.hasNext(); ) {
                StarTable table = (StarTable) it.next();
                long nr = table.getRowCount();
                assert nr >= 0;
                if ( irow < nr ) {
                    return table.getCell( irow, icol );
                }
                irow -= nr;
            }
            throw new ArrayIndexOutOfBoundsException();
        }
        else {
            throw new UnsupportedOperationException( "No random access" );
        }
    }

    public Object[] getRow( long irow ) throws IOException {
        if ( isRandom() ) {
            for ( Iterator it = tableList_.iterator(); it.hasNext(); ) {
                StarTable table = (StarTable) it.next();
                long nr = table.getRowCount();
                assert nr >= 0;
                if ( irow < nr ) {
                    return table.getRow( irow );
                }
                irow -= nr;
            }
            throw new ArrayIndexOutOfBoundsException();
        }
        else {
            throw new UnsupportedOperationException( "No random access" );
        }
    }

    public boolean isRandom() {
        return isRandom_ != null && isRandom_.booleanValue();
    }

    public RowSequence getRowSequence() throws IOException {
        return new ConcatRowSequence( getTableIterator() );
    }

    /**
     * Checks whether a given table is compatible with the metadata of
     * this one.  The main thing to check is that the columns have 
     * compatible types.
     *
     * @param   table  table to check against this one
     * @throws  IOException  in case of incompatibility
     */
    private void checkCompatible( StarTable table ) throws IOException {
        if ( table.getRowCount() == 0 ) {
            return;
        }
        int ncol0 = colInfos_.length;
        int ncol1 = table.getColumnCount();
        if ( ncol1 != ncol0 ) {
            throw new IOException( "Column count mismatch ("
                                 + ncol1 + " != " + ncol0 + ")" );
        }
        for ( int icol = 0; icol < ncol0; icol++ ) {
            ColumnInfo info0 = colInfos_[ icol ];
            ColumnInfo info1 = table.getColumnInfo( icol );
            if ( ! info0.getContentClass()
                        .isAssignableFrom( info1.getContentClass() ) ||
                 info0.isArray() != info1.isArray() ) {
                throw new IOException( "Column type mismatch (" +
                                       info1 + " not compatible with " +
                                       info0 + ")" );
            }
        }
    }

    /**
     * Called once, when all the constituent tables have been gathered 
     * and are stored in <code>tableList_</code>.
     * Updates certain bits of state which can't be known until then.
     */
    private synchronized void gotTables() {
        assert isRandom_ == null;
        boolean isRand = true;
        long nrow = 0L;
        for ( Iterator it = tableList_.iterator(); it.hasNext(); ) {
            StarTable table = (StarTable) it.next();
            isRand = isRand && table.isRandom();
            if ( nrow >= 0 ) {
                long nr = table.getRowCount();
                nrow = nr >= 0 ? nrow + nr
                               : -1L;
            }
        }
        isRandom_ = Boolean.valueOf( isRand );
        nrow_ = nrow;
    }

    /**
     * Returns an iterator over all the constituent tables of this
     * concatenation.
     *
     * @return  iterator over tables
     */
    private synchronized Iterator getTableIterator() {

        /* If the list of constituent tables is complete, just return an
         * iterator over it. */
        if ( tableIt_ == null ) {
            return tableList_.iterator();
        }

        /* If not, life is more interesting.  The initial iterator over tables
         * is still live.  The returned iterator will use existing elements
         * of the list when they're there, but when it reaches the end of
         * that list it will have to update the list and then return the
         * new element.  It is essential that two of these iterators don't
         * get confused about who is picking the next table from the initial
         * iterator, so careful use of synchronisation is made. */
        else {
            return new Iterator() {
                private int index_;
                public boolean hasNext() {
                    synchronized ( ConcatStarTable.this ) {
                        if ( index_ < tableList_.size() ) {
                            return true;
                        }
                        else if ( tableIt_ == null ) {
                            return false;
                        }
                        else if ( tableIt_.hasNext() ) {
                            StarTable table = (StarTable) tableIt_.next();
                            try {
                                checkCompatible( table );
                            }
                            catch ( IOException e ) {
                                logger_.warning( "Omitting incompatible table"
                                               + " #" + tableList_.size()
                                               + " - " + e.getMessage() );
                                table = new EmptyStarTable( table );
                            }
                            tableList_.add( table );
                            return true;
                        }
                        else {
                            tableIt_ = null;
                            gotTables();
                            return false;
                        }
                    }
                }
                public Object next() {
                    if ( hasNext() ) {
                        return tableList_.get( index_++ );
                    }
                    else {
                        throw new IllegalStateException();
                    }
                }
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }

    /**
     * Assembles column metadata objects which are compatible
     * between multiple tables.
     * Nullability, array shape and element size
     * are set to values which can accommodate all of the input tables.
     * If column count or column data types are inconsistent, an
     * IOException is thrown.
     *
     * <p>This utility method is not used by ConcatStarTable instances,
     * but it may be useful when preparing metadata tables for use
     * with the constructor.
     * 
     * @param  colInfos  input column metadata objects
     * @param  tables   list of tables with which columns must be compatible
     * @return   new array of new column metadata objects, based on input list
     * @throws  IOException  if compatibility cannot be achieved
     */
    public static ColumnInfo[] extendColumnTypes( ColumnInfo[] colInfos,
                                                  StarTable[] tables )
            throws IOException {
        int ncol = colInfos.length;
        ColumnInfo[] outInfos = new ColumnInfo[ ncol ];
        for ( int it = 0; it < tables.length; it++ ) {
            int ncol1 = tables[ it ].getColumnCount();
            if ( ncol1 != ncol ) {
                throw new IOException( "Column count mismatch ("
                                      + ncol1 + " != " + ncol + ")" );
            }
        }
        for ( int icol = 0; icol < ncol; icol++ ) {
            ColumnInfo info = new ColumnInfo( colInfos[ icol ] );
            for ( int itab = 0; itab < tables.length; itab++ ) {
                ColumnInfo info1 = tables[ itab ].getColumnInfo( icol );
                if ( ! info.getContentClass()
                           .isAssignableFrom( info1.getContentClass() ) ||
                     info.isArray() != info1.isArray() ) {
                    throw new IOException( "Column type mismatch (" +
                                           info1 + " not compatible with " +
                                           info + ")" );
                }
                if ( info1.isNullable() ) {
                    info.setNullable( true );
                }
                if ( info1.getElementSize() != info.getElementSize() ) {
                    info.setElementSize( -1 );
                }
                if ( info.isArray() ) {
                    int[] shape = info.getShape();
                    int[] shape1 = info1.getShape();
                    if ( shape == null || shape1 == null ||
                         shape.length == 0 || shape1.length == 0 ||
                         shape[ 0 ] < 1 || shape1[ 0 ] < 1 ||
                         shape.length != shape1.length ) {
                        info.setShape( new int[] { -1 } );
                    }
                    else if ( ! Arrays.equals( shape, shape1 ) ) {
                        shape[ shape.length - 1 ] = -1;
                        info1.setShape( shape );
                    }
                }
            }
            outInfos[ icol ] = info;
        }
        return outInfos;
    }

    /**
     * RowSequence implementation which uses an iterator over tables.
     */
    private class ConcatRowSequence implements RowSequence {

        private final Iterator tIt_;
        private RowSequence rseq_;

        /**
         * Constructs a row sequence which iterates over all the rows in
         * each table in an iterator in sequence.
         *
         * @param  tableIt  iterator over {@link StarTable} objects
         */
        ConcatRowSequence( Iterator tableIt ) {
            tIt_ = tableIt;
            rseq_ = EmptyRowSequence.getInstance();
        }

        public boolean next() throws IOException {
            while ( ! rseq_.next() ) {
                rseq_.close();
                if ( tIt_.hasNext() ) {
                    rseq_ = ((StarTable) tIt_.next()).getRowSequence();
                }
                else {
                    return false;
                }
            }
            return true;
        }

        public Object getCell( int icol ) throws IOException {
            return rseq_.getCell( icol );
        }

        public Object[] getRow() throws IOException {
            return rseq_.getRow();
        }

        public void close() throws IOException {
            rseq_.close();
        }
    }
}
