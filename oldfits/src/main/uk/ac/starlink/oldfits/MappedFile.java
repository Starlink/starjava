package uk.ac.starlink.oldfits;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.BufferUnderflowException;
import java.nio.BufferOverflowException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import nom.tam.util.RandomAccess;
import uk.ac.starlink.util.Loader;

/**
 * Provides mapped access to a data buffer using a single mapped byte buffer,
 * compatible with nom.tam.util classes.
 *
 * <p>This class implements the <code>nom.tam.util</code>
 * <code>ArrayDataInput</code>, <code>ArrayDataOutput</code>
 * and <code>RandomAccess</code> interfaces
 * in the same way that <code>nom.tam.util.BufferedFile</code> does.
 * Hence it can be used as a drop-in replacement for BufferedFile.
 * Unlike BufferedFile however, it does mapped access to files
 * (using java.nio.Buffer objects).  This may be moderately more efficient
 * for sequential access to a file, but is dramatically more efficient
 * if very random access is required.  This is because BufferedFile
 * effectively always assumes that you are going to read sequentially,
 * so that accessing a single datum distant from (or before) the last
 * datum accessed always results in filling a whole buffer.
 *
 * <h3>Limitations:</h3>
 * <ul>
 * <li>Files larger than Integer.MAX_VALUE bytes may not currently be accessed
 * <li>Access to very large files may fail if virtual memory runs out
 * </ul>
 *
 * @author   Mark Taylor (Starlink)
 * @since    30 Aug 2002
 */
public class MappedFile extends AbstractArrayDataIO implements RandomAccess {

    private final ByteBuffer niobuf_;
    private int size_;
    private long markPos_;

    /**
     * Constructs a MappedFile object from a byte buffer.
     *
     * @param   buf  byte buffer
     */
    public MappedFile( ByteBuffer buf ) {
        niobuf_ = buf;
    }

    /**
     * Constructs a MappedFile object by mapping the whole of
     * an existing file using read-only mode.
     *
     * @param  filename  name of the file to map
     * @throws  FileTooLongException  if the file is too long to map
     */
    public MappedFile( String filename ) throws IOException {
        this( filename, "r" );
    }

    /**
     * Constructs a MappedFile object by mapping the whole of
     * an existing file with a given mode.
     *
     * @param  filename  name of the file to map
     * @param  mode  mode
     * @throws  FileTooLongException  if the file is too long to map
     */
    public MappedFile( String filename, String mode ) throws IOException {
        this( getExistingFileBuffer( filename, mode ) );
    }

    /**
     * Constructs a MappedFile object by mapping part of an existing file
     * with a given mode.
     *
     * @param  filename  name of the file to map
     * @param  mode  mode
     * @param  start  offset of region to map
     * @param  size  length of region to map
     */
    public MappedFile( String filename, String mode, long start, int size )
            throws IOException {
        this( getNioBuffer( filename, mode, start, size ) );
    }

    public void seek( long offsetFromStart ) throws IOException {
        if ( offsetFromStart > niobuf_.capacity() ) {
            throw new IOException( "Attempt to seek beyond end of file" );
        }
        niobuf_.position( (int) offsetFromStart );
    }

    public long skip( long offset ) {
        return (long) skipBytes( (int) Math.min( offset,
                                                 (long) Integer.MAX_VALUE ) );
    }

    public long getFilePointer() {
        return (long) niobuf_.position();
    }

    public int skipBytes( int toSkip ) {
        int nskip = Math.max( toSkip, 0 );
        nskip = Math.min( toSkip, niobuf_.remaining() );
        niobuf_.position( niobuf_.position() + nskip );
        return nskip;
    }

    public void skipAllBytes( long toSkip ) throws IOException {
        if ( toSkip > 0 ) {
            if ( toSkip <= niobuf_.remaining() ) {
                int nskip = (int) toSkip;
                assert nskip == toSkip;
                niobuf_.position( niobuf_.position() + nskip );
            }
            else {
                throw new EOFException();
            }
        }
    }

    public void skipAllBytes( int toSkip ) throws IOException {
        skipAllBytes( (long) toSkip );
    }

    public boolean markSupported() {
        return true;
    }

    public void mark( int readlimit ) {
        markPos_ = getFilePointer();
    }

    public void reset() throws IOException {
        seek( markPos_ );
    }

    protected byte get() throws IOException {
        try {
            return niobuf_.get();
        }
        catch ( BufferUnderflowException e ) {
            throw (IOException) new EOFException().initCause( e );
        }
    }

    protected void get( byte[] buf, int offset, int length )
            throws IOException {
        try {
            niobuf_.get( buf, offset, length );
        }
        catch ( BufferUnderflowException e ) {
            throw (IOException) new EOFException().initCause( e );
        }
    }

    protected void put( byte b ) throws IOException {
        try {
            niobuf_.put( b );
        }
        catch ( BufferOverflowException e ) {
            throw (IOException) new EOFException().initCause( e );
        }
    }

    protected void put( byte[] buf, int offset, int length )
            throws IOException {
        try {
            niobuf_.put( buf, offset, length );
        }
        catch ( BufferOverflowException e ) {
            throw (IOException) new EOFException().initCause( e );
        }
    }

    public long length() {
        return niobuf_.capacity();
    }

    protected long remaining() {
        return niobuf_.remaining();
    }

    public void close() {
        // no action
    }

    public void flush() {
        if ( niobuf_ instanceof MappedByteBuffer ) {
            ((MappedByteBuffer) niobuf_).force();
        }
    }

    /**
     * Returns a mapped byte buffer which results from mapping a given file.
     *
     * @param   filename  filename
     * @param   mode   mapping mode "r" or "rw"
     * @param   start  offset of mapped region
     * @param   size   length of mapped region
     * @return  mapped byte buffer
     */
    private static MappedByteBuffer getNioBuffer( String filename, String mode,
                                                  long start, int size )
            throws IOException {
        RandomAccessFile raf = new RandomAccessFile( filename, mode );
        FileChannel channel = raf.getChannel();
        FileChannel.MapMode mapmode;
        if ( mode.equals( "r" ) ) {
            mapmode = FileChannel.MapMode.READ_ONLY;
        }
        else if ( mode.equals( "rw" ) ) {
            mapmode = FileChannel.MapMode.READ_WRITE;
        }
        else {
            throw new IllegalArgumentException(
                "Invalid mode string \"" + mode +
                "\" - must be \"r\" or \"rw\"" );
        }
        MappedByteBuffer buf;
        try {
            buf = channel.map( mapmode, start, size );
        }

        /* I wouldn't expect out of memory to be an issue here, since I 
         * I think it's address space not heap memory which is the 
         * significant resource, but you do see OOMEs inside IOEs 
         * for large files on 32-bit machines (note, the width of the
         * JVM is not the issue). */
        catch ( IOException e ) {
            if ( e.getCause() instanceof OutOfMemoryError ) {
                String msg = "Out of memory when mapping file " + filename;
                if ( ! Loader.is64Bit() ) {
                    msg += " (64-bit OS might help?)";
                }
                throw new FileTooLongException( msg, e );
            }
            else {
                throw e;
            }
        }
        finally {
            channel.close();  // has no effect on the validity of the mapping
        }
        return buf;
    }

    /**
     * Maps an existing file.
     *
     * @param  filename  name of the file to map
     * @param  mode  mode
     * @return  mapped buffer
     * @throws  FileTooLongException  if the file is too long to map
     */
    private static MappedByteBuffer getExistingFileBuffer( String filename,
                                                           String mode )
            throws IOException {
        File file = new File( filename );
        if ( ! file.exists() ) {
            throw new FileNotFoundException( "No such file " + filename );
        }
        long size = file.length();
        if ( size > Integer.MAX_VALUE ) {
            throw new FileTooLongException( filename + " too long to map: "
                                          + size + " > " + Integer.MAX_VALUE
                                          + " - use buffered reads instead" );
        }
        return getNioBuffer( filename, mode, 0L, (int) size );
    }

    /**
     * Exception indicating that a file is too long to map.
     */
    public static class FileTooLongException extends IOException {
        FileTooLongException( String msg ) {
            super( msg );
        }
        FileTooLongException( String msg, Throwable e ) {
            super( msg );
            initCause( e );
        }
    }
}
