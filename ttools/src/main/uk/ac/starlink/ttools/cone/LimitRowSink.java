package uk.ac.starlink.ttools.cone;

import java.io.IOException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.TableSink;

/**
 * TableSink wrapper implementation that truncates the row stream
 * at a given maximum.
 *
 * @author   Mark Taylor
 * @since    7 Nov 2014
 */
public class LimitRowSink implements TableSink {

    private final TableSink base_;
    private long nleft_;
    private boolean truncated_;

    /**
     * Constructor.
     *
     * @param  base  base sink
     * @param  maxrow  maximum number of rows this sink will transmit
     *                 to its base
     */
    public LimitRowSink( TableSink base, long maxrow ) {
        base_ = base;
        if ( maxrow < 0 ) {
            throw new IllegalArgumentException();
        }
        nleft_ = maxrow;
    }

    public void acceptMetadata( StarTable table ) throws TableFormatException {
        base_.acceptMetadata( table );
    }

    public void acceptRow( Object[] row ) throws IOException {
        if ( --nleft_ >= 0 ) {
            base_.acceptRow( row );
        }
        else {
            truncated_ = true;
        }
    }

    public void endRows() throws IOException {
        base_.endRows();
    }

    /**
     * Indicates whether this sink has received any rows that it has
     * refused to pass on to its base sink.
     *
     * @return  true iff truncation has actually been applied so far
     */
    public boolean isTruncated() {
        return truncated_;
    }
}
