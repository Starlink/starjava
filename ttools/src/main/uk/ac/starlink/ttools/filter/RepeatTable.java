package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import uk.ac.starlink.table.EmptyRowSequence;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.WrapperStarTable;

/**
 * Wrapper table which repeats the rows of the base table multiple times.
 *
 * @author   Mark Taylor
 * @since    5 Aug 2010
 */
public class RepeatTable extends WrapperStarTable {

    private final long count_;

    /**
     * Constructor.
     *
     * @param  base  base table
     * @param  count  number of repeats
     */
    public RepeatTable( StarTable base, long count ) {
        super( base );
        count_ = count;
    }

    public long getRowCount() {
        long baseNrow = super.getRowCount();
        return baseNrow >= 0 ? count_ * baseNrow
                             : baseNrow;
    }

    public Object getCell( long irow, int icol ) throws IOException {
        return super.getCell( getBaseRow( irow ), icol );
    }

    public Object[] getRow( long irow ) throws IOException {
        return super.getRow( getBaseRow( irow ) );
    }

    public RowSequence getRowSequence() throws IOException {
        return new RowSequence() {
            private long remaining_ = count_;
            private RowSequence rseq_ = EmptyRowSequence.getInstance();

            public boolean next() throws IOException {
                if ( rseq_.next() ) {
                    return true;
                }
                else if ( remaining_ > 0 ) {
                    remaining_--;
                    rseq_.close();
                    rseq_ = getBaseTable().getRowSequence();
                    return rseq_.next();
                }
                else {
                    return false;
                }
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
        };
    }

    /**
     * Returns the row in the base table corresponding to a row in the
     * current table.  Only works for random-access tables.
     *
     * @param  irow  current table row index
     * @return   base table row index
     */
    private long getBaseRow( long irow ) {
        long baseNrow = super.getRowCount();
        if ( baseNrow < 0 ) {
            throw new UnsupportedOperationException( "Not random access" );
        }
        else if ( baseNrow == 0 ) {
            throw new IllegalArgumentException( "Table has no rows" );
        }
        else {
            if ( irow / baseNrow < count_ ) {
                return irow % baseNrow;
            }
            else {
                throw new IllegalArgumentException( "No such row " + irow );
            }
        }
    }
}
