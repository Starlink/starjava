package uk.ac.starlink.table.storage;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import uk.ac.starlink.table.ByteStore;

/**
 * ByteStore implementation which discards bytes.  Not very useful.
 *
 * @author   Mark Taylor
 * @since    11 Jul 2008
 */
public class DiscardByteStore implements ByteStore {

    private final OutputStream devnull_;

    /**
     * Constructor.
     */
    public DiscardByteStore() {
        devnull_ = new OutputStream() {
            public void write( int b ) {
            }
        };
    }

    public OutputStream getOutputStream() {
        return devnull_;
    }

    public void copy( OutputStream out ) {
    }

    public ByteBuffer toByteBuffer() {
        return ByteBuffer.allocate( 0 );
    }

    public void close() {
    }
}
