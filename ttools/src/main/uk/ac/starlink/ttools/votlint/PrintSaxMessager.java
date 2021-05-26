package uk.ac.starlink.ttools.votlint;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import org.xml.sax.Locator;

/**
 * SaxMessager implementation that writes messages to a given print stream.
 * An effort is made not to output the same message millions of times.
 * The maximum number of times the same message will be output is
 * controlled by the <code>maxRepeat</code> parameter.
 *
 * @author   Mark Taylor
 * @since    29 Nov 2017
 */
public class PrintSaxMessager implements SaxMessager {

    private final PrintStream out_;
    private final boolean debug_;
    private final int maxRepeat_;
    private final Map<VotLintCode,Integer> msgMap_;

    /**
     * Constructor.
     *
     * @param  out    output stream to which messages will be written
     * @param  debug  if true, a stack trace will be output with each
     *                log message
     * @param  maxRepeat  maximum number of identical error messages
     *                    which will be logged
     */
    public PrintSaxMessager( PrintStream out, boolean debug, int maxRepeat ) {
        out_ = out;
        debug_ = debug;
        maxRepeat_ = maxRepeat;
        msgMap_ = new HashMap<VotLintCode,Integer>();
    }

    public void reportMessage( Level level, VotLintCode code, String msg,
                               Locator locator ) {

        /* See how many times (if any) we have output this same message
         * before now.  If it's more than a certain threshold, don't
         * bother to do it again. */
        int repeat = msgMap_.containsKey( code ) 
                   ? msgMap_.get( code ).intValue()
                   : 0;
        msgMap_.put( code, Integer.valueOf( repeat + 1 ) );
        if ( repeat < maxRepeat_ ) {

            /* Construct the text to output. */
            StringBuffer sbuf = new StringBuffer()
                .append( level );
            int il = -1;
            int ic = -1;
            if ( locator != null ) {
                ic = locator.getColumnNumber();
                il = locator.getLineNumber();
            }
            if ( il > 0 ) { 
                sbuf.append( " (l." )
                    .append( il );
                if ( ic > 0 ) {
                    sbuf.append( ", c." )
                        .append( ic );
                }
                sbuf.append( ")" );
            }
            sbuf.append( ": " )
                .append( msg );
            String text = sbuf.toString();

            /* Output the message. */
            out_.println( text );
            if ( debug_ ) {
                VotLintException err = new VotLintException( msg );
                err.fillInStackTrace();
                err.printStackTrace( out_ );
                out_.println();
            }
        }

        /* The first time we reach the repeat limit, make sure it's
         * reported. */
        else if ( repeat == maxRepeat_ ) {
            String txt = new StringBuffer()
               .append( Level.INFO )
               .append( " (maxrepeat=" )
               .append( maxRepeat_ )
               .append( ") suppressing more messages like \"" )
               .append( msg )
               .append( "\"" )
               .toString();
            out_.println( txt );
        }
    }
}
