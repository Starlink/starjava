package uk.ac.starlink.fits;

import java.io.DataOutput;
import java.io.IOException;

/**
 * Defines a place where bytes can be written to once and copied out of later.
 * 
 * @author   Mark Taylor
 * @since    10 Jul 2008
 */
interface ByteStore {

    /**
     * Returns an output stream which can be used to write to the store.
     * May be called multiple times; always returns the same object.
     *
     * @return  data sink stream
     */
    DataOutput getStream();

    /**
     * Returns the number of bytes which have been written to the sink stream
     * so far.
     *
     * @return   position in sink
     */
    long getPosition();

    /**
     * Copies all the data which has been written to the sink stream so far
     * into a given destination stream.
     *
     * @param  out  destination stream
     */
    void copy( DataOutput out ) throws IOException;

    /**
     * Tidies up.  Should be called after copy.
     * This object may not be usable following a call to this method.
     */
    void close() throws IOException;
}
