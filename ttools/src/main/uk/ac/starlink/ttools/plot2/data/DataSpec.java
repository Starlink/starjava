package uk.ac.starlink.ttools.plot2.data;

import java.io.IOException;
import java.util.Iterator;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.ttools.plot2.Equality;

/**
 * Defines the table-like data that is required for a certain plot.
 * This object does not store the data itself, but can be passed to a
 * {@link DataStore} object to obtain it.
 * A DataSpec has a small memory footprint, is cheap to produce,
 * and can be examined to determine what data is required.
 *
 * <p>The data specification is an aggregation of the following items:
 * </p>
 * <ul>
 * <li>The table the data comes from
 * <li>A list of the columns, or column-like objects, used from the table
 * <li>An identifier for a mask indicating which rows from the table will be
 *     included
 * </ul>
 *
 * <p>Two DataSpecs should evaluate equal if their specification of the
 * above items have the same content, that is if they would generate
 * the same {@link TupleSequence} when presented to a {@link DataStore}.
 *
 * @author   Mark Taylor
 * @since    7 Feb 2013
 */
@Equality
public interface DataSpec {
 
    /**
     * Returns the table object from which this data spec's data is obtained.
     *
     * @return   data source table
     */
    StarTable getSourceTable();

    /**
     * Returns the number of columns that this object produces.
     *
     * @return  TupleSequence column count
     */
    int getCoordCount();

    /**
     * Returns an identifier for the row mask for this object.
     *
     * @return   mask identifier, should implement equals sensibly
     */
    Object getMaskId();

    /**
     * Returns an identifier for one of the the columns produced by this object.
     *
     * @param  icoord  column index
     * @return   column identifier, should implement equals sensibly
     */
    Object getCoordId( int icoord );

    /**
     * Returns the coord reader that can read the data for one of this
     * object's output columns.
     * 
     * @param   icoord  column index
     * @return  column data reader
     */
    Coord getCoord( int icoord );

    /**
     * Returns an object that can be used to read the mask and coordinate
     * values from a row sequence derived from this object's source table.
     * A given UserDataReader can only be used from a single thread,
     * but multiple returns from this method may be used concurrently
     * in different threads.
     *
     * @return   new data reader
     */
    UserDataReader createUserDataReader();
}
