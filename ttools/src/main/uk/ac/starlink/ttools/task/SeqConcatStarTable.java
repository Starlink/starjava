package uk.ac.starlink.ttools.task;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.function.LongSupplier;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.EmptyRowSequence;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.RowSplittable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.WrapperStarTable;
import uk.ac.starlink.task.TaskException;

/**
 * StarTable implementation which concatenates a list of compatible 
 * constituent tables in sequence.
 * Unlike {@link uk.ac.starlink.table.ConcatStarTable} it does not 
 * attempt to provide random access or to cache tables that it's obtained
 * before.  This is particularly suitable for catting an enormous
 * number of tables together.
 *
 * @author   Mark Taylor
 * @since    4 Oct 2006
 */
public class SeqConcatStarTable extends WrapperStarTable {

    private final ColumnInfo[] colInfos_;
    private final TableProducer[] tProds_;

    /**
     * Constructor.
     *
     * @param   meta   table providing metadata
     * @param   tProds  array of table producers which provide the tables
     *          to join
     */
    public SeqConcatStarTable( StarTable meta, TableProducer[] tProds ) {
        super( meta );
        colInfos_ = Tables.getColumnInfos( meta );
        tProds_ = tProds;
    }

    public boolean isRandom() {
        return false;
    }

    public Object getCell( long irow, int icol ) {
        throw new UnsupportedOperationException();
    }

    public Object[] getRow( long irow ) {
        throw new UnsupportedOperationException();
    }

    public long getRowCount() {
        return -1L;
    }

    public RowSequence getRowSequence() throws IOException {
        return new ConcatRowSequence();
    }

    public RowSplittable getRowSplittable() throws IOException {
        return new SeqConcatRowSplittable();
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
                                       info0 );
            }
        }
    }

    /**
     * RowSequence implementation for this table.
     */
    private class ConcatRowSequence implements RowSequence {
        private final Iterator<TableProducer> prodIt =
            Arrays.asList( tProds_ ).iterator();
        private RowSequence rseq_ = EmptyRowSequence.getInstance();
        private StarTable table_;

        public boolean next() throws IOException {
            while ( ! rseq_.next() ) {
                rseq_.close();
                if ( table_ != null ) {
                    table_.close();
                    table_ = null;
                }
                if ( prodIt.hasNext() ) {
                    TableProducer tProd = prodIt.next();
                    StarTable table;
                    try {
                        table = tProd.getTable();
                    }
                    catch ( TaskException e ) {
                        throw new IOException( e.getMessage(), e );
                    }
                    checkCompatible( table );
                    rseq_ = table.getRowSequence();
                    table_ = table;
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

    /**
     * RowSplittable implementation for use with SeqConcatStarTable.
     * It just splits between consituent tables, not within them.
     */
    private class SeqConcatRowSplittable implements RowSplittable {

        private int itab_;
        private int ntab_;
        private RowSequence rseq_;
        private StarTable table_;

        /**
         * Public constructor.
         */
        public SeqConcatRowSplittable() {
            this( -1, tProds_.length );
        }

        /**
         * Recursive constructor.
         *
         * @param  itab    index before first table to be processed
         * @param  ntab    index after last table to be processed
         */
        private SeqConcatRowSplittable( int itab, int ntab ) {
            itab_ = itab;
            ntab_ = ntab;
        }

        public RowSplittable split() {
            if ( rseq_ == null && ntab_ - itab_ > 2 ) {
                int mid = ( 1 + itab_ + ntab_ ) / 2;
                RowSplittable split = new SeqConcatRowSplittable( itab_, mid );
                itab_ = mid - 1;
                return split;
            }
            else {
                return null;
            }
        }

        public long splittableSize() {
            return -1;
        }

        public LongSupplier rowIndex() {
            return null;
        }

        public boolean next() throws IOException {
            while ( true ) {
                while ( rseq_ == null ) {
                    if ( itab_ + 1 == ntab_ ) {
                        return false;
                    }
                    itab_++;
                    StarTable table;
                    try {
                        table = tProds_[ itab_ ].getTable();
                    }
                    catch ( TaskException e ) {
                        throw new IOException( e.getMessage(), e );
                    }
                    checkCompatible( table );
                    rseq_ = table.getRowSequence();
                    table_ = table;
                }
                if ( rseq_.next() ) {
                    return true;
                }
                else {
                    rseq_.close();
                    rseq_ = null;
                    if ( table_ != null ) {
                        table_.close();
                        table_ = null;
                    }
                }
            }
        }

        public Object getCell( int icol ) throws IOException {
            return rseq_.getCell( icol );
        }

        public Object[] getRow() throws IOException {
            return rseq_.getRow();
        }

        public void close() throws IOException {
            if ( rseq_ != null ) {
                rseq_ = null;
                itab_ = ntab_;
            }
        }
    }
}
