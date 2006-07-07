package uk.ac.starlink.table.storage;

import java.io.DataInput;
import java.io.IOException;

/**
 * Extends the {@link java.io.DataInput} interface to permit positioning
 * of the stream.
 *
 * @author   Mark Taylor (Starlink)
 * @since    3 Aug 2004
 */
interface SeekableDataInput extends DataInput {

    /**
     * Sets the file-pointer offset, measured from the beginning of this
     * stream, at which the next read occurs.  The offset may not be set
     * beyond the end of the stream.
     *
     * @param  pos  the offset position, measured in bytes from the start
     *         of this stream
     */
    void seek( long pos ) throws IOException;
}
