package uk.ac.starlink.ttools.task;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.EmptyRowSequence;
import uk.ac.starlink.table.RowSequence;
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
        private final Iterator prodIt = Arrays.asList( tProds_ ).iterator();
        private RowSequence rseq_ = EmptyRowSequence.getInstance();

        public boolean next() throws IOException {
            while ( ! rseq_.next() ) {
                rseq_.close();
                if ( prodIt.hasNext() ) {
                    TableProducer tProd = (TableProducer) prodIt.next();
                    StarTable table;
                    try {
                        table = tProd.getTable();
                    }
                    catch ( TaskException e ) {
                        throw (IOException) new IOException( e.getMessage() )
                                           .initCause( e );
                    }
                    checkCompatible( table );
                    rseq_ = table.getRowSequence();
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
