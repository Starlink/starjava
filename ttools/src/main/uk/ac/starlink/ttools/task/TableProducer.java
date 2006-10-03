package uk.ac.starlink.ttools.task;

import java.io.IOException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.task.TaskException;

/**
 * Provides a table.
 *
 * @author   Mark Taylor
 * @since    29 Sep 2006
 */
public interface TableProducer {

    /**
     * Provides a table.
     *
     * @return   table
     */
    public StarTable getTable() throws IOException, TaskException;
}
