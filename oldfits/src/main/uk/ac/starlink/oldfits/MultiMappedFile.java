package uk.ac.starlink.oldfits;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.logging.Logger;
import nom.tam.util.RandomAccess;

/**
 * ArrayDataIO implementation which works by mapping but is capable of
 * splitting a file up into multiple mapped sections.  This will be
 * necessary if it's larger than Integer.MAX_VALUE bytes, though note
 * that doing this is only going to be a good idea on a 64-bit OS.
 *
 * @author   Mark Taylor
 * @since    9 Jan 2007
 */
public class MultiMappedFile extends AbstractArrayDataIO
                             implements RandomAccess {

    private final FileChannel channel_;
    private final FileChannel.MapMode mode_;
    private final long length_;
    private final MappedByteBuffer[] niobufs_;
    private final int blockBytes_;
    private final int nblock_;
    private int iblock_;
    private long markPos_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.oldfits" );

    /**
     * Constructs a MultiMappedFile from a channel.
     *
     * @param   chan   file channel
     * @param   mode   mapping mode
     * @param   blockBytes   number of bytes per mapped block 
     *          (though the final one may have fewer)
     */
    public MultiMappedFile( FileChannel chan, FileChannel.MapMode mode,
                            int blockBytes )
            throws IOException {
        channel_ = chan;
        mode_ = mode;
        blockBytes_ = blockBytes;
        length_ = channel_.size();
        long nb = ( ( length_ - 1 ) / (long) blockBytes ) + 1;
        if ( nb > Integer.MAX_VALUE ) {
            throw new IllegalArgumentException( "Block count " + nb
                                              + " too high" );
        }
        nblock_ = toInt( nb );
        niobufs_ = new MappedByteBuffer[ nblock_ ];
        logger_.info( "FITS file mapped as " + nblock_ + " blocks of "
                    + blockBytes_ + " bytes" );
    }

    /**
     * Constructs a MultiMappedFile from a file.
     *
     * @param   file  file
     * @param   mode  mapping mode
     * @param   blockBytes  number of bytes per mapped block
     *          (though the final one may have fewer)
     */
    public MultiMappedFile( File file, FileChannel.MapMode mode,
                            int blockBytes )
            throws IOException {
        this( openChannel( file, mode ), mode, blockBytes );
    }

    public void seek( long offsetFromStart ) throws IOException {
        if ( offsetFromStart < 0 || offsetFromStart > length_ ) {
            throw new IllegalArgumentException( "Seek out of range: "
                                              + offsetFromStart );
        }
        int ib = toInt( offsetFromStart / (long) blockBytes_ );
        int ioff = toInt( offsetFromStart % (long) blockBytes_ );
        getBuffer( ib ).position( ioff );
        iblock_ = ib;
    }

    public long skip( long nskip ) throws IOException {
        long ns = Math.min( nskip, remaining() );
        seek( getFilePointer() + ns );
        return ns;
    }

    public void skipAllBytes( long nskip ) throws IOException {
        if ( nskip > 0 ) {
            if ( nskip <= remaining() ) {
                seek( getFilePointer() + nskip );
            }
            else {
                throw new EOFException();
            }
        }
    }

    public void skipAllBytes( int toSkip ) throws IOException {
        skipAllBytes( (long) toSkip );
    }

    public long getFilePointer() {
        try {
            return (long) iblock_ * blockBytes_
                 + getBuffer( iblock_ ).position();
        }
        catch ( IOException e ) {
            throw new RuntimeException( "Lost file pointer", e );
        }
    }

    public int skipBytes( int toSkip ) throws IOException {
        return toInt( skip( (long) toSkip ) );
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
            return getBuffer( iblock_ ).get();
        }
        catch ( BufferUnderflowException e ) {
            if ( iblock_ >= nblock_ - 1 ) {
                throw (IOException) new EOFException().initCause( e );
            }
            else {
                MappedByteBuffer buf = getBuffer( ++iblock_ );
                buf.position( 0 );
                return buf.get();
            }
        }
    }

    protected void get( byte[] buf, int offset, int length )
            throws IOException {
        while ( length > 0 ) {
            MappedByteBuffer niobuf = getBuffer( iblock_ );
            int nr = Math.min( length, niobuf.remaining() );
            niobuf.get( buf, offset, nr );
            length -= nr;
            offset += nr;
            if ( length > 0 ) {
                iblock_++;
                getBuffer( iblock_ ).position( 0 );
            }
        }
    }

    protected void put( byte b ) throws IOException {
        try {
            getBuffer( iblock_ ).put( b );
        }
        catch ( BufferOverflowException e ) {
            if ( iblock_ >= nblock_ - 1 ) {
                throw (IOException) new EOFException().initCause( e );
            }
            else {
                MappedByteBuffer buf = getBuffer( ++iblock_ );
                buf.position( 0 );
                buf.put( b );
            }
        }
    }

    protected void put( byte[] buf, int offset, int length )
            throws IOException {
        while ( length > 0 ) {
            MappedByteBuffer niobuf = getBuffer( iblock_ );
            int nw = Math.min( length, niobuf.remaining() );
            niobuf.put( buf, offset, nw );
            length -= nw;
            offset += nw;
            if ( length > 0 ) {
                iblock_++;
                getBuffer( iblock_ ).position( 0 );
            }
        }
    }

    public long length() {
        return length_;
    }

    protected long remaining() {
        try {
            return ( nblock_ - iblock_ - 1 ) * (long) blockBytes_
                 + getBuffer( iblock_ ).remaining();
        }
        catch ( IOException e ) {
            throw new RuntimeException( "Error examining mapped file", e );
        }
    }

    public void close() throws IOException {
        channel_.close();
    }

    public void flush() {
        for ( int ib = 0; ib < nblock_; ib++ ) {
            if ( niobufs_[ ib ] != null ) {
                niobufs_[ ib ].force();
            }
        }
    }

    /**
     * Returns the MappedByteBuffer corresponding to a given block of this
     * file.  The relevant region of the file is mapped (lazily) if 
     * necessary.
     *
     * @param  iblock   block index
     * @return   mapped buffer for block <code>iblock</code>
     */
    private MappedByteBuffer getBuffer( int iblock ) throws IOException {
        if ( niobufs_[ iblock ] == null ) {
            long offset = iblock * (long) blockBytes_;
            int leng = toInt( Math.min( length_ - offset, blockBytes_ ) );
            niobufs_[ iblock ] = channel_.map( mode_, offset, leng );
            logger_.config( "Mapping file region " + ( iblock + 1 ) + "/"
                          + nblock_ );
        }
        return niobufs_[ iblock ];
    }

    /**
     * Gets a FileChannel for a File.
     *
     * @param  file  file
     * @param  mode  mapping mode
     * @return  file channel
     */
    private static FileChannel openChannel( File file,
                                            FileChannel.MapMode mode )
            throws IOException {
        String rmode;
        if ( mode == FileChannel.MapMode.READ_ONLY ) {
            rmode = "r";
        }
        else if ( mode == FileChannel.MapMode.READ_WRITE ) {
            rmode = "rw";
        }
        else {
            throw new IllegalArgumentException( "Unsupported mode " + mode );
        }
        return new RandomAccessFile( file, rmode ).getChannel();
    }

    /**
     * Recasts a <code>long</code> value which is known to be in range 
     * to an <code>int</code>.
     *
     * @param   lval  long value, must be between Integer.MIN_VALUE
     *                and Integer.MAX_VALUE
     * @return  <code>int</code> equivalent of <code>lval</code>
     * @param   throws   AssertionError if <code>lval</code> is out of range
     *          (and asssertions are enabled)
     */
    private static int toInt( long lval ) {
        int ival = (int) lval;
        assert (long) ival == lval;
        return ival;
    }
}
