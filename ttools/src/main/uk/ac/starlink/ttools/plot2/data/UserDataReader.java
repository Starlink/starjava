package uk.ac.starlink.ttools.plot2.data;

import java.io.IOException;
import uk.ac.starlink.table.RowSequence;

/**
 * Reader that can interrogate a row sequence to return the coordinate and
 * mask values specified by a DataSpec.
 * It is applied to a RowSequence obtained from the DataSpec's source table.
 *
 * <p>Note this object is applied to an external RowSequence which must be
 * advanced externally, rather than owning and advancing its own RowSequence.
 * This is because multiple instances of this class may share the same
 * RowSequence.
 *
 * <p>The read methods are sequential and must be presented with RowSequence
 * and row index values appropriate for a given row in the table.
 * Despite the presence of the row index parameters, they are <em>not</em>
 * random-access methods, but the row index may be needed alongside the
 * correctly positioned row sequence.
 *
 * <p>Instances of this object are not expected to be usable from
 * multiple concurrent threads.
 *
 * @see   DataSpec
 * @author   Mark Taylor
 * @since    10 Feb 2013
 */
public interface UserDataReader {

    /**
     * Returns the mask flag for the current position in a row sequence.
     *
     * @param   rseq  row sequence positioned at the row of interest
     * @param   irow  row index of the row of interest
     * @return  true  iff the row of interest is included in the data set
     */
    boolean getMaskFlag( RowSequence rseq, long irow )
            throws IOException;

    /**
     * Returns a user coordinate value array for a given coordinate at
     * the current position in a row sequence.
     * The returned value is an array which may be re-used,
     * so it should not be assumed to retain its contents between
     * calls to this method.
     *
     * @param   rseq  row sequence positioned at the row of interest
     * @param   irow  row index of the row of interest
     * @param   icoord   coordinate index to read
     * @return  array of user values (not storage values)
     *          for the specified coordinate at the row of interest
     */
    Object[] getUserCoordValues( RowSequence rseq, long irow, int icoord )
            throws IOException;
}
