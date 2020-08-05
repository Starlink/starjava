package uk.ac.starlink.table;

import java.io.IOException;
import java.util.function.LongSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * RowSplittable based on a RowAccess.
 *
 * @author   Mark Taylor
 * @since    4 Aug 2020
 */
public class RandomRowSplittable implements RowSplittable {

    private final StarTable table_;
    private final RowAccess access_;
    private long irow_;
    private long nrow_;
    private static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.table.RandomRowSplittable" );

    /**
     * Constructs a splittable for a given table.
     *
     * @param  table  random-access table
     * @throws  UnsupportedOperationException  if table is not random-access
     */
    public RandomRowSplittable( StarTable table ) throws IOException {
        this( table, -1L, table.getRowCount() );
    }

    /**
     * Recursive constructor for internal use.
     *
     * @param  table  table
     * @param  irow   row index immediately before start of iteration range
     * @param  nrow   row index immediately after end of iteration range
     */
    private RandomRowSplittable( StarTable table, long irow, long nrow )
            throws IOException {
        table_ = table;
        if ( ! table.isRandom() ) {
            throw new UnsupportedOperationException( "Table not random access");
        }
        access_ = table.getRowAccess();
        irow_ = irow;
        nrow_ = nrow;
    }

    public long splittableSize() {
        return nrow_ - irow_;
    }

    public RandomRowSplittable split() {
        if ( nrow_ - irow_ > 2 ) {
            long mid = ( irow_ + nrow_ ) / 2;
            final RandomRowSplittable split;
            try {
                split = new RandomRowSplittable( table_, irow_, mid );
            }
            catch ( IOException e ) {
                logger_.log( Level.WARNING,
                             "Split failed with IOException: " + e, e );
                return null;
            }
            irow_ = mid - 1;
            return split;
        }
        else {
            return null;
        }
    }

    public LongSupplier rowIndex() {
        return () -> irow_;
    }

    public boolean next() throws IOException {
        if ( irow_ < nrow_ - 1 ) {
            irow_++;
            access_.setRowIndex( irow_ );
            return true;
        }
        else {
            return false;
        }
    }

    public Object getCell( int icol ) throws IOException {
        return access_.getCell( icol );
    }

    public Object[] getRow() throws IOException {
        return access_.getRow();
    }

    public void close() throws IOException {
        access_.close();
    }
}
