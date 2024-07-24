package uk.ac.starlink.fits;

import java.io.DataOutput;
import java.io.IOException;
import uk.ac.starlink.table.StarTable;

/**
 * Defines an object which can serialize a table to a FITS HDU.
 *
 * @author   Mark Taylor
 * @since    27 Jun 2006
 */
public interface FitsTableSerializer {

    /**
     * Returns header cards suitable for the HDU which will contain the table.
     * Additional metadata and an END marker will be added after these cards,
     * so the returned array must not contain the END card.
     *
     * @return  header cards
     */
    CardImage[] getHeader();

    /**
     * Writes the HDU data for the table to an output stream.
     * This is only intended to be called once following creation
     * of this object.  Subsequent calls result in undefined behaviour.
     *
     * @param  out  destination stream
     */
    void writeData( DataOutput out ) throws IOException;

    /**
     * Returns the number of rows which will be output.
     *
     * @return  row count
     */
    long getRowCount();

    /**
     * Returns the dimensions of the items which will be output for a
     * given column.  This will be <code>null</code> only if that column
     * is not being output.  Otherwise it will be a zero-element array
     * for a scalar, 1-element array for a vector, etc.
     * 
     * @param  icol  column to query
     * @return   dimensions array for data in column <code>icol</code>
     *           or <code>null</code>  for a column being skipped
     */
    int[] getDimensions( int icol );

    /**
     * Returns the FITS TFORM letter which describes the type of data
     * output for a given column.  This is as described by the FITS
     * standard - 'J' for 4-byte integer, 'A' for characters, etc.
     * If the column is not being output, <code>(char)0</code> will be
     * returned.
     *
     * @param  icol   column to query
     * @return   format letter for data in column <code>icol</code>,
     *           or 0 for a column being skipped
     */
    char getFormatChar( int icol );

    /**
     * Returns the bad value (text of the TNULLnn card), if any, used 
     * for a given column.
     *
     * @param   icol  column to query
     * @return   blank value string, or null if there is none or the 
     *           column is being skipped
     */
    String getBadValue( int icol );
}
