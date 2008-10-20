package uk.ac.starlink.ttools.task;

import java.io.IOException;
import uk.ac.starlink.table.StarTable;

/**
 * Interface which describes turning a string into a StarTable object.
 *
 * @author   Mark Taylor
 * @since    20 Oct 2008
 */
public interface TableLocator {

    /**
     * Returns a new or used table which is named by a given location.
     *
     * @param  location   table location
     * @return  table named by <code>location</code>
     */
    StarTable getTable( String location ) throws IOException;
}
