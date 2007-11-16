package uk.ac.starlink.ttools.task;

import java.io.IOException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.task.TaskException;

/**
 * Interface that defines mapping one table to another.
 *
 * @author   Mark Taylor
 * @since    16 Nov 2007
 */
public interface SingleTableMapping {

    /**
     * Converts an input table to an output table.
     *
     * @param  table  input table
     * @return  output table
     */
    StarTable map( StarTable table ) throws IOException, TaskException;
}
