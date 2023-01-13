package uk.ac.starlink.ecsv;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

/**
 * Reads lines of text from an input stream.
 *
 * @author   Mark Taylor
 * @since    28 Apr 2020
 */
public abstract class LineReader implements Closeable {

    private final InputStream in_;
    private static final int BUFSIZ = 1024 * 16;

    /**
     * Constructor.
     *
     * @param   in  underlying input stream
     */
    protected LineReader( InputStream in ) {
        in_ = in;
    }

    /**
     * Returns the next non-empty line of text from the input stream.
     * The line will not consist of only whitespace.
     *
     * @return   non-blank line, or null if the input is at an end
     */
    public abstract String readLine() throws IOException;

    public void close() throws IOException {
        in_.close();
    }
    
    /**
     * Returns a LineReader instance that just uses the lower 7 bits
     * of each input byte for character values.
     *
     * @param  in  input stream
     * @return   line reader
     */
    public static LineReader createAsciiLineReader( InputStream in ) {
        return new AsciiLineReader( in );
    }

    /**
     * Returns a LineReader instance that reads lines from an array.
     *
     * @param  lines   line array
     * @return   line reader
     */
    public static LineReader createArrayLineReader( final String[] lines ) {
        return new LineReader( null ) {
            int irow_;
            public String readLine() {
                while ( irow_ < lines.length ) {
                    String line = lines[ irow_ ];
                    if ( line != null && line.trim().length() > 0 ) {
                        return line;
                    }
                    irow_++;
                }
                return null;
            }
            @Override
            public void close() {
            }
        };
    }

    /**
     * LineReader implementation that just uses the lowest 7 bits for ASCII.
     */
    private static class AsciiLineReader extends LineReader {

        private final InputStream in_;
        private final byte[] bbuf_;
        private final StringBuilder sbuf_;
        private int nb_ = 0;
        private int ipos_ = 0;

        /**
         * Constructor.
         *
         * @param  in  input stream
         */
        public AsciiLineReader( InputStream in ) {
            super( in );
            in_ = in;
            bbuf_ = new byte[ BUFSIZ ];
            sbuf_ = new StringBuilder();
        }

        public String readLine() throws IOException {
            sbuf_.setLength( 0 );
            boolean hasContent = false;
            while ( true ) {
                if ( ipos_ >= nb_ ) {
                    nb_ = in_.read( bbuf_ );
                    if ( nb_ <= 0 ) {
                        assert nb_ < 0;
                        return null;
                    }
                    ipos_ = 0;
                }
                char chr = (char) ( bbuf_[ ipos_ ] & 0xff );
                switch ( chr ) {
                    case '\r':
                        break;
                    case '\n':
                        if ( hasContent ) {
                            return sbuf_.toString();
                        }
                        else {
                            sbuf_.setLength( 0 );
                        }
                        break;
                    case (byte) '\t':
                    case (byte) ' ':
                        sbuf_.append( chr );
                        break;
                    default:
                        hasContent = true;
                        sbuf_.append( chr );
                }
                ipos_++;
            }
        };
    }
}
