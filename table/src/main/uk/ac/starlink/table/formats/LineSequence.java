package uk.ac.starlink.table.formats;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class which can read lines from an ASCII input stream.
 * You can also push lines back if you want to unread them.
 *
 * @author   Mark Taylor
 * @since    7 Feb 2006
 */
class LineSequence {

    private final InputStream in_;
    private final StringBuffer sbuf_;
    private final List<String> readyLines_;

    /**
     * Constructs a new LineSequence.
     *
     * @param   in   input stream providing the data
     */
    public LineSequence( InputStream in ) {
        in_ = in;
        sbuf_ = new StringBuffer();
        readyLines_ = new ArrayList<String>();
    }

    /**
     * Returns the next line to read from the stream.
     * If the stream is empty, null is returned.
     * The line will not contain a terminating newline.
     * Lines terminated by a "\n" or "\r\n" are recognised.
     * Characters are assumed 8-bit.
     * The line which is returned is either a newly-read one, or one which
     * has previously been read and then pushed back using
     * {@link #replaceLine}.
     *
     * @return   next unread line, or null for end of stream
     */
    public String nextLine() throws IOException {
        if ( readyLines_.size() > 0 ) {
            return readyLines_.remove( 0 );
        }
        else {
            sbuf_.setLength( 0 );
            for ( int v; ( v = in_.read() ) >= 0; ) {
                char c = (char) v;
                if ( c == '\n' ) {
                    return trimCr( sbuf_ );
                }
                else {
                    sbuf_.append( c );
                }
            }
            if ( sbuf_.length() > 0 ) {
                return trimCr( sbuf_ );
            }
            else {
                return null;
            }
        }
    }

    /**
     * Unreads a line.  The submitted string <code>line</code> is replaced
     * in the input so that a subsequent invocation of {@link #nextLine}
     * will retrieve it before doing a new read from the stream.
     * <code>line</code> doesn't actually have to be a line which was 
     * previously read, though that's generally what this method is intended
     * for.
     *
     * @param   line    line to push back
     */
    public void replaceLine( String line ) {
        readyLines_.add( line );
    }

    /**
     * Returns the string content of a character sequence, with any trailing
     * CR ("<code>\r</code>") character removed.
     * This can be used on input lines so that "\r\n" line endings
     * are treated the same as "\n" line endings.
     *
     * @param  sbuf  character sequence that may or may not end
     *               with a single CR
     * @return  string value that does not include any single trailing CR
     *          from the input
     * @see   Goldfarb's First Law of Text Processing
     */
    private String trimCr( CharSequence line ) {
        int leng = line.length();
        int iEnd = leng > 0 && line.charAt( leng - 1 ) == '\r'
                 ? leng - 1
                 : leng;
        return line.subSequence( 0, iEnd ).toString();
    }
}
