package uk.ac.starlink.ttools.task;

import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.TableBuilder;

/**
 * Defines how Setting string values are generated from input values
 * which refer to tables.
 *
 * @author   Mark Taylor
 * @since    20 Sep 2017
 */
public interface TableNamer {

    /**
     * Derives a string value (a name) from a given table.
     *
     * @param  table  table object
     * @return  naming structure
     */
    CredibleString nameTable( StarTable table );

    /**
     * Returns a table input handler for the given table, if known.
     *
     * @return   input handler to use with generated table name if known,
     *           otherwise null
     */
    TableBuilder getTableFormat( StarTable table );
}
