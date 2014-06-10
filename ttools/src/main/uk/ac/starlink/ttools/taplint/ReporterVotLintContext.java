package uk.ac.starlink.ttools.taplint;

import org.xml.sax.Locator;
import uk.ac.starlink.ttools.votlint.VotLintContext;
import uk.ac.starlink.votable.VOTableVersion;

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
    public ReporterVotLintContext( VOTableVersion version, Reporter reporter ) {
        super( version, true, false, null );
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
        String label = "VO"
                     + AdhocCode.createLabelChars( type + ": " + msg,
                                                   AdhocCode.LABEL_LENGTH - 2 );
        ReportCode code = new AdhocCode( type, label );
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
        reporter_.report( code, sbuf.toString() );
    }
}
