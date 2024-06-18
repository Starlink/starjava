package uk.ac.starlink.ttools.votlint;

/**
 * Defines how to perform checks on attributes.
 *
 * @author   Mark Taylor (Starlink)
 * @since    7 Apr 2005
 */
public interface AttributeChecker {

    /**
     * Performs a syntactic and/or semantic check on an attribute
     * value for a given element.  Anything worthy of comment should
     * be logged through <code>handler</code>'s context.
     *
     * @param   attValue   the value of the attribute to check
     * @param   handler   the element on which <code>attValue</code> appears
     */
    void check( String attValue, ElementHandler handler );
}
