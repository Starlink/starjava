package uk.ac.starlink.table.jdbc;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;

/**
 * Determines how JDBC types are mapped to java types.
 *
 * @author   Mark Taylor
 * @since    2 Feb 2010
 * @see      uk.ac.starlink.table.jdbc.TypeMappers
 */
public interface TypeMapper {

    /**
     * Constructs a ValueHandler suitable for converting the data from
     * a column in a given ResultSet.
     *
     * @param  meta  result set metadata
     * @param  jcol1   JDBC column index (first column is 1)
     */
    ValueHandler createValueHandler( ResultSetMetaData meta, int jcol1 )
            throws SQLException;

    /**
     * Returns an ordered list of {@link uk.ac.starlink.table.ValueInfo}
     * objects representing the auxilliary metadata returned by
     * the ColumnInfo objects used by this mapper's ValueHandlers.
     * An empty array may be returned if not known.
     *
     * @see   uk.ac.starlink.table.StarTable#getColumnAuxDataInfos
     * @return  an unmodifiable ordered set of known metadata keys
     */
    List getColumnAuxDataInfos();
}
