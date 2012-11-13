package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;

/**
 * Defines additional column metadata and data for supplementing the
 * existing columns in a StarTable.
 * The data will typically be acquired by reference to a host table,
 * for instance calculating cell values in the additional columns
 * on the basis of values for columns in the host table.
 * The columns are assumed to have the same row count as the host table.
 *
 * @author   Mark Taylor
 * @since    2 Apr 2012
 */
public interface ColumnSupplement {

    /**
     * Returns the number of columns defined by this object.
     *
     * @return  number of columns
     */
    int getColumnCount();

    /**
     * Returns the column metadata object for a given column.
     *
     * @param  icol   column index within this object
     * @return   column metadata for the icol'th column defined by this object
     */
    ColumnInfo getColumnInfo( int icol );

    /**
     * Random access read of a cell defined by this object.
     *
     * @param  irow  row index
     * @param  icol  column index
     * @return   cell content
     */
    Object getCell( long irow, int icol ) throws IOException;

    /**
     * Random access read of a row defined by this object.
     *
     * @param   irow  row index
     * @return   array of cell contents for all the cells in this row
     */
    Object[] getRow( long irow ) throws IOException;

    /**
     * Returns a new iterator over the values in the columns defined by
     * this object.  The supplied row sequence must be from an appropriate
     * host table; if not, behaviour is undefined.
     *
     * @param   rseq   row sequence providing data from the host table
     * @return   iterator over row data from supplementary columns
     */
    SupplementSequence createSequence( RowSequence rseq ) throws IOException;
}
