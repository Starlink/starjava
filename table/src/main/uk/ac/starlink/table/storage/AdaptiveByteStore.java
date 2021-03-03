package uk.ac.starlink.table.storage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.logging.Logger;
import uk.ac.starlink.table.ByteStore;

/**
 * ByteStore which adopts a hybrid approach between use of memory 
 * and use of disk.  Bytes are written into an array in memory up to
 * a given size limit; if the amount written exceeds this limit, 
 * it's all put in a temporary file instead.
 *
 * <p>This class is intended to be a general purpose StoragePolicy 
 * implementation that does something sensible most of the time.
 * The details of the implementation may be changed following
 * experience.
 *
 * <p>The current implementation uses {@link java.nio.ByteBuffer#allocateDirect}
 * for byte arrays in memory apart from rather small ones.
 * On most OSes this corresponds to using <code>malloc()</code>,
 * thus avoiding heavy use of JVM heap memory.
 * Note very large arrays are still stored on disk, not directly
 * allocated.
 *
 * @author   Mark Taylor
 * @since    5 Nov 2009
 */
public class AdaptiveByteStore implements ByteStore {

    /* This object works in two phases.
     * In the first phase,
     *     baseOut_ instanceof BytesOutputStream
     *     count_ == number of bytes written < memLimit_
     *     file_ == null
     * In the second phase:
     *     baseOut_ instanceof FileOutputStream
     *     count_ is undefined
     *     file_ is the temporary file
     * Test (file_==null) to determine which phase it is in.
     */

    private final int memLimit_;
    private AdaptiveOutputStream out_;
    private OutputStream baseOut_;
    private int count_;
    private long length_;
    private File file_;

    /** Fraction of total maximum memory for default memory limit. */
    private static final float MAX_FRACT = 0.125f;

    /** Largest byte array to keep in heap (larger ones allocated direct). */
    private static final int MAX_HEAP = 64 * 1024;

    private static int defaultLimit_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.table.storage" );

    /**
     * Constructs a new store with a given maximum memory limit.
     *
     * @param   memLimit  maximum size of in-memory buffer
     */
    public AdaptiveByteStore( int memLimit ) throws IOException {
        try {
            baseOut_ = new BytesOutputStream();
        }
        catch ( OutOfMemoryError e ) {
            logger_.info( "Insufficient heap for " + memLimit
                        + " bytes - go direct to file storage" );
            file_ = createFile();
            baseOut_ = new FileOutputStream( file_ );
        }
        memLimit_ = memLimit;
        out_ = new AdaptiveOutputStream();
    }

    /**
     * Constructs a new store with a default memory limit.
     */
    public AdaptiveByteStore() throws IOException {
        this( getDefaultLimit() );
    }

    public OutputStream getOutputStream() {
        return out_;
    }

    public long getLength() {
        return length_;
    }

    public void copy( OutputStream out ) throws IOException {
        out_.flush();
        if ( file_ == null ) {
            ((BytesOutputStream) baseOut_).writeTo( out );
        }
        else {
            FileByteStore.copy( file_, out );
        }
    }

    public ByteBuffer[] toByteBuffers() throws IOException {
        out_.flush();
        if ( file_ == null ) {
            BytesOutputStream byteOut = (BytesOutputStream) baseOut_;
            byte[] buf = byteOut.toByteArray();
            final ByteBuffer bbuf;
            if ( count_ < MAX_HEAP ) {
                bbuf = ByteBuffer.wrap( byteOut.toByteArray() );
            }
            else {
                bbuf = ByteBuffer.allocateDirect( count_ );
                if ( bbuf.isDirect() ) {
                    logger_.info( "malloc " + count_ + " bytes" );
                }
                bbuf.put( byteOut.getBuf(), 0, byteOut.getCount() );
            }
            return new ByteBuffer[] { bbuf };
        }
        else {
            return FileByteStore.toByteBuffers( file_ );
        }
    }

    public void close() {
        try {
            out_.close();
        }
        catch ( IOException e ) {
            logger_.warning( "close error: " + e );
        }
        if ( file_ != null ) {
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

    /**
     * Ensures that the underlying output stream (baseOut_) is ready to
     * accept c bytes.
     *
     * @param   c   number of bytes about to be written
     */
    private void prepareToWrite( int c ) throws IOException {
        int c1 = count_ + c;

        /* If we're in phase 1 and the declared write would overwrite the
         * end of the memory buffer, shift to phase 2: copy all the data
         * from memory to a file, and continue. */
        if ( file_ == null ) {
            assert count_ <= memLimit_;
            if ( c1 > memLimit_ ) {
                baseOut_.close();
                file_ = createFile();
                BytesOutputStream byteOut = (BytesOutputStream) baseOut_;
                logger_.info( "AdaptiveByteStore: switching from memory buffer"
                            + " to temp file " + file_ + " at " + memLimit_
                            + " bytes" );
                baseOut_ = new FileOutputStream( file_ );
                byteOut.writeTo( baseOut_ ); 
            }
        }
        count_ = c1;
    }

    /**
     * Returns a temporary file, which will be deleted on exit.
     */
    private static File createFile() throws IOException {
        File file = File.createTempFile( "AdaptiveByteStore", ".bin" );
        file.deleteOnExit();
        return file;
    }

    /**
     * Calculates the default memory limit used by this class.
     *
     * @return  default memory limit
     */
    public static int getDefaultLimit() {
        if ( defaultLimit_ <= 0 ) {
            int maxmem = (int) Math.min( Runtime.getRuntime().maxMemory(),
                                         Integer.MAX_VALUE );
            defaultLimit_ = (int) ( maxmem * MAX_FRACT );
            logger_.info( "AdaptiveByteStore default memory limit = "
                        + formatByteCount( maxmem ) + " * " + MAX_FRACT + " = "
                        + formatByteCount( defaultLimit_ ) );
        }
        return defaultLimit_;
    }

    /**
     * Formats a number of bytes for human readability.
     *
     * @param  nbyte  number of bytes
     * @return  string
     */
    private static String formatByteCount( int nbyte ) {
        return (int) Math.round( (double) nbyte / 1024 / 1024 ) + "M";
    }

    /**
     * OutputStream implementation returned to the user of this ByteStore.
     */
    private class AdaptiveOutputStream extends OutputStream {

        public void write( int b ) throws IOException {
            prepareToWrite( 1 );
            baseOut_.write( b );
            length_++;
        }

        public void write( byte[] bs, int off, int len ) throws IOException {
            prepareToWrite( len );
            baseOut_.write( bs, off, len );
            length_ += len;
        }

        public void write( byte[] bs ) throws IOException {
            prepareToWrite( bs.length );
            baseOut_.write( bs );
            length_ += bs.length;
        }

        public void flush() throws IOException {
            baseOut_.flush();
        }

        public void close() throws IOException {
            baseOut_.close();
        }
    }

    /**
     * Extension of ByteArrayOutputStream which publicises protected fields.
     */
    private static class BytesOutputStream extends ByteArrayOutputStream {
        public byte[] getBuf() {
            return buf;
        }
        public int getCount() {
            return count;
        }
    }
}
