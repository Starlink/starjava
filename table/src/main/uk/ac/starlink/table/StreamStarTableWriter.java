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
     * Gets an output stream from <code>location</code> with reference to
     * <code>sto</code> and writes to it using this writer's
     * {@link #writeStarTable(uk.ac.starlink.table.StarTable,
     *                        java.io.OutputStream)} method.
     *
     * <p>This method just invokes the static utility method of the same name.
     *
     * @param  startab   table to write
     * @param  location  table destination
     * @param  sto       StarTableOutput
     */
    public void writeStarTable( StarTable startab, String location,
                                StarTableOutput sto )
            throws TableFormatException, IOException {
        writeStarTable( this, startab, location, sto );
    }

    /**
     * Utility method that writes a table to a location
     * using a given output handler.
     *
     * @param  writer   output handler
     * @param  startab   table to write
     * @param  location   destination
     * @param  sto      output controller
     */
    public static void writeStarTable( StarTableWriter writer,
                                       StarTable startab, String location,
                                       StarTableOutput sto )
            throws IOException {
        try ( OutputStream out =
                  new BufferedOutputStream( sto.getOutputStream( location ) ) ){
            writer.writeStarTable( startab, out );
            out.flush();
        }
    }
}
