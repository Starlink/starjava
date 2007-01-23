package uk.ac.starlink.task;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Compact log record formatter.  Unlike the default 
 * {@link java.util.logging.SimpleFormatter} this generally uses only 
 * a single line for each record.
 *
 * @author   Mark Taylor (Starlink)
 * @since    16 June 2005
 */
public class LineFormatter extends Formatter {

    private final boolean debug_;

    /**
     * Constructor. 
     *
     * @param   debug  iff true, provides more information per log message
     */
    public LineFormatter( boolean debug ) {
        debug_ = debug;
    }

    public String format( LogRecord record ) {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( record.getLevel().toString() )
            .append( ": " )
            .append( formatMessage( record ) );
        if ( debug_ ) {
            sbuf.append( ' ' )
                .append( '(' )
                .append( record.getSourceClassName() )
                .append( '.' )
                .append( record.getSourceMethodName() )
                .append( ')' );
        }
        sbuf.append( '\n' );
        return sbuf.toString();
    }
}
