package uk.ac.starlink.ttools.plot2.data;

import java.io.IOException;
import uk.ac.starlink.table.RowData;

/**
 * Reader that can interrogate a RowData to return the coordinate and
 * mask values specified by a DataSpec.
 * It is applied to a RowData obtained from the DataSpec's source table.
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
     * Returns the mask flag for a row.
     *
     * @param   rdata  row data for the row
     * @param   irow  row index of the row
     * @return  true  iff the row of interest is included in the data set
     */
    boolean getMaskFlag( RowData rdata, long irow )
            throws IOException;

    /**
     * Returns a user coordinate value array for a given coordinate in a row.
     * The returned value is an array which may be re-used,
     * so it should not be assumed to retain its contents between
     * calls to this method.
     *
     * @param   rdata row data
     * @param   irow  row index corresponding to the row
     * @param   icoord   coordinate index to read
     * @return  array of user values (not storage values)
     *          for the specified coordinate at the row of interest
     */
    Object[] getUserCoordValues( RowData rdata, long irow, int icoord )
            throws IOException;
}
