package uk.ac.starlink.vo;

import java.io.IOException;

/**
 * Object which can acquire table metadata about a TAP service.
 * The <code>read*</code> methods in this interface are in general
 * time-consuming and should not be invoked on the event dispatch thread.
 *
 * <p>This class deals with reading metadata from some external source,
 * not with caching or managing the results, which should be done as
 * required by clients of this class.
 *
 * <p>The only mandatory read method here is the top-level
 * {@link #readSchemas}.  This will supply all known schemas,
 * but each schema may or may not have its tables filled in;
 * those tables may or may not have their columns and foreign keys
 * filled in.  If the schemas do not have their tables filled in
 * (<code>schemaMeta.getTables()!=null</code>), then <code>readTables</code>
 * methods can be called to read the table metadata for each schema
 * as required; that table metadata may then be cached in the schema object
 * for later use.  If the tables are filled in to start with, then
 * <code>readTables</code> may throw an UnsupportedOperationException.
 * The same applies to filled in columns/foreign keys for table objects.
 *
 * @author   Mark Taylor
 * @since    18 Mar 2015
 */
public interface TapMetaReader {

    /**
     * Acquires metadata about schemas in a TAP service.
     * <p>May be slow.
     *
     * @return   schema metadata array
     */
    SchemaMeta[] readSchemas() throws IOException;

    /**
     * Acquires metadata about tables in a given schema from a TAP service.
     * <p>May be slow.  May throw UnsupportedOperationException if not needed.
     *
     * @param  schema  schema containing tables; not altered by call
     * @return  table metadata array
     */
    TableMeta[] readTables( SchemaMeta schema ) throws IOException;

    /**
     * Acquires metadata about columns in a given table from a TAP service.
     * <p>May be slow.  May throw UnsupportedOperationException if not needed.
     *
     * @param  table  table containing columns; not altered by call
     * @return  column metadata array
     */
    ColumnMeta[] readColumns( TableMeta table ) throws IOException;

    /**
     * Acquires metadata about foreign keys in a given table from a TAP service.
     * <p>May be slow.  May throw UnsupportedOperationException if not needed.
     *
     * @param  table  table containing columns; not altered by call
     * @return   foreign key metadata array
     */
    ForeignMeta[] readForeignKeys( TableMeta table ) throws IOException;

    /**
     * Returns a textual indication of where the metadata is coming from,
     * typically a URL.
     *
     * @return   address of metadata
     */
    String getSource();

    /**
     * Returns a textual indication of the method or protocol
     * this reader uses to acquire TAP metadata.
     * 
     * @return   short description of metadata acquisition method
     */
    String getMeans();
}
