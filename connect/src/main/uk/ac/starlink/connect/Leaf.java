package uk.ac.starlink.connect;

import java.io.IOException;
import java.io.OutputStream;
import uk.ac.starlink.util.DataSource;

/**
 * Represents a non-directory type file in a (possibly remote) filesystem.
 *
 * @author   Mark Taylor (Starlink)
 * @since    18 Feb 2005
 */
public interface Leaf extends Node {

    /**
     * Returns a data source (replayable input stream) containing the
     * data content of this leaf.
     *
     * @return   data source
     * @throws   FileNotFoundException  if the file named by this leaf does
     *           not exist
     * @throws   IOException  if some other error occurs
     */
    public DataSource getDataSource() throws IOException;

    /**
     * Returns an output stream from this leaf.
     * Whether the file named by this leaf already exists or not,
     * the returned stream should effectively write to a new file
     * (deleting an existing one if necessary).
     *
     * @return   output stream writing to the file named by this leaf
     * @throws   IOException  if there's some error
     */
    public OutputStream getOutputStream() throws IOException;
}
