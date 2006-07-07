package uk.ac.starlink.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Extends FileInputStream and reimplements most of its methods using
 * a MappedByteBuffer (NIO).  This is experimental; one might expect it
 * to lead to (dramatic?) improvements in file read performance.
 * Doesn't seem to though.  Not currently used.
 *
 * @author   Mark Taylor
 * @since    7 Feb 2006
 */
class MappedFileInputStream extends FileInputStream {

    private MappedByteBuffer mapped_;

    MappedFileInputStream( File file ) throws IOException {
        super( file );
        FileChannel chan = getChannel();
        long size = chan.size();
        if ( size < Integer.MAX_VALUE ) {
            mapped_ = chan.map( FileChannel.MapMode.READ_ONLY, 0, size );
        }
        else {
            throw new IOException( "File too long to map" );
        }
    }

    public int read() {
        return mapped_.hasRemaining() ? (int) mapped_.get()
                                      : -1;
    }

    public int read( byte[] buf ) {
        return read( buf, 0, buf.length );
    }

    public int read( byte[] buf, int off, int leng ) {
        int remain = mapped_.remaining();
        if ( remain == 0 ) {
            return -1;
        }
        else {
            int count = Math.min( leng, remain );
            mapped_.get( buf, off, count );
            return count;
        }
    }

    public long skip( long n ) {
        int count = Math.min( mapped_.remaining(), 
                              (int) Math.min( (long) Integer.MAX_VALUE, n ) );
        mapped_.position( mapped_.position() + count );
        return count;
    }

    public void close() throws IOException {
        getChannel().close();
        mapped_ = null;
        super.close();
    }
}
