package uk.ac.starlink.ttools.votlint;

import uk.ac.starlink.votable.VOTableVersion;

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
        VOTableVersion statedVersion =
            VOTableVersion.getKnownVersions().get( value ); 
        if ( statedVersion == null ) {
            context.warning( new VotLintCode( "VR9" ),
                             "Unknown VOTable version: " + value );
        }
        else {

            /* Compare with the version value in the context. */
            VOTableVersion contextVersion = context.getVersion();
            if ( ! contextVersion.equals( statedVersion ) ) {
                context.warning( new VotLintCode( "VRM" ),
                                 "Declared version "
                               + "(" + statedVersion + ")"
                               + " differs from version specified to linter "
                               + "(" + contextVersion + ")" );
            }
        }
    }
}
