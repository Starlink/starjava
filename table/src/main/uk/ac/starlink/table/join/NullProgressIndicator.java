package uk.ac.starlink.table.join;

/**
 * Dummy progress indicator.  All callbacks are no-ops.
 *
 * @author   Mark Taylor (Starlink)
 * @since    24 Mar 2004
 */
public class NullProgressIndicator implements ProgressIndicator {
    public void startStage( String stage ) {
    }
    public void setLevel( double level ) {
    }
    public void endStage() {
    }
    public void logMessage( String msg ) {
    }
}
