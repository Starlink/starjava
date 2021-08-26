package uk.ac.starlink.table;

import java.io.IOException;
import java.util.function.LongSupplier;

/**
 * RowSplittable based on a sequential RowSequence.
 * It cannot be split.
 *
 * @author   Mark Taylor
 * @since    4 Aug 2020
 */
public class SequentialRowSplittable implements RowSplittable {

    private final RowSequence rseq_;
    private final long nrow_;
    private long irow_;

    /**
     * Convenience constructor, constructs a RowSplittable from a table.
     *
     * @param  table  table
     */
    public SequentialRowSplittable( StarTable table ) throws IOException {
        this( table.getRowSequence(), table.getRowCount() );
    }

    /**
     * Constructs a RowSplittable from a row sequence, with an
     * indeterminate number of rows.
     *
     * @param  rseq  sequence
     */
    public SequentialRowSplittable( RowSequence rseq ) {
        this( rseq, -1L );
    }

    /**
     * Constructs a RowSplittable from a RowSequence and a given number
     * of rows.
     *
     * @param  rseq   row sequence
     * @param  nrow   row count, used as return value of
     *                <code>splittableSize</code>; may be -1 if not known
     */
    public SequentialRowSplittable( RowSequence rseq, long nrow ) {
        rseq_ = rseq;
        nrow_ = nrow;
        irow_ = -1;
    }

    public long splittableSize() {
        return nrow_;
    }

    public RowSplittable split() {
        return null;
    }

    public boolean next() throws IOException {
        irow_++;
        return rseq_.next();
    }

    public LongSupplier rowIndex() {
        return () -> irow_;
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
