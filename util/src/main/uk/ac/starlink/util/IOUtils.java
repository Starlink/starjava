package uk.ac.starlink.util;

import java.io.DataInput;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Provides static methods which do miscellaneous input/output tasks.
 *
 * @author   Mark Taylor
 */
public class IOUtils {

    /**
     * Skips over a number of bytes in a <tt>DataInput</tt>.
     * This is implemented using {@link java.io.DataInput#skipBytes} 
     * but differs from it in that it guarantees to skip the bytes 
     * as specified, or to throw an exception.
     *
     * @param  strm  the stream to skip through
     * @param  nskip  the number of bytes to skip
     * @throws  EOFException  if the end of file is reached
     * @throws  IOException  if an I/O error occurs
     * @throws  IllegalArgumentException  if <tt>nskip&lt;0</tt>
     */
    public static void skipBytes( DataInput strm, long nskip ) 
            throws IOException {
        if ( nskip < 0L ) {
            throw new IllegalArgumentException( "Can't skip backwards" );
        }

        /* Try to skip. */ 
        while ( nskip > 0L ) {

            /* Attempt to skip a chunk. */
            int iskip = 
                strm.skipBytes( (int) Math.min( nskip, 
                                                (long) Integer.MAX_VALUE ) );

            /* If no bytes were skipped, attempt to read a byte.  This will
             * either advance 1, or throw an EOFException. */
            if ( iskip == 0 ) {
                strm.readByte();
                iskip = 1;
            }

            /* Decrease remaining bytes and continue. */
            nskip -= iskip;
        }
    }

    /**
     * Skips over a number of bytes in an <tt>InputStream</tt>
     * This is implemented using {@link java.io.InputStream#skip}
     * but differs from it in that it guarantees to skip the bytes
     * as specified, or to throw an exception.
     *
     * @param  strm  the stream to skip through
     * @param  nskip  the number of bytes to skip
     * @throws  EOFException  if the end of file is reached
     * @throws  IOException  if an I/O error occurs
     * @throws  IllegalArgumentException  if <tt>nskip&lt;0</tt>
     */
    public static void skip( InputStream strm, long nskip )
           throws IOException {
        if ( nskip < 0L ) {
            throw new IllegalArgumentException( "Can't skip backwards" );
        }
        if ( nskip == 0L ) {
            return;
        }

        /* Try to skip up the the last byte. */
        long nskip1 = nskip - 1;
        while ( nskip1 > 0L ) {

            /* Attempt to skip a chunk. */
            long iskip = strm.skip( nskip1 );

            /* If no bytes were skipped, attempt to read a byte. */
            if ( iskip == 0 ) {
                if ( strm.read() == -1 ) {
                    throw new EOFException();
                }
                iskip = 1;
            }

            /* Decrease remaining bytes and continue. */
            nskip1 -= iskip;
        }

        /* Read the last skipped byte, and throw an exception if it's
         * at/after the end of the stream. */
        if ( strm.read() == -1 ) {
            throw new EOFException();
        }
    }
}
