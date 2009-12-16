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
 * This is intended to be a general purpose implementation that does
 * something sensible most of the time.
 *
 * @author   Mark Taylor
 * @since    5 Nov 2009
 */
public class AdaptiveByteStore implements ByteStore {

    /* This object works in two phases.
     * In the first phase,
     *     baseOut_ instanceof ByteArrayOutputStream
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
    private File file_;

    /** Fraction of total maximum memory for default memory limit. */
    private static final float MAX_FRACT = 0.125f;

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
            baseOut_ = new ByteArrayOutputStream();
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

    public void copy( OutputStream out ) throws IOException {
        out_.flush();
        if ( file_ == null ) {
            ((ByteArrayOutputStream) baseOut_).writeTo( out );
        }
        else {
            FileByteStore.copy( file_, out );
        }
    }

    public ByteBuffer toByteBuffer() throws IOException {
        out_.flush();
        return file_ == null
             ? ByteBuffer.wrap( ((ByteArrayOutputStream) baseOut_)
                               .toByteArray() )
             : FileByteStore.toByteBuffer( file_ );
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
            assert count_ < memLimit_;
            if ( c1 > memLimit_ ) {
                baseOut_.close();
                file_ = createFile();
                ByteArrayOutputStream byteOut =
                    (ByteArrayOutputStream) baseOut_;
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
    private static int getDefaultLimit() {
        if ( defaultLimit_ <= 0 ) {
            long maxmem = Runtime.getRuntime().maxMemory();
            defaultLimit_ =
                Math.min( (int) ( maxmem * MAX_FRACT ), Integer.MAX_VALUE );
            logger_.info( "AdaptiveByteStore default memory limit = "
                        + "min( " + maxmem + " * " + MAX_FRACT + ", 2^31 ) = "
                        + defaultLimit_ );
        }
        return defaultLimit_;
    }

    /**
     * OutputStream implementation returned to the user of this ByteStore.
     */
    private class AdaptiveOutputStream extends OutputStream {

        public void write( int b ) throws IOException {
            prepareToWrite( 1 );
            baseOut_.write( b );
        }

        public void write( byte[] bs, int off, int len ) throws IOException {
            prepareToWrite( len );
            baseOut_.write( bs, off, len );
        }

        public void write( byte[] bs ) throws IOException {
            prepareToWrite( bs.length );
            baseOut_.write( bs );
        }

        public void flush() throws IOException {
            baseOut_.flush();
        }

        public void close() throws IOException {
            baseOut_.close();
        }
    }
}
