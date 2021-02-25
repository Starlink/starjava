package uk.ac.starlink.parquet;

import org.apache.parquet.io.api.RecordConsumer;
import org.apache.parquet.schema.Type;

/**
 * Defines transmission of a java value type to a RecordConsumer
 * for writing to parquet tables.
 *
 * @author   Mark Taylor
 * @since    25 Feb 2021
 */
public interface Encoder {

    /**
     * Returns the name of the column that will be written to the parquet file.
     * This is the top-level field, which may correspond to a group type
     * containing lower-level elements, which will be assigned subordinate
     * names automatically.
     * 
     * <p>Note this name must obey the (undocumented?) parquet field
     * syntax constraints; it seems that roughly it has to look like
     * a java token.
     *
     * @return  field name
     */
    String getColumnName();

    /**
     * Returns the type of the top-level field that will be written.
     * This may be a primitive type or a group type.
     *
     * @return  column type
     */
    Type getColumnType();

    /**
     * Passes a supplied value representing a cell of this encoder's column
     * to a record consumer.  This is called between
     * RecordConsumer <code>startField</code> and <code>endField</code>
     * calls, but if additional structuring calls are required they
     * must be done by this method.
     *
     * @param   value   typed value to write
     * @param   consumer  value consumer
     */
    void addValue( Object value, RecordConsumer consumer );
}
