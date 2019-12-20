package uk.ac.starlink.table.storage;

import java.io.IOException;

/**
 * Accessor for stored values.
 * Although several data type accessor methods are provided,
 * in general for a given instance of this interface not all of them
 * will give a reasonable result; it is therefore
 * the responsibility of the user of an instance of this class to
 * keep track of what type is appropriate, since Java's type system
 * will not enforce it.  Attempting to retrieve the wrong type of data
 * will give a result (it must not throw an exception), but this
 * result may not be meaningful.
 * This point of this non-type-safe arrangement is to make it possible
 * to access primitive objects from this interface without the overhead
 * of object creation.
 *
 * <p>Attempting to access a value with an index out of range for this
 * reader will result in undefined behaviour.
 *
 * <p>Instances of this interface are not in general safe for concurrent
 * access from different threads.
 *
 * @author   Mark Taylor
 * @since    25 Apr 2017
 */
public interface ColumnReader {

    /**
     * Returns the number of entries in this column.
     *
     * @return  row count
     */
    long getRowCount();

    /**
     * Retrieve the value at a given index as an object.
     * Note it is permitted for the same instance to be returned each time,
     * if the object is mutable it may have different values each time
     * (for instance an array with different contents for each row).
     * Therefore the state or contents of the returned object must
     * not be relied on to stay the same between calls to this method.
     *
     * @param   ix  value index
     * @return  value
     */
    Object getObjectValue( long ix ) throws IOException;

    /**
     * Retrieve the current value of this sequence as a floating point number.
     *
     * @param   ix  value index
     * @return   best-efforts floating point value
     */
    double getDoubleValue( long ix ) throws IOException;

    /**
     * Retrieve the current value of this sequence as an integer.
     *
     * @param   ix  value index
     * @return   best-efforts integer value
     */
    int getIntValue( long ix ) throws IOException;

    /**
     * Retrieve the current value of this sequence as a long.
     *
     * @param   ix  value index
     * @return   best-efforts long integer value
     */
    long getLongValue( long ix ) throws IOException;

    /**
     * Retrieve the current value of this sequence as a boolean value.
     *
     * @param   ix  value index
     * @return   best-efforts boolean value
     */
    boolean getBooleanValue( long ix ) throws IOException;
}
