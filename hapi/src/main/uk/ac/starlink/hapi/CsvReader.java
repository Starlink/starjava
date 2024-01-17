package uk.ac.starlink.hapi;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import uk.ac.starlink.util.ByteList;

/**
 * CSV file reader.
 * This is intended for use with HAPI CSV responses, and hence
 * is supposed to conform to RFC 4180-flavoured CSV files
 * containing UTF-8 characters.
 *
 * @author   Mark Taylor
 * @since    11 Jan 2024
 * @see <a href="https://www.ietf.org/rfc/rfc4180.txt">RFC 4180</a>
 */
public class CsvReader {

    private final ByteList buf_;
    private final List<String> words_;
    private final Set<String> warnings_;
    private Byte byte0_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.hapi" );

    /**
     * Constructor.
     */
    public CsvReader() {
        buf_ = new ByteList();
        words_ = new ArrayList<String>();
        warnings_ = new HashSet<String>();
    }

    /**
     * Provides a single byte to be read before the start of the
     * input stream on the next invocation of {@link #readCsvRow}.
     * Once read, it will not be read again.
     *
     * @param  byte0  prefix byte
     */
    public void setPrefixByte( byte byte0 ) {
        byte0_ = Byte.valueOf( byte0 );
    }

    /**
     * Returns the next row from a given input stream.
     * A prefix byte, if it has been supplied, will be read first.
     * If the end of the stream is encountered, null is returned,
     * but the file is not closed.
     *
     * @param  in  input stream
     * @return  array of fields in the next row,
     *          or null if the end of the stream is reached
     */
    public String[] readCsvRow( InputStream in ) throws IOException {
        buf_.clear();
        words_.clear();
        State state = State.INIT;
        if ( byte0_ != null ) {
            state = processByte( state, byte0_.byteValue() );
            byte0_ = null;
        }
        while ( ! state.isEnd_ ) {
            state = processByte( state, in.read() );
        }
        return state == State.END_FILE ? null
                                       : words_.toArray( new String[ 0 ] );
    }

    /**
     * Schedule a warning message for reporting.
     * Multiple warning messages with the same text will only result
     * in out output message.
     *
     * @param   msg   message
     */
    private void warning( String msg ) {
        if ( warnings_.add( msg ) ) {
            logger_.warning( msg );
        }
    }

    /**
     * Process the next byte in the stream.
     *
     * @param  state  input machine state
     * @param  b   next byte
     * @return   new machine state
     */
    private State processByte( State state, int b ) {
        switch ( state ) {
            case INIT: {
                switch ( b ) {
                    case -1:
                         return State.END_FILE;
                    case '"':
                         return State.READING_QUOTED;
                    case ',':
                         addCurrentWord();
                         return State.INIT;
                    case '\r':
                    case '\n':
                         return State.INIT;
                    default:
                         addByte( b );
                         return State.READING_UNQUOTED;
                }
            }
            case READING_UNQUOTED: {
                switch ( b ) {
                    case -1:
                        addCurrentWord();
                        return State.END_FILE;
                    case ',':
                        addCurrentWord();
                        return State.INIT;
                    case '\r':
                    case '\n':
                        addCurrentWord();
                        return State.END_LINE;
                    case '"':
                        if ( isWhitespace( buf_ ) ) {
                            buf_.clear();
                            return State.READING_QUOTED;
                        }
                        else {
                            addByte( b );
                            return State.READING_QUOTED;
                        }
                    default:
                        addByte( b );
                        return State.READING_UNQUOTED;
                }
            }
            case READING_QUOTED: {
                switch ( b ) {
                    case -1:
                        warning( "EOF during quoted field" );
                        addCurrentWord();
                        return State.END_FILE;
                    case '"':
                        return State.QUOTE_IN_QUOTED;
                    default:
                        addByte( b );
                        return State.READING_QUOTED;
                }
            }
            case QUOTE_IN_QUOTED: {
                switch ( b ) {
                    case -1:
                        warning( "EOF during quoted field" );
                        addCurrentWord();
                        return State.END_FILE;
                    case '"':
                        addByte( (byte) '"' );
                        return State.READING_QUOTED;
                    default:
                        addCurrentWord();
                        return State.END_QUOTED;
                }
            }
            case END_QUOTED: {
                switch ( b ) {
                    case -1:
                        return State.END_FILE;
                    case ',':
                        return State.INIT;
                    case '\r':
                    case '\n':
                        return State.END_LINE;
                    case ' ':
                        warning( "Space following quoted field" );
                        return State.END_QUOTED;
                    default:
                        warning( "Content following quoted field" );
                        return State.END_QUOTED;
                }
            }
            default: {
                assert false;
                warning( "Unexpected state (programming error)" );
                return State.ERROR;
            }
        }
    }

    /**
     * Adds a new byte to the running field buffer.
     *
     * @param  b byte
     */
    private void addByte( int b ) {
        assert b == ( b & 0xff );
        buf_.add( (byte) b );
    }

    /**
     * Adds content of the the current field buffer to the words list,
     * and clears the field buffer.
     */
    private void addCurrentWord() {
        words_.add( buf_.decodeUtf8() );
        buf_.clear();
    }

    /**
     * Indicates whether a given buffer is composed only of whitespace.
     *
     * @param  buf  byte list
     * @return  true iff buf contains only whitespace
     */
    private static boolean isWhitespace( ByteList buf ) {
        int nb = buf.size();
        for ( int i = 0; i < nb; i++ ) {
            if ( i != ' ' ) {
                return false;
            }
        }
        return true;
    }

    /**
     * Enumeration of finite state machine states.
     */
    private enum State {
        INIT( false ),
        READING_UNQUOTED( false ),
        READING_QUOTED( false ),
        QUOTE_IN_QUOTED( false ),
        END_QUOTED( false ),
        END_LINE( true ),
        END_FILE( true ),
        ERROR( true );

        final boolean isEnd_;
        State( boolean isEnd ) {
            isEnd_ = isEnd;
        }
    }
}
