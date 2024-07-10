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
public interface Encoder<T> {

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
     * Converts an object presented for record consumption
     * to an object of this encoder's parameterised class.
     * The presented object ought to be of the right class anyway,
     * so this may just be a cast.
     * However, if the object is of an unexpected type,
     * or if its value corresponds to a blank value, null should be returned.
     *
     * @param   obj  object for encoding, expected to be of this encoder's type
     * @return   typed object for record consumption,
     *           or null if it does not correspond to a definite typed value
     */
    T typedValue( Object obj );

    /**
     * Passes a supplied non-null value representing a cell
     * of this encoder's column to a record consumer.
     * This is invoked between RecordConsumer <code>startField</code>
     * and <code>endField</code> calls, but if additional structuring calls
     * are required they must be done by this method.
     *
     * @param   value   non-null typed value to write
     * @param   consumer  value consumer
     */
    void addValue( T value, RecordConsumer consumer );
}
