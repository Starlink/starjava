package uk.ac.starlink.ttools.mode;

import java.io.IOException;
import java.io.PrintStream;
import uk.ac.starlink.table.RowSequence;
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

    public Parameter[] getAssociatedParameters() {
        return new Parameter[ 0 ];
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
    private static String getSummary( StarTable table )
            throws IOException {
        int ncol = table.getColumnCount();
        long nrow = table.getRowCount();
        if ( nrow < 0L ) {
            nrow = 0L;
            RowSequence rseq = table.getRowSequence();
            try {
                while ( rseq.next() ) {
                    nrow++;
                }
            }
            finally {
                rseq.close();
            }
        }
        return "columns: " + ncol + "   rows: " + nrow;
    }
}
