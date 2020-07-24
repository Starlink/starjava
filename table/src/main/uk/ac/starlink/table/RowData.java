package uk.ac.starlink.table;

import java.io.IOException;

/**
 * Allows access to the values in a single row of a table.
 *
 * @author   Mark Taylor
 * @since    24 Jul 2020
 */
public interface RowData {

    /**
     * Returns the contents of one cell in this row.
     *
     * @param   icol  column index
     * @return   cell contents
     * @throws   IOException  if there's a problem reading the value
     */
    Object getCell( int icol ) throws IOException;

    /**
     * Returns the contents of all the cells in this row.
     *
     * <p>Note that implementations are in general
     * (unless otherwise restricted by subtype documented contracts)
     * free to return the same array, with different contents,
     * on subsequent invocations of this method,
     * so callers should not rely on the contents being undisturbed.
     *
     * @return   array with one element for each column of this row,
     *           containing cell data; may be reused by subsequent invocations
     */
    Object[] getRow() throws IOException;
}
