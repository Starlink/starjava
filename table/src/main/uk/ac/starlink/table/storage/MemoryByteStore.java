package uk.ac.starlink.table.storage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import uk.ac.starlink.table.ByteStore;

/**
 * ByteStore implementation which stores bytes in a buffer in memory.
 *
 * @author   Mark Taylor
 * @since    11 Jul 2008
 */
public class MemoryByteStore extends ByteArrayOutputStream
                             implements ByteStore {

    public MemoryByteStore() {
    }

    public OutputStream getOutputStream() {
        return this;
    }

    public void copy( OutputStream out ) throws IOException {
        flush();
        out.write( this.buf, 0, this.count );
    }

    public long getLength() {
        return this.count;
    }

    public ByteBuffer[] toByteBuffers() {
        byte[] buf2 = new byte[ count ];
        System.arraycopy( buf, 0, buf2, 0, count );
        buf = buf2;
        return new ByteBuffer[] { ByteBuffer.wrap( buf, 0, count ) };
    }

    public void close() {
    }
}
