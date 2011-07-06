package uk.ac.starlink.ttools.taplint;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.HashSet;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import uk.ac.starlink.util.CountMap;

/**
 * Handles logging for validation messages.
 * The design (a single-level hierarchy of reporting stages) is not
 * particularly elegant or general, and may be revised at some point,
 * but is serves the current purposes of the TapLint tool.
 *
 * @author   Mark Taylor
 */
public class Reporter {

    private final PrintStream out_;
    private final Collection<ReportType> typeList_;
    private final int maxRepeat_;
    private final boolean debug_;
    private final int maxChar_;
    private final CountMap<String> codeMap_;
    private final CountMap<ReportType> typeMap_;
    private final NumberFormat countFormat_;
    private final String countXx_;
    private String scode_;
    private static final int MCODE_LENGTH = 4;  // message code length

    /**
     * Constructor.
     *
     * @param  out  destination stream
     * @param  types  message types to report; others are discarded
     * @param  maxRepeat  maximum number of times any given message
     *                    may be repeated; subsequent instances are suppressed
     * @param  debug  true iff you want to see full stacktraces for
     *                exceptions etc
     * @param  maxChar  maximum number of total characters per line of output
     */
    public Reporter( PrintStream out, ReportType[] types, int maxRepeat,
                     boolean debug, int maxChar ) {
        out_ = out;
        typeList_ = new HashSet( Arrays.asList( types ) );
        maxRepeat_ = maxRepeat;
        debug_ = debug;
        maxChar_ = maxChar;
        int maxDigit = (int) Math.ceil( Math.log10( maxRepeat_ + 1 ) );
        countFormat_ = new DecimalFormat( repeatChar( '0', maxDigit ) );
        countXx_ = repeatChar( 'x', maxDigit );
        codeMap_ = new CountMap<String>();
        typeMap_ = new CountMap<ReportType>();
    }

    /**
     * Signals beginning of reporting.
     */
    public void start() {
    }

    /**
     * Signals end of reporting.
     */
    public void end() {
        println();
        reportTotals();
        println();
    }

    /**
     * Begins a reporting section.
     *
     * @param   scode   short fixed-length (3-char?) identifier for the
     *                  section about to start
     * @param   message  terse (one-line) free-text description of the stage
     */
    public void startSection( String scode, String message ) {
        println();
        println( "Section " + scode + ": " + message );
        scode_ = scode;
    }

    /**
     * Returns the section code for the most recently-started section.
     *
     * @return  current section code
     */
    public String getSectionCode() {
        return scode_;
    }

    /**
     * Writes to the output stream a summary of messages which were
     * suppressed in a given stage because the maximum repeat count
     * was exceeded.
     *
     * @param   scode  section code to summarise
     */
    public void summariseUnreportedMessages( String scode ) {
        String dscode = "-" + scode + "-";
        int lds = dscode.length();
        for ( Iterator<String> it = codeMap_.keySet().iterator();
              it.hasNext(); ) {
            String ecode = it.next();
            if ( dscode.equals( ecode.substring( 1, 1 + lds ) ) ) {
                int count = codeMap_.getCount( ecode );
                if ( count > maxRepeat_ ) {
                    println( ecode + "-" + countXx_
                                + " (" + ( count - maxRepeat_ ) + " more)" );
                }
                it.remove();
            }
        }
    }

    /**
     * Ends the current section.
     */
    public void endSection() {
        scode_ = null;
    }

    /**
     * Reports a message.
     *
     * @param    type   message type
     * @param    code   short, fixed-length (4-char?) message code which ought
     *                  to identify which messages are essentially the same
     *                  as each other (though may refer to different data etc)
     * @param    message  free-text message; it may be multi-line and/or
     *                    longish, but may in practice be truncated on output
     */
    public void report( ReportType type, String code, String message ) {
        report( type, code, message, null );
    }

    /**
     * Reports a message with an associated throwable.
     *
     * @param    type   message type
     * @param    code   short, fixed-length message code which ought
     *                  to identify which messages are essentially the same
     *                  as each other (though may refer to different data etc);
     *                  preferably {@link #MCODE_LENGTH}-characters long
     * @param    message  free-text message; it may be multi-line and/or
     *                    longish, but may in practice be truncated on output
     * @param    err    throwable
     */
    public void report( ReportType type, String code, String message,
                        Throwable err ) {
        if ( ! typeList_.contains( type ) ) {
            return;
        }
        if ( message == null || message.trim().length() == 0 ) {
            message = "?";
        }
        if ( code == null || code.length() == 0 ) {
            code = createCode( message );
        }
        if ( code.length() > MCODE_LENGTH ) {
            code = code.substring( 0, MCODE_LENGTH );
        }
        if ( code.length() < MCODE_LENGTH ) {
            code += repeatChar( 'X', MCODE_LENGTH - code.length() );
        }
        assert code.length() == MCODE_LENGTH;
        StringBuffer codeBuf = new StringBuffer();
        codeBuf.append( type.getChar() )
               .append( '-' );
        if ( scode_ != null ) {
            codeBuf.append( scode_ )
                   .append( '-' );
        }
        codeBuf.append( code );
        String ecode = codeBuf.toString();
        typeMap_.addItem( type );
        int count = codeMap_.addItem( ecode );
        if ( count <= maxRepeat_ ) {
            String fcount = countFormat_.format( count );
            StringBuffer mbuf = new StringBuffer( message.trim() );
            if ( err != null ) {
                mbuf.append( " [" )
                    .append( stringifyError( err, false ) )
                    .append( "]" );
            }
            String[] lines = mbuf.toString().split( "[\\n\\r]+" );
            for ( int il = 0; il < lines.length; il++ ) {
                String line = lines[ il ];
                if ( line.trim().length() > 0 ) {
                    StringBuffer sbuf = new StringBuffer();
                    sbuf.append( ecode )
                        .append( il == 0 ? '-' : '+' )
                        .append( fcount )
                        .append( ' ' )
                        .append( lines[ il ] );
                    println( sbuf.toString() );
                }
            }
        }
        if ( debug_ && err != null ) {
            err.printStackTrace( out_ );
        }
    }

    /**
     * Provides a summary of any message types which were not reported.
     *
     * @param  code  message code for this summary
     * @param  types  array of types to report on, or null for all types
     */
    public void summariseUnreportedTypes( String code, ReportType[] types ) {
        if ( types == null ) {
            types = typeMap_.keySet().toArray( new ReportType[ 0 ] );
        }
        StringBuffer sbuf = new StringBuffer();
        for ( int it = 0; it < types.length; it++ ) {
            ReportType type = types[ it ];
            if ( it > 0 ) {
                sbuf.append( ", " );
            }
            sbuf.append( type.toString() )
                .append( ": " )
                .append( typeMap_.getCount( type ) );
        }
        report( ReportType.SUMMARY, code, sbuf.toString() );
    }

    /**
     * Clears any memory of which messages have been reported.
     */
    public void clear() {
        codeMap_.clear();
    }

    /**
     * Uses some hash function to generate a message code from text.
     * Probably unique, but not guaranteed to be.
     *
     * @param  msg  message text
     * @return   suitable message code
     */
    public String createCode( String msg ) {
        int hash = msg.hashCode();
        char[] chrs = new char[ MCODE_LENGTH ];
        for ( int i = 0; i < MCODE_LENGTH; i++ ) {
            chrs[ i ] = (char) ( 'A' + ( ( hash & 0x1f ) % ( 'Z' - 'A' ) ) );
            hash >>= 5;
        }
        return new String( chrs );
    }

    /**
     * Prints the total number of each report type reported by this object.
     */
    public void reportTotals() {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( "Totals: " );
        ReportType[] types = typeList_.toArray( new ReportType[ 0 ] );
        Arrays.sort( types );
        for ( int i = 0; i < types.length; i++ ) {
            ReportType type = types[ i ];
            if ( i > 0 ) {
                sbuf.append( "; " );
            }
            sbuf.append( type.getNames() )
                .append( ": " )
                .append( typeMap_.getCount( type ) );
        }
        println( sbuf.toString() );
    }

    /**
     * Generates a short string describing a throwable.
     *
     * @param  err  throwable
     * @param  showCauses  true for recursive listing of cause throwables
     * @return  summary string
     */
    private String stringifyError( Throwable err, boolean showCauses ) {
        StringBuilder sbuf = new StringBuilder();
        String emsg = err.getMessage();
        if ( emsg != null && emsg.trim().length() > 0 ) {
            sbuf.append( emsg );
        }
        else {
            sbuf.append( err.toString() );
        }
        if ( showCauses ) {
            Throwable err2 = err.getCause();
            if ( err2 != null ) {
                String emsg2 = err2.getMessage();
                if ( emsg2 == null || ! emsg2.equals( emsg ) ) {
                    sbuf.append( "; " )
                        .append( stringifyError( err2, true ) );
                }
            }
        }
        return sbuf.toString();
    }

    /**
     * Writes a blank line.
     */
    private void println() {
        println( "" );
    }

    /**
     * Writes a single line of text, truncating as required.
     *
     * @param   line  line to print
     */
    private void println( String line ) {
        int leng = line.length();
        if ( leng > maxChar_ ) {
            String ellipsis = "...";
            line = line.substring( 0, maxChar_ - ellipsis.length() )
                 + ellipsis;
        }
        out_.println( line );
    }

    /**
     * Generates a string which is a repeated sequence of the same character.
     *
     * @param   chr  character to repeat
     * @param  count  repeat count
     * @return  <code>count</code>-character string
     */
    private static String repeatChar( char chr, int count ) {
        char[] chrs = new char[ count ];
        for ( int i = 0; i < count; i++ ) {
            chrs[ i ] = chr;
        }
        return new String( chrs );
    }
}
