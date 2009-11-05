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
        out_ = new FileOutputStream( file_ );
    }

    /**
     * Constructs a new FileByteStore which uses a temporary file as
     * backing store.
     * The temporary file will be written to the default temporary
     * directory, given by the value of the <tt>java.io.tmpdir</tt>
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

    public void copy( OutputStream out ) throws IOException {
        out_.flush();
        copy( file_, out );
    }

    public ByteBuffer toByteBuffer() throws IOException {
        out_.flush();
        return toByteBuffer( file_ );
    }

    /**
     * Utility method to copy the contents of a file to an output stream.
     * The stream is not closed.
     *
     * @param  file  file
     * @param  out  destination stream
     */
    static void copy( File file, OutputStream out ) throws IOException {
        FileInputStream in = new FileInputStream( file );
        long size = file.length();
        FileChannel inChannel = in.getChannel();
        WritableByteChannel outChannel = out instanceof FileOutputStream
                                       ? ((FileOutputStream) out).getChannel()
                                       : Channels.newChannel( out );
        long count = inChannel.transferTo( 0, size, outChannel );
        in.close();
        if ( count < size ) {
            throw new IOException( "Only " + count + "/" + size
                                 + " bytes could be transferred" );
        }
    }

    /**
     * Utility method to return a ByteBuffer backed by a file.
     *
     * @param  file  file
     * @return   mapped byte buffer
     * @throws  IOException if the file is too large to map
     */
    static ByteBuffer toByteBuffer( File file ) throws IOException {
        long size = file.length();
        if ( size > Integer.MAX_VALUE ) {
            throw new IOException( "File too big to map" );
        }
        FileInputStream in = new FileInputStream( file );
        ByteBuffer bbuf = in.getChannel()
                            .map( FileChannel.MapMode.READ_ONLY, 0, size );
        in.close();
        return bbuf;
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
