package uk.ac.starlink.ttools.task;

import java.io.IOException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.ttools.mode.TableConsumer;

/**
 * Defines an operation which maps zero or more tables on input to zero or
 * more tables on output.
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
    void mapTables( StarTable[] in, TableConsumer[] out ) throws IOException;
}
