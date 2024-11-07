package uk.ac.starlink.votable;

/**
 * Values permitted for the type attribute on the VOTable RESOURCE element.
 *
 * @author   Mark Taylor
 * @since    7 Nov 2024
 */
public enum ResourceType {

    /** For <code>&lt;RESOURCE type='results'/&gt;</code>. */
    RESULTS( "results" ),

    /** For <code>&lt;RESOURCE type='meta'/&gt;</code>. */
    META( "meta" );

    private final String typeValue_;

    /**
     * Constructor.
     *
     * @param  value of type attribute
     */
    ResourceType( String typeValue ) {
        typeValue_ = typeValue;
    }

    /**
     * Returns the attribute value.
     *
     * @return  RESOURCE type attribute value
     */
    @Override
    public String toString() {
        return typeValue_;
    }
}
