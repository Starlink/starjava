package uk.ac.starlink.table;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Interface for table output handlers that can write multiple tables to
 * the same stream.  It should be possible in principle (and ideally
 * in practice using a corresponding input handler) to recover these as
 * an array of distinct tables by reading the result later.
 * It is not in general expected that the tables have similar characteristics.
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

    /**
     * Writes an array of StarTable objects to a given location.
     * Implementations are free to interpret the <code>location</code> argument
     * in any way appropriate for them.  Typically however the location
     * will simply be used to get an output stream (for instance interpreting
     * it as a filename).  In this case the <code>sto</code> argument should
     * normally be used to turn <code>location</code> into a stream.
     *
     * @param  tableSeq  sequence of tables to write
     * @param  location  destination for tables
     * @param  sto   StarTableOutput instance
     */
    void writeStarTables( TableSequence tableSeq, String location,
                          StarTableOutput sto )
           throws IOException;
}
