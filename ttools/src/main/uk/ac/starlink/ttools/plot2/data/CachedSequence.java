package uk.ac.starlink.ttools.plot2.data;

/**
 * Iterator over stored values.
 * The pointer is advanced to the next value in the sequence by calling
 * {@link #next}.
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
 * @author   Mark Taylor
 * @since    4 Feb 2013
 */
public interface CachedSequence {

    /**
     * Advance to the next item in the sequence (the next row).
     * Must be called before every access, including the first one.
     *
     * @return   true iff another value is available from this sequence
     */
    boolean next();

    /**
     * Retrieve the current value of this sequence as an object.
     * Note it is permitted for the same instance to be returned each time,
     * if the object is mutable it may have different values each time
     * (for instance an array with different contents for each row).
     * Therefore the state or contents of the returned object must
     * not be relied on to stay the same between calls to this method.
     *
     * @return  value
     */
    Object getObjectValue();

    /**
     * Retrieve the current value of this sequence as a floating point number.
     *
     * @return   value
     */
    double getDoubleValue();

    /**
     * Retrieve the current value of this sequence as an integer.
     *
     * @return   value
     */
    int getIntValue();

    /**
     * Retrieve the current value of this sequence as a boolean value.
     *
     * @return   value
     */
    boolean getBooleanValue();
}
