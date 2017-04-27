package uk.ac.starlink.ttools.plot2.data;

/**
 * Defines storage for a vector of data.
 *
 * <p>The usage sequence for an instance of this class is:
 * <ul>
 * <li>Call {@link #add} zero or more times
 * <li>Call {@link #endAdd}
 * <li>Call {@link #createReader} zero or more times
 * </ul>
 *
 * @author   Mark Taylor
 * @since    4 Feb 2013
 */
public interface CachedColumn {

    /**
     * Adds an entry to this vector data container.
     * In general the supplied value must be of an appropriate type for
     * this object.  It must not be null.
     *
     * @param   value  non-null value to store
     */
    void add( Object value );

    /**
     * Indicates that no more calls to {@link #add} will be made
     */
    void endAdd();

    /**
     * Returns the number of values added so far.
     *
     * @return  value count
     */
    long getRowCount();

    /**
     * Returns an object which is capable of accessing the values that were
     * added to this object.
     *
     * @return   cached data sequence
     */
    CachedReader createReader(); 
}
