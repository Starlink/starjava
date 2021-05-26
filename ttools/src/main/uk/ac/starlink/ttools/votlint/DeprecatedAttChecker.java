package uk.ac.starlink.ttools.votlint;

/**
 * Performs checking on attributes which are deprecated by the standard.
 *
 * @author   Mark Taylor (Starlink)
 * @since    7 Apr 2005
 */
public class DeprecatedAttChecker implements AttributeChecker {

    private final String attName_;

    /**
     * Constructor.
     *
     * @param  attName  attribute name
     */
    public DeprecatedAttChecker( String attName ) {
        attName_ = attName;
    }

    public void check( String attValue, ElementHandler handler ) {
        handler.warning( new VotLintCode( "DPR" ),
                         "Attribute " + attName_ + " is deprecated" );
    }
  
}
