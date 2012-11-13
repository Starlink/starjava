package uk.ac.starlink.topcat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
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
     * @param   debug  true iff additional debugging information should
     *                 be included with logging messages
     */
    public LineFormatter( boolean debug ) {
        debug_ = debug;
    }

    @Override
    public String format( LogRecord record ) {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( record.getLevel().toString() );
        sbuf.append( ": " )
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
        if ( debug_ ) {
            Throwable err = record.getThrown();
            if ( err != null ) {
                sbuf.append( getStackTrace( err ) );
            }
        }
        return sbuf.toString();
    }

    /**
     * Generates a string containing a stack trace for a given throwable.
     *
     * @param   e   error
     * @return  stacktrace
     */
    private static String getStackTrace( Throwable e ) {
        byte[] bbuf;
        try {
            ByteArrayOutputStream bOut = new ByteArrayOutputStream();
            e.printStackTrace( new PrintStream( bOut ) );
            bOut.close();
            bbuf = bOut.toByteArray();
        }
        catch ( IOException ioex ) {
            assert false;
            return "error generating stacktrace";
        }
        StringBuffer sbuf = new StringBuffer( bbuf.length );
        for ( int ic = 0; ic < bbuf.length; ic++ ) {
            char c = (char) bbuf[ ic ];
            sbuf.append( c );
        }
        return sbuf.toString();
    }
}
