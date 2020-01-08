package uk.ac.starlink.ttools.plot2.data;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.logging.Logger;
import uk.ac.starlink.table.ByteStore;
import uk.ac.starlink.table.storage.FileByteStore;

/**
 * ByteStore that stores data in a named file which appears completely
 * populated in the filesystem.  This works in a similar way to
 * {@link uk.ac.starlink.table.storage.FileByteStore}, but it writes
 * to a temporary file, and when it's complete it renames it to the
 * requested destination file.
 * The point of this is so that two copies of the same named file are
 * being written at once, they will not interfere with each other.
 *
 * <p>This implementation interferes slightly with the implicit contract
 * of ByteStore, in that all the writing to the output stream has to
 * be complete before a call to {@link #copy} or {@link #toByteBuffers},
 * but that's what you'd do in normal usage anyway.
 *
 * @author   Mark Taylor
 * @since    8 Jan 2020
 */
public class MoveFileByteStore implements ByteStore {

    private final File destFile_;
    private final OutputStream out_;
    private File workFile_;
    private long length_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.plot2.data" );

    /**
     * Constructor.
     *
     * @param  file  destination filename
     */
    public MoveFileByteStore( File file ) throws IOException {
        destFile_ = file;
        workFile_ = DiskCache.toWorkFilename( file );
        out_ = new FileOutputStream( workFile_ ) {
            public void write( int b ) throws IOException {
                super.write( b );
                length_++;
            }
            public void write( byte[] b ) throws IOException {
                super.write( b );
                length_ += b.length;
            }
            public void write( byte[] b, int off, int len ) throws IOException {
                super.write( b, off, len );
                length_ += len;
            }
        };
    }

    public OutputStream getOutputStream() {
        return out_;
    }

    public long getLength() {
        return length_;
    }

    public void copy( OutputStream out ) throws IOException {
        moveFile();
        FileByteStore.copy( destFile_, out );
    }

    public ByteBuffer[] toByteBuffers() throws IOException {
        moveFile();
        return FileByteStore.toByteBuffers( destFile_ );
    }

    private void moveFile() throws IOException {
        out_.flush();
        if ( workFile_ != null ) {
            if ( ! workFile_.renameTo( destFile_ ) ) {
                throw new IOException( "File move failed" );
            }
            workFile_ = null;
        }
    }

    public void close() {
        File file = workFile_ == null ? workFile_ : destFile_;
        try {
            out_.close();
        }
        catch ( IOException e ) {
            logger_.warning( "close error: " + e );
        }
        if ( file.delete() ) {
            logger_.info( "Deleting temporary file: " + file );
        }
        else if ( file.exists() ) {
            logger_.warning( "Failed to delete temporary file " + file );
        }
        else {
            logger_.info( "Temporary file got deleted before close" );
        }
    }
}
