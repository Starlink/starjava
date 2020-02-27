package uk.ac.starlink.feather;

import java.io.IOException;
import java.io.OutputStream;
import uk.ac.bristol.star.feather.ColStat;

/**
 * Collects all the values of a column and then writes them out as
 * a feather-format column.
 *
 * @author   Mark Taylor
 * @since    27 Feb 2020
 */
public interface ItemAccumulator {

    /**
     * Receive the next value in the column.
     *
     * @param  item  column cell value
     */
    void addItem( Object item ) throws IOException;

    /**
     * Writes a feather-format column to the given output stream
     * representing all the objects submitted by prior valls to
     * the <code>addItem</code> method.
     * The number of bytes written must be a multiple of 8.
     *
     * @param  out  destination stream
     * @return   details about what was written
     */
    ColStat writeColumnBytes( OutputStream out ) throws IOException;

    /**
     * Tidies up resources, in particular discarding any off-heap
     * temporary storage that may have been allocated.
     * Calling any of the other methods of this interface after
     * closing it has undefined effects.  Multiple calls to close
     * are permitted.
     */
    void close() throws IOException;
}
