package uk.ac.starlink.parquet;

import org.apache.parquet.column.ColumnReader;

/**
 * Can read values from a particular column in a parquet file.
 * Instances of this class should not be used from multiple
 * threads concurrently.
 *
 * @author   Mark Taylor
 * @since    24 Feb 2021
 */
public interface Decoder<T> {

    /**
     * Class of object that is read from column data.
     *
     * @return   content class
     */
    Class<T> getContentClass();

    /**
     * Reads a primitive value from the supplied column reader into
     * this decoder's current value.
     *
     * @param  crdr  column reader with a primitive value ready to read
     */
    void readItem( ColumnReader crdr );

    /**
     * Reads a null value into this decoder's current value.
     */
    void readNull();

    /**
     * Resets this reader to the state in which no reads have been called.
     */
    void clearValue();

    /**
     * Returns the value that has been read into this reader.
     * It will be the result of all the {@link #readItem readItem} and
     * {@link #readNull} calls since the last call to {@link #clearValue}
     * (in the case of a scalar value, there will only have been one
     * such call).
     *
     * @return   value of the most recently read column cell
     */
    T getValue();
}
