package uk.ac.starlink.ecsv;

import java.util.Map;

/**
 * Metadata structure read from an ECSV file.
 *
 * @author   Mark Taylor
 * @since    28 Apr 2020
 */
public interface EcsvMeta {

    /**
     * Returns the delimiter character used for this file.
     * It must be either ' ' or ','.
     *
     * @return  comma or space
     */
    char getDelimiter();

    /**
     * Returns an array of column metadata items for the columns
     * in the table.
     *
     * @return   column metadata array
     */
    EcsvColumn<?>[] getColumns();

    /**
     * Returns a structure giving per-table metadata,
     * as read from the ECSV header.
     *
     * @return   table metadata structure
     */
    Map<?,?> getTableMeta();

    /**
     * Returns the schema string if present.
     *
     * @return  schema string, or null
     */
    String getSchema();
}
