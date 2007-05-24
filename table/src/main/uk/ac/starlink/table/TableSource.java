package uk.ac.starlink.table;

/**
 * General purpose interface for objects which can supply a table.
 *
 * @author   Mark Taylor
 * @since    24 May 2007
 */
public interface TableSource {

    /**
     * Returns a table.
     *
     * @return  table
     */
    StarTable getStarTable();
}
