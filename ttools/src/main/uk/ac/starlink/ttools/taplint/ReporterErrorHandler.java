package uk.ac.starlink.ttools.taplint;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

/**
 * SAX ErrorHandler implementation based on a Reporter.
 *
 * @author   Mark Taylor
 * @since    3 Jun 2011
 */
public class ReporterErrorHandler implements ErrorHandler {

    private final Reporter reporter_;
    private int warningCount_;
    private int errorCount_;
    private int fatalCount_;

    /**
     * Constructor.
     *
     * @param   reporter   reporter object which handles delivering messages
     */
    public ReporterErrorHandler( Reporter reporter ) {
        reporter_ = reporter;
    }

    /**
     * Returns the number of fatal SAX errors encountered.
     *
     * @return   fatal error count
     */
    public int getFatalCount() {
        return fatalCount_;
    }

    /**
     * Returns a short summary of the errors encountered during the parse.
     *
     * @return   summary
     */
    public String getSummary() {
        return new StringBuffer()
            .append( "warnings: " )
            .append( warningCount_ )
            .append( ", " )
            .append( "errors: " )
            .append( errorCount_ )
            .append( ", " )
            .append( "fatal: " )
            .append( fatalCount_ )
            .toString();
    }

    public void warning( SAXParseException err ) {
        warningCount_++;
        report( ReportType.WARNING, err );
    }

    public void error( SAXParseException err ) {
        errorCount_++;
        report( ReportType.ERROR, err );
    }

    public void fatalError( SAXParseException err ) {
        fatalCount_++;
        report( ReportType.ERROR, err );
    }

    /**
     * Logs a SAXParseException through the reporter.
     *
     * @param   type   message type
     * @param   err    SAX error
     */
    private void report( ReportType type, SAXParseException err ) {
        String msg = err.getMessage();
        if ( msg == null ) {
            msg = err.toString();
        }
        String code = reporter_.createCode( msg );
        StringBuffer sbuf = new StringBuffer();
        int il = err.getLineNumber();
        int ic = err.getColumnNumber();
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
        reporter_.report( type, code, sbuf.toString() );
    }
}
