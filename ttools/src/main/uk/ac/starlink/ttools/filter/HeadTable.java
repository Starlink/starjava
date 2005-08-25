package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.WrapperRowSequence;
import uk.ac.starlink.table.WrapperStarTable;

/**
 * Wrapper table implementation which only contains the first N rows of
 * its base table.
 *
 * @author   Mark Taylor (Starlink)
 * @since    7 Mar 2005
 */
public class HeadTable extends WrapperStarTable {

    private final long nhead_;

    /**
     * Constructor.
     *
     * @param  base  base table
     * @param  nhead   number of rows at the head of the table to use
     */
    public HeadTable( StarTable base, long nhead ) {
        super( base );
        nhead_ = nhead;
    }

    public long getRowCount() {
        long nbase = super.getRowCount();
        return nbase >= 0L ? Math.min( nbase, nhead_ )
                           : -1L;
    }

    public Object getCell( long irow, int icol ) throws IOException {
        if ( irow < nhead_ ) {
            return super.getCell( irow, icol );
        }
        else {
            throw new IndexOutOfBoundsException();
        }
    }

    public Object[] getRow( long irow ) throws IOException {
        if ( irow < nhead_ ) {
            return super.getRow( irow );
        }
        else {
            throw new IndexOutOfBoundsException();
        }
    }

    public RowSequence getRowSequence() throws IOException {
        return new WrapperRowSequence( super.getRowSequence() ) {
            long nleft = nhead_;
            public boolean next() throws IOException {
                return nleft-- > 0 && super.next();
            }
        };
    }
}
