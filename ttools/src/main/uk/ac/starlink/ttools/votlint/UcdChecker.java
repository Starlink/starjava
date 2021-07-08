package uk.ac.starlink.ttools.votlint;

/**
 * Attribute checker for checking UCD syntax.
 *
 * @author   Mark Taylor
 * @since    8 Jul 2021
 */
public class UcdChecker implements AttributeChecker {

    /** Singleton instance. */
    public static final UcdChecker INSTANCE = new UcdChecker();

    private UcdChecker() {
    }

    public void check( String ucd, ElementHandler handler ) {
        VotLintContext context = handler.getContext();
        if ( context.isCheckUcd() ) {
            UcdStatus status = UcdStatus.getStatus( ucd );
            if ( status != null ) {
                UcdStatus.Code code = status.getCode();
                if ( code.isError() ) {
                    context.error( new VotLintCode( "UCD" ),
                                   "Bad UCD \"" + ucd + "\""
                                  + " (" + code + "): "
                                  + status.getMessage() );
                }
                else if ( code.isWarning() ) {
                    context.warning( new VotLintCode( "UCW" ),
                                     "Questionable UCD \"" + ucd + "\""
                                   + " (" + code + "): "
                                   + status.getMessage() );
                }
            }
        }
    }
}
