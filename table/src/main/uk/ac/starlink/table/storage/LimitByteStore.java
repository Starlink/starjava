package uk.ac.starlink.table.storage;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.logging.Logger;
import uk.ac.starlink.table.ByteStore;

/**
 * ByteStore wrapper class which will throw an IOException during writing
 * if an attempt is made to store more than a fixed number of bytes.
 *
 * @author   Mark Taylor
 * @since    29 Mar 2011
 */
public class LimitByteStore implements ByteStore {

    private final ByteStore base_;
    private final long limit_;
    private final OutputStream out_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.table.storage" );

    /**
     * Constructor.
     *
     * @param  base  base byte store
     * @param  limit  maximum capacity of this store in bytes
     */
    public LimitByteStore( ByteStore base, long limit ) {
        base_ = base;
        limit_ = limit;
        final OutputStream baseOut = base_.getOutputStream();
        out_ = new FilterOutputStream( baseOut ) {
            private long count_;

            public void write( byte[] buf, int off, int leng )
                    throws IOException {
                preWrite( leng );
                baseOut.write( buf, off, leng );
                count_ += leng;
            }

            public void write( byte[] buf ) throws IOException {
                preWrite( buf.length ); 
                baseOut.write( buf );
                count_ += buf.length;
            }

            public void write( int b ) throws IOException {
                preWrite( 1 );
                baseOut.write( b );
                count_++;
            }

            private void preWrite( int n ) throws IOException {
                if ( count_ + n > limit_ ) {
                    throw new IOException( "Write size limit of " + limit_
                                         + " exceeded" );
                }
            }
        };
    }

    public OutputStream getOutputStream() {
        return out_;
    }

    public long getLength() {
        return base_.getLength();
    }

    public void copy( OutputStream out ) throws IOException {
        base_.copy( out );
    }

    public ByteBuffer[] toByteBuffers() throws IOException {
        return base_.toByteBuffers();
    }

    public void close() {
        try {
            out_.close();
            base_.close();
        }
        catch ( IOException e ) {
            logger_.warning( "close error: " + e );
        }
    }
}
