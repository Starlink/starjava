package uk.ac.starlink.fits;

import java.io.EOFException;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.logging.Logger;

/**
 * Random-access BasicInput implementation that maps a given region of a file
 * as a monolithic byte buffer.
 * On close, an attempt is made to unmap the buffer.
 *
 * <p><strong>Note:</strong> <strong>DO NOT</strong> use an instance
 * of this class from multiple threads - see {@link Unmapper}.
 *
 * @author   Mark Taylor
 * @since    1 Dec 2014
 */
public class SimpleMappedInput implements BasicInput {

    private MappedByteBuffer niobuf_;
    private final String logName_;
    private final Unmapper unmapper_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.fits" );

    /**
     * Constructor.
     *
     * @param   chan  file channel, preferably read-only
     * @param   pos   offset into file of stream start
     * @param   size  number of bytes in stream
     * @param   logName  name for mapped region used in logging messages
     */
    public SimpleMappedInput( FileChannel chan, long pos, int size,
                              String logName )
            throws IOException {
        niobuf_ = chan.map( FileChannel.MapMode.READ_ONLY, pos, size );
        logger_.info( "Mapping as single file: " + logName );
        logName_ = logName;
        unmapper_ = Unmapper.getInstance();
    }

    public boolean isRandom() {
        return true;
    }

    public void seek( long pos ) throws EOFException {
        int ipos = (int) pos;
        if ( ipos == pos ) {
            try {
                niobuf_.position( ipos );
            }
            catch ( IllegalArgumentException e ) {
                if ( ipos > niobuf_.limit() ) {
                    throw (EOFException) new EOFException().initCause( e );
                }
                else {
                    throw e;
                }
            }
        }
        else {
            throw new EOFException( "Out of bounds: " + pos );
        }
    }

    public long getOffset() {
        return niobuf_.position();
    }

    public void skip( long nbyte ) throws IOException {
        seek( getOffset() + nbyte );
    }

    public byte readByte() throws EOFException {
        try {
            return niobuf_.get();
        }
        catch ( BufferUnderflowException e ) {
            throw (EOFException) new EOFException().initCause( e );
        }
    }

    public short readShort() throws EOFException {
        try {
            return niobuf_.getShort();
        }
        catch ( BufferUnderflowException e ) {
            throw (EOFException) new EOFException().initCause( e );
        }
    }

    public int readInt() throws EOFException {
        try {
            return niobuf_.getInt();
        }
        catch ( BufferUnderflowException e ) {
            throw (EOFException) new EOFException().initCause( e );
        }
    }

    public long readLong() throws EOFException {
        try {
            return niobuf_.getLong();
        }
        catch ( BufferUnderflowException e ) {
            throw (EOFException) new EOFException().initCause( e );
        }
    }

    public float readFloat() throws EOFException {
        try {
            return niobuf_.getFloat();
        }
        catch ( BufferUnderflowException e ) {
            throw (EOFException) new EOFException().initCause( e );
        }
    }
 
    public double readDouble() throws EOFException {
        try {
            return niobuf_.getDouble();
        }
        catch ( BufferUnderflowException e ) {
            throw (EOFException) new EOFException().initCause( e );
        }
    }

    public void close() {
        MappedByteBuffer niobuf = niobuf_;
        niobuf_ = null;
        if ( niobuf != null ) {
            boolean success = unmapper_.unmap( niobuf );
            logger_.config( "Attempt to unmap " + logName_ 
                          + ": " + ( success ? "succeed" : "fail" ) );
        }
    }
}
