package uk.ac.starlink.table;

import java.io.IOException;

/**
 * Interface describing an object which can store and retrive values 
 * from array-like storage.
 *
 * @author   Mark Taylor (Starlink)
 */
public interface ColumnData {

    /**
     * Stores a given value in a given row for this column.
     *
     * @param   val  the object to store
     * @param   irow  the row to store it in
     */
    void storeValue( long irow, Object val ) throws IOException;

    /**
     * Returns the value stored at a given row in this column.
     *
     * @param   irow  the row from which to retrieve the value
     * @return  the value stored at <tt>irow</tt>
     */
    Object readValue( long irow ) throws IOException;
}
