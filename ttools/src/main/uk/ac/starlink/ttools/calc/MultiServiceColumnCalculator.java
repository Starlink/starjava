package uk.ac.starlink.ttools.calc;

import java.io.IOException;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.TableSink;

/**
 * ColumnCalculator implementation which works by invoking a service for
 * each row.  In this case the calculation can be done row by row, but if
 * the service is slow use of the ColumnCalculator interface can allow
 * multiplexing for performance.
 *
 * @author   Mark Taylor
 * @since    14 Oct 2011
 */
public abstract class MultiServiceColumnCalculator<S>
        implements ColumnCalculator<S> {

    /**
     * Defines the service operation to be invoked for each row.
     *
     * @param  spec  specification object for the calculation
     */
    public abstract ServiceOperation createServiceOperation( S spec );

    public void calculateColumns( S spec, StarTable tupleTable, TableSink sink )
            throws IOException {

        /* The current, trivial, implementation is single-threaded.
         * A better implementation would allow some (configurable)
         * parallelism to cover service latency. */
        final ServiceOperation sop = createServiceOperation( spec );
        sink.acceptMetadata( sop.getResultMetadata() );
        RowSequence rseq = tupleTable.getRowSequence();
        try {
            while ( rseq.next() ) {
                Object[] inRow = rseq.getRow();
                Object[] outRow = sop.calculateRow( inRow );
                sink.acceptRow( outRow );
            }
        }
        finally {
            rseq.close();
            sink.endRows();
        }
    }
}
