package uk.ac.starlink.ttools.mode;

import java.io.IOException;
import java.io.PrintStream;
import uk.ac.starlink.table.RowCollector;
import uk.ac.starlink.table.RowRunner;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.RowSplittable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.ExecutionException;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.ttools.DocUtils;
import uk.ac.starlink.ttools.TableConsumer;

/**
 * Processing mode which just counts the number of rows and columns and
 * writes a summary to the output stream.
 *
 * @author   Mark Taylor
 * @since    15 Aug 2005
 */
public class CountMode implements ProcessingMode {

    private final boolean isParallel_;

    /**
     * Constructs an instance with default parallelism.
     */
    public CountMode() {
        this( true );
    }

    /**
     * Constructs an instance with specified paralellism.
     *
     * @param  isParallel  true to count in parallel where possible
     */
    public CountMode( boolean isParallel ) {
        isParallel_ = isParallel;
    }

    public Parameter<?>[] getAssociatedParameters() {
        return new Parameter<?>[ 0 ];
    }

    public String getDescription() {
        return DocUtils.join( new String[] {
            "<p>Counts the number of rows and columns",
            "and writes the result to standard output.",
            "</p>",
        } );
    }

    public TableConsumer createConsumer( Environment env ) {
        final PrintStream out = env.getOutputStream();
        return new TableConsumer() {
            public void consume( StarTable table ) throws IOException {
                out.println( getSummary( table ) );
            }
        };
    }

    /**
     * Counts rows and columns.
     *
     * @param  table  table
     * @return  short summary of rows and columns in the table
     */
    private String getSummary( StarTable table )
            throws IOException {
        int ncol = table.getColumnCount();
        long nrow = table.getRowCount();
        if ( nrow < 0L ) {
            RowRunner runner = isParallel_ ? RowRunner.DEFAULT
                                           : RowRunner.SEQUENTIAL;
            nrow = runner.collect( new CountCollector(), table )[ 0 ];
        }
        return "columns: " + ncol + "   rows: " + nrow;
    }

    /**
     * Collector implementation for counting rows.
     */
    private static class CountCollector extends RowCollector<long[]> {
        public long[] createAccumulator() {
            return new long[ 1 ];
        }
        public long[] combine( long[] acc1, long[] acc2 ) {
            return new long[] { acc1[ 0 ] + acc2[ 0 ] };
        }
        public void accumulateRows( RowSplittable rseq, long[] acc )
                throws IOException {
            long nrow = 0;
            while ( rseq.next() ) {
                nrow++;
            }
            acc[ 0 ] += nrow;
        }
    }
}
