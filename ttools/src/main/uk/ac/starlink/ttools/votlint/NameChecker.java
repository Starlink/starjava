package uk.ac.starlink.ttools.votlint;

/**
 * Attribute checker for VOTable name elements.
 *
 * @author   Mark Taylor (Starlink)
 * @since    7 Apr 2005
 */
public class NameChecker implements AttributeChecker {

    public void check( String nameValue, ElementHandler handler ) {

        /* Check that no other element with the same parent has the same
         * name.  It's not illegal, but it's probably confusing. */
        ElementHandler parent = handler.getAncestry().getParent();
        if ( parent != null ) {
            parent.registerChildName( handler.getRef(), nameValue );
        }
    }
}
