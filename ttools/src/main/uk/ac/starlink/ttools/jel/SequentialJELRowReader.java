package uk.ac.starlink.ttools.jel;

import java.io.IOException;
import java.util.function.LongSupplier;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.RowSplittable;
import uk.ac.starlink.table.StarTable;

/**
 * Implements JELRowReader for sequential access to a table.
 * This class also implements {@link uk.ac.starlink.table.RowSequence},
 * and this object should be treated in the same way as a row sequence
 * taken out on its table, that is iteration should proceed using the
 * <code>next</code> and <code>close</code> methods.
 *
 * @author   Mark Taylor (Starlink)
 * @since    8 Feb 2005
 */
public class SequentialJELRowReader extends StarTableJELRowReader
                                    implements RowSequence {

    
    private final RowSequence rseq_;
    private final LongSupplier rowIndex_;
    private long lrow_;

    /**
     * Constructs a new row reader for sequential access.
     * This constructor also takes out a row sequence on <code>table</code>.
     *
     * @param  table  table to read
     */
    public SequentialJELRowReader( StarTable table ) throws IOException {
        super( table );
        rseq_ = table.getRowSequence();
        rowIndex_ = () -> lrow_;
        lrow_ = -1;
    }

    /**
     * Constructs a new row reader for sequential access,
     * based on a supplied row splittable.
     *
     * <p><b>Note</b> if the {@link RowSplittable#rowIndex} method
     * of the supplied RowSplittable returns null,
     * the {@link #getCurrentRow} method of the object constructed
     * here will throw an exception.
     * Before using this reader to evaluate an expression,
     * the {@link #requiresRowIndex} method should be called to determine
     * if trouble lies ahead.
     *
     * @param  table  table to read
     * @param  rsplit   row splittable, must apply to the supplied table
     */
    public SequentialJELRowReader( StarTable table, RowSplittable rsplit ) {
        super( table );
        rseq_ = rsplit;
        rowIndex_ = rsplit.rowIndex();
        lrow_ = Long.MIN_VALUE;
    }

    public Object getCell( int icol ) throws IOException {
        return rseq_.getCell( icol );
    }

    public Object[] getRow() throws IOException {
        return rseq_.getRow();
    } 

    public boolean next() throws IOException {
        lrow_++;
        return rseq_.next();
    }

    /**
     * If the current row is not known, an RuntimeException will be thrown.
     */
    public long getCurrentRow() {
        if ( rowIndex_ != null ) {
            return rowIndex_.getAsLong();
        }
        else {
            throw new RuntimeException( "Reader does not know row index" );
        }
    }

    public void close() throws IOException {
        rseq_.close();
    }
}
