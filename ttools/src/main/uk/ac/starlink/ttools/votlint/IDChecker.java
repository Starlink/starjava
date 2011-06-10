package uk.ac.starlink.ttools.votlint;

/**
 * Attribute checker for processing ID attributes.
 */
public class IDChecker implements AttributeChecker {

    public void check( String value, ElementHandler handler ) {

        /* Register the ID value with the context. */
        handler.getContext().registerID( value, handler );
    }
}
