package uk.ac.starlink.table;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Partial implementation of {@link StarTableWriter} which can be subclassed
 * by writers which just write to output streams.
 *
 * @author   Mark Taylor (Starlink)
 * @since    8 Dec 2004
 */
public abstract class StreamStarTableWriter implements StarTableWriter {

    /**
     * Gets an output stream from <tt>location</tt> with reference to
     * <tt>sto</tt> and writes to it using this writer's
     * {@link #writeStarTable(uk.ac.starlink.table.StarTable,
     *                        java.io.OutputStream)} method.
     *
     * @param  startab   table to write
     * @param  location  table destination
     * @param  sto       StarTableOutput
     */
    public void writeStarTable( StarTable startab, String location,
                                StarTableOutput sto )
            throws TableFormatException, IOException {
        OutputStream out = null;
        try {
            out = sto.getOutputStream( location );
            out = new BufferedOutputStream( out );
            writeStarTable( startab, out );
            out.flush();
        }
        finally {
            if ( out != null ) {
                out.close();
            }
        }
    }
}
