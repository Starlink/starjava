package uk.ac.starlink.table.storage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.logging.Logger;
import uk.ac.starlink.table.ByteStore;

/**
 * ByteStore implementation which uses a temporary file.
 *
 * @author   Mark Taylor
 * @since    11 Jul 2008
 */
public class FileByteStore implements ByteStore {

    private final File file_;
    private final OutputStream out_;
    private long length_;
    private final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.table.storage" );

    /**
     * Constructs a new FileByteStore which uses the given file as a
     * backing store.  Nothing is done to mark this file as temporary.
     *
     * @param  file   location of the backing file which will be used
     * @throws IOException  if there is some I/O-related problem with
     *         opening the file
     * @throws SecurityException  if the current security context does not
     *         allow writing to a temporary file
     */
    public FileByteStore( File file ) throws IOException {
        file_ = file;
        out_ = new FileOutputStream( file_ ) {
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

    /**
     * Constructs a new FileByteStore which uses a temporary file as
     * backing store.
     * The temporary file will be written to the default temporary
     * directory, given by the value of the <code>java.io.tmpdir</code>
     * system property.
     *
     * @throws IOException  if there is some I/O-related problem with
     *         opening the file
     * @throws SecurityException  if the current security context does not
     *         allow writing to a temporary file
     */
    public FileByteStore() throws IOException {
        this( File.createTempFile( "FileByteStore", ".bin" ) );
        file_.deleteOnExit();
        logger_.info( "Creating new temporary file: " + file_ );
    }

    /**
     * Returns the file used by this store.
     *
     * @return  file
     */
    public File getFile() {
        return file_;
    }

    public OutputStream getOutputStream() {
        return out_;
    }

    public long getLength() {
        return length_;
    }

    public void copy( OutputStream out ) throws IOException {
        out_.flush();
        copy( file_, out );
    }

    public ByteBuffer[] toByteBuffers() throws IOException {
        out_.flush();
        return toByteBuffers( file_ );
    }

    /**
     * Utility method to copy the contents of a file to an output stream.
     * The stream is not closed.
     *
     * @param  file  file
     * @param  out  destination stream
     */
    public static void copy( File file, OutputStream out ) throws IOException {
        long size = file.length();
        try (
            FileInputStream in = new FileInputStream( file );
            FileChannel inChannel = in.getChannel()
        ) {
            WritableByteChannel outChannel =
                  out instanceof FileOutputStream
                ? ((FileOutputStream) out).getChannel()
                : Channels.newChannel( out );
            long pos = 0;
            while ( pos < size ) {
                pos += inChannel.transferTo( pos, size - pos, outChannel );
            }
            if ( pos != size ) {
                throw new IOException( "Error in byte transfer" );
            }
        }
    }

    /**
     * Returns a read-only ByteBuffer array representing the contents
     * of a file, with default maximum buffer length.
     * If the file can be represented in a single ByteBuffer the result will be
     * a single-element array; otherwise the concatenation of the buffers
     * in the result gives the file content.
     *
     * @param  file  file
     * @return   mapped byte buffers
     */
    public static ByteBuffer[] toByteBuffers( File file ) throws IOException {

        /* Maximum buffer length has to be <= Integer.MAX_VALUE.
         * Integer.MAX_VALUE is odd, so might possibly introduce
         * unnecessary alignment issues, so use a nearby value that's
         * a round number.  It's still around 2 billion. */
        return toByteBuffers( file, 0x7f000000 );
    }

    /**
     * Returns a read-only ByteBuffer array representing the contents
     * of a file, with configurable maximum buffer length.
     * If the file can be represented in a single ByteBuffer
     * of up to <code>maxLen</code> bytes the result will be
     * a single-element array; otherwise the concatenation of the buffers
     * in the result gives the file content.
     *
     * @param  file  file
     * @param  maxLen  maximum length of a single buffer
     * @return   mapped byte buffers
     */
    static ByteBuffer[] toByteBuffers( File file, int maxLen )
            throws IOException {
        long size = file.length();
        if ( size == 0 ) {
            return new ByteBuffer[] { ByteBuffer.allocate( 0 ) };
        }
        FileInputStream in = new FileInputStream( file );
        FileChannel chan = in.getChannel();
        FileChannel.MapMode mode = FileChannel.MapMode.READ_ONLY;
        long mBuf = ( ( size - 1 ) / maxLen ) + 1;
        int nBuf = (int) mBuf;
        if ( nBuf != mBuf ) {
            throw new IOException( "HOW big???" );
        }
        ByteBuffer[] bufs = new ByteBuffer[ nBuf ];
        for ( int ib = 0; ib < nBuf; ib++ ) {
            long start = ib * (long) maxLen;
            assert size >= 0;
            assert size - start > 0;
            long len = Math.min( size - start, maxLen );
            bufs[ ib ] = chan.map( mode, start, len );
        }
        in.close();
        return bufs;
    }

    public void close() {
        try {
            out_.close();
        }
        catch ( IOException e ) {
            logger_.warning( "close error: " + e );
        }
        if ( file_.delete() ) {
            logger_.info( "Deleting temporary file: " + file_ );
        }
        else if ( file_.exists() ) {
            logger_.warning( "Failed to delete temporary file " + file_ );
        }
        else {
            logger_.info( "Temporary file got deleted before close" );
        }
    }
}
