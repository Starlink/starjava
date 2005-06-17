package uk.ac.starlink.topcat;

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
    public String format( LogRecord record ) {
        return new StringBuffer()
           .append( record.getLevel().toString() )
           .append( ": " )
           .append( formatMessage( record ) )
           .append( ' ' )
           .append( '(' )
           .append( record.getSourceClassName() )
           .append( '.' )
           .append( record.getSourceMethodName() )
           .append( ')' )
           .append( '\n' )
           .toString();
    }
}
