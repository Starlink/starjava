package uk.ac.starlink.treeview;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

class BufferMaker {
    final private FileChannel chan;
    final private long position;
    final private long size;

    BufferMaker( FileChannel chan, long position, long size ) {
        this.chan = chan;
        this.position = position;
        this.size = size;
    }

    MappedByteBuffer makeBuffer() throws IOException {
        return chan.map( FileChannel.MapMode.READ_ONLY, position, size );
    }

    long getSize() {
        return size;
    }
}
