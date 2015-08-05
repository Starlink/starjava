package uk.ac.starlink.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Filter input stream that counts the number of bytes read.
 *
 * @author   Mark Taylor
 * @since    9 May 2014
 */
public class CountInputStream extends FilterInputStream {

    private long nRead_;
    private long nSkip_;

    /**
     * Constructor.
     *
     * @param  in  base input stream
     */
    public CountInputStream( InputStream in ) {
        super( in );
    }

    @Override
    public int read() throws IOException {
        int value = in.read();
        if ( value >= 0 ) {
            nRead_++;
        }
        return value;
    }

    @Override
    public int read( byte[] b ) throws IOException {
        int c = in.read( b );
        if ( c >= 0 ) {
            nRead_ += c;
        }
        return c;
    }

    @Override
    public int read( byte[] b, int off, int len ) throws IOException {
        int c = in.read( b, off, len );
        if ( c >= 0 ) {
            nRead_ += c;
        }
        return c;
    }

    @Override 
    public long skip( long n ) throws IOException {
        long c = in.skip( n );
        nSkip_ += c;
        return c;
    }

    /**
     * Returns the number of bytes successfully read so far from this stream.
     *
     * @return  number of bytes read
     */
    public long getReadCount() {
        return nRead_;
    }

    /**
     * Returns the number of bytes skipped from this stream.
     *
     * @return  number of bytes skipped
     */
    public long getSkipCount() {
        return nSkip_;
    }
}
