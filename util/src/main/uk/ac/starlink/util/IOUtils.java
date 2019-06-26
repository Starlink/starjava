package uk.ac.starlink.util;

import java.io.DataInput;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides static methods which do miscellaneous input/output tasks.
 *
 * @author   Mark Taylor
 */
public class IOUtils {

    private final static Map<List<Object>,String> resourceMap_ =
        new HashMap<List<Object>,String>();
    private final static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.util" );
    private final static byte[] lineSep_ = getLineSeparatorBytes();

    /**
     * Private constructor prevents instantiation.
     */
    private IOUtils() {
    }

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
        boolean hasSkipped = false;
        while ( nskip > 0L ) {

            /* Attempt to skip a chunk. */
            int jskip = (int) Math.min( nskip, (long) Integer.MAX_VALUE );
            int iskip;
            try {

                /* This switch on stream implementation type is a hack;
                 * really I just want to call DataInput.skipBytes here.
                 * The DataInput.skipBytes javadoc explicitly says that
                 * the method never throws EOFException.  However,
                 * nom.tam.util.BufferedDataInputStream sometimes does
                 * throw EOFException.  I am wary about fixing the bug in
                 * nom.tam because I don't know what else it might break.
                 * So work around it here in what should be a harmless way. */
                iskip = strm instanceof InputStream
                      ? (int) ((InputStream) strm).skip( jskip )
                      : strm.skipBytes( jskip );
                hasSkipped = true;
            }

            /* Annoyingly, skipBytes can throw an "Illegal Seek" exception if
             * the underlying stream does not support seek (e.g. stdin on
             * Linux).  This behaviour has not always been documented in
             * the InputStream javadocs (see Sun bug ID 6222822). 
             * If it looks like we've tripped over this here, log a suggested
             * explanation. */
            catch ( IOException e ) {
                if ( ! hasSkipped && ! ( e instanceof EOFException ) ) {
                    logger_.warning( "Input stream does not support seeks??" );
                }
                throw e;
            }

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
        boolean hasSkipped = false;
        while ( nskip1 > 0L ) {

            /* Attempt to skip a chunk. */
            long iskip;
            try {
                iskip = strm.skip( nskip1 );
                hasSkipped = true;
            }

            /* Annoyingly, skip can throw an "Illegal Seek" exception if
             * the underlying stream does not support seek (e.g. stdin on
             * Linux).  This behaviour has not always been documented in
             * the InputStream javadocs (see Sun bug ID 6222822). 
             * If it looks like we've tripped over this here, log a suggested
             * explanation. */
            catch ( IOException e ) {
                if ( ! hasSkipped && ! ( e instanceof EOFException ) ) {
                    logger_.warning( "Input stream does not support seeks??" );
                }
                throw e;
            }

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

    /**
     * Reads a static resource and returns the contents as a string.
     * The resource is read using <code>clazz.getResourceAsStream(name)</code>
     * and is assumed to have ASCII content.  The result is cached so that
     * subsequent calls will return the same value.
     * If it can't be read, "?" is returned, and a message is written
     * through the logging system at the requested level.
     * This is intended for short files such as version strings.
     *
     * @param  clazz  class defining relative location of resource
     * @param  name   resource name relative to <code>clazz</code>
     * @param  level  logging level for failure; if null a default value
     *                is used (currently WARNING)
     * @return resource content string
     * @see    java.lang.Class#getResourceAsStream
     */
    public static String getResourceContents( Class<?> clazz, String name,
                                              Level level ) {
        List<Object> key = Arrays.asList( new Object[] { clazz, name } );
        if ( ! resourceMap_.containsKey( key ) ) {
            String value = null;
            InputStream in = null;
            try {
                in = clazz.getResourceAsStream( name );
                if ( in != null ) {
                    StringBuffer sbuf = new StringBuffer();
                    for ( int b; ( b = in.read() ) > 0; ) {
                        sbuf.append( (char) b );
                    }
                    value = sbuf.toString().trim();
                }
            }
            catch ( IOException e ) {
            }
            finally {
                if ( in != null ) {
                    try {
                        in.close();
                    }
                    catch ( IOException e ) {
                    }
                }
            }
            if ( value == null ) {
                logger_.log( level == null ? Level.WARNING : level,
                             "Couldn't read requested resource " + name );
                value = "?";
            }
            resourceMap_.put( key, value );
        }
        return resourceMap_.get( key );   
    }

    /**
     * Writes a string to an output stream followed by a new line.
     * Unlike {@link java.io.PrintStream#println}, an IOException may
     * be thrown.
     *
     * @param  out  destination stream
     * @param  line  line to write
     */
    public static void println( OutputStream out, String line )
            throws IOException {
        out.write( line.getBytes() );
        out.write( lineSep_ );
    }

    /**
     * Returns the platform's line separator as a byte array given the
     * platform's default encoding.  May or may not be equal to {'\n'}.
     *
     * @return  line separator byte sequence
     */
    public static byte[] getLineSeparatorBytes() {
        try {
            return System.getProperty( "line.separator", "\n" ).getBytes();
        }
        catch ( SecurityException e ) {
            return "\n".getBytes();
        }
    }

    /**
     * Copies all the bytes from a given input stream to a given output stream.
     * Neither stream is closed following the copy.
     *
     * @param  in  source
     * @param  out destination
     */
    public static void copy( InputStream in, OutputStream out )
            throws IOException {
        int bufsiz = 8096; 
        byte[] buf = new byte[ bufsiz ];
        for ( int n; ( n = in.read( buf ) ) > 0; ) {
            out.write( buf, 0, n );
        }
    }

    /** 
     * Reads a number of bytes from a stream.  The specified number of
     * bytes or the whole of the file is read, whichever is shorter.
     *  
     * @param   in  input stream
     * @param   maxLeng  maximum number of bytes to read
     * @return   buffer of bytes containing <tt>maxLeng</tt> bytes
     *           read from <tt>in</tt>, or fewer if the stream ended early
     */     
    public static byte[] readBytes( InputStream in, int maxLeng )
            throws IOException {
        byte[] buf = new byte[ maxLeng ];
        int pos = 0;
        while ( maxLeng - pos > 0 ) {
            int ngot = in.read( buf, pos, maxLeng - pos );
            if ( ngot > 0 ) {
                pos += ngot; 
            }
            else { 
                break;
            }   
        }
        if ( pos < maxLeng ) { 
            byte[] buf2 = new byte[ pos ];
            System.arraycopy( buf, 0, buf2, 0, pos );
            buf = buf2;
        }
        return buf;
    }
}
