package uk.ac.starlink.ttools.plot2.data;

/**
 * Defines a capability for storing vectors of typed data.
 *
 * @author   Mark Taylor
 * @since    4 Feb 2013
 */
public interface CachedColumnFactory {

    /**
     * Creates a data storage object capable of storing a vector of 
     * a given type.  Both fixed and unknown element counts are supported.
     *
     * @param  type   data type of elements to be stored
     * @param  nrow   maximum number of elements to be stored;
     *                if a value &lt;0 is supplied,
     *                an indeterminate number is permitted
     * @return   storage object
     */
    CachedColumn createColumn( StorageType type, long nrow );
}
