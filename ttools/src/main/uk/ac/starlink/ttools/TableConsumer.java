package uk.ac.starlink.ttools;

import java.io.IOException;
import uk.ac.starlink.table.StarTable;

/**
 * Disposes of a table.
 *
 * @author   Mark Taylor
 * @since    9 Aug 2005
 */
public interface TableConsumer {

    /**
     * Consumes a table.
     *
     * @param   table  table to consume
     */
    public void consume( StarTable table ) throws IOException;
}
