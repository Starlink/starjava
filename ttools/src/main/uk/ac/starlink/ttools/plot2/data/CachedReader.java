package uk.ac.starlink.ttools.plot2.data;

/**
 * Accessor for stored values.
 * Although several data type accessor methods are provided,
 * in general for a given instance of this class only one of these will
 * give a non-error return (the same one for all rows); it is therefore
 * the responsibility of the user of an instance of this class to
 * keep track of what type is appropriate, since Java's type system
 * will not enforce it.  Attempting to retrieve the wrong type of data
 * will give a result (it must not throw an exception), but this
 * result may not be meaningful.
 * This non-type-safe arrangement is used so that primitive objects can
 * be accessed from this interface.
 *
 * <p>Attempting to access a value with an index out of range for this
 * reader will also result in undefined behaviour.
 *
 * @author   Mark Taylor
 * @since    25 Apr 2017
 */
public interface CachedReader {

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
    Object getObjectValue( long ix );

    /**
     * Retrieve the current value of this sequence as a floating point number.
     *
     * @param   ix  value index
     * @return   value
     */
    double getDoubleValue( long ix );

    /**
     * Retrieve the current value of this sequence as an integer.
     *
     * @param   ix  value index
     * @return   value
     */
    int getIntValue( long ix );

    /**
     * Retrieve the current value of this sequence as a long.
     *
     * @param   ix  value index
     * @return   value
     */
    long getLongValue( long ix );

    /**
     * Retrieve the current value of this sequence as a boolean value.
     *
     * @param   ix  value index
     * @return   value
     */
    boolean getBooleanValue( long ix );
}
