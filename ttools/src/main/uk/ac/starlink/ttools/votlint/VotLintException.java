package uk.ac.starlink.ttools.votlint;

/**
 * Exception used only for creating the debugging messages in VotLintContext.
 *
 * @author   Mark Taylor (Starlink)
 * @since    7 Apr 2005
 */
class VotLintException extends Exception {

    /**
     * Constructor.
     *
     * @param   msg  message
     */
    public VotLintException( String msg ) {
        super( msg );
    }
}
