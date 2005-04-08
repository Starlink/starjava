package uk.ac.starlink.ttools.lint;

/**
 * Exception used only for creating the debugging messages in LintContext.
 *
 * @author   Mark Taylor (Starlink)
 * @since    7 Apr 2005
 */
class LintException extends Exception {

    /**
     * Constructor.
     *
     * @param   msg  message
     */
    public LintException( String msg ) {
        super( msg );
    }
}
