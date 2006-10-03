package uk.ac.starlink.ttools.task;

import java.io.IOException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.task.TaskException;

/**
 * Defines an operation which maps zero or more tables on input to a table
 * on output.
 *
 * @author   Mark Taylor
 * @since    15 Aug 2005
 */
public interface TableMapping {

    /**
     * Perform table mapping.
     *
     * @param  in  table sources
     * @param  out  table sinks
     */
    StarTable mapTables( StarTable[] in ) throws IOException, TaskException;
}
