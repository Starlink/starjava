package uk.ac.starlink.ttools.taplint;

import org.xml.sax.Locator;
import uk.ac.starlink.ttools.votlint.VotLintContext;

/**
 * VotLintContext implementation which delivers its output via
 * a TapLint-style reporter.
 *
 * @author   Mark Taylor
 * @since    10 Jun 2011
 */
public class ReporterVotLintContext extends VotLintContext {

    private final Reporter reporter_;

    /**
     * Constructor.
     *
     * @param  reporter   validation message destination
     */
    public ReporterVotLintContext( Reporter reporter ) {
        reporter_ = reporter;
    }

    @Override
    public void info( String msg ) {
        report( ReportType.INFO, msg );
    }

    @Override
    public void warning( String msg ) {
        report( ReportType.WARNING, msg );
    }

    @Override
    public void error( String msg ) {
        report( ReportType.ERROR, msg );
    }

    /**
     * Delivers validation messages to the reporter.
     *
     * @param  type  message type
     * @param  msg   message text
     */
    private void report( ReportType type, String msg ) {
        String code = "VO"
                    + reporter_.createCode( type + ": " + msg )
                     .substring( 0, 2 );
        int il = -1;
        int ic = -1;
        Locator locator = getLocator();
        if ( locator != null ) {
            ic = locator.getColumnNumber();
            il = locator.getLineNumber();
        }
        StringBuffer sbuf = new StringBuffer();
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
