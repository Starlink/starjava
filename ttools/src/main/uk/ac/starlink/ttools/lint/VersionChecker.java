package uk.ac.starlink.ttools.lint;

/**
 * Attribute checker for the VOTABLE element's version attribute.
 *
 * @author   Mark Taylor (Starlink)
 * @since    7 Apr 2005
 */
public class VersionChecker implements AttributeChecker {

    public void check( String value, ElementHandler handler ) {

        /* Update the version value in the context. */
        String version = handler.getContext().getVersion();
        if ( version == null ) {
            handler.getContext().setVersion( value );
        }
        else if ( ! version.equals( value ) ) {
            handler.warning( "Declared version (" + value + ") differs from " +
                             "version specified to linter (" + version + ")" );

        }
    }
}
