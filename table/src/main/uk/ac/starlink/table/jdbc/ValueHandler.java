package uk.ac.starlink.table.jdbc;

import uk.ac.starlink.table.ColumnInfo;

/**
 * Defines how the data from a particular column of a JDBC table are turned
 * into java objects.
 *
 * @author   Mark Taylor
 * @since    2 Feb 2010
 * @see   TypeMapper
 */
public interface ValueHandler {

    /**
     * Returns the column metadata for this column.
     *
     * @return   column metadata
     */
    ColumnInfo getColumnInfo();

    /**
     * Translates a value from the form that it takes in the ResultSet to
     * a form which can be used for further processing.
     *
     * @param   baseValue  ResultSet value
     * @return  value for processing
     */
    Object getValue( Object baseValue );
}
