package uk.ac.starlink.votable;

import java.io.IOException;

/**
 * Interface which may be implemented by {@link Table} 
 * objects to provide random access to their data.
 */
public interface RandomTable {

    /**
     * Must return the actual number of rows.
     *
     * @return  number of rows, an integer &gt;=0
     */
    int getRowCount();

    /**
     * Returns a given row.
     *
     * @param  irow  the row index (first row is 0)
     * @return the array of cell values
     */
    Object[] getRow( int irow ) throws IOException;

    /**
     * Returns a given cell.
     * 
     * @param  icol  the column index (first column is 0)
     * @return irow  the row index (first row is 0)
     */
    Object getCell( int irow, int icol ) throws IOException;
}
