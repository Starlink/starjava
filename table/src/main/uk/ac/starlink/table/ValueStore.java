package uk.ac.starlink.table;

/**
 * Interface for storing a vector of values.  Depending on implementation,
 * these values may be objects or primitives.
 *
 * @author   Mark Taylor
 * @since    2 Nov 2005
 */
public interface ValueStore {

    /**
     * Returns the class of value which this object can store.
     *
     * @return  primitive class
     */
    Class getType();

    /**
     * Returns the length of the vector.
     *
     * @return  vector length
     */
    long getLength();

    /**
     * Stores a vector of values in this object.
     * <code>array</code> must be an array of values matching 
     * <code>getType</code>.  Element <code>ioff</code> of <code>array</code>
     * is stored at index <code>index</code>, elemnt <code>ioff+1</code> at
     * <code>index+1</code>, etc.
     *
     * @param  index  starting offset to write to
     * @param  array  array of values to store
     * @param  ioff   offset into array from which the first value is taken
     * @param  count  number of values to transfer
     */
    void put( long index, Object array, int ioff, int count );

    /**
     * Retrieves a vector of values from this object.
     * <code>array</code> must be an array of type matching
     * <code>getType</code>.  Every element of <code>array</code> 
     * will be filled with values; the first retrieved from offset
     * <code>index</code>, the second from <code>index+1</code>, etc.
     *
     * @param   index  starting offset
     * @param   array  array to accept data
     * @param   ioff   offset into array to which the first value is copied
     * @param   count  number of values to transfer
     */
    void get( long index, Object array, int ioff, int count );
}
