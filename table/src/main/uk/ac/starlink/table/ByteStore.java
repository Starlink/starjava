package uk.ac.starlink.table;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Defines a place where bytes can be written to and then read from.
 *
 * @author   Mark Taylor
 * @since    11 Jul 2008
 */
public interface ByteStore {

    /**
     * Returns an output stream which can be used to write to the store.
     * May be called multiple times; always returns the same object.
     * Note that this is not in general buffered - it is the responsibility
     * of the user to take steps like wrapping it in a 
     * {@link java.io.BufferedOutputStream} for efficiency if required.
     *
     * @return  data sink stream
     */
    OutputStream getOutputStream();

    /** 
     * Takes all the data written so far into this store's sink stream
     * and copies it to a destination stream.  The output stream is not
     * closed.
     *
     * @param  out  data destination stream
     */
    void copy( OutputStream out ) throws IOException;

    /**
     * Returns a byte buffer containing the bytes written to this store.
     * The buffer's <em>limit</em> indicates the amount of data written.
     *
     * <p>Usual usage will be to write all data, then call this method once;
     * this model may affect implementation decisions about efficiency.
     *
     * @return   byte buffer containing bytes written
     * @throws   IOException  if there is a problem, including if the
     *           number of byte written exceeds <code>Integer.MAX_VALUE</code>
     */
    ByteBuffer toByteBuffer() throws IOException;

    /**
     * Tidies up.  Should be called when the data in this object is no 
     * longer required.
     * This object may no longer be usable following a call to this method.
     */
    void close();
}
