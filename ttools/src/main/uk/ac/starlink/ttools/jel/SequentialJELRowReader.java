package uk.ac.starlink.ttools.jel;

import java.io.IOException;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;

/**
 * Implements JELRowReader for sequential access to a table.
 * This class also implements {@link uk.ac.starlink.table.RowSequence},
 * and this object should be treated in the same way as a row sequence
 * taken out on its table, that is iteration should proceed using the
 * <tt>next</tt> and <tt>close</tt> methods.
 *
 * @author   Mark Taylor (Starlink)
 * @since    8 Feb 2005
 */
public class SequentialJELRowReader extends StarTableJELRowReader
                                    implements RowSequence {

    
    private final RowSequence rseq_;
    private long lrow_ = -1L;

    /**
     * Constructs a new row reader for sequential access.
     * This constructor also takes out a row sequence on <tt>table</tt>.
     *
     * @param  table  table to read
     */
    public SequentialJELRowReader( StarTable table ) throws IOException {
        super( table );
        rseq_ = table.getRowSequence();
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

    public long getCurrentRow() {
        return lrow_;
    }

    public void close() throws IOException {
        rseq_.close();
    }
}
