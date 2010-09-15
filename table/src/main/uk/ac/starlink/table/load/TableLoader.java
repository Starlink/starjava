package uk.ac.starlink.table.load;

import java.io.IOException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;

/**
 * Interface defining an object which can load tables.
 *
 * @author   Mark Taylor
 * @since    13 Sept 2010
 */
public interface TableLoader {

    /**
     * Returns a short textual label describing what is being loaded.
     * This may be presented to a waiting user.
     *
     * @return   load label
     */
    String getLabel();

    /**
     * Loads one or more tables.
     * This method may be time-consuming, and should not be called on
     * the event dispatch thread.
     *
     * @param  tfact   table factory
     * @return   loaded tables
     */
    StarTable[] loadTables( StarTableFactory tfact ) throws IOException;
}
