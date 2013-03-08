package uk.ac.starlink.ttools.plot2.data;

/**
 * Defines storage for a vector of data.
 *
 * <p>The usage sequence for an instance of this class is:
 * <ul>
 * <li>Call {@link #add} zero or more times
 * <li>Call {@link #endAdd}
 * <li>Call {@link #createSequence}
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
     * Indicates that no more calls to {@link #endAdd} will be made
     */
    void endAdd();

    /**
     * Returns an object which is capable of supplying in order all the
     * values that were added to this object.
     *
     * @return   cached data sequence
     */
    CachedSequence createSequence(); 
}
