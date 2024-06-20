package uk.ac.starlink.ttools.taplint;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import uk.ac.starlink.util.CountMap;

/**
 * OutputReporter implementation for exporting JSON structured output.
 *
 * @author   Mark Taylor
 * @since    23 Oct 2016
 */
public class JsonOutputReporter implements OutputReporter {

    private final PrintStream out_;
    private final Collection<ReportType> typeList_;
    private final int maxRepeat_;
    private final boolean debug_;
    private final int maxChar_;
    private final JsonWriter jsoner_;
    private final CountMap<SecCode> codeMap_;
    private final CountMap<ReportType> rtypeMap_;
    private int isect_;
    private int irep_;
    private String scode_;

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
    public JsonOutputReporter( PrintStream out, ReportType[] types,
                               int maxRepeat, boolean debug, int maxChar ) {
        out_ = out;
        typeList_ = new HashSet<ReportType>( Arrays.asList( types ) );
        maxRepeat_ = maxRepeat;
        debug_ = debug;
        maxChar_ = maxChar;
        codeMap_ = new CountMap<SecCode>();
        rtypeMap_ = new CountMap<ReportType>();
        jsoner_ = new JsonWriter( 2, true );
    }

    public void start( String[] announcements ) {
        StringBuffer sbuf = new StringBuffer( "{" );
        sbuf.append( jsoner_
                    .jsonPair( "announce", Arrays.asList( announcements ),
                               1, false ) )
            .append( "," )
            .append( jsoner_.getIndent( 1 ) )
            .append( "\"sections\": [" );
        out_.print( sbuf.toString() );
    }

    public void end() {
        out_.print( jsoner_.getIndent( 1 ) + "]," ); // end sections
        Map<String,Object> tmap = new TreeMap<String,Object>();
        int i = 0;
        for ( ReportType rtype : typeList_ ) {
            tmap.put( rtype.toString(),
                      Integer.valueOf( rtypeMap_.getCount( rtype ) ) );
        }
        out_.print( jsoner_.jsonPair( "totals", tmap, 1, false ) );
        out_.println( "\n}" );
    }

    public void startSection( String scode, String message ) {
        scode_ = scode;
        irep_ = 0;
        StringBuffer sbuf = new StringBuffer();
        if ( isect_++ > 0 ) {
            sbuf.append( "," );
        }
        sbuf.append( jsoner_.getIndent( 2 ) )
            .append( "{" )
            .append( jsoner_.jsonPair( "code", scode, 3, false ) )
            .append( "," )
            .append( jsoner_.jsonPair( "title", message, 3, false ) )
            .append( "," )
            .append( jsoner_.getIndent( 3 ) )
            .append( "\"reports\": [" );
        out_.print( sbuf.toString() );
    }

    public void endSection() {
        scode_ = null;
        out_.print( jsoner_.getIndent( 3 ) + "]" );
        out_.print( jsoner_.getIndent( 2 ) + "}" );
    }

    public String getSectionCode() {
        return scode_;
    }

    public void report( ReportCode code, String message ) {
        report( code, message, null );
    }

    public void report( ReportCode code, String message, Throwable err ) {
        ReportType rtype = code.getType();
        if ( ! typeList_.contains( rtype ) ) {
            return;
        }
        rtypeMap_.addItem( rtype );
        String key = rtype.getChar() + "-" + scode_ + "-" + code.getLabel();
        int count = codeMap_.addItem( new SecCode( scode_, code ) );
        if ( count <= maxRepeat_ ) {
            Map<String,Object> rmap = new LinkedHashMap<String,Object>();
            rmap.put( "level", rtype.toString() );
            rmap.put( "code", code.getLabel() );
            rmap.put( "text", message );
            rmap.put( "iseq", Integer.valueOf( count ) );
            if ( err != null ) {
                Map<String,String> emap = new LinkedHashMap<String,String>();
                emap.put( "class", err.getClass().getName() );
                emap.put( "errmsg", err.getMessage() );
                if ( debug_ ) {
                    StringWriter bufWriter = new StringWriter();
                    PrintWriter pWriter = new PrintWriter( bufWriter );
                    err.printStackTrace( pWriter );
                    pWriter.flush();
                    emap.put( "stacktrace", bufWriter.getBuffer().toString() );
                }
                rmap.put( "error", emap );
            }
            StringBuffer sbuf = new StringBuffer();
            if ( irep_++ > 0 ) {
                sbuf.append( "," );
            }
            jsoner_.toJson( sbuf, rmap, 4, false );
            out_.print( sbuf.toString() );
        }
    }

    public void summariseUnreportedMessages( String scode ) {
        for ( Iterator<SecCode> it = codeMap_.keySet().iterator();
              it.hasNext(); ) {
            SecCode sc = it.next();
            if ( ( sc.section_ == null && scode == null ) ||
                 sc.section_.equals( scode ) ) {
                int count = codeMap_.getCount( sc );
                if ( count > maxRepeat_ ) {
                    ReportCode code = sc.code_;
                    ReportType rtype = code.getType();
                    Map<String,Object> rmap =
                        new LinkedHashMap<String,Object>();
                    rmap.put( "level", rtype.toString() );
                    rmap.put( "code", code.getLabel() );
                    rmap.put( "more", Integer.valueOf( count - maxRepeat_ ) );
                    StringBuffer sbuf = new StringBuffer();
                    if ( irep_++ > 0 ) {
                        sbuf.append( "," );
                    }
                    jsoner_.toJson( sbuf, rmap, 4, false );
                    out_.print( sbuf.toString() );
                }
                it.remove();
            }
        }
    }

    /**
     * Aggregates a section ID and a report code.
     */
    private static class SecCode {
        final String section_;
        final ReportCode code_;
        final String txt_;

        /**
         * Constructor.
         *
         * @param  section  section name
         * @param  code   report code
         */
        SecCode( String section, ReportCode code ) {
            section_ = section;
            code_ = code;
            txt_ = new StringBuffer()
                  .append( code.getType().getChar() )
                  .append( '-' )
                  .append( section )
                  .append( '-' )
                  .append( code.getLabel() )
                  .toString();
        }

        @Override
        public int hashCode() {
            return txt_.hashCode();
        }

        @Override
        public boolean equals( Object o ) {
            return o instanceof SecCode
                && ((SecCode) o).txt_.equals( this.txt_ );
        }

        @Override
        public String toString() {
            return txt_;
        }
    }
}
