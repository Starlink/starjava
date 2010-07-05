package uk.ac.starlink.table;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Interface for table output handlers that can write multiple tables to
 * the same stream.  It should be possible in principle (and preferably
 * in practice using a corresponding input handler) to recover these as
 * an array of distinct tables by reading the result later.
 *
 * @author   Mark Taylor
 * @since    1 Jul 2010
 */
public interface MultiStarTableWriter extends StarTableWriter {

    /**
     * Writes an array of StarTable objects to a given output stream.
     * The implementation can assume that the stream is suitable for
     * direct writing (for instance it should not normally wrap it in a 
     * <code>BufferedOutputStream</code>), and should not close it at
     * the end of the call.
     *
     * @param  tableSeq  sequence of tables to write
     * @param  out  destination stream
     */
    void writeStarTables( TableSequence tableSeq, OutputStream out )
           throws IOException;
}
