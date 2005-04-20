package uk.ac.starlink.ttools.pipe;

import java.io.IOException;
import java.io.PrintStream;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;

/**
 * Processing mode which just counts the number of rows and columns and
 * writes a summary to the output stream.
 */
public class CountMode extends ProcessingMode {

    public String getName() {
        return "count";
    }

    /**
     * Counts rows and columns and writes output.
     */
    public void process( StarTable table ) throws IOException {
        PrintStream out = getOutputStream();
        RowSequence rseq = table.getRowSequence();
        long nrow = 0L;
        int ncol = table.getColumnCount();
        try {
            while ( rseq.next() ) {
                nrow++;
            }
            out.println( "columns: " + ncol + "   rows: " + nrow );
        }
        finally {
            rseq.close();
        }
    }
}
