package uk.ac.starlink.votable;

import java.io.IOException;

/**
 * Interface describing an object which can read a set of table data
 * rows sequentially.  The <tt>readRow</tt> method iterates through
 * all the rows in the table, then returns <tt>null</tt>.
 *
 * @author   Mark Taylor (Starlink)
 */
public interface RowStepper {

    /**
     * Returns the next available row from the table as an array of objects,
     * or <tt>null</tt> if the end of the table has been reached.
     * Each invocation will return an array with the same number of elements.
     *
     * @return   an array of objects representing the data in the row
     * @throws   IOException if there is I/O trouble
     */
    Object[] nextRow() throws IOException;
}
