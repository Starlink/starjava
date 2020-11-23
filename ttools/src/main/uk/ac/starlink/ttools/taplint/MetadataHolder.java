package uk.ac.starlink.ttools.taplint;

import uk.ac.starlink.vo.SchemaMeta;

/**
 * Provides table metadata.
 *
 * @author   Mark Taylor
 * @since    24 Jun 2011
 */
public interface MetadataHolder {

    /**
     * Returns the table metadata which will be used to frame example
     * ADQL queries.
     * The returned objects should be fully populated (table and column
     * lists in place, rather than needing further read operations).
     *
     * @return   tableset schema metadata array
     */
    SchemaMeta[] getTableMetadata();

    /**
     * Indicates whether the TableMeta objects in the metadata tree
     * contained by this object are expected to contain
     * column and foreign key metadata.
     *
     * @return   true if TableMetas are populated with ColumnMeta
     *           and ForeignMeta arrays where appropriate;
     *           false if those have been omitted
     */
    boolean hasDetail();
}
