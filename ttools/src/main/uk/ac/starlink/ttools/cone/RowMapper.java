package uk.ac.starlink.ttools.cone;

/**
 * Defines a mapping between objects of some given type and table row index.
 *
 * <p>The following invariants must hold:
 * <pre>
 *    rowIdToIndex(rowIndexToId(ix)) == ix
 *    rowIndexToId(rowIdToIndex(id)) == id
 * </pre>
 *
 *
 * @author   Mark Taylor
 * @since    14 May 2014
 */
public interface RowMapper<I> {

    /**
     * Returns the type of object used for representing row indices.
     *
     * @return   mapper object class
     */
    Class<I> getIdClass();

    /**
     * Returns the table row index corresponding to a given typed identifier.
     *
     * @param  id   identifier object
     * @return  corresponding row index
     */
    long rowIdToIndex( I id );

    /**
     * Returns a typed identifer corresponding to a given row index.
     *
     * @param  index  row index
     * @return  corresponding identifier object
     */
    I rowIndexToId( long index );
}
