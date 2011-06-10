package uk.ac.starlink.ttools.votlint;

/**
 * Attribute checker for the VOTABLE element's version attribute.
 *
 * @author   Mark Taylor (Starlink)
 * @since    7 Apr 2005
 */
public class VersionChecker implements AttributeChecker {

    public void check( String value, ElementHandler handler ) {
        VotLintContext context = handler.getContext();

        /* Check the stated version is known. */
        VotableVersion statedVersion =
            VotableVersion.getVersionByNumber( value ); 
        if ( statedVersion == null ) {
            context.warning( "Unknown VOTable version: " + value );
        }
        else {

            /* Compare with the version value in the context. */
            VotableVersion contextVersion = context.getVersion();

            /* If not yet set, set it now. */
            if ( contextVersion == null ) {
                context.setVersion( statedVersion );
            }

            /* If already set, check for consistency. */
            else if ( ! contextVersion.equals( statedVersion ) ) {
                context.warning( "Declared version ("
                               + statedVersion.getNumber()
                               + ") differs from version specified to linter ("
                               + contextVersion.getNumber()
                               + ")" );
            }
        }
    }
}
